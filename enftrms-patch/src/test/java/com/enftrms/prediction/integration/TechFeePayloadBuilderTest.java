package com.enftrms.prediction.integration;

import com.enftrms.prediction.integration.TechFeePayloadBuilder.NewsArticle;
import com.enftrms.prediction.integration.TechFeePayloadBuilder.TechFeePayloadInput;
import com.enftrms.prediction.integration.TechFeePayloadBuilder.TfeeRow;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TechFeePayloadBuilderTest {

    @Test
    void fromRow_emitsExcelStyleBlock() {
        TfeeRow row = new TfeeRow();
        row.ttlPayGvstmAm = 100_000_000L;
        row.ttlUseGvstmAm = 99_000_000L;
        row.salesOccurYn = "Y";
        row.tfeeAm = 200_000L;
        row.techCtrbPt = 0.5;
        row.baseYear = 2026;
        row.techIpmtCntrDe = LocalDate.of(2023, 6, 1);

        Map<String, Object> out = TechFeePayloadBuilder.fromRow(row);

        assertThat(out).containsKey("row");
        @SuppressWarnings("unchecked")
        Map<String, Object> rowMap = (Map<String, Object>) out.get("row");
        assertThat(rowMap.get("ttlPayGvstmAm")).isEqualTo(100_000_000L);
        assertThat(rowMap.get("salesOccurYn")).isEqualTo("Y");
        assertThat(rowMap.get("techIpmtCntrDe")).isEqualTo("2023-06-01");
    }

    @Test
    void fromRow_withArticles_attachesArticleList() {
        TfeeRow row = new TfeeRow();
        row.ttlUseGvstmAm = 1000;
        NewsArticle a = new NewsArticle();
        a.title = "넥스트바이오, 식약처 혁신의료기기 지정";
        a.publishedAt = "2025-01-20";

        Map<String, Object> out = TechFeePayloadBuilder.fromRow(row, List.of(a));
        assertThat(out).containsKey("articles");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> articles = (List<Map<String, Object>>) out.get("articles");
        assertThat(articles).hasSize(1);
        assertThat(articles.get(0).get("title")).asString().contains("식약처");
    }

    @Test
    void build_withFinancialsAndExternalMetrics() {
        TechFeePayloadInput in = new TechFeePayloadInput();
        in.financials = Map.of("salesGrowthRate", 0.18, "operatingMargin", 0.06);
        in.externalMetrics = Map.of("g2bBidCount", 7);
        in.newsEventCounts = Map.of("PRODUCT_LAUNCH", 1, "CERTIFICATION", 2);
        in.predAt = LocalDate.of(2026, 5, 11);

        Map<String, Object> out = TechFeePayloadBuilder.build(in);

        assertThat(out).containsKeys("financials", "externalMetrics", "newsEventCounts", "predAt");
        assertThat(out.get("predAt")).isEqualTo("2026-05-11");
    }

    @Test
    void build_explicitCountsTakesPrecedenceOverArticles() {
        TechFeePayloadInput in = new TechFeePayloadInput();
        in.row = new TfeeRow();
        in.newsEventCounts = Map.of("CERTIFICATION", 1);
        NewsArticle a = new NewsArticle();
        a.title = "공급계약 체결";
        in.articles = List.of(a);

        Map<String, Object> out = TechFeePayloadBuilder.build(in);

        assertThat(out).containsKey("newsEventCounts");
        assertThat(out).doesNotContainKey("articles");
    }
}
