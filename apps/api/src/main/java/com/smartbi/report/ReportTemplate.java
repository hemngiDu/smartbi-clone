package com.smartbi.report;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="report_template")
public class ReportTemplate {
    @Id private UUID id;
    @Column(nullable=false,length=120) private String name;
    @Column(nullable=false,length=120) private String ownerId;
    @Column(nullable=false,columnDefinition="text") private String definitionJson;
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    protected ReportTemplate() {}
    public ReportTemplate(String name,String ownerId,String definitionJson){id=UUID.randomUUID();this.name=name;this.ownerId=ownerId;this.definitionJson=definitionJson;createdAt=updatedAt=Instant.now();}
    public void update(String name,String definitionJson){this.name=name;this.definitionJson=definitionJson;updatedAt=Instant.now();}
    public UUID getId(){return id;} public String getName(){return name;} public String getOwnerId(){return ownerId;} public String getDefinitionJson(){return definitionJson;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
