<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.User
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceUser.jsp
 *
 * 서비스 > 정보 수정
 *
 * 작성일 - 2011/03/28, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0201");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(cfg.login());
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth("S") || !cfg.isAuth("U")) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "A");

// 인스턴스 생성
	User objUser = new User(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
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

		if (StringEx.isEmpty(email) || StringEx.isEmpty(cellphone)) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("필수 입력정보가 존재하지 않습니다.", "UTF-8") + "\"}");
			return;
		}

		try {
			//error = objUser.update(cfg.getLong("user.seq"), cfg.getLong("user.company"), cfg.getLong("user.organ"), cfg.getLong("user.auth"), newPass, employeeNo, email, telephone, cellphone, fax, enabled);
			//error = objUser.update(cfg.getLong("user.seq"), cfg.getLong("user.company"), cfg.getLong("user.organ"), cfg.getLong("user.auth"), newPass, employeeNo, email, telephone, cellphone, fax, enabled, smsSoldout, smsState);
			error = objUser.update(cfg.getLong("user.seq"), cfg.getLong("user.company"), cfg.getLong("user.organ"), cfg.getLong("user.auth"), newPass, employeeNo, email, telephone, cellphone, fax, enabled, smsSoldout, smsState, smsClosing);
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
			error = objUser.detail(cfg.getLong("user.seq"));
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null) {
			out.print(Message.alert(error));
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
		<col width="110" />
		<col width="*"/>
	</colgroup>
	<tr>
		<th><span>아이디 *</span></th>
		<td><%=Html.getText(objUser.data.get("ID"))%></td>
	</tr>
	<tr>
		<th><span>비밀번호 변경</span></th>
		<td><input type="password" id="newPass" class="checkForm txtInput" /></td>
	</tr>
	<tr>
		<th><span>비밀번호 확인</span></th>
		<td><input type="password" id="cfmPass" class="checkForm txtInput" /></td>
	</tr>
	<tr>
		<th><span>이름 *</span></th>
		<td><%=Html.getText(objUser.data.get("NAME"))%></td>
	</tr>
	<tr>
		<th><span>사번</span></th>
		<td><input type="text" id="employeeNo" class="checkForm txtInput" value="<%=Html.getText(objUser.data.get("EMPLOYEE_NO"))%>" maxlength="12" /></td>
	</tr>
	<tr>
		<th><span>이메일 *</span></th>
		<td>
			<input type="text" id="email_1" class="checkForm txtInput" value="<%=Html.getText(objUser.data.get("EMAIL_1"))%>" option='{"isMust" : true, "message" : "이메일 아이디를 입력하세요."}' />
			@
			<input type="text" id="email_2" class="checkForm txtInput" value="<%=Html.getText(objUser.data.get("EMAIL_2"))%>" option='{"isMust" : true, "message" : "이메일 도메인을 입력하세요."}' />
			<select onchange="$('email_2').value = this.options[selectedIndex].value;">
				<option value="">직접입력</option>
			<% for (String v : StringEx.split(cfg.get("list.email"), ";")) { %>
				<option value="<%=v%>"><%=v%></option>
			<% } %>
			</select>
		</td>
	</tr>
	<tr>
		<th><span>전화번호 *</span></th>
		<td>
			<select id="telephone_1" class="checkForm" style="width:60px" onchange="$('telephone_2').focus();" option='{"isMust" : true, "message" : "일반전화 지역번호를 선택하세요."}'>
				<option value="">- 선택</option>
			<% for (String v : StringEx.split(cfg.get("list.telephone"), ";")) { %>
				<option value="<%=v%>"<%=(objUser.data.get("TELEPHONE_1").equals(v) ? " selected" : "")%>><%=v%></option>
			<% } %>
			</select>
			-
			<input type="text" id="telephone_2" class="checkForm txtInput" style="width:40px;" value="<%=Html.getText(objUser.data.get("TELEPHONE_2"))%>" maxlength="4" onkeyup="Common.moveFocus(4, this, $('telephone_3'));" option='{"isMust" : true, "varType" : "number", "message" : "일반전화 국번을 입력하세요."}' />
			-
			<input type="text" id="telephone_3" class="checkForm txtInput" style="width:40px;" value="<%=Html.getText(objUser.data.get("TELEPHONE_3"))%>" maxlength="4" option='{"isMust" : true, "equalLength" : true, "varType" : "number", "message" : "일반전화번호를 입력하세요."}' />
			<input type="checkbox" id="telephoe_none" class="checkbox" onclick="_none(this.checked, 'telephone');"<%=(objUser.data.get("TELEPHONE").equals("--") ? " checked" : "")%> /><label for="telephoe_none">없음</label>
		</td>
	</tr>
	<tr>
		<th><span>휴대전화 *</span></th>
		<td>
			<select id="cellphone_1" class="checkForm" style="width:60px" onchange="$('cellphone_2').focus();" option='{"isMust" : true, "message" : "휴대전화번호를 선택하세요."}'>
				<option value="">- 선택</option>
			<% for (String v : StringEx.split(cfg.get("list.cellphone"), ";")) { %>
				<option value="<%=v%>"<%=(objUser.data.get("CELLPHONE_1").equals(v) ? " selected" : "")%>><%=v%></option>
			<% } %>
			</select>
			-
			<input type="text" id="cellphone_2" class="checkForm txtInput" style="width:40px;" value="<%=Html.getText(objUser.data.get("CELLPHONE_2"))%>" maxlength="4" onkeyup="Common.moveFocus(4, this, $('cellphone_3'));" option='{"isMust" : true, "varType" : "number", "message" : "휴대전화번호를 입력하세요."}' />
			-
			<input type="text" id="cellphone_3" class="checkForm txtInput" style="width:40px;" value="<%=Html.getText(objUser.data.get("CELLPHONE_3"))%>" maxlength="4" option='{"isMust" : true, "equalLength" : true, "varType" : "number", "message" : "휴대전화번호를 입력하세요."}' />
			<input type="checkbox" id="sms_soldout" value="Y" class="checkbox"<%=(objUser.data.get("IS_SMS_SOLDOUT").equals("Y") ? " checked" : "")%> <%=(objUser.data.get("IS_SMS_ENABLED").equals("Y") ? "" : "disabled")%> onclick="checkAuth()"/><label for="sms_soldout" onclick="checkAuth()">품절정보 SMS 수신</label>
			<input type="checkbox" id="sms_state" value="Y" class="checkbox"<%=(objUser.data.get("IS_SMS_STATE").equals("Y") ? " checked" : "")%> <%=(objUser.data.get("IS_SMS_ENABLED").equals("Y") ? "" : "disabled")%> onclick="checkAuth()"/><label for="sms_state" onclick="checkAuth()">단말기상태  SMS 수신</label>
			<input type="checkbox" id="sms_closing" value="Y" class="checkbox"<%=(objUser.data.get("IS_SMS_CLOSING").equals("Y") ? " checked" : "")%> <%=(objUser.data.get("IS_SMS_ENABLED").equals("Y") ? "" : "disabled")%> onclick="checkAuth()"/><label for="sms_closing" onclick="checkAuth()">단말기마감  SMS 수신</label>
			
		</td>
	</tr>
	<tr>
		<th><span>팩스번호 *</span></th>
		<td>
			<select id="fax_1" class="checkForm" style="width:60px" onchange="$('fax_2').focus();" option='{"isMust" : true, "message" : "팩스 지역번호를 선택하세요."}'>
				<option value="">- 선택</option>
			<% for (String v : StringEx.split(cfg.get("list.fax"), ";")) { %>
				<option value="<%=v%>"<%=(objUser.data.get("FAX_1").equals(v) ? " selected" : "")%>><%=v%></option>
			<% } %>
			</select>
			-
			<input type="text" id="fax_2" class="checkForm txtInput" style="width:40px;" value="<%=Html.getText(objUser.data.get("FAX_2"))%>" maxlength="4" onkeyup="Common.moveFocus(4, this, $('fax_3'));" option='{"isMust" : true, "varType" : "number", "message" : "팩스 국번을 입력하세요."}' />
			-
			<input type="text" id="fax_3" class="checkForm txtInput" style="width:40px;" value="<%=Html.getText(objUser.data.get("FAX_3"))%>" maxlength="4" option='{"isMust" : true, "equalLength" : true, "varType" : "number", "message" : "팩스번호를 입력하세요."}' />
			<input type="checkbox" id="fax_none" class="checkbox" onclick="_none(this.checked, 'fax');"<%=(objUser.data.get("FAX").equals("--") ? " checked" : "")%> /><label for="fax_none">없음</label>
		</td>
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
		<th class="last"><span>권한</span></th>
		<td class="last"><%=Html.getText(StringEx.setDefaultValue(objUser.data.get("AUTH"), "-"))%></td>
	</tr>
</table>

<div class="buttonArea">
	<input type="button" value="" class="btnModify" onclick="_save();" />
</div>

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script type="text/javascript">
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

		if (!Common.isAvailableEmail($('email_1').value + '@' + $('email_2').value)) {
			window.alert('유효하지 않은 이메일 정보입니다.');
			$('email_1').value = '';
			$('email_2').value = '';
			$('email_1').focus();
			return;
		} else if (!confirm('수정하시겠습니까?')) {
			return;
		}

		$('sbmsg').show();

		new Ajax.Request(location.pathname, {
			asynchronous : true,
			parameters : {
				newPass : encodeURIComponentEx($('newPass').value)
			,	employeeNo : encodeURIComponentEx($('employeeNo').value)
			,	email : encodeURIComponentEx($('email_1').value + '@' + $('email_2').value)
			,	telephone : encodeURIComponentEx($('telephone_1').value + '-' + $('telephone_2').value + '-' + $('telephone_3').value)
			,	cellphone : encodeURIComponentEx($('cellphone_1').value + '-' + $('cellphone_2').value + '-' + $('cellphone_3').value)
			,	fax : encodeURIComponentEx($('fax_1').value + '-' + $('fax_2').value + '-' + $('fax_3').value)
			,	sms_soldout : $('sms_soldout').checked ? 'Y' : 'N'
			,	sms_state : $('sms_state').checked ? 'Y' : 'N'
			,	sms_closing : $('sms_closing').checked ? 'Y' : 'N'					
			,	enabled : '<%=Html.getText(objUser.data.get("IS_ENABLED"))%>'
			},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('수정이 완료되었습니다.');
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
//	}.bind(this));
</script>
<%!
	private String organ(String organ) {
		return StringEx.replace(StringEx.replace(Html.getText(StringEx.setDefaultValue(organ, "-")), "{", "<s>"), "}", "</s>");
	}
%>