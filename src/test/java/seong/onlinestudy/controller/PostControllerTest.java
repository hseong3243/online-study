package seong.onlinestudy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import seong.onlinestudy.MyUtils;
import seong.onlinestudy.domain.*;
import seong.onlinestudy.dto.*;
import seong.onlinestudy.enumtype.GroupCategory;
import seong.onlinestudy.enumtype.PostCategory;
import seong.onlinestudy.request.post.PostCreateRequest;
import seong.onlinestudy.request.post.PostUpdateRequest;
import seong.onlinestudy.request.post.PostsDeleteRequest;
import seong.onlinestudy.service.PostService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static seong.onlinestudy.constant.SessionConst.LOGIN_MEMBER;
import static seong.onlinestudy.docs.DocumentFormatGenerator.getDefaultValue;

@AutoConfigureRestDocs
@WebMvcTest(PostController.class)
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(RestDocumentationExtension.class)
class PostControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    MockHttpSession session;

    @MockBean
    PostService postService;

    public PostControllerTest() {
        session = new MockHttpSession();
    }

    @BeforeEach
    void init(WebApplicationContext context, RestDocumentationContextProvider provider) {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(provider))
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .build();
    }

    @Test
    public void getPosts() throws Exception {
        //given
        MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
        request.add("groupId", "1");
        request.add("search", "검색어");
        request.add("category", "CHAT");
        request.add("studyIds", "1, 2, 3");
        request.add("page", "0");
        request.add("size", "10");

        Member member = MyUtils.createMember("member", "member");
        setField(member, "id", 1L);
        MemberDto memberDto = MemberDto.from(member);

        GroupDto groupDto = createGroupDto();

        CommentDto commentDto = createCommentDto(memberDto);

        PostDto postDto = createPostDto(memberDto, groupDto, commentDto);

        PostStudyDto postStudyDto = createPostStudyDto();
        postDto.setPostStudies(List.of(postStudyDto));

        PageRequest pageRequest = PageRequest.of(0, 10);

        given(postService.getPosts(any())).willReturn(new PageImpl<>(List.of(postDto), pageRequest, 1L));

        //when
        ResultActions resultActions = mvc.perform(get("/api/v1/posts")
                .params(request));

        //then
        resultActions.andExpect(status().isOk())
                .andDo(print())
                .andDo(document("posts-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestParameters(
                                parameterWithName("page").attributes(getDefaultValue("0")).description("페이지 번호"),
                                parameterWithName("size").attributes(getDefaultValue("10")).description("페이지 사이즈"),
                                parameterWithName("memberId").description("회원 엔티티 아이디").optional(),
                                parameterWithName("groupId").description("그룹 엔티티 아이디").optional(),
                                parameterWithName("search").description("게시글 제목 대상 검색어").optional(),
                                parameterWithName("category").description("게시글 카테고리(Enum Type 탭 참고)").optional(),
                                parameterWithName("studyIds").description("스터디 엔티티 아이디 목록").optional()
                        ),
                        responseFields(
                                beneathPath("data").withSubsectionId("data"),
                                fieldWithPath("postId").type(NUMBER).description("게시글 엔티티 아이디"),
                                fieldWithPath("title").type(STRING).description("게시글 제목"),
                                fieldWithPath("content").type(STRING).description("게시글 본문"),
                                fieldWithPath("category").type(STRING).description("게시글 카테고리"),
                                fieldWithPath("createdAt").type(STRING).description("게시글 생성일"),
                                fieldWithPath("viewCount").type(NUMBER).description("게시글 조회수"),
                                fieldWithPath("deleted").type(BOOLEAN).description("게시글 삭제 여부"),

                                subsectionWithPath("member").type(OBJECT).description("게시글 작성자"),
                                fieldWithPath("member.memberId").type(NUMBER).description("작성자 엔티티 아이디"),
                                fieldWithPath("member.username").type(STRING).description("작성자 아이디"),
                                fieldWithPath("member.nickname").type(STRING).description("작성자 닉네임"),
                                fieldWithPath("member.deleted").type(BOOLEAN).description("작성자 삭제 여부"),

                                subsectionWithPath("group").type(OBJECT).description("게시글 작성 그룹"),
                                fieldWithPath("group.groupId").type(NUMBER).description("작성 그룹 엔티티 아이디"),
                                fieldWithPath("group.name").type(STRING).description("작성 그룹명"),
                                fieldWithPath("group.headcount").type(NUMBER).description("작성 그룹 최대 회원수"),
                                fieldWithPath("group.createdAt").type(STRING).description("작성 그룹 생성일"),
                                fieldWithPath("group.description").type(STRING).description("작성 그룹 설명"),
                                fieldWithPath("group.category").type(STRING).description("작성 그룹 카테고리"),
                                fieldWithPath("group.deleted").type(BOOLEAN).description("작성 그룹 삭제 여부"),

                                subsectionWithPath("postStudies").type(ARRAY).description("스터디 태그 목록"),
                                fieldWithPath("postStudies[].postStudyId").type(NUMBER).description("스터디 태그 엔티티 아이디"),
                                fieldWithPath("postStudies[].studyId").type(NUMBER).description("스터디 엔티티 아이디"),
                                fieldWithPath("postStudies[].name").type(STRING).description("스터디 이름"),

                                subsectionWithPath("comments").type(ARRAY).description("게시글 댓글 목록"),
                                fieldWithPath("comments[].commentId").type(NUMBER).description("댓글 엔티티 아이디"),
                                fieldWithPath("comments[].content").type(STRING).description("댓글 내용"),
                                fieldWithPath("comments[].member").type(OBJECT).description("댓글 작성자"),
                                fieldWithPath("comments[].createdAt").type(STRING).description("댓글 작성일"),
                                fieldWithPath("comments[].postId").type(NUMBER).description("게시글 엔티티 아이디"),
                                fieldWithPath("comments[].deleted").type(BOOLEAN).description("댓글 삭제 여부")
                        )));
    }

    @Test
    public void createPost() throws Exception {
        //given
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("제목");
        request.setContent("본문");
        request.setCategory(PostCategory.CHAT);
        request.setStudyIds(List.of(1L, 2L));
        request.setGroupId(1L);

        Member testMember = MyUtils.createMember("member", "member");
        session.setAttribute(LOGIN_MEMBER, 1L);

        given(postService.createPost(any(), anyLong())).willReturn(1L);

        //when
        ResultActions resultActions = mvc.perform(post("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
                .session(session));

        //then
        resultActions.andExpect(status().isCreated())
                .andDo(print())
                .andDo(document("post-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("title").type(STRING).description("게시글 제목"),
                                fieldWithPath("content").type(STRING).description("게시글 본문"),
                                fieldWithPath("category").type(STRING).description("게시글 카테고리"),
                                fieldWithPath("studyIds").type(ARRAY).description("게시글 스터디 태그 목록").optional(),
                                fieldWithPath("groupId").type(NUMBER).description("게시글 연관 그룹")
                        ),
                        responseFields(
                                fieldWithPath("code").type(STRING).description("HTTP 상태 코드"),
                                fieldWithPath("data").type(NUMBER).description("게시글 엔티티 아이디")
                        )
                ));
    }

    @Test
    public void getPost() throws Exception {
        //given
        Member member = MyUtils.createMember("member", "member");
        setField(member, "id", 1L);
        MemberDto memberDto = MemberDto.from(member);

        GroupDto groupDto = createGroupDto();

        CommentDto commentDto = createCommentDto(memberDto);

        PostDto postDto = createPostDto(memberDto, groupDto, commentDto);

        PostStudyDto postStudyDto = createPostStudyDto();
        postDto.setPostStudies(List.of(postStudyDto));

        given(postService.getPost(anyLong())).willReturn(postDto);

        //when
        ResultActions resultActions = mvc.perform(get("/api/v1/posts/{postId}", 1));

        //then
        resultActions.andExpect(status().isOk())
                .andDo(print())
                .andDo(document("post-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("postId").description("게시글 엔티티 아이디")
                        ),
                        responseFields(
                                beneathPath("data").withSubsectionId("data"),
                                fieldWithPath("postId").type(NUMBER).description("게시글 엔티티 아이디"),
                                fieldWithPath("title").type(STRING).description("게시글 제목"),
                                fieldWithPath("content").type(STRING).description("게시글 본문"),
                                fieldWithPath("category").type(STRING).description("게시글 카테고리"),
                                fieldWithPath("createdAt").type(STRING).description("게시글 생성일"),
                                fieldWithPath("viewCount").type(NUMBER).description("게시글 조회수"),
                                fieldWithPath("deleted").type(BOOLEAN).description("게시글 삭제 여부"),

                                subsectionWithPath("member").type(OBJECT).description("게시글 작성자"),
                                fieldWithPath("member.memberId").type(NUMBER).description("작성자 엔티티 아이디"),
                                fieldWithPath("member.username").type(STRING).description("작성자 아이디"),
                                fieldWithPath("member.nickname").type(STRING).description("작성자 닉네임"),
                                fieldWithPath("member.deleted").type(BOOLEAN).description("작성자 삭제 여부"),

                                subsectionWithPath("group").type(OBJECT).description("게시글 작성 그룹"),
                                fieldWithPath("group.groupId").type(NUMBER).description("작성 그룹 엔티티 아이디"),
                                fieldWithPath("group.name").type(STRING).description("작성 그룹명"),
                                fieldWithPath("group.headcount").type(NUMBER).description("작성 그룹 최대 회원수"),
                                fieldWithPath("group.createdAt").type(STRING).description("작성 그룹 생성일"),
                                fieldWithPath("group.description").type(STRING).description("작성 그룹 설명"),
                                fieldWithPath("group.category").type(STRING).description("작성 그룹 카테고리"),
                                fieldWithPath("group.deleted").type(BOOLEAN).description("작성 그룹 삭제 여부"),

                                subsectionWithPath("postStudies").type(ARRAY).description("스터디 태그 목록"),
                                fieldWithPath("postStudies[].postStudyId").type(NUMBER).description("스터디 태그 엔티티 아이디"),
                                fieldWithPath("postStudies[].studyId").type(NUMBER).description("스터디 엔티티 아이디"),
                                fieldWithPath("postStudies[].name").type(STRING).description("스터디 이름"),

                                subsectionWithPath("comments").type(ARRAY).description("게시글 댓글 목록"),
                                fieldWithPath("comments[].commentId").type(NUMBER).description("댓글 엔티티 아이디"),
                                fieldWithPath("comments[].content").type(STRING).description("댓글 내용"),
                                fieldWithPath("comments[].member").type(OBJECT).description("댓글 작성자"),
                                fieldWithPath("comments[].createdAt").type(STRING).description("댓글 작성일"),
                                fieldWithPath("comments[].postId").type(NUMBER).description("게시글 엔티티 아이디"),
                                fieldWithPath("comments[].deleted").type(BOOLEAN).description("댓글 삭제 여부")
                        )));
    }

    @Test
    public void updatePost() throws Exception {
        //given
        PostUpdateRequest request = new PostUpdateRequest();
        request.setContent("업데이트 본문");
        request.setTitle("업데이트 제목");
        request.setCategory(PostCategory.CHAT);
        request.setStudyIds(List.of(1L, 2L));

        Member testMember = MyUtils.createMember("member", "member");
        session.setAttribute(LOGIN_MEMBER, 1L);

        //when
        ResultActions resultActions = mvc.perform(patch("/api/v1/posts/{postId}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
                .session(session));

        //then
        resultActions.andExpect(status().isOk())
                .andDo(print())
                .andDo(document("post-update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("postId").description("게시글 엔티티 아이디")
                        ),
                        requestFields(
                                fieldWithPath("title").type(STRING).description("업데이트할 제목").optional(),
                                fieldWithPath("content").type(STRING).description("업데이트할 본문").optional(),
                                fieldWithPath("category").type(STRING).description("업데이트할 카테고리(Enum Type)").optional(),
                                fieldWithPath("studyIds").type(ARRAY).description("업데이트할 연관된 스터디 목록").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").description("HTTP 상태 코드"),
                                fieldWithPath("data").type(NUMBER).description("업데이트 된 게시글 엔티티 ID")
                        )));
    }

    @Test
    public void deletePost() throws Exception {
        //given
        Member member = MyUtils.createMember("member", "member");
        session.setAttribute(LOGIN_MEMBER, 1L);

        //when
        ResultActions resultActions = mvc.perform(delete("/api/v1/posts/{postId}", 1)
                .session(session));

        //then
        resultActions.andExpect(status().isOk())
                .andDo(print())
                .andDo(document("post-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("postId").description("게시글 엔티티 아이디")
                        ),
                        responseFields(
                                fieldWithPath("code").type(STRING).description("HTTP 상태 코드"),
                                fieldWithPath("data").type(STRING).description("게시글 삭제 메시지")
                        )));
    }

    @Test
    void deletePosts() throws Exception {
        //given
        Member member = MyUtils.createMember("member", "member");
        session.setAttribute(LOGIN_MEMBER, 1L);

        PostsDeleteRequest request = new PostsDeleteRequest();
        request.setMemberId(1L);

        //when
        ResultActions resultActions = mvc.perform(post("/api/v1/posts/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                .session(session));

        //then
        resultActions.andExpect(status().isOk())
                .andDo(print())
                .andDo(document("posts-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("memberId").description("회원 엔티티 아이디")
                        ),
                        responseFields(
                                fieldWithPath("code").type(STRING).description("HTTP 상태 코드"),
                                fieldWithPath("data").type(STRING).description("게시글 삭제 메시지")
                        )));
    }

    private PostStudyDto createPostStudyDto() {
        PostStudyDto postStudy = new PostStudyDto();
        postStudy.setPostStudyId(1L);
        postStudy.setStudyId(1L);
        postStudy.setName("스터디명");

        return postStudy;
    }

    private GroupDto createGroupDto() {
        GroupDto group = new GroupDto();
        group.setGroupId(1L);
        group.setName("그룹명");
        group.setHeadcount(30);
        group.setCreatedAt(LocalDateTime.now());
        group.setDescription("그룹 설명");
        group.setCategory(GroupCategory.IT);
        return group;
    }

    private PostDto createPostDto(MemberDto memberDto, GroupDto groupDto, CommentDto commentDto) {
        PostDto postDto = new PostDto();
        postDto.setPostId(1L);
        postDto.setTitle("제목");
        postDto.setContent("내용");
        postDto.setCategory(PostCategory.CHAT);
        postDto.setCreatedAt(LocalDateTime.now());
        postDto.setViewCount(1);
        postDto.setMember(memberDto);
        postDto.setGroup(groupDto);
        postDto.setComments(List.of(commentDto));
        return postDto;
    }

    private CommentDto createCommentDto(MemberDto memberDto) {
        CommentDto commentDto = new CommentDto();
        commentDto.setCommentId(1L);
        commentDto.setContent("내용");
        commentDto.setPostId(1L);
        commentDto.setMember(memberDto);
        commentDto.setCreatedAt(LocalDateTime.now());
        return commentDto;
    }
}