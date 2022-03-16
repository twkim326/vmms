<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.Log
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /system/SystemBatchLog.jsp
 *
 * 시스템 > 서비스 로그 > 배치 로그
 *
 * 작성일 - 2011/03/23, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0107");

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
	cfg.put("window.mode", "A");

// 전송된 데이터
	int pageNo = StringEx.str2int(request.getParameter("page"), 1);
	String sDate = StringEx.getKeyword(StringEx.charset(request.getParameter("sDate")));
	String eDate = StringEx.getKeyword(StringEx.charset(request.getParameter("eDate")));
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));

	if("".equals(sDate)) {
		sDate = com.nucco.lib.DateTime.date("yyyyMMdd");
	}

	if("".equals(eDate)) {
		eDate = com.nucco.lib.DateTime.date("yyyyMMdd");
	}

	if (sDate.compareTo(eDate) > 0) {
		String temp = sDate;

		sDate = eDate;
		eDate = temp;
	}

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "page:sDate:eDate:sField:sQuery");

// 인스턴스 생성
	Log objLog = new Log(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objLog.batch(pageNo, sDate, eDate, sField, sQuery);
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

<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/calendar/js/calendar.js"></script>
<div class="title">
	<span>서비스 로그 조회</span>
</div>

<form method="get">
<table cellspacing="0" class="tableType02">
	<colgroup>
		<col width="110" />
		<col width="*"/>
		<col width="73" />
	</colgroup>
	<tr>
		<th><span>검색어</span></th>
		<td>
			<select name="sField">
				<option value="">- 검색필드</option>
				<option value="SERVER_NAME"<%=sField.equals("SERVER_NAME") ? " selected" : ""%>>서버</option>
				<option value="SERVICE_NAME"<%=sField.equals("SERVICE_NAME") ? " selected" : ""%>>서비스</option>
				<option value="ERR_DESC"<%=sField.equals("ERR_DESC") ? " selected" : ""%>>설명</option>
			</select>
			<input type="text" name="sQuery" class="txtInput" value="<%=Html.getText(sQuery)%>" />
		</td>
		<td rowspan="2" class="center last">
		  <input type="button" value="" class="btnSearch" onclick="_search(this.form);" />
		</td>
	</tr>
	<tr>
		<th><span>검색기간</span></th>
		<td>
			<input type="text" name="sDate" id="sDate" class="txtInput" value="<%=Html.getText(sDate)%>" style="width:80px;" maxlength="8" onkeyup="if (this.value != '' && !/^[0-9]+$/.test(this.value)) { window.alert('숫자만 입력이 가능합니다.'); this.value = ''; }" />
			<img src="<%=cfg.get("imgDir")%>/web/icon_calendar.gif" style="cursor:pointer;" alt="날짜 선택" onclick="(function () {
					var e = event ? event : window.event;
					Calendar.open($('sDate'), null, Event.pointerX(e), Event.pointerY(e), $('sDate').value);
				})(event)" />
			~
			<input type="text" name="eDate" id="eDate" class="txtInput" value="<%=Html.getText(eDate)%>" style="width:80px;" maxlength="8" onkeyup="if (this.value != '' && !/^[0-9]+$/.test(this.value)) { window.alert('숫자만 입력이 가능합니다.'); this.value = ''; }" />
			<img src="<%=cfg.get("imgDir")%>/web/icon_calendar.gif" style="cursor:pointer;" alt="날짜 선택" onclick="(function () {
					var e = event ? event : window.event;
					Calendar.open($('eDate'), null, Event.pointerX(e), Event.pointerY(e), $('eDate').value);
				})(event)" />
			<input type="button" class="button1" value="오늘" onclick="_setDate(0);" />
			<input type="button" class="button1" value="어제" onclick="_setDate(-1);" />
			<input type="button" class="button1" value="이번달" onclick="_setDate(1);" />
			<input type="button" class="button1" value="지난달" onclick="_setDate(2);" />
		</td>
	</tr>
</table>
</form>

<!-- 검색결과 -->
<div class="infoBar mt23">
	검색 결과 : <strong><%=StringEx.comma(objLog.records)%></strong>건
	<select onchange="location.href = this.options[selectedIndex].value;">
		<option value="SystemBatchLog.jsp" selected>배치 로그</option>
		<option value="SystemChangeLog.jsp">변경 로그</option>
	</select>
</div>
<table cellpadding="0" cellspacing="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="50" />
		<col width="100" />
		<col width="200" />
		<col width="120" />
		<col width="120" />
		<col width="*" />
		<col width="45" />
		<col width="45" />
	</colgroup>
	<thead>
		<tr>
			<th nowrap>순번</th>
			<th nowrap>서버</th>
			<th nowrap>서비스</th>
			<th nowrap>시작시간</th>
			<th nowrap>종료시간</th>
			<th nowrap>설명</th>
			<th nowrap>성공</th>
			<th nowrap>실패</th>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objLog.list.size(); i++) { GeneralConfig c = (GeneralConfig) objLog.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>">
			<td class="center number" nowrap><%=c.getLong("NO")%></td>
			<td class="center"><%=Html.getText(c.get("SERVER_NAME"))%></td>
			<td class="center"><%=Html.getText(c.get("SERVICE_NAME"))%></td>
			<td class="center number" nowrap><%=date(c.get("START_DATE") + c.get("START_TIME"))%></td>
			<td class="center number" nowrap><%=date(c.get("END_DATE") + c.get("END_TIME"))%></td>
			<td class="left"><%=Html.nl2br(Html.getText(StringEx.setDefaultValue(c.get("ERR_DESC"), "-")))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getInt("SUCCESS"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getInt("FAILURE"))%></td>
		</tr>
<% } %>
<% if (objLog.list.size() == 0) { %>
		<tr>
			<td colspan="8" align="center">등록된 기록이 없습니다</td>
		</tr>
<% } %>
	</tbody>
</table>
<!-- # 검색결과 -->

<!-- 페이징 -->
<div class="paging"><%=Pager.getList(request, pageNo, cfg.getInt("limit.page"), objLog.pages, "", "page", "", buttons)%></div>
<!-- # 페이징 -->

<%@ include file="../../footer.inc.jsp" %>

<script type="text/javascript">
	function _search(o) {
		if (o.sQuery.value != '' && o.sField.selectedIndex == 0) {
			window.alert('검색 필드를 선택하세요.');
			return;
		} else if (o.sQuery.value == '' && o.sField.selectedIndex > 0) {
			window.alert('검색어를 입력하세요.');
			return;
		}

		o.submit();
	}

	function _setDate(n) {
		var d = new Date();

		if (n > 0) {
			var s = new Date(d.getFullYear(), d.getMonth() - n + 1, 1);
			var e = new Date(d.getFullYear(), d.getMonth() - n + 2, 0);

			$("sDate").value = s.getFullYear() + (s.getMonth() < 9 ? "0" : "") + (s.getMonth() + 1) + (s.getDate() <= 9 ? "0" : "") + s.getDate();
			$("eDate").value = e.getFullYear() + (e.getMonth() < 9 ? "0" : "") + (e.getMonth() + 1) + (e.getDate() <= 9 ? "0" : "") + e.getDate();
		} else {
			var s = new Date(d.getFullYear(), d.getMonth(), d.getDate() + n);

			$("sDate").value = s.getFullYear() + (s.getMonth() < 9 ? "0" : "") + (s.getMonth() + 1) + (s.getDate() <= 9 ? "0" : "") + s.getDate();
			$("eDate").value = s.getFullYear() + (s.getMonth() < 9 ? "0" : "") + (s.getMonth() + 1) + (s.getDate() <= 9 ? "0" : "") + s.getDate();
		}
	}
</script>
<%!
	private String date(String date) {
		if (date.length() != 14) {
			return "-";
		}

		return date.substring(0, 4)
			+ "-" + date.substring(4, 6)
			+ "-" + date.substring(6, 8)
			+ " " + date.substring(8, 10)
			+ ":" + date.substring(10, 12)
			+ ":" + date.substring(12, 14);
	}
%>