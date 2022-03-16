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
 * place.jsp
 *
 * 위치 정보
 *
 * 작성일 - 2011/04/06, 정원광
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
	String WHERE;

	if (cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
		WHERE = " C.COMPANY_SEQ = " + cfg.getLong("user.company");
	} else {
		WHERE = " C.COMPANY_SEQ = " + company;
	}

	if (organ > 0) { // 조직
		//20160113 INDEX 힌트 추가
		//WHERE += " AND C.ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + organ + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + organ + ")";
		WHERE += " AND C.ORGANIZATION_SEQ IN ("
					+ " SELECT /*+ INDEX(AA) */"
							+ " SEQ"
						+ " FROM TB_ORGANIZATION AA"
						+ " WHERE SEQ = " + organ
					+ " UNION"
					+ " SELECT /*+ INDEX(AB) */"
							+ " SEQ"
						+ " FROM TB_ORGANIZATION AB"
						+ " WHERE SORT = 1"
						+ " START WITH PARENT_SEQ = " + organ
						+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
				+ ")"
			;
	}

// 목록 가져오기
	String responseText = "";

	try {
		//20160113 JOIN 단순화 및 INDEX(A) 힌트를 FULL(A)로 변경, 정렬(PLACE, SEQ) 추가
		//ps = dbLib.prepareStatement(conn, "SELECT /*+ ORDERED USE_NL(B C) INDEX(A PK_VM) */ A.SEQ, A.PLACE FROM TB_VENDING_MACHINE_PLACE A INNER JOIN TB_VENDING_MACHINE B ON A.VM_SEQ = B.SEQ INNER JOIN TB_VENDING_MACHINE_HISTORY C ON B.SEQ = C.VM_SEQ WHERE " + WHERE + " GROUP BY A.SEQ, A.PLACE");
		ps = dbLib.prepareStatement(conn,
				"SELECT /*+ ORDERED FULL(A) USE_NL(B C) */"
						+ " B.SEQ, B.PLACE, B.END_DATE"
					+ " FROM TB_VENDING_MACHINE_HISTORY A"
						+ " INNER JOIN TB_VENDING_MACHINE_PLACE B"
							+ " ON A.VM_SEQ = B.VM_SEQ"
								+ " AND ( A.END_DATE IS NULL OR A.END_DATE <= B.END_DATE )"  //20180309 jwhwang
						+ " INNER JOIN TB_VENDING_MACHINE C"
							+ " ON A.VM_SEQ = C.SEQ"
					+ " WHERE " + WHERE
					+ " GROUP BY B.PLACE, B.SEQ, B.END_DATE"
					+ " ORDER BY B.PLACE, B.SEQ"
			);
		rs = ps.executeQuery();

		while (rs.next()) {
			//20160113 [END_DATE]를 설치장소 앞에 추가
			//responseText += (StringEx.isEmpty(responseText) ? "" : ",") + "{\"seq\" : " + rs.getLong("SEQ") + ", \"name\"" : \"" + URLEncoder.encode(rs.getString("PLACE").replaceAll("'", ""), "UTF-8") + "\"}";
			String endDate = rs.getString("END_DATE");
			responseText += (StringEx.isEmpty(responseText) ? "" : ",")
					+ "{"
						+ "\"seq\" : " + rs.getLong("SEQ")
						+ ", \"name\" : \"" + URLEncoder.encode(((endDate == null ? "" : "[" + endDate + "] ") + rs.getString("PLACE")).replaceAll("'", ""), "UTF-8")
					+ "\"}"
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