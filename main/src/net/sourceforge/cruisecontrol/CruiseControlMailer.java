package net.sourceforge.cruisecontrol;

import java.io.*;
import java.util.*;
import javax.mail.*;

public class CruiseControlMailer extends Mailer {

    public CruiseControlMailer(String mailhost, String from) {
        super(mailhost, from);
    }
    
    private Set getEmails(CruiseControlProperties props, String list, 
                          boolean lastBuildSuccessful) {

        //The buildmaster is always included in the email names.
        Set emails = new HashSet(props.getBuildmaster());

        //If the build failed then the failure notification emails are included.
        if (!lastBuildSuccessful) {
            emails.addAll(props.getNotifyOnFailure());
        }

        if (props.isMapSourceControlUsersToEmail()) {
            emails.addAll(getSetFromString(list));
        }

        return translateAliases(props, emails);
    }

    public void emailReport(CruiseControlProperties props, String userList, 
                            String subject, String logFileName,
                            boolean lastBuildSuccessful) {

        Set emails = getEmails(props, userList, lastBuildSuccessful);

        StringBuffer logMessage = new StringBuffer("Sending mail to:");
        for (Iterator iter = emails.iterator(); iter.hasNext();) {
            logMessage.append(" " + iter.next());
        }
        System.out.println(logMessage.toString());

        String message = "View results here -> " + props.getServletURL() + "?"
                         + logFileName.substring(logFileName.lastIndexOf(File.separator) + 1, 
                                              logFileName.lastIndexOf("."));

        try {
            sendMessage(emails, subject, message);
        } catch (javax.mail.MessagingException me) {
            System.out.println("Unable to send email.");
            me.printStackTrace();
        }
    }

    /**
     * Forms a set of unique words/names from the comma
     * delimited list provided. Maybe empty, never null.
     * 
     * @param commaDelim String containing a comma delimited list of words,
     *                   e.g. "paul,Paul, Tim, Alden,,Frank".
     * @return Set of words; maybe empty, never null.
     */
    private Set getSetFromString(String commaDelim) {
        Set elements = new TreeSet();
        if (commaDelim == null) {
            return elements;
        }

        StringTokenizer st = new StringTokenizer(commaDelim, ",");
        while (st.hasMoreTokens()) {
            String mapped = st.nextToken().trim();
            elements.add(mapped);
        }

        return elements;
    }

    private Set translateAliases(CruiseControlProperties props, 
                                 Set possibleAliases) {
        Set returnAddresses = new HashSet();
        boolean aliasPossible = false;
        for (Iterator iter = possibleAliases.iterator(); iter.hasNext();) {
            String nextName = (String) iter.next();
            if (nextName.indexOf("@") > -1) {
                //The address is already fully qualified.
                returnAddresses.add(nextName);
            } else if (props.useEmailMap()) {
                File emailmapFile = new File(props.getEmailmapFilename());
                Properties emailmap = new Properties();
                try {
                    emailmap.load(new FileInputStream(emailmapFile));
                } catch (Exception e) {
                    System.out.println("error reading email map file: " + props.getEmailmapFilename());
                    e.printStackTrace();
                }

                String mappedNames = emailmap.getProperty(nextName);
                if (mappedNames == null) {
                    if (props.getDefaultEmailSuffix() != null) {
                        nextName += props.getDefaultEmailSuffix();
                    }
                    returnAddresses.add(nextName);
                } else {
                    returnAddresses.addAll(getSetFromString(mappedNames));
                    aliasPossible = true;
                }
            } else {
                if (props.getDefaultEmailSuffix() != null) {
                    nextName += props.getDefaultEmailSuffix();
                }
                returnAddresses.add(nextName);
            }
        }

        if (aliasPossible) {
            returnAddresses = translateAliases(props, returnAddresses);
        }

        return returnAddresses;
    }
}
