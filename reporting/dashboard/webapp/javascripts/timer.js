var Timer = Class.create();
Timer.prototype = {
	initialize: function(project_name) {
	    this.elapsed_time = 0;
	    this.project_name = project_name;
	    this.stopped = true;
		var _timer = this
		this.executer = new PeriodicalExecuter(function(){_timer.update()}, 1);
		this.executer.stop();
	},
	last_build_duration:function(build_duration) {
	    this.build_duration = build_duration;
	},
	set_elapsed_time:function(already_elapsed_time) {
		this.elapsed_time = already_elapsed_time;
	},
	get_elapsed_time:function(){
		return this.elapsed_time;
	},
	update:function() {
	    this.elapse();
		this.update_element_content(this.project_name + '_time_elapsed_lable', 'Elapsed ');
		this.update_element_content(this.project_name + '_time_elapsed', this.report_elapsed());
		this.update_element_content(this.project_name + '_time_remaining_lable', ' remaining ');
		this.update_element_content(this.project_name + '_time_remaining', this.report_remaining());
	},
	update_element_content:function(id, text) {
		var element = $(id)
		if(element) {
			if (text != '') {
				element.innerHTML = text;
			} else {
				element.cleanTextNode();
			}
		}
	},
	start:function() {
		this.executer.registerCallback();
	    this.stopped = false;
	},
	stop:function() {
		this.stopped = true;
	    this.elapsed_time=0;
	    this.executer.stop();
	    this.update_element_content(this.project_name + '_time_elapsed', '')
	    this.update_element_content(this.project_name + '_time_remaining', '')
	    this.update_element_content(this.project_name + '_time_elapsed_lable', '')
	    this.update_element_content(this.project_name + '_time_remaining_lable', '')	    
	},
	is_stopped:function() {
		return this.stopped;	
	},
	getPeriodicalExecuter : function() {
	    return this.executer;
	},
	seconds_to_minute : function(sec) {
		return Math.floor(sec/60)
	},
	minutes_to_hr : function(min) {
		return this.seconds_to_minute(min)
	},
	time : function(sec) {
		var min = this.seconds_to_minute(sec);
  	    var hr = this.minutes_to_hr(sec/60);
		return this.digit_pad(hr) + ":" + this.digit_pad(min % 60) + ":" + this.digit_pad(sec % 60);
	},
	elapse : function() {
		this.elapsed_time++;
	},
	report_elapsed : function() {
		return this.time(this.elapsed_time)
	},
	report_remaining : function() {
		var remaining_time = this.build_duration - this.elapsed_time;
		var remaining_str = remaining_time > 0 ? '' : ' longer than last build';
		return this.time(Math.abs(remaining_time)) + remaining_str
	},
	digit_pad : function (digit) {return ((digit > 9)?"":"0") + digit},
	get_project_name : function() {
  		return  this.project_name;
	},
	get_last_build_duration : function() {
		return this.build_duration;
	}
}
