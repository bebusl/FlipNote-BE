package flipnote.image.application.port.out;

import flipnote.image.domain.model.reference.ReferenceType;

public interface ImageS3KeyPort {
	String getS3KeyByReference(ReferenceType type, Long referenceId);
}
