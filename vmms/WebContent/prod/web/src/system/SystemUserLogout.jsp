<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.User
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /system/SystemUserLogout.jsp
 *
 * 시스템 > 계정 > 로그아웃
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

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(cfg.login(cfg.get("script.index")));
		return;
	}

// 인스턴스 생성
	User objUser = new User(cfg);

// 로그아웃 처리
	String error = objUser.logout(response, request.getRemoteAddr());

	if (error != null) {
		out.print(error);
		return;
	}

// 이동
	response.sendRedirect(cfg.get("script.index"));
%>