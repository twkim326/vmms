<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.User
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /system/SystemUserRegist.jsp
 *
 * 시스템 > 계정 > 등록
 *
 * 작성일 - 2011/03/23, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0101");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(cfg.login());
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth("I")) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "B");

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "page:company:organ:auth:sField:sQuery");

// 인스턴스 생성
	User objUser = new User(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		long company = StringEx.str2long(request.getParameter("company"), 0);
		long organ = StringEx.str2long(request.getParameter("organ"), 0);
		long auth = StringEx.str2long(request.getParameter("auth"), 0);
		String name = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("name"), ""), "UTF-8");
		String employeeNo = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("employeeNo"), ""), "UTF-8");
		String enabled = StringEx.setDefaultValue(request.getParameter("enabled"), "N");

		if ( StringEx.isEmpty(name) || StringEx.isEmpty(employeeNo)) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("필수 입력정보가 존재하지 않습니다.", "UTF-8") + "\"}");
			return;
		}

		try {
			error = objUser.regist(company, organ, name, employeeNo, enabled);
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null) {
			out.print("{\"code\" : \"FAILv, \"message\" : \"" + URLEncoder.encode(error.replaceAll("'", ""), "UTF-8") + "\"}");
			return;
		}

		out.print("{\"code\" : \"SUCCESS\"}");
		return;
	}
%>
<%@ include file="../../header.inc.jsp" %>
<div id="window">
	<div class="title">
		<span>운영자등록</span>
		<a>* 표시가 된 항목은 필수입력사항입니다.</a>
	</div>
	
	<table cellspacing="0" class="tableType03 tableType05" id="regist">
		<colgroup>
			<col width="130" />
			<col width="*"/>
		</colgroup>
		<tr>
			<th><span>이름 *</span></th>
			<td><input type="text" id="name" class="checkForm txtInput" maxlength="12" option='{"isMust" : true, "message" : "이름을 입력하세요."}' /></td>
		</tr>
		<tr>
			<th><span>사번 *</span></th>
			<td>
				<input type="text" id="employeeNo" class="checkForm txtInput" maxlength="12" option='{"isMust" : true, "message" : "사번을 입력하세요."}' /> 
				<input type="button" value="중복확인" class="button2" onclick="_unique();" />
				<a class="desc" style="padding-top:5px"> * 아이디와 비밀번호는 사번으로 설정됩니다.<br/> * 비밀번호는 계정관리에서 변경할 수 있습니다.</a>
			</td>
		</tr>
		<tr>
			<th><span>소속 *</span></th>
			<td>
				<select id="company" class="<%=(cfg.getLong("user.company") > 0 ? "disabled" : "")%>" onchange="Company.organ(this.options[selectedIndex].value.evalJSON(), 0, 0, 'A');"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
					<option value='{"seq" : 0, "depth" : 0}'>- 소속</option>
				</select>
			</td>
		</tr>
		<tr>
			<th><span>조직</span></th>
			<td>
				<span id="organ">
					<select id="organ0" onchange="Company.organ($('company').value.evalJSON(), 1, this.options[selectedIndex].value, 'A');">
						<option value="-1">- 조직</option>
					</select>
				</span>
			</td>
		</tr>
		<tr style="display:none">
			<th><span>권한</span></th>
			<td>
				<select id="auth">
					<option value="-1">- 권한</option>
				</select>
			</td>
		</tr>
		<tr>
			<th class="last"><span>계정 활성화</span></th>
			<td class="last"><input type="checkbox" id="enabled" value="Y" class="checkbox" /><label for="enabled">사용 가능한 계정입니다.</label></td>
		</tr>
	</table>
	
	<div class="buttonArea">
		<input type="button" value="" class="btnRegiS" onclick="_save();" />
		<input type="button" value="" class="btnCancelS" onclick="new parent.IFrame().close();" />
	</div>

<%@ include file="../../footer.inc.jsp" %>
</div>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script type="text/javascript">
	var unique = false;

	function _none(v, o) {
		for (var i = 1; i <= 3; i++) {
			var obj = $(o + '_' + i);

			if (v) {
				obj.value = '';
				obj.disabled = true;
				obj.addClassName('disabled');
			} else {
				obj.disabled = false;
				obj.removeClassName('disabled');
			}
		}
	}

	function _unique() {
		var o = {employeeNo : $('employeeNo')};

		if (o.employeeNo.value == '') {
			window.alert('사번을 입력하세요.');
			o.id.focus();
			return;
		} 

		unique = false;

		new Ajax.Request('SystemUserUniqueID.jsp', {
			asynchronous : false,
			parameters : {id : encodeURIComponentEx($('employeeNo').value)},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						if (confirm('사용 가능한 사번입니다.\n\n해당 사번을 사용하시겠습니까?')) {
							unique = true;
						} else {
							o.employeeNo.value = '';
							o.employeeNo.focus();
						}
					} else {
						window.alert(decodeURIComponentEx(e.message));
						o.employeeNo.value = '';
						o.employeeNo.focus();
					}
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
					o.employeeNo.value = '';
					o.employeeNo.focus();
				}
			}.bind(this)
		});
	}

	function _save() {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		}

		var error = Common.checkForm($('regist'));

		if (error != '') {
			window.alert(error);
			return;
		}

		if ($('employeeNo').value.match(/[^a-zA-Z0-9]/g)) {
			window.alert("사번을 영문+숫자로 입력하세요.");
			$('employeeNo').value = '';
			$('employeeNo').focus();
			return;
		} else if (!unique) {
			window.alert('사번 중복확인을 하세요.');
			return;
		} 

		var obj = {company : $('company'), organ : null, auth : $('auth')};
		var com = {seq : 0, depth : 0};
		var organ = 0;
		var depth = 0;

		if (obj.company.selectedIndex > 0) {
			com = obj.company.options[obj.company.selectedIndex].value.evalJSON();

			for (var i = com.depth; i >= 0; i--) {
				obj.organ = $('organ' + i);

				if (obj.organ && obj.organ.selectedIndex > 0) {
					organ = obj.organ.options[obj.organ.selectedIndex].value;
					depth = i;
					break;
				}
			}
		}

		 if (com.seq <= 0 ) {
			window.alert('소속을 선택하세요.');
			return;
		} else if (!confirm('등록하시겠습니까?')) {
			return;
		}

		$('sbmsg').show();

		new Ajax.Request(location.pathname, {
			asynchronous : true,
			parameters : {
				company : com.seq
			,	organ : organ
			,	auth : obj.auth.options[obj.auth.selectedIndex].value
			,	name : encodeURIComponentEx($('name').value)
			,	employeeNo : encodeURIComponentEx($('employeeNo').value)
			,	enabled : $('enabled').checked ? 'Y' : 'N'
			},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('등록이 완료되었습니다.');
						parent.location.reload();
					} else {
						window.alert(decodeURIComponentEx(e.message));
					}
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}

				$('sbmsg').hide();
			}.bind(this)
		});
	}

//	Event.observe(window, 'load', function (event) {
		Company.depth = <%=cfg.getInt("user.organ.depth")%>;
		Company.company(<%=cfg.getLong("user.company")%>, <%=cfg.getLong("user.organ")%>, <%=cfg.getLong("user.auth")%>);
//	}.bind(this));
</script>