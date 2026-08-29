package flipnote.image.application.port.in;

import java.util.List;
import java.util.Map;

import flipnote.image.domain.model.reference.ReferenceType;

public interface GetImageUrlByReferenceUseCase {
    String getUrl(ReferenceType type, Long referenceId);
    Map<Long, String> getUrls(List<Long> referenceIds);
}
