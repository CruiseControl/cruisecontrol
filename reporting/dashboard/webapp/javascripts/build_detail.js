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
var BuildDetail = Class.create();

BuildDetail.tree_navigator = function (element, url) {
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

BuildDetail.toggle_tab_content = function (element){
	var ul = $(element).ancestors()[0];
	var childElements = $A(ul.immediateDescendants())
	childElements.each(function(elem) {
		$(elem).removeClassName('current_tab');
		var div = BuildDetail.get_corresponding_content_element($(elem));
		if (div) {
			div.hide()
		}
	});
	$(element).addClassName('current_tab');
	var div = BuildDetail.get_corresponding_content_element($(element));
	if (div) {
		div.show()
	}
}

BuildDetail.get_corresponding_content_element = function (li_element) {
	var spans = $A($(li_element).getElementsBySelector('span'));
	if (spans.size() > 0) {
        var id = spans.first().innerHTML.toLowerCase().replace(/ /ig, "_");
        return $(id);
	}
}

Event.observe(window, 'load', function() {
    $$('.tab_toggle').each(function (elem) {
		elem.onclick = function() {BuildDetail.toggle_tab_content(this)};		
	})
    $$('.collapsible_title').each(function (elem) {
		elem.observe('click', function () {toggle_test_detail(this)});
	})
});

function toggle_test_detail(title) {
    var test_content = $(title).nextSiblings()[0];
    test_content.visible() ? _hide_content(title, test_content) : _show_content(title, test_content);
}

function expand_all() {
    $$('.collapsible_content').each(function (elem) {
        var title = elem.previousSiblings().first();
        _show_content(title, elem);
    })
}

function collapse_all() {
    $$('.collapsible_content').each(function (elem) {
        var title = elem.previousSiblings().first();
        _hide_content(title, elem);
    })
}

function _show_content(title, content) {
    title.removeClassName("title_message_collapsed");
    title.addClassName("title_message_expanded");
    content.show();
}

function _hide_content(title, content) {
    title.removeClassName("title_message_expanded");
    title.addClassName("title_message_collapsed");
    content.hide();
}