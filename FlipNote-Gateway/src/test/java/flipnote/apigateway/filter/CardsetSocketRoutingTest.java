package flipnote.apigateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

import java.net.URI;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.WebsocketRoutingFilter;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.socket.server.WebSocketService;

import reactor.core.publisher.Mono;

class CardsetSocketRoutingTest {
	private URI firstCardsetRoute() throws Exception {
		var sources = new YamlPropertySourceLoader().load("production-routes",
			new FileSystemResource("src/main/resources/application.yml"));
		var gateway = new Binder(ConfigurationPropertySources.from(sources))
			.bind("spring.cloud.gateway", GatewayProperties.class).get();
		return gateway.getRoutes().stream()
			.filter(route -> List.of("cardset-private", "cardset-websocket").contains(route.getId()))
			.sorted(Comparator.comparingInt(RouteDefinition::getOrder))
			.findFirst().orElseThrow().getUri();
	}

	@SuppressWarnings("unchecked")
	private WebsocketRoutingFilter filter(WebSocketService service) {
		ObjectProvider<List<HttpHeadersFilter>> headers = mock(ObjectProvider.class);
		when(headers.getIfAvailable(any())).thenAnswer(ignored -> new ArrayList<>());
		return new WebsocketRoutingFilter(mock(WebSocketClient.class), service, headers);
	}

	@Test
	void initialPollingRemainsHttp() throws Exception {
		var service = mock(WebSocketService.class);
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
			"/v1/card-sets/ws/?EIO=4&transport=polling"));
		exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, firstCardsetRoute());
		filter(service).filter(exchange, ignored -> Mono.empty()).block();
		assertEquals("http", exchange.<URI>getAttribute(GATEWAY_REQUEST_URL_ATTR).getScheme());
		verifyNoInteractions(service);
	}

	@Test
	void upgradeOnTheHttpRouteIsForwardedAsWebsocket() throws Exception {
		var service = mock(WebSocketService.class);
		when(service.handleRequest(any(), any())).thenReturn(Mono.empty());
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
			"/v1/card-sets/ws/?EIO=4&transport=websocket").header("Upgrade", "websocket"));
		exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, firstCardsetRoute());
		filter(service).filter(exchange, ignored -> Mono.empty()).block();
		assertEquals("ws", exchange.<URI>getAttribute(GATEWAY_REQUEST_URL_ATTR).getScheme());
		verify(service).handleRequest(eq(exchange), any());
	}
}
