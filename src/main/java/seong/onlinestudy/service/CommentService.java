package seong.onlinestudy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seong.onlinestudy.domain.Comment;
import seong.onlinestudy.domain.Member;
import seong.onlinestudy.domain.Post;
import seong.onlinestudy.dto.CommentDto;
import seong.onlinestudy.exception.PermissionControlException;
import seong.onlinestudy.repository.CommentRepository;
import seong.onlinestudy.repository.MemberRepository;
import seong.onlinestudy.repository.PostRepository;
import seong.onlinestudy.request.comment.CommentsGetRequest;
import seong.onlinestudy.request.comment.CommentCreateRequest;
import seong.onlinestudy.request.comment.CommentUpdateRequest;
import seong.onlinestudy.request.comment.CommentsDeleteRequest;

import java.util.NoSuchElementException;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentService {

    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Transactional
    public Long createComment(CommentCreateRequest request, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("잘못된 접근입니다."));

        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 게시글입니다."));

        Comment comment = Comment.create(request);
        comment.setMemberAndPost(member, post);
        log.info("댓글이 작성되었습니다. postId={}, commentId={}, memberId={}",
                post.getId(), comment.getId(), member.getId());

        return comment.getId();
    }

    @Transactional
    public Long updateComment(Long commentId, CommentUpdateRequest request, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 댓글입니다."));

        //작성자 정보가 같지 않으면
        if (!comment.getMember().getId().equals(memberId)) {
            throw new PermissionControlException("댓글 수정 권한이 없습니다.");
        }

        String oldContent = comment.getContent();

        comment.update(request);
        log.info("댓글이 수정되었습니다. commentId={}, oldContent={}, newContent={}",
                comment.getId(), oldContent, comment.getContent());

        return comment.getId();
    }

    @Transactional
    public Long deleteComment(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 댓글입니다."));

        if(!comment.getMember().getId().equals(memberId)) {
            throw new PermissionControlException("댓글 삭제 권한이 없습니다.");
        }

        comment.delete();

        return comment.getId();
    }

    public Page<CommentDto> getComments(CommentsGetRequest request) {
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());
        Page<Comment> commentsWithPage =
                commentRepository.findComments(request.getMemberId(), request.getPostId(), pageRequest);

        return commentsWithPage.map(CommentDto::from);
    }

    @Transactional
    public void deleteComments(CommentsDeleteRequest request, Long memberId) {
        if(!request.getMemberId().equals(memberId)) {
            throw new PermissionControlException("권한이 없습니다.");
        }

        commentRepository.softDeleteAllByMemberId(request.getMemberId());
    }
}
