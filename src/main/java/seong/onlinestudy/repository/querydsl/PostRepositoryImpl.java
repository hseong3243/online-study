package seong.onlinestudy.repository.querydsl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import seong.onlinestudy.domain.*;
import seong.onlinestudy.enumtype.PostCategory;

import javax.persistence.EntityManager;
import java.util.List;

import static seong.onlinestudy.domain.QComment.comment;
import static seong.onlinestudy.domain.QGroup.group;
import static seong.onlinestudy.domain.QMember.member;
import static seong.onlinestudy.domain.QPost.post;
import static seong.onlinestudy.domain.QPostStudy.postStudy;

public class PostRepositoryImpl implements PostRepositoryCustom {

    JPAQueryFactory query;

    public PostRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public Page<Post> findPostsWithComments(Long memberId, Long groupId, String search, PostCategory category, List<Long> studyIds, Pageable pageable) {

        OrderSpecifier order = post.createdAt.desc();

        List<Post> posts = query
                .select(post).distinct()
                .from(post)
                .leftJoin(post.comments, comment).fetchJoin()
                .leftJoin(comment.member, member).fetchJoin()
                .leftJoin(post.member, member).fetchJoin()
                .leftJoin(post.group, group).fetchJoin()
                .leftJoin(post.postStudies, postStudy)
                .where(memberIdEq(memberId),
                        groupIdEq(groupId),
                        searchContains(search),
                        categoryEq(category),
                        studyIdIn(studyIds),
                        post.deleted.isFalse())
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .orderBy(order)
                .fetch();

        Long count = query
                .select(post.countDistinct())
                .from(post)
                .leftJoin(post.postStudies, postStudy)
                .where(memberIdEq(memberId),
                        groupIdEq(groupId),
                        searchContains(search),
                        categoryEq(category),
                        studyIdIn(studyIds),
                        post.deleted.isFalse())
                .fetchOne();


        return new PageImpl<>(posts, pageable, count);
    }

    private BooleanExpression memberIdEq(Long memberId) {
        return memberId != null ? post.member.id.eq(memberId) : null;
    }

    private BooleanExpression studyIdIn(List<Long> studyIds) {
        return studyIds != null && !studyIds.isEmpty() ? postStudy.study.id.in(studyIds) : null;
    }

    private BooleanExpression categoryEq(PostCategory category) {
        return category != null ? post.category.eq(category) : null;
    }

    private BooleanExpression searchContains(String search) {
        return search != null && !search.isBlank() ? post.title.contains(search) : null;
    }

    private BooleanExpression groupIdEq(Long groupId) {
        return groupId != null ? post.group.id.eq(groupId) : null;
    }
}
