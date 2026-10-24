package com.smartbi.report;
import com.smartbi.metadata.*;
import com.smartbi.query.QueryExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportRenderServiceTest {
 private final SqlDatasetRepository repositories=mock(SqlDatasetRepository.class);private final QueryExecutionService queries=mock(QueryExecutionService.class);private final SqlDataset dataset=mock(SqlDataset.class);private final UUID id=UUID.randomUUID();
 ReportRenderServiceTest(){when(dataset.getOwnerId()).thenReturn("alice");when(repositories.findById(id)).thenReturn(Optional.of(dataset));}
 @Test void queriesARegionOnceAndKeepsFieldsOnTheSameRows(){
  result(List.of(Map.of("region","East","amount",10),Map.of("region","West","amount",20)));
  var a=new ReportDefinition.Binding(id,"region",ReportDefinition.Expansion.DOWN,null,ReportDefinition.Aggregate.NONE,"detail");var b=new ReportDefinition.Binding(id,"amount",ReportDefinition.Expansion.DOWN,null,ReportDefinition.Aggregate.NONE,"detail");
  var d=new ReportDefinition(5,3,List.of(cell(1,0,"",a),cell(1,1,"",b),cell(2,0,"footer",null)),List.of(),Map.of(2,44),Map.of());var rendered=service().render(d,Map.of("minimum",100),"alice",false);
  assertThat(rendered.cells()).extracting(ReportRenderService.RenderedCell::row,ReportRenderService.RenderedCell::column,ReportRenderService.RenderedCell::value).containsExactly(tuple(1,0,"East"),tuple(1,1,10),tuple(2,0,"West"),tuple(2,1,20),tuple(3,0,"footer"));
  assertThat(rendered.rowHeights()).containsEntry(3,44);verify(queries,times(1)).execute(eq(dataset),any(),eq("alice"));
 }
 @Test void shiftsStaticCellsAndMergesWhenRowsAreInserted(){
  result(List.of(Map.of("v",1),Map.of("v",2),Map.of("v",3)));var d=new ReportDefinition(5,4,List.of(cell(1,0,"",binding("v","r")),cell(3,1,"total",null)),List.of(new ReportDefinition.Merge(3,1,3,2)),Map.of(),Map.of(1,90));var rendered=service().render(d,Map.of(),"alice",false);
  assertThat(rendered.cells()).anySatisfy(c->{assertThat(c.row()).isEqualTo(5);assertThat(c.column()).isEqualTo(1);assertThat(c.value()).isEqualTo("total");});assertThat(rendered.merges()).containsExactly(new ReportDefinition.Merge(5,1,5,2));assertThat(rendered.rows()).isEqualTo(7);
 }
 @Test void evaluatesForwardReferencesAndSumAfterExpansion(){
  result(List.of(Map.of("amount",10),Map.of("amount",20),Map.of("amount",30)));var cells=List.of(cell(1,0,"",binding("amount","detail")),cell(2,1,"=C3+C3",null),cell(2,2,"2",null),cell(3,0,"=SUM(A2:A3)",null));var rendered=service().render(new ReportDefinition(5,3,cells,List.of(),Map.of(),Map.of()),Map.of(),"alice",false);
  assertThat(value(rendered,4,1).toString()).isEqualTo("4");assertThat(value(rendered,5,0).toString()).isEqualTo("60");
 }
 @Test void reportsCyclesAndRejectsExpandedMergeConflicts(){
  var cycle=service().render(new ReportDefinition(1,2,List.of(cell(0,0,"=B1+B1",null),cell(0,1,"=A1+A1",null)),List.of(),Map.of(),Map.of()),Map.of(),"alice",false);assertThat(value(cycle,0,0)).isEqualTo("#CYCLE!");
  result(List.of(Map.of("v",1),Map.of("v",2)));var conflict=new ReportDefinition(3,2,List.of(cell(0,0,"",binding("v","r"))),List.of(new ReportDefinition.Merge(0,0,1,0)),Map.of(),Map.of());assertThatThrownBy(()->service().render(conflict,Map.of(),"alice",false)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("conflicts");
 }
 private void result(List<Map<String,Object>> rows){when(queries.execute(eq(dataset),any(),eq("alice"))).thenReturn(new QueryExecutionService.PreviewResult(UUID.randomUUID(),List.copyOf(rows.getFirst().keySet()),rows,rows.size(),0,200,false));}
 private ReportRenderService service(){return new ReportRenderService(repositories,queries);}private ReportDefinition.Binding binding(String field,String region){return new ReportDefinition.Binding(id,field,ReportDefinition.Expansion.DOWN,null,ReportDefinition.Aggregate.NONE,region);}private ReportDefinition.Cell cell(int row,int col,String value,ReportDefinition.Binding binding){return new ReportDefinition.Cell(row,col,value,null,binding);}private Object value(ReportRenderService.RenderedReport report,int row,int col){return report.cells().stream().filter(c->c.row()==row&&c.column()==col).findFirst().orElseThrow().value();}
}
