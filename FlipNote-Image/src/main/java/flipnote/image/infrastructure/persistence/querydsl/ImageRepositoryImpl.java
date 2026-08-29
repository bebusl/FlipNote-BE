package flipnote.image.infrastructure.persistence.querydsl;

import static flipnote.image.domain.model.image.QImage.*;
import static flipnote.image.domain.model.reference.QImageRef.*;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import flipnote.image.domain.model.image.Image;
import flipnote.image.domain.model.image.QImage;
import flipnote.image.domain.model.reference.QImageRef;
import flipnote.image.domain.model.reference.Reference;
import flipnote.image.domain.model.reference.ReferenceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ImageRepositoryImpl implements ImageRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public Optional<Image> findAttachedImage(Long referenceId, ReferenceType type) {

		BooleanBuilder where = new BooleanBuilder()
			.and(imageRef.reference.type.eq(type))
			.and(imageRef.reference.id.eq(referenceId));

		Image image = queryFactory
			.select(imageRef.image)
			.from(imageRef)
			.where(where)
			.fetchOne();


		return Optional.ofNullable(image);
	}

	@Override
	public List<Image> findImagesNotExist(Long lastId, int batchSize) {

		BooleanBuilder where = new BooleanBuilder();

		if(lastId != null) {
			where.and(image.id.lt(lastId));
		}

		/**
		 * 참조하고 있지 않는 이미지 찾기
		 * select 1 ...
		 */
		BooleanExpression findNotReferenceImage = JPAExpressions.selectOne()
			.from(imageRef)
			.where(imageRef.image.eq(image))
			.notExists();

		where.and(findNotReferenceImage);

		return queryFactory
			.selectFrom(image)
			.where(where)
			.orderBy(image.id.desc())
			.limit(batchSize)
			.fetch();

	}

}
