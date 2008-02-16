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
var StatisticsObserver = Class.create();
StatisticsObserver.prototype = {
	initialize:function() { },
	notify : function(json) {
		if (!json.length) return;
	    if (json.length == 0) return; 
		var statistics = $H({passed:0,failed:0,building:0,inactive:0,discontinued:0})
		for (var i = 0; i < json.length; i++) {
	    	this.category(json[i], statistics);
	    }
		this.calculate(statistics);
	  	this.update(statistics);
	},
	calculate : function (statistics, tag) {
		var total = 0;
		statistics.each(function(pair){
			if (pair.key != "inactive" && pair.key != "discontinued" ) {
				total += pair.value;
			}
		});
		statistics.set("total", total);
	    var rate = ((statistics.get('passed') / total) * 100).toFixed(0);
        statistics.set('rate', isNaN(rate) ? "0%" : rate+"%");
		return statistics; 
	},
	category : function (json, statistics) {
		if (!json) return;
		if (!json.building_info) return;
		if (!json.building_info.current_status) return;
		var status = is_inactive(json) ? 'inactive' : json.building_info.current_status.toLowerCase();
		if (status == 'building' || status == 'discontinued' || status == 'inactive') {
			statistics.set(status, statistics.get(status) + 1);
		} else {
            var previous_result = json.building_info.previous_result.toLowerCase();
            statistics.set(previous_result, statistics.get(previous_result) + 1);
		}
	},
	update : function (statistics_infos) {
		var infos = $A(['passed', 'failed', 'building', 'total', 'rate', 'inactive', 'discontinued'])
		infos.each(function(info) {
		    var statistic = $('statistics_' + info).innerHTML;
            $('statistics_' + info).update(statistics_infos.get(info) + statistic.substring(statistic.indexOf(' ')));
		});
	}
}
