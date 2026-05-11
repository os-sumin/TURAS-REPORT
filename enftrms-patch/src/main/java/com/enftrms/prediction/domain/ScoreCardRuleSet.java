package com.enftrms.prediction.domain;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * scorecard.yml 의 Java 모델.
 */
@Data
public class ScoreCardRuleSet {
    private String ruleVersion;
    private String effectiveFrom;
    private Map<String, Dimension> dimensions;
    private List<Grade> grades;

    @Data
    public static class Dimension {
        private String label;
        private double weight;
        private String source;
        private List<Rule> rules;
        private Map<String, Double> eventScores;
    }

    @Data
    public static class Rule {
        private String name;
        private double max;
        private String type;            // threshold, bucket, linear, binary, percentile, log_scaled
        private String input;
        private Double scoreWhenTrue;   // for binary
        private Double slope;           // for linear
        private Double cap;             // for linear / log_scaled
        private List<Threshold> thresholds;
        private List<Bucket> buckets;
    }

    @Data
    public static class Threshold {
        private Double gte;
        private Double lte;
        private double score;
    }

    @Data
    public static class Bucket {
        private List<Double> range;   // [lowInclusive, highExclusive]
        private double score;
    }

    @Data
    public static class Grade {
        private double min;
        private String code;
        private String label;
        private String action;
    }
}
