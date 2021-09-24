<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Sales
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /sales/SalesReportVMDetail.jsp
 *
 * 자판기 매출정보 > 조건별 매출 현황 > 자판기별
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

	long depth = StringEx.str2long(request.getParameter("depth"), 0);
	
	if (oMode == -1) oMode = 1;
	if (oType == -1) oType = 0;
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
		error = objSales.column(sType, company, organ,depth, place, sDate, eDate, payTypes, oMode, oType);
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

<% if (company == 0 || StringEx.isEmpty(sType) || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) { %>
<div class="infoSearch">
	<img src="<%=cfg.get("imgDir")%>/web/txt_searchInfo.gif" alt="상단의 검색 조건을 통해 데이터를 검색해 주세요." />
</div>
<% } else { %>
<div class="infoBar">
	검색 결과 : <strong><%=StringEx.comma(objSales.list.size())%></strong>건
	<input type="button" class="button2" onclick="_excel();" value="엑셀저장" />
</div>
<div style="width:100%; overflow:auto; overflow-y:hidden; padding-bottom:10px;">
<form name="form" id="form" method="post">
<input type="hidden" id="sType" name="sType" value="<%=sType%>" />
<input type="hidden" name="_sType" value="<%=sType%>" />
<input type="hidden" name="company" value="<%=company%>" />
<input type="hidden" name="organ" value="<%=organ%>" />
<input type="hidden" id="place" name="place" value="<%=place%>" />
<input type="hidden" name="_place" value="<%=place%>" />
<input type="hidden" name="sDate" value="<%=sDate%>" />
<input type="hidden" name="_sDate" value="<%=sDate%>" />
<input type="hidden" name="eDate" value="<%=eDate%>" />
<input type="hidden" name="_eDate" value="<%=eDate%>" />
<input type="hidden" name="paymentAll" value="<%=StringEx.setDefaultValue(payment, "")%>" />
<% if (payTypes != null) for (String payType : payTypes) { %>
<input type="hidden" name="payment" value="<%=payType%>" />
<% } %>
<input type="hidden" id="stepAll" name="stepAll" value="Y" />
<input type="hidden" id="step" name="step" value="29" />
<input type="hidden" id="oMode" name="oMode" value="<%=oMode%>" />
<input type="hidden" id="oType" name="oType" value="<%=oType%>" />
<input type="hidden" id="goUrl" name="goUrl" value="<%=StringEx.replace(request.getRequestURL().toString(), "\"", "\\\"")%>" />
<table cellpadding="0" cellspacing="0" cellspacing="0" class="tableType01">
	<thead>
		<tr>
			<th rowspan="2" nowrap><a href="javascript:_sort(1,<%=(oMode == 1 && oType == 0 ? 1 : 0)%>);" class="<%=(oMode == 1 ? (oType == 1 ? "desc" : "asc") : "none")%>" onmouseover="this.className = '<%=(oMode == 1 ? (oType == 1 ? "asc" : "desc") : "asc")%>';" onmouseout="this.className = '<%=(oMode == 1 ? (oType == 1 ? "desc" : "asc") : "none")%>';" title="<%=objSales.data.get("ORGAN")%>별 정렬 <%=(oMode == 1 && oType == 0 ? "(↑)" : "(↓)")%>"><%=objSales.data.get("ORGAN")%></a></th>
			<th rowspan="2" nowrap><a href="javascript:_sort(2,<%=(oMode == 2 && oType == 0 ? 1 : 0)%>);" class="<%=(oMode == 2 ? (oType == 1 ? "desc" : "asc") : "none")%>" onmouseover="this.className = '<%=(oMode == 2 ? (oType == 1 ? "asc" : "desc") : "asc")%>';" onmouseout="this.className = '<%=(oMode == 2 ? (oType == 1 ? "desc" : "asc") : "none")%>';" title="설치위치별 정렬 <%=(oMode == 2 && oType == 0 ? "(↑)" : "(↓)")%>">설치 위치</a></th>
			<!-- //20120604 자판기코드 추가-->
			<th rowspan="2" nowrap>자판기코드</th>
			<th rowspan="2" nowrap>단말기ID</th>
			<th rowspan="2" nowrap>컬럼</th>
			<th rowspan="2" nowrap>상품명</th>
			<th colspan="2" nowrap>현금</th>
			<th colspan="2" nowrap>신용</th>
			<th colspan="2" nowrap>간편결제</th>
			<th colspan="2" nowrap>선불</th>
			<th colspan="2" nowrap>정보수집</th>
			<th colspan="2" nowrap>합계</th>
		</tr>
		<tr>
			<th nowrap>건수</th>
			<th nowrap>금액</th>
			<th nowrap>건수</th>
			<th nowrap>금액</th>
			<th nowrap>건수</th>
			<th nowrap>금액</th>
			<th nowrap>건수</th>
			<th nowrap>금액</th>
			<th nowrap>건수</th>
			<th nowrap>금액</th>
			<th nowrap>건수</th>
			<th nowrap>금액</th>
		</tr>
	</thead>
	<tbody>
	<%
		// 20160112 상단에 합계 추가
		if (objSales.list.size() > 0) { %>
		<tr>
		 	<td class="center" colspan="6" style="background-color:#e9e9e9;" nowrap>총계</td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_PAYCO"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_PAYCO"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_PREPAY"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_PREPAY"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_ITEM"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_ITEM"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_TOTAL"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_TOTAL"))%></td>
		</tr>
	<%	}
	
		// 2016012 중간에 소계 추가
		String[] groupColumns = new String[] { "ORGAN", "PLACE" };
		String[] summaryColumns = {
				"CNT_CASH", "AMOUNT_CASH", "CNT_CARD", "AMOUNT_CARD", "CNT_PAYCO", "AMOUNT_PAYCO",
				"CNT_PREPAY", "AMOUNT_PREPAY", "CNT_ITEM", "AMOUNT_ITEM", "CNT_TOTAL", "AMOUNT_TOTAL"
			};
		String[] groupValues = new String[groupColumns.length];
		long[][] summaryValues = new long[groupColumns.length][summaryColumns.length];

		for (int i = 0; i < objSales.list.size(); i++) {
			GeneralConfig c = (GeneralConfig) objSales.list.get(i);

			if ((groupValues[0] != null) && !c.get(groupColumns[0]).equals(groupValues[0])) { %>
		<tr>
			<td class="center number" nowrap></td>
		 	<td class="center" colspan="5" style="background-color:#e9e9e9;" nowrap>소계(<%=groupValues[1]%>)</td>
			 	<% for (int j = 0; j < summaryColumns.length; j++) { %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(summaryValues[1][j])%></td>
				<% } %>
		</tr>
		<tr>
		 	<td class="center" colspan="6" style="background-color:#e9e9e9;" nowrap>합계(<%=groupValues[0]%>)</td>
		 	<% for (int j = 0; j < summaryColumns.length; j++) { %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(summaryValues[0][j])%></td>
			<%		summaryValues[0][j] = summaryValues[1][j] = c.getLong(summaryColumns[j]);
				}
		 	
				groupValues[0] = StringEx.setDefaultValue(c.get(groupColumns[0]), "");
				groupValues[1] = StringEx.setDefaultValue(c.get(groupColumns[1]), "");
			%>
		</tr>
		<% } else if ((groupValues[1] != null) && !c.get(groupColumns[1]).equals(groupValues[1])) { %>
		<tr>
			<td class="center number" nowrap></td>
		 	<td class="center" colspan="5" style="background-color:#e9e9e9;" nowrap>소계(<%=groupValues[1]%>)</td>
		 	<% for (int j = 0; j < summaryColumns.length; j++) { %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(summaryValues[1][j])%></td>
			<%		summaryValues[0][j] += summaryValues[1][j] = c.getLong(summaryColumns[j]);
				}
		 	
		 		groupValues[1] = StringEx.setDefaultValue(c.get(groupColumns[1]), "");
		 	%>
		</tr>
		<% } else {
				if (groupValues[0] == null) groupValues[0] = StringEx.setDefaultValue(c.get(groupColumns[0]), "");
				if (groupValues[1] == null) groupValues[1] = StringEx.setDefaultValue(c.get(groupColumns[1]), "");
				
				for (int j = 0; j < summaryColumns.length; j++) {
					long value = c.getLong(summaryColumns[j]);
				
					summaryValues[0][j] += value;
					summaryValues[1][j] += value;
				}
			} %>
		<tr onclick="_detail(<%=c.getLong("VM_PLACE_SEQ")%>, <%=c.getLong("AMOUNT_TOTAL")%>);" style="cursor:pointer;<%=c.getLong("AMOUNT_TOTAL") < 0L ? "background-color:#e9aaaa;" : ""%>">
			<td nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("ORGAN"), "-"))%></td>
			<td nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PLACE"), "-"))%></td>
			<!-- //20120604 자판기코드 추가-->
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("VM_CODE"), "-"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("TERMINAL_ID"), "-"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("COL_NO"), "-"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PRODUCT"), "-"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_CASH"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_CASH"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_CARD"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_CARD"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_PAYCO"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_PAYCO"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_PREPAY"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_PREPAY"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_ITEM"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_ITEM"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_TOTAL"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_TOTAL"))%></td>
		</tr>
	<% } %>
	<% if (objSales.list.size() == 0) { %>
		<tr>
		<!-- //20120604 자판기코드 추가-->
		<!-- 
			<td class="center" colspan="10">해당 기간동안 발생한 매출이 없습니다</td>
		 -->		
			<td class="center" colspan="14">해당 기간동안 발생한 매출이 없습니다</td>
		</tr>
	<% } else { %>
		<% if (groupValues[0] != null) { %>
		<tr>
			<td class="center number" nowrap></td>
		 	<td class="center" colspan="5" style="background-color:#e9e9e9;" nowrap>소계(<%=groupValues[1]%>)</td>
			<% for (int j = 0; j < summaryColumns.length; j++) { %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(summaryValues[1][j])%></td>
			<% } %>
		</tr>
		<tr>
		 	<td class="center" colspan="6" style="background-color:#e9e9e9;" nowrap>합계(<%=groupValues[0]%>)</td>
		 	<% for (int j = 0; j < summaryColumns.length; j++) { %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(summaryValues[0][j])%></td>
			<% } %>
		</tr>
		<% } %>
		<tr>
			<!-- //20120604 자판기코드 추가-->
			<!-- 
			<td class="center" colspan="2" style="background-color:#e9e9e9;" nowrap>합계</td>
			 -->
			<td class="center" colspan="6" style="background-color:#e9e9e9;" nowrap>총계</td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_PAYCO"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_PAYCO"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_PREPAY"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_PREPAY"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_ITEM"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_ITEM"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_TOTAL"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_TOTAL"))%></td>
		</tr>
	<% } %>
	</tbody>
</table>
</form>
</div>
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
	
	function _sort(mode, type) {
		$("oMode").value = mode;
		$("oType").value = type;
	
		_submit($("form"), "", "_self");
	}
	
	function _detail(place, amount) {
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
		_submit($("form"), "SalesReportVMColumnExcel.jsp", "_top");
	<% } %>
	}
</script>