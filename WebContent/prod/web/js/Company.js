var Company = {
	depth : 0,
// 소속
	company : function (company, organ, value, isAll) {
		var o = {company : $("company"), auth : $("auth"), user : $("user"), sbmsg : $("sbmsg")};

		o.sbmsg.show();

		for (var i = o.company.length - 1; i > 0; i--) {
			o.company.options[i] = null;
		}

		new Ajax.Request(G_TOP_DIR + "/common/src/company.jsp", {
			asynchronous : true,
			parameters : {organ : organ ? organ : 0, mode : (value && value.mode ? value.mode : ""), isAll : (isAll ? isAll : "")},
			onSuccess : function (xmlHttp) {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == "EXPIRE") {
					top.location.reload();
					return;
				} else if (e.code == "FAIL") {
					window.alert("소속 정보를 가져오는데 실패하였습니다.");
					o.sbmsg.hide();
					return;
				}

				e.data.each (function (data, i) {
					o.company.options[i + 1] = new Option(decodeURIComponentEx(data.name), '{"seq" : ' + data.seq + ', "depth" : ' + data.depth + '}');

					if (data.seq == company) {
						o.company.options[i + 1].selected = true;
					}
				}.bind(this));

				if (company > 0) { // 소속이 선택되어 있을 때
					var com = o.company.value.evalJSON();

					if (organ > 0) { // 조직이 선택되어 있을 때
						var dsp = [];

						e.organ.each (function (data, i) {
							dsp[i] = data.seq;
						}.bind(this));

						this.organ(com, 0, 0, value ? value.mode : "", dsp);

						e.organ.each (function (data, i) {
							this.organ(com, i + 1, data.seq, value ? value.mode : "", dsp);
						}.bind(this));
					} else {
						this.organ(com, 0, 0, value ? value.mode : "");
					}
	
					if (value && value.mode == "A" && o.auth) { // 권한이 선택되어 있을 때
						for (var i = 0; i < o.auth.length; i++) {
							if (o.auth.options[i].value == value.auth) {
								o.auth.options[i].selected = true;
								break;
							}
						}
					} else if (value && value.mode == "B" && o.user) { // 계정이 선택되어 있을 때
						for (var i = 0; i < o.user.length; i++) {
							if (o.user.options[i].value == value.user) {
								o.user.options[i].selected = true;
								break;
							}
						}
					}
				}

				o.sbmsg.hide();
			}.bind(this)
		});
	},
// 조직
	organ : function (company, depth, organ, mode, dsp, isAll) {
		var o = {organ : $("organ"), change : $("organ" + depth), auth : $("auth"), user : $("user"), place : $("place")};

		o.organ.getElementsBySelector("select").findAll(function (s) {
			var _depth = parseInt(s.id.substr(o.organ.id.length, s.id.length));

			if (_depth > depth) {
				s.replace("");
			}
		}.bind(this));

		if (organ == "-1") {
			this.auth(company, depth - 2);
			this.user(company, $("organ" + (depth - 2)) ? $("organ" + (depth - 2)).value : -1);
			this.place(company, $("organ" + (depth - 2)) ? $("organ" + (depth - 2)).value : -1);
			
			if (o.change) {
				o.change.replace("");
			}
			
			return;
		} else if (depth > company.depth) {		
			this.auth(company, depth - 1);
			this.user(company, $("organ" + (depth - 1)).value);
			this.place(company, $("organ" + (depth - 1)).value);
			return;
		}

		if (o.change) { // 다음 단계의 셀렉트 박스가 있을 경우
			o.change.options[0].text = "- loading";

			for (var i = o.change.length - 1; i > 0; i--) {
				o.change.options[i] = null;
			}
		} else {
			j$('#organ').append('<select id="' + o.organ.id + depth + '"><option value="-1">- loading</option></select>');
			var selectID='#'+o.organ.id+depth;
			j$(selectID).addClass('js-example-basic-single');
			j$(selectID).addClass('js-example-responsive');
			//o.organ.insert('<select id="' + o.organ.id + depth + '" class="'+'js-example-basic-single js-example-responsive'+'" style="'+'margin-right:4px'+'"><option value="-1">- loading</option></select>');
		}

		switch (mode) {
			case "A" :
				o.auth.options[0].text = "- loading";

				for (var i = o.auth.length - 1; i > 0; i--) {
					o.auth.options[i] = null;
				}

				break;
			case "B" :
				o.user.options[0].text = "- loading";

				for (var i = o.user.length - 1; i > 0; i--) {
					o.user.options[i] = null;
				}

				break;
			case "C" :
				o.place.options[0].text = "- loading";

				for (var i = o.place.length - 1; i > 0; i--) {
					o.place.options[i] = null;
				}

				break;
		}

		new Ajax.Request(G_TOP_DIR + "/common/src/organ.jsp", {
			asynchronous : true,
			parameters : {company : company.seq, organ : organ, depth : depth, mode : mode, isAll : (isAll ? isAll : "")},
			onSuccess : function (xmlHttp) {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == "EXPIRE") {
					top.location.reload();
					return;
				} else if (e.code == "FAIL") {
					window.alert("조직 정보를 가져오는데 실패하였습니다.");
					return;
				}

				if (!o.change) {
					o.change = $(o.organ.id + depth);

					if (e.disp == "") {
						o.change.replace("");
						return;
					}

					o.change.observe("change", function (event) {
						this.organ(company, depth + 1, o.change.options[o.change.selectedIndex].value, mode, null, isAll ? isAll : "");
					}.bind(this));
				}

				e.data.each (function (data, i) {
					o.change.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);

					if (dsp && Common.inArray(data.seq, dsp)) {
						o.change.options[i + 1].selected = true;
					}
				}.bind(this));

				o.change.options[0].text = "- " + (e.disp ? decodeURIComponentEx(e.disp) : "조직");

				if (this.depth >= depth) {
					o.change.disabled = true;
					o.change.addClassName("disabled");
				}

				switch (mode) {
					case "A" :
//						o.auth.options[0].text = "- loading";

						e.auth.each (function (data, i) {
							o.auth.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);
						}.bind(this));

						o.auth.options[0].text = "- 권한";
						break;
					case "B" :
//						o.user.options[0].text = "- loading";

						e.user.each (function (data, i) {
							o.user.options[i + 1] = new Option(decodeURIComponentEx(data.name) + "/" + data.id, data.seq);
						}.bind(this));

						o.user.options[0].text = "- 계정";
						break;
					case "C" :
//						o.place.options[0].text = "- loading";

						e.place.each (function (data, i) {
							o.place.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);
						}.bind(this));

						o.place.options[0].text = "- 설치위치";
						break;
				}
			}.bind(this)
		});
	},
// 권한
	auth : function (company, depth, n) {
		var o = $("auth");

		if (!o || o.tagName.toLowerCase() != "select") {
			return;
		}

		o.options[0] = new Option("- loading", 0);

		for (var i = o.length - 1; i > 0; i--) {
			o.options[i] = null;
		}

		new Ajax.Request(G_TOP_DIR + "/common/src/auth.jsp", {
			asynchronous : true,
			parameters : {company : company.seq, depth : depth ? depth : -1},
			onSuccess : function (xmlHttp) {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == "EXPIRE") {
					top.location.reload();
					return;
				} else if (e.code == "FAIL") {
					window.alert("권한 정보를 가져오는데 실패하였습니다.");
					return;
				}

				e.data.each (function (data, i) {
					o.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);

					if (data.seq == n) {
						o.options[i + 1].selected = true;
					}
				}.bind(this));

				o.options[0].text = "- 권한";
			}.bind(this)
		});
	},
// 계정
	user : function (company, organ, n) {
		var o = $("user");

		if (!o || o.tagName.toLowerCase() != "select") {
			return;
		}

		o.options[0] = new Option("- loading", 0);

		for (var i = o.length - 1; i > 0; i--) {
			o.options[i] = null;
		}

		new Ajax.Request(G_TOP_DIR + "/common/src/user.jsp", {
			asynchronous : true,
			parameters : {company : company.seq, organ : organ},
			onSuccess : function (xmlHttp) {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == "EXPIRE") {
					top.location.reload();
					return;
				} else if (e.code == "FAIL") {
					window.alert("계정 정보를 가져오는데 실패하였습니다.");
					return;
				}

				e.data.each (function (data, i) {
					o.options[i + 1] = new Option(decodeURIComponentEx(data.name) + "/" + data.id, data.seq);

					if (data.seq == n) {
						o.options[i + 1].selected = true;
					}
				}.bind(this));

				o.options[0].text = "- 계정";
			}.bind(this)
		});
	},
// 설치 위치
	place : function (company, organ, n) {
		var o = $("place");

		if (!o || o.tagName.toLowerCase() != "select") {
			return;
		}

		o.options[0] = new Option("- loading", 0);

		for (var i = o.length - 1; i > 0; i--) {
			o.options[i] = null;
		}

		new Ajax.Request(G_TOP_DIR + "/common/src/place.jsp", {
			asynchronous : true,
			parameters : {company : company.seq, organ : organ},
			onSuccess : function (xmlHttp) {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == "EXPIRE") {
					top.location.reload();
					return;
				} else if (e.code == "FAIL") {
					window.alert("위치 정보를 가져오는데 실패하였습니다.");
					return;
				}

				e.data.each (function (data, i) {
					o.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);

					if (data.seq == n) {
						o.options[i + 1].selected = true;
					}
				}.bind(this));

				o.options[0].text = "- 설치위치";
			}.bind(this)
		});
	}
}

var CompanyEx = {
// 조직
	organ : function (company, depth, organ) {
		var o = {organ : $("organ"), change : $("organ" + depth), place : $("place")};

		o.organ.getElementsBySelector("select").findAll(function (s) {
			var _depth = parseInt(s.id.substr(o.organ.id.length, s.id.length));

			if (_depth > depth) {
				s.replace("");
			}
		}.bind(this));

		if (organ == "-1") {
			this.place($("organ" + (depth - 2)) ? $("organ" + (depth - 2)).value : -1);

			if (o.change) {
				o.change.replace("");
			}

			return;
		} else if (depth > company.depth) {
			this.place($("organ" + (depth - 1)).value);
			return;
		}
		
		if (o.change) { // 다음 단계의 셀렉트 박스가 있을 경우
			o.change.options[0].text = "- loading";

			for (var i = o.change.length - 1; i > 0; i--) {
				o.change.options[i] = null;
			}
		} else {
			o.organ.insert('<select id="' + o.organ.id + depth + '" class="js-example-basic-single js-example-responsive" style="margin-right:4px"><option value="-1">- loading</option></select>');
		}
		
		o.place.options[0].text = "- loading";

		for (var i = o.place.length - 1; i > 0; i--) {
			o.place.options[i] = null;
		}

		new Ajax.Request(G_TOP_DIR + "/web/src/sales/SalesOrgan.jsp", {
			asynchronous : true,
			parameters : {organ : organ, depth : depth},
			onSuccess : function (xmlHttp) {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == "FAIL") {
					window.alert(e.message ? decodeURIComponentEx(e.message) : "조직 정보를 가져오는데 실패하였습니다.");
					return;
				}

				if (!o.change) {
					o.change = $(o.organ.id + depth);

					if (e.disp == "") {
						o.change.replace("");
						return;
					}

					o.change.observe("change", function (event) {
						this.organ(company, depth + 1, o.change.options[o.change.selectedIndex].value);
					}.bind(this));
				}

				e.data.each (function (data, i) {
					o.change.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);
				}.bind(this));

				o.change.options[0].text = "- " + (e.disp ? decodeURIComponentEx(e.disp) : "조직");

				e.place.each (function (data, i) {
					o.place.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);
				}.bind(this));

				o.place.options[0].text = "- 설치위치";
			}.bind(this)
		});
	},
// 설치 위치
	place : function (organ, n) {
		var o = $("place");

		if (!o) {
			return;
		}

		o.options[0] = new Option("- loading", 0);

		for (var i = o.length - 1; i > 0; i--) {
			o.options[i] = null;
		}

		new Ajax.Request(G_TOP_DIR + "/web/src/sales/SalesPlace.jsp", {
			asynchronous : true,
			parameters : {organ : organ},
			onSuccess : function (xmlHttp) {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == "FAIL") {
					window.alert(e.message ? decodeURIComponentEx(e.message) : "위치 정보를 가져오는데 실패하였습니다.");
					return;
				}

				e.data.each (function (data, i) {
					o.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);

					if (data.seq == n) {
						o.options[i + 1].selected = true;
					}
				}.bind(this));

				o.options[0].text = "- 설치위치";
			}.bind(this)
		});
	},
// 사업자 번호
	businessNo: function (first, second){
		console.log(first);
		console.log(second);
	}
}
var CompanyEX2 = {
		depth : 0,
	// 소속
		company : function (company, organ, userorgan, value, isAll) {
			var o = {company : $("company"), auth : $("auth"), user : $("user"), sbmsg : $("sbmsg")};

			o.sbmsg.show();

			for (var i = o.company.length - 1; i > 0; i--) {
				o.company.options[i] = null;
			}

			new Ajax.Request(G_TOP_DIR + "/common/src/company2.jsp", {
				asynchronous : true,
				parameters : {organ : organ ? organ : 0, mode : (value && value.mode ? value.mode : ""), isAll : (isAll ? isAll : ""), userorgan : userorgan ? userorgan : 0},
				onSuccess : function (xmlHttp) {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == "FAIL") {
						window.alert("소속 정보를 가져오는데 실패하였습니다.");
						o.sbmsg.hide();
						return;
					}

					e.data.each (function (data, i) {
						o.company.options[i + 1] = new Option(decodeURIComponentEx(data.name), '{"seq" : ' + data.seq + ', "depth" : ' + data.depth + '}');

						if (data.seq == company) {
							o.company.options[i + 1].selected = true;
						}
					}.bind(this));

					if (company > 0) { // 소속이 선택되어 있을 때
						var com = o.company.value.evalJSON();

						if (organ > 0) { // 조직이 선택되어 있을 때
							var dsp = [];

							e.organ.each (function (data, i) {
								dsp[i] = data.seq;
							}.bind(this));
							this.organ(com, 0, 0, "", dsp);
							
							e.organ.each (function (data, i) {
								this.organ(com, i + 1, data.seq, "", dsp);
								
							}.bind(this));
						} else {
							this.organ(com, 0, 0, "");
							
						}
						
						if (userorgan > 0) { // 사용자가 선택되었을 경우
							var dsp = [];
							
							e.userorgan.each (function (data, i) {
								dsp[i] = data.seq;
							}.bind(this));

							this.organDup(com, 0, 0, value ? value.mode : "", dsp);

							e.userorgan.each (function (data, i) {
								this.organDup(com, i + 1, data.seq, value ? value.mode : "", dsp);
							}.bind(this));
						} else {
							this.organDup(com, 0, 0, value ? value.mode : "");
						}		
						
						if (value && value.mode == "B" && o.user) { // 계정이 선택되어 있을 때
							for (var i = 0; i < o.user.length; i++) {
								if (o.user.options[i].value == value.user) {
									o.user.options[i].selected = true;
									break;
								}
							}
						}
						/*
						if (value && value.mode == "A" && o.auth) { // 권한이 선택되어 있을 때
							for (var i = 0; i < o.auth.length; i++) {
								if (o.auth.options[i].value == value.auth) {
									o.auth.options[i].selected = true;
									break;
								}
							}
						} else if (value && value.mode == "B" && o.user) { // 계정이 선택되어 있을 때
							for (var i = 0; i < o.user.length; i++) {
								if (o.user.options[i].value == value.user) {
									o.user.options[i].selected = true;
									break;
								}
							}
						}
						*/
					}

					o.sbmsg.hide();
				}.bind(this)
			});
		},	
	// 조직
		organ : function (company, depth, organ, mode, dsp, isAll) {
			var o = {organ : $("organ"), change : $("organ" + depth), auth : $("auth"), user : $("user"), place : $("place"), terminal : $("terminal_id")};

			o.organ.getElementsBySelector("select").findAll(function (s) {
				var _depth = parseInt(s.id.substr(o.organ.id.length, s.id.length));

				if (_depth > depth) {
					s.replace("");
				}
			}.bind(this));

			if (organ == "-1") {

				switch (mode) {
				case "A" :
					this.auth(company, depth - 2);
					break;
				case "B" :
					this.user(company, $("organ" + (depth - 2)) ? $("organ" + (depth - 2)).value : -1);
					break;
				case "C" :
					this.place(company, $("organ" + (depth - 2)) ? $("organ" + (depth - 2)).value : -1);
					break;
				}

				if (o.change) {
					o.change.replace("");
				}

				return;
			} else if (depth > company.depth) {
				
				switch (mode) {
				case "A" :
					this.auth(company, depth - 1);
					break;
				case "B" :
					this.user(company, $("organ" + (depth - 1)).value);
					break;
				case "C" :
					this.place(company, $("organ" + (depth - 1)).value);
					break;
				}				
				return;
			}

			if (o.change) { // 다음 단계의 셀렉트 박스가 있을 경우
				o.change.options[0].text = "- loading";

				for (var i = o.change.length - 1; i > 0; i--) {
					o.change.options[i] = null;
				}
			} else {
				o.organ.insert('<select id="' + o.organ.id + depth + '" class="js-example-basic-single js-example-responsive" style="margin-right:4px"><option value="-1">- loading</option></select>');
				//2017.06.30 jwhwang 추가

				if (o.terminal != null) {
					$("organ" + depth).onchange = function(){ _terminal(); _user();};
					_user();_terminal();
				}
			}

			switch (mode) {
				case "A" :
					o.auth.options[0].text = "- loading";

					for (var i = o.auth.length - 1; i > 0; i--) {
						o.auth.options[i] = null;
					}

					break;
				case "B" :
					o.user.options[0].text = "- loading";

					for (var i = o.user.length - 1; i > 0; i--) {
						o.user.options[i] = null;
					}

					break;
				case "C" :
					o.place.options[0].text = "- loading";

					for (var i = o.place.length - 1; i > 0; i--) {
						o.place.options[i] = null;
					}

					break;
			}

			new Ajax.Request(G_TOP_DIR + "/common/src/organ.jsp", {
				asynchronous : true,
				parameters : {company : company.seq, organ : organ, depth : depth, mode : mode, isAll : (isAll ? isAll : "")},
				onSuccess : function (xmlHttp) {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == "FAIL") {
						window.alert("조직 정보를 가져오는데 실패하였습니다.");
						return;
					}

					if (!o.change) {
						o.change = $(o.organ.id + depth);

						if (e.disp == "") {
							o.change.replace("");
							return;
						}

						o.change.observe("change", function (event) {
							this.organ(company, depth + 1, o.change.options[o.change.selectedIndex].value, mode, null, isAll ? isAll : "");
						}.bind(this));
					}

					e.data.each (function (data, i) {
						o.change.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);

						if (dsp && Common.inArray(data.seq, dsp)) {
							o.change.options[i + 1].selected = true;
						}
					}.bind(this));

					o.change.options[0].text = "- " + (e.disp ? decodeURIComponentEx(e.disp) : "조직");

					if (this.depth >= depth) {
						o.change.disabled = true;
						o.change.addClassName("disabled");
					}

					switch (mode) {
						case "A" :
//							o.auth.options[0].text = "- loading";

							e.auth.each (function (data, i) {
								o.auth.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);
							}.bind(this));

							o.auth.options[0].text = "- 권한";
							break;
						case "B" :
//							o.user.options[0].text = "- loading";

							e.user.each (function (data, i) {
								o.user.options[i + 1] = new Option(decodeURIComponentEx(data.name) + "/" + data.id, data.seq);
							}.bind(this));

							o.user.options[0].text = "- 계정";
							break;
						case "C" :
//							o.place.options[0].text = "- loading";

							e.place.each (function (data, i) {
								o.place.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);
							}.bind(this));

							o.place.options[0].text = "- 설치위치";
							break;
					}
				}.bind(this)
			});
		},
		// 조직
		organDup : function ( company, depth, organ, mode, dsp, isAll) {

			var o = {organ : $("organDup"), change : $("organDup" + depth), auth : $("auth"), user : $("user"), place : $("place")};
			if (o.organ) {
				o.organ.getElementsBySelector("select").findAll(function (s) {
					var _depth = parseInt(s.id.substr(o.organ.id.length, s.id.length));
	
					if (_depth > depth) {
						s.replace("");
					}
				}.bind(this));
			}

			if (organ == "-1") {
				switch (mode) {
				case "A" :
					this.auth(company, depth - 2);
					break;
				case "B" :
					this.user2(company, $("organDup" + (depth - 2)) ? $("organDup" + (depth - 2)).value : -1);
					break;
				case "C" :
					this.place(company, $("organDup" + (depth - 2)) ? $("organDup" + (depth - 2)).value : -1);
					break;
				}

				if (o.change) {
					o.change.replace("");
				}

				return;
			} else if (depth > company.depth) {
				switch (mode) {
				case "A" :
					this.auth(company, depth - 1);
					break;
				case "B" :
					this.user2(company, $("organDup" + (depth - 1)).value);
					break;
				case "C" :
					this.place(company, $("organDup" + (depth - 1)).value);
					break;
				}				
				return;
			}

			if (o.change) { // 다음 단계의 셀렉트 박스가 있을 경우
				o.change.options[0].text = "- loading";

				for (var i = o.change.length - 1; i > 0; i--) {
					o.change.options[i] = null;
				}
			} else {
				if (o.organ)
					o.organ.insert('<select id="' + o.organ.id + depth + '" class="js-example-basic-single js-example-responsive" style="margin-right:4px"><option value="-1">- loading</option></select>');
			}

			switch (mode) {
				case "A" :
					o.auth.options[0].text = "- loading";

					for (var i = o.auth.length - 1; i > 0; i--) {
						o.auth.options[i] = null;
					}

					break;
				case "B" :
					o.user.options[0].text = "- loading";

					for (var i = o.user.length - 1; i > 0; i--) {
						o.user.options[i] = null;
					}

					break;
				case "C" :
					o.place.options[0].text = "- loading";

					for (var i = o.place.length - 1; i > 0; i--) {
						o.place.options[i] = null;
					}

					break;
			}

			new Ajax.Request(G_TOP_DIR + "/common/src/organ2.jsp", {
				asynchronous : true,
				parameters : {company : company.seq, organ : organ, depth : depth, mode : mode, isAll : (isAll ? isAll : "")},
				onSuccess : function (xmlHttp) {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == "FAIL") {
						window.alert("조직 정보를 가져오는데 실패하였습니다.");
						return;
					}

					if (!o.change) {
						o.change = $(o.organ.id + depth);

						if (e.disp == "") {
							o.change.replace("");
							return;
						}

						o.change.observe("change", function (event) {
							this.organDup(company, depth + 1, o.change.options[o.change.selectedIndex].value, mode, null, isAll ? isAll : "");
						}.bind(this));
					}

					e.data.each (function (data, i) {
						o.change.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);

						if (dsp && Common.inArray(data.seq, dsp)) {
							o.change.options[i + 1].selected = true;
						}
					}.bind(this));

					o.change.options[0].text = "- " + (e.disp ? decodeURIComponentEx(e.disp) : "조직");

					if (this.depth >= depth) {
						o.change.disabled = true;
						o.change.addClassName("disabled");
					}

					switch (mode) {
						case "A" :
//							o.auth.options[0].text = "- loading";

							e.auth.each (function (data, i) {
								o.auth.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);
							}.bind(this));

							o.auth.options[0].text = "- 권한";
							break;
						case "B" :
//							o.user.options[0].text = "- loading";

							e.user.each (function (data, i) {
								o.user.options[i + 1] = new Option(decodeURIComponentEx(data.name) + "/" + data.id, data.seq);
								// console.log($('uid'))
								if($('uid').value == data.id)
									o.user.options[i+1].selected = true;
							}.bind(this));
							o.user.options[0].text = "- 계정";
							break;
						case "C" :
//							o.place.options[0].text = "- loading";

							e.place.each (function (data, i) {
								o.place.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);
							}.bind(this));

							o.place.options[0].text = "- 설치위치";
							break;
					}
				}.bind(this)
			});
		},				
	// 권한
		auth : function (company, depth, n) {
			var o = $("auth");

			if (!o || o.tagName.toLowerCase() != "select") {
				return;
			}

			o.options[0] = new Option("- loading", 0);

			for (var i = o.length - 1; i > 0; i--) {
				o.options[i] = null;
			}

			new Ajax.Request(G_TOP_DIR + "/common/src/auth.jsp", {
				asynchronous : true,
				parameters : {company : company.seq, depth : depth ? depth : -1},
				onSuccess : function (xmlHttp) {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == "FAIL") {
						window.alert("권한 정보를 가져오는데 실패하였습니다.");
						return;
					}

					e.data.each (function (data, i) {
						o.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);

						if (data.seq == n) {
							o.options[i + 1].selected = true;
						}
					}.bind(this));

					o.options[0].text = "- 권한";
				}.bind(this)
			});
		},
		// 계정
		user2 : function (company, organ, n) {
			var o = $("user");

			if (!o || o.tagName.toLowerCase() != "select") {
				return;
			}

			o.options[0] = new Option("- loading", 0);

			for (var i = o.length - 1; i > 0; i--) {
				o.options[i] = null;
			}

			new Ajax.Request(G_TOP_DIR + "/common/src/user2.jsp", {
				asynchronous : true,
				parameters : {company : company.seq, organ : organ},
				onSuccess : function (xmlHttp) {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == "FAIL") {
						window.alert("계정 정보를 가져오는데 실패하였습니다.");
						return;
					}

					e.data.each (function (data, i) {
						o.options[i + 1] = new Option(decodeURIComponentEx(data.name) + "/" + data.id, data.seq);

						if (data.seq == n) {
							o.options[i + 1].selected = true;
						}
					}.bind(this));

					o.options[0].text = "- test";
				}.bind(this)
			});
		},		
	// 계정
		user : function (company, organ, n) {
			var o = $("user");

			if (!o || o.tagName.toLowerCase() != "select") {
				return;
			}

			o.options[0] = new Option("- loading", 0);

			for (var i = o.length - 1; i > 0; i--) {
				o.options[i] = null;
			}

			new Ajax.Request(G_TOP_DIR + "/common/src/user.jsp", {
				asynchronous : true,
				parameters : {company : company.seq, organ : organ},
				onSuccess : function (xmlHttp) {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == "FAIL") {
						window.alert("계정 정보를 가져오는데 실패하였습니다.");
						return;
					}

					e.data.each (function (data, i) {
						o.options[i + 1] = new Option(decodeURIComponentEx(data.name) + "/" + data.id, data.seq);

						if (data.seq == n) {
							o.options[i + 1].selected = true;
						}
					}.bind(this));

					o.options[0].text = "- 계정";
				}.bind(this)
			});
		},
	// 설치 위치
		place : function (company, organ, n) {
			var o = $("place");

			if (!o || o.tagName.toLowerCase() != "select") {
				return;
			}

			o.options[0] = new Option("- loading", 0);

			for (var i = o.length - 1; i > 0; i--) {
				o.options[i] = null;
			}

			new Ajax.Request(G_TOP_DIR + "/common/src/place.jsp", {
				asynchronous : true,
				parameters : {company : company.seq, organ : organ},
				onSuccess : function (xmlHttp) {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == "FAIL") {
						window.alert("위치 정보를 가져오는데 실패하였습니다.");
						return;
					}

					e.data.each (function (data, i) {
						o.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);

						if (data.seq == n) {
							o.options[i + 1].selected = true;
						}
					}.bind(this));

					o.options[0].text = "- 설치위치";
				}.bind(this)
			});
		}
	}
