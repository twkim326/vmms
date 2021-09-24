<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.Goods
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceGoods.jsp
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
	long group = StringEx.str2long(request.getParameter("group"), 0);
	int pageNo = StringEx.str2int(request.getParameter("page"), 1);
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "company:group:pageNo:sField:sQuery");

// 인스턴스 생성
	Goods objGoods = new Goods(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objGoods.getList(company, group, pageNo, sField, sQuery);
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
			<select id="company" onchange="_group(this.options[selectedIndex].value, 0);"<%=(cfg.getLong("user.company") > 0 ? " class='disabled' disabled" : "")%>>
				<option value="0">- 소속</option>
		<% for (int i = 0; i < objGoods.company.size(); i++) { GeneralConfig c = (GeneralConfig) objGoods.company.get(i); %>
				<option value="<%=c.getLong("SEQ")%>"<%=((c.getLong("SEQ") == company || c.getLong("SEQ") == cfg.getLong("user.company")) ? " selected" : "")%>><%=c.get("NAME")%></option>
		<% } %>
			</select>
			<select id="maker" onchange="_group($('company').value, this.options[selectedIndex].value);">
				<option value="0">- 그룹군</option>
			</select>
			<select id="group">
				<option value="0">- 그룹</option>
			</select>
		</td>
	</tr>
</table>

<!-- 검색결과 -->
<div class="infoBar mt23">
	검색 결과 : <strong><%=StringEx.comma(objGoods.records)%></strong>건
<% if (cfg.isAuth("I")) { %>
	<input type="button" class="button2" value="개별등록" onclick="_regist();" />
	<input type="button" class="button2" value="일괄등록" onclick="_bundle();" style="right:80px;" />
<% } %>
</div>
<table cellpadding="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="50" />
		<col width="100" />
		<col width="100" />
		<col width="100" />
		<col width="70" />
		<col width="*" />
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
			<th nowrap>그룹군</th>
			<th nowrap>그룹</th>
			<th nowrap>코드</th>
			<th nowrap>상품명</th>
			<th nowrap>등록일</th>
			<th nowrap>수정일</th>
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
			<th nowrap>관리</th>
	<% } %>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objGoods.list.size(); i++) { GeneralConfig c = (GeneralConfig) objGoods.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>" title="<%=Html.getText(c.get("MEMO"))%>">
			<td class="center number"><%=c.getLong("NO")%></td>
			<td class="center"><%=Html.getText(StringEx.setDefaultValue(c.get("COMPANY"), "-"))%></td>
			<td class="center"><%=Html.getText(StringEx.setDefaultValue(c.get("GROUP_0"), "-"))%></td>
			<td class="center"><%=Html.getText(StringEx.setDefaultValue(c.get("GROUP_1"), "-"))%></td>
			<td class="center number"><%=Html.getText(c.get("CODE"))%></td>
			<td><%=Html.getText(c.get("NAME"))%></td>
			<td class="center number"><%=Html.getText(c.get("CREATE_DATE"))%></td>
			<td class="center number"><%=Html.getText(StringEx.setDefaultValue(c.get("MODIFY_DATE"), "-"))%></td>
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
			<td class="center">
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
<% if (objGoods.list.size() == 0) { %>
		<tr>
			<td colspan="<%=((cfg.isAuth("U") || cfg.isAuth("D")) ? "9" : "8")%>" align="center">등록된 상품이 없습니다</td>
		</tr>
<% } %>
	</tbody>
</table>
<!-- # 검색결과 -->

<!-- 페이징 -->
<div class="paging"><%=Pager.getList(request, pageNo, cfg.getInt("limit.page"), objGoods.pages, "", "page", "", buttons)%></div>
<!-- # 페이징 -->

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<form method="get" name="search" id="search">
<input type="hidden" name="company" value="" />
<input type="hidden" name="group" value="" />
<input type="hidden" name="sField" value="" />
<input type="hidden" name="sQuery" value="" />
</form>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/layer/js/iframe.js"></script>
<script type="text/javascript">
	function _search(o) {
		var obj = {
					company : $('company')
				,	maker : $('maker')
				,	group : $('group')
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
		o.group.value = obj.group.selectedIndex > 0 ? obj.group.options[obj.group.selectedIndex].value : obj.maker.options[obj.maker.selectedIndex].value;
		o.sField.value = (obj.sQuery.value == '' ? '' : obj.sField.options[obj.sField.selectedIndex].value);
		o.sQuery.value = obj.sQuery.value;
		o.submit();
	}

	function _group(company, group, n) {
		var o = {maker : $('maker'), group : $('group')};

		for (var i = o.group.length - 1; i > 0; i--) {
			o.group.options[i] = null;
		}

		if (group == 0) {
			for (var i = o.maker.length - 1; i > 0; i--) {
				o.maker.options[i] = null;
			}
		}

		$('sbmsg').show();

		new Ajax.Request('ServiceGoodsGroup.jsp', {
			asynchronous : true,
			parameters : {company : company, group : group},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'FAIL') {
						window.alert(e.message ? decodeURIComponentEx(e.message) : '그룹 정보를 가져오는데 실패하였습니다.');
						$('sbmsg').hide();
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
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}

				$('sbmsg').hide();
			}.bind(this)
		});
	}

	function _bundle() {
		new IFrame(510, 225, 'ServiceGoodsRegistBundle.jsp').open();
	}

	function _regist() {
		new IFrame(510, 300, 'ServiceGoodsRegist.jsp').open();
	}

	function _modify(n) {
		new IFrame(510, 300, 'ServiceGoodsModify.jsp?seq=' + n).open();
	}

	function _delete(n) {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		} else if (!confirm('선택하신 상품을 삭제하시겠습니까?')) {
			return;
		}

		$('sbmsg').show();

		new Ajax.Request('ServiceGoodsDelete.jsp', {
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
	if (group > 0) {
		if (objGoods.data.getInt("DEPTH") == 0) {
			out.println("_group(" + company + ", 0, " + group + ")");
			out.println("_group(" + company + ", " + group + ", " + group + ")");
		} else {
			out.println("_group(" + company + ", 0, " + objGoods.data.getLong("PARENT_SEQ") + ")");
			out.println("_group(" + company + ", " + objGoods.data.getLong("PARENT_SEQ") + ", " + group + ")");
		}
	} else {
		out.println("_group(" + company + ", 0)");
	}
%>
//	}.bind(this));
</script>