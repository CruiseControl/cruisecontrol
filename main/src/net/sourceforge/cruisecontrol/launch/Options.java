/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.launch;

import java.io.File;

public interface Options {

    /** String used to separate items in the array-holding options */
    static final String ITEM_SEPARATOR = File.pathSeparator;

    /**
     * @return iterable through all known option keys
     */
    Iterable<String> allOptionKeys();

    /**
     * Checks, if the given option is known, i.e. it can be get by {@link #getOptionRaw(String)} and
     * typed getters.
     *
     * @param key the key to check
     * @return <code>true</code> if the given option has been set, <code>false</code> if not so get methods
     *         are going to return the hard-coded default value.
     */
    boolean knowsOption(String key);

    /**
     * Gets the option value as the raw string.
     *
     * @param key the name of the option to search for.
     * @return the value of the option or <code>null</code> if such option is not set
     * @throws IllegalArgumentException if the exception does not exist or does not match the type
     */
    String getOptionRaw(String key) throws IllegalArgumentException;
    /**
     * Gets the option value as the given type. The value returned can be cast to the given type
     * without error thrown
     *
     * @param key the name of the option to search for.
     * @param type the required type of the option
     * @return the value of the option (can be cast to the given type without error) or <code>null</code>
     *      if such option is not set or does not have the required type
     * @throws IllegalArgumentException if the exception does not exist or does not match the type
     */
    Object getOptionType(final String key, final Class< ? > type) throws IllegalArgumentException;

    /**
     * Sets the option of the given type. Passig <code>null</code> value will remove the value from the
     * configuration.
     *
     * @param key the name of the option
     * @param val the value of the option (of the given type)
     * @param owner the instance which is allowed to change the options
     * @throws IllegalAccessError when the owner instance is wrong
     * @throws IllegalArgumentException when the value is invalid
     */
    void setOption(final String key, final String val, final Object owner)
            throws IllegalAccessError, IllegalArgumentException;
}