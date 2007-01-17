/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of Project states following standard typesafe enumeration
 * pattern in Java.
 */
public final class ProjectState implements Serializable {
    private static final Map ALL_STATES = new HashMap();

    public static final ProjectState QUEUED =
            new ProjectState(1, "queued", "in build queue");
    public static final ProjectState IDLE =
            new ProjectState(0, "idle", "idle");
    public static final ProjectState BOOTSTRAPPING =
            new ProjectState(2, "bootstrapping", "bootstrapping");
    public static final ProjectState MODIFICATIONSET =
            new ProjectState(3, "modificationset", "checking for modifications");
    public static final ProjectState BUILDING =
            new ProjectState(4, "building", "now building");
    public static final ProjectState MERGING_LOGS =
            new ProjectState(5, "merging", "merging accumulated log files");
    public static final ProjectState PUBLISHING =
            new ProjectState(6, "publishing", "publishing build results");
    public static final ProjectState PAUSED =
            new ProjectState(7, "paused", "paused");
    public static final ProjectState STOPPED =
            new ProjectState(8, "stopped", "stopped");
    public static final ProjectState WAITING =
            new ProjectState(9, "waiting", "waiting for next time to build");

    private String description;
    private String name;
    private int code;

    private ProjectState (int code, String name, String desc) {
        this.code = code;
        this.name = name;
        this.description = desc;
        ALL_STATES.put(name, this);
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * A <strong>magic</strong> method used by Java Object Serialization. This
     * allows ProjectState to force the deserialization process to use one of
     * the ProjectState enum instances that already exist.
     *
     * @return a replacement object instance
     * @throws ObjectStreamException never actually thrown
     */
    private Object readResolve() throws ObjectStreamException {
        return ALL_STATES.get(name);
    }
}
