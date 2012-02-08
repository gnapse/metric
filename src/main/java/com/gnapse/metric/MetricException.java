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

/**
 * Signals an error related to the unit conversion process, such as an attempt to perform an
 * incompatible unit conversion, or a reference to an unknown unit name, etc.  It is also used to
 * wrap some other exceptions that may occur in the process of unit conversion, but that cannot
 * propagate directly out of their execution context.
 *
 * @author Ernesto García
 */
public final class MetricException extends Exception {

    private MetricException(String message) {
        super(message);
    }

    private MetricException(String message, Throwable cause) {
        super(message, cause);
    }

    //
    // Factory methods
    //

    /**
     * Creates a new {@code MetricException} with a message built from the given message format and
     * arguments.
     * @param message the message string
     * @param args the arguments used to build the actual message
     * @return a new {@code MetricException}
     */
    public static MetricException withMessage(String message, Object... args) {
        return new MetricException(String.format(message, args));
    }

    /**
     * Creates a new {@code MetricException} that wraps the given throwable, which is the real
     * {@linkplain Throwable#getCause() cause} of the exception being thrown.
     * @param cause the exception to be wrapped
     * @return a new {@code MetricException} with the same message as the given cause
     */
    public static MetricException wrapper(Throwable cause) {
        return new MetricException(cause.getMessage(), cause);
    }

    /*
     * Creates a new {@code MetricException} with the specified message, that wraps the given
     * throwable, which is the real {@linkplain Throwable#getCause() cause} of the exception being
     * thrown.
     * @param message the message string
     * @param cause the exception to be wrapped
     * @return a new {@code MetricException}
     */
    public static MetricException wrapper(String message, Throwable cause) {
        return new MetricException(message, cause);
    }

    /**
     * Creates a new {@code MetricException} that signals that the a {@link Property} instance with
     * the given name was defined with no unit definitions.
     * @param propertyName the name of the property that caused the error
     * @return a new {@code MetricException}
     */
    public static MetricException invalidEmptyProperty(String propertyName) {
        final String message = String.format(
                "Property %s must have at least one unit definition", propertyName);
        return new MetricException(message);
    }

    /**
     * Creates a new {@code MetricException} that signals that the specified name was used to
     * reference to a {@link Unit unit}, but there's no unit defined with that name.
     * @param name the unknown unit name that caused this exception
     * @return a new {@code MetricException}
     */
    public static MetricException unknownUnitName(String name) {
        final String message = String.format("Name '%s' does not refer to any known unit", name);
        return new MetricException(message);
    }

    /**
     * Creates a new {@code MetricException} that signals that the specified name was used to
     * reference to a {@link Property property}, but there's no property defined with that name.
     * @param name the unknown property name that caused this exception
     * @return a new {@code MetricException}
     */
    public static MetricException unknownPropertyName(String name) {
        final String message = String.format(
                "Name '%s' does not refer to any known property", name);
        return new MetricException(message);
    }

    /**
     * Creates a new {@code MetricException} that signals that the specified name was used to refer
     * to more than one unit in a unit definition file.
     * @param name the duplicate unit name that caused this error
     * @return a new {@code MetricException}
     */
    public static MetricException duplicateUnitName(String name) {
        final String message = String.format("Duplicate unit name '%s'", name);
        return new MetricException(message);
    }

    /**
     * Creates a new {@code MetricException} that signals that the specified name was used to refer
     * to more than one property in a unit definition file.
     * @param name the duplicate property name that caused this error
     * @return a new {@code MetricException}
     */
    public static MetricException duplicatePropertyName(String name) {
        final String message = String.format("Duplicate property name '%s'", name);
        return new MetricException(message);
    }

    /**
     * Creates a new {@code MetricException} that signals an attempt to declare two properties with
     * the same dimension in the same universe.
     * @param original the first property defined with the conflicting dimensions
     * @param duplicate the property that caused the error by having the same dimensions as the
     *     original
     * @return a new {@code MetricException}
     */
    public static MetricException duplicateDerivedProperty(Property original, Property duplicate) {
        final String message = String.format("'%s' has the same dimensions of property '%s'",
                duplicate.getName(), original.getName());
        return new MetricException(message);
    }

    /**
     * Creates a new {@code MetricException} that signals an attempt to make a unit conversion or
     * a unit comparison between two units that measure different properties.
     * @param sourceUnit the source unit of the conversion, or one of the two units in a comparison
     * @param destUnit the destination unit of the conversion, or the other unit in a comparison
     * @return a new {@code MetricException}
     */
    public static MetricException incompatibleUnits(Unit sourceUnit, Unit destUnit) {
        final String message = String.format("Cannot convert or compare '%s' to '%s'",
                toString(sourceUnit), toString(destUnit));
        return new MetricException(message);
    }

    /**
     * Creates a new {@code MetricException} that signals an attempt to define a unit in terms of a
     * unit from another property.
     * @param unit the unit incorrectly defined
     * @param baseUnit the incorrect base unit used in the definition
     * @return a new {@code MetricException}
     */
    public static MetricException incompatibleBaseUnit(Unit unit, Unit baseUnit) {
        final String message = String.format("'%s' cannot serve as a %s base unit",
                toString(baseUnit), unit.getProperty().getName());
        return new MetricException(message);
    }

    //
    // Private helper methods
    //

    private static String toString(Unit unit) {
        if (unit.isValid()) {
            return String.format("%s (a unit of %s)",
                    unit.getPluralName(), unit.getProperty().getName());
        } else {
            return String.format("%s (an unkown and invalid unit)", unit.toString());
        }
    }

}
