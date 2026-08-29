package flipnote.image.application.port.in;

import flipnote.image.application.port.in.result.ChangeImageResult;
import flipnote.image.domain.model.reference.ReferenceType;

public interface ChangeImageUseCase {
	ChangeImageResult changeImage(Long imageRefId, ReferenceType type, Long referenceId);
}
