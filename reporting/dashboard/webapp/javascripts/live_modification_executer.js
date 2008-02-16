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
var LiveModificationExecuter = Class.create();

LiveModificationExecuter.prototype = {
	project_name : null,
	initialize: function(project_name) {
		this.project_name = project_name;
	},
	live_modification : function(project_name) {
		var me = this;
		new Ajax.Request(context_path('getCommitMessage.ajax?project=' + this.project_name), {
	    asynchronous: 1,
	    method:'GET',
	    onSuccess: function(transport) {
				var json = transport.responseText
				json = json ? eval('(' + json + ')') : null;
	        	me._update_live_commit_message(json);

	    	}
		});
	},
	_update_live_commit_message : function(modifications) {
		var modifications_string = "";
        if (!modifications) return;
        if (modifications.length == 0) return;
        $("modification_keys").update("");
        for(var i = 0; i < modifications.length; i++) {
	    	if (modifications[i]) {
                $('modification_keys').insert(this._generate_html(modifications[i]));
            }
	    }
	},
    _generate_html : function(modification) {
        var files = $A(modification.files);
        var trs = "";
        files.each(function(file) {
            trs += "<tr>" +
                    "<td>[rev. " + file.revision + "]</td>" +
                    "<td title='" + file.action +"'><img src='" + context_path('images/table_' + file.action + '.gif') + "' alt='" + file.action + "'> " + file.filename + "</td>"+
                  "</tr>";
        });
        var html = "<table class='modification'>" +
                                   "<tbody>" +
                                       "<tr class='comment'>" +
                                          "<td title='user' class='user'>" + modification.user + "</td>" +
                                          "<td>" + modification.comment + "</td>" +
                                      "</tr>" +
                                      trs
                                "</tbody>" +
                    "</table>";
        return html;
    }

}
