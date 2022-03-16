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
 * /service/ServiceVMGoodsList.jsp
 *
 * 서비스 > 운영 자판기 > 상품 검색
 *
 * 작성일 - 2011/04/03, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0205");


// 전송된 데이터
	String bizNo = request.getParameter("bizNo");

// 인스턴스 생성
	 Vendor objVendor = new Vendor(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objVendor.cardSearch(bizNo);
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
	for (int i = 0; i < objVendor.cardList.size(); i++) {
		GeneralConfig c = (GeneralConfig) objVendor.cardList.get(i);
		responseText += (StringEx.isEmpty(responseText) ? "" : ",")
				+ "{"
					+ "\"biz_name\" : \"" + c.get("BIZ_NAME").trim() + "\""
					+ ", \"biz_no\" : \"" + c.get("BIZ_NO").trim() + "\""
					+ ", \"ssc\" : \"" + c.get("SSC").trim() + "\""
					+ ", \"shc\" : \"" + c.get("SHC").trim() + "\""
					+ ", \"bcc\" : \"" + c.get("BCC").trim() + "\""
					+ ", \"kbc\" : \"" + c.get("KBC").trim() + "\""
					+ ", \"hdc\" : \"" + c.get("HDC").trim() + "\""
					+ ", \"ltc\" : \"" + c.get("LTC").trim() + "\""
					+ ", \"nhc\" : \"" + c.get("NHC").trim() + "\""
					+ ", \"hnc\" : \"" + c.get("HNC").trim() + "\""
					+ "}"
			;
	}

// 결과 출력
	out.print("{\"code\" : \"SUCCESS\", \"data\" : [" + responseText + "]}");
	return;
%>