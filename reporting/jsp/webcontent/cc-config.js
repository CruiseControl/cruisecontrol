/*
 * Take advantage of the fact that objects in JavaScript also serve as hashes to
 * track the state of each node in the tree (collapsed/expanded).
 */
var opened = new Object();

function toggleTreeElements(id) {
    Element.toggle('available-' + id);
    Element.toggle(id + '-hr');
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

function collapseTree(id, treeIcon) {
    toggleTreeElements(id);
    closeIcon(treeIcon);
}

function expandTree(id, treeIcon) {
    toggleTreeElements(id);
    openIcon(treeIcon);
}

function updateAvailablePlugins(id, treeIcon) {
    var isOpened = isOpen(treeIcon);
    return function(request) {
        if (isOpened != true) {
            expandTree(id, treeIcon);
        }
    }
}

function updateConfiguredPlugins(id, treeIcon) {
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
//    new Ajax.Updater('plugin-details', url, {
//        asynchronous: true,
//        method: 'get'
//    });
}

function loadPlugins(url, updateId, onSuccess) {
    removePreviousResultMessages();
    new Ajax.Updater(updateId, url, {
        asynchronous: true,
        method: 'get',
        onSuccess: (updateId == 'plugin-details' ? updateDetails : onSuccess)
    });
}

function loadAvailablePlugins(url, id) {
    var treeIcon = $(id + '-tree-icon');
    var onSuccess = updateAvailablePlugins(id, treeIcon); 
    loadPlugins(url, 'available-' + id, onSuccess);
}

function loadConfiguredPlugins(url, id) {
    var treeIcon = $(id + '-tree-icon');
    var onSuccess = updateConfiguredPlugins(id, treeIcon); 
    loadPlugins(url, id, onSuccess);
}
