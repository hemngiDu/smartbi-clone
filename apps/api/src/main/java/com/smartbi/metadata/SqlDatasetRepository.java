package com.smartbi.metadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface SqlDatasetRepository extends JpaRepository<SqlDataset, UUID> {}

