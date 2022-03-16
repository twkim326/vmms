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
 * /system/SystemUserLogDetail.jsp
 *
 * 시스템 > 계정 > 접속 기록 > 조회
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
	long seq = StringEx.str2int(request.getParameter("seq"), 0);
	int cPageNo = StringEx.str2int(request.getParameter("cPageNo"), 1);

	if (seq == 0) {
		out.print("계정번호가 존재하지 않습니다.");
		return;
	}

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "page:dateType:sDate:eDate:sField:sQuery");

// 인스턴스 생성
	User objUser = new User(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objUser.log(seq, cPageNo);
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

<table cellspacing="0" class="tableType03">
	<colgroup>
		<col width="110" />
		<col width="*"/>
		<col width="110" />
		<col width="*"/>
	</colgroup>
	<tr>
		<th><span>아이디</span></th>
		<td><%=Html.getText(objUser.data.get("ID"))%></td>
		<th><span>사번</span></th>
		<td><%=Html.getText(StringEx.setDefaultValue(objUser.data.get("EMPLOYEE_NO"), "-"))%></td>
	</tr>
	<tr>
		<th class="last"><span>이름</span></th>
		<td class="last"><%=Html.getText(objUser.data.get("NAME"))%></td>
		<th class="last"><span>등급</span></th>
		<td class="last"><%=Html.getText(objUser.data.get("AUTH"))%></td>
	</tr>
</table>

<!-- 검색결과 -->
<div class="infoBar mt23">
	검색 결과 : <strong><%=StringEx.comma(objUser.records)%></strong>건
	<input type="button" class="button2" value="목록보기" onclick="location.href = 'SystemUserLog.jsp?1=1<%=addParam%>';" />
</div>
<table cellpadding="0" cellspacing="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="50" />
		<col width="*" />
		<col width="*" />
		<col width="100" />
		<col width="100" />
	</colgroup>
	<thead>
		<tr>
			<th nowrap>순번</th>
			<th nowrap>로그인 날짜</th>
			<th nowrap>로그아웃 날짜</th>
			<th nowrap>접속 IP</th>
			<th nowrap>이용 시간</th>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objUser.list.size(); i++) { GeneralConfig c = (GeneralConfig) objUser.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>">
			<td class="center number"><%=c.getLong("NO")%></td>
			<td class="center number"><%=Html.getText(c.get("IN_DATE"))%></td>
			<td class="center number"><%=Html.getText(StringEx.setDefaultValue(c.get("OUT_DATE"), "-"))%></td>
			<td class="center number"><%=Html.getText(c.get("IN_IP"))%></td>
			<td class="center number"><%=displayTime(c.getInt("CONN_TIME"))%></td>
		</tr>
<% } %>
	</tbody>
</table>
<!-- # 검색결과 -->

<!-- 페이징 -->
<div class="paging"><%=Pager.getList(request, cPageNo, cfg.getInt("limit.page"), objUser.pages, "", "cPageNo", "", buttons)%></div>
<!-- # 페이징 -->

<%@ include file="../../footer.inc.jsp" %>
<%!
	private String displayTime(int time) {
		if (time == 0) {
			return "-";
		}

		String h = StringEx.format(Math.abs(time / (60 * 60)), "0", 2);
		String m = StringEx.format(Math.abs((time % (60 * 60)) / 60), "0", 2);
		String s = StringEx.format(Math.abs((time % (60 * 60)) % 60), "0", 2);

		return h + ":" + m + ":" + s;
	}
%>