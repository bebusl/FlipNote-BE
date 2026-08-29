package flipnote.image.adapter.out.other;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import flipnote.image.application.port.out.DefaultImagePort;
import flipnote.image.domain.model.reference.ReferenceType;

@Component
public class DefaultImageAdapter implements DefaultImagePort {

	@Value("${image.default.group}")
	private String defaultGroupImage;

	@Value("${image.default.user}")
	private String defaultUserImage;

	@Value("${image.default.cardSet}")
	private String defaultCardSetImage;

	@Override
	public String defaultUrl(ReferenceType type) {
		return switch (type) {
			case USER -> defaultUserImage;
			case GROUP -> defaultGroupImage;
			case CARD_SET -> defaultCardSetImage;
		};
	}
}
