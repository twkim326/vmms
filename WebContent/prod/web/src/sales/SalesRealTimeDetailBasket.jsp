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
 * /service/VMSalesCount.jsp
 *
 * 자판기 매출정보 / 거래내역 / 장바구니 상세조회
 *
 * 작성일 - 2019-06-17 김태우
 *
 */

// 헤더
	request.setCharacterEncoding("UTF-8");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0402");

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
	cfg.put("window.mode", "B");

// 전송된 데이터



String Title_NO=StringEx.getKeyword(StringEx.charset(request.getParameter("NO")));
long LONG_NO=Long.parseLong(Title_NO);
String Title_ITEM_COUNT_minus=StringEx.getKeyword(StringEx.charset(request.getParameter("ITEM_COUNT_minus")));
long LONG_ITEM_COUNT_minus=Long.parseLong(Title_ITEM_COUNT_minus);

String Title_TRANSACTION_DATE=StringEx.getKeyword(StringEx.charset(request.getParameter("TRANSACTION_DATE")));
String Title_ORGAN=StringEx.getKeyword(StringEx.charset(request.getParameter("ORGAN")));
String Title_VMCODE=StringEx.getKeyword(StringEx.charset(request.getParameter("VMCODE")));
String Title_PLACE =StringEx.getKeyword(StringEx.charset(request.getParameter("PLACE")));
String Title_GOODS=StringEx.getKeyword(StringEx.charset(request.getParameter("GOODS")));
String Title_AMOUNT=StringEx.getKeyword(StringEx.charset(request.getParameter("AMOUNT")));
String Title_PAY_TYPE=StringEx.getKeyword(StringEx.charset(request.getParameter("PAY_TYPE")));
String Title_INPUT_TYPE=StringEx.getKeyword(StringEx.charset(request.getParameter("INPUT_TYPE")));
String Title_PAY_STEP=StringEx.getKeyword(StringEx.charset(request.getParameter("PAY_STEP")));
String Title_CLOSING_DATE=StringEx.getKeyword(StringEx.charset(request.getParameter("CLOSING_DATE")));
String Title_PAY_DATE=StringEx.getKeyword(StringEx.charset(request.getParameter("PAY_DATE")));
String Title_CANCEL_DATE =StringEx.getKeyword(StringEx.charset(request.getParameter("CANCEL_DATE")));

String Title_TERMINAL_ID=StringEx.getKeyword(StringEx.charset(request.getParameter("TERMINAL_ID")));
String Title_TRANSACTION_NO=StringEx.getKeyword(StringEx.charset(request.getParameter("TRANSACTION_NO")));

String Title_TERMINAL_TRANS_SEQ=StringEx.getKeyword(StringEx.charset(request.getParameter("TERMINAL_TRANS_SEQ")));

// 인스턴스 생성
	/* VM objSales = new VM(cfg); */
	Sales objSales = new Sales(cfg);
// 메서드 호출
	String error = null;

//Sales.java 호출 (장바구니 상세 조회)
	try {		
		error = objSales.Detail_Basket(Title_TERMINAL_ID,Title_TRANSACTION_NO);	// 쿼리수정으로 인한 파라미터 수정 scheo 20200714
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
<!-- 주석추가 : 20190613 김태우
	 기능 : 모달창 -->
	 
	 
<!-- TEST : 지앤비벤딩 7루트 청솔학원(노원점) 강북청솔학원 8층  
2019-06-17 15:35:37
2000014353	남양)츄파춥스포도345 1500 -->
<!-- 모달 시작 -->
<div id="window">
	<div class="title">
		<span>장바구니 상세보기</span>
	</div>
	<div style="border: 1px solid #397cc8;"></div>
	<br/>
<%-- 	

<!-- 2019-06-17 김태우 추가 -->
<!-- SalesRealTimeDetail.jsp 에서 넘긴 파라미터-->
<input type="hidden" name="TERMINAL_ID" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("TERMINAL_ID")))%>" />
<input type="hidden" name="TRANSACTION_NO" value="<%=StringEx.getKeyword(StringEx.charset(request.getParameter("TRANSACTION_NO")))%>" />
<input type="hidden" name="NO" value="<%=c.getLong("NO")%>" />
<input type="hidden" name="TRANSACTION_DATE" value="<%=c.get("TRANSACTION_DATE")%>" />
<input type="hidden" name="ORGAN" value="<%=Html.getText(StringEx.setDefaultValue(c.get("ORGAN"), "-"))%>" />
<input type="hidden" name="PLACE" value="<%=Html.getText(StringEx.setDefaultValue(c.get("PLACE"), "-"))%>" />
<input type="hidden" name="VMCODE" value="<%=Html.getText(StringEx.setDefaultValue(c.get("VMCODE"), "-"))%>" />
<input type="hidden" name="GOODS" value="<%=Html.getText(StringEx.setDefaultValue(c.get("GOODS"), "-"))%>" />
<input type="hidden" name="AMOUNT" value="<%=StringEx.comma(c.getLong("AMOUNT"))%>" />
<input type="hidden" name="PAY_TYPE" value="<%=Html.getText(StringEx.setDefaultValue(c.get("PAY_TYPE").equals("선불") ? c.get("PAY_CARD") : c.get("PAY_TYPE"), "-"))%>" />
<input type="hidden" name="INPUT_TYPE" value="<%=Html.getText(StringEx.setDefaultValue(c.get("INPUT_TYPE"), "-"))%>" />
<input type="hidden" name="PAY_STEP" value="<%=Html.getText(StringEx.setDefaultValue(c.get("PAY_STEP"), "-"))%>" />
<input type="hidden" name="CLOSING_DATE" value="<%=Html.getText(StringEx.setDefaultValue(c.get("CLOSING_DATE"), "-"))%>" />
<input type="hidden" name="PAY_DATE" value="<%=Html.getText(StringEx.setDefaultValue(c.get("PAY_DATE"), "-"))%>" />
<input type="hidden" name="CANCEL_DATE" value="<%=Html.getText(StringEx.setDefaultValue(c.get("CANCEL_DATE"), "-"))%>" />
 --%>
	<div style="    
	background: url(../web/icon_tit.gif) 0 0 no-repeat;
    padding-left: 12px;
    padding-bottom: 6px;
    clear: both;
    color: #397cc8;
    font-size: 12px;
    font-weight: bold;
    position: relative;">
		<span>* 장바구니 총합 거래</span>
	</div>
	<table cellpadding="0" cellspacing="0" class="tableType01">
		<colgroup>
			<col width="*">
			<col width="*">
			<col width="*">
			<col width="*">
			<col width="*">	
			<col width="*">
			<col width="*">
			<col width="*">		
		</colgroup>
		<thead>
			<tr>
				<th rowspan="2" nowrap=""> 순번 </th>
				<th colspan="1" nowrap=""> 거래일시 </th>
				<th colspan="1" nowrap=""> 조직 </th>
				<th colspan="1" nowrap=""> 설치위치 </th>
				<th colspan="1" nowrap=""> 자판기코드 </th>
				<th colspan="1" nowrap=""> 상품 </th>
				<th colspan="1" nowrap=""> 금액 </th>
				<th colspan="1" nowrap=""> 수단 </th>
				<th colspan="1" nowrap=""> 입력 </th>
				<%-- 카드번호, 카드사 데이터, 승인번호 추가 scheo 20200713  --%>
				<th colspan="1" nowrap=""> 카드번호</th>
				<th colspan="1" nowrap=""> 카드사 </th>
				<th colspan="1" nowrap=""> 승인번호 </th>
				<th colspan="1" nowrap=""> 거래일련번호 </th>
				<th colspan="1" nowrap=""> 상태 </th>
				<th colspan="1" nowrap=""> 마감일시 </th>
				<th colspan="1" nowrap=""> 입금(예정)일 </th>
				<th colspan="1" nowrap=""> 취소일시 </th>
				
			</tr>
		</thead>
		<tbody>
<%	if (objSales.list.size()  > 0 ) {
		GeneralConfig c = (GeneralConfig) objSales.list.get(0); %>		
		<tr>
			<td class="center number" nowrap><%=LONG_NO%></td>
			<td class="center number" nowrap><%=Title_TRANSACTION_DATE%></td>
			<td class="center" nowrap><%=Title_ORGAN%></td>
			<td class="center" nowrap><%=Title_PLACE %></td>
			<td class="center" nowrap><%=Title_VMCODE%></td>
			<td class="center" nowrap><%=Title_GOODS+" 외 "+LONG_ITEM_COUNT_minus+ " 건"%></td>
			<td class="right number" nowrap><%=Title_AMOUNT%></td>
			<td class="center" nowrap><%=Title_PAY_TYPE%></td>
			<td class="center" nowrap><%=Title_INPUT_TYPE%></td>
			<%-- 카드번호, 카드사 데이터 추가 scheo 20200713  --%>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("CARD_NO"), "-"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PURCHASE_ORGAN_NAME"), "-"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("APPROVAL_NO"), "-"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("TERMINAL_TRANS_SEQ"), "-"))%></td>
			<td class="center" nowrap><%=Title_PAY_STEP%></td>
			<td class="center number" nowrap><%=Title_CLOSING_DATE%></td>
			<td class="center number" nowrap><%=Title_PAY_DATE %></td>
			<td class="center number" nowrap><%=Title_CANCEL_DATE%></td>
		</tr>
<%-- 		<tr >
			<td class="center number" nowrap><%=NO%></td>
			<td class="center number" nowrap><%=TRANSACTION_DATE%></td>
			<td nowrap><%=ORGAN%></td>	
			<td nowrap><%=PLACE%></td>
			<td class="center" nowrap><%=VMCODE%></td>	
			<td nowrap><%=GOODS%></td>
			<td class="right number" nowrap><%=AMOUNT%></td>
			<td class="center" nowrap><%=PAY_TYPE%></td>
			<td class="center" nowrap><%=INPUT_TYPE%></td>
			<td class="center" nowrap><%=PAY_STEP%></td>
			<td class="center number" nowrap><%=CLOSING_DATE%></td>
			<td class="center number" nowrap><%=PAY_DATE%></td>
			<td class="center number" nowrap><%=CANCEL_DATE%></td>
		</tr>	 --%>	
<% } %>

		</tbody>		
	</table>
	<br/>
	<br/>
	<div style="    
		background: url(../web/icon_tit.gif) 0 0 no-repeat;
	    padding-left: 12px;
	    padding-bottom: 6px;
	    clear: both;
	    color: #397cc8;
	    font-size: 12px;
	    font-weight: bold;
	    position: relative;">
		<span>* 장바구니 상세 내역</span>
	</div>
	<table cellpadding="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="15">
		<col width="50">	
		<col width="*">	
		<col width="40">
		<col width="75">
		<col width="50">
		<col width="70">
		<col width="45">
		<col width="70">
		<col width="45">
		<col width="40">
		<col width="40">
		<col width="30">
		<col width="40">
		<col width="30">
		
	</colgroup>
	<thead>
		<tr>
			<th rowspan="2" nowrap=""> 순번 </th>
			<th colspan="1" nowrap=""> 거래일시 </th>
			<th colspan="1" nowrap=""> 조직 </th>
			<th colspan="1" nowrap=""> 설치위치 </th>
			<th colspan="1" nowrap=""> 자판기코드 </th>
			<th colspan="1" nowrap=""> 상품 </th>
			<th colspan="1" nowrap=""> 금액 </th>
			<th colspan="1" nowrap=""> 수단 </th>
			<th colspan="1" nowrap=""> 입력 </th>
			<th colspan="1" nowrap=""> 상태 </th>
			<th colspan="1" nowrap=""> 마감일시 </th>
			<th colspan="1" nowrap=""> 입금(예정)일 </th>
			<th colspan="1" nowrap=""> 취소일시 </th>
			
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objSales.list.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>" title="단말기 ID = <%=Title_TERMINAL_ID%>, 거래번호 = <%=Title_TRANSACTION_NO%>" onclick="more_detail();";>
			<td class="center number" nowrap><%=i+1%></td>
			<td class="center number" nowrap><%=Title_TRANSACTION_DATE%></td>
			<td nowrap><%=Title_ORGAN%></td>
			<td nowrap><%=Title_PLACE %></td>
			<td class="center" nowrap><%=Title_VMCODE%></td>
			<td nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("SEARCH_PRODUCT_NAME"), "-"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("SEARCH_AMOUNT"))%></td>
			<td class="center" nowrap><%=Title_PAY_TYPE%></td>
			<td class="center" nowrap><%=Title_INPUT_TYPE%></td>
			<td class="center" nowrap><%=Title_PAY_STEP%></td>
			<td class="center number" nowrap><%=Title_CLOSING_DATE%></td>
			<td class="center number" nowrap><%=Title_PAY_DATE %></td>
			<td class="center number" nowrap><%=Title_CANCEL_DATE%></td>
		</tr>
<% } %>

<% if (objSales.list.size() == 0) { %>
		<tr>
			<td colspan="21" align="center">장바구니 내역이 없습니다</td>
		</tr>
<% } %>
	</tbody>
	</table>
<!-- # 검색결과 -->
	<div class="buttonArea">
		<input type="button" value="" class="btnCancelS" onclick="new parent.IFrame().close();" />
	</div>
<%@ include file="../../footer.inc.jsp" %>
</div>
<!-- 모달 종료 -->

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script type="text/javascript">

</script>