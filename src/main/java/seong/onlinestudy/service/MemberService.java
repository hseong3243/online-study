package seong.onlinestudy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import seong.onlinestudy.domain.Member;
import seong.onlinestudy.dto.MemberDto;
import seong.onlinestudy.exception.DuplicateElementException;
import seong.onlinestudy.repository.*;
import seong.onlinestudy.request.member.MemberCreateRequest;
import seong.onlinestudy.request.member.MemberDuplicateCheckRequest;
import seong.onlinestudy.request.member.MemberUpdateRequest;

import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long createMember(MemberCreateRequest request) {
        memberRepository.findByUsername(request.getUsername())
                .ifPresent(member -> {
                    throw new DuplicateElementException("이미 존재하는 아이디입니다.");
                });

        request.passwordCheck();

        Member member = Member.createMember(request);
        passwordToEncoded(member);

        memberRepository.save(member);

        return member.getId();
    }

    private void passwordToEncoded(Member member) {
        String encodedPassword = passwordEncoder.encode(member.getPassword());
        member.updatePassword(encodedPassword);
    }

    @Transactional
    public Long updateMember(Long memberId, MemberUpdateRequest request) {
        Member findMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원입니다."));

        updatePassword(request, findMember);
        findMember.update(request);

        return findMember.getId();
    }

    private void updatePassword(MemberUpdateRequest request, Member findMember) {
        if(StringUtils.hasText(request.getPasswordNew())) {
            request.passwordCheck();
            memberPasswordCheck(request.getPasswordOld(), findMember.getPassword());

            String encodedPassword = passwordEncoder.encode(request.getPasswordNew());
            findMember.updatePassword(encodedPassword);
        }
    }

    private void memberPasswordCheck(String password, String encodedPassword) {
        if(!passwordEncoder.matches(password, encodedPassword)) {
            throw new IllegalArgumentException("패스워드가 일치하지 않습니다.");
        }
    }

    public void duplicateCheck(MemberDuplicateCheckRequest request) {
        memberRepository.findByUsername(request.getUsername())
                .ifPresent(member -> {
                    throw new DuplicateElementException("이미 존재하는 아이디입니다.");
                });
    }

    public MemberDto getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원입니다."));

        return MemberDto.from(member);
    }

    @Transactional
    public void deleteMember(Long memberId) {
        Member findMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 회웝입니다."));

        //티켓과 기록은 남겨둠
        //그룹장인 그룹은 삭제, 그룹원인 그룹은 탈퇴, 게시글 삭제, 댓글 삭제
        groupMemberRepository.deleteAllByMemberIdRoleIsNotMaster(memberId);
        groupRepository.softDeleteAllByMemberIdRoleIsMaster(memberId);
        postRepository.softDeleteAllByMemberId(memberId);
        commentRepository.softDeleteAllByMemberId(memberId);

        findMember.delete();
    }
}
