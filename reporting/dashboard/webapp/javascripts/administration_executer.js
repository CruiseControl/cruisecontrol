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
var AdministrationExecuter = Class.create();

AdministrationExecuter.prototype = {
	initialize: function(btn){
		this.btn = btn;
	},
	check_out_project : function() {
	    var url = $('url').value;
	    var projectName = $('projectName').value;
	    this.btn.disabled = true;
		var _executer = this;
	    $('url_icon').src = context_path("images/wait.gif");
	    new Ajax.Request(context_path('addProjectFromVersionControl.ajax'), {
	        parameters: "url=" + url + "&projectName=" + projectName + "&vcsType=" + $('vcsType').value + "&moduleName=" + $('moduleName').value,
	        asynchronous:1,
	        method: 'GET',
	        onComplete: function(transport) {
		     	var json = transport.responseText
			    json = json ? eval('(' + json + ')') : null
	            _executer.ajax_update_icons_and_invoke_callback_function(json);
	        }
	    });
	},
	ajax_update_icons_and_invoke_callback_function : function (json) {
	    $('url_icon').src = '';
	    $('projectName_icon').src = '';
		var eval_method = "this." + json.result.ok + "(json)"
	    eval(eval_method);
		this.btn.disabled = false;
	},
	success : function (json) {
		$('url_icon').src = context_path('images/accept.gif');
        $('projectName_icon').src = context_path("images/accept.gif");
        $('moduleName_icon').src = context_path("images/accept.gif");
        $('vcError').update("");
        $('vc_success_info').update("Project added. " + json.result.response);
        new TransparentMenu('vc_success_info',{insideElement : {id:'trans_message_container', width:'500px', height:'40px'}, displayMode:'now', hideDelay:25, hideMode:'timeout'})
	},
	failure : function (json) {
		var icon_id = json.result.field + '_icon';
        $(icon_id).src = context_path("images/exclamation.gif");
        $('vc_success_info').innerHTML = "";
        $('vcError').update(json.result.response);
	}
};

Event.observe(window, 'load', function() {
	if (!$('add_project_btn')) return;
    Event.observe($('add_project_btn'), 'click', function() {new AdministrationExecuter(this).check_out_project()});
});
