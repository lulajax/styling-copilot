package com.company.fashion.modules.match.strategy;

import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.match.ai.AiLanguage;
import com.company.fashion.modules.match.entity.MatchRecord;
import com.company.fashion.modules.member.entity.Member;
import java.util.List;

public record RecommendationRequest(
    Member member,
    List<Clothing> candidates,
    List<MatchRecord> history,
    String scene,
    AiLanguage language
) {
}
