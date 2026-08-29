package flipnote.group.infrastructure.persistence.querydsl;

import java.util.List;

import flipnote.group.domain.model.group.Category;
import flipnote.group.domain.model.group.GroupInfo;

public interface GroupRepositoryCustom {
	List<GroupInfo> findAllByCursor(Long lastId, Category category, int pageSize, String groupName, Long userId);

	List<GroupInfo> findAllByCursorAndUserId(Long lastId, Category category, int pageSize, Long userId, String groupName);

	List<GroupInfo> findAllByCursorAndCreatedUserId(Long cursorId, Category category, int size, Long id, String groupName);
}
