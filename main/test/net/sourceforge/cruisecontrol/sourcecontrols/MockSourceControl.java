/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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

import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Hashtable;

public class MockSourceControl implements SourceControl {

    private int _version;
    private Hashtable _properties = new Hashtable();
    private String _property = null;
    private String _propertyOnDelete = null;

    // added this because otherwise the unit-tests doesn't work
    // if the machine is slow. I promise, this was hard to catch :-)
    private Date _modifiedDate = new Date();

    
    public void setProperty(String property) {
        _property = property;
    }

    public void setPropertyOnDelete(String propertyOnDelete) {
        _propertyOnDelete = propertyOnDelete;
    }

    public Hashtable getProperties() {
        return _properties;
    }

    public void setType(int version) {
        _version = version;
    }

    public List getModifications(Date lastBuild, Date now) {
        ArrayList result = new ArrayList();

        if (_version == 1) {
            //build up a couple Modification objects
            Modification mod1 = new Modification();
            mod1.type = "Checkin";
            mod1.fileName = "file1";
            mod1.folderName = "dir1";
            mod1.userName = "user1";
            mod1.modifiedTime = _modifiedDate;
            mod1.comment ="comment1";
            result.add(mod1);

            Modification mod2 = new Modification();
            mod2.type = "Checkin";
            mod2.fileName = "file2";
            mod2.folderName = "dir2";
            mod2.userName = "user2";
            mod2.modifiedTime = _modifiedDate;
            mod2.comment = "comment2";
            result.add(mod2);

            if( _property != null ) {
                _properties.put(_property, "true");
            }
        }

        if (_version == 2) {
            Modification mod3 = new Modification();
            mod3.type = "Checkin";
            mod3.fileName  = "file3";
            mod3.folderName = "dir3";
            mod3.userName = "user3";
            mod3.modifiedTime = _modifiedDate;
            mod3.comment = "comment3";
            result.add(mod3);

            Modification mod4 = new Modification();
            mod4.type = "Checkin";
            mod4.fileName = "file4";
            mod4.folderName = "dir4";
            mod4.userName = "user4";
            mod4.modifiedTime = _modifiedDate;
            mod4.comment = "comment4";
            result.add(mod4);

            if( _propertyOnDelete != null ) {
                _properties.put(_propertyOnDelete, "true");
            }
        }


        return result;
    }

}