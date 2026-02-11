package com.company.fashion.modules.match.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Business lifecycle status for a match record.
 */
@Schema(description = "Match record status enum")
public enum MatchRecordStatus {
  // Generated but not confirmed by operator.
  DRAFT,
  // Confirmed by operator as selected look.
  ACCEPTED,
  // Used in actual livestream.
  BROADCASTED,
  // Rejected by operator.
  REJECTED
}
