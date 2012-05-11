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

import com.gnapse.common.inflector.Inflectors;
import com.gnapse.common.inflector.Rule;
import com.gnapse.common.math.BigFraction;
import com.gnapse.common.math.Factorization;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * A class responsible for loading currency exchange rates information and building the money
 * property, which has currencies as its units.  This allows a {@link Universe} to perform
 * conversions between currencies.  Supports loading the currency definitions and exchange rates
 * remotely from the Internet and keeps a local copy that is reused whenever possible and updated
 * when the local version is old enough and there's an Internet connection.
 *
 * @author Ernesto García
 */
final class CurrencyLoader {

    private final Property property;

    private final Universe universe;

    private final File currencyFile;

    private static final Logger LOGGER = Logger.getLogger(CurrencyLoader.class.getName());

    /**
     * The number of milliseconds in an hour.
     */
    private static final long ONE_HOUR = 3600 * 1000;

    /**
     * The URL where the currency names are loaded from remotely.
     */
    private static final String CURRENCY_NAMES_URL
            = "http://openexchangerates.org/currencies.json";

    /**
     * The URL where the currency exchange rates are loaded from remotely.
     */
    private static final String LATEST_EXCHANGE_RATES_URL
            = "http://openexchangerates.org/latest.json";

    /**
     * A set of currency names that will be ignored if they exist in the currency definitions file.
     *
     * <p>This is a temporary solution to avoid importing currencies with names that clash with the
     * names of existing units from other properties (e.g. CUP, the Cuban Peso).</p>
     */
    private static final Set<String> IGNORED_CURRENCIES = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("CUP")));

    /**
     * A map that defines extra custom names for certain currency codes.
     */
    private final Multimap<String, String> currencyAliases;

    /*
     * Add some custom pluralization rules to properly handle a few problematic currency names.
     */
    static {
        Inflectors.getPluralInflector().addRules(new Rule[] {
            Rule.irregulars(new String[][] {
                { "CFA Franc BCEAO", "CFA Francs BCEAO" },
            }),

            Rule.inflectSuffix("\\b(lita)s", "$3i"),
            Rule.inflectSuffix("\\b(lat)s", "$3i"),
            Rule.inflectSuffix("\\b(boliviano)", "$3s"),
        });
    }

    /**
     * Loads currency definitions for the given universe (either locally or remotely) and builds a
     * new property with all the loaded currency as its units.  The new property will have the
     * given names.
     */
    CurrencyLoader(Universe universe, List<String> names, Multimap<String, String> currencyAliases)
            throws MetricException {
        this.universe = universe;
        this.currencyAliases = currencyAliases;
        this.currencyFile = universe.getCurrencyFile();

        try {
            JSONObject currencyDefinitions = loadCurrencyDefinitions();
            Iterable<UnitDefinition> unitDefs = createUnitDefinitions(currencyDefinitions);
            property = new Property(universe, names);
            for (UnitDefinition def : unitDefs) {
                property.addUnits(def);
            }
            property.freezeUnits();
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, MetricException.class);
            throw Throwables.propagate(e);
        }
    }

    /**
     * The money property built by this currency loader.
     */
    public Property getProperty() {
        return property;
    }

    /**
     * The universe for which this currency loader performs its task.
     */
    public Universe getUniverse() {
        return universe;
    }

    /**
     * Creates a list of unit definitions from the JSON currency specification.
     * @param map the JSON currency specification, already parsed and loaded as a {@link Map}.
     * @return the resulting list of unit definitions
     */
    private List<UnitDefinition> createUnitDefinitions(JSONObject map) {
        final Map names = (Map) map.get("names");
        final Map rates = (Map) map.get("rates");
        final String baseUnitCode = (String) map.get("base");
        final Factorization<String> baseUnitFactors = Factorization.factor(baseUnitCode, 1);
        final List<UnitDefinition> result = new ArrayList<UnitDefinition>(rates.size());

        String code = baseUnitCode;
        String name = (String) names.get(code);
        BigFraction rate = BigFraction.ONE;
        result.add(newUnitDef(code, name, null, rate));

        for (Object obj : rates.entrySet()) {
            Map.Entry entry = (Map.Entry) obj;

            code = (String) entry.getKey();
            if (code.equals(baseUnitCode) || IGNORED_CURRENCIES.contains(code)) {
                continue;
            }

            name = (String) names.get(code);
            if (name == null) {
                continue;
            }

            rate = BigFraction.valueOf((Number) entry.getValue());
            if (rate.equals(BigFraction.ZERO)) {
                continue;
            }

            result.add(newUnitDef(code, name, baseUnitFactors, rate.reciprocal()));
        }

        return result;
    }

    /**
     * Convenience helper method that creates a new unit definition from the given information.
     */
    private UnitDefinition newUnitDef(String code, String name,
            Factorization<String> baseUnitFactors, BigFraction rate) {
        List<String> shortNames = Arrays.asList(code, code.toLowerCase());
        List<String> longNames = Lists.newArrayList(name, name.toLowerCase());
        longNames.addAll(currencyAliases.get(code));
        return new UnitDefinition(longNames, shortNames, baseUnitFactors, rate, null, null);
    }

    /**
     * Loads the currency definitions from the currency file, and updates it from the remote sources
     * if the local version is older than a day, or if there's no local cached version yet.
     *
     * @return the loaded JSON content already converted to a {@link Map}.
     * @throws IOException if there's a problem reading the file, or loading the updated remote
     *     definitions
     * @throws ParseException if there's a problem while parsing the JSON file or the remote version
     */
    private JSONObject loadCurrencyDefinitions() throws IOException, ParseException {
        JSONObject result = loadLocalCurrencyDefinitions();
        if (result == null) {
            result = loadRemoteCurrencyDefinitions();
        }

        boolean isLocal = (Boolean) result.get("local");
        double timestamp = ((Number) result.get("timestamp")).longValue() * 1000;
        Date today = new Date();

        if (isLocal && today.getTime() - timestamp > ONE_HOUR) {
            try {
                result = loadRemoteCurrencyDefinitions();
                isLocal = false;
            } catch (Exception e) {
                LOGGER.info("Could not load currency exchange rates remotely");
            }
        }

        if (!isLocal) {
            try {
                Files.write(result.toJSONString(), currencyFile, Charsets.UTF_8);
            } catch (IOException e) {
                LOGGER.warning("Could not save remote exchange rates locally");
            }
        }

        LOGGER.info(String.format(
                "Currency exchange rates were loaded %s", isLocal ? "locally" : "remotely"));

        return result;
    }

    /**
     * Loads the remote currency definitions.
     */
    @SuppressWarnings(value = {"unchecked"})
    private JSONObject loadRemoteCurrencyDefinitions() throws IOException, ParseException {
        try {
            URL currencyNamesURL = new URL(CURRENCY_NAMES_URL);
            URL latestExchangeRatesURL = new URL(LATEST_EXCHANGE_RATES_URL);
            JSONObject currencyNames = loadJSON(currencyNamesURL.openStream());
            JSONObject latestExchangeRates = loadJSON(latestExchangeRatesURL.openStream());
            latestExchangeRates.put("names", currencyNames);
            latestExchangeRates.put("local", false);
            LOGGER.fine("Remote currency exchange rates were loaded successfully");
            return latestExchangeRates;
        } catch (MalformedURLException ex) {
            throw Throwables.propagate(ex);
        }
    }

    /**
     * Loads the currency definitions from the local file.
     */
    @SuppressWarnings(value = "unchecked")
    private JSONObject loadLocalCurrencyDefinitions() {
        try {
            InputStream in = new FileInputStream(currencyFile);
            JSONObject result = loadJSON(in);
            result.put("local", true);
            LOGGER.fine("Cached currency exchange rates were loaded successfully");
            return result;
        } catch (Exception ex) {
            LOGGER.info("Could not load cached currency exchange rates");
            return null;
        }
    }

    /**
     * Loads and parses the JSON code from the given input stream.
     */
    private static JSONObject loadJSON(InputStream in) throws IOException, ParseException {
        Reader reader = new InputStreamReader(in, Charsets.UTF_8);
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(reader);
    }

}
