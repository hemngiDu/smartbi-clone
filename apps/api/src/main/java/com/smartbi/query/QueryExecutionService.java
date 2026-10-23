package com.smartbi.query;

import com.smartbi.metadata.SqlDataset;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service;
import java.sql.*; import java.time.*; import java.util.*; import java.util.concurrent.ConcurrentHashMap;

@Service
public class QueryExecutionService {
    private static final Logger audit=LoggerFactory.getLogger("AUDIT");
    private final ExternalPoolRegistry pools; private final SqlSafetyValidator validator; private final int timeoutSeconds,maxRows,maxPageSize;
    private final ConcurrentHashMap<UUID,ActiveQuery> active=new ConcurrentHashMap<>();
    public QueryExecutionService(ExternalPoolRegistry pools,SqlSafetyValidator validator,@Value("${smartbi.query.timeout-seconds:30}") int timeoutSeconds,@Value("${smartbi.query.max-rows:500}") int maxRows,@Value("${smartbi.query.max-page-size:200}") int maxPageSize){this.pools=pools;this.validator=validator;this.timeoutSeconds=timeoutSeconds;this.maxRows=maxRows;this.maxPageSize=maxPageSize;}
    public PreviewResult execute(SqlDataset dataset,PreviewRequest request,String ownerId){
        UUID queryId=request.queryId()==null?UUID.randomUUID():request.queryId(); int page=Math.max(0,request.page()); int size=request.size()<=0?100:Math.min(request.size(),maxPageSize);
        int remaining=maxRows-page*size;if(remaining<=0)throw new QueryException("PAGE_LIMIT_EXCEEDED","Requested page exceeds the maximum row limit");int fetch=Math.min(size,remaining);
        validator.validate(dataset.getSqlText()); var parsed=NamedParameterSql.parse(dataset.getSqlText()); String paged="select * from ("+parsed.sql()+") smartbi_page limit ? offset ?"; Instant started=Instant.now();
        try(Connection connection=pools.get(dataset.getDataSource()).getConnection()){
            connection.setReadOnly(true);connection.setAutoCommit(false);
            try(PreparedStatement statement=connection.prepareStatement(paged)){
                statement.setQueryTimeout(timeoutSeconds);statement.setMaxRows(fetch+1);statement.setFetchSize(Math.min(fetch+1,100));parsed.bind(statement,request.parameters()==null?Map.of():request.parameters());statement.setInt(parsed.names().size()+1,fetch+1);statement.setInt(parsed.names().size()+2,page*size);ActiveQuery activeQuery=new ActiveQuery(ownerId,statement);if(active.putIfAbsent(queryId,activeQuery)!=null)throw new QueryException("QUERY_ID_CONFLICT","Query id is already active");
                try(ResultSet rs=statement.executeQuery()){
                    var md=rs.getMetaData();var columns=new ArrayList<String>();for(int i=1;i<=md.getColumnCount();i++)columns.add(md.getColumnLabel(i));var rows=new ArrayList<Map<String,Object>>();boolean extra=false;
                    while(rs.next()){if(rows.size()>=fetch){extra=true;break;}var row=new LinkedHashMap<String,Object>();for(int i=1;i<=md.getColumnCount();i++)row.put(columns.get(i-1),rs.getObject(i));rows.add(row);}
                    boolean truncated=extra||page*size+rows.size()>=maxRows;audit.info("event=query_complete queryId={} datasetId={} dataSourceId={} rows={} durationMs={} status=success",queryId,dataset.getId(),dataset.getDataSource().getId(),rows.size(),Duration.between(started,Instant.now()).toMillis());return new PreviewResult(queryId,columns,rows,rows.size(),page,size,truncated);
                }finally{active.remove(queryId,activeQuery);}
            }catch(SQLException e){connection.rollback();throw e;}
        }catch(SQLException e){active.remove(queryId);audit.warn("event=query_complete queryId={} datasetId={} dataSourceId={} durationMs={} status=failure sqlState={}",queryId,dataset.getId(),dataset.getDataSource().getId(),Duration.between(started,Instant.now()).toMillis(),e.getSQLState());if("57014".equals(e.getSQLState()))throw new QueryException("QUERY_CANCELLED","Query was cancelled or timed out",e);throw new QueryException("QUERY_EXECUTION_FAILED","The data source could not execute the query",e);}
    }
    public boolean cancel(UUID queryId,String requester,boolean admin){ActiveQuery query=active.get(queryId);if(query==null)return false;if(!admin&&!query.ownerId().equals(requester))throw new org.springframework.security.access.AccessDeniedException("Query cancellation denied");try{query.statement().cancel();audit.info("event=query_cancel queryId={} requester={} status=requested",queryId,requester);return true;}catch(SQLException e){throw new QueryException("QUERY_CANCEL_FAILED","Query cancellation failed",e);}}
    record ActiveQuery(String ownerId,Statement statement){}
    public record PreviewRequest(UUID queryId,Map<String,Object> parameters,int page,int size){}
    public record PreviewResult(UUID queryId,List<String> columns,List<Map<String,Object>> rows,int rowCount,int page,int size,boolean truncated){}
}
