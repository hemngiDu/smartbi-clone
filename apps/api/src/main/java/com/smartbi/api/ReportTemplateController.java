package com.smartbi.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.report.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@RestController @RequestMapping("/api/v1/report-templates")
public class ReportTemplateController {
 private final ReportTemplateRepository templates;private final ReportRenderService renderer;private final ObjectMapper json;
 public ReportTemplateController(ReportTemplateRepository templates,ReportRenderService renderer,ObjectMapper json){this.templates=templates;this.renderer=renderer;this.json=json;}
 @GetMapping @Transactional(readOnly=true) public List<View> list(Authentication auth){return (MetadataController.isAdmin(auth)?templates.findAll():templates.findAllByOwnerIdOrderByUpdatedAtDesc(auth.getName())).stream().map(this::view).toList();}
 @GetMapping("/{id}") @Transactional(readOnly=true) public View get(@PathVariable UUID id,Authentication auth){return view(owned(id,auth));}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','EDITOR')") public View create(@Valid @RequestBody SaveRequest request,Authentication auth){String definition=serialize(request.definition());return view(templates.save(new ReportTemplate(request.name().trim(),auth.getName(),definition)));}
 @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','EDITOR')") public View update(@PathVariable UUID id,@Valid @RequestBody SaveRequest request,Authentication auth){var template=owned(id,auth);template.update(request.name().trim(),serialize(request.definition()));return view(templates.save(template));}
 @PostMapping("/{id}/render") @Transactional(readOnly=true) public ReportRenderService.RenderedReport render(@PathVariable UUID id,@RequestBody(required=false) RenderRequest request,Authentication auth){var t=owned(id,auth);return renderer.render(parse(t.getDefinitionJson()),request==null||request.parameters()==null?Map.of():request.parameters(),auth.getName(),MetadataController.isAdmin(auth));}
 private ReportTemplate owned(UUID id,Authentication auth){var value=templates.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Report template not found"));MetadataController.requireOwner(value.getOwnerId(),auth);return value;}
 private String serialize(ReportDefinition value){renderer.validateDefinition(value);try{return json.writeValueAsString(value);}catch(JsonProcessingException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid report definition",e);}}
 private ReportDefinition parse(String value){try{return json.readValue(value,ReportDefinition.class);}catch(JsonProcessingException e){throw new IllegalStateException("Stored report definition is invalid",e);}}
 private View view(ReportTemplate value){return new View(value.getId(),value.getName(),parse(value.getDefinitionJson()),value.getCreatedAt(),value.getUpdatedAt());}
 public record SaveRequest(@NotBlank @Size(max=120) String name,@NotNull @Valid ReportDefinition definition){}
 public record RenderRequest(Map<String,Object> parameters){}
 public record View(UUID id,String name,ReportDefinition definition,Instant createdAt,Instant updatedAt){}
}
