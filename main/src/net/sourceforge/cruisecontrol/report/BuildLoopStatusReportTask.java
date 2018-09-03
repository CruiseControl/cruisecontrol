/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.report;

import java.util.TimerTask;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;

import net.sourceforge.cruisecontrol.BuildLoopInformationBuilder;



public class BuildLoopStatusReportTask extends TimerTask {
    private final BuildLoopInformationBuilder builder;

    private static final Logger LOGGER = Logger.getLogger(BuildLoopStatusReportTask.class);

    private final HttpClient http;

    private final String url;

    private String sent;

    private String response;

    public BuildLoopStatusReportTask(BuildLoopInformationBuilder builder, String url) {
        this(builder, url, new HttpClient(), 3000);
    }

    public BuildLoopStatusReportTask(BuildLoopInformationBuilder builder, String url, HttpClient http,
            int timeout) {
        this.builder = builder;
        this.url = url;
        this.http = http;
        this.http.getParams().setSoTimeout(timeout);
    }

    @Override
    public void run() {
        run(new PostMethod(url));
    }

    public synchronized String getSent() {
        return this.sent;
    }

    public synchronized String getReponse() {
        return this.response;
    }

    public synchronized void run(PostMethod postMethod) {
        try {
            this.sent = builder.buildBuildLoopInformation().toXml();
            postMethod.setRequestEntity(new StringRequestEntity(sent));
            int statusCode = http.executeMethod(postMethod);
            if (statusCode != HttpStatus.SC_OK) {
                LOGGER.warn("Method failed: " + postMethod.getStatusLine());
            }
            this.response = new String(postMethod.getResponseBody());
        } catch (Exception e) {
            LOGGER.warn("Failed to reach dashboard instance : " + this.url
                    + ", either the dashboard has not started up or there is a network problem.", e);
        } finally {
            postMethod.releaseConnection();
        }
    }
}
