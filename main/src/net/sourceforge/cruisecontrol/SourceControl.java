/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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

package net.sourceforge.cruisecontrol;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This interface defines behavior required by ModificationSet.java when
 * gathering information about the changes made to whatever source control tool
 * that you choose.
 *
 * SourceControl implementations commonly define 2 special properties:
 * <ul>
 * <li> <code>void setProperty(String property)</code>:
 * name of property to define if a modification is detected.
 * The property should be added to the set of properties returned by the {@link #getProperties()} call.
 * Allows the underlying build script to do conditional actions if the files watched by this
 * SourceControl have been modified.
 * </li>
 * <li><code>void setPropertyOnDelete(String property)</code>:
 * name of property to define if a deletion is detected.
 * The property should be added to the set of properties returned by the {@link #getProperties()} call.
 * </li>
 * </ul>
 *
 * @author <a href="mailto:alden@thoughtworks.com">Alden Almagro</a>
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @version $Id$
 */
public interface SourceControl extends Serializable {

    /**
     *  Get a List of Modifications detailing all the changes between now and
     *  the last build
     *
     *@param  lastBuild date of last build
     *@param  now current date
     *@return List of Modification objects
     */
    public List<Modification> getModifications(Date lastBuild, Date now);


    public void validate() throws CruiseControlException;

    /**
     * Any properties that have been set in this sourcecontrol.
     * Will be passed onto the Builder, which may then pass the properties to the underlying
     * build implementation. For example, the Ant builder will define these properties so that
     * the underlying Ant script can use them.
     * @return a Map of name, value pairs
     */
    public Map<String, String> getProperties();
}
