/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, 2006, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.gendoc;

import java.io.Serializable;

/**
 * This class represents an attribute that can be associated with * a plugin.
 * The setter methods of this class are package-private to prevent modification
 * after the class has been constructed.
 * 
 * @author Anthony Love (lovea@msoe.edu)
 * @version 1.0
 */
public class AttributeInfo extends MemberInfo implements Serializable, Comparable<Object> {

    /** ID for serialization. */
    private static final long serialVersionUID = 1L;

    /** The name of the attribute, as it would appear in an XML config file */
    private String name = null;
    
    /** The Attribute's type. */
    private AttributeType type = null;

    /** The Attribute's default value */
    private String defaultValue = null;
    
    /**
     * Creates an Attribute Object with all fields defaulted.
     */
    public AttributeInfo() {
        // Empty.
    }
    
    /**
     * Returns the member's name
     * 
     * @return The member's name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the member's name to the given value
     * 
     * @param name The new name for the member.
     */
    protected void setName(String name) {
        this.name = name;
    }
    
    /**
     * Returns the Attribute's type
     * 
     * @return The Attribute's type
     */
    public AttributeType getType() {
        return type;
    }

    /**
     * Sets the Attribute's type to the given value
     * 
     * @param type The new type for this attribute.
     */
    protected void setType(AttributeType type) {
        this.type = type;
    }

    /**
     * Returns the Attribute's default value
     * 
     * @return The Attribute's default value
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Sets the Attribute's name to the given value
     * 
     * @param defaultValue The desired default value
     */
    protected void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    /**
     * Imposes an alphabetical by-name ordering on AttributeInfo objects. See toString().
     * @param o Object to compare to.
     * @return Result of comparison.
     */
    public int compareTo(Object o) {
        // Sort plugins by name.
        return this.toString().compareTo(o.toString());
    }
    
    /**
     * Gets a string representation (i.e. the name) for this attribute. This is used to properly
     * implement compareTo(Object).
     * @return The plugin's name.
     */
    @Override
    public String toString() {
        return name;
    }

}
