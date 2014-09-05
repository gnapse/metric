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

import static com.gnapse.common.inflector.Inflectors.pluralOf;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.gnapse.common.math.BigFraction;
import com.gnapse.common.math.Factorization;
import com.gnapse.common.math.Factorization.Factor;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Represents the concept of a <a href="http://en.wikipedia.org/wiki/Units_of_measurement">unit of
 * measurement</a>, used to express magnitudes of some {@linkplain Property physical property}
 * measured in a {@linkplain Universe universe}.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Units_of_measurement">Units of measurement
 *     (Wikipedia)</a>
 * @author Ernesto García
 */
public final class Unit implements Comparable<Unit> {

    private String longName, shortName;

    private final Property property;

    private final BigFraction multiplier, offset;

    private Set<String> names = Collections.emptySet();

    private final Factorization<Unit> factors;

    private final UnitPrefix prefix;

    //
    // Constructors
    //

    /**
     * Creates a new non-derived unit.
     *
     * @param property the property measured by the new unit
     * @param longNames the list of long names of the new unit
     * @param shortNames the list of short names of the new unit
     * @param baseUnit the base unit
     * @param prefix the SI prefix to apply to this unit relative to the base unit
     * @param multiplier the multiplier used to convert to this unit from its base
     * @param offset the offset used to convert to this unit from its base
     */
    private Unit(Property property, List<String> longNames, List<String> shortNames,
            Unit baseUnit, UnitPrefix prefix, BigFraction multiplier, BigFraction offset)
            throws MetricException {

        checkArgument(prefix == null || (baseUnit != null && baseUnit.prefix == null));
        checkArgument(prefix == null || multiplier == null);
        checkArgument(prefix == null || offset == null);

        if (prefix == null) {
            if (multiplier == null) multiplier = BigFraction.ONE;
            if (offset == null) offset = BigFraction.ZERO;
        } else {
            multiplier = prefix.getMultiplier();
            offset = BigFraction.ZERO;
        }

        checkArgument(!multiplier.equals(BigFraction.ZERO));

        this.property = checkNotNull(property);
        this.prefix = prefix;
        this.factors = null;
        processNames(longNames, shortNames);

        if (baseUnit != null) {
            if (baseUnit.getProperty() != property) {
                throw MetricException.incompatibleBaseUnit(this, baseUnit);
            }
            this.offset = offset.add(multiplier.multiply(baseUnit.offset));
            this.multiplier = multiplier.multiply(baseUnit.multiplier);
        } else {
            this.multiplier = BigFraction.ONE;
            this.offset = BigFraction.ZERO;
        }
    }

    /**
     * Creates a new non-derived unit with no prefix applied.
     *
     * @param property the property measured by the new unit
     * @param longNames the list of long names of the new unit
     * @param shortNames the list of short names of the new unit
     * @param baseUnit the base unit
     * @param multiplier the multiplier used to convert to this unit from its base
     * @param offset the offset used to convert to this unit from its base
     */
    Unit(Property property, List<String> longNames, List<String> shortNames,
            Unit baseUnit, BigFraction multiplier, BigFraction offset) throws MetricException {
        this(property, longNames, shortNames, baseUnit, null, multiplier, offset);
    }

    /**
     * Creates a new non-derived unit which is the base unit of its property.
     *
     * @param property the property measured by the new unit
     * @param longNames the list of long names of the new unit
     * @param shortName the list of short names of the new unit
     */
    Unit(Property property, List<String> longNames, List<String> shortNames)
            throws MetricException {
        this(property, longNames, shortNames, null, null, null, null);
    }

    /**
     * Creates a non-derived unit obtained by applying a prefix to an existing unit.
     *
     * @param baseUnit the base unit to which the prefix will be applied to obtain the new unit
     * @param prefix the prefix to apply to the base unit to obtain the new unit
     * @param longNames the list of long names of the new unit, without the prefix applied
     * @param shortName the list of short names of the new unit, without the prefix applied
     */
    Unit(Unit baseUnit, UnitPrefix prefix, List<String> longNames, List<String> shortNames)
            throws MetricException {
        this(baseUnit.property, longNames, shortNames, baseUnit, prefix, null, null);
    }

    /**
     * Creates a new derived unit.
     *
     * @param property the property measured by the new unit
     * @param factors the unit factors that are combined to derived this new unit
     * @param isBase if the new unit is the base unit of its property
     */
    Unit(Property property, Factorization<Unit> factors, boolean isBase) {
        factors = reduceFactors(factors);
        checkArgument(!factors.isEmpty());
        checkArgument(!factors.isSingleItem());

        Factorization<Property> fp = factors.transformItems(Property.getUnitPropertyFn);
        fp = Property.reduceFactors(fp);
        checkArgument((fp.isSingleItem() && property.isFundamental()) ||
                fp.equals(property.getDimensions()));

        this.property = checkNotNull(property);
        this.longName = null;
        this.shortName = null;
        this.offset = BigFraction.ZERO;
        this.multiplier = combineMultipliers(factors);
        this.factors = factors;
        this.prefix = null;
    }

    /**
     * Creates an invalid unit, one that is derived by a combination of units that does not map to
     * any known property declared in this universe.
     */
    Unit(Factorization<Unit> factors) {
        checkArgument(!factors.isEmpty());
        checkArgument(!factors.isSingleItem());

        this.factors = factors;
        this.property = null;
        this.longName = null;
        this.shortName = null;
        this.offset = null;
        this.multiplier = null;
        this.prefix = null;
    }

    private static Factorization<Unit> reduceFactors(Factorization<Unit> f) {
        Factorization<Unit> result = Factorization.empty();
        for (Factor<Unit> factor : f.factors()) {
            Unit unit = factor.getItem();
            if (unit.isDerived()) {
                result = result.multiply(unit.getFactors().pow(factor.getExponent()));
            } else {
                result = result.multiply(factor);
            }
        }
        return result;
    }

    private void processNames(List<String> longNames, List<String> shortNames) {
        this.names = Sets.newLinkedHashSet();
        this.longName = Iterables.getFirst(longNames, null);
        this.shortName = Iterables.getFirst(shortNames, null);

        if (prefix == null) {
            for (String name : longNames) {
                names.add(name);
                names.add(pluralOf(name));
                if (name.startsWith("degree ")) {
                    names.add(name.substring(7));
                }
            }
            for (String name : shortNames) {
                names.add(name);
            }
        } else {
            if (longName != null) {
                longName = prefix.getLongName() + longName;
            }
            if (shortName != null) {
                shortName = prefix.getShortName() + shortName;
            }
            for (String name : longNames) {
                names.add(prefix.getLongName() + name);
                names.add(prefix.getLongName() + pluralOf(name));
            }
            for (String name : shortNames) {
                names.add(prefix.getShortName() + name);
            }
        }

        this.names = Collections.unmodifiableSet(names);
    }

    //
    // Getters and setters
    //

    /**
     * The short (abbreviated) name of this unit.  If there's no short name, the long name is
     * returned.  If there's no long name, the factors expression is returned, or else {@code null}.
     */
    public String getShortName() {
        final String name = (shortName != null) ? shortName : longName;
        if (name == null) {
            return (factors == null) ? null : factors.toFractionString(toStringShortName);
        }
        return name;
    }

    /**
     * The long (standard) name of this unit.  If there's no long name, the short name is returned.
     * If there's no short name, the factors expression is returned, or else {@code null}.
     */
    public String getLongName() {
        final String name = (longName != null) ? longName : shortName;
        if (name == null) {
            return (factors == null) ? null : factors.toFractionString(toStringShortName);
        }
        return name;
    }

    /**
     * The long name of this unit, in plural form.  If there's no long name, the short name is
     * returned, not pluralized.  If there's no short name then the factors expression is returned,
     * or else {@code null}.
     */
    public String getPluralName() {
        final String name = (longName != null) ? pluralOf(longName) : shortName;
        if (name == null) {
            return (factors == null) ? null : factors.toString();
        }
        return name;
    }

    /**
     * A set of all the names of this unit.
     */
    public Set<String> getNames() {
        return names;
    }

    /**
     * The coefficient used to convert a quantity of this unit to its base unit.
     */
    public BigFraction getMultiplier() {
        return multiplier;
    }

    /**
     * The offset of this unit with respect to its base unit.
     */
    public BigFraction getOffset() {
        return offset;
    }

    /**
     * The universe of the property expressed by this unit.
     */
    public Universe getUniverse() {
        if (property != null) {
            return property.getUniverse();
        } else {
            /*
             * This is an invalid unit (i.e. corresponding to no declared property) so it is
             * guaranteed to consist of a non-empty factorization, so we extract the universe info
             * from any of the units in the factorization.
             */
            return Iterables.getFirst(factors.items(), null).getUniverse();
        }
    }

    /**
     * The property expressed by this unit.  It cannot normally be {@code null}.  If it is, it means
     * that this is an invalid unit, one derived by a combination of units that does not correspond
     * to any valid property in the universe.
     */
    public Property getProperty() {
        return property;
    }

    /**
     * The base unit of this unit, which is always the {@linkplain Property#getBaseUnit() base unit
     * of its property}.
     *
     * <p>If this unit is the base unit of its property, then this method returns {@code this}. If
     * this unit is {@linkplain #isValid() invalid} then this method returns {@code null}.</p>
     */
    public Unit getBaseUnit() {
        return (property == null) ? null : property.getBaseUnit();
    }

    /**
     * Determines if this is the base unit of its property.
     */
    public boolean isBase() {
        return (property != null) && (property.getBaseUnit() == this);
    }

    /**
     * Determines if this unit conversion parameters include an offset different from zero.
     */
    public boolean hasOffset() {
        return offset != null && !offset.equals(BigFraction.ZERO);
    }

    /**
     * Determines if this unit is derived by expressing it in terms of other units.
     */
    public boolean isDerived() {
        return factors != null;
    }

    /**
     * Determines if this is an valid unit, i.e.&nbsp;one that corresponds to a known property.
     */
    public boolean isValid() {
        return property != null;
    }

    /**
     * The factors composing this unit, or {@code null} if the unit is not derived.
     */
    public Factorization<Unit> getFactors() {
        return factors;
    }

    /**
     * The dimensions of this unit, which are the dimensions of its property.  It can only be
     * {@code null} if this unit is an {@linkplain #isValid() invalid} unit.
     */
    public Factorization<Property> getDimensions() {
        return (property == null) ? null : property.getDimensions();
    }

    /**
     * Determines if this unit is compatible with the other unit, meaning that each unit is able to
     * be converted to the other.
     */
    public boolean isCompatibleWith(Unit otherUnit) {
        return isValid() && otherUnit.isValid() &&
                getDimensions().equals(otherUnit.getDimensions());
    }

    //
    // Conversion methods
    //

    /**
     * Converts the value expressed in this unit, to the value expressed in the given unit.
     *
     * @param unit the unit to which the value will be converted
     * @param value the value to convert
     * @return the value converted to the given unit
     * @throws MetricException if this unit is not {@linkplain #isCompatibleWith(Unit) compatible}
     *     with the given unit
     * @throws NullPointerException if either argument is {@code null}
     */
    public BigFraction convertTo(Unit unit, BigFraction value) throws MetricException {
        checkNotNull(value);
        checkUnits(this, unit);

        if (unit == this) {
            return value;
        }

        if (this.isBase()) {
            return unit.convertFromBaseUnit(value);
        }

        BigFraction baseValue = this.convertToBaseUnit(value);

        if (unit.isBase()) {
            return baseValue;
        }

        return unit.convertFromBaseUnit(baseValue);
    }

    /**
     * Converts the value expressed in the given unit, to the value expressed in this unit.
     *
     * @param unit the unit from which the value will be converted
     * @param value the value to convert
     * @return the value converted to this unit
     * @throws MetricException if this unit is not {@linkplain #isCompatibleWith(Unit) compatible}
     *     with the given unit
     * @throws NullPointerException if either argument is {@code null}
     */
    public BigFraction convertFrom(Unit unit, BigFraction value) throws MetricException {
        checkNotNull(value);
        checkUnits(unit, this);

        if (unit == this) {
            return value;
        }

        if (this.isBase()) {
            return unit.convertToBaseUnit(value);
        }

        BigFraction baseValue = this.convertFromBaseUnit(value);

        if (unit.isBase()) {
            return baseValue;
        }

        return unit.convertToBaseUnit(baseValue);
    }

    /**
     * Converts the value expressed in this unit, to the value expressed in the
     * {@linkplain #getBaseUnit() base unit}.
     *
     * @param value the value to convert
     * @return the value converted to the base unit
     * @throws NullPointerException if the given value is {@code null}
     */
    public BigFraction convertToBaseUnit(BigFraction value) {
        if (isBase()) {
            return checkNotNull(value);
        } else {
            return value.multiply(multiplier).add(offset);
        }
    }

    /**
     * Converts the value expressed in the {@linkplain #getBaseUnit() base unit}, to the value
     * expressed in this unit.
     *
     * @param value the value to convert
     * @return the value converted to this unit
     * @throws NullPointerException if the given value is {@code null}
     */
    public BigFraction convertFromBaseUnit(BigFraction value) {
        if (isBase()) {
            return checkNotNull(value);
        } else {
            return value.subtract(offset).divide(multiplier);
        }
    }

    //
    // toString and variants
    //

    @Override
    public String toString() {
        return getLongName();
    }

    /**
     * Returns a string representation of this unit, yielding the plural or singular form according
     * to the value.
     */
    public String toStringForValue(Number value) {
        if (isDerived()) {
            return factors.toFractionString(toStringShortName);
        }
        if (BigFraction.ONE.equals(value)) {
            return getLongName();
        }
        return getPluralName();
    }

    /**
     * A function that converts a unit to a string, always yielding the unit's short name.
     */
    public static final Function<Unit, String> toStringShortName = new Function<Unit, String>() {
        @Override
        public String apply(Unit unit) {
            return unit.getShortName();
        }
    };

    /**
     * A function that converts a unit to a string, always yielding the unit's long name.
     */
    public static final Function<Unit, String> toStringLongName = new Function<Unit, String>() {
        @Override
        public String apply(Unit unit) {
            return unit.getLongName();
        }
    };

    /**
     * A function that converts a unit to a string, always yielding the unit's plural name.
     */
    public static final Function<Unit, String> toStringPluralName = new Function<Unit, String>() {
        @Override
        public String apply(Unit unit) {
            return unit.getPluralName();
        }
    };

    //
    // Object
    //

    @Override
    public boolean equals(Object object) {
        if (object == null) return false;
        if (object == this) return true;
        if (object instanceof Unit) {
            Unit unit = (Unit)object;
            if (unit.property != property) {
                return false;
            }
            if (factors == null) {
                return Objects.equal(multiplier, unit.multiplier) &&
                        Objects.equal(offset, unit.offset);
            } else {
                return factors.equals(unit.factors);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(property, multiplier, offset, factors);
    }

    //
    // Private helper methods
    //

    /**
     * Combines the multipliers in all the units of the given factorization in order to derive the
     * multiplier to be used for the unit derived from such factorization.
     */
    private static BigFraction combineMultipliers(Factorization<Unit> unitExpr) {
        final Set<Factor<Unit>> factors = unitExpr.factors();
        BigFraction result = BigFraction.ONE;
        for (Factor<Unit> factor : factors) {
            Unit unit = factor.getItem();
            int exponent = factor.getExponent();
            checkArgument(!unit.hasOffset());
            BigFraction multiplier = unit.getMultiplier();
            result = result.multiply(multiplier.pow(exponent));
        }
        return result;
    }

    /**
     * Verifies that the two given units are compatible (i.e.&nbsp;both units measure the same
     * {@linkplain Property physical property}).  If they're not, a {@link MetricException} is
     * thrown.
     *
     * @param sourceUnit the source unit of the attempted conversion operation
     * @param destUnit the destination unit of the attempted conversion operation
     * @throws MetricException if both units measure different properties.
     */
    private static void checkUnits(Unit sourceUnit, Unit destUnit) throws MetricException {
        if (!sourceUnit.isCompatibleWith(destUnit)) {
            throw MetricException.incompatibleUnits(sourceUnit, destUnit);
        }
    }

    /**
     * Compares this unit with the specified quantity for order.  Returns a negative integer,
     * zero, or a positive integer as this unit is smaller than, equal to, or larger than the
     * specified quantity.  A unit is smaller if it has a larger multiplier, and vice-versa.
     *
     * @param otherUnit the unit to compare to
     * @return a negative integer, zero, or a positive integer as this unit is smaller than, equal
     *     to, or larger than the specified unit.
     * @throws RuntimeException if the specified unit expresses values from another property,
     *     thus being incompatible to be compared to this unit.  In this case the {@code
     *     RuntimeException} thrown refers to the original {@link MetricException} as its
     *     {@linkplain RuntimeException#getCause() cause}.
     */
    @Override
    public int compareTo(Unit otherUnit) {
        try {
            checkUnits(this, otherUnit);
            return otherUnit.multiplier.compareTo(this.multiplier);
        } catch (MetricException ex) {
            throw Throwables.propagate(ex);
        }
    }

}
