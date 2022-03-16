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
 * /system/SystemUserLogin.jsp
 *
 * 시스템 > 계정 > 로그인
 *
 * 작성일 - 2011/03/21, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session);

// 이미 로그인한 사용자인 경우
	if (cfg.isLogin()) {
		response.sendRedirect(cfg.get("script.index"));
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "B");

// 이동할 경로
//	String goUrl = StringEx.setDefaultValue(StringEx.charset(request.getParameter("goUrl")), cfg.get("topDir") + "/");
	String goUrl = cfg.get("script.index");

// 인스턴스 생성
	User objUser = new User(cfg);

// 로그인 처리
	if (request.getMethod().equals("POST")) {
		String id = StringEx.charset(request.getParameter("id"));
		String pass = StringEx.charset(request.getParameter("pass"));

		if (StringEx.isEmpty(id)) {
			out.print(Message.alert("아이디를 입력하세요."));
			return;
		} else if (StringEx.isEmpty(pass)) {
			out.print(Message.alert("비밀번호를 입력하세요."));
			return;
		}

		String error = objUser.login(response, id, pass, request.getRemoteAddr());

		if (!StringEx.isEmpty(error)) {
			out.print(Message.alert(error));
			return;
		}

		response.sendRedirect(goUrl);
		return;
	}
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="ko" lang="ko">
<head>
	<title><%=cfg.get("title")%></title>
	<meta http-equiv="imagetoolbar" content="no" />
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<link rel="stylesheet" type="text/css" href="<%=cfg.get("imgDir")%>/css/login.css">
	<link rel="shortcut icon" href="/image/web/favicon_ubcn.png">		
	<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/js/prototype/prototype-1.6.0.3.js"></script>
	<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/js/scriptaculous/1.8.3/scriptaculous.js"></script>
	<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/js/Common.js"></script>
	<script type="text/javascript" src="<%=cfg.get("svcDir")%>/js/SvrLink.js"></script>
</head>
<body>
<div id="wrap">
	<div id="login">
	<div style="margin-bottom: 5%;">
    <ul>
        <li style="float: left; width: 50%;"><img src="<%=cfg.get("imgDir")%>/web/login/systemLogo.gif" alt="VMMS | Vending Machine Management System" /></li>
        <li style="float: right; width: 48%;">
        	<div style="background: #f5f5f5; border: 1px solid #d4d4d4;">
		 		<a style="color: #397cc8; font-weight: bold; font-size: 12px; vertical-align: middle;">카드사 심사현황 조회</a>
				<input type="text" id="biz_no" placeholder="'-'없는 사업자번호" class="ipt" maxlength="10" style="width: 110px;"/>
				<input type="button" class="button2" value="검색" onclick="_select();"/>
			</div>
		</li>
    </ul>        
</div>
		<div id="loginBox">
		<!-- 로그인 입력 폼 -->
			<div class="loginForm">
			<form method="post" action="<%=request.getRequestURI()%>" onsubmit="return setLogin();">
			<input type="hidden" name="goUrl" value="<%=goUrl%>" />
				<p>
					<img src="<%=cfg.get("imgDir")%>/web/login/txt_id.gif" alt="아이디" />
					<input type="text" name="id" id="id" class="ipt mb3" />
					<br />
					<img src="<%=cfg.get("imgDir")%>/web/login/txt_pw.gif" alt="비밀번호" />
					<input type="password" name="pass" id="pass" class="ipt" />
				</p>
				<p>
					<input type="submit" value="" class="btnLogin" />
				</p>
			</form>
			</div>
		<!-- # 로그인 입력 폼 -->
		</div>
		<div id="copyright">
			<table>
			<td>
				<img src="<%=cfg.get("imgDir")%>/web/footer_logo.gif" alt="UBCN" />
			</td>
			<td>
			<address style="font-size:11px; margin-right:100px; font-style: normal;">
				서울특별시 금천구 가산디지털1로 212, 710호 (가산동, 코오롱애스턴)<br />
				<b style="font-size:11px;">TEL</b> 02-2082-7880 &nbsp;
				<b style="font-size:11px;">FAX</b> 02-2082-7881
			</address>
			</td>
			</table>
		</div>
	</div>
</div>
</body>
</html>

<script type="text/javascript">
	function setLogin() {
		var o = {id : $('id'), pass : $('pass')};

		if (!o.id.value) {
			window.alert("아이디를 입력하세요.");
			o.id.focus();
			return false;
		} else if (!o.pass.value) {
			window.alert("비밀번호를 입력하세요.");
			o.pass.focus();
			return false;
		}

		return true;
	}
	
	function _select() {
		if($('biz_no').value.length != 10){
			alert("10자리의 사업자등록 번호를 입력해주세요.");
			return;
		}
		new Ajax.Request('./system/SystemCardSearch.jsp', {
			asynchronous : true,
			parameters : {
			        bizNo : $('biz_no').value
			},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();
					var i = 0;

					if(e.data.length != 0){
						e.data.each (function (data, i) {
							alert("사업장명 : " + data.biz_name +
								  "\n 삼성카드 : " + data.ssc + "\n 신한카드 : " + data.shc + "\n 비씨카드 : " + data.bcc + "\n 국민카드 : " + data.kbc +
								  "\n 현대카드 : " + data.hdc + "\n 롯데카드 : " + data.ltc + "\n 농협카드 : " + data.nhc + "\n 하나카드 : " + data.hnc);
						}.bind(this));
					}else{
						alert("사업자등록번호를 확인하세요.");
					}
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}

				o.sbmsg.hide();
			}.bind(this)
		});
	}
	
	Event.observe(window, 'load', function (event) {
		var o = {id : $('id'), pass : $('pass')};

		if (o.id.value != '') {
			o.pass.focus();
		} else {
			o.id.focus();
		}
	}.bind(this));
</script>