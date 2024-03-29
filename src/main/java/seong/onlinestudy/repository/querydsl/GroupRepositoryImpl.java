package seong.onlinestudy.repository.querydsl;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import seong.onlinestudy.domain.*;
import seong.onlinestudy.dto.GroupDto;
import seong.onlinestudy.enumtype.GroupCategory;
import seong.onlinestudy.enumtype.OrderBy;

import javax.persistence.EntityManager;
import java.util.List;

import static seong.onlinestudy.domain.QGroup.*;
import static seong.onlinestudy.domain.QGroupMember.groupMember;
import static seong.onlinestudy.domain.QStudyTicket.studyTicket;
import static seong.onlinestudy.domain.QTicket.ticket;

public class GroupRepositoryImpl implements GroupRepositoryCustom{

    private final JPAQueryFactory query;

    public GroupRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public Page<Group> findGroups(Long memberId, GroupCategory category, String search, List<Long> studyIds, OrderBy orderBy, Pageable pageable) {

        OrderSpecifier order;
        switch (orderBy) {
            case MEMBERS:
                order = groupMember.count().desc();
                break;
            case TIME:
                order = ticket.ticketRecord.activeTime.sum().desc();
                break;
            default:
                order = group.createdAt.desc();
        }

        List<Group> groups = query
                .selectFrom(group)
                .join(group.groupMembers, groupMember)
                .leftJoin(group.tickets, ticket)
                .leftJoin(studyTicket).on(studyTicket.eq(ticket))
                .where(memberIdEq(groupMember.member, memberId),
                        categoryEq(category),
                        nameContains(search),
                        studyIdsIn(studyTicket.study, studyIds),
                        group.deleted.isFalse())
                .groupBy(group.id)
                .orderBy(order)
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetch();

        Long total = query
                .select(group.countDistinct())
                .from(group)
                .join(group.groupMembers, groupMember)
                .leftJoin(group.tickets, ticket)
                .leftJoin(studyTicket).on(studyTicket.eq(ticket))
                .where(memberIdEq(groupMember.member, memberId),
                        categoryEq(category),
                        nameContains(search),
                        studyIdsIn(studyTicket.study, studyIds),
                        group.deleted.isFalse())
                .fetchOne();

        return new PageImpl<>(groups, pageable, total);
    }

    @Override
    public Page<GroupDto> findGroupsAndMapToGroupDto(Long memberId, GroupCategory category, String search,
                                                     List<Long> studyIds, OrderBy orderBy, Pageable pageable) {
        OrderSpecifier order;
        switch (orderBy) {
            case MEMBERS:
                order = groupMember.count().desc();
                break;
            case TIME:
                order = ticket.ticketRecord.activeTime.sum().desc();
                break;
            default:
                order = group.createdAt.desc();
        }

        List<GroupDto> groupDtos = query
                .select(Projections.constructor(GroupDto.class,
                        group.id.as("groupId"),
                        group.name,
                        group.headcount,
                       ExpressionUtils.as(
                       JPAExpressions.select(groupMember.countDistinct())
                                .from(groupMember)
                                .where(groupMember.group.eq(group)),
                               "memberSize"
                       ),
                        group.deleted,
                        group.createdAt,
                        group.description,
                        group.category))
                .from(group)
                .join(group.groupMembers, groupMember)
                .leftJoin(studyTicket).on(studyTicket.group.eq(group))
                .where(memberIdEq(groupMember.member, memberId),
                        categoryEq(category),
                        nameContains(search),
                        studyIdsIn(studyTicket.study, studyIds),
                        group.deleted.isFalse())
                .groupBy(group.id)
                .orderBy(order)
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetch();

        Long total = query
                .select(group.countDistinct())
                .from(group)
                .join(group.groupMembers, groupMember)
                .leftJoin(studyTicket).on(studyTicket.group.eq(group))
                .where(memberIdEq(groupMember.member, memberId),
                        categoryEq(category),
                        nameContains(search),
                        studyIdsIn(studyTicket.study, studyIds),
                        group.deleted.isFalse())
                .fetchOne();

        return new PageImpl<>(groupDtos, pageable, total);
    }

    private BooleanExpression memberIdEq(QMember member, Long memberId) {
        return memberId != null ? member.id.eq(memberId) : null;
    }

    private BooleanExpression studyIdsIn(QStudy study, List<Long> studyIds) {
        return studyIds != null && !studyIds.isEmpty() ? study.id.in(studyIds) : null;
    }

    private BooleanExpression categoryEq(GroupCategory category) {
        return category != null ? group.category.eq(category) : null;
    }

    private BooleanExpression nameContains(String search) {
        return search != null ? group.name.contains(search) : null;
    }


}
