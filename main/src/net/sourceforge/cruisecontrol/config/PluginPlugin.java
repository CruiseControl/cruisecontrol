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

import org.jdom.Element;

/**
 * <p>A <code>&lt;plugin&gt;</code> element registers a classname with an alias for use within the
 * configuration file.</p>
 * <p>Plugins can also be <a href=\"plugins.html#preconfiguration\">pre-configured</a> at registration time.
 * This can greatly reduce the configuration file size.</p>
 * <p>The <a href=\"plugins.html\">plugins</a> page contains a discussion of the plugin architecture
 * used with CruiseControl.</p>
 */
public class PluginPlugin {
  private String name;
  private String classname;
  private Element transformedElement;

  /**
   * The alias used to refer to the plugin elsewhere in the configuration file.
   * @param name the plugin name
   * @required
   */
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * The class that implements the plugin.
   * @param className the plugin class name
   * @required
   */
  public void setClassname(final String className) {
    this.classname = className;
  }

  public String getName() {
    return name;
  }

  public String getClassname() {
    return classname;
  }

  public void configure(final Element element) {
    setName(element.getAttributeValue("name").toLowerCase());
    setClassname(element.getAttributeValue("classname"));

    final Element clonedPluginElement = (Element) element.clone();
    clonedPluginElement.removeAttribute("name");
    clonedPluginElement.removeAttribute("classname");
    clonedPluginElement.setName(this.name);
    this.transformedElement = clonedPluginElement;
  }

  public Element getTransformedElement() {
    return transformedElement;
  }
}
