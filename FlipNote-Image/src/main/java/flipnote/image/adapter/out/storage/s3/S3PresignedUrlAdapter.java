package flipnote.image.adapter.out.storage.s3;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import flipnote.image.application.port.out.PresignedUrlPort;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class S3PresignedUrlAdapter implements PresignedUrlPort {

	private final S3Presigner s3Presigner;

	@Value("${cloud.aws.bucket}")
	private String bucket;

	/**
	 * PresignedUrl 발급
	 * @param s3Key
	 * @param contentType
	 * @param expireMinutes
	 * @return
	 */
	@Override
	public String issuePresignedUrl(String s3Key, String contentType, int expireMinutes) {

		/**
		 * 발급시 필요한 오브젝트
		 */
		PutObjectRequest s3Object = PutObjectRequest.builder()
			.bucket(bucket)
			.key(s3Key)
			.contentType(contentType)
			.build();

		/**
		 * S3에 발급 요청
		 */
		PutObjectPresignRequest req = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(expireMinutes))
			.putObjectRequest(s3Object)
			.build();

		return s3Presigner.presignPutObject(req).url().toString();
	}
}
