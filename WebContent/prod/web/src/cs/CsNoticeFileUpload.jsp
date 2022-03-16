<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.Notice
			, java.io.File
			, java.util.Enumeration
			, com.oreilly.servlet.MultipartRequest
			, com.oreilly.servlet.multipart.DefaultFileRenamePolicy
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /cs/CsNoticeRegist.jsp
 *
 * 고객센터 > 공지사항 > 등록/수정
 *
 * 작성일 - 2011/04/03, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0501");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(cfg.login());
		return;
	}

// 페이지 권한 체크
	if (!(cfg.isAuth("I") || cfg.isAuth("U"))) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "A");

// 전송된 데이터
	long seq = StringEx.str2long(request.getParameter("seq"), 0);
	String realDir="";
	
	String saveDir="download";
	String encType="utf-8";
	int maxSize=5*1024*1024;//5Mbyte
	
	ServletContext context=this.getServletContext();
	realDir=context.getRealPath(saveDir);
	
	MultipartRequest multi=null;
	try{
		multi=new MultipartRequest(
					request,
					realDir,
					maxSize,
					encType,
					new DefaultFileRenamePolicy()
				);
		Enumeration en=multi.getParameterNames();
		while(en.hasMoreElements()){
			String name=(String)en.nextElement();
			String value=multi.getParameter(name);			
		}
		en=multi.getFileNames();
		while(en.hasMoreElements()){
			String name=(String)en.nextElement();
			String originFile=multi.getOriginalFileName(name);
			
			String systemFile=multi.getFilesystemName(name);
			String fileType=multi.getContentType(name);
			
			File file=multi.getFile(name);
						
		}
	}catch(Exception e){
		out.println("파일에러");
	}
	
	

%>

<%response.sendRedirect("/prod/web/src/cs/CsNotice.jsp"); %> 


