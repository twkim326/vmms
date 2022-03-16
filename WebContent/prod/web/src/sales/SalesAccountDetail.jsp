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
 * /sales/SalesAccountDetail.jsp
 *
 * 자판기 매출정보 > 월 정산 레포트 > 세부 내용
 *
 * 작성일 - 2011/04/09, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0404");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(Message.refresh(cfg.get("script.login") + "?goUrl=" + StringEx.encode(cfg.get("topDir") + "/web/src/sales/SalesAccount.jsp"), null, "top"));
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
	//String[] payTypes = "03".equals(sType) ? new String[] { "01" } : new String[] { "11" };
	// scheo 2019.02.25 허승찬 
	String[] payTypes = null;
	if("03".equals(sType)){
		payTypes = new String[] { "01" };
	}else if ("04".equals(sType)){
		payTypes = new String[] { "11" };
	}else{
		payTypes = new String[] { "07" };
	};
	
	String sDate = StringEx.getKeyword(StringEx.charset(request.getParameter("sDate")));
	String eDate = StringEx.getKeyword(StringEx.charset(request.getParameter("eDate")));
	int oMode = StringEx.str2int(request.getParameter("oMode"), -1);
	int oType = StringEx.str2int(request.getParameter("oType"), -1);

	long company = cfg.getLong("user.company");
	long organ = cfg.getLong("user.organ");

	if (oMode == -1) oMode = 1;
	if (oType == -1) oType = 0;

// 인스턴스 생성
	Sales objSales = new Sales(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objSales.summary(sType, company, organ, 0, sDate, eDate, payTypes, oMode, oType);
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

<%	if (StringEx.isEmpty(sType) || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) { %>
<div class="infoSearch">
	<img src="<%=cfg.get("imgDir")%>/web/txt_searchInfo.gif" alt="상단의 검색 조건을 통해 데이터를 검색해 주세요." />
</div>
<%	} else { %>
<div class="infoBar">
	검색 결과 : <strong><%=StringEx.comma(objSales.list.size())%></strong>건
	<input type="button" class="button2" onclick="_excel();" value="엑셀저장" />
</div>
<div style="width:100%; overflow:auto; overflow-y:hidden; padding-bottom:10px;">
<form name="form" id="form" method="post">
<input type="hidden" id="sType" name="sType" value="<%=sType%>" />
<input type="hidden" id="company" name="company" value="<%=company%>" />
<input type="hidden" id="organ" name="organ" value="<%=organ%>" />
<input type="hidden" id="place" name="place" value="" />
<input type="hidden" id="sDate" name="sDate" value="<%=sDate%>" />
<input type="hidden" name="_sDate" value="<%=sDate%>" />
<input type="hidden" id="eDate" name="eDate" value="<%=eDate%>" />
<input type="hidden" name="_eDate" value="<%=eDate%>" />
<input type="hidden" name="paymentAll" value="N" />
<input type="hidden" name="_paymentAll" value="N" />
	<% if (payTypes != null) for (String payType : payTypes) { %>
<input type="hidden" name="payment" value="<%=payType%>" />
<input type="hidden" name="_payment" value="<%=payType%>" />
	<% } %>
<input type="hidden" id="stepAll" name="stepAll" value="Y" />
<input type="hidden" id="step" name="step" value="29" />
<input type="hidden" id="oMode" name="oMode" value="<%=oMode%>" />
<input type="hidden" id="_oMode" name="_oMode" value="<%=oMode%>" />
<input type="hidden" id="oType" name="oType" value="<%=oType%>" />
<input type="hidden" id="_oType" name="_oType" value="<%=oType%>" />
<input type="hidden" name="goUrl" value="<%=request.getRequestURL()%>" />
<table id="detail" cellpadding="0" cellspacing="0" class="tableType06">
	<thead>
		<tr>
	<%	if (company > 0) { %>
			<th colspan="2" rowspan="2" nowrap><a href="javascript:_sort(1,<%=(oMode == 1 && oType == 0 ? 1 : 0)%>);" class="<%=(oMode == 1 ? (oType == 1 ? "desc" : "asc") : "none")%>" onmouseover="this.className = '<%=(oMode == 1 ? (oType == 1 ? "asc" : "desc") : "asc")%>';" onmouseout="this.className = '<%=(oMode == 1 ? (oType == 1 ? "desc" : "asc") : "none")%>';" title="<%=objSales.data.get("ORGAN")%>별 정렬 <%=(oMode == 1 && oType == 0 ? "(↑)" : "(↓)")%>"><%=objSales.data.get("ORGAN")%></a></th>
			<th rowspan="2" nowrap><a href="javascript:_sort(2,<%=(oMode == 2 && oType == 0 ? 1 : 0)%>);" class="<%=(oMode == 2 ? (oType == 1 ? "desc" : "asc") : "none")%>" onmouseover="this.className = '<%=(oMode == 2 ? (oType == 1 ? "asc" : "desc") : "asc")%>';" onmouseout="this.className = '<%=(oMode == 2 ? (oType == 1 ? "desc" : "asc") : "none")%>';" title="설치 위치별 정렬 <%=(oMode == 2 && oType == 0 ? "(↑)" : "(↓)")%>">설치위치</a></th>
			<th rowspan="2" nowrap>자판기코드</th>
	<%	} else { %>
			<th rowspan="2" nowrap>소속</th>
			<th colspan="2" rowspan="2" nowrap>거래처</th>
	<%	} %>
			<th rowspan="2" nowrap>판매기간</th>
	<%	if ("04".equals(sType)) { %>
		<%	for (int i = 0; i < objSales.company.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.company.get(i); %>
			<th colspan="3" nowrap><%=c.get("NAME")%></th>
		<%	} %>
			<th colspan="3" nowrap>합계</th>
		</tr>
		<tr>
		<%	for (int i = 0; i < objSales.company.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.company.get(i); %>
			<th nowrap>건수</th>
			<th nowrap>수수료</th>
			<th nowrap>입금액</th>
		<%	} %>
			<th nowrap>건수</th>
			<th nowrap>수수료</th>
			<th nowrap>입금액</th>
	<%	} else if("03".equals(sType)) { %>
			<th colspan="3" nowrap>신용</th>
		</tr>
		<tr>
			<th nowrap>건수</th>
			<th nowrap>수수료</th>
			<th nowrap>입금액</th>
			<!-- scheo 2018.10.15 -->
	<%	} else {%>
			<th colspan="3" nowrap>간편결제</th>
		</tr>
		<tr>
			<th nowrap>건수</th>
			<th nowrap>수수료</th>
			<th nowrap>입금액</th>
			<!-- scheo 2018.10.15 -->
	<% } %>
			
		</tr>
	</thead>
	<tbody>
	<%	if (objSales.list.size() <= 0) { %>
		<tr>
			<td class="center" colspan="<%="03".equals(sType) ? 14 : (prepayCompanyCount * 3 + 11)%>">해당 기간동안 발생한 매출이 없습니다</td>
		</tr>
	<%
		} else {
			ArrayList<String> summaryColumnList = new ArrayList<String>();
	%>
		<tr>
			<td class="center" colspan="<%=company > 0 ? 5 : 4%>" style="background-color:#e9e9e9;">총계</td>
	<%	if ("03".equals(sType)) {
			summaryColumnList.add("PAY_CNT_CARD");
			summaryColumnList.add("COMMISSION_CARD");
			summaryColumnList.add("PAY_AMOUNT_CARD");
	%>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("COMMISSION_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_CARD"))%></td>
	<%	} else if ("05".equals(sType)) {
			summaryColumnList.add("PAY_CNT_PAYCO");
			summaryColumnList.add("COMMISSION_PAYCO");
			summaryColumnList.add("PAY_AMOUNT_PAYCO");
	%>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_PAYCO"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("COMMISSION_PAYCO"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_PAYCO"))%></td>
	<%	} else {
			for (int j = 0; j < prepayCompanyCount; j++) {
				GeneralConfig s = (GeneralConfig) objSales.company.get(j);

				summaryColumnList.add("PAY_CNT_PREPAY_" + s.get("CODE"));
				summaryColumnList.add("COMMISSION_PREPAY_" + s.get("CODE"));
				summaryColumnList.add("PAY_AMOUNT_PREPAY_" + s.get("CODE"));
		%>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("COMMISSION_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_PREPAY_" + s.get("CODE")))%></td>
		<%	}
			summaryColumnList.add("PAY_CNT_PREPAY");
			summaryColumnList.add("COMMISSION_PREPAY");
			summaryColumnList.add("PAY_AMOUNT_PREPAY");
		%>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_PREPAY"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("COMMISSION_PREPAY"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_PREPAY"))%></td>
	<%	} %>
		</tr>
		<%
			// 20160125 중간에 소계 추가
			String[] groupColumns
				= (oMode == 2) ? new String[] { "PLACE", "PARENT_ORGAN" }
				: company > 0 ? new String[] { "PARENT_ORGAN", "ORGAN" }
				: new String[] { "COMPANY", "PARENT_ORGAN" };
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
		 	<td class="center" colspan="<%=company > 0 ? 4 : 3%>" style="background-color:#e9e9e9;" nowrap>소계(<%=groupValues[1]%>)</td>
					<%	for (int j = 0; j < summaryColumns.length; j++) { %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(summaryValues[1][j])%></td>
					<%	} %>
		</tr>
				<%	} %>
		<tr>
		 	<td class="center" colspan="<%=company > 0 ? 5 : 4%>" style="background-color:#e9e9e9;" nowrap>합계(<%=groupValues[0]%>)</td>
		 		<%	for (int j = 0; j < summaryColumns.length; j++) { %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(summaryValues[0][j])%></td>
				<%
						summaryValues[0][j] = summaryValues[1][j] = c.getLong(summaryColumns[j]);
					}

					groupValues[0] = StringEx.setDefaultValue(c.get(groupColumns[0]), "");
					groupValues[1] = StringEx.setDefaultValue(c.get(groupColumns[1]), "");
			%>
		</tr>
			<%	} else if ((groupValues[1] != null) && !c.get(groupColumns[1]).equals(groupValues[1])) { %>
		<tr>
				<%	if (!StringEx.isEmpty(groupValues[0])) { %>
			<td class="center" nowrap>&nbsp;</td>
			<td class="center" colspan="<%=company > 0 ? 4 : 3%>" style="background-color:#e9e9e9;" nowrap>소계(<%=groupValues[1]%>)</td>
				<%	} else { %>
			<td class="center" colspan="<%=company > 0 ? 5 : 4%>" style="background-color:#e9e9e9;" nowrap>소계(<%=groupValues[1]%>)</td>
				<%	} %>
		 		<%	for (int j = 0; j < summaryColumns.length; j++) { %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(summaryValues[1][j])%></td>
				<%
						summaryValues[0][j] += summaryValues[1][j] = c.getLong(summaryColumns[j]);
					}
			
				groupValues[1] = StringEx.setDefaultValue(c.get(groupColumns[1]), "");
				%>
		</tr>
			<%
				} else {
					if (groupValues[0] == null) groupValues[0] = StringEx.setDefaultValue(c.get(groupColumns[0]), "");
					if (groupValues[1] == null) groupValues[1] = StringEx.setDefaultValue(c.get(groupColumns[1]), "");
					
					for (int j = 0; j < summaryColumns.length; j++) {
						long value = c.getLong(summaryColumns[j]);

						summaryValues[0][j] += value;
						summaryValues[1][j] += value;
					}
				}
			%>
		<tr onclick="_detail(<%=c.getLong("COMPANY_SEQ")%>, <%=c.getLong("ORGANIZATION_SEQ")%>, <%=c.getLong("VM_PLACE_SEQ")%>, <%=c.getLong("AMOUNT_TOTAL")%>);" style="cursor:pointer;<%=payAmountTotal < 0L ? "background-color:#e9aaaa;" : ""%>">
			<%	if (company <= 0) { %>
			<td nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("COMPANY"), "-"))%></td>
			<%	} %>
			<%	if (StringEx.isEmpty(c.get("PARENT_ORGAN"))) { %>
			<td colspan="2" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("ORGAN"), "-"))%></td>
			<%	} else { %>
			<td nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PARENT_ORGAN"), "-"))%></td>
			<td nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("ORGAN"), "-"))%></td>
			<%	} %>
			<%	if (company > 0) { %>
			<td nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PLACE"), "-"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("VM_CODE"), "-"))%></td>
			<%	} %>
			<td class="center number" nowrap><%=Html.getText(c.get("START_DATE"))%> ~ <%=Html.getText(c.get("END_DATE"))%></td>
			<%	if ("03".equals(sType)) { %>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_CNT_CARD"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("COMMISSION_CARD"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_AMOUNT_CARD"))%></td>
			<%  } else if ("05".equals(sType)) { %>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_CNT_PAYCO"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("COMMISSION_PAYCO"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_AMOUNT_PAYCO"))%></td>
			<%	} else {
					for (int j = 0; j < prepayCompanyCount; j++) { GeneralConfig s = (GeneralConfig) objSales.company.get(j); %>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_CNT_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("COMMISSION_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_AMOUNT_PREPAY_" + s.get("CODE")))%></td>
				<%	} %>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_CNT_PREPAY"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("COMMISSION_PREPAY"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_AMOUNT_PREPAY"))%></td>
			<%	} %>
		</tr>
		<%	} %>
		<%	if (!StringEx.isEmpty(groupValues[1])) { %>
		<tr>
			<%	if (!StringEx.isEmpty(groupValues[0])) { %>
			<td class="center number" nowrap>&nbsp;</td>
		 	<td class="center" colspan="<%=company > 0 ? 4 : 3%>" style="background-color:#e9e9e9;" nowrap>소계(<%=groupValues[1]%>)</td>
			<%	} else { %>
		 	<td class="center" colspan="<%=company > 0 ? 5 : 4%>" style="background-color:#e9e9e9;" nowrap>소계(<%=groupValues[1]%>)</td>
		 	<%	} %>
			<%	for (int j = 0; j < summaryColumns.length; j++) { %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(summaryValues[1][j])%></td>
			<%	} %>
		</tr>
		<%	} %>
		<%	if (!StringEx.isEmpty(groupValues[0])) { %>
		<tr>
			<td class="center" colspan="<%=company > 0 ? 5 : 4%>" style="background-color:#e9e9e9;" nowrap>합계(<%=groupValues[0]%>)</td>
			<%	for (int j = 0; j < summaryColumns.length; j++) { %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(summaryValues[0][j])%></td>
			<%	} %>
		</tr>
		<%	} %>
		<tr>
			<td class="center" colspan="<%=company > 0 ? 5 : 4%>" style="background-color:#e9e9e9;">총계</td>
		<%	if ("03".equals(sType)) { %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("COMMISSION_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_CARD"))%></td>
		<%  } else if ("05".equals(sType)) { %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_PAYCO"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("COMMISSION_PAYCO"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_PAYCO"))%></td>
		<%	} else {
				for (int j = 0; j < prepayCompanyCount; j++) { GeneralConfig s = (GeneralConfig) objSales.company.get(j); %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("COMMISSION_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_PREPAY_" + s.get("CODE")))%></td>
			<%	} %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_PREPAY"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("COMMISSION_PREPAY"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_PREPAY"))%></td>
		<%	} %>
		</tr>
	<%	} %>
	</tbody>
</table>
</form>
</div>
<%	} %>

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

	function _sort(mode, type) {
		$("oMode").value = mode;
		$("oType").value = type;

		_submit($("form"), "", "_self");
	}

	function _detail(company, organ, place, amount) {
		$("company").value = company;
		$("organ").value = organ;
		$("place").value = place;
		$("oMode").value = 2;
		$("oType").value = 0;

		_submit($("form"), "SalesReportDailyDetail.jsp", "_self")
	}

	function _excel() {
	<%	if ((objSales.list != null) && (objSales.list.size() > 65000)) { %>
		window.alert("65,000 라인을 초과하는 데이터입니다.\n\n보다 상세한 검색 조건으로 검색 후 다운로드하세요.");
	<%	} else { %>
		_submit($("form"), "SalesAccountExcel.jsp", "_top");
	<%	} %>
	}

//	Event.observe(window, 'load', function (event) {
	var o = $('detail');

	if (o) {
		var w = o.clientWidth;

		if (w < 960) {
			o.setStyle({width : '960px'});
		}
	}
//}.bind(this));
</script>
