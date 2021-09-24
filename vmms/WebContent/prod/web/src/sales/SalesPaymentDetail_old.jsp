<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.Sales
			, java.util.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /sales/SalesPaymentDetail.jsp
 *
 * 자판기 매출정보 > 매출별 입금 현황 > 세부 내용
 *
 * 작성일 - 2011/05/10, 정원광
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
		out.print(Message.reload("top"));
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
	String payment = StringEx.getKeyword(StringEx.charset(request.getParameter("paymentAll")));
	String[] payTypes = "Y".equals(payment) ? null : request.getParameterValues("payment");
	String sDate = StringEx.getKeyword(StringEx.charset(request.getParameter("sDate")));
	String eDate = StringEx.getKeyword(StringEx.charset(request.getParameter("eDate")));
	int oMode = StringEx.str2int(request.getParameter("oMode"), -1);
	int oType = StringEx.str2int(request.getParameter("oType"), -1);

	if (oMode == -1) oMode = 1;
	if (oType == -1) oType = 0;
	if (payTypes != null) {
		for (int i = 0; i < payTypes.length; i++) {
			payTypes[i] = StringEx.getKeyword(StringEx.charset(payTypes[i]));
		}
	}

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "company:organ:place:sDate:eDate");

// 인스턴스 생성
	Sales objSales = new Sales(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objSales.payment(company, organ, place, sDate, eDate, payTypes, oMode, oType);
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

<% if (company == 0 || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) { %>
<div class="infoSearch">
	<img src="<%=cfg.get("imgDir")%>/web/txt_searchInfo.gif" alt="상단의 검색 조건을 통해 데이터를 검색해 주세요." />
</div>
<% } else { %>
<div class="infoBar">
	검색 결과 : <strong><%=StringEx.comma(objSales.data.get("LISTCNT"))%></strong>건
	<input type="button" class="button2" onclick="_excel();" value="엑셀저장" />
</div>
<div style="width:960px; overflow:auto; overflow-y:hidden; padding-bottom:10px;">
<table cellpadding="0" cellspacing="0" class="tableType06" id="detail">
	<thead>
		<tr>
			<th rowspan="3" nowrap><a href="?oMode=1&oType=<%=(oMode == 1 && oType == 0 ? 1 : 0) + addParam%>" class="<%=(oMode == 1 ? (oType == 1 ? "desc" : "asc") : "none")%>" onmouseover="this.className = '<%=(oMode == 1 ? (oType == 1 ? "asc" : "desc") : "asc")%>';" onmouseout="this.className = '<%=(oMode == 1 ? (oType == 1 ? "desc" : "asc") : "none")%>';" title="<%=objSales.data.get("ORGAN")%>별 정렬 <%=(oMode == 1 && oType == 0 ? "(↑)" : "(↓)")%>"><%=objSales.data.get("ORGAN")%></a></th>
			<th rowspan="3" nowrap><a href="?oMode=2&oType=<%=(oMode == 2 && oType == 0 ? 1 : 0) + addParam%>" class="<%=(oMode == 2 ? (oType == 1 ? "desc" : "asc") : "none")%>" onmouseover="this.className = '<%=(oMode == 2 ? (oType == 1 ? "asc" : "desc") : "asc")%>';" onmouseout="this.className = '<%=(oMode == 2 ? (oType == 1 ? "desc" : "asc") : "none")%>';" title="설치 위치별 정렬 <%=(oMode == 2 && oType == 0 ? "(↑)" : "(↓)")%>">설치 위치</a></th>
			<th rowspan="3" nowrap>자판기코드</th>
			<th colspan="<%=(4 + objSales.company.size() * 2)%>" nowrap>마감</th>
			<th colspan="2">매입보류</th>
			<th colspan="2">매입거절</th>
			<th colspan="<%=(3 + objSales.company.size() * 2)%>" nowrap>입금</th>
		</tr>
		<tr>
			<th rowspan="2" nowrap>시작일시</th>
			<th rowspan="2" nowrap>종료일시</th>
			<th colspan="2" nowrap>신용</th>
	<% for (int i = 0; i < objSales.company.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.company.get(i); %>
			<th colspan="2" nowrap><%=c.get("NAME")%></th>
	<% } %>
			<th rowspan="2" nowrap>건수</th>
			<th rowspan="2" nowrap>금액</th>
			<th rowspan="2" nowrap>건수</th>
			<th rowspan="2" nowrap>금액</th>
			<th rowspan="2" nowrap>날짜</th>
			<th colspan="2" nowrap>신용</th>
	<% for (int i = 0; i < objSales.company.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.company.get(i); %>
			<th colspan="2" nowrap><%=c.get("NAME")%></th>
	<% } %>
		</tr>
		<tr>
			<th nowrap>건수</th>
			<th nowrap>금액</th>
	<% for (int i = 0; i < objSales.company.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.company.get(i); %>
			<th nowrap>건수</th>
			<th nowrap>금액</th>
	<% } %>
			<th nowrap>건수</th>
			<th nowrap>금액</th>
	<% for (int i = 0; i < objSales.company.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.company.get(i); %>
			<th nowrap>건수</th>
			<th nowrap>금액</th>
	<% } %>
		</tr>
	</thead>
	<tbody>
	<% if (objSales.list.size() > 0) { %>
		<% 
	
			for (int i = 0; i < objSales.list.size(); i++) 
			{ 
				GeneralConfig c = (GeneralConfig) objSales.list.get(i); 
				

				String strPayCnt = c.get("ROWSPANCNT");
				String strRowspan = "";
				
				if(strPayCnt.equals("1"))
				{
					strRowspan = "rowspan=\"1\"";
				}
				else if(strPayCnt.equals("0"))
				{
					strRowspan = "";
				}
				else
				{
					strRowspan = "rowspan=\"" + strPayCnt + "\"";
				}
		%>
		<tr>
			<td <%=strRowspan%> class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("ORGAN"), "-"))%></td>
			<td <%=strRowspan%> nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PLACE"), "-"))%></td>
			<td <%=strRowspan%> nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("VM_CODE"), "-"))%></td>
			<td <%=strRowspan%> class="center number" nowrap><%=Html.getText(c.get("START_DATE"))%></td>
			<td <%=strRowspan%> class="center number" nowrap><%=Html.getText(c.get("END_DATE"))%></td>
			<td <%=strRowspan%> class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_CARD"))%></td>
			<td <%=strRowspan%> class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_CARD"))%></td>
			<% for (int j = 0; j < objSales.company.size(); j++) { GeneralConfig s = (GeneralConfig) objSales.company.get(j); 
			%>
			<td <%=strRowspan%> class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_PREPAY_" + s.get("CODE")))%></td>
			<td <%=strRowspan%> class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_PREPAY_" + s.get("CODE")))%></td>
			<% } %>
			<td <%=strRowspan%> class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_POSTPONE"))%></td>
			<td <%=strRowspan%> class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_POSTPONE"))%></td>
			<td <%=strRowspan%> class="right number" nowrap><%=StringEx.comma(c.getLong("CNT_REFUSE"))%></td>
			<td <%=strRowspan%> class="right number" nowrap><%=StringEx.comma(c.getLong("AMOUNT_REFUSE"))%></td>
		<%
		String strTpPaydate = c.get("PAY_DATE");
		if( strTpPaydate == null || strTpPaydate.equals(""))
		{
			%>
		<td colspan="<%=(6 + objSales.company.size() * 2)%>" class="center" nowrap>입금 현황이 없습니다</td>
		</tr>					
			<%
		}
		else if(Integer.parseInt(strPayCnt) > 1 )
		{
				int kx = 0;
				for(kx=0 ; kx < Integer.parseInt(strPayCnt) ; kx++)
				{
					if(kx==0)
					{	
		%>
			<td style="background-color:<%=(c.getLong("PAY_DATE") >= StringEx.str2long(sDate) && c.getLong("PAY_DATE") <= StringEx.str2long(eDate) ? "" : "#f0f7ff")%>;" class="center number" nowrap><%=date(c.get("PAY_DATE"))%></td>
			<td onclick="_goods('<%=c.get("PAY_DATE")%>', '<%=c.get("CLOSING_DATE")%>', <%=c.get("ORGANIZATION_SEQ")%>, <%=c.get("VM_PLACE_SEQ")%>, '');" style="cursor:pointer; background-color:<%=(c.getLong("PAY_DATE") >= StringEx.str2long(sDate) && c.getLong("PAY_DATE") <= StringEx.str2long(eDate) ? "" : "#f0f7ff")%>;" class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_CNT_CARD"))%></td>
			<td onclick="_goods('<%=c.get("PAY_DATE")%>', '<%=c.get("CLOSING_DATE")%>', <%=c.get("ORGANIZATION_SEQ")%>, <%=c.get("VM_PLACE_SEQ")%>, '');" style="cursor:pointer; background-color:<%=(c.getLong("PAY_DATE") >= StringEx.str2long(sDate) && c.getLong("PAY_DATE") <= StringEx.str2long(eDate) ? "" : "#f0f7ff")%>;" class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_AMOUNT_CARD"))%></td>
					<% for (int k = 0; k < objSales.company.size(); k++) { GeneralConfig s = (GeneralConfig) objSales.company.get(k); %>
			<td onclick="_goods('<%=c.get("PAY_DATE")%>', '<%=c.get("CLOSING_DATE")%>', <%=c.get("ORGANIZATION_SEQ")%>, <%=c.get("VM_PLACE_SEQ")%>, '<%=s.get("CODE")%>');" style="cursor:pointer; background-color:<%=(c.getLong("PAY_DATE") >= StringEx.str2long(sDate) && c.getLong("PAY_DATE") <= StringEx.str2long(eDate) ? "" : "#f0f7ff")%>;" class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_CNT_PREPAY_" + s.get("CODE")))%></td>
			<td onclick="_goods('<%=c.get("PAY_DATE")%>', '<%=c.get("CLOSING_DATE")%>', <%=c.get("ORGANIZATION_SEQ")%>, <%=c.get("VM_PLACE_SEQ")%>, '<%=s.get("CODE")%>');" style="cursor:pointer; background-color:<%=(c.getLong("PAY_DATE") >= StringEx.str2long(sDate) && c.getLong("PAY_DATE") <= StringEx.str2long(eDate) ? "" : "#f0f7ff")%>;" class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_AMOUNT_PREPAY_" + s.get("CODE")))%></td>
					<% } %>
			</tr>		
		<%
					}
					else
					{
						GeneralConfig cx = (GeneralConfig) objSales.list.get(i+kx); 
		%>
					<tr>
			<td style="background-color:<%=(cx.getLong("PAY_DATE") >= StringEx.str2long(sDate) && cx.getLong("PAY_DATE") <= StringEx.str2long(eDate) ? "" : "#f0f7ff")%>;" class="center number" nowrap><%=date(cx.get("PAY_DATE"))%></td>
			<td onclick="_goods('<%=cx.get("PAY_DATE")%>', '<%=cx.get("CLOSING_DATE")%>', <%=cx.get("ORGANIZATION_SEQ")%>, <%=cx.get("VM_PLACE_SEQ")%>, '');" style="cursor:pointer; background-color:<%=(cx.getLong("PAY_DATE") >= StringEx.str2long(sDate) && cx.getLong("PAY_DATE") <= StringEx.str2long(eDate) ? "" : "#f0f7ff")%>;" class="right number" nowrap><%=StringEx.comma(cx.getLong("PAY_CNT_CARD"))%></td>
			<td onclick="_goods('<%=cx.get("PAY_DATE")%>', '<%=cx.get("CLOSING_DATE")%>', <%=cx.get("ORGANIZATION_SEQ")%>, <%=cx.get("VM_PLACE_SEQ")%>, '');" style="cursor:pointer; background-color:<%=(cx.getLong("PAY_DATE") >= StringEx.str2long(sDate) && cx.getLong("PAY_DATE") <= StringEx.str2long(eDate) ? "" : "#f0f7ff")%>;" class="right number" nowrap><%=StringEx.comma(cx.getLong("PAY_AMOUNT_CARD"))%></td>
					<% for (int k = 0; k < objSales.company.size(); k++) { GeneralConfig s = (GeneralConfig) objSales.company.get(k); %>
			<td onclick="_goods('<%=cx.get("PAY_DATE")%>', '<%=cx.get("CLOSING_DATE")%>', <%=cx.get("ORGANIZATION_SEQ")%>, <%=cx.get("VM_PLACE_SEQ")%>, '<%=s.get("CODE")%>');" style="cursor:pointer; background-color:<%=(cx.getLong("PAY_DATE") >= StringEx.str2long(sDate) && cx.getLong("PAY_DATE") <= StringEx.str2long(eDate) ? "" : "#f0f7ff")%>;" class="right number" nowrap><%=StringEx.comma(cx.getLong("PAY_CNT_PREPAY_" + s.get("CODE")))%></td>
			<td onclick="_goods('<%=cx.get("PAY_DATE")%>', '<%=cx.get("CLOSING_DATE")%>', <%=cx.get("ORGANIZATION_SEQ")%>, <%=cx.get("VM_PLACE_SEQ")%>, '<%=s.get("CODE")%>');" style="cursor:pointer; background-color:<%=(cx.getLong("PAY_DATE") >= StringEx.str2long(sDate) && cx.getLong("PAY_DATE") <= StringEx.str2long(eDate) ? "" : "#f0f7ff")%>;" class="right number" nowrap><%=StringEx.comma(cx.getLong("PAY_AMOUNT_PREPAY_" + s.get("CODE")))%></td>
								<% } %>
					</tr>		
		<%
					}
				}
				
				i = i+(kx-1);
			}
			else if(Integer.parseInt(strPayCnt) == 1 )
			{	
		%>
			<td style="background-color:<%=(c.getLong("PAY_DATE") >= StringEx.str2long(sDate) && c.getLong("PAY_DATE") <= StringEx.str2long(eDate) ? "" : "#f0f7ff")%>;" class="center number" nowrap><%=date(c.get("PAY_DATE"))%></td>
			<td onclick="_goods('<%=c.get("PAY_DATE")%>', '<%=c.get("CLOSING_DATE")%>', <%=c.get("ORGANIZATION_SEQ")%>, <%=c.get("VM_PLACE_SEQ")%>, '');" style="cursor:pointer; background-color:<%=(c.getLong("PAY_DATE") >= StringEx.str2long(sDate) && c.getLong("PAY_DATE") <= StringEx.str2long(eDate) ? "" : "#f0f7ff")%>;" class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_CNT_CARD"))%></td>
			<td onclick="_goods('<%=c.get("PAY_DATE")%>', '<%=c.get("CLOSING_DATE")%>', <%=c.get("ORGANIZATION_SEQ")%>, <%=c.get("VM_PLACE_SEQ")%>, '');" style="cursor:pointer; background-color:<%=(c.getLong("PAY_DATE") >= StringEx.str2long(sDate) && c.getLong("PAY_DATE") <= StringEx.str2long(eDate) ? "" : "#f0f7ff")%>;" class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_AMOUNT_CARD"))%></td>
					<% for (int k = 0; k < objSales.company.size(); k++) { GeneralConfig s = (GeneralConfig) objSales.company.get(k); %>
			<td onclick="_goods('<%=c.get("PAY_DATE")%>', '<%=c.get("CLOSING_DATE")%>', <%=c.get("ORGANIZATION_SEQ")%>, <%=c.get("VM_PLACE_SEQ")%>, '<%=s.get("CODE")%>');" style="cursor:pointer; background-color:<%=(c.getLong("PAY_DATE") >= StringEx.str2long(sDate) && c.getLong("PAY_DATE") <= StringEx.str2long(eDate) ? "" : "#f0f7ff")%>;" class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_CNT_PREPAY_" + s.get("CODE")))%></td>
			<td onclick="_goods('<%=c.get("PAY_DATE")%>', '<%=c.get("CLOSING_DATE")%>', <%=c.get("ORGANIZATION_SEQ")%>, <%=c.get("VM_PLACE_SEQ")%>, '<%=s.get("CODE")%>');" style="cursor:pointer; background-color:<%=(c.getLong("PAY_DATE") >= StringEx.str2long(sDate) && c.getLong("PAY_DATE") <= StringEx.str2long(eDate) ? "" : "#f0f7ff")%>;" class="right number" nowrap><%=StringEx.comma(c.getLong("PAY_AMOUNT_PREPAY_" + s.get("CODE")))%></td>
					<%		
						}
					%>
			</tr>			
			<% 
			 } 
			%>
		<% } %>
		<tr>
			<td colspan="3" class="center" style="background-color:#e9e9e9;" nowrap>합계</td>
			<td colspan="2" class="center" style="background-color:#e9e9e9;" nowrap>-</td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_CARD"))%></td>
		<% for (int i = 0; i < objSales.company.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.company.get(i); %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_PREPAY_" + c.get("CODE")))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_PREPAY_" + c.get("CODE")))%></td>
		<% } %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_POSTPONE"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_POSTPONE"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("CNT_REFUSE"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("AMOUNT_REFUSE"))%></td>
			<td class="center" style="background-color:#e9e9e9;" nowrap>-</td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_CARD"))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_CARD"))%></td>
		<% for (int i = 0; i < objSales.company.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.company.get(i); %>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_CNT_PREPAY_"+ c.get("CODE")))%></td>
			<td class="right number" style="background-color:#e9e9e9;" nowrap><%=StringEx.comma(objSales.data.getLong("PAY_AMOUNT_PREPAY_"+ c.get("CODE")))%></td>
		<% } %>
		</tr>
	<% } else {
		
				int intColspan = 14 + (objSales.company.size()*4);
		%>
		<tr>
			<td class="center" colspan="<%=intColspan%>">해당 기간동안 발생한 매출이 없습니다</td>
		</tr>
	<% } %>
	</tbody>
</table>
</div>
<% } %>

<%@ include file="../../footer.inc.jsp" %>

<script language="javascript">
	function _excel() {
		var records = <%=(objSales.list == null ? 0 : objSales.list.size())%>;

		if (records > 10000) {
			window.alert('10,000 라인을 초과하는 데이터입니다.\n\n보다 상세한 검색 조건으로 검색 후 다운로드하세요.');
			return;
		}

		location.href = 'SalesPaymentExcel.jsp?1=1<%=addParam%>';
	}
	function _goods(aDate, cDate, organ, place, card) {
		Common.openWin('SalesPaymentDifference.jsp?company=<%=company%>&organ=' + organ + '&place=' + place + '&aDate=' + aDate + '&cDate=' + cDate + '&card=' + card, 'SalesPaymentDifference', 530, 400, 'yes');
	}

//	Event.observe(window, 'load', function (event) {
		var o = $('detail');

		if (o) {
			var w = o.clientWidth;

			if (w < 960) {
				o.setStyle({width : '960px'});
			}
		}
//	}.bind(this));
</script>
<%!
	private String date(String date) {
		return StringEx.isEmpty(date) ? "-" : date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8);
	}
%>