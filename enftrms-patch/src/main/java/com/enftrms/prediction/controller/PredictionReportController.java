package com.enftrms.prediction.controller;

import com.enftrms.prediction.dto.PredictionResultDto;
import com.enftrms.prediction.mapper.PredictionMapper;
import com.enftrms.prediction.service.ReportGenerator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/tech-fee-prediction")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class PredictionReportController {

    private final ReportGenerator generator;
    private final PredictionMapper mapper;

    public PredictionReportController(ReportGenerator generator, PredictionMapper mapper) {
        this.generator = generator;
        this.mapper = mapper;
    }

    /** 과제별 리포트 — 본 패치는 HTML 반환. EnFTRMS 합본 시 PDF 변환기로 교체 */
    @GetMapping(value = "/result/{predId}/report.html",
                produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> reportHtml(@PathVariable long predId) {
        PredictionResultDto r = findById(predId);
        if (r == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(generator.renderHtmlReport(r));
    }

    @GetMapping("/priority.xlsx")
    public ResponseEntity<byte[]> priorityXlsx(@RequestParam(required = false) List<String> grade,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "1000") int size) {
        List<String> grades = (grade == null || grade.isEmpty())
                ? List.of("VERY_HIGH", "HIGH") : grade;
        int offset = Math.max(0, (page - 1) * size);
        List<PredictionResultDto> rows = mapper.selectPriorityList(grades, offset, size);

        byte[] body = generator.renderPriorityXlsx(rows);
        String fname = URLEncoder.encode("기술료예측_우선순위.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fname)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    private PredictionResultDto findById(long predId) {
        // 본 패치 단계에서는 ID 기반 조회 매퍼를 추가하지 않고, 차원 점수만으로 리포트 생성.
        List<com.enftrms.prediction.dto.DimensionScoreDto> dims = mapper.selectDimensionScores(predId);
        if (dims == null || dims.isEmpty()) return null;
        return PredictionResultDto.builder()
                .predId(predId).dimensions(dims).totalScore(
                        dims.stream().mapToDouble(com.enftrms.prediction.dto.DimensionScoreDto::getScore).sum()
                ).build();
    }

    @SuppressWarnings("unused")
    private List<String> emptyToDefault(List<String> in) {
        return in == null ? Collections.emptyList() : in;
    }
}
