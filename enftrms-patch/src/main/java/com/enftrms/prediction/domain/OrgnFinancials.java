package com.enftrms.prediction.domain;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class OrgnFinancials {
    private final Double salesGrowthRate;     // 매출 증가율
    private final Double operatingMargin;     // 영업이익률
    private final Double debtRatio;           // 부채비율
    private final Double cashToGovFund;       // 현금성자산 / 정부지원금
    private final Double rndIntensity;        // R&D비 / 매출
    private final Double assetGrowthRate;     // 유무형 자산 증가율
}
