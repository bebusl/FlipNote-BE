package flipnote.user.interfaces.grpc;

import java.util.List;

import org.springframework.grpc.server.service.GrpcService;

import flipnote.user.application.UserService;
import flipnote.user.application.result.UserResult;
import flipnote.user.grpc.GetUserByEmailRequest;
import flipnote.user.grpc.GetUserByEmailResponse;
import flipnote.user.grpc.GetUserByTokenRequest;
import flipnote.user.grpc.GetUserByTokenResponse;
import flipnote.user.grpc.GetUserRequest;
import flipnote.user.grpc.GetUserResponse;
import flipnote.user.grpc.GetUsersRequest;
import flipnote.user.grpc.GetUsersResponse;
import flipnote.user.grpc.UserQueryServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class GrpcUserQueryService extends UserQueryServiceGrpc.UserQueryServiceImplBase {

	private final UserService userService;

	@Override
	public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
		UserResult user = userService.findActiveUserById(request.getUserId())
			.orElseThrow(() -> Status.NOT_FOUND
				.withDescription("사용자를 찾을 수 없습니다.")
				.asRuntimeException());

		responseObserver.onNext(toUserResponse(user));
		responseObserver.onCompleted();
	}

	@Override
	public void getUsers(GetUsersRequest request, StreamObserver<GetUsersResponse> responseObserver) {
		List<Long> userIds = request.getUserIdsList();
		List<UserResult> users = userService.findActiveUsersByIds(userIds);

		GetUsersResponse response = GetUsersResponse.newBuilder()
			.addAllUsers(users.stream().map(this::toUserResponse).toList())
			.build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void getUserByEmail(GetUserByEmailRequest request, StreamObserver<GetUserByEmailResponse> responseObserver) {
		userService.findActiveUserByEmail(request.getEmail())
			.ifPresentOrElse(
				user -> responseObserver.onNext(
					GetUserByEmailResponse.newBuilder()
						.setExists(true)
						.setUser(toUserResponse(user))
						.build()
				),
				() -> responseObserver.onNext(
					GetUserByEmailResponse.newBuilder()
						.setExists(false)
						.build()
				)
			);

		responseObserver.onCompleted();
	}

	@Override
	public void getUserByToken(GetUserByTokenRequest request, StreamObserver<GetUserByTokenResponse> responseObserver) {
		log.debug("getUserByToken accessToken [{}]", request.getAccessToken());

		String accessToken = request.getAccessToken().trim();
		UserResult user = userService.findUserByToken(accessToken);

		responseObserver.onNext(
			GetUserByTokenResponse.newBuilder()
				.setUserId(user.id())
				.setNickname(user.nickname())
				.build()
		);
		responseObserver.onCompleted();
	}

	private GetUserResponse toUserResponse(UserResult user) {
		return GetUserResponse.newBuilder()
			.setId(user.id())
			.setEmail(user.email())
			.setNickname(user.nickname())
			.setProfileImageUrl(user.profileImageUrl())
			.build();
	}
}
