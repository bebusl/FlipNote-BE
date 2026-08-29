package flipnote.apigateway.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class TokenValidationClient {

    private final WebClient webClient;

    public TokenValidationClient(@Value("${app.user-service.url}") String userServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }

    public Mono<TokenValidationResponse> validateToken(String token) {
        return webClient.post()
                .uri("/v1/auth/token/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TokenValidationRequest(token))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<TokenValidationResponse>>() {})
                .map(ApiResponse::data);
    }

    public record TokenValidationRequest(
            @JsonProperty("token") String token) {}

    public record TokenValidationResponse(
            @JsonProperty("userId") Long userId,
            @JsonProperty("email") String email,
            @JsonProperty("role") String role) {}

    public record ApiResponse<T>(
            @JsonProperty("status") int status,
            @JsonProperty("code") String code,
            @JsonProperty("message") String message,
            @JsonProperty("data") T data) {}
}
