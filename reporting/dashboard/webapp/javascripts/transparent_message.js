// Copyright (c) 2006 Sébastien Gruhier (http://xilinus.com, http://itseb.com)
// 
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
// 
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
// VERSION 1.0

Position.windowInfo = function() {
  var windowWidth, windowHeight;
  var pageWidth, pageHeight;
  
  if (window.innerHeight && window.scrollMaxY) {  
    pageWidth = document.body.scrollWidth;
    pageHeight = window.innerHeight + window.scrollMaxY;
  } else if (document.body.scrollHeight > document.body.offsetHeight){ // all but Explorer Mac
    pageWidth = document.body.scrollWidth;
    pageHeight = document.body.scrollHeight;
  } else { // Explorer Mac...would also work in Explorer 6 Strict, Mozilla and Safari
    pageWidth = document.body.offsetWidth;
    pageHeight = document.body.offsetHeight;
  }
  
  if (self.innerHeight) { // all except Explorer
    windowWidth = self.innerWidth;
    windowHeight = self.innerHeight;
  } else if (document.documentElement && document.documentElement.clientHeight) { // Explorer 6 Strict Mode
    windowWidth = document.documentElement.clientWidth;
    windowHeight = document.documentElement.clientHeight;
  } else if (document.body) { // other Explorers
    windowWidth = document.body.clientWidth;
    windowHeight = document.body.clientHeight;
  } 
  var xScroll = document.documentElement.scrollLeft || document.body.scrollLeft;
  var yScroll = document.documentElement.scrollTop || document.body.scrollTop;

  // for small pages with total height less then height of the viewport
  pageHeight = Math.max(windowHeight, pageHeight);

  // for small pages with total width less then width of the viewport
  pageWidth = Math.max(windowWidth, pageWidth);
  return { width: windowWidth, height: windowHeight, pageWidth: pageWidth, pageHeight: pageHeight, xScroll: xScroll, yScroll: yScroll};
}

var TransparentMenu = Class.create();

TransparentMenu.DefaultOptions = {
  top: null,
  left: null,
  showEffect: Effect.Appear,
  showEffectOptions: {duration: 0.2}, 
  hideEffect: Effect.Fade,
  hideEffectOptions: {duration: 0.2},
  showMode: "onload",
  hideMode: "timeout",
  hideDelay: 2,
  insideElement: {id: null, width: "auto", height: "auto"},
  afterElement: null,
  fullscreen: false
}
TransparentMenu.instances = $H();

TransparentMenu.hide = function(id) {
  if (TransparentMenu.instances[id])
    TransparentMenu.instances[id].hide();
}

TransparentMenu.show = function(id) {
  if (TransparentMenu.instances[id]) 
    TransparentMenu.instances[id].show();
  else 
    new TransparentMenu(id,  arguments[1]);
}

TransparentMenu.prototype = {
	initialize: function(id) {
	  this.options  = Object.extend(Object.extend({},TransparentMenu.DefaultOptions), arguments[1] || {});   
	  this.options.insideElement =  Object.extend(Object.extend({},TransparentMenu.DefaultOptions.insideElement), this.options.insideElement || {});   
		this.id = id;

		if (this.options.displayMode == "onload")
	    Event.observe(window, "load", this._init.bindAsEventListener(this));
	  else
	    this._init(null);
	  TransparentMenu.instances[id] = this;
  },
  
  show: function() {
    new this.options.showEffect(this.element, this.options.showEffectOptions);

    // Safari bug fix
    if (navigator.appVersion.match(/Konqueror|Safari|KHTML/))
      setTimeout(this._setPostion.bindAsEventListener(this), 10)

    if (this.options.hideMode == "timeout") 
      setTimeout(this.hide.bindAsEventListener(this), this.options.hideDelay*1000);
    else if (this.options.hideMode == "click" || this.options.hideMode == "mousemove") {
      this.bindEvent = this._startHideEvent.bindAsEventListener(this);
      Event.observe(document.body, this.options.hideMode, this.bindEvent);      
    }    
  },
    
  hide: function() {    
    new this.options.hideEffect(this.element, this.options.hideEffectOptions);
  },
    
  _init: function(event) {
    this.element = $(this.id);
    this._setPostion();
    
    // Get opacity from css if not specify for Fade effect
    if (this.element.getOpacity() && this.options.showEffect == Effect.Appear && ! this.options.showEffectOptions.to)
      this.options.showEffectOptions.to = this.element.getOpacity();
     
    this.show(); 
  },
  
  _setPostion: function() {
    var windowInfo = Position.windowInfo();
    var dim = this.element.getDimensions();

    // Inside an element
    if (this.options.insideElement.id != null) {    
      var relativeElement = $(this.options.insideElement.id);
      var position = Position.cumulativeOffset(relativeElement);
      var dimension = relativeElement.getDimensions();
      this.element.style.left = position[0] + "px";
      this.element.style.top = position[1] + "px";

      if (this.options.insideElement.width == 'auto') {
        this.element.style.width =  dimension.width + "px";
      }
      if (this.options.insideElement.height == 'auto')
        this.element.style.height = dimension.height + "px";
    }
    // Else full screen message
    else if (this.options.fullscreen) { 
      this.element.style.left = "0px";
      this.element.style.top = "0px";
      this.element.style.width = windowInfo.pageWidth + "px";
      this.element.style.height = windowInfo.pageHeight + "px";
    }
    // Relative to body or element
    else {
      var dy = 0;  		
      // Relative to an element
      if (this.options.afterElement != null) {
        var position = Position.cumulativeOffset($(this.options.afterElement));
        dy =  position[1];
        // temporary solution to make transparent message appear over the correct element
        dy -=140;
      }
      // Else to body, add yScroll to have it visible from anywhere
      else
        dy = windowInfo.yScroll;
        
      // Compute left position using user options
      if (this.options.left != null)
        this.element.style.left = windowInfo.xScroll + this.options.left + "px";
      else
        this.element.style.left = windowInfo.xScroll + ((windowInfo.width - dim.width) / 2 ) + "px";
      
      // Compute top position useing user options
      if (this.options.top != null)
        this.element.style.top = dy + this.options.top + "px";
      else
        this.element.style.top = dy + ((windowInfo.height - dim.height) / 2 ) + "px";
    }
  },
  
  _startHideEvent: function() {
    var dim = this.element.getDimensions();
    
    Event.stopObserving(window, this.options.hideMode, this.bindEvent);      
    setTimeout(this.hide.bindAsEventListener(this), this.options.hideDelay*1000);
  }
}
