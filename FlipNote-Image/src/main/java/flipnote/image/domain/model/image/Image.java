package flipnote.image.domain.model.image;

import flipnote.image.domain.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 32)
	private String hash;

	@Column(nullable = false, length = 1024)
	private String s3Key;

	private String mimeType;

	private Long sizeBytes;

	@Builder
	private Image(String hash, String s3Key) {
		this.hash = hash;
		this.s3Key = s3Key;
	}

	/**
	 * s3 presignedUrl 생성시 임시로 저장하는 메서드
	 * @param hash
	 * @param s3Key
	 * @return
	 */
	public static Image createBeforeSave(String hash, String s3Key) {
		if(hash == null || hash.isBlank()) throw new IllegalArgumentException("hash is Blank");
		if(s3Key == null || s3Key.isBlank()) throw new IllegalArgumentException("s3Key is Blank");

		return Image.builder()
			.hash(hash)
			.s3Key(s3Key)
			.build();
	}
}
