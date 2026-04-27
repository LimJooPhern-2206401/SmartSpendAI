package com.example.smartspendai.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class SpendingPieChartView extends View {
    private final Paint slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF chartBounds = new RectF();
    private final List<Slice> slices = new ArrayList<>();
    private String centerText = "RM 0.00";
    private double totalAmount = 0;

    public SpendingPieChartView(Context context) {
        super(context);
        init();
    }

    public SpendingPieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpendingPieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        centerPaint.setColor(Color.WHITE);
        textPaint.setColor(Color.rgb(31, 42, 41));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    public void setSlices(List<Slice> newSlices, String newCenterText) {
        slices.clear();
        totalAmount = 0;
        for (Slice slice : newSlices) {
            if (slice.amount > 0) {
                slices.add(slice);
                totalAmount += slice.amount;
            }
        }
        centerText = newCenterText;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height);
        float padding = size * 0.08f;
        float left = (width - size) / 2f + padding;
        float top = (height - size) / 2f + padding;
        float right = (width + size) / 2f - padding;
        float bottom = (height + size) / 2f - padding;
        chartBounds.set(left, top, right, bottom);

        if (slices.isEmpty() || totalAmount <= 0) {
            slicePaint.setColor(Color.rgb(215, 226, 223));
            canvas.drawArc(chartBounds, 0, 360, true, slicePaint);
            drawCenter(canvas, size, "No spending");
            return;
        }

        float startAngle = -90f;
        for (Slice slice : slices) {
            float sweepAngle = (float) ((slice.amount / totalAmount) * 360f);
            slicePaint.setColor(slice.color);
            canvas.drawArc(chartBounds, startAngle, sweepAngle, true, slicePaint);
            startAngle += sweepAngle;
        }

        drawCenter(canvas, size, centerText);
    }

    private void drawCenter(Canvas canvas, int size, String text) {
        float radius = size * 0.24f;
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, centerPaint);
        textPaint.setTextSize(Math.max(24f, size * 0.07f));
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float y = getHeight() / 2f - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(text, getWidth() / 2f, y, textPaint);
    }

    public static class Slice {
        private final double amount;
        private final int color;

        public Slice(double amount, int color) {
            this.amount = amount;
            this.color = color;
        }
    }
}
