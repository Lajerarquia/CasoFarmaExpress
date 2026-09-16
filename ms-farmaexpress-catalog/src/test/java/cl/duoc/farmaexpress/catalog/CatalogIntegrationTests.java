package cl.duoc.farmaexpress.catalog;

import cl.duoc.farmaexpress.catalog.repository.MedicamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired MedicamentoRepository repository;
    private final JsonMapper json = JsonMapper.builder().build();
    private static final String BASE = "/api/catalog/medicamentos";

    @BeforeEach
    void clearData() { repository.deleteAll(); }

    @Test
    void completeCrudWorksWithoutAuthentication() throws Exception {
        long id = create("MED-001");
        mvc.perform(get(BASE)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("MED-001"));
        mvc.perform(get(BASE + "/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Paracetamol"))
                .andExpect(jsonPath("$.precio").value(1990));
        mvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(payload("MED-001", "2490.50", "3")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(3))
                .andExpect(jsonPath("$.precio").value(2490.50));
        assertEquals(3, repository.findById(id).orElseThrow().getStock());
        mvc.perform(delete(BASE + "/" + id)).andExpect(status().isNoContent());
        mvc.perform(get(BASE + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void duplicateSkuReturnsConflictOnCreateAndUpdate() throws Exception {
        create("MED-001");
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                .content(payload(" MED-001 ", "100", "1"))).andExpect(status().isConflict());
        long second = create("MED-002");
        mvc.perform(put(BASE + "/" + second).contentType(MediaType.APPLICATION_JSON)
                .content(payload("MED-001", "100", "1"))).andExpect(status().isConflict());
        assertEquals("MED-002", repository.findById(second).orElseThrow().getSku());
    }

    @Test
    void rejectsInvalidPricesStocksAndRequiredFields() throws Exception {
        for (String body : new String[] {
                "{}", payload(" ", "10", "1"), payload("MED", "-1", "1"),
                payload("MED", "1.999", "1"), payload("MED", "10000000000", "1"),
                payload("MED", "10", "-1"), payload("a".repeat(101), "10", "1"),
                payload("MED", "null", "1"), payload("MED", "10", "null")}) {
            mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, repository.count());
    }

    @Test
    void missingUpdateAndDeleteReturn404() throws Exception {
        mvc.perform(put(BASE + "/99999").contentType(MediaType.APPLICATION_JSON)
                .content(payload("MED", "10", "1"))).andExpect(status().isNotFound());
        mvc.perform(delete(BASE + "/99999")).andExpect(status().isNotFound());
    }

    private long create(String sku) throws Exception {
        String body = mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                .content(payload(sku, "1990", "120")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode created = json.readTree(body);
        return created.get("id").asLong();
    }

    private String payload(String sku, String precio, String stock) {
        return "{\"sku\":\"" + sku + "\",\"nombre\":\"Paracetamol\",\"precio\":" + precio
                + ",\"stock\":" + stock + "}";
    }
}
