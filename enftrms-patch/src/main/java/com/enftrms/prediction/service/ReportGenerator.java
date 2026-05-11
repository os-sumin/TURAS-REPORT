package com.enftrms.prediction.service;

import com.enftrms.prediction.dto.DimensionScoreDto;
import com.enftrms.prediction.dto.PredictionResultDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 보고서 생성기.
 * - PDF: 본 패치는 HTML 변환 결과를 그대로 반환(서버측 PDF 변환기는 EnFTRMS 합본 단계에서 연결).
 *        사업기획서 VII-3 의 외부 보고 표현 변환(원시수치 → 등급/문구)을 여기서 적용.
 * - XLSX: 우선순위 목록 다운로드 — POI 로 생성.
 */
@Service
public class ReportGenerator {

    public byte[] renderHtmlReport(PredictionResultDto r) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='ko'><head><meta charset='UTF-8'>")
          .append("<title>기술료 납부 가능성 예측 리포트</title>")
          .append("<style>")
          .append("body{font-family:'Malgun Gothic',sans-serif;padding:24px;color:#222}")
          .append("h1{font-size:20px}h2{font-size:16px;border-bottom:1px solid #ccc;padding-bottom:6px;margin-top:24px}")
          .append("table{border-collapse:collapse;width:100%;margin-top:8px}")
          .append("th,td{border:1px solid #ccc;padding:8px;text-align:left;font-size:13px}")
          .append("th{background:#f4f6fa}")
          .append(".grade-VERY_HIGH{color:#b00020;font-weight:700}")
          .append(".grade-HIGH{color:#1565c0;font-weight:700}")
          .append("</style></head><body>");

        sb.append("<h1>기술료 납부 가능성 예측 리포트</h1>");
        sb.append("<table>")
          .append("<tr><th>과제 ID</th><td>").append(safe(r.getSbjtId())).append("</td></tr>")
          .append("<tr><th>기업 ID</th><td>").append(safe(r.getOrgnId())).append("</td></tr>")
          .append("<tr><th>예측 시점</th><td>").append(r.getPredAt()).append("</td></tr>")
          .append("<tr><th>총점</th><td>").append(r.getTotalScore()).append(" / 100</td></tr>");
        if (r.getGrade() != null) {
            sb.append("<tr><th>예측 등급</th><td class='grade-").append(r.getGrade().getCode()).append("'>")
              .append(r.getGrade().getLabel()).append("</td></tr>")
              .append("<tr><th>추천 조치</th><td>").append(r.getGrade().getAction()).append("</td></tr>");
        }
        sb.append("<tr><th>룰 버전</th><td>").append(r.getRuleVersion()).append("</td></tr></table>");

        sb.append("<h2>차원별 점수</h2><table>")
          .append("<tr><th>차원</th><th>점수</th><th>만점</th><th>외부 보고용 표현</th></tr>");
        if (r.getDimensions() != null) {
            for (DimensionScoreDto d : r.getDimensions()) {
                sb.append("<tr><td>").append(safe(d.getLabel())).append("</td>")
                  .append("<td>").append(d.getScore()).append("</td>")
                  .append("<td>").append(d.getMaxScore()).append("</td>")
                  .append("<td>").append(toExternalPhrase(d)).append("</td></tr>");
            }
        }
        sb.append("</table></body></html>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] renderPriorityXlsx(List<PredictionResultDto> rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("우선순위");
            Row h = sh.createRow(0);
            String[] headers = { "과제ID", "기업ID", "총점", "등급", "추천조치", "예측시점", "룰버전" };
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);

            int rowIdx = 1;
            for (PredictionResultDto r : rows) {
                Row row = sh.createRow(rowIdx++);
                row.createCell(0).setCellValue(safe(r.getSbjtId()));
                row.createCell(1).setCellValue(safe(r.getOrgnId()));
                row.createCell(2).setCellValue(r.getTotalScore());
                row.createCell(3).setCellValue(r.getGrade() != null ? r.getGrade().getLabel() : "");
                row.createCell(4).setCellValue(r.getGrade() != null ? r.getGrade().getAction() : "");
                row.createCell(5).setCellValue(r.getPredAt() == null ? "" : r.getPredAt().toString());
                row.createCell(6).setCellValue(safe(r.getRuleVersion()));
            }
            for (int i = 0; i < headers.length; i++) sh.autoSizeColumn(i);
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("XLSX 생성 실패", e);
        }
    }

    /** 사업기획서 VII-3: 내부 분석값 → 외부 보고용 표현 변환 */
    private static String toExternalPhrase(DimensionScoreDto d) {
        double pct = d.getMaxScore() == 0 ? 0 : d.getScore() / d.getMaxScore();
        if (pct >= 0.8) return "강한 긍정 신호";
        if (pct >= 0.6) return "긍정 신호";
        if (pct >= 0.4) return "중립";
        if (pct >= 0.2) return "약한 부정 신호";
        return "정보 부족 또는 부정";
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
