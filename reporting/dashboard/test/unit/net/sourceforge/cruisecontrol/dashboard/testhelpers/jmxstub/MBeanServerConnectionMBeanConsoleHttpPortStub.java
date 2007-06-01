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
package net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

public class MBeanServerConnectionMBeanConsoleHttpPortStub implements MBeanServerConnection {

    private static final int DEFAULT_HTTP_PORT = 8000;

    public Object getAttribute(ObjectName arg0, String arg1) {
        return new Integer(DEFAULT_HTTP_PORT);
    }

    public Set queryNames(ObjectName arg0, QueryExp arg1) throws IOException {
        Set result = new HashSet();
        try {
            result.add(new ObjectName("Adapter:name=HttpAdaptor"));
            return result;
        } catch (Exception e) {
            return result;
        }
    }

    public void addNotificationListener(ObjectName name, NotificationListener listener,
            NotificationFilter filter, Object handback) throws InstanceNotFoundException,
            IOException {
        // TODO Auto-generated method stub

    }

    public void addNotificationListener(ObjectName name, ObjectName listener,
            NotificationFilter filter, Object handback) throws InstanceNotFoundException,
            IOException {
        // TODO Auto-generated method stub

    }

    public ObjectInstance createMBean(String className, ObjectName name)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
            MBeanException, NotCompliantMBeanException, IOException {

        return null;
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
            MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {

        return null;
    }

    public ObjectInstance createMBean(String className, ObjectName name, Object[] params,
            String[] signature) throws ReflectionException, InstanceAlreadyExistsException,
            MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {

        return null;
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
            Object[] params, String[] signature) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
            NotCompliantMBeanException, InstanceNotFoundException, IOException {

        return null;
    }

    public AttributeList getAttributes(ObjectName name, String[] attributes)
            throws InstanceNotFoundException, ReflectionException, IOException {

        return null;
    }

    public String getDefaultDomain() throws IOException {

        return null;
    }

    public String[] getDomains() throws IOException {

        return null;
    }

    public Integer getMBeanCount() throws IOException {

        return null;
    }

    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException,
            IntrospectionException, ReflectionException, IOException {

        return null;
    }

    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException,
            IOException {

        return null;
    }

    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
            throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {

        return null;
    }

    public boolean isInstanceOf(ObjectName name, String className)
            throws InstanceNotFoundException, IOException {

        return false;
    }

    public boolean isRegistered(ObjectName name) throws IOException {

        return false;
    }

    public Set queryMBeans(ObjectName name, QueryExp query) throws IOException {

        return null;
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {

    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {

    }

    public void removeNotificationListener(ObjectName name, ObjectName listener,
            NotificationFilter filter, Object handback) throws InstanceNotFoundException,
            ListenerNotFoundException, IOException {

    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener,
            NotificationFilter filter, Object handback) throws InstanceNotFoundException,
            ListenerNotFoundException, IOException {

    }

    public void setAttribute(ObjectName name, Attribute attribute)
            throws InstanceNotFoundException, AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException, ReflectionException, IOException {

    }

    public AttributeList setAttributes(ObjectName name, AttributeList attributes)
            throws InstanceNotFoundException, ReflectionException, IOException {

        return null;
    }

    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException,
            MBeanRegistrationException, IOException {

    }
}
