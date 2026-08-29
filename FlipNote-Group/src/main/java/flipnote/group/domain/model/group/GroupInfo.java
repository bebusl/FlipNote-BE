package flipnote.group.domain.model.group;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
public class GroupInfo {
	Long groupId;
	String name;
	String description;
	Category category;
	Long imageRefId;
	String imageUrl;

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
}
