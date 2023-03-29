package seong.onlinestudy.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import seong.onlinestudy.MyUtils;
import seong.onlinestudy.domain.*;
import seong.onlinestudy.request.*;

import javax.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static seong.onlinestudy.MyUtils.*;
import static seong.onlinestudy.domain.QGroup.group;
import static seong.onlinestudy.domain.QGroupMember.groupMember;
import static seong.onlinestudy.domain.QRecord.record;
import static seong.onlinestudy.domain.QStudy.study;
import static seong.onlinestudy.domain.QStudyTicket.studyTicket;
import static seong.onlinestudy.domain.QTicket.ticket;

@Slf4j
@DataJpaTest
class GroupRepositoryCustomTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory query;

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    GroupRepository groupRepository;
    @Autowired
    StudyRepository studyRepository;
    @Autowired
    TicketRepository ticketRepository;

    List<Member> members;
    List<Group> groups;
    List<Study> studies;
    List<Ticket> tickets;

    @BeforeEach
    void init() {
        query = new JPAQueryFactory(em);

        members = MyUtils.createMembers(50);
        memberRepository.saveAll(members);

        groups = MyUtils.createGroups(members, 5);
        groupRepository.saveAll(groups);

        MyUtils.joinMembersToGroups(members, groups);

        studies = MyUtils.createStudies(5);
        studyRepository.saveAll(studies);

        Random random = new Random();

        tickets = new ArrayList<>();
        for(int i=0; i<200; i++) {
            Ticket studyTicket = createStudyTicket(
                            members.get(random.nextInt(members.size())),
                            groups.get(random.nextInt(groups.size())),
                            studies.get(random.nextInt(studies.size())));
            tickets.add(studyTicket);
        }
        ticketRepository.saveAll(tickets);
    }

    @Test
    void findGroups_조건없음() throws InterruptedException {
        //given
        GroupCategory category = null;
        String search = null;
        List<Long> studyIds = null;

        OrderBy orderBy = OrderBy.TIME;

        //when
        PageRequest pageRequest = PageRequest.of(0, 5);

        Page<Group> findGroupsWithPage = groupRepository.findGroups(
                pageRequest, category, search, studyIds, orderBy);

        List<Group> findGroups = findGroupsWithPage.getContent();

        //then
        if(orderBy != OrderBy.MEMBERS && orderBy != OrderBy.TIME) {
            this.groups.sort((o1, o2) -> (int) (o2.getId() - o1.getId()));

            assertThat(findGroups).containsAll(this.groups);
            assertThat(findGroups).containsExactlyElementsOf(this.groups);
        } else if(orderBy == OrderBy.MEMBERS) {
            assertThat(findGroups.get(0).getGroupMembers().size()).isEqualTo(2);
            assertThat(findGroups.get(findGroups.size()-1).getGroupMembers().size()).isEqualTo(1);
        } else if(orderBy == OrderBy.TIME){
            List<Long> studyTimes = new ArrayList<>();
            for (Group group : findGroups) {
                long studyTime = 0L;
                for (Ticket groupTicket : group.getTickets()) {
                    studyTime += groupTicket.getRecord().getActiveTime();
                }

                studyTimes.add(studyTime);
            }

            long studyTimeForCheck = studyTimes.get(0);
            for (Long studyTime : studyTimes) {
                assertThat(studyTime).isLessThanOrEqualTo(studyTimeForCheck);
                studyTimeForCheck = studyTime;
            }
        }
    }

    @Test
    void findGroups_스터디조건() {
        //given
        Study testStudy = studies.get(0);
        List<Ticket> filteredTickets = getFilteredTickets(testStudy);
        List<Group> testGroups = filteredTickets.stream().map(Ticket::getGroup).distinct().collect(Collectors.toList());

        //when
        PageRequest pageRequest = PageRequest.of(0, 5);
        Page<Group> findGroupsWithPage = groupRepository
                .findGroups(pageRequest, null, null, List.of(testStudy.getId()), OrderBy.CREATEDAT);
        List<Group> findGroups = findGroupsWithPage.getContent();

        //then
        log.info("findGroups.size()={}", findGroups.size());
        assertThat(findGroups).containsExactlyInAnyOrderElementsOf(testGroups);
    }

    private List<Ticket> getFilteredTickets(Study testStudy) {
        List<Ticket> filteredTickets = tickets.stream().filter(ticket -> {
            if(ticket instanceof StudyTicket) {
                StudyTicket studyTicket = (StudyTicket) ticket;

                return studyTicket.getStudy().equals(testStudy);
            } else {
                return false;
            }
        }).collect(Collectors.toList());
        return filteredTickets;
    }

    private BooleanExpression categoryEq(GroupCategory category) {
        return category != null ? group.category.eq(category) : null;
    }

    private BooleanExpression nameContains(String search) {
        return search != null ? group.name.contains(search) : null;
    }

    private BooleanExpression studyIdsIn(QStudy studyTicket, List<Long> studyIds) {
        return studyIds != null && studyIds.size() > 0 ? studyTicket.id.in(studyIds) : null;
    }
}