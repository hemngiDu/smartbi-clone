package com.smartbi.metadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
public interface SqlDatasetRepository extends JpaRepository<SqlDataset, UUID> { List<SqlDataset> findAllByOwnerId(String ownerId); }

