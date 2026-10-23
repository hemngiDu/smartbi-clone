package com.smartbi.api;
import com.smartbi.metadata.SqlDatasetRepository;import com.smartbi.query.QueryExecutionService;import org.springframework.http.HttpStatus;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.bind.annotation.*;import org.springframework.web.server.ResponseStatusException;import java.util.UUID;
@RestController @RequestMapping("/api/v1")
public class DatasetPreviewController{
 private final SqlDatasetRepository datasets;private final QueryExecutionService queries;public DatasetPreviewController(SqlDatasetRepository datasets,QueryExecutionService queries){this.datasets=datasets;this.queries=queries;}
 @PostMapping("/datasets/{id}/preview") @Transactional(readOnly=true) public QueryExecutionService.PreviewResult preview(@PathVariable UUID id,@RequestBody(required=false) QueryExecutionService.PreviewRequest request){var dataset=datasets.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Dataset not found"));return queries.execute(dataset,request==null?new QueryExecutionService.PreviewRequest(null,null,0,100):request);}
 @DeleteMapping("/queries/{queryId}") @ResponseStatus(HttpStatus.ACCEPTED) public CancelResult cancel(@PathVariable UUID queryId){return new CancelResult(queryId,queries.cancel(queryId));} public record CancelResult(UUID queryId,boolean cancellationRequested){}
}
