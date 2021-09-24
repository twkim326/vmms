<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.VM
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceVMRegist.jsp
 *
 * 서비스 > 운영 자판기 > 등록
 *
 * 작성일 - 2011/04/04, 정원광
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
	if (!(cfg.isAuth("I") || cfg.isAuth("U"))) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "A");

// 전송된 데이터
	long seq = StringEx.str2long(request.getParameter("seq"), 0);

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "page:company:organ:sField:sQuery");

// 인스턴스 생성
	VM objVM = new VM(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		/*
		long company = cfg.get("user.operator").equals("Y") ? cfg.getLong("user.company") : StringEx.str2long(request.getParameter("company_"), 0);
		long organ =  cfg.get("user.operator").equals("Y") ? cfg.getLong("user.organ") : StringEx.str2long(request.getParameter("organ_"), 0);
		long user =  cfg.get("user.operator").equals("Y") ? cfg.getLong("user.seq") : StringEx.str2long(request.getParameter("user"), 0);
		*/
		long company = StringEx.str2long(request.getParameter("company_"), 0);
		long organ =   StringEx.str2long(request.getParameter("organ_"), 0);
		long user =  StringEx.str2long(request.getParameter("user"), 0);
		
		String code = StringEx.charset(request.getParameter("code"));
		String model = StringEx.charset(request.getParameter("model"));
		String terminal = StringEx.charset(request.getParameter("terminal"));
		String modem = StringEx.charset(request.getParameter("modem"));
		String place = StringEx.charset(request.getParameter("place"));
		String place_code = StringEx.charset(request.getParameter("place_code"));
		String place_no = StringEx.charset(request.getParameter("place_no"));
		int sgcnt = StringEx.str2int(request.getParameter("sgcnt"), 0);
		String strReflectFlag = StringEx.charset(request.getParameter("reflectflag"));
		String aspCharge = StringEx.charset(request.getParameter("aspCharge"));
		String placeMove = StringEx.charset(request.getParameter("placeMove"));

		int type = StringEx.str2int(request.getParameter("Regitype"));

		// 출입단말기 상태에 대한 상태값 추가.(2020.12.23 By Chae)
		String accessStatus = StringEx.charset(request.getParameter("accessStatus"));

		// UserID
		String UID = StringEx.charset(request.getParameter("UID"));
		if (company == 0 || user == 0 || StringEx.isEmpty(code) || StringEx.isEmpty(terminal) || StringEx.isEmpty(place)|| StringEx.isEmpty(strReflectFlag)) {
			out.print(Message.alert("필수 입력정보가 존재하지 않습니다.", 0, "parent", "try { parent._clear(); } catch (e) {}"));
			return;
		}
		
		if (!"Y".equals(aspCharge)) aspCharge = "N";

		try {
			//20120509 상품정보 반영플래그 추가
			//error = objVM.regist(request, seq, company, organ, user, code, terminal, model, place, modem, sgcnt);
			//error = objVM.regist(request, seq, company, organ, user, code, terminal, model, place, modem, sgcnt, strReflectFlag, aspCharge, placeMove);
			//error = objVM.regist(request, seq, company, organ, user, code, terminal, model, place, place_code, place_no, modem, sgcnt, strReflectFlag, aspCharge, placeMove, type);
			// 출입단말 상태 추가.(2020.12.23 By Chae)
			error = objVM.regist(request, seq, company, organ, user, code, terminal, model, place, place_code, place_no, modem, sgcnt, strReflectFlag, aspCharge, placeMove, type, accessStatus);
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null && StringEx.str2long(error) == 0) {
			out.print(Message.alert(error, 0, "parent", "try { parent._clear(); } catch (e) {}"));
			return;
		}

		out.print(Message.refresh("ServiceVM.jsp?seq=" + error + addParam, "설정하신 내용의 등록이 완료되었습니다.", "parent"));
		return;
	} else {
		try {
			error = objVM.regist(seq);
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null) {
			out.print(error);
			return;
		}
	}

// 운영 정보 또는 설치 위치가 오늘보다 미래인 케이스가 존재할 경우
	String confirm = "";

	if (!StringEx.isEmpty(objVM.data.get("LATEST_HISTORY_DATE")) && DateTime.getDifferDay(DateTime.date("yyyyMMdd"), objVM.data.get("LATEST_HISTORY_DATE")) > 0) {
		confirm = objVM.data.get("LATEST_HISTORY_DATE").substring(0, 4) + "년 " + objVM.data.get("LATEST_HISTORY_DATE").substring(4, 6) + "월 " + objVM.data.get("LATEST_HISTORY_DATE").substring(6, 8) + "일에 자판기 운영 정보가 적용될 예정입니다.";
	} else if (!StringEx.isEmpty(objVM.data.get("LATEST_PLACE_DATE")) && DateTime.getDifferDay(DateTime.date("yyyyMMdd"), objVM.data.get("LATEST_PLACE_DATE")) > 0) {
		confirm = objVM.data.get("LATEST_PLACE_DATE").substring(0, 4) + "년 " + objVM.data.get("LATEST_PLACE_DATE").substring(4, 6) + "월 " + objVM.data.get("LATEST_PLACE_DATE").substring(6, 8) + "일에 자판기 설치 위치가 적용될 예정입니다.";
	}
%>
<%@ include file="../../header.inc.jsp" %>

<div class="title">
	<span>운영 자판기 관리</span>
</div>
<input type="hidden" id="uid" value="<%=StringEx.charset(request.getParameter("UID"))%>"/>
<form method="post" name="save_" id="save_" action="<%=request.getRequestURI()%>?<%=request.getQueryString()%>" target="__save">
<table cellspacing="0" class="tableType03" id="regist">
	<colgroup>
		<col width="130" />
		<col width="350"/>
		<col width="130" />
		<col width="350"/>
	</colgroup>
<% if (cfg.get("user.operator").equals("Y")) { %>
<!-- 
	<tr>
		<th><span>소속</span></th>
		<td colspan="3"><%=objVM.data.get("COMPANY")%></td>
	</tr>
	<tr>
		<th><span>조직</span></th>
		<td colspan="3"><%=objVM.data.get("ORGAN")%></td>
	</tr>
	<tr>
		<th><span>담당자</span></th>
		<td><%=cfg.get("user.name")%></td>
		<th><span>자판기 코드</span></th>
		<td>
			<input type="text" name="code" id="code" value="<%=Html.getText(objVM.data.get("CODE"))%>" class="checkForm txtInput" maxlength="20" option='{"isMust" : true, "varType" : "ALNUM", "message" : "자판기 코드를 입력하세요."}' />
			<input type="button" value="검색" class="button1" onclick="_code(<%=cfg.getLong("user.company")%>, '&sField=A.CODE&sQuery=' + escape($('code').value));" />
		</td>
	</tr>
-->	
	<tr>
		<th><span>소속</span></th>
		<td colspan="3">
			<input type="hidden" name="company_" id="company_" value="<%=(seq > 0 ? objVM.data.getLong("COMPANY_SEQ") : cfg.getLong("user.company"))%>" />
			<select name="company" id="company" class="checkForm<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%> js-example-basic-single js-example-responsive" style="width: 20%" onchange="CompanyEX2.organ(this.options[selectedIndex].value.evalJSON(), 0, 0);CompanyEX2.organDup(this.options[selectedIndex].value.evalJSON(), 0, 0,'B');
				_vendor(this.options[selectedIndex].value.evalJSON().seq);_category(this.options[selectedIndex].value.evalJSON().seq, 0);
				this.form.company_.value = this.options[selectedIndex].value.evalJSON().seq;" option='{"isMust" : true, "message" : "소속을 선택하세요."}'<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
				<option value='{"seq" : 0, "depth" : 0}'>- 소속</option>
			</select>
		</td>
	</tr>
	<tr>
		<th><span>자판기 조직</span></th>
		<td colspan="3">
			<input type="hidden" name="organ_" id="organ_" class="checkForm" value="" option='{"isMust" : true, "message" : "조직을 선택하세요."}' />
			<span id="organ">
				<select id="organ0" class="checkForm js-example-basic-single js-example-responsive" onchange="CompanyEX2.organ($('company').value.evalJSON(), 1, this.options[selectedIndex].value);">
					<option value="-1">- 조직</option>
				</select>
			</span>
		</td>
	</tr>
<!-- 	
	<tr>
		<th><span>담당자</span></th>
		<td>
			<select name="user" id="user" class="checkForm" option='{"isMust" : true, "message" : "담당자를 선택하세요."}'>
				<option value="0">- 계정</option>
			</select>
		</td>
		<th><span>자판기 코드</span></th>
		<td>
			<input type="text" name="code" id="code" value="<%=Html.getText(objVM.data.get("CODE"))%>" class="checkForm txtInput" maxlength="20" option='{"isMust" : true, "varType" : "number", "message" : "자판기 코드를 입력하세요."}' />
			<input type="button" value="검색" class="button1" onclick="_code($('company').value.evalJSON().seq, '&sField=A.CODE&sQuery=' + escape($('code').value));" />
		</td>
	</tr>
 -->
 	<tr>
		<th><span>담당자</span></th>
		<td colspan="3">
			<input type="hidden" name="organDup_" id="organDup_" class="checkForm" value="" />
			<span id="organDup">
 				<select id="organDup0" class="checkForm" onchange="CompanyEX2.organDup($('company').value.evalJSON(), 1, this.options[selectedIndex].value,'B');">
					<option value="-1">- 조직</option>
				</select>
			</span>
			<select name="user" id="user" class="checkForm" option='{"isMust" : true, "message" : "담당자를 선택하세요."}'>
				<option value="0">- 계정</option>
			</select>
		</td>
	</tr>
	<tr>		
		<th><span>자판기 코드</span></th>
		<td colspan="3">
		<!--20120913 자판기 코드 문자가능 수정
			<input type="text" name="code" id="code" value="<%=Html.getText(objVM.data.get("CODE"))%>" class="checkForm txtInput" maxlength="20" option='{"isMust" : true, "varType" : "number", "message" : "자판기 코드를 입력하세요."}' />
		 -->
		 <!-- 
		 	<input type="text" name="code" id="code" value="<%=Html.getText(objVM.data.get("CODE"))%>" class="checkForm txtInput" maxlength="20" option='{"isMust" : true, "varType" : "ALNUM", "message" : "자판기 코드를 입력하세요."}' />
		  -->
			<input type="text" name="code" id="code" value="<%=Html.getText(objVM.data.get("CODE"))%>" class="checkForm txtInput" maxlength="20" option='{"isMust" : true, "message" : "자판기 코드를 입력하세요."}' />
			<input type="button" value="검색" class="button1" onclick="_code($('company').value.evalJSON().seq, '&sField=A.CODE&sQuery=' + escape($('code').value));" />
		</td>
	</tr>	
<% } else { %>
	<tr>
		<th><span>소속</span></th>
		<td colspan="3">
			<input type="hidden" name="company_" id="company_" value="<%=(seq > 0 ? objVM.data.getLong("COMPANY_SEQ") : cfg.getLong("user.company"))%>" />
			<select name="company" id="company" class="checkForm<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%> js-example-basic-single js-example-responsive" style="width: 20%" onchange="CompanyEX2.organ(this.options[selectedIndex].value.evalJSON(), 0, 0, '');CompanyEX2.organDup(this.options[selectedIndex].value.evalJSON(), 0, 0,'B');
				_vendor(this.options[selectedIndex].value.evalJSON().seq);_category(this.options[selectedIndex].value.evalJSON().seq, 0);
				this.form.company_.value = this.options[selectedIndex].value.evalJSON().seq;" option='{"isMust" : true, "message" : "소속을 선택하세요."}'<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
				<option value='{"seq" : 0, "depth" : 0}'>- 소속</option>
			</select>
		</td>
	</tr>
	<tr>
		<th><span>자판기 조직</span></th>
		<td colspan="3">
			<input type="hidden" name="organ_" id="organ_" class="checkForm" value="" option='{"isMust" : true, "message" : "조직을 선택하세요."}' />
			<span id="organ">
				<select id="organ0" class="checkForm js-example-basic-single js-example-responsive" style="width: 20%" onchange="CompanyEX2.organ($('company').value.evalJSON(), 1, this.options[selectedIndex].value, '');">
					<option value="-1">- 조직</option>
				</select>
			</span>
		</td>
	</tr>
<!-- 	
	<tr>
		<th><span>담당자</span></th>
		<td>
			<select name="user" id="user" class="checkForm" option='{"isMust" : true, "message" : "담당자를 선택하세요."}'>
				<option value="0">- 계정</option>
			</select>
		</td>
		<th><span>자판기 코드</span></th>
		<td>
			<input type="text" name="code" id="code" value="<%=Html.getText(objVM.data.get("CODE"))%>" class="checkForm txtInput" maxlength="20" option='{"isMust" : true, "varType" : "number', "message" : "자판기 코드를 입력하세요."}' />
			<input type="button" value="검색" class="button1" onclick="_code($('company').value.evalJSON().seq, '&sField=A.CODE&sQuery=' + escape($('code').value));" />
		</td>
	</tr>
 -->
 	<tr>
		<th><span>담당자</span></th>
		<td colspan="3">
			<input type="hidden" name="organDup_" id="organDup_" class="checkForm" value="" />
			<span id="organDup">
				<select id="organDup0" class="checkForm" onchange="CompanyEX2.organDup($('company').value.evalJSON(), 1, this.options[selectedIndex].value,'B');">
					<option value="-1">- 조직</option>
				</select>
			</span>
			<select name="user" id="user" class="checkForm" option='{"isMust" : true, "message" : "담당자를 선택하세요."}'>
				<option value="0">- 계정</option>
			</select>
		</td>
	</tr>
	<tr>		
		<th><span>자판기 코드</span></th>
		<td colspan="3">
		<!--20120913 자판기 코드 문자가능 수정		
			<input type="text" name="code" id="code" value="<%=Html.getText(objVM.data.get("CODE"))%>" class="checkForm txtInput" maxlength="20" option='{"isMust" : true, "varType" : "number", "message" : "자판기 코드를 입력하세요."}' />
		 -->
		 <!-- 
		 	<input type="text" name="code" id="code" value="<%=Html.getText(objVM.data.get("CODE"))%>" class="checkForm txtInput" maxlength="20" option='{"isMust" : true, "varType" : "ALNUM", "message" : "자판기 코드를 입력하세요."}' />
		  -->
			<input type="text" name="code" id="code" value="<%=Html.getText(objVM.data.get("CODE"))%>" class="checkForm txtInput" maxlength="20" option='{"isMust" : true, "message" : "자판기 코드를 입력하세요."}' />
			<input type="button" value="검색" class="button1" onclick="_code($('company').value.evalJSON().seq, '&sField=A.CODE&sQuery=' + escape($('code').value));" />
		</td>
	</tr>	
<% } %>
	<tr>
		<th><span>모델</span></th>
		<td><input type="text" name="model" id="model" value="<%=Html.getText(objVM.data.get("MODEL"))%>" class="checkForm txtInput" maxlength="20" /></td>
		<th><span>단말기 ID</span></th>
		<td><input type="text" name="terminal" id="terminal" value="<%=Html.getText(objVM.data.get("TERMINAL_ID"))%>" class="checkForm txtInput" maxlength="12" option='{"isMust" : true, "varType" : "number", "message" : "단말기 ID를 입력하세요."}' /></td>
	</tr>
	<tr>
		<th><span>모뎀 번호</span></th>
		<td><input type="text" name="modem" id="modem" value="<%=Html.getText(objVM.data.get("MODEM"))%>" class="checkForm txtInput" maxlength="20" /></td>
		<th><span>ASP 과금</span></th>
		<td><input type="radio" name="aspCharge" value="Y"<%="Y".equals(objVM.data.get("ASP_CHARGE")) ? " checked" : ""%>>예
			<input type="radio" name="aspCharge" value="N"<%=!"Y".equals(objVM.data.get("ASP_CHARGE")) ? " checked" : ""%>>아니오</td>
	</tr>
	<tr>
		<th><span>설치 위치</span></th>
		<td>
			<input type="text" name="place" id="place" value="<%=Html.getText(objVM.data.get("PLACE"))%>" class="checkForm txtInput" style="width:330px" maxlength="100" option='{"isMust" : true, "message" : "설치 위치를 입력하세요."}' onchange="_change_pace()" />
			<input type="hidden" name="place_org" id="place_org" value="<%=Html.getText(objVM.data.get("PLACE"))%>" />

		</td>
		<th><span id="placeMoveHeader" style="display:none">설치 위치 이동여부</span></th>
		<td>
			<span id="placeMoveBody" style="display:none">			
				<input type="radio" name="placeMove"  value="Y" />예 이동합니다.
				<input type="radio" name="placeMove"  value="N" />아니오 위치명만 수정합니다.
			</span>
		</td>
	</tr>
	<tr>
		<th><span>설치 위치 코드</span></th>
		<td><input type="text" name="place_code" id="place_code" value="<%=Html.getText(objVM.data.get("PLACE_CODE"))%>" class="checkForm txtInput" maxlength="20" /></td>
		<th><span>설치 위치 번호</span></th>
		<td><input type="text" name="place_no" id="place_no" value="<%=Html.getText(objVM.data.get("PLACE_NO"))%>" class="checkForm txtInput" maxlength="20" /></td>
	</tr>
	<!-- 비가동 상태 여부 체크 하는 로직 추가.-->
	<tr>
		<th <% if (objVM.error.size() == 0) { %> class="last"<% } %>><span>비가동 여부</span></th>
		<td <% if (objVM.error.size() == 0) { %> class="last"<% } %>>
			<span>
				<select name="accessStatus" id="accessStatus" class="checkForm">
					<option value="O" <% if("O".equals(objVM.data.get("ACCESS_STATUS"))){ %> selected <%}%> >-상태</option>
					<option value="C" <% if("C".equals(objVM.data.get("ACCESS_STATUS"))){ %> selected <%}%>>폐점</option>
					<option value="R" <% if("R".equals(objVM.data.get("ACCESS_STATUS"))){ %> selected <%}%>>휴점</option>
				</select>
			</span>
		</td>
		<th class="last"><span></span></th>
		<td class="last">
		</td>
	</tr>
<% if (objVM.error.size() > 0) { %>
	<tr>
		<th class="last"><span>판매 이상</span></th>
		<td class="last" colspan="3">
<%
		out.print("<strong style='display:block; color:#ff0000; height:17px; padding-top:3px;'>");

		for (int i = 0; i < objVM.error.size(); i++) {
			GeneralConfig c = (GeneralConfig) objVM.error.get(i);

			out.print((i > 0 ? "," : "") + c.get("COL_NO"));
		}

		out.print("번째 컬럼의 상품을 등록하세요.</strong>");
%>
		</td>
	</tr>
<% } %>
</table>

<% if (!StringEx.isEmpty(confirm)) { %>
<div style="text-align:right; margin-top:5px; font-weight:bold; color:#ff4500;">* <%=confirm%></div>
<% } %>

<div class="title mt18" style="position:relative;">
	<span>자판기 상품 등록</span>
</div>

<table cellspacing="0" class="tableType04">
	<tr>
		<td>
			<span>상품 검색</span>
			<select id="vendor">
				<option value="0">- 공급자</option>
		<% for (int i = 0; i < objVM.vendor.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.vendor.get(i); %>
				<option value="<%=c.getLong("SEQ")%>"><%=c.get("NAME")%></option>
		<% } %>				
			</select>
			<select id="maker" onchange="_category(<%=(cfg.get("user.operator").equals("Y") ? cfg.getLong("user.company") : "$('company').value.evalJSON().seq")%>, this.options[selectedIndex].value);">
				<option value="0">- 그룹군</option>
			</select>
			<select id="category">
				<option value="0">- 그룹</option>
			</select>
			<select id="sField">
				<option value="A.NAME">상품이름</option>
				<option value="A.CODE">상품코드</option>
				<option value="A.MEMO">상품설명</option>
				<option value="A.BAR_CODE">바코드</option>
			</select>
			<input type="text" id="sQuery" class="txtInput" />
			<input type="button" value="검색" class="button1" onclick="_product();" />
		</td>
	</tr>
	<tr>
	<%
		String strReflectFlag = objVM.data.get("REFLECT_FLAG");
		String strSelReflectFlag1 = "";
		String strSelReflectFlag2 = "";
	
		if(strReflectFlag.equals("N"))
		{
			strSelReflectFlag1 = "";
			strSelReflectFlag2 = "selected";
		}
		else			
		{
			strSelReflectFlag1 = "selected";
			strSelReflectFlag2 = "";			
		}
	%>
		<td>
			<span>기판매상품처리기준</span><select id="reflectflag" name="reflectflag" onchange="javascrit:changeleflect();">
<%-- 				<option value="Y" <%=strSelReflectFlag1%>>처리함</option> --%>
				<option value="N" <%=strSelReflectFlag2%> selected>처리안함</option>				
			</select>&nbsp;&nbsp;&nbsp;<input type="text" id="selreflectflagmsg" size='30' value="(상품별 통계 및 ERP 처리불가)" readonly style="border: 0px solid #d3d3d3;" />
		</td>
	</tr>	
	<tr>
		<td style="border-top:1px solid #ccc;">
			<div class="box">
				<ul id="product">
					<li><a class="none">상품을 검색하세요</a></li>
				</ul>
			</div>
			<div class="arr"></div>
			<div class="box" style="width:550px;">
				<input type="hidden" name="sgcnt" id="sgcnt" value="<%=objVM.product.size()%>" />
				<table cellpadding="0" cellspacing="0" id="sArea" style="<%=(objVM.product.size() == 0 ? "display:none;" : "")%>">
					<colgroup>
						<col width="50" />
						<col width="350"/>
						<col width="80"/>
						<col width="30" />
					</colgroup>
					<tbody id="sList">
					<tr>
						<th>칼럼번호</th>
						<th>상품</th>
						<th>가격</th>
						<th>삭제</th>
					</tr>
			<% for (int i = 0; i < objVM.product.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.product.get(i); %>
					<input type="hidden" name="seq<%=(i + 1)%>" id="seq<%=(i + 1)%>" value="<%=c.getLong("PRODUCT_SEQ")%>" />
					<tr>
						<td><input type="text" name="col<%=(i + 1)%>" id="col<%=(i + 1)%>" value="<%=c.getInt("COL_NO")%>" maxlength="4" class="input col" /></td> <%-- scheo --%>
						<td><input type="text" name="name<%=(i + 1)%>" id="name<%=(i + 1)%>" value="<%=Html.getText(c.get("NAME"))%>" class="input name" readonly /></td>
						<td nowrap><input type="text" name="price<%=(i + 1)%>" id="price<%=(i + 1)%>" value="<%=c.getInt("PRICE")%>" class="input price" readonly />원</td>
						<td><input type="checkbox" name="isDel<%=(i + 1)%>" id="isDel<%=(i + 1)%>" value="Y" class="checkbox" onclick="_sDel(<%=(i + 1)%>)" /></td>
					</tr>
			<% } %>
					</tbody>
				</table>
				<ul id="sNone" style="<%=(objVM.product.size() == 0 ? "" : "display:none;")%>">
					<li><a class="none">상품을 선택하세요</a></li>
				</ul>
			</div>
		</td>
	</tr>
</table>

<div class="buttonArea">
	<input type="button" value="" class="<%=(seq > 0 ? "btnModify" : "btnRegi")%>" onclick="_save();" />
	<input type="button" value="" class="btnList" onclick="location.href = 'ServiceVM.jsp?1=1<%=addParam%>';" />
</div>
</form>

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<iframe src="about:blank" name="__save" id="__save" style="display:none"></iframe>
<style type="text/css">
	div.box {width:300px; height:200px; padding:5px; overflow:auto; float:left; border-width:1px; border-style:solid; border-color:#ccc;}
	div.box ul li a {height:12px; line-height:14px; display:block; margin:0; padding:3px; border:1px solid #fff; cursor:pointer; overflow:hidden;}
	div.box ul li a:hover {border:1px solid #ff4500;}
	div.box ul li a.none {border-style:none; cursor:default; padding:80px 0 0 0; text-align:center;}
	div.box table th {font-weight:bold; font-size:11px; text-align:center;}
	div.box table td {height:18px; padding:5px 0 0 0; text-align:center;}
	div.box table td input.input {height:18px; line-height:150%; border:1px solid #ccc;}
	div.box table td input.col {width:35px; padding:0 4px; text-align:right;}
	div.box table td input.name {width:360px;padding:0 4px; }
	div.box table td input.price {width:55px;padding:0 4px; text-align:right; }
	div.arr { width:50px; height:210px; background:url(<%=cfg.get("imgDir")%>/web/icon_arr_right.gif) no-repeat center center; float:left; }
</style>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/layer/js/iframe.js"></script>
<script type="text/javascript">
	function _code(company, addParam) {
		if (company == 0) {
			window.alert('소속을 선택하세요.');
			return;
		}

		new IFrame(510, 580, 'ServiceVMMasterWin.jsp?company=' + company + addParam).open();
	}
	
	function _change_pace() {
		if ( $('place_org').value != "" && ($('place').value != $('place_org').value)) {
			 $('placeMoveBody').style.display = "block";
			 $('placeMoveHeader').style.display = "block";
		} else {
			 $('placeMoveBody').style.display = "none";	
			 $('placeMoveHeader').style.display = "none";	
		}
	}
	
	function _vendor(company) {
		var o = {vendor : $('vendor')};
		
		for (var i = o.vendor.length - 1; i > 0; i--) {
			o.vendor.options[i] = null;
		}
		
		new Ajax.Request('ServiceProductVendor.jsp', {
			asynchronous : true,
			parameters : {company : company},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'FAIL') {
						window.alert(e.message ? decodeURIComponentEx(e.message) : '공급자 정보를 가져오는데 실패하였습니다.');
						o.sbmsg.hide();
						return;
					}

					var s = o.vendor;
					
					e.data.each (function (data, i) {
						s.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);

					}.bind(this));
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}

				o.sbmsg.hide();
			}.bind(this)
		});
	}
	
	function _category(company, category, n) {
		var o = {maker : $('maker'), category : $('category'), product : $('product'), sbmsg : $('sbmsg')};

		for (var i = o.category.length - 1; i > 0; i--) {
			o.category.options[i] = null;
		}

		if (category == 0) {
			for (var i = o.maker.length - 1; i > 0; i--) {
				o.maker.options[i] = null;
			}
		}

		o.product.update('<li><a class="none">상품을 검색하세요</a></li>');
		o.sbmsg.show();

		new Ajax.Request('ServiceProductCategory.jsp', {
			asynchronous : true,
			parameters : {company : company, category : category},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'FAIL') {
						window.alert(e.message ? decodeURIComponentEx(e.message) : '그룹 정보를 가져오는데 실패하였습니다.');
						o.sbmsg.hide();
						return;
					}

					var s = o.category;

					if (category == 0) {
						s = o.maker;
					}

					e.data.each (function (data, i) {
						s.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);

						if (data.seq == n) {
							s.options[i + 1].selected = true;
						}
					}.bind(this));
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}

				o.sbmsg.hide();
			}.bind(this)
		});
	}

	function _product() {
		var o = {company : $('company_'), vendor : $('vendor'), maker : $('maker'), category : $('category'), sField : $('sField'), sQuery : $('sQuery'), product : $('product'), sbmsg : $('sbmsg')};

		if (o.sbmsg.visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		}
/*
		if (o.vendor.selectedIndex == 0) {			
			window.alert('공급자를 선택하세요.');
			o.vendor.focus();
			return;
		}
*/		
//		if (o.category.selectedIndex == 0) {
			/*
			if (o.maker.selectedIndex == 0) {
				window.alert('그룹군을 선택하세요.');
				o.maker.focus();
				return;
			}*/
			/* else if (o.sQuery.value == '') {
				window.alert('검색어를 입력하세요.');
				o.sQuery.focus();
				return;
			}*/
//		}

		if ($('company').selectedIndex == 0) {			
			window.alert('소속을 선택하세요.');
			$('company').focus();
			return;
		}
		
		o.product.update('');
		o.sbmsg.show();

		new Ajax.Request('ServiceVMProductList.jsp', {
			asynchronous : true,
			parameters : {
			        company : o.company.value
				,	vendor : o.vendor.value
				,	category : o.category.selectedIndex == 0 ? o.maker.options[o.maker.selectedIndex].value : o.category.options[o.category.selectedIndex].value
				,	sField : o.sField.options[o.sField.selectedIndex].value
				,	sQuery : encodeURIComponentEx(o.sQuery.value)
			},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'FAIL') {
						window.alert(e.message ? decodeURIComponentEx(e.message) : '상품 정보를 가져오는데 실패하였습니다.');
						o.sbmsg.hide();
						return;
					}

					e.data.each (function (data, i) {
						o.product.insert('<li><a onclick="_move(' + data.seq + ', \'' + decodeURIComponent(data.name) + '\', ' + data.price + ' );" title="' + decodeURIComponent(data.name) + ' | ' + data.price +  '원">' + data.code + ' | ' + decodeURIComponent(data.name) + '</a></li>');
					}.bind(this));

					if (e.data.length == 0) {
						o.product.update('<li><a class="none">등록된 상품이 없습니다</a></li>');
					}
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}

				o.sbmsg.hide();
			}.bind(this)
		});
	}

	function _move(seq, name, price) {
		var sgcnt = parseInt($('sgcnt').value) + 1;

		if (sgcnt > 99) {
			window.alert('운영 자판기 상품은 99개까지만 등록이 가능합니다.');
			return;
		}
		
		$('sgcnt').value = sgcnt;
		$('sNone').hide();
		$('sArea').show();
		$('sList').insert('<input type="hidden" name="seq' + sgcnt + '" id="seq' + sgcnt + '" value="' + seq + '" />'
			+ '<tr>'
			+ '<td><input type="text" name="col' + sgcnt + '" id="col' + sgcnt + '" maxlength="4" class="input col" /></td>'
			+ '<td><input type="text" name="name' + sgcnt + '" id="name' + sgcnt + '" value="' + name + '" class="input name" readonly /></td>'
			+ '<td><input type="text" name="price' + sgcnt + '" id="price' + sgcnt + '" value="' + price + '" class="input price" readonly />원</td>'
			+ '<td><input type="checkbox" name="isDel' + sgcnt + '" id="isDel' + sgcnt + '" value="Y" class="checkbox" onclick="_sDel(' + sgcnt + ')" /></td>'
			+ '</tr>');
	}

	function _sDel(i) {
		var o = {col : $('col' + i), name : $('name' + i), isDel : $('isDel' + i)};

		if (o.isDel.checked) {
			o.col.value = o.col.defaultValue;
			o.col.readOnly = true;
			o.col.addClassName('disabled');
			o.name.addClassName('disabled');
		} else {
			o.col.readOnly = false;
			o.col.removeClassName('disabled');
			o.name.removeClassName('disabled');
		}
	}

	function _save() {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		}

		for (var i = $('company').value.evalJSON().depth; i >= 0; i--) {
			var organ = $('organ' + i);

			if (organ && organ.selectedIndex > 0) {
				$('organ_').value = organ.options[organ.selectedIndex].value;
				break;
			}
		}
		
		var error = Common.checkForm($('regist'));
		var sgcnt = parseInt($('sgcnt').value);
		var check = new Array();
		var index = 0;

		if (error != '') {
			window.alert(error);
			return;
		}
		
		var placeMoveAnswer = $$('input:checked[type="radio"][name="placeMove"]').pluck('value');
		if ( $('place_org').value != "" && ($('place').value != $('place_org').value)) {
			if ( placeMoveAnswer == "") {
				window.alert('설치 위치 이동 여부를 선택하세요.');
				return;		
			}
		} else {
			placeMoveAnswer = "Y";	
		}
		
//		if (!$('organ_').value || $('organ_').value == '0') {
//			window.alert('조직을 선택하세요.');
//			return;
//		}

		for (var i = 1; i <= sgcnt; i++) {
			var o = {col : $('col' + i), isDel : $('isDel' + i)};

			if (o.isDel.checked) {
				continue;
			}

			if (!/^[0-9]+$/.test(o.col.value)) {
				window.alert(i + '번째 순번을 숫자로 입력하세요.');
				o.col.focus();
				return;
			} else if (Common.inArray(o.col.value, check)) {
				window.alert(i + '번째 순번이 이미 존재합니다.');
				o.col.focus();
				return;
			}

			check[index++] = o.col.value;
		}
		var confirmMessage = "";
		if ( placeMoveAnswer == "Y") {
			confirmMessage = '<%=(StringEx.isEmpty(confirm) ? "" : confirm + "\\n\\n")%>등록하시겠습니까?';
		} else {
			confirmMessage = '수정하시겠습니까?';	
		}
		
		if (!confirm(confirmMessage)) {
			return;
		}	

		$('sbmsg').show();
		$('save_').submit();
	}

	function _clear() {
		$('sbmsg').hide();
	}

	function changeleflect()
	{
		if($('reflectflag').value == 'N')
		{
			$('selreflectflagmsg').style.visibility="";
		}
		else
		{
			$('selreflectflagmsg').style.visibility="hidden";	
		}
	}

	changeleflect();

//	Event.observe(window, 'load', function (event) {
		
<% if (cfg.get("user.operator").equals("Y")) { %>
		_category(<%=cfg.getLong("user.company")%>, 0);

		CompanyEX2.depth = <%=cfg.getInt("user.organ.depth")%>;
		<%if (seq > 0) { %>		
		//CompanyEX2.company(<%=cfg.getLong("user.company")%>, <%=objVM.data.getLong("ORGANIZATION_SEQ")%>,<%=objVM.data.getLong("USER_ORG_SEQ")%>, {mode : 'B', user : <%=objVM.data.getLong("USER_SEQ")%>});
		CompanyEX2.company(<%=cfg.getLong("user.company")%>, <%=objVM.data.getLong("ORGANIZATION_SEQ")%>,<%=cfg.getLong("user.organ")%>, {mode : 'B', user : <%=objVM.data.getLong("USER_SEQ")%>});
		<%} else{ %>
		CompanyEX2.company(<%=cfg.getLong("user.company")%>, <%=cfg.getLong("user.organ")%>,<%=cfg.getLong("user.organ")%>, {mode : 'B', user : <%=cfg.getLong("user.seq")%>});
		<%}%>		
<% } else if (seq > 0) { %>
		_category(<%=objVM.data.getLong("COMPANY_SEQ")%>, 0);
		
		CompanyEX2.depth = <%=cfg.getInt("user.organ.depth")%>;
		CompanyEX2.company(<%=objVM.data.getLong("COMPANY_SEQ")%>, <%=objVM.data.getLong("ORGANIZATION_SEQ")%>,<%=objVM.data.getLong("USER_ORG_SEQ")%>, {mode : 'B', user : <%=objVM.data.getLong("USER_SEQ")%>});
		
		//Company.depth = <%=cfg.getInt("user.organ.depth")%>;
		//Company.company(<%=objVM.data.getLong("COMPANY_SEQ")%>, <%=objVM.data.getLong("ORGANIZATION_SEQ")%>, {mode : 'B', user : <%=objVM.data.getLong("USER_SEQ")%>});
<% } else { %>
		_category(<%=cfg.getLong("user.company")%>, 0);
		
		CompanyEX2.depth = <%=cfg.getInt("user.organ.depth")%>;
		CompanyEX2.company(<%=cfg.getLong("user.company")%>, <%=cfg.getLong("user.organ")%>, <%=cfg.getLong("user.organ")%>, {mode : 'B', user : <%=cfg.getLong("user.seq")%>});
		
		//Company.depth = <%=cfg.getInt("user.organ.depth")%>;
		//Company.company(<%=cfg.getLong("user.company")%>, <%=cfg.getLong("user.organ")%>, {mode : 'B', user : <%=cfg.getLong("user.seq")%>});
<% } %>
//	}.bind(this));
</script>