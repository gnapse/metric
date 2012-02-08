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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Maps.EntryTransformer;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class informally represents the concept of a factor or product of items, and allows it to
 * be manipulated from an algebraic point of view.  The depicted product is abstract in the sense
 * that it does not represent the result of that product, nor does it need to have defined the
 * actual notion of evaluating the product or to perform a multiplication.  It only aims at
 * portraying the algebraic expression of that product.
 *
 * <p>Items in a factorization are of a generic type {@code T}.  Each item is present in the
 * factorization with an associated exponent, which informally expresses the number of times the
 * item is present in the factorization.  The exponent of an item can be positive or negative, but
 * never zero.  When an item's exponent becomes zero that item ceases to be present in the
 * factorization.</p>
 *
 * <p>Instances of this class are immutable, and methods for multiplication, division,
 * exponentiation, and any other arithmetic operation provided will always return a new
 * factorization instance as a result of the operation.  Thus instances of this class are safe to be
 * used as keys in a map or items in a set.</p>
 *
 * <p>Constructors in this class are all private.  New instances can be created via any of the
 * factory method provided, or by performing an arithmetic operation on an existing instance.</p>
 *
 * @author Ernesto García
 */
public class Factorization<T> {

    /**
     * Maps each factor in this factorization to its exponent.
     */
    private final Map<T, Integer> factors;

    /**
     * A virtual map that functions as a read-only view of the factors in this factorization that
     * have a positive exponent.
     */
    private Map<T, Integer> numerator;

    /**
     * A virtual map that functions as a read-only view of the factors in this factorization that
     * have a negative exponent.
     */
    private Map<T, Integer> denominator;

    //
    // class Factor
    //

    /*
     * Factors are represented internally as {@linkplain Entry map entries} but this is an internal
     * design feature, and should not be exposed in the public API of the {@link Factorization}
     * class.  In addition, {@linkplain Entry map entries} are modifiable by client code and
     * could potentially allow external code to modify a factorization.
     */

    /**
     * A class to represent factors in a {@link Factorization}.  A factor consists of a single item
     * exponentiated.
     */
    public static final class Factor<T> {
        private final T item;
        private final int exponent;

        /**
         * Creates a new factor representing {@code item^exponent}.
         * @param item the item of the factor
         * @param exponent the exponent of the factor
         */
        public Factor(T item, int exponent) {
            checkArgument(exponent != 0);
            this.item = checkNotNull(item);
            this.exponent = exponent;
        }

        /**
         * Creates a new factor representing {@code item^1}.
         * @param item the item of the factor
         */
        public Factor(T item) {
            this(item, 1);
        }

        private Factor(Entry<T, Integer> entry) {
            this(entry.getKey(), entry.getValue());
        }

        /**
         * The item of this factor.
         */
        public T getItem() {
            return item;
        }

        /**
         * The exponent of this factor.
         */
        public int getExponent() {
            return exponent;
        }

        @Override
        public String toString() {
            return (exponent == 1) ? item.toString() : String.format("%s^%d", item, exponent);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj != null && obj instanceof Factor) {
                final Factor factor = (Factor)obj;
                return (exponent == factor.exponent) && item.equals(factor.item);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(item, exponent);
        }
    }

    //
    // Internal constructors
    //

    /**
     * Creates a new empty factorization.
     */
    private Factorization() {
        this.factors = Collections.emptyMap();
    }

    /**
     * Creates a new factorization whose internal representation is defined by the given map.
     */
    private Factorization(Map<T, Integer> map) {
        this.factors = ImmutableMap.copyOf(map);
    }

    /**
     * Creates a new factorization my multiplying an existing one with the new factor
     * {@code item^exponent}.
     */
    private Factorization(Factorization<T> f, T item, int exponent) {
        checkNotNull(item);
        checkArgument(exponent != 0);

        if (f == null || f.isEmpty()) {
            this.factors = Collections.singletonMap(item, exponent);
            return;
        }

        final Map<T, Integer> map = Maps.newLinkedHashMap(f.factors);
        multiply(map, item, exponent);
        this.factors = Collections.unmodifiableMap(map);
    }

    //
    // Public factory methods
    //

    /**
     * Creates an empty factorization.
     * @param <T> the type of items of the factorization
     * @return the new empty factorization
     */
    public static <T> Factorization<T> empty() {
        return new Factorization<T>();
    }

    /**
     * Creates a factorization representing {@code item^exponent}.
     * @param <T> the type of items of the factorization
     * @param item the item in the single factor
     * @param exponent the exponent of the item in the factor
     * @return the new factorization
     */
    public static <T> Factorization<T> factor(T item, int exponent) {
        if (exponent == 0) {
            return empty();
        }
        return new Factorization<T>(null, item, exponent);
    }

    /**
     * Creates a new factorization from the given factor.
     * @param <T> the type of items of the factorization
     * @param factor the factor that will conform the new factorization
     * @return the new factorization consisting of a single factor
     */
    public static <T> Factorization<T> factor(Factor<T> factor) {
        return factor(factor.item, factor.exponent);
    }

    /**
     * Creates a factorization representing {@code numerator^1/denominator^1}.
     * @param <T> the type of items of the factorization
     * @param numerator the numerator of the fraction
     * @param denominator the denominator of the fraction
     * @return the new factorization
     */
    public static <T> Factorization<T> fraction(T numerator, T denominator) {
        checkNotNull(numerator);
        checkNotNull(denominator);

        if (numerator.equals(denominator)) {
            return empty();
        }

        final Map<T, Integer> map = Maps.newLinkedHashMap();
        map.put(numerator, 1);
        map.put(denominator, -1);
        return new Factorization<T>(map);
    }

    /**
     * Creates a factorization representing {@code numerator/denominator}.
     * @param <T> the type of items of the factorization
     * @param numerator the numerator of the fraction
     * @param denominator the denominator of the fraction
     * @return the new factorization
     */
    public static <T> Factorization<T> fraction(Factor<T> numerator, Factor<T> denominator) {
        return factor(numerator).divide(denominator);
    }

    /**
     * Creates a factorization representing <code>items[0] * items[1] * &#133; * items[n-1]</code>.
     * @param <T> the type of items of the factorization
     * @param items the items to include in the product
     * @return the new factorization
     */
    public static <T> Factorization<T> product(T... items) {
        final Map<T, Integer> map = Maps.newLinkedHashMap();
        for (T item : items) {
            multiply(map, item, 1);
        }
        return new Factorization<T>(map);
    }

    //
    // Attribute and query methods
    //

    /**
     * Determines if this consists of the empty factorization.
     */
    public boolean isEmpty() {
        return factors.isEmpty();
    }

    /**
     * Determines if this factorization has only a single factor.
     */
    public boolean isSingleFactor() {
        return factors.size() == 1;
    }

    private Entry<T, Integer> getSingleEntry() {
        if (factors.size() == 1) {
            return factors.entrySet().iterator().next();
        } else {
            return null;
        }
    }

    /**
     * Returns the only factor in this factorization, or {@code null} if there's more than one
     * factor, or if it's empty.
     */
    public Factor<T> getSingleFactor() {
        final Entry<T, Integer> singleEntry = getSingleEntry();
        if (singleEntry != null) {
            return new Factor<T>(singleEntry);
        } else {
            return null;
        }
    }

    /**
     * Determines if this factorization consists of a single factor with exponent {@code 1}.
     */
    public boolean isSingleItem() {
        final Entry<T, Integer> singleEntry = getSingleEntry();
        return singleEntry != null && singleEntry.getValue().intValue() == 1;
    }

    /**
     * Returns the item of the only factor in this factorization, if there's only one factor and
     * the exponent of the item is {@code 1}.  Otherwise it returns {@code null}.
     */
    public T getSingleItem() {
        final Entry<T, Integer> singleEntry = getSingleEntry();
        return (singleEntry != null) && (singleEntry.getValue().intValue() == 1)
                ? singleEntry.getKey() : null;
    }

    /**
     * Returns a set of all the factors in this factorization.
     */
    public Set<Factor<T>> factors() {
        final Set<Factor<T>> result = Sets.newLinkedHashSet();
        for (Entry<T, Integer> entry : factors.entrySet()) {
            result.add(new Factor<T>(entry));
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Returns a set containing the items present in this factorization.
     */
    public Set<T> items() {
        return Collections.unmodifiableSet(factors.keySet());
    }

    /**
     * Returns a map derived from this factorization by mapping each item to its exponent.
     */
    public Map<T, Integer> toMap() {
        return ImmutableMap.copyOf(factors);
    }

    /**
     * A map view of this factorization, containing only those factors with positive exponents.
     *
     * <p>If one interprets a factorization as a fraction, the numerator consists of those factors
     * that have a positive exponent.</p>
     */
    private Map<T, Integer> numeratorEntries() {
        if (numerator == null) {
            numerator = Maps.filterValues(factors, new Predicate<Integer>() {
                @Override
                public boolean apply(Integer exponent) {
                    return exponent.intValue() > 0;
                }
            });
        }
        return numerator;
    }

    /**
     * A map view of this factorization, containing only those factors with negative exponents.
     *
     * <p>If one interprets a factorization as a fraction, the denominator consists of those factors
     * that have a negative exponent.</p>
     */
    private Map<T, Integer> denominatorEntries() {
        if (denominator == null) {
            denominator = Maps.filterValues(factors, new Predicate<Integer>() {
                @Override
                public boolean apply(Integer exponent) {
                    return exponent.intValue() < 0;
                }
            });
        }
        return denominator;
    }

    /**
     * Creates a new factorization of another type {@code U}, by using a function that maps items
     * of type {@code T} to items of type {@code U}.
     *
     * <p>Note that there need not be a one-to-one correspondence between factors in this
     * factorization and factors in the resulting one.  This is because different items from this
     * factorization may or may not map to the same item of type {@code U}.</p>
     *
     * @param <U> the type of items of the new factorization
     * @param transformer the function that transforms items of type {@code T} to items of type
     *     {@code U}.
     * @return the new factorization derived from this one by the transformer function
     * @throws NullPointerException if the transformer function is {@code null}, or if it maps any
     *     of the items in this factorization to {@code null}.
     */
    public <U> Factorization<U> transformItems(Function<? super T, ? extends U> transformer) {
        checkNotNull(transformer);

        final Map<U, Integer> result = Maps.newLinkedHashMap();
        for (Map.Entry<T, Integer> entry : factors.entrySet()) {
            U newItem = transformer.apply(entry.getKey());
            multiply(result, newItem, entry.getValue());
        }

        return new Factorization<U>(result);
    }

    //
    // Arithmetic operations
    //

    /**
     * Returns {@code this * item^exponent}.
     */
    public Factorization<T> multiply(T item, int exponent) {
        if (exponent == 0) {
            return this;
        }
        return new Factorization<T>(this, item, exponent);
    }

    /**
     * Returns {@code this * f}.
     */
    public Factorization<T> multiply(Factor<T> f) {
        return multiply(f.item, f.exponent);
    }

    /**
     * Returns <code>this * (item[0] * item[1] * &#133; * item[n-1])</code>.
     */
    public Factorization<T> multiply(T... items) {
        return multiply(Factorization.product(items));
    }

    /**
     * Returns {@code this * f}.
     */
    public Factorization<T> multiply(Factorization<T> f) {
        if (this.isEmpty()) {
            return f;
        }
        if (f.isEmpty()) {
            return this;
        }

        final Map<T, Integer> map = Maps.newLinkedHashMap(factors);
        for (Map.Entry<T, Integer> entry : f.factors.entrySet()) {
            multiply(map, entry.getKey(), entry.getValue());
        }
        return new Factorization<T>(map);
    }

    /**
     * Returns {@code this/item^exponent}.
     */
    public Factorization<T> divide(T item, int exponent) {
        return multiply(item, -exponent);
    }

    /**
     * Returns {@code this / f}.
     */
    public Factorization<T> divide(Factor<T> f) {
        return divide(f.item, f.exponent);
    }

    /**
     * Returns <code>this / (item[0] * item[1] * &#133; * item[n-1])</code>.
     */
    public Factorization<T> divide(T... items) {
        return divide(Factorization.product(items));
    }

    /**
     * Returns {@code this/f}.
     */
    public Factorization<T> divide(Factorization<T> f) {
        if (f.isEmpty()) {
            return this;
        }

        final Map<T, Integer> map = Maps.newLinkedHashMap(factors);
        for (Map.Entry<T, Integer> entry : f.factors.entrySet()) {
            multiply(map, entry.getKey(), -entry.getValue());
        }
        return new Factorization<T>(map);
    }

    /**
     * Returns {@code this^exp}.
     */
    public Factorization<T> pow(int exp) {
        if (exp == 0) {
            return empty();
        }
        if (exp == 1 || this.isEmpty()) {
            return this;
        }

        final Map<T, Integer> map = Maps.newLinkedHashMap();
        for (Map.Entry<T, Integer> entry : factors.entrySet()) {
            map.put(entry.getKey(), exp*entry.getValue());
        }
        return new Factorization<T>(map);
    }

    /**
     * Returns {@code this^-1}.
     */
    public Factorization<T> inverse() {
        return pow(-1);
    }

    /**
     * Returns a new factorization consisting of all the factors in this one that have a positive
     * exponent.
     */
    public Factorization<T> numerator() {
        return new Factorization<T>(numeratorEntries());
    }

    /**
     * Returns a new factorization consisting of all the factors in this one that have a negative
     * exponent, but inverted to make the exponents positive.
     */
    public Factorization<T> denominator() {
        return new Factorization<T>(denominatorEntries()).inverse();
    }

    /**
     * Inserts an item into the given map with the specified exponent.  This method modifies the
     * map, and it is used as an internal helper method throughout the implementation of this class.
     *
     * @param <T> the type of items of the map
     * @param map the map where the new factor will be applied
     * @param item the item to apply to the map
     * @param exponent the exponent of the item
     */
    private static <T> void multiply(Map<T, Integer> map, T item, int exponent) {
        checkNotNull(item);

        final Integer exp = map.get(item);
        if (exp != null) {
            exponent += exp.intValue();
        }

        if (exponent == 0) {
            map.remove(item);
        } else {
            map.put(item, exponent);
        }
    }

    //
    // Object-related functionality
    //

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Factorization)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        final Factorization f = (Factorization)obj;
        return f.factors.equals(this.factors);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(factors);
    }

    //
    // toString method and helpers
    //

    private static <T> String toString(
            Map<T, Integer> map, final Function<T, String> itemsToString, final int signum) {
        final EntryTransformer<T, Integer, String> transformer
                = new Maps.EntryTransformer<T, Integer, String>() {
            @Override
            public String transformEntry(T key, Integer value) {
                final String keyString = itemsToString.apply(key);
                return value.intValue() == signum ?
                        keyString :
                        String.format("%s^%s", keyString, signum*value);
            }
        };
        final Collection<String> items = Maps.transformEntries(map, transformer).values();

        if (items.isEmpty()) {
            return "1";
        }
        if (items.size() == 1) {
            return items.iterator().next();
        }

        return joiner.join(items);
    }

    /**
     * Just so that we don't instantiate a new joiner every time we invoke the toString method.
     */
    private static final Joiner joiner = Joiner.on(' ');

    /**
     * The standard function used to convert items to string. It merely invokes the toString method
     * of the item, but has a check for {@code null} items (that should not exist anyway) and
     * returns the empty string in that case.
     */
    private final Function<T, String> toStringFn = new Function<T, String>() {
        @Override
        public String apply(T input) {
            return (input == null) ? "" : input.toString();
        }
    };

    @Override
    public String toString() {
        return toFractionString(null);
    }

    /**
     * Generates a string representation of this factorization expressed in fraction form.
     * @param itemsToString the function used to convert items to a string representation; if
     *     {@code null} then items will be converted to string using their {@link Object#toString}
     *     method.
     */
    public String toFractionString(Function<T, String> itemsToString) {
        if (itemsToString == null) {
            itemsToString = this.toStringFn;
        }
        final String num = toString(numeratorEntries(), itemsToString, 1);
        final String denom = toString(denominatorEntries(), itemsToString, -1);
        return denom.equals("1") ? num : String.format("%s / %s", num, denom);
    }

    /**
     * Generates a string representation of this factorization expressed as a product of factors.
     * @param itemsToString the function used to convert items to a string representation; if
     *     {@code null} then items will be converted to string using their {@link Object#toString}
     *     method.
     */
    public String toCanonicalString(Function<T, String> itemsToString) {
        if (itemsToString == null) {
            itemsToString = this.toStringFn;
        }
        final String num = toString(numeratorEntries(), itemsToString, 1);
        final String denom = toString(denominatorEntries(), itemsToString, 1);
        if (denom.equals("1")) {
            return num;
        }
        if (num.equals("1")) {
            return denom;
        }
        return num + denom;
    }

}
