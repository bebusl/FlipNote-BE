package flipnote.image.infrastructure.persistence.querydsl;

import java.util.List;
import java.util.Optional;

import flipnote.image.domain.model.image.Image;
import flipnote.image.domain.model.reference.Reference;
import flipnote.image.domain.model.reference.ReferenceType;

public interface ImageRepositoryCustom {

	/**
	 * 이미지 조회
	 * 이미지가 참조되는 곳의 타입과 아이디
	 * @param referenceId
	 * @param type
	 * @return
	 */
	Optional<Image> findAttachedImage(Long referenceId, ReferenceType type);

	List<Image> findImagesNotExist(Long lastId, int batchSize);
}
