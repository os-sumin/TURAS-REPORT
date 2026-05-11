package com.enftrms.prediction.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter @Builder
public class ExcelUploadResult {
    private final String batchId;
    private final int inserted;
    private final int skipped;
    private final List<RowError> errors;

    @Getter @Builder
    public static class RowError {
        private final int rowNumber;
        private final String reason;
    }
}
