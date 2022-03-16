<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Goods
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceGoodsGroup.jsp
 *
 * 서비스 > 상품 > 그룹
 *
 * 작성일 - 2011/04/02, 정원광
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
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("로그인이 필요합니다.", "UTF-8") + "\"}");
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth()) {
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("접근이 불가능한 페이지입니다.", "UTF-8") + "\"}");
		return;
	}

// 전송된 데이터
	long company = StringEx.str2long(request.getParameter("company"), 0);
	long group = StringEx.str2long(request.getParameter("group"), 0);

// 인스턴스 생성
	Goods objGoods = new Goods(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objGoods.group(company, group);
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

	for (int i = 0; i < objGoods.group.size(); i++) {
		GeneralConfig c = (GeneralConfig) objGoods.group.get(i);

		responseText += (StringEx.isEmpty(responseText) ? "" : ",")
				+ "{"
					+ "\"seq\" : " + c.getLong("SEQ")
					+ ", \"code\" : \"" + c.get("CODE").trim() + "\""
					+ ", \"name\" : \"" + URLEncoder.encode(c.get("NAME").replaceAll("'", "").trim(), "UTF-8") + "\""
				+ "}"
			;
	}

// 결과 출력
	out.print("{\"code\" : \"SUCCESS\", \"data\" : [" + responseText + "]}");
	return;
%>