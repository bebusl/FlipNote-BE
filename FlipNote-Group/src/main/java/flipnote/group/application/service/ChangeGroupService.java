package flipnote.group.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.group.adapter.out.entity.GroupEntity;
import flipnote.group.adapter.out.persistence.GroupRoleRepositoryAdapter;
import flipnote.group.application.port.in.ChangeGroupUseCase;
import flipnote.group.application.port.in.command.ChangeGroupCommand;
import flipnote.group.application.port.in.result.ChangeGroupResult;
import flipnote.group.application.port.in.result.CreateGroupResult;
import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import flipnote.group.infrastructure.persistence.jpa.GroupRepository;
import flipnote.image.grpc.v1.ActivateImageRequest;
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
public class ChangeGroupService implements ChangeGroupUseCase {

	private final GroupRepository jpaGroupRepository;
	private final GroupRoleRepositoryAdapter groupRoleRepository;
	private final ImageCommandServiceGrpc.ImageCommandServiceBlockingStub imageCommandServiceStub;

	@Value("${image.default.group}")
	private String GROUP_DEFAULT_URL;

	/**
	 * 그룹 수정
	 * @param cmd
	 * @return
	 */
	@Override
	@Transactional
	public ChangeGroupResult change(ChangeGroupCommand cmd) {

		GroupEntity entity = jpaGroupRepository.findById(cmd.groupId()).orElseThrow(
			() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND)
		);

		//오너 인지 확인
		boolean isOwner = groupRoleRepository.checkRole(cmd.userId(), entity.getId(), GroupMemberRole.OWNER);

		if(!isOwner) {
			throw new BusinessException(ErrorCode.NOT_OWNER);
		}

		entity.change(cmd);

		// // gRPC로 image 서비스에 url 조회
		// GetUrlByReferenceRequest request = GetUrlByReferenceRequest.newBuilder()
		// 	.setReferenceType(Type.GROUP)
		// 	.setReferenceId(cmd.groupId())
		// 	.build();
		//
		// GetUrlByReferenceResponse response = imageCommandServiceStub.getUrlByReference(request);
		// String imageUrl = response.getImageUrl();
		//
		// return ChangeGroupResult.of(entity, imageUrl);

		if (cmd.imageRefId() != null) {
			ActivateImageRequest request = ActivateImageRequest.newBuilder()
				.setImageRefId(cmd.imageRefId())
				.setReferenceType(Type.GROUP)
				.setReferenceId(cmd.groupId())
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

			GetUrlByReferenceRequest requestUrl = GetUrlByReferenceRequest.newBuilder()
				.setReferenceType(Type.GROUP)
				.setReferenceId(cmd.groupId())
				.build();

			GetUrlByReferenceResponse response = imageCommandServiceStub.getUrlByReference(requestUrl);
			String imageUrl = response.getImageUrl();

			return ChangeGroupResult.of(entity, imageUrl);
		}

		return ChangeGroupResult.of(entity, GROUP_DEFAULT_URL);
	}
}
