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

var BuildProfile = Class.create();

BuildProfile.FORCE_BUILD =
{ 
	width: '20',
	height: '20',
	active : {
		on_click : function() {new BuildProfile().force_build(this)},
		href : function(element) {return "javascript:void(0)"},
		src : 'images/icon-force-build.gif',
		text : "Force build"
	},

	inactive: {
		on_click : Prototype.emptyFunction,
		href : function(element) {return "javascript:void(0)"},
		src : 'images/icon-force-build-grey.gif',
		text : "Force build (disabled)"
	}
}

BuildProfile.CONFIG_PANEL =
{
	width: '35',
	height: '20',
	active : {
		on_click : function() {var toolkit_id = 'toolkit_' + this.id.replace("_config_panel", "");new Toolkit().show(toolkit_id)},
		href : function(element) {return "javascript:void(0)"},
		src : 'images/icon-config-dropdown.gif',
		text : "Configure project"
	},

	inactive : {
		on_click : Prototype.emptyFunction,
		href : function(element) {return "javascript:void(0)"},
		src : 'images/icon-config-dropdown-grey.gif',
		text : "Configure project"
	}
}

BuildProfile.ALL_BUILDS = {
	width: '20',
	height: '20',
	active: {
		on_click : Prototype.emptyFunction,
		href : function(element) {return context_path("project/list/all/" + element.id.replace("_all_builds", ""))},
		src : 'images/icon-view-all-builds.gif',
		text : "All builds"
	},
	inactive: {
		on_click : Prototype.emptyFunction,
		href : function(element) {return "javascript:void(0)"},
		src : 'images/icon-view-all-builds-grey.gif',
		text : "All builds"
	}
}

BuildProfile.ALL_SUCCESSFUL_BUILDS = {
	width: '20',
	height: '20',
	active: {
		on_click : Prototype.emptyFunction,
		href : function(element) {return context_path("project/list/passed/" + element.id.replace('_all_successful_builds', ""))},
		src : 'images/icon-all-successful-builds.gif',
		text : "All successful builds"
	},

	inactive: {
		on_click : Prototype.emptyFunction,
		href : function(element) {return "javascript:void(0)"},	
		src : 'images/icon-all-successful-builds-grey.gif',
		text : "All successful builds"
	}
}

BuildProfile.TARGET = ['.force_build_link', '.config_panel_link', '.all_builds_link', '.all_successful_builds_link'];

BuildProfile.prototype = {
	_me : null,
	initialize   : function() {
		_me = this;
	},
	create_links : function() {
		$A(BuildProfile.TARGET).each(function (class_name) {
				_me._create_individual_links(class_name);
			}
		);		
	},
	_create_individual_links: function(class_name) {
	    $$(class_name).each(function(element) {
			eval("_me.create_" + class_name.replace(".", "") + "(element)");
		});
	},
    create_force_build_link : function(element) {
        _me.create_link(element,BuildProfile.FORCE_BUILD, !_me.is_force_build_enabled(element));
    },
    create_config_panel_link : function(element) {
        _me.create_link(element,BuildProfile.CONFIG_PANEL, !_me.is_config_panel_enabled(element));
    },
    create_all_builds_link : function(element) {
        _me.create_link(element, BuildProfile.ALL_BUILDS, _me.is_inactive(element));
    },
	create_all_successful_builds_link : function(element) {
        _me.create_link(element, BuildProfile.ALL_SUCCESSFUL_BUILDS, _me.is_inactive(element));
    },
	create_link : function(element, obj, is_inactive) {
		var link_obj = is_inactive ?  obj.inactive : obj.active;
		element.onclick = link_obj.on_click;
		$(element).immediateDescendants().each(function(elem){$(elem).remove()})
        var img = _me.create_img(link_obj.src, link_obj.text, obj.width, obj.height);
        element.appendChild(img);
        element.href = link_obj.href(element); 
	},
    create_img : function(imgSrc, text, width, height) {
        var img = document.createElement("img");
        img.width = width;
        img.height = height;
        img.title = text;
        img.alt = text;
        img.src = context_path(imgSrc);
        return img;
    },
    is_force_build_enabled : function(element) {
        return $(element).hasClassName('force_build_enabled');
    },
    is_config_panel_enabled : function(element) {
       return $(element).hasClassName('config_panel_enabled');
    },
    is_inactive : function(element) {
        return _me.get_container($(element)).hasClassName('inactive');
    },
    get_container :function(link) {
        return link.ancestors()[0].ancestors()[0];
    },
    force_build : function(elem) {
        if (!elem) return;
        var pars = 'projectName=' + elem.id.replace("_forcebuild", "");
        new Ajax.Updater($('trans_content'), context_path('forcebuild.ajax'), {
            method: 'POST',
            parameters: pars,
            onSuccess: function() {
                new TransMessage('trans_message', _me.get_container(elem), {type:TransMessage.TYPE_NOTICE});
            },
            onFailure: function() {
                new TransMessage('trans_message', _me.get_container(elem), {type:TransMessage.TYPE_ERROR});
            }
        });
    }
}

Event.observe(window, 'load', function() {
    var profile = new BuildProfile();
    profile.create_links();
});
