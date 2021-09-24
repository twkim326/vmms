<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Product
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceProductRegist.jsp
 *
 * 서비스 > 상품 > 등록
 *
 * 작성일 - 2017/05/19, 황재원
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

// 인스턴스 생성
	Product objProduct = new Product(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		long company = StringEx.str2long(request.getParameter("company"), 0);
		long vendor = StringEx.str2long(request.getParameter("vendor"), 0);
		long category = StringEx.str2long(request.getParameter("category"), 0);
		long price = StringEx.str2long(request.getParameter("price"), 0);
		
		String code = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("code"), ""), "UTF-8");
		String name = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("name"), ""), "UTF-8");
		String barcode = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("barcode"), ""), "UTF-8");
		String memo = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("memo"), ""), "UTF-8");
		boolean isAuto = StringEx.setDefaultValue(request.getParameter("isAuto"), "N").equals("Y");

		if (category == 0 || (!isAuto && StringEx.isEmpty(code)) || StringEx.isEmpty(name)) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("필수 입력값이 존재하지 않습니다.", "UTF-8") + "\"}");
			return;
		}

		try {
			error = objProduct.regist(0, company, vendor, category, code, name, price, barcode, memo, isAuto);
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
			error = objProduct.regist();
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
		<a>* 는 필수 입력항목입니다.</a>
	</div>

	<table cellspacing="0" class="tableType03 tableType05" id="regist">
		<colgroup>
			<col width="110" />
			<col width="*"/>
		</colgroup>
		<tr>
			<th><span>소속 *</span></th>
			<td>
				<select id="company" class="checkForm<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%> js-example-basic-single js-example-responsive" style="width: 40%" onchange="_category(this.options[selectedIndex].value, 0);"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%> option='{"isMust" : true, "message" : "소속을 선택하세요."}'>
					<option value="0">- 선택하세요</option>
			<% for (int i = 0; i < objProduct.company.size(); i++) { GeneralConfig c = (GeneralConfig) objProduct.company.get(i); %>
					<option value="<%=c.getLong("SEQ")%>"<%=(c.getLong("SEQ") == cfg.getLong("user.company") ? " selected" : "")%>><%=c.get("NAME")%></option>
			<% } %>
				</select>
			</td>
		</tr>
		<tr>
			<th><span>공급자 *</span></th>
			<td>
				<select id="vendor" class="checkForm"   option='{"isMust" : true, "message" : "공급자를 선택하세요."}'>
					<option value="0">- 선택하세요</option>
			<% for (int i = 0; i < objProduct.vendor.size(); i++) { GeneralConfig c = (GeneralConfig) objProduct.vendor.get(i); %>
					<option value="<%=c.getLong("SEQ")%>"><%=c.get("NAME")%></option>
			<% } %>					
				</select>
			</td>
		</tr>
		<tr>
			<th><span>그룹군 *</span></th>
			<td>
				<select id="maker" class="checkForm"  onchange="_category($('company').value, this.options[selectedIndex].value);" option='{"isMust" : true, "message" : "그룹군을 선택하세요."}'>
					<option value="0">- 선택하세요</option>
				</select>
			</td>
		</tr>
		<tr>
			<th><span>그룹</span></th>
			<td>
				<select id="category" class="checkForm">
					<option value="0">- 선택하세요</option>
				</select>
			</td>
		</tr>
		<tr>
			<th rowspan="2"><span>코드 *</span></th>
			<td><input type="text" id="code" class="checkForm txtInput" maxlength="20" option='{"isMust" : true, "varType" : "ALNUM", "message" : "코드를 입력하세요."}' /> <input type="checkbox" id="isAuto" class="checkbox" onclick="if (this.checked) { $('code').value = ''; $('code').disabled = true; $('code').addClassName('disabled'); } else { $('code').disabled = false; $('code').removeClassName('disabled'); }" /><label for="isAuto">자동생성</label></td>
		</tr>
		<tr>
			<td> 코드는 영문(대) 및 숫자만 입력가능 합니다.</td>
		</tr>
		<tr>
			<th><span>상품명 *</span></th>
			<td><input type="text" id="name" class="checkForm txtInput" maxlength="100" style="width:370px" option='{"isMust" : true, "message" : "상품명을 입력하세요."}' /></td>
		</tr>
		<tr>
			<th><span>단가</span></th>
			<td><input type="text" id="price" class="checkForm txtInput" maxlength="20" /></td>
		</tr>
		<tr>
			<th><span>설명</span></th>
			<td><input type="text" id="memo" class="checkForm txtInput" maxlength="200" style="width:370px" /></td>
		</tr>
		<tr>
			<th class="last"><span>바코드</span></th>
			<td class="last"><input type="text" id="barcode" class="checkForm txtInput" maxlength="20" /></td>
		</tr>
	</table>

	<div class="buttonArea">
		<input type="button" value="" class="btnRegiS" onclick="_save();" />
		<input type="button" value="" class="btnCancelS" onclick="new parent.IFrame().close();" />
	</div>
</div>

<%@ include file="../../footer.inc.jsp" %>

<script type="text/javascript">
	function _category(company, category) {
		var o = {company : $('company'), maker : $('maker'), category : $('category')};

		for (var i = o.category.length - 1; i > 0; i--) {
			o.category.options[i] = null;
		}

		if (category == 0) {
			for (var i = o.maker.length - 1; i > 0; i--) {
				o.maker.options[i] = null;
			}

			o.maker.options[0].text = '- loading';
		} else {
			o.category.options[0].text = '- loading';
		}

		new Ajax.Request('ServiceProductCategory.jsp', {
			asynchronous : false,
			parameters : {company : company, category : category},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'FAIL') {
						window.alert(e.message ? decodeURIComponentEx(e.message) : '그룹 정보를 가져오는데 실패하였습니다.');
						return;
					}

					var s = o.category;

					if (category == 0) {
						s = o.maker;
					}

					e.data.each (function (data, i) {
						s.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);
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
				company : $('company').value
			,   vendor : $('vendor').value
			,	category : $('category').selectedIndex > 0 ? $('category').options[$('category').selectedIndex].value : $('maker').options[$('maker').selectedIndex].value
			,	code : encodeURIComponentEx($('code').value)
			,	name : encodeURIComponentEx($('name').value)
			,   price : $('price').value
			,	barcode : encodeURIComponentEx($('barcode').value)
			,	memo : encodeURIComponentEx($('memo').value)
			,	isAuto : $('isAuto').checked ? "Y" : "N"
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

//	Event.observe(window, 'load', function (event) {
		_category(0, 0);
//	}.bind(this));
</script>