/****************************************************************************
* CruiseControl, a Continuous Integration Toolkit
* Copyright (c) 2001, ThoughtWorks, Inc.
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
****************************************************************************/

package net.sourceforge.cruisecontrol.distributed;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Date;

import org.jdom.Element;
import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.distributed.core.ProgressRemote;
import net.sourceforge.cruisecontrol.distributed.core.RemoteResult;

public interface BuildAgentService extends Remote {

    /**
     * Performs a build on a build agent.
     * @param nestedBuilder the builder to execute on the agent
     * @param projectProperties cc properties for the current project
     * @param distributedAgentProperties ccdist properties for this remote build
     * @param progressRemote callback object to provide progress info as the build progresses,
     * can be null.
     * @param remoteResults build artifacts to be returned from the agent to the master, used to agent clear results.
     * @return the build log xml document
     * @throws RemoteException if the remote call fails
     */
    Element doBuild(Builder nestedBuilder, Map<String, String> projectProperties,
                           Map<String, String> distributedAgentProperties, ProgressRemote progressRemote,
                           RemoteResult[] remoteResults) throws RemoteException;

    /**
     * @return the host name on which the Build Agent is running.
     * @throws RemoteException if the remote call fails
     */
    String getMachineName() throws RemoteException;

    /**
     * @return the date this Build Agent started running (not when a specific build started).
     * @throws RemoteException if the remote call fails
     */
    Date getDateStarted() throws RemoteException;


    void claim() throws RemoteException;
    /**
     * @return The date this Build Agent became busy.
     * @throws RemoteException if the remote call fails
     */
    Date getDateClaimed() throws RemoteException;
    /**
     * @return true if this agent is busy.
     * @throws RemoteException if the remote call fails
     */
    boolean isBusy() throws RemoteException;
    /**
     * @return the project being built now, or null if no project is being built.
     * @throws RemoteException if the remote call fails
     */
    String getProjectName() throws RemoteException;


    boolean resultsExist(String resultsType) throws RemoteException;
    byte[] retrieveResultsAsZip(String resultsType) throws RemoteException;

    boolean remoteResultExists(int idx) throws RemoteException;
    byte[] retrieveRemoteResult(int resultIdx) throws RemoteException;

    void clearOutputFiles() throws RemoteException;


    void kill(boolean afterBuildFinished) throws RemoteException;
    /**
     * @return true if a kill command is waiting to execute.
     * @throws RemoteException if the remote call fails
     */
    boolean isPendingKill() throws RemoteException;
    /**
     * @return Date when kill was requested, null if no kill pending.
     * @throws RemoteException if the remote call fails
     */
    Date getPendingKillSince() throws RemoteException;


    void restart(boolean afterBuildFinished) throws RemoteException;
    /**
     * @return true if a restart command is waiting to execute.
     * @throws RemoteException if the remote call fails
     */
    boolean isPendingRestart() throws RemoteException;
    /**
     * @return Date when restart was requested, null if no restart pending.
     * @throws RemoteException if the remote call fails
     */
    Date getPendingRestartSince() throws RemoteException;

    String asString() throws RemoteException;

    /**
     * @param entryOverrides additional entries defined for this agent.
     * @throws RemoteException if the remote call fails
     */
    void setEntryOverrides(PropertyEntry[] entryOverrides) throws RemoteException;
    /**
     * @return additional entries defined for this agent. 
     * @throws RemoteException if the remote call fails
     */
    PropertyEntry[] getEntryOverrides() throws RemoteException;
}
