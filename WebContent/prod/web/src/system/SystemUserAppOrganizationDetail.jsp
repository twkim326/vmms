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
 * /system/SystemUserAppOrganizationDetail.jsp
 *
 * 시스템 > 계정 > 매출 조회 조직 추가 > 조회
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
	long company = StringEx.str2long(request.getParameter("company"), 0);
	long organ = StringEx.str2long(request.getParameter("organ"), 0);
	int depth = StringEx.str2int(request.getParameter("depth"), 0);

	if (company == 0) {
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("소속정보가 존재하지 않습니다.", "UTF-8") + "\"}");
		return;
	}

// 인스턴스 생성
	User objUser = new User(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objUser.appOrganList(company, organ, depth);
	} catch (Exception e) {
		error = e.getMessage();
	}

// 에러 처리
	if (error != null) {
		out.print("{\"code\" : \"FAIL\"}");
		return;
	}

// 조직
	String responseText = "";

	for (int i = 0; i < objUser.organ.size(); i++) {
		GeneralConfig c = (GeneralConfig) objUser.organ.get(i);

		responseText += (StringEx.isEmpty(responseText) ? "" : ",")
				+ "{"
					+ "\"seq\" : " + c.getLong("SEQ")
					+ ", \"name\" : \"" + URLEncoder.encode(c.getString("NAME").replaceAll("'", ""), "UTF-8") + "\""
				+ "}"
			;
	}

// 결과 출력
	out.print("{\"code\" : \"SUCCESS\", \"disp\" : \"" + URLEncoder.encode(objUser.data.get("TITLE").replaceAll("'", ""), "UTF-8") + "\", \"data\" : [" + responseText + "]}");
%>