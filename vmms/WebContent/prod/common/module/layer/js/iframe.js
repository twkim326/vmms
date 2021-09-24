var IFrame = Class.create();
var __objIFrame__ = null;

IFrame.prototype = {
	width : 0,
	height : 0,
	margin : {w : 0, h : 0},
	src : null,
	x : 0,
	y : 0,

	initialize : function (width, height, src) {
		this.width = width;
		this.height = height;
		this.src = src;
	},

	open : function (x, y) {
		var objBody = $$('body')[0];
		var scrolls = this.getScrollSize(window);
		var size = this.getPageSize();
		var div;
		var frm;
		var img;

		this.close();
		this.x = x ? x : (scrolls.width - this.width - 10) / 2;
		this.y = y ? y : ((scrolls.height - this.height - 10) / 2) + scrolls.top;

		if (this.x < 0) {
			this.x = 0;
		}

		if (this.y < 0) {
			this.y = 0;
		}

		objBody.appendChild(Builder.node('div', {id : '_OVERS_', style : 'position:absolute; width:' + size.width + 'px; height:' + size.height + 'px; left:0; top:0; background-color:#000000; display:none; z-index:998;'}));

		new Effect.Appear('_OVERS_', {
			duration : 0.1,
			from : 0.0,
			to : 0.5,
			afterFinish : (function () {
				$('_OVERS_').observe('click', function () { this.close(); }.bind(this));

				objBody.appendChild(Builder.node('div', {id : '_LAYER_', style : 'z-index:999;'}, [
					Builder.node('img', {id : '_LAYER_LOAD_', src : G_TOP_DIR + '/common/module/layer/images/loading.gif', style : 'margin:' + Math.abs((this.height - 22) / 2) + 'px 0 0 ' + Math.abs((this.width - 22) / 2) + 'px'}),
					Builder.node('iframe', {id : '_LAYER_BODY_', src : this.src, frameBorder : 0, scrolling : 'no', style : 'display:none'})
				]));

				div = $('_LAYER_');
				div.setStyle({
					background : '#FFFFFF',
					borderColor : '#FFFFFF',
					borderWidth : '5px',
					borderStyle : 'solid',
					display : 'none',
					position : 'absolute',
					left : this.x + 'px',
					top : this.y + 'px',
					width : (this.width + this.margin.w) + 'px',
					height : (this.height + this.margin.h) + 'px'
				});

				img = $('_LAYER_LOAD_');
				frm = $('_LAYER_BODY_');
				frm.setStyle({
					background : '#FFFFFF',
					marginWidth : 0,
					marginHeight : 0,
					leftMargin : 0,
					topMargin : 0,
					width : this.width + 'px',
					height : this.height + 'px'
				});
				frm.observe('load', function () {
					img.hide();
					frm.show();

					var w = frm.contentWindow.document.body.scrollWidth;
					var h = frm.contentWindow.document.body.scrollHeight;

					if (this.width < w && this.height < h) {
						this.resize(w, h);
					} else if (this.width < w) {
						this.resize(w, this.height);
					} else if (this.height < h) {
						this.resize(this.width, h);
					}

//					frm.contentWindow.document.oncontextmenu = new Function("return false");
//					frm.contentWindow.document.ondragstart = new Function("return false");
//					frm.contentWindow.document.onselectstart = new Function("return false");
				}.bind(this));

				new Effect.Parallel(
				[
					new Effect.Appear(div, {sync : true})
				],
				{
					duration : 0,
					afterFinish : (function () {
						div.focus();
					})
				});

//				new Draggable(div, {scroll : window});
			}.bind(this))
		});

		__objIFrame__ = this;
	},

	close : function () {
		try {
			$('_LAYER_').remove();
			$('_OVERS_').remove();
		} catch (e) {
		}
	},

	resize : function (w, h) {
//		$('_LAYER_').setStyle({width : (w + this.margin.w) + 'px', height : (h + this.margin.h) + 'px'});
//		$('_LAYER_BODY_').setStyle({width : w + 'px', height : h + 'px'});
		$('_LAYER_').morph('width : ' + (w + this.margin.w) + 'px; height : ' + (h + this.margin.h) + 'px');
		$('_LAYER_BODY_').morph('width : ' + w + 'px; height : ' + h + 'px');

		var s = this.getPageSize();
		var o = {width : this.x + (w + 10) + this.margin.w * 2, height : this.y + (h + 10) + this.margin.h * 2};

		$('_OVERS_').setStyle({width : (s.width > o.width ? s.width : o.width) + 'px', height : (s.height > o.height ? s.height : o.height) + 'px'});
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
	}
}