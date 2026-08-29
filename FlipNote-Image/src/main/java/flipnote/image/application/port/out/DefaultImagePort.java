package flipnote.image.application.port.out;

import flipnote.image.domain.model.reference.ReferenceType;

public interface DefaultImagePort {
	String defaultUrl(ReferenceType type);
}
