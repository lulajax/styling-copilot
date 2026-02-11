package com.company.fashion.modules.match.repository;

import com.company.fashion.modules.match.entity.MatchTask;
import com.company.fashion.modules.match.dto.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MatchTaskRepository extends JpaRepository<MatchTask, String> {

  Page<MatchTask> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

  Page<MatchTask> findAllByOrderByCreatedAtDesc(Pageable pageable);

  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update MatchTask t set t.status = :status, t.errorMessage = :errorMessage where t.id = :taskId")
  int updateStatusAndError(@Param("taskId") String taskId, @Param("status") TaskStatus status, @Param("errorMessage") String errorMessage);
}
