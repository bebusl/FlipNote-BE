package flipnote.group.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import flipnote.group.adapter.out.entity.GroupEntity;
import flipnote.group.infrastructure.persistence.querydsl.GroupRepositoryCustom;
import jakarta.persistence.LockModeType;

public interface GroupRepository extends JpaRepository<GroupEntity, Long>, GroupRepositoryCustom {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select g from GroupEntity g where g.id = :id")
	Optional<GroupEntity> findByIdForUpdate(@Param("id") Long id);

}
