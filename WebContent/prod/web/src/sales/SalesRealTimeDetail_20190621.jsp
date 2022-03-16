<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.Sales
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /sales/SalesRealTimeDetail.jsp
 *
 * 자판기 매출정보 > 상세 매출 현황 > 세부 내용
 *
 * 작성일 - 2011/04/07, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0402");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(Message.refresh(cfg.get("script.login") + "?goUrl=" + StringEx.encode(cfg.get("topDir") + "/web/src/sales/SalesRealTime.jsp"), null, "top"));
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth()) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "B");

// 전송된 데이터
	String sType = StringEx.getKeyword(StringEx.charset(request.getParameter("sType")));
	long company = StringEx.str2long(request.getParameter("company"), 0);
	long organ = StringEx.str2long(request.getParameter("organ"), 0);
	long place = StringEx.str2long(request.getParameter("place"), 0);
	String sDate = StringEx.getKeyword(StringEx.charset(request.getParameter("sDate")));
	String eDate = StringEx.getKeyword(StringEx.charset(request.getParameter("eDate")));
	String product = StringEx.getKeyword(StringEx.charset(request.getParameter("product")));
	String payment = StringEx.getKeyword(StringEx.charset(request.getParameter("paymentAll")));
	String[] payTypes = "Y".equals(payment) ? null : request.getParameterValues("payment");
	String step = StringEx.getKeyword(StringEx.charset(request.getParameter("stepAll")));
	String[] paySteps = "Y".equals(step) ? null : request.getParameterValues("step");
	int pageNo = StringEx.str2int(request.getParameter("page"), 1);
	String goUrl = StringEx.charset(request.getParameter("goUrl"));
	String _goUrl = StringEx.charset(request.getParameter("_goUrl"));

	if (payTypes != null) {
		for (int i = 0; i < payTypes.length; i++) {
			payTypes[i] = StringEx.getKeyword(StringEx.charset(payTypes[i]));
		}
	}

	if (paySteps != null) {
		for (int i = 0; i < paySteps.length; i++) {
			paySteps[i] = StringEx.getKeyword(StringEx.charset(paySteps[i]));
		}
	} else if (!StringEx.isEmpty(goUrl)) {
		paySteps = new String[0];
	}

// 인스턴스 생성
	Sales objSales = new Sales(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objSales.sales(sType, company, organ, place, sDate, eDate, product, payTypes, paySteps, pageNo);
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

<% if (company == 0 || StringEx.isEmpty(sType) || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) { %>
<div class="infoSearch">
	<img src="<%=cfg.get("imgDir")%>/web/txt_searchInfo.gif" alt="상단의 검색 조건을 통해 데이터를 검색해 주세요." />
</div>
<% } else { %>
<div class="infoBar">
	검색 결과 : <strong><%=StringEx.comma(objSales.records)%></strong>건
	<input type="button" class="button2" onclick="_excel();" value="엑셀저장" />&nbsp;<b>(엑셀저장 시 매출거래만 저장됩니다.)</b>
	<% if (!StringEx.isEmpty(goUrl)) { %>
	<input type="button" class="button2" value="집계보기" onclick="_return();" style="right:80px;" />
	<% } %>
</div>
<div style="width:100%; overflow:auto; overflow-y:hidden; padding-bottom:10px;">
<form name="form" id="form" method="post">
<input type="hidden" id="sType" name="sType" value="<%=sType%>" />
<input type="hidden" name="company" value="<%=company%>" />
<input type="hidden" name="organ" value="<%=organ%>" />
<input type="hidden" id="place" name="place" value="<%=place%>" />
<input type="hidden" id="product" name="product" value="<%=product%>" />
<input type="hidden" id="sDate" name="sDate" value="<%=sDate%>" />
<input type="hidden" id="eDate" name="eDate" value="<%=eDate%>" />
<input type="hidden" name="paymentAll" value="<%=StringEx.getKeyword(StringEx.charset(payment))%>" />
<% if (payTypes != null) for (String payType : payTypes) { %>
<input type="hidden" name="payment" value="<%=payType%>" />
<% } %>
<input type="hidden" name="stepAll" value="<%=StringEx.getKeyword(StringEx.charset(step))%>" />
<% if (paySteps != null) for (String payStep : paySteps) { %>
<input type="hidden" name="step" value="<%=payStep%>" />
<% } %>
<input type="hidden" id="page" name="page" value="<%=pageNo%>" />
<% if (!StringEx.isEmpty(goUrl)) { %>
<input type="hidden" id="_sType" name="_sType" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("_sType")))%>" />
<input type="hidden" id="_place" name="_place" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("_place")))%>" />
<input type="hidden" id="_product" name="_product" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("_product")))%>" />
<input type="hidden" id="_sDate" name="_sDate" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("_sDate")))%>" />
<input type="hidden" id="_eDate" name="_eDate" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("_eDate")))%>" />
<input type="hidden" name="oMode" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("oMode")))%>" />
<input type="hidden" name="oType" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("oType")))%>" />
<input type="hidden" id="goUrl" name="goUrl" value="<%=StringEx.replace(goUrl, "\"", "\\\"")%>" />
<% } %>
<% if (!StringEx.isEmpty(_goUrl)) { %>
<input type="hidden" id="__place" name="__place" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("__place")))%>" />
<input type="hidden" id="__sDate" name="__sDate" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("__sDate")))%>" />
<input type="hidden" id="__eDate" name="__eDate" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("__eDate")))%>" />
<input type="hidden" name="_oMode" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("_oMode")))%>" />
<input type="hidden" name="_oType" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("_oType")))%>" />
<input type="hidden" id="_goUrl" name="_goUrl" value="<%=StringEx.replace(_goUrl, "\"", "\\\"")%>" />
<% } %>
<table cellpadding="0" cellspacing="0" class="tableType01">
	<thead>
		<tr>
			<th nowrap>순번</th>
			<th nowrap>거래일시</th>
			<th nowrap><%=objSales.data.get("ORGAN")%></th>
			<th nowrap>설치위치</th>
			<th nowrap>자판기코드</th>
			<th nowrap>상품</th>
			<th nowrap>금액</th>
			<th nowrap>수단</th>
			<th nowrap>입력</th>
			<th nowrap>상태</th>
			<th nowrap>마감일시</th>
			<th nowrap>입금(예정)일</th>
			<th nowrap>취소일시</th>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objSales.list.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>" title="단말기 ID = <%=c.get("TERMINAL_ID")%>, 거래번호 = <%=c.get("TRANSACTION_NO")%>">
			<td class="center number" nowrap><%=c.getLong("NO")%></td>
			<td class="center number" nowrap><%=c.get("TRANSACTION_DATE")%></td>
			<td nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("ORGAN"), "-"))%></td>
			<td nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PLACE"), "-"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("VMCODE"), "-"))%></td>
			<td nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("GOODS"), "-"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PAY_TYPE").equals("선불") ? c.get("PAY_CARD") : c.get("PAY_TYPE"), "-"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("INPUT_TYPE"), "-"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PAY_STEP"), "-"))%></td>
			<td class="center number" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("CLOSING_DATE"), "-"))%></td>
			<td class="center number" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PAY_DATE"), "-"))%></td>
			<td class="center number" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("CANCEL_DATE"), "-"))%></td>
		</tr>
<% } %>
<% if (objSales.list.size() == 0) { %>
		<tr>
			<td colspan="13" align="center">등록된 상세 매출 내역이 없습니다</td>
		</tr>
<% } %>
	</tbody>
</table>
</form>
</div>
<div class="paging"><%=Pager.getList(pageNo, cfg.getInt("limit.page"), objSales.pages, "javascript:_page(", ");", "", buttons)%></div>
<% } %>

<%@ include file="../../footer.inc.jsp" %>

<script type="text/javascript">
	function _submit(form, action, target) {
		if (parent && (target != "_top")) {
			var progress = parent.$("sbmsg");

			if (progress && progress.show) progress.show();
		}

		if (form) {
			form.action = action;
			form.target = target;
			form.submit();
		}
	}
<% if (!StringEx.isEmpty(goUrl)) { %>
	function _return() {
		var url = $("goUrl").value;

		$("sType").value = $("_sType").value;
		$("place").value = $("_place").value;
		$("product").value = $("_product").value;
		$("sDate").value = $("_sDate").value;
		$("eDate").value = $("_eDate").value;
	<% if (!StringEx.isEmpty(_goUrl)) { %>
		$("_place").value = $("__place").value;
		$("_sDate").value = $("__sDate").value;
		$("_eDate").value = $("__eDate").value;
		$("goUrl").value = $("_goUrl").value;
	<% } else { %>
		$("goUrl").value = "";
	<% } %>
		_submit($("form"), url, "_self");
	}
<% } %>
	function _page(no) {
		$("page").value = no;

		_submit($("form"), "", "_self");
	}

	function _excel() {
<% if (objSales.records > 65000) { %>
		window.alert("65,000 라인을 초과하는 데이터입니다.\n\n보다 상세한 검색 조건으로 검색 후 다운로드하세요.");
<% } else { %>
		_submit($("form"), "SalesRealTimeExcel.jsp", "_top");
<% } %>
	}
</script>