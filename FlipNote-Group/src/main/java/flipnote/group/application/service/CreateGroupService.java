package flipnote.group.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.group.adapter.out.entity.GroupEntity;
import flipnote.group.adapter.out.entity.GroupMemberEntity;
import flipnote.group.adapter.out.entity.RoleEntity;
import flipnote.group.application.port.in.CreateGroupUseCase;
import flipnote.group.application.port.in.command.CreateGroupCommand;
import flipnote.group.application.port.in.result.CreateGroupResult;
import flipnote.group.application.port.out.GroupMemberRepositoryPort;
import flipnote.group.application.port.out.GroupRepositoryPort;
import flipnote.group.application.port.out.GroupRoleRepositoryPort;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import flipnote.image.grpc.v1.ActivateImageRequest;
import flipnote.image.grpc.v1.ActivateImageResponse;
import flipnote.image.grpc.v1.GetUrlByReferenceRequest;
import flipnote.image.grpc.v1.GetUrlByReferenceResponse;
import flipnote.image.grpc.v1.ImageCommandServiceGrpc;
import flipnote.image.grpc.v1.Type;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateGroupService implements CreateGroupUseCase {

	private final GroupRepositoryPort groupRepository;
	private final GroupMemberRepositoryPort groupMemberRepository;
	private final GroupRoleRepositoryPort groupRoleRepository;
	private final ImageCommandServiceGrpc.ImageCommandServiceBlockingStub imageCommandServiceStub;

	/**
	 * 그룹 생성
	 * @param cmd
	 * @return
	 */
	@Override
	@Transactional
	public CreateGroupResult create(CreateGroupCommand cmd) {

		GroupEntity group = GroupEntity.create(cmd);

		//그룹 도메인 -> 엔티티 변환 후 저장
		Long groupId = groupRepository.saveNewGroup(group);
		
		//그룹 역할 생성
		RoleEntity role = groupRoleRepository.create(groupId);

		log.debug("{}", role.getId());

		//생성자 오너 역할로 저장
		GroupMemberEntity groupMember = GroupMemberEntity.create(groupId, cmd.userId(), role);

		groupMemberRepository.save(groupMember);

		log.debug("{}", groupMember.getId());

		if (cmd.imageRefId() != null) {
			ActivateImageRequest request = ActivateImageRequest.newBuilder()
				.setImageRefId(cmd.imageRefId())
				.setReferenceType(Type.GROUP)
				.setReferenceId(groupId)
				.build();
			// gRPC 호출

			try {
				imageCommandServiceStub.activateImage(request);
			} catch (StatusRuntimeException e) {
				switch (e.getStatus().getCode()) {
					case NOT_FOUND -> throw new BusinessException(ErrorCode.IMAGE_NOT_FOUND);
					case INVALID_ARGUMENT -> throw new BusinessException(ErrorCode.IMAGE_INVALID_REQUEST);
					case INTERNAL -> throw new BusinessException(ErrorCode.IMAGE_SERVER_ERROR);
					default -> throw new BusinessException(ErrorCode.IMAGE_SERVICE_ERROR);
				}
			}
			return CreateGroupResult.of(groupId);
		}

		return CreateGroupResult.of(groupId);
	}
}
