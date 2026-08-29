package flipnote.group.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import flipnote.group.api.dto.request.ChangeGroupRequestDto;
import flipnote.group.api.dto.request.CreateGroupRequestDto;
import flipnote.group.api.dto.request.GroupListRequestDto;
import flipnote.group.api.dto.response.ChangeGroupResponseDto;
import flipnote.group.api.dto.response.CheckOwnerResponseDto;
import flipnote.group.api.dto.response.CreateGroupResponseDto;
import flipnote.group.api.dto.response.CursorPagingResponseDto;
import flipnote.group.api.dto.response.FindGroupResponseDto;
import flipnote.group.application.port.in.ChangeGroupUseCase;
import flipnote.group.application.port.in.CheckOwnerUseCase;
import flipnote.group.application.port.in.CreateGroupUseCase;
import flipnote.group.application.port.in.DeleteGroupUseCase;
import flipnote.group.application.port.in.FindGroupUseCase;
import flipnote.group.application.port.in.command.ChangeGroupCommand;
import flipnote.group.application.port.in.command.CheckOwnerCommand;
import flipnote.group.application.port.in.command.CreateGroupCommand;
import flipnote.group.application.port.in.command.DeleteGroupCommand;
import flipnote.group.application.port.in.command.FindGroupCommand;
import flipnote.group.application.port.in.result.CheckOwnerResult;
import flipnote.group.domain.model.group.GroupInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/groups")
public class GroupController {

	private final CreateGroupUseCase createGroupUseCase;
	private final ChangeGroupUseCase changeGroupUseCase;
	private final FindGroupUseCase findGroupUseCase;
	private final DeleteGroupUseCase deleteGroupUseCase;
	private final CheckOwnerUseCase checkOwnerUseCase;

	/**
	 * 그룹 생성 API
	 * @param userId
	 * @param req
	 * @return
	 */
	@PostMapping("")
	public ResponseEntity<CreateGroupResponseDto> createGroup(
		@RequestHeader("X-USER-ID") Long userId,
	    @RequestBody @Valid CreateGroupRequestDto req) {

		CreateGroupCommand cmd = new CreateGroupCommand(
			userId,
			req.name(),
			req.category(),
			req.description(),
			req.joinPolicy(),
			req.visibility(),
			req.maxMember(),
			req.imageRefId()
		);

		var result = createGroupUseCase.create(cmd);
		CreateGroupResponseDto res = CreateGroupResponseDto.from(result.groupId());
		return ResponseEntity.ok(res);
	}

	/**
	 * 그룹 수정 API
	 * @param userId
	 * @param groupId
	 * @param req
	 * @return
	 */
	@PutMapping("/{groupId}")
	public ResponseEntity<ChangeGroupResponseDto> changeGroup(
		@RequestHeader("X-USER-ID") Long userId,
		@PathVariable Long groupId,
		@RequestBody @Valid ChangeGroupRequestDto req) {

		ChangeGroupCommand cmd = new ChangeGroupCommand(
			groupId,
			userId,
			req.name(),
			req.category(),
			req.description(),
			req.joinPolicy(),
			req.visibility(),
			req.maxMember(),
			req.imageRefId()
		);

		var result = changeGroupUseCase.change(cmd);

		ChangeGroupResponseDto res = ChangeGroupResponseDto.from(result);
		return ResponseEntity.ok(res);
	}

	/**
	 * 그룹 상세 조회 API
	 * @param userId
	 * @param groupId
	 * @return
	 */
	@GetMapping("/{groupId}")
	public ResponseEntity<FindGroupResponseDto> findGroup(
		@RequestHeader("X-USER-ID") Long userId,
		@PathVariable("groupId") Long groupId) {

		FindGroupCommand cmd = new FindGroupCommand(userId, groupId);

		var result = findGroupUseCase.findGroup(cmd);

		FindGroupResponseDto res = FindGroupResponseDto.from(result);

		return ResponseEntity.ok(res);
	}

	/**
	 * 그룹 삭제
	 * @param userId
	 * @param groupId
	 * @return
	 */
	@DeleteMapping("/{groupId}")
	public ResponseEntity<Void> deleteGroup(
		@RequestHeader("X-USER-ID") Long userId,
		@PathVariable("groupId") Long groupId) {

		DeleteGroupCommand cmd = new DeleteGroupCommand(userId, groupId);

		deleteGroupUseCase.deleteGroup(cmd);

		return ResponseEntity.noContent().build();
	}

	/**
	 * 그룹 전체 조회
	 * @param userId
	 * @param req
	 * @return
	 */
	@GetMapping
	public ResponseEntity<CursorPagingResponseDto<GroupInfo>> findGroup(
		@RequestHeader("X-USER-ID") Long userId,
		@Valid @ModelAttribute GroupListRequestDto req
	) {
		CursorPagingResponseDto<GroupInfo> res = findGroupUseCase.findAllGroup(userId, req);

		return ResponseEntity.ok(res);
	}

	/**
	 * 내가 가입한 그룹 전체 조회
	 * @param userId
	 * @param req
	 * @return
	 */
	@GetMapping("/me")
	public ResponseEntity<CursorPagingResponseDto<GroupInfo>> findMyGroup(
		@RequestHeader("X-USER-ID") Long userId,
		@Valid @ModelAttribute GroupListRequestDto req
	) {
		CursorPagingResponseDto<GroupInfo> res = findGroupUseCase.findMyGroup(userId, req);

		return ResponseEntity.ok(res);
	}

	/**
	 * 내가 생성한 그룹 전체 조회
	 * @param userId
	 * @param req
	 * @return
	 */
	@GetMapping("/created")
	public ResponseEntity<CursorPagingResponseDto<GroupInfo>> findCreatedGroup(
		@RequestHeader("X-USER-ID") Long userId,
		@Valid @ModelAttribute GroupListRequestDto req
	) {
		CursorPagingResponseDto<GroupInfo> res = findGroupUseCase.findCreatedGroup(userId, req);

		return ResponseEntity.ok(res);
	}

	/**
	 * 그룹 수정 권한 체크
	 * @param userId
	 * @param groupId
	 * @return
	 */
	@GetMapping("/{groupId}/managers")
	public ResponseEntity<CheckOwnerResponseDto> checkManagerPermission(
		@RequestHeader("X-USER-ID") Long userId,
		@PathVariable("groupId") Long groupId) {

		CheckOwnerCommand cmd = new CheckOwnerCommand(userId, groupId);

		CheckOwnerResult result = checkOwnerUseCase.checkOwner(cmd);

		CheckOwnerResponseDto res = CheckOwnerResponseDto.from(result);

		return ResponseEntity.ok(res);
	}
}
