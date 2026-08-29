package flipnote.group.adapter.out.entity;

import flipnote.group.application.port.in.command.ChangeGroupCommand;
import flipnote.group.application.port.in.command.CreateGroupCommand;
import flipnote.group.domain.model.BaseEntity;
import flipnote.group.domain.model.group.Category;
import flipnote.group.domain.model.group.JoinPolicy;
import flipnote.group.domain.model.group.Visibility;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "app_groups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false, length = 50)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Category category;

	@Column(nullable = false)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private JoinPolicy joinPolicy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Visibility visibility;

	@Column(nullable = false)
	private Integer maxMember;

	private Long imageRefId;

	@Column(nullable = false)
	private Integer memberCount;

	@Builder
	private GroupEntity(String name, Category category, String description, JoinPolicy joinPolicy, Visibility visibility,
		Integer maxMember, Long imageRefId, Integer memberCount) {
		this.name = name;
		this.category = category;
		this.description = description;
		this.joinPolicy = joinPolicy;
		this.visibility = visibility;
		this.maxMember = maxMember;
		this.imageRefId = imageRefId;
		this.memberCount = memberCount;
	}

	/**
	 * 업데이트
	 * @param cmd
	 */
	public void change(
		ChangeGroupCommand cmd
	) {
		this.name = cmd.name();
		this.category = cmd.category();
		this.description = cmd.description();
		this.joinPolicy = cmd.joinPolicy();
		this.visibility = cmd.visibility();
		this.maxMember = cmd.maxMember();
		this.imageRefId = cmd.imageRefId();
	}

	/**
	 * 신규로 그룹 생성
	 * @param cmd
	 * @return
	 */
	public static GroupEntity create(CreateGroupCommand cmd) {
		validate(cmd);

		return GroupEntity.builder()
			.name(cmd.name())
			.category(cmd.category())
			.description(cmd.description())
			.joinPolicy(cmd.joinPolicy())
			.visibility(cmd.visibility())
			.maxMember(cmd.maxMember())
			.imageRefId(cmd.imageRefId())
			.memberCount(0)
			.build();
	}

	/**
	 * 파라미터 검증
	 * @param cmd
	 */
	private static void validate(CreateGroupCommand cmd) {
		if (cmd == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		if (cmd.name() == null || cmd.name().isBlank()) {
			throw new BusinessException(ErrorCode.GROUP_INVALID_NAME);
		}
		if (cmd.name().length() > 50) {
			throw new BusinessException(ErrorCode.GROUP_NAME_TOO_LONG);
		}
		if (cmd.maxMember() < 1 || cmd.maxMember() > 100) {
			throw new BusinessException(ErrorCode.GROUP_INVALID_MAX_MEMBER);
		}
		if (cmd.category() == null) {
			throw new BusinessException(ErrorCode.GROUP_INVALID_CATEGORY);
		}
		if (cmd.joinPolicy() == null) {
			throw new BusinessException(ErrorCode.GROUP_INVALID_JOIN_POLICY);
		}
		if (cmd.visibility() == null) {
			throw new BusinessException(ErrorCode.GROUP_INVALID_VISIBILITY);
		}
		if (cmd.description() == null || cmd.description().isBlank()) {
			throw new BusinessException(ErrorCode.GROUP_INVALID_DESCRIPTION);
		}
	}

	public void plusCount() {

		if(this.memberCount+1 > this.maxMember) {
			throw new BusinessException(ErrorCode.GROUP_MEMBER_LIMIT_EXCEEDED);
		}

		this.memberCount++;
	}

	public void minusCount() {

		if(this.memberCount-1 < 0) {
			throw new BusinessException(ErrorCode.MEMBER_COUNT_UNDERFLOW);
		}

		this.memberCount--;
	}
}
