package flipnote.image.application.port.in.result;

public record ChangeImageResult(Long imageRefId, String url) {
	public static ChangeImageResult from(Long imageRefId, String url) {
		return new ChangeImageResult(imageRefId, url);
	}
}
