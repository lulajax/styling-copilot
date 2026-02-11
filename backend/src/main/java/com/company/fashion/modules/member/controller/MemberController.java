package com.company.fashion.modules.member.controller;

import com.company.fashion.common.api.PageResponse;
import com.company.fashion.common.api.Result;
import com.company.fashion.modules.member.dto.CreateMemberRequest;
import com.company.fashion.modules.member.dto.MemberResponse;
import com.company.fashion.modules.member.dto.UpdateMemberRequest;
import com.company.fashion.modules.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@Tag(name = "Member", description = "Member profile management APIs")
@SecurityRequirement(name = "bearerAuth")
public class MemberController {

  private final MemberService memberService;

  public MemberController(MemberService memberService) {
    this.memberService = memberService;
  }

  @PostMapping
  @Operation(summary = "Create a member")
  @ApiResponse(responseCode = "200", description = "Member created")
  public Result<MemberResponse> create(@Valid @RequestBody CreateMemberRequest request) {
    return Result.ok(memberService.create(request));
  }

  @PutMapping("/{memberId}")
  @Operation(summary = "Update a member")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Member updated"),
      @ApiResponse(responseCode = "404", description = "Member not found")
  })
  public Result<MemberResponse> update(
      @Parameter(description = "Member ID", required = true) @PathVariable Long memberId,
      @RequestBody UpdateMemberRequest request
  ) {
    return Result.ok(memberService.update(memberId, request));
  }

  @GetMapping("/{memberId}")
  @Operation(summary = "Get member detail")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Query success"),
      @ApiResponse(responseCode = "404", description = "Member not found")
  })
  public Result<MemberResponse> get(
      @Parameter(description = "Member ID", required = true) @PathVariable Long memberId
  ) {
    return Result.ok(memberService.get(memberId));
  }

  @GetMapping
  @Operation(summary = "List members")
  public Result<PageResponse<MemberResponse>> list(
      @Parameter(description = "Page number, starts from 0") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size
  ) {
    return Result.ok(memberService.list(page, size));
  }

  @DeleteMapping("/{memberId}")
  @Operation(summary = "Delete member (soft delete)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Delete success"),
      @ApiResponse(responseCode = "404", description = "Member not found")
  })
  public Result<Void> delete(
      @Parameter(description = "Member ID", required = true) @PathVariable Long memberId
  ) {
    memberService.delete(memberId);
    return Result.ok();
  }
}
