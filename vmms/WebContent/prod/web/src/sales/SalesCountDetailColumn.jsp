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
 * /sales/SalesCountDetailColumn.jsp
 *
 * 자판기 매출정보 > 조건별 매출 현황 > 일별
 *
 * 작성일 - 2017/07/10, 황재원
 *
 */

 
// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0406");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(Message.refresh(cfg.get("script.login") + "?goUrl=" + StringEx.encode(cfg.get("topDir") + "/web/src/sales/SalesCount.jsp"), null, "top"));
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
	
	String pType = request.getParameter("pType");
	String sDate = StringEx.getKeyword(StringEx.charset(request.getParameter("sDate")));
	String eDate = StringEx.getKeyword(StringEx.charset(request.getParameter("eDate")));
	int oMode = StringEx.str2int(request.getParameter("oMode"), -1);
	int oType = StringEx.str2int(request.getParameter("oType"), -1);
	String goUrl = StringEx.charset(request.getParameter("goUrl"));
	
	long _organ = StringEx.str2long(request.getParameter("_organ"), 0);
	long _place = StringEx.str2long(request.getParameter("_place"), 0);
	String _sDate = StringEx.getKeyword(StringEx.charset(request.getParameter("_sDate")));
	String _eDate = StringEx.getKeyword(StringEx.charset(request.getParameter("_eDate")));
	int _oMode = StringEx.str2int(request.getParameter("_oMode"), -1);
	int _oType = StringEx.str2int(request.getParameter("_oType"), -1);
	String _goUrl = StringEx.charset(request.getParameter("_goUrl"));
	
	long organSeq = StringEx.str2long(request.getParameter("organSeq"));
	long placeSeq = StringEx.str2long(request.getParameter("placeSeq"));
	String startDate = StringEx.getKeyword(StringEx.charset(request.getParameter("startDate")));
	String endDate = StringEx.getKeyword(StringEx.charset(request.getParameter("endDate")));
	String terminalId = StringEx.getKeyword(StringEx.charset(request.getParameter("terminalId")));

	long depth = StringEx.str2long(request.getParameter("depth"), 0);
	
	if (oMode == -1) oMode = 0;
	if (oType == -1) oType = 1;


// 인스턴스 생성
	Sales objSales = new Sales(cfg);

// 메서드 호출
	String error = null;

	try {
		if (pType.equals("0") )
			error = objSales.salesCount(company, organSeq, depth, placeSeq, startDate, endDate, terminalId);
		else
			error = objSales.salesCountNew(company, organSeq, depth, placeSeq, startDate, endDate, terminalId);				
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

<%
	if (company == 0  || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
%>
<div class="infoSearch">
	<img src="<%=cfg.get("imgDir")%>/web/txt_searchInfo.gif" alt="상단의 검색 조건을 통해 데이터를 검색해 주세요." />
</div>
<%
	} else {
%>
<div class="infoBar">
	컬럼수 : <strong><%=StringEx.comma(objSales.list.size())%></strong> 
	<input type="button" class="button2" onclick="_excel()" value="엑셀저장" />
	<% if (!StringEx.isEmpty(goUrl)) { %>
	<input type="button" class="button2" value="집계보기" onclick="_return();" style="right:150px;" />
	<input type="button" class="button2" value="거래보기" onclick="_detail(<%=organSeq%>, <%=placeSeq%>, '<%=startDate%>', '<%=endDate%>', '<%=terminalId%>');" style="right:80px;" />
	<% } %>
</div>

<div style="width:100%; overflow:auto; overflow-y:hidden; padding-bottom:10px;">
<form name="form" id="form" method="post">
<input type="hidden" id="sType" name="sType" value="<%=sType%>" />
<input type="hidden" name="_sType" value="<%=sType%>" />
<input type="hidden" name="company" value="<%=company%>" />
<input type="hidden" name="organ" value="<%=organ%>" />
<input type="hidden" id="pType" name="pType" value="<%=pType%>" />
<input type="hidden" id="place" name="place" value="<%=place%>" />
<input type="hidden" id="_place" name="_place" value="<%=place%>" />
<input type="hidden" id="sDate" name="sDate" value="<%=sDate%>" />
<input type="hidden" id="_sDate" name="_sDate" value="<%=sDate%>" />
<input type="hidden" id="eDate" name="eDate" value="<%=eDate%>" />
<input type="hidden" id="_eDate" name="_eDate" value="<%=eDate%>" />
<input type="hidden" id="oMode" name="oMode" value="<%=oMode%>" />
<input type="hidden" id="oType" name="oType" value="<%=oType%>" />
<input type="hidden" id="goUrl" name="goUrl" value="<%=StringEx.replace(request.getRequestURL().toString(), "\"", "\\\"")%>" />
<% if (!StringEx.isEmpty(goUrl)) { %>
<input type="hidden" id="__place" name="__place" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("_place")))%>" />
<input type="hidden" id="__sDate" name="__sDate" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("_sDate")))%>" />
<input type="hidden" id="__eDate" name="__eDate" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("_eDate")))%>" />
<input type="hidden" id="_oMode" name="_oMode" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("_oMode")))%>" />
<input type="hidden" id="_oType" name="_oType" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("_oType")))%>" />
<input type="hidden" id="_goUrl" name="_goUrl" value="<%=StringEx.replace(goUrl, "\"", "\\\"")%>" />
<% } %>
<input type="hidden" id="organSeq" name="organSeq" value="<%=organSeq %>"/>
<input type="hidden" id="placeSeq" name="placeSeq" value="<%=placeSeq %>" />
<input type="hidden" id="startDate" name="startDate" value="<%=startDate %>" />
<input type="hidden" id="endDate" name="endDate"  value="<%=endDate %>"/>
<input type="hidden" id="terminalId" name="terminalId" value="<%=terminalId %>" />

<table id="detail" cellpadding="0" cellspacing="0" class="tableType06">
	<thead>
		<tr>
			<th colspan="2" rowspan="2" nowrap><%=objSales.data.get("ORGAN")%></th>
			<th rowspan="2" nowrap>설치위치</th>
			<th rowspan="2" nowrap>자판기코드</th>
			<th rowspan="2" nowrap>단말기ID</th>
			<th rowspan="2" nowrap>컬럼</th>
			<th colspan="4" nowrap>상품</th>
			<th rowspan="2" nowrap>마감기간</th>
			<th colspan="2" nowrap>현금</th>
			<th colspan="2" nowrap>신용</th>
			<th colspan="2" nowrap>간편결제</th>	<!-- scheo -->
	<%	for (int i = 0; i < objSales.company.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.company.get(i); %>
			<th colspan="2" nowrap><%=c.get("NAME")%></th>
	<%	} %>
			<th colspan="2" nowrap>합계</th>
		</tr>
		<tr>
			<th nowrap>품명</th>
			<th nowrap>코드</th>
			<th nowrap>등록가</th>
			<th nowrap>판매가</th>
			<th nowrap>건수</th>
			<th nowrap>금액</th>
			<th nowrap>건수</th>
			<th nowrap>금액</th>
			<th nowrap>건수</th>
			<th nowrap>금액</th>
			<th nowrap>건수</th>	<!-- scheo -->
			<th nowrap>금액</th>
	<%	for (int i = 0; i < objSales.company.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.company.get(i); %>
			<th nowrap>건수</th>
			<th nowrap>금액</th>
	<%	} %>	
		</tr>
	</thead>
	<tbody>
<%	if (objSales.list.size() <= 0) { %>	
		<tr>
			<td class="center" colspan="<%=(prepayCompanyCount * 2 + 17)%>">해당 기간동안 발생한 매출이 없습니다</td>
		</tr>
	<%
	} else {
		ArrayList<String> summaryColumnList = new ArrayList<String>();	
	%>	
		<tr>
			<td class="center" colspan="11" style="background-color:#e9e9e9;">총계</td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_SMTPAY"))%></td>	<!-- scheo -->
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_SMTPAY"))%></td>
		<%
			for (int j = 0; j < prepayCompanyCount; j++) {
				GeneralConfig s = (GeneralConfig) objSales.company.get(j);
		%>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_PREPAY_" + s.get("CODE")))%></td>
		<%	} %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_TOTAL"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_TOTAL"))%></td>
		</tr>	
		<%	
			for (int i = 0; i < objSales.list.size(); i++) {
				GeneralConfig c = (GeneralConfig) objSales.list.get(i); %>
		<tr class="<%=c.getLong("PRODUCT_PRICE")!=0 && c.getLong("PRODUCT_PRICE") != c.getLong("PRICE") ?"bgAlertLine": (i % 2 == 1 ? "bgLine" : "")%>" >		<!-- scheo-->
				<%	if (StringEx.isEmpty(c.get("PARENT_ORGAN"))) { %>
			<td colspan="2" class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("ORGAN"), "-"))%></td>
				<%	} else { %>
			<td nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PARENT_ORGAN"), "-"))%></td>
			<td nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("ORGAN"), "-"))%></td>
				<%	} %>
			<td nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PLACE"), "-"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("VM_CODE"), "-"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("TERMINAL_ID"), "-"))%></td>
			<td class="right center" nowrap><%=StringEx.comma(c.getLong("COL_NO"))%></td>
			<td class="left" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PRODUCT_NAME"), "미등록 상품"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PRODUCT_CODE"), "-"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PRODUCT_PRICE"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PRICE"))%></td>
			<td class="center number" nowrap><%=c.get("START_DATE")%> ~ <%=c.get("END_DATE")%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_CASH"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_CASH"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_CARD"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_CARD"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_SMTPAY"))%></td>	<!-- scheo -->
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_SMTPAY"))%></td>
				<%	for (int j = 0; j < prepayCompanyCount; j++) { GeneralConfig s = (GeneralConfig) objSales.company.get(j); %>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_PREPAY_" + s.get("CODE")))%></td>
				<%	} %>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_TOTAL"))%></td>
			<td  class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_TOTAL"))%></td>
		</tr>
		<%	} %>		
		<tr>
			<td class="center" colspan="11" style="background-color:#e9e9e9;">총계</td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_CASH"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_SMTPAY"))%></td>	<!-- scheo -->
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_SMTPAY"))%></td>
			<% for (int j = 0; j < prepayCompanyCount; j++) { GeneralConfig s = (GeneralConfig) objSales.company.get(j); %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_PREPAY_" + s.get("CODE")))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_PREPAY_" + s.get("CODE")))%></td>
			<% } %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_TOTAL"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_TOTAL"))%></td>
		</tr>		
	<%	} %>			
	</tbody>
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

	function _detail(organSeq, placeSeq, startDate, endDate, terminalId) {
		$("organSeq").value = organSeq;
		$("placeSeq").value = placeSeq;
		$("startDate").value = startDate;
		$("endDate").value = endDate;
		$("terminalId").value = terminalId;
		_submit($("form"), "SalesCountDetailRealTime.jsp", "_self")
	}

	function _excel() {
	<% if ((objSales.list != null) && (objSales.list.size() > 65000)) { %>
		window.alert("65,000 라인을 초과하는 데이터입니다.\n\n보다 상세한 검색 조건으로 검색 후 다운로드하세요.");
	<% } else { %>
		_submit($("form"), "SalesCountDetailColumnExcel.jsp", "_top");
	<% } %>
	}
</script>