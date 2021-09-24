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
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0303");

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
//
	boolean isAlert = false;

// 전송된 데이터
	long company = StringEx.str2long(request.getParameter("company"), 0);
	long organ = StringEx.str2long(request.getParameter("organ"), 0);
	int pageNo = StringEx.str2int(request.getParameter("page"), 1);
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));
	String sDate = StringEx.getKeyword(StringEx.charset(request.getParameter("sDate")));
	String eDate = StringEx.getKeyword(StringEx.charset(request.getParameter("eDate")));
	long search_flag = StringEx.str2long(request.getParameter("search_flag"), 0);
	
	if(StringEx.isEmpty(sDate)) sDate = com.nucco.lib.DateTime.date("yyyyMMdd");
	if(StringEx.isEmpty(eDate)) eDate = com.nucco.lib.DateTime.date("yyyyMMdd");
	// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "page:company:organ:sField:sQuery");

// 인스턴스 생성
	VM objVM = new VM(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objVM.getSalesCountList(company, organ, pageNo, sDate, eDate, sField, sQuery, search_flag);
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
	<span>판매계수 현황조회</span>
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
			<select id="sField">	<!--코레일유통 요청사항 20191107 scheo -->
				<%if(cfg.getLong("user.company") == 264) {%>
					<!-- <option value="">- 검색필드</option>  scheo 20200423 코레일 요청-->
					<option value="B.CODE"<%=sField.equals("B.CODE") ? " selected" : ""%>>자판기코드</option>
					<option value="B.TERMINAL_ID"<%=sField.equals("B.TERMINAL_ID") ? " selected" : ""%>>단말기 ID</option>
					<option value="B.PLACE"<%=sField.equals("B.PLACE") ? " selected" : ""%>>설치위치</option>
				<%} else { %>
					<option value="">- 검색필드</option>
					<option value="B.TERMINAL_ID"<%=sField.equals("B.TERMINAL_ID") ? " selected" : ""%>>단말기 ID</option>
					<option value="B.CODE"<%=sField.equals("B.CODE") ? " selected" : ""%>>자판기코드</option>
					<option value="B.PLACE"<%=sField.equals("B.PLACE") ? " selected" : ""%>>설치위치</option>
				<%} %>
			</select>
		<!-- <th><span>단말기 ID</span></th> -->
			<input type="text" id="sQuery" class="txtInput" value="<%=Html.getText(sQuery)%>" />
		</td>
		<td rowspan="3" class="center last">
			<input type="button" value="" class="btnSearch" onclick="_search($('search'));" />
		</td>
	</tr>
	<tr>
		<th><span>소속/조직</span></th>
		<td>
			<select id="company" class="js-example-basic-single js-example-responsive" style="width: 20%" class="<%=(cfg.getLong("user.company") > 0 ? "disabled" : "")%>" onchange="Company.organ(this.options[selectedIndex].value.evalJSON(), 0, 0);"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
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

<!-- 검색결과 -->
<div class="infoBar mt23">
	검색 결과 : <strong><%=StringEx.comma(objVM.records)%></strong>건
</div>
<table cellpadding="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="50" />
		<col width="*" />
		<col width="*" />
		<col width="*" />
		<col width="50" />
		<col width="50" />
		<col width="80" />
		<col width="120" />
		<col width="30" />
		<col width="30" />
		<col width="30" />
		<col width="30" />
		<col width="30" />
		<col width="120" />
		<col width="30" />
	</colgroup>
	<thead>
		<tr>
			<th rowspan="2" nowrap> 순번 </th>
			<th rowspan="2" nowrap> 소속 </th>
			<th rowspan="2" nowrap> 조직 </th>
			<th rowspan="2" nowrap> 설치위치 </th>
			<% if(cfg.getLong("user.company") == 0 || cfg.getLong("user.company") == 264) { %>
			<th colspan="2" nowrap> 위치코드 </th>
			<%} else { %>
			<th rowspan="2" nowrap> 단말기 코드 </th>
			<%} %>
			<th rowspan="2" nowrap> 단말기 ID </th>
			<th colspan="2" nowrap> 마감 </th>
			<th colspan="4" nowrap> 컬럼 </th>
			<th rowspan="2" nowrap> 등록일시 </th>
			<th rowspan="2" nowrap> 상태 </th>
			<th rowspan="2" nowrap>  </th>
		</tr>
		<tr>
			<% if(cfg.getLong("user.company") == 0 || cfg.getLong("user.company") == 264) { %>
			<th nowrap> 자판기 </th>
			<th nowrap> VMMS </th>
			<%} %>
			<th nowrap> 일시 </th>
			<th nowrap> 모드 </th>
			<th nowrap> 전체 </th>
			<th nowrap> 시작 </th>
			<th nowrap> 끝 </th>
			<th nowrap> 수집 </th>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objVM.list.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.list.get(i); 
		if (c.getLong("COL_COUNT") + 1 != c.getLong("CNT")) {
			
			if (c.getLong("COMPANY_SEQ")== 264) {
				// 코레일 유통일 경우 
				//scheo 20191104 등록가 판매가 차이 빨간글씨표시
				if ( c.getLong("COL_COUNT") > 24 && c.getLong("COL_COUNT") == c.getLong("COL_NO_MAX") && c.getLong("COL_NO_MIN") == 0 && Html.getText(c.get("PRICE")).equals(Html.getText(c.get("PRODUCT_PRICE"))) ) isAlert = false;
				else isAlert = true;
			} else {
				// 멀티자판기인 경우, 64, 57개 컬럼일 경우 정상
				if (c.getLong("CNT")==64 || c.getLong("CNT")==57 ) isAlert = false;
				else isAlert = true;
			}
			
		} else if(c.getLong("COL_COUNT") + 1 == c.getLong("CNT") && !Html.getText(c.get("PRICE")).equals(Html.getText(c.get("PRODUCT_PRICE")))) {
			isAlert = true;
		} else isAlert = false;
%>	
		<tr class="<%=isAlert ? "bgAlertText" : (i % 2 == 1 ? "bgLine" : "")%>" >   <!-- scheo 20191104 등록가 판매가 차이 빨간글씨표시 -->
			<td class="center number"><%=c.getLong("NO")%></td>
			<td><%=Html.getText(StringEx.setDefaultValue(c.get("COMPANY"), "-"))%></td>
			<td><%=Html.getText(StringEx.setDefaultValue(c.get("ORGAN"), "-"))%></td>
			<td><%=Html.getText(c.get("PLACE"))%></td>
			<% if(cfg.getLong("user.company") == 0 || cfg.getLong("user.company") == 264) { %>
			<td class="center number" <%=c.get("COMPANY_SEQ").equals("264")?(c.get("PLACE_CD").equals(c.get("PLACE_CODE"))?"":"style='background-color: #ff0000;color:black;'" ):""%> nowrap><%=Html.getText(c.get("PLACE_CD"))%></td>
			<%} %>
			<td class="center number" <%=c.get("COMPANY_SEQ").equals("264")?(c.get("PLACE_CODE").equals(c.get("PLACE_CD"))?"":"style='background-color: #ff0000;color:black;'" ):""%> nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("PLACE_CODE"), "&nbsp;"))%></td>
			<td class="center number" nowrap><%=Html.getText(c.get("TERMINAL_ID"))%></td>
			<td class="center" nowrap><%=Html.getText(c.get("COUNT_DATE"))%></td>
			<td class="center"><%=Html.getText(c.get("COUNT_MODE"))%></td>
			<td class="center number"><%=Html.getText(c.get("COL_COUNT"))%></td>
			<td class="center number"><%=Html.getText(c.get("COL_NO_MIN"))%></td>
			<td class="center number"><%=Html.getText(c.get("COL_NO_MAX"))%></td>
			<td class="center number"><%=Html.getText(c.get("CNT"))%></td>
			<td class="center" nowrap><%=Html.getText(c.get("REG_DATE"))%></td>
			<td class="center" nowrap><%=Html.getText(c.get("STATE"))%></td>
			<td class="center" nowrap><input type="button" class="button2" value="컬럼보기" onclick="_detail('<%=c.get("TERMINAL_ID")%>', '<%=c.get("COUNT_DATE")%>');"  /></td>
		</tr>		
<% } %>
<% if (objVM.list.size() == 0) { %>
		<tr>
			<td colspan="16" align="center">검색어를 입력 또는 소속/조직을 선택해 주세요.</td>
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
<form method="get" name="search" id="search">
<input type="hidden" name="company" value="" />
<input type="hidden" name="organ" value="" />
<input type="hidden" name="sField" value="" />
<input type="hidden" name="sQuery" value="" />
<input type="hidden" name="sDate" value="" />
<input type="hidden" name="eDate" value="" />
<input type="hidden" name="search_flag" value="" />
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
	    o.search_flag.value = 1;
		var obj = {company : $('company'), organ : null, sDate : $('sDate'), eDate : $('eDate'), sField : $('sField'), sQuery : $('sQuery')};
		var com = obj.company.options[obj.company.selectedIndex].value.evalJSON();
		/* var sField=document.getElementById("sField");
		var sQuery=document.getElementById("sQuery"); */
	
		for (var i = com.depth; i >= 0; i--) {
			obj.organ = $('organ' + i);

			if (obj.organ && obj.organ.selectedIndex > 0) {
				o.organ.value = obj.organ.options[obj.organ.selectedIndex].value;
				break;
			}
		}
		/* alert(sField.options[sField.selectedIndex].value);
		if(sField.options[sField.selectedIndex].value==''){
			window.alert('검색필드를 선택하여 주세요.');
			return;
		}else if(sQuery.value==''){
			window.alert('검색어를 입력하여 주세요.');
			return;
		} */
		o.company.value = com.seq;
		if ( obj.sDate.value == '' ||  obj.eDate.value == '') {
			window.alert('검색 기간을 선택하세요.');
			return;		
		}
		
		o.sDate.value = obj.sDate.value;
		o.eDate.value = obj.eDate.value;
		
		o.sField.value = (obj.sQuery.value == '' ? '' : obj.sField.options[obj.sField.selectedIndex].value);
		o.sQuery.value = obj.sQuery.value;
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
