package seong.onlinestudy.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import seong.onlinestudy.MyUtils;
import seong.onlinestudy.domain.*;
import seong.onlinestudy.dto.GroupDto;
import seong.onlinestudy.dto.GroupStudyDto;
import seong.onlinestudy.exception.PermissionControlException;
import seong.onlinestudy.repository.GroupMemberRepository;
import seong.onlinestudy.repository.GroupRepository;
import seong.onlinestudy.repository.StudyRepository;
import seong.onlinestudy.request.GroupCreateRequest;
import seong.onlinestudy.request.GroupsGetRequest;
import seong.onlinestudy.request.MemberCreateRequest;
import seong.onlinestudy.request.OrderBy;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@Slf4j
@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    GroupRepository groupRepository;
    @Mock
    StudyRepository studyRepository;
    @Mock
    GroupMemberRepository groupMemberRepository;

    @InjectMocks
    GroupService groupService;


    @Test
    void joinGroup() {
        //given
        Member master = createMember("memberA", "test1234");
        Member memberA = createMember("memberB", "test1234");

        GroupMember groupMember = GroupMember.createGroupMember(master, GroupRole.MASTER);
        Group group = createGroup("test", 30, groupMember);

        given(groupRepository.findById(1L)).willReturn(Optional.of(group));

        //when
        Long groupId = groupService.joinGroup(1L, memberA);

        //then
        Group findGroup = groupRepository.findById(1L).get();
        assertThat(findGroup).isEqualTo(group);
        assertThat(findGroup.getGroupMembers().size()).isEqualTo(2);
    }

    @Test
    @DisplayName("그룹 생성")
    void createGroup() {
        //given
        Member member = createMember("memberA", "test1234");
        GroupCreateRequest request = getGroupCreateRequest("groupA", 30);

        //when
        groupService.createGroup(request, member);

        //then
    }

    @Test
    @DisplayName("그룹 삭제")
    void deleteGroup() {
        //given
        Member master = createMember("memberA", "test1234");
        Member memberA = createMember("memberB", "test1234");

        GroupMember groupMember = GroupMember.createGroupMember(master, GroupRole.MASTER);
        GroupMember groupMember2 = GroupMember.createGroupMember(memberA, GroupRole.USER);

        Group group = createGroup("test", 30, groupMember);
        group.addGroupMember(groupMember2);
        ReflectionTestUtils.setField(master, "id", 1L);
        ReflectionTestUtils.setField(memberA, "id", 2L);


        given(groupRepository.findById(1L)).willReturn(Optional.of(group));

        //when
        Long groupId = 1L;
        groupService.deleteGroup(groupId, master);

        //then
    }

    @Test
    @DisplayName("그룹 삭제_권한 없음")
    void deleteGroup_권한없음() {
        //given
        Member master = createMember("memberA", "test1234");
        Member memberA = createMember("memberB", "test1234");

        GroupMember groupMember = GroupMember.createGroupMember(master, GroupRole.MASTER);
        GroupMember groupMember2 = GroupMember.createGroupMember(memberA, GroupRole.USER);

        Group group = createGroup("test", 30, groupMember);
        group.addGroupMember(groupMember2);
        ReflectionTestUtils.setField(master, "id", 1L);
        ReflectionTestUtils.setField(memberA, "id", 2L);

        given(groupRepository.findById(1L)).willReturn(Optional.of(group));

        //when
        Long groupId = 1L;

        //then
        assertThatThrownBy(() -> groupService.deleteGroup(groupId, memberA))
                .isInstanceOf(PermissionControlException.class);
    }

    @Test
    void 그룹조회() {
        //given
        GroupsGetRequest request = new GroupsGetRequest();
        request.setPage(0); request.setSize(10);

        Member member = MyUtils.createMember("testMember", "testMember");
        Group group1 = MyUtils.createGroup("테스트그룹", 30, member);
        Group group2 = MyUtils.createGroup("테스트그룹2", 30, member);
        ReflectionTestUtils.setField(group1, "id", 1L);
        ReflectionTestUtils.setField(group2, "id", 2L);

        GroupStudyDto groupStudyDto1 = new GroupStudyDto(1L, 1L, "테스트스터디", 1000);
        GroupStudyDto groupStudyDto2 = new GroupStudyDto(2L, 2L, "테스트스터디2", 2000);

        GroupMember groupMember = GroupMember.createGroupMember(member, GroupRole.MASTER);
        group1.addGroupMember(groupMember);
        group2.addGroupMember(groupMember);

        PageImpl<Group> testGroups = new PageImpl<>(List.of(group1, group2), PageRequest.of(request.getPage(), request.getSize()), 2);

        given(groupRepository.findGroups(any(), any(), any(), any(), any()))
                .willReturn(testGroups);
        given(studyRepository.findStudiesInGroups(testGroups.getContent()))
                .willReturn(List.of(groupStudyDto1, groupStudyDto2));
        given(groupMemberRepository.findGroupMasters(any())).willReturn(List.of(groupMember, groupMember));


        //when
        Page<GroupDto> groups = groupService.getGroups(request);

        //then
        List<GroupDto> groupDtos = groups.getContent();
        assertThat(groupDtos).anySatisfy(group -> {
            assertThat(group.getName()).isEqualTo("테스트그룹");

            assertThat(group.getStudies()).anySatisfy(study -> {
                assertThat(study.getName()).isEqualTo("테스트스터디");
            });
        });
        assertThat(groupDtos.get(0).getStudies().get(0).getName()).isEqualTo("테스트스터디1");
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