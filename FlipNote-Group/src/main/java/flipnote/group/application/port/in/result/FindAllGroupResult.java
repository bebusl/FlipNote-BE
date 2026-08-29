package flipnote.group.application.port.in.result;

import java.util.List;

import flipnote.group.domain.model.group.GroupInfo;

public record FindAllGroupResult(List<GroupInfo> groups) {
}
