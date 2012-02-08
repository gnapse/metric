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

import com.gnapse.common.parser.Tokenizer.Token;

/**
 * Signals a parsing error.
 *
 * @author Ernesto García
 */
public class SyntaxError extends Exception {

    private final Token token;

    /**
     * Creates a new syntax error associated to the specified token, and with the given message.
     * @param message the error message
     * @param token the token at which the error occurred
     */
    public SyntaxError(String message, Token token) {
        super(buildMessage(message, token));
        this.token = token;
    }

    /**
     * The token at which the error occurred.
     */
    public Token getToken() {
        return token;
    }

    private static String buildMessage(String message, Token token) {
        final java.io.File sourceFile = (token == null) ? null : token.getFile();
        if (token == null && sourceFile == null) {
            return message;
        }
        if (token == null) {
            return String.format("%s (%s)", message, sourceFile.getName());
        }
        if (sourceFile == null) {
            return String.format("%s (%d:%d)", message,
                    token.getLineNumber(), token.getLinePosition());
        }
        return String.format("%s (%s:%d)", message, sourceFile.getName(), token.getLineNumber());
    }

}
