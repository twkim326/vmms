<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="ko" lang="ko">
	<head>
		<title><%=cfg.get("title")%></title>
		<meta http-equiv="imagetoolbar" content="no" />
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
		<link rel="stylesheet" type="text/css" href="<%=cfg.get("imgDir")%>/css/common.css">		
		<link rel="shortcut icon" href="/image/web/favicon_ubcn.png">
		<!-- 
		<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/js/prototype/prototype-1.6.0.3.js"></script>
		<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/js/scriptaculous/1.8.3/scriptaculous.js"></script>
		 -->
		<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/js/prototype/prototype-1.7.3.js"></script>
		<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/js/scriptaculous/1.9.0/scriptaculous.js"></script>
		<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/js/Common.js"></script>
		<script type="text/javascript" src="<%=cfg.get("svcDir")%>/js/SvrLink.js"></script>
		<script type="text/javascript" src="<%=cfg.get("svcDir")%>/js/Company.js"></script>
		<script type="text/javascript" language="javascript">
			var Navi = {
				show : function (n) {
					$('menuBox').getElementsBySelector('ul').findAll(function (o) {
						if (o.getAttribute('name') == n) {
							o.className = 'showY';
						} else if (o.getAttribute('name')) {
							o.className = 'showN';
						}
					}.bind(this));
				}
			}
			
			function _cardRegi() {
				new IFrame(510, 150, '/prod/web/src/card_regist.jsp').open();
			}
			  function installMobileHome(){
			    
			    var iconUrl = "https://vmms.ubcn.co.kr/image/web/favicon_ubcn.ico";
			    var title = "VMMS 링크";
			    var url = "https://vmms.ubcn.co.kr/";

			    addShortCut(url, iconUrl, title);
			  }
			  /**
			 * 접속한 브라우저가 모바일인지 체크
			 * @returns
			 */
			function isMobile(){
			  var isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry/i.test(navigator.userAgent) ? true : false;
			  return isMobile;
			}

			/**
			* 바로가기 추가
			*/
			function addShortCut(url, iconUrl, title){
				
				if(!isMobile()){
					alert("모바일에서만 홈 화면에 바로가기를 추가할 수 있습니다.");
					return;
				}
				
				var userAgent = navigator.userAgent.toLowerCase();
				if(userAgent.match(/android/)){
			      	//alert("안드로이드 일 경우,");
					var appUrl = "naversearchapp://addshortcut?url=" + encodeURIComponent(url) + "&icon=" + encodeURIComponent(iconUrl) + "&title=" + encodeURIComponent(title) + "&serviceCode=housechecklist&version=7";
					window.open(appUrl);
				}else{
					alert("ios 계열은 직접 홈 버튼 추가를 사용하셔야 합니다.", 'F');
					return;
				}
			}
		</script>
		<!-- jQuery , jQuery와 prototype간 충돌 방지 소스, select2 내용 추가. -->
		<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/js/jQuery3_5_0.js"></script>
		<script type="text/javascript">
			jQuery.noConflict();
			var j$=jQuery;
			j$(document).ready(function() {
			    j$('.js-example-basic-single').select2();
			    j$(".js-example-responsive").select2({
			        width: 'resolve' // need to override the changed default
			    });
			});
		</script>
		<link href="https://cdn.jsdelivr.net/npm/select2@4.1.0-beta.1/dist/css/select2.min.css" rel="stylesheet" />
		<script src="https://cdn.jsdelivr.net/npm/select2@4.1.0-beta.1/dist/js/select2.min.js"></script>
		<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/layer/js/iframe.js"></script>
	</head>
	<body class="<%=(cfg.get("window.mode").equals("A") ? "" : "open")%>">
<% if (cfg.get("window.mode").equals("A")) { %>
		<div id="wrap">
			<!-- Gnb menu area -->
			<div id="header">
				<h1>
					<img src="<%=cfg.get("imgDir")%>/web/logo.gif" alt="VMMS" usemap="#goHome" />
					<map name="goHome" id="goHome">
						<area shape="rect" coords="0,0,110,20" href="<%=cfg.get("svcDir")%>/src/main.jsp" />
					</map>
				</h1>
				<ul>
					<!-- <li><a href="#" onclick="installMobileHome();">홈 화면에 추가</a>&nbsp;&nbsp;|&nbsp;&nbsp;</li> -->
					<li><a href="<%=cfg.get("script.index")%>">메인페이지</a>&nbsp;&nbsp;|&nbsp;&nbsp;</li>
					<li><a href="<%=cfg.get("svcDir")%>/src/system/SystemUserLogout.jsp">로그아웃</a></li>
				</ul>
			</div>

			<div id="container" class="clearfx">
				<!-- Main menu area -->
				<div id="mainMenu">
					<img src="<%=cfg.get("imgDir")%>/web/img_systemTitle.gif" alt="무인자판기 관리시스템" class="ssTitle" />
					<!-- menu -->
					<div id="menuBox">
<%
	if (cfg.mMenu != null && cfg.sMenu != null) {
		for (int i = 0; i < cfg.mMenu.size(); i++) {
			GeneralConfig m = (GeneralConfig) cfg.mMenu.get(i);
			int sMenu = 0;

			out.println("<div>");
			out.println("<div class='tabMenu show" + m.get("IS_SELECTED") + "'><a onclick='Navi.show(\"" + m.get("SEQ") + "\");'>" + m.get("NAME") + "</a></div>");
			out.println("<div class='subMenu'>");
			out.println("<ul class='show" + m.get("IS_SELECTED") + "' name='" + m.get("SEQ") + "'>");

			for (int j = 0; j < cfg.sMenu.size(); j++) {
				GeneralConfig s = (GeneralConfig) cfg.sMenu.get(j);
				if (s.get("PARENT_SEQ").equals(m.get("SEQ"))) {
					out.println("<li class='show" + s.get("IS_SELECTED") + (sMenu++ == 0 ? " first" : "") + "'><a href='" + cfg.get("topDir") + s.get("SRC") + "'>" + s.get("NAME") + "</a></li>");
				}
			}

			out.println("</ul>");
			out.println("</div>");
			out.println("</div>");
		}
	}

	String[] THIS_WEEK = {"", "일", "월", "화", "수", "목", "금", "토"};
	String   WEEK_NAME = THIS_WEEK[DateTime.getDayOfWeek()];
%>
					</div>
					<!-- # menu -->
					
					<!-- login -->
					<div id="loginBox">
						<strong><%=DateTime.date("yyyy")%>년 <%=DateTime.date("MM")%>월 <%=DateTime.date("dd")%>일 <%=WEEK_NAME%>요일</strong><br />
						<span><strong><script language="javascript">document.write(Common.strCut('<%=cfg.get("user.name").replaceAll("'", "")%>', 8));</script></strong>님, 반갑습니다.</span>
						<a href="<%=cfg.get("svcDir")%>/src/service/ServiceUser.jsp"><img src="<%=cfg.get("imgDir")%>/web/btn_changeInfo.gif" alt="" /></a>
						<a href="<%=cfg.get("svcDir")%>/src/system/SystemUserLogout.jsp"><img src="<%=cfg.get("imgDir")%>/web/btn_logout.gif" alt="" /></a>
					</div>
					<!-- # login -->
					<%	long user_company = cfg.getLong("user.company");
					if( user_company == 0) {%>
						<div style="margin-left:14px; margin-top:10px;"><img src="<%=cfg.get("imgDir")%>/web/btn_registcard.gif" alt="" onclick="_cardRegi();"/></div>
					<% }%>
				</div>

				<div id="contents">
<% } %>
