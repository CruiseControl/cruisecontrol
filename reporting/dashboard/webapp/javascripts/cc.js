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
/**
 * BEGIN: using ajax call to update dashboard.
 */
var active_build_status = ['passed', 'failed', 'building', 'passed_building', 'failed_building', 'inactive', 'build_profile_inactive', 'failed_level_0', 'failed_level_1', 'failed_level_2', 'failed_level_3', 'failed_level_4', 'failed_level_5', 'failed_level_6', 'failed_level_7', 'failed_level_8', 'passed_level_0', 'passed_level_1', 'passed_level_2', 'passed_level_3', 'passed_level_4', 'passed_level_5', 'passed_level_6', 'passed_level_7', 'passed_level_8']

function ajax_periodical_refresh_dashboard_executer() {
    var executer = new PeriodicalExecuter(function() {
	        executer.stop();
	        var ajaxRequest = new Ajax.Request(context_path('getProjectBuildStatus.ajax'), {
	            asynchronous:1,
	            method: 'GET',
	            onComplete: function() {
	                executer.registerCallback();
	            },
	            onSuccess: function(transport) {
	            	var json = transport.responseText
				    json = json ? eval('(' + json + ')') : null
	                ajax_periodical_refresh_dashboard_executer_oncomplete(json)
	            }
	        }
		);
    }, 5);
}


function ajax_periodical_refresh_dashboard_executer_oncomplete(json) {
    if (!json) return
    update_projects_status(json)
    update_statistics_status(json)
    update_cc_status(json)
}

function update_projects_status(json) {
    if (!json.length) return 
    for (var i = 0; i < json.length; i++) {
		if (!json[i]) continue;
        ajax_periodical_refresh_dashboard_update_project_box(json[i]);
    }
}


function ajax_periodical_refresh_dashboard_update_project_box(json) {
	if (json.building_info.building_status.toLowerCase() == 'inactive') return;
	ajax_periodical_refresh_dashboard_update_inactive_partial_links(json)
	new BuildProfileExecuter().execute(json)
	new BuildBarExecuter().execute(json)
	ajax_periodical_refresh_dashboard_update_tooltip(json)
	eval_timer_object(json.building_info.project_name, json.building_info.building_status, evaluate_time_to_seconds(json.building_info.build_duration), json.building_info.build_time_elapsed);
}

function ajax_periodical_refresh_dashboard_update_inactive_partial_links(json) {
	var profile_id = json.building_info.project_name + '_profile'
	if(!$(profile_id).hasClassName("inactive")) return

    $(json.building_info.project_name + '_forcebuild').onclick = function(event) {ajax_force_build("projectName", json.building_info.project_name, event)}
    $(json.building_info.project_name + '_config_panel').onclick = function(event) {new Toolkit().show('toolkit_' + json.building_info.project_name, event)}
    $(json.building_info.project_name + '_all_builds').href = context_path('project/list/all/' + json.building_info.project_name)
    $(json.building_info.project_name + '_all_successful_builds').href = context_path('project/list/passed/' + json.building_info.project_name)
}

function clean_active_css_class_on_element(element) {
	$A(active_build_status).each(function(status) {
	    Element.removeClassName($(element), status);
	    Element.removeClassName($(element), 'build_profile_' + status);
    })
}

function toggle_tab(name) {
	var tab = $('tabContent'+name);
	var nodes = $A($$("#tabscontent .tabContent"));
	nodes.each(function(node){
			Element.hide(node);
	});
   	Element.toggle(tab);    	
	switch_current_tab_in_tab_view(name);
	switch_layout_in_tab_view(name);
}

function switch_current_tab_in_tab_view(name) {
	Element.removeClassName($('dashboard'), 'currenttab');
	Element.removeClassName($('builds'), 'currenttab');
	if (name == 1) {
		 var header = $('dashboard')
	} else {
		 var header = $('builds')
	}
	Element.addClassName(header, 'currenttab');
}

function switch_layout_in_tab_view(name) {
	if (name == 2) {
		Element.removeClassName($('project_summary_panel'),'yui-u');
		Element.addClassName($('project_summary_panel'),'yui-g');
	} else {
		Element.addClassName($('project_summary_panel'),'yui-u');
		Element.removeClassName($('project_summary_panel'),'yui-g');
	}
}
