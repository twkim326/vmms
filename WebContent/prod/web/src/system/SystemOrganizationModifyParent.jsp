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
 * /system/SystemOrganizationModifyParent.jsp
 *
 * 시스템 > 조직관리 > 상위 조직 수정
 *
 * 작성일 - 2011/04/20, 정원광
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
		long organ = StringEx.str2long(request.getParameter("organ"), 0);
		String sDate = StringEx.setDefaultValue(request.getParameter("sDate"), "");

		if (organ == 0 || StringEx.isEmpty(sDate)) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("필수 입력정보가 존재하지 않습니다.", "UTF-8") + "\"}");
			return;
		}

		try {
			error = objOrgan.modify(seq, organ, sDate);
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode(error.replaceAll("'", ""), "UTF-8") + "\"}");
			return;
		}

		out.print("{\"code\" : \"SUCCESS\"}");
		return;
	} else {
		try {
			error = objOrgan.regist(seq);
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
		<a>* 수정 후 반드시 운영 자판기의 담당 계정을 확인 및 설정하세요.</a>
	</div>

	<table cellspacing="0" class="tableType03 tableType05" id="regist">
		<colgroup>
			<col width="110" />
			<col width="*"/>
		</colgroup>
		<tr>
			<th><span>수정 전 조직</span></th>
			<td><%=objOrgan.data.get("NAME")%></td>
		</tr>
		<tr>
			<th><span>수정 후 조직</span></th>
			<td>
				<select name="organ" id="organ" class="checkForm" option='{"isMust" : true, "message" : "수정 후 조직을 선택하세요."}'>
					<option value='{"seq" : 0, "name" : "", commission : "0"}'>- 선택하세요</option>
			<% for (int i = 0; i < objOrgan.organ.size(); i++) { GeneralConfig c = (GeneralConfig) objOrgan.organ.get(i); %>
					<option value='{"seq" : <%=c.getLong("SEQ")%>, "name" : "<%=c.get("NAME").replaceAll("'", "")%>", "commission" : "<%=c.get("COMMISSION_RATE")%>"}'<%=(c.getLong("SEQ") == objOrgan.data.getLong("PARENT_SEQ") ? " selected" : "")%>><%=c.get("NAME")%></option>
			<% } %>
				</select>
			</td>
		</tr>
		<tr>
			<th class="last"><span>변경 요청일</span></th>
			<td class="last">
				<select id="sy" class="checkForm" onchange="Common.changeDate($('sd'), this.options[selectedIndex].value, $('sm').value);" option='{"isMust" : true, "message" : "변경 요청일(년)을 선택하세요."}'>
					<option value="">- 년</option>
					<script language="javascript">Common.yyList('', 2007, <%=DateTime.date("yyyy")%>);</script>
				</select>
				<select id="sm" class="checkForm" onchange="Common.changeDate($('sd'), $('sy').value, this.options[selectedIndex].value);" option='{"isMust" : true, "message" : "변경 요청일(월)을 선택하세요."}'>
					<option value="">- 월</option>
					<script language="javascript">Common.mmList('');</script>
				</select>
				<select id="sd" class="checkForm" option='{"isMust" : true, "message" : "변경 요청일(일)을 선택하세요."}'>
					<option value="">- 일</option>
					<script language="javascript">Common.ddList('', '', '');</script>
				</select>
			</td>
		</tr>
	</table>

	<div class="buttonArea">
		<input type="button" value="" class="btnModifyS" onclick="_save();" />
		<input type="button" value="" class="btnCancelS" onclick="new parent.IFrame().close();" />
	</div>
</div>

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none; left:220px; top:50px;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script type="text/javascript">
	function _save() {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		}

		var error = Common.checkForm($('regist'));

		if (error != '') {
			window.alert(error);
			return;
		} else if (!confirm('입력하신 내용을 수정하시겠습니까?\n\n시간이 걸릴 수 있으니 잠시만 기다려 주세요.')) {
			return;
		}

		var o = $('organ').value.evalJSON();

		$('sbmsg').show();

		new Ajax.Request(location.pathname, {
			asynchronous : false,
			parameters : {
				seq : <%=seq%>
			,	organ : o.seq
			,	sDate : $('sy').value + $('sm').value + $('sd').value
			},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('수정이 완료되었습니다.');
						parent._organ(<%=objOrgan.data.getLong("COMPANY_SEQ")%>, o.seq, <%=objOrgan.data.getInt("DEPTH")%>);
						parent._select(o.seq, o.name, <%=objOrgan.data.getInt("DEPTH")%>, o.commission);
						new parent.IFrame().close();
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