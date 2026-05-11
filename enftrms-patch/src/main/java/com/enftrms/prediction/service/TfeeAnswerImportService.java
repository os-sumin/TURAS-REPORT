package com.enftrms.prediction.service;

import com.enftrms.prediction.dto.ExcelUploadResult;
import com.enftrms.prediction.mapper.PredictionMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 사용자가 제공한 엑셀 양식(PS_ORGN_TFEE_CCLT 시트) 적재.
 * 헤더는 6행에 있으며, 데이터는 7행부터. 컬럼 위치는 양식 기준으로 고정 매핑.
 */
@Service
public class TfeeAnswerImportService {

    private static final int HEADER_ROW_INDEX = 5;   // 0-based (양식 6행)
    private static final int DATA_START_ROW   = 6;

    /** 양식 컬럼 인덱스 매핑 (제공된 엑셀 분석 결과 기준 — 변경 시 본 상수만 수정) */
    private static final int COL_SBJT_ID         = 14;  // 과제ID
    private static final int COL_ORGN_NAME       = 10;  // 성과소유기관 명 (참조용)
    private static final int COL_OWN_BIZNO       = 11;  // 성과소유기관 사업자등록번호 (FRUT_OWN_ORGN_ID 대체)
    private static final int COL_TECH_BIZNO      = 26;  // 기술실시 기관 사업자등록번호
    private static final int COL_PAY_GVSTM       = 19;  // 지급 정부지원금
    private static final int COL_USE_GVSTM       = 24;  // 실사용 정부지원금
    private static final int COL_TECH_CTRB_PT    = 43;  // 기술기여도 M1
    private static final int COL_RND_INCOME      = 42;  // R&D 수익금액 L1
    private static final int COL_BASE_YEAR       = 38;  // 기준보고년도
    private static final int COL_SALES_OCCUR_YN  = 41;  // 매출발생여부
    private static final int COL_TFEE_AM         = 46;  // 기술료 금액 O

    private final PredictionMapper mapper;

    public TfeeAnswerImportService(PredictionMapper mapper) { this.mapper = mapper; }

    @Transactional
    public ExcelUploadResult upload(MultipartFile file) throws Exception {
        String batchId = "EXL-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        List<Map<String, Object>> rows = new ArrayList<>();
        List<ExcelUploadResult.RowError> errors = new ArrayList<>();
        int skipped = 0;

        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = DATA_START_ROW; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r == null) { skipped++; continue; }
                String sbjtId = readStr(r, COL_SBJT_ID);
                String orgnId = readStr(r, COL_OWN_BIZNO);
                if (isBlank(sbjtId) || isBlank(orgnId)) { skipped++; continue; }

                try {
                    Map<String, Object> row = new HashMap<>();
                    row.put("sbjtId", sbjtId);
                    row.put("frutOwnOrgnId", orgnId);
                    row.put("techIpmtOrgnId", readStr(r, COL_TECH_BIZNO));
                    row.put("baseYear", readInt(r, COL_BASE_YEAR));
                    row.put("ttlPayGvstmAm", readLong(r, COL_PAY_GVSTM));
                    row.put("ttlUseGvstmAm", readLong(r, COL_USE_GVSTM));
                    row.put("salesOccurYn", "Y".equalsIgnoreCase(readStr(r, COL_SALES_OCCUR_YN)) ? "Y" : "N");
                    row.put("rndIncomeAm", readLong(r, COL_RND_INCOME));
                    row.put("techCtrbPt", readDouble(r, COL_TECH_CTRB_PT));
                    row.put("tfeeAm", readLong(r, COL_TFEE_AM));
                    row.put("answerY", deriveAnswer(row));
                    row.put("excelBatchId", batchId);
                    rows.add(row);
                } catch (Exception ex) {
                    errors.add(ExcelUploadResult.RowError.builder()
                            .rowNumber(i + 1).reason(ex.getMessage()).build());
                }
            }
        }

        int inserted = rows.isEmpty() ? 0 : mapper.insertAnswerBatch(rows);
        return ExcelUploadResult.builder()
                .batchId(batchId).inserted(inserted).skipped(skipped).errors(errors).build();
    }

    private static Integer deriveAnswer(Map<String, Object> row) {
        Long tfee = (Long) row.getOrDefault("tfeeAm", 0L);
        String yn = (String) row.getOrDefault("salesOccurYn", "N");
        if ("Y".equals(yn) && tfee != null && tfee > 0) return 1;
        return 0;
    }

    /* ===== Cell 읽기 헬퍼 ===== */
    private static String readStr(Row r, int col) {
        Cell c = r.getCell(col);
        if (c == null) return null;
        switch (c.getCellType()) {
            case STRING:  return c.getStringCellValue().trim();
            case NUMERIC: return String.valueOf((long) c.getNumericCellValue());
            case BOOLEAN: return Boolean.toString(c.getBooleanCellValue());
            case FORMULA: try { return c.getStringCellValue(); } catch (Exception e) { return String.valueOf(c.getNumericCellValue()); }
            default:      return null;
        }
    }
    private static Long readLong(Row r, int col) {
        Cell c = r.getCell(col);
        if (c == null) return 0L;
        if (c.getCellType() == CellType.NUMERIC) return (long) c.getNumericCellValue();
        try { return Long.parseLong(readStr(r, col)); } catch (Exception e) { return 0L; }
    }
    private static Integer readInt(Row r, int col) {
        Long v = readLong(r, col);
        return v == null ? null : v.intValue();
    }
    private static Double readDouble(Row r, int col) {
        Cell c = r.getCell(col);
        if (c == null) return 0d;
        if (c.getCellType() == CellType.NUMERIC) return c.getNumericCellValue();
        try { return Double.parseDouble(readStr(r, col)); } catch (Exception e) { return 0d; }
    }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
