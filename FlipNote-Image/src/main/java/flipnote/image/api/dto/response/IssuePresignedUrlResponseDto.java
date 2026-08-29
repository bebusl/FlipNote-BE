package flipnote.image.api.dto.response;

import java.net.URL;

import flipnote.image.application.port.in.result.IssuePresignedUrlResult;

public record IssuePresignedUrlResponseDto(
	String url,
	Long imageRefId
) {
	public static IssuePresignedUrlResponseDto from(IssuePresignedUrlResult result) {
		return new IssuePresignedUrlResponseDto(result.presignedUrl(), result.imageRefId());
	}
}
