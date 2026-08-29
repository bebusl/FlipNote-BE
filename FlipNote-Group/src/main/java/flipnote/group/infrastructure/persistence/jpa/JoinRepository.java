package flipnote.group.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import flipnote.group.adapter.out.entity.JoinEntity;

public interface JoinRepository extends JpaRepository<JoinEntity, Long> {

	boolean existsByUserIdAndGroupId(Long userId, Long groupId);

	List<JoinEntity> findAllByGroupId(Long groupId);

	List<JoinEntity> findAllByUserId(Long userId);
}
