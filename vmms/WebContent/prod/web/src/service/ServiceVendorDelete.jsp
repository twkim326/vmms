<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Vendor
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceVendorDelete.jsp
 *
 * 서비스 > 상품 ㅅ공급자 관리 > 삭제
 *
 * 작성일 - 2017/04/28, 황재원
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0206");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("로그인이 필요합니다.", "UTF-8") + "\"}");
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth("D")) {
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("접근이 불가능한 페이지입니다.", "UTF-8") + "\"}");
		return;
	}

// 전송된 데이터
	long seq = StringEx.str2long(request.getParameter("seq"), 0);

// 인스턴스 생성
	Vendor objVendor = new Vendor(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objVendor.delete(seq);
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