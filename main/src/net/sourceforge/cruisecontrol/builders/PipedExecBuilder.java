/********************************************************************************
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.gendoc.annotations.Cardinality;
import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.DescriptionFile;
import net.sourceforge.cruisecontrol.gendoc.annotations.ExamplesFile;
import net.sourceforge.cruisecontrol.gendoc.annotations.ManualChildName;
import net.sourceforge.cruisecontrol.gendoc.annotations.Required;
import net.sourceforge.cruisecontrol.gendoc.annotations.SkipDoc;
import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;


/**
 * Piped exec builder class.
 *
 * Executes a set of command line scripts where one can be piped from another as well as one may
 * wait until another finishes. It determines whether each of the scripts was successful or not.
 * Each script is configured independently in the same way as scripts run by {@link ExecBuilder},
 * with the extension of piping and waiting facility configured through objects returned by
 * {@link #createExec()} method.
 *
 * Individual scripts in the builder are started simultaneously whenever possible.
 *
 * Configuration example for this plugin:
 * <pre>
 * {@code
 *         <cruisecontrol>
 *         <schedule>
 *          <pipedexec workingdir="${workingdir.default}" timeout="3000"/>
 *               <exec id="1" command="exec1" args="-a1 -a2"     timeout="10"/>
 *               <exec id="2" command="exec2" args="-a1"         pipefrom="1"/>
 *               <exec id="3" command="exec3" args="-a1 -a2 -a3" pipefrom="2" workingdir="${workingdir.special}"/>
 *               <exec id="4" command="exec3" args="-a1 -a2"     pipefrom="1" waitfor="2"/>
 *               <exec id="5" command="exec4" args="-a1  data"   pipefrom="4"/>
 *           </piped_exec>
 *           </schedule>
 *     <cruisecontrol>
 * }
 * </pre>
 *
 * @author <a href="mailto:dtihelka@kky.zcu.cz">Dan Tihelka</a>
 */
@DescriptionFile
@ExamplesFile
public class PipedExecBuilder extends Builder implements PipedScript.EnvGlue {

    /** Serialization UID */
    private static final long serialVersionUID = -6632406315466647230L;

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(PipedExecBuilder.class);

    /** Build timeout in seconds, set by {@link #setTimeout(long)}. */
    private long timeout = ScriptRunner.NO_TIMEOUT;
    /** The working directory where the commands are to be executed, set by
     * {@link #setWorkingDir(String)}. */
    private String workingDir;
    /** The list of scripts to execute during build. Once the script is started, it is moved
     *  to the list of started scripts. */
    private final LinkedList<PipedScript> scripts = new LinkedList<PipedScript>();
    /** The list of scripts to execute during build. Once the script is started, it is moved
     *  to the list of started scripts. */
    private final LinkedList<Special> specials = new LinkedList<Special>();

    /**
     * Validate the attributes for the plugin.
     */
    @Override
    public void validate() throws CruiseControlException {
        super.validate();

        /* Auxiliary ID holder */
        Set<String> auxIDs = new HashSet<String>(scripts.size());

        /*
         * Resolve specials
         * Process the repiped. Must be processed prior to disables, since a repiped script may originally
         * be in a pipe which is going to be disabled  */
        for (Iterator<PipedScript> siter = scripts.iterator(); siter.hasNext(); ) {
             final PipedScript s = siter.next();

             for (Iterator<Special> xiter = specials.iterator(); xiter.hasNext(); ) {
                  final Special x = xiter.next();
                  /* Validate the specials */
                  x.validate();
                  /* ID matching */
                  if (s.getID().equals(x.getID()) && x.repipe()) {
                      s.setPipeFrom(x.getPipeFrom());
                      s.setWaitFor(x.newWaitFor(s.getWaitFor()));
                      xiter.remove();
                      break;
                  }
             }
        }
        /* Process the disabled now */
        for (Iterator<PipedScript> siter = scripts.iterator(); siter.hasNext(); ) {
             final PipedScript s = siter.next();

             for (Iterator<Special> xiter = specials.iterator(); xiter.hasNext(); ) {
                  final Special x = xiter.next();
                  /* ID matching */
                  if (s.getID().equals(x.getID()) && x.disable()) {
                      final Collection<PipedScript> todel = findPipedSeq(s.getID(), scripts);
                      todel.add(s);
                      /* Remove the command from the sequence and all the commands which are piped from it,
                       * since they would not be started ... */
                      scripts.removeAll(todel);
                      /* Remove waitFor from all the scripts affected */
                      for (PipedScript p : scripts) {
                           auxIDs.addAll(Arrays.asList(p.getWaitFor()));
                           for (PipedScript d : todel) {
                               auxIDs.remove(d.getID()); // Removes if contained
                           }
                           /* Changed */
                           if (auxIDs.size() != p.getWaitFor().length) {
                               p.setWaitFor(PipedScript.Helpers.join(auxIDs));
                           }
                           auxIDs.clear();
                      }
                      /* Re-assign the iterator (checkstyle suppresses in main/checkstyleSuppressions.xml) */
                      siter = scripts.iterator();
                      xiter.remove();
                      break;
                  }
             }
        }
        specials.clear();
        auxIDs.clear();

        /*
         * Check the (remaining) scripts for basic setting
         */
        for (PipedScript s : scripts) {
            /* ID must be unique */
            ValidationHelper.assertFalse(auxIDs.contains(s.getID()), "ID " + s.getID() + " is not unique");
            auxIDs.add(s.getID());

            /* Pass config variables to the exec script, if it does not have set them. Must be done
               before s.validate(), since it sets the variables to a default value */
            if (s.getWorkingDir() == null) {
                s.setWorkingDir(workingDir);
            }
            /* Let it validate itself */
            s.validate();

            /* Cannot be piped or wait for itself */
            ValidationHelper.assertIsSet(s.getID(), "ID", s.getClass());
            ValidationHelper.assertFalse(inList(s.getPipeFrom(), s.getID()),
                    "Script " + s.getID() + " cannot pipe from itself");
            ValidationHelper.assertFalse(inList(s.getWaitFor(), s.getID()),
                    "Script " + s.getID() + " cannot wait for itself");
            /* If the script is piped from for another script, the "another: must exist */
            for (String p : s.getPipeFrom()) {
                ValidationHelper.assertTrue(findScript(p, scripts) != null,
                        "Script " + s.getID() + " is piped from non-existing script " + p);
            }
            for (String w : s.getWaitFor()) {
                ValidationHelper.assertTrue(findScript(w, scripts) != null,
                        "Script " + s.getID() + " waits for non-existing script " + w);
            }

            /* Set the environment glue */
            s.setEnvGlue(this);
        }
        auxIDs.clear();

        /*
         * Loops detection
         */
        for (PipedScript s : scripts) {
            auxIDs = checkLoop(s, auxIDs, new HashSet<String>());
        }
    }

    /**
     * Execute the commands and return the results as XML
     */
    @Override
    public Element build(final Map<String, String> buildProperties, final Progress progressIn)
        throws CruiseControlException {

        final ThreadPool threads = new ThreadPool();
        final long startTime = System.currentTimeMillis();
        final LinkedList<PipedScript> tostart = new LinkedList<PipedScript>(scripts);
        final LinkedList<PipedScript> started = new LinkedList<PipedScript>();


        final Element buildLogElement = new Element("build");

        /* Go through the list of scripts until all were started and finished (tostart contains
         * those not started yet, started those running or not finished yet) */
        while (tostart.size() > 0 || started.size() > 0) {
            ListIterator<PipedScript> iter = tostart.listIterator();

            /* Go through all scripts to start and look for those which can be started now */
            while (iter.hasNext()) {
                PipedScript s = iter.next();
                boolean canStart = true;

                /* Script can start if:
                 * - it is not piped from another script
                 * - it is piped from another script and the script was started
                 * - it waits for another script and the script is finished
                 */
                for (final PipedScript p : tostart) {
                    if (!canStart) {
                        break;
                    }
                    if (inList(s.getPipeFrom(), p.getID()) || inList(s.getWaitFor(), p.getID())) {
                        canStart = false;
                    }
                }
                for (final PipedScript p : started) {
                    if (!canStart) {
                        break;
                    }
                    if (inList(s.getWaitFor(), p.getID()) && !p.isDone()) {
                        canStart = false;
                    }
                }
                /* If cannot be started, try another one */
                if (!canStart) {
                    continue;
                }

                long remainTime = this.timeout != ScriptRunner.NO_TIMEOUT
                                               ?  this.timeout - (System.currentTimeMillis() - startTime) / 1000
                                               :  Long.MAX_VALUE;
                if (s.getTimeout() == ScriptRunner.NO_TIMEOUT || s.getTimeout() > remainTime) {
                    s.setTimeout(remainTime);
                }
                /* And stuff for #build() method */
                s.setBuildLogParent(buildLogElement);
                s.setBuildProperties(buildProperties);
                s.setProgress(progressIn);
                /* Pipe to the required script */
                for (String p : s.getPipeFrom()) {
                    s.setInputProvider(findScript(p, started).getOutputReader(), p);
                }

                /* Initialize the script */
                s.initialize();
                /* Now start the script and set its thread to the pool */
                threads.startThread(s, s.getID());

                /* And move it from tostart array into started array. Reset the iterator, which
                 * allows to run all scripts except those waiting for others */
                iter.remove();
                iter = tostart.listIterator();
                started.add(s);

                // !!!!!!
                // WINDOWS SPECIFIC HACK:
                // Under Windows (tested on Windows XP with SP3, but suppose that it affects all
                // lower versions as well) we have found problems when several commands are started
                // simultaneously - although the process terminates successfully (finish reports 0
                // status), the reading from STDOUT/STDERR of a process blocks forever ... It is later
                // caught by timeout killer, but the whole pipe does not finish correctly.
                // If this does not occur on Windows Vista (and higher], check just for Windows XP
                // and lower can be added.
                // Preventing very fast concurrent process spawning seems to fix it (tests are OK). But
                // if you still find such problem, use threads.join(). It will lead to horrible
                // performance of the pipe under the affected windows versions, but it should be safe.
                if (Util.isWindows()) {
                    threads.join(1000);
                }
            }

            /* All scripts which could be started up to now were started ...
             * Try to join some scripts */
            threads.join(1000);

            /* And check if some scripts were finished */
            iter = started.listIterator();
            while (iter.hasNext()) {
                PipedScript s = iter.next();

                /* Remove the script from 'started' map when finished and not required by any
                 * other script not started yet */
                if (s.isDone() && findPipedFrom(s.getID(), tostart) == null) {
                    s.finish(); // mark as finished (will not be used anymore) to save memory
                    iter.remove();
                }
            }

            /* Sanity check - if running time > timeout, leave the loop with error message */
            if (System.currentTimeMillis() - startTime > this.timeout * 1000) {
                LOG.warn("Build timeout timer of " + timeout + " seconds has expired");
                synchronized (buildLogElement) {
                    buildLogElement.setAttribute("error", "build timeout");
                }
                break;
            }
        }

        /* Wait for all scripts to finish (they may be killed by their own timeouts) */
        threads.join();
        for (PipedScript s : scripts) {
             s.finish(); // Mark as finished
        }

        /* Set the time it took to exec command */
        buildLogElement.setAttribute("time", DateUtil.getDurationAsString((System.currentTimeMillis() - startTime)));
        /* Go through children (individual commands), and check if there is an "error" attribute
         * in them. Copy it if so */
        for (Object e : buildLogElement.getChildren()) {
             Attribute a = ((Element) e).getAttribute("error");
             if (a != null) {
                 buildLogElement.setAttribute(a.detach());
                 break;
             }
        }

        //note: what other attributes/information should be stored in the element?
        //      ExecScript.setBuildLogHeader()????
        return buildLogElement;
    } // build

    /**
     * Execute the commands and return the results as XML
     */
    @Override
    public Element buildWithTarget(final Map<String, String> properties,
            final String target, final Progress progress)
            throws CruiseControlException {

        // final String origArgs = args;
        // try {
        // args = target;
        return build(properties, progress);
        // } finally {
        // args = origArgs;
        // }
    }

    /**
     * Sets the working directory where all the scripts are to be executed. Can be overridden
     * by the configuration of individual scripts.
     *
     * @param dir the directory where the command is to be executed
     */
    @Description("The directory in which all the scripts or commands are going to be executed. Each "
            + " individual script or command may override this value.")
    @Default("CC working directory")
    public void setWorkingDir(String dir) {
        this.workingDir = dir;
    } // setWorkingDir

    /**
     * Sets the working directory where all the scripts are to be executed. Can be overridden
     * by the configuration of individual scripts, but only by lower value.
     *
     * @param timeout build timeout in seconds
     */
    @Description("Build will be halted if the scripts or commands running under the pipedexec continue "
            + "longer than the specified timeout. Each individual script or command may also set its own "
            + "limitation. Value in seconds.")
    @Default("no timeout")
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    } // setWorkingDir

    /**
     * Creates object into which <code>{@code <exec />}</code> tag will be set. Each call returns new
     * object which is expected to be set by CC. The attribute is not required; if not
     * specified, nothing will be executed.
     *
     * @return new {@link PipedExecScript} object to configure.
     */
    @Cardinality(min = 0, max = -1)
    @ManualChildName("exec")
    @Description("Defines a script to be executed within its parent builder, pipsed from/to another script(s) "
            + "within the same builder.")
    public PipedExecScript createExec() {
        final PipedExecScript exec = new PipedExecScript();
        scripts.add(exec);
        return  exec;
    } // createExec

    @Cardinality(min = 0, max = -1)
    @ManualChildName("repipe")
    @Description("Changes the input of an already defined plugin, repiping it from the <i>id</i>s defined here.")
    @SuppressWarnings("javadoc")
    public Repipe createRepipe() {
        final Repipe obj = new Repipe();
        specials.add(obj);
        return obj;
    }
    @Cardinality(min = 0, max = -1)
    @ManualChildName("disable")
    @Description("Disables an already defined script.")
    @SuppressWarnings("javadoc")
    public Disable createDisable() {
        final Disable obj = new Disable();
        specials.add(obj);
        return obj;
    }

    /**
     * Adds object into the builder. It is similar to {@link #createExec()}, but allows to add any
     * 3rd party plugin implementing the {@link PipedScript} interface.
     *
     * @param execobj the implementation of {@link PipedScript} interface.
     */
    @SkipDoc // TODO: should be documented???
    public void add(PipedScript execobj) {
        scripts.add(execobj);
    } // add

    /**
     * Finds script with the given ID in the given array of scripts.
     *
     * @param id the ID of the script to look for.
     * @param tostart the list of scripts to be searched.
     * @return the instance of {@link Script} or <code>null</code> if not found.
     */
    private static PipedScript findScript(String id, Collection<PipedScript> tostart) {
        for (PipedScript s : tostart) {
            if (id != null && id.equals(s.getID())) {
                return s;
            }
        }
        return null;
    }
    /**
     * Checks. if there is a script in {@link #scripts} array which is required to be piped
     * from script with given ID. If there are more scripts piped from the same ID, it is not determined
     * which one of them is get.
     *
     * @param id the ID of the script to look for.
     * @param scripts the collection of scripts to be searched.
     * @return the instance of {@link PipedScript} or <code>null</code> if not found.
     */
    private static PipedScript findPipedFrom(String id, Collection<PipedScript> scripts) {
        for (PipedScript s : scripts) {
             if (inList(s.getPipeFrom(), id)) {
                return s;
             }
        }
        return null;
    }

    /**
     * Method used for the detection of loops in piped commands. It works with two sets. First,
     * the set of script IDs which are not in loop (they lead to a script not piped or not
     * waiting for another script). The second is the set of scripts under check, but it cannot
     * be determined yet, if they are in loop.
     *
     * The algorithm is as follows:
     * <ul>
     *    <li> if the script is already in 'not-in-loop' set, return immediately
     *    <li> if the script is not piped from another script, neither it is waiting for another
     *         script, put it into 'not-in-loop' set and return
     *    <li> if the script is piped from another script, or it is waiting for another script,
     *         put it into 'checking' set and check recursively the scripts which this depends
     *         on
     *    <li> if the script is found in 'checking' set, loop is detected (it is checked twice
     *         during the recursive calls)
     *    <li> if none of the predecessors is in loop (recursion is left), move the script from
     *         'checking' to 'not-in-loop' set and return.
     * </ul>
     *
     * @param s the script to check
     * @param notInLoop the 'not-in-loop' set
     * @param checking the 'checking' set
     * @return 'not-in-loop' set with the ID of current script added when it is not in a loop
     * @throws CruiseControlException if loop is detected.
     */
    private Set<String> checkLoop(final PipedScript s, Set<String> notInLoop, Set<String> checking)
            throws CruiseControlException {
        /* No script */
        if (s == null) {
            return notInLoop;
        }

        final String id = s.getID();
        /* Already determined not in loop */
        if (notInLoop.contains(id)) {
            return notInLoop;
        }

        /* If piped, check recursively the piped sequence */
        for (final String p : s.getPipeFrom()) {
            /* If the predecessor is in checking set, loop is detected! */
            if (checking.contains(p)) {
                throw new CruiseControlException("Loop detected, ID " + id + " is within loop");
            }
            /* Cannot detect loop now, check the predecessor */
            checking.add(id);
            notInLoop = checkLoop(findScript(p, scripts), notInLoop, checking);
        }
        /* If waiting, check recursively as well */
        for (final String w : s.getWaitFor()) {
            /* Predecessor in checking set, loop detected! */
            if (checking.contains(w)) {
                throw new CruiseControlException("Loop detected, ID " + id + " is within loop");
            }
            /* Cannot detect loop now, ... */
            checking.add(id);
            notInLoop = checkLoop(findScript(w, scripts), notInLoop, checking);
        }

        /* Exception was not thrown, not in loop */
        checking.remove(id);
        notInLoop.add(id);
        return notInLoop;
    } // checkLoop

    /**
     * Finds all scripts piped from the given script
     *
     * @param id the ID of the script to look for.
     * @param scripts the list of scripts to be looked it.
     * @return collection of scripts piped to the given script
     */
    private static Collection<PipedScript> findPipedSeq(String id, Collection<PipedScript> scripts) {
        Collection<PipedScript> piped = new HashSet<PipedScript>(10);
        PipedScript found;

        /* Copy the collection, since data will be removed from it */
        scripts = new HashSet<PipedScript>(scripts);

        while ((found = findPipedFrom(id, scripts)) != null) {
                scripts.remove(found);
                /* Add the piped and find these piped to it */
                piped.add(found);
                piped.addAll(findPipedSeq(found.getID(), scripts));
        }
        /* Get the sequence */
        return piped;
    } // findPipedSeq

    /**
     * Checks, if the given string item is in the list. The method is primarily used to check ID in
     * {@link PipedScript#getPipeFrom()} and {@link PipedScript#getWaitFor()}.
     *
     * @param list the list to check (does not have to be sorted)
     * @param item the string to search
     * @return <code>true</code> if found, <code>false</code> otherise
     */
    private static boolean inList(String[] list, String item) {
        for (String s : list) {
            if (item.equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the list of IDs of the scripts known (either registered when called prior to {@link #validate()},
     * or ready to be executed when called after). The method is just for testing purposes.
     */
    public Collection<String> getKnownIDs() {
        final Collection<String> ids = new HashSet<String>(scripts.size());
        for (PipedScript s : scripts) {
            ids.add(s.getID());
        }
        return ids;
    }


    /* ----------- NESTED CLASSES ----------- */

    /**
     * Special object used to mark scripts as repiped or disabled.
     */
    private abstract class Special {

        /** Value set by {@link #setID(String)} */
        private String id = null;

        /** @return <code>true</code> when repipe is required, <code>false</code> otherwise */
        abstract boolean repipe();
        /** @return <code>true</code> when disable is required, <code>false</code> otherwise */
        abstract boolean disable();

        /** Checks, if script ID was set by {@link #setID(String)}. As the <i>id</i> is required,
         *  do not forget to call this method when overriding! */
        public void validate() throws CruiseControlException {
            ValidationHelper.assertIsSet(id, "id", getClass());
        }

        /** @return the ID of the script to be piped from, when {@link #repipe()} gets <code>true</code>;
         *      <code>null</code> otherwise */
        abstract String getPipeFrom();
        /** @return the ID of the script to wait for, when {@link #repipe()} gets <code>true</code> and
         *      wait change was required; <code>null</code> otherwise */
        abstract String getWaitFor();

        @Description("The <i>id</i> of already existing script to change the configuration for.")
        @Required
        @SuppressWarnings("javadoc")
        public void setID(String value) {
            id = value;
        }
        /** @return the value set by {@link #setID(String)} */
        public String getID() {
            return id;
        }
        /** It gets the new ID to wait for as a merge of the value get by {@link #getWaitFor()} and the
         *  value passed as the option. The rules are as follow:
         *  - if {@link #getWaitFor()} returns <code>null</code>, orig is returned (no change of waiting)
         *  - if {@link #getWaitFor()} returns "", empty list is returned (do not wait)
         *  - otherwise, the list of values of {@link #getWaitFor()} is get
         *
         *  @param orig the original ID to wait for
         *  @return the new ID (or comma separated list of IDs) to wait for
         */
        public String newWaitFor(String[] orig) {
            final String waitfor = getWaitFor();
            return waitfor == null ? PipedScript.Helpers.join(orig) : waitfor;
        }
    }
    @DescriptionFile("PipedExecBuilder.repipe.html")
    @ExamplesFile("PipedExecBuilder.repipe.examples.html")
    public final class Repipe extends Special {

        /** Value set by {@link #setPipeFrom(String)} */
        private String pipeFrom = null;
        /** Value set by {@link #setWaitFor(String)} */
        private String waitfor = null;

        @Override
        public void validate() throws CruiseControlException {
            super.validate();
            ValidationHelper.assertIsSet(pipeFrom, "pipefrom", getClass());
        }

        @Override
        public boolean repipe() {
            return true;
        }
        @Override
        public boolean disable() {
            return false;
        }

        @Description("Sets the new <i>id</i>s of script to be piped from.")
        @Required
        @SuppressWarnings("javadoc")
        public void setPipeFrom(String value) {
            pipeFrom = value;
        }
        @Override
        public String getPipeFrom() {
            return pipeFrom;
        }
        @Description("Sets the new <i>id</i>s of script to wait for. If not set, the wait is not changed in "
                + "the original script, but if empty string is set, the original waiting is removed.")
        @SuppressWarnings("javadoc")
        public void setWaitFor(String value) {
            waitfor = value;
        }
        @Override
        public String getWaitFor() {
            return waitfor;
        }
    }
    @DescriptionFile("PipedExecBuilder.disable.html")
    @ExamplesFile("PipedExecBuilder.disable.examples.html")
    public final class Disable extends Special {

        @Override
        public boolean repipe() {
            return false;
        }
        @Override
        public boolean disable() {
            return true;
        }

        @Override
        public String getPipeFrom() {
            return null;
        }
        @Override
        public String getWaitFor() {
            return null;
        }
    }

    /**
     * Simple class with pool of started threads. It implements {@link #join()} method waiting
     * for any (or all) threads in the pool.
     */
    private class ThreadPool {

        /** The list of threads in the pool. */
        private final List<Thread> threads = new ArrayList<Thread>();

        /**
         * Creates and <b>starts</b> new thread with the given {@link Runnable} implementation.
         * @param runnable the implementation of {@link Runnable} to start.
         * @param name the name of the thread.
         */
        void startThread(Runnable runnable, String name) {
            final Thread t = new Thread(runnable, name + " build thread");
            t.start();
            threads.add(t);
        }

        /**
         * Waits at least some time for some threads to die.
         * @param millis the number of milliseconds to wait.
         */
        void join(long millis) {
            /* Remove the threads not being alive */
            for (int i = threads.size() - 1; i >= 0; i--) {
                 if (!threads.get(i).isAlive()) {
                     threads.remove(i);
                 }
            }
            /* And try to join the others */
            millis = millis / (threads.size() > 0 ? threads.size() : 1);
            for (Thread t : threads) {
                try {
                    t.join(millis < 10 ? 10 : millis);
                } catch (InterruptedException e) {
                    /* Did not die in the given time ... */
                }
            }
        }
        /**
         * Waits for all threads to die.
         */
        void join() {
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    /* Should not happen */
                }
            }
            threads.clear();
        }
    } // ThreadPool


} // PipedExecBuilder

