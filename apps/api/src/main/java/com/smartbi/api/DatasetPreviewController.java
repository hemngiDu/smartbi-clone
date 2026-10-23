package com.smartbi.api;

import com.smartbi.metadata.SqlDatasetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/v1/datasets")
public class DatasetPreviewController {
    private static final int MAX_ROWS = 500;
    private final SqlDatasetRepository datasets;
    private final JdbcTemplate jdbc;
    public DatasetPreviewController(SqlDatasetRepository datasets, JdbcTemplate jdbc) { this.datasets = datasets; this.jdbc = jdbc; }

    @PostMapping("/{id}/preview") @Transactional(readOnly = true)
    public PreviewResult preview(@PathVariable UUID id) {
        var dataset = datasets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dataset not found"));
        MetadataController.validateReadOnlySql(dataset.getSqlText());
        List<Map<String, Object>> rows = jdbc.query(connection -> {
            var statement = connection.prepareStatement(dataset.getSqlText());
            statement.setMaxRows(MAX_ROWS);
            statement.setQueryTimeout(30);
            return statement;
        }, new ColumnMapRowMapper());
        List<String> columns = rows.isEmpty() ? List.of() : new ArrayList<>(rows.getFirst().keySet());
        return new PreviewResult(columns, rows, rows.size(), rows.size() == MAX_ROWS);
    }
    public record PreviewResult(List<String> columns, List<Map<String,Object>> rows, int rowCount, boolean truncated) {}
}

