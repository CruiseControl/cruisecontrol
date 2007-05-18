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
package net.sourceforge.cruisecontrol.dashboard.web.validator;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.web.command.ConfigurationCommand;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

public class ConfigXmlNameValidatorTest extends TestCase {
    private ConfigXmlNameValidator validator;

    private static final String CONFIG_FILE_LOCATION_FIELD_NAME = "configFileLocation";

    private ConfigurationCommand configurationCommand;

    private Errors errors;

    public void setUp() throws Exception {
        validator = new ConfigXmlNameValidator();
        configurationCommand = new ConfigurationCommand();

        configurationCommand.setConfigFileLocation(DataUtils.createDefaultCCConfigFile().getPath());
        errors = new BindException(configurationCommand, "target");
    }

    public void testConfigFileShouldNotBeBlank() {
        configurationCommand.setConfigFileLocation("  ");
        validator.validate(configurationCommand, errors);

        assertEquals(1, errors.getErrorCount());
        FieldError configFileError = errors.getFieldError(CONFIG_FILE_LOCATION_FIELD_NAME);
        assertEquals("configFileLocation.format.error", configFileError.getCode());
        assertEquals("The configuration file path cannot be blank.", configFileError
                .getDefaultMessage());
    }

    public void testConfigFileShouldNotSupportExtensionOtherThanXml() {
        configurationCommand.setConfigFileLocation("file.txt");
        validator.validate(configurationCommand, errors);
        assertEquals(1, errors.getErrorCount());
        FieldError configFileError = errors.getFieldError(CONFIG_FILE_LOCATION_FIELD_NAME);
        assertEquals("configFileLocation.format.error", configFileError.getCode());
        assertEquals("The configuration file path must end with '.xml'.", configFileError
                .getDefaultMessage());
    }
}
