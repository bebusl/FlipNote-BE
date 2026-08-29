package flipnote.image.domain.policy;

import org.springframework.stereotype.Component;

/**
 * 이미지 s3 저장 형식 정책
 */
@Component
public class ImageNamingPolicy {

	/**
	 * 파일면만 추출
	 * @param fileName
	 * @return
	 */
	public String hashFromFileName(String fileName) {
		return fileName.split("\\.")[0];
	}

	/**
	 * s3키 추출
	 * @param fileName
	 * @return
	 */
	public String s3KeyFromFileName(String fileName) {
		return "image/" + fileName;
	}

	/**
	 * 파일명에서 컨텐츠 타입 추출
	 * @param fileName
	 * @return
	 */
	public String contentTypeFromFileName(String fileName) {
		String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

		return switch (extension) {
			case "jpg", "jpeg" -> "image/jpeg";
			case "png" -> "image/png";
			case "gif" -> "image/gif";
			case "webp" -> "image/webp";
			default -> "application/octet-stream";
		};
	}
}
