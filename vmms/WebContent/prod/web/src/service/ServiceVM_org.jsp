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
 * /service/ServiceVM.jsp
 *
 * 서비스 > 운영 자판기 > 목록
 *
 * 작성일 - 2011/04/02, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0202");

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
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));
	String aspCharge = StringEx.getKeyword(StringEx.charset(request.getParameter("aspCharge")));
	long company = StringEx.str2long(request.getParameter("company"), 0);
	long organ = StringEx.str2long(request.getParameter("organ"), 0);
	int pageNo = StringEx.str2int(request.getParameter("page"), 1);

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "page:company:organ:sField:sQuery:aspCharge");

// 인스턴스 생성
	VM objVM = new VM(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objVM.getList(company, organ, aspCharge, pageNo, sField, sQuery);
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
	<span>운영 자판기 관리</span>
</div>
<form name="search" id="search" method="get">
<input type="hidden" id="_company" />
<input type="hidden" id="_organ" />
<table cellspacing="0" class="tableType02">
	<colgroup>
		<col width="110" />
		<col width="*"/>
		<col width="73" />
	</colgroup>
	<tr>
		<th><span>검색어</span></th>
		<td>
			<select id="sField" name="sField">
				<option value="">- 검색필드</option>
				<option value="A.TERMINAL_ID"<%=sField.equals("A.TERMINAL_ID") ? " selected" : ""%>>단말기 ID</option>
				<option value="A.MODEL"<%=sField.equals("A.MODEL") ? " selected" : ""%>>모델</option>
				<option value="A.PLACE"<%=sField.equals("A.PLACE") ? " selected" : ""%>>설치위치</option>
				<option value="C.NAME"<%=sField.equals("C.NAME") ? " selected" : ""%>>담당자</option>
				<option value="A.CODE"<%=sField.equals("A.CODE") ? " selected" : ""%>>자판기코드</option>
			</select>
			<input type="text" id="sQuery" name="sQuery" class="txtInput" value="<%=Html.getText(sQuery)%>" />
		</td>
		<td rowspan="3" class="center last">
			<input type="button" value="" class="btnSearch" onclick="_search();" />
		</td>
	</tr>
		<th><span>ASP 과금</span></th>
		<td>
			<input type="radio" name="aspCharge" value=""<%=!"Y".equals(aspCharge) && !"N".equals(aspCharge) ? " checked" : ""%> checked>전체
			<input type="radio" name="aspCharge" value="Y"<%="Y".equals(aspCharge) ? " checked" : ""%>>예
			<input type="radio" name="aspCharge" value="N"<%="N".equals(aspCharge) ? " checked" : ""%>>아니오
		</td>
	</tr>
	<tr>
		<th><span>소속/조직</span></th>
		<td>
			<select id="company" class="<%=(cfg.getLong("user.company") > 0 ? "disabled" : "")%>" onchange="Company.organ(this.options[selectedIndex].value.evalJSON(), 0, 0);"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
				<option value='{"seq" : 0, "depth" : 0}'>- 소속</option>
			</select>
			<span id="organ">
				<select id="organ0" onchange="Company.organ($('company').value.evalJSON(), 1, this.options[selectedIndex].value);">
					<option value="-1">- 조직</option>
				</select>
			</span>
		</td>
	</tr>
</table>
</form>

<!-- 검색결과 -->
<div class="infoBar mt23">
	검색 결과 : <strong><%=StringEx.comma(objVM.records)%></strong>건
	<div>
		<input type="button" class="button2 excel_save_modify" onclick="_excel();" value="엑셀저장" />
	<% if (cfg.isAuth("I")) { %>
		<input type="button" class="button2" value="개별등록" onclick="location.href = 'ServiceVMRegist.jsp?1=1<%=addParam%>';" />
	<!-- 	<input type="button" class="button2" value="일괄등록" onclick="_bundle();" style="right:80px;" />  -->
	<% } %>
	</div>
</div>
<table cellpadding="0" cellspacing="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="40" /><!-- 순번 -->
		<col width="100" /><!-- 소속 -->
		<col width="*" /><!-- 자판기 조직 -->
		<col width="50" /><!-- 코드 -->
		<col width="70" /><!-- 단말기 ID -->
		<col width="90" /><!-- 모델 -->
		<col width="*" /><!-- 설치위치 -->
		<col width="80" /><!-- 담당자 -->
		<col width="55" /><!-- 담당자ID -->
		<col width="40" /><!-- 과금 -->
		<col width="70" /><!-- 등록일 -->
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
		<col width="<%=(cfg.isAuth("U") && cfg.isAuth("D") ? "120" : "60")%>" /><!-- 관리 -->
	<% } %>
	</colgroup>
	<thead>
		<tr>
			<th nowrap>순번</th>
			<th nowrap>소속</th>
			<th nowrap>자판기 조직</th>
			<th nowrap>코드</th>
			<th nowrap>단말기 ID</th>
			<th nowrap>모델</th>
			<th nowrap>설치위치</th>
			<th nowrap>담당자</th>
			<th nowrap>담당자ID</th>
			<th nowrap>과금</th>
			<th nowrap>등록일</th>
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
			<th nowrap>관리</th>
	<% } %>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objVM.list.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>">
			<td class="center number"><%=c.getLong("NO")%></td>
			<td class="center"><%=Html.getText(StringEx.setDefaultValue(c.get("COMPANY"), "-"))%></td>
			<td><%=Html.getText(StringEx.setDefaultValue(c.get("ORGAN"), "-"))%></td>
			<td class="center number" nowrap><%=Html.getText(c.get("CODE"))%></td>
			<td class="center number" nowrap><%=Html.getText(c.get("TERMINAL_ID"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("MODEL"), "-"))%></td>
			<td><%=Html.getText(c.get("PLACE"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("USER_NAME"), "-"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("USER_ID"), "-"))%></td>
			<td class="center"><%=Html.getText(StringEx.setDefaultValue(c.get("ASP_CHARGE"), "N"))%></td>
			<td class="center number" nowrap><%=Html.getText(c.get("CREATE_DATE"))%></td>
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
			<td class="center" nowrap>
			<% if (cfg.isAuth("U")) { %>
				<input type="button" class="button1" value="수정" onclick="location.href = 'ServiceVMRegist.jsp?seq=<%=c.getLong("SEQ") + addParam%>';" />
			<% } %>
			<% if (cfg.isAuth("D")) { %>
				<input type="button" class="button1" value="삭제" onclick="_delete(<%=c.getLong("SEQ")%>);" />
			<% } %>
			</td>
	<% } %>
		</tr>
<% } %>
<% if (objVM.list.size() == 0) { %>
		<tr>
			<td colspan="<%=((cfg.isAuth("U") || cfg.isAuth("D")) ? "12" : "11")%>" align="center">등록된 자판기가 없습니다</td>
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
	function _search() {
		var o = { company : $("company"), sField : $("sField"), sQuery : $("sQuery") };

		if (o.sQuery.value != '' && o.sField.selectedIndex == 0) {
			window.alert('[검색항목]를 선택하세요.');
			return;
		}
		if (o.sQuery.value == '' && o.sField.selectedIndex > 0) {
			window.alert('[검색어]를 입력하세요.');
			return;
		}

		var organ = null;
		var company = o.company.value.evalJSON();

		for (var i = company.depth; i >= 0; i--) {
			var obj = $("organ" + i);

			if (obj && obj.selectedIndex > 0) {
				organ = obj.value;
				break;
			}
		}

		$("_company").name = "company";
		$("_company").value = company.seq;
		$("_organ").name = "organ";
		$("_organ").value = organ;

		$("search").submit();
	}

	function _delete(n) {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		}

		if (!confirm('선택하신 자판기를 삭제하시겠습니까?')) {
			return;
		}

		$('sbmsg').show();

		new Ajax.Request('ServiceVMDelete.jsp', {
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

	function _bundle() {
		new IFrame(510, 185, 'ServiceVMRegistBundle.jsp').open();
	}

	function _excel() {
		var records = <%=(objVM.list == null ? 0 : objVM.list.size())%>;

		if (records > 65000) {
			window.alert('65,000 라인을 초과하는 데이터입니다.\n\n보다 상세한 검색 조건으로 검색 후 다운로드하세요.');
			return;
		}

		location.href = 'ServiceVMExcel.jsp?1=1<%=addParam%>';
	}

//	Event.observe(window, 'load', function (event) {
		Company.depth = <%=cfg.getInt("user.organ.depth")%>;
		Company.company(<%=(cfg.getLong("user.company") > 0 ? cfg.getLong("user.company") : company)%>, <%=(organ > 0 ? organ : cfg.getLong("user.organ"))%>);
//	}.bind(this));
</script>