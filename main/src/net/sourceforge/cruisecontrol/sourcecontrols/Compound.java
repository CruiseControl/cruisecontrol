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
import java.util.List;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.SourceControl;

/**
 * This class implements a Compound source control with one triggers 
 * section and one targets section.  
 * 
 * The triggers section contains one or more source controls that act 
 * as triggers for the modifications, i.e. the modificationset 
 * returned will be empty unless one or more of the source 
 * controls in the triggers section returns a non-empty
 * modification list.
 *
 * The targets section contains source controls for targets that will
 * be built (if modified) if and only if any of the source controls 
 * in the triggers section is modified. 
 * 
 * It is possible to add the trigger modifications to the list of
 * returned modifications if the "includeTriggerChanges"
 * attribute is set to true in the <compound...> tag corresponding to 
 * this class.
 * 
 * The following is an example of how to use this source control in
 * the config.xml file:
 * 
 *      &lt;modificationset quietperiod="1" &gt;
 *           &lt;compound includeTriggerChanges="false"&gt;
 *               &lt;triggers&gt;
 *                   &lt;filesystem folder="./mod_file.txt" /&gt;
 *               &lt;/triggers&gt;
 *               &lt;targets&gt;
 *                   &lt;cvs 
 *                       cvsroot=":pserver:user@cvs_repo.com:/cvs" 
 *                   /&gt;
 *               &lt;/targets&gt;
 *           &lt;/compound&gt; 
 *       &lt;/modificationset&gt;
 *
 * @author  <a href="mailto:will.gwaltney@sas.com">Will Gwaltney</a>
 */
public class Compound implements SourceControl {
    
    private Hashtable properties = new Hashtable();
    private String property;
    private String propertyOnDelete;

    private Triggers triggers = null;
    private Targets targets = null;
    private boolean includeTriggerChanges = false;
    
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
     * Returns a list of modifications since the last build. 
     * First check for any modifications from the triggers.
     * If there are none, then return an empty list.  Otherwise
     * return the modifications from the targets, and from the
     * triggers also if the includeTriggerChanges member variable
     * is true.
     * 
     * @param   lastBuild   the date and time of the last build
     * @param   now         the current date and time
     * 
     * @return  a list of the modifications
     */
    public List getModifications(Date lastBuild, Date now) {        
        List triggerMods = null;
        List targetMods = new ArrayList();
        
        triggerMods = triggers.getModifications(lastBuild, now);
        // System.out.println("Number of trigger mods is " + triggerMods.size());
        
        if (!triggerMods.isEmpty()) {
            targetMods = targets.getModifications(lastBuild, now);
            // System.out.println("Number of target mods is " + targetMods.size());
        }

        if (includeTriggerChanges) {
            targetMods.addAll(triggerMods);
        }
        
        return targetMods;
    }
    
    /**
     * Confirms that there is exactly one triggers block and one targets
     * block even if the triggers mods are included and the target
     * block is empty (otherwise you wouldn't need a compound block
     * to begin with).
     * 
     * @throws  a CruiseControlException if the validation fails
     */
    public void validate() throws CruiseControlException {
        // confirm we have a triggers block
        if (triggers == null) {
            throw new CruiseControlException(
            "Error: there must be exactly one \"triggers\" block in a compound block.");
        }
        
        // confirm we have a targets block
        if (targets == null) {
            throw new CruiseControlException(
            "Error: there must be exactly one \"targets\" block in a compound block.");
        }
        
    }
    
    /**
     * Creates an empty Triggers object and returns it to 
     * the calling routine to be filled.
     * 
     * @return  an empty Triggers object
     */
    public Object createTriggers() {
        Triggers tr = new Triggers(this);
        this.triggers = tr;
        return tr;
    }
    
    /**
     * Creates an empty Targets object and returns it to 
     * the calling routine to be filled.
     * 
     * @return  an empty Targets object
     */
    public Object createTargets() {
        Targets targ = new Targets(this);
        this.targets = targ;
        return targ;
    }
    
    /**
     * Sets whether to include modifications from the triggers
     * when getModifications() returns the mods list.
     * 
     * @param changes   true to include trigger changes, false otherwise
     */
    public void setIncludeTriggerChanges(String changes) {
        this.includeTriggerChanges = changes.equalsIgnoreCase("true");
    }
}
