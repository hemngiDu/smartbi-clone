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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

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
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public DataSourceView createDataSource(@Valid @RequestBody CreateDataSource request, Authentication authentication) {
        pools.validateDefinition(request.type(), request.jdbcUrl()); validateCredential(request);
        var saved = dataSources.save(new DataSourceDefinition(request.name(), request.type(), request.jdbcUrl(), request.username(), request.credentialReference(), authentication.getName()));
        return DataSourceView.from(saved);
    }
    @GetMapping("/data-sources")
    public List<DataSourceView> listDataSources(Authentication auth) { return (isAdmin(auth) ? dataSources.findAll() : dataSources.findAllByOwnerId(auth.getName())).stream().map(DataSourceView::from).toList(); }

    @PostMapping("/data-sources/{id}/test")
    public ConnectionTest testDataSource(@PathVariable UUID id, Authentication auth) {
        var source = dataSources.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Data source not found"));
        requireOwner(source.getOwnerId(), auth);
        long started = System.nanoTime();
        try (var temporary = pools.create(source); Connection connection = temporary.getConnection()) {
            return new ConnectionTest(true, (System.nanoTime()-started)/1_000_000, connection.getMetaData().getDatabaseProductName(), connection.getMetaData().getDatabaseProductVersion());
        } catch (QueryException e) { throw e; }
        catch (Exception e) { throw new QueryException("DATASOURCE_CONNECTION_FAILED", "Could not connect to the data source", e); }
    }

    @PostMapping("/datasets") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public DatasetView createDataset(@Valid @RequestBody CreateDataset request, Authentication auth) {
        validator.validate(request.sql());
        var source = dataSources.findById(request.dataSourceId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Data source not found"));
        requireOwner(source.getOwnerId(), auth);
        return DatasetView.from(datasets.save(new SqlDataset(request.name(), source, request.sql().trim(), auth.getName())));
    }
    @GetMapping("/datasets") @Transactional(readOnly = true)
    public List<DatasetView> listDatasets(Authentication auth) { return (isAdmin(auth) ? datasets.findAll() : datasets.findAllByOwnerId(auth.getName())).stream().map(DatasetView::from).toList(); }

    private static void validateCredential(CreateDataSource request) {
        if (request.credentialReference() == null || !request.credentialReference().startsWith("env://")) throw new QueryException("CREDENTIAL_REFERENCE_INVALID", "Use an env:// credential reference; plaintext passwords are never accepted");
    }
    static boolean isAdmin(Authentication auth) { return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")); }
    static void requireOwner(String owner, Authentication auth) { if (!isAdmin(auth) && !owner.equals(auth.getName())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Resource access denied"); }

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

