<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Company
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /system/SystemCompanyRegist.jsp
 *
 * 시스템 > 소속 > 등록
 *
 * 작성일 - 2011/03/25, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0104");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(Message.reload("top"));
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth("I")) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "B");

// 인스턴스 생성
	Company objCompany = new Company(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		String name = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("name"), ""), "UTF-8");
		String memo = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("memo"), ""), "UTF-8");
		String isUsedClosing = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("isUsedClosing"), ""), "UTF-8");
		String isViewClosing = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("isViewClosing"), ""), "UTF-8");
		String isRawSalescount = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("isRawSalescount"), ""), "UTF-8");
		String isSMSEnabled = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("isSMSEnabled"), ""), "UTF-8");
		//2020-12-03 김태우 추가
		String isKAKAOEnabled = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("isKAKAOEnabled"), ""), "UTF-8");
		long kAKAOMaxCount= StringEx.str2long(request.getParameter("kAKAOMaxCount"), 0);
		String group = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("group"), ""), "UTF-8");

		if (StringEx.isEmpty(name)) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("필수 입력정보가 존재하지 않습니다.", "UTF-8") + "\"}");
			return;
		}

		try {
			//error = objCompany.regist(0, name, memo, isUsedClosing, isViewClosing, StringEx.split(StringEx.arrange(group, ",", ","), ","));
			//error = objCompany.regist(0, name, memo, isUsedClosing, isViewClosing, isRawSalescount, isSMSEnabled, StringEx.split(StringEx.arrange(group, ",", ","), ","));
			/*2020-12-03 김태우 추가*/
			error = objCompany.regist(0, name, memo, isUsedClosing, isViewClosing, isRawSalescount, isSMSEnabled, isKAKAOEnabled, kAKAOMaxCount, StringEx.split(StringEx.arrange(group, ",", ","), ","));
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
			error = objCompany.regist(0);
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
		<span>소속관리</span>
	</div>

	<table cellspacing="0" class="tableType03 tableType05" id="regist">
		<colgroup>
			<col width="110" />
			<col width="*"/>
		</colgroup>
		<tr>
			<th><span>소속</span></th>
			<td><input type="text" id="name" class="checkForm txtInput" maxlength="20" option='{"isMust" : true, "message" : "소속을 입력하세요."}' /></td>
		</tr>
		<tr>
			<th><span>설명</span></th>
			<td><input type="text" id="memo" class="checkForm txtInput" maxlength="200" style="width:370px" /></td>
		</tr>
		<tr>
			<th><span>마감 처리</span></th>
			<td>
				<select id="isUsedClosing">
					<option value="N">처리안함</option>
					<option value="Y">처리함</option>
				</select>
			</td>
		</tr>
		<tr>
			<th><span>마감 출력</span></th>
			<td>
				<select id="isViewClosing">
					<option value="N">출력안함</option>
					<option value="Y">출력함</option>
				</select>
			</td>
		</tr>
		<tr>
			<th><span>판매계수</span></th>
			<td>
				<select id="isRawSalescount">
					<option value="N">보정함</option>
					<option value="Y">보정안함</option>
				</select>
			</td>
		</tr>
		<tr>
			<th><span>SMS발송</span></th>
			<td>
				<select id="isSMSEnabled">
					<option value="N">발송안함</option>
					<option value="Y">발송함</option>
				</select>
			</td>
		</tr>
		<%--2020-12-03 김태우 추가--%>
		<tr>
			<th><span>알림톡발송</span></th>
			<td>
				<select id="isKAKAOEnabled" onchange="_change()">
					<option value="N">발송안함</option>
					<option value="Y">발송함</option>
				</select>
			</td>
		</tr>
		<tr>
			<th class="last"><span>제품군</span></th>
			<td class="last" id="groupList">
<%
	for (int i = 0; i < objCompany.group.size(); i++) {
		GeneralConfig c = (GeneralConfig) objCompany.group.get(i);

		if (i > 0 && i % 3 == 0) {
			out.println("<br />");
		}

		out.println("<input type='checkbox' id='group" + i + "' value='" + c.getLong("SEQ") + "' class='checkbox'" + (c.get("IS_SELECTED").equals("Y") ? " checked" : "") + " /><label for='group" + i + "'>" + c.get("NAME") + "</label>");
	}

	if (objCompany.group.size() == 0) {
		out.println("등록된 상품 그룹이 없습니다.");
	}
%>
			</td>
		</tr>
	</table>

	<div class="buttonArea">
		<input type="button" value="" class="btnRegiS" onclick="_save();" />
		<input type="button" value="" class="btnCancelS" onclick="new parent.IFrame().close();" />
	</div>
</div>

<%@ include file="../../footer.inc.jsp" %>

<script type="text/javascript">
	function _change(){
		var selectKAKAO=document.getElementById("isKAKAOEnabled");			//알림톡 발송
		var selectKAKAOYN=selectKAKAO.options[selectKAKAO.selectedIndex].value;


		if(selectKAKAOYN=="Y"){
			confirm("알림톡발송을 선택하신게 맞습니까? " +
					"\n※서비스 설정 시, 별도 과금이 부과됩니다.");
		}else if(selectKAKAOYN=="N"){
			alert("알림톡을 발송하지 않습니다.");
		}

	}
	function _save() {
		var selectKAKAO=document.getElementById("isKAKAOEnabled");			//알림톡 발송
		var selectKAKAOText=selectKAKAO.options[selectKAKAO.selectedIndex].text;
		var error = Common.checkForm($('regist'));

		if (error != '') {
			window.alert(error);
			return;
		} else if (!confirm('입력하신 내용을 수정하시겠습니까?'+ '\n알림톡발송 : '+selectKAKAOText)) {
			return;
		}

		var group = '';

		$('groupList').getElementsBySelector('input').findAll(function (o) {
			if (o.checked) {
				group += ',' + o.value;
			}
		}.bind(this));

		new Ajax.Request(location.pathname, {
			asynchronous : false,
			parameters : {
				name : encodeURIComponentEx($('name').value)
			,	memo : encodeURIComponentEx($('memo').value)
			,	isUsedClosing : encodeURIComponentEx($('isUsedClosing').value)
			,	isViewClosing : encodeURIComponentEx($('isViewClosing').value)
			,	isRawSalescount : encodeURIComponentEx($('isRawSalescount').value)
			,	isSMSEnabled : encodeURIComponentEx($('isSMSEnabled').value)
				//2020-12-03 김태우 추가
			//,	isKAKAOEnabled : encodeURIComponentEx($('isKAKAOEnabled').value)
			,	group : encodeURIComponentEx(group)
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