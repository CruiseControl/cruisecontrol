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

import net.sourceforge.cruisecontrol.gendoc.html.HtmlUtils;

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
    private String name;
    
    /** The Attribute's type. */
    private AttributeType type;

    /** The Attribute's default value */
    private String defaultValue;
    
    /**
     * Creates an Attribute Object with all fields defaulted.
     */
    public AttributeInfo() {
        // Empty.
    }
    
    /**
     * Creates a pre-populated AttributeInfo. This can be used to create mock objects
     * for testing.
     * @param name Name.
     * @param type Type.
     * @param description Description.
     * @param title Title.
     * @param defaultValue Default value.
     * @param cardinality Array of 2 integers. The first is the minimum cardinality. The
     *        second is the maximum cardinality. This has to be an array to obey the
     *        checkstyle requirement that no method have more than 7 parameters.
     * @param cardinalityNote Cardinality note.
     */
    public AttributeInfo(
            String name,
            AttributeType type,
            String description,
            String title,
            String defaultValue,
            int[] cardinality,
            String cardinalityNote
    ) {
        super(description, title, cardinality[0], cardinality[1], cardinalityNote);
        
        setName(name);
        setType(type);
        setDefaultValue(defaultValue);
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
     * Returns the member's Title, or the name if the title was not
     * specified.
     * 
     * @return The member's Title
     */
    @Override
    public String getTitle() {
        String title = super.getTitle();
        return (title == null) ? name : title;
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
    void setType(AttributeType type) {
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
    void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    /**
     * Generates the HTML documentation for this attribute.
     * @param text Text buffer to write to.
     */
    void writeHtml(StringBuilder text) {
        text
        .append("<tr>\n")
        .append("<td>")
        .append(getName())
        .append("</td>\n");
        
        writeRequired(text);
        
        text
        .append("<td>")
        .append(HtmlUtils.emptyIfNull(getDescription()))
        .append("</td>\n")
        .append("</tr>\n");
    }
    
    /**
     * Writes the contents of the "Required?" column for this member.
     * @param text Text buffer to write to.
     */
    private void writeRequired(StringBuilder text) {
        text
        .append("<td>")
        .append((getMinCardinality() > 0) ? "<b>Required</b>" : "Optional");
        
        String note = getCardinalityNote();
        if (note != null) {
            text
            .append(". ")
            .append(note);
            
            if (!note.trim().endsWith(".")) {
                text.append('.');
            }
        }
        
        String defalt = getDefaultValue();
        if (defalt != null) {
            if (note == null) {
                text.append('.');
            }
            
            text
            .append(" Defaults to \"")
            .append(defalt)
            .append("\".");
        }
        
        text.append("</td>\n");
    }
    
    static void writeTableStart(StringBuilder text) {
        text
        .append("<table class=\"documentation\">\n")
        .append("<thead>\n")
        .append("<tr>\n")
        .append("<th>Attribute</th>\n")
        .append("<th>Required?</th>\n")
        .append("<th>Description</th>\n")
        .append("</tr>\n")
        .append("</thead>\n")
        .append("<tbody>\n");
    }
    
    static void writeTableEnd(StringBuilder text) {
        text
        .append("</tbody>\n")
        .append("</table>\n");
    }
    
    /**
     * Imposes an alphabetical by-name ordering on AttributeInfo objects. See toString().
     * @param o Object to compare to.
     * @return Result of comparison.
     */
    public int compareTo(Object o) {
        if (o == null) {
            return 1; // Sort nulls before non-nulls.
        } else {
            // Sort attributes by name.
            return this.toString().compareTo(o.toString());
        }
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
