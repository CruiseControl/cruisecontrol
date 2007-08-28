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
var BuildProfileObserver = Class.create(); 

BuildProfileObserver.prototype = Object.extend(new BuildBaseObserver(), {
	initialize : function() {
	},
	notify : function(json) {
		this.activate(json);
	    var profile_id = json.building_info.project_name + '_profile'
	    clean_active_css_class_on_element(profile_id)
	   	Element.addClassName($(profile_id), json.building_info.css_class_name)
	
	    var project_build_date = json.building_info.project_name + '_build_date';
	    $(project_build_date).innerHTML = " at " + json.building_info.latest_build_date;
	    var project_build_detail = json.building_info.project_name + '_build_detail';
	    $(project_build_detail).href =   new BuildBaseObserver().get_link(json);
	},
	activate: function(json) {
		var profile_id = json.building_info.project_name + '_profile';
		
		if(!$(profile_id).hasClassName("inactive")) return;
	    $(json.building_info.project_name + '_forcebuild').onclick = function() {new BuildProfile().force_build(this)};
		var img =  $(json.building_info.project_name + '_forcebuild').immediateDescendants()[0];
		if (img) {
			img.src = context_path('images/icon-force-build.gif');
			img.title = "Force build";
			img.alt = "Force build";
		}
		$(json.building_info.project_name + '_config_panel').onclick = function() {new Toolkit().show('toolkit_' + json.building_info.project_name)};
	    $(json.building_info.project_name + '_all_builds').href = context_path('project/list/all/' + json.building_info.project_name);
	    $(json.building_info.project_name + '_all_successful_builds').href = context_path('project/list/passed/' + json.building_info.project_name);
	}
})
