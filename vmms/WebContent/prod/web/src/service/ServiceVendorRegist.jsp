<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Vendor
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceVendorRegist.jsp
 *
 * 서비스 > 상품 공급자 관리 > 등록
 *
 * 작성일 - 2017/04/28, 황재원
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0206");

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
	Vendor objVendor = new Vendor(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		String name = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("name"), ""), "UTF-8");
		String memo = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("memo"), ""), "UTF-8");

		if (StringEx.isEmpty(name)) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("필수 입력정보가 존재하지 않습니다.", "UTF-8") + "\"}");
			return;
		}

		try {
			error = objVendor.regist(0, name, memo);
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
			error = objVendor.regist(0);
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
		<span>상품 공급자 관리</span>
	</div>

	<table cellspacing="0" class="tableType03 tableType05" id="regist">
		<colgroup>
			<col width="110" />
			<col width="*"/>
		</colgroup>
		<tr>
			<th><span>공급자명</span></th>
			<td><input type="text" id="name" class="checkForm txtInput" maxlength="20" option='{"isMust" : true, "message" : "상품 공급자를 입력하세요."}' /></td>
		</tr>
		<tr>
			<th><span>설명</span></th>
			<td><input type="text" id="memo" class="checkForm txtInput" maxlength="200" style="width:370px" /></td>
		</tr>
	</table>

	<div class="buttonArea">
		<input type="button" value="" class="btnRegiS" onclick="_save();" />
		<input type="button" value="" class="btnCancelS" onclick="new parent.IFrame().close();" />
	</div>
</div>

<%@ include file="../../footer.inc.jsp" %>

<script type="text/javascript">
	function _save() {
		var error = Common.checkForm($('regist'));

		if (error != '') {
			window.alert(error);
			return;
		} else if (!confirm('입력하신 내용을 등록하시겠습니까?')) {
			return;
		}


		new Ajax.Request(location.pathname, {
			asynchronous : false,
			parameters : {
				name : encodeURIComponentEx($('name').value)
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