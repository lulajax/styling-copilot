package com.company.fashion.modules.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.fashion.common.exception.BusinessException;
import com.company.fashion.modules.member.model.BodyProfileV2;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class BodyProfileServiceTest {

  private final BodyProfileService bodyProfileService = new BodyProfileService(new ObjectMapper());

  @Test
  void shouldNormalizeLegacyBodyDataForRead() {
    BodyProfileV2 profile = bodyProfileService.normalizeForReadToModel("""
        {
          "height": 168,
          "shape": "x"
        }
        """);

    assertThat(profile.getVersion()).isEqualTo(2);
    assertThat(profile.getMeasurements().getHeightCm()).isEqualTo(168.0);
    assertThat(profile.getMeasurements().getWeightKg()).isNotNull();
    assertThat(profile.getMeasurements().getTopSize()).isEqualTo("M");
    assertThat(profile.getMeasurements().getBottomSize()).isEqualTo("M");
  }

  @Test
  void shouldRejectWhenCoreFieldsMissingOnWrite() {
    assertThatThrownBy(() -> bodyProfileService.normalizeAndValidate("""
        {
          "version": 2,
          "measurements": {
            "heightCm": 168.0
          }
        }
        """))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("weightKg is required");
  }

  @Test
  void shouldRejectWhenValueOutOfRange() {
    assertThatThrownBy(() -> bodyProfileService.normalizeAndValidate("""
        {
          "version": 2,
          "measurements": {
            "heightCm": 168.0,
            "weightKg": 49.0,
            "shoulderWidthCm": 38.0,
            "bustCm": 84.0,
            "waistCm": 30.0,
            "hipCm": 89.0,
            "topSize": "S",
            "bottomSize": "S"
          }
        }
        """))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("waistCm");
  }

  @Test
  void shouldCalculateDerivedMetrics() {
    BodyProfileV2 profile = bodyProfileService.normalizeAndValidateToModel("""
        {
          "version": 2,
          "measurements": {
            "heightCm": 168.0,
            "weightKg": 49.0,
            "shoulderWidthCm": 38.0,
            "bustCm": 84.0,
            "waistCm": 62.0,
            "hipCm": 89.0,
            "bodyShape": "X",
            "legRatio": "long",
            "topSize": "S",
            "bottomSize": "S"
          }
        }
        """);

    assertThat(profile.getDerived().getBmi()).isEqualTo(17.4);
    assertThat(profile.getDerived().getWhr()).isEqualTo(0.7);
    assertThat(profile.getDerived().getShapeClass()).isEqualTo("X");
    assertThat(profile.getMeasurements().getTopSize()).isEqualTo("S");
    assertThat(profile.getMeasurements().getBottomSize()).isEqualTo("S");
  }

  @Test
  void shouldDropRetiredMeasurementsFromNormalizedOutput() throws Exception {
    String normalized = bodyProfileService.normalizeForRead("""
        {
          "heightCm": 168.0,
          "weightKg": 49.0,
          "shoulderWidthCm": 38.0,
          "bustCm": 84.0,
          "waistCm": 62.0,
          "hipCm": 89.0,
          "inseamCm": 74.0,
          "torsoLengthCm": 56.0,
          "neckCm": 31.0,
          "thighCm": 50.0,
          "bodyShape": "X",
          "legRatio": "long",
          "topSize": "S",
          "bottomSize": "S"
        }
        """);

    JsonNode measurements = new ObjectMapper().readTree(normalized).path("measurements");
    assertThat(measurements.has("inseamCm")).isFalse();
    assertThat(measurements.has("torsoLengthCm")).isFalse();
    assertThat(measurements.has("neckCm")).isFalse();
    assertThat(measurements.has("thighCm")).isFalse();
  }
}
