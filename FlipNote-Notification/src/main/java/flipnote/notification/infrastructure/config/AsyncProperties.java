package flipnote.notification.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.async")
public class AsyncProperties {
	private int corePoolSize = 4;
	private int maxPoolSize = 10;
	private int queueCapacity = 100;
}
