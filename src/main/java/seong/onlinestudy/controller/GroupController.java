package seong.onlinestudy.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import seong.onlinestudy.argumentresolver.Login;
import seong.onlinestudy.controller.response.PageResult;
import seong.onlinestudy.controller.response.Result;
import seong.onlinestudy.dto.GroupDto;
import seong.onlinestudy.exception.InvalidSessionException;
import seong.onlinestudy.request.group.GroupCreateRequest;
import seong.onlinestudy.request.group.GroupUpdateRequest;
import seong.onlinestudy.request.group.GroupsDeleteRequest;
import seong.onlinestudy.request.group.GroupsGetRequest;
import seong.onlinestudy.service.GroupService;

import javax.validation.Valid;

import java.util.List;

import static seong.onlinestudy.constant.SessionConst.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/groups")
    @ResponseStatus(value = HttpStatus.CREATED)
    public Result<Long> createGroup(@RequestBody @Valid GroupCreateRequest createRequest,
                                    @Login Long memberId) {
        Long groupId = groupService.createGroup(createRequest, memberId);

        return new Result<>("201", groupId);
    }

    @PostMapping("/groups/{groupId}/join")
    @ResponseStatus(value = HttpStatus.CREATED)
    public Result<Long> joinGroup(@PathVariable Long groupId,
                          @Login Long memberId) {
        Long joinedGroupId = groupService.joinGroup(groupId, memberId);

        return new Result<>("201", joinedGroupId);
    }

    @PostMapping("/groups/{groupId}/quit")
    public Result<String> quitGroup(@PathVariable Long groupId,
                                  @Login Long memberId) {
        groupService.quitGroup(groupId, memberId);

        return new Result<>("200", "ok");
    }

    @GetMapping("/groups")
    public Result<List<GroupDto>> getGroups(@Valid GroupsGetRequest request) {
        Page<GroupDto> groupsWithPageInfo = groupService.getGroups(request);

        return new PageResult<>("200", groupsWithPageInfo.getContent(), groupsWithPageInfo);
    }

    @GetMapping("/groups/{id}")
    public Result<GroupDto> getGroup(@PathVariable Long id) {
        GroupDto group = groupService.getGroup(id);

        return new Result<>("200", group);
    }

    @DeleteMapping("/groups/{id}")
    public Result<String> deleteGroup(@PathVariable Long id,
                                      @Login Long memberId) {
        groupService.deleteGroup(id, memberId);

        return new Result<>("200", "deleted");
    }

    @PostMapping("/groups/{id}")
    public Result<Long> updateGroup(@PathVariable Long id,
                                    @RequestBody @Valid GroupUpdateRequest request,
                                    @Login Long memberId) {
        Long groupId = groupService.updateGroup(id, request, memberId);

        return new Result<>("200", groupId);
    }

    @PostMapping("/groups/delete")
    public Result<String> deleteGroups(@RequestBody @Valid GroupsDeleteRequest request,
                                       @Login Long memberId) {
        groupService.deleteGroups(request, memberId);

        return new Result<>("200", "delete groups");
    }

    @PostMapping("/groups/quit")
    public Result<String> quitGroups(@RequestBody @Valid GroupsDeleteRequest request,
                                     @Login Long memberId) {
        groupService.quitGroups(request, memberId);

        return new Result<>("200", "quit groups");
    }
}
