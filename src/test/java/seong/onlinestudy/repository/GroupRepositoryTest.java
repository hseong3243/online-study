package seong.onlinestudy.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import seong.onlinestudy.MyUtils;
import seong.onlinestudy.domain.*;
import seong.onlinestudy.request.GroupCreateRequest;
import seong.onlinestudy.request.MemberCreateRequest;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static seong.onlinestudy.domain.GroupRole.*;
import static seong.onlinestudy.domain.QGroup.group;

@Slf4j
@DataJpaTest
class GroupRepositoryTest {

    @Autowired
    GroupRepository groupRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    EntityManager em;

    @TestConfiguration
    static class TestConfig {

        @PersistenceContext
        EntityManager em;

        @Bean
        JPAQueryFactory jpaQueryFactory() {
            return new JPAQueryFactory(em);
        }
    }

    @BeforeEach
    void init() {

    }

    @Test
    @DisplayName("그룹 생성")
    void createGroup() {
        //given
        Member member = createMember("memberTest", "test1234");
        GroupMember groupMember = GroupMember.createGroupMember(member, MASTER);
        Group group = createGroup("groupTest", 20, groupMember);

        //when
        groupRepository.save(group);

        //then
        Group findGroup = groupRepository.findById(group.getId()).get();
        assertThat(findGroup.getName()).isEqualTo("groupTest");
        assertThat(findGroup.getGroupMembers().size()).isEqualTo(1);
    }

    @Test
    void 그룹가입() {
        //given
        Member memberA = createMember("memberA", "test1234");
        GroupMember groupMemberA = GroupMember.createGroupMember(memberA, MASTER);
        Group groupA = createGroup("test", 30, groupMemberA);
        groupRepository.save(groupA);

        Member memberB = createMember("memberB", "test1234");
        GroupMember groupMemberB = GroupMember.createGroupMember(memberB, USER);

        //when
        groupA.addGroupMember(groupMemberB);

        //then
        Group findGroup = groupRepository.findById(groupA.getId()).get();
        assertThat(findGroup.getName()).isEqualTo("test");
        assertThat(findGroup.getGroupMembers().size()).isEqualTo(2);

    }

    @Test
    void getGroupMember() {
        //given


        //when
        Group group = groupRepository.findAll().get(0);

        //then
        log.info("groupMembers={}",group.getGroupMembers());
        List<GroupMember> groupMembers = group.getGroupMembers();
        assertThat(groupMembers.size()).isEqualTo(2);
    }

    @Test
    void getGroups() {
        //given
        List<Member> members = MyUtils.createMembers(50);
        memberRepository.saveAll(members);

        List<Group> groups = new ArrayList<>();
        for(int i=0; i<20; i++) {
            groups.add(MyUtils.createGroup("테스트그룹" + 1, 30, members.get(i)));
        }
        groupRepository.saveAll(groups);

        PageRequest request = PageRequest.of(0, 10);

        //when
        Page<Group> findGroups = groupRepository.findGroups(request, null, null, null, null);

        //then
        Group[] groupArr = new Group[10];
        for(int i=0; i<10; i++) {
            groupArr[i] = groups.get(i);
        }

        assertThat(findGroups.getContent()).containsExactly(groupArr);
        assertThat(findGroups.getSize()).isEqualTo(10);
    }


    private BooleanExpression categoryEq(GroupCategory category) {
        return category != null ? group.category.eq(category) : null;
    }

    private BooleanExpression nameContains(String search) {
        return search != null ? group.name.contains(search) : null;
    }

    private Group createGroup(String name, int count, GroupMember groupMember) {
        GroupCreateRequest groupRequest = getGroupCreateRequest(name, count);
        return Group.createGroup(groupRequest, groupMember);
    }


    private Member createMember(String username, String password) {
        MemberCreateRequest memberRequest;
        memberRequest = getMemberCreateRequest(username, password);
        return Member.createMember(memberRequest);
    }

    private GroupCreateRequest getGroupCreateRequest(String name, int count) {
        GroupCreateRequest groupRequest = new GroupCreateRequest();
        groupRequest.setName(name);
        groupRequest.setHeadcount(count);
        return groupRequest;
    }

    private MemberCreateRequest getMemberCreateRequest(String username, String password) {
        MemberCreateRequest memberRequest = new MemberCreateRequest();
        memberRequest.setUsername(username);
        memberRequest.setPassword(password);
        memberRequest.setNickname(username);
        return memberRequest;
    }

}