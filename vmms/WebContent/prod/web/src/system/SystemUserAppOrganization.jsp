<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.User
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /system/SystemUserAppOrganization.jsp
 *
 * 시스템 > 계정 > 매출 조회 조직 추가
 *
 * 작성일 - 2011/03/29, 정원광
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
		out.print(Message.reload("top"));
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth("U")) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "B");

// 전송된 데이터
	long seq = StringEx.str2long(request.getParameter("seq"), 0);
	long company = StringEx.str2long(request.getParameter("company"), 0);

	if (seq == 0) {
		out.print("계정번호가 존재하지 않습니다.");
		return;
	} else if (company == 0) {
		out.print("소속정보가 존재하지 않습니다.");
		return;
	}

// 인스턴스 생성
	User objUser = new User(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objUser.appOrganList(company, 0, 0);
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

<div id="window">
	<div class="title">
		<span>계정관리</span>
	</div>

	<table cellspacing="0" class="tableType04 tableType05">
		<tr>
			<td id="organ">
				<select id="company" class="disabled" disabled>
					<option value='{"seq" : 0, "depth" : 0}'>- 소속</option>
			<% for (int i = 0; i < objUser.company.size(); i++) { GeneralConfig c = (GeneralConfig) objUser.company.get(i); %>
					<option value='{"seq" : <%=c.getLong("SEQ")%>, "depth" : <%=c.getLong("DEPTH")%>}'<%=(company == c.getLong("SEQ") ? " selected" : "")%>><%=c.get("NAME")%></option>
			<% } %>
				</select>
				<select id="organ0" onchange="_organ(<%=company%>, 1, this.options[selectedIndex].value)">
					<option value="-1">- <%=objUser.data.get("TITLE")%></option>
			<% for (int i = 0; i < objUser.organ.size(); i++) { GeneralConfig c = (GeneralConfig) objUser.organ.get(i); %>
					<option value="<%=c.getLong("SEQ")%>"><%=c.get("NAME")%></option>
			<% } %>
				</select>
			</td>
		</tr>
	</table>

	<div class="buttonArea">
		<input type="button" value="" class="btnRegiS" onclick="_save();" />
		<input type="button" value="" class="btnCancelS" onclick="new parent.IFrame().close();" />
	</div>
</div>

<%@ include file="../../footer.inc.jsp" %>

<script type="text/javascript">
	function _organ(company, depth, organ) {
		var o = {organ : $('organ'), change : $('organ' + depth)};

		o.organ.getElementsBySelector('select').findAll(function (s) {
			var _depth = parseInt(s.id.substr(o.organ.id.length, s.id.length));

			if (_depth > depth) {
				s.replace('');
			}
		}.bind(this));

		if (organ == '-1') {
			if (o.change) {
				o.change.replace('');
			}

			return;
		}

		if (o.change) { // 다음 단계의 셀렉트 박스가 있을 경우
			o.change.options[0].text = '- loading';

			for (var i = o.change.length - 1; i > 0; i--) {
				o.change.options[i] = null;
			}
		} else {
			o.organ.insert('<select id="' + o.organ.id + depth + '"><option value="-1">- loading</option></select> ');
		}

		new Ajax.Request('SystemUserAppOrganizationDetail.jsp', {
			method : "get",
			asynchronous : true,
			parameters : {seq : <%=seq%>, company : company, organ : organ, depth : depth},
			onSuccess : function (xmlHttp) {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == 'FAIL') {
					window.alert('조직 정보를 가져오는데 실패하였습니다.');
					return;
				}

				if (!o.change) {
					o.change = $(o.organ.id + depth);

					if (e.disp == '') {
						o.change.replace('');
						return;
					}

					o.change.observe('change', function (event) {
						_organ(company, depth + 1, o.change.options[o.change.selectedIndex].value);
					}.bind(this));
				}

				e.data.each (function (data, i) {
					o.change.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.seq);
				}.bind(this));

				o.change.options[0].text = '- ' + (e.disp ? decodeURIComponentEx(e.disp) : '조직');
			}.bind(this)
		});
	}

	function _save() {
		var obj = {company : $('company'), organ : null};
		var com = {seq : 0, depth : 0};
		var org = {seq : 0, name : ''};

		if (obj.company.selectedIndex > 0) {
			com = obj.company.options[obj.company.selectedIndex].value.evalJSON();

			for (var i = com.depth; i >= 0; i--) {
				obj.organ = $('organ' + i);

				if (obj.organ && obj.organ.selectedIndex > 0) {
					org.seq = obj.organ.options[obj.organ.selectedIndex].value;
					break;
				}
			}

			for (var i = 0; i <= com.depth; i++) {
				obj.organ = $('organ' + i);

				if (obj.organ && obj.organ.selectedIndex > 0) {
					org.name += (org.name == '' ? '' : '/') + obj.organ.options[obj.organ.selectedIndex].text;
				}
			}
		}

		if (com.seq == 0) {
			window.alert('소속을 선택하세요.');
			return;
		} else if (org.seq == 0) {
			window.alert('조직을 선택하세요.');
			return;
		} else if (!confirm('선택하신 조직을 추가하시겠습니까?')) {
			return;
		}

		new Ajax.Request('SystemUserAppOrganizationRegist.jsp', {
			asynchronous : false,
			parameters : {seq : <%=seq%>, organ : org.seq},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('선택하신 조직의 추가가 완료되었습니다.');
						parent._addAppOrgan(<%=seq%>, org.seq, org.name);
						new parent.IFrame().close();
					} else {
						window.alert(decodeURIComponentEx(e.message));
					}
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}
			}.bind(this)
		});
	}
</script>