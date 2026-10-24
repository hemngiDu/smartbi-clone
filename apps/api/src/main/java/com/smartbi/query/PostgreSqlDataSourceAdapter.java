package com.smartbi.query;

import com.smartbi.metadata.DataSourceDefinition;
import com.smartbi.metadata.DataSourceType;
import com.zaxxer.hikari.HikariConfig;
import org.springframework.stereotype.Component;

@Component
public class PostgreSqlDataSourceAdapter implements DataSourceAdapter {
    private final JdbcUrlPolicy urlPolicy;
    public PostgreSqlDataSourceAdapter(JdbcUrlPolicy urlPolicy) { this.urlPolicy = urlPolicy; }
    public boolean supports(DataSourceDefinition d) { return d.getType() == DataSourceType.POSTGRESQL; }
    public void validateUrl(String jdbcUrl) { urlPolicy.validatePostgreSql(jdbcUrl); }
    public void validateRuntime(DataSourceDefinition definition) { urlPolicy.validateResolvedTarget(definition.getJdbcUrl()); }
    public void configure(HikariConfig config, DataSourceDefinition d, char[] password) {
        validateRuntime(d);
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(d.getJdbcUrl());
        config.setUsername(d.getUsername());
        config.setPassword(new String(password));
        config.addDataSourceProperty("ApplicationName", "smartbi-query-engine");
        config.addDataSourceProperty("options", "-c default_transaction_read_only=on");
        config.addDataSourceProperty("readOnly", "true");
    }
}
