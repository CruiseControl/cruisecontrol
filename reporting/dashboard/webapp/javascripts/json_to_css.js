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
var JsonToCss = Class.create();

JsonToCss.prototype = {
    initialize : function() {},

    update_bar : function(json) {
        var projectName = json.building_info.project_name;
        var bar = $(projectName + "_bar");
        if (!bar) return;
        var css_class_name = this._get_css_class_name(json.building_info.current_status, json.building_info.previous_result);
        this._renew_class_name(bar, [css_class_name]);
    },

    update_profile : function(json) {
        var projectName = json.building_info.project_name + '_profile';
        var css_class_name = this._get_css_class_name(json.building_info.current_status, json.building_info.previous_result);
        this._renew_class_name(projectName, [css_class_name]);
    },
    update_config_panel : function(json) {
        var element_id = json.building_info.project_name + '_config_panel';
        $(element_id).removeClassName("config_panel_disabled");
        $(element_id).removeClassName("config_panel_enabled");
        if (is_discontinued(json)) {
	        $(element_id).addClassName("config_panel_disabled");
        } else {
	        $(element_id).addClassName("config_panel_enabled");
        }
    },
    update_force_build : function(json) {
        var element_id = json.building_info.project_name + '_forcebuild';
        $(element_id).removeClassName("force_build_disabled");
        $(element_id).removeClassName("force_build_enabled");
        if (should_forcebuild_be_disabled(json)) {
	        $(element_id).addClassName("force_build_disabled");
        } else {
	        $(element_id).addClassName("force_build_enabled");
        }    
    },
    update_tooltip : function(json) {
        var projectName = json.building_info.project_name;
        var tooltip = $('tooltip_' + projectName);
        if (!tooltip) return;
        tooltip.className = '';
        var css_class_name = this._get_css_class_name(json.building_info.current_status, json.building_info.previous_result, 'tooltip_');
        this._renew_class_name(tooltip, ['tooltip', css_class_name]);
    },

    update_level : function(json) {
        var projectName = json.building_info.project_name;
        var level = $(projectName + "_level");
        this._renew_class_name(level, ['level_'  + json.building_info['level']]);
        reround(level);
    },

    update_build_detail_header : function(json) {
		var css_class_name = this._get_css_class_name(json.building_info.current_status, json.building_info.previous_result)
        this._renew_class_name('build_detail_summary_container', [css_class_name]);
	},
	
	update_build_list : function(json, id) {
		var elementId = "build_list_" + id
		var css_class_name = this._get_css_class_name(json.building_info.current_status, json.building_info.previous_result)
        this._renew_class_name(elementId, [css_class_name]);
	},

    _get_css_class_name : function(current_status, previous_result, prefix) {
        if (!prefix) prefix = '';
        previous_result = previous_result.toLowerCase();
        current_status = current_status.toLowerCase();
        var prefixed_result = prefix + previous_result;
        if (previous_result == "unknown" && current_status == "queued") {
            return "queued inactive";
        } else if (previous_result == "unknown" && current_status != "building") {
			return "inactive";
        } else if (current_status == "queued") {
            return "queued " + prefixed_result;
        } else if (current_status == "paused") {
            return "paused " + prefixed_result;
        } else if (current_status == "discontinued") {
        	return "discontinued " + prefixed_result;
        } else if (current_status == "building") {
            return prefix + "building_" + previous_result;
        } else {
           return prefixed_result;
        }
    },
    
    _renew_class_name : function(elementOrId, cssClasses) {
        var element = $(elementOrId);
        clean_active_css_class_on_element(element);
        $A(cssClasses).each(function(cssClass) {
            Element.addClassName(element, cssClass);
        });
    }

}

var json_to_css = new JsonToCss();
