package com.smartbi.metadata;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "data_source_definition")
public class DataSourceDefinition {
    @Id private UUID id;
    @Column(nullable = false, unique = true) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private DataSourceType type;
    @Column(name = "jdbc_url", nullable = false, length = 1000) private String jdbcUrl;
    @Column(nullable = false) private String username;
    @Column(name = "credential_reference", length = 500) private String credentialReference;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected DataSourceDefinition() {}
    public DataSourceDefinition(String name, DataSourceType type, String jdbcUrl, String username, String credentialReference) {
        this.id = UUID.randomUUID(); this.name = name; this.type = type; this.jdbcUrl = jdbcUrl;
        this.username = username; this.credentialReference = credentialReference; this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public DataSourceType getType() { return type; }
    public String getJdbcUrl() { return jdbcUrl; }
    public String getUsername() { return username; }
    public String getCredentialReference() { return credentialReference; }
    public Instant getCreatedAt() { return createdAt; }
}

