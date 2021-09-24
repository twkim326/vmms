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
 * user.jsp
 *
 * 계정 정보
 *
 * 작성일 - 2011/04/02, 정원광
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
	long organ = StringEx.str2long(request.getParameter("organ"), 0);

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
	String WHERE = "";

	if (cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
		WHERE += " AND COMPANY_SEQ = " + cfg.getLong("user.company");
	} else {
		WHERE += " AND COMPANY_SEQ = " + company;
	}


	organ = (organ > 0 ? organ: cfg.getLong("user.organ"));
	if (organ > 0) { // 조직
//20160221 INDEX 힌트 추가, UNION 통합
//		WHERE += " AND ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + organ + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + organ + ")";
		WHERE += " AND ORGANIZATION_SEQ IN ("
					+ " SELECT /*+ INDEX(A_B) */"
							+ " SEQ"
						+ " FROM TB_ORGANIZATION A_B"
						+ " WHERE SORT = 1"
						+ " START WITH SEQ = " + organ
						+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
				+ " )"
			;
	} 
	// 2016.06.26 jwhwang
	//	if (cfg.getLong("user.organ") > 0) { // 시스템 관리자가 아닌 경우, 내 하위 조직들만 검색
//20160221 INDEX 힌트 추가
//		WHERE += " AND ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ)";
		WHERE += " AND ORGANIZATION_SEQ IN ("
					+ " SELECT /*+ INDEX(A_A) */"
							+" SEQ"
						+ " FROM TB_ORGANIZATION A_A"
						+ " WHERE SORT = 1"
						+ " START WITH PARENT_SEQ = " + cfg.getLong("user.organ")
						+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
				+ " )"
			;
	}
	
	WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

// 목록 가져오기
	String responseText = "";

	try {
//20160221 INDEX 힌트 변경, ORDER BY 추가
//		ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_USER) */ SEQ, ID, NAME FROM TB_USER A WHERE " + WHERE);
		ps = dbLib.prepareStatement(conn,
				"SELECT" + (WHERE.length() > 0 ? " /*+ INDEX(A) */" : "")
						+" SEQ,"
						+ " ID,"
						+ " NAME"
					+ " FROM TB_USER A"
					+ WHERE
					+ " ORDER BY NAME, ID"
			);
		rs = ps.executeQuery();

		while (rs.next()) {
			responseText += (StringEx.isEmpty(responseText) ? "" : ",")
					+ "{"
						+ "\"seq\" : " + rs.getLong("SEQ")
						+ ", \"id\" : \"" + rs.getString("ID") + "\""
						+ ", \"name\" : \"" + URLEncoder.encode(rs.getString("NAME").replaceAll("'", ""), "UTF-8") + "\""
					+ "}"
				;
		}
	} catch (Exception e) {
		logger.error(e);
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