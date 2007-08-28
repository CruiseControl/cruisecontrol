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
 var CCStatusObserver = Class.create();
 
 CCStatusObserver.prototype = {
    initialize : function() {},
    notify     : function (json) {
        if (json.error){
            $('cruisecontrol_status').show();
			this.reset_timer();
			this.remove_handler();
        } else if($('cruisecontrol_status').visible()) {
			$('cruisecontrol_status').hide();
			if (!json) return;
            if (!json.length) return; 
            for (var i = 0; i < json.length; i++) {
                if (!json[i]) continue;
                $(json[i].building_info.project_name + '_forcebuild').onclick =  new BuildProfile().force_build_enabled.on_click; 
			}
        }
    },
	reset_timer : function() {
		function _reset_timer_by_class_name (class_name) {
			$A($$(class_name)).each(
				function(building_project) {
	            	var project_name = $(building_project).id.replace("_profile", "")
	            	eval_timer_object(project_name, "", 0, 0);
	        	}
			)
		}
		_reset_timer_by_class_name('.building_passed');
		_reset_timer_by_class_name('.building_failed');
		_reset_timer_by_class_name('.building_unknown');
	},
	remove_handler : function() {
		$A($$('.force_build_link')).each(function(element){
                $(element).onclick = Prototype.emptyFunction;
		})
	}
}
