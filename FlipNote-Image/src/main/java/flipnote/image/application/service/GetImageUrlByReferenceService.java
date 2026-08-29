package flipnote.image.application.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.image.application.port.in.GetImageUrlByReferenceUseCase;
import flipnote.image.application.port.out.DefaultImagePort;
import flipnote.image.application.port.out.ImagePort;
import flipnote.image.application.port.out.ImageRefPort;
import flipnote.image.application.port.out.PublicUrlPort;
import flipnote.image.domain.model.reference.ReferenceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetImageUrlByReferenceService implements GetImageUrlByReferenceUseCase {

	private final ImagePort imagePort;
	private final PublicUrlPort publicUrlPort;
	private final DefaultImagePort defaultImagePort;
	private final ImageRefPort imageRefPort;

	@Override
	@Transactional(readOnly = true)
	public String getUrl(ReferenceType type, Long referenceId) {

		log.debug("{}{}", referenceId, type);

		var image = imagePort.findByReference(type, referenceId);

		// 이미지 없을 시 기본 이미지 출력
		if(image.isEmpty()) {
			return publicUrlPort.urlOf(defaultImagePort.defaultUrl(type));
		}

		log.debug("{} {}", image.get().id(), image.get().s3Key());

		return publicUrlPort.urlOf(image.get().s3Key());
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Long, String> getUrls(List<Long> referenceIds) {
		return referenceIds.stream()
			.distinct()
			.collect(Collectors.toMap(
				id -> id,
				id -> imageRefPort.getUrlByRefId(id)
			));
	}
}
