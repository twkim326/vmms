<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Auth
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /system/SystemAuthDetail.jsp
 *
 * 시스템 > 권한관리 > 조회
 *
 * 작성일 - 2011/03/24, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0103");

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

// 전송된 내용
	long company = StringEx.str2long(request.getParameter("company"), 0);
	long auth = StringEx.str2long(request.getParameter("auth"), 0);

// 인스턴스 생성
	Auth objAuth = new Auth(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objAuth.detail(company, auth);
	} catch (Exception e) {
		error = e.getMessage();
	}

// 에러 처리
	if (error != null) {
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode(error.replaceAll("'", ""), "UTF-8") + "\"}");
		return;
	}

// 조직
	String organText = "";

	for (int i = 0; i < objAuth.organ.size(); i++) {
		GeneralConfig c = (GeneralConfig) objAuth.organ.get(i);

		organText += (StringEx.isEmpty(organText) ? "" : ",")
				+ "{"
					+ "\"seq\" : " + c.getLong("SEQ")
					+ ", \"name\" : \"" + URLEncoder.encode(c.getString("NAME").replaceAll("'", ""), "UTF-8") + "\""
					+ ", \"depth\" : " + c.getInt("DEPTH")
				+ "}"
			;
	}

// 권한
	String authText = "";

	for (int i = 0; i < objAuth.auth.size(); i++) {
		GeneralConfig c = (GeneralConfig) objAuth.auth.get(i);

		authText += (StringEx.isEmpty(authText) ? "" : ",")
				+ "{"
					+"\"seq\" : " + c.getLong("SEQ")
					+ ", \"name\" : \"" + URLEncoder.encode(c.getString("NAME").replaceAll("'", ""), "UTF-8") + "\""
					+ ", \"depth\" : " + c.getInt("DEPTH")
					+ ", \"isAppOrgan\" : \"" + c.get("IS_APP_ORGAN") + "\""
					+ ", \"isAbleAppOrgan\" : \"" + c.get("IS_ABLE_APP_ORGAN") + "\""
				+ "}"
			;
	}

// 메뉴
	String menuText = "";

	for (int i = 0; i < objAuth.menu.size(); i++) {
		GeneralConfig c = (GeneralConfig) objAuth.menu.get(i);

		menuText += (StringEx.isEmpty(menuText) ? "" : ",")
				+ "{"
					+ "\"seq\" : \"" + c.getString("SEQ") + "\""
					+ ", \"depth\" : " + c.getInt("DEPTH")
					+ ", \"S\" : \"" + c.getString("ENABLE_S") + "\""
					+ ", \"I\" : \"" + c.getString("ENABLE_I") + "\""
					+ ", \"U\" : \"" + c.getString("ENABLE_U") + "\""
					+ ", \"D\" : \"" + c.getString("ENABLE_D") + "\""
				+ "}"
			;
	}

// 결과 출력
	out.print("{\"code\" : \"SUCCESS\", \"organ\" : [" + organText + "], \"auth\" : [" + authText + "], \"menu\" : [" + menuText + "]}");
%>