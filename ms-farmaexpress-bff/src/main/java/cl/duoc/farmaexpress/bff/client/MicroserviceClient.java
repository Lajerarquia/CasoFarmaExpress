package cl.duoc.farmaexpress.bff.client;

import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import java.util.List;

/** Forwards only explicit API routes; never accepts a destination URL from a browser. */
public class MicroserviceClient {
    private final RestClient restClient;

    public MicroserviceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public ResponseEntity<byte[]> exchange(HttpMethod method, String path,
            MultiValueMap<String, String> query, byte[] body, String authorization) {
        RestClient.RequestBodySpec request = restClient.method(method)
                .uri(builder -> builder.path(path).queryParams(query).build())
                .accept(MediaType.APPLICATION_JSON);
        // Relay point for the future OAuth2 integration. No token validation is performed here yet.
        if (authorization != null && !authorization.isBlank()) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        // exchange preserves upstream 4xx/5xx responses instead of converting them into BFF errors.
        return request.exchange((sent, received) -> {
            HttpHeaders headers = new HttpHeaders();
            for (String name : List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.RETRY_AFTER,
                    HttpHeaders.WWW_AUTHENTICATE)) {
                List<String> values = received.getHeaders().get(name);
                if (values != null) {
                    headers.put(name, values);
                }
            }
            return new ResponseEntity<>(received.getBody().readAllBytes(), headers, received.getStatusCode());
        });
    }
}
