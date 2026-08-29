package flipnote.group.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import flipnote.group.api.dto.request.ChangeRoleRequestDto;
import flipnote.group.api.dto.response.ChangeRoleResponseDto;
import flipnote.group.api.dto.response.FindGroupMemberResponseDto;
import flipnote.group.application.port.in.ChangeRoleUseCase;
import flipnote.group.application.port.in.FindGroupMemberUseCase;
import flipnote.group.application.port.in.KickMemberUseCase;
import flipnote.group.application.port.in.command.ChangeRoleCommand;
import flipnote.group.application.port.in.command.FindGroupMemberCommand;
import flipnote.group.application.port.in.command.KickMemberCommand;
import flipnote.group.application.port.in.result.ChangeRoleResult;
import flipnote.group.application.port.in.result.FindGroupMemberResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/groups/{groupId}/members")
public class MemberController {

	private final FindGroupMemberUseCase findGroupMemberUseCase;
	private final KickMemberUseCase kickMemberUseCase;
	private final ChangeRoleUseCase changeRoleUseCase;

	/**
	 * 그룹 내 멤버 전체 조회
	 * @param userId
	 * @param groupId
	 * @return
	 */
	@GetMapping("")
	public ResponseEntity<FindGroupMemberResponseDto> findGroupMembers(
		@RequestHeader("X-USER-ID") Long userId,
		@PathVariable("groupId") Long groupId) {

		FindGroupMemberCommand cmd = new FindGroupMemberCommand(userId, groupId);

		FindGroupMemberResult result = findGroupMemberUseCase.findGroupMember(cmd);

		FindGroupMemberResponseDto res = FindGroupMemberResponseDto.from(result);

		return ResponseEntity.ok(res);
	}

	/**
	 * 그룹 멤버 추방
	 * @param userId
	 * @param groupId
	 * @param memberId
	 * @return
	 */
	@DeleteMapping("/{memberId}")
	public ResponseEntity<Void> kickGroupMember(
		@RequestHeader("X-USER-ID") Long userId,
		@PathVariable("groupId") Long groupId,
		@PathVariable("memberId") Long memberId
	) {
		KickMemberCommand cmd = new KickMemberCommand(userId, groupId, memberId);

		kickMemberUseCase.kickMember(cmd);

		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{memberId}")
	public ResponseEntity<ChangeRoleResponseDto> changeMemberRole(
		@RequestHeader("X-USER-ID") Long userId,
		@PathVariable("groupId") Long groupId,
		@PathVariable("memberId") Long memberId,
		@RequestBody ChangeRoleRequestDto req
	) {
		ChangeRoleCommand cmd = new ChangeRoleCommand(userId, memberId, groupId, req.role());

		ChangeRoleResult changeRoleResult = changeRoleUseCase.ChangeRole(cmd);

		ChangeRoleResponseDto res = ChangeRoleResponseDto.of(changeRoleResult);

		return ResponseEntity.ok(res);
	}
}
