package flipnote.group.application.port.in.result;

import flipnote.group.adapter.out.entity.JoinEntity;

public record ApplicationFormResult(
	JoinEntity join) {
	public static ApplicationFormResult of(JoinEntity join) {
		return new ApplicationFormResult(join);
	}
}
