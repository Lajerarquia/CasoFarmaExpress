package cl.duoc.farmaexpress.bff.controller;

import cl.duoc.farmaexpress.bff.client.MicroserviceClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bff/prescriptions")
public class PrescriptionBffController {
    private final MicroserviceClient client;

    public PrescriptionBffController(@Qualifier("prescriptionsClient") MicroserviceClient client) {
        this.client = client;
    }

    @GetMapping
    public ResponseEntity<byte[]> find(@RequestParam MultiValueMap<String, String> query,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return client.exchange(HttpMethod.GET, "/api/prescriptions", query, null, authorization);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return client.exchange(HttpMethod.GET, "/api/prescriptions/" + id, new LinkedMultiValueMap<>(), null, authorization);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> create(@RequestBody byte[] body,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return client.exchange(HttpMethod.POST, "/api/prescriptions", new LinkedMultiValueMap<>(), body, authorization);
    }

    @PutMapping(value = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> update(@PathVariable Long id, @RequestBody byte[] body,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return client.exchange(HttpMethod.PUT, "/api/prescriptions/" + id + "/status",
                new LinkedMultiValueMap<>(), body, authorization);
    }
}
