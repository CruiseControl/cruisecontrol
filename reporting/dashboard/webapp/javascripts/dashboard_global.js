function evaluate_time_to_seconds(time) {
	if (!time) return;
	time = time.replace("seconds", "", "gi");
	time = time.replace("minutes", "*60+", "gi");
	time = time.replace("hours", " *3600+" , "gi");
	time = time.replace(/\+$/gi, "");
	try{
		return eval(time) - 0
	} catch (err) {
		return 0
	}
}

function join_root_with_context(port, context) {
    return "http://" + window.location.hostname + ":" + port + "/" + context;
}

function context_path(relative_path) {
    return contextPath + "/" + relative_path
}
