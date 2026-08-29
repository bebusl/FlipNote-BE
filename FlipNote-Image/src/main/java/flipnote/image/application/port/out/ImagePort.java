package flipnote.image.application.port.out;

import java.util.Optional;

import flipnote.image.domain.model.reference.ReferenceType;

public interface ImagePort {

    Optional<ImageRow> findByHash(String hash);
    Optional<ImageRow> findByReference(ReferenceType referenceType, Long referenceId);
    ImageRow save(newImage newImage);

    ImageHeadRow findImageHeadById(Long imageId);

    void updateMetadata(Long imageId, String mimeType, long contentLength);

    record ImageRow(long id, String hash, String s3Key) {}
    record newImage(String hash, String s3Key) {}
    record ImageHeadRow(long id, String s3Key, String mimeType, Long sizeBytes) {}
}
