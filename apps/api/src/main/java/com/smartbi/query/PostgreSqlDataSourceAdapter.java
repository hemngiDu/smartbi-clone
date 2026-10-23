package com.smartbi.query;

import com.smartbi.metadata.DataSourceDefinition;
import com.smartbi.metadata.DataSourceType;
import com.zaxxer.hikari.HikariConfig;
import org.springframework.stereotype.Component;

@Component
public class PostgreSqlDataSourceAdapter implements DataSourceAdapter {
    public boolean supports(DataSourceDefinition d) { return d.getType() == DataSourceType.POSTGRESQL; }
    public void configure(HikariConfig config, DataSourceDefinition d, char[] password) {
        if (!d.getJdbcUrl().startsWith("jdbc:postgresql://")) throw new QueryException("DATASOURCE_URL_INVALID", "PostgreSQL JDBC URL is required");
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(d.getJdbcUrl());
        config.setUsername(d.getUsername());
        config.setPassword(new String(password));
        config.addDataSourceProperty("ApplicationName", "smartbi-query-engine");
        config.addDataSourceProperty("options", "-c default_transaction_read_only=on");
    }
}
