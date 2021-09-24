<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Group
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceGroupModify.jsp
 *
 * 서비스 > 그룹 > 수정
 *
 * 작성일 - 2011/03/30, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0204");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print("로그인이 필요합니다.");
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
		out.print("그룹 정보가 존재하지 않습니다.");
		return;
	}

// 인스턴스 생성
	Group objGroup = new Group(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		long company = StringEx.str2long(request.getParameter("company"), 0);
		String cate = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("cate"), ""), "UTF-8");
		String code = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("code"), ""), "UTF-8");
		String name = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("name"), ""), "UTF-8");
		String memo = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("memo"), ""), "UTF-8");

		if (StringEx.isEmpty(name)) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("필수 입력값이 존재하지 않습니다.", "UTF-8") + "\"}");
			return;
		}

		try {
			if (StringEx.isEmpty(cate)) {
				error = objGroup.modify(seq, company, name, memo);
			} else {
				error = objGroup.regist(cate, code, name, memo, false);
			}
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
			error = objGroup.detail(seq);
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
		<span>상품 그룹 관리</span>
		<a>* 코드는 영문(대) 및 숫자만 가능합니다.</a>
	</div>

	<table cellspacing="0" class="tableType03 tableType<%=(objGroup.data.getInt("DEPTH") == 0 ? "07" : "05")%>" id="regist">
		<colgroup>
			<col width="110" />
			<col width="*"/>
		</colgroup>
		<tr>
			<th><span>소속</span></th>
			<td>
				<select id="company" class="checkForm<%=(cfg.getLong("user.company") > 0 || objGroup.data.getInt("DEPTH") > 0 ? " disabled" : "")%>" onchange="_cate();"<%=(cfg.getLong("user.company") > 0 || objGroup.data.getInt("DEPTH") > 0 ? " disabled" : "")%>>
					<option value="0">- 선택하세요</option>
			<% for (int i = 0; i < objGroup.company.size(); i++) { GeneralConfig c = (GeneralConfig) objGroup.company.get(i); %>
					<option value="<%=c.getLong("SEQ")%>"<%=(c.getLong("SEQ") == objGroup.data.getLong("COMPANY_SEQ") ? " selected" : "")%>><%=c.get("NAME")%></option>
			<% } %>
				</select>
			</td>
		</tr>
<% if (objGroup.data.getInt("DEPTH") > 0) { %>
		<tr>
			<th><span>그룹군</span></th>
			<td>
				<select id="cate" class="disabled" disabled option='{"isMust" : true, "message" : "그룹군을 선택하세요."}'>
					<option value='{"seq" : 0, "code" : ""}'>- 선택하세요</option>
				</select>
				<input type="button" class="button1" value="등록" onclick="_regist();" />
				<input type="button" class="button1" value="수정" onclick="_modify();" />
				<input type="button" class="button1" value="삭제" onclick="_delete();" />
			</td>
		</tr>
		<tr>
			<th><span>코드</span></th>
			<td><input type="text" id="code" value="<%=Html.getText(objGroup.data.get("CODE"))%>" class="checkForm txtInput disabled" maxlength="20" readonly option='{"isMust" : true, "varType" : "ALNUM", "message" : "코드를 입력하세요."}' /></td>
		</tr>
<% } %>
		<tr>
			<th><span><%=(objGroup.data.getInt("DEPTH") > 0 ? "그룹명" : "그룹군")%></span></th>
			<td><input type="text" id="name" value="<%=Html.getText(objGroup.data.get("NAME"))%>" class="checkForm txtInput" maxlength="20" option='{"isMust" : true, "message" : "<%=(objGroup.data.getInt("DEPTH") > 0 ? "그룹명" : "그룹군")%>을 입력하세요."}' /></td>
		</tr>
		<tr>
			<th class="last"><span>설명</span></th>
			<td class="last"><input type="text" id="memo" value="<%=Html.getText(objGroup.data.get("MEMO"))%>" class="checkForm txtInput" maxlength="200" style="width:370px" /></td>
		</tr>
	</table>

	<div class="buttonArea">
		<input type="button" value="" class="btnModifyS" onclick="_save();" />
		<input type="button" value="" class="btnCancelS" onclick="new parent.IFrame().close();" />
	</div>
</div>

<%@ include file="../../footer.inc.jsp" %>

<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/layer/js/iframe.js"></script>
<script type="text/javascript">
	function _regist() {
		new IFrame(460, 170, 'ServiceGroupRegist.jsp?depth=0&company=' + $('company').options[$('company').selectedIndex].value).open();
	}

	function _modify() {
		if ($('cate').selectedIndex == 0) {
			window.alert('수정할 그룹군을 선택하세요.');
			return;
		}

		new IFrame(460, 170, 'ServiceGroupModify.jsp?seq=' + $('cate').value.evalJSON().seq).open();
	}

	function _delete() {
		if ($('cate').selectedIndex == 0) {
			window.alert('삭제할 그룹군을 선택하세요.');
			return;
		} else if (!confirm('선택하신 그룹군을 삭제하시겠습니까?')) {
			return;
		}

		new Ajax.Request('ServiceGroupDelete.jsp', {
			asynchronous : false,
			parameters : {seq : $('cate').value.evalJSON().seq},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('삭제가 완료되었습니다.');
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

	function _cate(n) {
		var o = {company : $('company'), cate : $('cate')};

		if (!o.company || !o.cate) {
			return;
		}

		for (var i = o.cate.options.length - 1; i > 0; i--) {
			o.cate.options[i] = null;
		}

		o.cate.options[0].text = '- loading';

		new Ajax.Request('ServiceGroupTop.jsp', {
			asynchronous : true,
			parameters : {company : o.company.options[o.company.selectedIndex].value},
			onSuccess : function (xmlHttp) {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == 'FAIL') {
					window.alert(e.message ? decodeURIComponentEx(e.message) : '그룹군을 가져오는데 실패하였습니다.');
					return;
				}

				e.data.each (function (data, i) {
					o.cate.options[i + 1] = new Option(decodeURIComponentEx(data.name), '{"seq" : ' + data.seq + ', "code" : "' + data.code + '"}');

					if (data.seq == n) {
						o.cate.options[i + 1].selected = true;
					}
				}.bind(this));

				o.cate.options[0].text = '- 선택하세요';
			}.bind(this)
		});
	}

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
				seq : '<%=seq%>'
			,	company : $('company') ? $('company').value : ''
			,	cate : $('cate') ? encodeURIComponentEx($('cate').value.evalJSON().code) : ''
			,	code : $('code') ? encodeURIComponentEx($('code').value) : ''
			,	name : encodeURIComponentEx($('name').value)
			,	memo : encodeURIComponentEx($('memo').value)
			},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('수정이 완료되었습니다.');
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

//	Event.observe(window, 'load', function (event) {
		_cate(<%=objGroup.data.getLong("PARENT_SEQ")%>);
//	}.bind(this));
</script>