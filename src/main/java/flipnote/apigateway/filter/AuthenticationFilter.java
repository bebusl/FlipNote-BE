package flipnote.apigateway.filter;

import flipnote.apigateway.client.TokenValidationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final TokenValidationClient tokenValidationClient;

    public AuthenticationFilter(TokenValidationClient tokenValidationClient) {
        super(Config.class);
        this.tokenValidationClient = tokenValidationClient;
    }

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            HttpCookie cookie = exchange.getRequest().getCookies().getFirst(ACCESS_TOKEN_COOKIE);

            if (cookie == null) {
                log.warn("Missing access token cookie");
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String token = cookie.getValue();

            return tokenValidationClient.validateToken(token)
                    .flatMap(response -> {
                        log.debug("Authenticated user: id={}, email={}, role={}",
                                response.userId(), response.email(), response.role());

                        ServerWebExchange modifiedExchange = exchange.mutate()
                                .request(r -> r
                                        .header("X-User-Id", String.valueOf(response.userId()))
                                        .header("X-User-Email", response.email())
                                        .header("X-User-Role", response.role()))
                                .build();

                        return chain.filter(modifiedExchange);
                    })
                    .onErrorResume(e -> {
                        log.error("Token validation failed: {}", e.getMessage());
                        return onError(exchange, HttpStatus.UNAUTHORIZED);
                    });
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
    }
}
