<%@ page import="
			com.nucco.*,
			com.nucco.cfg.*,
			com.nucco.lib.*,
			com.nucco.beans.Sales,
			java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /sales/SalesPlace.jsp
 *
 * 자판기 매출정보 > 설치 위치 검색
 *
 * 작성일 - 2011/04/07, 정원광
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
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("로그인이 필요합니다.", "UTF-8") + "\"}");
		return;
	}

// 전송된 데이터
	long organ = StringEx.str2long(request.getParameter("organ"), 0);

// 인스턴스 생성
	Sales objSales = new Sales(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objSales.place(organ);
	} catch (Exception e) {
		error = e.getMessage();
	}

// 에러 처리
	if (error != null) {
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode(error.replaceAll("'", ""), "UTF-8") + "\"}");
		return;
	}

// 목록 생성
	String responseText = "";

	for (int i = 0; i < objSales.place.size(); i++) {
		GeneralConfig c = (GeneralConfig) objSales.place.get(i);

		responseText += (StringEx.isEmpty(responseText) ? "" : ",")
				+ "{"
					+ "\"seq\" : " + c.getLong("SEQ")
					+ ", \"name\" : \"" + URLEncoder.encode(c.get("PLACE").replaceAll("'", "").trim(), "UTF-8") + "\""
				+ "}"
			;
	}

// 결과 출력
	out.print("{\"code\" : \"SUCCESS\", \"data\" : [" + responseText + "]}");
	return;
%>