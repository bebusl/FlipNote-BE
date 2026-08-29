package flipnote.image.application.port.in.command;

import flipnote.image.domain.model.reference.ReferenceType;

public record IssuePresignedUrlCommand(String fileName, ReferenceType type) {}

