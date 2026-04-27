package com.example.smartspendai.util;

import java.text.DecimalFormat;

public final class CurrencyFormatter {
    private static final DecimalFormat FORMAT = new DecimalFormat("'RM' #,##0.00");

    private CurrencyFormatter() {
    }

    public static String format(double amount) {
        return FORMAT.format(amount);
    }
}
