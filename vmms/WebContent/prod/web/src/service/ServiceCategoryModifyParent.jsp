<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Category
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceCategoryModifyParent.jsp
 *
 * 서비스 > 상품 카테고리 관리 > 상위 카테고리 수정
 *
 * 작성일 - 2011/04/20, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0204");

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

	if (seq == 0) {
		out.print("필수 입력정보가 존재하지 않습니다.");
		return;
	}

// 인스턴스 생성
	Category objCate = new Category(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		long cate = StringEx.str2long(request.getParameter("cate"), 0);

		if (cate == 0) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("필수 입력정보가 존재하지 않습니다.", "UTF-8") + "\"}");
			return;
		}

		try {
			error = objCate.modify(seq, cate);
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode(error.replaceAll("'", ""), "UTF-8") + "\"}");
			return;
		}

		out.print("{\"code\" : \"SUCCESS\", \"company_seq\" : \"" + objCate.data.getString("COMPANY_SEQ") + "\", \"parent_code\" : \"" + objCate.data.getString("CODE") + "\"}");
		return;
	} else {
		try {
			error = objCate.regist(seq);
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null) {
			out.print(error);
			return;
		}
	}
%>
<%@ include file="../../header.inc.jsp" %>

<div id="window">
	<div class="title">
		<span>상품 그룹관리</span>
	</div>

	<table cellspacing="0" class="tableType03 tableType05" id="regist">
		<colgroup>
			<col width="110" />
			<col width="*"/>
		</colgroup>
		<tr>
			<th><span>수정 전 그룹군</span></th>
			<td><%=objCate.data.get("NAME")%></td>
		</tr>
		<tr>
			<th><span>수정 후 그룹군</span></th>
			<td>
				<select name="cate" id="cate" class="checkForm" option='{"isMust" : true, "message" : "수정 후 그룹군을 선택하세요."}'>
					<option value='{"seq" : 0, "name" : ""}'>- 선택하세요</option>
			<% for (int i = 0; i < objCate.cate.size(); i++) { GeneralConfig c = (GeneralConfig) objCate.cate.get(i); %>
					<option value='{"seq" : <%=c.getLong("SEQ")%>, "name" : "<%=c.get("NAME").replaceAll("'", "")%>"}'<%=(c.getLong("SEQ") == objCate.data.getLong("PARENT_SEQ") ? " selected" : "")%>><%=c.get("NAME")%></option>
			<% } %>
				</select>
			</td>
		</tr>
	</table>

	<div class="buttonArea">
		<input type="button" value="" class="btnModifyS" onclick="_save();" />
		<input type="button" value="" class="btnCancelS" onclick="new parent.IFrame().close();" />
	</div>
</div>

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none; left:220px; top:50px;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script type="text/javascript">
	function _save() {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		}

		var error = Common.checkForm($('regist'));

		if (error != '') {
			window.alert(error);
			return;
		} else if (!confirm('입력하신 내용을 수정하시겠습니까?.')) {
			return;
		}

		var o = $('cate').value.evalJSON();

		$('sbmsg').show();

		new Ajax.Request(location.pathname, {
			asynchronous : false,
			parameters : {
				seq : <%=seq%>
			,	cate : o.seq
			},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();
					if (e.code == 'SUCCESS') {
						window.alert('수정이 완료되었습니다.');
						parent._cate(e.company_seq, o.seq, 1);
						parent._select(o.seq, o.name, e.parent_code, 1);
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