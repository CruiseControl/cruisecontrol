/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.SourceControl;

/**
 * The class identifies the source control objects that it
 * contains as triggers to invoke builds on corresponding
 * targets when the triggers are modified. See the Compound 
 * class for more information about how this class is used.
 * 
 * @author  <a href="mailto:will.gwaltney@sas.com">Will Gwaltney</a>
 
 */
public class Triggers implements SourceControl {
    
    private Hashtable properties = new Hashtable();
    private String property;
    private String propertyOnDelete;
    
    private ArrayList sourceControls = new ArrayList();
    private Compound parent;
    
    /**
     * Public constructor for reflection purposes.
     *
     */
    public Triggers() {
    }
    
    /**
     * Constructor that the Compound class uses to create
     * an object of this class.
     * 
     * @param parent    the parent of this object (an
     *                  object of class Compound) 
     */
    public Triggers(Compound parent) {
        this.parent = parent;
    }
    
    public Hashtable getProperties() {
        return this.properties;
    }
    
    public void setProperty(String property) {
        this.property = property;
        
    }
    
    public void setPropertyOnDelete(String property) {
        this.propertyOnDelete = property;
    }
    
    /**
     * Returns a list of modifications since the last build
     * by querying the sourceControl that this object contains.
     * 
     * @param   lastBuild   the date and time of the last build
     * @param   now         the current date and time
     * 
     * @return  a list of the modifications
     */
    public List getModifications(Date lastBuild, Date now) {
        ArrayList retVal = new ArrayList();
        
        Iterator it = sourceControls.iterator();
       
        while (it.hasNext()) {
            retVal.addAll(((SourceControl) it.next()).getModifications(lastBuild, now));
        }
        
        return retVal;
    }
    
    /**
     * Confirms that the sourceControl that this object wraps
     * has been set.
     * 
     * @throws  a CruiseControlException if the validation fails
     */
    public void validate() throws CruiseControlException {
        if (sourceControls.isEmpty()) {
            throw new CruiseControlException("Error: there must be at least one source control in a triggers block.");
        }
        if (!(parent instanceof Compound)) {
            throw new CruiseControlException("Error: triggers blocks must be contained within compound blocks.");
        }
    }
    
    /**
     * Adds a sourcecontrol to the list of sourcecontrols that
     * this object contains.
     * 
     * @param sc the sourceControl object to add
     */
    public void add(SourceControl sc) {
        sourceControls.add(sc);
    }
}
