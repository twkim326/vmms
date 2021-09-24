<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Organization
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /system/SystemOrganizationDetail.jsp
 *
 * 시스템 > 조직관리 > 조회
 *
 * 작성일 - 2011/03/28, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0105");

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
	long organ = StringEx.str2long(request.getParameter("organ"), -1);
	int depth = StringEx.str2int(request.getParameter("depth"), 0);

// 인스턴스 생성
	Organization objOrgan = new Organization(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objOrgan.detail(company, organ, depth);
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

	if (objOrgan.organ != null) {
		for (int i = 0; i < objOrgan.organ.size(); i++) {
			GeneralConfig c = (GeneralConfig) objOrgan.organ.get(i);

			organText += (StringEx.isEmpty(organText) ? "" : ",")
					+ "{"
						+ "\"seq\" : " + c.getLong("SEQ")
						+ ", \"name\" : \"" + URLEncoder.encode(c.getString("NAME").replaceAll("'", ""), "UTF-8") + "\""
					+ "}"
				;
		}
	}

// 결과 출력
	out.print("{\"code\" : \"SUCCESS\", \"organ\" : [" + organText + "], \"data\" : {\"seq\" : " + objOrgan.data.getLong("SEQ") + ", \"name\" : \"" + URLEncoder.encode(objOrgan.data.get("NAME").replaceAll("'", ""), "UTF-8") + "\"}}");
%>