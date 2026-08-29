package flipnote.group.global.config;

import flipnote.group.global.constants.HttpConstants;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${swagger.server-url:http://localhost:8080}")
    private String swaggerServerUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("FlipNote Group API")
                .description("FlipNote Group API")
                .version("1.0.0"))
            .servers(List.of(
                new Server().url(swaggerServerUrl).description("Configured"),
                new Server().url("http://localhost:8084").description("Local")
            ));
    }

    @Bean
    public OperationCustomizer hideInternalHeaders() {
        return (operation, handlerMethod) -> {
            if (operation.getParameters() != null) {
                operation.getParameters().removeIf(p ->
                    HttpConstants.USER_ID_HEADER.equals(p.getName()));
            }
            return operation;
        };
    }
}
