Tooltip.visible_tooltip = null;
Tooltip.scheduled_task_ids = $A([]);


Tooltip.prototype.hideTooltip = function(event){
	var me = this;
	Tooltip.scheduled_task_ids.push(setTimeout(function(){me.tool_tip.hide()}, 2000));
}

var destroy_orig = Tooltip.prototype.destroy;
var registerEvents_orig = Tooltip.prototype.registerEvents;
var showTooltip_orig = Tooltip.prototype.showTooltip;

Tooltip.prototype.destroy = function() {
	Event.stopObserving(this.tool_tip, "mouseover", this.cancelScheduledTask);
	Event.stopObserving(this.tool_tip, "mouseout", this.eventMouseOut);
    destroy_orig.apply(this);
}

Tooltip.prototype.registerEvents = function() {
	Event.observe(this.tool_tip, "mouseover", this.cancelScheduledTask);
	Event.observe(this.tool_tip, "mouseout", this.eventMouseOut);
    registerEvents_orig.apply(this);
}

Tooltip.prototype.cancelScheduledTask = function() {
	Tooltip.scheduled_task_ids.each(function(id) {
		clearTimeout(id);
	})
}

Tooltip.prototype.showTooltip = function(event) {
	this.cancelScheduledTask();
	if (!Tooltip.visible_tooltip) {
		Tooltip.visible_tooltip = this;	
	} else if (Tooltip.visible_tooltip != this) {
		if (Tooltip.visible_tooltip.tool_tip) {
			Tooltip.visible_tooltip.tool_tip.hide();	
		}
		Tooltip.visible_tooltip = this;
	}
    this.tool_tip.clonePosition(this.element, {offsetTop:150,
                                               offsetLeft:35,
                                               setLeft:true,
                                               setTop:true,
                                               setWidth:false,
                                               setHeight:false}
     );
    this.tool_tip.show();
}
