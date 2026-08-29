package flipnote.image.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import flipnote.image.domain.model.reference.ImageRef;
import flipnote.image.domain.model.reference.ReferenceType;
import flipnote.image.infrastructure.persistence.querydsl.ImageRefRepositoryCustom;

@Repository
public interface ImageRefRepository extends JpaRepository<ImageRef, Long>, ImageRefRepositoryCustom {
	/**
	 * 참조되는 타입 및 아이디로 이미지 조회
	 * @param type 그룹, 카드셋, 유저
	 * @param id 해당 타입의 참조 아이디
	 * @return
	 */
	Optional<ImageRef> findByReference_TypeAndReference_Id(ReferenceType type, Long id);


	/**
	 * 이미지를 참조하는 컬럼 조회
	 * @param imageId
	 * @return
	 */
	boolean existsByImage_Id(Long imageId);

	/**
	 * 이미지 아이디 조회
	 * @param imageRefId
	 * @return
	 */
	@Query("select ir.image.id from ImageRef ir where ir.id = :imageRefId")
	Long findImageIdByImageRefId(@Param("imageRefId") Long imageRefId);
}
