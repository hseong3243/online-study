package seong.onlinestudy.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class StudiesGetRequest {

    private int page;
    private int size;
    private String name;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    private int days;

    public StudiesGetRequest() {
        page = 0;
        size = 10;
        name = "";
        //6일 전 05시부터 다음날 05시까지
        date = LocalDate.now().minusDays(6);
        days = 7;
    }
}
