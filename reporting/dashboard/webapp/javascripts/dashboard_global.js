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

function context_path(relative_path) {
    return contextPath + "/" + relative_path
}
