package com.smartbi.report;

import com.smartbi.metadata.SqlDatasetRepository;
import com.smartbi.query.QueryExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;

@Service
public class ReportRenderService {
 private final SqlDatasetRepository datasets; private final QueryExecutionService queries;
 public ReportRenderService(SqlDatasetRepository datasets,QueryExecutionService queries){this.datasets=datasets;this.queries=queries;}
 public RenderedReport render(ReportDefinition d,Map<String,Object> parameters,String ownerId,boolean admin){
  validateDefinition(d); var out=new TreeMap<Position,RenderedCell>(Comparator.comparingInt(Position::row).thenComparingInt(Position::column));
  for(var cell:d.cells()) if(cell.binding()==null||cell.binding().expansion()==ReportDefinition.Expansion.NONE) out.put(new Position(cell.row(),cell.column()),new RenderedCell(cell.row(),cell.column(),formula(cell.value(),out),cell.style(),cell.binding()));
  var occupied=new HashSet<>(out.keySet()); var merges=new ArrayList<>(d.merges());
  for(var cell:d.cells()) if(cell.binding()!=null&&cell.binding().expansion()!=ReportDefinition.Expansion.NONE){
   var dataset=datasets.findById(cell.binding().datasetId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Dataset not found"));
   if(!admin&&!dataset.getOwnerId().equals(ownerId))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Resource access denied");
   var result=queries.execute(dataset,new QueryExecutionService.PreviewRequest(null,parameters,0,200),ownerId); var rows=aggregate(result.rows(),cell.binding()); int i=0;
   for(var row:rows){int rr=cell.row()+(cell.binding().expansion()==ReportDefinition.Expansion.DOWN?i:0),cc=cell.column()+(cell.binding().expansion()==ReportDefinition.Expansion.RIGHT?i:0);var p=new Position(rr,cc);if(!occupied.add(p)||insideMerge(p,merges))throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Expanded cell conflicts at "+rr+","+cc);out.put(p,new RenderedCell(rr,cc,row.get(cell.binding().field()),cell.style(),cell.binding()));i++;}
  }
  return new RenderedReport(out.values().stream().toList(),merges,d.rowHeights(),d.columnWidths(),out.keySet().stream().mapToInt(Position::row).max().orElse(d.rows()-1)+1,out.keySet().stream().mapToInt(Position::column).max().orElse(d.columns()-1)+1);
 }
 private List<Map<String,Object>> aggregate(List<Map<String,Object>> rows,ReportDefinition.Binding b){if(b.aggregate()==null||b.aggregate()==ReportDefinition.Aggregate.NONE)return rows;if(b.groupBy()==null||b.groupBy().isBlank())throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"groupBy is required for aggregate binding");var groups=new LinkedHashMap<Object,List<Map<String,Object>>>();rows.forEach(r->groups.computeIfAbsent(r.get(b.groupBy()),x->new ArrayList<>()).add(r));var out=new ArrayList<Map<String,Object>>();groups.forEach((k,v)->{var m=new LinkedHashMap<String,Object>();m.put(b.groupBy(),k);m.put(b.field(),b.aggregate()==ReportDefinition.Aggregate.COUNT?v.size():v.stream().map(r->r.get(b.field())).filter(Number.class::isInstance).map(Number.class::cast).map(n->new BigDecimal(n.toString())).reduce(BigDecimal.ZERO,BigDecimal::add));out.add(m);});return out;}
 private Object formula(String value,Map<Position,RenderedCell> out){if(value==null||!value.startsWith("="))return value;var parts=value.substring(1).split("\\+");BigDecimal sum=BigDecimal.ZERO;for(var part:parts){var p=parseRef(part.trim());var v=out.get(p);if(v==null||!(v.value() instanceof Number)&&!(v.value() instanceof String))return "#REF!";try{sum=sum.add(new BigDecimal(v.value().toString()));}catch(NumberFormatException e){return "#VALUE!";}}return sum.stripTrailingZeros();}
 private Position parseRef(String ref){try{int split=0;while(split<ref.length()&&Character.isLetter(ref.charAt(split)))split++;int col=0;for(char c:ref.substring(0,split).toUpperCase().toCharArray())col=col*26+(c-'A'+1);return new Position(Integer.parseInt(ref.substring(split))-1,col-1);}catch(Exception e){return new Position(-1,-1);}}
 private boolean insideMerge(Position p,List<ReportDefinition.Merge> ms){return ms.stream().anyMatch(m->p.row()>=m.startRow()&&p.row()<=m.endRow()&&p.column()>=m.startColumn()&&p.column()<=m.endColumn()&&!(p.row()==m.startRow()&&p.column()==m.startColumn()));}
 public void validateDefinition(ReportDefinition d){if(d.rows()<1||d.rows()>500||d.columns()<1||d.columns()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Template dimensions out of range");for(var c:d.cells())if(c.row()<0||c.row()>=d.rows()||c.column()<0||c.column()>=d.columns())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cell outside template");for(var m:d.merges())if(m.startRow()<0||m.startColumn()<0||m.endRow()<m.startRow()||m.endColumn()<m.startColumn()||m.endRow()>=d.rows()||m.endColumn()>=d.columns())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid merge");}
 record Position(int row,int column){}
 public record RenderedCell(int row,int column,Object value,ReportDefinition.CellStyle style,ReportDefinition.Binding binding){}
 public record RenderedReport(List<RenderedCell> cells,List<ReportDefinition.Merge> merges,Map<Integer,Integer> rowHeights,Map<Integer,Integer> columnWidths,int rows,int columns){}
}
