package com.company.fashion.modules.member.repository;

import com.company.fashion.modules.member.entity.Member;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

  Optional<Member> findByIdAndDeletedFalse(Long id);

  Page<Member> findAllByDeletedFalse(Pageable pageable);
}
