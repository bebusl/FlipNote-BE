package flipnote.group.application.service;

import org.springframework.stereotype.Service;

import flipnote.group.application.port.in.CheckOwnerUseCase;
import flipnote.group.application.port.in.command.CheckOwnerCommand;
import flipnote.group.application.port.in.result.CheckOwnerResult;
import flipnote.group.application.port.out.GroupMemberRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CheckOwnerService implements CheckOwnerUseCase {

	private final GroupMemberRepositoryPort groupMemberRepository;

	/**
	 * 유저가 오너인지 체크
	 * @param cmd
	 * @return
	 */
	@Override
	public CheckOwnerResult checkOwner(CheckOwnerCommand cmd) {

		boolean isOwner = groupMemberRepository.checkOwner(cmd.groupId(), cmd.userId());

		return new CheckOwnerResult(isOwner);
	}
}
