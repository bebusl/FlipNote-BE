package flipnote.image.application.port.in;

import flipnote.image.domain.model.reference.ReferenceType;

public interface ActivateImageUseCase {
	String activateImage(Long imageRefId, ReferenceType referenceType, Long referenceId);
}
