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
 * This class contains a simple main method which tests
 * the implementation of the NoExitSecurityManager.
 * This class should be replaced by a jUnit test case.
 */
public class TestNoExitSecurity {
    public static void main(String[] args) {
        System.setSecurityManager(new NoExitSecurityManager());

        for (int i=0;i<3; i++) {

            try {
                System.exit(1);
                System.out.println("Test failed. An exception was not thrown.");
            } catch (ExitException ee) {
                System.out.println("Test passed. An exception WAS thrown. Stack trace is:");
                ee.printStackTrace();
            }
        }
    }
}
