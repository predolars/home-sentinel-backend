package org.com.homesentinelbackend.services;

import org.com.homesentinelbackend.models.LoginSession;
import org.com.homesentinelbackend.models.auth.VerisureAuthHeader;
import org.com.homesentinelbackend.models.auth.VerisureSecurityHeader;
import org.com.homesentinelbackend.models.graphql.GraphQLRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class GraphQLClient {

    @Value("${api.url}")
    private String apiUrl;

    @Value("${credentials.user}")
    private String username;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GraphQLClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public JsonNode sendRequest(GraphQLRequest<?> request, LoginSession session) throws Exception {
        return sendRequest(request, session, null);
    }

    public JsonNode sendRequest(GraphQLRequest<?> request, LoginSession session, VerisureSecurityHeader securityHeader) throws Exception {
        String payloadJson = objectMapper.writeValueAsString(request);

        VerisureAuthHeader authHeader = session.buildAuthHeader(username);
        String authHeaderJson = objectMapper.writeValueAsString(authHeader);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("auth", authHeaderJson)
                .POST(HttpRequest.BodyPublishers.ofString(payloadJson));

        if (securityHeader != null) {
            String securityHeaderJson = objectMapper.writeValueAsString(securityHeader);
            requestBuilder.header("security", securityHeaderJson);
        }

        if (session.getCapabilities() != null) {
            requestBuilder.header("x-capabilities", session.getCapabilities());
        }

        HttpRequest httpRequest = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        response.headers().firstValue("x-capabilities").ifPresent(session::setCapabilities);

        return objectMapper.readTree(response.body());
    }
}