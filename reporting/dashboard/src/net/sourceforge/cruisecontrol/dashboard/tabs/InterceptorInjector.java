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
package net.sourceforge.cruisecontrol.dashboard.tabs;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;

public class InterceptorInjector {
    private HandlerInterceptor[] interceptorsOfFramework = new HandlerInterceptor[0];

    public HandlerExecutionChain mergeInterceptorsToTabs(ProceedingJoinPoint pjp) throws Throwable {
        HandlerExecutionChain handlerExecutionChain = (HandlerExecutionChain) pjp.proceed();
        if (handlerExecutionChain == null) {
            return null;
        }
        return new HandlerExecutionChain(handlerExecutionChain.getHandler(),
                mergeInterceptors(handlerExecutionChain));
    }

    private HandlerInterceptor[] mergeInterceptors(HandlerExecutionChain handlerExecutionChain) {
        HandlerInterceptor[] tabInterceptors = handlerExecutionChain.getInterceptors();
        if (tabInterceptors == null) {
            return interceptorsOfFramework;
        }
        HandlerInterceptor[] result =
                new HandlerInterceptor[interceptorsOfFramework.length + tabInterceptors.length];
        System.arraycopy(interceptorsOfFramework, 0, result, 0, interceptorsOfFramework.length);
        System.arraycopy(tabInterceptors, 0, result, interceptorsOfFramework.length, tabInterceptors.length);
        return result;
    }

    public void setInterceptors(HandlerInterceptor[] interceptorsOfFramework) {
        this.interceptorsOfFramework = interceptorsOfFramework;
    }
}
