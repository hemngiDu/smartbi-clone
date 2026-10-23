package com.smartbi.api;

import com.smartbi.metadata.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1")
public class MetadataController {
    private final DataSourceRepository dataSources;
    private final SqlDatasetRepository datasets;
    public MetadataController(DataSourceRepository dataSources, SqlDatasetRepository datasets) {
        this.dataSources = dataSources; this.datasets = datasets;
    }

    @PostMapping("/data-sources") @ResponseStatus(HttpStatus.CREATED)
    public DataSourceView createDataSource(@Valid @RequestBody CreateDataSource request) {
        var saved = dataSources.save(new DataSourceDefinition(request.name(), request.type(), request.jdbcUrl(), request.username(), request.credentialReference()));
        return DataSourceView.from(saved);
    }
    @GetMapping("/data-sources")
    public List<DataSourceView> listDataSources() { return dataSources.findAll().stream().map(DataSourceView::from).toList(); }

    @PostMapping("/datasets") @ResponseStatus(HttpStatus.CREATED)
    public DatasetView createDataset(@Valid @RequestBody CreateDataset request) {
        validateReadOnlySql(request.sql());
        var source = dataSources.findById(request.dataSourceId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Data source not found"));
        return DatasetView.from(datasets.save(new SqlDataset(request.name(), source, request.sql().trim())));
    }
    @GetMapping("/datasets") @Transactional(readOnly = true)
    public List<DatasetView> listDatasets() { return datasets.findAll().stream().map(DatasetView::from).toList(); }

    static void validateReadOnlySql(String sql) {
        String normalized = sql.stripLeading().toLowerCase();
        Set<String> forbidden = Set.of("insert", "update", "delete", "merge", "drop", "alter", "create", "truncate", "grant", "revoke", "call", "copy");
        boolean containsWriteKeyword = forbidden.stream().anyMatch(keyword -> Pattern.compile("\\b" + keyword + "\\b").matcher(normalized).find());
        if (!(normalized.startsWith("select ") || normalized.startsWith("with ")) || normalized.contains(";") || normalized.contains("--") || normalized.contains("/*") || containsWriteKeyword) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only a single SELECT or WITH query is allowed");
        }
    }

    public record CreateDataSource(@NotBlank @Size(max=120) String name, @NotNull DataSourceType type,
                                   @NotBlank String jdbcUrl, @NotBlank String username, String credentialReference) {}
    public record DataSourceView(UUID id, String name, DataSourceType type, String jdbcUrl, String username, Instant createdAt) {
        static DataSourceView from(DataSourceDefinition value) { return new DataSourceView(value.getId(), value.getName(), value.getType(), value.getJdbcUrl(), value.getUsername(), value.getCreatedAt()); }
    }
    public record CreateDataset(@NotBlank @Size(max=120) String name, @NotNull UUID dataSourceId, @NotBlank String sql) {}
    public record DatasetView(UUID id, String name, UUID dataSourceId, String sql, Instant createdAt) {
        static DatasetView from(SqlDataset value) { return new DatasetView(value.getId(), value.getName(), value.getDataSource().getId(), value.getSqlText(), value.getCreatedAt()); }
    }
}

