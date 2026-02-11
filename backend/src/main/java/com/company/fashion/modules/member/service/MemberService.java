package com.company.fashion.modules.member.service;

import com.company.fashion.common.api.PageResponse;
import com.company.fashion.common.exception.BusinessException;
import com.company.fashion.modules.member.dto.CreateMemberRequest;
import com.company.fashion.modules.member.dto.MemberResponse;
import com.company.fashion.modules.member.dto.UpdateMemberRequest;
import com.company.fashion.modules.member.entity.Member;
import com.company.fashion.modules.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

  private final MemberRepository memberRepository;
  private final BodyProfileService bodyProfileService;

  public MemberService(MemberRepository memberRepository, BodyProfileService bodyProfileService) {
    this.memberRepository = memberRepository;
    this.bodyProfileService = bodyProfileService;
  }

  @Transactional
  public MemberResponse create(CreateMemberRequest request) {
    Member member = new Member();
    member.setName(request.name());
    member.setBodyData(bodyProfileService.normalizeAndValidate(request.bodyData()));
    String photoUrl = (request.photoUrl() == null || request.photoUrl().isBlank())
        ? "https://pics.youliaolive.cn/fashion/member/2026/02/10/model.png"
        : request.photoUrl();
    member.setPhotoUrl(photoUrl);
    member.setStyleTags(request.styleTags());
    Member saved = memberRepository.save(member);
    return toResponse(saved);
  }

  @Transactional
  public MemberResponse update(Long id, UpdateMemberRequest request) {
    Member member = getActiveEntity(id);
    if (request.name() != null && !request.name().isBlank()) {
      member.setName(request.name());
    }
    if (request.bodyData() != null) {
      member.setBodyData(bodyProfileService.normalizeAndValidate(request.bodyData()));
    }
    if (request.photoUrl() != null) {
      member.setPhotoUrl(request.photoUrl());
    }
    if (request.styleTags() != null) {
      member.setStyleTags(request.styleTags());
    }
    return toResponse(memberRepository.save(member));
  }

  @Transactional(readOnly = true)
  public MemberResponse get(Long id) {
    return toResponse(getActiveEntity(id));
  }

  @Transactional(readOnly = true)
  public PageResponse<MemberResponse> list(int page, int size) {
    Page<Member> memberPage = memberRepository.findAllByDeletedFalse(PageRequest.of(page, size));
    return new PageResponse<>(
        memberPage.getContent().stream().map(this::toResponse).toList(),
        memberPage.getTotalElements(),
        page,
        size
    );
  }

  @Transactional
  public void delete(Long id) {
    Member member = getActiveEntity(id);
    member.setDeleted(true);
    memberRepository.save(member);
  }

  @Transactional(readOnly = true)
  public Member getActiveEntity(Long id) {
    return memberRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new BusinessException(404, "Member not found"));
  }

  private MemberResponse toResponse(Member member) {
    return new MemberResponse(
        member.getId(),
        member.getName(),
        bodyProfileService.normalizeForRead(member.getBodyData()),
        member.getPhotoUrl(),
        member.getStyleTags()
    );
  }
}
