/*
 * Copyright 2021, Bloomberg Finance L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions: The above copyright
 * notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.bloomberglp.blpapiexamples.demoapps.util.argparser;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Defines a list of {@link Arg}s that conceptually belong to the same group. When help is printed,
 * these {@link Arg}s are grouped together.
 */
public class ArgGroup {
    /**
     * When printing help, align the description at this position, if the full name of the the
     * argument is longer than this position, wrap the description to next line.
     */
    private static final int DESCRIPTION_POSITION = 24;

    protected static final String DESCRIPTION_INDENTATION = getIndentation(DESCRIPTION_POSITION);

    protected final String description;
    protected final List<Arg> args = new ArrayList<>();

    public ArgGroup(String description, Arg... args) {
        if (description == null) {
            throw new IllegalArgumentException("description must not be null");
        }

        this.description = description;
        add(args);
    }

    public String getDescription() {
        return description;
    }

    public ArgGroup add(Arg... newArgs) {
        if (newArgs == null) {
            throw new IllegalArgumentException("args must not be null");
        }

        for (Arg arg : newArgs) {
            this.args.add(arg);
        }

        return this;
    }

    public Arg add(String... forms) {
        Arg arg = new Arg(forms);
        add(arg);
        return arg;
    }

    public List<Arg> getArgs() {
        return Collections.unmodifiableList(args);
    }

    /**
     * Returns the full description of the argument hat includes the description, the default value
     * and if the argument can be specified multiple times.
     */
    private static String getArgFullDescription(Arg arg) {
        StringBuilder sb = new StringBuilder();
        String argDescription = arg.getDescription() == null ? "" : arg.getDescription();

        String[] descriptionLines = argDescription.split("[\r\n]+");
        String firstLineDescription = descriptionLines[0];
        String defaultValue = arg.defaultValueToString();
        String helpAndDefault = (firstLineDescription + " " + defaultValue).trim();
        sb.append(helpAndDefault);

        if (arg.getMode().allowMultipleValues()) {
            if (sb.length() > 0) {
                sb.append(". ");
            }

            sb.append("Can be specified multiple times.");
        }

        // If the description contains new lines, each line is indented.
        String eol = System.lineSeparator();
        for (int i = 1; i < descriptionLines.length; ++i) {
            sb.append(eol).append(DESCRIPTION_INDENTATION).append(descriptionLines[i]);
        }

        if (!arg.getChoices().isEmpty()) {
            if (sb.length() > 0) {

                // Print choices in the next line and indent
                sb.append(eol).append(DESCRIPTION_INDENTATION);
            }

            sb.append("(Choices: " + arg.getChoices() + ")");
        }

        return sb.toString();
    }

    public void printHelp(PrintWriter pw) {
        pw.println(description + ":");

        if (args.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder(200);
        for (Arg arg : args) {
            String fullName = arg.getFullName();
            sb.append("  ").append(fullName); // add 2 space indentation

            String fullDescription = getArgFullDescription(arg);
            if (!fullDescription.isEmpty()) {
                int length = 2 + fullName.length();

                // must have one space in between
                if (length + 2 > DESCRIPTION_POSITION) {

                    // Wrap to next line
                    sb.append(System.lineSeparator());

                    // This is a new line, reset length
                    length = 0;
                }

                // Add indentation up to description position
                sb.append(getIndentation(DESCRIPTION_POSITION - length));

                sb.append(fullDescription);
            }

            pw.println(sb);
            sb.delete(0, sb.length());
        }
    }

    @Override
    public String toString() {
        return description;
    }

    private static String getIndentation(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            sb.append(' ');
        }

        return sb.toString();
    }
}
