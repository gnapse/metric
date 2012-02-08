/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.gnapse.metric;

import static com.google.common.base.Preconditions.checkNotNull;

import com.gnapse.common.math.BigFraction;
import com.gnapse.common.math.Factorization;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Represents a unit definition expression, also responsible for creating new unit instances
 * resulting from the definition it represents.
 *
 * @author Ernesto García
 */
class UnitDefinition {

    private final List<String> longNames;

    private final List<String> shortNames;

    private final EnumSet<UnitPrefix> prefixes;

    private final Factorization<String> baseUnitFactors;

    private final BigFraction multiplier;

    private final BigFraction offset;

    UnitDefinition(List<String> longNames, List<String> shortNames,
            Factorization<String> baseUnitFactors,
            BigFraction multiplier, BigFraction offset,
            EnumSet<UnitPrefix> prefixes) {
        this.longNames = checkNotNull(longNames);
        this.shortNames = checkNotNull(shortNames);
        this.baseUnitFactors = baseUnitFactors;
        this.multiplier = (multiplier == null) ? BigFraction.ONE : multiplier;
        this.offset = (offset == null) ? BigFraction.ZERO : offset;
        this.prefixes = (prefixes == null) ? EnumSet.noneOf(UnitPrefix.class) : prefixes;
    }

    /**
     * Builds new units stemming from this definition.
     */
    List<Unit> buildUnits(Property p) throws MetricException {
        final Unit baseUnit = buildCoreUnit(p);

        final List<Unit> units = Lists.newArrayListWithCapacity(prefixes.size() + 1);
        units.add(baseUnit);
        for (UnitPrefix prefix : prefixes) {
            final Unit prefixedUnit = new Unit(baseUnit, prefix, longNames, shortNames);
            units.add(prefixedUnit);
        }

        return Collections.unmodifiableList(units);
    }

    /**
     * Creates the core unit of this definition, for the given property.
     */
    private Unit buildCoreUnit(Property property) throws MetricException {
        if (baseUnitFactors == null || baseUnitFactors.isEmpty()) {
            return new Unit(property, longNames, shortNames);
        } else {
            final Universe universe = property.getUniverse();
            final Factorization<Unit> fu = universe.getUnitFactors(baseUnitFactors);
            final Unit baseUnit = universe.getUnitByFactors(fu);
            return new Unit(property, longNames, shortNames, baseUnit, multiplier, offset);
        }
    }

}
