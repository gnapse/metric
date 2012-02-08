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

import com.gnapse.common.parser.SyntaxError;
import com.gnapse.metric.ConversionQuery;
import com.gnapse.metric.MetricException;
import com.gnapse.metric.Universe;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * A simple application that reads unit conversion queries from the command-line arguments.
 *
 * @author Ernesto García
 */
public class Main {

    private static final String UNIVERSE_FILE_NAME = "universe.txt";

    private static final String LOGGING_PROPERTIES_FILE_NAME = "logging.properties";

    private static final String APP_FOLDER = getAppFolder();

    private static final Logger LOGGER = Logger.getLogger(Main.class.getCanonicalName());

    /**
     * Application starting point.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            initLogging();
            run(args);
        } catch(MetricException e) {
            LOGGER.severe(e.getMessage());
        } catch(SyntaxError e) {
            LOGGER.severe(e.getMessage());
        } catch(Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Runs the application's core functionality.
     *
     * @param args the command line arguments
     * @throws SyntaxError if a syntax error occurs parsing unit definitions or conversion queries
     * @throws MetricException if a logical error occurs while interpreting and performing the unit
     *     conversion queries
     * @throws IOException if an I/O error occurs while trying to read the unit definition file or
     *     the currency exchange rates
     */
    private static void run(String[] args) throws SyntaxError, MetricException, IOException {
        final File universeFile = copyResource(UNIVERSE_FILE_NAME);
        final Universe universe = new Universe(universeFile);
        final Iterable<String> queries = Splitter.on(",").split(Joiner.on(" ").join(args));
        for (String queryStr : queries) {
            ConversionQuery query = universe.convert(queryStr);
            System.out.println(query.toStringResults());
        }
    }

    /**
     * Loads logging configuration file and initializes logging.
     *
     * @throws IOException if an error occurs while trying to read the logging property file
     */
    private static void initLogging() throws IOException {
        // Set UTF-8 output for standard system streams
        System.setOut(new PrintStream(System.out, true, "UTF-8"));
        System.setErr(new PrintStream(System.err, true, "UTF-8"));

        // Load logging properties file
        final File loggingSettingsFile = copyResource(LOGGING_PROPERTIES_FILE_NAME);
        final InputStream in = new FileInputStream(loggingSettingsFile);
        try {
            LogManager.getLogManager().readConfiguration(in);
        } finally {
            in.close();
        }
    }

    /**
     * Extracts the resource file with the given name to the application folder, if the target file
     * does not exist.
     *
     * @param resourceName the name of the resource file to copy to the application folder
     * @return a reference to the resulting file
     * @throws IOException if an I/O error occurs while performing the copy
     */
    private static File copyResource(String resourceName) throws IOException {
        final File destFile = new File(String.format("%s/%s", APP_FOLDER, resourceName));
        if (!destFile.exists()) {
            Files.createParentDirs(destFile);
            final OutputStream out = new FileOutputStream(destFile);
            try {
                Resources.copy(Resources.getResource(resourceName), out);
            } finally {
                out.close();
            }
        }
        return destFile;
    }

    /**
     * Returns the folder where the application binaries are located.  If the application is being
     * run from a {@code .jar} file, this returns the absolute path of the folder where the
     * {@code .jar} file is located.  If the application is being run directly from the tree
     * structure with compiled {@code .class} files, this method returns the absolute path of the
     * parent of the root folder where the class files tree structure is located.
     *
     * @return the absolute path of the folder where the application's binary files are located
     */
    private static String getAppFolder() {
        try {
            return new File(Main.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath())
                    .getAbsoluteFile().getParentFile().getAbsolutePath();
        } catch (URISyntaxException e) {
            // This shouldn't happen
            throw Throwables.propagate(e);
        }
    }

}
