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
var BuildDetailObserver = Class.create();

BuildDetailObserver.prototype = {
    initialize: function(project_name) {
        this.project_name = project_name;
        this.is_timer_observer_notified = false;
        this.start_line_number = 0;
    },
    notify : function(jsonArray) {
        for (var i = 0; i < jsonArray.length; i++) {
            if (!jsonArray[i]) return;
            if (jsonArray[i].building_info.project_name == this.project_name) {
                this._notify(jsonArray[i]);
            }
        }
    },
    _notify : function(json) {
        var is_output_empty = this.update_live_output();
        if (!this.is_timer_observer_notified) {
            timer_observer.notify([json]);
            this.is_timer_observer_notified = true;
        }
        if (this._is_build_finished(json, is_output_empty)) {
            this.update_page(json);
        }
        ;
    },
    update_live_output : function() {
        var is_output_empty = false;
        var _this = this;
        var ajaxRequest = new Ajax.Request(context_path('getProjectBuildOutput.ajax'), {
            asynchronous:false,
            method: 'GET',
            parameters: 'project=' + _this.project_name + '&start=' + _this.start_line_number,
            onSuccess: function(transport, next_start_as_json) {
                if (next_start_as_json) {
                    _this.start_line_number = next_start_as_json[0];
                    var build_output = transport.responseText;
                    is_output_empty = _this._update_live_output(build_output);
                } else
                {
                    is_output_empty = true;
                }

            }
        });
        return is_output_empty;
    },
    _update_live_output: function (build_output) {
        var is_output_empty = false;
        if (!build_output) {
            is_output_empty = true;
        } else
        {
            is_output_empty = false;
            $('buildoutput_pre').insert(build_output.replace(/\n/g, "<br>"));
        }
        return is_output_empty;
    },
    _is_build_finished : function(json, is_output_empty) {
        var current_status = json.building_info.current_status.toLowerCase();
        return current_status != "building" && is_output_empty;
    },
    update_page : function(json) {
        this.update_build_detail_summary_panel(json);
        this.display_error_message_if_necessary(json);
        this.update_config_panel_icon();
        timer_observer.notify([json]);
    },
    display_error_message_if_necessary : function(json) {
        if (is_previous_unknown(json)) {
            var text = $$WordBreaker.break_text(logRoot + json.building_info.project_name);
            $('trans_content').update("Failed to find log in <br/>" + text);
            new TransMessage('trans_message', $('build_detail_summary_container'), {type:TransMessage.TYPE_ERROR, autoHide:false, height:50});
        }
    },
    update_build_detail_summary_panel : function (json) {
        var previous_status = json.building_info.previous_result.toLowerCase();
        $$('.build_detail_summary h3')[0].innerHTML = this.project_name
                + " <span class='build_status'>" + previous_status + "</span> (<a>see details</a>)"
        $$('.build_detail_summary h3')[0].select("A")[0].writeAttribute("href", context_path("tab/build/detail/" +
                                                                                             this.project_name));
        var div = $$('.build_detail_summary')[0].ancestors()[0]
        clean_active_css_class_on_element(div);
        $(div).addClassName(previous_status.toLowerCase());
        reround(div);
    },
    update_config_panel_icon : function() {
        var control_panel = $$(".build_detail_summary .control_panel")[0];
        if (control_panel) {
            control_panel.show();
        }
    }
}