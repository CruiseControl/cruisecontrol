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
 * Superclas containing functionality common between ChildInfo and AttributeInfo.
 * @author Seth Pollen (pollens@msoe.edu)
 */
public class MemberInfo implements Serializable {

    /** ID for serialization. */
    private static final long serialVersionUID = 1L;
    
    /** The description of the member. */
    private String description = null;
    
    /** The member's minimum cardinality */
    private int minCardinality = 0;
    
    /** The member's maximum cardinality */
    private int maxCardinality = -1;

    /** The human-readable title for the member. */
    private String title = null;

    /**
     * Creates a MemberInfo object with all fields defaulted.
     */
    public MemberInfo() {
        // Empty.
    }

    /**
     * Sets the member's Title to the given value
     * 
     * @param title The new title for the member.
     */
    protected void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * Returns the member's Title
     * 
     * @return The member's Title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Returns the member's description
     * 
     * @return The member's description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the member's description to the given value
     * @param description the new value of description
     */
    protected void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Returns the minimum cardinality
     * 
     * @return The minimum cardinality
     */
    public int getMinCardinality() {
        return minCardinality;
    }

    /**
     * Sets the minimum cardinality 
     * 
     * @param minCardinality The desired minimum cardinality
     */
    protected void setMinCardinality(int minCardinality) {
        // Error check the value before assigning it.
        if (minCardinality < 0) {
            minCardinality = 0;
        }
        this.minCardinality = minCardinality;
    }

    /**
     * Returns the maximum cardinality
     * 
     * @return The maximum cardinality. A value of -1 indicates no maximum.
     */
    public int getMaxCardinality() {
        return maxCardinality;
    }

    /**
     * Sets the maximum cardinality
     * 
     * @param maxCardinality The desired maximum cardinality
     */
    protected void setMaxCardinality(int maxCardinality) {
        // Error check the value before assigning it.
        if (maxCardinality < -1) {
            maxCardinality = -1;
        }
        this.maxCardinality = maxCardinality;
    }
    
}
