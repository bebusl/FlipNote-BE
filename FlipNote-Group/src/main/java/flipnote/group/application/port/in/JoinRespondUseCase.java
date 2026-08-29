package flipnote.group.application.port.in;

import flipnote.group.application.port.in.command.JoinRespondCommand;
import flipnote.group.application.port.in.result.JoinRespondResult;

public interface JoinRespondUseCase {
	JoinRespondResult joinRespond(JoinRespondCommand cmd);
}
