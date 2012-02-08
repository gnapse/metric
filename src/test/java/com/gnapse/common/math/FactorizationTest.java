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

package com.gnapse.common.math;

import static org.junit.Assert.*;

import com.gnapse.common.math.Factorization.Factor;
import com.google.common.base.Function;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for the {@link Factorization} class.
 *
 * @author Ernesto García
 */
public class FactorizationTest {

    public FactorizationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static final String ITEM_METER = "meter";
    private static final String ITEM_SECOND = "second";
    private static final String ITEM_KG = "kg";

    private static final Factor<String> FACTOR_METER = new Factor<String>(ITEM_METER);
    private static final Factor<String> FACTOR_SECOND = new Factor<String>(ITEM_SECOND);
    private static final Factor<String> FACTOR_KG = new Factor<String>(ITEM_KG);
    private static final Factor<String> FACTOR_SQUARE_METER = new Factor<String>(ITEM_METER, 2);
    private static final Factor<String> FACTOR_SQUARE_SECOND = new Factor<String>(ITEM_SECOND, 2);

    private static final Factorization<String> EMPTY = Factorization.empty();
    private static final Factorization<String> METER = Factorization.factor(FACTOR_METER);
    private static final Factorization<String> SECOND = Factorization.factor(FACTOR_SECOND);
    private static final Factorization<String> KG = Factorization.factor(FACTOR_KG);
    private static final Factorization<String> HERTZ = SECOND.pow(-1);

    private static final Factorization<String> SQUARE_METER
            = Factorization.factor(FACTOR_SQUARE_METER);

    private static final Factorization<String> SQUARE_SECOND
            = Factorization.factor(FACTOR_SQUARE_SECOND);

    private static final Factorization<String> METERS_PER_SECOND
            = Factorization.fraction(ITEM_METER, ITEM_SECOND);

    private static final Factorization<String> MPS_PER_SECOND
            = METERS_PER_SECOND.divide(ITEM_SECOND);

    private static final Factorization<String> NEWTON
            = KG.multiply(MPS_PER_SECOND);

    /**
     * Test the creation of an empty factorization and the behavior of some methods and properties
     * acting on it.
     */
    @Test
    public void testEmptyFactorizations() {
        assertTrue(EMPTY.isEmpty());
        assertTrue(EMPTY.factors().isEmpty());
        assertTrue(EMPTY.items().isEmpty());
        assertNull(EMPTY.getSingleFactor());
        assertNull(EMPTY.getSingleItem());
        assertEquals("1", EMPTY.toString());

        assertEquals(EMPTY, EMPTY.inverse());
        assertEquals(EMPTY, EMPTY.multiply(EMPTY));
        assertEquals(EMPTY, EMPTY.divide(EMPTY));
        assertEquals(SQUARE_SECOND, EMPTY.multiply(SQUARE_SECOND));
        assertEquals(SQUARE_SECOND, SQUARE_SECOND.divide(EMPTY));
        assertEquals(SQUARE_SECOND.inverse(), EMPTY.divide(SQUARE_SECOND));
    }

    /**
     * Test the creation of factorizations consisting of a single factor.
     */
    @Test
    public void testSingleFactorAndItem() {
        assertFalse(SQUARE_METER.isEmpty());
        assertTrue(SQUARE_METER.isSingleFactor());
        assertFalse(SQUARE_METER.isSingleItem());
        assertNotNull(SQUARE_METER.getSingleFactor());
        assertNull(SQUARE_METER.getSingleItem());

        assertFalse(METER.isEmpty());
        assertTrue(METER.isSingleFactor());
        assertTrue(METER.isSingleItem());
        assertNotNull(METER.getSingleFactor());
        assertNotNull(METER.getSingleItem());

        assertFalse(EMPTY.isSingleFactor());
        assertFalse(EMPTY.isSingleItem());
        assertNull(EMPTY.getSingleFactor());
        assertNull(EMPTY.getSingleItem());
    }

    /**
     * Test factorizations consisting of a simple fraction {@code m/s}.
     */
    @Test
    public void testFraction() {
        Factorization<String> MPS = METERS_PER_SECOND;
        assertFalse(MPS.isEmpty());
        assertFalse(MPS.isSingleFactor());
        assertFalse(MPS.isSingleItem());

        Factorization<String> numerator = MPS.numerator();
        Factorization<String> denominator = MPS.denominator();
        assertTrue(numerator.isSingleItem());
        assertTrue(denominator.isSingleItem());
        assertEquals(METER, numerator);
        assertEquals(SECOND, denominator);
        assertEquals(denominator.inverse(), MPS.divide(numerator));
        assertEquals(numerator, MPS.multiply(denominator));

        Factorization<String> inverse = MPS.inverse();
        assertEquals(numerator, inverse.denominator());
        assertEquals(denominator, inverse.numerator());
        assertEquals(EMPTY, MPS.multiply(inverse));
    }

    /**
     * Test of product method, of class Factorization.
     */
    @Test
    public void testProduct() {
        Factorization<String> M1 = Factorization.product(ITEM_METER);
        Factorization<String> M2 = Factorization.product(ITEM_METER, ITEM_METER);
        Factorization<String> M3 = Factorization.product(ITEM_METER, ITEM_METER, ITEM_METER);
        Factorization<String> MSEC_KG2 = Factorization.product(
                ITEM_METER, ITEM_SECOND, ITEM_KG, ITEM_KG);

        assertEquals(SQUARE_METER, M2);
        assertEquals(Factorization.factor(ITEM_METER, 3), M3);
        assertEquals(1, M1.getSingleFactor().getExponent());
        assertEquals(2, M2.getSingleFactor().getExponent());
        assertEquals(3, M3.getSingleFactor().getExponent());
        assertEquals(EMPTY, Factorization.product());
        assertFalse(MSEC_KG2.isSingleFactor());
        assertEquals(NEWTON, MSEC_KG2.divide(ITEM_SECOND, 3).divide(ITEM_KG));
    }

    private void assertFactorsCount(int expectedCount, Factorization<String> f) {
        assertEquals(expectedCount, f.factors().size());
        assertEquals(expectedCount, f.items().size());
        assertEquals(expectedCount, f.toMap().size());
    }

    /**
     * Tests the methods that give a collection view of the factorization.
     */
    @Test
    public void testCollectionViews() {
        Factorization<String> M3 = Factorization.product(ITEM_METER, ITEM_METER, ITEM_METER);
        assertFalse(M3.factors().size() == 3);
        assertFactorsCount(1, M3);

        // Altering the exponent still keeps the size (as long as it does not become empty!)
        assertFactorsCount(1, M3.divide(ITEM_METER));
        assertFactorsCount(1, M3.multiply(ITEM_METER));

        // Introducing a new item (by division or multiplication) increases the number of factors
        assertFactorsCount(2, M3.divide(ITEM_SECOND));
        assertFactorsCount(2, M3.multiply(ITEM_SECOND));

        // Newton = kg * m / s^2 so it has three factors
        assertFactorsCount(3, NEWTON);
    }

    /**
     * Test of transformItems method, of class Factorization.
     */
    @Test
    public void testTransformItems() {
        final Factorization<String> F1 = Factorization.product("km", "kg", "cc").divide("kelvin");
        final Factorization<String> F2
                = Factorization.product("second", "yard").divide("year", "league");

        //
        // Transform items to their class
        //

        final Function<Object, Class> obj2class = new Function<Object, Class>() {
            @Override public Class apply(Object obj) {
                return obj.getClass();
            }
        };

        // km * kg * cc / kelvin => String^3 / String => String^2
        assertEquals(Factorization.factor(String.class, 2), F1.transformItems(obj2class));

        // kg * m / s^2 => String*String / String^2 => 1 (EMPTY)
        assertEquals(EMPTY, NEWTON.transformItems(obj2class));

        // m / s => String / String => 1 (EMPTY)
        assertEquals(EMPTY, METERS_PER_SECOND.transformItems(obj2class));

        // m / s^2 => String / String^2 => String^-1
        assertEquals(Factorization.factor(String.class, -1),
                MPS_PER_SECOND.transformItems(obj2class));

        // m^2 => String^2
        assertEquals(Factorization.factor(String.class, 2),
                SQUARE_METER.transformItems(obj2class));

        //
        // Transform string items to their length
        //

        final Function<String, Integer> str2len = new Function<String, Integer>() {
            @Override public Integer apply(String str) {
                return str.length();
            }
        };

        // km * kg * cc / kelvin => 2^3 / 6
        assertEquals(Factorization.factor(2, 3).divide(6), F1.transformItems(str2len));

        // kg * meter / second^2 => 2 * 5 / 6^2
        assertEquals(Factorization.product(2, 5).divide(6).divide(6),
                NEWTON.transformItems(str2len));

        // second * yard / year * league = 6*4 / 4*6 = 1 (EMPTY)
        assertEquals(EMPTY, F2.transformItems(str2len));

        //
        // Transform string items to the first character in the string
        //

        final Function<String, Character> firstChar = new Function<String, Character>() {
            @Override public Character apply(String str) {
                return str.charAt(0);
            }
        };

        // km * kg * cc / kelvin = k^2 * c / k = k*c
        assertEquals(Factorization.product('k', 'c'), F1.transformItems(firstChar));

        // second * yard / year * league = s*y / y*l = s/l
        assertEquals(Factorization.fraction('s', 'l'), F2.transformItems(firstChar));

    }

    /**
     * Test multiplication and division.
     */
    @Test
    public void testMultiplicationAndDivision() {
        assertEquals(MPS_PER_SECOND, NEWTON.divide(ITEM_KG));

        Factorization<String> KG3 = Factorization.product(ITEM_KG, ITEM_KG, ITEM_KG);
        assertEquals(KG3, KG.multiply(KG).multiply(FACTOR_KG));
        assertEquals(KG3, KG.multiply(ITEM_KG, 2));
        assertEquals(KG3, KG.multiply(ITEM_KG, 4).divide(ITEM_KG, ITEM_KG));

        assertEquals(METERS_PER_SECOND, EMPTY.multiply(METERS_PER_SECOND));
        assertEquals(METERS_PER_SECOND, METERS_PER_SECOND.multiply(EMPTY));
        assertEquals(METERS_PER_SECOND, METERS_PER_SECOND.divide(EMPTY));

        assertEquals(EMPTY, KG3.divide(KG3));
        assertEquals(EMPTY, KG3.multiply(KG3.inverse()));

        assertEquals(HERTZ, MPS_PER_SECOND.divide(METERS_PER_SECOND));
    }

    /**
     * Test inversion and exponentiation.
     */
    @Test
    public void testInversionAndExponentiation() {
        assertEquals(NEWTON, NEWTON.pow(1));
        assertEquals(SECOND, SECOND.pow(-1).inverse());
        assertEquals(METER, METER.inverse().pow(-1));

        assertEquals(Factorization.product(ITEM_KG, ITEM_KG, ITEM_KG), KG.pow(3));
        assertEquals(SQUARE_METER, METER.pow(2));
        assertEquals(SQUARE_SECOND.divide(METER), MPS_PER_SECOND.inverse());
        assertEquals(MPS_PER_SECOND.multiply(METER), METERS_PER_SECOND.pow(2));

        Factorization<String> S4 = Factorization.factor(ITEM_SECOND, -4);
        assertEquals(S4, SECOND.pow(4).inverse());
        assertEquals(S4, SECOND.inverse().pow(2).pow(2));
        assertEquals(S4, SECOND.pow(-4));
    }

    /**
     * Test of numerator and denominator.
     */
    @Test
    public void testNumeratorAndDenominator() {
        Factorization<String> numerator = NEWTON.numerator();
        Factorization<String> denominator = NEWTON.denominator();
        assertEquals(KG.multiply(METER), numerator);
        assertEquals(SECOND.pow(2), denominator);
        assertEquals(NEWTON, numerator.divide(denominator));

        assertTrue(MPS_PER_SECOND.numerator().isSingleItem());
        assertTrue(MPS_PER_SECOND.denominator().isSingleFactor());
        assertFalse(MPS_PER_SECOND.denominator().isSingleItem());

        assertTrue(KG.denominator().isEmpty());
        assertEquals(EMPTY, HERTZ.numerator());
    }

}
