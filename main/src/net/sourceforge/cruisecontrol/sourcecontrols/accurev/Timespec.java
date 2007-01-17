/********************************************************************************
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
 ********************************************************************************/
/*
 * Created on 29-Jun-2005 by norru
 *
 * Copyright (C) Sony Computer Entertainment Europe
 *               Studio Liverpool Server Group
 *
 * Authors:
 *     Nicola Orru' <Nicola_Orru@scee.net>
 */
package net.sourceforge.cruisecontrol.sourcecontrols.accurev;

/**
 * Contains the abstract definition of the time-spec parameter fragment
 * <p/>
 * According to the 'accurev help' reference, time-spec can be:
 * <p/>
 * <ul>
 * <li>time in YYYY/MM/DD HH:MM:SS format</li>
 * <li>time keyword: now</li>
 * <li>transaction number</li>
 * <li>transaction keyword: highest</li>
 * </ul>
 * <p/>
 * <p/>
 * See the corresponding specific subclasses for them:
 * </p>
 *
 * @author <a href="mailto:Nicola_Orru@scee.net">Nicola Orru'</a>
 * @see DateTimespec
 * @see KeywordTimespec
 * @see TransactionNumberTimespec
 */
public abstract class Timespec {
    private int count;
    private boolean hasCount;

    /**
     * Creates a new Timespec with no count fragment
     */
    public Timespec() {
    }

    /**
     * Creates a new Timespec having the given count
     *
     * @param count the count
     */
    public Timespec(int count) {
        this.count = count;
        this.hasCount = true;
    }

    /**
     * Returns the main part (without count) of the timespec
     *
     * @return the formatted main timespec
     */
    public abstract String format();

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(format());
        if (hasCount) {
            buf.append(".");
            buf.append(count);
        }
        return buf.toString();
    }
}
