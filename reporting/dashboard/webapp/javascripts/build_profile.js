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

BuildProfile.prototype = {
	_me : null,
	initialize   : function(){
		_me = this;
	},
	force_build_enabled : {
		on_click : function() {new BuildProfile().force_build(this)},
		href : "javascript:void(0)",
		src : 'images/icon-force-build.gif',
		text : "Force build"
	},
	force_build_disabled : {
		on_click : Prototype.emptyFunction,
		href : null,
		src : 'images/icon-force-build-grey.gif',
		text : "Force build (disabled)"
	},
	create_links : function() {
		var _profile = this;
		$$('.force_build_link').each(function(element) {
			var forceBuild = _me.choose_force_build(element);
			element.onclick = forceBuild.on_click;
			var img = _me.create_img(forceBuild.src, forceBuild.text);
			element.appendChild(img);
			if (forceBuild.href) {
				element.href = forceBuild.href; 
			}
		})
	},
	create_img : function(imgSrc, text) {
		var img = document.createElement("img");
		img.width = '20';
		img.height = '20';
		img.title = text;
		img.alt = text;
		img.src = context_path(imgSrc);
		return img;
	},
	is_force_build_enabled : function(element) {
		return element.hasClassName('force_build_enabled');
	},
	is_inactive : function(element) {
		return _me.get_container(element).hasClassName('inactive');
	},
	get_container :function(link) {
		return $(link.id.replace("_forcebuild", "_profile"));
	},
	choose_force_build : function(element) {
		if (_me.is_force_build_enabled(element) && !_me.is_inactive(element)) {
			return _me.force_build_enabled;
		} else {
			return _me.force_build_disabled;
		}		
	},
	force_build : function(elem) {
		if (!elem) return;
	    var pars = 'projectName=' + elem.id.replace("_forcebuild", "");
		new TransparentMenu('trans_message',{afterElement: _me.get_container(elem), displayMode:'now', hideDelay:3, hideMode:'timeout', top:2, left:null})
	    new Ajax.Request('forcebuild.ajax', { method: 'GET', parameters: pars });
	}
}

Event.observe(window, 'load', function() {new BuildProfile().create_links()});
