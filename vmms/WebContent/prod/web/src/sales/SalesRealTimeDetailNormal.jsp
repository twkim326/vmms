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
 * 작성일 - 2020-07-10 scheo
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

	try {		
		error = objSales.Detail_Normal(Title_TERMINAL_ID,Title_TRANSACTION_NO);
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

<!-- 모달 시작 -->
<div id="window">
	<div class="title">
		<span>거래내역 상세보기</span>
	</div>
	<div style="border: 1px solid #397cc8;"></div>
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
		<span>* 거래내역 상세</span>
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
			<td class="center"nowrap><%=Title_GOODS%></td>
			<td class="right number" nowrap><%=Title_AMOUNT%></td>
			<td class="center" nowrap><%=Title_PAY_TYPE%></td>
			<td class="center" nowrap><%=Title_INPUT_TYPE%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("CARD_NO"), "-"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PURCHASE_ORGAN_NAME"), "-"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("APPROVAL_NO"), "-"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("TERMINAL_TRANS_SEQ"), "-"))%></td>
			<td class="center" nowrap><%=Title_PAY_STEP%></td>
			<td class="center number" nowrap><%=Title_CLOSING_DATE%></td>
			<td class="center number" nowrap><%=Title_PAY_DATE %></td>
			<td class="center number" nowrap><%=Title_CANCEL_DATE%></td>
		</tr>
<% } %>


<% if (objSales.list.size() == 0) { %>
		<tr>
			<td colspan="21" align="center">거래 내역이 없습니다</td>
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