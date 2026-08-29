package flipnote.image.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.image.application.port.in.ChangeImageUseCase;
import flipnote.image.application.port.in.result.ChangeImageResult;
import flipnote.image.application.port.out.DefaultImagePort;
import flipnote.image.application.port.out.ImagePort;
import flipnote.image.application.port.out.ImageRefPort;
import flipnote.image.application.port.out.ImageS3KeyPort;
import flipnote.image.application.port.out.PublicUrlPort;
import flipnote.image.domain.model.reference.ReferenceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChangeImageService implements ChangeImageUseCase {

	private final ImageRefPort imageRefPort;
	private final DefaultImagePort defaultImagePort;
	private final ImageS3KeyPort imageS3KeyPort;
	private final PublicUrlPort publicUrlPort;

	@Override
	@Transactional
	public ChangeImageResult changeImage(Long newImageRefId, ReferenceType type, Long referenceId) {

		//참조 대상에 연결된 ref 조회
		var current = imageRefPort.findByReference(type, referenceId);

		//1. 제거만 할 경우
		if(newImageRefId == null) {
			current.ifPresent(id -> imageRefPort.delete(id));

			log.debug("다른 대상일 경우");
			return ChangeImageResult.from(null, defaultImagePort.defaultUrl(type));

		}

		//2. 신규 이미지로 변경할 경우
		var target = imageRefPort.findById(newImageRefId);

		//3. 다른 대상으로 연결되어있으면 금지
		if(target.referenceId() != null &&
			!(target.type() == type && target.referenceId() == referenceId)
		) {
			log.debug("다른 대상 연결");
			log.error("conflict_image_ref");
			throw new IllegalArgumentException("conflict_image_ref");
		}

		// 다른 경우 기존 연결 제거 후 새 연결
		if(current.isPresent() && !current.get().equals(newImageRefId)) {

			log.debug("기존 연결 삭제 후 새연결");

			current.ifPresent(imageRefPort::delete);
			imageRefPort.activate(newImageRefId, type, referenceId);
		}

		String s3Key = imageS3KeyPort.getS3KeyByReference(type, referenceId);
		String url = publicUrlPort.urlOf(s3Key);

		return ChangeImageResult.from(newImageRefId, url);
	}
}
