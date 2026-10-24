package com.smartbi.query;

import com.smartbi.metadata.DataSourceDefinition;
import com.zaxxer.hikari.HikariConfig;

public interface DataSourceAdapter {
    boolean supports(DataSourceDefinition definition);
    void validateRuntime(DataSourceDefinition definition);
    void configure(HikariConfig config, DataSourceDefinition definition, char[] password);
}
