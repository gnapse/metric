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
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Represents a quantity of a {@linkplain Property property}, consisting of a scalar numeric value
 * and the {@linkplain Unit unit} in which it is expressed.
 *
 * @author Ernesto García
 */
public final class Quantity implements Comparable<Quantity> {

    /**
     * The numeric value represented by this quantity.
     */
    private final BigFraction value;

    /**
     * The unit in which this quantity is expressed.
     */
    private final Unit unit;

    /**
     * Creates a quantity with a value of 0 and measured in the given unit.
     */
    public static Quantity zero(Unit unit) {
        return new Quantity(BigFraction.ZERO, unit);
    }

    /**
     * Sums all the given quantities and returns the result as a quantity expressed in the specified
     * unit.
     *
     * @param resultUnits the unit in which the resulting quantity shall be expressed
     * @param quantities the quantities to sum
     * @return a quantity consisting of the sum of all the given quantities, and expressed in the
     *     specified results unit
     * @throws IncompatibleUnitsException if any of the quantities to sum represents a value of a
     *     property different than the property in which the result shall be expressed
     */
    public static Quantity sum(Unit resultUnits, Quantity... quantities) throws MetricException {
        checkAdditiveQuantities(Arrays.asList(quantities));

        if (quantities.length == 0) {
            return zero(resultUnits);
        }

        if (quantities.length == 1) {
            return quantities[0].convertTo(resultUnits);
        }

        BigFraction result = quantities[0].convertTo(resultUnits).getValue();
        for (int i = 1; i < quantities.length; ++i) {
            Quantity q = quantities[i].convertTo(resultUnits);
            result = result.add(q.getValue());
        }
        return new Quantity(result, resultUnits);
    }

    /**
     * Sums all the given quantities and returns the result as a quantity expressed in the specified
     * unit.
     *
     * @param resultUnits the unit in which the resulting quantity shall be expressed
     * @param quantities the quantities to sum
     * @return a quantity consisting of the sum of all the given quantities, and expressed in the
     *     specified results unit
     * @throws IncompatibleUnitsException if any of the quantities to sum represents a value of a
     *     property different than the property in which the result shall be expressed
     */
    public static Quantity sum(Unit resultUnits, Iterable<Quantity> quantities)
            throws MetricException {
        checkAdditiveQuantities(quantities);

        final Iterator<Quantity> it = quantities.iterator();
        if (!it.hasNext()) {
            return zero(resultUnits);
        }

        Quantity first = it.next().convertTo(resultUnits);
        if (!it.hasNext()) {
            return first;
        }

        BigFraction result = first.getValue();
        while (it.hasNext()) {
            Quantity q = it.next().convertTo(resultUnits);
            result = result.add(q.getValue());
        }
        return new Quantity(result, resultUnits);
    }

    /**
     * Creates a new quantity representing the given numeric value expressed in the specified unit.
     */
    public Quantity(BigFraction value, Unit unit) {
        this.value = checkNotNull(value);
        this.unit = checkNotNull(unit);
    }

    /**
     * The value of this quantity.
     */
    public BigFraction getValue() {
        return value;
    }

    /**
     * The unit in which this quantity is expressed.
     */
    public Unit getUnit() {
        return unit;
    }

    /**
     * The property that this quantity measures.
     */
    public Property getProperty() {
        return unit.getProperty();
    }

    /**
     * The universe in which this quantity is measured.
     */
    public Universe getUniverse() {
        return unit.getUniverse();
    }

    @Override
    public String toString() {
        final String valueStr = unit.getUniverse().getNumberFormatter().apply(value.toBigDecimal());
        String unitName = unit.toStringForValue(value);
        return String.format("%s %s", valueStr, unitName);
    }

    /**
     * Determines if this quantity is equal to the given object.
     *
     * <p>A different quantity with the same numeric value expressed in the same unit as this one is
     * obviously equal to this quantity.  Moreover, if the given object is a quantity of the same
     * property, and when converted to the unit in which this quantity is expressed, the values are
     * the same, then both quantities are equal.  If the given unit is a quantity of a different
     * property, then {@code false} is returned.</p>
     *
     * <p>For example, {@code 10 cm} equals {@code 100 mm} and {@code 0 celcius} equals
     * {@code 32 fahrenheit}.  On the other hand {@code 10 m} is not equal to {@code 10 yards},
     * {@code 1 meter} is not equal to {@code 1 kelvin}.</p>
     *
     * @param o the object to test for equality
     * @return {@code true} if the given object is a quantity that expresses the same magnitude that
     *     is represented by this quantity; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Quantity)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        Quantity q = (Quantity)o;
        if (this.unit.equals(q.unit) && this.value.equals(q.value)) {
            return true;
        }

        try {
            BigFraction v = q.convertTo(this.unit).getValue();
            return v.equals(this.value);
        } catch (MetricException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(unit, value);
    }

    /**
     * Converts this quantity to be expressed in the other unit given.
     *
     * @param otherUnit the unit to which this quantity will be conversed
     * @return a quantity representing the same amount of this one, but expressed in the other unit
     *     given.
     * @throws IllegalArgumentException if the given unit and the unit of this quantity are from
     *     different properties.
     */
    public Quantity convertTo(Unit otherUnit) throws MetricException {
        if (otherUnit == this.unit) {
            return this;
        }
        BigFraction newValue = unit.convertTo(otherUnit, value);
        return new Quantity(newValue, otherUnit);
    }

    /**
     * Compares this quantity with the specified quantity for order.  Returns a negative integer,
     * zero, or a positive integer as this quantity is less than, equal to, or greater than the
     * specified quantity.
     *
     * @param otherQuantity the quantity to compare to
     * @return a negative integer, zero, or a positive integer as this quantity is less than, equal
     *     to, or greater than the specified quantity.
     * @throws RuntimeException if the specified quantity expresses a value of another property,
     *     thus being incompatible to be compared to this quantity.  In this case the {@code
     *     RuntimeException} thrown refers to the original {@link MetricException} as its {@link
     *     RuntimeException#getCause() cause}.
     */
    @Override
    public int compareTo(Quantity otherQuantity) {
        try {
            return value.compareTo(otherQuantity.convertTo(this.unit).getValue());
        } catch (MetricException ex) {
            throw Throwables.propagate(ex);
        }
    }

    private static void checkAdditiveQuantities(Iterable<Quantity> quantities)
            throws MetricException {
        final int size = Iterables.size(quantities);
        if (size > 1) {
            final boolean hasOffset = Iterables.any(quantities, new Predicate<Quantity>() {
                @Override
                public boolean apply(Quantity q) {
                    return q.getUnit().hasOffset();
                }
            });
            if (hasOffset) {
                throw MetricException.withMessage("Cannot sum non-absolute quantities");
            }
        }
    }

}
