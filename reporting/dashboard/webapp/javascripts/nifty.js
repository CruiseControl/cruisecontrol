function NiftyCheck(){
if(!document.getElementById || !document.createElement)
    return(false);
var b=navigator.userAgent.toLowerCase();
if(b.indexOf("msie 5")>0 && b.indexOf("opera")==-1)
    return(false);
return(true);
}

function _round(node, bk, color, size){
    AddTop(node,bk,color,size);
    AddBottom(node,bk,color,size);
}

function round(selector){
	var array = $A($$(selector));
	array.each(function(node) {
		_round(node, outsideColor(node.parentNode), insideColor(node));
	});
}

function round_top(selector){
	var array = $A($$(selector));
	array.each(function(node) {
		AddTop(node, outsideColor(node.parentNode), insideColor(node));
	});
}

function reround(node) {
	remove_corners(node);
	_round(node,outsideColor(node.parentNode),insideColor(node));
}

function remove_corners(node) {
	$A(node.childNodes).each (function (ele) {
		if (ele.tagName == 'B')
			Element.remove(ele);
	});
}

function outsideColor(node){
	return getColor(node,
		function(finishedNode) {
			return finishedNode.tagName.toLowerCase() == 'body';
		},
		function(nextNode) {
			return nextNode.parentNode;
		}
	);
}

function insideColor(node){
	return getColor(node,
		function(finishedNode) {
			return finishedNode.immediateDescendants().size == 0;
		},
		function(nextNode) {
			return nextNode.immediateDescendants()[0];
		}
	);
}

function getColor(node, finished, next) {
	if (!node) return 'white';
	if (finished(node)) return 'white';
	var color = Element.getStyle(node, 'background-color');
	//safari will return rgba(0,0,0,0) for the transparent color; 
	if (color == null || color == "transparent" || color.indexOf("rgba") > -1) {
		return getColor(next(node), finished, next);
	}
	return color;
}

function RoundedTop(selector,bk,color,size){
var i;
var v=getElementsBySelector(selector);
for(i=0;i<v.length;i++)
    AddTop(v[i],bk,color,size);
}

function RoundedBottom(selector,bk,color,size){
var i;
var v=getElementsBySelector(selector);
for(i=0;i<v.length;i++)
    AddBottom(v[i],bk,color,size);
}

function AddTop(el,bk,color,size){
if ($(el).hasClassName("dynamic_width")) {
	$(el).setStyle({
	  width: $(el.immediateDescendants()[0]).getDimensions().width
	});
};
var i;
var d=document.createElement("b");
var cn="r";
var lim=4;
if(size && size=="small"){ cn="rs"; lim=2}
d.className="rtop";
d.style.backgroundColor=bk;


for(i=1;i<=lim;i++){
    var x=document.createElement("b");
    x.className=cn + i;
    x.style.backgroundColor=color;
    d.appendChild(x);
    }
el.insertBefore(d,el.firstChild);
}

function AddBottom(el,bk,color,size){
var i;
var d=document.createElement("b");
var cn="r";
var lim=4;
if(size && size=="small"){ cn="rs"; lim=2}
d.className="rbottom";
d.style.backgroundColor=bk;
for(i=lim;i>0;i--){
    var x=document.createElement("b");
    x.className=cn + i;
    x.style.backgroundColor=color;
    d.appendChild(x);
    }
el.appendChild(d,el.firstChild);
}

function getElementsBySelector(selector){
	return $$(selector);
}

Event.observe(window, 'load', function() {
  round(".round_corner");
});

Event.observe(window, 'load', function() {
  round_top(".round_top");
});
