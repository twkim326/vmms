<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.VM
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%>
<%
/**
 * /service/VMRun.jsp
 *
 * 자판기 운영정보 > 가동상태 > 목록
 *
 * 작성일 - 2011/04/03, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0301");

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
	long user_id = cfg.getLong("user.company");
	long company = StringEx.str2long(request.getParameter("company"), 0);
	long organ = StringEx.str2long(request.getParameter("organ"), 0);
	int pageNo = StringEx.str2int(request.getParameter("page"), 1);
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));
	
// 수집오류 단말기 체크
	String collectChk = request.getParameter("collectChk");


// URL에 더해질 파라미터
	//String addParam = Param.addParam(request, "page:company:organ:sField:sQuery");
	String addParam = Param.addParam(request, "page:company:organ:sField:sQuery:collectChk");

// 인스턴스 생성
	VM objVM = new VM(cfg);

// 메서드 호출
	String error = null;

// 수집오류 flag 초기화
	if ((collectChk == null) || (collectChk.equals(""))){
		collectChk = "N";
	}

	try {
		//error = objVM.getList(company, organ, null, pageNo, sField, sQuery);
		error = objVM.getList(company, organ, null, pageNo, sField, sQuery, collectChk);
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
	<span>자판기 가동상태 조회</span>
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
				<option value="A.TERMINAL_ID"<%=sField.equals("A.TERMINAL_ID") ? " selected" : ""%>>단말기 ID</option>
				<option value="A.PLACE"<%=sField.equals("A.PLACE") ? " selected" : ""%>>설치위치</option>
				<option value="A.MODEL"<%=sField.equals("A.MODEL") ? " selected" : ""%>>모델</option>
				<option value="C.NAME"<%=sField.equals("C.NAME") ? " selected" : ""%>>담당자명</option>
				<!-- 20200421 cdh 자판기코드 추가 -->
				<option value="A.CODE"<%=sField.equals("A.CODE") ? " selected" : "" %>>자판기코드</option>
				<option value="MST.BUSINESSNO"<%=sField.equals("MST.BUSINESSNO") ? " selected" : ""%>>사업자번호</option>
			</select>
			<input type="text" id="sQuery" class="txtInput" value="<%=Html.getText(sQuery)%>" />
		</td>
		<td rowspan="2" class="center last">
			<input type="button" value="" class="btnSearch" onclick="_search($('search'));" />
		</td>
	</tr>
	<tr>
		<th><span>소속/조직</span></th>
		<td>
			<select id="company" class="<%=(cfg.getLong("user.company") > 0 ? "disabled" : "")%> js-example-basic-single js-example-responsive" style="width: 20%" onchange="Company.organ(this.options[selectedIndex].value.evalJSON(), 0, 0);"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
				<option value='{"seq" : 0, "depth" : 0}'>- 소속</option>
			</select>
			<span id="organ">
				<select id="organ0" class="js-example-basic-single js-example-responsive" style="width: 18%" onchange="Company.organ($('company').value.evalJSON(), 1, this.options[selectedIndex].value);">
					<option value="-1">- 조직</option>
				</select>
			</span>
			<span>
				<%if(collectChk.equals("Y")){%> 
					<input id="collectChk" type="checkbox" name="chkflag" value="Y" size="23" checked>수집오류 단말기 확인
				<%}else { %>
					<input id="collectChk" type="checkbox" name="chkflag" value ="N" size="23">수집오류 단말기 확인
				<%}%>
			</span>
		</td>
	</tr>
</table>

<!-- 검색결과 -->
<div class="infoBar mt23">
	검색 결과 : <strong><%=StringEx.comma(objVM.records)%></strong>건
	<!-- 20180607 엑셀다운로드 추가 허승찬 변경 시작-->
	<input type="button" class="button2 excel_save_modify2" onclick="_excel();" value="엑셀저장" />
	<!-- 20180607 엑셀다운로드 추가 허승찬 변경 종료-->
</div>
<table cellpadding="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="50" />
		<col width="*" />
		<col width="*" />
		<!--col width="80" /-->
		<col width="80" />
		<col width="130" />
		<col width="*" />
		<!-- 20130308 자판기상태정보 변경 시작 -->
		<!--
			<col width="80" />
			<col width="45" />
			<col width="40" />
			<col width="60" />
			<col width="40" />
			<col width="60" />
		  -->
			<col width="*" />
			<col width="40" />
			<col width="60" />
			<col width="*" />	
		<!-- 20130308 자판기상태정보 변경 시작 -->
			<col width="130" />
			<col width="130" />	<!-- 20200421 최종거래일 추가 -->
	</colgroup>
	<thead>
		<tr>
			<th rowspan="2" nowrap>순번</th>
			<th rowspan="2" nowrap>소속</th>
			<th rowspan="2" nowrap>조직</th>
			<!--th rowspan="2">코드</th-->
			<th rowspan="2" nowrap>단말기 ID</th>
			<th rowspan="2" nowrap>모델</th>
			<th rowspan="2" nowrap>설치위치</th>
			<th rowspan="2" nowrap>담당자</th>
			<!-- 20130308 자판기상태정보 변경 시작 -->
			<!-- 
			<th colspan="4" nowrap>자판기 이상 상태</th>
			<th rowspan="2" nowrap>판매이상</th>
			 -->
			<th colspan="<%=((user_id == 0||user_id==337||user_id==1142||user_id==1216||user_id==1415||user_id==3||user_id==14) ? "3" : "2")%>" nowrap>자판기 이상 상태</th>  <!--20191017 scheo 주제어부 삭제 -->
			<!-- 20130308 자판기상태정보 변경 끝 -->
			<!-- 20200421 최종거래일 추가 -->
			<!-- 20201105 chae 휘닉스벤딩, 유비쿼터스커뮤니케이션 조건부 추가 유광권부장님 요청 -->
			<th rowspan="2" nowrap>최종거래일</th>
			<th rowspan="2" nowrap>수집시간</th>
			<% if (cfg.isAuth("D")) { %>
			<th rowspan="2" bgcolor="#f5f5dc">사업자번호</th>
			<th rowspan="2" bgcolor="#f5f5dc">BIZTYPE</th>
			<th rowspan="2" bgcolor="#f5f5dc"> 상호명</th>
			<% } %>
		</tr>
		<tr>
		<!-- 20130308 자판기상태정보 변경 시작 -->
		<!-- 
			<th nowrap>미등록</th>
		 -->
		 <!-- 20130308 자판기상태정보 변경 종료 -->
			<th nowrap>품절</th>
			<% if((user_id == 0||user_id==337||user_id==1142||user_id==1216||user_id==34||user_id==1415||user_id==3||user_id==14)){ %>
			<!-- 20191017 scheo 주제어부 삭제 -->
			<!-- 20191022 twkim 주제어부 일부 추가 김재유대리님 요청 , 20191204 롯데칠성 추가(34) 유광권 부장님 요청-->
			<!-- 20201105 chae 휘닉스벤딩, 유비쿼터스커뮤니케이션 조건부 추가 유광권부장님 요청 -->
			<th nowrap>주제어부</th>
			<% } %>
			<th nowrap>P/D</th>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objVM.list.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>" onclick="location.href = 'VMRunDetail.jsp?seq=<%=c.getLong("SEQ") + addParam%>';" style="cursor:pointer;">
			<td class="center number"><%=c.getLong("NO")%></td>
			<td class="left" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("COMPANY"), "-"))%></td>
			<td><%=Html.getText(StringEx.setDefaultValue(c.get("ORGAN"), "-"))%></td>
			<!--td class="center number"><%=Html.getText(c.get("CODE"))%></td-->
			<td class="center number"><%=Html.getText(c.get("TERMINAL_ID"))%></td>
			<td class="center"><%=Html.getText(StringEx.setDefaultValue(c.get("MODEL"), "-"))%></td>
			<td><%=Html.getText(c.get("PLACE"))%></td>
			<td class="center" nowrap><%=Html.getText(StringEx.setDefaultValue(c.get("USER_NAME"), "-"))%></td>
		<% if (c.get("IS_UPDATE").equals("N")) { %>
					<!-- 20130308 자판기상태정보 변경 시작 -->
					<!-- <td class="center" colspan="4"><img src="<%=cfg.get("imgDir")%>/web/icon_error.png" style="width:20px; height:20px;" alt="상태 정보 미수신" /></td> -->
			<td class="center" colspan="3"><img src="<%=cfg.get("imgDir")%>/web/status_delay.png" style="width:20px; height:20px;" alt="상태 정보 미수신" /></td>
						<!-- 20130308 자판기상태정보 변경 종료-->
		<% } else if (c.get("IS_EXPIRE").equals("Y")) { %>
					<!-- 20130308 자판기상태정보 변경 시작 -->
					<!-- <td class="center" colspan="4"><img src="<%=cfg.get("imgDir")%>/web/icon_error.png" style="width:20px; height:20px;" alt="상태 정보 수신 3시간 초과" /></td> -->
<!-- 					20131213 3시간에서 24시간으로 변경 -->
<%-- 				    <td class="center" colspan="3"><img src="<%=cfg.get("imgDir")%>/web/status_delay.png" style="width:20px; height:20px;" alt="상태 정보 수신 3시간 초과" /></td> --%>
				    <td class="center" colspan="<%=((user_id == 0||user_id==337||user_id==1142||user_id==1216||user_id==1415||user_id==3||user_id==14) ? "3" : "2")%>"><img src="<%=cfg.get("imgDir")%>/web/status_delay.png" style="width:20px; height:20px;" alt="상태 정보 수신 24시간 초과" /></td> <!-- 20191017 scheo 주제어부 삭제 --><!-- 20191022 twkim 주제어부 일부 추가 김재유대리님 요청 -->
						<!-- 20130308 자판기상태정보 변경 종료 -->
						<!-- 20201105 chae 휘닉스벤딩, 유비쿼터스커뮤니케이션 조건부 추가 유광권부장님 요청 -->		
			
		<% } else { %>
			<!-- 20130308 자판기상태정보 변경 시작 -->
		<!-- 
			<td class="center"><%=(c.get("IS_EMPTY_COL").equals("Y") ? "<img src='" + cfg.get("imgDir") + "/web/nonregistered.png' style='width:20px; height:20px;' alt='미등록 컬럼 존재' />" : "-")%></td>
		 -->
		 	<!-- 20130308 자판기상태정보 변경 종료 -->
		 	<!-- 20201105 chae 휘닉스벤딩, 유비쿼터스커뮤니케이션 조건부 추가 유광권부장님 요청 -->
			<td class="center"><%=(c.get("IS_SOLD_OUT").equals("Y") ? "<img src='" + cfg.get("imgDir") + "/web/icon_error.png' style='width:20px; height:20px;' alt='품절 컬럼 존재' />" : "-")%></td>
			<% if((user_id == 0||user_id==337||user_id==1142||user_id==1216||user_id==1415||user_id==3||user_id==14)){ %>			<!-- 20191022 twkim 주제어부 일부 추가 김재유대리님 요청 -->
				<td class="center"><%=(c.get("IS_CONTROL_ERROR").equals("Y") ? "<img src='" + cfg.get("imgDir") + "/web/icon_error.png' style='width:20px; height:20px;' alt='주제어부 이상' />" : "-")%></td><!-- 20191017 scheo 주제어부 삭제 -->
			<% } %>
			<td class="center"><%=(c.get("IS_PD_ERROR").equals("Y") ? "<img src='" + cfg.get("imgDir") + "/web/icon_error.png' style='width:20px; height:20px;' alt='P/D 이상' />" : "-")%></td>
		<% } %>
			<!-- 20130308 자판기상태정보 변경 시작 -->
			<!-- 
			<td class="center"><%=(c.getLong("EMPTY_COL_SELLING") > 0 ? "<img src='" + cfg.get("imgDir") + "/web/nonregistered.png' style='width:20px; height:20px;' alt='등록되지 않은 컬럼 판매' />" : "-")%></td>
			 -->
			<!-- 20130308 자판기상태정보 변경 종료-->
			<!-- 20200421 cdh 단말기 최종 거래일자 추가 시작 -->
			<td align="center" nowrap><%=Html.getText(c.get("FINAL_TRAN_DATE"))%></td>
			<!-- 20200421 cdh 단말기 최종 거래일자 추가 종료 -->
			<td align="center" nowrap><%=Html.getText(c.get("COLLECT_DATE"))%></td>
			<% if ( cfg.isAuth("D")) { %>
			<td class="center " style="background-color: beige" nowrap><%=Html.getText(c.get("BUSINESSNO"))%></td>
			<td class="center " style="background-color: beige" nowrap><%=Html.getText(c.get("BIZTYPE"))%></td>
			<td class="center " style="background-color: beige" nowrap><%=Html.getText(c.get("MERCHANTNAME"))%></td>
			<% } %>
		</tr>
<% } %>
<% if (objVM.list.size() == 0) { %>
		<% if (cfg.isAuth("D")) {%>
		<td colspan="15" align="center">등록된 자판기가 없습니다</td>
		<%}else {%>
		<td colspan="12" align="center">등록된 자판기가 없습니다</td>
		<%}%>
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
<input type="hidden" name="collectChk" value="" />
</form>

<script type="text/javascript">
	function _search(o) {
		//var obj = {company : $('company'), organ : null, sField : $('sField'), sQuery : $('sQuery')};
		var obj = {company : $('company'), organ : null, sField : $('sField'), sQuery : $('sQuery'), collectChk : $('collectChk')};
		var com = obj.company.options[obj.company.selectedIndex].value.evalJSON();
	
		for (var i = com.depth; i >= 0; i--) {
			obj.organ = $('organ' + i);

			if (obj.organ && obj.organ.selectedIndex > 0) {
				o.organ.value = obj.organ.options[obj.organ.selectedIndex].value;
				break;
			}
		}

		if (obj.sQuery.value != '' && obj.sField.selectedIndex == 0) {
			window.alert('검색 필드를 선택하세요.');
			return;
		} else if (obj.sQuery.value == '' && obj.sField.selectedIndex > 0) {
			window.alert('검색어를 입력하세요.');
			return;
		}
		
		/* 201013 수집오류 체크박스 체크여부 */
		if (com.seq != 0){
			if (document.getElementsByName("chkflag")[0].checked==true) {
				o.collectChk.value = 'Y';
			}else {
				o.collectChk.value = 'N';
			}
		}else {
			o.collectChk.value = 'N';
		}
		/* 201013 수집오류 체크박스 체크여부 끝 */

		o.company.value = com.seq;
		o.sField.value = (obj.sQuery.value == '' ? '' : obj.sField.options[obj.sField.selectedIndex].value);
		o.sQuery.value = obj.sQuery.value;
		o.submit();
	}

//	Event.observe(window, 'load', function (event) {
		Company.depth = <%=cfg.getInt("user.organ.depth")%>;
		Company.company(<%=(cfg.getLong("user.company") > 0 ? cfg.getLong("user.company") : company)%>, <%=(organ > 0 ? organ : cfg.getLong("user.organ"))%>);
//	}.bind(this));
		/*엑셀 다운로드 추가  20180607 허승찬*/
		function _excel() {
			var records = <%=(objVM.list == null ? 0 : objVM.list.size())%>;

			if (records > 65000) {
				window.alert('65,000 라인을 초과하는 데이터입니다.\n\n보다 상세한 검색 조건으로 검색 후 다운로드하세요.');
				return;
			}

			location.href = 'VMExcel.jsp?1=1<%=addParam%>';
		}
		/*엑셀 다운로드 추가 종료*/
</script>