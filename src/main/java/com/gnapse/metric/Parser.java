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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.gnapse.common.math.BigFraction;
import com.gnapse.common.math.Factorization;
import com.gnapse.common.parser.SyntaxError;
import com.gnapse.common.parser.Tokenizer;
import com.gnapse.common.parser.Tokenizer.Token;
import com.gnapse.common.parser.Tokenizer.TokenType;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses universe definition files and conversion queries in the context of a given
 * {@link Universe} instance.
 *
 * @author Ernesto García
 */
class Parser {

    private Tokenizer tokenizer;

    private Token tok;

    private final Universe universe;

    private static final Logger LOGGER = Logger.getLogger(Parser.class.getName());

    private static final Joiner wordJoiner = Joiner.on(' ');

    /**
     * Stores the keywords recognized as separator in a query expression.  These keywords are the
     * ones recognized to separate the quantity to convert and the unit to which it should be
     * converted.
     */
    private static final Set<String> keywords =
            Collections.unmodifiableSet(Sets.newHashSet(Arrays.asList("in", "to", "as")));

    /**
     * Special prefixes that can be applied to unit names.  This does not refer to the
     * {@linkplain UnitPrefix SI prefixes}, but to 'cubic' and 'square', affecting the exponent of
     * the unit.
     */
    private static final Set<String> dimensionPrefix =
            Collections.unmodifiableSet(Sets.newHashSet(Arrays.asList("square", "cubic")));

    //
    // Public API
    //

    /**
     * Creates a new parser for the given universe.
     * @param universe the universe in which this parser operates
     */
    Parser(Universe universe) {
        this.universe = checkNotNull(universe);
    }

    /**
     * Parses a conversion query from the contents of the given string.
     * @param query the string containing the conversion query
     * @return the conversion query parsed from the query string
     * @throws SyntaxError if the query string has some syntax error making it unable to interpret
     *     a conversion query out of it
     * @throws MetricException if the conversion query is semantically incorrect (e.g. refers to an
     *     unknown unit, requests conversion between incompatible units, etc.)
     */
    ConversionQuery parseConversionQuery(String query) throws SyntaxError, MetricException {
        initTokenizer(new Tokenizer(query));

        List<Quantity> quantities = parseQuantities();

        ConversionQuery result = null;
        if (tok.is(TokenType.EOF)) {
            result = new ConversionQuery(quantities);
        } else {
            Unit destUnit = parseUnitExpression();
            result = new ConversionQuery(quantities, destUnit);
        }

        tok.checkType(TokenType.EOF);
        tokenizer = null;
        tok = null;

        return result;
    }

    /**
     * Parses the universe definition file for this parser's {@link Universe}.
     * @throws IOException if some error occurs trying to read the universe or currency files, or
     *    while reading updated currency exchange rates from the internet.
     * @throws SyntaxError if the universe definition file or the currencies file has some syntax
     *     error
     * @throws MetricException if there are semantical errors in the universe definition file, or
     *     in the currency definitions
     */
    void parseUniverseFile() throws IOException, SyntaxError, MetricException {
        initTokenizer(new Tokenizer(universe.getUniverseFile()));

        do {
            parsePropertyDefinition();
        } while (!tok.is(TokenType.EOF));

        tokenizer = null;
        tok = null;
    }

    //
    // Private parsing methods
    //

    private void initTokenizer(Tokenizer t) throws SyntaxError {
        tokenizer = t;
        tokenizer.addKeywords("plus", "and", "per", "PI");
        tok = tokenizer.nextToken();
    }

    private Property parsePropertyDefinition() throws SyntaxError, MetricException {
        Factorization<Property> fp = null;
        List<String> names = parseNamesList();

        if (tok.is(TokenType.DOLLAR)) {
            try {
                tok = tok.next();
                names.add("$"); // ensuring the money property isn't added twice in the same universe
                Multimap<String, String> currencyAliases = parseCurrencyAliases();
                return new CurrencyLoader(universe, names, currencyAliases).getProperty();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Currency definitions were not loaded");
                return null;
            }
        }

        if (tok.is(TokenType.EQUALS)) {
            tok = tok.next();
            Factorization<String> fs = parseFactorization();
            fp = fs.transformItems(universe.getPropertyByNameFn);
        }

        tok.checkType(TokenType.LBRACE);
        tok = tok.next();

        Property newProperty = new Property(universe, names, fp);
        while (!tok.is(TokenType.RBRACE)) {
            UnitDefinition def = parseUnitDefinition();
            newProperty.addUnits(def);
        }
        tok = tok.next();

        newProperty.freezeUnits();
        return newProperty;
    }

    private Multimap<String, String> parseCurrencyAliases() throws SyntaxError {
        Multimap<String, String> result = HashMultimap.create();

        tok.checkType(TokenType.LBRACE);
        tok = tok.next();

        while (!tok.is(TokenType.RBRACE)) {
            String code = tok.getWord();
            tok = tok.next();
            tok.checkType(TokenType.COLON);
            tok = tok.next();

            List<String> aliases = parseNamesList();
            tok.checkType(TokenType.SEMICOLON);
            tok = tok.next();

            for (String alias : aliases) {
                result.put(code, alias);
                result.put(code, pluralOf(alias));
                if (!alias.toLowerCase().equals(alias)) {
                    alias = alias.toLowerCase();
                    result.put(code, alias);
                    result.put(code, pluralOf(alias));
                }
            }
        }
        tok = tok.next();

        return result;
    }

    private UnitDefinition parseUnitDefinition() throws SyntaxError {
        EnumSet<UnitPrefix> prefixes = EnumSet.noneOf(UnitPrefix.class);
        if (tok.is(TokenType.LBRACE)) {
            tok = tok.next();
            prefixes = parsePrefixes();
            tok.checkType(TokenType.RBRACE);
            tok = tok.next();
        }

        List<String> longNames = parseNamesList();
        List<String> shortNames = Collections.emptyList();
        if (tok.is(TokenType.LPAREN)) {
            tok = tok.next();
            shortNames = parseNamesList();
            tok.checkType(TokenType.RPAREN);
            tok = tok.next();
        }

        BigFraction multiplier = null, offset = null;
        Factorization<String> baseUnitFactors = null;
        if (tok.is(TokenType.EQUALS)) {
            tok = tok.next();
            multiplier = parseNumber();
            baseUnitFactors = parseFactorization();
            if (tok.isOneOf(TokenType.PLUS, TokenType.MINUS)) {
                offset = parseNumber();
            }
        }

        tok.checkType(TokenType.SEMICOLON);
        tok = tok.next();

        return new UnitDefinition(longNames, shortNames,
                baseUnitFactors, multiplier, offset, prefixes);
    }

    //
    // Parse names, numbers and prefixes
    //

    private String parseName() throws SyntaxError {
        List<String> words = Lists.newArrayList();
        do {
            words.add(tok.getWord());
            tok = tok.next();
        } while (tok.is(TokenType.WORD));
        return wordJoiner.join(words);
    }

    private List<String> parseNamesList() throws SyntaxError {
        List<String> result = Lists.newArrayList();
        if (tok.is(TokenType.WORD)) {
            result.add(parseName());
        }
        while (tok.is(TokenType.COMMA)) {
            tok = tok.next();
            result.add(parseName());
        }
        return result;
    }

    /**
     * Parses the name of a unit using the given tokenizer as the source of tokens.
     *
     * @param tok the source of tokens, split from the query expression.
     * @param stopWithKeyword determines if while parsing for a unit name this method should
     *     recognize valid keywords and stop processing the unit name.
     * @return A string with the name of the unit parsed.
     * @throws SyntaxError if the tokens returned break in some way the structure of the valid
     *     syntax for unit names.
     */
    private String parseUnitName() throws SyntaxError {
        List<String> nameParts = Lists.newArrayList();
        String word = null;

        // Parse the first word into the name parts
        word = tok.getWord();
        nameParts.add(word);
        tok = tok.next();

        // If the first word was a dimension specification (square or cubic),
        // then parse the next word too, into the name parts, no matter what it is.
        if (dimensionPrefix.contains(word)) {
            nameParts.add(tok.getWord());
            tok = tok.next();
        }

        String result = wordJoiner.join(nameParts);
        if (universe.getUnitByName(result) != null) {
            return result;
        }

        // Now continue to add words into the name until a keyword or another token type comes next
        // But, if at any moment we have a valid unit name in the universe, then return it.
        while (tok.is(TokenType.WORD)) {
            word = tok.getWord();
            if (keywords.contains(word)) {
                break;
            }
            tok = tok.next();
            nameParts.add(word);
            result = wordJoiner.join(nameParts);
            if (universe.getUnitByName(result) != null) {
                return result;
            }
        }

        return result;
    }

    private Unit parseUnitExpression() throws SyntaxError, MetricException {
        Factorization<String> fs = parseFactorization();
        Factorization<Unit> fu = universe.getUnitFactors(fs);
        return universe.getUnitByFactors(fu);
    }

    /**
     * Attempts to interpret the current token as a numeric value.  It recognizes explicit numeric
     * values, but also translates some keywords that refer to recognized numeric constants such as
     * {@link BigFraction#PI PI}.  If the current token is neither a number or a keyword referring
     * to a recognized numeric constant, then it returns {@code null} or fails by throwing a
     * {@link SyntaxError} exception, depending on the {@code force} parameter.
     *
     * @param force if {@code true} then it tries to force the detection of a number and fails by
     *     throwing a {@link SyntaxError} exception if it can't; if {@code false} then it returns
     *     {@code null} if no number could be interpreted from the current token.
     * @return the numeric value recognized from the current token, or {@code null} if the
     *     {@code force} parameter is {@code false} and the current token does not represent a
     *     number.
     * @throws SyntaxError if the {@code force} parameter is {@code true} and the current token does
     *     not represent a number.
     */
    private BigFraction getTokenNumber(boolean force) throws SyntaxError {
        if (tok.is("PI")) {
            return BigFraction.PI;
        }
        if (tok.is(TokenType.NUMBER)) {
            return BigFraction.valueOf(tok.getNumber());
        }
        if (force) {
            tok.checkType(TokenType.NUMBER);
        }
        return null;
    }

    /**
     * Parses a numeric value.  It recognizes single numbers and simple arithmetic expressions which
     * are reduced to a single numeric value by performing the arithmetic operations implied by the
     * expression.  Any recognized expression can be optionally preceded by a {@code +} or
     * {@code -} sign to alter the sign of the resulting value.
     *
     * <p>Additionally this method also returns the numeric value {@link BigFraction#ONE 1} if
     * there's no recognized numeric value or expression.  In that case the tokenizer's current
     * token stays the same.</p>
     *
     * <p>The following is a comprehensive list of all forms of arithmetic expressions that this
     * method recognizes:</p>
     *
     * <ul>
     *   <li><pre>&lt;num&gt;</pre></li>
     *   <li><pre>&lt;num&gt; * &lt;num&gt;</pre></li>
     *   <li><pre>&lt;num&gt; / &lt;num&gt;</pre></li>
     *   <li><pre>&lt;num&gt; * &lt;num&gt; / &lt;num&gt;</pre></li>
     * </ul>
     *
     * <p>In all cases in the above list the pattern {@code <num>} stands for either an
     * explicit integer or floating point number as parsed by the {@link Tokenizer#parseNumber()
     * Tokenizer's parseNumber method}, or a keyword representing a constant value, such as
     * {@link BigFraction#PI PI}.</p>
     *
     * <p>As mentioned above, the result is always a reduced {@link BigFraction fraction} resulting
     * from evaluating the expression found, including the optional sign preceding the
     * expression.</p>
     *
     * @return the numeric value of the expression parsed, or {@link BigFraction#ONE 1} if no
     *     numeric expression was found.
     */
    private BigFraction parseNumber() throws SyntaxError {
        int signum = 1;
        if (tok.isOneOf(TokenType.PLUS, TokenType.MINUS)) {
            signum = tok.is(TokenType.PLUS) ? 1 : -1;
            tok = tok.next();
        }
        BigFraction num = getTokenNumber(false);

        if (num == null) {
            return BigFraction.ONE;
        }

        tok = tok.next();

        if (tok.is(TokenType.STAR)) {
            tok = tok.next();
            num = num.multiply(getTokenNumber(true));
            tok = tok.next();
        }

        if (tok.is(TokenType.SLASH)) {
            tok = tok.next();
            num = num.divide(getTokenNumber(true));
            tok = tok.next();
        }

        return signum < 0 ? num.negate() : num;
    }

    /**
     * Parses a {@link UnitPrefix prefix} name.
     * @return the {@link UnitPrefix prefix} found
     * @throws SyntaxError if it does not recognize a valid prefix name
     */
    private UnitPrefix parsePrefix() throws SyntaxError {
        String prefixName = tok.getWord();
        UnitPrefix prefix = UnitPrefix.getLong(prefixName);
        if (prefix == null) {
            throw new SyntaxError(String.format("Invalid prefix name %s", prefixName), tok);
        }
        tok = tok.next();
        return prefix;
    }

    /**
     * Parses the list of prefixes, if any.
     */
    private EnumSet<UnitPrefix> parsePrefixes() throws SyntaxError {
        EnumSet<UnitPrefix> prefixes = EnumSet.noneOf(UnitPrefix.class);

        prefixes.add(parsePrefix());
        while (tok.is(TokenType.COMMA)) {
            tok = tok.next(); // skip the comma
            prefixes.add(parsePrefix());
        }

        return prefixes;
    }

    //
    // Parsing quantities
    //

    /**
     * Parses a quantity, consisting of a number followed by a unit expression.
     *
     * @throws SyntaxError if it does not recognize a valid quantity syntax
     * @throws UnknownNameException if the unit expression does not map to a valid unit in the
     *     universe
     */
    private Quantity parseQuantity() throws SyntaxError, MetricException {
        BigFraction value = parseNumber();
        Unit unit = parseUnitExpression();
        return new Quantity(value, unit);
    }

    /**
     * Parses a series or list of quantities, optionally binded by natural language "binding words"
     * such as "and", "plus", or by the use of the comma as a separator.
     */
    private List<Quantity> parseQuantities() throws SyntaxError, MetricException {
        List<Quantity> quantities = Lists.newArrayList();
        quantities.add(parseQuantity());

        while (!tok.isOneOf(TokenType.WORD, TokenType.EOF)) {
            if (tok.is(TokenType.COMMA)) {
                tok = tok.next();
            }
            if (tok.isOneOf("and", "plus")) {
                tok = tok.next();
            }
            quantities.add(parseQuantity());
        }

        if (!tok.is(TokenType.EOF)) {
            // Check that we hit the conversion magic word and skip it
            checkState(keywords.contains(tok.getWord()));
            tok = tok.next();
        }

        return quantities;
    }

    //
    // Factorization parsing methods
    //

    private int parseExponent() throws SyntaxError {
        int exp = 1;
        if (tok.is(TokenType.CARET)) {
            tok = tok.next();
            int signum = 1;
            if (tok.isOneOf(TokenType.PLUS, TokenType.MINUS)) {
                signum = tok.is(TokenType.PLUS) ? 1 : -1;
                tok = tok.next();
            }
            exp = tok.getNumber().intValue() * signum;
            tok = tok.next();
        }
        return exp;
    }

    private Factorization<String> parseFactor() throws SyntaxError {
        if (tok.is(TokenType.LPAREN)) {
            tok = tok.next();
            Factorization<String> f = parseFactorization();
            tok.checkType(TokenType.RPAREN);
            tok = tok.next();
            int exp = parseExponent();
            f = f.pow(exp);
            return f;
        } else {
            String unitName = parseUnitName();
            int exp = parseExponent();
            if (unitName.startsWith("cubic ")) {
                exp *= 3;
                unitName = unitName.substring(6);
            } else if (unitName.startsWith("square ")) {
                exp *= 2;
                unitName = unitName.substring(7);
            } else if (unitName.startsWith("inverse ")) {
                exp *= -1;
                unitName = unitName.substring(8);
            }
            return Factorization.factor(unitName, exp);
        }
    }

    private Factorization<String> parseFactorMultiplication() throws SyntaxError {
        Factorization<String> f = parseFactor();
        while (tok.isOneOf(TokenType.STAR, TokenType.LPAREN, TokenType.WORD)
                && !tok.isOneOf(keywords)) {
            if (tok.is(TokenType.STAR)) {
                tok = tok.next();
            }
            f = f.multiply(parseFactor());
        }
        return f;
    }

    private Factorization<String> parseFactorDivision() throws SyntaxError {
        Factorization<String> f = parseFactor();
        while ((tok.isOneOf(TokenType.STAR, TokenType.SLASH, TokenType.LPAREN, TokenType.WORD)
                || tok.is("per")) && !tok.isOneOf(keywords)) {
            if (tok.isOneOf(TokenType.STAR, TokenType.SLASH) || tok.is("per")) {
                tok = tok.next();
            }
            f = f.multiply(parseFactor());
        }
        return f;
    }

    private Factorization<String> parseFactorization() throws SyntaxError {
        Factorization<String> f = parseFactorMultiplication();
        if (tok.is(TokenType.SLASH) || tok.is("per")) {
            tok = tok.next();
            f = f.divide(parseFactorDivision());
        }
        return f;
    }

}
