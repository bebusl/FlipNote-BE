package flipnote.user.interfaces.grpc;

import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.stereotype.Component;

import flipnote.user.domain.common.BizException;
import flipnote.user.domain.common.ErrorCode;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GrpcExceptionHandlerImpl implements GrpcExceptionHandler {

	@Override
	public StatusException handleException(Throwable t) {
		switch (t) {
			case BizException e -> {
				ErrorCode errorCode = e.getErrorCode();
				log.warn("gRPC BizException: code={}, status={}, message={}",
					errorCode.getCode(), errorCode.getStatus(), errorCode.getMessage());
				return toGrpcStatus(errorCode)
					.withDescription(errorCode.getMessage())
					.asException();
			}
			case StatusException e -> {
				log.warn("gRPC StatusException: status={}, description={}",
					e.getStatus().getCode(), e.getStatus().getDescription());
				return e;
			}
			case StatusRuntimeException e -> {
				log.warn("gRPC StatusRuntimeException: status={}, description={}",
					e.getStatus().getCode(), e.getStatus().getDescription());
				return e.getStatus().asException(e.getTrailers());
			}
			default -> {
			}
		}
		log.error("gRPC Unhandled exception", t);
		return Status.INTERNAL.withDescription("Internal server error").asException();
	}

	private Status toGrpcStatus(ErrorCode errorCode) {
		return switch (errorCode.getStatus()) {
			case 400 -> Status.INVALID_ARGUMENT;
			case 401 -> Status.UNAUTHENTICATED;
			case 403 -> Status.PERMISSION_DENIED;
			case 404 -> Status.NOT_FOUND;
			case 409 -> Status.ALREADY_EXISTS;
			case 429 -> Status.RESOURCE_EXHAUSTED;
			default -> Status.INTERNAL;
		};
	}
}
