package flipnote.image.infrastructure.persistence.querydsl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import flipnote.image.domain.model.reference.ImageRef;
import flipnote.image.domain.model.reference.QImageRef;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ImageRefRepositoryImpl implements ImageRefRepositoryCustom {

	// private final JpaQueryFactory queryFactory;
	private final JPAQueryFactory queryFactory;

	QImageRef imageRef = QImageRef.imageRef;

	/**
	 * <=의 경우 중복 문제 발생 가능성 높아짐
	 *
	 * @param cutOffTime 특정 시간 ex) 10분
	 * @param lastId 커서 기반 마지막 id
	 * @param pageable 커서 기반
	 * @return
	 */
	@Override
	public List<ImageRef> findByCreatedAtLessThanAndIdLessThan(
		LocalDateTime cutOffTime,
		Long lastId,
		Pageable pageable) {

		int limit = pageable.getPageSize();

		BooleanBuilder where = new BooleanBuilder()
			.and(imageRef.image.isNull())
			.and(imageRef.createdAt.lt(cutOffTime));

		if (lastId != null) {
			where.and(imageRef.id.lt(lastId));
		}

		return queryFactory
			.selectFrom(imageRef)
			.where(where)
			.orderBy(imageRef.id.desc())
			.limit(limit)
			.fetch();
	}
}
