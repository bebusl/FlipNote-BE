package flipnote.group.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import flipnote.group.api.dto.request.ApplicationFormRequestDto;
import flipnote.group.api.dto.request.JoinRespondRequestDto;
import flipnote.group.api.dto.response.ApplicationFormResponseDto;
import flipnote.group.api.dto.response.FindJoinFormListResponseDto;
import flipnote.group.api.dto.response.JoinRespondResponseDto;
import flipnote.group.application.port.in.DeleteJoinUseCase;
import flipnote.group.application.port.in.JoinRespondUseCase;
import flipnote.group.application.port.in.JoinUseCase;
import flipnote.group.application.port.in.command.ApplicationFormCommand;
import flipnote.group.application.port.in.command.DeleteJoinCommand;
import flipnote.group.application.port.in.command.FindJoinFormCommand;
import flipnote.group.application.port.in.command.JoinRespondCommand;
import flipnote.group.application.port.in.result.ApplicationFormResult;
import flipnote.group.application.port.in.result.FindJoinFormListResult;
import flipnote.group.application.port.in.result.JoinRespondResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/groups/{groupId}/joins")
@RequiredArgsConstructor
public class JoinController {

	private final JoinUseCase joinUseCase;
	private final JoinRespondUseCase joinRespondUseCase;
	private final DeleteJoinUseCase deleteJoinUseCase;

	/**
	 * 가입 신청 요청
	 * @param userId
	 * @param groupId
	 * @param req
	 * @return
	 */
	@PostMapping("")
	public ResponseEntity<ApplicationFormResponseDto> joinRequest(
		@RequestHeader("X-USER-ID") Long userId,
		@PathVariable("groupId") Long groupId,
		@Valid @RequestBody ApplicationFormRequestDto req) {

		ApplicationFormCommand cmd = new ApplicationFormCommand(userId, groupId, req.joinIntro());

		ApplicationFormResult result = joinUseCase.joinRequest(cmd);

		ApplicationFormResponseDto res = ApplicationFormResponseDto.from(result);

		return ResponseEntity.status(HttpStatus.CREATED).body(res);
	}

	/**
	 * 해당 그룹 가입 신청 리스트 조회
	 * @param userId
	 * @param groupId
	 * @return
	 */
	@GetMapping("")
	public ResponseEntity<FindJoinFormListResponseDto> findGroupJoinList(
		@RequestHeader("X-USER-ID") Long userId,
		@PathVariable("groupId") Long groupId) {

		FindJoinFormCommand cmd = new FindJoinFormCommand(userId, groupId);

		FindJoinFormListResult result = joinUseCase.findJoinFormList(cmd);
		
		FindJoinFormListResponseDto res = FindJoinFormListResponseDto.from(result);

		return ResponseEntity.ok(res);
	}

	/**
	 * 가입 신청 수락 여부 응답
	 * @param userId
	 * @param groupId
	 * @param joinId
	 * @param req
	 * @return
	 */
	@PatchMapping("/{joinId}")
	public ResponseEntity<JoinRespondResponseDto> respondToJoinRequest(
		@RequestHeader("X-USER-ID") Long userId,
		@PathVariable("groupId") Long groupId,
		@PathVariable("joinId") Long joinId,
		@Valid @RequestBody JoinRespondRequestDto req) {

		JoinRespondCommand cmd = new JoinRespondCommand(groupId, userId, joinId, req.joinStatus());

		JoinRespondResult result = joinRespondUseCase.joinRespond(cmd);

		JoinRespondResponseDto res = JoinRespondResponseDto.of(result);

		return ResponseEntity.ok(res);
	}

	/**
	 * 가입 신청 취소
	 * @param userId
	 * @param groupId
	 * @param joinId
	 * @return
	 */
	@DeleteMapping("/{joinId}")
	public ResponseEntity<Void> deleteJoin(
		@RequestHeader("X-USER-ID") Long userId,
		@PathVariable("groupId") Long groupId,
		@PathVariable("joinId") Long joinId
	) {
		DeleteJoinCommand cmd = new DeleteJoinCommand(userId, joinId);
		deleteJoinUseCase.deleteJoin(cmd);

		return ResponseEntity.ok().build();
	}

}
