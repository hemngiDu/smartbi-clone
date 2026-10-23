package com.smartbi.api;

import com.smartbi.metadata.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.smartbi.query.*;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class MetadataController {
    private final DataSourceRepository dataSources;
    private final SqlDatasetRepository datasets;
    private final SqlSafetyValidator validator;
    private final ExternalPoolRegistry pools;
    public MetadataController(DataSourceRepository dataSources, SqlDatasetRepository datasets, SqlSafetyValidator validator, ExternalPoolRegistry pools) {
        this.dataSources = dataSources; this.datasets = datasets; this.validator = validator; this.pools = pools;
    }

    @PostMapping("/data-sources") @ResponseStatus(HttpStatus.CREATED)
    public DataSourceView createDataSource(@Valid @RequestBody CreateDataSource request) {
        validateSource(request);
        var saved = dataSources.save(new DataSourceDefinition(request.name(), request.type(), request.jdbcUrl(), request.username(), request.credentialReference()));
        return DataSourceView.from(saved);
    }
    @GetMapping("/data-sources")
    public List<DataSourceView> listDataSources() { return dataSources.findAll().stream().map(DataSourceView::from).toList(); }

    @PostMapping("/data-sources/{id}/test")
    public ConnectionTest testDataSource(@PathVariable UUID id) {
        var source = dataSources.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Data source not found"));
        long started = System.nanoTime();
        try (var temporary = pools.create(source); Connection connection = temporary.getConnection()) {
            return new ConnectionTest(true, (System.nanoTime()-started)/1_000_000, connection.getMetaData().getDatabaseProductName(), connection.getMetaData().getDatabaseProductVersion());
        } catch (QueryException e) { throw e; }
        catch (Exception e) { throw new QueryException("DATASOURCE_CONNECTION_FAILED", "Could not connect to the data source", e); }
    }

    @PostMapping("/datasets") @ResponseStatus(HttpStatus.CREATED)
    public DatasetView createDataset(@Valid @RequestBody CreateDataset request) {
        validator.validate(request.sql());
        var source = dataSources.findById(request.dataSourceId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Data source not found"));
        return DatasetView.from(datasets.save(new SqlDataset(request.name(), source, request.sql().trim())));
    }
    @GetMapping("/datasets") @Transactional(readOnly = true)
    public List<DatasetView> listDatasets() { return datasets.findAll().stream().map(DatasetView::from).toList(); }

    private static void validateSource(CreateDataSource request) {
        if (request.type() != DataSourceType.POSTGRESQL) throw new QueryException("DATASOURCE_TYPE_UNSUPPORTED", "Only PostgreSQL is currently implemented");
        if (!request.jdbcUrl().startsWith("jdbc:postgresql://")) throw new QueryException("DATASOURCE_URL_INVALID", "PostgreSQL JDBC URL is required");
        if (request.credentialReference() == null || !request.credentialReference().startsWith("env://")) throw new QueryException("CREDENTIAL_REFERENCE_INVALID", "Use an env:// credential reference; plaintext passwords are never accepted");
    }

    public record CreateDataSource(@NotBlank @Size(max=120) String name, @NotNull DataSourceType type,
                                   @NotBlank String jdbcUrl, @NotBlank String username, String credentialReference) {}
    public record DataSourceView(UUID id, String name, DataSourceType type, String jdbcUrl, String username, Instant createdAt) {
        static DataSourceView from(DataSourceDefinition value) { return new DataSourceView(value.getId(), value.getName(), value.getType(), value.getJdbcUrl(), value.getUsername(), value.getCreatedAt()); }
    }
    public record ConnectionTest(boolean connected, long latencyMs, String product, String version) {}
    public record CreateDataset(@NotBlank @Size(max=120) String name, @NotNull UUID dataSourceId, @NotBlank String sql) {}
    public record DatasetView(UUID id, String name, UUID dataSourceId, String sql, Instant createdAt) {
        static DatasetView from(SqlDataset value) { return new DatasetView(value.getId(), value.getName(), value.getDataSource().getId(), value.getSqlText(), value.getCreatedAt()); }
    }
}

