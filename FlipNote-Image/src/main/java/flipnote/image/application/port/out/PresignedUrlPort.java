package flipnote.image.application.port.out;

public interface PresignedUrlPort {
	/**
	 * presignedUrl 발급
	 * @param s3Key
	 * @param contentType
	 * @param expireMinutes
	 * @return
	 */
	String issuePresignedUrl(String s3Key, String contentType, int expireMinutes);
}
