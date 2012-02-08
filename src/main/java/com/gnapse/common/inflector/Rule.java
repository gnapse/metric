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

package com.gnapse.common.inflector;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A rule that states how a word changes from one form to another.
 *
 * @author Ernesto García
 */
@Beta
public abstract class Rule {

    //
    // Core public interface of a Rule
    //

    /**
     * Determines if this rule applies to the specified word.
     *
     * @param word the word that is being tested
     * @return {@code true} if this rule is applicable to the specified word; {@code false}
     *     otherwise
     */
    public abstract boolean appliesTo(String word);

    /**
     * Applies this rule to the given word, returning the word modified by this rule.
     *
     * @param word the word to apply this rule to
     * @return the word modified by this rule
     */
    public final String applyTo(String word) {
        checkArgument(appliesTo(word), "Rule is not applicable to word %s", word);
        return _applyTo(word);
    }

    protected abstract String _applyTo(String word);

    @Override
    public abstract String toString();

    //
    // Methods deriving a new rule form an existing one
    //

    /**
     * Creates a new rule derived from this one, but that restricts itself to be applied only to the
     * given set of words.
     *
     * <p>Note that words in the given set are considered to be case-insensitive.  This means that
     * any word matching a word in the set, disregarding letter case, will be applied this rule.</p>
     *
     * @param words the set of words that the new rule is restricted to
     * @return the new rule built by restricting this rule
     */
    public final Rule onlyForWords(Iterable<String> words) {
        final Set<String> wordSet = Sets.newHashSet(toLowerCase(words));

        return new Rule() {
            @Override public boolean appliesTo(String word) {
                return Rule.this.appliesTo(word) && wordSet.contains(word.toLowerCase());
            }

            @Override protected String _applyTo(String word) {
                return Rule.this.applyTo(word);
            }

            @Override public String toString() {
                return String.format("%s only for words %s", Rule.this, shortListStr(wordSet));
            }
        };
    }

    /**
     * Equivalent to {@link #onlyForWords(Iterable)}.
     */
    public final Rule onlyForWords(String... words) {
        return onlyForWords(Arrays.asList(words));
    }

    /**
     * Creates a new rule derived from this one, but that restricts itself to be applied only to
     * words that are <strong>not included</strong> in the given set of words.
     *
     * <p>Note that words in the given set are considered to be case-insensitive.  This means that
     * any word matching a word in the set, disregarding letter case, will be applied this rule.</p>
     *
     * @param words the set of words for which the new rule does not apply
     * @return the new rule built by restricting this rule
     */
    public final Rule exceptForWords(Iterable<String> words) {
        final Set<String> wordSet = Sets.newHashSet(toLowerCase(words));

        return new Rule() {
            @Override public boolean appliesTo(String word) {
                return Rule.this.appliesTo(word) && !wordSet.contains(word.toLowerCase());
            }

            @Override protected String _applyTo(String word) {
                return Rule.this.applyTo(word);
            }

            @Override public String toString() {
                return String.format("%s except for words %s", Rule.this, shortListStr(wordSet));
            }
        };
    }

    /**
     * Equivalent to {@link #exceptForWords(Iterable)}.
     */
    public final Rule exceptForWords(String... words) {
        return exceptForWords(Arrays.asList(words));
    }

    /**
     * Creates a new rule derived from this one, but that restricts itself to be applied only to
     * words that match the given pattern.
     *
     * @param pattern the pattern of words that the new rule will be applied to
     * @return the new rule built by restricting this rule
     */
    public final Rule forWordsMatching(final Pattern pattern) {
        return new Rule() {
            @Override public boolean appliesTo(String word) {
                return Rule.this.appliesTo(word) && pattern.matcher(word).matches();
            }

            @Override protected String _applyTo(String word) {
                return Rule.this.applyTo(word);
            }

            @Override public String toString() {
                return String.format("%s for words matching %s", Rule.this, pattern);
            }
        };
    }

    /**
     * Equivalent to {@link #forWordsMatching(Pattern)}.
     */
    public final Rule forWordsMatching(String pattern) {
        return forWordsMatching(Pattern.compile(pattern));
    }

    /**
     * Creates a new rule derived from this one, but that restricts itself to be applied only to
     * words that end with the given pattern.
     *
     * @param pattern the pattern of word suffix that the new rule will be applied to
     * @return the new rule built by restricting this rule
     */
    public final Rule forWordsEndingWith(String pattern) {
        return forWordsMatching(String.format("(?i).*(%s)$", pattern));
    }

    /**
     * Creates a new rule derived from this one, but that restricts itself to be applied only to
     * words that <strong>do not</strong> match the given pattern.
     *
     * @param pattern the pattern of words that the new rule will not be applied to
     * @return the new rule built by restricting this rule
     */
    public final Rule forWordsNotMatching(final Pattern pattern) {
        return new Rule() {
            @Override public boolean appliesTo(String word) {
                return Rule.this.appliesTo(word) && !pattern.matcher(word).matches();
            }

            @Override protected String _applyTo(String word) {
                return Rule.this.applyTo(word);
            }

            @Override public String toString() {
                return String.format("%s for words NOT matching %s", Rule.this, pattern);
            }
        };
    }

    /**
     * Equivalent to {@link #forWordsNotMatching(Pattern)}.
     */
    public final Rule forWordsNotMatching(String pattern) {
        return forWordsNotMatching(Pattern.compile(pattern));
    }

    /**
     * Creates a new rule derived from this one, but that restricts itself to be applied only to
     * words that <strong>do not</strong> end with the given pattern.
     *
     * @param pattern the pattern of word suffix that the new rule will be applied to
     * @return the new rule built by restricting this rule
     */
    public final Rule forWordsNotEndingWith(String pattern) {
        return forWordsNotMatching(String.format("(?i).*(%s)$", pattern));
    }

    /**
     * Creates a new rule derived from this one, but that restricts itself to be applied only to
     * words that comply to the specified condition or predicate.
     *
     * @param condition the predicate that determines whether this the new rule applies to a word
     * @return the new rule built by restricting this rule
     */
    public final Rule constrainedBy(final Predicate<String> condition) {
        return new Rule() {
            @Override public boolean appliesTo(String word) {
                return Rule.this.appliesTo(word) && condition.apply(word);
            }

            @Override protected String _applyTo(String word) {
                return Rule.this.applyTo(word);
            }

            @Override public String toString() {
                return String.format("%s constrained by a condition", Rule.this);
            }
        };
    }

    //
    // Methods for creating new rules
    //

    /**
     * The identity rule, which applies to all words and returns the word unmodified.
     */
    public static final Rule IDENTITY = new Rule() {
        @Override public boolean appliesTo(String word) { return true; }
        @Override protected String _applyTo(String word) { return word; }
        @Override public String toString() { return "Rule{IDENTITY}"; }
    };

    /**
     * Creates a new rule that maps words to their inflected form by using the specified function.
     * This rule applies to those words for which the function returns a non-null value and does not
     * throw an exception.
     *
     * @param fn the function used to build the rule
     * @return the new rule built from the given function
     */
    public static Rule forFunction(final Function<String, String> fn) {
        return new Rule() {
            @Override public boolean appliesTo(String word) {
                try {
                    return fn.apply(word) != null;
                } catch (Throwable e) {
                    return false;
                }
            }

            @Override protected String _applyTo(String word) {
                return fn.apply(word);
            }

            @Override public String toString() {
                return Objects.toStringHelper(Rule.class)
                        .addValue("FUNCTION").toString();
            }
        };
    }

    /**
     * Creates a new rules that applies to words matching a regular expression and applies the
     * specified replacement to the matching string in order to inflect it.
     *
     * @param pattern the pattern of words for which this rule applies
     * @param replacement the replacement string to apply to matching words
     * @return the new rule built from the given arguments
     */
    public static Rule inflectPattern(final Pattern pattern, final String replacement) {
        return new AbstractRegexRule(pattern) {
            @Override protected String _applyTo(Matcher matcher) {
                return matcher.replaceFirst(replacement);
            }

            @Override public String toString() {
                return Objects.toStringHelper(Rule.class)
                        .add("pattern", pattern)
                        .add("replacement", replacement)
                        .toString();
            }
        };
    }

    /**
     * Equivalent to {@link #inflectPattern(Pattern, String)}.
     */
    public static Rule inflectPattern(String pattern, String replacement) {
        return inflectPattern(Pattern.compile(pattern), replacement);
    }

    /**
     * Creates a new rule that applies to words matching a regular expression and inflects them by
     * invoking the given function on the {@link Matcher} that matched the pattern.
     *
     * @param pattern the pattern of words for which this rule applies
     * @param fn the function that inflects words based on the pattern's {@link Matcher}.
     * @return the new rule built from the given arguments
     */
    public static Rule inflectPattern(final Pattern pattern, final Function<Matcher, String> fn) {
        return new AbstractRegexRule(pattern) {
            @Override protected String _applyTo(Matcher matcher) {
                return fn.apply(matcher);
            }

            @Override public String toString() {
                return Objects.toStringHelper(Rule.class)
                        .add("pattern", pattern)
                        .add("replacement", "FUNCTION")
                        .toString();
            }
        };
    }

    /**
     * Equivalent to {@link #inflectPattern(Pattern, Function)}.
     */
    public static Rule inflectPattern(String pattern, final Function<Matcher, String> fn) {
        return inflectPattern(Pattern.compile(pattern), fn);
    }

    /**
     * Creates a new rule that applies to words ending with the given suffix pattern, and inflects
     * words by replacing the matching suffix with {@code newSuffix}.
     *
     * <p>Note that the suffix string is a regular expression pattern, and therefore the
     * replacement string may contain references to captured groups.  The whole string before the
     * matching suffix can be referenced by the {@code $1} captured group, and the whole matching
     * suffix can be referenced by the {@code $2} captured group.  Therefore captured groups within
     * the provided pattern itself are numbered from {@code $3} on.</p>
     *
     * @param suffix the suffix pattern of the words that this rule applies to
     * @param newSuffix the new suffix that replaces the matching suffix to form the inflected form
     *    of words
     * @return the new rule built from the given arguments
     */
    public static Rule inflectSuffix(String suffix, String newSuffix) {
        final String pattern = String.format("(?i)(.*)(%s)$", suffix);
        final String replacement = String.format("$1%s", checkNotNull(newSuffix));

        return inflectPattern(pattern, replacement);
    }

    /**
     * Creates a new rule that applies to words ending with any of the given suffix patterns, and
     * inflects words by replacing the matching suffix with {@code newSuffix}.
     *
     * @param suffixes the set of suffix patterns of the words that this rule applies to
     * @param newSuffix the new suffix that replaces the matching suffix to form the inflected form
     *    of words
     * @return the new rule built from the given arguments
     */
    public static Rule inflectSuffix(Iterable<String> suffixes, String newSuffix) {
        final String pattern = disjunction("(?i)^(.*)(%s)$", suffixes);
        final String replacement = String.format("$1%s", checkNotNull(newSuffix));

        return inflectPattern(pattern, replacement);
    }

    /**
     * Creates a new rule for irregular words.  The created rule applies to any word that is a key
     * in the given map, and returns the value corresponding to that key, as the inflected form.
     *
     * @param mapping the map used to define the irregular forms covered by the resulting rule
     * @return the new rule built from the given irregular mapping
     */
    public static Rule irregulars(final Map<String, String> mapping) {
        final Map<String, String> _mapping = toLowerCaseKeys(mapping);

        return new AbstractRegexRule(disjunction(mapping.keySet())) {
            @Override protected String _applyTo(Matcher matcher) {
                return _mapping.get(matcher.group(0).toLowerCase());
            }

            @Override public String toString() {
                return Objects.toStringHelper(Rule.class)
                        .add("irregulars", shortListStr(_mapping.keySet())).toString();
            }
        };
    }

    /**
     * Equivalent to {@link #irregulars(Map)}.
     */
    public static Rule irregulars(String[][] mapping) {
        return irregulars(toMap(mapping));
    }

    /**
     * Creates a new rule for a single irregular word.  The given rule explicitely states what is
     * the inflected form of the specified word.
     *
     * @param original the original word
     * @param inflected the inflected form of the original word
     * @return the new rule built from the given arguments
     */
    public static Rule irregular(String original, String inflected) {
        return irregulars(toMap(new String[][] {
            { original, inflected }
        }));
    }

    //
    // Internal helper methods and classes
    //

    private static abstract class AbstractRegexRule extends Rule {

        private final Pattern pattern;

        public AbstractRegexRule(Pattern pattern) {
            this.pattern = checkNotNull(pattern);
        }

        public AbstractRegexRule(String pattern, int flags) {
            this(Pattern.compile(pattern, flags));
        }

        public AbstractRegexRule(String pattern, boolean caseInsensitive) {
            this(pattern, caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
        }

        public AbstractRegexRule(String pattern) {
            this(pattern, true);
        }

        @Override
        public boolean appliesTo(String word) {
            return pattern.matcher(word).matches();
        }

        @Override
        protected String _applyTo(String word) {
            final Matcher matcher = pattern.matcher(word);
            checkArgument(matcher.matches());
            return _applyTo(matcher);
        }

        protected abstract String _applyTo(Matcher matcher);

    }

    /**
     * Returns a string representation of the given word list, shortened to 3 items.
     */
    protected static String shortListStr(Iterable<String> words) {
        return Joiner.on(",").join(Iterables.limit(words, 3)).concat(",...");
    }

    /**
     * Returns a copy of the given list of words, with all words converted to
     * {@linkplain String#toLowerCase() lower case}.
     */
    protected static Iterable<String> toLowerCase(Iterable<String> words) {
        return Iterables.transform(words, new Function<String, String>() {
            @Override public String apply(String input) {
                return input.toLowerCase();
            }
        });
    }

    /**
     * Returns a copy of the given map, with all keys converted to
     * {@linkplain String#toLowerCase() lower case}.
     */
    protected static Map<String, String> toLowerCaseKeys(Map<String, String> words) {
        final Map<String, String> result = Maps.newHashMapWithExpectedSize(words.size());
        for (Map.Entry<String, String> entry : words.entrySet()) {
            String previousValue = result.put(entry.getKey().toLowerCase(), entry.getValue());
            checkArgument(previousValue == null, "Illegal irregular mapping");
        }
        return result;
    }

    //
    // Helper methods to manipulate data for rule builders
    //

    private static final Joiner disjunctionJoiner = Joiner.on('|');

    public static String disjunction(String... words) {
        return disjunctionJoiner.join(words);
    }

    public static String disjunction(Iterable<String> words) {
        return disjunctionJoiner.join(words);
    }

    public static String disjunction(String format, String[] words) {
        return String.format(format, disjunctionJoiner.join(words));
    }

    public static String disjunction(String format, Iterable<String> words) {
        return String.format(format, disjunctionJoiner.join(words));
    }

    public static Map<String, String> toMap(String[][] arr, boolean reversed) {
        final int key = reversed ? 1 : 0;
        final Map<String, String> result = Maps.newHashMapWithExpectedSize(arr.length);
        for (String[] entry : arr) {
            String previousValue = result.put(entry[key].toLowerCase(), entry[1-key]);
            checkArgument(previousValue == null, "Illegal irregular mapping");
        }
        return result;
    }

    public static Map<String, String> toMap(String[][] arr) {
        return toMap(arr, false);
    }

    public static BiMap<String, String> toBiMap(String[][] arr, boolean reversed) {
        final int key = reversed ? 1 : 0;
        final BiMap<String, String> result = HashBiMap.create(arr.length);
        for (String[] entry : arr) {
            String previousValue = result.put(entry[key].toLowerCase(), entry[1-key]);
            checkArgument(previousValue == null, "Illegal irregular mapping");
        }
        return result;
    }

    public static BiMap<String, String> toBiMap(String[][] arr) {
        return toBiMap(arr, false);
    }

}
