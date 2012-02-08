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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An implementation of the {@link Inflector} interface that keeps a list of inflection
 * {@linkplain Rule rules} that define how to inflect words.
 *
 * @author Ernesto García
 */
@Beta
public class RuleBasedInflector implements Inflector {

    /**
     * A list containing the rules that define this {@code Inflector}.  The priority of each rule is
     * determined by their order in the list, with higher priority rules coming first.
     */
    protected final List<Rule> rules = Lists.newLinkedList();

    /**
     * Creates a new {@code RuleBasedInflector} with no rules.
     */
    public RuleBasedInflector() {
    }

    /**
     * Creates a new {@code RuleBasedInflector} with the given set of rules.  The rules in the given
     * list are expected to be listed from higher to lower priority.
     *
     * @param rules the list of rules to use in this {@code Inflector}.
     */
    public RuleBasedInflector(Iterable<Rule> rules) {
        Iterables.addAll(this.rules, rules);
    }

    /**
     * Equivalent to {@link #RuleBasedInflector(Iterable)}.
     */
    public RuleBasedInflector(Rule... rules) {
        this.rules.addAll(Arrays.asList(rules));
    }

    /**
     * Adds a new rule to this {@code Inflector}.  The new rule will be processed before all other
     * rules that were in the list before this call.
     *
     * @param rule the new rule to add to this {@code Inflector}
     */
    public final void addRule(Rule rule) {
        rules.add(0, rule);
    }

    /**
     * Adds new rules to this {@code Inflector}.  The new rules must be listed from higher to lower
     * priority.
     *
     * @param newRules the list of new rules to add to this {@code Inflector}
     */
    public final void addRules(Iterable<Rule> newRules) {
        List<Rule> reversed = ImmutableList.copyOf(newRules).reverse();
        for (Rule rule : reversed) {
            rules.add(0, rule);
        }
    }

    /**
     * Equivalent to {@link #addRules(Iterable)}.
     */
    public final void addRules(Rule... newRules) {
        addRules(Arrays.asList(newRules));
    }

    @Override
    public final String apply(String word) {
        final Pattern pattern = Pattern.compile("\\A(\\s*)(.+?)(\\s*)\\Z");
        final Matcher matcher = pattern.matcher(word);
        if (matcher.matches()) {
            final String pre = matcher.group(1);
            final String originalWord = matcher.group(2);
            final String post = matcher.group(3);
            final String inflectedWord = applyRules(originalWord);
            return new StringBuilder()
                    .append(pre)
                    .append(matchCase(originalWord, inflectedWord))
                    .append(post)
                    .toString();
        }
        return word;
    }

    /**
     * Scans the list of rules and applies the first one that applies to the specified word.
     *
     * @param word the word to apply the rules to
     * @return the word inflected by the first rule that applies in the list, or the unmodified
     *     word if no rule applies.  The fact that the word is returned unmodified does not imply
     *     that no rule applied, since there could exist rules that specify that the inflected form
     *     of that word is the word itself.
     */
    private String applyRules(String word) {
        for (Rule rule : rules) {
            if (rule.appliesTo(word)) {
                return rule.applyTo(word);
            }
        }
        return word;
    }

    /**
     * Returns the inflected word with a letter casing that is akin to the letter casing of the
     * original word.  If the original word was capitalized, then the inflected word is returned
     * capitalize.  If the original word was all uppercase, then the inflected word is returned all
     * uppercase.
     *
     * @param originalWord the original word
     * @param inflectedWord the inflected word
     * @return the inflected word modified to have the same letter casing than the original word
     */
    private String matchCase(String originalWord, String inflectedWord) {
        checkNotNull(inflectedWord, "\"%s\" inflected to null", originalWord);
        if (!originalWord.matches("^.*\\p{Ll}.*$")) {
            return inflectedWord.toUpperCase();
        } else if (originalWord.matches("^\\p{Lu}.*")) {
            return inflectedWord.substring(0, 1).toUpperCase() + inflectedWord.substring(1);
        }
        return inflectedWord;
    }

}
