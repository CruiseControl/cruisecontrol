function evaluate_time_to_seconds(time) {
	if (!time) return;
	time = time.replace("second", "", "gi");
	time = time.replace("minute", "*60+", "gi");
	time = time.replace("hour", " *3600+" , "gi");
	time = time.replace(/s/gi, "");
	time = time.replace(/\+$/gi, "");
	try{
		return eval(time) - 0
	} catch (err) {
		return 0
	}
}

function eval_timer_object(project_name, build_status, build_duration, elapsed_time) {
	var project_timer_var = project_name + '_timer';
	project_timer_var = project_timer_var.replace(/ /gi, "_");
	var timer = null;
	try {
		timer = eval(project_timer_var);
	} catch(err) {
		var expression =  project_timer_var + ' = new Timer("' + project_name + '")'
		timer = eval(expression);
	}
	if (!timer) return timer;
	var is_building = (build_status.toLowerCase() == 'building')
	if (is_building && timer.is_stopped()) {
		timer.set_elapsed_time(elapsed_time);
		timer.last_build_duration(build_duration);
		timer.start();
	}
	if (!is_building){
		timer.stop();
	}
	return timer;
}

function context_path(relative_path) {
    return contextPath + "/" + relative_path
}

function disable_bubble(e) {
	if (!e) var e = window.event;
	e.cancelBubble = true;
	if (e.stopPropagation) e.stopPropagation();
}
