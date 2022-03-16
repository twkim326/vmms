<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Vendor
			, com.oreilly.servlet.MultipartRequest
			, com.oreilly.servlet.multipart.DefaultFileRenamePolicy
			, java.io.*
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceProductRegistBundle.jsp
 *
 * 서비스 > 상품 > 등록
 *
 * 작성일 - 2017/05/19, 황재원
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
	Vendor objVendor = new Vendor(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		MultipartRequest req = new MultipartRequest(request, cfg.get("data.aDir.temp"), 100 * 1024 * 1024, Common.CHARSET, new DefaultFileRenamePolicy());

		long company = StringEx.str2long(req.getParameter("_company"), 0);
		File excel = req.getFile("excel");

		try {
			error = objVendor.regist(company, excel);
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null) {
			out.print(Message.alert(error, 0, "parent", "try { parent._clear(); } catch (e) {}"));
			return;
		}

		out.print("<script language='javascript'>"
			+ "window.alert('심사현황 등록이 완료되었습니다.');"
			+ "top.location.reload();"
			+ "</script>");
		return;
	}
%>
<%@ include file="../header.inc.jsp" %>

<div id="window">
	<div class="title">
		<span>카드사 심사현황 등록</span>
		<a>* 는 필수 입력항목입니다.</a>
	</div>
	
	<form method="post" name="save_" id="save_" onsubmit="return _save();" target="__save" enctype="multipart/form-data">
	<input type="hidden" id="_company" name="_company"/>
	<table cellspacing="0" class="tableType03 tableType05" id="regist">
		<colgroup>
			<col width="*"/>
		</colgroup>
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
		<a href="<%=cfg.get("topDir")%>/common/src/down.jsp?src=card_regist.xls" style="color:#888; font-weight:bold; font-size:11px; position:absolute; right:10px; top:10px;">샘플보기</a>
	</div>
</div>

<%@ include file="../footer.inc.jsp" %>
<div id="sbmsg" style="display:none; left:220px; top:75px;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<iframe src="about:blank" name="__save" id="__save" style="display:none"></iframe>
<script type="text/javascript">


	function _save() {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return false;
		}

		var error = Common.checkForm($('regist'));

		if (error != '') {
			window.alert(error);
			return false;
		} else if (!confirm('입력하신 내용을 등록하시겠습니까?')) {
			return false;
		}

		$("_company").value = $('company').value;
		
		$('sbmsg').show();

		return true;
	}
	
	function _clear() {
		$('sbmsg').hide();
	}

//	Event.observe(window, 'load', function (event) {
		
//	}.bind(this));
</script>