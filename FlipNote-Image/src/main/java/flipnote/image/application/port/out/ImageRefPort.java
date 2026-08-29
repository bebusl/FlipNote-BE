package flipnote.image.application.port.out;

import java.util.Optional;

import flipnote.image.domain.model.reference.ReferenceType;

public interface ImageRefPort {

    ImageRefAndImage save(Long imageId);

	//이미지 참조 활성화
	void activate(Long imageRefId, ReferenceType referenceType, Long referenceId);

	Long getImageIdByRefId(Long imageRefId);

	Optional<Long> findByReference(ReferenceType type, Long referenceId);

	void delete(Long id);

	ImageRefRow findById(Long newImageRefId);

	String getUrlByRefId(Long imageRefId);

	record ImageRefAndImage(Long imageRefId, Long imageId) {}
	record ImageRefRow(Long id, ReferenceType type, Long referenceId) {}
}
