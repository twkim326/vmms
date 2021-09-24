<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.Vendor
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceVendor.jsp
 *
 * 서비스 > 공급자 > 목록
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
	int pageNo = StringEx.str2int(request.getParameter("page"), 1);
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "page:sField:sQuery");

// 인스턴스 생성
	Vendor objVendor = new Vendor(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objVendor.getList(pageNo, sField, sQuery);
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
	<span>상품 공급자 관리</span>
</div>

<form method="get" onsubmit="return _check(this);">
<table cellspacing="0" class="tableType02">
	<colgroup>
		<col width="110" />
		<col width="*"/>
		<col width="73" />
	</colgroup>
	<tr>
		<th><span>검색어</span></th>
		<td>
			<select name="sField" id="sField">
				<option value="">- 검색필드</option>
				<option value="A.NAME"<%=sField.equals("A.NAME") ? " selected" : ""%>>공급자명</option>
				<option value="A.MEMO"<%=sField.equals("A.MEMO") ? " selected" : ""%>>설명</option>
			</select>
			<input type="text" name="sQuery" id="sQuery" class="txtInput" value="<%=sQuery%>" />
		</td>
		<td class="center">
			<input type="submit" value="" class="btnSearchS" />
		</td>
	</tr>
</table>
</form>

<!-- 검색결과 -->
<div class="infoBar mt23">
	검색 결과 : <strong><%=StringEx.comma(objVendor.records)%></strong>건
<% if (cfg.isAuth("I")) { %>
	<input type="button" class="button2" value="등록" onclick="_regist();" />
<% } %>
</div>
<table cellpadding="0" cellspacing="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="50" />
		<col width="200" />
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
			<th nowrap>공급자명</th>
			<th nowrap>설명</th>
			<th nowrap>등록일</th>
			<th nowrap>수정일</th>
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
			<th nowrap>관리</th>
	<% } %>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objVendor.list.size(); i++) { GeneralConfig c = (GeneralConfig) objVendor.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>">
			<td class="center number"><%=c.getLong("NO")%></td>
			<td class="center"><%=c.get("NAME")%></td>
			<td><%=StringEx.setDefaultValue(c.get("MEMO"), "-")%></td>
			<td class="center number"><%=StringEx.setDefaultValue(c.get("CREATE_DATE"), "-")%></td>
			<td class="center number"><%=StringEx.setDefaultValue(c.get("MODIFY_DATE"), "-")%></td>
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
			<td class="center">
			<% if (cfg.isAuth("U")) { %>
				<input type="button" class="button1" value="수정" onclick="_modify(<%=c.getLong("SEQ")%>);" />
			<% } %>
			<% if (cfg.isAuth("D")) { %>
				<input type="button" class="button1" value="삭제" onclick="_delete(<%=c.getLong("SEQ")%>);" />
			<% } %>
			</td>
	<% } %>
		</tr>
<% } %>
<% if (objVendor.list.size() == 0) { %>
		<tr>
			<td colspan="<%=((cfg.isAuth("U") || cfg.isAuth("D")) ? "6" : "5")%>" align="center">등록된 소속이 없습니다</td>
		</tr>
<% } %>
	</tbody>
</table>
<!-- # 검색결과 -->

<!-- 페이징 -->
<div class="paging"><%=Pager.getList(request, pageNo, cfg.getInt("limit.page"), objVendor.pages, "", "page", "", buttons)%></div>
<!-- # 페이징 -->

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/layer/js/iframe.js"></script>
<script type="text/javascript">
	function _check(o) {
		if (o.sQuery.value != '' && o.sField.selectedIndex == 0) {
			window.alert('검색 필드를 선택하세요.');
			return false;
		} else if (o.sQuery.value == '' && o.sField.selectedIndex > 0) {
			window.alert('검색어를 입력하세요.');
			return false;
		}

		return true;
	}

	function _regist() {
		new IFrame(510, 180, 'ServiceVendorRegist.jsp').open();
	}

	function _modify(n) {
		new IFrame(510, 180, 'ServiceVendorModify.jsp?seq=' + n).open();
	}

	function _delete(n) {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		} else if (!confirm('선택하신 소속을 삭제하시겠습니까?')) {
			return;
		}

		$('sbmsg').show();

		new Ajax.Request('ServiceVendorDelete.jsp', {
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
</script>