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
import java.text.DateFormat;

/**
 * data structure for holding data about a single modification
 * to a source control tool.
 */
public class Modification {

  public String type;
  public String fileName;
  public String folderName;
  public Date modifiedTime;
  public String userName;
  public String comment = "";
  
  public String toXml(DateFormat formatter) {
     StringBuffer sb = new StringBuffer();
     sb.append("   <modification type=\"" + type + "\">\n");
     sb.append("     <filename>" + fileName + "</filename>\n");
     sb.append("     <project>" + folderName + "</project>\n");
     sb.append("     <date>" + formatter.format(modifiedTime) + "</date>\n");
     sb.append("     <user>" + userName + "</user>\n");
     sb.append("     <comment><![CDATA[" + comment + "]]></comment>\n");
     sb.append("   </modification>\n");

     return sb.toString();
  }

  public String toString(DateFormat formatter) {
     StringBuffer sb = new StringBuffer();
     sb.append("FileName: " + fileName + "\n");
     sb.append("FolderName: " + folderName + "\n");
     sb.append("Last Modified: " + formatter.format(modifiedTime) + "\n");
     sb.append("UserName: " + userName + "\n");
     sb.append("Comment: " + comment + "\n\n");

     return sb.toString();
  }

}