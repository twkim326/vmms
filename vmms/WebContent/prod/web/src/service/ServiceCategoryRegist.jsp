<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Category
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceCategoryRegist.jsp
 *
 * 서비스 > 상품 카테고리 관리 > 등록
 *
 * 작성일 - 2017/05/16, 황재원
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0204");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("로그인이 필요합니다.", "UTF-8") + "\"}");
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth("I")) {
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("접근이 불가능한 페이지입니다.", "UTF-8") + "\"}");
		return;
	}

// 전송된 데이터
	long company = StringEx.str2long(request.getParameter("company"), 0);
	long cate = StringEx.str2long(request.getParameter("cate"), 0);
	int depth = StringEx.str2int(request.getParameter("depth"), 0);
	int sort = StringEx.str2int(request.getParameter("sort"), 0);
	String name = StringEx.replace(URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("name"), ""), "UTF-8"), "/", "");
	String code = StringEx.replace(URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("code"), ""), "UTF-8"), "/", "");

	if (company == 0 || StringEx.isEmpty(name)) {
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("필수 입력정보가 존재하지 않습니다.", "UTF-8") + "\"}");
		return;
	}

// 인스턴스 생성
	Category objCate = new Category(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objCate.regist(company, cate, depth, sort, name, code);
	} catch (Exception e) {
		error = e.getMessage();
	}

	if (error != null && StringEx.str2long(error) == 0) {
		out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode(error.replaceAll("'", ""), "UTF-8") + "\"}");
		return;
	}

// 결과 출력
	out.print("{\"code\" : \"SUCCESS\", \"seq\" : \"" +  + objCate.data.getLong("SEQ")  + "\", \"catcode\" : \"" + URLEncoder.encode(objCate.data.get("CODE").replaceAll("'", ""), "UTF-8") + "\"}");
%>