<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Code
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /system/SystemCode.jsp
 *
 * 시스템 > 코드 > 조회
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
	String type = StringEx.setDefaultValue(StringEx.getKeyword(StringEx.charset(request.getParameter("type"))), "");

// 인스턴스 생성
	Code objCode = new Code(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objCode.detail(type);
	} catch (Exception e) {
		error = e.getMessage();
	}

// 에러 처리
	if (error != null) {
		out.print(error);
		return;
	}
%>
<%@ include file="../../header.inc.jsp" %>

<div class="title">
	<span>코드 관리</span>
</div>

<form method="get" />
<table cellspacing="0" class="tableType04">
	<tr>
		<td>
			<span>코드 그룹</span>
			<select name="type" onchange="this.form.submit();">
				<option value="">- 선택하세요</option>
		<% for (int i = 0; i < objCode.type.size(); i++) { GeneralConfig c = (GeneralConfig) objCode.type.get(i); %>
				<option value="<%=c.get("TYPE")%>"<%=(c.get("TYPE").equals(objCode.data.get("TYPE")) ? " selected" : "")%>><%=c.get("NAME")%></option>
		<% } %>
			</select>
		<% if (cfg.isAuth("U")) { %>
			<input type="button" class="button1" value="수정" onclick="_modify(this.form.type.options[this.form.type.selectedIndex].value, '000');" />
		<% } %>
		<% if (cfg.isAuth("D")) { %>
			<input type="button" class="button1" value="삭제" onclick="_delete(this.form.type.options[this.form.type.selectedIndex].value, '000');" />
		<% } %>
		<% if (cfg.isAuth("I")) { %>
			<input type="button" class="button1" value="등록" onclick="_regist();" />
		<% } %>
		</td>
	</tr>
</table>
</form>

<% if (!StringEx.isEmpty(objCode.data.get("TYPE"))) { %>
<div class="title" style="margin:25px 0 0 0;">
	<span>그룹 정보</span>
</div>

<table cellpadding="0" cellspacing="0" cellspacing="0" class="tableType03" style="margin:5px 0;">
	<colgroup>
		<col width="110" />
		<col width="*"/>
		<col width="110" />
		<col width="*"/>
	</colgroup>
	<tr>
		<th><span>코드</span></th>
		<td><%=Html.getText(objCode.data.get("TYPE"))%></td>
		<th><span>코드명</span></th>
		<td><%=Html.getText(objCode.data.get("NAME"))%></td>
	</tr>
	<tr>
		<th class="last"><span>설명</span></th>
		<td class="last" colspan="3"><%=Html.getText(StringEx.setDefaultValue(objCode.data.get("MEMO"), "-"))%></td>
	</tr>
</table>
<% } %>

<div class="title" style="margin:25px 0 0 0;">
	<span>코드 정보</span>
<% if (cfg.isAuth("I")) { %>
	<a><input type="button" class="button2" value="신규등록" onclick="_regist('<%=Html.getText(objCode.data.get("TYPE"))%>');" /></a>
<% } %>
</div>

<table cellspacing="0" class="tableType01" style="margin:5px 0;">
	<colgroup>
		<col width="100" />
		<col width="100" />
		<col width="150" />
		<col width="*" />
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
		<col width="<%=(cfg.isAuth("U") && cfg.isAuth("D") ? "120" : "60")%>" />
	<% } %>
	</colgroup>
	<thead>
		<tr>
			<th nowrap>주코드</th>
			<th nowrap>부코드</th>
			<th nowrap>코드명</th>
			<th nowrap>설명</th>
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
			<th nowrap>관리</th>
	<% } %>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objCode.code.size(); i++) { GeneralConfig c = (GeneralConfig) objCode.code.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>">
			<td class="center"><%=Html.getText(c.get("TYPE"))%></td>
			<td class="center"><%=Html.getText(c.get("CODE"))%></td>
			<td class="center"><%=Html.getText(c.get("NAME"))%></td>
			<td><%=Html.getText(StringEx.setDefaultValue(c.get("MEMO"), "-"))%></td>
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
			<td class="center">
			<% if (cfg.isAuth("U")) { %>
				<input type="button" class="button1" value="수정" onclick="_modify('<%=Html.getText(c.get("TYPE"))%>', '<%=Html.getText(c.get("CODE"))%>');" />
			<% } %>
			<% if (cfg.isAuth("D")) { %>
				<input type="button" class="button1" value="삭제" onclick="_delete('<%=Html.getText(c.get("TYPE"))%>', '<%=Html.getText(c.get("CODE"))%>');" />
			<% } %>
			</td>
	<% } %>
		</tr>
<% } %>
<% if (objCode.code.size() == 0) { %>
		<tr>
			<td colspan="<%=((cfg.isAuth("U") || cfg.isAuth("D")) ? "5" : "4")%>" align="center">등록된 코드가 없습니다</td>
		</tr>
<% } %>
	</tbody>
</table>

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/layer/js/iframe.js"></script>
<script type="text/javascript">
	function _regist(type) {
		new IFrame(510, 180, 'SystemCodeRegist.jsp?type=' + (type ? type : '')).open();
	}

	function _modify(type, code) {
		new IFrame(510, 180, 'SystemCodeModify.jsp?type=' + type + '&code=' + code).open();
	}

	function _delete(type, code) {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		} else if (!confirm('선택하신 코드를 삭제하시겠습니까?')) {
			return;
		}

		$('sbmsg').show();

		new Ajax.Request('SystemCodeDelete.jsp', {
			asynchronous : true,
			parameters : {type : type, code : code},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('삭제가 완료되었습니다.');
						location.reload();
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
</script>