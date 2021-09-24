<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Goods
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceGoodsModify.jsp
 *
 * 서비스 > 상품 > 수정
 *
 * 작성일 - 2011/04/02, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0205");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print("로그인이 필요합니다.");
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth("I")) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "B");

// 전송된 데이터
	long seq = StringEx.str2long(request.getParameter("seq"), 0);

	if (seq == 0) {
		out.print("상품 정보가 존재하지 않습니다.");
		return;
	}

// 인스턴스 생성
	Goods objGoods = new Goods(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		long company = StringEx.str2long(request.getParameter("company"), 0);
		long group = StringEx.str2long(request.getParameter("group"), 0);
		String code = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("code"), ""), "UTF-8");
		String name = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("name"), ""), "UTF-8");
		String barcode = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("barcode"), ""), "UTF-8");
		String memo = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("memo"), ""), "UTF-8");

		if (group == 0 || StringEx.isEmpty(code) || StringEx.isEmpty(name)) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("필수 입력값이 존재하지 않습니다.", "UTF-8") + "\"}");
			return;
		}

		try {
			error = objGoods.regist(seq, company, group, code, name, barcode, memo, false);
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
			error = objGoods.modify(seq);
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
		<span>상품 관리</span>
		<a>* 코드는 영문(대) 및 숫자만 가능합니다.</a>
	</div>

	<table cellspacing="0" class="tableType03 tableType05" id="regist">
		<colgroup>
			<col width="110" />
			<col width="*"/>
		</colgroup>
		<tr>
			<th><span>소속</span></th>
			<td>
				<select id="company" class="checkForm<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>" onchange="_group(this.options[selectedIndex].value, 0);"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
					<option value="0">- 선택하세요</option>
			<% for (int i = 0; i < objGoods.company.size(); i++) { GeneralConfig c = (GeneralConfig) objGoods.company.get(i); %>
					<option value="<%=c.getLong("SEQ")%>"<%=(c.getLong("SEQ") == objGoods.data.getLong("COMPANY_SEQ") ? " selected" : "")%>><%=c.get("NAME")%></option>
			<% } %>
				</select>
			</td>
		</tr>
		<tr>
			<th><span>그룹군</span></th>
			<td>
				<select id="maker" class="checkForm"  onchange="_group($('company').value, this.options[selectedIndex].value);" option='{"isMust" : true, "message" : "그룹군을 선택하세요."}'>
					<option value="0">- 선택하세요</option>
				</select>
			</td>
		</tr>
		<tr>
			<th><span>그룹</span></th>
			<td>
				<select id="group" class="checkForm">
					<option value="0">- 선택하세요</option>
				</select>
			</td>
		</tr>
		<tr>
			<th><span>코드</span></th>
			<td><input type="text" id="code" class="checkForm txtInput disabled" value="<%=Html.getText(objGoods.data.get("CODE"))%>" maxlength="20" readonly option='{"isMust" : true, "varType" : "ALNUM", "message" : "코드를 입력하세요."}' /></td>
		</tr>
		<tr>
			<th><span>상품명</span></th>
			<td><input type="text" id="name" class="checkForm txtInput" value="<%=Html.getText(objGoods.data.get("NAME"))%>" maxlength="100" style="width:370px" option='{"isMust" : true, "message" : "상품명을 입력하세요."}' /></td>
		</tr>
		<tr>
			<th><span>설명</span></th>
			<td><input type="text" id="memo" class="checkForm txtInput" value="<%=Html.getText(objGoods.data.get("MEMO"))%>" maxlength="200" style="width:370px" /></td>
		</tr>
		<tr>
			<th class="last"><span>바코드</span></th>
			<td class="last"><input type="text" id="barcode" value="<%=Html.getText(objGoods.data.get("BAR_CODE"))%>" class="checkForm txtInput" maxlength="20" /></td>
		</tr>
	</table>

	<div class="buttonArea">
		<input type="button" value="" class="btnModifyS" onclick="_save();" />
		<input type="button" value="" class="btnCancelS" onclick="new parent.IFrame().close();" />
	</div>
</div>

<%@ include file="../../footer.inc.jsp" %>

<script type="text/javascript">
	function _group(company, group, n) {
		var o = {maker : $('maker'), group : $('group')};

		for (var i = o.group.length - 1; i > 0; i--) {
			o.group.options[i] = null;
		}

		if (group == 0) {
			for (var i = o.maker.length - 1; i > 0; i--) {
				o.maker.options[i] = null;
			}

			o.maker.options[0].text = '- loading';
		} else {
			o.group.options[0].text = '- loading';
		}

		new Ajax.Request('ServiceGoodsGroup.jsp', {
			asynchronous : false,
			parameters : {company : company, group : group},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'FAIL') {
						window.alert(e.message ? decodeURIComponentEx(e.message) : '그룹 정보를 가져오는데 실패하였습니다.');
						return;
					}

					var s = o.group;

					if (group == 0) {
						s = o.maker;
					}

					e.data.each (function (data, i) {
						s.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);

						if (data.seq == n) {
							s.options[i + 1].selected = true;
						}
					}.bind(this));

					s.options[0].text = '- 선택하세요';
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}
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
				seq : <%=seq%>
			,	company : $('company').value
			,	group : $('group').selectedIndex > 0 ? $('group').options[$('group').selectedIndex].value : $('maker').options[$('maker').selectedIndex].value
			,	code : encodeURIComponentEx($('code').value)
			,	name : encodeURIComponentEx($('name').value)
			,	barcode : encodeURIComponentEx($('barcode').value)
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
<%
	if (objGoods.data.getLong("GROUP_SEQ") > 0) {
		if (objGoods.data.getInt("GROUP_DEPTH") == 0) {
			out.println("_group(" + objGoods.data.getLong("COMPANY_SEQ") + ", 0, " + objGoods.data.getLong("GROUP_SEQ") + ")");
			out.println("_group(" + objGoods.data.getLong("COMPANY_SEQ") + ", " + objGoods.data.getLong("GROUP_SEQ") + ", " + objGoods.data.getLong("GROUP_SEQ") + ")");
		} else {
			out.println("_group(" + objGoods.data.getLong("COMPANY_SEQ") + ", 0, " + objGoods.data.getLong("PARENT_SEQ") + ")");
			out.println("_group(" + objGoods.data.getLong("COMPANY_SEQ") + ", " + objGoods.data.getLong("PARENT_SEQ") + ", " + objGoods.data.getLong("GROUP_SEQ") + ")");
		}
	} else {
		out.println("_group(" + objGoods.data.getLong("COMPANY_SEQ") + ", 0)");
	}
%>
//	}.bind(this));
</script>