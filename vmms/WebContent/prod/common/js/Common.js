var Common = {
	isChecked : false,

	// 객체 가져오기
	object : function (id) {
		if (document.getElementById && document.getElementById(id)) {
			return document.getElementById(id);
		} else if (document.getElementByName && document.getElementByName(id)) {
			return document.getElementByName(id);
		} else if (document.all && document.all(id)) {
			return document.all(id);
		} else if (document.layers && document.layers[id]) {
			return document.layers[id];
		} else {
			return false;
		}
	},

	// 체크박스 모두 선택하기
	selAll : function (frm, isObj) {
		if (isObj == true) {
			if (this.isChecked == false) {
				for (var i = 0; i <= frm; i++) {
					if (this.object("_a_" + i + "_").checked == true) {
						continue;
					} else {
						this.object("_a_" + i + "_").checked = true;
					}
				}

				this.isChecked = true;
			} else {
				for (var i = 0; i <= frm; i++) {
					if (this.object("_a_" + i + "_").checked == true) {
						this.object("_a_" + i + "_").checked = false;
					} else {
						continue;
					}
				}

				this.isChecked = false;
			}
			return false;
		} else {
			if (this.isChecked == false) {
				for (i = 0; i < frm.length; i++) {
					if (frm[i].type == "checkbox") {
						if (frm[i].checked == true) {
							continue;
						} else {
							frm[i].checked = true;
						}
					}
				}

				this.isChecked = true;
			} else {
				for (i = 0; i < frm.length; i++) {
					if (frm[i].type == "checkbox") {
						if (frm[i].checked == true) {
							frm[i].checked = false;
						} else {
							continue;
						}
					}
				}

				this.isChecked = false;
			}
		}
	},
	
	//공지사항 김태우 추가
	// 새창띄우기
	openWin : function (url, wname, width, height, scrl) {
		var winl = (screen.width - width) / 2; 
		var wint = (screen.height - height) / 2;

		if (typeof scrl == "undefined") {
			var scroll = "no";
		} else {
			var scroll = scrl;
		}

		return window.open(url, wname, "left=" + winl + ", top=" + wint + ", scrollbars=" + scroll + ", status=yes, resizable=no, width=" + width + ", height=" + height);
	},
	//새창 SUB 띄우기
	openWinSub1 : function (url, wname, width, height, scrl) {

		if (typeof scrl == "undefined") {
			var scroll = "no";
		} else {
			var scroll = scrl;
		}

		return window.open(url, wname, "left=" + (-1500) + ", top=" + 50 + ", scrollbars=" + scroll + ", status=yes, resizable=no, width=" + width + ", height=" + height);
	},
	//새창 SUB2 띄우기
	openWinSub2 : function (url, wname, width, height, scrl) {

		if (typeof scrl == "undefined") {
			var scroll = "no";
		} else {
			var scroll = scrl;
		}

		return window.open(url, wname, "left=" + (-1300) + ", top=" + 200 + ", scrollbars=" + scroll + ", status=yes, resizable=no, width=" + width + ", height=" + height);
	},
	//새창 SUB3 띄우기
	openWinSub3 : function (url, wname, width, height, scrl) {

		if (typeof scrl == "undefined") {
			var scroll = "no";
		} else {
			var scroll = scrl;
		}

		return window.open(url, wname, "left=" + (-1100) + ", top=" + 250 + ", scrollbars=" + scroll + ", status=yes, resizable=no, width=" + width + ", height=" + height);
	},

	// 포커스 이동
	moveFocus : function (num, fromform, toform) {
		var str = fromform.value.length;

		if (str == num) {
			toform.focus();
		}
	},

	// 이메일 체크
	isAvailableEmail : function (v) {
		var format = /^((\w|[\-\.])+)@((\w|[\-\.])+)\.([A-Za-z]+)$/;

		if (v.search(format) == -1) {
			return false;
		} else if (v.charAt(v.indexOf('@') + 1) == '.') {
			return false;
		} else {
			return true;
		}
	},

	// 금액에 콤마찍기
	formatNumber : function (n) {
		var num = new String(n).replace(/\-/gi, "").replace(/,/gi, "").replace(/\./gi, "");
		var sgn = parseInt(num) < 0 || new String(n).substr(0, 1) == "-" ? "-" : "";
		var len = num.length;
		var pos = 3;
		var tmp = "";

		if (isNaN(num)) {
			window.alert("Only number it will be able to input.");
			return 0;
		} else if (parseInt(num) == 0) {
			return num;
		}

		while (len > 0) {
			len -= pos;

			if (len < 0) {
				pos = len + pos;
				len = 0;
			}

			tmp = "," + num.substr(len, pos) + tmp;
		}

		return sgn + tmp.substr(1);
	},

	// 문자열 길이
	strLen : function (str) {
		var len = 0;
		var tmp = null;
		var i = 0;

		while (i < str.length) {
			tmp = str.charAt(i);

			if (escape(tmp).length > 4) {
				len += 2;
			} else if (tmp != "\r") {
				len++;
			}

			i++;
		}

		return len;
	},

	// 문자열 자르기
	strCut : function (str, len, tail) {
		if (len == 0 || this.strLen(str) <= len) {
			return str;
		}

		var t = null;
		var i = 0;
		var l = 0;
		var returnValue = "";

		while (i < str.length) {
			t = str.charAt(i);

			if (escape(t).length > 4) {
				l += 2;
			} else if (t != "\r") {
				l += 1;
			}

			returnValue += t;

			if (l >= len) {
				break;
			}

			i++;
		}

		return returnValue + (typeof tail == "undefined" ? "..." : tail);
	},

	// 대문자 -> 소문자
	strToLower : function (str) {
		return str.toLowerCase();
	},

	// 소문자 -> 대문자
	strToUpper : function (str) {
		return str.toUpperCase();
	},

	// 배열안에 값이 있는지 체크
	inArray : function (val, arr) {
		for (var i = 0; i < arr.length; i++) {
			if (arr[i] == val) {
				return true;
			}
		}

		return false;
	},

	// 라디오버튼 체크 여부
	radio : function (frm, act, val) {
		switch (act) {
			// 체크값 구하기
			case 1 :
				if (frm.length > 0) {
					for (var i = 0; i < frm.length; i++) {
						if (frm[i].checked == true) {
							return frm[i].value;
						}
					}
				} else {
					if (frm.checked == true) {
						return frm.value;
					}
				}

				break;

			// 해당 위치에 포커스
			case 2 :
				if (frm.length > 0) {
					for (var i = 0; i < frm.length; i++) {
						if (frm[i].value == val) {
							frm[i].checked = true;
							break;
						}
					}
				} else {
					if (frm.value == val) {
						frm.checked = true;
					}
				}

				break;

			// 체크된 박스의 순번
			case 3 :
				if (frm.length > 0) {
					for (var i = 0; i < frm.length; i++) {
						if (frm[i].value == val) {
							return i;
						}
					}
				} else {
					return 0;
				}

				break;

			// 체크여부
			default :
				if (frm.length > 0) {
					for (var i = 0; i < frm.length; i++) {
						if (frm[i].checked == true) {
							return true;
						}
					}
				} else {
					if (frm.checked == true) {
						return true;
					}
				}
		}

		return false;
	},

	// 소숫점 자릿수 맞추기
	round : function (num, pos) {
		var posV = Math.pow(10, (pos ? pos : 2));

		return Math.round(num * posV) / posV;
	},

	// 문자열 반복체크
	isRepetition : function (str, lmt) {
		if (str.length < 1) {
			return false;
		}

		for (var i = 0; i < str.length; i++) {
			var rpt = str.substr(i, 1);
			var key = "";

			for (var j = 0; j < lmt; j++) {
				key += rpt;
			}

			var chk = str.indexOf(key);

			if (chk < 0) {
				continue;
			} else {
				return true;
				break;
			}
		}

		return false;
	},

	// 쿠키값 제어
	cookie : function (name, value, expire) {
		if (value && expire) {
			var day = new Date();
			day.setDate(day.getDate() + expire);
			document.cookie = name + "=" + escape(value) + "; path=/; expires=" + day.toGMTString() + ";";
		} else {
			var org = document.cookie;
			var dlm = name + "=";
			var x = 0;
			var y = 0;
			var z = 0;

			while (x <= org.length) {
				y = x + dlm.length;

				if (org.substring(x, y) == dlm) {
					if ((z = org.indexOf(";", y)) == -1) {
						z = org.length;
					}

					return org.substring(y, z);
				}

				x = org.indexOf(" ", x) + 1;

				if (x == 0) {
					break;
				}
			}

			return "";
		}
	},

	// 날짜목록 (년)
	yyList : function (y, s, e) {
		day = new Date();

		if (typeof y == "undefined") {
			var yy = day.getFullYear();
		} else if (y == "") {
			var yy = 0;
		} else {
			var yy = parseInt(y);
		}

		for (var i = (e ? e : day.getFullYear() + 1); i >= (s ? s : 2009); i--) {
			document.write("<option value='" + i + "'" + (i == yy ? " selected" : "") + ">" + i + "년</option>");
		}
	},

	// 날짜목록 (월)
	mmList : function (m) {
		day = new Date();

		if (typeof m == "undefined") {
			var mm = day.getMonth() + 1;
		} else if (m == "") {
			var mm = 0;
		} else {
			var mm = (m.substr(0, 1) == "0") ? parseInt(m.substr(1, m.length)) : parseInt(m);
		}

		for (var i = 1; i <= 12; i++) {
			var n = (i < 10 ? "0" : "") + i;

			document.write("<option value='" + n + "'" + (i == mm ? " selected" : "") + ">" + n + "월</option>");
		}
	},

	// 날짜목록 (일)
	ddList : function (y, m, d) {
		day = new Date();

		if (typeof y == "undefined") {
			var yy = day.getFullYear();
		} else if (y == "") {
			var yy = 0;
		} else {
			var yy = parseInt(y);
		}

		if (typeof m == "undefined") {
			var mm = day.getMonth() + 1;
		} else if (m == "") {
			var mm = 0;
		} else {
			var mm = (m.substr(0, 1) == "0") ? parseInt(m.substr(1, m.length)) : parseInt(m);
		}

		if (typeof d == "undefined") {
			var dd = day.getDate();
		} else if (d == "") {
			var dd = 0;
		} else {
			var dd = (d.substr(0, 1) == "0") ? parseInt(d.substr(1, d.length)) : parseInt(d);
		}

		for (var i = 1; i <= this.endDate(yy, mm); i++) {
			var n = (i < 10 ? "0" : "") + i;

			document.write("<option value='" + n + "'" + (i == dd ? " selected" : "") + ">" + n + "일</option>");
		}
	},

	// 날짜목록 (시)
	hhList : function (h) {
		for (var i = 0; i <= 23; i++) {
			var n = (i < 10 ? "0" : "") + i;

			document.write("<option value='" + n + "'" + (n == h ? " selected" : "") + ">" + n + "시</option>");
		}
	},

	// 날짜목록 (분)
	iiList : function (m) {
		for (var i = 0; i <= 59; i++) {
			var n = (i < 10 ? "0" : "") + i;

			document.write("<option value='" + n + "'" + (n == m ? " selected" : "") + ">" + n + "분</option>");
		}
	},

	// 날짜목록 (초)
	ssList : function (s) {
		for (var i = 0; i <= 59; i++) {
			var n = (i < 10 ? "0" : "") + i;

			document.write("<option value='" + n + "'" + (n == s ? " selected" : "") + ">" + n + "초</option>");
		}
	},

	// 오늘 날짜
	ymd : function () {
		day = new Date();

		return day.getFullYear() + (day.getMonth() < 9 ? "0" : "") + (day.getMonth() + 1) + (day.getDate() < 10 ? "0" : "") + day.getDate();
	},

	// 윤년 여부
	isLeapYear : function (y) {
		if ((y % 4 == 0 && y % 100 != 0) || (y % 400 == 0 && y % 4000 != 0)) {
			return true;
		} else {
			return false;
		}
	},

	// 해당 월의 마지막 날짜
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

	// 목록 바꾸기
	changeDate : function (sel, y, m) {
		if (typeof y == "undefined" && typeof m == "undefined") {
			for (var i = 1; i <= 12; i++) {
				var n = (i < 10 ? "0" : "") + i;

				sel.options[i] = new Option(n, n);
			}
		} else {
			var ed = this.endDate(parseInt(y), (m.substr(0, 1) == "0") ? parseInt(m.substr(1, m.length)) : parseInt(m));

			for (var i = sel.length - 1; i > 0; i--) {
				sel.options[i] = null;
			}

			for (var i = 1; i <= ed; i++) {
				var n = (i < 10 ? "0" : "") + i;

				sel.options[i] = new Option(n, n);
			}
		}
	},

	// 좌/우 공백제거
	trim : function (str) {
		return str.replace(/(^\s*)|(\s*$)/gi, "");
	},

	// 배열 섞기
	shuffle : function (arr) {
		var tmp = [];

		for (var i = 0; i < arr.length; i++) {
			tmp[i] = arr[i];
		}

		tmp.sort ( function() { return Math.random() * 2 - 1; } );

		return tmp;
	},

	// 이미지 보정 사이즈
	imgResize : function (ow, oh, mw, mh) {
		var as = [mw, mh];
		var rw, rh;

		if (mw > 0 && mh > 0) {
			if (ow > mw || oh > mh) {
				rw = ow / mw;
				rh = oh / mh;

				if (rw > rh) {
					as[0] = mw;
					as[1] = Math.ceil(oh * mw / ow);
				} else {
					as[0] = Math.ceil(ow * mh / oh);
					as[1] = mh;
				}
			} else {
				as[0] = ow;
				as[1] = oh;
			}
		} else if (mw > 0) {
			if (ow > mw) {
				as[0] = mw;
				rw = mw / ow;
			} else {
				as[0] = ow;
				rw = 1;
			}

			as[1] = Math.ceil(oh * rw);
		} else if (mh > 0) {
			if (oh > mh) {
				as[1] = mh;
				rh = mh / oh;
			} else {
				as[1] = oh;
				rh = 1;
			}

			as[0] = Math.ceil(ow * rh);
		}

		return as;
	},

	// 기본값 설정
	setDefaultValue : function (value, defaultValue) {
		if (!value) {
			return defaultValue;
		}

		return value;
	},

	// 플래쉬 출력
	setFlash : function (s, w, h, param) {
		var doc = '<object classid="clsid:D27CDB6E-AE6D-11cf-96B8-444553540000" codebase="' + location.protocol + '//download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=9,0,0,0" width="' + w + '" height="' + h + '" id="' + (param && param.id ? param.id : Math.floor(Math.random() * 1000000)) + '"' + (param && param.css ? ' css="' + param.css + '"' : '') + (param && param.style ? ' style="' + param.style + '"' : '') + '>'
				+ '<param name="movie" value="' + s + '" />'
				+ '<param name="allowScriptAccess" value="always" />'
				+ '<param name="allowFullScreen" value="false" />'
				+ '<param name="wmode" value="transparent" />'
				+ '<param name="menu" value="false" />'
				+ '<param name="quality" value="high" />'
				+ '<param name="bgcolor" value="' + (param && param.bgcolor ? param.bgcolor : '#FFFFFF') + '" />'
				+ '<param name="scale" value="exactfit" />'
				+ (param && param.name ? '<param name="' + param.name + '" value="' + param.value + '" />' : '')
				+ '<embed src="' + s + '" allowScriptAccess="always" allowFullScreen="false" wmode="transparent" menu="false" quality="high" bgcolor="' + (param && param.bgcolor ? param.bgcolor : '#FFFFFF') + '" width="' + w + '" height="' + h + '" ' + (param ? param.name + '="' + param.value + '"' : '') + ' type="application/x-shockwave-flash" pluginspage="' + location.protocol + '//www.macromedia.com/shockwave/download/index.cgi?P1_Prod_Version=ShockwaveFlash"' + (param && param.css ? ' css="' + param.css + '"' : '') + (param && param.style ? ' style="' + param.style + '"' : '') + ' />'
				+ '</object>';

		if (param && param.obj) {
			this.object(param.obj).innerHTML = doc;
		} else {
			document.write(doc);
		}
	},

	// 비디오 출력
	setVideo : function (s, w, h) {
		var temp = s.split("?");

		if (temp[0].match(/\.(swf)$/i)) {
			this.setFlash(s, w, h);
			return;
		}

		var doc = '<object classid="clsid:22D6F312-B0F6-11D0-94AB-0080C74C7E95"'
				+ ' width="' + w + '" height="' + h + '" VIEWASTEXT'
				+ ' codebase="' + location.protocol + '//activex.microsoft.com/activex/controls/mplayer/en/nsmp2inf.cab#Version=5,1,52,701"'
				+ ' standby="Loading Microsoft Windows Media Player components..."'
				+ ' type="application/x-oleobject">\n'
				+ '<param name="AnimationAtStart" value="0" />\n'
				+ '<param name="BufferingTime" value="5" />\n'
				+ '<param name="EnableContextMenu" value="0" />\n'
				+ '<param name="Filename" value="' + s + '" />\n'
				+ '<param name="ShowDisplay" value="0" />\n'
				+ '<param name="ShowPositionControls" value="1" />\n'
				+ '<param name="ShowStatusBar" value="0" />\n'
				+ '<param name="ShowTracker" value="1" />\n'
				+ '<param name="Volume" value="-300" />\n'
				+ '<embed src="' + s + '" width="' + w + '" height="' + h + '" AnimationAtStart="0" BufferingTime="5" EnableContextMenu="0" ShowDisplay="1" ShowPositionControls="1" ShowStatusBar="1" ShowTracker="1" Volume="-300"></embed>\n'
				+ '</object>';

		document.write(doc);
	},

	// 페이지 사이즈
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

	// 스크롤 사이즈
	getScrollSize : function (obj) {
		var T, L, W, H;

		with (obj.document) {
			if (obj.document.documentElement && obj.document.documentElement.scrollTop) {
				T = obj.document.documentElement.scrollTop;
				L = obj.document.documentElement.scrollLeft;
			} else if (obj.document.body) {
				T = obj.document.body.scrollTop;
				L = obj.document.body.scrollLeft;
			}

			if (obj.innerWidth) {
				W = obj.innerWidth;
				H = obj.innerHeight;
			} else if (obj.document.documentElement && obj.document.documentElement.clientWidth) {
				W = obj.document.documentElement.clientWidth;
				H = obj.document.documentElement.clientHeight;
			} else {
				W = obj.document.body.offsetWidth;
				H = obj.document.body.offsetHeight
			}
		}

		return {top: T, left: L, width: W, height: H};
	},

	// 폼 체크
	checkForm : function (o) {
		var objBody = o ? o : $$('body')[0];
		var returnValue = '';

		objBody.getElementsBySelector('.checkForm').findAll(function (s) {
			var o = $(s);

			if (!o.getAttribute('option')) {
				return;
			} else if (returnValue != '') {
				return;
			}

			var v = o.getAttribute('option').evalJSON();

			switch (s.tagName.toLowerCase()) {
				case 'input' :
					switch (s.type.toLowerCase()) {
						case 'radio' :
							// RADIO 체크는 힘드네;
							break;
						case 'checkbox' :
							if (v.isMust && !s.checked) {
								returnValue = v.message;
							}
							break;
						default :
							if (v.isMust && s.value == '' && !o.disabled) {
								returnValue = v.message;
							} else if (s.value && !o.disabled) {
								if (o.getAttribute('maxlength') && parseInt(o.getAttribute('maxlength')) != s.value.length && v.equalLength) {
									returnValue = v.message + '\n\nERROR : ' + o.getAttribute('maxlength') + '자로 입력하세요.';
								} else if (o.getAttribute('maxlength') && parseInt(o.getAttribute('maxlength')) < s.value.length && !v.equalLength) {
									returnValue = v.message + '\n\nERROR : ' + o.getAttribute('maxlength') + '자 이하로 입력하세요.';
								} else {
									switch (v.varType) {
										case 'number' :
											if (!/^[0-9]+$/.test(s.value)) {
												returnValue = v.message + '\n\nERROR : 숫자만 입력이 가능합니다.';
											}
											break;
										case 'float' :
											if (!/^[0-9\.]+$/.test(s.value)) {
												returnValue = v.message + '\n\nERROR : 숫자만 입력이 가능합니다.';
											}
											break;
										case 'alnum' :
											if (!/^[a-z0-9]+$/.test(s.value)) {
												returnValue = v.message + '\n\nERROR : 영(소)문 및 숫자만 입력이 가능합니다.';
											}
											break;
										case 'ALNUM' :
											if (!/^[A-Z0-9]+$/.test(s.value)) {
												returnValue = v.message + '\n\nERROR : 영(대)문 및 숫자만 입력이 가능합니다.';
											}
											break;
										case 'email' :
											if (!this.isAvailableEmail(s.value)) {
												returnValue = v.message + '\n\nERROR : 형식이 일치하지 않습니다.';
											}
											break;
									}
								}
							}
					}
					break;
				case 'select' :
					if (v.isMust && s.options.selectedIndex == 0 && !o.disabled) {
						returnValue = v.message;
					}
					break;
				case 'textarea' :
					if (v.isMust && s.value == '' && !o.disabled) {
						returnValue = v.message;
					} else if (s.value && !o.disabled) {
						if (o.getAttribute('maxlength') && parseInt(o.getAttribute('maxlength')) < s.value.length) {
							returnValue = v.message + '\n\nERROR : ' + o.getAttribute('maxlength') + '자 이하로 입력하세요.';
						}
					}
			}

			if (returnValue != '') {
				try { s.focus(); } catch (e) {}
			}
		}.bind(this));

		return returnValue;
	}
}

// equals java.net.URLEncoder.encode(str, "UTF-8")
function encodeURIComponentEx(str) {
	var returnValue = "";
	var s, u;

	for (var i = 0; i < str.length; i++) {
		s = str.charAt(i);
		u = str.charCodeAt(i);

		if (s == " ") {
			returnValue += "+";
		} else {
			if (u == 0x2a || u == 0x2d || u == 0x2e || u == 0x5f || ((u >= 0x30) && (u <= 0x39)) || ((u >= 0x41) && (u <= 0x5a)) || ((u >= 0x61) && (u <= 0x7a))) {
				returnValue += s;
			} else {
				if ((u >= 0x0) && (u <= 0x7f)) {
					s = "0" + u.toString(16);
					returnValue += "%" + s.substr(s.length - 2);
				} else if (u > 0x1fffff) {
					returnValue += "%" + (oxf0 + ((u & 0x1c0000) >> 18)).toString(16);
					returnValue += "%" + (0x80 + ((u & 0x3f000) >> 12)).toString(16);
					returnValue += "%" + (0x80 + ((u & 0xfc0) >> 6)).toString(16);
					returnValue += "%" + (0x80 + (u & 0x3f)).toString(16);
				} else if (u > 0x7ff) {
					returnValue += "%" + (0xe0 + ((u & 0xf000) >> 12)).toString(16);
					returnValue += "%" + (0x80 + ((u & 0xfc0) >> 6)).toString(16);
					returnValue += "%" + (0x80 + (u & 0x3f)).toString(16);
				} else {
					returnValue += "%" + (0xc0 + ((u & 0x7c0) >> 6)).toString(16);
					returnValue += "%" + (0x80 + (u & 0x3f)).toString(16);
				}
			}
		}
	}

	return returnValue;
}

// equals java.net.URLDecoder.decode(str, "UTF-8")
function decodeURIComponentEx(str) {
	var returnValue = "";
	var s, u, n, f;

	for (var i = 0; i < str.length; i++) {
		s = str.charAt(i);

		if (s == "+") {
			returnValue += " ";
		} else {
			if (s != "%") {
				returnValue += s;
			} else {
				u = 0;
				f = 1;

				while (true) {
					var ss = "";

					for (var j = 0; j < 2; j++) {
						var sss = str.charAt(++i);

						if (((sss >= "0") && (sss <= "9")) || ((sss >= "a") && (sss <= "f"))  || ((sss >= "A") && (sss <= "F"))) {
							ss += sss;
						} else {
							--i;
							break;
						}
					}

					n = parseInt(ss, 16);

					if (n <= 0x7f) { u = n; f = 1; }
					if (n >= 0xc0 && n <= 0xdf) { u = n & 0x1f; f = 2; }
					if (n >= 0xe0 && n <= 0xef) { u = n & 0x0f; f = 3; }
					if (n >= 0xf0 && n <= 0xf7) { u = n & 0x07; f = 4; }
					if (n >= 0x80 && n <= 0xbf) { u = (u << 6) + (n & 0x3f); --f; }

					if (f <= 1) {
						break;
					}

					if (str.charAt(i + 1) == "%") {
						i++;
					} else {
						break;
					}
				}

				returnValue += String.fromCharCode(u);
			}
		}
	}

	return returnValue;
}


function datePeriod(sDate, eDate) {
	//alert(sDate);
	var startDate = new Date(parseInt(sDate.substr(0, 4), 10), parseInt(sDate.substr(4, 2), 10) - 1, parseInt(sDate.substr(6, 2), 10));
	var endDate = new Date(parseInt(eDate.substr(0, 4), 10), parseInt(eDate.substr(4, 2), 10) - 1, parseInt(eDate.substr(6, 2), 10));
	return (endDate.getTime() - startDate.getTime()) / 1000 / 60 / 60 / 24 ;
}

function dateAdd(sDate, nDays) {
	var endDate = new Date(parseInt(sDate.substr(0, 4), 10), parseInt(sDate.substr(4, 2), 10) - 1, parseInt(sDate.substr(6, 2), 10) + nDays);
	
	var yy = endDate.getFullYear();
	var mm = endDate.getMonth() + 1; mm = (mm < 10) ? '0' + mm : mm;
	var dd = endDate.getDate(); dd = (dd < 10) ? '0' + dd : dd;
	
	return '' + yy + mm + dd; 
}


