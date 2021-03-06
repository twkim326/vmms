<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.User
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /system/SystemUserVMOperator.jsp
 *
 * 시스템 > 계정 > 목록
 *
 * 작성일 - 2017/06/14, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0110");

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
	long company = StringEx.str2long(request.getParameter("company"), 0);
	long organ = StringEx.str2long(request.getParameter("organ"), 0);
	long auth = StringEx.str2long(request.getParameter("auth"), 0);
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "page:company:organ:auth:sField:sQuery");

// 인스턴스 생성
	User objUser = new User(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objUser.getVMOperatorList(pageNo, company, organ, sField, sQuery);
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
	<span>운영자관리</span>
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
				<option value="A.ID"<%=sField.equals("A.ID") ? " selected" : ""%>>아이디</option>
				<option value="A.NAME"<%=sField.equals("A.NAME") ? " selected" : ""%>>이름</option>
			</select>
			<input type="text" id="sQuery" class="txtInput" value="<%=Html.getText(sQuery)%>" />
		</td>
		<td rowspan="3" class="center last">
			<input type="button" value="" class="btnSearch" onclick="_search($('search'));" />
		</td>
	</tr>
	<tr>
		<th><span>소속/조직</span></th>
		<td>
			<select id="company" class="<%=(cfg.getLong("user.company") > 0 ? "disabled" : "")%>" onchange="Company.organ(this.options[selectedIndex].value.evalJSON(), 0, 0, 'A');"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
				<option value='{"seq" : 0, "depth" : 0}'>- 소속</option>
			</select>
			<span id="organ">
				<select id="organ0" onchange="Company.organ($('company').value.evalJSON(), 1, this.options[selectedIndex].value, 'A');">
					<option value="-1">- 조직</option>
				</select>
			</span>
		</td>
	</tr>
	 
	<tr style="display:none">
		<th><span>등급</span></th>
		<td class="pb4">
			<select id="auth">
				<option value="-1">- 권한</option>
			</select>
		</td>
	</tr>
	 <!-- -->
</table>

<!-- 검색결과 -->
<div class="infoBar mt23">
	검색 결과 : <strong><%=StringEx.comma(objUser.records)%></strong>건
<% if (cfg.isAuth("I")) { %>
	<input type="button" class="button1" value="등록" onclick="_regist();" />
<% } %>
</div>
<table cellpadding="0" cellspacing="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="50" />
		<col width="180" />
		<col width="*" />
		<col width="120" />
		<col width="120" />
		<col width="120" />
		<col width="80" />
	</colgroup>
	<thead>
		<tr>
			<th nowrap>순번</th>
			<th nowrap>소속</th>
			<th nowrap>조직</th>
			<th nowrap>이름</th>
			<th nowrap>아이디</th>
			<th nowrap>사번</th>
			<th nowrap>등록일</th>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objUser.list.size(); i++) { GeneralConfig c = (GeneralConfig) objUser.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>" onclick="location.href = 'SystemUserDetail.jsp?seq=<%=c.getLong("SEQ") + addParam%>';" style="cursor:pointer;">
			<td class="center number"><%=c.getLong("NO")%></td>
			<td><%=Html.getText(StringEx.setDefaultValue(c.get("COMPANY"), "-"))%></td>
			<td><%=organ(c.get("ORGAN"))%></td>
			<td class="center"><%=Html.getText(c.get("NAME"))%></td>
			<td class="center"><%=Html.getText(c.get("ID"))%></td>
			<td class="center"><%=Html.getText(c.get("EMPLOYEE_NO"))%></td>
			<td class="center number"><%=Html.getText(c.get("CREATE_DATE"))%></td>
		</tr>
<% } %>
<% if (objUser.list.size() == 0) { %>
		<tr>
			<td colspan="7" align="center">등록된 운영자가 없습니다</td>
		</tr>
<% } %>
	</tbody>
</table>
<!-- # 검색결과 -->

<!-- 페이징 -->
<div class="paging"><%=Pager.getList(request, pageNo, cfg.getInt("limit.page"), objUser.pages, "", "page", "", buttons)%></div>
<!-- # 페이징 -->

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/layer/js/iframe.js"></script>
<form method="get" name="search" id="search">
<input type="hidden" name="company" value="" />
<input type="hidden" name="organ" value="" />
<input type="hidden" name="auth" value="" />
<input type="hidden" name="sField" value="" />
<input type="hidden" name="sQuery" value="" />
</form>
<script type="text/javascript">
	function _search(o) {
		var obj = {
			company : $('company')
		,	organ : null
		,	auth : $('auth')
		,	sField : $('sField')
		,	sQuery : $('sQuery')
		};
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

		o.company.value = com.seq;
		o.auth.value = obj.auth.options[obj.auth.selectedIndex].value;
		o.sField.value = (obj.sQuery.value == '' ? '' : obj.sField.options[obj.sField.selectedIndex].value);
		o.sQuery.value = obj.sQuery.value;
		o.submit();
	}
	function _regist() {
		new IFrame(510, 320, 'SystemUserVMOperatorRegist.jsp?1=1<%=addParam%>').open();
	}

//	Event.observe(window, 'load', function (event) {
		Company.depth = <%=cfg.getInt("user.organ.depth")%>;
		Company.company(<%=(cfg.getLong("user.company") > 0 ? cfg.getLong("user.company") : company)%>, <%=(organ > 0 ? organ : cfg.getLong("user.organ"))%>, {mode : 'A', auth : <%=auth%>});
//	}.bind(this));
</script>
<%!
	private String organ(String organ) {
		return StringEx.replace(StringEx.replace(Html.getText(StringEx.setDefaultValue(organ, "-")), "{", "<s>"), "}", "</s>");
	}
%>