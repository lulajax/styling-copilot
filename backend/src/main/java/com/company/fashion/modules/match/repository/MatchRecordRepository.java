package com.company.fashion.modules.match.repository;

import com.company.fashion.modules.match.entity.MatchRecord;
import com.company.fashion.modules.match.entity.MatchRecordStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRecordRepository extends JpaRepository<MatchRecord, Long> {

  List<MatchRecord> findTop10ByMemberIdOrderByPerformanceScoreDesc(Long memberId);

  @Query("""
      select m from MatchRecord m
      where m.memberId = :memberId
      and m.clothingId in :clothingIds
      and m.status = :status
      and m.broadcastDate > :fromDate
      """)
  List<MatchRecord> findRecentDuplicates(
      @Param("memberId") Long memberId,
      @Param("clothingIds") List<Long> clothingIds,
      @Param("status") MatchRecordStatus status,
      @Param("fromDate") LocalDateTime fromDate
  );

  Optional<MatchRecord> findByIdAndMemberId(Long id, Long memberId);

  List<MatchRecord> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

  long countByMemberId(Long memberId);
}
