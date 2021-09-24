<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Organization
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /system/SystemOrganizationCommission.jsp
 *
 * 시스템 > 조직관리 > 수수료 등록
 *
 * 작성일 - 2011/05/25, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0105");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(Message.reload("top"));
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth("U")) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "B");

// 전송된 데이터
	long seq = StringEx.str2long(request.getParameter("seq"), 0);

	if (seq == 0) {
		out.print("필수 입력정보가 존재하지 않습니다.");
		return;
	}

// 인스턴스 생성
	Organization objOrgan = new Organization(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		int count = StringEx.str2int(request.getParameter("count"), 0);

		try {
			error = objOrgan.commission(request, seq, count);
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null) {
			out.print(Message.alert(error, 0, "parent", "try { parent._clear(); } catch (e) {}"));
			return;
		}

		out.print(Message.alert("입력하신 내용의 등록이 완료되었습니다.", 0, "parent", "try { new top.IFrame().close(); } catch (e) {}"));
		return;
	} else {
		try {
			error = objOrgan.commission(seq);
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

<div id="window">
	<div class="title">
		<span>조직관리</span>
	</div>

	<form method="post" name="save_" id="save_" onsubmit="return _save();" target="__save">
	<input type="hidden" name="seq" id="seq" value="<%=seq%>" />
	<input type="hidden" name="count" id="count" value="<%=objOrgan.company.size()%>" />
	<table cellspacing="0" class="tableType01 tableType08" id="regist">
		<colgroup>
			<col width="150" />
			<col width="*"/>
		</colgroup>
		<thead>
			<tr>
				<th nowrap>카드사</th>
				<th nowrap>수수료율 (%)</th>
			</tr>
		</thead>
		<tbody>
<% for (int i = 0; i < objOrgan.company.size(); i++) { GeneralConfig c = (GeneralConfig) objOrgan.company.get(i); %>
			<input type="hidden" name="card<%=(i + 1)%>" id="card<%=(i + 1)%>" value="<%=c.get("CODE")%>" />
			<input type="hidden" name="corg<%=(i + 1)%>" id="corg<%=(i + 1)%>" value="<%=c.get("COMMISSION_RATE")%>" />
			<tr>
				<td class="center"><%=Html.getText(c.get("NAME"))%></td>
				<td class="center" style="padding:5px;"><input type="text" name="commission<%=(i + 1)%>" id="commission<%=(i + 1)%>" value="<%=c.get("COMMISSION_RATE")%>" class="checkForm txtInput" maxlength="5" style="width:130px;" option='{"isMust" : true, "message" : "<%=Html.getText(c.get("NAME"))%>의 수수료율을 입력하세요."}' /></td>
			</tr>
<% } %>
<% if (objOrgan.company.size() == 0) { %>
			<tr>
				<td colspan="2" class="center">선불 매입사를 등록하세요</td>
			</tr>
<% } %>
		</tbody>
	</table>

	<div class="buttonArea">
		<input type="submit" value="" class="btnRegiS" />
		<input type="button" value="" class="btnCancelS" onclick="new parent.IFrame().close();" />
	</div>
	</form>
</div>

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none; left:120px; top:65px;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<iframe src="about:blank" name="__save" id="__save" style="display:none"></iframe>
<script type="text/javascript">
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