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
 * /system/VMRunLog.jsp
 *
 * 자판기 운영 정보 > 운영정보 로그조회 > 목록
 *
 * 작성일 - 2011/04/03, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0302");

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
	String sDate = StringEx.getKeyword(StringEx.charset(request.getParameter("sDate")));
	String eDate = StringEx.getKeyword(StringEx.charset(request.getParameter("eDate")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));
	
	if(StringEx.isEmpty(sDate)) sDate = com.nucco.lib.DateTime.date("yyyyMMdd");
	if(StringEx.isEmpty(eDate)) eDate = com.nucco.lib.DateTime.date("yyyyMMdd");

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "page:sDate:eDate:sQuery");
		
// 인스턴스 생성
	VM objVM = new VM(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objVM.log(pageNo, sDate, eDate, sQuery);
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
	<span>운영 정보 로그 조회</span>
</div>

<form method="get">
<table cellspacing="0" class="tableType02">
	<colgroup>
		<col width="110" />
		<col width="*"/>
		<col width="73" />
	</colgroup>
	<tr>
		<th><span>단말기 ID</span></th>
		<td><input type="text" name="sQuery" id="sQuery" class="txtInput" value="<%=Html.getText(sQuery)%>" /></td>
		<td rowspan="2" class="center last">
		  <input type="button" value="" class="btnSearch" onclick="_search(this.form);" />
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
</form>

<!-- 검색결과 -->
<div class="infoBar mt23">
	검색 결과 : <strong><%=StringEx.comma(objVM.records)%></strong>건
	<!-- 210304 cdh 엑셀다운로드 추가 -->
	<input type="button" class="button2 excel_save_modify2" onclick="_excel();" value="엑셀저장" />
	<!-- 엑셀다운로드 끝 -->
</div>
<table cellpadding="0" cellspacing="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="50" />
		<col width="120" />
		<col width="100" />
		<col width="*" />
			<!-- 20130308 자판기상태정보 변경 시작 -->
			<!-- 				
		<col width="80" />
			 -->
			<!-- 20130308 자판기상태정보 변경 종료 -->			
		<col width="80" />
		<col width="80" />
		<col width="80" />
	</colgroup>
	<thead>
		<tr>
			<th nowrap>순번</th>
			<th nowrap>보고시각</th>
			<th nowrap>단말기 ID</th>
			<th nowrap>설치위치</th>
			<!-- 20130308 자판기상태정보 변경 시작 -->
			<!-- 			
			<th nowrap>미등록</th>
			 -->
			<!-- 20130308 자판기상태정보 변경 종료 -->			
			<th nowrap>품절</th>
			<th nowrap>주제어부</th>
			<th nowrap>P/D</th>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objVM.list.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>">
			<td class="center number"><%=c.getLong("NO")%></td>
			<td class="center number"><%=Html.getText(c.get("CREATE_DATE"))%></td>
			<td class="center number" onclick="$('sQuery').value = '<%=Html.getText(c.get("TERMINAL_ID"))%>';" style="cursor:pointer;"><%=Html.getText(c.get("TERMINAL_ID"))%></td>
			<td><%=StringEx.setDefaultValue(Html.getText(c.get("PLACE")), "-")%></td>
			<!-- 20130308 자판기상태정보 변경 시작 -->
			<!-- 					
			<td class="center">
				<%=(StringEx.isEmpty(c.get("EMPTY_COL")) ? "-" : "<img onmouseover='$(\"0" + i + "\").show()' onmouseout='$(\"0" + i + "\").hide()' src='" + cfg.get("imgDir") + "/web/nonregistered.png' style='width:20px; height:20px;' alt='' />")%>
				<span id="0<%=i%>" class="description" style="display:none; z-index:999;">
					<span><%=c.get("EMPTY_COL")%></span>
				</span>
			</td>
			 -->
			<!-- 20130308 자판기상태정보 변경 종료 -->					
			<td class="center">
				<%=(StringEx.isEmpty(c.get("SOLD_OUT")) ? "-" : "<img onmouseover='$(\"1" + i + "\").show()' onmouseout='$(\"1" + i + "\").hide()' src='" + cfg.get("imgDir") + "/web/icon_error.png' style='width:20px; height:20px;' alt='' />")%>
				<span id="1<%=i%>" class="description" style="display:none; z-index:999;">
					<span><%=c.get("SOLD_OUT")%></span>
				</span>
			</td>
			<td class="center">
				<%=(StringEx.isEmpty(c.get("CONTROL_ERROR")) ? "-" : "<img onmouseover='$(\"2" + i + "\").show()' onmouseout='$(\"2" + i + "\").hide()' src='" + cfg.get("imgDir") + "/web/icon_error.png' style='width:20px; height:20px;' alt='' />")%>
				<span id="2<%=i%>" class="description" style="display:none; z-index:999;">
					<span><%=c.get("CONTROL_ERROR")%></span>
				</span>
			</td>
			<td class="center">
				<%=(StringEx.isEmpty(c.get("PD_ERROR")) ? "-" : "<img onmouseover='$(\"3" + i + "\").show()' onmouseout='$(\"3" + i + "\").hide()' src='" + cfg.get("imgDir") + "/web/icon_error.png' style='width:20px; height:20px;' alt='' />")%>
				<span id="3<%=i%>" class="description" style="display:none; z-index:999;">
					<span><%=c.get("PD_ERROR")%></span>
				</span>
			</td>
		</tr>
<% } %>
<% if (objVM.list.size() == 0) { %>
		<tr>
			<!-- 20130308 자판기상태정보 변경 시작 -->
			<!-- 				
			<td colspan="8" align="center">등록된 기록이 없습니다</td>
			 -->
			 <td colspan="7" align="center">등록된 기록이 없습니다</td>
			<!-- 20130308 자판기상태정보 변경 종료 -->					
		</tr>
<% } %>
	</tbody>
</table>
<!-- # 검색결과 -->

<!-- 페이징 -->
<div class="paging"><%=Pager.getList(request, pageNo, cfg.getInt("limit.page"), objVM.pages, "", "page", "", buttons)%></div>
<!-- # 페이징 -->

<%@ include file="../../footer.inc.jsp" %>

<style type="text/css">
	span.description {position:relative;}
	span.description span {position:absolute; left:-242px; top:20px; display:inline-block; width:200px; padding:10px; text-align:left; border-color:#397cc8; border-width:1px; border-style:solid; background-color:#f0f7ff;}
</style>
<script type="text/javascript">
	function _search(o) {
		o.submit();
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
	
	/* 210304 cdh 엑셀다운로드 추가 */
	function _excel() {
		var records = <%=(objVM.list == null ? 0 : objVM.list.size())%>;

		if (records > 65000) {
			window.alert('65,000 라인을 초과하는 데이터입니다.\n\n보다 상세한 검색 조건으로 검색 후 다운로드하세요.');
			return;
		}

		location.href = 'VMRunLogExcel.jsp?1=1<%=addParam%>';
	}
	/* 엑셀다운로드 추가 끝 */
</script>