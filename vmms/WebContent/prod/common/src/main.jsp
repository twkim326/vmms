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
 * main.jsp
 *
 * 메인 최근 게시물
 *
 * 작성일 - 2011/04/04, 정원광
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
	String isNotice = StringEx.setDefaultValue(request.getParameter("isNotice"), "N");
	String isFAQ = StringEx.setDefaultValue(request.getParameter("isFAQ"), "N");
	String isVMRun = StringEx.setDefaultValue(request.getParameter("isVMRun"), "N");
	String date = request.getParameter("date");

// 실행에 사용될 변수
	DBLibrary dbLib = new DBLibrary(logger);
	Connection conn = null;
	PreparedStatement ps = null;
	ResultSet rs = null;
	String error = null;
	String WHERE = null;

// DB 연결
	conn = dbLib.getConnection(cfg.get("db.jdbc.name"), cfg.get("db.jdbc.host"), cfg.get("db.jdbc.user"), cfg.get("db.jdbc.pass"));

	if (conn == null) {
		out.print("{\"code\" : \"FAIL\"}");
		return;
	}

// 매출
	String salesText = "";

	if (!StringEx.isEmpty(date)) {
		WHERE = " TRANSACTION_DATE = '" + date + "'"
				+ " AND PAY_STEP IN ('01', '02', '03')"
				+ " AND IS_ADJUST = 'N'";

		if (cfg.get("user.operator").equals("Y")) { // 자판기 운영자
			WHERE += " AND USER_SEQ = " + cfg.get("user.seq");
		} else {
			if (cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
				WHERE += " AND COMPANY_SEQ = " + cfg.getLong("user.company");
			}

			if (cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색
//20160221 INDEX 힌트 추가, UNION 통합
// 				WHERE += " AND ORGANIZATION_SEQ IN ("
//				      +  " SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//				      +  " UNION"
//				      +  " SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + cfg.getLong("user.organ")
//				      +  " UNION"
//				      +  " SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ IN (SELECT ORGANIZATION_SEQ FROM TB_USER_APP_ORGAN WHERE SEQ = " + cfg.getLong("user.seq") + ") CONNECT BY PRIOR SEQ = PARENT_SEQ"
//				      +  " UNION"
//				      +  " SELECT ORGANIZATION_SEQ FROM TB_USER_APP_ORGAN WHERE SEQ = " + cfg.getLong("user.seq")
//				      +  " )";
 				WHERE += " AND ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(A_A) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION A_A"
								+ " WHERE SORT = 1"
								+ " START WITH SEQ IN ("
										+ " SELECT " + cfg.getLong("user.organ") + " FROM DUAL"
										+ " UNION"
										+ " SELECT /*+ INDEX(A_A_A) */"
												+ " ORGANIZATION_SEQ"
											+ " FROM TB_USER_APP_ORGAN A_A_A"
											+ " WHERE SEQ = " + cfg.getLong("user.seq")
									+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+  " )";
			}
		}

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A IX_SALES_TRANSACTION_DATE) */"
						+ " NVL(COUNT(CASE WHEN PAY_TYPE = '01' THEN 1 END), 0) AS CNT_CARD,"
						+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' THEN AMOUNT END), 0) AS AMOUNT_CARD,"
						+ " NVL(COUNT(CASE WHEN PAY_TYPE = '10' THEN 1 END), 0) AS CNT_CASH,"
						+ " NVL(SUM(CASE WHEN PAY_TYPE = '10' THEN AMOUNT END), 0) AS AMOUNT_CASH,"
						+ " NVL(COUNT(CASE WHEN PAY_TYPE = '02' THEN 1 END), 0) AS CNT_RECEIPT,"
						+ " NVL(SUM(CASE WHEN PAY_TYPE = '02' THEN AMOUNT END), 0) AS AMOUNT_RECEIPT,"
						+ " NVL(COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END), 0) AS CNT_PREPAY,"
						+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN AMOUNT END), 0) AS AMOUNT_PREPAY,"
						+ " NVL(COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END), 0) AS CNT_SMARTPAY,"
						+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN AMOUNT END), 0) AS AMOUNT_SMARTPAY,"						
						+ " NVL(COUNT(CASE WHEN PAY_TYPE IN ('01', '02','07','10', '11') THEN 1 END), 0) AS CNT_TOTAL,"
						+ " NVL(SUM(CASE WHEN PAY_TYPE IN('01', '02','07','10', '11') THEN AMOUNT END), 0) AS AMOUNT_TOTAL"
						//+ " NVL(COUNT(1), 0) AS CNT_TOTAL,"
						//+ " NVL(SUM(AMOUNT), 0) AS AMOUNT_TOTAL"
					+ " FROM TB_SALES A"
					+ " WHERE" + WHERE
				);
			rs = ps.executeQuery();

			if (rs.next()) {
				salesText = "{"
							+ "\"card\" : \"" + StringEx.comma(rs.getLong("CNT_CARD")) + "\""
							+ ", \"cash\" : \"" + StringEx.comma(rs.getLong("CNT_CASH")) + "\""
							+ ", \"receipt\" : \"" + StringEx.comma(rs.getLong("CNT_RECEIPT")) + "\""
							+ ", \"prepay\" : \"" + StringEx.comma(rs.getLong("CNT_PREPAY")) + "\""
							+ ", \"smartpay\" : \"" + StringEx.comma(rs.getLong("CNT_SMARTPAY")) + "\""
							+ ", \"total\" : \"" + StringEx.comma(rs.getLong("CNT_TOTAL")) + "\""
						+ "}, {"
							+ "\"card\" : \"" + StringEx.comma(rs.getLong("AMOUNT_CARD")) + "\""
							+ ", \"cash\" : \"" + StringEx.comma(rs.getLong("AMOUNT_CASH")) + "\""
							+ ", \"receipt\" : \"" + StringEx.comma(rs.getLong("AMOUNT_RECEIPT")) + "\""	
							+ ", \"prepay\" : \"" + StringEx.comma(rs.getLong("AMOUNT_PREPAY")) + "\""
							+ ", \"smartpay\" : \"" + StringEx.comma(rs.getLong("AMOUNT_SMARTPAY")) + "\""
							+ ", \"total\" : \"" + StringEx.comma(rs.getLong("AMOUNT_TOTAL")) + "\""
						+"}"
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

// 자판기 가동 상태
	String vmRunText = "";

	if (isVMRun.equals("Y")) {
		WHERE = "";

		try {
			if (cfg.getLong("user.company") > 0) { // 소속별 :: 조직 기준
				if (cfg.get("user.operator").equals("Y")) { // 자판기 운영자
					WHERE += " AND A.USER_SEQ = " + cfg.get("user.seq");
				} else {
					WHERE += " AND A.COMPANY_SEQ = " + cfg.getLong("user.company");

					if (cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색
//20160221 INDEX 힌트 추가, UNION 통합
//						WHERE += " AND A.ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + cfg.getLong("user.organ") + ")";
						WHERE += " AND A.ORGANIZATION_SEQ IN ("
									+ " SELECT /*+ INDEX(A_A) */"
											+ " SEQ"
										+ " FROM TB_ORGANIZATION A_A"
										+ " WHERE SORT = 1"
										+ " START WITH SEQ = " + cfg.getLong("user.organ")
										+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
								+ ")"
							;
					}
				}
				WHERE += "AND A.CODE NOT LIKE 'X%X'";
			
			WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");
//20160221 INDEX 힌트 추가, 불필요한 JOIN 제거, 쿼리통합
//				ps = dbLib.prepareStatement(conn, "SELECT"
//					+ "  COUNT(1) AS TOTAL"
//					//+ ", COUNT(CASE WHEN (SYSDATE - NVL(C.CREATE_DATE, TO_DATE('20000101', 'YYYYMMDD'))) * 24 <= 3 AND A.IS_SOLD_OUT = 'N' AND A.IS_CONTROL_ERROR = 'N' AND A.IS_PD_ERROR = 'N' AND A.IS_EMPTY_COL = 'N' THEN 1 END) AS NORMAL"
//					+ ", COUNT(CASE WHEN A.IS_SOLD_OUT = 'N' THEN 1 END) AS NORMAL"
//					+ ", A.COMPANY_SEQ"
//					+ ", A.ORGANIZATION_SEQ"
//					//+ ", (SELECT COUNT(1) FROM (SELECT SB.VM_SEQ FROM TB_SALES SA INNER JOIN TB_VENDING_MACHINE_PLACE SB ON SA.VM_PLACE_SEQ = SB.SEQ WHERE SA.COL_NO > 0 GROUP BY SB.VM_SEQ HAVING COUNT(*) > 0) TA INNER JOIN TB_VENDING_MACHINE TB ON TA.VM_SEQ = TB.SEQ WHERE TB.ORGANIZATION_SEQ = A.ORGANIZATION_SEQ) AS EMPTY_COL_SELLING"
//					+ ", (SELECT LTRIM(SYS_CONNECT_BY_PATH(NAME, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS NAME"
//					+ " FROM TB_VENDING_MACHINE A LEFT JOIN TB_TXT_STATUS C ON (A.TERMINAL_ID = C.TERMINAL_ID AND A.TRANSACTION_NO = C.TRANSACTION_NO)"
//					+ " WHERE " + WHERE
//					+ " GROUP BY A.COMPANY_SEQ, A.ORGANIZATION_SEQ");
//				rs = ps.executeQuery();
//			} else { // 시스템 관리자 :: 소속 기준
//				ps = dbLib.prepareStatement(conn, "SELECT"
//					+ "  COUNT(1) AS TOTAL"
//					//+ ", COUNT(CASE WHEN (SYSDATE - NVL(C.CREATE_DATE, TO_DATE('20000101', 'YYYYMMDD'))) * 24 <= 3 AND A.IS_SOLD_OUT = 'N' AND A.IS_CONTROL_ERROR = 'N' AND A.IS_PD_ERROR = 'N' AND A.IS_EMPTY_COL = 'N' THEN 1 END) AS NORMAL"
//					+ ", COUNT(CASE WHEN A.IS_SOLD_OUT = 'N' THEN 1 END) AS NORMAL"
//					//+ ", (SELECT COUNT(*) FROM (SELECT SB.VM_SEQ FROM TB_SALES SA INNER JOIN TB_VENDING_MACHINE_PLACE SB ON SA.VM_PLACE_SEQ = SB.SEQ WHERE SA.COL_NO > 0 GROUP BY SB.VM_SEQ HAVING COUNT(*) > 0) TA INNER JOIN TB_VENDING_MACHINE TB ON TA.VM_SEQ = TB.SEQ WHERE TB.COMPANY_SEQ = A.COMPANY_SEQ) AS EMPTY_COL_SELLING"
//					+ ", A.COMPANY_SEQ, 0 AS ORGANIZATION_SEQ, B.NAME"
//					+ " FROM TB_VENDING_MACHINE A LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ LEFT JOIN TB_TXT_STATUS C ON (A.TERMINAL_ID = C.TERMINAL_ID AND A.TRANSACTION_NO = C.TRANSACTION_NO)"
//					+ " GROUP BY A.COMPANY_SEQ, B.NAME");
//				rs = ps.executeQuery();
			}
			
			ps = dbLib.prepareStatement(conn,
					"SELECT"
							+ " COUNT(*) AS TOTAL,"
							+ " COUNT(CASE WHEN SUBSTR(A.TRANSACTION_NO, 1, 6) = TO_CHAR(SYSDATE, 'YYMMDD') THEN 1 END ) AS RESPONSE, "
						    + " COUNT(CASE WHEN SUBSTR(A.TRANSACTION_NO, 1, 6) = TO_CHAR(SYSDATE, 'YYMMDD') AND IS_SOLD_OUT = 'Y' THEN 1 END ) AS SOLDOUT, "
							+ " A.COMPANY_SEQ,"
					+ (cfg.getLong("user.company") > 0
							? " A.ORGANIZATION_SEQ,"
							+ " (" 
									+ " SELECT /*+ INDEX(B) */"
											+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
										+ " FROM TB_ORGANIZATION B"
										+ " WHERE PARENT_SEQ = 0"
										+ " START WITH SEQ = A.ORGANIZATION_SEQ"
										+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
								+ " ) AS NAME"
							: " 0 AS ORGANIZATION_SEQ,"
							+ " (" 
									+ " SELECT /*+ INDEX(B) */"
											+ " NAME"
										+ " FROM TB_COMPANY B"
										+ " WHERE SEQ = A.COMPANY_SEQ"
								+ " ) AS NAME"
						)
					+ " FROM TB_VENDING_MACHINE A"
					+ WHERE
					+ " GROUP BY A.COMPANY_SEQ" + (cfg.getLong("user.company") > 0 ? ", A.ORGANIZATION_SEQ" : "")
					+ " ORDER BY 6"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				//vmRunText += (StringEx.isEmpty(vmRunText) ? "" : ",")
				//		+ "{"
				//			+ "\"total\" : " + rs.getLong("TOTAL")
				//			+ ", \"normal\" : " + rs.getLong("NORMAL")
				//			+ ", \"wrong\" : " + (rs.getLong("TOTAL") - rs.getLong("NORMAL"))
				//			+ ", \"empty\" : " + rs.getLong("EMPTY_COL_SELLING")
				//			+ ", \"company\" : " + rs.getLong("COMPANY_SEQ")
				//			+ ", \"organ\" : " + rs.getLong("ORGANIZATION_SEQ")
				//			+ ", \"name\" : \"" + URLEncoder.encode(rs.getString("NAME").replaceAll("'", ""), "UTF-8") + "\""
				//		+ "}"
				//	;
				//vmRunText += (StringEx.isEmpty(vmRunText) ? "" : ",")
				//		+ "{"
				//			+ "\"total\" : " + rs.getLong("TOTAL")
				//			+ ", \"normal\" : " + rs.getLong("NORMAL")
				//			+ ", \"wrong\" : " + (rs.getLong("TOTAL") - rs.getLong("NORMAL"))
				//			+ ", \"empty\" : 0"
				//			+ ", \"company\" : " + rs.getLong("COMPANY_SEQ")
				//			+ ", \"organ\" : " + rs.getLong("ORGANIZATION_SEQ")
				//			+ ", \"name\" : \"" + URLEncoder.encode(rs.getString("NAME").replaceAll("'", ""), "UTF-8") + "\""
				//		+ "}"
				//	;
				vmRunText += (StringEx.isEmpty(vmRunText) ? "" : ",")
						+ "{"
							+ "\"total\" : " + rs.getLong("TOTAL")
							+ ", \"response\" : " +  rs.getLong("RESPONSE")
							+ ", \"soldout\" : " +  rs.getLong("SOLDOUT")
							+ ", \"empty\" : 0"
							+ ", \"company\" : " + rs.getLong("COMPANY_SEQ")
							+ ", \"organ\" : " + rs.getLong("ORGANIZATION_SEQ")
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

// 공지사항
	String noticeText = "";

	if (isNotice.equals("Y")) {
		try {
//20160221 INDEX 힌트 변경, ORDER BY 추가
//			ps = dbLib.prepareStatement(conn, "SELECT * FROM (SELECT /*+ INDEX_DESC(A PK_NOTICE) */ SEQ, TITLE, TO_CHAR(CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE FROM TB_NOTICE A) S1 WHERE ROWNUM <= 5");
			ps = dbLib.prepareStatement(conn,
					"SELECT *"
						+ " FROM ("
								+ " SELECT /*+ INDEX(A) */"
										+ " SEQ,"
										+ " TITLE,"
										+ " TO_CHAR(CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE"
									+ " FROM TB_NOTICE A"
									+ " ORDER BY SEQ DESC"
							+ " )"
						+ " WHERE ROWNUM <= 5"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				noticeText += (StringEx.isEmpty(noticeText) ? "" : ",")
						+ "{"
							+ "\"seq\" : " + rs.getLong("SEQ")
							+ ", \"title\" : \"" + URLEncoder.encode(rs.getString("TITLE").replaceAll("'", ""), "UTF-8") + "\""
							+ ", \"date\" : \"" + rs.getString("CREATE_DATE") + "\""
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

// FAQ
	String faqText = "";

	if (isFAQ.equals("Y")) {
		try {
//20160221 INDEX 힌트 변경, ORDER BY 추가
//			ps = dbLib.prepareStatement(conn, "SELECT * FROM (SELECT /*+ INDEX_DESC(A PK_FAQ) */ SEQ, TITLE, TO_CHAR(CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE FROM TB_FAQ A) S1 WHERE ROWNUM <= 5");
			ps = dbLib.prepareStatement(conn,
					"SELECT *"
						+ " FROM ("
								+ " SELECT /*+ INDEX(A) */"
										+ " SEQ,"
										+ " TITLE,"
										+ " TO_CHAR(CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE"
									+ " FROM TB_FAQ A"
									+ " ORDER BY SEQ DESC"
							+ " )"
						+ " WHERE ROWNUM <= 5"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				faqText += (StringEx.isEmpty(faqText) ? "" : ",")
						+ "{"
							+"\"seq\" : " + rs.getLong("SEQ")
							+ ", \"title\" : \"" + URLEncoder.encode(rs.getString("TITLE").replaceAll("'", ""), "UTF-8") + "\""
							+ ", \"date\" : \"" + rs.getString("CREATE_DATE") + "\""
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
		out.print("{\"code\" : \"SUCCESS\", \"sales\" : [" + salesText + "], \"vmRun\" : [" + vmRunText + "], \"notice\" : [" + noticeText + "], \"faq\" : [" + faqText + "]}");
	} else {
		out.print("{\"code\" : \"FAIL\", \"error\" : \"" + error + "\"}");
	}
%>