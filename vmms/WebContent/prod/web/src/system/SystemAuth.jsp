<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.Auth
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /system/SystemAuth.jsp
 *
 * 시스템 > 권한관리
 *
 * 작성일 - 2011/03/24, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0103");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(cfg.login());
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth()) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "A");

// 인스턴스 생성
	Auth objAuth = new Auth(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		if (!cfg.isAuth("U")) {
			out.print(Message.alert("접근이 불가능한 페이지입니다.", 0, "parent", "try { parent._clear(); } catch (e) {}"));
			return;
		}

		long company = StringEx.str2long(request.getParameter("company"), 0);
		long auth = StringEx.str2long(request.getParameter("auth"), 0);
		int depth = StringEx.str2int(request.getParameter("depth"), -1);
		String isAppOrgan = StringEx.setDefaultValue(request.getParameter("isAppOrgan"), "N");
		int mcnt = StringEx.str2int(request.getParameter("mcnt"), 0);
		int scnt = StringEx.str2int(request.getParameter("scnt"), 0);

		if (company == 0) {
			out.print(Message.alert("소속을 선택하세요.", 0, "parent", "try { parent._clear(); } catch (e) {}"));
			return;
		} else if (auth == 0) {
			out.print(Message.alert("권한을 선택하세요.", 0, "parent", "try { parent._clear(); } catch (e) {}"));
			return;
		}

		try {
			error = objAuth.setMenu(request, company, auth, depth, isAppOrgan, mcnt, scnt);
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null) {
			out.print(Message.alert(error, 0, "parent", "try { parent._clear(); } catch (e) {}"));
			return;
		}

//		out.print(Message.alert("설정하신 내용의 등록이 완료되었습니다.", 0, "parent", "try { parent._clear(); parent._company(" + company + "); } catch (e) {}"));
		out.print(Message.alert("설정하신 내용의 등록이 완료되었습니다.", 0, "parent", "try { parent._clear(); } catch (e) {}"));
		return;
	} else {
		try {
			error = objAuth.regist();
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null) {
			out.print(error);
			return;
		}
	}
%>
<%@ include file="../../header.inc.jsp" %>

<div class="title">
	<span>권한관리</span>
</div>

<form method="post" name="save_" id="save_" target="__save">
<table cellspacing="0" class="tableType02">
	<colgroup>
		<col width="110" />
		<col width="*"/>
		<col width="73" />
	</colgroup>
	<tr>
		<th><span>소속</span></th>
		<td>
			<input type="hidden" name="company" id="company" value="<%=cfg.getLong("user.company")%>" />
			<select name="company_" id="company_" class="js-example-basic-single js-example-responsive" style="width: 20%" class="checkForm<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>" onchange="_company(this.options[selectedIndex].value)" option='{"isMust" : true, "message" : "소속을 선택하세요."}'<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
				<option value="0">- 선택하세요</option>
		<% for (int i = 0; i < objAuth.company.size(); i++) { GeneralConfig c = (GeneralConfig) objAuth.company.get(i); %>
				<option value="<%=c.getLong("SEQ")%>"<%=(cfg.getLong("user.company") == c.getLong("SEQ") ? " selected" : "")%>><%=c.get("NAME")%></option>
		<% } %>
			</select>
			<span>권한</span>
			<input type="hidden" name="auth" id="auth" />
			<select name="auth_" id="auth_" class="checkForm" onchange="_auth(this.options[selectedIndex].value);" option='{"isMust" : true, "message" : "권한을 선택하세요."}'>
				<option value='{"seq" : 0, "depth" : -1}'>- 선택하세요</option>
			</select>
		<% if (cfg.isAuth("U")) { %>
			<input type="button" class="button1" value="수정" onclick="(function () {
					var e = event ? event : window.event;
					_modify(Event.pointerX(e), Event.pointerY(e));
				})(event)" />
		<% } %>
		<% if (cfg.isAuth("D")) { %>
			<input type="button" class="button1" value="삭제" onclick="_delete();" />
		<% } %>
		<% if (cfg.isAuth("I")) { %>
			<input type="button" class="button2" value="등록" onclick="(function () {
					var e = event ? event : window.event;
					_regist(Event.pointerX(e), Event.pointerY(e));
				})(event)" />
		<% } %>
		</td>
	</tr>
</table>

<div class="title mt18">
	<span>계정 검색/등록 시 노출할 위치</span>
</div>

<table cellspacing="0" class="tableType04">
	<colgroup>
		<col width="*"/>
	</colgroup>
	<tr>
		<td id="organList">
			<a>* 소속을 선택하세요.</a>
		</td>
	</tr>
</table>

<div class="title mt18">
	<span>사용 메뉴 설정</span>
	<input type="button" class="button2" value="전체선택" onclick="_all();" style="position:absolute; right:0; bottom:2px;" />
</div>

<table cellspacing="0" class="tableType04">
	<tr>
		<td>
			<div class="box" id="programList">
				<ul>
<% for (int i = 0; i < objAuth.mProgram.size(); i++) {
	GeneralConfig m = (GeneralConfig) objAuth.mProgram.get(i);

	out.println("<li class='m'><input type='checkbox' id='aList" + m.get("SEQ") + "' value='{\"seq\" : \"" + m.get("SEQ") + "\", \"parent\" : \"00\", \"depth\" : 0, \"name\" : \"" + m.get("NAME") + "\"}' class='checkbox' disabled /><label for='aList" + m.get("SEQ") + "'>" + m.get("NAME") + "</label>");
	out.println("<ol id='sList" + m.get("SEQ") + "'>");

	for (int j = 0; j < objAuth.sProgram.size(); j++) {
		GeneralConfig s = (GeneralConfig) objAuth.sProgram.get(j);

		if (s.get("PARENT_SEQ").equals(m.get("SEQ"))) {
			out.println("<li><input type='checkbox' id='aList" + s.get("SEQ") + "' value='{\"seq\" : \"" + s.get("SEQ") + "\", \"parent\" : \"" + s.get("PARENT_SEQ") + "\", \"depth\" : 1, \"name\" : \"" + s.get("NAME") + "\"}' class='checkbox' onclick='return _select();' /><label for='aList" + s.get("SEQ") + "'>" + s.get("NAME") + "</label></li>");
		}
	}

	out.println("</ol>");
	out.println("</li>");
}
%>
				</ul>
			</div>
			<div class="arr"></div>
			<div class="box" id="menuList" style="width:600px;">
				<input type="hidden" name="mcnt" value="<%=objAuth.mProgram.size()%>" />
				<input type="hidden" name="scnt" value="<%=objAuth.sProgram.size()%>" />
				<ul>
<% for (int i = 0; i < objAuth.mProgram.size(); i++) {
	GeneralConfig m = (GeneralConfig) objAuth.mProgram.get(i);

	out.println("<li class='m' id='mList" + m.get("SEQ") + "' style='display:none'>");
	out.println("<input type='hidden' name='mseq" + i + "' value='" + m.get("SEQ") + "' />");
	out.println("<input type='hidden' name='muse" + i + "' id='mAble" + m.get("SEQ") + "' value='N' />");
	out.println(m.get("NAME"));
	out.println("<ol>");

	for (int j = 0; j < objAuth.sProgram.size(); j++) {
		GeneralConfig s = (GeneralConfig) objAuth.sProgram.get(j);

		if (s.get("PARENT_SEQ").equals(m.get("SEQ"))) {
			out.println("<li id='mList" + s.get("SEQ") + "' style='display:none'>");
			out.println("<input type='hidden' name='sseq" + j + "' value='" + s.get("SEQ") + "' />");
			out.println("<input type='hidden' name='suse" + j + "' id='mAble" + s.get("SEQ") + "' value='N' />");
			out.println("<a class='l'>" + s.get("NAME") + "</a>");
			out.print("<a class='r'>");
			out.print("<input type='checkbox' name='S" + j + "' id='APP_S" + s.get("SEQ") + "' value='Y' class='checkbox'" + (s.get("SUPPORT_EXEC").indexOf("S") >= 0 ? "" : " disabled") + " /><label for='APP_S" + s.get("SEQ") + "'>조회</label>");
			out.print("<input type='checkbox' name='I" + j + "' id='APP_I" + s.get("SEQ") + "' value='Y' class='checkbox'" + (s.get("SUPPORT_EXEC").indexOf("I") >= 0 ? "" : " disabled") + " /><label for='APP_I" + s.get("SEQ") + "'>등록</label>");
			out.print("<input type='checkbox' name='U" + j + "' id='APP_U" + s.get("SEQ") + "' value='Y' class='checkbox'" + (s.get("SUPPORT_EXEC").indexOf("U") >= 0 ? "" : " disabled") + " /><label for='APP_U" + s.get("SEQ") + "'>수정</label>");
			out.print("<input type='checkbox' name='D" + j + "' id='APP_D" + s.get("SEQ") + "' value='Y' class='checkbox'" + (s.get("SUPPORT_EXEC").indexOf("D") >= 0 ? "" : " disabled") + " /><label for='APP_D" + s.get("SEQ") + "'>삭제</label>");
			out.println("</a>");
			out.println("</li>");
		}
	}

	out.println("</ol>");
	out.println("</li>");
}
%>
				</ul>
			</div>
		</td>
	</tr>
</table>

<div class="buttonArea">
<% if (cfg.isAuth("U")) { %>
	<input type="button" value="" class="btnRegi" onclick="_save();" />
<% } %>
</div>
</form>

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<iframe src="about:blank" name="__save" id="__save" style="display:none"></iframe>
<style type="text/css">
	#boxLayer div {text-align:center;}
	#boxLayer .h {padding:15px 0 0 0; font-size:12px; line-height:120%;}
	#boxLayer .p input.txt {width:175px; height:19px; margin:5px 0 7px 0; border-color:#E4E2E0; border-width:1px; border-style:solid;}
	#boxLayer .b input.button1 {margin:0 2px 0 2px;}
	div.box {width:250px; height:200px; padding:5px; overflow:auto; float:left; border-width:1px; border-style:solid; border-color:#ccc;}
	div.box ul li {font-weight:bold;}
	div.box ul li ol {margin:5px 0 5px 10px;}
	div.box ul li ol li {font-weight:normal;}
	div.box ul li ol li a.l {width:360px; display:inline-block; cursor:default; font-size:12px; color:#555;}
	div.box ul li ol li a.r {display:inline-block; font-size:12px; color:#555;}
	div.arr { width:50px; height:210px; background:url(<%=cfg.get("imgDir")%>/web/icon_arr_right.gif) no-repeat center center; float:left; }
</style>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/layer/js/layer.js"></script>
<script type="text/javascript">
	function _company(n) {
		var o = {auth : $('auth_'), organList : $('organList')};

		$('sbmsg').show();

		for (var i = o.auth.length - 1; i > 0; i--) {
			o.auth.options[i] = null;
		}

		if (n <= 0) {
			o.organList.update('<a>* 소속을 선택하세요.</a>');
			$('sbmsg').hide();
			return;
		}

		new Ajax.Request('SystemAuthDetail.jsp', {
			asynchronous : true,
			parameters : {company : n},
			onSuccess : function (xmlHttp) {
				var e = xmlHttp.responseText.evalJSON();
				var s = '<input type="radio" name="depth" id="depth-1" value="-1" class="checkbox" /><label for="depth-1">전체</label> ';

				if (e.code == 'FAIL') {
					window.alert(e.message ? decodeURIComponentEx(e.message) : '소속 정보를 가져오는데 실패하였습니다.');
					$('sbmsg').hide();
					return;
				}

				e.auth.each (function (data, i) {
					o.auth.options[i + 1] = new Option(decodeURIComponentEx(data.name), '{"seq" : ' + data.seq + ', "depth" : ' + data.depth + ', "isAppOrgan" : "' + data.isAppOrgan + '", "isAbleAppOrgan" : "' + data.isAbleAppOrgan + '"}');
				}.bind(this));

				o.auth.options[0].text = '- 선택하세요';

				e.organ.each (function (data, i) {
					s += '<input type="radio" name="depth" id="depth' + data.depth + '" value="' + data.depth + '" class="checkbox" /><label for="depth' + i + '">' + decodeURIComponentEx(data.name) + '</label> ';
				}.bind(this));

				o.organList.update(s + '<a style="margin-right:12px;">|</a><input type="checkbox" name="isAppOrgan" id="isAppOrgan" value="Y" class="checkbox" /><label for="isAppOrgan">매출조회 조직추가 여부</label>');

				$('sbmsg').hide();
			}.bind(this)
		});
	}

	function _auth(v) {
		var o = {auth : v.evalJSON()};

		$('sbmsg').show();

		$('programList').getElementsBySelector('input').findAll(function (s) {
			s.checked = false;
		}.bind(this));

		_select();

		new Ajax.Request('SystemAuthDetail.jsp', {
			asynchronous : true,
			parameters : {auth : o.auth.seq},
			onSuccess : function (xmlHttp) {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == 'FAIL') {
					window.alert(e.message ? decodeURIComponentEx(e.message) : '소속 정보를 가져오는데 실패하였습니다.');
					$('sbmsg').hide();
					return;
				}

				e.menu.each (function (data, i) {
					$('aList' + data.seq).checked = true;
					$('mList' + data.seq).show();
					$('mAble' + data.seq).value = 'Y';

					if (data.depth == 1) {
						$('APP_S' + data.seq).checked = (data.S == 'Y');
						$('APP_I' + data.seq).checked = (data.I == 'Y');
						$('APP_U' + data.seq).checked = (data.U == 'Y');
						$('APP_D' + data.seq).checked = (data.D == 'Y');
					}
				}.bind(this));

				Common.radio(document.save_.depth, 2, o.auth.depth);

// -- 2011.04.20, 정원광
//				if (o.auth.isAbleAppOrgan == 'Y') {
//					$('isAppOrgan').checked = (o.auth.isAppOrgan == 'Y') ? true : false;
//					$('isAppOrgan').disabled = false;
//					$('isAppOrgan').removeClassName('disabled');
//				} else {
//					$('isAppOrgan').checked = false;
//					$('isAppOrgan').disabled = true;
//					$('isAppOrgan').addClassName('disabled');
//				}
				$('isAppOrgan').checked = (o.auth.isAppOrgan == 'Y') ? true : false;
// --

				$('sbmsg').hide();
			}.bind(this)
		});

		$('auth').value = o.auth.seq;
	}

	function _select(isAll) {
		$('programList').getElementsBySelector('input').findAll(function (s) {
			var o = s.value.evalJSON();

			if (o.depth == 0) {
				var _checked = false;

				$('sList' + o.seq).getElementsBySelector('input').findAll(function (_s) {
					if (_s.checked) {
						_checked = true;
					}
				}.bind(this));

				s.checked = _checked;
			}

			if (s.checked) {
				$('mList' + o.seq).show();
				$('mAble' + o.seq).value = 'Y';

				if (o.depth == 1 && isAll) {
					$('APP_S' + o.seq).checked = $('APP_S' + o.seq).disabled ? false : true;
					$('APP_I' + o.seq).checked = $('APP_I' + o.seq).disabled ? false : true;
					$('APP_U' + o.seq).checked = $('APP_U' + o.seq).disabled ? false : true;
					$('APP_D' + o.seq).checked = $('APP_D' + o.seq).disabled ? false : true;
				}
			} else {
				$('mList' + o.seq).hide();
				$('mAble' + o.seq).value = 'N';

				if (o.depth == 1) {
					$('APP_S' + o.seq).checked = false;
					$('APP_I' + o.seq).checked = false;
					$('APP_U' + o.seq).checked = false;
					$('APP_D' + o.seq).checked = false;
				}
			}
		}.bind(this));

		return true;
	}

	function _save() {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		}

		var sForm = $('save_');
		var error = Common.checkForm();

		if (error != '') {
			window.alert(error);
			return;
		} else if (!Common.radio(sForm.depth)) {
			window.alert('계정 검색/등록 시 노출할 위치를 선택하세요.');
			return;
		} else if (!confirm('설정하신 내용을 등록하시겠습니까?')) {
			return;
		}

		$('company').value = $('company_').value;
		$('sbmsg').show();
		$('save_').submit();
	}

	function _clear() {
		$('sbmsg').hide();
	}

	function _all() {
		if ($('company_').selectedIndex == 0) {
			window.alert('소속을 선택하세요.');
			return;
		} else if ($('auth_').selectedIndex == 0) {
			window.alert('권한을 선택하세요.');
			return;
		}

		$('programList').getElementsBySelector('input').findAll(function (s) {
			s.checked = true;
		}.bind(this));

		_select(1);
	}

	function _regist(x, y, value) {
		var o = {company : $('company_'), auth : $('auth_'), layer : null};

		if (o.company.selectedIndex == 0) {
			window.alert('소속을 선택하세요.');
			return;
		}

		if (typeof value != 'undefined') {
			if (value == '') {
				window.alert('등록하실 권한 이름을 입력하세요.');
				return;
			} else if (!confirm('입력하신 권한을 등록하시겠습니까?')) {
				return;
			}

			new Ajax.Request('SystemAuthRegist.jsp', {
				asynchronous : false,
				parameters : {company : o.company.value, name : encodeURIComponentEx(value)},
				onSuccess : function (xmlHttp) {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'FAIL') {
						window.alert(e.message ? decodeURIComponentEx(e.message) : '예기치 않은 오류가 발생하여 등록에 실패하였습니다.');
						return;
					}

					window.alert('권한 등록이 완료되었습니다.');
					(new Layer()).close();

					o.auth.options[o.auth.length] = new Option(value, '{"seq" : ' + e.seq + ', "depth" : -1}');
					o.auth.selectedIndex = o.auth.length - 1;

					_auth(o.auth.options[o.auth.selectedIndex].value);
				}.bind(this)
			});

			return;
		}

		o.layer = new Layer(230, 100, '<div id="boxLayer">'
			+ '<div class="h">등록하실 권한 이름을 입력하세요.</div>'
			+ '<div class="p"><input type="text" id="boxLayerName" class="txt" /></div>'
			+ '<div class="b"><input type="button" class="button1" id="boxLayerRegist" value="등록" /><input type="button" class="button1" id="boxLayerCancel" value="취소" /></div>'
			+ '</div>');
		o.layer.open(function () {
			$('boxLayerCancel').observe('click', function () { o.layer.close(); });
			$('boxLayerRegist').observe('click', function () { _regist(x, y, $('boxLayerName').value); }.bind(this));
			$('boxLayerName').focus();
		}.bind(this), x, y);
	}

	function _modify(x, y, value) {
		var o = {company : $('company_'), auth : $('auth_'), layer : null};

		if (o.company.selectedIndex == 0) {
			window.alert('소속을 선택하세요.');
			return;
		} else if (o.auth.selectedIndex == 0) {
			window.alert('권한을 선택하세요.');
			return;
		}

		if (typeof value != 'undefined') {
			if (value == '') {
				window.alert('수정하실 권한 이름을 입력하세요.');
				return;
			} else if (!confirm('선택하신 권한을 수정하시겠습니까?')) {
				return;
			}

			new Ajax.Request('SystemAuthRegist.jsp', {
				asynchronous : false,
				parameters : {seq : o.auth.value.evalJSON().seq, company : o.company.value, name : encodeURIComponentEx(value)},
				onSuccess : function (xmlHttp) {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'FAIL') {
						window.alert(e.message ? decodeURIComponentEx(e.message) : '예기치 않은 오류가 발생하여 수정에 실패하였습니다.');
						return;
					}

					window.alert('권한 수정이 완료되었습니다.');
					(new Layer()).close();

					o.auth.options[o.auth.selectedIndex].text = value;
				}.bind(this)
			});

			return;
		}

		o.layer = new Layer(230, 100, '<div id="boxLayer">'
			+ '<div class="h">수정하실 권한 이름을 입력하세요.</div>'
			+ '<div class="p"><input type="text" id="boxLayerName" class="txt" /></div>'
			+ '<div class="b"><input type="button" class="button1" id="boxLayerRegist" value="수정" /><input type="button" class="button1" id="boxLayerCancel" value="취소" /></div>'
			+ '</div>');
		o.layer.open(function () {
			$('boxLayerCancel').observe('click', function () { o.layer.close(); });
			$('boxLayerRegist').observe('click', function () { _modify(x, y, $('boxLayerName').value); }.bind(this));
			$('boxLayerName').value = o.auth.options[o.auth.selectedIndex].text;
		}.bind(this), x, y);
	}

	function _delete() {
		var o = {company : $('company_'), auth : $('auth_')};

		if (o.company.selectedIndex == 0) {
			window.alert('소속을 선택하세요.');
			return;
		} else if (o.auth.selectedIndex == 0) {
			window.alert('권한을 선택하세요.');
			return;
		} else if (!confirm('선택하신 권한을 삭제하시겠습니까?')) {
			return;
		}

		new Ajax.Request('SystemAuthDelete.jsp', {
			asynchronous : false,
			parameters : {seq : o.auth.value.evalJSON().seq},
			onSuccess : function (xmlHttp) {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == 'FAIL') {
					window.alert(e.message ? decodeURIComponentEx(e.message) : '예기치 않은 오류가 발생하여 삭제에 실패하였습니다.');
					return;
				}

				window.alert('권한 삭제가 완료되었습니다.');

				for (var i = 0; i < o.auth.length; i++) {
					if (o.auth.options[i].selected) {
						o.auth.options[i] = null;
						break;
					}
				}

				$('programList').getElementsBySelector('input').findAll(function (s) {
					s.checked = false;
				}.bind(this));

				_select();
			}.bind(this)
		});
	}

//	Event.observe(window, 'load', function (event) {
		<% if (cfg.getLong("user.company") > 0) { %>_company(<%=cfg.getLong("user.company")%>);<% } %>
//	}.bind(this));
</script>