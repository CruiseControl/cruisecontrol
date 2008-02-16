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
var WordBreaker = Class.create();

WordBreaker.prototype = {
	initialize : function(unit) {
		this.unit = unit;
		_inserter = this;
	},
	insert: function() {
		$$('.wbrSensitive').each(this.word_break);
	},
	word_break: function(element) {
		var html_in_element = element.innerHTML ? element.innerHTML.toLowerCase() : '';
		if (html_in_element.indexOf(_inserter.word_break_element()) > -1) {
			return;
		}
        element.update(_inserter.break_text(element.innerHTML));
    },
    break_text : function(text) {
        var textArray = text.toArray();
        if (!textArray) return;
        var content = '';
        for(var i = 0; i < textArray.length;i++) {
            if ((i + 1) % _inserter.unit == 0) {
                content += (textArray[i] + _inserter.word_break_element());
            } else {
                content += textArray[i];
            }
        }
        return content;
    },
    word_break_element: function() {
		return Prototype.Browser.Gecko ? '<wbr>' : '&shy;';
	}
}
var $$WordBreaker = new WordBreaker(5);

Event.observe(window, 'load', function() {
	$$WordBreaker.insert();
});

