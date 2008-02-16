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
var DashboardPeriodicalExecuter = Class.create();

DashboardPeriodicalExecuter.URL_GET_STATUS = 'getProjectBuildStatus.ajax';

DashboardPeriodicalExecuter.prototype = {
	observers : $A([]),
	is_execution_start : false,
	initialize: function() {
	},
	start : function() {
		this.is_execution_start = true;
		var executer = this;
		new PeriodicalExecuter(function(pe) {
	        pe.stop();
	        new Ajax.Request(context_path(DashboardPeriodicalExecuter.URL_GET_STATUS), {
	            asynchronous:1,
	            method: 'GET',
	            onComplete: function() {
	                pe.registerCallback();
	            },
	            onSuccess: function(transport) {
					executer._loop_observers(transport);
				}
			})
	    }, 5);
	},
	_loop_observers : function(transport) {
		var json_array = transport.responseText
	    json_array = json_array ? eval('(' + json_array + ')') : [];
		this.observers.each(function(observer){
			if(!observer) return;
			if(!observer.notify) return;
			observer.notify(json_array);
    	})

	},
	is_start : function() {
		return this.is_execution_start;
	},
	register : function() {
		for (var i = 0; i < arguments.length; i++) {
			this.observers.push(arguments[i]);	
		}
	},
	unregister : function(observer) {
		var position = this.observers.indexOf(observer);
		this.observers[position] = null;
		this.observers = this.observers.compact();
	},
	clean : function() {
		this.observers = $A([]);
	}
}

var dashboard_periodical_executer = new DashboardPeriodicalExecuter();

Event.observe(window, 'load', function() {
    dashboard_periodical_executer.start();
});
