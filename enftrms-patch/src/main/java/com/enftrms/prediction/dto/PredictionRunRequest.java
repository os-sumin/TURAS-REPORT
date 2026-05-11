package com.enftrms.prediction.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
public class PredictionRunRequest {
    private String sbjtId;
    private String orgnId;
    private boolean forceRefresh = false;
    private LocalDate predAt;     // null 이면 SYSDATE
}
