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

public interface BuildAgentService extends Remote {

    public Element doBuild(Builder nestedBuilder, Map projectProperties,
                           Map distributedAgentProperties) throws RemoteException;

    public String getMachineName() throws RemoteException;

    /**
     * @return the date this Build Agent started running (not when a specific build started).
     * @throws RemoteException if the remote call fails
     */
    public Date getDateStarted() throws RemoteException;

    public void claim() throws RemoteException;

    public Date getDateClaimed() throws RemoteException;

    public boolean isBusy() throws RemoteException;

    /**
     * @return the module being built now, or null if no module is being built.
     * @throws RemoteException if the remote call fails
     */
    public String getModule() throws RemoteException;

    public boolean resultsExist(String resultsType) throws RemoteException;

    public byte[] retrieveResultsAsZip(String resultsType) throws RemoteException;

    public void clearOutputFiles() throws RemoteException;

    public void kill(boolean afterBuildFinished) throws RemoteException;

    public void restart(boolean afterBuildFinished) throws RemoteException;

    public boolean isPendingKill() throws RemoteException;

    public Date getPendingKillSince() throws RemoteException;

    public boolean isPendingRestart() throws RemoteException;

    public Date getPendingRestartSince() throws RemoteException;

    public String asString() throws RemoteException;

    public void setEntryOverrides(PropertyEntry[] entryOverrides) throws RemoteException;

    public void clearEntryOverrides() throws RemoteException;
}
