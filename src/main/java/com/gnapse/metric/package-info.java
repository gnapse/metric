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

/**
 * This package contains a set of classes that work together to provide unit conversion
 * functionality, supporting queries given in natural language, and reading properties and unit
 * definitions from a file, with a flexible syntax that is easy to understand.  It also includes
 * functionality to load currency exchange rates from the Internet and perform currency conversion
 * as well.
 *
 * <h3>Core classes and concepts</h3>
 *
 * <p>In the center of the package classes is the {@link com.gnapse.metric.Universe}, which serves
 * as a context where unit conversions will take place.  A universe contains a set of
 * {@linkplain com.gnapse.metric.Property properties} that can be measured within that particular
 * universe.  Finally, within each property there's a set of {@linkplain com.gnapse.metric.Unit
 * measuring units} that serve the purpose of measuring the property.</p>
 *
 * <p>A property measurement consist of a numeric amount expressed in a certain unit of that
 * property.  This is also called a {@linkplain com.gnapse.metric.Quantity quantity} of that
 * property.  The same quantity of a property can be expressed in different units, introducing the
 * concept or process of unit conversion, which is the process of finding out the numeric amount of
 * a quantity expressed in a different unit.  The class {@link com.gnapse.metric.ConversionQuery}
 * represents such a conversion.</p>
 *
 * <h3>Conversion queries and supported syntax</h3>
 *
 * <p>Conversion queries can be stated in string values containing a natural language description of
 * the source value to convert and the unit that it will be converted to.  Below there's a list with
 * some examples of possible sentences that would be accepted, provided the unit names appearing in
 * these examples are properly defined.</p>
 *
 * <ul>
 * <li>2 meters <strong>in</strong> inches</li>
 * <li>4.5 kg <strong>in</strong> pound</li>
 * <li>1.4e7 eV <strong>in</strong> joules</li>
 * <li>100 mph <strong>in</strong> meters per second</li>
 * <li>1/3 kilometers/hour <strong>in</strong> feet/min</li>
 * <li>2 acre foot per year <strong>in</strong> cc per second</li>
 * <li>3*PI/8 radians <strong>in</strong> arcseconds</li>
 * <li>.45 kg m / square second <strong>in</strong> pound foot per s^2</li>
 * <li>22_550 square feet <strong>in</strong> hectares</li>
 * </ul>
 *
 * <p>Some details worth noting from the above list:</p>
 *
 * <ul>
 * <li>Unit names can be short or long names, plural or singular.  Names are case-sensitive.</li>
 * <li>The wide variety of number formats supported.</li>
 * <li>The use of some keywords to conveniently denote some arithmetic operation (e.g. {@code per},
 * {@code square}, etc.)</li>
 * <li>The possibility to express numbers with a limited use of multiplication and division,
 * instead of having to state the numbers in floating point notation (e.g. {@code 1/3} instead of
 * {@code 0.3333}, or {@code 3*PI/8}).</li>
 * <li>The use of the name {@code PI} to refer to the numeric value of the constant π.</li>
 * </ul>
 *
 * Some other advanced features are supported, such as omitting the result unit; stating a sum of
 * quantities to be converted, instead of just one; or omitting the numeric value.
 *
 * <h3>Unit definitions file</h3>
 *
 * {@link com.gnapse.metric.Universe} instances are initialized by loading properties and unit
 * definitions from a file.  The following is an small example of a universe with only six
 * properties and just a few units each.
 *
 * <pre>
 * length, distance {
 *     { nano, micro, milli, centi, deci, deca, hecto, kilo }
 *     meter, metre (m);
 *     inch (in) = 25.4 mm;
 *     foot (ft) = 12 inches;
 *     mile (mi) = 5280 feet;
 *     light year (ly) = 9_460_730_472_580_800 m;
 * }
 *
 * time {
 *     { micro, milli }
 *     second (s);
 *     minute (m) = 60 seconds;
 *     hour (h) = 60 minutes;
 *     day (d) = 24 hours;
 *     week (wk) = 7 days;
 * }
 *
 * mass {
 *     { milli, kilo }
 *     gram (g);
 *     pound (lb) = 0.45359237 kg;
 *     ounce (oz) = 1/16 pounds;
 * }
 *
 * area = square distance {
 *     acre (ac) = 43_560 feet^2;
 * }
 *
 * speed = distance/time {
 *     (mps) = meters per second;
 *     (kph) = km/h;
 *     (mph) = miles per hour;
 *     (fps) = feet per second;
 * }
 *
 * momentum = mass*speed {
 * }
 * </pre>
 *
 * <p>In the above example there are several features worth noting.</p>
 *
 * <ul>
 * <li>First of all properties and units may be defined with more than one name.  Units have long
 * names plus short names listed between parenthesis.  Long names must be provided only in their
 * singular form, and the library will internally generate the plural form of each.</li>
 * <li>Units can be defined with a set of SI prefixes (listed between curly braces before the unit
 * names), which implicitly defines several units from a single unit definition.</li>
 * <li>The first three properties (length, time and mass) are simple or <em>fundamental
 * properties</em> (i.e. self-defined and non-dependant on any other property).  In contrast, the
 * last three properties (area, speed and momentum) are <em>derived properties</em>, because they
 * are defined in terms of other properties.</li>
 * <li>Fundamental properties must have a base unit definition, which always comes first and has no
 * expression after the names.  Derived properties must not have this, since their base property is
 * derived from their own definition in terms of other properties.</li>
 * <li>A derived property must always come after all the properties on which it is defined.  They
 * can also have an empty unit definitions list, since units for these properties can be derived
 * dynamically from their definition in terms of other properties.  For instance, the momentum
 * property in the example above implicitly defines {@code kg*m/s} as one of its units, since this
 * can be derived from the definition of the property ({@code mass*distance/time}).</li>
 * </ul>
 *
 * <h3>Other features</h3>
 *
 * Besides the fundamental core features exposed above, there are some extra features that are worth
 * mentioning.
 *
 * <ul>
 * <li>Absolute precision in calculations by using {@link com.gnapse.common.math.BigFraction} to
 * represent numeric values internally.  All numbers are represented by a rational number, a
 * reduced fraction of the form {@code numerator/denominator}.  This allows for absolutely no loss
 * of precision, which is specially a problem in the division operation.  Rounding and conversion
 * to floating point notation only occurs when the final computed result is converted to a string
 * representation.</li>
 * <li>Control of the format of numbers in the output, giving different format to numbers of
 * different magnitude.  See class {@link com.gnapse.metric.NumberFormatter}.</li>
 * <li>Binary unit prefixes, in addition to the standard SI prefixes.  These are mostly used for
 * units related to digital information.  See class {@link com.gnapse.metric.UnitPrefix}.</li>
 * <li>Currency exchange rates, and dynamic loading of them from the Internet on demand.  See class
 * {@link com.gnapse.metric.CurrencyLoader}.</li>
 * <li>Unit names are pluralized using the
 * {@link com.gnapse.common.inflector.Inflectors#pluralOf(String)} method.  The package
 * {@link com.gnapse.common.inflector} has a complete implementation of plural and singular
 * inflections  for the English language, that is independent from this package and can be used for
 * other purposes as well.</li>
 * </ul>
 *
 * <h3>Dependencies on external libraries</h3>
 *
 * Classes in this package depend on the following external libraries:
 *
 * <dl>
 * <dt><strong><a href="http://code.google.com/p/guava-libraries/">Guava</a></strong></dt>
 * <dd>Google's Guava Libraries are indispensable and used throughout most of the source code, and
 * some of its classes are even exposed in the public API.  Specially useful and almost
 * irreplaceable are the collection classes and helpers in the {@code com.google.common.collect}
 * package.</dd>
 * <dt><strong><a href="http://code.google.com/p/json-simple/">JSON.simple</a></strong></dt>
 * <dd>A Java toolkit to encode or decode JSON text.  This library is used internally by the
 * {@linkplain com.gnapse.metric.CurrencyLoader currency loader} to decode the JSON-encoded
 * information about currency names and exchange rates stored online or cached locally.  Its use is
 * very limited and concealed in the private APIs, so it would be very easy to replace by another
 * functionally equivalent library if it's ever needed.</dd>
 * </dl>
 *
 */
package com.gnapse.metric;
