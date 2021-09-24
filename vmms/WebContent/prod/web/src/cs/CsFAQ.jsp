<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.FAQ
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /cs/CsFAQ.jsp
 *
 * 고객센터 > FAQ > 목록
 *
 * 작성일 - 2011/04/03, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0502");

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
	String cate = StringEx.getKeyword(StringEx.charset(request.getParameter("cate")));
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));
	int pageNo = StringEx.str2int(request.getParameter("page"), 1);

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "cate:page:sField:sQuery");

// 인스턴스 생성
	FAQ objFAQ = new FAQ(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objFAQ.getList(cate, pageNo, sField, sQuery);
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
	<span>공지사항</span>
</div>

<form method="get" onsubmit="return _check(this);">
<table cellspacing="0" class="tableType02">
	<colgroup>
		<col width="110" />
		<col width="*"/>
		<col width="73" />
	</colgroup>
	<tr>
		<th><span>검색어</span></th>
		<td>
			<select id="sField" name="sField">
				<option value="">- 검색필드</option>
				<option value="A.TITLE"<%=sField.equals("A.TITLE") ? " selected" : ""%>>제목</option>
				<option value="A.DETAIL"<%=sField.equals("A.DETAIL") ? " selected" : ""%>>내용</option>
			</select>
			<input type="text" id="sQuery" name="sQuery" class="txtInput" value="<%=Html.getText(sQuery)%>" />
		</td>

		<td rowspan="2" class="center">
			<input type="submit" value="" class="btnSearch<%=(objFAQ.cate.size() == 0 ? "S" : "")%>" />
		</td>
	</tr>
<% if (objFAQ.cate.size() > 0) { %>
	<tr>
		<th><span>카테고리</span></th>
		<td>
			<select id="cate" name="cate">
				<option value="">- 선택하세요</option>
	<% for (int i = 0; i < objFAQ.cate.size(); i++) { GeneralConfig c = (GeneralConfig) objFAQ.cate.get(i); %>
				<option value="<%=c.get("CODE")%>"<%=c.get("CODE").equals(cate) ? " selected" : ""%>><%=c.get("NAME")%></option>
	<% } %>
			</select>
			<input type="text" id="sQuery" class="txtInput" value="<%=Html.getText(sQuery)%>" />
		</td>
	</tr>
<% } %>
</table>
</form>

<!-- 검색결과 -->
<div class="infoBar mt23">
	검색 결과 : <strong><%=StringEx.comma(objFAQ.records)%></strong>건
<% if (cfg.isAuth("I")) { %>
	<input type="button" class="button1" value="등록" onclick="location.href = 'CsFAQRegist.jsp?1=1<%=addParam%>';" />
<% } %>
</div>
<table cellpadding="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="50" />
		<col width="*" />
		<col width="80" />
		<col width="60" />
	</colgroup>
	<thead>
		<tr>
			<th nowrap>순번</th>
			<th nowrap>제목</th>
			<th nowrap>날짜</th>
			<th nowrap>조회</th>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objFAQ.list.size(); i++) { GeneralConfig c = (GeneralConfig) objFAQ.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>" onclick="location.href = 'CsFAQDetail.jsp?seq=<%=c.getLong("SEQ") + addParam%>';" style="cursor:pointer;">
			<td class="center number"><%=c.getLong("NO")%></td>
			<td><%=Html.getText(c.get("TITLE"))%></td>
			<td class="center number"><%=Html.getText(c.get("CREATE_DATE"))%></td>
			<td class="center number"><%=c.getInt("READS")%></td>
		</tr>
<% } %>
<% if (objFAQ.list.size() == 0) { %>
		<tr>
			<td colspan="4" align="center">등록된 내용이 없습니다</td>
		</tr>
<% } %>
	</tbody>
</table>
<!-- # 검색결과 -->

<!-- 페이징 -->
<div class="paging"><%=Pager.getList(request, pageNo, cfg.getInt("limit.page"), objFAQ.pages, "", "page", "", buttons)%></div>
<!-- # 페이징 -->

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script type="text/javascript">
	function _check(o) {
		if (o.sQuery.value != '' && o.sField.selectedIndex == 0) {
			window.alert('검색 필드를 선택하세요.');
			return false;
		} else if (o.sQuery.value == '' && o.sField.selectedIndex > 0) {
			window.alert('검색어를 입력하세요.');
			return false;
		}

		return true;
	}
</script>