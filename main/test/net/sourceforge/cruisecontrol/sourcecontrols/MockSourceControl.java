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
package net.sourceforge.cruisecontrol.sourcecontrols;

import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.CruiseControlException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MockSourceControl implements SourceControl {

    private int version;
    private final Map<String, String> properties = new HashMap<String, String>();
    private String property = null;
    private String propertyOnDelete = null;

    // added this because otherwise the unit-tests doesn't work
    // if the machine is slow. I promise, this was hard to catch :-)
    private Date modifiedDate = new Date();

    public void setModifiedDate(final Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
    
    public void setProperty(String property) {
        this.property = property;
    }

    public void setPropertyOnDelete(String propertyOnDelete) {
        this.propertyOnDelete = propertyOnDelete;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setType(int version) {
        this.version = version;
    }

    public void validate() throws CruiseControlException {
    }

    public List<Modification> getModifications(Date lastBuild, Date now) {
        final ArrayList<Modification> result = new ArrayList<Modification>();

        if (version == 1) {
            //build up a couple Modification objects
            Modification mod1 = new Modification();
            Modification.ModifiedFile mod1file = mod1.createModifiedFile("file1", "dir1");
            mod1file.action = "Checkin";
            mod1.userName = "user1";
            mod1.modifiedTime = modifiedDate;
            mod1.comment = "comment1";
            result.add(mod1);

            Modification mod2 = new Modification();
            Modification.ModifiedFile mod2file = mod1.createModifiedFile("file2", "dir2");
            mod2file.action = "Checkin";
            mod2.userName = "user2";
            mod2.modifiedTime = modifiedDate;
            mod2.comment = "comment2";
            result.add(mod2);

            if (property != null) {
                properties.put(property, "true");
            }
        }

        if (version == 2) {
            Modification mod3 = new Modification();
            Modification.ModifiedFile mod3file = mod3.createModifiedFile("file3", "dir3");
            mod3file.action = "Checkin";
            mod3.userName = "user3";
            mod3.modifiedTime = modifiedDate;
            mod3.comment = "comment3";
            result.add(mod3);

            Modification mod4 = new Modification();
            Modification.ModifiedFile mod4file = mod4.createModifiedFile("file4", "dir4");
            mod4file.action = "Checkin";
            mod4.userName = "user4";
            mod4.modifiedTime = modifiedDate;
            mod4.comment = "comment4";
            result.add(mod4);

            if (propertyOnDelete != null) {
                properties.put(propertyOnDelete, "true");
            }
        }


        return result;
    }

}