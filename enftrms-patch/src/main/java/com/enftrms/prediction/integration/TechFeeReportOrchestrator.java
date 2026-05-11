package com.enftrms.prediction.integration;

import com.enftrms.prediction.integration.EnftrmsAnalysisClient.GenerateReportRequest;
import com.enftrms.prediction.integration.EnftrmsAnalysisClient.GenerateReportResponse;
import com.enftrms.prediction.integration.TechFeePayloadBuilder.NewsArticle;
import com.enftrms.prediction.integration.TechFeePayloadBuilder.TfeeRow;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 한 번의 호출로 TECH_FEE 섹션을 포함한 DOCX 최종보고서를 받는 오케스트레이터.
 *
 *   GenerateReportResponse resp = orchestrator.generate(
 *       "AIR-001",
 *       new OrgInfo(501L, "넥스트바이오"),
 *       new ProjectInfo(9001L, "S2022-00141998", "생물반응기..."),
 *       new ContractInfo(1L, "2026-NXB-001", "넥스트바이오 기술실시 계약", "DIRECT"),
 *       2026,
 *       row,                  // PS_ORGN_TFEE_CCLT 한 행
 *       articles              // 선택 — Google News RSS 등에서 미리 수집
 *   );
 *   byte[] docx = Base64.getDecoder().decode(resp.contentBase64);
 */
@Service
public class TechFeeReportOrchestrator {

    private final EnftrmsAnalysisClient client;

    public TechFeeReportOrchestrator(EnftrmsAnalysisClient client) { this.client = client; }

    public GenerateReportResponse generate(
            String reportId,
            OrgInfo org,
            ProjectInfo project,
            ContractInfo contract,
            Integer reportYear,
            TfeeRow row,
            List<NewsArticle> articles) {

        GenerateReportRequest req = new GenerateReportRequest();
        req.reportId = reportId;
        req.traceId = reportId;
        req.outputFormat = "DOCX";

        req.reportContext.reportYear = reportYear;
        req.reportContext.sections = List.of("SUMMARY", "TECH_FEE", "RECOMMENDATIONS");

        req.organization = org.toMap();
        req.project = project.toMap();
        req.contract = contract.toMap();

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("techFee", TechFeePayloadBuilder.fromRow(row, articles));
        req.analysis = analysis;

        return client.generateReport(req, reportId);
    }

    // ------------------------------------------------------------------

    public record OrgInfo(Long organizationId, String organizationName, String companySize) {
        public OrgInfo(Long id, String name) { this(id, name, null); }
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            if (organizationId   != null) m.put("organizationId", organizationId);
            if (organizationName != null) m.put("organizationName", organizationName);
            if (companySize      != null) m.put("companySize", companySize);
            return m;
        }
    }

    public record ProjectInfo(Long projectId, String projectCode, String projectName, String agencyName) {
        public ProjectInfo(Long id, String code, String name) { this(id, code, name, null); }
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            if (projectId   != null) m.put("projectId", projectId);
            if (projectCode != null) m.put("projectCode", projectCode);
            if (projectName != null) m.put("projectName", projectName);
            if (agencyName  != null) m.put("agencyName", agencyName);
            return m;
        }
    }

    public record ContractInfo(Long contractId, String contractNumber, String contractName, String licenseType) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            if (contractId     != null) m.put("contractId", contractId);
            if (contractNumber != null) m.put("contractNumber", contractNumber);
            if (contractName   != null) m.put("contractName", contractName);
            if (licenseType    != null) m.put("licenseType", licenseType);
            return m;
        }
    }
}
