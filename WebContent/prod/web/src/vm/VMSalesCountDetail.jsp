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
 * /service/VMSalesCount.jsp
 *
 * 자판기 운영정보 > 판매계수 > 목록
 *
 * 작성일 - 2017/07/18, 황재원
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0303");

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

	String terminal_id = (request.getParameter("terminal_id"));
	String count_date = (request.getParameter("count_date"));


// 인스턴스 생성
	VM objVM = new VM(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objVM.getSalesCountDetail(terminal_id, count_date);
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
<div id="window">
	<div class="title">
		<span>판매계수 컬럼상세</span>
	</div>

	<table cellpadding="0" cellspacing="0" class="tableType01">
		<colgroup>
			<col width="*" />
			<col width="*" />
			<col width="*" />
			<col width="*" />
			<col width="*" />	
			<col width="*" />
			<col width="*" />
			<col width="*" />		
		</colgroup>
		<thead>
			<tr>
				<th nowrap> 단말기 ID </th>
				<th nowrap> 수집일시 </th>
				<th nowrap> 수집타입 </th>
				<th nowrap> 수집모드 </th>
				<th nowrap> 전체컬럼수 </th>
				<th nowrap> 위치코드 </th>
				<th nowrap> 설치위치 </th>
				<th nowrap> 등록일시 </th>
			</tr>
		</thead>
		<tbody>
<%	if (objVM.list.size()  > 0 ) {
		GeneralConfig c = (GeneralConfig) objVM.list.get(0); %>
		<tr >
			<td class="center number" nowrap><%=Html.getText(c.get("TERMINAL_ID"))%></td>	
			<td class="center" nowrap><%=Html.getText(c.get("COUNT_DATE"))%></td>	
			<td class="center" nowrap><%=Html.getText(c.get("COUNT_TYPE"))%></td>
			<td class="center" nowrap><%=Html.getText(c.get("COUNT_MODE"))%></td>
			<td class="center number" nowrap><%=StringEx.comma(c.getLong("COL_COUNT"))%></td>
			<td class="center" nowrap><%=Html.getText(c.get("GOODS_CODE"))%></td>
			<td class="center" nowrap><%=Html.getText(c.get("PRODUCT_NAME"))%></td>
			<td class="center" nowrap><%=Html.getText(c.get("REG_DATE"))%></td>
		</tr>		
<% } %>

		</tbody>		
	</table>
	<br/>
	<table cellpadding="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="15" />
		<col width="50" />
		<col width="*" />		
		<col width="*" />	
		<col width="40" />
		<col width="50" />	
		<col width="*" />		
		<col width="75" />
		<col width="50" />
		<col width="70" />
		<col width="45" />
		<col width="70" />
		<col width="45" />
		<col width="40" />
		<col width="30" />
		<col width="40" />
		<col width="30" />
		
	</colgroup>
	<thead>
		<tr>
			<th rowspan="2" nowrap> 컬럼 </th>
			<th colspan="4" nowrap> 상품 </th>
			<!-- <th colspan="2" nowrap> 테스트상품 </th> -->
			<th colspan="2" nowrap> 전체 </th>
			<th colspan="2" nowrap> 현금 </th>
			<th colspan="2" nowrap> 카드 </th>
			<th colspan="2" nowrap> 테스트 </th>
			<th colspan="2" nowrap> 프리 </th>
			
		</tr>
		<tr>
			<th nowrap> 코드 </th>
			<th nowrap> 이름 </th>
			<th nowrap> 등록가 </th>
			<th nowrap> 판매가 </th>
			<!-- <th nowrap> 코드 </th>
			<th nowrap> 이름 </th>  -->
			<th nowrap> 금액 </th>
			<th nowrap> 건수 </th>
			<th nowrap> 금액 </th>
			<th nowrap> 건수 </th>
			<th nowrap> 금액 </th>
			<th nowrap> 건수 </th>
			<th nowrap> 금액 </th>
			<th nowrap> 건수 </th>
			<th nowrap> 금액 </th>
			<th nowrap> 건수 </th>
			
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objVM.list.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.list.get(i); %>
		<tr class="<%=StringEx.comma(c.getLong("PRODUCT_PRICE")).equals(StringEx.comma(c.getLong("PRICE"))) || (Html.getText(c.get("GOODS_CODE")).equals("1111111   ") || Html.getText(c.get("GOODS_CODE")).equals("9999999   ")) ? (i % 2 == 1 ? "bgLine" : "") : "bgAlertText" %>" value="<%= Html.getText(c.get("GOODS_CODE")) %>" > <!-- scheo 20191104 등록가 판매가 금액차이 수정 --> 
			<td class="center number" nowrap><%=StringEx.comma(c.getLong("COL_NO"))%></td>
			<td class="center" nowrap><%=Html.getText(c.get("GOODS_CODE"))%></td>
			<td class="left" nowrap><%=i==0?"&nbsp;":Html.getText(StringEx.setDefaultValue(c.get("PRODUCT_NAME"), "&nbsp;"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PRODUCT_PRICE"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PRICE"))%></td>
			<!-- <td class="center" nowrap><%=Html.getText(c.get("TEST_CODE"))%></td>
			<td class="left" nowrap><%=i==0?"&nbsp;":Html.getText(StringEx.setDefaultValue(c.get("TEST_PRODUCT_NAME"), "&nbsp;"))%></td>  -->
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("TOTAL_AMOUNT"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("TOTAL_COUNT"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CASH_AMOUNT"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CASH_COUNT"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CARD_AMOUNT"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("CARD_COUNT"))%></td>
			<td class="right number" nowrap>0</td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("TEST_COUNT"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("FREE_AMOUNT"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("FREE_COUNT"))%></td>
			
		</tr>		
<% } %>
<% if (objVM.list.size() == 0) { %>
		<tr>
			<td colspan="21" align="center">등록된 자판기가 없습니다</td>
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
<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script type="text/javascript">

</script>