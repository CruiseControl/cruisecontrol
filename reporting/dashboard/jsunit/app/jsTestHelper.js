function passed_json(projectName, level) {
	return construct_new_json(projectName, level, "Waiting", "Passed");
}

function building_json(projectName, type, level) {
	return construct_new_json(projectName, level, "Building", "Passed");
}

function failed_json(projectName, level) {
	return construct_new_json(projectName, level, "Waiting", "Failed");
}

function inactive_json(projectName, level) {
	return construct_new_json(projectName, level, "Waiting", "Unknown");
}

function discontinued_json(projectName, level) {
	return construct_new_json(projectName, level, "Discontinued", "Failed");
}

function paused_json(projectName, level) {
    json = construct_new_json(projectName, level, "Paused", "Passed");
    json.building_info.paused_class_name = "paused";
    return json;
}

function construct_new_json(projectname, level, current_status, previous_result) {
    return {building_info :
        {project_name : projectname,
         level : level,
         latest_build_date : "1 day ago",
         current_status : current_status,
         previous_result : previous_result}}
}