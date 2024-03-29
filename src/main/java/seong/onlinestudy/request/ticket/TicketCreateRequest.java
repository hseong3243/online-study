package seong.onlinestudy.request.ticket;

import lombok.Data;
import seong.onlinestudy.enumtype.TicketStatus;

import javax.validation.constraints.NotNull;

@Data
public class TicketCreateRequest {

    private Long studyId;

    @NotNull(message = "그룹 지정은 필수입니다.")
    private Long groupId;

    @NotNull(message = "상태 지정은 필수입니다.")
    private TicketStatus status;
}
