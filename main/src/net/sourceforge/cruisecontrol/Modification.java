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
 
package net.sourceforge.cruisecontrol;

import java.util.Date;
import java.text.DateFormat;

/**
 * data structure for holding data about a single modification
 * to a source control tool.
 * 
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 */
public class Modification {

  public String type = "unknown";
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