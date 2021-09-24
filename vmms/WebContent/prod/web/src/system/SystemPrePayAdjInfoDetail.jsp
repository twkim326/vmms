<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.Adjust
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /System/SystemPrePayAdjInfoDetail.jsp
 *
 * 시스템 > 입금정보조회 > SID에 따른 거래내역 조회
 *
 * 작성일 - 2012/01/30, 최문봉
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0109");

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
	String aDate = StringEx.getKeyword(StringEx.charset(request.getParameter("aDate")));
	String sSid = StringEx.getKeyword(StringEx.charset(request.getParameter("SID")));
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));
	int pageNo = StringEx.str2int(request.getParameter("page"), 1);

// 인스턴스 생성
	Adjust objAdj = new Adjust(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objAdj.detail( sSid , aDate, pageNo);
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
		<span>선불카드입금정산정보 현황</span>
	</div>

	<div class="infoBar3">
	검색 결과 : <strong><%=StringEx.comma(objAdj.records)%></strong>건
	</div>
	<table cellpadding="0" cellspacing="0" class="tableType01 tableType05">
	<colgroup>
		<col width="60" />
		<col width="*" />
		<col width="*" />
		<col width="30" />
		<col width="50" />
		<col width="50" />
		<col width="50" />
		<col width="50" />
	</colgroup>
	<thead>
		<tr>
			<th>단말기ID</th>
			<th>소속</th>
			<th>설치위치</th>
			<th>건수</th>
			<th>정산<br>금액</th>
			<th>수수료</th>			
			<th>미과<br>입금액</th>
			<th>입금액</th>
		</tr>
	</thead>
	<tbody>
	<% for (int i = 0; i < objAdj.list.size(); i++) { GeneralConfig c = (GeneralConfig) objAdj.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>">
			<td class="center"><%=c.get("TERMINAL_ID")%></td>
			<td class="center"><%=Html.getText(c.get("ORGAN"))%></td>
			<td class="center"><%=Html.getText(c.get("PLACE"))%></td>
			<td class="center number"><%=StringEx.comma(c.getLong("CNT"))%></td>
			<td class="right number"><%=StringEx.comma(c.getLong("AMOUNT"))%></td>
			<td class="right number"><%=StringEx.comma(c.getLong("COMMISSION"))%></td>
			<td class="right number"><%=StringEx.comma(c.getLong("OUTAMOUNT"))%></td>
			<td class="right number"><%=StringEx.comma(c.getLong("IN_AMOUNT"))%></td>			
		</tr>
	<% } %>
	<% if (objAdj.list.size() == 0) { %>
		<tr>
			<td colspan="9" align="center">내역이 없습니다.</td>
		</tr>
	<% } %>
	</tbody>
	</table>

	<div class="paging paging2"><%=Pager.getList(request, pageNo, cfg.getInt("limit.page"), objAdj.pages, "", "page", "", buttons)%></div>
</div>

<%@ include file="../../footer.inc.jsp" %>

<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/calendar/js/calendar.js"></script>
