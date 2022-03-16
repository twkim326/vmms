<%@page import="java.util.Map"%>
<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Product
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceVMGoodsList.jsp
 *
 * 서비스 > 운영 자판기 > 상품 검색
 *
 * 작성일 - 2011/04/03, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("UTF-8");
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
	//long seq = StringEx.str2long(request.getParameter("seq"), 0);
	String json = request.getParameter("jsonData");
	System.out.println(json);
/* // 인스턴스 생성
	 Product objPD = new Product(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objPD.tableDetailList(seq);
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
	for (int i = 0; i < objPD.tableDetailList.size(); i++) {
		GeneralConfig c = (GeneralConfig) objPD.tableDetailList.get(i);
		responseText += (StringEx.isEmpty(responseText) ? "" : ",")
				+ "{"
					+ "\"name\" : \"" + c.get("NAME").trim() + "\""
					+ ", \"col_no\" : " + c.get("COL_NO").trim()
					+ ", \"price\" : " + c.get("PRICE").trim()
					+ ", \"product_seq\" : " + c.get("PRODUCT_SEQ").trim()
					+ "}"
			;
	}

// 결과 출력
	out.print("{\"code\" : \"SUCCESS\", \"data\" : [" + responseText + "]}"); */
	return;
%>