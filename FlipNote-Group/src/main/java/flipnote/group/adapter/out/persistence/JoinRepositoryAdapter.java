package flipnote.group.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import flipnote.group.adapter.out.entity.JoinEntity;
import flipnote.group.application.port.out.JoinRepositoryPort;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import flipnote.group.infrastructure.persistence.jpa.JoinRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JoinRepositoryAdapter implements JoinRepositoryPort {

	private final JoinRepository joinRepository;

	@Override
	public boolean existsJoin(Long groupId, Long userId) {
		return joinRepository.existsByUserIdAndGroupId(userId, groupId);
	}

	@Override
	public void save(JoinEntity join) {
		joinRepository.save(join);
	}

	@Override
	public List<JoinEntity> findFormList(Long groupId) {

		List<JoinEntity> joinList = joinRepository.findAllByGroupId(groupId);

		return joinList;
	}

	@Override
	public JoinEntity findJoin(Long joinId) {

		JoinEntity entity = joinRepository.findById(joinId).orElseThrow(
			() -> new BusinessException(ErrorCode.JOIN_NOT_FOUND)
		);
		return entity;
	}

	@Override
	public JoinEntity updateJoin(JoinEntity join) {
		return joinRepository.save(join);
	}

	@Override
	public List<JoinEntity> findMyJoinList(Long userId) {
		List<JoinEntity> joinList = joinRepository.findAllByUserId(userId);

		return joinList;
	}

	@Override
	public void delete(JoinEntity join) {
		joinRepository.delete(join);
	}
}
