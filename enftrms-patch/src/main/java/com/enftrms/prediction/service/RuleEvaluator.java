package com.enftrms.prediction.service;

import com.enftrms.prediction.domain.ScoreCardRuleSet.Rule;
import com.enftrms.prediction.domain.ScoreCardRuleSet.Bucket;
import com.enftrms.prediction.domain.ScoreCardRuleSet.Threshold;

/**
 * 단일 룰 평가. 입력값(double 또는 boolean) → 점수.
 * 룰 타입: threshold, bucket, linear, binary, percentile, log_scaled
 */
public final class RuleEvaluator {

    private RuleEvaluator() {}

    public static double evaluate(Rule rule, Object input) {
        if (input == null) return 0d;
        String type = rule.getType() == null ? "threshold" : rule.getType();
        switch (type) {
            case "binary":      return evalBinary(rule, input);
            case "threshold":   return evalThreshold(rule, toDouble(input));
            case "bucket":      return evalBucket(rule, toDouble(input));
            case "linear":      return evalLinear(rule, toDouble(input));
            case "percentile":  return evalPercentile(rule, toDouble(input));
            case "log_scaled":  return evalLogScaled(rule, toDouble(input));
            default:            return 0d;
        }
    }

    private static double evalBinary(Rule r, Object input) {
        boolean v;
        if (input instanceof Boolean) v = (Boolean) input;
        else if (input instanceof Number) v = ((Number) input).intValue() != 0;
        else v = Boolean.parseBoolean(input.toString());
        return v ? (r.getScoreWhenTrue() != null ? r.getScoreWhenTrue() : r.getMax()) : 0d;
    }

    private static double evalThreshold(Rule r, double v) {
        if (r.getThresholds() == null) return 0d;
        for (Threshold t : r.getThresholds()) {
            if (t.getGte() != null && v >= t.getGte()) return cap(t.getScore(), r.getMax());
            if (t.getLte() != null && v <= t.getLte()) return cap(t.getScore(), r.getMax());
        }
        return 0d;
    }

    private static double evalBucket(Rule r, double v) {
        if (r.getBuckets() == null) return 0d;
        for (Bucket b : r.getBuckets()) {
            if (b.getRange() == null || b.getRange().size() < 2) continue;
            double lo = b.getRange().get(0), hi = b.getRange().get(1);
            if (v >= lo && v < hi) return cap(b.getScore(), r.getMax());
        }
        return 0d;
    }

    private static double evalLinear(Rule r, double v) {
        double slope = r.getSlope() != null ? r.getSlope() : 1.0;
        double raw   = Math.max(0d, v) * slope;
        return cap(raw, r.getCap() != null ? r.getCap() : r.getMax());
    }

    /** input 은 0.0 ~ 1.0 percentile rank */
    private static double evalPercentile(Rule r, double v) {
        double clipped = Math.max(0d, Math.min(1d, v));
        return cap(clipped * r.getMax(), r.getMax());
    }

    private static double evalLogScaled(Rule r, double v) {
        if (v <= 0) return 0d;
        double raw = Math.log10(v + 1d);
        return cap(raw, r.getCap() != null ? r.getCap() : r.getMax());
    }

    private static double cap(double v, double max) { return Math.min(v, max); }

    private static double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        if (o instanceof Boolean) return ((Boolean) o) ? 1d : 0d;
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0d; }
    }
}
