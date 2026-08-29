package flipnote.group.application.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.group.adapter.out.entity.GroupEntity;
import flipnote.group.api.dto.request.GroupListRequestDto;
import flipnote.group.api.dto.response.CursorPagingResponseDto;
import flipnote.group.application.port.in.FindGroupUseCase;
import flipnote.group.application.port.in.command.FindGroupCommand;
import flipnote.group.application.port.in.result.FindGroupResult;
import flipnote.group.application.port.out.GroupMemberRepositoryPort;
import flipnote.group.application.port.out.GroupRepositoryPort;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import flipnote.image.grpc.v1.GetUrlByReferenceRequest;
import flipnote.image.grpc.v1.GetUrlByReferenceResponse;
import flipnote.image.grpc.v1.GetUrlsByIdsRequest;
import flipnote.image.grpc.v1.GetUrlsByIdsResponse;
import flipnote.image.grpc.v1.ImageCommandServiceGrpc;
import flipnote.image.grpc.v1.Type;
import io.grpc.StatusRuntimeException;
import flipnote.group.domain.model.group.GroupInfo;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindGroupService implements FindGroupUseCase {

	private final GroupRepositoryPort groupRepository;
	private final GroupMemberRepositoryPort groupMemberRepository;
	private final ImageCommandServiceGrpc.ImageCommandServiceBlockingStub imageCommandServiceStub;

	@Value("${image.default.group}")
	private String GROUP_DEFAULT_URL;

	/**
	 * 하나의 그룹에 대한 정보 조회
	 * @param cmd
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public FindGroupResult findGroup(FindGroupCommand cmd) {

		// // 유저가 그룹 내에 존재하는지 확인
		// boolean isMember = groupMemberRepository.existsUserInGroup(cmd.groupId(), cmd.userId());
		//
		// if(!isMember) {
		// 	throw new BusinessException(ErrorCode.USER_NOT_IN_GROUP);
		// }

		GroupEntity group = groupRepository.findById(cmd.groupId());

		// gRPC로 image 서비스에 url 조회

		if(group.getImageRefId()==null) {
			return FindGroupResult.of(group, GROUP_DEFAULT_URL);
		}

		GetUrlByReferenceRequest request = GetUrlByReferenceRequest.newBuilder()
			.setReferenceType(Type.GROUP)
			.setReferenceId(cmd.groupId())
			.build();

		String imageUrl;
		try {
			GetUrlByReferenceResponse response = imageCommandServiceStub.getUrlByReference(request);
			imageUrl = response.getImageUrl();
		} catch (StatusRuntimeException e) {
			switch (e.getStatus().getCode()) {
				case NOT_FOUND -> throw new BusinessException(ErrorCode.IMAGE_NOT_FOUND);
				case INVALID_ARGUMENT -> throw new BusinessException(ErrorCode.IMAGE_INVALID_REQUEST);
				case INTERNAL -> throw new BusinessException(ErrorCode.IMAGE_SERVER_ERROR);
				default -> throw new BusinessException(ErrorCode.IMAGE_SERVICE_ERROR);
			}
		}

		return FindGroupResult.of(group, imageUrl);
	}

	/**
	 * 전체 그룹 조회
	 * @param userId
	 * @param req
	 * @return
	 */
	@Override
	public CursorPagingResponseDto<GroupInfo> findAllGroup(Long userId, GroupListRequestDto req) {
		List<GroupInfo> groups = groupRepository.findAllByCursor(
			req.getCursorId(),
			req.getCategory(),
			req.getSize(),
			req.getGroupName(),
			userId);

		enrichGroupsWithImageUrl(groups);

		return createGroupInfoCursorPagingResponse(req, groups);
	}

	/**
	 * 내가 가입한 그룹 전체 조회
	 * @param userId
	 * @param req
	 * @return
	 */
	@Override
	public CursorPagingResponseDto<GroupInfo> findMyGroup(Long userId, GroupListRequestDto req) {
		List<GroupInfo> groups = groupRepository.findAllByCursorAndUserId(
			req.getCursorId(),
			req.getCategory(),
			req.getSize(),
			userId,
			req.getGroupName());

		enrichGroupsWithImageUrl(groups);

		return createGroupInfoCursorPagingResponse(req, groups);
	}

	/**
	 * 내가 생성한 그룹 전체 조회
	 * @param userId
	 * @param req
	 * @return
	 */
	@Override
	public CursorPagingResponseDto<GroupInfo> findCreatedGroup(Long userId, GroupListRequestDto req) {
		List<GroupInfo> groups = groupRepository.findAllByCursorAndCreatedUserId(
			req.getCursorId(),
			req.getCategory(),
			req.getSize(),
			userId,
			req.getGroupName());

		enrichGroupsWithImageUrl(groups);

		return createGroupInfoCursorPagingResponse(req, groups);
	}

	/**
	 * 리스트 조회시 response 생성
	 */
	private CursorPagingResponseDto<GroupInfo> createGroupInfoCursorPagingResponse(GroupListRequestDto req,
		List<GroupInfo> groups) {
		boolean hasNext = groups.size() > req.getSize();

		if (hasNext) {
			groups = groups.subList(0, req.getSize());
		}

		Long nextCursor = hasNext ? groups.get(groups.size() - 1).getGroupId() : null;

		return CursorPagingResponseDto.of(groups, hasNext, nextCursor);
	}

	private void enrichGroupsWithImageUrl(List<GroupInfo> groups) {
		List<Long> imageRefIds = groups.stream()
			.map(GroupInfo::getImageRefId)
			.filter(Objects::nonNull)
			.toList();

		Map<Long, String> imageUrlMap = Collections.emptyMap();

		if (!imageRefIds.isEmpty()) {
			GetUrlsByIdsRequest request = GetUrlsByIdsRequest.newBuilder()
				.addAllIds(imageRefIds)
				.build();

			try {
				GetUrlsByIdsResponse response = imageCommandServiceStub.getUrlsByIds(request);
				imageUrlMap = response.getImageUrlsMap();

			} catch (StatusRuntimeException e) {
				switch (e.getStatus().getCode()) {
					case NOT_FOUND -> throw new BusinessException(ErrorCode.IMAGE_NOT_FOUND);
					case INVALID_ARGUMENT -> throw new BusinessException(ErrorCode.IMAGE_INVALID_REQUEST);
					case INTERNAL -> throw new BusinessException(ErrorCode.IMAGE_SERVER_ERROR);
					default -> throw new BusinessException(ErrorCode.IMAGE_SERVICE_ERROR);
				}
			}
		}

		// imageUrl 세팅 (null이면 기본 이미지)
		Map<Long, String> finalImageUrlMap = imageUrlMap;
		groups.forEach(group -> group.setImageUrl(
			group.getImageRefId() == null
				? GROUP_DEFAULT_URL
				: finalImageUrlMap.getOrDefault(group.getImageRefId(), GROUP_DEFAULT_URL)
		));
	}

	/**
	 * gRPC용 내가 가입한 그룹 전체 조회
	 * @param userId
	 * @return
	 */
	@Override
	public List<Long> findMyGroup(Long userId) {
		return groupMemberRepository.findGroupIdsByUserId(userId);
	}
}
