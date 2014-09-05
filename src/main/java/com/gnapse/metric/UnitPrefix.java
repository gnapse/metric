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

import static com.google.common.base.Preconditions.checkNotNull;

import com.gnapse.common.math.BigFraction;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * Enumeration representing the valid prefixes that alter a base unit.  It supports both SI prefixes
 * with base 10 and IEC binary prefixes of base 2.
 *
 * <p>Includes some utility methods to query and handle prefixes.</p>
 *
 * @see <a href="http://en.wikipedia.org/wiki/SI_prefix">SI prefix (Wikipedia)</a>
 * @see <a href="http://en.wikipedia.org/wiki/Binary_prefix">Binary prefix (Wikipedia)</a>
 * @author Ernesto García
 */
public enum UnitPrefix {

    //
    // SI Prefixes
    //

    /**
     * Denotes a factor of <code>10<sup>24</sup></code>.
     */
    YOTTA (BigFraction.TEN, +24, "Y"),

    /**
     * Denotes a factor of <code>10<sup>21</sup></code>.
     */
    ZETTA (BigFraction.TEN, +21, "Z"),

    /**
     * Denotes a factor of <code>10<sup>18</sup></code>.
     */
    EXA   (BigFraction.TEN, +18, "E"),

    /**
     * Denotes a factor of <code>10<sup>15</sup></code>.
     */
    PETA  (BigFraction.TEN, +15, "P"),

    /**
     * Denotes a factor of <code>10<sup>12</sup></code>.
     */
    TERA  (BigFraction.TEN, +12, "T"),

    /**
     * Denotes a factor of <code>10<sup>9</sup></code>.
     */
    GIGA  (BigFraction.TEN,  +9, "G"),

    /**
     * Denotes a factor of <code>10<sup>6</sup></code>.
     */
    MEGA  (BigFraction.TEN,  +6, "M"),

    /**
     * Denotes a factor of <code>10<sup>3</sup></code>.
     */
    KILO  (BigFraction.TEN,  +3, "k"),

    /**
     * Denotes a factor of <code>10<sup>2</sup></code>.
     */
    HECTO (BigFraction.TEN,  +2, "h"),

    /**
     * Denotes a factor of <code>10<sup>1</sup></code>.
     */
    DECA  (BigFraction.TEN,  +1, "da", "deka"),

    /**
     * Denotes a factor of <code>10<sup>-1</sup></code>.
     */
    DECI  (BigFraction.TEN,  -1, "d"),

    /**
     * Denotes a factor of <code>10<sup>-2</sup></code>.
     */
    CENTI (BigFraction.TEN,  -2, "c"),

    /**
     * Denotes a factor of <code>10<sup>-3</sup></code>.
     */
    MILLI (BigFraction.TEN,  -3, "m", "mili"),

    /**
     * Denotes a factor of <code>10<sup>-6</sup></code>.
     */
    MICRO (BigFraction.TEN,  -6, "u"),

    /**
     * Denotes a factor of <code>10<sup>-9</sup></code>.
     */
    NANO  (BigFraction.TEN,  -9, "n"),

    /**
     * Denotes a factor of <code>10<sup>-12</sup></code>.
     */
    PICO  (BigFraction.TEN, -12, "p"),

    /**
     * Denotes a factor of <code>10<sup>-15</sup></code>.
     */
    FEMTO (BigFraction.TEN, -15, "f"),

    /**
     * Denotes a factor of <code>10<sup>-18</sup></code>.
     */
    ATTO  (BigFraction.TEN, -18, "a"),

    /**
     * Denotes a factor of <code>10<sup>-21</sup></code>.
     */
    ZEPTO (BigFraction.TEN, -21, "z"),

    /**
     * Denotes a factor of <code>10<sup>-24</sup></code>.
     */
    YOCTO (BigFraction.TEN, -24, "y"),

    //
    // Binary prefixes
    //

    /**
     * Denotes a factor of <code>2<sup>10</sup></code>.
     */
    KIBI (BigFraction.TWO, 10, "Ki"),
    /**
     * Denotes a factor of <code>2<sup>30</sup></code>.
     */
    MEBI (BigFraction.TWO, 20, "Mi"),
    /**
     * Denotes a factor of <code>2<sup>30</sup></code>.
     */
    GIBI (BigFraction.TWO, 30, "Gi"),
    /**
     * Denotes a factor of <code>2<sup>40</sup></code>.
     */
    TEBI (BigFraction.TWO, 40, "Ti"),
    /**
     * Denotes a factor of <code>2<sup>50</sup></code>.
     */
    PEBI (BigFraction.TWO, 50, "Pi"),
    /**
     * Denotes a factor of <code>2<sup>60</sup></code>.
     */
    EXBI (BigFraction.TWO, 60, "Ei"),
    /**
     * Denotes a factor of <code>2<sup>70</sup></code>.
     */
    ZEBI (BigFraction.TWO, 70, "Zi"),
    /**
     * Denotes a factor of <code>2<sup>80</sup></code>.
     */
    YOBI (BigFraction.TWO, 80, "Yi");

    private final String shortName;

    private final BigFraction multiplier;

    private UnitPrefix(BigFraction base, int scale, String shortName, String... otherNames) {
        this.shortName = checkNotNull(shortName);
        this.multiplier = base.pow(scale);
        registerPrefix(this, otherNames);
    }

    private static Map<String, UnitPrefix> longPrefixes;

    private static Map<String, UnitPrefix> shortPrefixes;

    private static List<UnitPrefix> all;

    private static void registerPrefix(UnitPrefix p, String... otherNames) {
        if (longPrefixes == null) {
            longPrefixes = Maps.newHashMap();
        }
        if (shortPrefixes == null) {
            shortPrefixes = Maps.newHashMap();
        }
        longPrefixes.put(p.getLongName(), p);
        shortPrefixes.put(p.getShortName(), p);
        for (String n : otherNames) {
            longPrefixes.put(n, p);
        }
    }

    /**
     * An iterable collection of all the defined SI prefixes.
     */
    public static Iterable<UnitPrefix> all() {
        if (all == null) {
            all = ImmutableList.copyOf(UnitPrefix.values());
        }
        return all;
    }

    /**
     * The long name of this prefix
     */
    public String getLongName() {
        return name().toLowerCase();
    }

    /**
     * The short name of this prefix
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * The factor by which this prefix multiplies a unit.
     */
    public BigFraction getMultiplier() {
        return multiplier;
    }

    /**
     * Determines if the given prefix is valid or not.
     *
     * @param prefix the prefix to search for
     * @return {@code true} if the given argument is a valid prefix; {@code false} otherwise.
     */
    public static boolean isValidPrefix(String prefix) {
        return longPrefixes.containsKey(prefix) ||
                shortPrefixes.containsKey(prefix);
    }

    /**
     * Determines if the given prefix is a valid long prefix or not.
     *
     * @param prefix the prefix to search for
     * @return {@code true} if the given argument is a valid long prefix; {@code false} otherwise.
     */
    public static boolean isValidLongPrefix(String prefix) {
        return longPrefixes.containsKey(prefix);
    }

    /**
     * Determines if the given prefix is a valid short prefix or not.
     *
     * @param prefix the prefix to search for
     * @return {@code true} if the given argument is a valid short prefix; {@code false} otherwise.
     */
    public static boolean isValidShortPrefix(String prefix) {
        return shortPrefixes.containsKey(prefix);
    }

    /**
     * Returns the prefix with the given name, short or long.
     *
     * @param name the name of the prefix to search for
     * @return the prefix with the given name, or {@code null} if the name is not a valid prefix
     * name.
     */
    public static UnitPrefix get(String name) {
        final UnitPrefix prefix = longPrefixes.get(name);
        return (prefix != null) ? prefix : shortPrefixes.get(name);
    }

    /**
     * Returns the prefix with the given long name.
     *
     * @param longName the long name of the prefix to search for
     * @return the prefix with the given long name, or {@code null} if the name is not a valid long
     *     prefix name.
     */
    public static UnitPrefix getLong(String longName) {
        return longPrefixes.get(longName);
    }

    /**
     * Returns the prefix with the given short name.
     *
     * @param shortName the short name of the prefix to search for
     * @return the prefix with the given short name, or {@code null} if the name is not a valid
     *     short prefix name.
     */
    public static UnitPrefix getShort(String shortName) {
        return shortPrefixes.get(shortName);
    }

    /**
     * Extracts the prefix from the given unit name.  If the unit name is not prefixed by any of the
     * valid SI prefixes, then {@code null} is returned.
     *
     * @param unitName the name of the unit from which to extract the prefix
     * @return a valid SI prefix with which the given unit name starts, or {@code null} if no prefix
     * was found starting the given unit name.
     */
    public static UnitPrefix getPrefixFromUnitName(String unitName) {
        for (UnitPrefix prefix : all()) {
            if (unitName.startsWith(prefix.getLongName()))
                return prefix;
        }
        for (UnitPrefix prefix : all()) {
            if (unitName.startsWith(prefix.getShortName()))
                return prefix;
        }
        return null;
    }

    /**
     * Extracts the long prefix from the given unit name.  If the unit name is not prefixed by any
     * of the valid long SI prefixes, then {@code null} is returned.
     *
     * @param unitName the name of the unit to extract the long prefix from
     * @return a valid long SI prefix with which the given unit name starts, or {@code null} if no
     * long prefix was found starting the given unit name.
     */
    public static UnitPrefix getLongPrefixFromUnitName(String unitName) {
        for (UnitPrefix prefix : all()) {
            if (unitName.startsWith(prefix.getLongName()))
                return prefix;
        }
        return null;
    }

    /**
     * Extracts the short prefix from the given unit name.  If the unit name is not prefixed by any
     * of the valid short SI prefixes, then {@code null} is returned.
     *
     * @param unitName the name of the unit to extract the short prefix from
     * @return a valid short SI prefix with which the given unit name starts, or {@code null} if no
     * short prefix was found starting the given unit name.
     */
    public static UnitPrefix getShortPrefixFromUnitName(String unitName) {
        for (UnitPrefix prefix : all()) {
            if (unitName.startsWith(prefix.getShortName()))
                return prefix;
        }
        return null;
    }

}
