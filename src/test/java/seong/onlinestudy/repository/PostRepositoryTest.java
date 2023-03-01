package seong.onlinestudy.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import seong.onlinestudy.MyUtils;
import seong.onlinestudy.domain.*;

import javax.persistence.EntityManager;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static seong.onlinestudy.MyUtils.*;

@DataJpaTest
public class PostRepositoryTest {

    @Autowired
    EntityManager em;
    @Autowired
    PostRepository postRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    GroupRepository groupRepository;
    @Autowired
    StudyRepository studyRepository;

    List<Member> members;
    List<Group> groups;
    List<Study> studies;
    List<Post> posts;

    @BeforeEach
    void init() {
        members = createMembers(50, false);
        memberRepository.saveAll(members);

        groups = createGroups(members, 20, false);
        groupRepository.saveAll(groups);

        posts = createPosts(members, groups, 20, false);
        postRepository.saveAll(posts);

        studies = createStudies(10, false);
        studyRepository.saveAll(studies);
    }

    @Test
    void createPost() {
        Member member = memberRepository.findAll().get(0);

        Post post = MyUtils.createPost("test", "test", PostCategory.CHAT, member);
        postRepository.save(post);
    }

    @Test
    void getPost() {
        //given
        Member member = memberRepository.findAll().get(0);
        Post post = MyUtils.createPost("test", "test", PostCategory.CHAT, member);

        postRepository.save(post);

        //when
        Post findPost = postRepository.findByIdWithMemberAndGroup(post.getId()).get();

        //then
        assertThat(findPost).isEqualTo(post);
        assertThat(findPost.getMember()).isEqualTo(member);
    }

    @Test
    void getPost_withMemberAndGroup() {
        //given
        Member member = memberRepository.findAll().get(0);
        Post post = MyUtils.createPost("test", "test", PostCategory.CHAT, member);
        Group group = groups.get(0);
        post.setGroup(group);

        postRepository.save(post);

        //when
        Post findPost = postRepository.findByIdWithMemberAndGroup(post.getId()).get();

        //then
        assertThat(findPost).isEqualTo(post);
        assertThat(findPost.getMember()).isEqualTo(member);
        assertThat(findPost.getGroup()).isEqualTo(group);
    }

    @Test
    void getPostWithStudies_withoutStudies() {
        //given
        Member member = memberRepository.findAll().get(0);
        Post post = MyUtils.createPost("test", "test", PostCategory.CHAT, member);
        Group group = groups.get(0);
        post.setGroup(group);

        postRepository.save(post);

        //when
        Post findPost = postRepository.findByIdWithStudies(post.getId()).get();

        //then
        assertThat(findPost).isEqualTo(post);
    }

    @Test
    void getPostWithStudies() {
        //given
        Member member = memberRepository.findAll().get(0);
        Post post = MyUtils.createPost("test", "test", PostCategory.CHAT, member);
        Group group = groups.get(0);
        post.setGroup(group);

        postRepository.save(post);

        List<Study> studies = this.studies.subList(0, 5);
        for (Study study : studies) {
            PostStudy.create(post, study);
        }

        //when
        Post findPost = postRepository.findByIdWithStudies(post.getId()).get();

        //then
        assertThat(findPost).isEqualTo(post);
        List<Study> findStudies = findPost.getPostStudies().stream().map(PostStudy::getStudy).collect(Collectors.toList());
        assertThat(findStudies).containsExactlyInAnyOrderElementsOf(studies);
    }
}
