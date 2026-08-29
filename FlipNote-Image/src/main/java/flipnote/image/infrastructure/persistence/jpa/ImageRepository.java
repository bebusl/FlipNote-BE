package flipnote.image.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import flipnote.image.domain.model.image.Image;
import flipnote.image.infrastructure.persistence.querydsl.ImageRepositoryCustom;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long>, ImageRepositoryCustom {
	Optional<Image> findByHash(String fileName);

	/**
	 * 이미지 헤더 정보 업데이트
	 * @param imageId
	 * @param mimeType
	 * @param sizeBytes
	 * @return
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Image i set i.mimeType = :mimeType, i.sizeBytes = :sizeBytes where i.id = :id")
	int updateMetadata(@Param("id") Long imageId, @Param("mimeType") String mimeType, @Param("sizeBytes") Long sizeBytes);
}
