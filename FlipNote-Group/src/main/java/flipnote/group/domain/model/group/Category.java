package flipnote.group.domain.model.group;

public enum Category {
	IT, ENGLISH, MATH, SCIENCE, HISTORY, GEOGRAPHY, KOREAN;

	public static Category from(String category) {
		if (category == null || category.isEmpty()) {
			return null;
		}
		try {
			return Category.valueOf(category);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
