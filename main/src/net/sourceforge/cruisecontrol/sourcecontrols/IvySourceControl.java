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

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * A sourcecontrol implementation that monitors artifacts in an Ivy repository.
 *
 * @author <a href="mailto:paul@willowbark.com">Paul Julius</a>
 */
public class IvySourceControl extends FakeUserSourceControl {
    private static final Logger LOG = Logger.getLogger(IvySourceControl.class);

    private File ivyXml = new File("ivy.xml");
    private File ivySettings = new File("ivysettings.xml");

    /**
     * @param now IGNORED
     */
    @Override
    public List<Modification> getModifications(final Date lastBuild, final Date now) {
        try {
            return getModifications(lastBuild, artifacts());
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

        return Collections.emptyList();
    }

    @Override
    public void validate() throws CruiseControlException {
        ValidationHelper.assertExists(ivyXml, "ivyXml", getClass());
        ValidationHelper.assertExists(ivySettings, "ivySettings", getClass());
    }

    protected List<Modification> getModifications(final Date lastBuild, final Collection<Artifact> artifacts) {
        if (artifacts == null) {
            throw new IllegalArgumentException("Cannot process null artifact collection");
        }

        final ArrayList<Modification> mods = new ArrayList<Modification>();
        for (Artifact artifact : artifacts) {
            if (artifact.getPublicationDate().compareTo(lastBuild) > 0) {
                mods.add(modificationFor(artifact));
            }
        }
        return mods;
    }

    @SuppressWarnings ("unchecked")
    Collection<Artifact> artifacts() throws IOException, ParseException {
        final Ivy ivy = Ivy.newInstance();
        ivy.configure(ivySettings.toURI().toURL());
        return ivy.resolve(ivyXml.toURI().toURL()).getArtifacts();
    }

    private Modification modificationFor(final Artifact artifact) {
        final Modification modification =
                new Modification("ivy", getUserName(), "", null, artifact.getPublicationDate(),
                        artifact.getId().toString(), new ArrayList<Modification.ModifiedFile>());
        final Modification.ModifiedFile modfile =
                modification.createModifiedFile(artifact.getName(),
                        folderFor(artifact.getUrl(), artifact.getName()));
        modfile.action = "change";
        return modification;
    }

    private String folderFor(final URL url, final String name) {
        return url.toString().replace("/" + name, "");
    }

    public String getIvyXml() {
        return ivyXml.getAbsolutePath();
    }

    public void setIvyXml(final String ivyXml) {
        this.ivyXml = new File(ivyXml);
    }

    public void setIvySettings(final String ivySettings) {
        this.ivySettings = new File(ivySettings);
    }

    public String getIvySettings() {
        return ivySettings.getAbsolutePath();
    }
}
