var closeIcon = 'images/plus_nolines.gif';
var openIcon = 'images/minus_nolines.gif';

/*
Since objects in JS are hashes, this is being used to track the state of each node in the tree.
*/
var opened = new Object();

function addMessage(domElementID, message) {
    $(domElementID).innerHTML = message;
}

/*
This function handles toggling the +/- icon and handles hiding the subelement in the tree
*/
function updater(domElementID, icon) {

    //details don't have an associated icon, so just ignore it
    if (domElementID == "plugin-details") {
        return function(request){}
    }

    var isOpened = opened[icon.id];
    return function(request) {
        Element.toggle(domElementID);
        icon.src = (isOpened ? closeIcon : openIcon);
        opened[icon.id] = (isOpened ? false : true);
    }
}


/*
Defines the AJAX call to ansyncronoushly update the tree/details view
*/
function loadPlugins(url, domElementID) {
    var icon = $(domElementID + '-icon');
    new Ajax.Updater(domElementID, url, {
        asynchronous: true,
        method: 'get',
        onSuccess: updater(domElementID, icon),
        onFailure: function(request) {
            addMessage(domElementID + '-errors', 'Error loading ' + domElementID + '!');
        }
    });
}
