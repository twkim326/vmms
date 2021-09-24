<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.Commission
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /system/SystemCommission.jsp
 *
 * 시스템 > 수수료 > 목록
 *
 * 작성일 - 2011/06/11, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0108");

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
	String card = StringEx.getKeyword(StringEx.charset(request.getParameter("card")));

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "page:company:organ:card");

// 인스턴스 생성
	Commission objCommission = new Commission(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objCommission.getList(pageNo, company, organ, card);
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
	<span>수수료 관리</span>
</div>

<table cellspacing="0" class="tableType02">
	<colgroup>
		<col width="110" />
		<col width="*"/>
		<col width="73" />
	</colgroup>
	<tr>
		<th><span>소속&amp;조직</span></th>
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
		<td rowspan="2" class="center last">
			<input type="button" value="" class="btnSearch" onclick="_search($('search'));" />
		</td>
	</tr>
	<tr>
		<th><span>매입사</span></th>
		<td class="pb4">
			<select id="card">
				<option value="">- 선택하세요</option>
		<% for (int i = 0; i < objCommission.card.size(); i++) { GeneralConfig c = (GeneralConfig) objCommission.card.get(i); %>
				<option value="<%=c.get("CODE")%>"<%=(c.get("CODE").equals(card) ? " selected" : "")%>><%=c.get("NAME")%></option>
		<% } %>
			</select>
		</td>
	</tr>
</table>

<!-- 검색결과 -->
<div class="infoBar mt23">
	검색 결과 : <strong><%=StringEx.comma(objCommission.records)%></strong>건
<% if (cfg.isAuth("I")) { %>
	<input type="button" class="button2" value="신규등록" onclick="_regist();" />
<% } %>
</div>
<table cellpadding="0" cellspacing="0" cellspacing="0" class="tableType01">
	<colgroup>
		<col width="50" />
		<col width="*" />
		<col width="*" />
		<col width="80" />
		<col width="80" />
		<col width="80" />
		<col width="80" />
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
		<col width="<%=(cfg.isAuth("U") && cfg.isAuth("D") ? "120" : "60")%>" />
	<% } %>
	</colgroup>
	<thead>
		<tr>
			<th nowrap>순번</th>
			<th nowrap>소속</th>
			<th nowrap>조직</th>
			<th nowrap>매입사</th>
			<th nowrap>수수료율</th>
			<th nowrap>시작일</th>
			<th nowrap>종료일</th>
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
			<th nowrap>관리</th>
	<% } %>
		</tr>
	</thead>
	<tbody>
<% for (int i = 0; i < objCommission.list.size(); i++) { GeneralConfig c = (GeneralConfig) objCommission.list.get(i); %>
		<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>">
			<td class="center number"><%=c.getLong("NO")%></td>
			<td class="center"><%=c.get("COMPANY")%></td>
			<td><%=organ(c.get("ORGAN"))%></td>
			<td class="center"><%=organ(c.get("PAY_CARD_NAME"))%></td>
			<td class="center number"><%=c.get("COMMISSION_RATE")%>%</td>
			<td class="center number"><%=StringEx.setDefaultValue(c.get("START_DATE"), "-")%></td>
			<td class="center number"><%=StringEx.setDefaultValue(c.get("END_DATE"), "-")%></td>
	<% if (cfg.isAuth("U") || cfg.isAuth("D")) { %>
			<td class="center">
		<% if (c.get("IS_ABLED_MODIFY").equals("Y")) { %>
			<% if (cfg.isAuth("U")) { %>
				<input type="button" class="button1" value="수정" onclick="_modify(<%=c.getLong("SEQ")%>);" />
			<% } %>
			<% if (cfg.isAuth("D")) { %>
				<input type="button" class="button1" value="삭제" onclick="_delete(<%=c.getLong("SEQ")%>);" />
			<% } %>
		<% } else { %>
			-
		<% } %>
			</td>
	<% } %>
		</tr>
<% } %>
<% if (objCommission.list.size() == 0) { %>
		<tr>
			<td colspan="<%=((cfg.isAuth("U") || cfg.isAuth("D")) ? "8" : "7")%>" align="center">등록된 내역이 없습니다</td>
		</tr>
<% } %>
	</tbody>
</table>
<!-- # 검색결과 -->

<!-- 페이징 -->
<div class="paging"><%=Pager.getList(request, pageNo, cfg.getInt("limit.page"), objCommission.pages, "", "page", "", buttons)%></div>
<!-- # 페이징 -->

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<form method="get" name="search" id="search">
<input type="hidden" name="company" value="" />
<input type="hidden" name="organ" value="" />
<input type="hidden" name="card" value="" />
</form>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/layer/js/iframe.js"></script>
<script type="text/javascript">
	function _search(o) {
		var obj = {
			company : $('company')
		,	organ : null
		,	card : $('card')
		};
		var com = obj.company.options[obj.company.selectedIndex].value.evalJSON();

		for (var i = com.depth; i >= 0; i--) {
			obj.organ = $('organ' + i);

			if (obj.organ && obj.organ.selectedIndex > 0) {
				o.organ.value = obj.organ.options[obj.organ.selectedIndex].value;
				break;
			}
		}

		o.company.value = com.seq;
		o.card.value = obj.card.options[obj.card.selectedIndex].value;
		o.submit();
	}

	function _regist() {
		new IFrame(510, 260, 'SystemCommissionRegist.jsp').open();
	}

	function _modify(n) {
		new IFrame(510, 260, 'SystemCommissionModify.jsp?seq=' + n).open();
	}

	function _delete(n) {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		} else if (!confirm('선택하신 내용을 삭제하시겠습니까?')) {
			return;
		}

		$('sbmsg').show();

		new Ajax.Request('SystemCommissionDelete.jsp', {
			asynchronous : true,
			parameters : {seq : n},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('삭제가 완료되었습니다.');
						location.reload();
					} else {
						window.alert(decodeURIComponentEx(e.message));
					}
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}

				$('sbmsg').hide();
			}.bind(this)
		});
	}

//	Event.observe(window, 'load', function (event) {
		Company.depth = <%=cfg.getInt("user.organ.depth")%>;
		Company.company(<%=(cfg.getLong("user.company") > 0 ? cfg.getLong("user.company") : company)%>, <%=(organ > 0 ? organ : cfg.getLong("user.organ"))%>);
//	}.bind(this));
</script>
<%!
	private String organ(String organ) {
		return StringEx.replace(StringEx.replace(Html.getText(StringEx.setDefaultValue(organ, "-")), "{", "<s>"), "}", "</s>");
	}
%>