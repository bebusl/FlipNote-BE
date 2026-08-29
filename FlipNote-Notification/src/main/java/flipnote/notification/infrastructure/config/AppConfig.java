package flipnote.notification.infrastructure.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class AppConfig {

	@Bean
	public JsonMapperBuilderCustomizer jacksonCustomizer() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		return builder -> {
			SimpleModule module = new SimpleModule();
			module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
			builder.addModule(module);
		};
	}
}
