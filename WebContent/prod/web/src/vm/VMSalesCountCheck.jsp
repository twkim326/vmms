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
 * /service/VMSalesCount.jsp
 *
 * 자판기 운영정보 > 판매계수 > 목록
 *
 * 작성일 - 2017/07/18, 황재원
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0304");

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
	long organ = StringEx.str2long(request.getParameter("organ"), 0);
	int pageNo = StringEx.str2int(request.getParameter("page"), 1);
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));
	String sDate = StringEx.getKeyword(StringEx.charset(request.getParameter("sDate")));
	String eDate = StringEx.getKeyword(StringEx.charset(request.getParameter("eDate")));
	String salesCount = StringEx.getKeyword(StringEx.charset(request.getParameter("salesCount")));

	if(StringEx.isEmpty(salesCount)) salesCount = "";
	if(StringEx.isEmpty(sDate)) sDate = com.nucco.lib.DateTime.date("yyyyMMdd");
	if(StringEx.isEmpty(eDate)) eDate = com.nucco.lib.DateTime.date("yyyyMMdd");
	// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "page:company:organ:sField:sQuery");

// 인스턴스 생성
	VM objVM = new VM(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objVM.getSalesCountCheckList(company, organ, pageNo, sDate, eDate, sQuery, salesCount);
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

<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/calendar/js/calendar.js"></script>
<div class="title">
	<span>판매계수 점검</span>
</div>

<table cellspacing="0" class="tableType02">
	<colgroup>
		<col width="110" />
		<col width="*"/>
		<col width="73" />
	</colgroup>
	<tr>
		<th><span>단말기 ID</span></th>
		<td>
			<input type="text" id="sQuery" class="txtInput" value="<%=Html.getText(sQuery)%>" />
		</td>
		<td rowspan="4" class="center last">
			<input type="button" value="" class="btnSearch" onclick="_search($('search'));" />
		</td>
	</tr>
	<tr>
		<th><span>판매계수전송</span></th>
		<td>
			<input type="radio" name="SalesCountYN" id="salesCountYN_0" value=""<%=!"Y".equals(salesCount) && !"N".equals(salesCount) ? " checked" : ""%> checked>전체
			<input type="radio" name="SalesCountYN" id="salesCountYN_1" value="Y"<%="Y".equals(salesCount) ? " checked" : ""%>>예
			<input type="radio" name="SalesCountYN" id="salesCountYN_2" value="N"<%="N".equals(salesCount) ? " checked" : ""%>>아니오
		</td>
	</tr>
	<tr>
		<th><span>소속/조직</span></th>
		<td>
			<select id="company" class="<%=(cfg.getLong("user.company") > 0 ? "disabled" : "")%>"  class="js-example-basic-single js-example-responsive" style="width: 20%" onchange="Company.organ(this.options[selectedIndex].value.evalJSON(), 0, 0);"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
				<option value='{"seq" : 0, "depth" : 0}'>- 소속</option>
			</select>
			<span id="organ">
				<select id="organ0" class="js-example-basic-single js-example-responsive" style="width: 18%" onchange="Company.organ($('company').value.evalJSON(), 1, this.options[selectedIndex].value);">
					<option value="-1">- 조직</option>
				</select>
			</span>
		</td>
	</tr>
	<tr>
		<th><span>검색기간</span></th>
		<td>
			<input type="text" name="sDate" id="sDate" class="txtInput" value="<%=sDate%>" style="width:80px;" maxlength="8" onkeyup="if (this.value != '' && !/^[0-9]+$/.test(this.value)) { window.alert('숫자만 입력이 가능합니다.'); this.value = ''; }" />
			<img src="<%=cfg.get("imgDir")%>/web/icon_calendar.gif" style="cursor:pointer;" alt="날짜 선택" onclick="(function () {
					var e = event ? event : window.event;
					Calendar.open($('sDate'), null, Event.pointerX(e), Event.pointerY(e), $('sDate').value);
				})(event)" />
			~
			<input type="text" name="eDate" id="eDate" class="txtInput" value="<%=eDate%>" style="width:80px;" maxlength="8" onkeyup="if (this.value != '' && !/^[0-9]+$/.test(this.value)) { window.alert('숫자만 입력이 가능합니다.'); this.value = ''; }" />
			<img src="<%=cfg.get("imgDir")%>/web/icon_calendar.gif" style="cursor:pointer;" alt="날짜 선택" onclick="(function () {
					var e = event ? event : window.event;
					Calendar.open($('eDate'), null, Event.pointerX(e), Event.pointerY(e), $('eDate').value);
				})(event)" />
			<input type="button" class="button1" value="오늘" onclick="_setDate(0);" />
			<input type="button" class="button1" value="어제" onclick="_setDate(-1);" />
			<input type="button" class="button1" value="이번달" onclick="_setDate(1);" />
			<input type="button" class="button1" value="지난달" onclick="_setDate(2);" />
		</td>
	</tr>
</table>
<%	if ( StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) { %>
<div class="infoSearch">
	<img src="<%=cfg.get("imgDir")%>/web/txt_searchInfo.gif" alt="상단의 검색 조건을 통해 데이터를 검색해 주세요." />
</div>
<%	} else { %>
<!-- 검색결과 -->
<div class="infoBar mt23">
	검색 결과 : <strong><%=StringEx.comma(objVM.records1)%></strong>/<strong><%=StringEx.comma(objVM.records)%></strong>건
</div>
<table cellpadding="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="50" />
		<col width="*" />
		<col width="*" />
		<col width="*" />
		<col width="60" />
		<col width="80" />
		<col width="115" />
		<col width="115" />
		<col width="175" />
		<col width="30" />
	</colgroup>
	<thead>
		<tr>
			<th rowspan="2" nowrap> 순번 </th>
			<th rowspan="2" nowrap> 소속 </th>
			<th rowspan="2" nowrap> 조직 </th>
			<th rowspan="2" nowrap> 설치위치 </th>
			<th rowspan="2" nowrap> 설치코드 </th>
			<th rowspan="2" nowrap> 단말기 ID </th>
			<th colspan="4" nowrap> 마감 </th>
		</tr>
		<tr>
			<th nowrap> 최초 </th>
			<th nowrap> 직전 </th>
			<th nowrap>검색기간내 최종</th>
			<th nowrap> 모드 </th>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objVM.list.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>" >		
			<td class="center number"><%=c.getLong("NO")%></td>
			<td><%=Html.getText(StringEx.setDefaultValue(c.get("COMPANY"), "-"))%></td>
			<td><%=Html.getText(StringEx.setDefaultValue(c.get("ORGAN"), "-"))%></td>
			<td><%=Html.getText(c.get("PLACE"))%></td>
			<td class="center number" nowrap><%=StringEx.isEmpty(c.get("PLACE_CD"))?"&nbsp;":Html.getText(c.get("PLACE_CD"))%></td>
			<td class="center number" nowrap><%=Html.getText(c.get("TERMINAL_ID"))%></td>
			<td class="center" nowrap><%=c.get("MIN_COUNT_DATE").substring(0, 10)%>&nbsp;<input type="button" class="button1" value="컬럼" onclick="_detail('<%=c.get("TERMINAL_ID")%>', '<%=c.get("MIN_COUNT_DATE")%>')"  /></td>
			<td class="center" nowrap><%=StringEx.isEmpty(c.get("PREV_COUNT_DATE"))?"":c.get("PREV_COUNT_DATE").substring(0, 10)%><%=StringEx.isEmpty(c.get("PREV_COUNT_DATE"))?"&nbsp;":"&nbsp;<input type=\"button\" class=\"button1\" value=\"컬럼\" onclick=\"_detail('" + c.get("TERMINAL_ID")+ "', '" + c.get("PREV_COUNT_DATE")+ "')\" />"%></td>
			<td class="center" <%=(StringEx.isEmpty(c.get("COUNT_DATE")) ?"style='background-color: #ff0000'":"")%> nowrap><%=Html.getText(c.get("COUNT_DATE"))%><%=StringEx.isEmpty(c.get("COUNT_DATE"))?"&nbsp;":"&nbsp;<input type=\"button\" class=\"button1\" value=\"컬럼\" onclick=\"_detail('" + c.get("TERMINAL_ID")+ "', '" + c.get("COUNT_DATE")+ "')\" />"%></td>
			<td class="center"><%=StringEx.isEmpty(c.get("COUNT_MODE"))?"&nbsp;":Html.getText(c.get("COUNT_MODE"))%></td>
		</tr>		
<% } %>
<% if (objVM.list.size() == 0) { %>
		<tr>
			<td colspan="13" align="center">등록된 자판기가 없습니다</td>
		</tr>
<% } %>
	</tbody>
</table>
<!-- # 검색결과 -->

<!-- 페이징 -->
<div class="paging"><%=Pager.getList(request, pageNo, cfg.getInt("limit.page"), objVM.pages, "", "page", "", buttons)%></div>
<!-- # 페이징 -->
<%	} %>
<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<form method="get" name="search" id="search">
<input type="hidden" name="company" value="" />
<input type="hidden" name="organ" value="" />
<input type="hidden" name="sField" value="" />
<input type="hidden" name="sQuery" value="" />
<input type="hidden" name="salesCount" value="" />
<input type="hidden" name="sDate" value="" />
<input type="hidden" name="eDate" value="" />
</form>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/layer/js/iframe.js"></script>
<script type="text/javascript">
	function _setDate(n) {
		var d = new Date();
	
		if (n > 0) {
			var s = new Date(d.getFullYear(), d.getMonth() - n + 1, 1);
			var e = new Date(d.getFullYear(), d.getMonth() - n + 2, 0);
	
			$("sDate").value = s.getFullYear() + (s.getMonth() < 9 ? "0" : "") + (s.getMonth() + 1) + (s.getDate() <= 9 ? "0" : "") + s.getDate();
			$("eDate").value = e.getFullYear() + (e.getMonth() < 9 ? "0" : "") + (e.getMonth() + 1) + (e.getDate() <= 9 ? "0" : "") + e.getDate();
		} else {
			var s = new Date(d.getFullYear(), d.getMonth(), d.getDate() + n);
	
			$("sDate").value = s.getFullYear() + (s.getMonth() < 9 ? "0" : "") + (s.getMonth() + 1) + (s.getDate() <= 9 ? "0" : "") + s.getDate();
			$("eDate").value = s.getFullYear() + (s.getMonth() < 9 ? "0" : "") + (s.getMonth() + 1) + (s.getDate() <= 9 ? "0" : "") + s.getDate();
		}
	}
	
	function _search(o) {
		var obj = {company : $('company'), organ : null, sDate : $('sDate'), eDate : $('eDate'), sQuery : $('sQuery')};
		var com = obj.company.options[obj.company.selectedIndex].value.evalJSON();

		for (var i = com.depth; i >= 0; i--) {
			obj.organ = $('organ' + i);

			if (obj.organ && obj.organ.selectedIndex > 0) {
				o.organ.value = obj.organ.options[obj.organ.selectedIndex].value;
				break;
			}
		}

		o.company.value = com.seq;
		if ( obj.sDate.value == '' ||  obj.eDate.value == '') {
			window.alert('검색 기간을 선택하세요.');
			return;		
		}
		o.sDate.value = obj.sDate.value;
		o.eDate.value = obj.eDate.value

		o.sQuery.value = obj.sQuery.value;
		
		if ($('salesCountYN_0').checked) o.salesCount.value = $('salesCountYN_0').value;
		if ($('salesCountYN_1').checked) o.salesCount.value = $('salesCountYN_1').value;
		if ($('salesCountYN_2').checked) o.salesCount.value = $('salesCountYN_2').value;
	
		o.submit();
	}
	
	function _detail(terminal_id, count_date) {
		new IFrame(960, 500, 'VMSalesCountDetail.jsp?terminal_id=' + terminal_id + '&count_date=' + count_date).open();	
	}	
	
//	Event.observe(window, 'load', function (event) {
		Company.depth = <%=cfg.getInt("user.organ.depth")%>;
		Company.company(<%=(cfg.getLong("user.company") > 0 ? cfg.getLong("user.company") : company)%>, <%=(organ > 0 ? organ : cfg.getLong("user.organ"))%>);
//	}.bind(this));
</script>