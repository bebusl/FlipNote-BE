package flipnote.group.application.port.out;

import java.util.List;

import flipnote.group.adapter.out.entity.JoinEntity;

public interface JoinRepositoryPort {
	boolean existsJoin(Long groupId, Long userId);

	void save(JoinEntity join);

	List<JoinEntity> findFormList(Long groupId);

	JoinEntity findJoin(Long joinId);

	JoinEntity updateJoin(JoinEntity join);

	List<JoinEntity> findMyJoinList(Long userId);

	void delete(JoinEntity join);
}
