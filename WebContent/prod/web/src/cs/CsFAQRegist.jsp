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
 * /cs/CsFAQRegist.jsp
 *
 * 고객센터 > FAQ > 등록/수정
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
	if (!(cfg.isAuth("I") || cfg.isAuth("U"))) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "A");

// 전송된 데이터
	long seq = StringEx.str2long(request.getParameter("seq"), 0);

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "cate:page:sField:sQuery");

// 인스턴스 생성
	FAQ objFAQ = new FAQ(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		String cate = StringEx.charset(request.getParameter("cate"));
		String title = StringEx.charset(request.getParameter("title"));
		String detail = StringEx.charset(request.getParameter("detail"));

		if (StringEx.isEmpty(title)) {
			out.print(Message.alert("필수 입력정보가 존재하지 않습니다.", 0, "parent", "try { parent._clear(); } catch (e) {}"));
			return;
		}

		try {
			error = objFAQ.regist(seq, cate, title, detail);
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null && StringEx.str2long(error) == 0) {
			out.print(Message.alert(error, 0, "parent", "try { parent._clear(); } catch (e) {}"));
			return;
		}

		out.print(Message.refresh("CsFAQDetail.jsp?seq=" + error + addParam, "입력하신 내용의 등록이 완료되었습니다.", "parent"));
		return;
	} else {
		try {
			error = objFAQ.regist(seq);
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null) {
			out.print(error);
			return;
		}
	}
%>
<%@ include file="../../header.inc.jsp" %>

<div class="title">
	<span>공지사항</span>
</div>

<form method="post" name="save_" id="save_" action="<%=request.getRequestURI()%>?<%=request.getQueryString()%>" onsubmit="return _save();" target="__save">
<table cellspacing="0" class="tableType03" id="regist">
	<colgroup>
		<col width="130" />
		<col width="*"/>
	</colgroup>
<% if (objFAQ.cate.size() > 0) { %>
	<tr>
		<th><span>카테고리</span></th>
		<td>
			<select id="cate" name="cate">
				<option value="">- 선택하세요</option>
	<% for (int i = 0; i < objFAQ.cate.size(); i++) { GeneralConfig c = (GeneralConfig) objFAQ.cate.get(i); %>
				<option value="<%=c.get("CODE")%>"<%=c.get("CODE").equals(objFAQ.data.get("CATEGORY")) ? " selected" : ""%>><%=c.get("NAME")%></option>
	<% } %>
			</select>
		</td>
	</tr>
<% } %>
	<tr>
		<th><span>제목</span></th>
		<td><input type="text" name="title" id="title" value="<%=Html.getText(objFAQ.data.get("TITLE"))%>" class="checkForm txtInput" style="width:592px" maxlength="100" option='{"isMust" : true, "message" : "제목을 입력하세요."}' /></td>
	</tr>
	<tr>
		<th class="last"><span>내용</span></th>
		<td class="last">
			<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/ckeditor/ckeditor.js"></script>
			<textarea name="detail" id="detail"><%=Html.getText(objFAQ.data.get("DETAIL"))%></textarea>
			<script type="text/javascript">CKEDITOR.replace('detail');</script>
		</td>
	</tr>
</table>

<div class="buttonArea">
	<input type="submit" value="" class="btnRegi" />
	<input type="button" value="" class="btnList" onclick="location.href = 'CsFAQ.jsp?1=1<%=addParam%>';" />
</div>
</form>

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<iframe src="about:blank" name="__save" id="__save" style="display:none"></iframe>
<script language="javascript">
	function _save() {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return false;
		}

		var error = Common.checkForm($('regist'));

		if (error != '') {
			window.alert(error);
			return false;
		} else if (!confirm('입력하신 내용을 등록하시겠습니까?')) {
			return false;
		}

		$('sbmsg').show();

		return true;
	}
	function _clear() {
		$('sbmsg').hide();
	}
</script>