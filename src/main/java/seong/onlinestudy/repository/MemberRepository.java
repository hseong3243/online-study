package seong.onlinestudy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import seong.onlinestudy.domain.Group;
import seong.onlinestudy.domain.Member;
import seong.onlinestudy.dto.GroupMemberDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUsername(String username);

    /**
     * 주어진 group 에 소속된 Member 리스트를 반환한다.
     * @param groupId Group 의 id
     * @return Member 리스트를 반환
     */
    @Query("select m from Member m" +
            " join m.groupMembers gm on gm.group.id = :groupId")
    List<Member> findMembersInGroup(@Param("groupId") Long groupId);

    @Query("select m from Member m" +
            " join m.tickets t" +
            " join t.ticketRecord r" +
            " where t.startTime >= :startTime and t.startTime < :endTime" +
            " group by m.id" +
            " order by sum(r.activeTime) desc ")
    Page<Member> findMembersOrderByStudyTime(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime,
                                             Pageable pageable);
}
