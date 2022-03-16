<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.Notice
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /cs/CsNoticeDetail.jsp
 *
 * 고객센터 > 공지사항 > 조회
 *
 * 작성일 - 2011/04/03, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0501");

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
	long seq = StringEx.str2long(request.getParameter("seq"), 0);
	String cate = StringEx.getKeyword(StringEx.charset(request.getParameter("cate")));
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));

	if (seq == 0) {
		out.print("게시물 정보가 존재하지 않습니다.");
		return;
	}

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "cate:page:sField:sQuery");

// 인스턴스 생성
	Notice objNotice = new Notice(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objNotice.detail(seq, cate, sField, sQuery);
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
<style type="text/css">@import url("/prod/common/module/button_style.css")</style>
<div class="title">
	<span>공지사항</span>
</div>

<table cellspacing="0" class="tableType03">
	<colgroup>
		<col width="130" />
		<col width="*"/>
	</colgroup>
	<tr>
		<th><span>제목</span></th>
		<td><strong><%=(StringEx.isEmpty(objNotice.data.get("CATEGORY")) ? "" : "[" + Html.getText(objNotice.data.get("CATEGORY")) + "] ")%><%=Html.getText(objNotice.data.get("TITLE"))%></strong></td>
	</tr>
	<tr>
		<th><span>조회</span></th>
		<td><%=objNotice.data.getLong("READS")%></td>
	</tr>
	<tr>
		<th><span>날짜</span></th>
		<td><%=objNotice.data.get("CREATE_DATE")%></td>
	</tr>
	<% if (seq==18) { %>
	<tr>
		<th><span>내용</span></th>
		<td><%=objNotice.data.get("DETAIL")%></td>
	</tr>
	
	<tr>
		<th class="last"><span>첨부파일</span></th>
		<td class="last">
			<a download="" href="https://vmms.ubcn.co.kr/download/Mch_change_form.zip" data-filesize="0.363" class="download-button" style="padding: 10px 10px 10px 54px; margin-top: 0px;">명의이전 신청양식.zip</a>
			<a download="" href="https://vmms.ubcn.co.kr/download/koces_form.pdf" data-filesize="7" class="download-button" style="padding: 10px 10px 10px 54px; margin-top: 0px;">코세스 가맹서류신청서.pdf</a>
			<!--<a download="" href="https://vmms.ubcn.co.kr/download/UVM-300A(Guide).docx" data-filesize="3.47" class="download-button" style="padding: 10px 10px 10px 54px; margin-top: 0px;">UVM-300A 가이드.docx</a>-->
		</td>
	</tr>
	<%}else{ %>
	<tr>
		<th class="last"><span>내용</span></th>
		<td class="last"><%=objNotice.data.get("DETAIL")%></td>
	</tr>
	<%}%>

</table>

<div class="buttonArea" style="text-align:right; position:relative; margin-top:10px;">
	<input type="button" value="목록" class="button1" onclick="location.href = 'CsNotice.jsp?1=1<%=addParam%>';" style="position:absolute; left:0; top:0; " />
	&nbsp;
<% if (cfg.isAuth("D")) { %>
	<input type="button" value="삭제" class="button1" onclick="_delete(<%=seq%>);" />
<% } %>
<% if (cfg.isAuth("U")) { %>
	<input type="button" value="수정" class="button1" onclick="location.href = 'CsNoticeRegist.jsp?seq=<%=seq + addParam%>';" />
<% } %>
</div>

<% if (objNotice.data.getLong("PREV.SEQ") > 0 || objNotice.data.getLong("NEXT.SEQ") > 0) { %>
<table cellspacing="0" class="tableType03" style="margin-top:15px;">
	<colgroup>
		<col width="130" />
		<col width="*"/>
	</colgroup>
	<% if (objNotice.data.getLong("NEXT.SEQ") > 0) { %>
	<tr onclick="location.href = '?seq=<%=objNotice.data.getLong("NEXT.SEQ") + addParam%>';" style="cursor:pointer;">
		<th class="<%=(objNotice.data.getLong("PREV.SEQ") > 0 ? "" : "last")%>"><span>다음글</span></th>
		<td class="<%=(objNotice.data.getLong("PREV.SEQ") > 0 ? "" : "last")%>" colspan="3"><%=Html.getText(objNotice.data.get("NEXT.TITLE"))%></td>
	</tr>
	<% } %>
	<% if (objNotice.data.getLong("PREV.SEQ") > 0) { %>
	<tr onclick="location.href = '?seq=<%=objNotice.data.getLong("PREV.SEQ") + addParam%>';" style="cursor:pointer;">
		<th class="last"><span>이전글</span></th>
		<td class="last" colspan="3"><%=Html.getText(objNotice.data.get("PREV.TITLE"))%></td>
	</tr>
	<% } %>
</table>
<% } %>

<%@ include file="../../footer.inc.jsp" %>

<script type="text/javascript">
	function _delete(seq) {
		if (!confirm('해당 내용을 삭제하시겠습니까?')) {
			return;
		}

		new Ajax.Request('CsNoticeDelete.jsp', {
			asynchronous : false,
			parameters : {seq : seq},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('삭제가 완료되었습니다.');
						location.replace('CsNotice.jsp?1=1<%=addParam%>');
					} else {
						window.alert(decodeURIComponentEx(e.message));
					}
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}
			}.bind(this)
		});
	}
</script>