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
var BuildDetailExecuter = Class.create();

BuildDetailExecuter.is_build_finished = false;

BuildDetailExecuter.prototype = {
	initialize: function() {
	},
	ajax_refresh_active_build_commit_message : function(modifications) {
	    if(modifications.length == 0) {
	        $('modification_keys').innerHTML = "<h2>Build forced, No new code is committed into repository</h2>"
	        return
	    }
	    var modifications_string = ""
	    for(var i = 0; i < modifications.length; i++) {
	    	if (modifications[i]) {
				modifications_string += ("<li class='modification'><span class='user'>" + modifications[i].user + "</span><br/><span class='comment'>" + modifications[i].comment + "</span></li>")
	    	}        
	    }
	    $('modification_keys').innerHTML = modifications_string
	},
	ajax_periodical_refresh_active_build_executer : function(project_name) {
	    var executer = new PeriodicalExecuter(function() {
	        executer.stop();
	        var ajaxRequest = new Ajax.Request(context_path('getProjectBuildStatus.ajax'), {
	            asynchronous:1,
	            method: 'GET',
	            onComplete: function() {
			        if(BuildDetailExecuter.is_build_finished){
			            return;
			        }
	                executer.registerCallback();
	            },
	            onSuccess: function(transport) {
					var json = transport.responseText
					json = json ? eval('(' + json + ')') : null
	                BuildDetailExecuter.ajax_periodical_refresh_active_build_executer_oncomplete(json, project_name)
	            }
	        });
	    }, 5);
	},
	ajax_periodical_refresh_active_build_output_executer : function(project_name) {
	    var start = 0;
	    var executer = new PeriodicalExecuter(function() {
	        executer.stop();
	        var ajaxRequest = new Ajax.Request(context_path('getProjectBuildOutput.ajax'), {
	            asynchronous:1,
	            method: 'GET',
	            parameters: 'project=' + project_name + '&start=' + start,
	            onComplete: function() {
			        if(BuildDetailExecuter.is_build_finished){
			            return;
			        }
	                executer.registerCallback();
	            },
	            onSuccess: function(transport, next_start_as_json) {
	                start = next_start_as_json[0];
	                BuildDetailExecuter.ajax_periodical_refresh_active_build_output_executer_oncomplete(transport.responseText)
	            }
	        });
	    }, 2);
	}
}

//static method 
BuildDetailExecuter.ajax_periodical_refresh_active_build_output_executer_oncomplete = function(build_output) {
	    if (!build_output) return
	    $('buildoutput_pre').update($('buildoutput_pre').innerHTML + build_output.replace(/\n/g,"<br>"));
}

BuildDetailExecuter.build_finished = function() {
	BuildDetailExecuter.is_build_finished = true;
}

BuildDetailExecuter.ajax_periodical_refresh_active_build_executer_oncomplete = function(json, project_name) {
	if (!json) return
	for (var i = 0; i < json.length; i++) {
        if (!json[i]) continue;
        var building_info = json[i].building_info;
        if (building_info.project_name == project_name) {
            var building_status = building_info.building_status.toLowerCase();
            if (building_status == 'passed' || building_status == 'failed' ) {
                $$('.build_detail_summary h3')[0].innerHTML = project_name + " <span class='build_status'>" + building_status + "</span> (<a href='" + context_path("build/detail/" + project_name) + "'>see details</a>)"
				var div = $$('.build_detail_summary')[0].ancestors()[0]
				clean_active_css_class_on_element(div);
				$(div).addClassName(building_info.css_class_name);
				reround(div);
				BuildDetailExecuter.build_finished();
            }
            $$(".build_status").each(function(elem) {
                elem.innerHTML = building_status;
            });
            eval_timer_object(project_name, building_status, evaluate_time_to_seconds(building_info.build_duration), building_info.build_time_elapsed);
        }
	}
}

BuildDetailExecuter.ajax_tree_navigator = function (element, url) {
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

//TODO is this one Userfull now?
function toggle_tab_content(tab_ids, tab_id){
	$(tab_id).show()
    for (var i = 0; i < tab_ids.length; i++) {
	    if (tab_ids[i] != tab_id) {
		    $(tab_ids[i]).hide()
		}
    }
	return false;
}
