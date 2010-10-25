/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.gendoc.testplugins;

import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.gendoc.annotations.Cardinality;
import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.DescriptionFile;
import net.sourceforge.cruisecontrol.gendoc.annotations.ManualChildName;
import net.sourceforge.cruisecontrol.gendoc.annotations.Optional;
import net.sourceforge.cruisecontrol.gendoc.annotations.Required;
import net.sourceforge.cruisecontrol.gendoc.annotations.SkipDoc;
import net.sourceforge.cruisecontrol.gendoc.annotations.Title;

@Description("A") @Title("B")
public interface GoodRoot {
    
    @Cardinality(min = 1, max = 15) @Description("C") @Title("D")
    public GoodChild createChild();
    
    @ManualChildName("goodchild2")
    public GoodChild createChild2();
    
    @Cardinality(min = 2, max = -1, note="E") @ManualChildName("goodchild3")
    public GoodChild createChild3();
    
    @DescriptionFile
    public RecursiveChild createRecursiveChild();
    
    @DescriptionFile("/net/sourceforge/cruisecontrol/gendoc/testplugins/TestDoc.html")
    public void addSourceControl(SourceControl s);
    
    // Put in attributes to test possible annotation combinations.
    
    @Cardinality(min = 0, max = 1) @Description("F") @Title("G") @Default("H")
    public void set1(String a);
    
    @Cardinality(min = 1, max = 1, note="I")
    public void set2(String b);
    
    @Required
    public void set3(String c);
    
    @Optional
    public void set4(String d);
    
    @Required("J")
    public void set5(String e);
    
    @Optional("K")
    public void set6(String f);
    
    public void set7(String g);
    
    @SkipDoc
    public void setSomethingElse(Object obj);

}
