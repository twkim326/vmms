<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.db.DBLibrary
			, java.net.*
			, java.sql.*
			, org.apache.log4j.Logger
			, org.apache.log4j.PropertyConfigurator
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * auth.jsp
 *
 * 권한 정보
 *
 * 작성일 - 2011/03/21, 정원광
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
		out.print("{\"code\" : \"EXPIRE\"}");
		return;
	}

// 로거
	PropertyConfigurator.configure(cfg.get("config.log4j"));
	Logger logger = Logger.getLogger(getClass());

// 전송된 내용
	long company = StringEx.str2long(request.getParameter("company"), 0);
	int depth = StringEx.str2int(request.getParameter("depth"), -1);

// 실행에 사용될 변수
	DBLibrary dbLib = new DBLibrary(logger);
	Connection conn = null;
	PreparedStatement ps = null;
	ResultSet rs = null;
	String error = null;

// DB 연결
	conn = dbLib.getConnection(cfg.get("db.jdbc.name"), cfg.get("db.jdbc.host"), cfg.get("db.jdbc.user"), cfg.get("db.jdbc.pass"));

	if (conn == null) {
		out.print("{\"code\" : \"FAIL\"}");
		return;
	}

// 검색절
	String WHERE = " COMPANY_SEQ = " + company;

	if (depth > -1) {
		WHERE += " AND (DEPTH >= " + depth + " OR DEPTH = -1)";
	}

	if (cfg.getLong("user.company") > 0) {
		WHERE += " AND COMPANY_SEQ = " + cfg.getLong("user.company");
	}

// 목록 가져오기
	String responseText = "";

	try {
//20160221 INDEX 힌트 추가
		ps = dbLib.prepareStatement(conn,
				"SELECT /*+ INDEX(A) */"
						+ " SEQ,"
						+ " NAME"
					+ " FROM TB_AUTH A"
					+ " WHERE" + WHERE
					+ " ORDER BY DEPTH"
			);
		rs = ps.executeQuery();

		while (rs.next()) {
			responseText += (StringEx.isEmpty(responseText) ? "" : ",")
					+ "{"
						+ "\"seq\" : " + rs.getLong("SEQ")
						+ ", \"name\" : \"" + URLEncoder.encode(rs.getString("NAME").replaceAll("'", ""), "UTF-8") + "\""
					+ "}"
				;
		}
	} catch (Exception e) {
		error = e.getMessage();
	} finally {
		dbLib.close(rs);
		dbLib.close(ps);
	}

// 리소스 반환
	dbLib.close(conn);

// 결과 출력
	if (StringEx.isEmpty(error)) {
		out.print("{\"code\" : \"SUCCESS\", \"data\" : [" + responseText + "]}");
	} else {
		out.print("{\"code\" : \"FAIL\"}");
	}
%>