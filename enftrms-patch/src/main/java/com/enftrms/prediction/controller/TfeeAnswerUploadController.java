package com.enftrms.prediction.controller;

import com.enftrms.prediction.dto.ApiResponse;
import com.enftrms.prediction.dto.ExcelUploadResult;
import com.enftrms.prediction.service.TfeeAnswerImportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tech-fee-prediction/answers")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class TfeeAnswerUploadController {

    private final TfeeAnswerImportService service;
    public TfeeAnswerUploadController(TfeeAnswerImportService service) { this.service = service; }

    @PostMapping("/upload")
    public ApiResponse<ExcelUploadResult> upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return ApiResponse.fail("BAD_REQUEST", "파일이 비어있습니다.");
        }
        return ApiResponse.ok(service.upload(file));
    }
}
