<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Goods
			, com.oreilly.servlet.MultipartRequest
			, com.oreilly.servlet.multipart.DefaultFileRenamePolicy
			, java.io.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceGoodsRegistBundle.jsp
 *
 * 서비스 > 상품 > 등록 > 일괄 등록
 *
 * 작성일 - 2011/04/02, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0205");

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
	Goods objGoods = new Goods(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		MultipartRequest req = new MultipartRequest(request, cfg.get("data.aDir.temp"), 100 * 1024 * 1024, Common.CHARSET, new DefaultFileRenamePolicy());

		String group = req.getParameter("group");
		File excel = req.getFile("excel");

		try {
			error = objGoods.regist(group, excel);
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
			error = objGoods.regist();
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
		<span>상품 관리</span>
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
				<select name="company" id="company" class="checkForm<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>" onchange="_group(this.options[selectedIndex].value, 0);"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
					<option value="0">- 선택하세요</option>
			<% for (int i = 0; i < objGoods.company.size(); i++) { GeneralConfig c = (GeneralConfig) objGoods.company.get(i); %>
					<option value="<%=c.getLong("SEQ")%>"<%=(c.getLong("SEQ") == cfg.getLong("user.company") ? " selected" : "")%>><%=c.get("NAME")%></option>
			<% } %>
				</select>
			</td>
		</tr>
		<tr>
			<th><span>그룹군</span></th>
			<td>
				<select name="group" id="group" class="checkForm" option='{"isMust" : true, "message" : "그룹군을 선택하세요."}'>
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
		<a href="<%=cfg.get("topDir")%>/common/src/down.jsp?src=goods.xls" style="color:#888; font-weight:bold; font-size:11px; position:absolute; right:10px; top:10px;">샘플보기</a>
	</div>
</div>

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none; left:220px; top:75px;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<iframe src="about:blank" name="__save" id="__save" style="display:none"></iframe>
<script type="text/javascript">
	function _group(company) {
		var o = {company : $('company'), group : $('group')};

		for (var i = o.group.length - 1; i > 0; i--) {
			o.group.options[i] = null;
		}

		o.group.options[0].text = '- loading';

		new Ajax.Request('ServiceGoodsGroup.jsp', {
			asynchronous : false,
			parameters : {company : company, group : 0},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'FAIL') {
						window.alert(e.message ? decodeURIComponentEx(e.message) : '그룹 정보를 가져오는데 실패하였습니다.');
						return;
					}

					e.data.each (function (data, i) {
						o.group.options[i + 1] = new Option(decodeURIComponentEx(data.name), data.code);
					}.bind(this));

					o.group.options[0].text = '- 선택하세요.';
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}
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
		_group(0);
//	}.bind(this));
</script>