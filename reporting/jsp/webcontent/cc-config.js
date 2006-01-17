/*
 * Take advantage of the fact that objects in JavaScript also serve as hashes to
 * track the state of each node in the tree (collapsed/expanded).
 */
var opened = new Object();

function toggleTreeElements(id) {
    Element.toggle(id);
}

function isOpen(icon) {
    return opened[icon.id];
}

function closeIcon(icon) {
    icon.src = 'images/plus_nolines.gif';
    opened[icon.id] = false;
}

function openIcon(icon) {
    icon.src = 'images/minus_nolines.gif';
    opened[icon.id] = true;
}

/*
 * Expects forms named like "load-bootstrapper" and "load-sourcecontrol"
 */
function getPluginTypeFromFormName(name) {
    return /load-(.*)/.exec(name)[1];
}

function getSelectedValue(select) {
    return select[select.selectedIndex].value;
}

function buildUrlForForm(form, urlBase) {
    var name = form.name;
    var select = $('select-' + getPluginTypeFromFormName(name));
    return urlBase + '&pluginName=' + getSelectedValue(select);
}

/*
 * Since we've set the height of the plugin details iframe to a relative value (100% of its parent),
 * the simplest way to resize it is by just reloading.
 */
function resizeDetails() {
    var details = $('plugin-details');
    var url = details.src;
    details.src = url;
}

function collapseTree(id, treeIcon) {
    toggleTreeElements(id);
    closeIcon(treeIcon);
    resizeDetails();
}

function expandTree(id, treeIcon) {
    toggleTreeElements(id);
    openIcon(treeIcon);
    resizeDetails();
}

function updatePlugins(id, treeIcon) {
    var isOpened = isOpen(treeIcon);
    return function(request) {
        if (isOpened != true) {
            expandTree(id, treeIcon);
        } else {
            collapseTree(id, treeIcon);
        }
    }
}

/*
 * Details don't need to be "updated" on success
 */
function updateDetails(request) {
}

function removePreviousResultMessages() {
    var resultMessages = $('result-messages');
    if (resultMessages) {
        Element.remove(resultMessages);
    }
}

function loadPlugin(url) {
    removePreviousResultMessages();
    $('plugin-details').src = url;
}

function loadPlugins(url, id) {
    removePreviousResultMessages();
    var treeIcon = $(id + '-tree-icon');
    new Ajax.Updater(id, url, {
        asynchronous: true,
        method: 'get',
        onSuccess: (id == 'plugin-details' ? updateDetails : updatePlugins(id, treeIcon))
    });
}
