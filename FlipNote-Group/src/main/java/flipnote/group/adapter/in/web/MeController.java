package flipnote.group.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import flipnote.group.api.dto.request.InviteListRequest;
import flipnote.group.api.dto.response.FindMyJoinListResponseDto;
import flipnote.group.api.dto.response.PagingResponseDto;
import flipnote.group.application.port.in.FindInviteUseCase;
import flipnote.group.application.port.in.FindMyJoinListUseCase;
import flipnote.group.application.port.in.result.FindMyJoinListResult;
import flipnote.group.domain.model.invite.InviteMyInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class MeController {

	private final FindMyJoinListUseCase findMyJoinListUseCase;
	private final FindInviteUseCase findInviteUseCase;

	/**
	 * 내가 신청한 가입신청 리스트 조회
	 * @param userId
	 * @return
	 */
	@GetMapping("/groups/joins/me")
	public ResponseEntity<FindMyJoinListResponseDto> findGroupJoinMe(
		@RequestHeader("X-USER-ID") Long userId
	) {
		FindMyJoinListResult result = findMyJoinListUseCase.findMyJoinList(userId);

		FindMyJoinListResponseDto res = FindMyJoinListResponseDto.from(result);

		return ResponseEntity.ok(res);
	}

	/**
	 * 내가 받은 초대 리스트 조회
	 * @param userId
	 * @param req
	 * @return
	 */
	@GetMapping("/group-invitations")
	public ResponseEntity<PagingResponseDto<InviteMyInfo>> findIncomingInvites(
		@RequestHeader("X-USER-ID") Long userId,
		@Valid @ModelAttribute InviteListRequest req
	) {
		PagingResponseDto<InviteMyInfo> res = findInviteUseCase.findIncomingInvites(userId, req.getPageRequest());

		return ResponseEntity.ok(res);
	}
}
