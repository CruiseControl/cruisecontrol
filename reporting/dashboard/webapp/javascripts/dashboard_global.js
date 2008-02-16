function context_path(path_info) {
    return contextPath + "/" + path_info
}

ACTIVE_STATUS = $A(['passed', 'failed', 'inactive', 'discontinued', 'paused', 'queued',
				 'building_passed', 'building_failed', 'building_unknown', 'unknown',
				 'level_0', 'level_1', 'level_2', 'level_3', 'level_4', 'level_5', 'level_6', 'level_7', 'level_8']);

function clean_active_css_class_on_element(element) {
	ACTIVE_STATUS.each(function(status) {
	    Element.removeClassName($(element), status);
	    Element.removeClassName($(element), 'build_profile_' + status);
    })
}


function is_inactive(json) {
	return json.building_info.current_status.toLowerCase() != 'building'
			&& json.building_info.previous_result.toLowerCase() == 'unknown';
}

function is_previous_unknown(json) {
    return json.building_info.previous_result.toLowerCase() == 'unknown';
}

function is_discontinued(json) {
	return json.building_info.current_status.toLowerCase() == 'discontinued';
}

function is_paused(json) {
	return json.building_info.current_status.toLowerCase() == 'paused';
}

function is_building(json) {
	return json.building_info.current_status.toLowerCase() == 'building';
}

function is_globally_disabled() {
    return !global_force_build_enabled;
}

function should_forcebuild_be_disabled(json) {
    return is_globally_disabled() || is_discontinued(json) || is_paused(json) || is_building(json);
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