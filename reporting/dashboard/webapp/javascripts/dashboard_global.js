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

function escape_project_name(project_name){
    var md5 = hex_md5(project_name)
    return "_" + md5
    //we can return something like below to provide projct name information for debugging.
    //return project_name.replace(/[ -@#\.\*\+\\]/gi, "_") + md5
}

function eval_timer_object(project_name, build_status, build_duration, elapsed_time) {
	var project_timer_var = escape_project_name(project_name) + '_timer';
	var timer = null;
	try {
		timer = eval(project_timer_var);
	} catch(err) {
		var expression =  project_timer_var + ' = new Timer("' + project_name + '")'
		timer = eval(expression);
	}
	if (!timer) return;
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

function context_path(path_info) {
    return contextPath + "/" + path_info
}

function disable_bubble(e) {
	if (!e) var e = window.event;
	e.cancelBubble = true;
	if (e.stopPropagation) e.stopPropagation();
}

Element.addMethods({
  cleanTextNode: function(element) {
    element = $(element);
    var node = element.firstChild;
    while (node) {
      var nextNode = node.nextSibling;
      if (node.nodeType == 3)
        element.removeChild(node);
      node = nextNode;
    }
    return element;
  }
});