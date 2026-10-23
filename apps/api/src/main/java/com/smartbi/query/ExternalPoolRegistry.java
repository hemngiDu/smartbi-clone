package com.smartbi.query;

import com.smartbi.metadata.DataSourceDefinition;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExternalPoolRegistry {
    private final ConcurrentHashMap<UUID, HikariDataSource> pools = new ConcurrentHashMap<>();
    private final List<DataSourceAdapter> adapters;
    private final CredentialResolver credentials;
    private final int maximumPoolSize;
    private final long connectionTimeoutMs;
    public ExternalPoolRegistry(List<DataSourceAdapter> adapters, CredentialResolver credentials,
                                @Value("${smartbi.pools.maximum-pool-size:5}") int maximumPoolSize,
                                @Value("${smartbi.pools.connection-timeout-ms:5000}") long connectionTimeoutMs) {
        this.adapters = adapters; this.credentials = credentials; this.maximumPoolSize = maximumPoolSize; this.connectionTimeoutMs = connectionTimeoutMs;
    }
    public DataSource get(DataSourceDefinition definition) { return pools.computeIfAbsent(definition.getId(), ignored -> create(definition)); }
    public HikariDataSource create(DataSourceDefinition definition) {
        var adapter = adapters.stream().filter(a -> a.supports(definition)).findFirst()
            .orElseThrow(() -> new QueryException("DATASOURCE_TYPE_UNSUPPORTED", "Data source type is not implemented yet"));
        char[] password = credentials.resolve(definition.getCredentialReference());
        try {
            var config = new HikariConfig();
            config.setPoolName("smartbi-" + definition.getId()); config.setMaximumPoolSize(maximumPoolSize); config.setMinimumIdle(0);
            config.setConnectionTimeout(connectionTimeoutMs); config.setValidationTimeout(Math.min(connectionTimeoutMs, 3000));
            config.setReadOnly(true); config.setAutoCommit(false); config.setInitializationFailTimeout(connectionTimeoutMs);
            adapter.configure(config, definition, password);
            return new HikariDataSource(config);
        } finally { Arrays.fill(password, '\0'); }
    }
    public void evict(UUID id) { var pool = pools.remove(id); if (pool != null) pool.close(); }
    @PreDestroy public void close() { pools.values().forEach(HikariDataSource::close); pools.clear(); }
}
