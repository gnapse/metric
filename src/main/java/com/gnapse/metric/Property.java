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
import static com.google.common.base.Preconditions.checkState;

import com.gnapse.common.math.Factorization;
import com.gnapse.common.math.Factorization.Factor;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a physical property that can be perceived and measured in a given {@linkplain Universe
 * universe}.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Physical_property">Physical property (Wikipedia)</a>
 * @author Ernesto García
 */
public class Property implements UnitScope {

    private Unit baseUnit;

    private final String name;

    private final Set<String> names;

    private final Universe universe;

    private final Factorization<Property> dimensions;

    private final Map<String, Unit> unitsByName = Maps.newHashMap();

    private List<Unit> units = Lists.newArrayList();

    //
    // Constructors
    //

    /**
     * Creates a new derived property in the given universe with the specified names and
     * factorization.
     *
     * @param universe the universe where this property is defined
     * @param names the names of this property
     * @param factors the factors that define this property as a derivation of other properties
     * @throws MetricException if any of the property names is a duplicate of a property previously
     *     defined, or if there's a property already defined in the universe with the same dimension
     */
    Property(Universe universe, List<String> names, Factorization<Property> factors)
            throws MetricException {

        boolean isDerived = (factors != null) && !factors.isEmpty() && !factors.isSingleItem();

        checkArgument(names != null && !names.isEmpty());
        this.name = Iterables.getFirst(names, null);
        this.names = Collections.unmodifiableSet(Sets.newLinkedHashSet(names));

        this.universe = checkNotNull(universe);
        this.dimensions = isDerived ? reduceFactors(factors) : Factorization.factor(this, 1);
        universe.registerNewProperty(this);

        if (isDerived) {
            Factorization<Unit> baseUnitFactors = factors.transformItems(getPropertyBaseUnitFn);
            registerNewUnit(new Unit(this, baseUnitFactors, true));
        }
    }

    /**
     * Creates a new fundamental property in the given universe with the specified names.
     *
     * @param universe the universe where this property is defined
     * @param names the names of this property
     * @throws MetricException if any of the property names is a duplicate of a property previously
     *     defined
     */
    Property(Universe universe, List<String> names)
            throws MetricException {
        this(universe, names, null);
    }

    /**
     * Reduces a property factorization by expanding the factorizations of all non-fundamental
     * properties in it, until obtaining an equivalent factorization, but only containing
     * fundamental properties.
     *
     * @param f the factorization to reduce
     * @return the reduced factorization
     */
    static Factorization<Property> reduceFactors(Factorization<Property> f) {
        Factorization<Property> result = Factorization.empty();

        for (Factor<Property> factor : f.factors()) {
            Property property = factor.getItem();
            int exponent = factor.getExponent();
            if (property.isFundamental()) {
                result = result.multiply(property, exponent);
            } else {
                result = result.multiply(property.dimensions.pow(exponent));
            }
        }

        return result;
    }

    /**
     * Adds new units stemming from the given unit definition.
     * @param def the unit definition that defines some new units to be added to this property.
     */
    void addUnits(UnitDefinition def) throws MetricException {
        List<Unit> newUnits = def.buildUnits(this);
        for (Unit unit : newUnits) {
            registerNewUnit(unit);
        }
    }

    /**
     * Tells this property that all its named units have already been defined and registered.  This
     * seals off the possibility to add new units to the property.
     *
     * <p>This operation is performed by the parser during the universe-construction phase, after it
     * finished processing all unit definitions for the given property.  When a universe is finished
     * being built all its properties have already been built.</p>
     *
     * @throws MetricException if the property is not derived and has no registered units
     */
    void freezeUnits() throws MetricException {
        units = Collections.unmodifiableList(units);
        if (!isDerived() && units.isEmpty()) {
            throw MetricException.invalidEmptyProperty(this.getName());
        }
    }

    /**
     * Registers a new unit to this property.  It makes sure all unit names are unique in the scope
     * of this property and also registers the new unit in the {@linkplain #getUniverse() universe}.
     *
     * @param unit the new unit to be registered to this property
     * @throws MetricException if any of the new unit names is a duplicate for a name of a unit
     *     previously registered in the property or its universe.
     */
    private void registerNewUnit(Unit unit) throws MetricException {
        Iterable<String> unitNames = unit.getNames();

        // Check for duplicate names
        for (String unitName : unitNames) {
            if (unitsByName.containsKey(unitName)) {
                throw MetricException.duplicateUnitName(name);
            }
        }

        for (String unitName : unit.getNames()) {
            Unit prev = unitsByName.put(unitName, unit);
            checkState(prev == null); // This should not happen after the check we made above
        }
        if (units.isEmpty()) {
            checkState(baseUnit == null);
            baseUnit = unit;
        }
        units.add(unit);
        universe.registerNewUnit(unit);
    }

    //
    // Some basic getters and setters
    //

    /**
     * The universe in which this property is perceived and measured.
     */
    public Universe getUniverse() {
        return universe;
    }

    /**
     * The name of this property.
     */
    public String getName() {
        return name;
    }

    /**
     * The set of names of this property.
     */
    public Set<String> getNames() {
        return names;
    }

    /**
     * The base unit in which this property is measured.  Serves also as a base for other units to
     * be defined.
     */
    public Unit getBaseUnit() {
        return baseUnit;
    }

    /**
     * Determines if this property is a fundamental property, that is, one that is not expressed in
     * terms of other properties.
     */
    public boolean isFundamental() {
        return dimensions.isSingleItem();
    }

    /**
     * Determines if this property is a derived property, that is, one that is expressed in terms of
     * other properties.
     */
    public boolean isDerived() {
        return !dimensions.isSingleItem();
    }

    /**
     * The dimensions of this property, which consists of its factorization in terms of other
     * properties.
     */
    public Factorization<Property> getDimensions() {
        return dimensions;
    }

    @Override
    public String toString() {
        return dimensions.isSingleItem() ? getName() : dimensions.toString();
    }

    //
    // UnitScope implementation
    //

    @Override
    public Collection<Unit> getNamedUnits() {
        return units;
    }

    @Override
    public Unit getUnitByName(String unitName) {
        return unitsByName.get(unitName);
    }

    @Override
    public boolean hasUnitNamed(String name) {
        return unitsByName.containsKey(name);
    }

    //
    // Some useful mapping functions
    //

    /**
     * A function object that maps a unit name to the unit with that name in this property.
     *
     * <p>If the name does not map to a unit in this property it throws a {@link MetricException}
     * wrapped inside a {@link RuntimeException}.</p>
     */
    public final Function<String, Unit> getUnitByNameFn = new Function<String, Unit>() {
        @Override
        public Unit apply(String unitName) {
            Unit result = Property.this.getUnitByName(unitName);
            if (result == null) {
                Throwables.propagate(MetricException.unknownUnitName(unitName));
            }
            return result;
        }
    };

    /**
     * A function object that maps a property to its base unit.
     */
    public final static Function<Property, Unit> getPropertyBaseUnitFn
            = new Function<Property, Unit>() {
        @Override
        public Unit apply(Property property) {
            return property.getBaseUnit();
        }
    };

    /**
     * A function object that maps a unit to its property.
     */
    public final static Function<Unit, Property> getUnitPropertyFn
            = new Function<Unit, Property>() {
        @Override
        public Property apply(Unit unit) {
            return unit.getProperty();
        }
    };

}
