package com.smartbi.report;

import com.smartbi.metadata.*;
import com.smartbi.query.QueryExecutionService;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReportRenderServiceTest {
 @Test void expandsDatasetRowsDownDeterministically(){
  var repositories=mock(SqlDatasetRepository.class);var queries=mock(QueryExecutionService.class);var dataset=mock(SqlDataset.class);var id=UUID.randomUUID();
  when(dataset.getOwnerId()).thenReturn("alice");when(repositories.findById(id)).thenReturn(Optional.of(dataset));
  when(queries.execute(eq(dataset),any(),eq("alice"))).thenReturn(new QueryExecutionService.PreviewResult(UUID.randomUUID(),List.of("region"),List.of(Map.of("region","East"),Map.of("region","West")),2,0,200,false));
  var definition=new ReportDefinition(5,3,List.of(new ReportDefinition.Cell(1,0,"",null,new ReportDefinition.Binding(id,"region",ReportDefinition.Expansion.DOWN,null,ReportDefinition.Aggregate.NONE))),List.of(),Map.of(),Map.of());
  var result=new ReportRenderService(repositories,queries).render(definition,Map.of("minimum",100),"alice",false);
  assertThat(result.cells()).extracting(ReportRenderService.RenderedCell::value).containsExactly("East","West");
  verify(queries).execute(eq(dataset),argThat(r->r.parameters().equals(Map.of("minimum",100))),eq("alice"));
 }
 @Test void evaluatesSimpleAdditionFormula(){
  var service=new ReportRenderService(mock(SqlDatasetRepository.class),mock(QueryExecutionService.class));
  var cells=List.of(new ReportDefinition.Cell(0,0,"10",null,null),new ReportDefinition.Cell(0,1,"5",null,null),new ReportDefinition.Cell(0,2,"=A1+B1",null,null));
  assertThat(service.render(new ReportDefinition(2,3,cells,List.of(),Map.of(),Map.of()),Map.of(),"alice",false).cells().get(2).value().toString()).isEqualTo("15");
 }
}
