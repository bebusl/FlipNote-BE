package flipnote.group.infrastructure.persistence.querydsl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import flipnote.group.adapter.out.entity.QGroupEntity;
import flipnote.group.adapter.out.entity.QGroupMemberEntity;
import flipnote.group.domain.model.group.Category;
import flipnote.group.domain.model.group.GroupInfo;
import flipnote.group.domain.model.group.Visibility;
import flipnote.group.domain.model.member.GroupMemberRole;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class GroupRepositoryImpl implements GroupRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	QGroupEntity group = QGroupEntity.groupEntity;
	QGroupMemberEntity groupMember = QGroupMemberEntity.groupMemberEntity;

	/**
	 * 모든 그룹 전체 조회
	 * @param lastId
	 * @param category
	 * @param pageSize
	 * @return
	 */
	@Override
	public List<GroupInfo> findAllByCursor(Long lastId, Category category, int pageSize, String groupName, Long userId) {
		//삭제되지 않은
		BooleanBuilder where = new BooleanBuilder()
			.and(group.deletedAt.isNull());

		//커서 기반
		if (lastId != null) {
			where.and(group.id.lt(lastId));
		}

		if (groupName != null && !groupName.isBlank()) {
			where.and(group.name.contains(groupName));
		}

		//카테고리 제한
		if (category != null) {
			where.and(group.category.eq(category));
		}

		BooleanExpression isPublic = group.visibility.eq(Visibility.PUBLIC);

		BooleanExpression isMember = JPAExpressions.selectOne()
			.from(groupMember)
			.where(
				groupMember.groupId.eq(group.id),
				groupMember.userId.eq(userId)
			)
			.exists();

		where.and(isPublic.or(isMember));

		return queryFactory.select(Projections.constructor(
				GroupInfo.class,
				group.id,
				group.name,
				group.description,
				group.category,
				group.imageRefId,
				Expressions.nullExpression(String.class)
			))
			.from(group)
			.where(where)
			.orderBy(group.id.desc())
			.limit(pageSize + 1)
			.fetch();
	}

	/**
	 * 내가 가입한 그룹 전체 조회
	 * @param lastId
	 * @param category
	 * @param pageSize
	 * @param userId
	 * @return
	 */
	@Override
	public List<GroupInfo> findAllByCursorAndUserId(Long lastId, Category category, int pageSize, Long userId, String groupName) {

		BooleanBuilder where = new BooleanBuilder()
			.and(group.deletedAt.isNull())
			.and(groupMember.userId.eq(userId));

		if (lastId != null) {
			where.and(group.id.lt(lastId));
		}

		if (groupName != null && !groupName.isBlank()) {
			where.and(group.name.contains(groupName));
		}

		if (category != null) {
			where.and(group.category.eq(category));
		}

		return queryFactory
			.select(Projections.constructor(
				GroupInfo.class,
				group.id,
				group.name,
				group.description,
				group.category,
				group.imageRefId,
				Expressions.nullExpression(String.class)
			))
			.from(groupMember)
			.join(group).on(group.id.eq(groupMember.groupId))
			.where(where.and(groupMember.deletedAt.isNull()))
			.orderBy(group.id.desc())
			.limit(pageSize + 1)
			.fetch();
	}

	/**
	 * 내가 생성한 그룹 전체 조회
	 * @param lastId
	 * @param category
	 * @param pageSize
	 * @param userId
	 * @return
	 */
	@Override
	public List<GroupInfo> findAllByCursorAndCreatedUserId(Long lastId, Category category, int pageSize, Long userId, String groupName) {

		return queryFactory
			.select(Projections.constructor(
				GroupInfo.class,
				group.id,
				group.name,
				group.description,
				group.category,
				group.imageRefId,
				Expressions.nullExpression(String.class)
			))
			.from(group)
			.join(groupMember).on(groupMember.groupId.eq(group.id))
			.where(
				group.deletedAt.isNull(),
				groupMember.deletedAt.isNull(),
				groupMember.userId.eq(userId),
				groupMember.role.role.eq(GroupMemberRole.OWNER),
				lastId != null ? group.id.lt(lastId) : null,
				category != null ? group.category.eq(category) : null,
				groupName != null && !groupName.isBlank() ? group.name.contains(groupName) : null
			)
			.orderBy(group.id.desc())
			.limit(pageSize + 1)
			.fetch();
	}


}
