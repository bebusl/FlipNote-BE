package flipnote.image.adapter.in.grpc;


import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.grpc.server.service.GrpcService;

import flipnote.image.application.port.in.ActivateImageUseCase;
import flipnote.image.application.port.in.ChangeImageUseCase;
import flipnote.image.application.port.in.DeleteImageUseCase;
import flipnote.image.application.port.in.GetImageUrlByReferenceUseCase;
import flipnote.image.application.port.in.result.ChangeImageResult;
import flipnote.image.domain.model.reference.ReferenceType;
import flipnote.image.grpc.v1.ActivateImageRequest;
import flipnote.image.grpc.v1.ActivateImageResponse;
import flipnote.image.grpc.v1.ChangeImageRequest;
import flipnote.image.grpc.v1.ChangeImageResponse;
import flipnote.image.grpc.v1.DeleteByIdRequest;
import flipnote.image.grpc.v1.DeleteByIdResponse;
import flipnote.image.grpc.v1.GetUrlByReferenceRequest;
import flipnote.image.grpc.v1.GetUrlByReferenceResponse;
import flipnote.image.grpc.v1.GetUrlsByIdsRequest;
import flipnote.image.grpc.v1.GetUrlsByIdsResponse;
import flipnote.image.grpc.v1.ImageCommandServiceGrpc;
import flipnote.image.grpc.v1.Type;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ImageCommandGrpcService extends ImageCommandServiceGrpc.ImageCommandServiceImplBase {

	private final GetImageUrlByReferenceUseCase getImageUrlByReferenceUseCase;
	private final ActivateImageUseCase activateImageUseCase;
	private final ChangeImageUseCase changeImageUseCase;
	private final DeleteImageUseCase deleteImageUseCase;

	/**
	 * 참조 타입 및 아이디를 통해 url 조회
	 * @param request
	 * @param responseObserver
	 */
	@Override
	public void getUrlByReference(GetUrlByReferenceRequest request,
		StreamObserver<GetUrlByReferenceResponse> responseObserver) {
		try {
			ReferenceType type = mapType(request.getReferenceType());
			//url 조회
			String url = getImageUrlByReferenceUseCase.getUrl(type, request.getReferenceId());

			GetUrlByReferenceResponse res = GetUrlByReferenceResponse.newBuilder()
				.setImageUrl(url)
				.build();

			responseObserver.onNext(res);
			responseObserver.onCompleted();
		} catch (Exception e) {
			responseObserver.onError(e);
		}
	}

	/**
	 * 이미지 활성화
	 * @param request
	 * @param responseObserver
	 */
	@Override
	public void activateImage(ActivateImageRequest request, StreamObserver<ActivateImageResponse> responseObserver) {
		try {
			ReferenceType type = mapType(request.getReferenceType());

			log.debug("{} {} {}", type.name(), request.getImageRefId(), request.getReferenceId());

			String url = activateImageUseCase.activateImage(request.getImageRefId(), type, request.getReferenceId());

			ActivateImageResponse res = ActivateImageResponse.newBuilder()
				.setUrl(url)
				.build();

			responseObserver.onNext(res);
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.error("gRPC 에러 메시지: {}", e.getMessage());
			responseObserver.onError(e);
		}
	}

	@Override
	public void changeImage(ChangeImageRequest request, StreamObserver<ChangeImageResponse> responseObserver) {
		try {
			ReferenceType type = mapType(request.getReferenceType());
			ChangeImageResult changeImageResult = changeImageUseCase.changeImage(request.getImageRefId(), type,
				request.getReferenceId());

			ChangeImageResponse res = ChangeImageResponse.newBuilder()
				.setImageRefId(changeImageResult.imageRefId())
				.setUrl(changeImageResult.url())
				.build();

			responseObserver.onNext(res);
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.error("changeImage", e);
			responseObserver.onError(e);
		}
	}

	@Override
	public void getUrlsByIds(GetUrlsByIdsRequest request,
		StreamObserver<GetUrlsByIdsResponse> responseObserver) {
		try {
			Map<Long, String> urlMap = getImageUrlByReferenceUseCase.getUrls(
				request.getIdsList()
			);

			GetUrlsByIdsResponse res = GetUrlsByIdsResponse.newBuilder()
				.putAllImageUrls(urlMap)
				.build();

			responseObserver.onNext(res);
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.error("gRPC 에러 메시지: {}", e.getMessage());
			responseObserver.onError(e);
		}
	}

	@Override
	public void deleteById(DeleteByIdRequest request,
		StreamObserver<DeleteByIdResponse> responseObserver) {
		try {
			deleteImageUseCase.deleteImage(request.getImageRefId());

			responseObserver.onNext(DeleteByIdResponse.newBuilder().build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.error("gRPC 에러 메시지: {}", e.getMessage());
			responseObserver.onError(e);
		}
	}

	/**
	 * 타입 변환
	 * @param type
	 * @return
	 */
	private ReferenceType mapType(Type type) {
		return switch (type) {
			case USER -> ReferenceType.USER;
			case GROUP -> ReferenceType.GROUP;
			case CARD_SET -> ReferenceType.CARD_SET;
			default -> throw new IllegalArgumentException("INVALID_REFERENCE_TYPE");
		};
	}
}
