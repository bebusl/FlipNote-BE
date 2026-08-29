package flipnote.reaction.interfaces.grpc;

import java.util.List;
import java.util.Map;

import org.springframework.grpc.server.service.GrpcService;

import flipnote.reaction.application.bookmark.BookmarkReader;
import flipnote.reaction.application.like.LikeReader;
import flipnote.reaction.domain.bookmark.BookmarkTargetType;
import flipnote.reaction.domain.like.LikeTargetType;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import reaction.Reaction.AreReactedRequest;
import reaction.Reaction.AreReactedResponse;
import reaction.Reaction.IsReactedRequest;
import reaction.Reaction.IsReactedResponse;
import reaction.ReactionServiceGrpc;

@GrpcService
@RequiredArgsConstructor
public class ReactionGrpcService extends ReactionServiceGrpc.ReactionServiceImplBase {

	private final LikeReader likeReader;
	private final BookmarkReader bookmarkReader;

	@Override
	public void isLiked(IsReactedRequest request, StreamObserver<IsReactedResponse> responseObserver) {
		LikeTargetType targetType = parseTargetType(request.getTargetType(), LikeTargetType.class);
		boolean reacted = likeReader.isLiked(targetType, request.getTargetId(), request.getUserId());
		responseObserver.onNext(IsReactedResponse.newBuilder().setReacted(reacted).build());
		responseObserver.onCompleted();
	}

	@Override
	public void areLiked(AreReactedRequest request, StreamObserver<AreReactedResponse> responseObserver) {
		LikeTargetType targetType = parseTargetType(request.getTargetType(), LikeTargetType.class);
		List<Long> targetIds = request.getTargetIdsList();
		Map<Long, Boolean> results = likeReader.areLiked(targetType, targetIds, request.getUserId());
		responseObserver.onNext(AreReactedResponse.newBuilder().putAllResults(results).build());
		responseObserver.onCompleted();
	}

	@Override
	public void isBookmarked(IsReactedRequest request, StreamObserver<IsReactedResponse> responseObserver) {
		BookmarkTargetType targetType = parseTargetType(request.getTargetType(), BookmarkTargetType.class);
		boolean reacted = bookmarkReader.isBookmarked(targetType, request.getTargetId(), request.getUserId());
		responseObserver.onNext(IsReactedResponse.newBuilder().setReacted(reacted).build());
		responseObserver.onCompleted();
	}

	@Override
	public void areBookmarked(AreReactedRequest request, StreamObserver<AreReactedResponse> responseObserver) {
		BookmarkTargetType targetType = parseTargetType(request.getTargetType(), BookmarkTargetType.class);
		List<Long> targetIds = request.getTargetIdsList();
		Map<Long, Boolean> results = bookmarkReader.areBookmarked(targetType, targetIds, request.getUserId());
		responseObserver.onNext(AreReactedResponse.newBuilder().putAllResults(results).build());
		responseObserver.onCompleted();
	}

	private <T extends Enum<T>> T parseTargetType(String value, Class<T> enumClass) {
		try {
			return Enum.valueOf(enumClass, value);
		} catch (IllegalArgumentException e) {
			throw Status.INVALID_ARGUMENT
				.withDescription("Invalid target type: " + value)
				.asRuntimeException();
		}
	}
}
