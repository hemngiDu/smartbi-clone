package com.smartbi.metadata;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sql_dataset")
public class SqlDataset {
    @Id private UUID id;
    @Column(nullable = false, unique = true) private String name;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "data_source_id") private DataSourceDefinition dataSource;
    @Column(name = "sql_text", nullable = false, columnDefinition = "text") private String sqlText;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected SqlDataset() {}
    public SqlDataset(String name, DataSourceDefinition dataSource, String sqlText) {
        this.id = UUID.randomUUID(); this.name = name; this.dataSource = dataSource; this.sqlText = sqlText; this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public DataSourceDefinition getDataSource() { return dataSource; }
    public String getSqlText() { return sqlText; }
    public Instant getCreatedAt() { return createdAt; }
}

