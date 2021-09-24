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
 * /system/SystemUser.jsp
 *
 * 시스템 > 계정 > 목록
 *
 * 작성일 - 2011/03/21, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0101");

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
		error = objUser.getList(pageNo, company, organ, auth, sField, sQuery);
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
	<span>계정관리</span>
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
			<select id="company" class="js-example-basic-single js-example-responsive" style="width: 20%" class="<%=(cfg.getLong("user.company") > 0 ? "disabled" : "")%>" onchange="Company.organ(this.options[selectedIndex].value.evalJSON(), 0, 0, 'A');"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
				<option value='{"seq" : 0, "depth" : 0}'>- 소속</option>
			</select>
			<span id="organ">
				<select id="organ0" class="js-example-basic-single js-example-responsive" style="width: 18%" onchange="Company.organ($('company').value.evalJSON(), 1, this.options[selectedIndex].value, 'A');">
					<option value="-1">- 조직</option>
				</select>
			</span>
		</td>
	</tr>
	<tr>
		<th><span>등급</span></th>
		<td class="pb4">
			<select id="auth">
				<option value="-1">- 권한</option>
			</select>
		</td>
	</tr>
</table>

<!-- 검색결과 -->
<div class="infoBar mt23">
	검색 결과 : <strong><%=StringEx.comma(objUser.records)%></strong>건
	<div>
	<% if (cfg.getLong("user.company") == 0 || cfg.getLong("user.company") == 264) { %>
		<input type="button" class="button2 excel_save_modify" value="운영자등록" onclick="_regist_operator();" />
<% } %>	
<% if (cfg.isAuth("I")) { %>
		<input type="button" class="button2" value="계정등록" onclick="location.href = 'SystemUserRegist.jsp?1=1<%=addParam%>';" />
<% } %>
	</div>
</div>
<table cellpadding="0" cellspacing="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="50" />
		<col width="80" />
		<col width="180" />
		<col width="*" />
		<col width="150" />
		<col width="90" />
		<col width="100" />
		<col width="80" />
		<col width="80" />
		<col width="80" />
		<col width="80" />
		<col width="80" />
	</colgroup>
	<thead>
		<tr>
			<th rowspan="2" nowrap>순번</th>
			<th rowspan="2" nowrap>등급</th>
			<th rowspan="2" nowrap>소속</th>
			<th rowspan="2" nowrap>조직</th>
			<th rowspan="2" nowrap>이름</th>
			<th rowspan="2" nowrap>아이디</th>
			<th rowspan="2" nowrap>휴대전화</th>
			<th colspan="3" nowrap>SMS 수신</th>
			<th rowspan="2" nowrap>알림톡 수신</th>
			<th rowspan="2" nowrap>등록일</th>
		</tr>
		<tr>
			<th nowrap>품절정보</th>
			<th nowrap>단말기상태 </th>
			<th nowrap>마감정보 </th>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objUser.list.size(); i++) { GeneralConfig c = (GeneralConfig) objUser.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>" onclick="location.href = 'SystemUserDetail.jsp?seq=<%=c.getLong("SEQ") + addParam%>';" style="cursor:pointer;">
			<td class="center number"><%=c.getLong("NO")%></td>
			<td class="center"><%=Html.getText(StringEx.setDefaultValue(c.get("AUTH"), "-"))%></td>
			<td><%=Html.getText(StringEx.setDefaultValue(c.get("COMPANY"), "-"))%></td>
			<td><%=organ(c.get("ORGAN"))%></td>
			<td class="center"><%=Html.getText(c.get("NAME"))%></td>
			<td class="center"><%=Html.getText(c.get("ID"))%></td>
			<td class="center number"><%=Html.getText(c.get("CELLPHONE"))%></td>
			<td class="center number"><%=(Html.getText(c.get("IS_SMS_SOLDOUT")).equals("Y") ? "수신중" : "미수신")%></td>
			<td class="center number"><%=(Html.getText(c.get("IS_SMS_STATE")).equals("Y") ? "수신중" : "미수신")%></td>
			<td class="center number"><%=(Html.getText(c.get("IS_SMS_CLOSING")).equals("Y") ? "수신중" : "미수신")%></td>
			<%--2020-12-04 김태우 추가--%>
			<td class="center number"><%=(Html.getText(c.get("IS_KAKAO_STATUS")).equals("Y") ? "수신중" : "미수신")%></td>
			<td class="center number"><%=Html.getText(c.get("CREATE_DATE"))%></td>
		</tr>
<% } %>
<% if (objUser.list.size() == 0) { %>
		<tr>
			<td colspan="10" align="center">등록된 계정이 없습니다</td>
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
	function _regist_operator() {
		new IFrame(510, 270, 'SystemUserVMOperatorRegist.jsp?1=1<%=addParam%>').open();
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