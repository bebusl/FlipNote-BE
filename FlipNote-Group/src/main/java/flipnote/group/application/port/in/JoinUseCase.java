package flipnote.group.application.port.in;

import flipnote.group.application.port.in.command.ApplicationFormCommand;
import flipnote.group.application.port.in.command.FindJoinFormCommand;
import flipnote.group.application.port.in.result.ApplicationFormResult;
import flipnote.group.application.port.in.result.FindJoinFormListResult;

public interface JoinUseCase {
	ApplicationFormResult joinRequest(ApplicationFormCommand cmd);

	FindJoinFormListResult findJoinFormList(FindJoinFormCommand cmd);
}
