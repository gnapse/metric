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

package com.gnapse.metric.app;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A simple {@code LogRecord} formatter that will output a simple line with the log message tagged
 * as an error.  This formatter is intended to be used for {@link ConsoleHandler} instances only,
 * since it conveys no extra information about the log record, such as time and date, level, etc.
 * It is also mostly suitable only to show warnings or severe errors, and ignore lower level
 * messages that are not important or relevant enough to be shown to the end user.
 *
 * @author Ernesto García
 */
public class SimpleConsoleFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        return String.format("Error: %s\n", record.getMessage());
    }

}
