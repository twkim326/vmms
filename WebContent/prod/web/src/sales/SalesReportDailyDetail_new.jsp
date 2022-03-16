<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Sales
			, java.util.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /sales/SalesReportDailyDetail.jsp
 *
 * 자판기 매출정보 > 조건별 매출 현황 > 일별
 *
 * 작성일 - 2011/04/05, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0401");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(Message.refresh(cfg.get("script.login") + "?goUrl=" + StringEx.encode(cfg.get("topDir") + "/web/src/sales/SalesReport.jsp"), null, "top"));
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
	String payment = StringEx.getKeyword(StringEx.charset(request.getParameter("paymentAll")));
	String[] payTypes = "Y".equals(payment) ? null : request.getParameterValues("payment");
	String sDate = StringEx.getKeyword(StringEx.charset(request.getParameter("sDate")));
	String eDate = StringEx.getKeyword(StringEx.charset(request.getParameter("eDate")));
	int oMode = StringEx.str2int(request.getParameter("oMode"), -1);
	int oType = StringEx.str2int(request.getParameter("oType"), -1);
	String goUrl = StringEx.charset(request.getParameter("goUrl"));

	if (oMode == -1) oMode = 0;
	if (oType == -1) oType = 1;
	if (payTypes != null) {
		for (int i = 0; i < payTypes.length; i++) {
			payTypes[i] = StringEx.getKeyword(StringEx.charset(payTypes[i]));
		}
	}

// 인스턴스 생성
	Sales objSales = new Sales(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objSales.dailyEx(sType, company, organ, place, sDate, eDate, payTypes, oMode, oType);
	} catch (Exception e) {
		error = e.getMessage();
	}

// 에러 처리
	if (error != null) {
		out.print(error);
		return;
	}

	int prepayCompanyCount = (objSales.company != null) ? objSales.company.size() : 0;
%>
<%@ include file="../../header.inc.jsp" %>

<%	if (company == 0 || StringEx.isEmpty(sType) || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) { %>
<div class="infoSearch">
	<img src="<%=cfg.get("imgDir")%>/web/txt_searchInfo.gif" alt="상단의 검색 조건을 통해 데이터를 검색해 주세요." />
</div>
<%	} else { %>
<div class="infoBar">
	검색 결과 : <strong><%=StringEx.comma(objSales.list.size())%></strong>건
	<input type="button" class="button2" onclick="_excel()" value="엑셀저장" />
	<%	if (!StringEx.isEmpty(goUrl)) { %>
	<input type="button" class="button2" value="집계보기" onclick="_return();" style="right:80px;" />
	<%	} %>
</div>
<div style="width:100%; overflow:auto; overflow-y:hidden; padding-bottom:10px;">
<form name="form" id="form" method="post">
<input type="hidden" id="sType" name="sType" value="<%=sType%>" />
<input type="hidden" name="_sType" value="<%=sType%>" />
<input type="hidden" name="company" value="<%=company%>" />
<input type="hidden" name="organ" value="<%=organ%>" />
<input type="hidden" id="place" name="place" value="<%=place%>" />
<input type="hidden" id="_place" name="_place" value="<%=place%>" />
<input type="hidden" id="sDate" name="sDate" value="<%=sDate%>" />
<input type="hidden" id="_sDate" name="_sDate" value="<%=sDate%>" />
<input type="hidden" id="eDate" name="eDate" value="<%=eDate%>" />
<input type="hidden" id="_eDate" name="_eDate" value="<%=eDate%>" />
<input type="hidden" name="paymentAll" value="<%=StringEx.getKeyword(StringEx.charset(payment))%>" />
	<%	if (payTypes != null) for (String payType : payTypes) { %>
<input type="hidden" name="payment" value="<%=payType%>" />
	<%	} %>
<input type="hidden" id="stepAll" name="stepAll" value="Y" />
<input type="hidden" id="step" name="step" value="29" />
<input type="hidden" id="oMode" name="oMode" value="<%=oMode%>" />
<input type="hidden" id="oType" name="oType" value="<%=oType%>" />
<input type="hidden" id="goUrl" name="goUrl" value="<%=StringEx.replace(request.getRequestURL().toString(), "\"", "\\\"")%>" />
	<%	if (!StringEx.isEmpty(goUrl)) { %>
<input type="hidden" id="__place" name="__place" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("_place")))%>" />
<input type="hidden" id="__sDate" name="__sDate" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("_sDate")))%>" />
<input type="hidden" id="__eDate" name="__eDate" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("_eDate")))%>" />
<input type="hidden" id="_oMode" name="_oMode" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("_oMode")))%>" />
<input type="hidden" id="_oType" name="_oType" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("_oType")))%>" />
<input type="hidden" id="_goUrl" name="_goUrl" value="<%=StringEx.replace(goUrl, "\"", "\\\"")%>" />
	<%	} %>
<table cellpadding="0" cellspacing="0" class="tableType01">
	<thead>
		<tr>
			<th rowspan="3" nowrap><a href="javascript:_sort(0,<%=(oMode == 0 && oType == 0 ? 1 : 0)%>);" class="<%=(oMode == 0 ? (oType == 1 ? "desc" : "asc") : "none")%>" onmouseover="this.className = '<%=(oMode == 0 ? (oType == 1 ? "asc" : "desc") : "asc")%>';" onmouseout="this.className = '<%=(oMode == 0 ? (oType == 1 ? "desc" : "asc") : "none")%>';" title="날짜별 정렬 <%=(oMode == 0 && oType == 0 ? "(↑)" : "(↓)")%>">날짜</a></th>
			<th colspan="2" rowspan="3" nowrap><a href="javascript:_sort(1,<%=(oMode == 1 && oType == 0 ? 1 : 0)%>);" class="<%=(oMode == 1 ? (oType == 1 ? "desc" : "asc") : "none")%>" onmouseover="this.className = '<%=(oMode == 1 ? (oType == 1 ? "asc" : "desc") : "asc")%>';" onmouseout="this.className = '<%=(oMode == 1 ? (oType == 1 ? "desc" : "asc") : "none")%>';" title="<%=objSales.data.get("ORGAN")%>별 정렬 <%=(oMode == 1 && oType == 0 ? "(↑)" : "(↓)")%>"><%=objSales.data.get("ORGAN")%></a></th>
			<th rowspan="3" nowrap><a href="javascript:_sort(2,<%=(oMode == 2 && oType == 0 ? 1 : 0)%>);" class="<%=(oMode == 2 ? (oType == 1 ? "desc" : "asc") : "none")%>" onmouseover="this.className = '<%=(oMode == 2 ? (oType == 1 ? "asc" : "desc") : "asc")%>';" onmouseout="this.className = '<%=(oMode == 2 ? (oType == 1 ? "desc" : "asc") : "none")%>';" title="설치 위치별 정렬 <%=(oMode == 2 && oType == 0 ? "(↑)" : "(↓)")%>">설치위치</a></th>
			<th rowspan="3" nowrap>자판기코드</th>
			<th colspan="<%=(7 + prepayCompanyCount * 2)%>" nowrap>판매</th>
			<th colspan="4">매입청구</th>
			<th colspan="<%=(10 + prepayCompanyCount * 3)%>" nowrap>입금</th>
		</tr>
		<tr>
			<th rowspan="2" nowrap>판매기간</th>
			<th colspan="2" nowrap>현금</th>
			<th colspan="2" nowrap>신용</th>
	<%	for (int i = 0; i < objSales.company.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.company.get(i); %>
			<th colspan="2" nowrap><%=c.get("NAME")%></th>
	<%	} %>
			<th colspan="2" nowrap>합계</th>
			<th colspan="2" nowrap>미청구</th>
			<th colspan="2" nowrap>거절</th>
			<th rowspan="2" nowrap>입금기간</th>
			<th colspan="3" nowrap>현금</th>
			<th colspan="3" nowrap>신용</th>
	<%	for (int i = 0; i < objSales.company.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.company.get(i); %>
			<th colspan="3" nowrap><%=c.get("NAME")%></th>
	<%	} %>
			<th colspan="3" nowrap>합계</th>
		</tr>
		<tr>
			<th nowrap>건수</th>
			<th nowrap>금액</th>
			<th nowrap>건수</th>
			<th nowrap>금액</th>
	<%	for (int i = 0; i < objSales.company.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.company.get(i); %>
			<th nowrap>건수</th>
			<th nowrap>금액</th>
	<%	} %>
			<th nowrap>건수</th>
			<th nowrap>금액</th>

			<th nowrap>건수</th>
			<th nowrap>금액</th>
			<th nowrap>건수</th>
			<th nowrap>금액</th>

			<th nowrap>건수</th>
			<th nowrap>수수료</th>
			<th nowrap>금액</th>
			<th nowrap>건수</th>
			<th nowrap>수수료</th>
			<th nowrap>금액</th>
	<% for (int i = 0; i < objSales.company.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.company.get(i); %>
			<th nowrap>건수</th>
			<th nowrap>수수료</th>
			<th nowrap>금액</th>
	<% } %>
			<th nowrap>건수</th>
			<th nowrap>수수료</th>
			<th nowrap>금액</th>
		</tr>
	</thead>
	<tbody>
	<%	if (objSales.list.size() <= 0) { %>
		<tr>
		 	<td class="center" colspan="<%=(prepayCompanyCount * 4 + 16)%>">해당 기간동안 발생한 매출이 없습니다</td>
		</tr>
	<%	} else {
			ArrayList<String> summaryColumnList = new ArrayList<String>();	
	%>
		<tr>
			<td class="center" colspan="6" style="background-color:#e9e9e9;">총계</td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_CARD"))%></td>
		<%
			summaryColumnList.add("CNT_CASH");
			summaryColumnList.add("AMOUNT_CASH");
			summaryColumnList.add("CNT_CARD");
			summaryColumnList.add("AMOUNT_CARD");
			for (int j = 0; j < prepayCompanyCount; j++) {
				GeneralConfig s = (GeneralConfig) objSales.company.get(j);
				
				summaryColumnList.add("CNT_PREPAY_" + s.get("CODE"));
				summaryColumnList.add("AMOUNT_PREPAY_" + s.get("CODE"));
		%>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_PREPAY_" + s.get("CODE")))%></td>
		<%	} %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_TOTAL"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_TOTAL"))%></td>

			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_HELD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_HELD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_DECLINED"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_DECLINED"))%></td>

			<td class="center number" style="background-color:#e9e9e9;" nowrap>&nbsp;</td>

			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("COMMISION_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("COMMISSION_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_CARD"))%></td>
		<%
			summaryColumnList.add("CNT_TOTAL");
			summaryColumnList.add("AMOUNT_TOTAL");

			summaryColumnList.add("CNT_HELD");
			summaryColumnList.add("AMOUNT_HELD");
			summaryColumnList.add("CNT_DECLINED");
			summaryColumnList.add("AMOUNT_DECLINED");

			summaryColumnList.add("PAY_CNT_CASH");
			summaryColumnList.add("COMMISSION_CASH");
			summaryColumnList.add("PAY_AMOUNT_CASH");
			summaryColumnList.add("PAY_CNT_CARD");
			summaryColumnList.add("COMMISSION_CARD");
			summaryColumnList.add("PAY_AMOUNT_CARD");
			for (int j = 0; j < prepayCompanyCount; j++) {
				GeneralConfig s = (GeneralConfig) objSales.company.get(j);

				summaryColumnList.add("PAY_CNT_PREPAY_" + s.get("CODE"));
				summaryColumnList.add("COMMISSION_PREPAY_" + s.get("CODE"));
				summaryColumnList.add("PAY_AMOUNT_PREPAY_" + s.get("CODE"));
		%>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("COMMISSION_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_PREPAY_" + s.get("CODE")))%></td>
		<%	} %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_TOTAL"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("COMMISSION_TOTAL"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_TOTAL"))%></td>
		</tr>
		<%
			summaryColumnList.add("PAY_CNT_TOTAL");
			summaryColumnList.add("COMMISSION_TOTAL");
			summaryColumnList.add("PAY_AMOUNT_TOTAL");
		
			// 20160125 중간에 소계 추가
			String[] groupColumns
				= (oMode == 1) ? new String[] { "PARENT_ORGAN", "ORGAN" }
				: (oMode == 2) ? new String[] { "ORGAN", "PLACE" }
				: new String[] { "PARENT_ORGAN", "ORGAN" };
			String[] summaryColumns = summaryColumnList.toArray(new String[summaryColumnList.size()]);
			String[] groupValues = new String[2];
			long[][] summaryValues = new long[2][summaryColumns.length];
		
			for (int i = 0; i < objSales.list.size(); i++) {
				GeneralConfig c = (GeneralConfig) objSales.list.get(i);
				long payAmountTotal = c.getLong("PAY_AMOUNT_TOTAL");

				if (!StringEx.isEmpty(groupValues[0]) && !c.get(groupColumns[0]).equals(groupValues[0])) {
					if (!StringEx.isEmpty(groupValues[1])) {
		%>
		<tr>
			<td class="center number" nowrap></td>
		 	<td class="center" colspan="5" style="background-color:#e9e9e9;" nowrap>소계(<%=groupValues[1]%>)</td>
			 		<%	for (int j = 0; j < summaryColumns.length; j++) { %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(summaryValues[1][j])%></td>
						<%	if (j == 15) { %>
			<td class="center number" style="background-color:#e9e9e9;" nowrap>&nbsp;</td>
						<%	}
						} %>
		</tr>
				<%	} %>
		<tr>
		 	<td class="center" colspan="6" style="background-color:#e9e9e9;" nowrap>합계(<%=groupValues[0]%>)</td>
		 		<%	for (int j = 0; j < summaryColumns.length; j++) { %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(summaryValues[0][j])%></td>
					<%	if (j == 15) { %>
			<td class="center number" style="background-color:#e9e9e9;" nowrap>&nbsp;</td>
					<%	}
						summaryValues[0][j] = summaryValues[1][j] = c.getLong(summaryColumns[j]);
					}

					groupValues[0] = StringEx.setDefaultValue(c.get(groupColumns[0]), "");
					if (groupColumns[1] != null) groupValues[1] = StringEx.setDefaultValue(c.get(groupColumns[1]), "");
			%>
		</tr>
			<%	} else if ((groupValues[1] != null) && !c.get(groupColumns[1]).equals(groupValues[1])) { %>
		<tr>
				<%	if (!StringEx.isEmpty(groupValues[0])) { %>
			<td class="center" nowrap>&nbsp;</td>
			<td class="center" colspan="5" style="background-color:#e9e9e9;" nowrap>소계(<%=groupValues[1]%>)</td>
				<%	} else { %>
			<td class="center" colspan="6" style="background-color:#e9e9e9;" nowrap>소계(<%=groupValues[1]%>)</td>
				<%	}
		 			for (int j = 0; j < summaryColumns.length; j++) { %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(summaryValues[1][j])%></td>
					<%	if (j == 15) { %>
			<td class="center number" style="background-color:#e9e9e9;" nowrap>&nbsp;</td>
					<%	}
						summaryValues[0][j] += summaryValues[1][j] = c.getLong(summaryColumns[j]);
					}
			
		 			if (groupColumns[1] != null) groupValues[1] = StringEx.setDefaultValue(c.get(groupColumns[1]), "");
				%>
		</tr>
			<%	} else {
					if (groupValues[0] == null) groupValues[0] = StringEx.setDefaultValue(c.get(groupColumns[0]), "");
					if ((groupColumns[1] != null) && (groupValues[1] == null)) groupValues[1] = StringEx.setDefaultValue(c.get(groupColumns[1]), "");
					
					for (int j = 0; j < summaryColumns.length; j++) {
						long value = c.getLong(summaryColumns[j]);

						summaryValues[0][j] += value;
						summaryValues[1][j] += value;
					}
				} %>
		<tr onclick="_detail('<%=c.get("YYYYMMDD")%>', <%=c.getLong("VM_PLACE_SEQ")%>, <%=c.getLong("AMOUNT_TOTAL")%>);" style="cursor:pointer;<%=c.getLong("AMOUNT_TOTAL") < 0L ? "background-color:#e9aaaa;" : ""%>">
			<td class="center number" nowrap><%=c.get("DATE")%></td>
			<%	if (StringEx.isEmpty(c.get("PARENT_ORGAN"))) { %>
			<td colspan="2" class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("ORGAN"), "-"))%></td>
			<%	} else { %>
			<td nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PARENT_ORGAN"), "-"))%></td>
			<td nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("ORGAN"), "-"))%></td>
			<%	} %>
			<td nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PLACE"), "-"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("VM_CODE"), "-"))%></td>
			<td class="center number" nowrap><%=Html.getText(c.get("START_DATE"))%> ~ <%=Html.getText(c.get("END_DATE"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_CASH"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_CASH"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_CARD"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_CARD"))%></td>
			<%	for (int j = 0; j < prepayCompanyCount; j++) { GeneralConfig s = (GeneralConfig) objSales.company.get(j); %>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_PREPAY_" + s.get("CODE")))%></td>
			<%	} %>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_TOTAL"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_TOTAL"))%></td>

			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_HELD"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_HELD"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_DECLINED"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_DECLINED"))%></td>

			<td class="center number" nowrap><%=Html.getText(c.get("PAY_START_DATE"))%> ~ <%=Html.getText(c.get("PAY_END_DATE"))%></td>

			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_CNT_CASH"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("COMMISSION_CASH"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_AMOUNT_CASH"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_CNT_CARD"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("COMMISSION_CARD"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_AMOUNT_CARD"))%></td>
			<%	for (int j = 0; j < prepayCompanyCount; j++) { GeneralConfig s = (GeneralConfig) objSales.company.get(j); %>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_CNT_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("COMMISSION_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_AMOUNT_PREPAY_" + s.get("CODE")))%></td>
			<%	} %>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_CNT_TOTAL"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("COMMISSION_TOTAL"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_AMOUNT_TOTAL"))%></td>
		</tr>
		<%	}
			
			if (!StringEx.isEmpty(groupValues[1])) { %>
		<tr>
			<%	if (!StringEx.isEmpty(groupValues[0])) { %>
			<td class="center number" nowrap>&nbsp;</td>
		 	<td class="center" colspan="5" style="background-color:#e9e9e9;" nowrap>소계(<%=groupValues[1]%>)</td>
			<%	} else { %>
		 	<td class="center" colspan="6" style="background-color:#e9e9e9;" nowrap>소계(<%=groupValues[1]%>)</td>
		 	<%	}
			
				for (int j = 0; j < summaryColumns.length; j++) { %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(summaryValues[1][j])%></td>
				<%	if (j == 15) { %>
			<td class="center number" style="background-color:#e9e9e9;" nowrap>&nbsp;</td>
				<%	}
				} %>
		</tr>
		<%	}
			
			if (!StringEx.isEmpty(groupValues[0])) { %>
		<tr>
			<td class="center" colspan="6" style="background-color:#e9e9e9;" nowrap>합계(<%=groupValues[0]%>)</td>
			<%	for (int j = 0; j < summaryColumns.length; j++) { %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(summaryValues[0][j])%></td>
				<%	if (j == 15) { %>
			<td class="center number" style="background-color:#e9e9e9;" nowrap>&nbsp;</td>
				<%	}
				} %>
		</tr>
		<%	} %>
		<tr>
			<td class="center" colspan="6" style="background-color:#e9e9e9;">총계</td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_CARD"))%></td>
			<% for (int j = 0; j < prepayCompanyCount; j++) { GeneralConfig s = (GeneralConfig) objSales.company.get(j); %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_PREPAY_" + s.get("CODE")))%></td>
			<% } %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_TOTAL"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_TOTAL"))%></td>

			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_HELD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_HELD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_DECLINED"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_DECLINED"))%></td>

			<td class="center number" style="background-color:#e9e9e9;" nowrap>&nbsp;</td>

			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("COMMISSION_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("COMMISSION_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_CARD"))%></td>
			<% for (int j = 0; j < prepayCompanyCount; j++) { GeneralConfig s = (GeneralConfig) objSales.company.get(j); %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("COMMISSION_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_PREPAY_" + s.get("CODE")))%></td>
			<% } %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_TOTAL"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("COMMISSION_TOTAL"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_TOTAL"))%></td>
		</tr>
	<%	} %>
</table>
</form>
</div>
<%
	} %>
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

	function _return() {
		$("place").value = $("__place").value;
		$("sDate").value = $("__sDate").value;
		$("eDate").value = $("__eDate").value;
		$("oMode").value = $("_oMode").value;
		$("oType").value = $("_oType").value;

		_submit($("form"), $("_goUrl").value, "_self");
	}

	function _sort(mode, type) {
		$("oMode").value = mode;
		$("oType").value = type;
<% if (!StringEx.isEmpty(goUrl)) { %>
		$("_place").value = $("__place").value;
		$("_sDate").value = $("__sDate").value;
		$("_eDate").value = $("__eDate").value;
		$("goUrl").value = $("_goUrl").value;
<% } %>
		_submit($("form"), "", "_self");
	}

	function _detail(yyyymmdd, place, amount) {
		$("sDate").value = yyyymmdd;
		$("eDate").value = yyyymmdd;
		$("place").value = place;

		if (amount < 0) {
			var sType = $("sType");

			sType.value = "1" + sType.value.substr(1, 1);
			$("stepAll").value = "N";
			$("step").value = "29";
		}

		_submit($("form"), "SalesRealTimeDetail.jsp", "_self")
	}

	function _excel() {
	<% if ((objSales.list != null) && (objSales.list.size() > 65000)) { %>
		window.alert("65,000 라인을 초과하는 데이터입니다.\n\n보다 상세한 검색 조건으로 검색 후 다운로드하세요.");
	<% } else { %>
		_submit($("form"), "SalesReportDailyExcel.jsp", "_top");
	<% } %>
	}
</script>