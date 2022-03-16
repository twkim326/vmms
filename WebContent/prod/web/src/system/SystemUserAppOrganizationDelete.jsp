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
 * /system/SystemUserAppOrganizationDelete.jsp
 *
 * 시스템 > 계정 > 매출 조회 조직 추가 > 삭제
 *
 * 작성일 - 2011/03/30, 정원광
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
	if (!cfg.isAuth("U")) {
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("접근이 불가능한 페이지입니다.", "UTF-8") + "\"}");
		return;
	}

// 전송된 데이터
	long seq = StringEx.str2long(request.getParameter("seq"), 0);
	long organ = StringEx.str2long(request.getParameter("organ"), 0);

	if (seq == 0) {
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("계정정보가 존재하지 않습니다.", "UTF-8") + "\"}");
		return;
	} else if (organ == 0) {
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("조직정보가 존재하지 않습니다.", "UTF-8") + "\"}");
		return;
	}

// 인스턴스 생성
	User objUser = new User(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objUser.appOrganDelete(seq, organ);
	} catch (Exception e) {
		error = e.getMessage();
	}

// 에러 처리
	if (error != null) {
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode(error.replaceAll("'", ""), "UTF-8") + "\"}");
		return;
	}

// 결과 출력
	out.print("{\"code\" : \"SUCCESS\"}");
%>