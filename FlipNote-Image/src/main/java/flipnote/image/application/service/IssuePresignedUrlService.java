package flipnote.image.application.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.image.application.port.in.IssuePresignedUrlUseCase;
import flipnote.image.application.port.in.command.IssuePresignedUrlCommand;
import flipnote.image.application.port.in.result.IssuePresignedUrlResult;
import flipnote.image.application.port.out.ImagePort;
import flipnote.image.application.port.out.ImageRefPort;
import flipnote.image.application.port.out.PresignedUrlPort;
import flipnote.image.application.port.out.PublicUrlPort;
import flipnote.image.domain.policy.ImageNamingPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssuePresignedUrlService implements IssuePresignedUrlUseCase {

	private static final int EXPIRE_MINUTES = 5;

	private final ImagePort imagePort;
	private final ImageRefPort imageRefPort;
	private final PresignedUrlPort presignedUrlPort;
	private final PublicUrlPort publicUrlPort;

	private final ImageNamingPolicy imageNamingPolicy;

	@Override
	@Transactional
	public IssuePresignedUrlResult issuePresignedUrl(IssuePresignedUrlCommand cmd) {

		String fileName = cmd.fileName();

		if(fileName == null || fileName.isBlank()) {
			throw new IllegalArgumentException("fileName is Empty");
		}

		/**
		 * 파일명에서 내용 추출
		 */
		String hash = imageNamingPolicy.hashFromFileName(fileName);
		String s3Key = imageNamingPolicy.s3KeyFromFileName(fileName);
		String contentType = imageNamingPolicy.contentTypeFromFileName(fileName);

		/**
		 * 이미 존재하는 이미지가 있을 경우
		 * 기존 이미지에 연결한 ref를 생성하고 반환
		 */
		var existingImage = imagePort.findByHash(hash);

		log.debug(hash);

		if(existingImage.isPresent()) {
			// 이미지 참조 저장
			var imageRef = imageRefPort.save(existingImage.get().id());
			String url = publicUrlPort.urlOf(existingImage.get().s3Key());

			return new IssuePresignedUrlResult(imageRef.imageRefId(), url);
		}

		// 없으면 url을 발급
		String presignedUrl = presignedUrlPort.issuePresignedUrl(s3Key, contentType, EXPIRE_MINUTES);

		log.debug("presignedUrl "+presignedUrl);

		// 이미지 저장 후 이미지 참조 저장
		var saveImage = imagePort.save(new ImagePort.newImage(hash, s3Key));

		//이미지 참조에 hash랑 s3키만 존재하는 이미지 엔티티를 연결
		var saveImageRef = imageRefPort.save(saveImage.id());

		//발급 후 반환
		return new IssuePresignedUrlResult(saveImageRef.imageRefId(), presignedUrl);
	}
}
