var Layer = Class.create();

Layer.prototype = {
	width : 0,
	height : 0,
	margin : {w : 22, h : 29},
	contents : null,
	useLayerCordinates : true,

	initialize : function (width, height, contents) {
		this.width = width;
		this.height = height;
		this.contents = contents;
	},

	open : function (callback, x_, y_) {
		var objBody = $$('body')[0];
		var scrolls = this.getScrollSize(window);
		var size = this.getPageSize();
		var div;
		var scr;
		var s = 10;
		var x = 0;
		var y = 0;

		if (this.useLayerCordinates) {
			if (x_ && y_) {
				x = x_ - (this.width / 2);
				y = y_ + s;
			} else {
				x = LayerCordinates['X'] - (this.width / 2);
				y = LayerCordinates['Y'] + s;
			}
		} else {
			x = (scrolls.width - this.width) / 2;
			y = ((scrolls.height - this.height) / 2) + scrolls.top;
		}

		if ($('_LAYER_')) {
			this.close();
		}

		if (this.useLayerCordinates) {
			x = x < s ? s : (x > size.width - (this.width + this.margin.w + s) ? size.width - (this.width + this.margin.w + s) : x);
			y = y > size.height - (this.height + this.margin.h + s) ? size.height - (this.height + this.margin.h + s) : y;
		}

		objBody.appendChild(Builder.node('div', {id : '_LAYER_', style : 'cursor:move; z-index:999;'},
			Builder.node('div', {id : '_LAYER_INIT_', style : 'clear:both; padding:0 10px 10px 10px;'}, [
				Builder.node('div', {id : '_LAYER_HEAD_', style : 'width:100%; height:17px;'},
					Builder.node('img', {id : '_LAYER_HEAD_CLOSE_', src : G_TOP_DIR + '/common/module/layer/images/close.gif', style : 'float:right; margin:5px 1px 0 0; cursor:pointer; width:7px; height:7px;'})
				),
				Builder.node('div', {id : '_LAYER_BODY_'})
			])
		));

		div = $('_LAYER_');
		div.setStyle({
			background : '#EFEFEF',
			display : 'none',
			position : 'absolute',
			left : x + 'px',
			top : y + 'px',
			width : (this.width + this.margin.w) + 'px',
			height : (this.height + this.margin.h) + 'px'
		});

		scr = $('_LAYER_BODY_');
		scr.setStyle({
			background : '#FFFFFF',
			borderColor : '#C4C2C0 #E4E2E0 #E4E2E0 #C4C2C0',
			borderWidth : '1px',
			borderStyle : 'solid',
			width : this.width + 'px',
			height : this.height + 'px'
		});

		$('_LAYER_HEAD_CLOSE_').observe('click', function () { this.close(); }.bind(this));
		$('_LAYER_BODY_').update(this.contents);

		new Effect.Parallel(
		[
			new Effect.Appear(div, {sync : true})
		],
		{
			duration : 0.8,
			afterFinish : (function () {
				div.focus();

				if (Object.isFunction(callback)) {
					callback();
				}
			})
		});

		new Draggable(div, {scroll : window});
	},

	close : function () {
		Element.remove('_LAYER_');
	},

	getPageSize : function () {
		var x, y, w, h;

		if (window.innerHeight && window.scrollMaxY) {
			x = window.innerWidth + window.scrollMaxX;
			y = window.innerHeight + window.scrollMaxY;
		} else if (document.body.scrollHeight > document.body.offsetHeight) {
			x = document.body.scrollWidth;
			y = document.body.scrollHeight;
		} else {
			x = document.body.offsetWidth;
			y = document.body.offsetHeight;
		}

		if (self.innerHeight) {
			if (document.documentElement.clientWidth){
				w = document.documentElement.clientWidth;
			} else {
				w = self.innerWidth;
			}
			h = self.innerHeight;
		} else if (document.documentElement && document.documentElement.clientHeight) {
			w = document.documentElement.clientWidth;
			h = document.documentElement.clientHeight;
		} else if (document.body) {
			w = document.body.clientWidth;
			h = document.body.clientHeight;
		}

		return {width : x < w ? x : w, height : y < h ? h : y};
	},

	getScrollSize : function (obj) {
		var T, L, W, H;

		with (obj.document) {
			if (obj.document.documentElement && documentElement.scrollTop) {
				T = documentElement.scrollTop;
				L = documentElement.scrollLeft;
			} else if (obj.document.body) {
				T = body.scrollTop;
				L = body.scrollLeft;
			}

			if (obj.innerWidth) {
				W = obj.innerWidth;
				H = obj.innerHeight;
			} else if (obj.document.documentElement && documentElement.clientWidth) {
				W = documentElement.clientWidth;
				H = documentElement.clientHeight;
			} else {
				W = body.offsetWidth;
				H = body.offsetHeight
			}
		}

		return {top: T, left: L, width: W, height: H};
	}
}

var LayerCordinates = {};

document.observe('click', function (event) {
	var e = event ? event : window.event;

	LayerCordinates['X'] = Event.pointerX(e);
	LayerCordinates['Y'] = Event.pointerY(e);
});