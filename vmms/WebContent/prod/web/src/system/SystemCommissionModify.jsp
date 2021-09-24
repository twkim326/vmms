<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Commission
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /system/SystemCommissionModify.jsp
 *
 * 시스템 > 수수료 > 수정
 *
 * 작성일 - 2011/06/11, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0108");

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
		out.print("수수료 정보가 존재하지 않습니다.");
		return;
	}

// 인스턴스 생성
	Commission objCommission = new Commission(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		long company = StringEx.str2long(request.getParameter("company"), 0);
		long organ = StringEx.str2long(request.getParameter("organ"), 0);
		String card = StringEx.setDefaultValue(request.getParameter("card"), "");
		float commission = StringEx.str2float(request.getParameter("commission"));
		String sDate = StringEx.setDefaultValue(request.getParameter("sDate"), "");
		String eDate = StringEx.setDefaultValue(request.getParameter("eDate"), "");
		String memo = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("memo"), ""), "UTF-8");

		if (company == 0 || organ == 0 || StringEx.isEmpty(card) || StringEx.isEmpty(sDate)) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("필수 입력정보가 존재하지 않습니다.", "UTF-8") + "\"}");
			return;
		}

		try {
			error = objCommission.modify(seq, company, organ, card, commission, sDate, eDate, memo);
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
			error = objCommission.modify(seq);
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
		<span>수수료 관리</span>
		<a>* 최하위 조직에만 수수료를 등록하세요.</a>
	</div>

	<table cellspacing="0" class="tableType03 tableType05" id="regist">
		<colgroup>
			<col width="110" />
			<col width="*"/>
		</colgroup>
		<tr>
			<th><span>소속</span></th>
			<td><%=objCommission.data.get("COMPANY")%></td>
		</tr>
		<tr>
			<th><span>조직</span></th>
			<td><%=organ(objCommission.data.get("ORGAN"))%></td>
		</tr>
		<tr>
			<th><span>매입사</span></th>
			<td><%=objCommission.data.get("PAY_CARD_NAME")%></td>
		</tr>
		<tr>
			<th><span>수수료율</span></th>
			<td><input type="text" id="commission" class="checkForm txtInput" style="width:60px;" maxlength="5" option='{"isMust" : true, "varType" : "float", "message" : "수수료율을 입력하세요."}' /> %</td>
		</tr>
		<tr>
			<th><span>시작일</span></th>
			<td>
				<select id="sy" class="checkForm" onchange="Common.changeDate($('sd'), this.options[selectedIndex].value, $('sm').value);" option='{"isMust" : true, "message" : "시작일(년)을 선택하세요."}'>
					<option value="">- 년</option>
					<script language="javascript">Common.yyList('<%=DateTime.getAddDay(1).substring(0, 4)%>', 2007, <%=DateTime.date("yyyy")%>);</script>
				</select>
				<select id="sm" class="checkForm" onchange="Common.changeDate($('sd'), $('sy').value, this.options[selectedIndex].value);" option='{"isMust" : true, "message" : "시작일(월)을 선택하세요."}'>
					<option value="">- 월</option>
					<script language="javascript">Common.mmList('<%=DateTime.getAddDay(1).substring(4, 6)%>');</script>
				</select>
				<select id="sd" class="checkForm" option='{"isMust" : true, "message" : "시작일(일)을 선택하세요."}'>
					<option value="">- 일</option>
					<script language="javascript">Common.ddList('<%=DateTime.getAddDay(1).substring(0, 4)%>', '<%=DateTime.getAddDay(1).substring(4, 6)%>', '<%=DateTime.getAddDay(1).substring(6, 8)%>');</script>
				</select>
			</td>
		</tr>
		<tr>
			<th class="last"><span>설명</span></th>
			<td class="last"><input type="text" id="memo" value="<%=Html.getText(objCommission.data.get("MEMO"))%>" class="txtInput" maxlength="200" style="width:370px" /></td>
		</tr>
	</table>

	<div class="buttonArea">
		<input type="button" value="" class="btnModifyS" onclick="_save();" />
		<input type="button" value="" class="btnCancelS" onclick="new parent.IFrame().close();" />
	</div>
</div>

<%@ include file="../../footer.inc.jsp" %>

<script type="text/javascript">
	function _save() {
		var error = Common.checkForm($('regist'));
		var organ = 0;

		if (error != '') {
			window.alert(error);
			return;
		}

		var sDate = $('sy').value + $('sm').value + $('sd').value;

		if (parseInt(sDate) < parseInt(Common.ymd())) {
			window.alert('시작일이 과거입니다.');
			return;
		} else if (!confirm('입력하신 내용을 등록하시겠습니까?')) {
			return;
		}

		new Ajax.Request(location.pathname, {
			asynchronous : true,
			parameters : {
				seq : <%=seq%>
			,	company : <%=objCommission.data.getLong("COMPANY_SEQ")%>
			,	organ : <%=objCommission.data.getLong("ORGANIZATION_SEQ")%>
			,	card : '<%=objCommission.data.get("PAY_CARD")%>'
			,	commission : $('commission').value
			,	sDate : sDate
			,	memo : encodeURIComponentEx($('memo').value)
			},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('등록이 완료되었습니다.');
						parent.location.reload();
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
<%!
	private String organ(String organ) {
		return StringEx.replace(StringEx.replace(Html.getText(StringEx.setDefaultValue(organ, "-")), "{", "<s>"), "}", "</s>");
	}
%>