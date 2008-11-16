/**********************************************************************
 * This is a script to automatically add the "oddrow" class to every
 * second table row in the body of a table with a class of
 * "documentation". With the corresponding css declarations, this can
 * be used to display selected tables with rows in alternating colors,
 * without having to manually maintain the class of each row (which
 * can become tedious for large tables).
 *
 * To use this script, include this file in the head section of a html
 * file, by adding a line similar to the following:
 *    <script type="text/javascript" src="tables.js"></script> 
 *
 * This script was developed based upon the description, example code
 * and comments of the "Zebra Tables" article of "A List Apart":
 *    http://www.alistapart.com/articles/zebratables/
 *
 * Joe Schmetzer
 * http://www.exubero.com/
 *********************************************************************/
 
onload = stripeDocTables;

/**
 * Search for all tables with the class of "documentation", and pass
 * them to the stripeTable function.
 */
function stripeDocTables() {
    if( !document.getElementsByTagName ) return false;

    var tables = document.getElementsByTagName("table");
    for( var i = 0; i < tables.length; i++ ) {
        var table = tables[i];
        if( hasClass(table, "documentation") ) {
            stripeTable( table );
        }
    }
}

/**
 * Given a table, find all rows dirctly in the tbody (avoiding nested
 * tables), and appending the "oddrow" class to every second row.
 */
function stripeTable(table) {
    var oddrow = true;

    var bodies = getChildElementsByTagName(table, "tbody");
    for( var i = 0; i < bodies.length; i++ ) {
        var rows = getChildElementsByTagName(bodies[i], "tr");
        for (var j = 0; j < rows.length; j++) {
            if( oddrow ) {
                rows[j].className += " oddrow";
            }
            oddrow = !oddrow;
        }
    }
}

/**
 * Returns true if the given object has the specified class name.
 * This method is used to work around an IE bug.
 */
function hasClass(obj, className) {
    var result = false;
    if (obj.getAttributeNode("class") != null) {
        var classList = obj.getAttributeNode("class").value;
        result = classList.indexOf(className) >= 0;
    }
    return result;
}

var ELEMENT_NODE = 1;

/**
 * Returns the direct children of the given object with the specified
 * element name.
 */
function getChildElementsByTagName(obj, tagname) {
    tagname = tagname.toUpperCase();

    var matching = [];
    var child = obj.firstChild;
    while( child ) {
        if( child.nodeType == ELEMENT_NODE &&
            (child.nodeName.toUpperCase() == tagname)) {
            matching.push(child);
        }
        child = child.nextSibling;
    }

    return matching;
}
