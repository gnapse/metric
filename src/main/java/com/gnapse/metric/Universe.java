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
import static com.google.common.base.Preconditions.checkState;

import com.gnapse.common.math.Factorization;
import com.gnapse.common.parser.SyntaxError;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Represents a universe with {@linkplain Property physical properties} that can be measured.  It
 * provides a context where {@linkplain Unit unit} conversions are carried out.
 *
 * @author Ernesto García
 */
public final class Universe implements UnitScope {

    private final Parser parser = new Parser(this);

    private Function<BigDecimal, String> numberFormatter = new NumberFormatter();

    private Set<Property> properties = Sets.newHashSet();

    private final Map<String, Property> propertiesByName = Maps.newHashMap();

    private final Map<String, Unit> unitsByName = Maps.newHashMap();

    private final Map<Factorization<Property>, Property> propertiesByDimensions = Maps.newHashMap();

    private final Map<Factorization<Unit>, Unit> unitsByFactors = Maps.newHashMap();

    private final Set<Unit> unitSet = Sets.newHashSet();

    private final File universeFile, currencyFile;

    //
    // Some useful mapping functions
    //

    /**
     * A function that map unit names to units of this universe.
     */
    public final Function<String, Unit> getUnitByNameFn = new Function<String, Unit>() {
        @Override
        public Unit apply(String unitName) {
            final Unit result = Universe.this.getUnitByName(unitName);
            if (result == null) {
                Throwables.propagate(MetricException.unknownUnitName(unitName));
            }
            return result;
        }
    };

    /**
     * A function that maps property names to properties of this universe.
     */
    public final Function<String, Property> getPropertyByNameFn = new Function<String, Property>() {
        @Override
        public Property apply(String propertyName) {
            final Property result = Universe.this.getPropertyByName(propertyName);
            if (result == null) {
                Throwables.propagate(MetricException.unknownPropertyName(propertyName));
            }
            return result;
        }
    };

    /**
     * A function that maps properties to their base unit.
     */
    public final static Function<Property, Unit> getPropertyBaseFn
            = new Function<Property, Unit>() {
        @Override
        public Unit apply(Property property) {
            return property.getBaseUnit();
        }
    };

    /**
     * A function that maps units to their base unit.
     */
    public final static Function<Unit, Unit> getUnitBaseFn = new Function<Unit, Unit>() {
        @Override
        public Unit apply(Unit unit) {
            return unit.getProperty().getBaseUnit();
        }
    };

    //
    // Constructors
    //

    /**
     * Create a new universe from the given unit definition file and currency cache file.
     *
     * @param universeFile the unit definition file to be parsed and loaded into this universe
     * @param currencyFile the file where currency info and exchange rates are cached
     * @throws IOException if an error occurs while reading or writing files, or while reading
     *     currency exchange rates from the Internet
     * @throws SyntaxError if there's a syntax error in the files
     * @throws MetricException if there are semantic errors in the unit definitions loaded
     */
    public Universe(File universeFile, File currencyFile)
            throws IOException, SyntaxError, MetricException {
        this.universeFile = checkNotNull(universeFile);
        this.currencyFile = checkNotNull(currencyFile);
        parser.parseUniverseFile();
        properties = Collections.unmodifiableSet(properties);
    }

    /**
     * Create a new universe from the given unit definition file and currency cache file.
     *
     * @param universeFileName the name of the unit definition file to be parsed and loaded into
     *     this universe
     * @param currencyFileName the name of the file where currency info and exchange rates are
     *     cached
     * @throws IOException if an error occurs while reading or writing files, or while reading
     *     currency exchange rates from the Internet
     * @throws SyntaxError if there's a syntax error in the files
     * @throws MetricException if there are semantic errors in the unit definitions loaded
     */
    public Universe(String universeFileName, String currencyFileName)
            throws IOException, SyntaxError, MetricException {
        this(new File(universeFileName),
                (currencyFileName == null) ? null : new File(currencyFileName));
    }

    /**
     * Create a new universe from the given unit definition file.  The currency cache file is
     * inferred from the name of the universe definition file.
     *
     * @param universeFile the unit definition file to be parsed and loaded into this universe
     * @throws IOException if an error occurs while reading or writing files, or while reading
     *     currency exchange rates from the Internet
     * @throws SyntaxError if there's a syntax error in the files
     * @throws MetricException if there are semantic errors in the unit definitions loaded
     */
    public Universe(File universeFile) throws IOException, SyntaxError, MetricException {
        this(universeFile, getDefaultCurrencyFile(universeFile));
    }

    /**
     * Create a new universe from the given unit definition file.  The currency cache file is
     * inferred from the name of the universe definition file.
     *
     * @param universeFileName the name of the unit definition file to be parsed and loaded into
     *     this universe
     * @throws IOException if an error occurs while reading or writing files, or while reading
     *     currency exchange rates from the Internet
     * @throws SyntaxError if there's a syntax error in the files
     * @throws MetricException if there are semantic errors in the unit definitions loaded
     */
    public Universe(String universeFileName) throws IOException, SyntaxError, MetricException {
        this(new File(universeFileName));
    }

    private static File getDefaultCurrencyFile(File universeFile) throws IOException {
        final String baseName = universeFile.getName();
        final int dot = baseName.lastIndexOf('.');
        String name, ext;
        if (dot < 0) {
            name = baseName;
            ext = ".txt";
        } else {
            name = baseName.substring(0, dot);
            ext = baseName.substring(dot);
        }
        name = String.format("%s-currencies%s", name, ext);
        return new File(universeFile.getParentFile(), name);
    }

    //
    // Getters, setters and other properties
    //

    /**
     * The number formatter used by this universe.
     */
    public Function<BigDecimal, String> getNumberFormatter() {
        return numberFormatter;
    }

    /**
     * Sets the number formatter used by this universe.
     */
    public void setNumberFormatter(Function<BigDecimal, String> newFormatter) {
        this.numberFormatter = checkNotNull(newFormatter);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .addValue(String.format("%d properties", properties.size()))
                .addValue(String.format("%d named units", unitSet.size()))
                .toString();
    }

    @Override
    public Unit getUnitByName(String name) {
        return unitsByName.get(name);
    }

    @Override
    public boolean hasUnitNamed(String name) {
        return unitsByName.containsKey(name);
    }

    @Override
    public Collection<Unit> getNamedUnits() {
        return unitSet;
    }

    /**
     * Returns the property with the specified name.
     * @param name the name of the property to search for
     * @return the property with the specified name, or {@code null} if there's no property
     *     registered under that name in this universe
     */
    public Property getPropertyByName(String name) {
        return propertiesByName.get(name);
    }

    /**
     * Determines if there's a property with the specified name in this universe.
     * @param name the name of the property to search for
     * @return {@code true} if the specified name corresponds to some property defined in this
     *     universe; {@code false} otherwise
     */
    public boolean hasProperty(String name) {
        return propertiesByName.containsKey(name);
    }

    /**
     * The set of all properties defined in this universe.
     */
    public Set<Property> properties() {
        return properties;
    }

    /**
     * The file where this universe was loaded from.
     */
    File getUniverseFile() {
        return universeFile;
    }

    /**
     * The file where the currency definitions (if any) are cached locally.
     */
    File getCurrencyFile() {
        return currencyFile;
    }

    //
    // Adding new properties
    //

    /**
     * Registers a new property to this universe.  This method is used internally by this universe's
     * parser during the construction phase, and will result in an error if called after the
     * universe's constructor finished.
     */
    void registerNewProperty(Property newProperty) throws MetricException {
        Property p;

        for (String propertyName : newProperty.getNames()) {
            p = propertiesByName.put(propertyName, newProperty);
            if (p != null) {
                throw MetricException.duplicatePropertyName(propertyName);
            }
        }

        if (newProperty.isDerived()) {
            p = propertiesByDimensions.put(newProperty.getDimensions(), newProperty);
            if (p != null) {
                throw MetricException.duplicateDerivedProperty(p, newProperty);
            }
        }

        properties.add(newProperty);
    }

    /**
     * Registers a new unit to this universe.  This method is used internally by this universe's
     * parser during the construction phase, and will result in an error if called after the
     * universe's constructor finished.
     */
    void registerNewUnit(Unit unit) throws MetricException {
        if (unit.isDerived()) {
            unitsByFactors.put(unit.getFactors(), unit);
            return;
        }

        for (String unitName : unit.getNames()) {
            if (unitsByName.containsKey(unitName)) {
                throw MetricException.duplicateUnitName(unitName);
            }
        }

        Object result;
        for (String unitName : unit.getNames()) {
            result = unitsByName.put(unitName, unit);
            checkState(result == null);
        }
        unitSet.add(unit);
    }

    //
    // Unit conversion
    //

    /**
     * Parses the given conversion expression and processes the conversion query resulting from it.
     *
     * @param expression a string to be parsed to obtain a conversion query
     * @return the conversion query resulting from parsing the given expression in the context of
     *     this universe
     * @throws SyntaxError if there's a syntax error in the conversion expression
     * @throws MetricException if there's some other conversion-related error while processing the
     *     expression, such as invalid unit names or an attempt to perform an incompatible
     *     conversion, etc.
     */
    public ConversionQuery convert(String expression)
            throws SyntaxError, MetricException {
        return parser.parseConversionQuery(expression);
    }

    //
    // Building units from expressions
    //

    /**
     * Retrieves the unit corresponding to the given factorization, dynamically constructing it if
     * necessary.
     */
    public Unit getUnitByFactors(Factorization<Unit> unitFactors) {
        if (unitFactors.isSingleItem()) {
            return unitFactors.getSingleItem();
        }

        Unit unit = unitsByFactors.get(unitFactors);
        if (unit != null) {
            return unit;
        }

        Factorization<Property> fp = unitFactors.transformItems(Property.getUnitPropertyFn);
        fp = Property.reduceFactors(fp);
        final Property property = fp.isSingleItem()
                ? fp.getSingleItem()
                : propertiesByDimensions.get(fp);

        if (property == null) {
            // Return an invalid unit instance
            return new Unit(unitFactors);
        }
        unit = new Unit(property, unitFactors, false);
        unitsByFactors.put(unitFactors, unit);
        return unit;
    }

    /**
     * Converts the given string factorization to a corresponding unit factorization, by mapping
     * each string in the factorization to the unit with that name.  If any string item in the
     * factorization does not map to a valid unit in this universe then an exception is thrown.
     *
     * @param unitNameFactors the string factorization to convert
     * @return the factorization of units resulting from the conversion
     * @throws MetricException if any string item in the factorization does not map to a valid unit
     *     name in this universe
     */
    public Factorization<Unit> getUnitFactors(Factorization<String> unitNameFactors)
            throws MetricException {
        try {
            return unitNameFactors.transformItems(getUnitByNameFn);
        } catch (RuntimeException e) {
            Throwables.propagateIfInstanceOf(Throwables.getRootCause(e), MetricException.class);
            throw e;
        }
    }

}
