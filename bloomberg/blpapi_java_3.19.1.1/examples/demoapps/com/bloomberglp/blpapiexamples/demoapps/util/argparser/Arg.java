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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Defines a command line argument. An {@link Arg} is an optional argument that is associated with
 * either a {@link #shortForm}, e.g. -t, or a {@link #longForm}, e.g. --topic, in the command line.
 *
 * <p>An {@link Arg} has the following attributes:
 *
 * <ul>
 *   <li>Can have either a single value by default or multiple values, e.g. multiple topics.
 *   <li>Can have a default value, e.g. {@code "IBM US Equity"} for topic.
 *   <li>Can define an {@link #action} that is invoked when a value is read from the command line.
 *   <li>Optional by default and can be set to required.
 * </ul>
 */
public class Arg {
    private static final char PREFIX_CHAR = '-';

    /**
     * Short form of the argument. Only one prefix char + single char is allowed, the length must be
     * 2, e.g. -t
     */
    private String shortForm;

    /**
     * Long form of the argument. Only 2 prefix chars + multiple chars is allowed, the length must
     * be at least 4, e.g. --topic
     */
    private String longForm;

    /**
     * A name for the argument in usage messages. When the help message is displayed, it is used to
     * refer to the expected argument. e.g.
     *
     * <pre>
     *  -t topic
     * </pre>
     *
     * <p>Here {@code topic} is the {@link #metaVar}.
     */
    private String metaVar;

    private String description;
    private ArgMode mode = ArgMode.SINGLE_VALUE; // single value by default
    private String defaultValue;
    private Consumer<String> action;
    private boolean required = false; // optional by default

    /** Allowed values for this argument. If empty, any value is allowed. */
    private List<String> choices = new ArrayList<>();

    public Arg(String... forms) {
        if (forms == null || forms.length == 0) {
            throw new IllegalArgumentException("no short or long form was specified");
        }

        for (String form : forms) {
            assignForm(form);
        }
    }

    /** Assigns either {@link #shortForm} or {@link #longForm} from {@code form}. */
    private void assignForm(String form) {
        if (form == null) {
            throw new IllegalArgumentException("Null form");
        }

        int len = form.length();
        if (len < 2 // at least 2 chars long
                || form.charAt(0) != PREFIX_CHAR) { // must start with -
            throw new IllegalArgumentException("Invalid form: " + form);
        }

        if (form.charAt(1) == PREFIX_CHAR) {

            // Long form
            if (len < 4) { // should be at least 4 chars long
                throw new IllegalArgumentException("Invalid long form: " + form);
            }

            if (this.longForm != null) {
                throw new IllegalArgumentException("Long form was specified more than once");
            }

            this.longForm = form;
        } else {

            // Short form
            if (len != 2) {
                throw new IllegalArgumentException("Invalid short form: " + form);
            }

            if (this.shortForm != null) {
                throw new IllegalArgumentException("Short form was specified more than once");
            }

            this.shortForm = form;
        }
    }

    public String getShortForm() {
        return shortForm;
    }

    public String getLongForm() {
        return longForm;
    }

    public String getMetaVar() {
        return metaVar;
    }

    public Arg setMetaVar(String metaVar) {
        if (metaVar == null || metaVar.isEmpty()) {
            throw new IllegalArgumentException("metaVar must not be empty");
        }

        this.metaVar = metaVar;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Arg setDescription(String description) {
        this.description = description;
        return this;
    }

    public ArgMode getMode() {
        return mode;
    }

    public Arg setMode(ArgMode mode) {
        this.mode = mode;
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Arg setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    Consumer<String> getAction() {
        return action;
    }

    public Arg setAction(Consumer<String> action) {
        this.action = action;
        return this;
    }

    public boolean isRequired() {
        return required;
    }

    public Arg setRequired(boolean required) {
        this.required = required;
        return this;
    }

    public Arg setChoices(String... values) {
        if (values == null) {
            throw new IllegalArgumentException("choices must not be null");
        }

        for (String c : values) {
            choices.add(c);
        }

        return this;
    }

    public boolean isValidChoice(String value) {
        return choices.isEmpty() || choices.contains(value);
    }

    public List<String> getChoices() {
        return Collections.unmodifiableList(choices);
    }

    /** Returns {@code shortForm, longForm metaVar}, e.g. -t, --topic value */
    String getFullName() {
        String forms;
        if (shortForm == null) {
            forms = longForm;
        } else {
            if (longForm == null) {
                forms = shortForm;
            } else {
                forms = shortForm + ", " + longForm;
            }
        }

        if (!mode.requiresValue()) {
            return forms;
        }

        String mv = metaVar;
        if (mv == null) {
            if (longForm == null) {

                // use "VALUE" if there is no longForm
                mv = "VALUE";
            } else {

                // Use the capitalized word in longForm
                mv = longForm.substring(2).toUpperCase();
            }
        }

        return forms + " " + mv;
    }

    String defaultValueToString() {
        return defaultValue == null ? "" : "(default: " + defaultValue + ")";
    }

    @Override
    public String toString() {
        return "[" + getFullName() + "]";
    }
}
