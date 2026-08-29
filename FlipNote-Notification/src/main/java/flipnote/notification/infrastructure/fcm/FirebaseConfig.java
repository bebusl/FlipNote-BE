package flipnote.notification.infrastructure.fcm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class FirebaseConfig {

	@Value("${firebase.service-account-json}")
	private String serviceAccountJson;

	@PostConstruct
	public void initialize() throws IOException {
		if (FirebaseApp.getApps().isEmpty()) {
			try (InputStream stream = new ByteArrayInputStream(
				serviceAccountJson.getBytes(StandardCharsets.UTF_8)
			)) {
				FirebaseOptions options = FirebaseOptions.builder()
					.setCredentials(GoogleCredentials.fromStream(stream))
					.build();
				FirebaseApp.initializeApp(options);
				log.info("Firebase initialized successfully");
			}
		}
	}
}
