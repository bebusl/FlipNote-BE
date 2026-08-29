package flipnote.image.adapter.out.storage.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import flipnote.image.application.port.out.ObjectMetadataPort;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

@Component
@RequiredArgsConstructor
public class S3ObjectMetadataAdapter implements ObjectMetadataPort {

	private final S3Client s3Client;

	@Value("${cloud.aws.bucket}")
	private String bucket;

	@Override
	public ObjectMeta head(String s3Key) {
		var res = s3Client.headObject(
			HeadObjectRequest.builder()
				.bucket(bucket)
				.key(s3Key)
				.build()
		);

		return new ObjectMeta(res.contentType(), res.contentLength());
	}
}
