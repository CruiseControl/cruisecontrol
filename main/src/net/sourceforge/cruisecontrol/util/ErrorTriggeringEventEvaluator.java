/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
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

package net.sourceforge.cruisecontrol.util;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.TriggeringEventEvaluator;

/**
 * A class that triggers log4j's SMTPAppender. Look at the class in log4j at 
 * <a href="http://logging.apache.org/log4j/docs/api/org/apache/log4j/spi/TriggeringEventEvaluator.html">
 *    TriggeringEventEvaluator</a>. 
 * To enable this, you need to override CC's log4j configuration file (placed in the root of the cruisecontrol.jar).
 * You can add an appender with the following (this is in xml format you might as well use the property format).
 * 
 * &lt;appender name="MailAppender" class="org.apache.log4j.net.SMTPAppender"&gt;
 *   &lt;param name="BufferSize" value="100"/&gt;
 *   &lt;param name="EvaluatorClass" value="net.sourceforge.cruisecontrol.util.ErrorTriggeringEventEvaluator"/&gt;
 *   &lt;param name="From" value="cc@edb4tel.com"/&gt;
 *   &lt;param name="LocationInfo" value="false"/&gt;
 *   &lt;param name="SMTPHost" value="xxx"/&gt;
 *   &lt;param name="Subject" value="CC has had an error!!!"/&gt;
 *   &lt;param name="To" value="mora@elitserien-2004.com"/&gt;   <!-- NB the "to"-parameter is comma separated!! -->
 *   &lt;layout class="org.apache.log4j.PatternLayout"&gt;
 *     &lt;param name="ConversionPattern" value="%d{dd.MM.yyyy HH:mm:ss} %-5p [%x] [%c{3}] %m%n"/&gt;
 *   &lt;/layout&gt;
 * &lt;/appender&gt;
 * 
 * and add this appender to the root logger, you will receive a mail if an error occurs and the preceding 100 lines.
 * Read more about log4J at <a href="http://logging.apache.org/log4j/docs/documentation.html">log4j documentation</a>
 *
 * @author <a href="mailto:erik@zenior.no">Erik Romson</a>
 */
public class ErrorTriggeringEventEvaluator implements TriggeringEventEvaluator {

    public boolean isTriggeringEvent(LoggingEvent event) {
        return event.level.isGreaterOrEqual(Level.ERROR);
    }

}
