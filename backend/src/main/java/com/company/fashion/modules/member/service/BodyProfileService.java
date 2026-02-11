package com.company.fashion.modules.member.service;

import com.company.fashion.common.exception.BusinessException;
import com.company.fashion.modules.member.model.BodyDerivedMetrics;
import com.company.fashion.modules.member.model.BodyMeasurements;
import com.company.fashion.modules.member.model.BodyProfileV2;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class BodyProfileService {

  private static final Set<String> BODY_SHAPES = Set.of("X", "H", "A", "O");
  private static final Set<String> LEG_RATIOS = Set.of("short", "regular", "long");
  private static final Set<String> CLOTHING_SIZES = Set.of("XS", "S", "M", "L", "XL");

  private final ObjectMapper objectMapper;

  public BodyProfileService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String normalizeAndValidate(String rawBodyData) {
    BodyProfileV2 profile = normalizeToModel(rawBodyData, true);
    return toJson(profile);
  }

  public String normalizeForRead(String rawBodyData) {
    BodyProfileV2 profile = normalizeToModel(rawBodyData, false);
    return toJson(profile);
  }

  public BodyProfileV2 normalizeAndValidateToModel(String rawBodyData) {
    return normalizeToModel(rawBodyData, true);
  }

  public BodyProfileV2 normalizeForReadToModel(String rawBodyData) {
    return normalizeToModel(rawBodyData, false);
  }

  public BodyDerivedMetrics calculateDerived(BodyMeasurements measurements, String shapeClassOverride) {
    BodyDerivedMetrics derived = new BodyDerivedMetrics();

    double heightM = measurements.getHeightCm() / 100.0d;
    double bmi = measurements.getWeightKg() / (heightM * heightM);
    derived.setBmi(round(bmi, 1));

    double whr = measurements.getWaistCm() / measurements.getHipCm();
    derived.setWhr(round(whr, 2));

    String shapeClass = shapeClassOverride == null ? inferShapeClass(measurements) : shapeClassOverride;
    derived.setShapeClass(shapeClass);
    return derived;
  }

  private String inferShapeClass(BodyMeasurements measurements) {
    double bust = measurements.getBustCm();
    double waist = measurements.getWaistCm();
    double hip = measurements.getHipCm();

    double bustWaist = bust - waist;
    double hipWaist = hip - waist;
    double bustHipGap = Math.abs(bust - hip);

    if (waist >= bust && waist >= hip) {
      return "O";
    }
    if (bustWaist >= 20 && hipWaist >= 20 && bustHipGap <= 10) {
      return "X";
    }
    if (hipWaist - bustWaist >= 8) {
      return "A";
    }
    return "H";
  }

  private void assertRequiredCoreFields(BodyMeasurements measurements) {
    requireNotNull(measurements.getHeightCm(), "heightCm");
    requireNotNull(measurements.getWeightKg(), "weightKg");
    requireNotNull(measurements.getShoulderWidthCm(), "shoulderWidthCm");
    requireNotNull(measurements.getBustCm(), "bustCm");
    requireNotNull(measurements.getWaistCm(), "waistCm");
    requireNotNull(measurements.getHipCm(), "hipCm");
    requireNotBlank(measurements.getTopSize(), "topSize");
    requireNotBlank(measurements.getBottomSize(), "bottomSize");
  }

  private void requireNotNull(Double value, String field) {
    if (value == null) {
      throw new BusinessException(400, field + " is required");
    }
  }

  private void requireNotBlank(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(400, field + " is required");
    }
  }

  private void applyCoreDefaults(BodyMeasurements measurements) {
    if (measurements.getHeightCm() == null) {
      measurements.setHeightCm(165.0);
    }
    if (measurements.getWeightKg() == null) {
      measurements.setWeightKg(50.0);
    }
    if (measurements.getShoulderWidthCm() == null) {
      measurements.setShoulderWidthCm(38.0);
    }
    if (measurements.getBustCm() == null) {
      measurements.setBustCm(84.0);
    }
    if (measurements.getWaistCm() == null) {
      measurements.setWaistCm(64.0);
    }
    if (measurements.getHipCm() == null) {
      measurements.setHipCm(90.0);
    }
  }

  private void applyOptionalDefaults(BodyMeasurements measurements) {
    if (measurements.getLegRatio() == null || measurements.getLegRatio().isBlank()) {
      measurements.setLegRatio("regular");
    }
  }

  private void applySizeDefaults(BodyMeasurements measurements) {
    if (measurements.getTopSize() == null || measurements.getTopSize().isBlank()) {
      measurements.setTopSize("M");
    }
    if (measurements.getBottomSize() == null || measurements.getBottomSize().isBlank()) {
      measurements.setBottomSize("M");
    }
  }

  private void validateRanges(BodyMeasurements measurements) {
    validateRange("heightCm", measurements.getHeightCm(), 130, 200);
    validateRange("weightKg", measurements.getWeightKg(), 30, 120);
    validateRange("shoulderWidthCm", measurements.getShoulderWidthCm(), 30, 55);
    validateRange("bustCm", measurements.getBustCm(), 60, 140);
    validateRange("waistCm", measurements.getWaistCm(), 45, 120);
    validateRange("hipCm", measurements.getHipCm(), 70, 150);

    if (measurements.getBodyShape() != null && !BODY_SHAPES.contains(measurements.getBodyShape())) {
      throw new BusinessException(400, "bodyShape must be one of X,H,A,O");
    }
    if (!LEG_RATIOS.contains(measurements.getLegRatio())) {
      throw new BusinessException(400, "legRatio must be one of short,regular,long");
    }
    if (!CLOTHING_SIZES.contains(measurements.getTopSize())) {
      throw new BusinessException(400, "topSize must be one of XS,S,M,L,XL");
    }
    if (!CLOTHING_SIZES.contains(measurements.getBottomSize())) {
      throw new BusinessException(400, "bottomSize must be one of XS,S,M,L,XL");
    }
  }

  private void validateRange(String field, Double value, double min, double max) {
    if (value == null || value < min || value > max) {
      throw new BusinessException(400, field + " must be between " + min + " and " + max);
    }
  }

  private void normalizeEnums(BodyMeasurements measurements) {
    String bodyShape = measurements.getBodyShape();
    if (bodyShape != null) {
      measurements.setBodyShape(bodyShape.trim().toUpperCase(Locale.ROOT));
    }
    String legRatio = measurements.getLegRatio();
    if (legRatio != null) {
      measurements.setLegRatio(legRatio.trim().toLowerCase(Locale.ROOT));
    }
    String topSize = measurements.getTopSize();
    if (topSize != null) {
      measurements.setTopSize(topSize.trim().toUpperCase(Locale.ROOT));
    }
    String bottomSize = measurements.getBottomSize();
    if (bottomSize != null) {
      measurements.setBottomSize(bottomSize.trim().toUpperCase(Locale.ROOT));
    }
  }

  private BodyMeasurements readMeasurements(Map<String, Object> source) {
    BodyMeasurements measurements = new BodyMeasurements();
    measurements.setHeightCm(round(readDouble(source, "heightCm", "height"), 1));
    measurements.setWeightKg(round(readDouble(source, "weightKg", "weight"), 1));
    measurements.setShoulderWidthCm(round(readDouble(source, "shoulderWidthCm", "shoulderWidth"), 1));
    measurements.setBustCm(round(readDouble(source, "bustCm", "bust"), 1));
    measurements.setWaistCm(round(readDouble(source, "waistCm", "waist"), 1));
    measurements.setHipCm(round(readDouble(source, "hipCm", "hip"), 1));
    measurements.setBodyShape(readString(source, "bodyShape", "shape"));
    measurements.setLegRatio(readString(source, "legRatio"));
    measurements.setTopSize(readString(source, "topSize"));
    measurements.setBottomSize(readString(source, "bottomSize"));
    return measurements;
  }

  private Map<String, Object> parseRoot(String rawBodyData) {
    if (rawBodyData == null || rawBodyData.isBlank()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(rawBodyData, new TypeReference<>() {
      });
    } catch (Exception ex) {
      throw new BusinessException(400, "bodyData is not valid JSON");
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> nestedMap(Object value) {
    if (value instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    return Map.of();
  }

  private Double readDouble(Map<String, Object> source, String... keys) {
    for (String key : keys) {
      Object raw = source.get(key);
      Double parsed = toDouble(raw);
      if (parsed != null) {
        return parsed;
      }
    }
    return null;
  }

  private String readString(Map<String, Object> source, String... keys) {
    for (String key : keys) {
      Object raw = source.get(key);
      if (raw != null) {
        String value = String.valueOf(raw).trim();
        if (!value.isBlank()) {
          return value;
        }
      }
    }
    return null;
  }

  private Double toDouble(Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof Number number) {
      return number.doubleValue();
    }
    try {
      return Double.parseDouble(String.valueOf(raw));
    } catch (Exception ex) {
      return null;
    }
  }

  private Double round(Double value, int scale) {
    if (value == null) {
      return null;
    }
    return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
  }

  private String toJson(BodyProfileV2 profile) {
    try {
      return objectMapper.writeValueAsString(profile);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to serialize body profile", ex);
    }
  }

  private BodyProfileV2 normalizeToModel(String rawBodyData, boolean strictWrite) {
    if ((rawBodyData == null || rawBodyData.isBlank()) && strictWrite) {
      throw new BusinessException(400, "bodyData is required");
    }

    Map<String, Object> root = parseRoot(rawBodyData);
    Map<String, Object> measurementsRoot = nestedMap(root.get("measurements"));
    if (measurementsRoot.isEmpty()) {
      measurementsRoot = root;
    }

    BodyMeasurements measurements = readMeasurements(measurementsRoot);
    normalizeEnums(measurements);

    if (strictWrite) {
      assertRequiredCoreFields(measurements);
    } else {
      applyCoreDefaults(measurements);
    }

    applyOptionalDefaults(measurements);
    applySizeDefaults(measurements);
    validateRanges(measurements);

    String shapeClass = inferShapeClass(measurements);
    if (measurements.getBodyShape() == null || measurements.getBodyShape().isBlank()) {
      measurements.setBodyShape(shapeClass);
    }

    BodyDerivedMetrics derivedMetrics = calculateDerived(measurements, shapeClass);

    BodyProfileV2 profile = new BodyProfileV2();
    profile.setVersion(2);
    profile.setMeasurements(measurements);
    profile.setDerived(derivedMetrics);
    return profile;
  }
}
