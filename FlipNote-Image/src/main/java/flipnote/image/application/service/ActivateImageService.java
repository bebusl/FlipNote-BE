package flipnote.image.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.image.application.port.in.ActivateImageUseCase;
import flipnote.image.application.port.out.ImagePort;
import flipnote.image.application.port.out.ImageRefPort;
import flipnote.image.application.port.out.ObjectMetadataPort;
import flipnote.image.application.port.out.PublicUrlPort;
import flipnote.image.domain.model.reference.ReferenceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivateImageService implements ActivateImageUseCase {

	private final ImagePort imagePort;
	private final ImageRefPort imageRefPort;
	private final ObjectMetadataPort objectMetadataPort;
	private final PublicUrlPort publicUrlPort;

	/**
	 * 이미지 활성화
	 * @param imageRefId 이미지 참조 아이디
	 * @param referenceType 참조 타입
	 * @param referenceId 참조 아이디
	 */
	@Override
	@Transactional
	public String activateImage(Long imageRefId, ReferenceType referenceType, Long referenceId) {
		//이미지 참조 활성화
		imageRefPort.activate(imageRefId, referenceType, referenceId);

		//이미지 참조로부터 이미지 아이디 조회
		//범용성 문제로 두가지를 분리
		Long imageId = imageRefPort.getImageIdByRefId(imageRefId);

		var row = imagePort.findImageHeadById(imageId);

		String mimeType = row.mimeType();
		Long sizeBytes = row.sizeBytes();

		if(mimeType == null || mimeType.isBlank() || sizeBytes == null) {
			var metadata = objectMetadataPort.head(row.s3Key());
			imagePort.updateMetadata(imageId, metadata.contentType(), metadata.contentLength());
		}

		return publicUrlPort.urlOf(row.s3Key());
	}
}
