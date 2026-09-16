package cl.duoc.farmaexpress.bff.controller;

import cl.duoc.farmaexpress.bff.client.MicroserviceClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bff/catalog")
public class CatalogBffController {
    private static final String INTERNAL = "/api/catalog/medicamentos";
    private final MicroserviceClient client;

    public CatalogBffController(@Qualifier("catalogClient") MicroserviceClient client) {
        this.client = client;
    }

    @GetMapping({"", "/medicamentos"})
    public ResponseEntity<byte[]> find(@RequestParam MultiValueMap<String, String> query,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return client.exchange(HttpMethod.GET, INTERNAL, query, null, authorization);
    }

    @GetMapping("/medicamentos/{id}")
    public ResponseEntity<byte[]> get(@PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return client.exchange(HttpMethod.GET, INTERNAL + "/" + id, new LinkedMultiValueMap<>(), null, authorization);
    }

    @PostMapping(value = "/medicamentos", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> create(@RequestBody byte[] body,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return client.exchange(HttpMethod.POST, INTERNAL, new LinkedMultiValueMap<>(), body, authorization);
    }

    @PutMapping(value = "/medicamentos/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> update(@PathVariable Long id, @RequestBody byte[] body,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return client.exchange(HttpMethod.PUT, INTERNAL + "/" + id, new LinkedMultiValueMap<>(), body, authorization);
    }

    @DeleteMapping("/medicamentos/{id}")
    public ResponseEntity<byte[]> delete(@PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return client.exchange(HttpMethod.DELETE, INTERNAL + "/" + id, new LinkedMultiValueMap<>(), null, authorization);
    }
}
