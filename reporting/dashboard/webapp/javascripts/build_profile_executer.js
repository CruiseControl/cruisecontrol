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
var BuildProfileExecuter = Class.create(); 
BuildProfileExecuter.prototype = Object.extend(new BuildBaseExecuter(), {
	initialize : function() {
	},
	execute : function(json) {
	    var profile_id = json.building_info.project_name + '_profile'
	    clean_active_css_class_on_element(profile_id)
	   	Element.addClassName($(profile_id), 'build_profile_' + json.building_info.css_class_name)
	   	Element.addClassName($(profile_id), json.building_info.css_class_name)
	   	Element.addClassName($(profile_id), json.building_info.css_class_name_considering_last_status)
	
	    var project_build_date = json.building_info.project_name + '_build_date';
	    $(project_build_date).innerHTML = " at " + json.building_info.latest_build_date;
	    var project_build_detail = json.building_info.project_name + '_build_detail';
	    $(project_build_detail).href =   new BuildBaseExecuter().get_link(json);
	}
})


function ajax_force_build(parameter, project_name, e) {
	    var url = 'forcebuild.ajax';
	    var pars = parameter + '=' + project_name;
	    var profile_id = project_name  + '_profile';
		new TransparentMenu('trans_message',{afterElement: profile_id, displayMode:'now', hideDelay:3, hideMode:'timeout', top:2, left:null})
	    new Ajax.Request(url, { method: 'GET', parameters: pars });
	    disable_bubble(e);
}

function ajax_periodical_refresh_dashboard_update_tooltip(json){
		var projectName = json.building_info.project_name
		var tool_tip_id_prefix = 'tooltip_' + projectName
		$(tool_tip_id_prefix).className=''
		Element.addClassName($(tool_tip_id_prefix), 'tooltip')
		Element.addClassName($(tool_tip_id_prefix), 'tooltip_' + json.building_info.css_class_name)
		
		var buildStatus = json.building_info.building_status
		$(tool_tip_id_prefix + '_name').innerHTML = projectName
		$$WordBreaker.word_break($(tool_tip_id_prefix + '_name'));
		$(tool_tip_id_prefix + '_status').innerHTML = "Status: " + buildStatus
		if(buildStatus != 'inactive'){
		    $(tool_tip_id_prefix + '_date').innerHTML = "Date: " + json.building_info.latest_build_date
		}
}
