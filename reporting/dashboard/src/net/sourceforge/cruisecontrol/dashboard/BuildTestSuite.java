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
package net.sourceforge.cruisecontrol.dashboard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class BuildTestSuite {
    private float duration;

    private String name;

    private List testCases;

    public BuildTestSuite(String name, float duration) {
        this.duration = duration;
        this.name = name;
        this.testCases = null;
    }

    public int getNumberOfErrors() {
        return checkTestCases() ? getErrorTestCases().size() : 0;
    }

    public String getName() {
        return name;
    }

    public int getNumberOfFailures() {
        return checkTestCases() ? getFailingTestCases().size() : 0;
    }

    public int getNumberOfTests() {
        return checkTestCases() ? testCases.size() : 0;
    }

    public float getDurationInSeconds() {
        return duration;
    }

    public List getErrorTestCases() {
        if (checkTestCases()) {
            List errorCases = new ArrayList();
            for (int i = 0; i < testCases.size(); i++) {
                BuildTestCase testCase = (BuildTestCase) testCases.get(i);
                BuildTestCaseResult result = testCase.getResult();
                if (result.equals(BuildTestCaseResult.ERROR)) {
                    errorCases.add(testCase);
                }
            }
            return errorCases;
        }
        return null;
    }

    public List getFailingTestCases() {
        if (checkTestCases()) {
            List failingCases = new ArrayList();
            for (int i = 0; i < testCases.size(); i++) {
                BuildTestCase testCase = (BuildTestCase) testCases.get(i);
                if (testCase.getResult().equals(BuildTestCaseResult.FAILED)) {
                    failingCases.add(testCase);
                }
            }
            return failingCases;
        }
        return null;
    }

    public List getPassedTestCases() {
        if (checkTestCases()) {
            List passedCases = new ArrayList();
            for (int i = 0; i < testCases.size(); i++) {
                BuildTestCase testCase = (BuildTestCase) testCases.get(i);
                BuildTestCaseResult result = testCase.getResult();
                if (result.equals(BuildTestCaseResult.PASSED)) {
                    passedCases.add(testCase);
                }
            }
            return passedCases;
        }
        return null;
    }

    public void appendTestCases(List tests) {
        this.testCases = tests;
    }

    public void addTestCase(BuildTestCase testCase) {
        if (!checkTestCases()) {
            this.testCases = new LinkedList();
        }
        this.testCases.add(testCase);
    }

    private boolean checkTestCases() {
        return this.testCases != null;
    }

    public boolean isFailed() {
        if (checkTestCases()) {
            for (Iterator iterator = testCases.iterator(); iterator.hasNext();) {
                BuildTestCase testCase = (BuildTestCase) iterator.next();
                if (testCase.getResult().equals(BuildTestCaseResult.ERROR) || testCase.getResult()
                        .equals(BuildTestCaseResult.FAILED)) {
                    return true;
                }
            }
        }
        return false;
    }
}
