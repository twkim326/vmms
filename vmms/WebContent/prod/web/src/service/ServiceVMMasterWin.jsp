<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.VM
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceVMMasterWin.jsp
 *
 * 서비스 > 자판기 기초정보 > 목록 (새창)
 *
 * 작성일 - 2011/04/02, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0202");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(cfg.login());
		return;
	}

// 페이지 권한 체크
	if (!(cfg.isAuth("I") || cfg.isAuth("U"))) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "B");

// 전송된 데이터
	long company = StringEx.str2long(request.getParameter("company"), 0);
	int pageNo = StringEx.str2int(request.getParameter("page"), 1);
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));

	if (company == 0) {
		out.print("소속 정보가 존재하지 않습니다.");
		return;
	}

// 인스턴스 생성
	VM objVM = new VM(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objVM.master(company, pageNo, sField, sQuery, false);
	} catch (Exception e) {
		error = e.getMessage();
	}

// 에러 처리
	if (error != null) {
		out.print(error);
		return;
	}

// 페이지 버튼
	String[] buttons = {"<img src='" + cfg.get("imgDir") +"/web/btn_first.gif' alt='처음' />",
		"<img src='" + cfg.get("imgDir") +"/web/btn_pre.gif' alt='이전 " + cfg.getInt("limit.page") + "개' />",
		"<img src='" + cfg.get("imgDir") +"/web/btn_next.gif' alt='다음 " + cfg.getInt("limit.page") + "개' />",
		"<img src='" + cfg.get("imgDir") +"/web/btn_last.gif' alt='마지막' />"};
%>
<%@ include file="../../header.inc.jsp" %>

<div id="window">
	<div class="title">
		<span>운영 자판기 관리</span>
	</div>

	<div class="infoBar3">
	검색 결과 : <strong><%=StringEx.comma(objVM.records)%></strong>건
	<form method="get">
		<input type="hidden" name="company" value="<%=company%>" />
		<select name="sField" id="sField">
			<option value="A.CODE"<%=sField.equals("A.CODE") ? " selected" : ""%>>코드</option>
			<option value="A.REGION"<%=sField.equals("A.REGION") ? " selected" : ""%>>설치지역</option>
			<option value="A.PLACE"<%=sField.equals("A.PLACE") ? " selected" : ""%>>설치위치</option>
		</select>
		<input name="sQuery" type="text" id="sQuery" class="txtInput" value="<%=Html.getText(sQuery)%>" />
		<input type="submit" value="검색" class="button1" />
	</form>
	</div>
	<table cellpadding="0" cellspacing="0" class="tableType01 tableType05">
	<colgroup>
		<col width="60" />
		<col width="*" />
		<col width="200" />
	</colgroup>
	<thead>
		<tr>
			<th>코드</th>
			<th>설치지역</th>
			<th>설치위치</th>
		</tr>
	</thead>
	<tbody>
	<% for (int i = 0; i < objVM.list.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>" onclick="_select('<%=c.get("CODE")%>', '<%=Html.getText(c.get("MODEL").trim().replaceAll("'", ""))%>', '<%=Html.getText(c.get("PLACE").replaceAll("'", "").trim())%>');" style="cursor:pointer;">
			<td class="center"><%=Html.getText(StringEx.setDefaultValue(c.get("CODE"), "-"))%></td>
			<td class="center"><%=Html.getText(StringEx.setDefaultValue(c.get("REGION"), "-"))%></td>
			<td class="center"><%=Html.getText(c.get("PLACE"))%></td>
		</tr>
	<% } %>
	<% if (objVM.list.size() == 0) { %>
		<tr>
			<td colspan="3" align="center">등록된 자판기가 없습니다</td>
		</tr>
	<% } %>
	</tbody>
	</table>

	<div class="paging paging2"><%=Pager.getList(request, pageNo, 5, objVM.pages, "", "page", "", buttons)%></div>
</div>

<%@ include file="../../footer.inc.jsp" %>

<script type="text/javascript">
	function _select(code, model, place) {
		parent.$('code').value = code;
		parent.$('model').value = model;
		parent.$('place').value = place;

		(new parent.IFrame()).close();
	}
</script>