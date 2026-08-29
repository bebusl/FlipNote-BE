package flipnote.image.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import flipnote.image.application.port.out.ImageS3KeyPort;
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
public class ImageS3KeyAdapter implements ImageS3KeyPort {

	private final ImageRefRepository imageRefRepository;
	private final ImageRepository imageRepository;

	@Override
	public String getS3KeyByReference(ReferenceType type, Long referenceId) {
		Optional<ImageRef> imageRef = imageRefRepository.findByReference_TypeAndReference_Id(type, referenceId);

		Image image = imageRef.get().getImage();

		return image.getS3Key();
	}
}
