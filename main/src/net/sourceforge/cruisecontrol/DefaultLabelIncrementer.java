/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit                              *
 * Copyright (C) 2001  ThoughtWorks, Inc.                                       *
 * 651 W Washington Ave. Suite 500                                              *
 * Chicago, IL 60661 USA                                                        *
 *                                                                              *
 * This program is free software; you can redistribute it and/or                *
 * modify it under the terms of the GNU General Public License                  *
 * as published by the Free Software Foundation; either version 2               *
 * of the License, or (at your option) any later version.                       *
 *                                                                              *
 * This program is distributed in the hope that it will be useful,              *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of               *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
 * GNU General Public License for more details.                                 *
 *                                                                              *
 * You should have received a copy of the GNU General Public License            *
 * along with this program; if not, write to the Free Software                  *
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.  *
 ********************************************************************************/
 
package net.sourceforge.cruisecontrol;

/**
 * This class provides a default label incrementation.
 * This class expects the label format to be "x.y",
 * where x is any String and y is an integer.
 * 
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 */
public class DefaultLabelIncrementer implements LabelIncrementer {
    
    /**
     * Increments the label when a successful build occurs.
     * Assumes that the label will be in
     * the format of "x.y", where x can be anything, and y is an integer.
     * The y value will be incremented by one, the rest will remain the same.
     * 
     * @param oldLabel Label from previous successful build.
     * @return Label to use for most recent successful build.
     */
    public String incrementLabel(String oldLabel) {
        
        String prefix = oldLabel.substring(0, oldLabel.lastIndexOf(".") + 1);
        String suffix = oldLabel.substring(oldLabel.lastIndexOf(".") + 1, oldLabel.length());
        int i = Integer.parseInt(suffix);
        return prefix + ++i;
    }
}
