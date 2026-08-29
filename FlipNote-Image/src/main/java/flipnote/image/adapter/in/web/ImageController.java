package flipnote.image.adapter.in.web;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import flipnote.image.api.dto.request.IssuePresignedUrlRequestDto;
import flipnote.image.api.dto.response.IssuePresignedUrlResponseDto;
import flipnote.image.application.port.in.IssuePresignedUrlUseCase;
import flipnote.image.application.port.in.command.IssuePresignedUrlCommand;
import flipnote.image.application.port.in.result.IssuePresignedUrlResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/images")
@RequiredArgsConstructor
public class ImageController {

	private final IssuePresignedUrlUseCase issuePresignedUrlUseCase;

	@PostMapping("/upload")
	public ResponseEntity<IssuePresignedUrlResponseDto> issuePresignedUrl(
		@RequestBody @Valid IssuePresignedUrlRequestDto req) {

		IssuePresignedUrlCommand cmd = new IssuePresignedUrlCommand(req.fileName(), req.type());

		IssuePresignedUrlResult result = issuePresignedUrlUseCase.issuePresignedUrl(cmd);

		return ResponseEntity.ok(IssuePresignedUrlResponseDto.from(result));
	}
}
