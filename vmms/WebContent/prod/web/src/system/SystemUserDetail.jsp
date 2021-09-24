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
 * /system/SystemUserDetail.jsp
 *
 * 시스템 > 계정 > 조회/수정
 *
 * 작성일 - 2011/03/22, 정원광
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
	if (!cfg.isAuth()) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "A");

// 전송된 데이터
	long seq = StringEx.str2long(request.getParameter("seq"), 0);

	if (seq == 0) {
		out.print("계정번호가 존재하지 않습니다.");
		return;
	}

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "page:company:organ:auth:sField:sQuery");

// 인스턴스 생성
	User objUser = new User(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		if (!cfg.isAuth("U")) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("접근이 불가능한 페이지입니다.", "UTF-8") + "\"}");
			return;
		}

		long company = StringEx.str2long(request.getParameter("company"), 0);
		long organ = StringEx.str2long(request.getParameter("organ"), 0);
		long auth = StringEx.str2long(request.getParameter("auth"), 0);
		String newPass = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("newPass"), ""), "UTF-8");
		String employeeNo = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("employeeNo"), ""), "UTF-8");
		String email = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("email"), ""), "UTF-8");
		String telephone = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("telephone"), ""), "UTF-8");
		String cellphone = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("cellphone"), ""), "UTF-8");
		String smsSoldout = StringEx.setDefaultValue(request.getParameter("sms_soldout"), "N");
		String smsState = StringEx.setDefaultValue(request.getParameter("sms_state"), "N");
		String smsClosing = StringEx.setDefaultValue(request.getParameter("sms_closing"), "N");
		String fax = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("fax"), ""), "UTF-8");
		String enabled = StringEx.setDefaultValue(request.getParameter("enabled"), "N");
		String kakaoStatus = StringEx.setDefaultValue(request.getParameter("kakaoStatus"), "N");
		
		if (StringEx.isEmpty(email) || StringEx.isEmpty(cellphone)) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("필수 입력정보가 존재하지 않습니다.", "UTF-8") + "\"}");
			return;
		}

		try {
			//error = objUser.update(seq, company, organ, auth, newPass, employeeNo, email, telephone, cellphone, fax, enabled);
			//error = objUser.update(seq, company, organ, auth, newPass, employeeNo, email, telephone, cellphone, fax, enabled, smsSoldout, smsState);
			error = objUser.update(seq, company, organ, auth, newPass, employeeNo, email, telephone, cellphone, fax, enabled, smsSoldout, smsState, smsClosing, kakaoStatus);
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode(error.replaceAll("'", ""), "UTF-8") + "\"}");
			return;
		}

		out.print("{\"code\" : \"SUCCESS\"}");
		return;
	} else {
		try {
			error = objUser.detail(seq);
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
		<td><%=Html.getText(objUser.data.get("ID"))%></td>
	</tr>
<% if (cfg.isAuth("U")) { %>
	<tr>
		<th><span>비밀번호 변경</span></th>
		<td><input type="password" id="newPass" class="checkForm txtInput" /></td>
	</tr>
	<tr>
		<th><span>비밀번호 확인</span></th>
		<td><input type="password" id="cfmPass" class="checkForm txtInput" /></td>
	</tr>
<% } %>
	<tr>
		<th><span>이름 *</span></th>
		<td><%=Html.getText(objUser.data.get("NAME"))%></td>
	</tr>
<% if (cfg.isAuth("U")) { %>
	<tr>
		<th><span>사번</span></th>
		<td><input type="text" id="employeeNo" class="checkForm txtInput" value="<%=Html.getText(objUser.data.get("EMPLOYEE_NO"))%>" maxlength="12" /></td>
	</tr>
	<tr>
		<th><span>이메일 </span></th>
		<td>
			<input type="text" id="email_1" value="<%=Html.getText(objUser.data.get("EMAIL_1"))%>" option='{"isMust" : true, "message" : "이메일 아이디를 입력하세요."}' />
			@
			<input type="text" id="email_2" value="<%=Html.getText(objUser.data.get("EMAIL_2"))%>" option='{"isMust" : true, "message" : "이메일 도메인을 입력하세요."}' />
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
				<option value="<%=v%>"<%=(objUser.data.get("TELEPHONE_1").equals(v) ? " selected" : "")%>><%=v%></option>
			<% } %>
			</select>
			-
			<input type="text" id="telephone_2" style="width:40px;" value="<%=Html.getText(objUser.data.get("TELEPHONE_2"))%>" maxlength="4" onkeyup="Common.moveFocus(4, this, $('telephone_3'));" option='{"isMust" : true, "varType" : "number", "message" : "일반전화 국번을 입력하세요."}' />
			-
			<input type="text" id="telephone_3" style="width:40px;" value="<%=Html.getText(objUser.data.get("TELEPHONE_3"))%>" maxlength="4" option='{"isMust" : true, "equalLength" : true, "varType" : "number", "message" : "일반전화번호를 입력하세요."}' />
			<input type="checkbox" id="telephoe_none" class="checkbox" onclick="_none(this.checked, 'telephone');"<%=(objUser.data.get("TELEPHONE").equals("--") ? " checked" : "")%> /><label for="telephoe_none">없음</label>
		</td>
	</tr>
	<tr>
		<th><span>휴대전화 </span></th>
		<td>
			<select id="cellphone_1" style="width:60px" onchange="$('cellphone_2').focus();" option='{"isMust" : true, "message" : "휴대전화번호를 선택하세요."}'>
				<option value="">- 선택</option>
			<% for (String v : StringEx.split(cfg.get("list.cellphone"), ";")) { %>
				<option value="<%=v%>"<%=(objUser.data.get("CELLPHONE_1").equals(v) ? " selected" : "")%>><%=v%></option>
			<% } %>
			</select>
			-
			<input type="text" id="cellphone_2" style="width:40px;" value="<%=Html.getText(objUser.data.get("CELLPHONE_2"))%>" maxlength="4" onkeyup="Common.moveFocus(4, this, $('cellphone_3'));" option='{"isMust" : true, "varType" : "number", "message" : "휴대전화번호를 입력하세요."}' />
			-
			<input type="text" id="cellphone_3" style="width:40px;" value="<%=Html.getText(objUser.data.get("CELLPHONE_3"))%>" maxlength="4" option='{"isMust" : true, "equalLength" : true, "varType" : "number", "message" : "휴대전화번호를 입력하세요."}' />
			<input type="checkbox" id="sms_soldout" disabled="disabled" value="Y" class="checkbox"<%=(objUser.data.get("IS_SMS_SOLDOUT").equals("Y") ? " checked" : "")%> <%=(objUser.data.get("IS_SMS_ENABLED").equals("Y") ? "" : "disabled")%> onclick="checkAuth()"/><label for="sms_soldout" onclick="checkAuth()">품절정보 SMS 수신</label>
			<input type="checkbox" id="sms_state" disabled="disabled" value="Y" class="checkbox"<%=(objUser.data.get("IS_SMS_STATE").equals("Y") ? " checked" : "")%> <%=(objUser.data.get("IS_SMS_ENABLED").equals("Y") ? "" : "disabled")%> onclick="checkAuth()"/><label for="sms_state" onclick="checkAuth()">단말기상태  SMS 수신</label>
			<input type="checkbox" id="sms_closing" disabled="disabled" value="Y" class="checkbox"<%=(objUser.data.get("IS_SMS_CLOSING").equals("Y") ? " checked" : "")%> <%=(objUser.data.get("IS_SMS_ENABLED").equals("Y") ? "" : "disabled")%> onclick="checkAuth()"/><label for="sms_closing" onclick="checkAuth()">단말기마감  SMS 수신</label>
			<%--2020-12-04 김태우 추가--%>
			<input type="checkbox" id="kakao_status" disabled="disabled" value="Y" class="checkbox"<%=(objUser.data.get("IS_KAKAO_STATUS").equals("Y") ? " checked" : "")%> <%=(objUser.data.get("IS_KAKAO_STATUS").equals("Y") ? "" : "disabled")%> onclick="checkAuth()"/><label for="kakao_status" onclick="checkAuth()">단말기상태 알림톡 수신</label>
		</td>
	</tr>
	<tr>
		<th><span>팩스번호 </span></th>
		<td>
			<select id="fax_1" style="width:60px" onchange="$('fax_2').focus();" option='{"isMust" : true, "message" : "팩스 지역번호를 선택하세요."}'>
				<option value="">- 선택</option>
			<% for (String v : StringEx.split(cfg.get("list.fax"), ";")) { %>
				<option value="<%=v%>"<%=(objUser.data.get("FAX_1").equals(v) ? " selected" : "")%>><%=v%></option>
			<% } %>
			</select>
			-
			<input type="text" id="fax_2" style="width:40px;" value="<%=Html.getText(objUser.data.get("FAX_2"))%>" maxlength="4" onkeyup="Common.moveFocus(4, this, $('fax_3'));" option='{"isMust" : true, "varType" : "number", "message" : "팩스 국번을 입력하세요."}' />
			-
			<input type="text" id="fax_3" style="width:40px;" value="<%=Html.getText(objUser.data.get("FAX_3"))%>" maxlength="4" option='{"isMust" : true, "equalLength" : true, "varType" : "number", "message" : "팩스번호를 입력하세요."}' />
			<input type="checkbox" id="fax_none" class="checkbox" onclick="_none(this.checked, 'fax');"<%=(objUser.data.get("FAX").equals("--") ? " checked" : "")%> /><label for="fax_none">없음</label>
		</td>
	</tr>
	<tr>
		<th><span>소속</span></th>
		<td>
			<select id="company" class="<%=(cfg.getLong("user.company") > 0 ? "disabled" : "")%> js-example-basic-single js-example-responsive" style="width: 20%" onchange="Company.organ(this.options[selectedIndex].value.evalJSON(), 0, 0, 'A');"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
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
				<select id="organ0" onchange="Company.organ($('company').value.evalJSON(), 1, this.options[selectedIndex].value, 'A');" class="js-example-basic-single js-example-responsive" style="width: 20%">
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
	<% if (objUser.data.get("IS_APP_ORGAN").equals("Y")) { %>
	<tr>
		<th><span>매출조회 추가조직</span></th>
		<td>
			<div style="position:relative;">
				<div id="appOrganList">
<%
	for (int i = 0; i < objUser.appOrgan.size(); i++) {
		GeneralConfig c = (GeneralConfig) objUser.appOrgan.get(i);

		out.println("<div id='appOrgan" + c.getLong("ORGANIZATION_SEQ") + "'>" + organ(c.get("ORGAN")) + " <a onclick='_delAppOrgan(" + c.getLong("SEQ") + ", " + c.getLong("ORGANIZATION_SEQ") + ");' style='cursor:pointer;'>[×]</a></div>");
	}

	if (objUser.appOrgan.size() == 0) {
		out.println("<a class='desc' id='appOrganNone'>* 매출 조회 시 추가로 검색할 조직이 등록되지 않았습니다.</a>");
	}
%>
				</div>
				<input type="button" class="button2" value="조직추가" onclick="_appOrganList(<%=seq%>, <%=objUser.data.getLong("COMPANY_SEQ")%>);" style="position:absolute; right:0; top:0;" />
			</div>
		</td>
	</tr>
	<% } %>
	<tr>
		<th class="last"><span>계정 활성화</span></th>
		<td class="last"><input type="checkbox" id="enabled" value="Y" class="checkbox"<%=(objUser.data.get("IS_ENABLED").equals("Y") ? " checked" : "")%> /><label for="enabled">사용 가능한 계정입니다.</label></td>
	</tr>
<% } else { %>
	<tr>
		<th><span>이메일 *</span></th>
		<td><%=Html.getText(objUser.data.get("EMAIL"))%></td>
	</tr>
	<tr>
		<th><span>전화번호 *</span></th>
		<td><%=Html.getText(StringEx.replace(objUser.data.get("TELEPHONE"), "--", "-"))%></td>
	</tr>
	<tr>
		<th><span>휴대전화 *</span></th>
		<td><%=Html.getText(objUser.data.get("CELLPHONE"))%></td>
	</tr>
	<tr>
		<th><span>팩스번호 *</span></th>
		<td><%=Html.getText(StringEx.replace(objUser.data.get("FAX"), "--", "-"))%></td>
	</tr>
	<tr>
		<th><span>소속</span></th>
		<td><%=Html.getText(StringEx.setDefaultValue(objUser.data.get("COMPANY"), "-"))%></td>
	</tr>
	<tr>
		<th><span>조직</span></th>
		<td><%=organ(objUser.data.get("ORGAN"))%></td>
	</tr>
	<tr>
		<th><span>권한</span></th>
		<td><%=Html.getText(StringEx.setDefaultValue(objUser.data.get("ORGAN"), "-"))%></td>
	</tr>
	<% if (objUser.data.get("IS_APP_ORGAN").equals("Y")) { %>
	<tr>
		<th><span>매출조회 추가조직</span></th>
		<td>
<%
	for (int i = 0; i < objUser.appOrgan.size(); i++) {
		GeneralConfig c = (GeneralConfig) objUser.appOrgan.get(i);

		out.println("<div>" + organ(c.get("ORGAN")) + "</div>");
	}
%>
		</td>
	</tr>
	<% } %>
	<tr>
		<th class="last"><span>계정 활성화</span></th>
		<td class="last"><%=Html.getText(objUser.data.get("IS_ENABLED"))%></td>
	</tr>
<% } %>
</table>

<div class="buttonArea" style="position:relative;">
<% if (cfg.isAuth("U")) { %>
	<input type="button" value="" class="btnModify" onclick="_save();" />
<% } %>
	<input type="button" value="" class="btnList" onclick="location.href = 'SystemUser.jsp?1=1<%=addParam%>';" />
<% if (cfg.isAuth("D")) { %>
	<input type="button" value="" class="btnDelete" onclick="_delete();" style="position:absolute; right:0; top:0" />
<% } %>
</div>

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/layer/js/iframe.js"></script>
<script type="text/javascript">
<% if (cfg.isAuth("U")) { %>
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

	function _save() {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		} else if ($('newPass').value != '' && $('cfmPass').value == '') {
			window.alert('변경할 비밀번호를 확인해 주세요.');
			$('cfmPass').focus();
			return;
		} else if ($('newPass').value != $('cfmPass').value) {
			window.alert('비밀번호가 일치하지 않습니다.');
			$('newPass').value = '';
			$('cfmPass').value = '';
			$('newPass').focus();
			return;
		}

		var error = Common.checkForm($('regist'));

		if (error != '') {
			window.alert(error);
			return;
		}

		if ((!Common.isAvailableEmail($('email_1').value + '@' + $('email_2').value)) && $('email_1').value != '' ) {
			window.alert('유효하지 않은 이메일 정보입니다.');
			$('email_1').value = '';
			$('email_2').value = '';
			$('email_1').focus();
			return;
		}

		var obj = {company : $('company'), organ : null, auth : $('auth')};
		var com = {seq : 0, depth : 0};
		var organ = 0;

		if (obj.company.selectedIndex > 0) {
			com = obj.company.options[obj.company.selectedIndex].value.evalJSON();

			for (var i = com.depth; i >= 0; i--) {
				obj.organ = $('organ' + i);

				if (obj.organ && obj.organ.selectedIndex > 0) {
					organ = obj.organ.options[obj.organ.selectedIndex].value;
					break;
				}
			}
		}

		/* 2011.04.20, 정원광// if (com.seq > 0 && !organ) {
			window.alert('조직을 선택하세요.');
			return;
		} else */if (com.seq > 0 && obj.auth.selectedIndex == 0) {
			
			window.alert('권한을 선택하세요.');
			return;
		} else if (!confirm('수정하시겠습니까?')) {
			return;
		}

		$('sbmsg').show();

		new Ajax.Request(location.pathname, {
			asynchronous : true,
			parameters : {
				seq : <%=seq%>
			,	company : com.seq
			,	organ : organ
			,	auth : obj.auth.options[obj.auth.selectedIndex].value
			,	newPass : encodeURIComponentEx($('newPass').value)
			,	employeeNo : encodeURIComponentEx($('employeeNo').value)
			,	email : encodeURIComponentEx($('email_1').value + '@' + $('email_2').value)
			,	telephone : encodeURIComponentEx($('telephone_1').value + '-' + $('telephone_2').value + '-' + $('telephone_3').value)
			,	cellphone : encodeURIComponentEx($('cellphone_1').value + '-' + $('cellphone_2').value + '-' + $('cellphone_3').value)
			,	sms_soldout : $('sms_soldout').checked ? 'Y' : 'N'
			,	sms_state : $('sms_state').checked ? 'Y' : 'N'
			,	sms_closing : $('sms_closing').checked ? 'Y' : 'N'
				//2020-12-10 김태우 수정
			,	kakao_status : $('kakao_status').checked ? 'Y' : 'N'
			,	fax : encodeURIComponentEx($('fax_1').value + '-' + $('fax_2').value + '-' + $('fax_3').value)
			,	enabled : $('enabled').checked ? 'Y' : 'N'
			},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('수정이 완료되었습니다.');
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
		<%=(objUser.data.get("TELEPHONE").equals("--") ? "_none(true, 'telephone');" : "")%>
		<%=(objUser.data.get("FAX").equals("--") ? "_none(true, 'fax');" : "")%>
		Company.depth = <%=cfg.getInt("user.organ.depth")%>;
		Company.company(<%=objUser.data.getLong("COMPANY_SEQ")%>, <%=objUser.data.getLong("ORGANIZATION_SEQ")%>, {mode : 'A', auth : <%=objUser.data.getLong("AUTH_SEQ")%>});
//	}.bind(this));


		
		
<% } %>
<% if (cfg.isAuth("D")) { %>
	function _delete() {
		if (!confirm('삭제하시겠습니까?')) {
			return;
		}

		new Ajax.Request('SystemUserDelete.jsp', {
			asynchronous : false,
			parameters : {seq : <%=seq%>},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('삭제가 완료되었습니다.');
						location.replace('SystemUser.jsp?1=1<%=addParam%>');
					} else {
						window.alert(decodeURIComponentEx(e.message));
					}
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.'); 
				}
			}.bind(this)
		});
	}
<% } %>
<% if (objUser.data.get("IS_APP_ORGAN").equals("Y")) { %>
	function _appOrganList(seq, company) {
		new IFrame(510, 130, 'SystemUserAppOrganization.jsp?seq=' + seq + '&company=' + company).open();
	}

	function _addAppOrgan(seq, organ, text) {
		var o = {list : $('appOrganList'), none : $('appOrganNone')};

		if (o.none) {
			o.none.remove();
		}

		o.list.insert('<div id="appOrgan' + organ + '">' + text + ' <a onclick="_delAppOrgan(' + seq + ', ' + organ + ');" style="cursor:pointer;">[×]</a></div>');
	}

	function _delAppOrgan(seq, organ) {
		if (!confirm('선택하신 조직을 삭제하시겠습니까?')) {
			return;
		}

		new Ajax.Request('SystemUserAppOrganizationDelete.jsp', {
			asynchronous : false,
			parameters : {seq : seq, organ : organ},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('선택하신 조직의 삭제가 완료되었습니다.');
						$('appOrgan' + organ).remove();

						if (Common.trim($('appOrganList').innerHTML) == '') {
							$('appOrganList').update('<a class="desc" id="appOrganNone">* 매출 조회 시 추가로 검색할 조직이 등록되지 않았습니다.</a>');
						}
					} else {
						window.alert(decodeURIComponentEx(e.message));
					}
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}
			}.bind(this)
		});
	}
<% } %>

	function checkAuth()  {
		/*
		var o = $("auth");
		if (o && o.selectedIndex == 0 ) {
			for (var i = o.length - 1; i > 0; i--) {
				if ( o.options[i].value == <%=objUser.data.getLong("AUTH_SEQ")%> ) {
					o.options[i].selected = true;	
				}
			}
		}
		*/
	}
	

</script>
<%!
	private String organ(String organ) {
		return StringEx.replace(StringEx.replace(Html.getText(StringEx.setDefaultValue(organ, "-")), "{", "<s>"), "}", "</s>");
	}
%>