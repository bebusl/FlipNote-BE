package flipnote.image.application.port.out;

public interface ObjectMetadataPort {

	ObjectMeta head(String s3Key);

	record ObjectMeta(String contentType, long contentLength) {}
}
