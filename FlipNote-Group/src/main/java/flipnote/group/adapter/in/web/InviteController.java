package flipnote.group.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import flipnote.group.api.dto.request.CreateInviteRequestDto;
import flipnote.group.api.dto.request.InviteListRequest;
import flipnote.group.api.dto.request.InviteRespondRequestDto;
import flipnote.group.api.dto.response.CreateInviteResponseDto;
import flipnote.group.api.dto.response.PagingResponseDto;
import flipnote.group.application.port.in.CancelInviteUseCase;
import flipnote.group.application.port.in.CreateInviteUseCase;
import flipnote.group.application.port.in.FindInviteUseCase;
import flipnote.group.application.port.in.InviteRespondUseCase;
import flipnote.group.application.port.in.command.CancelInviteCommand;
import flipnote.group.application.port.in.command.CreateInviteCommand;
import flipnote.group.application.port.in.command.FindOutgoingInviteCommand;
import flipnote.group.application.port.in.command.InviteRespondCommand;
import flipnote.group.application.port.in.result.CreateInviteResult;
import flipnote.group.domain.model.invite.InviteInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/groups/{groupId}/invitations")
@RequiredArgsConstructor
public class InviteController {

	private final CreateInviteUseCase createInviteUseCase;
	private final CancelInviteUseCase cancelInviteUseCase;
	private final InviteRespondUseCase inviteRespondUseCase;
	private final FindInviteUseCase findInviteUseCase;

	@PostMapping("")
	public ResponseEntity<CreateInviteResponseDto> createInvite(
		@RequestHeader("X-USER-ID") Long userId,
		@RequestHeader("X-User-Email") String userEmail,
		@PathVariable("groupId") Long groupId,
		@Valid @RequestBody CreateInviteRequestDto req
	) {

		CreateInviteCommand cmd = new CreateInviteCommand(userId, userEmail, groupId, req.email());

		CreateInviteResult result = createInviteUseCase.createInvite(cmd);

		CreateInviteResponseDto res = CreateInviteResponseDto.from(result);

		return ResponseEntity.status(HttpStatus.CREATED).body(res);
	}

	@DeleteMapping("/{invitationId}")
	public ResponseEntity<Void> cancelInvite(
		@RequestHeader("X-USER-ID") Long userId,
		@PathVariable("groupId") Long groupId,
		@PathVariable("invitationId") Long invitationId
	) {

		CancelInviteCommand cmd = new CancelInviteCommand(userId, groupId, invitationId);

		cancelInviteUseCase.cancelInvite(cmd);

		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{invitationId}")
	public ResponseEntity<Void> respondToInvite(
		@RequestHeader("X-USER-ID") Long userId,
		@PathVariable("groupId") Long groupId,
		@PathVariable("invitationId") Long invitationId,
		@Valid @RequestBody InviteRespondRequestDto req
	) {

		InviteRespondCommand cmd = new InviteRespondCommand(
			groupId, userId, invitationId, req.status().toInviteStatus()
		);

		inviteRespondUseCase.respondToInvite(cmd);

		return ResponseEntity.ok().build();
	}

	@GetMapping("")
	public ResponseEntity<PagingResponseDto<InviteInfo>> findOutgoingInvites(
		@RequestHeader("X-USER-ID") Long userId,
		@PathVariable("groupId") Long groupId,
		@Valid @ModelAttribute InviteListRequest req
	) {

		FindOutgoingInviteCommand cmd = new FindOutgoingInviteCommand(userId, groupId, req.getPageRequest());

		PagingResponseDto<InviteInfo> res = findInviteUseCase.findOutgoingInvites(cmd);

		return ResponseEntity.ok(res);
	}
}
