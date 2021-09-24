<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.VM
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceVMMaster.jsp
 *
 * 서비스 > 자판기 기초정보 > 목록
 *
 * 작성일 - 2011/04/02, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0203");

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
	int pageNo = StringEx.str2int(request.getParameter("page"), 1);
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "company:pageNo:sField:sQuery");

// 인스턴스 생성
	VM objVM = new VM(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objVM.master(company, pageNo, sField, sQuery);
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
	<span>자판기 기초정보 관리</span>
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
				<option value="A.CODE"<%=sField.equals("A.CODE") ? " selected" : ""%>>코드</option>
				<option value="A.REGION"<%=sField.equals("A.REGION") ? " selected" : ""%>>설치지역</option>
				<option value="A.PLACE"<%=sField.equals("A.PLACE") ? " selected" : ""%>>설치위치</option>
			</select>
			<input name="sQuery" type="text" id="sQuery" class="txtInput" value="<%=Html.getText(sQuery)%>" />
		</td>
		<td rowspan="2" class="center last">
		  <input type="submit" value="" class="btnSearch" />
		</td>
	</tr>
	<tr>
		<th><span>소속</span></th>
		<td>
			<select name="company" id="company"<%=(cfg.getLong("user.company") > 0 ? " class='disabled' disabled" : "")%> class="js-example-basic-single js-example-responsive" style="width: 20%" >
				<option value="0">- 소속</option>
		<% for (int i = 0; i < objVM.company.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.company.get(i); %>
				<option value="<%=c.getLong("SEQ")%>"<%=((c.getLong("SEQ") == company || c.getLong("SEQ") == cfg.getLong("user.company")) ? " selected" : "")%>><%=c.get("NAME")%></option>
		<% } %>
			</select>
		</td>
	</tr>
</table>
</form>

<!-- 검색결과 -->
<div class="infoBar mt23">
	검색 결과 : <strong><%=StringEx.comma(objVM.records)%></strong>건
<% if (cfg.isAuth("I")) { %>
	<input type="button" class="button2" value="개별등록" onclick="_regist();" />
	<input type="button" class="button2" value="일괄등록" onclick="_bundle();" style="right:80px;" />
<% } %>
</div>
<table cellpadding="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="50" />
		<col width="*" />
		<col width="100" />
		<col width="*" />
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
			<th nowrap>코드</th>
			<th nowrap>설치지역</th>
			<th nowrap>설치위치</th>
			<th nowrap>등록일</th>
			<th nowrap>수정일</th>
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
			<th nowrap>관리</th>
	<% } %>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objVM.list.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>">
			<td class="center number"><%=c.getLong("NO")%></td>
			<td class="left"><%=Html.getText(StringEx.setDefaultValue(c.get("COMPANY"), "-"))%></td>
			<td class="center number"><%=Html.getText(StringEx.setDefaultValue(c.get("CODE"), "-"))%></td>
			<td><%=Html.getText(StringEx.setDefaultValue(c.get("REGION"), "-"))%></td>
			<td><%=Html.getText(c.get("PLACE"))%></td>
			<td class="center number"><%=Html.getText(c.get("CREATE_DATE"))%></td>
			<td class="center number"><%=Html.getText(StringEx.setDefaultValue(c.get("MODIFY_DATE"), "-"))%></td>
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
			<td class="center">
			<% if (cfg.isAuth("U")) { %>
				<input type="button" class="button1" value="수정" onclick="_modify(<%=c.getLong("COMPANY_SEQ")%>, '<%=c.get("CODE")%>');" />
			<% } %>
			<% if (cfg.isAuth("D")) { %>
				<input type="button" class="button1" value="삭제" onclick="_delete(<%=c.getLong("COMPANY_SEQ")%>, '<%=c.get("CODE")%>');" />
			<% } %>
			</td>
	<% } %>
		</tr>
<% } %>
<% if (objVM.list.size() == 0) { %>
		<tr>
			<td colspan="<%=((cfg.isAuth("U") || cfg.isAuth("D")) ? "8" : "7")%>" align="center">등록된 자판기가 없습니다</td>
		</tr>
<% } %>
	</tbody>
</table>
<!-- # 검색결과 -->

<!-- 페이징 -->
<div class="paging"><%=Pager.getList(request, pageNo, cfg.getInt("limit.page"), objVM.pages, "", "page", "", buttons)%></div>
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

	function _bundle() {
		new IFrame(510, 185, 'ServiceVMMasterRegistBundle.jsp').open();
	}

	function _regist() {
		new IFrame(510, 240, 'ServiceVMMasterRegist.jsp').open();
	}

	function _modify(company, code) {
		new IFrame(510, 240, 'ServiceVMMasterModify.jsp?company=' + company + '&code=' + code).open();
	}

	function _delete(company, code) {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		} else if (!confirm('선택하신 자판기를 삭제하시겠습니까?')) {
			return;
		}

		$('sbmsg').show();

		new Ajax.Request('ServiceVMMasterDelete.jsp', {
			asynchronous : true,
			parameters : {company : company, code : code},
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