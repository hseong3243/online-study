package seong.onlinestudy.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import seong.onlinestudy.argumentresolver.Login;
import seong.onlinestudy.controller.response.Result;
import seong.onlinestudy.dto.MemberTicketDto;
import seong.onlinestudy.dto.TicketDto;
import seong.onlinestudy.exception.InvalidSessionException;
import seong.onlinestudy.request.ticket.TicketCreateRequest;
import seong.onlinestudy.request.ticket.TicketGetRequest;
import seong.onlinestudy.service.TicketService;

import javax.validation.Valid;
import java.util.List;

import static seong.onlinestudy.constant.SessionConst.LOGIN_MEMBER;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/tickets")
    public Result<List<MemberTicketDto>> getTickets(@Valid TicketGetRequest ticketGetRequest) {

        List<MemberTicketDto> memberTickets = ticketService.getTickets(ticketGetRequest);

        return new Result<>("200", memberTickets);
    }

    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<Long> createTicket(@RequestBody @Valid TicketCreateRequest createTicketRequest,
                             @Login Long memberId) {
        Long ticketId = ticketService.createTicket(createTicketRequest, memberId);

        return new Result<>("201", ticketId);
    }

    @PatchMapping("/tickets/{ticketId}")
    public Result<Long> expiredTicket(@PathVariable("ticketId") Long ticketId,
                                      @Login Long memberId) {
        Long expiredTicketId = ticketService.expireTicket(ticketId, memberId);

        return new Result<>("200", expiredTicketId);
    }

    @GetMapping("/tickets/{ticketId}")
    public Result<TicketDto> getTicket(@PathVariable("ticketId") Long ticketId) {
        TicketDto ticket = ticketService.getTicket(ticketId);

        return new Result<>("200", ticket);
    }
}
