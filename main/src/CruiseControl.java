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
 
import net.sourceforge.cruisecontrol.*;

/**
 * This class wraps the MasterBuild process to 
 * snazzify the command line. 
 * (instead of "javac net.sourceforge.cruisecontrol.MasterBuild"
 *  the command line is snazzier, in the form of "java CruiseControl")
 * 
 * * @author alden almagro (alden@thoughtworks.com), Paul Julius (pdjulius@thoughtworks.com), ThoughtWorks, Inc. 2001
 */
public class CruiseControl {
    public static void main(String[] args) {
        MasterBuild.main(args);
    }
}
