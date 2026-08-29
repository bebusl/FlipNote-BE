package flipnote.image.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;

import flipnote.image.application.port.out.ImagePort;
import flipnote.image.domain.model.image.Image;
import flipnote.image.domain.model.reference.ReferenceType;
import flipnote.image.infrastructure.persistence.jpa.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ImageRepositoryAdapter implements ImagePort {

    private final ImageRepository imageRepository;

    /**
     * hash로 이미지 찾기
     * @param hash
     * @return
     */
    @Override
    public Optional<ImageRow> findByHash(String hash) {
        return imageRepository.findByHash(hash)
            .map(image -> new ImageRow(image.getId(), image.getHash(), image.getS3Key()));
    }

    /**
     * 타입과 아이디를 통해 이미지 조회
     * @param referenceType
     * @param referenceId
     * @return
     */
    @Override
    public Optional<ImageRow> findByReference(ReferenceType referenceType, Long referenceId) {
        return imageRepository.findAttachedImage(referenceId, referenceType)
            .map(image -> new ImageRow(image.getId(), image.getHash(), image.getS3Key()));
    }

    /**
     * presignedUrl 생성시 임시 이미지 저장
     * @param newImage
     * @return
     */
    @Override
    public ImageRow save(newImage newImage) {

        Image image = imageRepository.save(Image.createBeforeSave(newImage.hash(), newImage.s3Key()));

        log.debug("saved id = " + image.getId());

        return new ImageRow(image.getId(), image.getHash(), image.getS3Key());
    }

    /**
     * 이미지 아이디를 통해 이미지의 메타 데이터 정보 출력
     * @param imageId
     * @return
     */
    @Override
    public ImageHeadRow findImageHeadById(Long imageId) {

        Image image = imageRepository.findById(imageId).orElseThrow(
            () -> new IllegalArgumentException("image not Exist")
        );

        return new ImageHeadRow(image.getId(),image.getS3Key(), image.getMimeType(), image.getSizeBytes());
    }

    /**
     * 이미지의 메타테이더 정보 업데이트
     * @param imageId
     * @param mimeType
     * @param contentLength
     */
    @Override
    public void updateMetadata(Long imageId, String mimeType, long contentLength) {
        int updated =  imageRepository.updateMetadata(imageId, mimeType, contentLength);

        if(updated == 0) throw new IllegalArgumentException("Image not Exist");
    }
}
