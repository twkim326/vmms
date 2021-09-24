<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Sales
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /sales/SalesRealTime.jsp
 *
 * 자판기 매출정보 > 상세 매출 현황
 *
 * 작성일 - 2011/04/07, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0402");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(Message.refresh(cfg.get("script.login") + "?goUrl=" + StringEx.encode(cfg.get("topDir") + "/web/src/sales/SalesRealTime.jsp"), null, "top"));
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth()) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "A");

// 인스턴스 생성
	Sales objSales = new Sales(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objSales.report(2);
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
	<span>거래내역</span>
</div>

<form id="search" name="search" method="post" action="SalesRealTimeDetail.jsp" target="detail">
<input type="hidden" id="_company" />
<input type="hidden" id="_organ" />
<table cellspacing="0" class="tableType02">
	<colgroup>
		<col width="110" />
		<col width="*"/>
		<col width="73" />
	</colgroup>
	<tr>
		<th><span>소속/조직</span></th>
		<td>
			<select id="company" class="<%=(cfg.getLong("user.company") > 0 ? "disabled" : "")%>" onchange="Company.organ(this.options[selectedIndex].value.evalJSON(), 0, 0, 'C', null, 'Y');"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
				<option value='{"seq" : 0, "depth" : 0}'>- 소속</option>
		<% for (int i = 0; i < objSales.company.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.company.get(i); %>
				<option value='{"seq" : <%=c.getLong("SEQ")%>, "depth" : <%=c.getInt("DEPTH")%>}'<%=(c.getLong("SEQ") == cfg.getLong("user.company") ? " selected" : "")%>><%=c.get("NAME")%></option>
		<% } %>
			</select>
			<span id="organ">
		<% if (cfg.getLong("user.company") > 0) { // 소속이 지정되어 있는 계정 // %>
				<select id="organ0" onchange="CompanyEx.organ($('company').value.evalJSON(), 1, this.options[selectedIndex].value);">
					<option value="-1">- <%=objSales.data.get("TITLE")%></option>
			<% for (int i = 0; i < objSales.organ.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.organ.get(i); %>
				<option value="<%=c.getLong("SEQ")%>"><%=c.get("NAME")%></option>
			<% } %>
				</select>
		<% } else { // 시스템 관리자 // %>
				<select id="organ0" onchange="Company.organ($('company').value.evalJSON(), 1, this.options[selectedIndex].value, 'C', null, 'Y');">
					<option value="-1">- 조직</option>
				</select>
		<% } %>
			</span>
		</td>
		<td rowspan="5" class="center last">
			<input type="button" value="" class="btnSearch" onclick="_search();" />
		</td>
	</tr>
	<tr>
		<th><span>설치위치</span></th>
		<td>
			<select name="place" id="place">
				<option value="0">- 설치위치</option>
		<% if (cfg.getLong("user.company") > 0) { // 소속이 지정되어 있는 계정 // %>
			<% for (int i = 0; i < objSales.place.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.place.get(i); %>
				<option value="<%=c.getLong("SEQ")%>"><%=c.get("PLACE")%></option>
			<% } %>
		<% } %>
			</select>
		</td>
	</tr>
	<tr>
		<th><span>결제유형</span></th>
		<td>
			<input type="checkbox" id="payment" name="paymentAll" value="Y" class="checkbox" checked onclick="_checkAll(this);"> 전체
		<% for (int i = 0; i < objSales.payment.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.payment.get(i); %>
			<input type="checkbox" id="payment<%=i%>" name="payment" value="<%=c.get("CODE")%>" class="checkbox" checked onclick="_check(this);"> <%=c.get("NAME")%>
		<% } %>
		</td>
	</tr>
	<tr>
		<th><span>진행상태</span></th>
		<td>
			<input type="checkbox" id="step" name="stepAll" value="Y" class="checkbox" checked onclick="_checkAll(this);"> 전체
		<% for (int i = 0; i < objSales.step.size(); i++) { GeneralConfig c = (GeneralConfig) objSales.step.get(i); %>
			<input type="checkbox" id="step<%=i%>" name="step" value="<%=c.get("CODE")%>" class="checkbox" checked onclick="_check(this);"> <%=c.get("NAME")%>
		<% } %>
		</td>
	</tr>
	<tr>
		<th><span>조회기간</span></th>
		<td>
			<select name="sType" id="sType">
				<option value="01">거래일</option>
				<option value="02">마감일</option>
				<option value="03">입금일</option>
				<option value="04">매입청구일</option>
				<option value="05">입금예정일</option>
				<option value="11">취소일</option>
			</select>
			<input type="text" name="sDate" id="sDate" class="txtInput" style="width:80px;" maxlength="8" onkeyup="if (this.value != '' && !/^[0-9]+$/.test(this.value)) { window.alert('숫자만 입력이 가능합니다.'); this.value = ''; }" />
			<img src="<%=cfg.get("imgDir")%>/web/icon_calendar.gif" style="cursor:pointer;" alt="날짜 선택" onclick="(function () {
					var e = event ? event : window.event;
					Calendar.open($('sDate'), null, Event.pointerX(e), Event.pointerY(e), $('sDate').value);
				})(event)" />
			~
			<input type="text" name="eDate" id="eDate" class="txtInput" style="width:80px;" maxlength="8" onkeyup="if (this.value != '' && !/^[0-9]+$/.test(this.value)) { window.alert('숫자만 입력이 가능합니다.'); this.value = ''; }" />
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
</form>

<iframe id="detail" name="detail" src="SalesRealTimeDetail.jsp" scrolling="no" frameBorder="0" class="mt23" style="width:100%; height:300px; border-style:none;"></iframe>

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/calendar/js/calendar.js"></script>
<script type="text/javascript">
	function _checkAll(o) {
		for (var i = 0;$(o.id + i);i++) $(o.id + i).checked = o.checked;
	}
	
	function _check(o) {
		var checked = o.checked;
		
		if (o.checked) {
			for (var i = 0;$(o.name + i);i++) checked = checked && $(o.name + i).checked;
		}

		$(o.name).checked = checked;
	}

	function _search() {
		var o = { company : $("company"), sDate : $("sDate"), eDate : $("eDate") };

		if (o.company.selectedIndex == 0) {
			window.alert("[소속]을 선택하세요.");
			return;
		}

		var checked = $("payment").checked;
		for (var i = 0; !checked && ($("payment" + i) != null); i++) checked = $("payment" + i).checked;

		if (!checked) {
			window.alert('[결제유형]을 선택하세요.');
			return;
		}

		checked = $("step").checked;
		for (var i = 0; !checked && ($("step" + i) != null); i++) checked = $("step" + i).checked;

		if (!checked) {
			window.alert('[진행상태]를 선택하세요.');
			return;
		}

		if (!o.sDate.value) {
			window.alert('[검색기간 - 시작일]을 입력하세요.');
			return;
		}
		if (!o.eDate.value) {
			window.alert('[검색기간 - 종료일]을 입력하세요.');
			return;
		}
		if (o.sDate.value > o.eDate.value) {
			window.alert('[검색기간 - 종료일]이 [검색기간 - 시작일]보다 이전입니다.');
			return;
		}
		//3개월이 지난 거래내역에 대해서 검색하지 않는다. 2019-10-18 김태우 추가 .
		if ((o.eDate.value-o.sDate.value)>300) {
			window.alert('[검색기간]은 년월일 관계없이 최대 3개월을 넘길 수 없습니다.\n 기간을 나누어 검색하여 주세요.');
			return;
		}

		$("sbmsg").show();

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

	Event.observe("detail", "load", function (event) {
		var o = { detail : $("detail"), sbmsg : $("sbmsg") };
		o.sbmsg.hide();
		o.detail.setStyle({ 'height' : '(o.detail.contentWindow.document.body.scrollHeight + 1) + "px"'});
			}.bind(this));

//	Event.observe(window, 'load', function (event) {
		Company.depth = <%=cfg.getInt("user.organ.depth")%>;
//	}.bind(this));
</script>