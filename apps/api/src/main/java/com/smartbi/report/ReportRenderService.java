package com.smartbi.report;

import com.smartbi.metadata.SqlDatasetRepository;
import com.smartbi.query.QueryExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.*;

@Service
public class ReportRenderService {
 private static final Pattern CELL_REF=Pattern.compile("([A-Za-z]+)([1-9][0-9]*)");
 private static final Pattern SUM=Pattern.compile("(?i)SUM\\s*\\(\\s*([A-Za-z]+[1-9][0-9]*)\\s*:\\s*([A-Za-z]+[1-9][0-9]*)\\s*\\)");
 private final SqlDatasetRepository datasets; private final QueryExecutionService queries;
 public ReportRenderService(SqlDatasetRepository datasets,QueryExecutionService queries){this.datasets=datasets;this.queries=queries;}

 public RenderedReport render(ReportDefinition d,Map<String,Object> parameters,String ownerId,boolean admin){
  validateDefinition(d);var regions=regions(d);var insertions=new ArrayList<Insertion>();var regionRows=new LinkedHashMap<Region,List<Map<String,Object>>>();
  for(var region:regions){
   var dataset=datasets.findById(region.datasetId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Dataset not found"));
   if(!admin&&!dataset.getOwnerId().equals(ownerId))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Resource access denied");
   var result=queries.execute(dataset,new QueryExecutionService.PreviewRequest(null,parameters,0,200),ownerId);
   var rows=aggregate(result.rows(),region.cells());regionRows.put(region,rows);
   if(rows.size()>1)insertions.add(new Insertion(region.expansion(),region.anchor(),rows.size()-1));
  }
  insertions.sort(Comparator.comparing((Insertion i)->i.expansion().name()).thenComparingInt(Insertion::at));
  var mappedMerges=d.merges().stream().map(m->mapMerge(m,insertions)).toList();var raw=new TreeMap<Position,RawCell>();
  var bound=regions.stream().flatMap(r->r.cells().stream()).collect(java.util.stream.Collectors.toSet());
  for(var cell:d.cells())if(!bound.contains(cell))put(raw,map(new Position(cell.row(),cell.column()),insertions),new RawCell(cell.value(),cell.style(),cell.binding()),mappedMerges);
  for(var entry:regionRows.entrySet()){var region=entry.getKey();int index=0;for(var row:entry.getValue()){
   for(var cell:region.cells()){var anchor=map(new Position(cell.row(),cell.column()),insertions);var target=region.expansion()==ReportDefinition.Expansion.DOWN?new Position(anchor.row()+index,anchor.column()):new Position(anchor.row(),anchor.column()+index);put(raw,target,new RawCell(row.get(cell.binding().field()),cell.style(),cell.binding()),mappedMerges);}index++;
  }}
  var evaluated=new HashMap<Position,Object>();var visiting=new HashSet<Position>();
  var cells=raw.entrySet().stream().map(e->new RenderedCell(e.getKey().row(),e.getKey().column(),evaluate(e.getKey(),raw,evaluated,visiting,insertions),e.getValue().style(),e.getValue().binding())).toList();
  int rows=d.rows()+insertions.stream().filter(i->i.expansion()==ReportDefinition.Expansion.DOWN).mapToInt(Insertion::count).sum();int columns=d.columns()+insertions.stream().filter(i->i.expansion()==ReportDefinition.Expansion.RIGHT).mapToInt(Insertion::count).sum();
  return new RenderedReport(cells,mappedMerges,mapSizes(d.rowHeights(),insertions,ReportDefinition.Expansion.DOWN),mapSizes(d.columnWidths(),insertions,ReportDefinition.Expansion.RIGHT),rows,columns);
 }

 private List<Region> regions(ReportDefinition d){
  var grouped=new LinkedHashMap<String,List<ReportDefinition.Cell>>();
  for(var c:d.cells())if(c.binding()!=null&&c.binding().expansion()!=null&&c.binding().expansion()!=ReportDefinition.Expansion.NONE){var b=c.binding();String key=b.regionId()!=null&&!b.regionId().isBlank()?"id:"+b.regionId():"legacy:"+b.datasetId()+":"+b.expansion()+":"+(b.expansion()==ReportDefinition.Expansion.DOWN?c.row():c.column());grouped.computeIfAbsent(key,k->new ArrayList<>()).add(c);}
  var out=new ArrayList<Region>();grouped.forEach((key,cells)->{var first=cells.getFirst();var b=first.binding();
   for(var c:cells){var x=c.binding();if(!x.datasetId().equals(b.datasetId())||x.expansion()!=b.expansion()||!Objects.equals(x.groupBy(),b.groupBy())||norm(x.aggregate())!=norm(b.aggregate()))fail("Binding region must use one dataset, direction and aggregation");}
   if(cells.stream().anyMatch(c->b.expansion()==ReportDefinition.Expansion.DOWN?c.row()!=first.row():c.column()!=first.column()))fail("Binding region fields must share an anchor row or column");
   out.add(new Region(key,b.datasetId(),b.expansion(),b.expansion()==ReportDefinition.Expansion.DOWN?first.row():first.column(),List.copyOf(cells)));
  });out.sort(Comparator.comparing((Region r)->r.expansion().name()).thenComparingInt(Region::anchor).thenComparing(Region::key));return out;
 }
 private ReportDefinition.Aggregate norm(ReportDefinition.Aggregate a){return a==null?ReportDefinition.Aggregate.NONE:a;}
 private List<Map<String,Object>> aggregate(List<Map<String,Object>> rows,List<ReportDefinition.Cell> cells){
  var b=cells.getFirst().binding();if(norm(b.aggregate())==ReportDefinition.Aggregate.NONE)return rows;if(b.groupBy()==null||b.groupBy().isBlank())fail("groupBy is required for aggregate binding");
  var groups=new LinkedHashMap<Object,List<Map<String,Object>>>();rows.forEach(r->groups.computeIfAbsent(r.get(b.groupBy()),x->new ArrayList<>()).add(r));var out=new ArrayList<Map<String,Object>>();
  groups.forEach((k,v)->{var m=new LinkedHashMap<String,Object>();m.put(b.groupBy(),k);for(var cell:cells){String field=cell.binding().field();if(field.equals(b.groupBy()))m.put(field,k);else m.put(field,norm(b.aggregate())==ReportDefinition.Aggregate.COUNT?v.size():v.stream().map(r->r.get(field)).filter(Number.class::isInstance).map(Number.class::cast).map(n->new BigDecimal(n.toString())).reduce(BigDecimal.ZERO,BigDecimal::add));}out.add(m);});return out;
 }
 private Object evaluate(Position p,Map<Position,RawCell> raw,Map<Position,Object> done,Set<Position> visiting,List<Insertion> insertions){
  if(done.containsKey(p))return done.get(p);var cell=raw.get(p);if(cell==null)return null;if(!visiting.add(p))return "#CYCLE!";Object value=cell.value();if(value instanceof String text&&text.startsWith("="))value=evalExpression(text.substring(1),raw,done,visiting,insertions);visiting.remove(p);done.put(p,value);return value;
 }
 private Object evalExpression(String expression,Map<Position,RawCell> raw,Map<Position,Object> done,Set<Position> visiting,List<Insertion> insertions){
  Matcher sum=SUM.matcher(expression.trim());if(sum.matches()){var a=map(parseRef(sum.group(1)),insertions);var b=map(parseRef(sum.group(2)),insertions);BigDecimal total=BigDecimal.ZERO;for(int r=Math.min(a.row(),b.row());r<=Math.max(a.row(),b.row());r++)for(int c=Math.min(a.column(),b.column());c<=Math.max(a.column(),b.column());c++){Object value=evaluate(new Position(r,c),raw,done,visiting,insertions);if(value==null||value.toString().isBlank())continue;if(isError(value))return value;var n=number(value);if(n==null)return "#VALUE!";total=total.add(n);}return total;}
  var parts=expression.split("\\+",-1);if(parts.length<2)return "#FORMULA!";BigDecimal total=BigDecimal.ZERO;for(var part:parts){Matcher ref=CELL_REF.matcher(part.trim());if(!ref.matches())return "#FORMULA!";var pos=map(parseRef(part.trim()),insertions);if(!raw.containsKey(pos))return "#REF!";Object value=evaluate(pos,raw,done,visiting,insertions);if(isError(value))return value;var n=number(value);if(n==null)return "#VALUE!";total=total.add(n);}return total;
 }
 private boolean isError(Object value){return value instanceof String text&&text.startsWith("#");}private BigDecimal number(Object value){if(value==null||isError(value))return null;try{return new BigDecimal(value.toString());}catch(NumberFormatException e){return null;}}
 private Position parseRef(String ref){Matcher m=CELL_REF.matcher(ref.trim());if(!m.matches())return new Position(-1,-1);int col=0;for(char c:m.group(1).toUpperCase().toCharArray())col=col*26+c-'A'+1;return new Position(Integer.parseInt(m.group(2))-1,col-1);}
 private Position map(Position p,List<Insertion> insertions){int row=p.row(),column=p.column();for(var i:insertions)if(i.expansion()==ReportDefinition.Expansion.DOWN&&p.row()>i.at())row+=i.count();else if(i.expansion()==ReportDefinition.Expansion.RIGHT&&p.column()>i.at())column+=i.count();return new Position(row,column);}
 private ReportDefinition.Merge mapMerge(ReportDefinition.Merge m,List<Insertion> is){var a=map(new Position(m.startRow(),m.startColumn()),is);var b=map(new Position(m.endRow(),m.endColumn()),is);return new ReportDefinition.Merge(a.row(),a.column(),b.row(),b.column());}
 private Map<Integer,Integer> mapSizes(Map<Integer,Integer> source,List<Insertion> insertions,ReportDefinition.Expansion expansion){var out=new TreeMap<Integer,Integer>();source.forEach((k,v)->{var p=expansion==ReportDefinition.Expansion.DOWN?map(new Position(k,0),insertions):map(new Position(0,k),insertions);out.put(expansion==ReportDefinition.Expansion.DOWN?p.row():p.column(),v);});return out;}
 private void put(Map<Position,RawCell> out,Position p,RawCell cell,List<ReportDefinition.Merge> merges){if(out.putIfAbsent(p,cell)!=null||insideMerge(p,merges))fail("Rendered cell conflicts at "+p.row()+","+p.column());}
 private boolean insideMerge(Position p,List<ReportDefinition.Merge> ms){return ms.stream().anyMatch(m->p.row()>=m.startRow()&&p.row()<=m.endRow()&&p.column()>=m.startColumn()&&p.column()<=m.endColumn()&&!(p.row()==m.startRow()&&p.column()==m.startColumn()));}
 private void fail(String message){throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,message);}
 public void validateDefinition(ReportDefinition d){if(d.rows()<1||d.rows()>500||d.columns()<1||d.columns()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Template dimensions out of range");var occupied=new HashSet<Position>();for(var c:d.cells()){if(c.row()<0||c.row()>=d.rows()||c.column()<0||c.column()>=d.columns())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cell outside template");if(!occupied.add(new Position(c.row(),c.column())))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Duplicate cell");}for(var m:d.merges())if(m.startRow()<0||m.startColumn()<0||m.endRow()<m.startRow()||m.endColumn()<m.startColumn()||m.endRow()>=d.rows()||m.endColumn()>=d.columns())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid merge");for(int i=0;i<d.merges().size();i++)for(int j=i+1;j<d.merges().size();j++)if(overlaps(d.merges().get(i),d.merges().get(j)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Overlapping merges");}
 private boolean overlaps(ReportDefinition.Merge a,ReportDefinition.Merge b){return a.startRow()<=b.endRow()&&b.startRow()<=a.endRow()&&a.startColumn()<=b.endColumn()&&b.startColumn()<=a.endColumn();}
 record Position(int row,int column) implements Comparable<Position>{public int compareTo(Position other){int r=Integer.compare(row,other.row);return r!=0?r:Integer.compare(column,other.column);}}
 record Insertion(ReportDefinition.Expansion expansion,int at,int count){} record Region(String key,UUID datasetId,ReportDefinition.Expansion expansion,int anchor,List<ReportDefinition.Cell> cells){} record RawCell(Object value,ReportDefinition.CellStyle style,ReportDefinition.Binding binding){}
 public record RenderedCell(int row,int column,Object value,ReportDefinition.CellStyle style,ReportDefinition.Binding binding){} public record RenderedReport(List<RenderedCell> cells,List<ReportDefinition.Merge> merges,Map<Integer,Integer> rowHeights,Map<Integer,Integer> columnWidths,int rows,int columns){}
}
