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