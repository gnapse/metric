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

import com.gnapse.common.math.BigFraction;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;

/**
 * Represents a query that performs a conversion of a sum of quantities to a specific unit.
 *
 * @author Ernesto García
 */
public class ConversionQuery {

    private final List<Quantity> quantities;

    private final Quantity quantity;

    private final Quantity result;

    /**
     * Converts the specified quantity to the specified unit.
     *
     * @param quantity the quantity to convert
     * @param destUnit the unit to convert the quantity to
     * @throws MetricException if the quantity measures a different property than the unit
     */
    public ConversionQuery(Quantity quantity, Unit destUnit) throws MetricException {
        this.quantity = quantity;
        this.quantities = Collections.singletonList(quantity);
        this.result = quantity.convertTo(destUnit);
    }

    /**
     * Converts the given list (sum) of quantities to the specified unit.
     *
     * @param quantities the list or sum of quantities to convert
     * @param destUnit the unit to convert the quantities to
     * @throws MetricException if any of the quantities measures a different property than the unit,
     *     or if some of the quantities is {@linkplain Unit#hasOffset() not absolute}.
     */
    public ConversionQuery(List<Quantity> quantities, Unit destUnit) throws MetricException {
        this.quantities = ImmutableList.copyOf(quantities);
        this.quantity = Quantity.sum(destUnit, quantities);
        this.result = quantity.convertTo(destUnit);
    }

    /**
     * Converts the given list (sum) of quantities to the base unit of their property
     *
     * @param quantities the list or sum of quantities to convert
     * @throws MetricException if not all quantities measure the same property, or if some of the
     *     quantities is {@linkplain Unit#hasOffset() not absolute}.
     */
    public ConversionQuery(List<Quantity> quantities) throws MetricException {
        checkArgument(!quantities.isEmpty());
        Unit destUnit = quantities.get(0).getProperty().getBaseUnit();
        this.quantities = ImmutableList.copyOf(quantities);
        this.quantity = Quantity.sum(destUnit, quantities);
        this.result = quantity.convertTo(destUnit);
    }

    /**
     * Converts the quantity defined by the given value and source unit, to the specified
     * destination unit.
     *
     * @param value the value to convert
     * @param sourceUnit the unit in which the value is expressed
     * @param destUnit the unit to convert the value to
     * @throws MetricException if the two units do not measure the same property
     */
    public ConversionQuery(BigFraction value, Unit sourceUnit, Unit destUnit)
            throws MetricException {
        this(new Quantity(value, sourceUnit), destUnit);
    }

    /**
     * Converts the quantity defined by the given value and source unit, to the specified
     * destination unit.  Units are specified by name and those names are resolved against the given
     * unit scope.
     *
     * @param scope the unit scope used to resolve the given unit names
     * @param value the value to convert
     * @param sourceUnitName the name of the unit in which the value is expressed
     * @param destUnitName the name of the unit to convert the value to
     * @throws MetricException if the two units do not measure the same property, or if any of the
     *     names does not correspond to a valid unit in the given scope
     */
    public ConversionQuery(UnitScope scope, BigFraction value,
            String sourceUnitName, String destUnitName) throws MetricException {
        Unit sourceUnit = scope.getUnitByName(sourceUnitName);
        Unit destUnit = scope.getUnitByName(destUnitName);

        if (sourceUnit == null) {
            throw MetricException.unknownUnitName(sourceUnitName);
        }
        if (destUnit == null) {
            throw MetricException.unknownUnitName(destUnitName);
        }

        this.quantity = new Quantity(value, sourceUnit);
        this.quantities = Collections.singletonList(this.quantity);
        this.result = quantity.convertTo(destUnit);
    }

    /**
     * The list of quantities of this conversion query.
     */
    public List<Quantity> getQuantities() {
        return quantities;
    }

    /**
     * The sum of all quantities of this conversion query.
     */
    public Quantity getQuantity() {
        return quantity;
    }

    /**
     * The sum of all quantities of this conversion query, already converted to the destination
     * unit.
     */
    public Quantity getResult() {
        return result;
    }

    /**
     * The string representation of sum of the quantities of this conversion query
     */
    public String toStringExpression() {
        if (quantities.size() == 1) {
            return quantities.get(0).toString();
        }

        String str = Joiner.on(") + (").join(quantities);
        return String.format("(%s)", str);
    }

    /**
     * The string representation of this conversion query and the results of the conversion.
     */
    public String toStringResults() {
        String expr = toStringExpression();
        return String.format("%s = %s", expr, result);
    }

    /**
     * The string representation of this conversion query in its original query form, with no
     * results.
     */
    public String toStringQuery() {
        String expr = toStringExpression();
        return String.format("%s in %s", expr, result.getUnit().getPluralName());
    }

    @Override
    public String toString() {
        return toStringResults();
    }

}
