function passed_json(projectName, level) {
	return construct_new_json("passed", "Passed", projectName, level);
}

function building_json(projectName, type, level) {
	return construct_new_json("building_" + type, "Building", projectName, level);
}

function failed_json(projectName, level) {
	return construct_new_json("failed", "Failed", projectName, level);
}

function inactive_json(projectName, level) {
	return construct_new_json("inactive", "Inactive", projectName, level);
}

function construct_new_json(classname, status, projectname, level) {
	return {building_info : {css_class_name : classname, project_name : projectname, building_status : status, level : level}}
}