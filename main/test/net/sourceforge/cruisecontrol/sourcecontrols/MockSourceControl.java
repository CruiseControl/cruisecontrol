package net.sourceforge.cruisecontrol.sourcecontrols;

import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Hashtable;

public class MockSourceControl implements SourceControl {

    private int _version;
    private Hashtable _properties;
    // added this because otherwise the unit-tests doesn't work 
    // if the machine is slow. I promise, this was hard to catch :-)
    private Date _modifiedDate = new Date();

    
    public void setProperty(String property) {
    }

    public void setPropertyOnDelete(String propertyOnDelete) {
    }

    public Hashtable getProperties() {
        return _properties;
    }

    public void setType(int version) {
        _version = version;
    }

    public List getModifications(Date lastBuild, Date now, long quietPeriod) {
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
        }


        return result;
    }

}