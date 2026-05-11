package com.enftrms.prediction.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Python AI 서비스(/internal/ai/reports/generate) 호출 클라이언트.
 *
 * 환경 변수
 *   ENFTRMS_ANALYSIS_BASE_URL  (예: http://enftrms-ai:8001)
 *   ENFTRMS_ANALYSIS_TOKEN     X-Internal-Token 값
 *   ENFTRMS_ANALYSIS_TIMEOUT_SEC  (선택, 기본 180)
 */
@Component
public class EnftrmsAnalysisClient {

    private static final String GENERATE_PATH = "/internal/ai/reports/generate";

    private final WebClient webClient;
    private final String internalToken;
    private final Duration timeout;
    private final ObjectMapper json = new ObjectMapper();

    public EnftrmsAnalysisClient(
            @Value("${enftrms.analysis.base-url:http://localhost:8001}") String baseUrl,
            @Value("${enftrms.analysis.token:change-me-local-token}") String internalToken,
            @Value("${enftrms.analysis.timeout-sec:180}") int timeoutSeconds) {
        this.internalToken = internalToken;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    /** TECH_FEE 섹션 + 다른 섹션을 포함한 DOCX 생성 요청 */
    public GenerateReportResponse generateReport(GenerateReportRequest req, String traceId) {
        try {
            String body = json.writeValueAsString(req);
            return webClient.post()
                    .uri(GENERATE_PATH)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Internal-Token", internalToken)
                    .header("X-Trace-Id", traceId == null ? req.reportId : traceId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(GenerateReportResponse.class)
                    .block(timeout);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize report request", e);
        }
    }

    // ------------------------------------------------------------------
    //  Wire DTOs — camelCase, 양쪽 CamelModel 호환
    // ------------------------------------------------------------------

    public static final class GenerateReportRequest {
        @JsonProperty("traceId")      public String traceId;
        @JsonProperty("reportId")     public String reportId;
        @JsonProperty("outputFormat") public String outputFormat = "DOCX";
        @JsonProperty("reportContext") public ReportContextDto reportContext = new ReportContextDto();
        @JsonProperty("contract")     public Map<String, Object> contract = new LinkedHashMap<>();
        @JsonProperty("organization") public Map<String, Object> organization = new LinkedHashMap<>();
        @JsonProperty("project")      public Map<String, Object> project = new LinkedHashMap<>();
        @JsonProperty("survey")       public Map<String, Object> survey = new LinkedHashMap<>();
        @JsonProperty("analysis")     public Map<String, Object> analysis = new LinkedHashMap<>();
        @JsonProperty("financialInputs") public List<Map<String, Object>> financialInputs = List.of();
    }

    public static final class ReportContextDto {
        @JsonProperty("reportYear")        public Integer reportYear;
        @JsonProperty("includeGptAnalysis") public boolean includeGptAnalysis = false;
        @JsonProperty("sections")          public List<String> sections = List.of("SUMMARY", "TECH_FEE", "RECOMMENDATIONS");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GenerateReportResponse {
        @JsonProperty("reportId")      public String reportId;
        @JsonProperty("status")        public String status;          // COMPLETED | FAILED
        @JsonProperty("contentType")   public String contentType;
        @JsonProperty("fileName")      public String fileName;
        @JsonProperty("contentBase64") public String contentBase64;
        @JsonProperty("metadata")      public Map<String, Object> metadata;
    }
}
