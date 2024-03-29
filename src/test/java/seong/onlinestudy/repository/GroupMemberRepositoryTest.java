package seong.onlinestudy.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import seong.onlinestudy.MyUtils;
import seong.onlinestudy.domain.Group;
import seong.onlinestudy.domain.GroupMember;
import seong.onlinestudy.dto.GroupMemberDto;
import seong.onlinestudy.enumtype.GroupRole;
import seong.onlinestudy.domain.Member;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static seong.onlinestudy.MyUtils.*;

@Slf4j
@DataJpaTest
class GroupMemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    GroupMemberRepository groupMemberRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    GroupRepository groupRepository;

    @Test
    void findGroupMasters() {
        //given
        List<Member> masters = createMembers(10);
        List<Member> members = createMembers(10, 30);
        memberRepository.saveAll(masters);
        memberRepository.saveAll(members);

        List<Group> groups = createGroups(masters, 10);
        groupRepository.saveAll(groups);

        List<GroupMember> groupMembers = new ArrayList<>();
        for (Member member : members) {
            groupMembers.add(GroupMember.createGroupMember(member, GroupRole.USER));
        }
        for(int i=0; i<members.size(); i++) {
            groups.get(i%groups.size()).addGroupMember(groupMembers.get(i));
        }

        //when
        List<GroupMember> result = groupMemberRepository.findGroupMasters(groups);

        //then
        assertThat(result.size()).isEqualTo(masters.size());
    }

    @Test
    void deleteAllByMemberId() {
        //given
        List<Member> members = createMembers(10);
        List<Group> groups = createGroups(members, 5);

        Member testMember = createMember("member", "member");
        MyUtils.joinMembersToGroups(List.of(testMember), groups);

        memberRepository.saveAll(members);
        memberRepository.save(testMember);

        groupRepository.saveAll(groups);

        //when
        groupMemberRepository.deleteAllByMemberIdRoleIsNotMaster(testMember.getId());
        em.flush();
        em.clear();

        //then
        List<Group> findGroups = groupRepository.findAll();
        assertThat(findGroups.size()).isEqualTo(5);

        assertThat(findGroups).allSatisfy(findGroup -> {
            List<Long> findMemberIds = findGroup.getGroupMembers().stream()
                    .map(groupMember -> groupMember.getMember().getId())
                    .collect(Collectors.toList());

            assertThat(findMemberIds).doesNotContain(testMember.getId());
        });
    }

    @Test
    public void findGroupMastersInGroupIds() {
        //given
        List<Member> members = createMembers(10);
        List<Group> groups = createGroups(members, 2);
        for(int i=2; i<6; i++) {
            GroupMember groupMember = GroupMember.createGroupMember(members.get(i), GroupRole.USER);
            groups.get(0).addGroupMember(groupMember);
        }
        for(int i=6; i<10; i++) {
            GroupMember groupMember = GroupMember.createGroupMember(members.get(i), GroupRole.USER);
            groups.get(1).addGroupMember(groupMember);
        }
        memberRepository.saveAll(members);
        groupRepository.saveAll(groups);

        //when
        List<Long> groupIds = groups.stream().map(Group::getId).collect(Collectors.toList());
        List<GroupMemberDto> groupMasters = groupMemberRepository.findGroupMastersInGroupIds(groupIds);

        //then
        List<Long> masterMemberIds = groupMasters.stream()
                .map(GroupMemberDto::getMemberId)
                .collect(Collectors.toList());
        List<Long> testMemberIds = members.subList(0, 2).stream()
                .map(Member::getId)
                .collect(Collectors.toList());
        assertThat(masterMemberIds).containsExactlyInAnyOrderElementsOf(testMemberIds);

    }
}