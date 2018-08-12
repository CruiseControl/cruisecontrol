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
package net.sourceforge.cruisecontrol.bootstrappers;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.builders.AbstractAntBuilderDelegate;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.ExamplesFile;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;

import java.util.HashMap;

/**
 * A thin wrapper around the AntBuilder class, this class allows you to call an Ant script as a bootstrapper.
 *
 * @see net.sourceforge.cruisecontrol.builders.AntBuilder
 * @author <a href="mailto:rjmpsmith@hotmail.com">Robert J. Smith</a>
 */
@Description("Executes an Ant script which implements a custom bootstrapper.")
@ExamplesFile
public class AntBootstrapper extends AbstractAntBuilderDelegate implements Bootstrapper {

    private static final Logger LOG = Logger.getLogger(AntBootstrapper.class);

    /**
     * @see net.sourceforge.cruisecontrol.Bootstrapper#bootstrap()
     */
    public void bootstrap() throws CruiseControlException {
        final Element result = getDelegate().build(new HashMap<String, String>(), null);
        if (result == null) {
            throw new CruiseControlException("Build returned null.  Bootstrap failed.");
        } else {
            final Attribute error = result.getAttribute("error");
            if (error != null) {
                throw new CruiseControlException("Bootstrap failed with error: " + error.getValue());
            }
            LOG.info("Bootstrap successful.");
        }
    }

}
