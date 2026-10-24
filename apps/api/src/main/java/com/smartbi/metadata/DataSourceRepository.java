package com.smartbi.metadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
public interface DataSourceRepository extends JpaRepository<DataSourceDefinition, UUID> { List<DataSourceDefinition> findAllByOwnerId(String ownerId); }

