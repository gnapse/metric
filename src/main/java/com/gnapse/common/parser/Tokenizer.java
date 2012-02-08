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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;

/**
 * Splits a stream of characters into tokens.
 *
 * @author Ernesto García
 */
public class Tokenizer {

    /**
     * The types of token recognizable by a {@code Tokenizer}.
     */
    public static enum TokenType {
        /**
         * The type of tokens that represents an illegal or invalid sequence while trying to parse
         * some  other token type or comment.  These types of tokens are created to be fed to a
         * {@link SyntaxError} that signals the error.
         */
        ERROR,

        /**
         * The type of tokens that represent a character with no special meaning for a
         * {@link Tokenizer}.
         */
        UNKNOWN,

        /**
         * The type of tokens that represent an end-of-line sequence in the input.  These kind of
         * tokens are only returned if the @{code Tokenizer} was created with the option to
         * recognize and return such tokens.  Otherwise these character sequences are considered
         * whitespace.
         */
        EOL,

        /**
         * The type of token that signals that the input has been completely parsed and there's
         * nothing left to scan.  It follows then that tokens of this type are only returned once by
         * a {@code Tokenizer} and are always the last token returned.
         */
        EOF,

        /**
         * The type for a token representing the character {@code '('}.
         */
        LPAREN,

        /**
         * The type for a token representing the character {@code ')'}.
         */
        RPAREN,

        /**
         * The type for a token representing the character <code>'{'</code>.
         */
        LBRACE,

        /**
         * The type for a token representing the character <code>'}'</code>.
         */
        RBRACE,

        /**
         * The type for a token representing the character {@code '='}.
         */
        EQUALS,

        /**
         * The type for a token representing the character {@code ','}.
         */
        COMMA,

        /**
         * The type for a token representing the character {@code ':'}.
         */
        COLON,

        /**
         * The type for a token representing the character {@code ';'}.
         */
        SEMICOLON,

        /**
         * The type for a token representing the character {@code '+'}.
         */
        PLUS,

        /**
         * The type for a token representing the character {@code '-'}.
         */
        MINUS,

        /**
         * The type for a token representing the character {@code '*'}.
         */
        STAR,

        /**
         * The type for a token representing the character {@code '/'}.
         */
        SLASH,

        /**
         * The type for a token representing the character {@code '^'}.
         */
        CARET,

        /**
         * The type for a token representing the character {@code '$'}.
         */
        DOLLAR,

        /**
         * The type for a token representing a string representation of a numeric value.
         */
        NUMBER,

        /**
         * The type for a token representing a word, consisting of a letter followed by alphanumeric
         * characters.
         */
        WORD,

        /**
         * The type for a token representing a keyword, which are just like words declared "special"
         * by adding them to the set of keywords of the {@code Tokenizer} by using the
         * {@link #addKeywords(java.lang.String[]) addKeywords(String...)} method.
         */
        KEYWORD,
    }

    /**
     * Represents a token parsed by the tokenizer.
     */
    public final class Token {

        public final TokenType type;

        private final Object value;

        private final int position;

        private final int length;

        private final int lineNumber;

        private final int linePosition;

        private Token(TokenType type, int pos, int lineNum, int linePos, int len, Object value) {
            checkArgument(
                    type == TokenType.WORD ||
                    type == TokenType.KEYWORD ||
                    type == TokenType.NUMBER ||
                    type == TokenType.UNKNOWN);

            checkArgument(type != TokenType.WORD    || value instanceof String);
            checkArgument(type != TokenType.KEYWORD || value instanceof String);
            checkArgument(type != TokenType.NUMBER  || value instanceof BigDecimal);
            checkArgument(type != TokenType.UNKNOWN || value instanceof Character);

            if (type == TokenType.WORD || type == TokenType.KEYWORD) {
                checkArgument(new String(input, pos, len).equals(value));
            }

            if (type == TokenType.UNKNOWN) {
                checkArgument(len == 1);
                checkArgument(value.equals(input[pos]));
            }

            this.type = type;
            this.position = pos;
            this.lineNumber = lineNum;
            this.linePosition = linePos;
            this.length = len;
            this.value = checkNotNull(value);
        }

        private Token(TokenType type, int pos, int lineNum, int linePos) {
            checkArgument(
                    type != TokenType.WORD    &&
                    type != TokenType.KEYWORD &&
                    type != TokenType.NUMBER  &&
                    type != TokenType.UNKNOWN);
            this.type = type;
            this.position = pos;
            this.lineNumber = lineNum;
            this.linePosition = linePos;
            this.length = 1;
            this.value = null;
        }

        private Token() {
            this.type = TokenType.ERROR;
            this.position = Tokenizer.this.pos;
            this.lineNumber = Tokenizer.this.lineNumber;
            this.linePosition = Tokenizer.this.linePosition;
            this.length = 1;
            this.value = null;
        }

        @Override
        public String toString() {
            if (type == TokenType.UNKNOWN) {
                return String.format("%s '%c'", type.toString(), value);
            }
            if (value != null) {
                String str = new String(input, this.position, this.length);
                return String.format("%s '%s'", type.toString(), str);
            }
            return type.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof Token)) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            final Token t = (Token)obj;
            return t.getTokenizer() == this.getTokenizer() &&
                    t.position == this.position &&
                    t.lineNumber == this.lineNumber &&
                    t.linePosition == this.linePosition &&
                    t.length == this.length &&
                    t.type == this.type &&
                    Objects.equal(t.value, this.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.getTokenizer(), position,
                    lineNumber, linePosition, length, type, value);
        }

        /**
         * The position or index where this token starts in the input.
         */
        public int getPosition() {
            return position;
        }

        /**
         * The line number where this token is located in the input.
         */
        public int getLineNumber() {
            return lineNumber;
        }

        /**
         * The position of this token in the line it appears.
         */
        public int getLinePosition() {
            return linePosition;
        }

        /**
         * The tokenizer that generated this token.
         */
        public Tokenizer getTokenizer() {
            return Tokenizer.this;
        }

        /**
         * The file where this token is located, or {@code null} if this token's tokenizer is
         * parsing from a string and not from a file's contents.
         */
        public File getFile() {
            return Tokenizer.this.file;
        }

        /**
         * The length or number of characters of this token's representation in the input.
         */
        public int getLength() {
            return length;
        }

        /**
         * The string value of the word represented by this token.
         * @throws SyntaxError if this token is not of type {@link TokenType#WORD}
         */
        public String getWord() throws SyntaxError {
            checkType(TokenType.WORD);
            return (String)value;
        }

        /**
         * The string value of the keyword represented by this token.
         * @throws SyntaxError if this token is not of type {@link TokenType#KEYWORD}
         */
        public String getKeyword() throws SyntaxError {
            checkType(TokenType.KEYWORD);
            return (String)value;
        }

        /**
         * The numeric value of the number represented by this token.
         * @throws SyntaxError if this token is not of type {@link TokenType#NUMBER}
         */
        public BigDecimal getNumber() throws SyntaxError {
            checkType(TokenType.NUMBER);
            return (BigDecimal)value;
        }

        /**
         * The unknown character represented by this token.
         * @throws SyntaxError if this token is not of type {@link TokenType#UNKNOWN}
         */
        public Character getUnknownCharacter() throws SyntaxError {
            checkType(TokenType.UNKNOWN);
            return (Character)value;
        }

        /**
         * The value associated to this token, or {@code null} if no value is associated with it.
         */
        public Object getValue() {
            return value;
        }

        /**
         * Determines if this token it of the specified type.
         */
        public boolean is(TokenType type) {
            return this.type == type;
        }

        /**
         * Determines if this token is a {@linkplain TokenType#WORD word} or a
         * {@linkplain TokenType#KEYWORD keyword} with the given string value.
         */
        public boolean is(String word) {
            return isOneOf(TokenType.KEYWORD, TokenType.WORD) && value.equals(word);
        }

        /**
         * Determines if this token's type is one of the specified types.
         */
        public boolean isOneOf(TokenType... types) {
            for (TokenType t : types) {
                if (type == t) return true;
            }
            return false;
        }

        /**
         * Determines if this token is a {@linkplain TokenType#WORD word} or a
         * {@linkplain TokenType#KEYWORD keyword} whose value is one of the specified string values.
         */
        public boolean isOneOf(String... words) {
            if (!isOneOf(TokenType.KEYWORD, TokenType.WORD)) return false;
            for (String word : words) {
                if (word.equals(value)) return true;
            }
            return false;
        }

        /**
         * Determines if this token is a {@linkplain TokenType#WORD word} or a
         * {@linkplain TokenType#KEYWORD keyword} whose value is one of the specified string values.
         */
        public boolean isOneOf(Iterable<String> words) {
            if (type != TokenType.WORD) return false;
            for (String word : words) {
                if (word.equals(value)) return true;
            }
            return false;
        }

        /**
         * Checks that this token's type is one of the specified types.
         * @param types the list of types to check against this token's type
         * @throws SyntaxError if this token's type is not one of the specified types
         */
        public void checkType(TokenType... types) throws SyntaxError {
            if (!isOneOf(types)) {
                final String message = String.format("Unexpected token %s", this);
                throw new SyntaxError(message, this);
            }
        }

        public Token next() throws SyntaxError {
            return Tokenizer.this.nextToken();
        }

    } // class Token

    private final char[] input;

    private File file;

    private int pos = 0;

    private int lineNumber = 1;

    private int linePosition = 1;

    private final int inputLength;

    private final boolean returnEolToken;

    private Token currentToken = null;

    private Set<String> keywords = Sets.newHashSet();

    /**
     * Creates a new tokenizer for the given input string.  The second parameter specifies if this
     * tokenizer should recognize end-of-line character sequences as tokens.
     *
     * @param input the string to be split into tokens.
     * @param returnEolToken if {@code true} then this tokenizer recognizes end-of-line character
     *     sequences and return them as a token of type {@link TokenType#EOL}; otherwise these
     *     character sequences are ignored as whitespace
     */
    public Tokenizer(String input, boolean returnEolToken) {
        this.input = input.toCharArray();
        this.inputLength = this.input.length;
        this.returnEolToken = returnEolToken;
    }

    /**
     * Creates a new tokenizer for the given input file.  The second parameter specifies if this
     * tokenizer should recognize end-of-line character sequences as tokens.
     *
     * @param file the file whose contents will be parsed by this tokenizer
     * @param returnEolToken if {@code true} then this tokenizer recognizes end-of-line character
     *     sequences and return them as a token of type {@link TokenType#EOL}; otherwise these
     *     character sequences are ignored as whitespace
     * @throws IOException if some error occurs while trying to read the contents of the file
     */
    public Tokenizer(File file, boolean returnEolToken) throws IOException {
        this(Files.toString(file, Charsets.UTF_8), returnEolToken);
        this.file = file;
    }

    /**
     * Creates a new tokenizer for the given input string, and ignoring end-of-line character
     * sequences.
     *
     * @param input the string to be split into tokens
     */
    public Tokenizer(String input) {
        this(input, false);
    }

    /**
     * Creates a new tokenizer for the contents of the given file, and ignoring end-of-line
     * character sequences.
     *
     * @param file the file whose contents will be parsed by this tokenizer
     * @throws IOException if some error occurs while trying to read the contents of the file
     */
    public Tokenizer(File file) throws IOException {
        this(file, false);
    }

    /**
     * Allows client classes to "go back and forward in time" and start parsing again from the given
     * token on.  As a result of this method call this tokenizer returns to the state it was when
     * the given token was the {@linkplain #currentToken() current token}.
     *
     * @param tok the token representing the state to which this tokenizer will set itself
     * @throws IllegalArgumentException if the given token is from another tokenizer
     */
    public void setCurrentToken(Token tok) {
        checkArgument(tok.getTokenizer() == this);
        this.pos = tok.position + tok.length;
        this.linePosition = tok.linePosition + tok.length;
        this.lineNumber = tok.lineNumber;
        this.currentToken = tok;
    }

    /**
     * Adds the given strings to be recognized as keywords by this tokenizer.
     * @param keywords the list of strings to be recognized as keywords
     */
    public void addKeywords(String... keywords) {
        this.keywords.addAll(Arrays.asList(keywords));
    }

    /**
     * Removes the given strins to no longer be recognized as keywords by this tokenizer.
     * @param keywords the list of strings to be removed from the keywords list
     */
    public void removeKeywords(String... keywords) {
        this.keywords.removeAll(Arrays.asList(keywords));
    }

    /**
     * Determines if the given word is considered a keyword by this tokenizer.
     * @param str the word to check for against the list of keywords of this tokenizer
     * @return {@code true} if the given word is a keyword in this tokenizer; {@code false}
     *     otherwise
     */
    public boolean isKeyword(String str) {
        return this.keywords.contains(str);
    }

    private Token createToken(TokenType type, String value) {
        final int len = value.length();
        final int start = pos - len + 1;
        currentToken = new Token(type, start, lineNumber, linePosition - len + 1, len, value);
        return currentToken;
    }

    private Token createToken(TokenType type, char c) {
        currentToken = new Token(type, pos, lineNumber, linePosition, 1, c);
        return currentToken;
    }

    private Token createToken(TokenType type, int start, int len, Object value) {
        currentToken = new Token(type, start, lineNumber, linePosition - len + 1, len, value);
        return currentToken;
    }

    private Token createToken(TokenType type) {
        currentToken = new Token(type, pos, lineNumber, linePosition);
        return currentToken;
    }

    private boolean isWhitespace(char c) {
        if (c == '\r' || c == '\n') {
            return false;
        }
        return Character.isWhitespace(c);
    }

    private void nextChar() throws SyntaxError {
        if (pos >= inputLength) {
            throw new SyntaxError("Unexpected end-of-file", currentToken);
        }
        ++pos;
        ++linePosition;
    }

    /**
     * Parses and returns the next token in the input.
     *
     * @throws SyntaxError if while trying to parse the next token this tokenizer reaches the end
     *     of file unexpectedly, or if it founds an incorrect syntax or unexpected and invalid
     *     character in the middle of parsing the next token
     */
    public Token nextToken() throws SyntaxError {
        checkState(currentToken == null || currentToken.type != TokenType.EOF);

        Token result = null;

        while (result == null) {
            while (pos < inputLength && isWhitespace(input[pos])) {
                nextChar();
            }

            if (pos >= inputLength) {
                return createToken(TokenType.EOF);
            }

            switch (input[pos]) {
                case '\n':
                    if (pos > 0 && input[pos-1] == '\r') {
                        ++pos;
                        break;
                    }
                    // Intentionally pass through the next case
                case '\r':
                    if (returnEolToken) {
                        result = createToken(TokenType.EOL);
                    }
                    ++lineNumber;
                    linePosition = 0;
                    break;
                case '(':
                    result = createToken(TokenType.LPAREN);
                    break;
                case ')':
                    result = createToken(TokenType.RPAREN);
                    break;
                case '{':
                    result = createToken(TokenType.LBRACE);
                    break;
                case '}':
                    result = createToken(TokenType.RBRACE);
                    break;
                case '=':
                    result = createToken(TokenType.EQUALS);
                    break;
                case ',':
                    result = createToken(TokenType.COMMA);
                    break;
                case ':':
                    result = createToken(TokenType.COLON);
                    break;
                case ';':
                    result = createToken(TokenType.SEMICOLON);
                    break;
                case '+':
                    result = createToken(TokenType.PLUS);
                    break;
                case '-':
                    result = createToken(TokenType.MINUS);
                    break;
                case '*':
                    result = createToken(TokenType.STAR);
                    break;
                case '$':
                    result = createToken(TokenType.DOLLAR);
                    break;
                case '#':
                    skipLineComment();
                    break;
                case '/':
                    if (pos + 1 < inputLength) {
                        if (input[pos+1] == '/') {
                            skipLineComment();
                            break;
                        }
                        if (input[pos+1] == '*') {
                            skipBlockComment();
                            break;
                        }
                    }
                    result = createToken(TokenType.SLASH);
                    break;
                case '^':
                    result = createToken(TokenType.CARET);
                    break;
                case '.':
                    result = parseNumber();
                    break;
                default:
                    if (Character.isDigit(input[pos])) {
                        result = parseNumber();
                    } else if (Character.isJavaIdentifierStart(input[pos])) {
                        result = parseWord();
                    } else {
                        result = createToken(TokenType.UNKNOWN, input[pos]);
                    }
            } // end switch
            nextChar();
        } // end while

        return result;

    }

    /**
     * The line number where this tokenizer is currently parsing.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * The position of this tokenizer within the line where is currently parsing.
     */
    public int getLinePosition() {
        return linePosition;
    }

    /**
     * The current token.
     */
    public Token currentToken() {
        return currentToken;
    }

    /**
     * The type of the current token.
     */
    public TokenType currentType() {
        checkState(currentToken != null);
        return currentToken.type;
    }

    /**
     * Ensures that the current token is of the given type.
     *
     * @param type the type that is expected the current token to be.
     * @throws SyntaxError if the current token is not of the expected type.
     */
    public void checkTokenType(TokenType type) throws SyntaxError {
        if (currentToken.type != type) {
            final String message = String.format("Unexpected token: %s", currentToken);
            throw new SyntaxError(message, currentToken);
        }
    }

    private Token parseWord() throws SyntaxError {
        checkState(Character.isJavaIdentifierStart(input[pos]));

        final int start = pos;
        do {
            nextChar();
        } while (pos < inputLength && Character.isJavaIdentifierPart(input[pos]));

        while (pos + 1 < inputLength && input[pos] == '-' &&
                Character.isJavaIdentifierStart(input[pos+1])) {
            nextChar();
            do {
                nextChar();
            } while (pos < inputLength && Character.isJavaIdentifierPart(input[pos]));
        }

        final String word = new String(input, start, pos - start);
        final TokenType type = keywords.contains(word) ? TokenType.KEYWORD : TokenType.WORD;

        // Skip back to the last character that's part of the token
        --pos;
        --linePosition;

        return createToken(type, word);
    }

    private Token parseNumber() throws SyntaxError {
        checkState(Character.isDigit(input[pos]) || input[pos] == '.');

        final int start = pos;

        // Parse the integer part
        if (input[pos] != '.') {
            do {
                nextChar();
            } while (pos < inputLength && (Character.isDigit(input[pos]) ||
                    input[pos] == '_' || input[pos] == '\''));
        }

        // Parse the fractional part, if any
        if (pos < inputLength && input[pos] == '.') {
            nextChar(); // skip the dot
            if (pos >= inputLength || !Character.isDigit(input[pos])) {
                throw new SyntaxError("Invalid number format", new Token());
            }
            nextChar(); // skip the first digit after the dot
            while (pos < inputLength &&  Character.isDigit(input[pos])) {
                nextChar();
            }
        }

        // Parse the exponent part, if any
        if (pos < inputLength && (input[pos] == 'e' || input[pos] == 'E')) {
            nextChar(); // skip the 'e'...
            nextChar(); // ...and the character after it
            if (input[pos-1] == '+' || input[pos-1] == '-') {
                nextChar();
            }
            if (!Character.isDigit(input[pos-1])) {
                throw new SyntaxError("Invalid number format", new Token());
            }
            while (pos < inputLength && Character.isDigit(input[pos])) {
                nextChar();
            }
        }

        // The number cannot be followed by a letter or a dot
        if (pos < inputLength &&
                (Character.isJavaIdentifierStart(input[pos]) || input[pos] == '.')) {
            throw new SyntaxError("Invalid number format", new Token());
        }

        final String str = new String(input, start, pos - start);
        final BigDecimal num = new BigDecimal(str.replaceAll("[_']", ""));

        // Skip back to the last character that's part of the token
        --pos;
        --linePosition;

        return createToken(TokenType.NUMBER, start, pos - start + 1, num);
    }

    private void skipLineComment() throws SyntaxError {
        if (input[pos] != '#') {
            checkState(pos + 1 < inputLength && input[pos] == '/' && input[pos+1] == '/');
        }
        do {
            nextChar();
        } while (pos + 1 < inputLength && input[pos+1] != '\r' && input[pos+1] != '\n');
    }

    private void skipBlockComment() throws SyntaxError {
        checkState(pos + 1 < inputLength && input[pos] == '/' && input[pos+1] == '*');

        // Skip the two comment-opening characters
        nextChar();
        nextChar();

        // Scan for the two closing characters and continue to keep track of line number and pos
        while (pos < inputLength) {
            switch (input[pos]) {
                case '\n':
                    if (pos > 0 && input[pos-1] == '\r') {
                        ++pos;
                        break;
                    }
                    // Intentionally pass through the next case
                case '\r':
                    ++lineNumber;
                    linePosition = 0;
                    break;
                case '*':
                    if (pos + 1 < inputLength && input[pos+1] == '/') {
                        nextChar();
                        return;
                    }
            }
            nextChar();
        }

        throw new SyntaxError("Unexpected end of file inside comment block", new Token());
    }

}
