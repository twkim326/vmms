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
	cfg.put("window.mode", "A");

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
		String id = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("id"), ""), "UTF-8");
		String newPass = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("newPass"), ""), "UTF-8");
		String name = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("name"), ""), "UTF-8");
		String employeeNo = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("employeeNo"), ""), "UTF-8");
		String email = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("email"), ""), "UTF-8");
		String telephone = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("telephone"), ""), "UTF-8");
		String cellphone = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("cellphone"), ""), "UTF-8");
		String fax = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("fax"), ""), "UTF-8");
		String enabled = StringEx.setDefaultValue(request.getParameter("enabled"), "N");

		if (StringEx.isEmpty(id) || StringEx.isEmpty(newPass) || StringEx.isEmpty(name) || StringEx.isEmpty(email) || StringEx.isEmpty(cellphone)) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("필수 입력정보가 존재하지 않습니다.", "UTF-8") + "\"}");
			return;
		}

		try {
			error = objUser.regist(company, organ, auth, id, newPass, name, employeeNo, email, telephone, cellphone, fax, enabled);
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

<div class="title">
	<span>계정관리</span>
	<a>* 표시가 된 항목은 필수입력사항입니다.</a>
</div>

<table cellspacing="0" class="tableType03" id="regist">
	<colgroup>
		<col width="130" />
		<col width="*"/>
	</colgroup>
	<tr>
		<th><span>아이디 *</span></th>
		<td>
			<input type="text" id="id" class="checkForm txtInput" maxlength="12" option='{"isMust" : true, "message" : "아이디를 입력하세요."}' onkeyup="unique = false;" />
			<input type="button" value="중복확인" class="button2" onclick="_unique();" />
		</td>
	</tr>
	<tr>
		<th><span>비밀번호 *</span></th>
		<td><input type="password" id="newPass" class="checkForm txtInput" option='{"isMust" : true, "message" : "비밀번호를 입력하세요."}' /></td>
	</tr>
	<tr>
		<th><span>비밀번호 확인 *</span></th>
		<td><input type="password" id="cfmPass" class="checkForm txtInput" option='{"isMust" : true, "message" : "비밀번호를 확인하세요."}' /></td>
	</tr>
	<tr>
		<th><span>이름 *</span></th>
		<td><input type="text" id="name" class="checkForm txtInput" maxlength="12" option='{"isMust" : true, "message" : "이름을 입력하세요."}' /></td>
	</tr>
	<tr>
		<th><span>사번</span></th>
		<td><input type="text" id="employeeNo" class="checkForm txtInput" maxlength="12" /></td>
	</tr>
	<tr>
		<th><span>이메일 </span></th>
		<td>
			<input type="text" id="email_1" option='{"isMust" : true, "message" : "이메일 아이디를 입력하세요."}' />
			@
			<input type="text" id="email_2" option='{"isMust" : true, "message" : "이메일 도메인을 입력하세요."}' />
			<select onchange="$('email_2').value = this.options[selectedIndex].value;">
				<option value="">직접입력</option>
			<% for (String v : StringEx.split(cfg.get("list.email"), ";")) { %>
				<option value="<%=v%>"><%=v%></option>
			<% } %>
			</select>
		</td>
	</tr>
	<tr>
		<th><span>전화번호 </span></th>
		<td>
			<select id="telephone_1" style="width:60px" onchange="$('telephone_2').focus();" option='{"isMust" : true, "message" : "일반전화 지역번호를 선택하세요."}'>
				<option value="">- 선택</option>
			<% for (String v : StringEx.split(cfg.get("list.telephone"), ";")) { %>
				<option value="<%=v%>"><%=v%></option>
			<% } %>
			</select>
			-
			<input type="text" id="telephone_2" style="width:40px;" maxlength="4" onkeyup="Common.moveFocus(4, this, $('telephone_3'));" option='{"isMust" : true, "varType" : "number", "message" : "일반전화 국번을 입력하세요."}' />
			-
			<input type="text" id="telephone_3" style="width:40px;" maxlength="4" option='{"isMust" : true, "equalLength" : true, "varType" : "number", "message" : "일반전화번호를 입력하세요."}' />
			<input type="checkbox" id="telephoe_none" class="checkbox" onclick="_none(this.checked, 'telephone');" /><label for="telephoe_none">없음</label>
		</td>
	</tr>
	<tr>
		<th><span>휴대전화 </span></th>
		<td>
			<select id="cellphone_1" style="width:60px" onchange="$('cellphone_2').focus();" option='{"isMust" : true, "message" : "휴대전화번호를 선택하세요."}'>
				<option value="">- 선택</option>
			<% for (String v : StringEx.split(cfg.get("list.cellphone"), ";")) { %>
				<option value="<%=v%>"><%=v%></option>
			<% } %>
			</select>
			-
			<input type="text" id="cellphone_2" style="width:40px;" maxlength="4" onkeyup="Common.moveFocus(4, this, $('cellphone_3'));" option='{"isMust" : true, "varType" : "number", "message" : "휴대전화번호를 입력하세요."}' />
			-
			<input type="text" id="cellphone_3" style="width:40px;" maxlength="4" option='{"isMust" : true, "equalLength" : true, "varType" : "number", "message" : "휴대전화번호를 입력하세요."}' />
		</td>
	</tr>
	<tr>
		<th><span>팩스번호 </span></th>
		<td>
			<select id="fax_1" style="width:60px" onchange="$('fax_2').focus();" option='{"isMust" : true, "message" : "팩스 지역번호를 선택하세요."}'>
				<option value="">- 선택</option>
			<% for (String v : StringEx.split(cfg.get("list.fax"), ";")) { %>
				<option value="<%=v%>"><%=v%></option>
			<% } %>
			</select>
			-
			<input type="text" id="fax_2" style="width:40px;" maxlength="4" onkeyup="Common.moveFocus(4, this, $('fax_3'));" option='{"isMust" : true, "varType" : "number", "message" : "팩스 국번을 입력하세요."}' />
			-
			<input type="text" id="fax_3" style="width:40px;" maxlength="4" option='{"isMust" : true, "equalLength" : true, "varType" : "number", "message" : "팩스번호를 입력하세요."}' />
			<input type="checkbox" id="fax_none" class="checkbox" onclick="_none(this.checked, 'fax');" /><label for="fax_none">없음</label>
		</td>
	</tr>
	<tr>
		<th><span>소속</span></th>
		<td>
			<select id="company" class="<%=(cfg.getLong("user.company") > 0 ? "disabled" : "")%> js-example-basic-single js-example-responsive" style="width: 40%" onchange="Company.organ(this.options[selectedIndex].value.evalJSON(), 0, 0, 'A');"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
				<option value='{"seq" : 0, "depth" : 0}'>- 소속</option>
			</select>
		<% if (cfg.getLong("user.company") == 0) { %>
			<a class="desc">* 소속을 선택하지 않을 경우, 시스템 관리자로 등록이 됩니다.</a>
		<% } %>
		</td>
	</tr>
	<tr>
		<th><span>조직</span></th>
		<td>
			<span id="organ">
				<select id="organ0" onchange="Company.organ($('company').value.evalJSON(), 1, this.options[selectedIndex].value, 'A');" class="js-example-basic-single js-example-responsive" style="width: 40%">
					<option value="-1">- 조직</option>
				</select>
			</span>
		</td>
	</tr>
	<tr>
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
	<input type="button" value="" class="btnRegi" onclick="_save();" />
	<input type="button" value="" class="btnList" onclick="location.href = 'SystemUser.jsp?1=1<%=addParam%>';" />
</div>

<%@ include file="../../footer.inc.jsp" %>

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
		var o = {id : $('id'), pass : $('newPass')};

		if (o.id.value == '') {
			window.alert('아이디를 입력하세요.');
			o.id.focus();
			return;
		} else if (o.id.value.match(/[^a-z0-9]/g)) {
			window.alert('아이디를 영(소)문+숫자로 입력하세요.');
			o.id.value = '';
			o.id.focus();
			return;
		}

		unique = false;

		new Ajax.Request('SystemUserUniqueID.jsp', {
			asynchronous : false,
			parameters : {id : encodeURIComponentEx($('id').value)},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						if (confirm('사용 가능한 아이디입니다.\n\n해당 아이디를 사용하시겠습니까?')) {
							o.pass.focus();
							unique = true;
						} else {
							o.id.value = '';
							o.id.focus();
						}
					} else {
						window.alert(decodeURIComponentEx(e.message));
						o.id.value = '';
						o.id.focus();
					}
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
					o.id.value = '';
					o.id.focus();
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

		if ($('id').value.match(/[^a-z0-9]/g)) {
			window.alert("아이디를 영(소)문+숫자로 입력하세요.");
			$('id').value = '';
			$('id').focus();
			return;
		} else if (!unique) {
			window.alert('아이디 중복확인을 하세요.');
			return;
		} else if ($('newPass').value != $('cfmPass').value) {
			window.alert('비밀번호가 일치하지 않습니다.');
			$('newPass').value = '';
			$('cfmPass').value = '';
			$('newPass').focus();
			return;
		} else if ((!Common.isAvailableEmail($('email_1').value + '@' + $('email_2').value)) && $('email_1').value != '' ) {
			window.alert('유효하지 않은 이메일 정보입니다.');
			$('email_1').value = '';
			$('email_2').value = '';
			$('email_1').focus();
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

		/* 2011.04.20, 정원광// if (com.seq > 0 && !organ) {
			window.alert('조직을 선택하세요.');
			return;
		} else if (com.seq > 0 && Company.depth >= depth) {
			window.alert('조직을 선택하세요.');
			return;
		} else */if (com.seq > 0 && obj.auth.selectedIndex == 0) {
			window.alert('권한을 선택하세요.');
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
			,	id : encodeURIComponentEx($('id').value)
			,	newPass : encodeURIComponentEx($('newPass').value)
			,	name : encodeURIComponentEx($('name').value)
			,	employeeNo : encodeURIComponentEx($('employeeNo').value)
			,	email : encodeURIComponentEx($('email_1').value + '@' + $('email_2').value)
			,	telephone : encodeURIComponentEx($('telephone_1').value + '-' + $('telephone_2').value + '-' + $('telephone_3').value)
			,	cellphone : encodeURIComponentEx($('cellphone_1').value + '-' + $('cellphone_2').value + '-' + $('cellphone_3').value)
			,	fax : encodeURIComponentEx($('fax_1').value + '-' + $('fax_2').value + '-' + $('fax_3').value)
			,	enabled : $('enabled').checked ? 'Y' : 'N'
			},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('등록이 완료되었습니다.');
						location.href = 'SystemUser.jsp?1=1<%=addParam%>';
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