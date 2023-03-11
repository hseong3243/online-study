package seong.onlinestudy.request;

import lombok.Data;

@Data
public class GroupUpdateRequest {

    private String description;
    private Integer headcount;
}
