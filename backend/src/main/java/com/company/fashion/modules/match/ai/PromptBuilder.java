package com.company.fashion.modules.match.ai;

import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.match.entity.MatchRecord;
import com.company.fashion.modules.member.entity.Member;
import com.company.fashion.modules.member.model.BodyMeasurements;
import com.company.fashion.modules.member.service.BodyProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

  private final RecommendationTemplate recommendationTemplate;
  private final PreviewTemplate previewTemplate;

  public PromptBuilder(BodyProfileService bodyProfileService) {
    this.recommendationTemplate = new RecommendationTemplate(bodyProfileService);
    this.previewTemplate = new PreviewTemplate(bodyProfileService);
  }

  public String buildPrompt(
      Member member,
      List<Clothing> candidates,
      List<MatchRecord> history,
      String scene,
      AiLanguage language
  ) {
    return recommendationTemplate.build(member, candidates, history, scene, language);
  }

  public String buildPreviewPrompt(Member member, List<Clothing> selected, String scene, AiLanguage language) {
    return previewTemplate.build(member, selected, scene, language);
  }

  static final class RecommendationTemplate {

    private final BodyProfileService bodyProfileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    RecommendationTemplate(BodyProfileService bodyProfileService) {
      this.bodyProfileService = bodyProfileService;
    }

    String build(
        Member member,
        List<Clothing> candidates,
        List<MatchRecord> history,
        String scene,
        AiLanguage language
    ) {
      BodyMeasurements measurements = resolveMeasurements(member);

      StringJoiner clothingJoiner = new StringJoiner("\n");
      for (Clothing clothing : candidates) {
        clothingJoiner.add(buildClothingLine(clothing));
      }

      StringJoiner historyJoiner = new StringJoiner("\n");
      for (MatchRecord record : history) {
        historyJoiner.add("- {clothingId:" + record.getClothingId() + ", score:" + safe(record.getPerformanceScore()) + "}");
      }

      return """
          You are a fashion recommendation assistant for livestream styling.
          Output JSON object only, no markdown.
          JSON object must contain field:
          - outfits(array of recommendation items)
          Each recommendation item must have fields:
          - topClothingId(number)
          - bottomClothingId(number)
          - score(number 0-100)
          - reason(string)

          Member profile:
          - name: %s
          - heightCm: %s, weightKg: %s, bodyShape: %s
          - shoulderWidthCm: %s, bustCm: %s, waistCm: %s, hipCm: %s
          - memberTopSize: %s, memberBottomSize: %s
          - styleTags: %s
          - scene: %s

          Candidate clothing (evaluate style and size fitness against member measurements):
          %s

          Recent history:
          %s

          Rules:
          1) Use candidate IDs listed above only.
          2) Every recommendation must be a valid TOP + BOTTOM pair.
          3) Return up to 8 valid outfits, ranked by score descending.
          4) Clothing items can be reused across different outfits to provide more options.
          5) Keep score in [0, 100].
          6) Prioritize diverse style combinations and good size fit.
          7) Consider size compatibility: compare clothing measurements with member body measurements.
          8) In reason field, mention size fit assessment (e.g., "Shoulder width 38cm fits member 39cm well").
          9) The reason field must be written in %s.
          """.formatted(
          safe(member.getName()),
          safe(measurements.getHeightCm()),
          safe(measurements.getWeightKg()),
          safe(measurements.getBodyShape()),
          safe(measurements.getShoulderWidthCm()),
          safe(measurements.getBustCm()),
          safe(measurements.getWaistCm()),
          safe(measurements.getHipCm()),
          safe(measurements.getTopSize()),
          safe(measurements.getBottomSize()),
          safe(member.getStyleTags()),
          safe(scene),
          clothingJoiner,
          history.isEmpty() ? "- none" : historyJoiner.toString(),
          (language == null ? AiLanguage.EN : language).promptLabel()
      );
    }

    private String buildClothingLine(Clothing clothing) {
      StringBuilder sb = new StringBuilder();
      sb.append("- {id:").append(clothing.getId())
          .append(", name:\"").append(safe(clothing.getName()))
          .append("\", type:").append(safe(clothing.getClothingType()))
          .append(", styleTags:\"").append(safe(clothing.getStyleTags())).append("\"");

      Map<String, Object> sizeMap = parseSizeData(clothing.getSizeData());
      if (!sizeMap.isEmpty()) {
        sb.append(", size:{");
        StringJoiner sizeJoiner = new StringJoiner(", ");

        addSizeField(sizeJoiner, sizeMap, "shoulderWidthCm");
        addSizeField(sizeJoiner, sizeMap, "bustCm");
        addSizeField(sizeJoiner, sizeMap, "waistCm");
        addSizeField(sizeJoiner, sizeMap, "hipCm");
        addSizeField(sizeJoiner, sizeMap, "lengthCm");
        addSizeField(sizeJoiner, sizeMap, "sleeveLengthCm");
        addSizeField(sizeJoiner, sizeMap, "inseamCm");
        addSizeField(sizeJoiner, sizeMap, "topSize");
        addSizeField(sizeJoiner, sizeMap, "bottomSize");

        sb.append(sizeJoiner).append("}");
      }

      sb.append("}");
      return sb.toString();
    }

    private void addSizeField(StringJoiner joiner, Map<String, Object> sizeMap, String field) {
      Object value = sizeMap.get(field);
      if (value != null) {
        joiner.add(field + ":" + value);
      }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSizeData(String sizeData) {
      if (sizeData == null || sizeData.isBlank()) {
        return Map.of();
      }
      try {
        return objectMapper.readValue(sizeData, Map.class);
      } catch (Exception e) {
        return Map.of();
      }
    }

    private BodyMeasurements resolveMeasurements(Member member) {
      try {
        return bodyProfileService.normalizeForReadToModel(member.getBodyData()).getMeasurements();
      } catch (Exception ex) {
        BodyMeasurements defaults = new BodyMeasurements();
        defaults.setHeightCm(165.0);
        defaults.setWeightKg(50.0);
        defaults.setBodyShape("H");
        defaults.setShoulderWidthCm(38.0);
        defaults.setBustCm(84.0);
        defaults.setWaistCm(64.0);
        defaults.setHipCm(90.0);
        defaults.setTopSize("M");
        defaults.setBottomSize("M");
        return defaults;
      }
    }

    private String safe(Object value) {
      return value == null ? "" : String.valueOf(value);
    }
  }

  static final class PreviewTemplate {

    private final BodyProfileService bodyProfileService;

    PreviewTemplate(BodyProfileService bodyProfileService) {
      this.bodyProfileService = bodyProfileService;
    }

    String build(Member member, List<Clothing> selected, String scene, AiLanguage language) {
      BodyMeasurements measurements = resolveMeasurements(member);

      StringJoiner clothingJoiner = new StringJoiner("\n");
      for (Clothing clothing : selected) {
        clothingJoiner.add("- {name:\"" + safe(clothing.getName()) + "\", imageUrl:\"" + safe(clothing.getImageUrl())
            + "\", styleTags:\"" + safe(clothing.getStyleTags()) + "\", clothingType:" + safe(clothing.getClothingType())
            + "}");
      }

      return """
          You are a livestream fashion stylist.
          Generate an outfit preview as JSON object only with fields:
          title, outfitDescription, imagePrompt.

          Member:
          - name: %s
          - bodyProfile: {heightCm:%s, weightKg:%s, bodyShape:%s}
          - memberTopSize: %s
          - memberBottomSize: %s
          - styleTags: %s
          - scene: %s

          Selected outfit pieces metadata:
          %s

          Constraints:
          1) Keep title within 10 words.
          2) outfitDescription should be 1-2 sentences.
          3) Use the attached member and clothing reference images as the visual ground truth.
          4) imagePrompt must describe a realistic virtual try-on composite showing the member wearing the selected outfit.
          5) Output strict JSON only, without markdown.
          6) title, outfitDescription, and imagePrompt must all be written in %s.
          """.formatted(
          safe(member.getName()),
          safe(measurements.getHeightCm()),
          safe(measurements.getWeightKg()),
          safe(measurements.getBodyShape()),
          safe(measurements.getTopSize()),
          safe(measurements.getBottomSize()),
          safe(member.getStyleTags()),
          safe(scene),
          clothingJoiner,
          (language == null ? AiLanguage.EN : language).promptLabel()
      );
    }

    private BodyMeasurements resolveMeasurements(Member member) {
      try {
        return bodyProfileService.normalizeForReadToModel(member.getBodyData()).getMeasurements();
      } catch (Exception ex) {
        BodyMeasurements defaults = new BodyMeasurements();
        defaults.setHeightCm(165.0);
        defaults.setWeightKg(50.0);
        defaults.setBodyShape("H");
        defaults.setShoulderWidthCm(38.0);
        defaults.setBustCm(84.0);
        defaults.setWaistCm(64.0);
        defaults.setHipCm(90.0);
        defaults.setTopSize("M");
        defaults.setBottomSize("M");
        return defaults;
      }
    }

    private String safe(Object value) {
      return value == null ? "" : String.valueOf(value);
    }
  }
}
