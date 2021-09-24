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
 * /system/SystemUserUniqueID.jsp
 *
 * 시스템 > 계정 > 아이디 중복 체크
 *
 * 작성일 - 2011/03/23, 정원광
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
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("로그인이 필요합니다.", "UTF-8") + "\"}");
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth()) {
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("접근이 불가능한 페이지입니다.", "UTF-8") + "\"}");
		return;
	}

// 전송된 데이터
	String id = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("id"), ""), "UTF-8");

	if (StringEx.isEmpty(id)) {
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("아이디가 존재하지 않습니다", "UTF-8") + "\"}");
		return;
	}

// 인스턴스 생성
	User objUser = new User(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objUser.checkUniqueID(id);
	} catch (Exception e) {
		error = e.getMessage();
	}

	if (error != null) {
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode(error.replaceAll("'", ""), "UTF-8") + "\"}");
		return;
	}

// 결과 출력
	out.print("{\"code\" : \"SUCCESS\"}");
%>