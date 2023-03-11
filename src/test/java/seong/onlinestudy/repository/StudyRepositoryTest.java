package seong.onlinestudy.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import seong.onlinestudy.domain.*;
import seong.onlinestudy.dto.StudyDto;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static seong.onlinestudy.MyUtils.*;
import static seong.onlinestudy.domain.TicketStatus.STUDY;

@DataJpaTest
class StudyRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    StudyRepository studyRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    PostRepository postRepository;
    @Autowired
    TicketRepository ticketRepository;
    @Autowired
    GroupRepository groupRepository;

    @Test
    void findStudiesWithMember() {
        //given
        List<Member> members = createMembers(10);
        memberRepository.saveAll(members);

        List<Study> studies = createStudies(6);
        studyRepository.saveAll(studies);

        Group group = createGroup("group", 30, members.get(0));
        groupRepository.save(group);

        Ticket ticket = createTicket(STUDY, members.get(0), studies.get(4), group);
        ticketRepository.save(ticket);
        setField(ticket, "activeTime", 3600);
        Ticket ticket1 = createTicket(STUDY, members.get(0), studies.get(5), group);
        ticketRepository.save(ticket1);

        List<Ticket> tickets = new ArrayList<>();
        for(int i=1; i<members.size(); i++) {
            GroupMember.createGroupMember(members.get(i), GroupRole.USER);
            tickets.add(createTicket(STUDY, members.get(i), studies.get(i%4), group));
        }
        ticketRepository.saveAll(tickets);


        //when
        LocalDateTime now = LocalDateTime.now();

        List<Study> findStudies = em.createQuery("select s from Study s" +
                        " join s.tickets t on t.member = :member and t.startTime >= :startTime and t.startTime < :endTime" +
                        " group by s.id" +
                        " order by sum(t.activeTime) desc", Study.class)
                .setParameter("member", members.get(0))
                .setParameter("startTime", now.minusDays(3))
                .setParameter("endTime", now.plusDays(3))
                .getResultList();

/*        Page<Study> studiesByMember
                = studyRepository.findStudiesByMember(members.get(0), now.minusDays(3), now.plusDays(3), PageRequest.of(0, 2));
        List<Study> findStudies = studiesByMember.getContent();*/

        //then
        assertThat(findStudies).containsExactlyElementsOf(List.of(studies.get(4), studies.get(5)));
    }

    @Test
    void findStudiesWithMember_티켓페치조인() {
        //given
        List<Member> members = createMembers(10);
        memberRepository.saveAll(members);

        List<Study> studies = createStudies(6);
        studyRepository.saveAll(studies);

        Group group = createGroup("group", 30, members.get(0));
        groupRepository.save(group);

        Ticket ticket = createTicket(STUDY, members.get(0), studies.get(4), group);
        ticketRepository.save(ticket);
        setTicketEnd(ticket, 3600);

        Ticket ticket1 = createTicket(STUDY, members.get(0), studies.get(5), group);
        ticketRepository.save(ticket1);

        Ticket ticket2 = createTicket(STUDY, members.get(0), studies.get(4), group);
        ticketRepository.save(ticket2);
        setTicketEnd(ticket2, 2800);

        List<Ticket> tickets = new ArrayList<>();
        for(int i=1; i<members.size(); i++) {
            GroupMember.createGroupMember(members.get(i), GroupRole.USER);
            tickets.add(createTicket(STUDY, members.get(i), studies.get(i%4), group));
        }
        ticketRepository.saveAll(tickets);


        //when
        LocalDateTime now = LocalDateTime.now();

/*        List<Study> findStudies = em.createQuery("select distinct s from Study s" +
                        " join fetch s.tickets t " +
                        " where t.member = :member and t.startTime >= :startTime and t.startTime < :endTime" +
                        " order by t.startTime desc", Study.class)
                .setParameter("member", members.get(0))
                .setParameter("startTime", now.minusDays(3))
                .setParameter("endTime", now.plusDays(3))
                .getResultList();*/

        Page<Study> studiesByMember
                = studyRepository.findStudiesByMember(members.get(0).getId(), now.minusDays(3), now.plusDays(3), PageRequest.of(0, 2));
        List<Study> findStudies = studiesByMember.getContent();

        //then
        assertThat(findStudies).containsExactlyElementsOf(List.of(studies.get(4), studies.get(5)));
        List<StudyDto> studyDtos = findStudies.stream().map(study -> {
            StudyDto studyDto = StudyDto.from(study);
            studyDto.setStudyTime(study);

            return studyDto;
        }).collect(Collectors.toList());

        //티켓이 만료되어 활성화된 시간이 0보다 큰 경우
        assertThat(studyDtos).anySatisfy(studyDto -> {
            assertThat(studyDto.getStudyTime()).isEqualTo(3600 + 2800);
            assertThat(studyDto.getEndTime()).isEqualTo(ticket.getEndTime().format(DateTimeFormatter.ISO_DATE_TIME));
        });
        //티켓이 만료되지 않아 활성화된 시간이 0인 경우
        assertThat(studyDtos).anySatisfy(studyDto -> {
            assertThat(studyDto.getStudyTime()).isEqualTo(0);
            assertThat(studyDto.getEndTime()).isEqualTo(ticket1.getStartTime().format(DateTimeFormatter.ISO_DATE_TIME));
        });
    }

}