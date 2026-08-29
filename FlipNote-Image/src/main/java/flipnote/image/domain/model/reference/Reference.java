package flipnote.image.domain.model.reference;

import flipnote.image.domain.model.reference.ReferenceType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reference {

	/**
	 * 그룹, 유저, 카드셋 타입
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "reference_type")
	private ReferenceType type;

	/**
	 * 이미지가 참조되는 타입의 아이디
	 * ex) 그룹 아이디, 유저 아이디, 카드셋 아이디
	 */
	@Column(name = "reference_id")
	private Long id;

	private Reference(ReferenceType type, Long id) {
		if(type == null || id == null) {
			throw new IllegalArgumentException("parameter is required");
		}

		this.type = type;
		this.id = id;
	}

	public void activate(ReferenceType referenceType, Long referenceId) {
		this.type = referenceType;
		this.id = referenceId;
	}

	public static Reference of(ReferenceType type, Long id) {
		return new Reference(type, id);
	}
}
