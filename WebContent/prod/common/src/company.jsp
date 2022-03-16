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
%>
<%
/**
 * company.jsp
 *
 * 소속 정보
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
	long organ = StringEx.str2long(request.getParameter("organ"), 0);
	String isAll = StringEx.setDefaultValue(request.getParameter("isAll"), "N");

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

	if (cfg.getLong("user.company") > 0) {
		WHERE += " WHERE SEQ = " + cfg.getLong("user.company");
	}

// 목록 가져오기
	String responseText = "";

	try {
		//20160108 INDEX(A PK_COMPANY)에서 PK_COMPANY 제거, 서브쿼리 INDEX 힌트 추가
		//20160113 정렬순서에 SEQ 추가
		//ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_COMPANY) */ A.SEQ, A.NAME, (SELECT MAX(DEPTH) AS DEPTH FROM TB_ORGANIZATION WHERE COMPANY_SEQ = A.SEQ) AS DEPTH FROM TB_COMPANY A WHERE " + WHERE);
		ps = dbLib.prepareStatement(conn,
				"SELECT /*+ INDEX(A) */"
						+ " SEQ, NAME, ("
								+ " SELECT /*+ INDEX(AA) */"
										+ " MAX(DEPTH) AS DEPTH"
									+ " FROM TB_ORGANIZATION AA"
									+ " WHERE COMPANY_SEQ = A.SEQ"
							+ " ) AS DEPTH"
					+ " FROM TB_COMPANY A"
					+ WHERE
					+ " ORDER BY NAME, SEQ"
				);
		rs = ps.executeQuery();

		while (rs.next()) {
			responseText += (StringEx.isEmpty(responseText) ? "" : ",")
					+ "{"
						+ "\"seq\" : " + rs.getLong("SEQ")
						+ ", \"name\" : \"" + URLEncoder.encode(rs.getString("NAME").replaceAll("'", ""), "UTF-8") + "\""
						+ ", \"depth\" : " + rs.getInt("DEPTH")
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

// 조직 트리 가져오기
	String organText = "";

	if (StringEx.isEmpty(error) && organ > 0) {
		try {
			//20160108 INDEX 힌트 추가
			//20160113 정렬순서에 SEQ 추가
			//ps = dbLib.prepareStatement(conn, "SELECT SEQ FROM TB_ORGANIZATION " + (isAll.equals("N") ? "WHERE IS_ENABLED = 'Y'" : "") + " START WITH SEQ = " + organ + " CONNECT BY SEQ = PRIOR PARENT_SEQ ORDER BY DEPTH");
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " SEQ"
						+ " FROM TB_ORGANIZATION A"
						+ (isAll.equals("N") ? " WHERE IS_ENABLED = 'Y'" : "")
						+ " START WITH SEQ = " + organ
						+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						+ " ORDER BY DEPTH, SEQ"
					);
			rs = ps.executeQuery();

			while (rs.next()) {
				organText += (StringEx.isEmpty(organText) ? "" : ",")
						+ "{"
							+ "\"seq\" : " + rs.getLong("SEQ")
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
	}

// 리소스 반환
	dbLib.close(conn);

// 결과 출력
	if (StringEx.isEmpty(error)) {
		out.print("{\"code\" : \"SUCCESS\", \"data\" : [" + responseText + "], \"organ\" : [" + organText + "]}");
	} else {
		out.print("{\"code\" : \"FAIL\"}");
	}
%>
