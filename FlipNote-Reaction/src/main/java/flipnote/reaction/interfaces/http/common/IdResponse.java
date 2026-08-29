package flipnote.reaction.interfaces.http.common;

public record IdResponse(
	Long id
) {
	public static IdResponse from(Long id) {
		return new IdResponse(id);
	}
}
