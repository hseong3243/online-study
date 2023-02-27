package seong.onlinestudy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seong.onlinestudy.domain.*;
import seong.onlinestudy.dto.PostDto;
import seong.onlinestudy.dto.PostStudyDto;
import seong.onlinestudy.exception.UnAuthorizationException;
import seong.onlinestudy.repository.GroupRepository;
import seong.onlinestudy.repository.PostRepository;
import seong.onlinestudy.repository.PostStudyRepository;
import seong.onlinestudy.repository.StudyRepository;
import seong.onlinestudy.request.PostCreateRequest;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final GroupRepository groupRepository;
    private final StudyRepository studyRepository;
    private final PostStudyRepository postStudyRepository;

    public Page<Post> getPosts(int page, int size, String search, PostCategory category, List<Long> studyIds) {
        return null;
    }

    @Transactional
    public Long createPost(PostCreateRequest request, Member loginMember) {
        Post post = Post.createPost(request, loginMember);

        if(request.getGroupId() != null) {
            Group group = groupRepository.findGroupWithMembers(request.getGroupId())
                    .orElseThrow(() -> new NoSuchElementException("존재하지 않는 그룹입니다."));

            Optional<GroupMember> groupMember = group.getGroupMembers().stream()
                    .filter(gm -> gm.getMember().getId().equals(loginMember.getId())).findFirst();
            groupMember.orElseThrow(() -> new UnAuthorizationException("접근 권한이 없습니다."));

            post.setGroup(group);
        }

        if(request.getStudyIds() != null) {
            List<Study> studies = studyRepository.findAllById(request.getStudyIds());
            List<PostStudy> postStudies = studies.stream().map(study -> PostStudy.create(post, study))
                    .collect(Collectors.toList());
        }

        Post savedPost = postRepository.save(post);

        return savedPost.getId();
    }

    public PostDto getPost(Long postId) {
        Post post = postRepository.findByIdWithMemberAndGroup(postId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 게시글입니다."));

        PostDto postDto = PostDto.from(post);

        //게시글 학습 태그 리스트 조회
        List<PostStudy> postStudies = postStudyRepository.findStudiesWherePost(post);
        List<PostStudyDto> postStudyDtos = postStudies.stream()
                .map(PostStudyDto::from).collect(Collectors.toList());

        postDto.setPostStudies(postStudyDtos);

        return postDto;
    }
}