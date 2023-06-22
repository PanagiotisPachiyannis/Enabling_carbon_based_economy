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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parses command line arguments. To set up {@link ArgParser}, call {@link ArgParser#addArg} to add
 * a single argument or {@link #addGroup(ArgGroup)} to add a group of arguments.
 *
 * <p>When parsing command line arguments, first collects the values of each argument and invokes
 * the argument action if defined, then applies the default value if an argument has defined a
 * default value but received no values from the command line.
 */
public class ArgParser {

    /** An {@link ArgGroup} whose arguments don't belong to any other groups. */
    private final ArgGroup argGroupGeneral;

    private final String title;
    private final Class<?> exampleClass;

    private final List<Arg> args = new ArrayList<>();

    /**
     * A map of the short form and the long form of an argument to the index of the argument in
     * {@link #args}.
     */
    private final Map<String, Integer> argIndices = new HashMap<>();

    private final List<ArgGroup> argGroups = new ArrayList<>();

    /** A map of the index of an argument to the values from the command line. */
    private Map<Integer, List<String>> values = new HashMap<>();

    public ArgParser(String title, Class<?> exampleClass) {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(exampleClass, "exampleClass must not be null");

        this.title = title;
        this.exampleClass = exampleClass;

        // Add general group with help argument by default.
        Arg argHelp =
                new Arg("-h", "--help")
                        .setDescription("Show this help message and exit")
                        .setMode(ArgMode.NO_VALUE)
                        .setAction(
                                value -> {

                                    // Print help message and exit
                                    printHelp();
                                    System.exit(0);
                                });
        argGroupGeneral = new ArgGroup("General", argHelp);
        addGroup(argGroupGeneral);
    }

    /** Adds an argument specified by {@code forms} and puts it in the general group. */
    public Arg addArg(String... forms) {
        Arg arg = new Arg(forms);
        validateAndAdd(arg);
        argGroupGeneral.add(arg);
        return arg;
    }

    public ArgParser addGroup(ArgGroup group) {
        if (group.getArgs().isEmpty()) {
            throw new IllegalArgumentException("No arguments are defined in group " + group);
        }

        group.getArgs().forEach(this::validateAndAdd);
        argGroups.add(group);
        return this;
    }

    public void parse(String[] cmdLineArgs) {
        for (int i = 0; i < cmdLineArgs.length; ++i) {
            String option = cmdLineArgs[i];

            Integer index = argIndices.get(option);
            if (index == null) {
                throw new IllegalArgumentException("Unknown argument " + option);
            }

            Arg arg = args.get(index);
            List<String> argValues = values.computeIfAbsent(index, k -> new ArrayList<>());

            ArgMode argMode = arg.getMode();
            if (!argMode.allowMultipleValues() && !argValues.isEmpty()) {
                throw new IllegalArgumentException(
                        "Option " + arg + " must be specified only once.");
            }

            String value = null;
            if (argMode.requiresValue()) {
                if (i + 1 == cmdLineArgs.length) {
                    throw new IllegalArgumentException("Option " + arg + " is missing a value");
                }

                value = cmdLineArgs[++i];

                // Verify if the value is one of the choices
                if (!arg.isValidChoice(value)) {
                    throw new IllegalArgumentException(
                            "Option "
                                    + arg
                                    + ": invalid choice '"
                                    + value
                                    + "' (choose from "
                                    + arg.getChoices()
                                    + ")");
                }
            }

            // if an argument requires no value, add null to values to
            // know how many times it is specified, useful for -verbose
            argValues.add(value);

            // Invoke the action if it exists
            if (arg.getAction() != null) {
                arg.getAction().accept(value);
            }
        }

        // Validate required arguments and apply the default value if the
        // argument is not present but has a default value.
        for (int index = 0; index < args.size(); ++index) {
            List<String> argValues = values.get(index);
            if (argValues != null && !argValues.isEmpty()) {
                continue;
            }

            // Throw if no values from command line or no default value if the
            // argument is required.
            Arg arg = args.get(index);
            String defaultValue = arg.getDefaultValue();
            if (defaultValue == null) {
                if (arg.isRequired()) {
                    throw new IllegalArgumentException("Missing option " + arg);
                }
            } else {

                // Apply the default value
                argValues = new ArrayList<>();
                values.put(index, argValues);

                argValues.add(defaultValue);

                // Invoke the action if it exists
                if (arg.getAction() != null) {
                    arg.getAction().accept(defaultValue);
                }
            }
        }
    }

    private void validateAndAdd(Arg arg) {
        if (argIndices.containsKey(arg.getShortForm())
                || argIndices.containsKey(arg.getLongForm())) {
            throw new IllegalArgumentException("Argument " + arg + " already exists");
        }

        // This is the index of arg in args
        int index = args.size();
        args.add(arg);

        if (arg.getShortForm() != null) {
            argIndices.put(arg.getShortForm(), index);
        }

        if (arg.getLongForm() != null) {
            argIndices.put(arg.getLongForm(), index);
        }
    }

    public void printHelp() {
        PrintWriter pw = new PrintWriter(System.out);
        printHelp(pw);
        pw.flush();
    }

    public void printHelp(PrintWriter pw) {
        pw.println(title);
        pw.println("Usage: java " + exampleClass.getName() + " [-h|--help] [options]");
        for (ArgGroup group : argGroups) {
            pw.println();
            group.printHelp(pw);
        }
    }
}
