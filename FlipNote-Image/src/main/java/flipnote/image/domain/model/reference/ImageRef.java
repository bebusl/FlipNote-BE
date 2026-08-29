package flipnote.image.domain.model.reference;

import flipnote.image.domain.model.BaseEntity;
import flipnote.image.domain.model.image.Image;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "image_reference")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageRef extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Embedded
	private Reference reference;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "image_id")
	private Image image;

	@Version
	private Long version;

	@Builder
	private ImageRef(Reference reference, Image image) {
		this.reference = reference;
		this.image = image;
	}

	public static ImageRef createImageRef(Image image) {
		return ImageRef.builder()
			.image(image)
			.build();
	}

	public void activate(ReferenceType referenceType, Long referenceId) {
		this.reference = Reference.of(referenceType, referenceId);
	}

	/**
	 * 이미지 연결 여부
	 * @return
	 */
	public boolean isAttached() {
		return this.image != null && !isDeleted();
	}

	/**
	 * 이미지 연결
	 * @param image
	 */
	public void attach(Image image) {
		if (isDeleted()) throw new IllegalStateException("삭제된 참조는 연결할 수 없음");
		if (image == null) throw new IllegalArgumentException("이미지 필요");
		this.image = image;
	}

	/**
	 * 이미지 연결만 끊기(미연결로)
	 */
	public void detach() {
		if (isDeleted()) throw new IllegalStateException("삭제된 참조는 변경할 수 없음");
		this.image = null;
	}

	/**
	 * 이미지 교체
	 * @param replaceImage
	 */
	public void replaceImage(Image replaceImage) {
		if (isDeleted()) throw new IllegalStateException("삭제된 참조는 교체할 수 없음");
		if (replaceImage == null) throw new IllegalArgumentException("이미지 필요");
		this.image = replaceImage;
	}

	/**
	 * 그룹, 유저, 카드셋 삭제할 경우 완전 삭제
	 */
	public void deactivate() {
		if (isDeleted()) return;
		this.image = null;
		this.markDeleted();
	}

	/**
	 * 특정 대상의 것인지 확인
	 * @param type 이미지 참조된 종류
	 * @param referenceId 이미지 참조 아이디
	 * @return
	 */
	public boolean isFor(ReferenceType type, Long referenceId) {
		return this.reference.getType() == type && this.reference.getId().equals(referenceId);
	}

}
