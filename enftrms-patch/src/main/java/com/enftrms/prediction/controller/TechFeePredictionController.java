package com.enftrms.prediction.controller;

import com.enftrms.prediction.dto.ApiResponse;
import com.enftrms.prediction.dto.PredictionResultDto;
import com.enftrms.prediction.dto.PredictionRunRequest;
import com.enftrms.prediction.service.PredictionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tech-fee-prediction")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class TechFeePredictionController {

    private final PredictionService service;
    public TechFeePredictionController(PredictionService service) { this.service = service; }

    @PostMapping("/run")
    public ApiResponse<PredictionResultDto> run(@RequestBody PredictionRunRequest req) {
        PredictionResultDto result = service.run(
                req.getSbjtId(), req.getOrgnId(), req.getPredAt(), req.isForceRefresh());
        return ApiResponse.ok(result);
    }

    @GetMapping("/result")
    public ApiResponse<PredictionResultDto> latest(@RequestParam String sbjtId,
                                                   @RequestParam String orgnId) {
        PredictionResultDto r = service.latest(sbjtId, orgnId);
        if (r == null) return ApiResponse.fail("NOT_FOUND", "분석 결과가 없습니다. 분석을 먼저 실행해주세요.");
        return ApiResponse.ok(r);
    }
}
