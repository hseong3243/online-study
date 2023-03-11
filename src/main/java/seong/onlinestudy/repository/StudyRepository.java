package seong.onlinestudy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import seong.onlinestudy.domain.Study;

import java.time.LocalDateTime;
import java.util.Optional;

public interface StudyRepository extends JpaRepository<Study, Long>, StudyRepositoryCustom {

    Optional<Study> findByName(String name);
    Page<Study> findAllByNameContains(String name, Pageable pageable);

    /**
     * Study 엔티티로부터 ticket 의 member 가 일치하고, startTime 이상, endTime 미만인 Ticket 을 조인한 스터디 목록을 조회한다.
     * @param memberId ticket 의 member
     * @param startTime ticket 의 startTime 이 startTime 이상
     * @param endTime ticket 의 startTime 이 endTime 미만
     * @param pageable
     * @return Study 리스트를 반환
     */
    @Query("select s from Study s" +
            " join s.tickets t on t.member.id = :memberId and t.startTime >= :startTime and t.startTime < :endTime" +
            " group by s.id" +
            " order by sum(t.activeTime) desc")
    Page<Study> findStudiesByMember(@Param("memberId") Long memberId, @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime, Pageable pageable);

}
