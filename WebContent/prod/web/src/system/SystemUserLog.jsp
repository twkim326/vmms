<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.User
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /system/SystemUserLog.jsp
 *
 * 시스템 > 계정 > 접속 기록 > 목록
 *
 * 작성일 - 2011/03/23, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0102");

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
	String dateType = StringEx.getKeyword(StringEx.charset(request.getParameter("dateType")));
	String sDate = StringEx.getKeyword(StringEx.charset(request.getParameter("sDate")));
	String eDate = StringEx.getKeyword(StringEx.charset(request.getParameter("eDate")));
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));

	if ("".equals(dateType)) {
		dateType = "IN";
	}

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
	String addParam = Param.addParam(request, "page:dateType:sDate:eDate:sField:sQuery");

// 인스턴스 생성
	User objUser = new User(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objUser.log(pageNo, dateType, sDate, eDate, sField, sQuery);
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

<div class="title">
	<span>계정 접속로그 조회</span>
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
				<option value="B.ID"<%=sField.equals("B.ID") ? " selected" : ""%>>아이디</option>
				<option value="B.NAME"<%=sField.equals("B.NAME") ? " selected" : ""%>>이름</option>
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
			<select name="dateType">
				<option value="">- 기준시간</option>
				<option value="IN"<%=dateType.equals("IN") ? " selected" : ""%>>로그인</option>
				<option value="OUT"<%=dateType.equals("OUT") ? " selected" : ""%>>로그아웃</option>
			</select>
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
	검색 결과 : <strong><%=StringEx.comma(objUser.records)%></strong>건
</div>
<table cellpadding="0" cellspacing="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="50" />
		<col width="*" />
		<col width="*" />
		<col width="*" />
		<col width="120" />
		<col width="120" />
	</colgroup>
	<thead>
		<tr>
			<th nowrap>순번</th>
			<th nowrap>등급</th>
			<th nowrap>이름</th>
			<th nowrap>아이디</th>
			<th nowrap>로그인 날짜</th>
			<th nowrap>로그아웃 날짜</th>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objUser.list.size(); i++) { GeneralConfig c = (GeneralConfig) objUser.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>" onclick="location.href = 'SystemUserLogDetail.jsp?seq=<%=c.getLong("SEQ") + addParam%>';" style="cursor:pointer;">
			<td class="center number"><%=c.getLong("NO")%></td>
			<td class="center"><%=Html.getText(StringEx.setDefaultValue(c.get("AUTH"), "-"))%></td>
			<td class="center"><%=Html.getText(c.get("NAME"))%></td>
			<td class="center"><%=Html.getText(c.get("ID"))%></td>
			<td class="center number"><%=Html.getText(c.get("IN_DATE"))%></td>
			<td class="center number"><%=Html.getText(StringEx.setDefaultValue(c.get("OUT_DATE"), "-"))%></td>
		</tr>
<% } %>
<% if (objUser.list.size() == 0) { %>
		<tr>
			<td colspan="6" align="center">등록된 기록이 없습니다</td>
		</tr>
<% } %>
	</tbody>
</table>
<!-- # 검색결과 -->

<!-- 페이징 -->
<div class="paging"><%=Pager.getList(request, pageNo, cfg.getInt("limit.page"), objUser.pages, "", "page", "", buttons)%></div>
<!-- # 페이징 -->

<%@ include file="../../footer.inc.jsp" %>

<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/calendar/js/calendar.js"></script>
<script type="text/javascript">
	function _search(o) {
		if (o.sQuery.value != '' && o.sField.selectedIndex == 0) {
			window.alert('검색 필드를 선택하세요.');
			return;
		} else if (o.sQuery.value == '' && o.sField.selectedIndex > 0) {
			window.alert('검색어를 입력하세요.');
			return;
		} else if (!(o.sDate.value == '' && o.eDate.value == '') && o.dateType.selectedIndex == 0) {
			window.alert('기준 시간을 선택하세요.');
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