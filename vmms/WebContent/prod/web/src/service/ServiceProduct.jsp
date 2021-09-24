<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.Product
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceProduct.jsp
 *
 * 서비스 > 상품 > 목록
 *
 * 작성일 - 2011/03/31, 정원광
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
	long company = StringEx.str2long(request.getParameter("company"), 0);
	long vendor = StringEx.str2long(request.getParameter("vendor"), 0);
	long category = StringEx.str2long(request.getParameter("category"), 0);
	
	int pageNo = StringEx.str2int(request.getParameter("page"), 1);
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "company:category:pageNo:sField:sQuery");

// 인스턴스 생성
	Product objProduct = new Product(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objProduct.getList(company, vendor, category, pageNo, sField, sQuery);
	} catch (Exception e) {
		error = e.getMessage();
	}

// 에러 처리
	if (error != null) {
		out.print(error);
		return;
	}

// 페이지 버튼
	String[] buttons = {"<img src='" + cfg.get("imgDir") +"/web/btn_first.gif' alt='처음' />",
		"<img src='" + cfg.get("imgDir") +"/web/btn_pre.gif' alt='이전 " + cfg.getInt("limit.page") + "개' />",
		"<img src='" + cfg.get("imgDir") +"/web/btn_next.gif' alt='다음 " + cfg.getInt("limit.page") + "개' />",
		"<img src='" + cfg.get("imgDir") +"/web/btn_last.gif' alt='마지막' />"};
%>
<%@ include file="../../header.inc.jsp" %>

<div class="title">
	<span>상품 관리</span>
</div>

<table cellspacing="0" class="tableType02">
	<colgroup>
		<col width="110" />
		<col width="*"/>
		<col width="73" />
	</colgroup>
	<tr>
		<th><span>검색어</span></th>
		<td>
			<select id="sField">
				<option value="">- 검색필드</option>
				<option value="A.CODE"<%=sField.equals("A.CODE") ? " selected" : ""%>>코드</option>
				<option value="A.NAME"<%=sField.equals("A.NAME") ? " selected" : ""%>>상품명</option>
			</select>
			<input type="text" id="sQuery" class="txtInput" value="<%=Html.getText(sQuery)%>" />
		</td>
		<td rowspan="2" class="center last">
		  <input type="button" value="" class="btnSearch" onclick="_search($('search'));" />
		</td>
	</tr>
	<tr>
		<th rowspan="2"><span>소속/그룹</span></th>
		<td>
			<select id="company" onchange="_category(this.options[selectedIndex].value, 0);"<%=(cfg.getLong("user.company") > 0 ? " class='disabled' disabled" : "")%> class="js-example-basic-single js-example-responsive" style="width: 20%" >
				<option value="0">- 소속</option>
		<% for (int i = 0; i < objProduct.company.size(); i++) { GeneralConfig c = (GeneralConfig) objProduct.company.get(i); %>
				<option value="<%=c.getLong("SEQ")%>"<%=((c.getLong("SEQ") == company || c.getLong("SEQ") == cfg.getLong("user.company")) ? " selected" : "")%>><%=c.get("NAME")%></option>
		<% } %>
			</select>
			<select id="vendor" class="js-example-basic-single js-example-responsive" style="width: 15%">
				<option value="0">- 공급자</option>
		<% for (int i = 0; i < objProduct.vendor.size(); i++) { GeneralConfig c = (GeneralConfig) objProduct.vendor.get(i); %>
				<option value="<%=c.getLong("SEQ")%>"<%=(c.getLong("SEQ") == vendor ? " selected":"")%>><%=c.get("NAME")%></option>
		<% } %>
			</select>
			<select id="maker" onchange="_category($('company').value, this.options[selectedIndex].value);" class="js-example-basic-single js-example-responsive" style="width: 10%">
				<option value="0">- 그룹군</option>
			</select>
			<select id="category" class="js-example-basic-single js-example-responsive" style="width: 10%">
				<option value="0">- 그룹</option>
			</select>
		</td>
	</tr>
</table>

<!-- 검색결과 -->
<div class="infoBar mt23">
	검색 결과 : <strong><%=StringEx.comma(objProduct.records)%></strong>건
<% if (cfg.isAuth("I")) { %>
	<input type="button" class="button2" value="개별등록" onclick="_regist();" />
	<input type="button" class="button2" value="일괄등록" onclick="_bundle();" style="right:80px;" />
<% } %>
</div>
<table cellpadding="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="50" />
		<col width="*" />
		<col width="90" />
		<col width="90" />
		<col width="90" />
		<col width="150" />
		<col width="*" />
		<col width="80" />
		<col width="120" />
		<col width="80" />
		<col width="80" />
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
		<col width="<%=(cfg.isAuth("U") && cfg.isAuth("D") ? "120" : "60")%>" />
	<% } %>
	</colgroup>
	<thead>
		<tr>
			<th nowrap>순번</th>
			<th nowrap>소속</th>
			<th nowrap>공급자</th>
			<th nowrap>그룹군</th>
			<th nowrap>그룹</th>
			<th nowrap>코드</th>
			<th nowrap>상품명</th>
			<th nowrap>판매가</th>
			<th nowrap>바코드</th>
			<th nowrap>등록일</th>
			<th nowrap>수정일</th>
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
			<th nowrap>관리</th>
	<% } %>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objProduct.list.size(); i++) { GeneralConfig c = (GeneralConfig) objProduct.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>" title="<%=Html.getText(c.get("MEMO"))%>">
			<td class="center number"><%=c.getLong("NO")%></td>
			<td class="left" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("COMPANY"), "-"))%></td>
			<td class="center"><%=Html.getText(StringEx.setDefaultValue(c.get("VENDOR"), "-"))%></td>
			<td class="center"><%=Html.getText(StringEx.setDefaultValue(c.get("GROUP_0"), "-"))%></td>
			<td class="center"><%=Html.getText(StringEx.setDefaultValue(c.get("GROUP_1"), "-"))%></td>
			<td class="center number"><%=Html.getText(c.get("CODE"))%></td>
			<td nowrap><%=Html.getText(c.get("NAME"))%></td>
			<td class="right number" nowrap><%=StringEx.comma(c.getLong("PRICE"))%></td>
			<td class="center"><%=Html.getText(StringEx.setDefaultValue(c.get("BAR_CODE"), "&nbsp;"))%></td>
			<td class="center number" nowrap><%=Html.getText(c.get("CREATE_DATE"))%></td>
			<td class="center number" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("MODIFY_DATE"), "-"))%></td>
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
			<td class="center" nowrap>
		<% if ((cfg.getLong("user.company") > 0 && c.getLong("COMPANY_SEQ") == cfg.getLong("user.company")) || cfg.getLong("user.company") == 0) { %>
			<% if (cfg.isAuth("U")) { %>
				<input type="button" class="button1" value="수정" onclick="_modify(<%=c.getLong("SEQ")%>);" />
			<% } %>
			<% if (cfg.isAuth("D")) { %>
				<input type="button" class="button1" value="삭제" onclick="_delete(<%=c.getLong("SEQ")%>);" />
			<% } %>
		<% } else { %>
				-
		<% } %>
			</td>
	<% } %>
		</tr>
<% } %>
<% if (objProduct.list.size() == 0) { %>
		<tr>
			<td colspan="<%=((cfg.isAuth("U") || cfg.isAuth("D")) ? "12" : "11")%>" align="center">등록된 상품이 없습니다</td>
		</tr>
<% } %>
	</tbody>
</table>
<!-- # 검색결과 -->

<!-- 페이징 -->
<div class="paging"><%=Pager.getList(request, pageNo, cfg.getInt("limit.page"), objProduct.pages, "", "page", "", buttons)%></div>
<!-- # 페이징 -->

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<form method="get" name="search" id="search">
<input type="hidden" name="company" value="" />
<input type="hidden" name="vendor" value="" />
<input type="hidden" name="category" value="" />
<input type="hidden" name="sField" value="" />
<input type="hidden" name="sQuery" value="" />
</form>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/layer/js/iframe.js"></script>
<script type="text/javascript">
	function _search(o) {
		var obj = {
					company : $('company')
				,	vendor : $('vendor')
				,	maker : $('maker')
				,	category : $('category')
				,	sField : $('sField')
				,	sQuery : $('sQuery')
			};

		if (obj.sQuery.value != '' && obj.sField.selectedIndex == 0) {
			window.alert('검색 필드를 선택하세요.');
			return;
		} else if (obj.sQuery.value == '' && obj.sField.selectedIndex > 0) {
			window.alert('검색어를 입력하세요.');
			return;
		}

		o.company.value = obj.company.options[obj.company.selectedIndex].value;
		o.vendor.value = obj.vendor.options[obj.vendor.selectedIndex].value;
		o.category.value = obj.category.selectedIndex > 0 ? obj.category.options[obj.category.selectedIndex].value : obj.maker.options[obj.maker.selectedIndex].value;
		o.sField.value = (obj.sQuery.value == '' ? '' : obj.sField.options[obj.sField.selectedIndex].value);
		o.sQuery.value = obj.sQuery.value;
		o.submit();
	}

	function _category(company, category, n) {
		var o = {maker : $('maker'), category : $('category')};

		for (var i = o.category.length - 1; i > 0; i--) {
			o.category.options[i] = null;
		}

		if (category == 0) {
			for (var i = o.maker.length - 1; i > 0; i--) {
				o.maker.options[i] = null;
			}
		}

		$('sbmsg').show();

		new Ajax.Request('ServiceProductCategory.jsp', {
			asynchronous : true,
			parameters : {company : company, category : category},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'FAIL') {
						window.alert(e.message ? decodeURIComponentEx(e.message) : '그룹 정보를 가져오는데 실패하였습니다.');
						$('sbmsg').hide();
						return;
					}

					var s = o.category;

					if (category == 0) {
						s = o.maker;
					}

					e.data.each (function (data, i) {
						s.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);

						if (data.seq == n) {
							s.options[i + 1].selected = true;
						}
					}.bind(this));
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}

				$('sbmsg').hide();
			}.bind(this)
		});
	}

	function _bundle() {
		new IFrame(510, 200, 'ServiceProductRegistBundle.jsp').open();
	}

	function _regist() {
		new IFrame(510, 375, 'ServiceProductRegist.jsp').open();
	}

	function _modify(n) {
		new IFrame(510, 375, 'ServiceProductModify.jsp?seq=' + n).open();
	}

	function _delete(n) {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		} else if (!confirm('선택하신 상품을 삭제하시겠습니까?')) {
			return;
		}

		$('sbmsg').show();

		new Ajax.Request('ServiceProductDelete.jsp', {
			asynchronous : true,
			parameters : {seq : n},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('삭제가 완료되었습니다.');
						location.reload();
					} else {
						window.alert(decodeURIComponentEx(e.message));
					}
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}

				$('sbmsg').hide();
			}.bind(this)
		});
	}

//	Event.observe(window, 'load', function (event) {
<%
	if (category > 0) {
		if (objProduct.data.getInt("DEPTH") == 0) {
			out.println("_category(" + company + ", 0, " + category + ")");
			out.println("_category(" + company + ", " + category + ", " + category + ")");
		} else {
			out.println("_category(" + company + ", 0, " + objProduct.data.getLong("PARENT_SEQ") + ")");
			out.println("_category(" + company + ", " + objProduct.data.getLong("PARENT_SEQ") + ", " + category + ")");
		}
	} else {
		out.println("_category(" + company + ", 0)");
	}
%>
//	}.bind(this));
</script>