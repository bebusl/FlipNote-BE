package flipnote.image.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.image.application.port.in.DeleteImageUseCase;
import flipnote.image.application.port.out.ImageRefPort;
import flipnote.image.domain.model.reference.ReferenceType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteImageService implements DeleteImageUseCase {

	private final ImageRefPort imageRefPort;

	@Override
	@Transactional
	public void deleteImage(Long imageRefId) {
		imageRefPort.delete(imageRefId);
	}
}
