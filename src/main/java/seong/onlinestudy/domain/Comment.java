package seong.onlinestudy.domain;

import lombok.Getter;
import org.hibernate.annotations.Where;
import seong.onlinestudy.request.comment.CommentCreateRequest;
import seong.onlinestudy.request.comment.CommentUpdateRequest;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Lob
    private String content;
    private boolean deleted;
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    public void setMemberAndPost(Member member, Post post) {
        this.post = post;
        post.getComments().add(this);

        this.member = member;
        member.getComments().add(this);
    }

    public void update(CommentUpdateRequest request) {
        this.content = request.getContent();
    }

    public void delete() {
        this.deleted = true;
    }

    public static Comment create(CommentCreateRequest request) {
        Comment comment = new Comment();
        comment.content = request.getContent();
        comment.deleted = false;
        comment.createdAt = LocalDateTime.now();

        return comment;
    }
}
