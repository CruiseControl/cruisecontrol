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
package net.sourceforge.cruisecontrol.config;

import net.sourceforge.cruisecontrol.CruiseControlException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.log4j.Logger;

/**
 * The interface defining methods responsible for the general resolving of files used
 * to control CruiseControl.
 * 
 * The aim is to limit file system access to a {@link FileResolver} implementation,
 * which is good for testing, but also useful if we wanted to do something like try resolving 
 * paths against different known contexts (e.g. parent directories) rather than being limited 
 * to the working directory.
 * 
 * As the bonus, the resolved files are automatically monitored for changes which cause the 
 * reload of Cruisecontrol configuration.   
 */
public interface FileResolver {

    /**
     * Resolves the path (either absolute or relative to the location of CruiseControl 
     * configuration file).
     * 
     * @param path to file to resolve. 
     * @return stream to read the content of file from.
     * @throws CruiseControlException if file can not be read.
     */
    InputStream getInputStream(String path) throws CruiseControlException;

    
    /**
     * Dummy FileResolver implementation for case when "real" FileResolver is not available.
     * The implementation is straight wrapper for 
     * <code>return new BufferedInputStream(new FileInputStream(path))</code>, nothing more.
     */
    public static final class DummyResolver implements FileResolver {
        private static final Logger LOG = Logger.getLogger(DummyResolver.class);

        /** The implementation of {@link FileResolver#getInputStream(String)} returning raw
         *  stream to read data from */
        public InputStream getInputStream(final String path) throws CruiseControlException {
            LOG.warn("Using dummy resolver for file '" + path + "'. Changes in the file will not be reflected" 
                + "in the project!");
            final File file = new File(path);
            try {
                return new BufferedInputStream(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                throw new CruiseControlException("exception when opening file " + file.getAbsolutePath(), e);
            }
        }
    }
    
}
