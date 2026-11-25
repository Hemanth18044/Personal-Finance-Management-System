package com.finance.util;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.sql.Date;
import java.util.Locale;

/**
 * Utility for formatting currency and date values across Swing views.
 */
public class CurrencyFormatter {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    public static String formatDate(Date date) {
        if (date == null) return "";
        return DATE_FORMAT.format(date);
    }

    public static Date parseDate(String dateStr) {
        try {
            java.util.Date parsed = DATE_FORMAT.parse(dateStr);
            return new Date(parsed.getTime());
        } catch (Exception e) {
            return new Date(System.currentTimeMillis());
        }
    }
}
