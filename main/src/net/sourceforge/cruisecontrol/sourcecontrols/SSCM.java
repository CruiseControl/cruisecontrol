/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import net.sourceforge.cruisecontrol.util.StreamLogger;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.IO;
import org.apache.log4j.Logger;

/**
 * This defines a child element for the ModificationSet element.
 *
 * @author Matt Harp
 */
public class SSCM implements net.sourceforge.cruisecontrol.SourceControl {

    private static final Logger LOG = Logger.getLogger(SSCM.class);
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

    private final SSCMCLIStringParam strparamBranch = new SSCMCLIStringParam("branch", "-b", false);
    private final SSCMCLIStringParam strparamRepository = new SSCMCLIStringParam("repository", "-p", false);
    private final SSCMCLIStringParam strparamFile = new SSCMCLIStringParam("file", "", false);
    private final SSCMCLIStringParam strparamServerConnect = new SSCMCLIStringParam("serverconnect", "-z", false);
    private final SSCMCLIStringParam strparamServerLogin = new SSCMCLIStringParam("serverlogin", "-y", false);
    private final SSCMCLIBoolParam fparamSearchRegExp = new SSCMCLIBoolParam("searchregexp", "-x", false);
    private final SSCMCLIBoolParam fparamRecursive = new SSCMCLIBoolParam("recursive", "-r", false);

    private final SourceControlProperties properties = new SourceControlProperties();

    public void validate() throws CruiseControlException { /* nothing is required */ }

    public void setBranch(String str) {
        strparamBranch.setData(str);
    }

    public void setRepository(String str) {
        strparamRepository.setData(str);
    }

    public void setFile(String str) {
        strparamFile.setData(str);
    }

    public void setServerConnect(String str) {
        strparamServerConnect.setData(str);
    }

    public void setServerLogin(String str) {
        strparamServerLogin.setData(str);
    }

    public void setSearchRegExp(String str) {
        if (str.equals("1")) {
            fparamSearchRegExp.setData(null);
        }
    }

    public void setRecursive(String str) {
        if (str.equals("1")) {
            fparamRecursive.setData(null);
        }
    }

    public List<Modification> getModifications(final Date lastBuild, final Date now) {
        final List<SSCMCLIParam> paramList = new ArrayList<SSCMCLIParam>();
        if (!strparamFile.isSet()) {
            strparamFile.setData("/");
        }
        paramList.add(strparamFile);
        paramList.add(strparamBranch);
        paramList.add(strparamRepository);
        paramList.add(fparamRecursive);
        paramList.add(fparamSearchRegExp);
        paramList.add(strparamServerLogin);
        paramList.add(strparamServerConnect);

        List<Modification> listMods = executeCLICommand(paramList, buildDateTimeRangeCLIParam(lastBuild, now));

        if (listMods == null) {
            listMods = Collections.emptyList();
        }

        if (!listMods.isEmpty()) {
            properties.modificationFound();
        }

        return listMods;
    }

    public Map<String, String> getProperties() {
        return properties.getPropertiesAndReset();
    }

    public void setProperty(String property) {
        properties.assignPropertyName(property);
    }

    protected List<Modification> executeCLICommand(final List<SSCMCLIParam> paramList, final String strDTRangeParam) {
        List<Modification> listMods = null;
        final Commandline command = new Commandline();
        command.setExecutable("sscm");
        command.createArgument().setValue("cc");

        // Next, we just iterate through the list, adding entries.
        boolean fAllRequirementsMet = true;
        for (int i = 0; i < paramList.size() && fAllRequirementsMet; ++i) {
            final SSCMCLIParam param = paramList.get(i);
            if (param != null) {
                if (param.checkRequired()) {
                    final String str = param.getFormatted();
                    if (str != null) {
                        command.createArgument().setValue(str);
                        LOG.debug("Added cmd part: " + str);
                    }
                } else {
                    fAllRequirementsMet = false;
                    LOG.error("Required parameter '" + param.getParamName() + "' is missing!");
                }
            }
        }

        if (fAllRequirementsMet) {

            command.createArgument().setValue(strDTRangeParam);
            LOG.debug("Added DTRangeParam: " + strDTRangeParam);

            try {
                final Process process = command.execute();
                // logs process error stream at info level
                final Thread stderr = new Thread(new StreamPumper(process.getErrorStream(),
                            StreamLogger.getInfoLogger(LOG)));
                stderr.start();

                final InputStream input = process.getInputStream();
                listMods = parseCLIOutput(input);

                process.waitFor();
                stderr.join();

                IO.close(process);
            } catch (IOException e) {
                LOG.error("Problem trying to execute command line process", e);
            } catch (InterruptedException e) {
                LOG.error("Problem trying to execute command line process", e);
            }
        }

        return listMods;
    }

    protected List<Modification> parseCLIOutput(final InputStream input) throws IOException {
        final List<Modification> listMods = new ArrayList<Modification>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        String line = reader.readLine();
        LOG.debug("\nSSCM mod line: " + line + "\n");

        // -meh. Kind of lame, but total-0 will work.
        if (!"total-0".equals(line)) {
            while ((line = reader.readLine()) != null) {
                final Modification mod = parseOutputLine(line);
                if (mod != null) {
                    listMods.add(mod);
                }
            }
        }

        return listMods;
    }

    protected Modification parseOutputLine(final String str) {
        LOG.debug("Output-" + str + "-\n");

        if (str == null || str.length() == 0) {
            return null;
        }
        Modification mod = new Modification("sscm");
        final Modification.ModifiedFile modfile = mod.createModifiedFile(null, null);

        boolean fValid = false;
        final String strToken = "><";
        int iLeft = 1;

        // Repository
        int iRight = str.indexOf(strToken, iLeft);
        if (iRight > iLeft) {
            modfile.folderName = str.substring(iLeft, iRight);
            iLeft = iRight + strToken.length();

            // Filename
            iRight = str.indexOf(strToken, iLeft);
            if (iRight > iLeft) {
                modfile.fileName = str.substring(iLeft, iRight);
                iLeft = iRight + strToken.length();

                // Revision
                iRight = str.indexOf(strToken, iLeft);
                if (iRight > iLeft) {
                    mod.revision = str.substring(iLeft, iRight);
                    iLeft = iRight + strToken.length();

                    // Event
                    iRight = str.indexOf(strToken, iLeft);
                    if (iRight > iLeft) {
                        modfile.action = str.substring(iLeft, iRight);
                        iLeft = iRight + strToken.length();

                        // Date
                        iRight = str.indexOf(strToken, iLeft);
                        if (iRight > iLeft) {
                            mod.modifiedTime = buildDateTimeFromCLIOutput(str.substring(iLeft, iRight));
                            iLeft = iRight + strToken.length();

                            // Comment
                            iRight = str.indexOf(strToken, iLeft);
                            if (iRight >= iLeft) {
                                mod.comment = str.substring(iLeft, iRight);
                                iLeft = iRight + strToken.length();

                                // User
                                iRight = str.indexOf(strToken, iLeft);
                                if (iRight > iLeft) {
                                    mod.userName = str.substring(iLeft, iRight);
                                    iLeft = iRight + strToken.length();

                                    // Email
                                    iRight = str.indexOf(">", iLeft);
                                    if (iRight >= iLeft) {
                                        mod.emailAddress = str.substring(iLeft, iRight);
                                        fValid = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!fValid) {
            mod = null;
            LOG.debug("Invalid output; skipping this entry");
        }
        return (mod);
    }

    protected String buildDateTimeRangeCLIParam(final Date lastBuild, final Date now) {
        final String strLast = formatter.format(lastBuild);
        final String strNow = formatter.format(now);
        return "-d" + strLast + ":" + strNow;
    }

    protected Date buildDateTimeFromCLIOutput(final String str) {
        Date dt;
        try {
            dt = formatter.parse(str);
        } catch (ParseException e) {
            dt = null;
            LOG.error("Unable to parse DateTime from Surround", e);
        }
        return dt;
    }

    public abstract static class SSCMCLIParam {
        public SSCMCLIParam(String strParamNameIN, String strParamIN, boolean fIsRequiredIN) {
            strParamName = strParamNameIN;
            strParam = strParamIN;
            fIsRequired = fIsRequiredIN;
            fIsSet = false;
        }

        public String getParamName() {
            return (strParamName);
        }

        public String getParam() {
            return (strParam);
        }

        public void setRequired(boolean f) {
            fIsRequired = f;
        }

        public boolean isRequired() {
            return fIsRequired;
        }

        public boolean isSet() {
            return fIsSet;
        }

        public boolean checkRequired() {
            return !(isRequired() && !isSet());
        }

        public abstract String getFormatted();

        public abstract void setData(Object obj);

        protected void setSet(boolean f) {
            fIsSet = f;
        }

        private final String strParamName;
        private final String strParam;
        private boolean fIsRequired;
        private boolean fIsSet;
    }

    public static class SSCMCLIBoolParam extends SSCMCLIParam {
        public SSCMCLIBoolParam(String strParamNameIN, String strParamIN, boolean fIsRequiredIN) {
            super(strParamNameIN, strParamIN, fIsRequiredIN);
        }

        public void setData(Object obj) {
            fData = true;
            setSet(true);
        }

        public String getFormatted() {
            String str = null;
            if (isSet() && fData) {
                str = getParam();
            }
            return str;
        }

        private boolean fData;
    }

    public static class SSCMCLIStringParam extends SSCMCLIParam {
        public SSCMCLIStringParam(String strParamNameIN, String strParamIN, boolean fIsRequiredIN) {
            super(strParamNameIN, strParamIN, fIsRequiredIN);
        }

        public void setData(Object obj) {
            strData = (String) obj;
            setSet(true);
        }

        public String getFormatted() {
            String str = null;
            if (isSet()) {
                str = getParam() + strData;
            }
            return str;
        }

        private String strData;
    }

}

