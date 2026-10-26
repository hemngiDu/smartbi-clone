package com.smartbi.report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate,UUID>{List<ReportTemplate> findAllByOwnerIdOrderByUpdatedAtDesc(String ownerId);}
