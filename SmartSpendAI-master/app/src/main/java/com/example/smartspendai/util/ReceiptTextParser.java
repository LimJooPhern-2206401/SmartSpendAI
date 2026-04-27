package com.example.smartspendai.util;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReceiptTextParser {
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:rm|myr|usd|sgd|\\$)?\\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})|[0-9]+(?:[.,][0-9]{2}))"
    );
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?i)\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}|\\d{1,2}\\s+[A-Za-z]{3,9}\\s+\\d{2,4})\\b"
    );

    private ReceiptTextParser() {
    }

    public static ReceiptScanResult parse(String rawText) {
        if (TextUtils.isEmpty(rawText)) {
            return ReceiptScanResult.empty();
        }

        List<String> lines = getMeaningfulLines(rawText);
        String merchantName = detectMerchantName(lines);
        String detectedDate = detectDate(rawText);
        List<AmountCandidate> amountCandidates = detectAmountCandidates(lines);
        AmountCandidate bestAmount = chooseBestAmount(amountCandidates);
        String suggestedCategory = suggestCategory(merchantName, rawText);

        List<String> candidateLabels = new ArrayList<>();
        for (AmountCandidate candidate : amountCandidates) {
            String label = String.format(Locale.US, "%.2f", candidate.value);
            if (!candidateLabels.contains(label)) {
                candidateLabels.add(label);
            }
            if (candidateLabels.size() == 3) {
                break;
            }
        }

        return new ReceiptScanResult(
                merchantName,
                bestAmount == null ? null : bestAmount.value,
                detectedDate,
                bestAmount == null ? "" : bestAmount.line,
                suggestedCategory,
                candidateLabels
        );
    }

    private static List<String> getMeaningfulLines(String rawText) {
        String[] rawLines = rawText.split("\\r?\\n");
        List<String> lines = new ArrayList<>();
        for (String rawLine : rawLines) {
            String line = normalizeWhitespace(rawLine);
            if (!TextUtils.isEmpty(line)) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static String detectMerchantName(List<String> lines) {
        for (String line : lines) {
            String normalizedLine = line.trim();
            if (normalizedLine.length() < 3 || normalizedLine.length() > 36) {
                continue;
            }
            if (!normalizedLine.matches(".*[A-Za-z].*")) {
                continue;
            }

            String lower = normalizedLine.toLowerCase(Locale.US);
            if (lower.contains("receipt")
                    || lower.contains("tax invoice")
                    || lower.contains("subtotal")
                    || lower.contains("total")
                    || lower.contains("cash")
                    || lower.contains("change")
                    || lower.contains("qty")
                    || lower.contains("table")
                    || lower.contains("thank")
                    || lower.contains("date")
                    || lower.contains("time")
                    || lower.contains("invoice")
                    || lower.contains("tel")
                    || lower.contains("sst")) {
                continue;
            }
            return normalizedLine;
        }
        return "";
    }

    private static String detectDate(String rawText) {
        Matcher matcher = DATE_PATTERN.matcher(rawText);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static List<AmountCandidate> detectAmountCandidates(List<String> lines) {
        List<AmountCandidate> candidates = new ArrayList<>();
        for (String line : lines) {
            String lowerLine = line.toLowerCase(Locale.US);
            Matcher matcher = AMOUNT_PATTERN.matcher(line);
            while (matcher.find()) {
                double value = parseAmountToken(matcher.group(1));
                if (value <= 0 || value > 100000) {
                    continue;
                }

                int score = (int) Math.round(value);
                if (lowerLine.contains("grand total")
                        || lowerLine.contains("net total")
                        || lowerLine.contains("amount due")
                        || lowerLine.contains("balance due")) {
                    score += 120;
                } else if (lowerLine.contains("total")) {
                    score += 90;
                } else if (lowerLine.contains("subtotal")) {
                    score += 50;
                }

                if (lowerLine.contains("tax")
                        || lowerLine.contains("rounding")
                        || lowerLine.contains("service")
                        || lowerLine.contains("change")
                        || lowerLine.contains("cash")
                        || lowerLine.contains("discount")
                        || lowerLine.contains("qty")) {
                    score -= 45;
                }

                if (line.contains("RM") || line.contains("rm") || line.contains("$")) {
                    score += 18;
                }

                if (hasReceiptTotalShape(line, matcher.group(1))) {
                    score += 16;
                }

                candidates.add(new AmountCandidate(value, line, score));
            }
        }

        candidates.sort((left, right) -> Integer.compare(right.score, left.score));
        return candidates;
    }

    private static boolean hasReceiptTotalShape(String line, String token) {
        int numberIndex = line.indexOf(token);
        return numberIndex > 0 && numberIndex >= line.length() / 3;
    }

    private static AmountCandidate chooseBestAmount(List<AmountCandidate> candidates) {
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static double parseAmountToken(String token) {
        String sanitized = token.replaceAll("[^0-9,\\.]", "");
        if (TextUtils.isEmpty(sanitized)) {
            return -1;
        }

        int lastComma = sanitized.lastIndexOf(',');
        int lastDot = sanitized.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                sanitized = sanitized.replace(".", "");
                sanitized = sanitized.replace(',', '.');
            } else {
                sanitized = sanitized.replace(",", "");
            }
        } else if (lastComma >= 0) {
            sanitized = sanitized.replace(',', '.');
        }

        try {
            return Double.parseDouble(sanitized);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static String suggestCategory(String merchantName, String rawText) {
        String context = (merchantName + " " + rawText).toLowerCase(Locale.US);
        if (containsAny(context, "restaurant", "cafe", "coffee", "food", "mart", "grocery", "bakery")) {
            return "Food";
        }
        if (containsAny(context, "grab", "petrol", "fuel", "parking", "toll", "bus", "train", "taxi")) {
            return "Transport";
        }
        if (containsAny(context, "electric", "water", "internet", "wifi", "phone", "utility", "bill", "tenaga")) {
            return "Bills";
        }
        if (containsAny(context, "cinema", "movie", "netflix", "spotify", "game", "karaoke")) {
            return "Entertainment";
        }
        if (containsAny(context, "book", "school", "college", "university", "course", "tuition")) {
            return "Education";
        }
        if (containsAny(context, "saving", "deposit", "investment")) {
            return "Savings";
        }
        return "Others";
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeWhitespace(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }

    public static final class ReceiptScanResult {
        private final String merchantName;
        private final Double detectedAmount;
        private final String detectedDate;
        private final String matchedLine;
        private final String suggestedCategory;
        private final List<String> candidateAmounts;

        private ReceiptScanResult(
                String merchantName,
                Double detectedAmount,
                String detectedDate,
                String matchedLine,
                String suggestedCategory,
                List<String> candidateAmounts
        ) {
            this.merchantName = merchantName == null ? "" : merchantName;
            this.detectedAmount = detectedAmount;
            this.detectedDate = detectedDate == null ? "" : detectedDate;
            this.matchedLine = matchedLine == null ? "" : matchedLine;
            this.suggestedCategory = suggestedCategory == null ? "" : suggestedCategory;
            this.candidateAmounts = Collections.unmodifiableList(new ArrayList<>(candidateAmounts));
        }

        public static ReceiptScanResult empty() {
            return new ReceiptScanResult("", null, "", "", "", new ArrayList<>());
        }

        public String getMerchantName() {
            return merchantName;
        }

        public Double getDetectedAmount() {
            return detectedAmount;
        }

        public String getDetectedDate() {
            return detectedDate;
        }

        public String getMatchedLine() {
            return matchedLine;
        }

        public String getSuggestedCategory() {
            return suggestedCategory;
        }

        public List<String> getCandidateAmounts() {
            return candidateAmounts;
        }

        public boolean hasDetectedAmount() {
            return detectedAmount != null && detectedAmount > 0;
        }

        public boolean hasAnySignal() {
            return hasDetectedAmount()
                    || !TextUtils.isEmpty(merchantName)
                    || !TextUtils.isEmpty(detectedDate)
                    || !candidateAmounts.isEmpty();
        }
    }

    private static final class AmountCandidate {
        private final double value;
        private final String line;
        private final int score;

        private AmountCandidate(double value, String line, int score) {
            this.value = value;
            this.line = line;
            this.score = score;
        }
    }
}
