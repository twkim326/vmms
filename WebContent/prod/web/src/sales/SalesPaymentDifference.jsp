<%@ page import="com.nucco.*,
	com.nucco.cfg.*,
	com.nucco.lib.*,
	com.nucco.lib.http.Param,
	com.nucco.beans.Sales" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
/**
 * /sales/SalesPaymentDifference.jsp
 *
 * 자판기 매출정보 > 매출별 입금 현황 > 입금 예정일과 입금일이 다른 판매 내역
 *
 * 작성일 - 2011/05/11, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0405");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(Message.refresh("SalesPayment.jsp", null, "opener.top", true));
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
	long company = StringEx.str2long(request.getParameter("company"), 0);
	long organ = StringEx.str2long(request.getParameter("organ"), 0);
	long place = StringEx.str2long(request.getParameter("place"), 0);
	String aDate = StringEx.getKeyword(StringEx.charset(request.getParameter("aDate")));
	String cDate = StringEx.getKeyword(StringEx.charset(request.getParameter("cDate")));
	String card = StringEx.getKeyword(StringEx.charset(request.getParameter("card")));
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));
	int pageNo = StringEx.str2int(request.getParameter("page"), 1);

// 인스턴스 생성
	Sales objSales = new Sales(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objSales.difference(company, organ, place, aDate, cDate, card, sField, sQuery, pageNo);
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
		<span>마감별 입금 현황</span>
	</div>

	<div class="infoBar3">
	검색 결과 : <strong><%=StringEx.comma(objSales.records)%></strong>건
	<form method="get">
		<input type="hidden" name="company" value="<%=company%>" />
		<input type="hidden" name="organ" value="<%=organ%>" />
		<input type="hidden" name="place" value="<%=place%>" />
		<input type="hidden" name="aDate" value="<%=aDate%>" />
		<input type="hidden" name="cDate" value="<%=cDate%>" />
		<input type="hidden" name="card" value="<%=card%>" />
		<select name="sField" id="sField">
			<option value="A.TRANSACTION_DATE"<%=sField.equals("A.TRANSACTION_DATE") ? " selected" : ""%>>거래일</option>
			<option value="A.CLOSING_DATE"<%=sField.equals("A.CLOSING_DATE") ? " selected" : ""%>>마감일</option>
		</select>
		<input type="text" name="sQuery" id="sQuery" value="<%=Html.getText(sQuery)%>" class="txtInput" style="width:80px;" maxlength="8" onkeyup="if (this.value != '' && !/^[0-9]+$/.test(this.value)) { window.alert('숫자만 입력이 가능합니다.'); this.value = ''; }" onclick="(function () {
			var e = event ? event : window.event;
			Calendar.open($('sQuery'), null, null, Event.pointerY(e), $('sQuery').value);
		})(event)" />
		<input type="submit" value="검색" class="button1" />
	</form>
	</div>
	<table cellpadding="0" cellspacing="0" class="tableType01 tableType05">
	<colgroup>
		<col width="*" />
		<col width="*" />
		<col width="45" />
		<col width="45" />
		<col width="50" />
		<col width="80" />
	</colgroup>
	<thead>
		<tr>
			<th>거래일</th>
			<th>마감일</th>
			<th>거래</th>
			<th>정산</th>
			<th>수단</th>
			<th>비고</th>
		</tr>
	</thead>
	<tbody>
	<% for (int i = 0; i < objSales.list.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>">
			<td class="center number"><%=Html.getText(c.get("TRANSACTION_DATE"))%></td>
			<td class="center number"><%=Html.getText(c.get("CLOSING_DATE"))%></td>
			<td class="right number"><%=StringEx.comma(c.getLong("AMOUNT"))%></td>
			<td class="right number"><%=StringEx.comma(c.getLong("AMOUNT") - c.getLong("COMMISSION") + c.getLong("OUTAMOUNT"))%></td>
			<td class="center"><%=Html.getText(StringEx.setDefaultValue(c.get("PAY_TYPE"), "-"))%></td>
			<td class="center"><%=Html.getText(StringEx.setDefaultValue(c.get("REASON"), "-"))%></td>
		</tr>
	<% } %>
	<% if (objSales.list.size() == 0) { %>
		<tr>
			<td colspan="6" align="center">입금 예정일과 실 입금일이 다른 판매 내역이 없습니다</td>
		</tr>
	<% } %>
	</tbody>
	</table>

	<div class="paging paging2"><%=Pager.getList(request, pageNo, cfg.getInt("limit.page"), objSales.pages, "", "page", "", buttons)%></div>
</div>

<%@ include file="../../footer.inc.jsp" %>

<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/calendar/js/calendar.js"></script>
