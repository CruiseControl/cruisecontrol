/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/

package net.sourceforge.cruisecontrol.util;

/**
 * Methods to perform operations on <code>main()</code> arguments.
 */
public final class MainArgs {
    private MainArgs() { }

    public static final int NOT_FOUND = -1;

    public static int parseInt(String[] args, String argName, int defaultIfNoParam, int defaultIfNoValue) {
        String intString = parseArgument(args,
                                         argName,
                                         Integer.toString(defaultIfNoParam),
                                         Integer.toString(defaultIfNoValue));
        try {
            return Integer.parseInt(intString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "-" + argName + " parameter, specified as '" + intString + "', requires integer argument");
        }
    }

    /**
     * Searches the array of args for the value corresponding to a particular
     * argument name. This method assumes that the argName doesn't include
     * a "-", but adds one while looking through the array. For example, if a
     * user is supposed to type "-port", the appropriate argName to supply to
     * this method is just "port".
     *
     * This method also allows the specification
     * of a default argument value, in case one was not specified.
     *
     * @param args Application arguments like those specified to the standard
     *      Java main function.
     * @param argName Name of the argument, without any preceeding "-",
     *      i.e. "port" not "-port".
     * @param defaultIfNoParam A default argument value,
     *      in case the parameter argName was not specified
     * @param defaultIfNoValue A default argument value,
     *      in case the parameter argName was specified without a value
     * @return The argument value found, or the default if none was found.
     */
    public static String parseArgument(String[] args, String argName,
                                       String defaultIfNoParam, String defaultIfNoValue) {
        int argIndex = findIndex(args, argName);
        if (argIndex == NOT_FOUND) {
            return defaultIfNoParam;
        }
        // check to see if the user supplied a value for the parameter;
        // if not, return the supplied default
        if (argIndex == args.length - 1            // last arg
            || args[argIndex + 1].charAt(0) == '-' // start of new param
        ) {
            return defaultIfNoValue;
        }
        return args[argIndex + 1];
    }

    public static int findIndex(String[] args, String argName) {

        String searchString = "-" + argName;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(searchString)) {
                return i;
            }
        }
        return NOT_FOUND;
    }

    public static boolean argumentPresent(String[] args, String argName) {
        return findIndex(args, argName) != NOT_FOUND;
    }

    public static boolean parseBoolean(String[] args, String argName, boolean defaultIfNoParam,
            boolean defaultIfNoValue) {
        String booleanString =
                parseArgument(args, argName, Boolean.toString(defaultIfNoParam), Boolean
                        .toString(defaultIfNoValue));
        try {
            return Boolean.valueOf(booleanString).booleanValue();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("-" + argName + " parameter, specified as '" + booleanString
                    + "', requires boolean argument");
        }
    }
}
