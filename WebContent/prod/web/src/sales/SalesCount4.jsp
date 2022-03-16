<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.Sales
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /sales/SalesClosing.jsp
 *
 * 자판기 매출정보 > 매출 마감 현황
 *
 * 작성일 - 2011/04/07, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0406");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(Message.refresh(cfg.get("script.login") + "?goUrl=" + StringEx.encode(cfg.get("topDir") + "/web/src/sales/SalesClosing.jsp"), null, "top"));
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

		if(StringEx.isEmpty(sDate)) sDate = com.nucco.lib.DateTime.date("yyyyMMdd");
		if(StringEx.isEmpty(eDate)) eDate = com.nucco.lib.DateTime.date("yyyyMMdd");
		// URL에 더해질 파라미터
		String addParam = Param.addParam(request, "page:company:organ:sField:sQuery");
// 인스턴스 생성
	Sales objSales = new Sales(cfg);

// 메서드 호출
	String error = null;

	try {		
			error = objSales.salesCountListNew(company, organ, sDate, eDate, oMode, oType,sField,sQuery);
	} catch (Exception e) {
		error = e.getMessage();
	}

// 에러 처리
	if (error != null) {
		out.print(error);
		return;
	}

%>
<%@ include file="../../header.inc.jsp" %>
<div class="title">
	<span>판매계수집계</span>
</div>

<table cellspacing="0" class="tableType02">
	<colgroup>
		<col width="110" />
		<col width="*"/>
		<col width="73" />
	</colgroup>
	</colgroup>
	<tr>
		<th><span>검색어</span></th>
		<td>
			<select id="sField">
				<option value="">- 검색필드</option>
				<option value="B.TERMINAL_ID"<%=sField.equals("B.TERMINAL_ID") ? " selected" : ""%>>단말기 ID</option>
				<option value="B.CODE"<%=sField.equals("B.CODE") ? " selected" : ""%>>자판기코드</option>
				<option value="B.PLACE"<%=sField.equals("B.PLACE") ? " selected" : ""%>>설치위치</option>
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


<div style="position:relative;">
	<a style="position:absolute; right:0; top:5px; font-size:11px; color:#888;">* 당일 마감집계는 오늘버튼을 이용해 주시기 바랍니다. 마감집계와 거래내역은 다를 수 있습니다.</a>
</div>

<iframe id="detail" name="detail" src="SalesCountDetail.jsp" scrolling="no" frameBorder="0" class="mt23" style="width:100%; height:207px; border-style:none;"></iframe>

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<form method="get" name="search" id="search">
<input type="hidden" name="company" value="" />
<input type="hidden" name="organ" value="" />
<input type="hidden" name="sField" value="" />
<input type="hidden" name="sQuery" value="" />
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
		var obj = {company : $('company'), organ : null, sDate : $('sDate'), eDate : $('eDate'), sField : $('sField'), sQuery : $('sQuery')};
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

		o.sField.value = (obj.sQuery.value == '' ? '' : obj.sField.options[obj.sField.selectedIndex].value);
		o.sQuery.value = obj.sQuery.value;
		alert(o.company.value+o.organ.value+o.sQuery.value+o.sField.value);
		o.submit();
	}
	
	function _detail(terminal_id, count_date) {
		new IFrame(960, 500, 'SalesCountDetail.jsp?terminal_id=' + terminal_id + '&count_date=' + count_date).open();	
	}
	
	
//	Event.observe(window, 'load', function (event) {
		Company.depth = <%=cfg.getInt("user.organ.depth")%>;
		Company.company(<%=(cfg.getLong("user.company") > 0 ? cfg.getLong("user.company") : company)%>, <%=(organ > 0 ? organ : cfg.getLong("user.organ"))%>);
//	}.bind(this));
</script>