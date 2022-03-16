<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, java.io.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * down.jsp
 *
 * 다운로드
 *
 * 작성일 - 2011/04/20, 정원광
 *
 */

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session);

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(cfg.login());
		return;
	}

// 전송된 데이터
	String src = StringEx.replace(request.getParameter("src"), "..", "");

	if (StringEx.isEmpty(src)) {
		out.print("파일 정보가 존재하지 않습니다.");
		return;
	}

// 파일 인스턴스
	File file = new File(StringEx.replace(cfg.get("data.aDir") + "/" + src, "//", "/"));

// 헤더 수정
	response.setHeader("content-type", "application/octet-stream");
	response.setHeader("content-disposition", "attachment; filename=" + file.getName());
	response.setHeader("content-length", StringEx.long2str(file.length()));

// 다운로드
	FileEx.write(response, file);
%>