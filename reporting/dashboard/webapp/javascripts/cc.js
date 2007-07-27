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
 * We use prototype stuff here, for example $() to replace document.getElementById()
 */
 
 


/**
 * BEGIN: using ajax call to update dashboard.
 */
var active_build_status = ['passed', 'failed', 'building', 'inactive', 'build_profile_inactive', 'failed_level_0', 'failed_level_1', 'failed_level_2', 'failed_level_3', 'failed_level_4', 'failed_level_5', 'failed_level_6', 'failed_level_7', 'failed_level_8', 'passed_level_0', 'passed_level_1', 'passed_level_2', 'passed_level_3', 'passed_level_4', 'passed_level_5', 'passed_level_6', 'passed_level_7', 'passed_level_8']

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


function ajax_tree_navigator(element, url) {
    var id = url.replace(/\//gi, "_");
	var spanElem = $(element).ancestors()[0];
	if ($(id).visible()) {
		spanElem.removeClassName("opened_directory");
		spanElem.addClassName("directory");
		$(id).hide();
	} else {
        new Ajax.Request(url, {
            asynchronous:0,
            method: 'GET',
            onSuccess: function(transport) {
				spanElem.removeClassName("directory");
				spanElem.addClassName("opened_directory");
				$(id).innerHTML = transport.responseText;
				$(id).show();
            }
        });
	}
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
        ajax_periodical_refresh_dashboard_update_project_box(json[i]);
    }
}

function update_statistics_status(json) {
    if (!json.length) return 
    if (json.length == 0) return 
	var statistics = $H({passed:0,failed:0,building:0,bootstrapping:0,modificationset:0})
    for (var i = 0; i < json.length; i++) {
    	category_projects_info_by_status(json[i], statistics);
    }
    set_inactive_projects_amount(statistics)
    calculate_projects_statistics(statistics)
  	ajax_periodical_refresh_statistics_summary_infos(statistics);
}

function update_cc_status(json) {
    if (json.error){
        $('cruisecontrol_status').show();
        $A($$('.building')).each(function(building_project){
           	var project_name = $(building_project).id.replace("_profile", "")
            eval_timer_object(project_name, "anystatusbutbuilding", 0, 0)
        })
        $A($$('.force_build_link')).each(function(element){
            $(element).onclick = null
        })
    } else if($('cruisecontrol_status').visible()) {
        $('cruisecontrol_status').hide();
        if (!json.length) return 
        if (json.length == 0) return 
        for (var i = 0; i < json.length; i++) {
            $(json[i].building_info.project_name + '_forcebuild').onclick = function(event) {
            	var project_name = this.id.replace("_forcebuild","")
            	ajax_force_build("projectName", project_name, event)
            }
        }
    }
}

function ajax_periodical_refresh_dashboard_update_project_box(json) {
	if (json.building_info.building_status.toLowerCase() == 'bootstrapping') return;
	if (json.building_info.building_status.toLowerCase() == 'modificationset') return;
	if (json.building_info.building_status.toLowerCase() == 'inactive') return;
	ajax_periodical_refresh_dashboard_update_inactive_partial_links(json)
	ajax_periodical_refresh_dashboard_update_project_build_detail(json)
	ajax_periodical_refresh_dashboard_update_project_bar(json)
	ajax_periodical_refresh_dashboard_update_tooltip(json)
	eval_timer_object(json.building_info.project_name, json.building_info.building_status, evaluate_time_to_seconds(json.building_info.build_duration), json.building_info.build_time_elapsed);
}

function ajax_periodical_refresh_dashboard_update_project_build_detail(json) {
    var class_name = json.building_info.building_status.toLowerCase();
    var profile_id = json.building_info.project_name + '_profile'
    clean_active_css_class_on_element(profile_id)
   	Element.addClassName($(profile_id), json.building_info.css_class_name)
   	Element.addClassName($(profile_id), 'build_profile_' + json.building_info.css_class_name)

    var project_build_date = json.building_info.project_name + '_build_date';
    $(project_build_date).innerHTML = " at " + json.building_info.latest_build_date;
    var project_build_detail = json.building_info.project_name + '_build_detail';
    $(project_build_detail).href = get_link_by_building_status(json)
}

function ajax_periodical_refresh_dashboard_update_inactive_partial_links(json) {
	var profile_id = json.building_info.project_name + '_profile'
	if(!$(profile_id).hasClassName("inactive")) return

    $(json.building_info.project_name + '_forcebuild').onclick = function(event) {ajax_force_build("projectName", json.building_info.project_name, event)}
    $(json.building_info.project_name + '_config_panel').onclick = function(event) {display_toolkit('toolkit_' + json.building_info.project_name, event)}
    $(json.building_info.project_name + '_all_builds').href = context_path('project/list/all/' + json.building_info.project_name)
    $(json.building_info.project_name + '_all_successful_builds').href = context_path('project/list/passed/' + json.building_info.project_name)
}

function clean_active_css_class_on_element(element) {
	$A(active_build_status).each(function(status) {
	    Element.removeClassName($(element), status);
	    Element.removeClassName($(element), 'build_profile_' + status);
    })
}

function ajax_periodical_refresh_dashboard_update_project_bar(json) {
	var projectName = json.building_info.project_name;
	var buildStatus = json.building_info.building_status
    var bar = $(projectName + "_bar")
    clean_active_css_class_on_element(bar)
   	Element.addClassName(bar, json.building_info.css_class_name_for_dashboard)
    reround(bar)
    var bar_link_id = projectName + "_bar_link"
    $(bar_link_id).href = get_link_by_building_status(json)
}

function ajax_periodical_refresh_dashboard_update_tooltip(json){
	var projectName = json.building_info.project_name
	var buildStatus = json.building_info.building_status
	var tool_tip_id_prefix = 'tooltip_' + projectName
	$(tool_tip_id_prefix).className=''
	Element.addClassName($(tool_tip_id_prefix), 'tooltip')
	Element.addClassName($(tool_tip_id_prefix), 'tooltip_' + json.building_info.css_class_name)
	
	$(tool_tip_id_prefix + '_name').innerHTML = projectName
	$(tool_tip_id_prefix + '_status').innerHTML = "Status: " + buildStatus
	if(buildStatus != 'inactive'){
	    $(tool_tip_id_prefix + '_date').innerHTML = "Date: " + json.building_info.latest_build_date
	}
}

function ajax_periodical_refresh_statistics_summary_infos(statistics_infos) {
	var infos = $A(['passed', 'failed', 'building', 'total', 'rate', 'inactive'])
	infos.each(function(info) {
	    var statistic = $('statistics_' + info).innerHTML;
		$('statistics_' + info).innerHTML = statistics_infos[info] + statistic.substring(statistic.indexOf(' '));
	});
}

function set_inactive_projects_amount(statistics) {
	statistics['inactive'] = $A($$('.inactive.bar')).size()
}

function category_projects_info_by_status(json, statistics) {
	if (!json) return;
	if (!json.building_info) return;
	if (!json.building_info.building_status) return;
	var status = json.building_info.building_status.toLowerCase();
	if (status == 'inactive') return;
	if (status == 'modificationset' || status == 'bootstrapping') {
		status = 'building';
	}
	statistics[status] +=  1;
}

function calculate_projects_statistics(statistics) {
	var total = 0;
	statistics.each(function(pair){
		total += pair.value;
	});
	statistics['total'] = total;
    var rate = ((statistics['passed'] / (total - statistics['inactive'])) * 100).toFixed(0)
	statistics['rate'] = isNaN(rate) ? "0%" : rate+"%";
	return statistics; 
}

var displayed_toolkit

function display_toolkit(id, e) {
    disable_bubble(e);
	if (displayed_toolkit) {
		$(displayed_toolkit).hide();
	}
	$(id).show();
	displayed_toolkit = $(id);
}

function close_toolkit(element, e) {
	$(element).hide()
}


function get_link_by_building_status(json) {
    if (!json)  return;
    if (!json.building_info) return;
    if (!json.building_info.building_status) return;
    if (json.building_info.building_status == 'Building') {
        return 'build/detail/live/' + json.building_info.project_name
    } else {
        return 'build/detail/' + json.building_info.project_name
    }

}
/**
 * END: using ajax call to update dashboard.
 */




/**
 * BEGIN: using ajax call to update ActiveBuild Page.
 */
function ajax_periodical_refresh_active_build_executer(project_name) {
    var executer = new PeriodicalExecuter(function() {
        executer.stop();
        var ajaxRequest = new Ajax.Request(context_path('getProjectBuildStatus.ajax'), {
            asynchronous:1,
            method: 'GET',
            onComplete: function() {
		        if(active_build_finished()){
		            return;
		        }
                executer.registerCallback();
            },
            onSuccess: function(transport) {
				var json = transport.responseText
				json = json ? eval('(' + json + ')') : null
                ajax_periodical_refresh_active_build_executer_oncomplete(json, project_name)
            }
        });
    }, 5);
}

function ajax_periodical_refresh_active_build_executer_oncomplete(json, project_name) {
    if (!json) return
    for (var i = 0; i < json.length; i++) {
        var building_info = json[i].building_info;
        if (building_info.project_name == project_name) {
            var building_status = building_info.building_status.toLowerCase();
            if (building_status == 'passed' || building_status == 'failed' ) {
                $$('.build_detail_summary h3')[0].innerHTML = project_name + " <span class='build_status'>" + building_status + "</span> (<a href='" + context_path("build/detail/" + project_name + "/" + building_info.latest_build_log_file_name) + "'>see details</a>)"
                $$('.build_detail_summary')[0].ancestors()[0].className = building_info.css_class_name
            }
            $$(".build_status").each(function(elem) {
                elem.innerHTML = building_status;
            });
            eval_timer_object(project_name, building_status, evaluate_time_to_seconds(building_info.build_duration), building_info.build_time_elapsed);
        }
    }
}

function ajax_periodical_refresh_active_build_output_executer(project_name) {
    var start = 0;
    var executer = new PeriodicalExecuter(function() {
        executer.stop();
        var ajaxRequest = new Ajax.Request(context_path('getProjectBuildOutput.ajax'), {
            asynchronous:1,
            method: 'GET',
            parameters: 'project=' + project_name + '&start=' + start,
            onComplete: function() {
		        if(active_build_finished()){
		            return;
		        }
                executer.registerCallback();
            },
            onSuccess: function(transport, next_start_as_json) {
                start = next_start_as_json[0]
                ajax_periodical_refresh_active_build_output_executer_oncomplete(transport.responseText)
            }
        });
    }, 2);
}

function ajax_periodical_refresh_active_build_output_executer_oncomplete(build_output) {
    if (!build_output) return
    $('buildoutput_span').innerHTML += build_output
}

function ajax_refresh_active_build_commit_message(modifications) {
    if(modifications.length == 0) {
        $('modification_keys').innerHTML = "<h2>Build forced, No new code is committed into repository</h2>"
        return
    }
    var modifications_string = ""
    for(var i=0; i<modifications.length; i++){
        modifications_string += ("<li class='modification'><span class='user'>" + modifications[i].user + "</span><br/><span class='comment'>" + modifications[i].comment + "</span></li>")
    }
    $('modification_keys').innerHTML = modifications_string
}

function active_build_finished(){
    return $$('.build_detail_summary')[0].ancestors()[0].className != "building";
}
/**
 * END: using ajax call to update ActiveBuild Page.
 */




/**
 * BEGIN: add project.
 */
function checkAndAddProject() {
    var url = $('url').value;
    var projectName = $('projectName').value;
    disableAddProjectButtons(true);
    var vcIcon = $('url_icon');
    vcIcon.src = context_path("images/wait.gif");
    new Ajax.Request(context_path('addProjectFromVersionControl.ajax'), {
        parameters: "url=" + url + "&projectName=" + projectName + "&vcsType=" + $('vcsType').value + "&moduleName=" + $('moduleName').value,
        asynchronous:1,
        method: 'GET',
        onComplete: function(transport) {
	     	var json = transport.responseText
		    json = json ? eval('(' + json + ')') : null
            ajax_update_icons_and_invoke_callback_function(json);
        }
    });
}

function ajax_update_icons_and_invoke_callback_function(json) {
    $('url_icon').src = '';
    $('projectName_icon').src = '';
    var resultOk = json.result.ok == "success";
    var field = json.result.field
    if (resultOk) {
        $('url_icon').src = context_path('images/accept.png');
        $('projectName_icon').src = context_path("images/accept.png");
        $('moduleName_icon').src = context_path("images/accept.png");
        $('vcError').innerHTML = "";
        $('vc_success_info').innerHTML = "Project added. " + json.result.response;
        new TransparentMenu('vc_success_info',{insideElement : {id:'trans_message_container', width:'500px', height:'40px'}, displayMode:'now', hideDelay:25, hideMode:'timeout'})
    } else {
        var icon_id = field + '_icon'
        $(icon_id).src = context_path("images/exclamation.png");
        $('vc_success_info').innerHTML = "";
        $('vcError').innerHTML = json.result.response;
    }
    disableAddProjectButtons(false);
}

function disableAddProjectButtons(disabled) {
    $('addButton').disabled = disabled;
}
/**
 * END: add project.
 */



/**
 * BEGIN: toggle tab
 */
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

function toggle_tab_content(tab_ids, tab_id){
	$(tab_id).show()
    for (var i = 0; i < tab_ids.length; i++) {
	    if (tab_ids[i] != tab_id) {
		    $(tab_ids[i]).hide()
		}
    }
	return false;
}

function build_detail_toggle_tab_content(element){
	var ul = $(element).ancestors()[0];
	var childElements = $A(ul.immediateDescendants())
	childElements.each(function(elem) {
		$(elem).removeClassName('current_tab');
		var div = get_corresponding_content_element($(elem));
		if (div) {
			div.hide()
		}
	});
	$(element).addClassName('current_tab');
	var div = get_corresponding_content_element($(element))
	if (div) {
		div.show()
	}
}

function get_corresponding_content_element(li_element) {
	var spans = $A($(li_element).getElementsBySelector('span'));
	if (spans.size() > 0) {
		var id_for_div = spans.first().innerHTML;
		return $(id_for_div.toLowerCase())
	}
}
/**
 * END: toggle tab
 */



/**
 * BEGIN: force build
 */
function ajax_force_build(parameter, project_name, e) {
    var url = 'forcebuild.ajax';
    var pars = parameter + '=' + project_name;
    var profile_id = project_name  + '_profile';
	new TransparentMenu('trans_message',{afterElement: profile_id, displayMode:'now', hideDelay:3, hideMode:'timeout', top:2, left:null})
    new Ajax.Request(url, { method: 'GET', parameters: pars });
    disable_bubble(e);
}
/**
 * END: force build
 */