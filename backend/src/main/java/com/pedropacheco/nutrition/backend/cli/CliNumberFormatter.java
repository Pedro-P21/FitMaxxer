package com.pedropacheco.nutrition.backend.cli;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Formats displayed measurements as whole numbers without changing stored values. */
final class CliNumberFormatter {
    private CliNumberFormatter() {}

    static String whole(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    static String whole(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
