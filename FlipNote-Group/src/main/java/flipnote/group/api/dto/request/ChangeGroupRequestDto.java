package flipnote.group.api.dto.request;

import flipnote.group.domain.model.group.Category;
import flipnote.group.domain.model.group.JoinPolicy;
import flipnote.group.domain.model.group.Visibility;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChangeGroupRequestDto(
	@NotBlank String name,
	@NotNull Category category,
	@NotBlank String description,
	@NotNull JoinPolicy joinPolicy,
	@NotNull Visibility visibility,
	@NotNull @Min(1) @Max(100) Integer maxMember,
	Long imageRefId
) {
}
