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
var ConfigPanelActivator = Class.create();
var ForceBuildActivator = Class.create();
var AllBuildsActivator = Class.create();
var AllPassedBuildsActivator = Class.create();
var ContentUpdater = Class.create();
var BuildProfileObserver = Class.create(); 

ConfigPanelActivator.prototype = {
	initialize : function(build_profile) {
		this.build_profile = build_profile;
	},
	activateOrInactivate : function(is_disabled, json) {
		if (!this.need_change(is_disabled, json)) return;

		if (is_disabled) {
			$(json.building_info.project_name + '_config_panel').removeClassName("config_panel_disabled");
			$(json.building_info.project_name + '_config_panel').addClassName("config_panel_enabled");
		} else {
			$(json.building_info.project_name + '_config_panel').removeClassName("config_panel_enabled");
			$(json.building_info.project_name + '_config_panel').addClassName("config_panel_disabled");
		}

		this.build_profile.create_config_panel_link($(json.building_info.project_name + '_config_panel'));
	},
	need_change : function(is_disabled, json) {
		return !!(is_disabled ^ is_discontinued(json))
	}
}

ForceBuildActivator.prototype = {
	initialize : function(build_profile) {
		this.build_profile = build_profile;
	},
	activateOrInactivate : function(is_disabled, json) {
		if (!this.need_change(is_disabled, json)) return;
		if (is_disabled) {
			$(json.building_info.project_name + '_forcebuild').removeClassName("force_build_disabled");
			$(json.building_info.project_name + '_forcebuild').addClassName("force_build_enabled");
		} else {
			$(json.building_info.project_name + '_forcebuild').removeClassName("force_build_enabled");
			$(json.building_info.project_name + '_forcebuild').addClassName("force_build_disabled");
		}
		this.build_profile.create_force_build_link($(json.building_info.project_name + '_forcebuild'));
	},
	need_change : function(is_disabled, json) {
		return !!(is_disabled ^ should_forcebuild_be_disabled(json))
	}
}


var BaseBuildsActivator = Class.create();

BaseBuildsActivator.prototype = {
	initialize : function(build_profile) {
		this.build_profile = build_profile;
	},
	need_change : function(is_disabled, json) {
		var should_be_disabled = is_inactive(json);
		return !!(is_disabled ^ should_be_disabled)
	}
}

AllBuildsActivator.prototype = Object.extend(new BaseBuildsActivator(), {
	activateOrInactivate : function(is_disabled, json) {
		if (!this.need_change(is_disabled, json)) return;
		this.build_profile.create_all_builds_link($(json.building_info.project_name + '_all_builds'));
	}
});

AllPassedBuildsActivator.prototype = Object.extend(new BaseBuildsActivator(), {
	activateOrInactivate : function(is_disabled, json) {
		if (!this.need_change(is_disabled, json)) return;
		this.build_profile.create_all_successful_builds_link($(json.building_info.project_name + '_all_successful_builds'));
	}
});

ContentUpdater.prototype = {
	initialize : function() {
	},
	update : function (json, link) {
        var project_build_date = json.building_info.project_name + '_build_date';
        $(project_build_date).update(json.building_info.latest_build_date);

        var project_previous_result_text = json.building_info.project_name + '_previous_result_text';
        var previous_text = this.previousResultText(json)
        $(project_previous_result_text).update(previous_text);

        var project_build_detail = json.building_info.project_name + '_build_detail';
    	$(project_build_detail).href =  link;
   },
   previousResultText : function(json) {
       if (is_previous_unknown(json)) return "";
       var previous_result = json.building_info.previous_result.toLowerCase();
       //TODO spike why IE needs extra nbsp; to make word-spacing work?
       previous_result  += Prototype.Browser.IE ? "&nbsp;" : "";
       if (is_discontinued(json)) {
           return "last " + previous_result;
       } else {
           return previous_result;
       }
   }
}


BuildProfileObserver.prototype = Object.extend(new BuildBaseObserver(), {
	profile : null,
	initialize : function() {
		this.profile = new BuildProfile();
		this.config_panel_activator = new ConfigPanelActivator(this.profile);
		this.force_build_activator = new ForceBuildActivator(this.profile);
		this.all_builds_activator = new AllBuildsActivator(this.profile);
		this.all_passed_builds_activator = new AllPassedBuildsActivator(this.profile);
		this.content_updater = new ContentUpdater();
	},
	notify : function(jsonArray) {
		for (var i = 0; i < jsonArray.length; i++) {
			if (!jsonArray[i]) return;
			this._notify(jsonArray[i]);		
		}
	},
	_notify : function(json) {
	    var profile_id = json.building_info.project_name + '_profile';
		if (!$(profile_id)) return;

		var is_force_build_disabled   = $(json.building_info.project_name + '_forcebuild').hasClassName("force_build_disabled");
		var is_config_panel_disabled  = $(json.building_info.project_name + '_config_panel').hasClassName("config_panel_disabled");
		var is_inactive_before_update = $(profile_id).hasClassName("inactive");

        json_to_css.update_profile(json);

		this.config_panel_activator.activateOrInactivate(is_config_panel_disabled, json);
		this.force_build_activator.activateOrInactivate(is_force_build_disabled, json);
		this.all_builds_activator.activateOrInactivate(is_inactive_before_update, json);
		this.all_passed_builds_activator.activateOrInactivate(is_inactive_before_update, json);
		this.content_updater.update(json, this.get_link(json));
	}
})
