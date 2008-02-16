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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.aspectj.lang.ProceedingJoinPoint;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

public class InterceptorInjectorTest extends MockObjectTestCase {

    private static final class HandlerInterceptorSub implements HandlerInterceptor {
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                Exception ex) throws Exception {
        }

        public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                ModelAndView modelAndView) throws Exception {
        }

        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                throws Exception {
            return false;
        }
    }

    public void testShouldMergeInterceptors() throws Throwable {
        HandlerInterceptor interceptorOfFramework = new HandlerInterceptorSub();
        HandlerInterceptor interceptorOfTab = new HandlerInterceptorSub();
        HandlerInterceptor[] interceptorsOfFramework = new HandlerInterceptor[] {interceptorOfFramework};
        HandlerInterceptor[] interceptorsOfTab = new HandlerInterceptor[] {interceptorOfTab};

        Mock proceedingJoinPoint = mock(ProceedingJoinPoint.class);
        proceedingJoinPoint.expects(once()).method("proceed").will(
                returnValue(new HandlerExecutionChain(null, interceptorsOfTab)));
        InterceptorInjector injector = new InterceptorInjector();
        injector.setInterceptors(interceptorsOfFramework);

        HandlerExecutionChain handlers =
                injector.mergeInterceptorsToTabs((ProceedingJoinPoint) proceedingJoinPoint.proxy());

        assertEquals(2, handlers.getInterceptors().length);
        assertSame(interceptorOfFramework, handlers.getInterceptors()[0]);
        assertSame(interceptorOfTab, handlers.getInterceptors()[1]);
    }

    public void testShouldReturnNullWhenNoHandlerFound() throws Throwable {
        Mock proceedingJoinPoint = mock(ProceedingJoinPoint.class);
        proceedingJoinPoint.expects(once()).method("proceed").will(returnValue(null));
        InterceptorInjector injector = new InterceptorInjector();

        HandlerExecutionChain handlers =
                injector.mergeInterceptorsToTabs((ProceedingJoinPoint) proceedingJoinPoint.proxy());

        assertNull(handlers);
    }

    public void testShouldNotChangeHandler() throws Throwable {
        SimpleUrlHandlerMapping handler = new SimpleUrlHandlerMapping();

        Mock proceedingJoinPoint = mock(ProceedingJoinPoint.class);
        proceedingJoinPoint.expects(once()).method("proceed").will(
                returnValue(new HandlerExecutionChain(handler, null)));
        InterceptorInjector injector = new InterceptorInjector();

        HandlerExecutionChain handlers =
                injector.mergeInterceptorsToTabs((ProceedingJoinPoint) proceedingJoinPoint.proxy());

        assertSame(handler, handlers.getHandler());
    }

    public void testShouldJustReturnInterceptorsOfFrameworkIfNoTabInterceptors() throws Throwable {
        HandlerInterceptor interceptorOfFramework = new HandlerInterceptorSub();
        HandlerInterceptor[] interceptorsOfFramework = new HandlerInterceptor[] {interceptorOfFramework};

        Mock proceedingJoinPoint = mock(ProceedingJoinPoint.class);
        proceedingJoinPoint.expects(once()).method("proceed").will(
                returnValue(new HandlerExecutionChain(null, null)));
        InterceptorInjector injector = new InterceptorInjector();
        injector.setInterceptors(interceptorsOfFramework);

        HandlerExecutionChain handlers =
                injector.mergeInterceptorsToTabs((ProceedingJoinPoint) proceedingJoinPoint.proxy());

        assertEquals(1, handlers.getInterceptors().length);
        assertSame(interceptorOfFramework, handlers.getInterceptors()[0]);
    }

}
