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

import java.util.Date;

/**
 * This interface defines the method required to increment
 * the label used in the MasterBuild process. This label
 * is incorporated into the log filename when a successful
 * build occurs.
 * 
 * @author alden almagro (alden@thoughtworks.com), Paul Julius (pdjulius@thoughtworks.com), ThoughtWorks, Inc. 2001
 */
public interface LabelIncrementer {

    /**
     * Increments the label when a successful build occurs.
     * The oldLabel should be transformed and returned as
     * the new label.
     * 
     * @param oldLabel Label from previous successful build.
     * @return Label to use for most recent successful build.
     */
    public String incrementLabel(String oldLabel);
}
