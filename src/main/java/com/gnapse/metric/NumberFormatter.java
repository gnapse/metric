/*
 * Copyright (C) 2012 Gnapse.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gnapse.metric;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Strings;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formats numeric values using different settings according to the magnitude of the number being
 * formatted.
 *
 * <p>For this purpose, this class partitions the set of <em>positive</em> real numbers into three
 * subsets, using two threshold values, {@linkplain #getLargeThreshold() large} and
 * {@linkplain #getSmallThreshold() small}.  Numbers whose absolute value is larger than or equal to
 * the large threshold are formatted using the {@linkplain #getLargeFormat() large number apply};
 * numbers whose absolute value is smaller than or equal to the small threshold are formatted using
 * the {@linkplain #getSmallFormat() small number apply}.  All other numbers laying in between the
 * thresholds are formatted using the {@linkplain #getDefaultFormat() default number apply}.</p>
 *
 * <p>Note that the thresholds partition only the set of positive real numbers, so to determine the
 * magnitude of a number and the formatter to use for it, the sign of the number is disregarded.</p>
 *
 * <p>The intuitive purpose of this class is to avoid printing out numeric values such as
 * {@code 0.0000000000000000021} or {@code 6241509479607718382.942} which are better expressed as
 * {@code 2.1e-18} and {@code 6.241509479607718382942e18} respectively, giving a better sense of
 * their magnitude.</p>
 *
 * @author Ernesto García
 */
public final class NumberFormatter implements Function<BigDecimal, String> {

    private final Locale locale;

    private DecimalFormat defaultFormat;

    private DecimalFormat smallFormat;

    private DecimalFormat largeFormat;

    private BigDecimal smallThreshold = new BigDecimal("1e-6");

    private BigDecimal largeThreshold = new BigDecimal("1e+12");

    //
    // Public API
    //

    /**
     * Creates a new number formatter that follows the number formatting rules of the
     * {@link Locale#US Locale.US} locale.
     */
    public NumberFormatter() {
        this(Locale.US);
    }

    /**
     * Creates a new number formatter that follows the number formatting rules of the given locale.
     * @param locale the locale that rules number formatting for this formatter
     */
    public NumberFormatter(Locale locale) {
        this.locale = checkNotNull(locale);
        defaultFormat = createPlainFormat();
        smallFormat = largeFormat = createExponentFormat();
    }

    /**
     * Returns the {@link DecimalFormat} object used to apply the given number according to its
     * value.
     */
    public DecimalFormat getFormat(BigDecimal number) {
        number = number.abs();
        if (number.compareTo(smallThreshold) <= 0 && !number.equals(BigDecimal.ZERO)) {
            return smallFormat;
        }
        if (number.compareTo(largeThreshold) >= 0) {
            return largeFormat;
        }
        return defaultFormat;
    }

    /**
     * Formats the given number using the {@linkplain DecimalFormat apply} corresponding to its
     * value.
     *
     * @param number the number to apply
     * @return a string representation of the number
     */
    @Override
    public String apply(BigDecimal number) {
        return getFormat(number).format(number);
    }

    //
    // Threshold getters and setters
    //

    /**
     * The threshold for small numbers in this formatter.  Numbers smaller than this threshold will
     * be considered <em>small</em>, and will be formatted according to the
     * {@linkplain #getSmallFormat() small numbers apply}.
     *
     * @return the threshold for numbers to be considered small by this formatter
     */
    public BigDecimal getSmallThreshold() {
        return smallThreshold;
    }

    /**
     * The threshold for large numbers in this formatter.  Numbers larger than this threshold will
     * be considered <em>large</em>, and will be formatted according to the
     * {@linkplain #getLargeFormat() large numbers apply}.
     *
     * @return the threshold for numbers to be considered large by this formatter
     */
    public BigDecimal getLargeThreshold() {
        return largeThreshold;
    }

    /**
     * Sets the threshold for small numbers in this formatter.  Numbers smaller than this threshold
     * will be considered <em>small</em>, and will be formatted according to the
     * {@linkplain #getSmallFormat() small numbers apply}.
     *
     * @param smallThreshold the new small threshold.
     * @return this number formatter
     * @throws IllegalArgumentException if the new small threshold is not a positive number, or if
     *     it's larger than or equal to the large threshold.
     */
    public NumberFormatter setSmallThreshold(BigDecimal smallThreshold) {
        checkArgument(smallThreshold.compareTo(BigDecimal.ZERO) > 0); // small > 0
        checkArgument(smallThreshold.compareTo(largeThreshold) < 0);  // small < large
        this.smallThreshold = smallThreshold;
        return this;
    }

    /**
     * Sets the threshold for large numbers in this formatter.  Numbers larger than this threshold
     * will be considered <em>large</em>, and will be formatted according to the
     * {@linkplain #getLargeFormat() large numbers apply}.
     *
     * @param largeThreshold the new large threshold.
     * @return this number formatter
     * @throws IllegalArgumentException if the new large threshold is smaller than or equal to the
     *     small threshold.
     */
    public NumberFormatter setLargeThreshold(BigDecimal largeThreshold) {
        checkArgument(largeThreshold.compareTo(smallThreshold) > 0); // large > small
        this.largeThreshold = largeThreshold;
        return this;
    }

    //
    // Format getters and setters
    //

    /**
     * The number apply used by this formatter to convert <em>normal</em> numbers to a string
     * representation.
     */
    public DecimalFormat getDefaultFormat() {
        return defaultFormat;
    }

    /**
     * The number apply used by this formatter to convert <em>small</em> numbers to a string
     * representation.
     */
    public DecimalFormat getSmallFormat() {
        return smallFormat;
    }

    /**
     * The number apply used by this formatter to convert <em>large</em> numbers to a string
     * representation.
     */
    public DecimalFormat getLargeFormat() {
        return largeFormat;
    }

    /**
     * Sets the default number apply, to be used by this formatter to convert <em>normal</em>
     * numbers to a string representation.
     *
     * @param defaultFormat the new default number apply
     * @return this number formatter
     */
    public NumberFormatter setDefaultFormat(DecimalFormat defaultFormat) {
        this.defaultFormat = checkNotNull(defaultFormat);
        return this;
    }

    /**
     * Sets the small number apply, to be used by this formatter to convert <em>small</em> numbers
     * to a string representation.
     *
     * @param smallFormat the new small number apply
     * @return this number formatter
     */
    public NumberFormatter setSmallFormat(DecimalFormat smallFormat) {
        this.smallFormat = checkNotNull(smallFormat);
        return this;
    }

    /**
     * Sets the large number apply, to be used by this formatter to convert <em>large</em> numbers
     * to a string representation.
     *
     * @param largeFormat the new large number apply
     * @return this number formatter
     */
    public NumberFormatter setLargeFormat(DecimalFormat largeFormat) {
        this.largeFormat = checkNotNull(largeFormat);
        return this;
    }

    //
    // Helper methods
    //

    private static final char DEFAULT_GROUP_SEPARATOR = ' ';

    private static final String DEFAULT_EXPONENT_SEPARATOR = "e";

    private static final int MAX_FRACTION_DIGITS = 20;

    private DecimalFormat createPlainFormat() {
        final DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(this.locale);
        symbols.setGroupingSeparator(DEFAULT_GROUP_SEPARATOR);
        final DecimalFormat format = (DecimalFormat)NumberFormat.getNumberInstance(this.locale);
        format.setDecimalFormatSymbols(symbols);
        format.setMaximumFractionDigits(MAX_FRACTION_DIGITS);
        return format;
    }

    private DecimalFormat createExponentFormat() {
        final DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(this.locale);
        symbols.setExponentSeparator(DEFAULT_EXPONENT_SEPARATOR);
        final DecimalFormat format = (DecimalFormat)NumberFormat.getNumberInstance(this.locale);
        final String pattern = String.format("#.%sE0#", Strings.repeat("#", MAX_FRACTION_DIGITS));
        format.setDecimalFormatSymbols(symbols);
        format.applyPattern(pattern);
        return format;
    }

}
