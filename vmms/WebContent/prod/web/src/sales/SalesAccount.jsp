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
 * /sales/SalesAccount.jsp
 *
 * 자판기 매출정보 > 월 정산 레포트
 *
 * 작성일 - 2011/04/09, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0404");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(Message.refresh(cfg.get("script.login") + "?goUrl=" + StringEx.encode(cfg.get("topDir") + "/web/src/sales/SalesAccount.jsp"), null, "top"));
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
		error = objSales.report(1);
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
	<span>정산현황</span>
</div>

<form name="search" id="search" method="post" action="SalesAccountDetail.jsp" target="detail">
<table cellspacing="0" class="tableType02">
	<colgroup>
		<col width="110" />
		<col width="*"/>
		<col width="73" />
	</colgroup>
	<tr>
		<th><span>정산대상</span></th>
		<td>
			<input type="radio" id="sType" name="sType" value="03" onclick="_clearDate();" checked /> 신용(입금일)
			<input type="radio" name="sType" value="04" onclick="_clearDate();" /> 선불(매입청구일)
			<!-- J.W.Chae 2021.02.19 간편결제 정산현황 조회 기준 수정 -->
			<input type="radio" id="s2Type" name="sType" value="05" onclick="_clearDate();" /> 간편결제(매입청구일)
		</td>
		<td rowspan="2" class="center last">
			<input type="button" value="" class="btnSearch" onclick="_search();" />
		</td>
	</tr>
	<tr>
		<th><span>정산기간</span></th>
		<td>
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
			<input type="button" class="button1" value="이번달" onclick="_setDate(0);" />
			<input type="button" class="button1" value="지난달" onclick="_setDate(-1);" />
			<input type="button" class="button1" value="-2개월" onclick="_setDate(-2);" />
			<input type="button" class="button1" value="-3개월" onclick="_setDate(-3);" />
		</td>
	</tr>
</table>
</form>

<iframe id="detail" name="detail" src="SalesAccountDetail.jsp" scrolling="no" frameBorder="0" class="mt23" style="min-width:960px; width:100%; height:207px; border-style:none;"></iframe>

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/calendar/js/calendar.js"></script>
<script type="text/javascript">
	function _clearDate() {
		$("eDate").value = $("sDate").value = "";
	}
	function _setDate(n) {
		console.log("1111")
		var d = new Date();
		var delta = $("sType").checked ? 0 : $("s2Type").checked ? 0 : 1;
		console.log(delta)
		var s = new Date(d.getFullYear(), d.getMonth() + n, 1 + delta);
		var e = new Date(d.getFullYear(), d.getMonth() + n + 1, 0 + delta);

		$("sDate").value = s.getFullYear() + (s.getMonth() < 9 ? "0" : "") + (s.getMonth() + 1) + (s.getDate() <= 9 ? "0" : "") + s.getDate();
		$("eDate").value = e.getFullYear() + (e.getMonth() < 9 ? "0" : "") + (e.getMonth() + 1) + (e.getDate() <= 9 ? "0" : "") + e.getDate();
		console.log($("sDate").value)
		console.log($("eDate").value)
	}

	function _search() {
		var o = { sDate : $('sDate'), eDate : $('eDate') };

		if (!o.sDate.value) {
			window.alert("[정산기간 - 시작일]을 입력하세요.");
			return;
		}
		if (!o.eDate.value) {
			window.alert("[정산기간 - 종료일]을 입력하세요.");
			return;
		}
		if (o.sDate.value > o.eDate.value) {
			window.alert("[정산기간 - 종료일]이 [집계기간 - 시작일]보다 이전입니다.");
			return;
		}
		//3개월이 지난 거래내역에 대해서 검색하지 않는다. 2019-10-18 김태우 추가 .
		//if ((o.eDate.value-o.sDate.value)>300) {
		//	window.alert('[검색기간]은 년월일 관계없이 최대 3개월을 넘길 수 없습니다.\n 기간을 나누어 검색하여 주세요.');
		//	return;
		//}

		$("sbmsg").show();
		$("search").submit();
	}

	Event.observe("detail", "load", function (event) {
		var o = {detail : $("detail"), sbmsg : $("sbmsg")};

		o.sbmsg.hide();
		o.detail.setStyle({height : o.detail.contentWindow.document.body.scrollHeight + "px"});
	}.bind(this));
</script>