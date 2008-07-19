/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2008, Paul Julius
 * PO Box 1812
 * N Sioux City, SD 57049
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

import static junit.framework.Assert.assertEquals;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.util.IO;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ArtifactRevisionId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:paul@willowbark.com">Paul Julius</a>
 */
public class IvySourceControlTest {
    private static final Date A_LONG_TIME_AGO = new GregorianCalendar(1905, Calendar.JANUARY, 1).getTime();
    private static final Date EVEN_LONGER_AGO = new GregorianCalendar(1904, Calendar.JANUARY, 1).getTime();
    private IvySourceControl ivy;

    @Before
    public void setUp() {
        ivy = new IvySourceControl();
    }

    @Test
    public void shouldReportNoModificationsWhenNoArtifactsFound() {
        List<Artifact> artifacts = new ArrayList<Artifact>();
        assertEquals(0, ivy.getModifications(null, artifacts).size());
    }

    @Test
    public void shouldReportModifiedWhenArtifactPublicationAfterLastBuild() {
        List<Modification> modifications =
                getTestingMod();
        assertEquals(1, modifications.size());
        assertEquals("foo.jar", modifications.get(0).getFileName());
    }

    @Test
    public void shouldReportNotModifiedWhenArtifactPublicationBeforeLastBuildOnSecondRun() {
        Collection<Artifact> artifacts = artifacts(new MockArtifact("foo.jar", EVEN_LONGER_AGO));
        getMods(artifacts); //First run
        List<Modification> secondRun = getMods(artifacts);
        assertEquals(0, secondRun.size());
    }

    private List<Modification> getMods(Collection<Artifact> artifacts) {
        return ivy.getModifications(A_LONG_TIME_AGO, artifacts);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldBombWhenNullArtifacts() {
        getMods(null);
    }

    @Test
    public void shouldReportNoDependenciesWhenAllOlder() {
        List<Modification> modifications =
                getMods(artifacts(new MockArtifact("foo.jar", EVEN_LONGER_AGO),
                        new MockArtifact("bar.jar", EVEN_LONGER_AGO)));
        assertEquals(0, modifications.size());
    }

    @Test
    public void shouldBeIvyType() {
        assertEquals("ivy", firstMod().getType());
    }

    @Test
    public void shouldSetCommentsToBlank() {
        assertEquals("", firstMod().getComment());
    }

    @Test
    public void shouldSetUserToDefault() {
        assertEquals("User", firstMod().getUserName());
    }

    @Test
    public void shouldBeAbleToChangeUsernameReported() {
        String user = "myusername" + System.currentTimeMillis();
        ivy.setUserName(user);
        assertEquals(user, firstMod().getUserName());
    }

    @Test
    public void shouldSetRevisionDateToPublicationDate() {
        Date publicationDate = new Date();
        List<Modification> mods = getMods(artifacts(new MockArtifact("foo.jar", publicationDate)));
        assertEquals(publicationDate, mods.get(0).getModifiedTime());
    }

    @Test
    public void shouldSetRevisionToArtifactRevision() {
        MockArtifact mockArtifact = new MockArtifact("foo.jar");
        ModuleId id = new ModuleId("organization", "name");
        ArtifactRevisionId artifactId =
                new ArtifactRevisionId(
                        new ArtifactId(id, "name", "type", "ext" + System.currentTimeMillis()),
                        new ModuleRevisionId(id, "revision")
                );
        mockArtifact.setArtifactRevisionId(artifactId);
        List<Modification> mods = getMods(artifacts(mockArtifact));
        assertEquals(artifactId.toString(), mods.get(0).getRevision());
    }

    @Test
    public void shouldSetActionToChange() {
        assertEquals("change", ((Modification.ModifiedFile) firstMod().getModifiedFiles().get(0)).action);
    }

    @Test
    public void shouldSetFolderToUrlMinusFilename() {
        MockArtifact mockArtifact = new MockArtifact("foo.jar");
        mockArtifact.setUrl("file://dev/null/foo.jar");
        List<Modification> mods = getMods(artifacts(mockArtifact));
        assertEquals("file://dev/null", mods.get(0).getFolderName());
    }

    @Test
    public void shouldDefaultIvyXmlToLocalOne() {
        assertEquals(new File("ivy.xml").getAbsolutePath(), ivy.getIvyXml());
    }

    @Test
    public void shouldDefaultIvySettingsToLocalOne() {
        assertEquals(new File("ivysettings.xml").getAbsolutePath(), ivy.getIvySettings());
    }

    @Test
    public void shouldPassValidationWhenRequiredParametersSet() throws IOException, CruiseControlException {
        File ivyXml = File.createTempFile(getClass().getName(), "ivy.xml");
        File ivySettings = File.createTempFile(getClass().getName(), "ivysettings.xml");
        try {
            ivy.setIvyXml(ivyXml.getAbsolutePath());
            ivy.setIvySettings(ivySettings.getAbsolutePath());
            ivy.validate();
        } finally {
            IO.delete(ivyXml);
        }
    }

    @Test(expected = CruiseControlException.class)
    public void shouldFailValidationWhenIvyXmlDoesNotExist() throws CruiseControlException, IOException {
        File ivySettings = File.createTempFile(getClass().getName(), "ivysettings.xml");
        try {
            ivy.setIvyXml("THISFILEDOESNTEXIST.ivy.xml");
            ivy.setIvySettings(ivySettings.getAbsolutePath());
            ivy.validate();
        } finally {
            IO.delete(ivySettings);
        }
    }

    @Test(expected = CruiseControlException.class)
    public void shouldFailValidationWhenIvySettingsDoesNotExist() throws CruiseControlException, IOException {
        File ivyXml = File.createTempFile(getClass().getName(), "ivy.xml");
        try {
            ivy.setIvyXml(ivyXml.getAbsolutePath());
            ivy.setIvySettings("THISFILEDOESNTEXIST.ivysettings.xml");
            ivy.validate();
        } finally {
            IO.delete(ivyXml);
        }
    }

    private Modification firstMod() {
        return getTestingMod().get(0);
    }

    private List<Modification> getTestingMod() {
        return getMods(artifacts(new MockArtifact("foo.jar")));
    }

    private Collection<Artifact> artifacts(Artifact... facts) {
        Collection<Artifact> artifacts = new ArrayList<Artifact>();
        artifacts.addAll(Arrays.asList(facts));
        return artifacts;
    }


    private static class MockArtifact implements Artifact {
        private final String name;
        private final Date publicationDate;
        private ArtifactRevisionId artifactRevisionId;
        private String url;

        public MockArtifact(String name) {
            this(name, new Date());
        }

        public MockArtifact(String name, Date publicationDate) {
            this.name = name;
            this.publicationDate = publicationDate;
            this.artifactRevisionId =
                    new ArtifactRevisionId(
                            new ArtifactId(new ModuleId("organization", "name"), "name", "type", "ext"),
                            new ModuleRevisionId(new ModuleId("organization", "name"), "revision")
                    );
            this.url = "file://bar";
        }

        public ModuleRevisionId getModuleRevisionId() {
            return null;
        }

        public Date getPublicationDate() {
            return publicationDate;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return null;
        }

        public String getExt() {
            return null;
        }

        public URL getUrl() {
            try {
                return new URL(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                fail(e.getMessage());
                return null;
            }
        }

        public String[] getConfigurations() {
            return new String[0];
        }

        public ArtifactRevisionId getId() {
            return artifactRevisionId;
        }

        public boolean isMetadata() {
            return false;
        }

        public String getAttribute(String attName) {
            return null;
        }

        public String getStandardAttribute(String attName) {
            return null;
        }

        public String getExtraAttribute(String attName) {
            return null;
        }

        public Map getAttributes() {
            return null;
        }

        public Map getStandardAttributes() {
            return null;
        }

        public Map getExtraAttributes() {
            return null;
        }

        public Map getQualifiedExtraAttributes() {
            return null;
        }

        public void setArtifactRevisionId(ArtifactRevisionId artifactRevisionId) {
            this.artifactRevisionId = artifactRevisionId;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
