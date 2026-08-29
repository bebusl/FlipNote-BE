package flipnote.image.application.port.in;

import flipnote.image.application.port.in.command.IssuePresignedUrlCommand;
import flipnote.image.application.port.in.result.IssuePresignedUrlResult;

public interface IssuePresignedUrlUseCase {
	IssuePresignedUrlResult issuePresignedUrl(IssuePresignedUrlCommand cmd);
}
