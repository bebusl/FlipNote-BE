package flipnote.group.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.group.adapter.out.entity.JoinEntity;
import flipnote.group.adapter.out.persistence.JoinRepositoryAdapter;
import flipnote.group.application.port.in.DeleteJoinUseCase;
import flipnote.group.application.port.in.command.DeleteJoinCommand;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteJoinService implements DeleteJoinUseCase {

	private final JoinRepositoryAdapter joinRepository;

	@Override
	@Transactional
	public void deleteJoin(DeleteJoinCommand cmd) {

		JoinEntity join = joinRepository.findJoin(cmd.joinId());

		if(!join.getUserId().equals(cmd.userId())) {
			throw new BusinessException(ErrorCode.PERMISSION_DENIED);
		}
		joinRepository.delete(join);
	}
}
