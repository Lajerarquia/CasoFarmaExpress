package cl.duoc.farmaexpress.bff.config;

import cl.duoc.farmaexpress.bff.client.MicroserviceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class HttpClientConfig {
    @Bean
    MicroserviceClient prescriptionsClient(
            @Value("${services.prescriptions.url}") String url,
            @Value("${services.connect-timeout}") Duration connectTimeout,
            @Value("${services.read-timeout}") Duration readTimeout) {
        return client(url, connectTimeout, readTimeout);
    }

    @Bean
    MicroserviceClient catalogClient(
            @Value("${services.catalog.url}") String url,
            @Value("${services.connect-timeout}") Duration connectTimeout,
            @Value("${services.read-timeout}") Duration readTimeout) {
        return client(url, connectTimeout, readTimeout);
    }

    private MicroserviceClient client(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        URI uri = URI.create(baseUrl);
        boolean httpScheme = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
        if (!httpScheme || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty() && !uri.getPath().equals("/"))) {
            throw new IllegalArgumentException("Service URL must be an HTTP(S) origin without credentials or path");
        }
        if (connectTimeout.isZero() || connectTimeout.isNegative() || readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("HTTP timeouts must be positive");
        }
        HttpClient http = HttpClient.newBuilder().connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NEVER).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(readTimeout);
        return new MicroserviceClient(RestClient.builder()
                .baseUrl(baseUrl.replaceAll("/$", "")).requestFactory(factory).build());
    }
}
