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
var TimerObserver = Class.create(); 

TimerObserver.prototype = {
	timers : [],
	initialize : function() {
	},
	notify : function(jsonArray) {
		for (var i = 0; i < jsonArray.length; i++) {
			if (!jsonArray[i]) return;
			this._notify(jsonArray[i]);		
		}
	},
	_notify : function(json) {
		this.start_timer(this.get_timer(json.building_info.project_name), json);
	},
	get_timer : function(project_name) {
		var timer = this.timers[project_name];
		if (!timer) {
			timer = new Timer(project_name);
            timer.context_node = $(project_name + "_profile");
			this.timers[project_name] = timer;
        }
		return timer;
	},
	start_timer : function(timer, json) {
		var build_status = json.building_info.current_status;
		var build_duration = this.evaluate_time_to_seconds(json.building_info.build_duration);
		var elapsed_time = json.building_info.build_time_elapsed;

		var is_building = (build_status.toLowerCase() == 'building')
		
		if (!is_building) {timer.stop();}
		if (is_building && timer.is_stopped()) {
			timer.set_elapsed_time(elapsed_time);
			timer.last_build_duration(build_duration);
			timer.start();
		}
	},

    stop_timer : function(project_name) {
		var timer = this.get_timer(project_name);
		if (timer) {timer.stop();}	
	},
	evaluate_time_to_seconds : function (time) {
		if (!time) return;
		time = time.replace("second", "", "gi");
		time = time.replace("minute", "*60+", "gi");
		time = time.replace("hour", " *3600+" , "gi");
		time = time.replace(/s/gi, "");
		time = time.replace(/\+$/gi, "");
		try{
			return eval(time) - 0
		} catch (err) {
			return 0
		}
	}
}
var timer_observer = new TimerObserver();
