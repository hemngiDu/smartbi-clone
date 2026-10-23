package com.smartbi.metadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface DataSourceRepository extends JpaRepository<DataSourceDefinition, UUID> {}

