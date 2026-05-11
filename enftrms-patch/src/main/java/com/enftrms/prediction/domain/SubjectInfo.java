package com.enftrms.prediction.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter @Builder
public class SubjectInfo {
    private final String sbjtId;
    private final String sbjtName;
    private final String ksic;
    private final LocalDate endDe;
    private final long totalGovFund;
    private final long totalProjectCost;
}
