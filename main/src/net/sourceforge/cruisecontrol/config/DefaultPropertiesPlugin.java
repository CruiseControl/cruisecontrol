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
package net.sourceforge.cruisecontrol.config;

import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.DescriptionFile;
import net.sourceforge.cruisecontrol.gendoc.annotations.ExamplesFile;
import net.sourceforge.cruisecontrol.gendoc.annotations.Optional;
import net.sourceforge.cruisecontrol.gendoc.annotations.SkipDoc;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
import net.sourceforge.cruisecontrol.ProjectXMLHelper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.ResolverUser;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@DescriptionFile
@ExamplesFile
public class DefaultPropertiesPlugin implements PropertiesPlugin, ResolverUser {
   private String file;
   private String environment;
   private String name;
   private String value;
   private String toupper;
   private FileResolver fileResolver; // used to get file to read properties from


  /**
   * Sets the instance of {@link FileResolver}. It must be ensured that this method
   * is called earlier than the other methods using the file resolver. And it is claimed
   * in {@link ResolverUser#setFileResolver(FileResolver)} description that it really is
   * ensured.
   *
   * @param resolver the instance to fill;
   */
  @SkipDoc
  public void setFileResolver(final FileResolver resolver) {

    fileResolver = resolver;
  }
  /**
   * The implementation of {@link ResolverUser#setXmlResolver(XmlResolver)}. It does nothing,
   * actually, since XML resolver is not needed here.
   */
  @SkipDoc
  public void setXmlResolver(final XmlResolver resolver) {
    // not needed
  }

  @Description("The name of the property to set.")
  @Optional("Exactly one of name, environment, or file is required.")
  public void setName(String name) {
    this.name = name;
  }

  @Description(
      "The prefix to use when retrieving environment variables. Thus if you specify "
      + "environment=\"myenv\" you will be able to access OS-specific environment variables "
      + "via property names such as \"myenv.PATH\" or \"myenv.MAVEN_HOME\".")
  @Optional("Exactly one of name, environment, or file is required.")
  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  @Description("The filename of the property file to load.")
  @Optional("Exactly one of name, environment, or file is required.")
  public void setFile(String file) {
    this.file = file;
  }

  @Description("The value of the property. This may contain any previously defined properties.")
  @Optional("Must be set if name was set.")
  public void setValue(String value) {
    this.value = value;
  }

  @Description("Used in conjunction with environment. If set to true, all environment "
      + "variable names will be converted to upper case.")
  @Optional
  @Default("false")
  public void setToupper(String toupper) {
    this.toupper = toupper;
  }
  
  /**
   * Called after the configuration is read to make sure that all the mandatory parameters were specified..
   * @throws CruiseControlException if there was a configuration error.
   */
  public void validate() throws CruiseControlException {
      if (name == null && file == null && environment == null) {
        ValidationHelper.fail("At least one of name, file or environment must be set.");
      }
      if ((name != null && (file != null || environment != null)
       || (file != null && (environment != null)))) {
        ValidationHelper.fail("At most one of name, file or environment can be set.");
      }

      if (file != null && file.trim().length() > 0) {
          // TODO FIXME add exists check.
      }
      if (name != null && value == null) {
        ValidationHelper.fail("name and value must be set simultaneoulsy.");
      }
  }

  public void loadProperties(final Map<String, String> props, final boolean failIfMissing)
          throws CruiseControlException {

    // Resolve file name, if there are properties missing
    final String fname = Util.parsePropertiesInString(props, this.file, true);
    final boolean toUpperValue = "true".equals(toupper);

    if (fname != null && fname.trim().length() > 0) {
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(fileResolver.getInputStream(fname)));
            try {
                // Read the theFile line by line, expanding macros
                // as we go. We must do this manually to preserve the
                // order of the properties.
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.length() == 0 || line.charAt(0) == '#') {
                        continue;
                    }
                    final int index = line.indexOf('=');
                    if (index < 0) {
                        continue;
                    }
                    final String parsedName
                        = Util.parsePropertiesInString(props, line.substring(0, index).trim(), failIfMissing);
                    final String parsedValue
                        = Util.parsePropertiesInString(props, line.substring(index + 1).trim(), failIfMissing);
                    ProjectXMLHelper.setProperty(props, parsedName, parsedValue);
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new CruiseControlException("Could not load properties from the File \"" + fname
                    + "\".", e);
        }
    } else if (environment != null) {
        // Load the environment into the project's properties
        for (final String line : new OSEnvironment().getEnvironment()) {
            int index = line.indexOf('=');
            if (index < 0) {
                continue;
            }
            // If the toupper attribute was set, upcase the variables
            final StringBuilder propName = new StringBuilder(environment);
            propName.append(".");
            if (toUpperValue) {
                propName.append(line.substring(0, index).toUpperCase());
            } else {
                propName.append(line.substring(0, index));
            }
            final String parsedValue
                    = Util.parsePropertiesInString(props, line.substring(index + 1), failIfMissing);
            ProjectXMLHelper.setProperty(props, propName.toString(), parsedValue);
        }
    } else {
        final String parsedValue = Util.parsePropertiesInString(props, value, failIfMissing);
        ProjectXMLHelper.setProperty(props, name, parsedValue);
    }
  }

}
