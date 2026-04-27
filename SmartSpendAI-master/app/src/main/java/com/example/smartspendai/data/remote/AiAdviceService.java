package com.example.smartspendai.data.remote;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.example.smartspendai.BuildConfig;
import com.example.smartspendai.data.model.BudgetCategory;
import com.example.smartspendai.data.model.BudgetSummary;
import com.example.smartspendai.util.CurrencyFormatter;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;

import java.text.DateFormatSymbols;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiAdviceService {
    private static final String GEMINI_MODEL = BuildConfig.GEMINI_MODEL;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final GenerativeModelFutures model;

    public AiAdviceService() {
        GenerativeModel generativeModel = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel(GEMINI_MODEL);
        model = GenerativeModelFutures.from(generativeModel);
    }

    public void requestBudgetAdvice(BudgetSummary budgetSummary, String userQuestion, AdviceCallback callback) {
        try {
            Content prompt = new Content.Builder()
                    .addText(buildUserPrompt(budgetSummary, userQuestion))
                    .build();

            ListenableFuture<GenerateContentResponse> responseFuture = model.generateContent(prompt);
            Futures.addCallback(responseFuture, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String answer = result == null ? null : result.getText();
                    String finalAnswer = normalizeAnswer(answer, budgetSummary, userQuestion);
                    mainHandler.post(() -> callback.onSuccess(200, finalAnswer));
                }

                @Override
                public void onFailure(Throwable throwable) {
                    String message = throwable == null ? "Firebase AI request failed." : throwable.getMessage();
                    mainHandler.post(() -> callback.onError(message));
                }
            }, executorService);
        } catch (Exception exception) {
            mainHandler.post(() -> callback.onError(exception.getMessage()));
        }
    }

    private String buildUserPrompt(BudgetSummary budgetSummary, String userQuestion) {
        return "You are a helpful budgeting assistant for students. Always answer in complete sentences, never stop mid-sentence, and always include exact figures when the user asks about budget totals, spent amounts, income, allocated amounts, or remaining amounts. Use only the provided budget data. Keep the answer short but complete, in plain text without markdown.\n\n"
                + "Answer the user's budgeting question using this budget snapshot.\n\n"
                + buildBudgetSnapshot(budgetSummary)
                + "\nUser question: " + userQuestion
                + "\n\nIf the question asks for totals, spent amount, income, allocation, or remaining budget, answer with the exact value first and then one short explanation.";
    }

    private String buildBudgetSnapshot(BudgetSummary budgetSummary) {
        StringBuilder builder = new StringBuilder();
        builder.append("Month: ")
                .append(getMonthLabel(budgetSummary.getMonth()))
                .append(' ')
                .append(budgetSummary.getYear())
                .append("\nIncome: ")
                .append(CurrencyFormatter.format(budgetSummary.getIncome()))
                .append("\nTotal allocated: ")
                .append(CurrencyFormatter.format(budgetSummary.getTotalAllocated()))
                .append("\nTotal spent: ")
                .append(CurrencyFormatter.format(budgetSummary.getTotalSpent()))
                .append("\nUnallocated: ")
                .append(CurrencyFormatter.format(budgetSummary.getUnallocatedAmount()))
                .append("\nCategories:");

        for (BudgetCategory category : budgetSummary.getCategories()) {
            builder.append("\n- ")
                    .append(category.getName())
                    .append(": allocated ")
                    .append(CurrencyFormatter.format(category.getAllocatedAmount()))
                    .append(", spent ")
                    .append(CurrencyFormatter.format(category.getSpentAmount()))
                    .append(", remaining ")
                    .append(CurrencyFormatter.format(category.getRemainingAmount()));
        }
        return builder.toString();
    }

    private String normalizeAnswer(String answer, BudgetSummary budgetSummary, String userQuestion) {
        if (TextUtils.isEmpty(answer)) {
            return buildFallbackAnswer(budgetSummary, userQuestion);
        }

        String trimmed = answer.trim();
        if (trimmed.length() < 40 || looksIncomplete(trimmed)) {
            return buildFallbackAnswer(budgetSummary, userQuestion);
        }
        return trimmed;
    }

    private boolean looksIncomplete(String answer) {
        String trimmed = answer.trim();
        if (trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?")) {
            return false;
        }

        String lower = trimmed.toLowerCase(Locale.US);
        return lower.endsWith(" a")
                || lower.endsWith(" an")
                || lower.endsWith(" the")
                || lower.endsWith(" for")
                || lower.endsWith(" of")
                || lower.endsWith(" total")
                || lower.endsWith(" shows")
                || lower.endsWith(" is")
                || lower.endsWith(" are")
                || lower.endsWith(" april")
                || lower.endsWith(" this year");
    }

    private String buildFallbackAnswer(BudgetSummary budgetSummary, String userQuestion) {
        String question = userQuestion.toLowerCase(Locale.US);
        String monthYear = getMonthLabel(budgetSummary.getMonth()) + " " + budgetSummary.getYear();

        if (question.contains("how much") && question.contains("spent") && question.contains("year")) {
            return "This screen only has your budget data for " + monthYear + ", not the whole year. For " + monthYear + ", you have spent " + CurrencyFormatter.format(budgetSummary.getTotalSpent()) + " so far.";
        }
        if (question.contains("how much") && question.contains("spent")) {
            return "For " + monthYear + ", you have spent " + CurrencyFormatter.format(budgetSummary.getTotalSpent()) + " so far.";
        }
        if (question.contains("budget")) {
            return "For " + monthYear + ", your income is " + CurrencyFormatter.format(budgetSummary.getIncome()) + ", your total allocated budget is " + CurrencyFormatter.format(budgetSummary.getTotalAllocated()) + ", and your unallocated amount is " + CurrencyFormatter.format(budgetSummary.getUnallocatedAmount()) + ".";
        }
        if (question.contains("income")) {
            return "For " + monthYear + ", your income is " + CurrencyFormatter.format(budgetSummary.getIncome()) + ".";
        }
        if (question.contains("unallocated") || question.contains("remaining budget")) {
            return "For " + monthYear + ", your unallocated amount is " + CurrencyFormatter.format(budgetSummary.getUnallocatedAmount()) + ".";
        }
        if (question.contains("allocated")) {
            return "For " + monthYear + ", your total allocated amount is " + CurrencyFormatter.format(budgetSummary.getTotalAllocated()) + ".";
        }

        return "For " + monthYear + ", your income is " + CurrencyFormatter.format(budgetSummary.getIncome()) + ", your total allocated amount is " + CurrencyFormatter.format(budgetSummary.getTotalAllocated()) + ", and your total spent amount is " + CurrencyFormatter.format(budgetSummary.getTotalSpent()) + ".";
    }

    private String getMonthLabel(int month) {
        String[] monthNames = new DateFormatSymbols(Locale.US).getMonths();
        int monthIndex = Math.max(0, Math.min(month - 1, 11));
        return monthNames[monthIndex];
    }

    public interface AdviceCallback {
        void onSuccess(int statusCode, String advice);

        void onError(String errorMessage);
    }
}

