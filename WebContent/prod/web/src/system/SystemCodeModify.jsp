<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Code
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /system/SystemCodeModify.jsp
 *
 * 시스템 > 코드 > 수정
 *
 * 작성일 - 2011/03/28, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0106");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(Message.reload("top"));
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth("U")) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "B");

// 전송된 데이터
	String type = StringEx.setDefaultValue(StringEx.getKeyword(StringEx.charset(request.getParameter("type"))), "");
	String code = StringEx.setDefaultValue(StringEx.getKeyword(StringEx.charset(request.getParameter("code"))), "");

	if (StringEx.isEmpty(type) || StringEx.isEmpty(code)) {
		out.print("필수 입력정보가 존재하지 않습니다.");
		return;
	}

// 인스턴스 생성
	Code objCode = new Code(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		String name = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("name"), ""), "UTF-8");
		String memo = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("memo"), ""), "UTF-8");

		if (StringEx.isEmpty(name)) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("필수 입력정보가 존재하지 않습니다.", "UTF-8") + "\"}");
			return;
		}

		try {
			error = objCode.update(type, code, name, memo);
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
			error = objCode.update(type, code);
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
		<span>코드관리</span>
	</div>

	<table cellspacing="0" class="tableType03 tableType05" id="regist">
		<colgroup>
			<col width="110" />
			<col width="*"/>
		</colgroup>
<% if (objCode.data.get("CODE").equals("000")) { %>
		<tr>
			<th><span>코드</span></th>
			<td><%=objCode.data.get("TYPE")%></td>
		</tr>
<% } else { %>
		<tr>
			<th><span>모코드</span></th>
			<td><%=objCode.data.get("TYPE")%></td>
		</tr>
		<tr>
			<th><span>자코드</span></th>
			<td><%=objCode.data.get("CODE")%></td>
		</tr>
<% } %>
		<tr>
			<th><span>코드명</span></th>
			<td><input type="text" id="name" value="<%=Html.getText(objCode.data.get("NAME"))%>" class="checkForm txtInput" maxlength="20" option='{"isMust" : true, "message" : "코드명을 입력하세요."}' /></td>
		</tr>
		<tr>
			<th class="last"><span>설명</span></th>
			<td class="last"><input type="text" id="memo" value="<%=Html.getText(objCode.data.get("MEMO"))%>" class="checkForm txtInput" maxlength="50" style="width:370px" /></td>
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
		} else if (!confirm('입력하신 내용을 수정하시겠습니까?')) {
			return;
		}

		new Ajax.Request(location.pathname, {
			asynchronous : false,
			parameters :
			{
				type : '<%=objCode.data.get("TYPE")%>'
			,	code : '<%=objCode.data.get("CODE")%>'
			,	name : encodeURIComponentEx($('name').value)
			,	memo : encodeURIComponentEx($('memo').value)
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