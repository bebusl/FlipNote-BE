package flipnote.group.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import flipnote.group.api.dto.request.AddPermissionRequestDto;
import flipnote.group.api.dto.request.RemovePermissionRequestDto;
import flipnote.group.api.dto.response.AddPermissionResponseDto;
import flipnote.group.api.dto.response.MyPermissionResponseDto;
import flipnote.group.api.dto.response.RemovePermissionResponseDto;
import flipnote.group.application.port.in.AddPermissionUseCase;
import flipnote.group.application.port.in.MyPermissionUseCase;
import flipnote.group.application.port.in.RemovePermissionUseCase;
import flipnote.group.application.port.in.command.MyPermissionCommand;
import flipnote.group.application.port.in.command.PermissionCommand;
import flipnote.group.application.port.in.result.AddPermissionResult;
import flipnote.group.application.port.in.result.MyPermissionResult;
import flipnote.group.application.port.in.result.RemovePermissionResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/groups/{groupId}/permissions")
public class PermissionController {
	
	private final AddPermissionUseCase addPermissionUseCase;
	private final RemovePermissionUseCase removePermissionUseCase;
	private final MyPermissionUseCase myPermissionUseCase;

	/**
	 * 하위 권한 추가
	 */
	@PostMapping("")
	public ResponseEntity<AddPermissionResponseDto> addDownPermission(
		@RequestHeader("X-USER-ID") Long userId,
		@PathVariable("groupId") Long groupId,
		@Valid @RequestBody AddPermissionRequestDto req) {

		PermissionCommand cmd = new PermissionCommand(userId, groupId, req.changeRole(), req.permission());

		AddPermissionResult result = addPermissionUseCase.addPermission(cmd);

		AddPermissionResponseDto res = AddPermissionResponseDto.from(result);

		return ResponseEntity.ok(res);
	}

	/**
	 * 하위 권한 삭제
	 * @param userId
	 * @param groupId
	 * @param req
	 * @return
	 */
	@DeleteMapping("")
	public ResponseEntity<RemovePermissionResponseDto> changeDownPermission(
		@RequestHeader("X-USER-ID") Long userId,
		@PathVariable("groupId") Long groupId,
		@Valid @RequestBody RemovePermissionRequestDto req) {

		PermissionCommand cmd = new PermissionCommand(userId, groupId, req.changeRole(), req.permission());

		RemovePermissionResult result = removePermissionUseCase.removePermission(cmd);

		RemovePermissionResponseDto res = RemovePermissionResponseDto.from(result);

		return ResponseEntity.ok(res);
	}

	/**
	 * 특정 그룹 내 권한 확인
	 * @param userId
	 * @param groupId
	 * @return
	 */
	@GetMapping("")
	public ResponseEntity<MyPermissionResponseDto> findMyPermission(
		@RequestHeader("X-USER-ID") Long userId,
		@PathVariable("groupId") Long groupId
	) {
		MyPermissionCommand cmd = new MyPermissionCommand(groupId, userId);

		MyPermissionResult result = myPermissionUseCase.findMyPermission(cmd);

		MyPermissionResponseDto res = MyPermissionResponseDto.from(result);

		return ResponseEntity.ok(res);
	}
}
