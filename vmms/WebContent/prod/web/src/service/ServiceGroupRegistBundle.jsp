<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Group
			, com.oreilly.servlet.MultipartRequest
			, com.oreilly.servlet.multipart.DefaultFileRenamePolicy
			, java.io.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceGroupRegistBundle.jsp
 *
 * 서비스 > 그룹 > 등록 > 일괄 등록
 *
 * 작성일 - 2011/03/31, 정원광
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
		out.print("로그인이 필요합니다.");
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth("I")) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "B");

// 인스턴스 생성
	Group objGroup = new Group(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		MultipartRequest req = new MultipartRequest(request, cfg.get("data.aDir.temp"), 100 * 1024 * 1024, Common.CHARSET, new DefaultFileRenamePolicy());

		String cate = req.getParameter("cate");
		File excel = req.getFile("excel");

		try {
			error = objGroup.regist(cate, excel);
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null) {
			out.print(Message.alert(error, 0, "parent", "try { parent._clear(); } catch (e) {}"));
			return;
		}

		out.print("<script language='javascript'>"
			+ "window.alert('일괄등록이 완료되었습니다.');"
			+ "top.location.reload();"
			+ "</script>");
		return;
	} else {
		try {
			error = objGroup.regist(0, 1);
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
		<span>상품 그룹 관리</span>
	</div>

	<form method="post" name="save_" id="save_" onsubmit="return _save();" target="__save" enctype="multipart/form-data">
	<table cellspacing="0" class="tableType03 tableType05" id="regist">
		<colgroup>
			<col width="110" />
			<col width="*"/>
		</colgroup>
		<tr>
			<th><span>소속</span></th>
			<td>
				<select name="company" id="company" class="checkForm<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>" onchange="_cate();"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
					<option value="0">- 선택하세요</option>
			<% for (int i = 0; i < objGroup.company.size(); i++) { GeneralConfig c = (GeneralConfig) objGroup.company.get(i); %>
					<option value="<%=c.getLong("SEQ")%>"<%=(c.getLong("SEQ") == cfg.getLong("user.company") ? " selected" : "")%>><%=c.get("NAME")%></option>
			<% } %>
				</select>
			</td>
		</tr>
		<tr>
			<th><span>그룹군</span></th>
			<td>
				<select name="cate" id="cate" class="checkForm" option='{"isMust" : true, "message" : "그룹군을 선택하세요."}'>
					<option value="">- 선택하세요</option>
				</select>
			</td>
		</tr>
		<tr>
			<th class="last"><span>엑셀</span></th>
			<td class="last"><input type="file" name="excel" id="excel" class="checkForm txtInput txtInput2" option='{"isMust" : true, "message" : "일괄 등록할 엑셀 파일을 업로드하세요."}' /></td>
		</tr>
	</table>

	<div class="buttonArea">
		<input type="submit" value="" class="btnRegiS" />
		<input type="button" value="" class="btnCancelS" onclick="new parent.IFrame().close();" />
	</div>
	</form>

	<div style="border:1px solid #ddd; padding:10px; color:#888; font-size:11px; margin-top:15px; position:relative;">
		* 반드시 정해진 형식에 맞는 엑셀을 업로드하세요.
		<a href="<%=cfg.get("topDir")%>/common/src/down.jsp?src=group.xls" style="color:#888; font-weight:bold; font-size:11px; position:absolute; right:10px; top:10px;">샘플보기</a>
	</div>
</div>

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none; left:220px; top:75px;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<iframe src="about:blank" name="__save" id="__save" style="display:none"></iframe>
<script type="text/javascript">
	function _cate() {
		var o = {company : $('company'), cate : $('cate')};

		if (!o.company || !o.cate) {
			return;
		}

		for (var i = o.cate.options.length - 1; i > 0; i--) {
			o.cate.options[i] = null;
		}

		o.cate.options[0].text = '- loading';

		new Ajax.Request('ServiceGroupTop.jsp', {
			asynchronous : true,
			parameters : {company : o.company.options[o.company.selectedIndex].value},
			onSuccess : function (xmlHttp) {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == 'FAIL') {
					window.alert(e.message ? decodeURIComponentEx(e.message) : '그룹군을 가져오는데 실패하였습니다.');
					return;
				}

				e.data.each (function (data, i) {
					o.cate.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.code);
				}.bind(this));

				o.cate.options[0].text = '- 선택하세요';
			}.bind(this)
		});
	}

	function _save() {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		}

		var error = Common.checkForm($('regist'));

		if (error != '') {
			window.alert(error);
			return false;
		} else if (!confirm('입력하신 내용을 등록하시겠습니까?')) {
			return false;
		}

		$('sbmsg').show();

		return true;
	}

	function _clear() {
		$('sbmsg').hide();
	}

//	Event.observe(window, 'load', function (event) {
		_cate();
//	}.bind(this));
</script>