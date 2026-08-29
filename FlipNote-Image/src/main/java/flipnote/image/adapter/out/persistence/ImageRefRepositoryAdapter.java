package flipnote.image.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import flipnote.image.application.port.out.ImageRefPort;

import flipnote.image.application.port.out.PublicUrlPort;
import flipnote.image.domain.model.image.Image;
import flipnote.image.domain.model.reference.ImageRef;
import flipnote.image.domain.model.reference.ReferenceType;
import flipnote.image.infrastructure.persistence.jpa.ImageRefRepository;
import flipnote.image.infrastructure.persistence.jpa.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ImageRefRepositoryAdapter implements ImageRefPort {

    private final ImageRefRepository imageRefRepository;
    private final ImageRepository imageRepository;
    private final PublicUrlPort publicUrlPort;

    /**
     * 참조한 타입과 아이디를 통해 ref 저장
     * @param imageId
     * @return
     */
    @Override
    public ImageRefAndImage save(Long imageId) {

        Image image = imageRepository.findById(imageId).orElseThrow(
            () -> new IllegalArgumentException("image is blank")
        );

        ImageRef imageRef = imageRefRepository.save(ImageRef.createImageRef(image));

        log.debug(imageRef.getId().toString());

        return new ImageRefAndImage(imageRef.getId(), image.getId());
    }

    /**
     * 이미지 참조 활성화 참조되는 타입과 아이디를 업데이트
     * @param imageRefId
     * @param referenceType
     * @param referenceId
     */
    @Override
    public void activate(Long imageRefId, ReferenceType referenceType, Long referenceId) {

		ImageRef imageRef = imageRefRepository.findById(imageRefId).orElseThrow(
			() -> new IllegalArgumentException("ImageRef is Blank")
		);

		imageRef.activate(referenceType, referenceId);

		imageRefRepository.save(imageRef);

    }

    /**
     * 이미지 아이디 조회
     * @param imageRefId
     * @return
     */
    @Override
    public Long getImageIdByRefId(Long imageRefId) {
        return imageRefRepository.findImageIdByImageRefId(imageRefId);
    }

    /**
     * 참조타입과 아이디로부터 이미지 조회
     * @param type
     * @param referenceId
     * @return
     */
    @Override
    public Optional<Long> findByReference(ReferenceType type, Long referenceId) {
        return imageRefRepository
            .findByReference_TypeAndReference_Id(type, referenceId)
            .map(ImageRef::getId);
    }

    @Override
    public void delete(Long id) {
        imageRefRepository.deleteById(id);
    }

    @Override
    public ImageRefRow findById(Long imageRefId) {

		log.debug("{}", imageRefId);

        Optional<ImageRef> imageRefOptional = imageRefRepository.findById(imageRefId);

		log.debug("{}", imageRefOptional.get().getId());

        ImageRef imageRef = imageRefOptional.get();

        // ImageRef imageRef = imageRefRepository.findById(imageRefId).orElseThrow(
        //     () -> new IllegalArgumentException("error")
        // );

        if(imageRef.getReference()==null) {
            return new ImageRefRow(imageRef.getId(), null, null);
        }

        ReferenceType type = imageRef.getReference().getType();
        Long referenceId = imageRef.getReference().getId();

        return new ImageRefRow(imageRef.getId(), type, referenceId);
    }

    @Override
    public String getUrlByRefId(Long imageRefId) {

        Image image = imageRefRepository.findById(imageRefId).get().getImage();

        return publicUrlPort.urlOf(image.getS3Key());
    }
}
