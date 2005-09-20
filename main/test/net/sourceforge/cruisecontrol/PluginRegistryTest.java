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
package net.sourceforge.cruisecontrol;

import org.jdom.Element;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.builders.AntBuilder;
import net.sourceforge.cruisecontrol.listeners.ListenerTestPlugin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class PluginRegistryTest extends TestCase {

    public void testGetDefaultPropertiesNoPlugin() {
        PluginRegistry registry = PluginRegistry.createRegistry();
        assertEquals(0, registry.getDefaultProperties("qwertyuiop").size());
    }
 
    public void testGettingPluginClass() throws Exception {
        PluginRegistry registry = PluginRegistry.createRegistry();

        assertNotNull(registry.getPluginClass("ant"));
    }

    public void testRegisteringPluginNoClass() {
        PluginRegistry registry = PluginRegistry.createRegistry();

        final String nonExistentClassname =
                "net.sourceforge.cruisecontrol.Foo" + System.currentTimeMillis();
        registry.register("foo", nonExistentClassname);
        try {
            registry.getPluginClass("foo");
            fail("Expected an exception when getting a plugin"
                    + " class that isn't loadable.");
        } catch (CruiseControlException expected) {
        }
    }

    public void testAddingPlugin() throws Exception {
        PluginRegistry registry = PluginRegistry.createRegistry();

        //Add a plugin with a classname that exists
        final Class antBuilderClass = AntBuilder.class;
        final String antBuilderClassname = antBuilderClass.getName();
        registry.register("ant", antBuilderClassname);

        //It should be registered
        assertTrue(registry.isPluginRegistered("ant"));
        assertEquals(antBuilderClassname, registry.getPluginClassname("ant"));
        assertEquals(antBuilderClass, registry.getPluginClass("ant"));
    }

    public void testRootRegistryAndClassnameOverrideOverwrite() throws Exception {
        PluginRegistry registry = PluginRegistry.createRegistry();
        String antClassName = registry.getPluginClassname("ant");
        String nonExistentClassname =
            "net.sourceforge.cruisecontrol.Foo" + System.currentTimeMillis();
        Element pluginElement = new Element("plugin");
        pluginElement.setAttribute("name", "ant");
        pluginElement.setAttribute("classname", nonExistentClassname);
        PluginRegistry.registerToRoot(pluginElement);
        // did the overwrite work?
        assertNotSame(antClassName, registry.getPluginClassname("ant"));
        // now override the root definition in the child registry
        registry.register("ant", antClassName);
        // does it mask the parent definition?
        assertEquals(antClassName, registry.getPluginClassname("ant"));

        // restore the root definition, or we'll wreck the other tests
        pluginElement.setAttribute("classname", antClassName);
        PluginRegistry.registerToRoot(pluginElement);
    }

    // 2 levels of plugin registry, 1 plugin, 2 properties defined in parent, one overriden in child
    public void testGetPluginConfigOverride() throws Exception {

        PluginRegistry registry = PluginRegistry.createRegistry();

        // 2 plugins in the root, with the same plugin class, but different names
        Element rootPluginElement = new Element("plugin");
        rootPluginElement.setAttribute("name", "testlistener");
        rootPluginElement.setAttribute("classname", ListenerTestPlugin.class.getName());
        rootPluginElement.setAttribute("string", "default");
        rootPluginElement.setAttribute("string2", "otherdefault");

        Element rootStringWrapper = new Element("stringwrapper");
        rootStringWrapper.setAttribute("string", "wrapper");
        rootPluginElement.addContent(rootStringWrapper);

        PluginRegistry.registerToRoot(rootPluginElement);

        Element otherRootPluginElement = new Element("plugin");
        otherRootPluginElement.setAttribute("name", "testlistener2");
        otherRootPluginElement.setAttribute("classname", ListenerTestPlugin.class.getName());
        otherRootPluginElement.setAttribute("string", "default2");

        PluginRegistry.registerToRoot(otherRootPluginElement);

        Map defaultProperties;
        // check getDefaultProperties
        defaultProperties = registry.getDefaultProperties("testlistener");
        assertEquals(2, defaultProperties.size());
        assertEquals("default", defaultProperties.get("string"));
        assertEquals("otherdefault", defaultProperties.get("string2"));

        defaultProperties = registry.getDefaultProperties("testlistener2");
        assertEquals(1, defaultProperties.size());
        assertEquals("default2", defaultProperties.get("string"));

        // now let's make some 'overrides'
        Element pluginElement = new Element("plugin");
        pluginElement.setAttribute("name", "testlistener");
        pluginElement.setAttribute("string", "overriden");

        Element stringWrapper = new Element("stringwrapper");
        stringWrapper.setAttribute("string", "overriden");
        pluginElement.addContent(stringWrapper);

        registry.register(pluginElement);

        // test the first plugin
        defaultProperties = registry.getDefaultProperties("testlistener");
        assertEquals(2, defaultProperties.size());
        assertEquals("overriden", defaultProperties.get("string"));
        assertEquals("otherdefault", defaultProperties.get("string2"));

        Element pluginConfig = registry.getPluginConfig("testlistener");
        assertEquals("testlistener", pluginConfig.getName());
        assertEquals("overriden", pluginConfig.getAttributeValue("string"));
        assertEquals("otherdefault", pluginConfig.getAttributeValue("string2"));

        List wrappers = pluginConfig.getChildren("stringwrapper");
        assertEquals(2, wrappers.size());
        Set expectedWrapperAttributes = new TreeSet();
        expectedWrapperAttributes.add("wrapper");
        expectedWrapperAttributes.add("overriden");

        Set wrapperAttributes = new TreeSet();
        for (int i = 0; i < wrappers.size(); i++) {
            org.jdom.Element element = (org.jdom.Element) wrappers.get(i);
            wrapperAttributes.add(element.getAttributeValue("string"));
        }
        assertEquals(expectedWrapperAttributes, wrapperAttributes);

        // test the second plugin
        defaultProperties = registry.getDefaultProperties("testlistener2");
        assertEquals(1, defaultProperties.size());
        assertEquals("default2", defaultProperties.get("string"));

        Element otherPluginConfig = registry.getPluginConfig("testlistener2");
        assertEquals("testlistener2", otherPluginConfig.getName());
        assertEquals("default2", otherPluginConfig.getAttributeValue("string"));
        assertEquals(null, otherPluginConfig.getAttributeValue("string2"));

        PluginRegistry.resetRootRegistry();
    }

    public void testCaseInsensitivityPluginNames() throws Exception {
        //Plugin names are treated as case-insensitive by CruiseControl, so
        //  a plugin named TestName and testname are the same.
        PluginRegistry registry = PluginRegistry.createRegistry();

        //Add a plugin with an all lowercase name
        final String antBuilderClassname = AntBuilder.class.getName();
        registry.register("testname", antBuilderClassname);

        //It should be registered
        assertTrue(registry.isPluginRegistered("testname"));

        //If we ask if other case versions are registered, it should
        //  say "yes"
        assertTrue(registry.isPluginRegistered("TESTNAME"));
        assertTrue(registry.isPluginRegistered("Testname"));
        assertTrue(registry.isPluginRegistered("TestName"));
        assertTrue(registry.isPluginRegistered("tESTnAME"));
    }

    public void testPluginRegistry() throws Exception {

        verifyPluginClass(
                "currentbuildstatusbootstrapper",
                "net.sourceforge.cruisecontrol.bootstrappers.CurrentBuildStatusBootstrapper");
        verifyPluginClass(
                "cvsbootstrapper",
                "net.sourceforge.cruisecontrol.bootstrappers.CVSBootstrapper");
        verifyPluginClass(
                "p4bootstrapper",
                "net.sourceforge.cruisecontrol.bootstrappers.P4Bootstrapper");
        verifyPluginClass(
                "svnbootstrapper",
                "net.sourceforge.cruisecontrol.bootstrappers.SVNBootstrapper");
        verifyPluginClass(
                "vssbootstrapper",
                "net.sourceforge.cruisecontrol.bootstrappers.VssBootstrapper");
        verifyPluginClass("alienbrain", "net.sourceforge.cruisecontrol.sourcecontrols.AlienBrain");
        verifyPluginClass("clearcase", "net.sourceforge.cruisecontrol.sourcecontrols.ClearCase");
        verifyPluginClass("cvs", "net.sourceforge.cruisecontrol.sourcecontrols.CVS");
        verifyPluginClass("filesystem", "net.sourceforge.cruisecontrol.sourcecontrols.FileSystem");
        verifyPluginClass("httpfile", "net.sourceforge.cruisecontrol.sourcecontrols.HttpFile");
        verifyPluginClass("mks", "net.sourceforge.cruisecontrol.sourcecontrols.MKS");
        verifyPluginClass("p4", "net.sourceforge.cruisecontrol.sourcecontrols.P4");
        verifyPluginClass("pvcs", "net.sourceforge.cruisecontrol.sourcecontrols.PVCS");
        verifyPluginClass("svn", "net.sourceforge.cruisecontrol.sourcecontrols.SVN");
        // skipped because not everyone has starteam api jar
        // verifyPluginClass("starteam", "net.sourceforge.cruisecontrol.sourcecontrols.StarTeam");
        verifyPluginClass("vss", "net.sourceforge.cruisecontrol.sourcecontrols.Vss");
        verifyPluginClass("vssjournal", "net.sourceforge.cruisecontrol.sourcecontrols.VssJournal");
        verifyPluginClass("compound", "net.sourceforge.cruisecontrol.sourcecontrols.Compound");
        verifyPluginClass("triggers", "net.sourceforge.cruisecontrol.sourcecontrols.Triggers");
        verifyPluginClass("targets", "net.sourceforge.cruisecontrol.sourcecontrols.Targets");
        verifyPluginClass("ant", "net.sourceforge.cruisecontrol.builders.AntBuilder");
        verifyPluginClass("maven", "net.sourceforge.cruisecontrol.builders.MavenBuilder");
        verifyPluginClass("pause", "net.sourceforge.cruisecontrol.PauseBuilder");
        verifyPluginClass(
                "labelincrementer",
                "net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer");
        verifyPluginClass(
                "artifactspublisher",
                "net.sourceforge.cruisecontrol.publishers.ArtifactsPublisher");
        verifyPluginClass(
                "currentbuildstatuspublisher",
                "net.sourceforge.cruisecontrol.publishers.CurrentBuildStatusPublisher");
        verifyPluginClass("email", "net.sourceforge.cruisecontrol.publishers.LinkEmailPublisher");
        verifyPluginClass(
                "htmlemail",
                "net.sourceforge.cruisecontrol.publishers.HTMLEmailPublisher");
        verifyPluginClass("execute", "net.sourceforge.cruisecontrol.publishers.ExecutePublisher");
        verifyPluginClass("scp", "net.sourceforge.cruisecontrol.publishers.SCPPublisher");
        verifyPluginClass("modificationset", "net.sourceforge.cruisecontrol.ModificationSet");
        verifyPluginClass("schedule", "net.sourceforge.cruisecontrol.Schedule");
        verifyPluginClass("buildstatus", "net.sourceforge.cruisecontrol.sourcecontrols.BuildStatus");
        verifyPluginClass("clearcasebootstrapper", "net.sourceforge.cruisecontrol.bootstrappers.ClearCaseBootstrapper");
        verifyPluginClass("xsltlogpublisher", "net.sourceforge.cruisecontrol.publishers.XSLTLogPublisher");
        verifyPluginClass("httpfile", "net.sourceforge.cruisecontrol.sourcecontrols.HttpFile");
        verifyPluginClass("currentbuildstatuslistener",
                "net.sourceforge.cruisecontrol.listeners.CurrentBuildStatusListener");
        verifyPluginClass("onsuccess", "net.sourceforge.cruisecontrol.publishers.OnSuccessPublisher");
    }

    static void verifyPluginClass(String pluginName, String expectedName)
            throws Exception {
        PluginRegistry registry = PluginRegistry.loadDefaultPluginRegistry();

        assertTrue(registry.isPluginRegistered(pluginName));

        String className = registry.getPluginClassname(pluginName);
        assertEquals(expectedName, className);

        Class pluginClass = Class.forName(className);
        // casts to suppress varargs warnings under sdk >= 1.5
        pluginClass.getConstructor((Class[]) null).newInstance((Object[]) null);
    }
}
