package flipnote.image.adapter.out;

import org.springframework.stereotype.Component;

import flipnote.image.application.port.out.ImageStoragePort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class S3ImageStorageAdapter implements ImageStoragePort {

    @Override
    public PresignedUrl issuePutPresignedUrl(String s3Key, String contentType, long contentLength) {
        return null;
    }

    @Override
    public void deleteObject(String s3Key) {

    }
}
