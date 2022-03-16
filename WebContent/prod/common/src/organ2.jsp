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
 * organ2.jsp
 *
 * 조직 정보
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
		out.print("{\"code\" : \"FAIL\"}");
		return;
	}

// 로거
	PropertyConfigurator.configure(cfg.get("config.log4j"));
	Logger logger = Logger.getLogger(getClass());

// 전송된 내용
	long company = StringEx.str2long(request.getParameter("company"), 0);
	long organ = StringEx.str2long(request.getParameter("organ"), 0);
	int depth = StringEx.str2int(request.getParameter("depth"), 0);
	String mode = StringEx.setDefaultValue(request.getParameter("mode"), "");
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
	String WHERE = " COMPANY_SEQ = " + company + " AND SORT = 1";

	if (isAll.equals("N")) {
		WHERE += " AND IS_ENABLED = 'Y'";
	}

	if (organ > 0) { // 선택된 조직의 하위 조직
		WHERE += " AND PARENT_SEQ = " + organ;
	} else {
		WHERE += " AND PARENT_SEQ = 0";
	}

	if (cfg.getLong("user.company") > 0) {
		WHERE += " AND COMPANY_SEQ = " + cfg.getLong("user.company");
	}

	if (cfg.getLong("user.organ") > 0) {
		//20160108 INDEX 힌트 추가, PARENT_SEQ를 SEQ로 변경하여 쿼리통합
		//WHERE += " AND SEQ IN (SELECT SEQ FROM TB_ORGANIZATION START WITH PARENT_SEQ = " + cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION START WITH SEQ = " + cfg.getLong("user.organ") + " CONNECT BY SEQ = PRIOR PARENT_SEQ)";
		WHERE += " AND SEQ IN ("
					+ " SELECT /*+ INDEX() */"
							+ " SEQ"
						+ " FROM TB_ORGANIZATION"
						+ " START WITH SEQ = " + cfg.getLong("user.organ")
						+ " CONNECT BY PRIOR SEQ =  PARENT_SEQ"
				+ " )"
			;
	}

// 목록 가져오기
	String responseText = "";

	try {
		//20160108 INDEX(A PK_ORGANIZATION)을 INDEX(A)로 변경, CASE WHEN을 DECODE로 변경
		//20160113 정렬(NAME, SEQ) 추가
		//ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_ORGANIZATION) */ SEQ, NAME || (CASE WHEN IS_ENABLED = 'N' THEN '(' || TO_CHAR(MODIFY_DATE, 'YYMMDD') || ')' ELSE '' END) AS NAME FROM TB_ORGANIZATION A WHERE " + WHERE);
		ps = dbLib.prepareStatement(conn,
				"SELECT /*+ INDEX(A) */"
						+ " SEQ, NAME || DECODE(IS_ENABLED, 'N', '(' || TO_CHAR(MODIFY_DATE, 'YYMMDD') || ')', '') AS NAME"
					+ " FROM TB_ORGANIZATION A"
					+ " WHERE" + WHERE
					+ " ORDER BY NAME, SEQ"
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
		logger.error(e);
		error = e.getMessage();
	} finally {
		dbLib.close(rs);
		dbLib.close(ps);
	}

// 명칭 가져오기
	//20160108 INDEX 힌트 추가
	//String display = URLEncoder.encode(dbLib.getResult(conn, "SELECT NAME FROM TB_ORGANIZATION WHERE COMPANY_SEQ = " + company + " AND DEPTH = " + depth + " AND SORT = 0 AND ROWNUM = 1").replaceAll("'", ""), "UTF-8");
	String display = URLEncoder.encode(
			dbLib.getResult(conn,
					"SELECT /*+ INDEX(A) */"
							+ " NAME"
						+ " FROM TB_ORGANIZATION A"
						+ " WHERE COMPANY_SEQ = " + company
							+ " AND DEPTH = " + depth
							+ " AND SORT = 0"
							+ " AND ROWNUM = 1"
				).replaceAll("'", ""),
			"UTF-8"
		);

// 권한 가져오기
	String authText = "";

	if (StringEx.isEmpty(error) && mode.equals("A")) {
		try {
			//20160108 INDEX 힌트 추가
			//ps = dbLib.prepareStatement(conn, "SELECT SEQ, NAME FROM TB_AUTH WHERE COMPANY_SEQ = " + company + " AND (DEPTH >= " + (depth - 1) + " OR DEPTH = -1) ORDER BY DEPTH");
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " SEQ, NAME"
						+ " FROM TB_AUTH A"
						+ " WHERE COMPANY_SEQ = " + company
						+ " AND (DEPTH >= " + (depth - 1) + " OR DEPTH = -1)"
						+ " ORDER BY DEPTH"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				authText += (StringEx.isEmpty(authText) ? "" : ",")
						+ "{"
							+ "\"seq\" : " + rs.getLong("SEQ")
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
	}

// 계정 가져오기
	String userText = "";

	if (StringEx.isEmpty(error) && mode.equals("B") && company > 0) {
		WHERE = " WHERE COMPANY_SEQ = "
				// 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
				+ (cfg.getLong("user.company") > 0 ? cfg.getLong("user.company") : company)
			;

		
		organ = (organ > 0 ? organ: cfg.getLong("user.organ"));
		if (organ > 0) { // 조직
			//20160113 INDEX 힌트 추가, PARENT_SEQ를 SEQ로 변경하여 쿼리 병합
			//WHERE += " AND ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + organ + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + organ + ")";
			WHERE += " AND ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(C) */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION C"
							+ " WHERE SORT = 1"
							+ " START WITH SEQ = " + organ
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;
		} 
		//// 2017.06.26 jwhwang
		//if (cfg.getLong("user.organ") > 0) { // 시스템 관리자가 아닌 경우, 내 하위 조직들만 검색
			//20160113 INDEX 힌트 추가
			//WHERE += " AND ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ)";
		//	WHERE += " AND ORGANIZATION_SEQ IN ("
		//				+ " SELECT /*+ INDEX(B) */"
		//						+ " SEQ"
		//					+ " FROM TB_ORGANIZATION B"
		//					+ " WHERE SORT = 1"
		//					+ " START WITH PARENT_SEQ = " + cfg.getLong("user.organ")
		//					+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
		//			+ " )"
		//		;
		//}

		try {
			//20160113 INDEX(A PKUSER)를 INDEX(A)로 변경, 정렬(NAME, SEQ) 추가
			//ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_USER) */ SEQ, ID, NAME FROM TB_USER A WHERE " + WHERE + "UNION SELECT /*+ INDEX(A PK_USER) */ SEQ, ID, NAME FROM TB_USER A WHERE ORGANIZATION_SEQ=" + organ );
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " SEQ, ID, NAME"
						+ " FROM TB_USER A"
						+ WHERE
					+ " ORDER BY NAME, SEQ"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				userText += (StringEx.isEmpty(userText) ? "" : ",")
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
	}

// 설치 위치
	String placeText = "";

	if (StringEx.isEmpty(error) && mode.equals("C") && company > 0) {
		if (cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE = " A.COMPANY_SEQ = " + cfg.getLong("user.company");
		} else {
			WHERE = " A.COMPANY_SEQ = " + company;
		}

		if (organ > 0) { // 조직
			//20160108 INDEX 힌트 추가, PARENT_SEQ를 SEQ로 변경하여 쿼리 병합
			WHERE += " AND B.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX() */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION"
							+ " WHERE SORT = 1"
							+ " START WITH SEQ = " + organ
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;
		}

		try {
			//20160108 INDEX(A PK_USER)를 INDEX(A)로 변경
			//TODO: JOIN 순서 및 색인 확인필요(TB_SALES)
			//ps = dbLib.prepareStatement(conn, "SELECT /*+ ORDERED USE_NL(B) INDEX(A PK_VM) */ A.SEQ, A.PLACE FROM TB_VENDING_MACHINE_PLACE A INNER JOIN TB_SALES B ON A.SEQ = B.VM_PLACE_SEQ WHERE " + WHERE + " GROUP BY A.SEQ, A.PLACE");
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED INDEX(A) USE_NL(B) */"
							+ " A.SEQ, A.PLACE"
						+ " FROM TB_VENDING_MACHINE_PLACE A"
							+ " INNER JOIN TB_SALES B"
								+ " ON A.SEQ = B.VM_PLACE_SEQ"
						+ " WHERE" + WHERE
						+ " GROUP BY A.SEQ, A.PLACE"
						+ " ORDER BY A.SEQ, A.PLACE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				//20160113 [END_DATE]를 설치위치 앞에 추가
				//placeText += (StringEx.isEmpty(placeText) ? "" : ",") + "{\"seq\" : " + rs.getLong("SEQ") + ", \"name\" : \"" + URLEncoder.encode(rs.getString("PLACE").replaceAll("'", ""), "UTF-8") + "\"}";
				String endDate = rs.getString("END_DATE");
				placeText += (StringEx.isEmpty(placeText) ? "" : ",")
						+ "{"
							+ "\"seq\" : " + rs.getLong("SEQ")
							+ ", \"name\" : \"" + URLEncoder.encode(((endDate == null ? "" : "[" + endDate + "] ") + rs.getString("PLACE")).replaceAll("'", ""), "UTF-8") + "\""
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
		out.print("{\"code\" : \"SUCCESS\", \"disp\" : \"" + display + "\", \"data\" : [" + responseText + "], \"auth\" : [" + authText + "], \"user\" : [" + userText + "], \"place\" : [" + placeText + "]}");
	} else {
		out.print("{\"code\" : \"FAIL\"}");
	}
%>