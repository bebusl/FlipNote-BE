package flipnote.image.application.port.out;

public interface ImageStoragePort {
    PresignedUrl issuePutPresignedUrl(String s3Key, String contentType, long contentLength);
    void deleteObject(String s3Key);

    record PresignedUrl(String url, long expiresAtEpochSec) {}
}
