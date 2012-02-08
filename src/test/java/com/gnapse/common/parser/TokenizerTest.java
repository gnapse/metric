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

package com.gnapse.common.parser;

import static org.junit.Assert.*;

import com.gnapse.common.parser.Tokenizer.Token;
import com.gnapse.common.parser.Tokenizer.TokenType;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Some test cases for the {@link Tokenizer} class.
 *
 * @author Ernesto García
 */
public class TokenizerTest {

    public TokenizerTest() {
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

    /**
     * Tests if the Tokenizer recognizes keywords correctly.
     */
    @Test
    public void testKeywords() throws SyntaxError {
        final Tokenizer tokenizer = new Tokenizer("public final void dummy(final String void)");
        tokenizer.addKeywords("public", "void");

        Token tok = tokenizer.nextToken();
        assertEquals(TokenType.KEYWORD, tok.type);
        assertEquals("public", tok.getKeyword());

        tok = tok.next();
        assertEquals(TokenType.WORD, tok.type);
        assertEquals("final", tok.getWord());

        tok = tok.next();
        assertEquals(TokenType.KEYWORD, tok.type);
        assertEquals("void", tok.getKeyword());

        tokenizer.addKeywords("final");
        tokenizer.removeKeywords("void");

        tok = tok.next(); // skip 'dummy'
        tok = tok.next(); // skip LPAREN

        tok = tok.next();
        assertEquals(TokenType.KEYWORD, tok.type);
        assertEquals("final", tok.getKeyword());

        tok = tok.next(); // skip 'String'

        tok = tok.next();
        assertEquals(TokenType.WORD, tok.type);
        assertEquals("void", tok.getWord());
    }

    /**
     * Tests if the Tokenizer keeps correct track of line numbers in all situations
     */
    @Test
    public void testLineNumbers() throws SyntaxError {
        final String text = "One\nTwo 2\r\nThree //3\rFour\n\rSix/*\nSeven\n*/Eight";
        final Tokenizer tokenizer = new Tokenizer(text);

        Token tok = tokenizer.nextToken(); // WORD 'One'
        assertEquals(1, tokenizer.getLineNumber());
        assertEquals(1, tok.getLineNumber());

        tok = tok.next(); // WORD 'Two'
        assertEquals(2, tokenizer.getLineNumber());
        assertEquals(2, tok.getLineNumber());

        tok = tok.next(); // NUMBER '2'
        assertEquals(2, tokenizer.getLineNumber());
        assertEquals(2, tok.getLineNumber());

        tok = tok.next(); // WORD 'Three'
        assertEquals(3, tokenizer.getLineNumber());
        assertEquals(3, tok.getLineNumber());

        tok = tok.next(); // WORD 'Four'
        assertEquals(4, tokenizer.getLineNumber());
        assertEquals(4, tok.getLineNumber());

        tok = tok.next(); // WORD 'Six'
        assertEquals(6, tokenizer.getLineNumber());
        assertEquals(6, tok.getLineNumber());

        tok = tok.next(); // WORD 'Eight'
        assertEquals(8, tokenizer.getLineNumber());
        assertEquals(8, tok.getLineNumber());

        tok = tok.next(); // EOF
        assertEquals(8, tokenizer.getLineNumber());
        assertEquals(8, tok.getLineNumber());
    }

    /**
     * Tests if the Tokenizer keeps correct track of line position in all situations
     */
    @Test
    public void testLinePositions() throws SyntaxError {
        final Tokenizer tokenizer = new Tokenizer("One Five Ten\n 2  5  Eight 14");

        Token tok = tokenizer.nextToken(); // WORD 'One'
        assertEquals(1, tok.getLinePosition());

        tok = tok.next(); // WORD 'Five'
        assertEquals(5, tok.getLinePosition());

        tok = tok.next(); // WORD 'Ten'
        assertEquals(10, tok.getLinePosition());

        tok = tok.next(); // NUMBER '2'
        assertEquals(2, tok.getLinePosition());

        tok = tok.next(); // NUMBER '5'
        assertEquals(5, tok.getLinePosition());

        tok = tok.next(); // WORD 'Eight'
        assertEquals(8, tok.getLinePosition());

        tok = tok.next(); // NUMBER '14'
        assertEquals(14, tok.getLinePosition());

        tok = tok.next(); // EOF
        assertEquals(16, tok.getLinePosition());
    }

    /**
     * Tests if the Tokenizer parses words correctly.
     */
    @Test
    public void testParsingWords() throws SyntaxError {
        final String text = "first-word second-2\nthird3\r-fourth4" +
                "/* no words */fifth--sixth final7-word8 # 5 more extra comment words here";
        final Tokenizer tokenizer = new Tokenizer(text);

        Token tok = tokenizer.nextToken(); // WORD 'first-word'
        assertEquals("first-word", tok.getWord());

        tok = tok.next(); // WORD 'second'
        assertEquals("second", tok.getWord());

        tok = tok.next(); // MINUS '-'
        tok = tok.next(); // NUMBER '2'

        tok = tok.next(); // WORD 'third3'
        assertEquals("third3", tok.getWord());

        tok = tok.next(); // MINUS '-'

        tok = tok.next(); // WORD 'fourth4'
        assertEquals("fourth4", tok.getWord());

        tok = tok.next(); // WORD 'fifth'
        assertEquals("fifth", tok.getWord());

        tok = tok.next(); // MINUS '-'
        tok = tok.next(); // MINUS '-'

        tok = tok.next(); // WORD 'sixth'
        assertEquals("sixth", tok.getWord());

        tok = tok.next(); // WORD 'final7-word8'
        assertEquals("final7-word8", tok.getWord());

        tok = tok.next(); // EOF
        assertEquals(TokenType.EOF, tok.type);
    }

    /**
     * Test if the Tokenizer parses numbers correctly.
     */
    @Test
    public void testParsingNumbers() throws SyntaxError {
        final String text = "0.0 11e-1 2.2e0//4 5.5e-3\n3.3/*5.5\n4 ignored*/.44e1 5.5 and6";
        final Tokenizer tokenizer = new Tokenizer(text);
        Token tok = tokenizer.nextToken();

        for (char c = '0'; c <= '5'; ++c) {
            BigDecimal expectedNumber = new BigDecimal(String.format("%s.%s", c, c));
            assertEquals(expectedNumber, tok.getNumber());
            tok = tok.next();
        }

        assertEquals("and6", tok.getWord());
        assertEquals(TokenType.EOF, tok.next().type);
    }

    /**
     * Test if the Tokenizer recognizes incorrect numbers.
     */
    @Test
    public void testParsingIncorrectNumbers() {
        final Iterable<String> numbers = Splitter.on(' ').split("12et 345t 72ee 216e 34.5.2 23.");

        for (String num : numbers) {
            Tokenizer tokenizer = new Tokenizer(num);
            try {
                Token tok = tokenizer.nextToken();
                fail(String.format("Should fail trying to parse a number from the token %s", tok));
            } catch(SyntaxError ex) {
            }
        }
    }

    private static final String text = new StringBuilder()
            .append("/**")
            .append(" * Force is what changes an object's velocity or shape.")
            .append(" */")
            .append("force =mass*acceleration ${\n")
            .append("  newton,Newton (N,n)=.31 kg m per s^2;\n")
            .append("  dyne= 1e-5 g*cm/s^ 2;\n")
            .append("  cc=cubic centimeter;\n")
            .append("  fahrenheit =-5/9 K+273.15;")
            .append("}\n")
            .toString();

    private static final Object[] expectedArr = new Object[] {
        // force = mass * acceleration ${
        "force", TokenType.EQUALS, "mass", TokenType.STAR, "acceleration",
        TokenType.DOLLAR, TokenType.LBRACE,

        // newton,Newton (N,n)
        "newton", TokenType.COMMA, "Newton",
        TokenType.LPAREN, "N", TokenType.COMMA, "n", TokenType.RPAREN,

        // = .31 kg m per s^2;
        TokenType.EQUALS, new BigDecimal(".31"),
        "kg", "m", "per", "s", TokenType.CARET, new BigDecimal(2),
        TokenType.SEMICOLON,

        // dyne = 1e-5 g* cm /s^ 2;
        "dyne", TokenType.EQUALS, new BigDecimal("1e-5"), "g", TokenType.STAR,
        "cm", TokenType.SLASH, "s", TokenType.CARET, new BigDecimal(2),
        TokenType.SEMICOLON,

        // cc = cubic centimeter;
        "cc", TokenType.EQUALS, "cubic", "centimeter",
        TokenType.SEMICOLON,

        // fahrenheit =-5/9 K+273.15;
        "fahrenheit", TokenType.EQUALS, TokenType.MINUS, new BigDecimal(5),
        TokenType.SLASH, new BigDecimal(9), "K", TokenType.PLUS, new BigDecimal("273.15"),
        TokenType.SEMICOLON,

        // }
        TokenType.RBRACE,
        TokenType.EOF,
    };

    @Test
    public void testIntegrality() throws SyntaxError {
        final Tokenizer t = new Tokenizer(text);
        t.addKeywords("cubic", "per");

        // Parse all tokens and get them in a list
        final List<Token> tokens = Lists.newArrayList();
        Token tok;
        do {
            tok = t.nextToken();
            tokens.add(tok);
        } while (!tok.is(TokenType.EOF));

        int len = tokens.size();
        assertEquals(expectedArr.length, len);
        for (int i = 0; i < len; ++i) {
            Object expected = expectedArr[i];
            Object actual;
            tok = tokens.get(i);
            if (expected instanceof TokenType) {
                actual = tok.type;
            } else if (expected instanceof BigDecimal) {
                actual = tok.getNumber();
            } else if (expected instanceof String) {
                actual = t.isKeyword((String)expected) ? tok.getKeyword() : tok.getWord();
            } else {
                fail("Invalid expected value");
                actual = null;
            }
            assertEquals(expected, actual);
        }
    }

    /**
     * Tests if the tokenizer correctly sets it self to a different token position.
     */
    @Test
    public void testSetCurrentToken() throws SyntaxError {
        final Tokenizer t = new Tokenizer(text);

        // Parse all tokens and get them in a list
        final List<Token> tokens = Lists.newArrayList();
        Token tok;
        do {
            tok = t.nextToken();
            tokens.add(tok);
        } while (!tok.is(TokenType.EOF));

        // Obtain a permutation of 0..n-1
        final Random random = new Random();
        int length = tokens.size();
        int[] permutation = new int[length];
        for (int i = 0; i < length; ++i) {
            permutation[i] = i;
        }
        for (int i = length; i > 1; ) {
            final int j = random.nextInt(i);
            --i;
            // swap i <-> j
            int tmp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = tmp;
        }

        // Iterate over tokens in the permutation's order
        for (int i = 0; i < length; ++i) {
            int pos = permutation[i];
            t.setCurrentToken(tokens.get(pos));
            if (pos + 1 == length) {
                try {
                    t.nextToken();
                    fail("nextToken while at EOF should have thrown an IllegalStateException");
                } catch (IllegalStateException e) {
                }
            } else {
                assertEquals(tokens.get(pos+1), t.nextToken());
            }
        }
    }

}
