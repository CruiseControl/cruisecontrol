/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005 ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.bootstrappers.CVSBootstrapper;
import net.sourceforge.cruisecontrol.builders.AntBuilder;
import net.sourceforge.cruisecontrol.buildloggers.MergeLogger;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import net.sourceforge.cruisecontrol.listeners.CurrentBuildStatusListener;
import net.sourceforge.cruisecontrol.publishers.FTPPublisher;
import net.sourceforge.cruisecontrol.publishers.email.EmailMapping;
import net.sourceforge.cruisecontrol.publishers.email.PropertiesMapper;
import net.sourceforge.cruisecontrol.sourcecontrols.ConcurrentVersionsSystem;

public class PluginTypeTest extends TestCase {

    public void testGettingTypeForPlugin() {
        PluginType type = PluginType.find(CVSBootstrapper.class);
        assertSame(PluginType.BOOTSTRAPPER, type);
        type = PluginType.find(ProjectConfig.Bootstrappers.class);
        assertSame(PluginType.BOOTSTRAPPERS, type);

        type = PluginType.find(AntBuilder.class);
        assertSame(PluginType.BUILDER, type);

        type = PluginType.find(CCDateFormat.class);
        assertSame(PluginType.DATE_FORMAT, type);

        type = PluginType.find(DefaultLabelIncrementer.class);
        assertSame(PluginType.LABEL_INCREMENTER, type);

        type = PluginType.find(CurrentBuildStatusListener.class);
        assertSame(PluginType.LISTENER, type);
        type = PluginType.find(ProjectConfig.Listeners.class);
        assertSame(PluginType.LISTENERS, type);

        type = PluginType.find(Log.class);
        assertSame(PluginType.LOG, type);

        type = PluginType.find(EmailMapping.class);
        assertSame(PluginType.MAP, type);

        type = PluginType.find(MergeLogger.class);
        assertSame(PluginType.MERGE_LOGGER, type);

        type = PluginType.find(ModificationSet.class);
        assertSame(PluginType.MODIFICATION_SET, type);

        type = PluginType.find(ProjectConfig.class);
        assertSame(PluginType.PROJECT, type);

        type = PluginType.find(PropertiesMapper.class);
        assertSame(PluginType.EMAIL_MAPPER, type);

        type = PluginType.find(FTPPublisher.class);
        assertSame(PluginType.PUBLISHER, type);
        type = PluginType.find(ProjectConfig.Publishers.class);
        assertSame(PluginType.PUBLISHERS, type);

        type = PluginType.find(Schedule.class);
        assertSame(PluginType.SCHEDULE, type);

        type = PluginType.find(ConcurrentVersionsSystem.class);
        assertSame(PluginType.SOURCE_CONTROL, type);
    }

    public void testExceptions() {
        try {
            PluginType.find(Object.class);
            fail("Should not be able to find plugin type for Object.");
        } catch (IllegalArgumentException expected) {
            assertEquals("class java.lang.Object is not a CruiseControl plugin.", expected.getMessage());
        }

        try {
            PluginType.find((Class) null);
            fail("Should not be able to find plugin type for null.");
        } catch (IllegalArgumentException expected) {
            assertEquals("null is not a CruiseControl plugin.", expected.getMessage());
        }

        try {
            PluginType.find((String) null);
            fail("Should not be able to find plugin type for null.");
        } catch (IllegalArgumentException expected) {
            assertEquals("null is not a CruiseControl plugin.", expected.getMessage());
        }
    }

    public void testGettingTypes() {
        PluginType[] types = PluginType.getTypes();

        assertNotNull(types);
        assertTrue(0 < types.length);
    }

    public void testGettingNameForPlugin() {
        PluginType type = PluginType.find(CVSBootstrapper.class);
        assertEquals("bootstrapper", type.getName());

        type = PluginType.find(FTPPublisher.class);
        assertEquals("publisher", type.getName());

        type = PluginType.find(ConcurrentVersionsSystem.class);
        assertEquals("sourcecontrol", type.getName());
    }

    public void testEquals() {
        assertFalse(PluginType.BOOTSTRAPPER.equals(null));
        assertFalse(PluginType.BOOTSTRAPPER.equals(new Object()));
        assertFalse(PluginType.BOOTSTRAPPER.equals(PluginType.SOURCE_CONTROL));
        assertTrue(PluginType.BOOTSTRAPPER.equals(PluginType.BOOTSTRAPPER));
    }

    public void testToString() {
        assertEquals("bootstrapper", PluginType.BOOTSTRAPPER.toString());
        assertEquals("publisher", PluginType.PUBLISHER.toString());
        assertEquals("sourcecontrol", PluginType.SOURCE_CONTROL.toString());
    }
}
