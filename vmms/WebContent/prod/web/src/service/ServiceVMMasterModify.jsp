<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.VM
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/SystemVMMasterModify.jsp
 *
 * 서비스 > 자판기 기초정보 > 수정
 *
 * 작성일 - 2011/04/02, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0203");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print("로그인이 필요합니다.");
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth("I")) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "B");

// 전송된 데이터
	long company = StringEx.str2long(request.getParameter("company"), 0);
	String code = StringEx.charset(request.getParameter("code"));

	if (company == 0 || StringEx.isEmpty(code)) {
		out.print("자판기 정보가 존재하지 않습니다.");
		return;
	}

// 인스턴스 생성
	VM objVM = new VM(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		String region = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("region"), ""), "UTF-8");
		String place = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("place"), ""), "UTF-8");
		String model = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("model"), ""), "UTF-8");

		if (company == 0 || StringEx.isEmpty(place)) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("필수 입력값이 존재하지 않습니다.", "UTF-8") + "\"}");
			return;
		}

		try {
			error = objVM.master(company, code, region, place, model, false);
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
			error = objVM.master(company, code);
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

<div id="window">
	<div class="title">
		<span>자판기 기초정보 관리</span>
		<a>* 코드는 영문(대) 및 숫자만 가능합니다.</a>
	</div>

	<table cellspacing="0" class="tableType03 tableType05" id="regist">
		<colgroup>
			<col width="110" />
			<col width="*"/>
		</colgroup>
		<tr>
			<th><span>소속</span></th>
			<td>
				<select id="company" class="checkForm<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%> js-example-basic-single js-example-responsive" style="width: 40%" option='{"isMust" : true, "message" : "소속을 선택하세요."}'<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
					<option value="0">- 선택하세요</option>
			<% for (int i = 0; i < objVM.company.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.company.get(i); %>
					<option value="<%=c.getLong("SEQ")%>"<%=(c.getLong("SEQ") == objVM.data.getLong("COMPANY_SEQ") ? " selected" : "")%>><%=c.get("NAME")%></option>
			<% } %>
				</select>
			</td>
		</tr>
		<tr>
			<th><span>코드</span></th>
			<td><input type="text" id="code" class="checkForm txtInput disabled" value="<%=Html.getText(objVM.data.get("CODE"))%>" maxlength="20" readonly option='{"isMust" : true, "varType" : "ALNUM", "message" : "코드를 입력하세요."}' /></td>
		</tr>
		<tr>
			<th><span>설치지역</span></th>
			<td><input type="text" id="region" class="checkForm txtInput" value="<%=Html.getText(objVM.data.get("REGION"))%>" maxlength="100" style="width:370px" /></td>
		</tr>
		<tr>
			<th><span>설치위치</span></th>
			<td><input type="text" id="place" class="checkForm txtInput" value="<%=Html.getText(objVM.data.get("PLACE"))%>" maxlength="100" style="width:370px" option='{"isMust" : true, "message" : "설치위치를 입력하세요."}' /></td>
		</tr>
		<tr>
			<th class="last"><span>모델</span></th>
			<td class="last"><input type="text" id="model" class="checkForm txtInput" value="<%=Html.getText(objVM.data.get("MODEL"))%>" maxlength="100" /></td>
		</tr>
	</table>

	<div class="buttonArea">
		<input type="button" value="" class="btnModifyS" onclick="_save();" />
		<input type="button" value="" class="btnCancelS" onclick="new parent.IFrame().close();" />
	</div>
</div>

<%@ include file="../../footer.inc.jsp" %>

<script type="text/javascript">
	function _save() {
		var error = Common.checkForm($('regist'));

		if (error != '') {
			window.alert(error);
			return;
		} else if (!confirm('입력하신 내용을 등록하시겠습니까?')) {
			return;
		}

		new Ajax.Request(location.pathname, {
			asynchronous : false,
			parameters : {
				company : $('company').value
			,	code : $('code').value
			,	region : encodeURIComponentEx($('region').value)
			,	place : encodeURIComponentEx($('place').value)
			,	model : encodeURIComponentEx($('model').value)
			},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('수정이 완료되었습니다.');
						parent.location.reload();
					} else {
						window.alert(decodeURIComponentEx(e.message));
					}
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}
			}.bind(this)
		});
	}
</script>