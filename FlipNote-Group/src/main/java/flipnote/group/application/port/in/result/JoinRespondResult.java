package flipnote.group.application.port.in.result;

import flipnote.group.adapter.out.entity.JoinEntity;

public record JoinRespondResult(
	JoinEntity join
) {
	public static JoinRespondResult of(JoinEntity join) {
		return new JoinRespondResult(join);
	}
}
