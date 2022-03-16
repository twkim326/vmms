var Calendar = {
	topDir : G_TOP_DIR,
	cms : null,
	y : 0,
	m : 0,
	ymd : null,
	callback : null,
	div : null,

	show : function (x, y) {
		var objBody = $$('body')[0];
		var scrolls = this.getScrollSize(window);
		var width = 336;
		var height = 240;
		var left = x ? x : (scrolls.width - width) / 2;
		var top = y ? y : ((scrolls.height - height) / 2) + scrolls.top;

		this.hide();

		objBody.appendChild(Builder.node('div', {id : '_calendar_', style : 'cursor:move; z-index:999;'},
			Builder.node('div', {id : '_c_init_', style : 'clear:both; padding:0 10px 10px 10px;'}, [
				Builder.node('div', {id : '_c_head_', style : 'width:100%; height:17px;'},
					Builder.node('img', {id : '_c_head_close_', src : this.topDir + '/common/module/calendar/images/close.gif', style : 'float:right; margin:5px 1px 0 0; cursor:pointer; width:7px; height:7px;'})
				),
				Builder.node('div', {id : '_c_body_', style : 'width:100%; padding:1px; background-color:#FFFFFF; border-color:#C4C2C0 #E4E2E0 #E4E2E0 #C4C2C0; border-width:1px; border-style:solid;'})
			])
		));

		$('_c_head_close_').observe('click', function () { this.hide(); }.bind(this));

		this.div = $('_calendar_');
		this.div.setStyle({
			background : '#EFEFEF',
			display : 'none',
			position : 'absolute',
			left : (left < 0 ? 0 : left) + 'px',
			top : (top < 0 ? 0 : top) + 'px',
			width : width + 'px'
		});

		this.update();

		new Effect.Parallel(
		[
			new Effect.Appear(this.div, {sync : true})
		],
		{
			duration : 0.8,
			afterFinish : (function () {
				this.div.focus();
			}.bind(this))
		});

		new Draggable(this.div, {scroll : window});
	},

	hide : function () {
		try { this.div.remove(); } catch (e) {}
	},

	open : function (field, callback, x, y, v) {
		this.cms = '1';
		this.ymd = field;
		this.callback = callback ? callback : null;

		if (v != '' && v.length >= 6) {
			this.y = parseInt(v.substr(0, 4));
			this.m = parseInt(v.substr(4, 1) == '0' ? v.substr(5, 1) : v.substr(4, 2));
		}

		this.show(x, y);
	},

	setDate : function (d) {
		Calendar.ymd.value = d;

		if (Calendar.callback != null) {
			if (!Calendar.callback()) {
				return;
			}
		}

		this.hide();
	},

	update : function (act) {
		switch (act) {
			case -1 :
				if (this.m == 1) {
					this.y -= 1;
					this.m = 12;
				} else {
					this.m -= 1;
				}

				break;
			case 1 :
				if (this.m == 12) {
					this.y += 1;
					this.m = 1;
				} else {
					this.m += 1;
				}

				break;
			case -12 :
				this.y -= 1;
				break;
			case 12 :
				this.y += 1;
				break;
			default :
				if (this.y == 0 && this.m == 0) {
					var day = new Date();

					this.y = day.getFullYear();
					this.m = day.getMonth() + 1;
				}
		}

		this.make();
	},

	make : function () {
		var doc = '';
		var week = new Date(this.y, this.m - 1, 1).getDay();
		var sTD = "font:normal 8pt tahoma,verdana,arial;line-height:140%;";
		var sTDTitle = "background-color:#F5F6F8;";

		doc += '<table width="316" cellpadding="2" cellspacing="1" border="0" bgcolor="#B8C1C7" style="cursor:default;">'
		    +  '<tr height="25" align="center">'
		    +  '<td colspan="7" style="' + sTD + sTDTitle + '"><img src="' + this.topDir + '/common/module/calendar/images/first.gif" width="13" height="13" style="cursor:pointer; margin-right:3px;" id="_c_p_yy_" /><img src="' + this.topDir + '/common/module/calendar/images/prev.gif" width="13" height="13" style="cursor:pointer; margin-right:5px;" id="_c_prev_" /><strong>' + this.y + '.' + (this.m >= 10 ? this.m : '0' + this.m) + '</strong><img src="' + this.topDir + '/common/module/calendar/images/next.gif" width="13" height="13" style="cursor:pointer; margin-left:5px;" id="_c_next_" /><img src="' + this.topDir + '/common/module/calendar/images/last.gif" width="13" height="13" style="cursor:pointer; margin-left:3px;" id="_c_n_yy_" /></td>'
		    +  '</tr>'
		    +  '<tr height="25" align="center">'
		    +  '<td width="40" style="' + sTD + sTDTitle + '">SUN</td>'
		    +  '<td width="40" style="' + sTD + sTDTitle + '">MON</td>'
		    +  '<td width="40" style="' + sTD + sTDTitle + '">TUE</td>'
		    +  '<td width="40" style="' + sTD + sTDTitle + '">WED</td>'
		    +  '<td width="40" style="' + sTD + sTDTitle + '">THU</td>'
		    +  '<td width="40" style="' + sTD + sTDTitle + '">FRI</td>'
		    +  '<td width="40" style="' + sTD + sTDTitle + '">SAT</td>'
		    +  '</tr>'
		    +  '<tr height="25" align="center" bgcolor="#FFFFFF">';

		for (var i = 0; i < week; i++) {
			doc += '<td style="' + sTD + '">&nbsp;</td>';
		}

		for (var i = 1; i <= this.endDate(this.y, this.m); i++) {
			if (i > 1 && week == 0) {
				doc += '</tr>'
					+  '<tr height="25" align="center" bgcolor="#FFFFFF">';
			}

			var color = "";

			if (week == 0) {
				color = '#FF0000';
			} else if (week == 6) {
				color = '#0000FF';
			} else {
				color = '#000000';
			}

			doc += '<td onclick="Calendar.setDate(\'' + this.y + (this.m >= 10 ? this.m : '0' + this.m) + (i >= 10 ? i : '0' + i) + '\');" style="cursor:pointer; color:' + color + ';' + sTD + '" onmouseover="this.style.backgroundColor = \'#EFEFEF\';" onmouseout="this.style.backgroundColor = \'\';">' + i + '</font></td>';

			if (week == 6) {
				week = 0;
			} else {
				week++;
			}
		}

		if (week != 0) {
			for (var i = 6; i >= week; i--) {
				doc += '<td style="' + sTD + '">&nbsp;</td>';
			}
		}

		doc += '</tr>'
		    +  '</table>';

		$('_c_body_').update(doc);
		$('_c_prev_').observe('click', function () { this.update(-1); }.bind(this));
		$('_c_next_').observe('click', function () { this.update(1); }.bind(this));
		$('_c_p_yy_').observe('click', function () { this.update(-12); }.bind(this));
		$('_c_n_yy_').observe('click', function () { this.update(12); }.bind(this));
	},

	isLeapYear : function (y) {
		if ((y % 4 == 0 && y % 100 != 0) || (y % 400 == 0 && y % 4000 != 0)) {
			return true;
		} else {
			return false;
		}
	},

	endDate : function (y, m) {
		var edate = [0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

		if (m == 2) {
			if (this.isLeapYear(y) == true) {
				return 29;
			} else {
				return 28;
			}
		} else {
			return edate[m];
		}
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
