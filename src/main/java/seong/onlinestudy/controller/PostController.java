package seong.onlinestudy.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import seong.onlinestudy.argumentresolver.Login;
import seong.onlinestudy.controller.response.PageResult;
import seong.onlinestudy.controller.response.Result;
import seong.onlinestudy.dto.PostDto;
import seong.onlinestudy.exception.InvalidSessionException;
import seong.onlinestudy.request.post.PostCreateRequest;
import seong.onlinestudy.request.post.PostUpdateRequest;
import seong.onlinestudy.request.post.PostsDeleteRequest;
import seong.onlinestudy.request.post.PostsGetRequest;
import seong.onlinestudy.service.PostService;

import javax.validation.Valid;
import java.util.List;

import static seong.onlinestudy.constant.SessionConst.LOGIN_MEMBER;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PostController {

    private final PostService postService;

    @GetMapping("/posts")
    public Result<List<PostDto>> getPosts(@Valid PostsGetRequest request) {
        Page<PostDto> postsWithPageInfo = postService.getPosts(request);

        return new PageResult<>("200", postsWithPageInfo.getContent(), postsWithPageInfo);
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<Long> createPost(@RequestBody @Valid PostCreateRequest request,
                                   @Login Long memberId) {
        Long postId = postService.createPost(request, memberId);

        return new Result<>("201", postId);
    }

    @GetMapping("/posts/{postId}")
    public Result<PostDto> getPost(@PathVariable("postId") Long postId) {
        PostDto post = postService.getPost(postId);

        return new Result<>("200", post);
    }

    @PatchMapping("/posts/{postId}")
    public Result<Long> updatePost(@PathVariable("postId") Long postId,
                                   @RequestBody @Valid PostUpdateRequest request,
                                   @Login Long memberId) {
        Long updatePostId = postService.updatePost(postId, request, memberId);

        return new Result<>("200", updatePostId);
    }

    @DeleteMapping("/posts/{postId}")
    public Result<String> deletePost(@PathVariable("postId") Long postId,
                                   @Login Long memberId) {
        postService.deletePost(postId, memberId);

        return new Result<>("200", "delete post");
    }

    @PostMapping("/posts/delete")
    public Result<String> deletePosts(@RequestBody @Valid PostsDeleteRequest request,
                                      @SessionAttribute(value = LOGIN_MEMBER, required = false) Long memberId) {
        postService.deletePosts(request, memberId);

        return new Result<>("200", "delete Posts");
    }
}
