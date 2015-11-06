/********************************************************************************
 *
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
 *
 ********************************************************************************/
package net.sourceforge.cruisecontrol.builders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jdom.Element;
import org.junit.Ignore;

import junit.framework.TestCase;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.builders.CMakeBuilder.Option;
import net.sourceforge.cruisecontrol.builders.ExecBuilder;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.IO;


/**
 * Test case for {@link CMakeBuilder}.
 */
public class CMakeBuilderTest extends TestCase
{

    /** The name of the binary built */
    private static final  String binname   = "cmakebuilder_testapp";
    /** The list of files created during the test */
    private final  FilesToDelete files2del = new FilesToDelete();
    
    
    /**
     * Setup test environment.
     */
    @Override
    protected void setUp() throws Exception {
    	File cmakelist;
    	File main_c;
    	
        /* Create the content of 'main.c' */
        main_c = File.createTempFile("main_CMakeBuilderTest", ".c");
        main_c.deleteOnExit();
        IO.write(main_c,    "#include <stdio.h>\n"
                          + "\n"
                          + "#ifdef  ALLOW_INVALID_CODE\n"
                          + "        This code is invalid and must cause compile error!\n"
                          + "#endif\n"
                          + "\n"
                          + "#ifndef TEXT_TO_PRINT\n"
                          + "#define TEXT_TO_PRINT \"Default text to print. To change it, define TEXT_TO_PRINT macro\"\n"
                          + "#endif\n"
                          + "\n"
                          + "int main() {\n"
                          + "    printf(\"%s\\n\", TEXT_TO_PRINT);\n"
                          + "    return 0;\n"
                          + "}\n");
        cmakelist = new File(main_c.getParent(), "CMakeLists.txt"); // Must not be random name!
        cmakelist.deleteOnExit();
        IO.write(cmakelist, "# The name of project\n"
                          + "project(CMakeBuilderTestProject)\n"
                          + "\n"
                          + "# Minimum required CMake version\n"
                          + "cmake_minimum_required(VERSION 2.6)\n"
                          + "\n"        
                          + "# Optional binary name set by -DBINARY_NAME=name\n"
                          + "# If not set, binary is named according to the project name\n"
                          + "if    (NOT DEFINED BINARY_NAME)\n"
                          + "       set(BINARY_NAME CMakeBuilderTestProject)\n"
                          + "endif (NOT DEFINED BINARY_NAME)\n"
                          + "\n"
                          + "# This option enables invalid code which caused build failure\n"
                          + "if    (ALLOW_INVALID_CODE)\n"
                          + "       add_definitions(-DALLOW_INVALID_CODE)\n"
                          + "endif (ALLOW_INVALID_CODE)\n"
                          + "\n"
                          + "# Text to print, if defined\n"
                          + "if    (TEXT_TO_PRINT)\n"
                          + "       add_definitions(-DTEXT_TO_PRINT=${TEXT_TO_PRINT})\n"
                          + "endif (TEXT_TO_PRINT)\n"
                          + "\n"
                          + "# Create the executable from given sources\n"
                          + "add_executable(${BINARY_NAME} " + main_c.getName() + ")\n"
                          + "\n"
                          + "# Where to install the application: CMAKE_INSTALL_PREFIX/bin/\n"
                          + "install(TARGETS ${BINARY_NAME} DESTINATION bin/)\n");

    
        /* Create new config mock object with all the required attributes correctly set */
        config = new ConfigMock();
        config.srcroot = cmakelist.getParent();
        config.builddir = new File(config.srcroot, "build").getAbsolutePath();
        config.addOption("-D BINARY_NAME=" + binname);
        config.addOption("-D CMAKE_VERBOSE_MAKEFILE=ON");
    
        /* Set the files to delete */
        files2del.add(cmakelist);
        files2del.add(main_c);
        files2del.add(new File(config.builddir));
    }
    /**
     * Clears test environment.
     */
    @Override
    protected void tearDown()
    {
        files2del.delete();
    }

    /**
     * Tests a validate failure - cases when srcroot attribute is not set
     */
    public void testValidate_noSrcRoot()
    {
        CMakeBuilder builder;

        try {
            /* Set invalid srcroot */
            config.srcroot = new File(config.srcroot).getParent();

            /* Create builder object and validate it */
            builder = config.builderFactory();
            builder.validate();
            /* Test failed, exception was expected! */
            fail("builder was validated even when srcroot was not set");
        }
        catch (CruiseControlException exc) {
            /* correct */
            assertTrue(exc.getMessage().contains("srcroot"));
        }
    }

    /**
     * Tests a validate failure - cases when srcroot attribute is not set
     */
    public void testValidate_invalidSrcRoot()
    {
        CMakeBuilder builder;

        try {
            /* Set all except srcroot */
            config.srcroot = null;

            /* Create builder object and validate it */
            builder = config.builderFactory();
            builder.validate();
            /* Test failed, exception was expected! */
            fail("builder was validated even when srcroot was not set");
        }
        catch (CruiseControlException exc) {
            /* correct */
            assertTrue(exc.getMessage().contains("srcroot"));
        }
    }

    /**
     * Tests a validate failure - cases when builddir attribute is not set
     */
    public void testValidate_noBuildDir()
    {
        CMakeBuilder builder;

        try {
            /* Set all except builddir */
            config.builddir = null;

            /* Create builder object and validate it */
            builder = config.builderFactory();
            builder.validate();
            /* Test failed, exception was expected! */
            fail("builder was validated even when buildpath was not set");
        }
        catch (CruiseControlException tExc) {
            /* correct */
        }
    }

    /**
     * Tests correct build.
     * @throws CruiseControlException if failed
     */
    @Ignore("Needs CMake package installed")
    public void testBuild_buildSuccess() throws CruiseControlException
    {
        CMakeBuilder builder;
        Element      buildLog;

        config.addBuild("make", "");

        /* Build the test project */
        builder = config.builderFactory();
        builder.validate();
        buildLog = builder.build(new HashMap<String, String>(), null);

        /* error attribute must not be set and the binary file must exist */
        assertNull(buildLog.getAttributeValue("error"));
        assertTrue(new File(config.builddir, binname).exists());

        /* Try to execute the command */
        testBinaryCall(config.builddir, binname);
    }

    /**
     * Tests is values in CMake defines are correctly quoted when contain white characters.
     * @throws CruiseControlException if failed
     */
    @Ignore("Needs CMake package installed")
    public void testBuild_defineQuoting() throws CruiseControlException
    {
        CMakeBuilder builder;
        Element      buildLog;
        String       text2print = "This text is supposed to be printed by the compiled binary";

        /* Add special define with white characters and set distdir (result will be executed) */
        config.addOption("-D TEXT_TO_PRINT='\"" + text2print + "\"'");
        config.addBuild("make", "");

        /* Build the test project */
        builder = config.builderFactory();
        builder.validate();
        buildLog = builder.build(new HashMap<String, String>(), null);

        /* error attribute must not be set and the binary file must exist */
        assertNull(buildLog.getAttributeValue("error"));
        assertTrue(new File(config.builddir, binname).exists());

        /* Try to execute the command */
        assertEquals(text2print, testBinaryCall(config.builddir, binname));
    }

    /**
     * Tests a build failure - enable invalid code in the build .cpp file
     * @throws CruiseControlException if failed
     */
    @Ignore("Needs CMake package installed")
    public void testBuild_invalidMake() throws CruiseControlException
    {
        CMakeBuilder builder;
        Element      buildLog;

        /* Add option which allows the build of invalid code */
        config.addOption("-D ALLOW_INVALID_CODE:BOOL=ON");
        config.addBuild("make", "");

        /* Build the test project */
        builder = config.builderFactory();
        builder.validate();
        buildLog = builder.build(new HashMap<String, String>(), null);

        /* error attribute must be set and the binary file must not exist */
        assertNotNull(buildLog.getAttributeValue("error"));
        assertFalse(new File(config.builddir, binname).exists());
    }

    /** @return the instance of {@link CMakeBuilder} */
    protected CMakeBuilder createCMakeBuilder() {
        return new CMakeBuilder();
    }

    /**
     * Tests the binary built - it execs the binary and checks if it ends with retcode 0. The
     * output of the binary is read and returned by the method.
     *
     * @param  workingDir the directory where the binary is placed
     * @param  binaryName the name of binary to start
     * @return everything print on stdout
     */
    private static String testBinaryCall(String workingDir, String binaryName)
    {
        Commandline cmdline = new Commandline();

        try {
            BufferedReader stdoutReader;
            StringBuffer   stdoutData;
            String         line;
            Process        process;

            /* Prepare command to run */
            cmdline.setWorkingDir(new File(workingDir));
            cmdline.setExecutable(new File(workingDir, binaryName).getCanonicalPath());
            /* Run the command */
            process = cmdline.execute();
            process.waitFor();
            /* Must end with 0 retcode */
            assertEquals(0, process.exitValue());

            /* Read the output of the command */
            stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            stdoutData   = new StringBuffer();
            /* Read and compare line by line */
            while ((line = stdoutReader.readLine()) != null)
                stdoutData.append(line);

            /* And return it */
            return stdoutData.toString();

        } catch (Exception exc) {
            exc.printStackTrace();
            fail("Exception when trying to execute built binary " + cmdline.toString());
            /* just for correct compiling */
            return "";
        }
    }


    /** The configuration object; filled in {@link #setUp()} method in such a way that all
     *  required attributes are correctly set */
    private ConfigMock  config;

    /**
     * Mock XML configuration element. It contains the same attributes as allowed by <cmake />
     * config element for {@link CMakeBuilder} object. The not-<code>null</code> attributes
     * are expected to be filled. Note that it does not contain the attributes common to all
     * builders!
     *
     * Isn't something like this (more generic, of course) already available through
     * cruisecontrol test package?
     */
    private class ConfigMock
    {
        /** <cmake srcroot="" ... /> attribute */
        String         srcroot  = null;
        /** <cmake builddir="" ... /> attribute */
        String 	       builddir = null;
        /** <cmake timeout="" ... /> attribute */
        String 		   timeout  = null;

        /** The list of child <option name="" value="" /> elements */
        List<String>   options  = new ArrayList<String>();
        /** The list of child <build exec="" args="" ... /> elements */
        List<String[]> builds   = new ArrayList<String[]>();

        /** Adds <option /> child element */
        void addOption(String option) {
            options.add(option);
        }
        /** Adds <build /> child element */
        void addBuild(String exec, String args) {
            builds.add(new String[]{ exec, args});
        }

        /** Creates {@link CMakeBuilder} according to the attributes set (it is like instantiating
         *  the object from <cmake /> XML config element). */
        CMakeBuilder builderFactory()
        {
            CMakeBuilder builder = createCMakeBuilder();

            /* Set individual attributes */
            if (srcroot != null)
                builder.setSrcRoot(srcroot);
            if (builddir != null)
                builder.setBuildDir(builddir);
            if (timeout != null)
                builder.setTimeout(Long.parseLong(timeout));

            /* Child configuration elements */
            for (String o : options) {
                 Option O = (Option) builder.createOption();
                 /* Set filled items */
                 if (!"".equals(o))
                     O.setValue(o);
            }
            /* Child build elements */
            for (String[] b : builds) {
                 ExecBuilder B = builder.createBuild();
                 /* Set filled items */
                 B.setCommand(b[0]);
                 B.setArgs(b[1]);
            }

            /* Return the filled object */
            return builder;
        }

    }

}
