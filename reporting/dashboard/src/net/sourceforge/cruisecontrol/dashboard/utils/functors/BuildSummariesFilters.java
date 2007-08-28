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
package net.sourceforge.cruisecontrol.dashboard.utils.functors;

import java.io.FilenameFilter;

import org.joda.time.DateTime;

public final class BuildSummariesFilters {
    private BuildSummariesFilters() {
    }

    private static FilenameFilter cclog = new CCLogFilter();
    private static SuccessLogFilter success = new SuccessLogFilter();

    private static FilenameFilter succeeded = and(cclog, success);

    private static FilenameFilter failed = and(cclog, not(success));

    public static FilenameFilter cclogFilter() {
        return cclog;
    }

    public static FilenameFilter succeedFilter() {
        return succeeded;
    }

    public static ReportableFilter earliestSucceededFilter(DateTime time) {
        return new ReportAdapterFilter(and(succeeded, earliestLog(time)));
    }

    public static ReportableFilter lastSucceedFilter(DateTime time) {
        return new ReportAdapterFilter(and(succeeded, lastLog(time)));
    }

    public static ReportableFilter earliestFailedFilter(DateTime time) {
        return new ReportAdapterFilter(and(failed, earliestLog(time)));
    }

    public static ReportableFilter lastFailedFilter(DateTime time) {
        return new ReportAdapterFilter(and(failed, lastLog(time)));
    }

    public static ReportableFilter lastFilter() {
        return new ReportAdapterFilter(and(cclog, new NearestLogFilter(new DateTime())));
    }

    private static final FilenameFilter and(FilenameFilter first, FilenameFilter second) {
        return new And(new FilenameFilter[] {first, second});
    }

    private static final Not not(FilenameFilter filter) {
        return new Not(filter);
    }

    private static FilenameFilter earliestLog(final DateTime time) {
        return and(not(new OlderLogFilter(time)), new NearestLogFilter(time));
    }

    private static FilenameFilter lastLog(final DateTime time) {
        return and(new OlderLogFilter(time), new NearestLogFilter(time));
    }
}
