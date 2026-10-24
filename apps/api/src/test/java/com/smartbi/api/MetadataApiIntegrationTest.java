package com.smartbi.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class MetadataApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test void createsSourceAndDatasetWithoutReturningCredentialReference() throws Exception {
        String sourceBody = mvc.perform(post("/api/v1/data-sources").header("X-API-Key","test-alice-key").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"platform\",\"type\":\"POSTGRESQL\",\"jdbcUrl\":\"jdbc:postgresql://postgres/smartbi\",\"username\":\"smartbi\",\"credentialReference\":\"env://SPRING_DATASOURCE_PASSWORD\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.credentialReference").doesNotExist()).andReturn().getResponse().getContentAsString();
        JsonNode source = json.readTree(sourceBody);
        String datasetJson = "{\"name\":\"health_dataset\",\"dataSourceId\":\"%s\",\"sql\":\"select 1 as healthy\"}".formatted(source.get("id").asText());
        String datasetBody = mvc.perform(post("/api/v1/datasets").header("X-API-Key","test-alice-key").contentType(MediaType.APPLICATION_JSON).content(datasetJson))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(datasetBody).get("dataSourceId").asText()).isEqualTo(source.get("id").asText());
    }

    @Test void rejectsMutatingSql() throws Exception {
        String body = "{\"name\":\"bad\",\"dataSourceId\":\"00000000-0000-0000-0000-000000000000\",\"sql\":\"delete from sql_dataset\"}";
        mvc.perform(post("/api/v1/datasets").header("X-API-Key","test-alice-key").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }

    @Test void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test void defaultsToDenyAndIsolatesResources() throws Exception {
        mvc.perform(get("/api/v1/data-sources")).andExpect(status().isUnauthorized());
        String name="private-"+java.util.UUID.randomUUID();
        String created=mvc.perform(post("/api/v1/data-sources").header("X-API-Key","test-alice-key").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\""+name+"\",\"type\":\"POSTGRESQL\",\"jdbcUrl\":\"jdbc:postgresql://postgres:5432/smartbi\",\"username\":\"reader\",\"credentialReference\":\"env://TEST_ONLY\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id=json.readTree(created).get("id").asText();
        mvc.perform(post("/api/v1/data-sources/"+id+"/test").header("X-API-Key","test-bob-key")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/datasets").header("X-API-Key","test-bob-key").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"cross-"+java.util.UUID.randomUUID()+"\",\"dataSourceId\":\""+id+"\",\"sql\":\"select 1\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/data-sources").header("X-API-Key","test-bob-key")).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '"+id+"')]").isEmpty());
    }

    @Test void rejectsSsrFAndDangerousJdbcParameters() throws Exception {
        for(String url:java.util.List.of("jdbc:postgresql://169.254.169.254/meta","jdbc:postgresql://postgres:5432/smartbi?socketFactory=evil","jdbc:mysql://postgres/db"))
            mvc.perform(post("/api/v1/data-sources").header("X-API-Key","test-alice-key").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"bad-"+java.util.UUID.randomUUID()+"\",\"type\":\"POSTGRESQL\",\"jdbcUrl\":\""+url+"\",\"username\":\"reader\",\"credentialReference\":\"env://TEST_ONLY\"}")).andExpect(status().isBadRequest());
    }

    @Test void rejectsCredentialReferencesOutsideServerAllowList() throws Exception {
        mvc.perform(post("/api/v1/data-sources").header("X-API-Key","test-alice-key").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"bad-credential\",\"type\":\"POSTGRESQL\",\"jdbcUrl\":\"jdbc:postgresql://postgres:5432/smartbi\",\"username\":\"reader\",\"credentialReference\":\"env://PATH\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(System.getenv("PATH")))));
    }

    @Test void savesReopensAndIsolatesReportTemplate() throws Exception {
        String unique="report-"+java.util.UUID.randomUUID();
        String payload="{\"name\":\""+unique+"\",\"definition\":{\"rows\":4,\"columns\":3,\"cells\":[{\"row\":0,\"column\":0,\"value\":\"Title\",\"style\":{\"bold\":true}}],\"merges\":[],\"rowHeights\":{\"0\":32},\"columnWidths\":{\"0\":140}}}";
        String body=mvc.perform(post("/api/v1/report-templates").header("X-API-Key","test-alice-key").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.definition.cells[0].value").value("Title")).andReturn().getResponse().getContentAsString();
        String id=json.readTree(body).get("id").asText();
        mvc.perform(get("/api/v1/report-templates/"+id).header("X-API-Key","test-alice-key")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value(unique));
        mvc.perform(get("/api/v1/report-templates/"+id).header("X-API-Key","test-bob-key")).andExpect(status().isForbidden());
    }
}

