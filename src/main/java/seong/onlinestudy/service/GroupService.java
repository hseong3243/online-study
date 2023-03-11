package seong.onlinestudy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seong.onlinestudy.domain.*;
import seong.onlinestudy.dto.GroupDto;
import seong.onlinestudy.dto.GroupMemberDto;
import seong.onlinestudy.dto.GroupStudyDto;
import seong.onlinestudy.repository.GroupMemberRepository;
import seong.onlinestudy.repository.GroupRepository;
import seong.onlinestudy.repository.MemberRepository;
import seong.onlinestudy.repository.StudyRepository;
import seong.onlinestudy.exception.PermissionControlException;
import seong.onlinestudy.request.GroupCreateRequest;
import seong.onlinestudy.request.GroupsGetRequest;

import java.util.*;
import java.util.stream.Collectors;
import java.util.NoSuchElementException;

import static seong.onlinestudy.domain.GroupRole.*;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final StudyRepository studyRepository;
    private final MemberRepository memberRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Transactional
    public Long createGroup(GroupCreateRequest createRequest, Member member) {
        Member findMember = memberRepository.findById(member.getId())
                .orElseThrow(() -> new NoSuchElementException("잘못된 접근입니다."));

        GroupMember groupMember = GroupMember.createGroupMember(findMember, MASTER);
        Group group = Group.createGroup(createRequest, groupMember);

        groupRepository.save(group);
        log.info("그룹이 생성되었습니다. group={}", group);

        return group.getId();
    }

    @Transactional
    public Long joinGroup(Long groupId, Member member) {
        Member findMember = memberRepository.findById(member.getId())
                .orElseThrow(() -> new NoSuchElementException("잘못된 접근입니다."));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 그룹입니다."));

        GroupMember groupMember = GroupMember.createGroupMember(findMember, USER);
        group.addGroupMember(groupMember);
        log.info("멤버 {}가 그룹 {}에 가입하였습니다.", findMember, group);

        return group.getId();
    }

    public Page<GroupDto> getGroups(GroupsGetRequest request) {
        Page<Group> groups = groupRepository.findGroups(PageRequest.of(request.getPage(), request.getSize()),
                request.getCategory(), request.getSearch(), request.getStudyIds(), request.getOrderBy());

        List<GroupStudyDto> groupStudies = studyRepository.findStudiesInGroups(groups.getContent());

        List<GroupMember> groupMasters = groupMemberRepository.findGroupMasters(groups.getContent());
        List<GroupMemberDto> groupMemberDtos = groupMasters.stream()
                .map(GroupMemberDto::from).collect(Collectors.toList());

        Page<GroupDto> groupDtos = groups.map(group -> {
            GroupDto groupDto = GroupDto.from(group);
            groupDto.setMemberSize(group.getGroupMembers().size());

            Iterator<GroupStudyDto> studyIter = groupStudies.iterator();
            while(studyIter.hasNext()) {
                GroupStudyDto study = studyIter.next();
                if(study.getGroupId().equals(group.getId())) {
                    groupDto.getStudies().add(study);
                    studyIter.remove();
                }
            }

            Iterator<GroupMemberDto> memberIter = groupMemberDtos.iterator();
            while(memberIter.hasNext()) {
                GroupMemberDto member = memberIter.next();
                if (member.getGroupId().equals(group.getId())) {
                    groupDto.getGroupMembers().add(member);
                    memberIter.remove();
                }
            }

            return groupDto;
        });

        return groupDtos;
    }

    public GroupDto getGroup(Long id) {
        Group group = groupRepository.findGroupWithMembers(id)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 그룹입니다."));

        GroupDto groupDto = GroupDto.from(group);
        List<GroupMemberDto> groupMemberDtos = group.getGroupMembers().stream()
                .map(GroupMemberDto::from).collect(Collectors.toList());
        groupDto.setGroupMembers(groupMemberDtos);

        return groupDto;
    }

    @Transactional
    public void deleteGroup(Long id, Member loginMember) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 그룹입니다."));

        GroupMember master = group.getGroupMembers().stream().filter(groupMember ->
                groupMember.getRole().equals(MASTER)).findFirst().get();
        if(!master.getMember().getId().equals(loginMember.getId())) {
            throw new PermissionControlException("권한이 없습니다.");
        }

        groupRepository.delete(group);
    }

    @Transactional
    public void quitGroup(Long groupId, Member loginMember) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 그룹입니다."));

        Member member = memberRepository.findById(loginMember.getId())
                .orElseThrow(() -> new NoSuchElementException("잘못된 접근입니다."));

        groupMemberRepository.deleteByMember(member);
    }
}
