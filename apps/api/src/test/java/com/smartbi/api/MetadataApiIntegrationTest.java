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

    @Test void createsSourceDatasetAndPreviewsReadOnlySql() throws Exception {
        String sourceBody = mvc.perform(post("/api/v1/data-sources").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"platform\",\"type\":\"POSTGRESQL\",\"jdbcUrl\":\"jdbc:postgresql://postgres/smartbi\",\"username\":\"smartbi\",\"credentialReference\":\"env://SPRING_DATASOURCE_PASSWORD\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.credentialReference").doesNotExist()).andReturn().getResponse().getContentAsString();
        JsonNode source = json.readTree(sourceBody);
        String datasetJson = "{\"name\":\"health_dataset\",\"dataSourceId\":\"%s\",\"sql\":\"select 1 as healthy\"}".formatted(source.get("id").asText());
        String datasetBody = mvc.perform(post("/api/v1/datasets").contentType(MediaType.APPLICATION_JSON).content(datasetJson))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String datasetId = json.readTree(datasetBody).get("id").asText();
        mvc.perform(post("/api/v1/datasets/{id}/preview", datasetId))
            .andExpect(status().isOk()).andExpect(jsonPath("$.rowCount").value(1)).andExpect(jsonPath("$.rows[0].healthy").value(1));
    }

    @Test void rejectsMutatingSql() throws Exception {
        String body = "{\"name\":\"bad\",\"dataSourceId\":\"00000000-0000-0000-0000-000000000000\",\"sql\":\"delete from sql_dataset\"}";
        mvc.perform(post("/api/v1/datasets").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }

    @Test void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}

