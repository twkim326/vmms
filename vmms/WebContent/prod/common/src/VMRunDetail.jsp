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
 * VMRunDetail.jsp
 *
 * 메인 최근 게시물
 *
 * 작성일 - 2019/03/13, 허승찬
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
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String error = null;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}

	// 검색절
		String WHERE = " A.SEQ = " + seq;
		
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 
			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
				WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			}
			
		} else { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
		}
		
		if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색
			WHERE += " AND A.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_A) */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION"
							+ " WHERE SORT = 1"
							+ " START WITH SEQ = " + this.cfg.getLong("user.organ")
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;
		}
		//this.data = new GeneralConfig();
		
		// 자판기 정보
		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED INDEX(A) USE_NL(B C D) */"
							+ " A.*,"
							+ " B.NAME AS COMPANY,"
							+ " C.NAME AS USER_NAME,"
							+ " ( "
									+ " SELECT /*+ INDEX(E) */"
											+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
										+ " FROM TB_ORGANIZATION E"
										+ " WHERE PARENT_SEQ = 0"
										+ " START WITH SEQ = A.ORGANIZATION_SEQ"
										+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
								+ " ) AS ORGAN,"
							+ " CASE WHEN D.CONTROL_ERROR IS NOT NULL THEN FN_CONTROL_ERROR(D.CONTROL_ERROR) ELSE '' END AS CONTROL_ERROR,"
							+ " CASE WHEN D.PD_ERROR IS NOT NULL THEN FN_PD_ERROR(D.PD_ERROR) ELSE '' END AS PD_ERROR,"
							+ " CASE WHEN D.EMPTY_COL IS NOT NULL THEN FN_EMPTY_COL(D.EMPTY_COL) ELSE '' END AS EMPTY_COL,"
							+ " CASE WHEN D.SOLD_OUT IS NOT NULL THEN FN_SOLD_OUT(D.SOLD_OUT) ELSE '' END AS SOLD_OUT,"
							+ " TO_CHAR(D.CREATE_DATE, 'YYYYMMDDHH24MISS') AS TRAN_DATE,"
							+ " TO_CHAR(D.CREATE_DATE, 'YYYY-MM-DD HH24:MI:SS') AS TRAN_DATE_2"
						+ " FROM TB_VENDING_MACHINE A"
							+ " INNER JOIN TB_COMPANY B"
								+ " ON A.COMPANY_SEQ = B.SEQ"
							+ " LEFT JOIN TB_USER C"
								+ " ON A.USER_SEQ = C.SEQ"
							+ " LEFT JOIN TB_TXT_STATUS D"
								+ " ON A.TERMINAL_ID = D.TERMINAL_ID"
									+ " AND A.TRANSACTION_NO = D.TRANSACTION_NO"
						+ " WHERE" + WHERE
				);
			rs = ps.executeQuery();

			if (rs.next()) {
				vmText = "{"
					+ "\"COMPANY_SEQ\" : \"" + StringEx.comma(rs.getLong("COMPANY_SEQ")) + "\""
					+ "\"ORGANIZATION_SEQ\" : \"" + StringEx.comma(rs.getLong("ORGANIZATION_SEQ")) + "\""
					+ "\"USER_SEQ\" : \"" + StringEx.comma(rs.getLong("USER_SEQ")) + "\""
					+ "\"CODE\" : \"" + StringEx.comma(rs.getString("CODE")) + "\""
					+ "\"TERMINAL_ID\" : \"" + StringEx.comma(rs.getString("TERMINAL_ID")) + "\""
					+ "\"MODEL\" : \"" + StringEx.comma(rs.getString("MODEL")) + "\""
					+ "\"PLACE\" : \"" + StringEx.comma(rs.getString("PLACE")) + "\""
					+ "\"MODEM\" : \"" + StringEx.comma(rs.getString("MODEM")) + "\""
					+ "\"TRANSACTION_NO\" : \"" + StringEx.comma(rs.getString("TRANSACTION_NO")) + "\""
					+ "\"IS_SOLD_OUT\" : \"" + StringEx.comma(rs.getString("IS_SOLD_OUT")) + "\""
					+ "\"IS_CONTROL_ERROR\" : \"" + StringEx.comma(rs.getString("IS_CONTROL_ERROR")) + "\""
					+ "\"IS_PD_ERROR\" : \"" + StringEx.comma(rs.getString("IS_PD_ERROR")) + "\""
					+ "\"IS_EMPTY_COL\" : \"" + StringEx.comma(rs.getString("IS_EMPTY_COL")) + "\""
					+ "\"COMPANY\" : \"" + StringEx.comma(rs.getString("COMPANY")) + "\""
					+ "\"USER_NAME\" : \"" + StringEx.comma(rs.getString("USER_NAME")) + "\""
					+ "\"ORGAN\" : \"" + StringEx.comma(rs.getString("ORGAN")) + "\""
					+ "\"CONTROL_ERROR\" : \"" + StringEx.comma(rs.getString("CONTROL_ERROR")) + "\""
					+ "\"IS_PD_ERROR\" : \"" + StringEx.comma(rs.getString("PD_ERROR")) + "\""
					+ "\"EMPTY_COL\" : \"" + StringEx.comma(rs.getString("EMPTY_COL")) + "\""
					+ "\"SOLD_OUT\" : \"" + StringEx.comma(rs.getString("SOLD_OUT")) + "\""
					+ "\"TRAN_DATE\" : \"" + StringEx.comma(rs.getString("TRAN_DATE_2")) + "\""
					+ "\"IS_UPDATE\" : \"" + StringEx.isEmpty(rs.getString("TRAN_DATE")) ?"N" : "Y" + "\""
					+ "\"IS_EXPIRE\" : \"" + StringEx.isEmpty(rs.getString("TRAN_DATE"))&& DateTime.getDifferTime(rs.getString("TRAN_DATE")) > 3600 * 24 ? "Y" : "N" + "\""
				+"}"
			;
				/* this.data.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				this.data.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				this.data.put("USER_SEQ", rs.getLong("USER_SEQ"));
				this.data.put("CODE", rs.getString("CODE"));
				this.data.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				this.data.put("MODEL", rs.getString("MODEL"));
				this.data.put("PLACE", rs.getString("PLACE"));
				this.data.put("MODEM", rs.getString("MODEM"));
				this.data.put("TRANSACTION_NO", rs.getString("TRANSACTION_NO"));
				this.data.put("IS_SOLD_OUT", rs.getString("IS_SOLD_OUT"));
				this.data.put("IS_CONTROL_ERROR", rs.getString("IS_CONTROL_ERROR"));
				this.data.put("IS_PD_ERROR", rs.getString("IS_PD_ERROR"));
				this.data.put("IS_EMPTY_COL", rs.getString("IS_EMPTY_COL"));
				this.data.put("COMPANY", rs.getString("COMPANY"));
				this.data.put("USER_NAME", rs.getString("USER_NAME"));
				this.data.put("ORGAN", rs.getString("ORGAN"));
				this.data.put("CONTROL_ERROR", rs.getString("CONTROL_ERROR"));
				this.data.put("PD_ERROR", rs.getString("PD_ERROR"));
				this.data.put("EMPTY_COL", rs.getString("EMPTY_COL"));
				//20130308 자판기상태정보 변경 시작
				this.data.put("SOLD_OUT", rs.getString("SOLD_OUT"));
				//20130308 자판기상태정보 변경 종료
				this.data.put("TRAN_DATE", rs.getString("TRAN_DATE_2"));
				this.data.put("IS_UPDATE", StringEx.isEmpty(rs.getString("TRAN_DATE")) ? "N" : "Y");
				this.data.put("IS_EXPIRE", !StringEx.isEmpty(rs.getString("TRAN_DATE")) && DateTime.getDifferTime(rs.getString("TRAN_DATE")) > 3600 * 24 ? "Y" : "N"); */
			} else {
				error = "등록되지 않았거나 조회할 권한이 없는 자판기입니다.";
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 자판기 상품
		this.product = new ArrayList<GeneralConfig>();

		if (StringEx.isEmpty(error)) {
			try {
	//INDEX 힌트 변경
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ ORDERED INDEX(A) USE_NL(B) */"
								+ " A.COL_NO,"
								+ " A.PRODUCT_SEQ,"
								+ " A.IS_SOLD_OUT,"
								+ " B.NAME"
							+ " FROM TB_VENDING_MACHINE_PRODUCT A"
								+ " INNER JOIN TB_PRODUCT B"
									+ " ON A.PRODUCT_SEQ = B.SEQ"
							+ " WHERE A.VM_SEQ = ?"
							+ " ORDER BY A.COL_NO"
					);
				ps.setLong(1, seq);
				rs = ps.executeQuery();

				while (rs.next()) {
					vmProduct += (StringEx.isEmpty(vmRunText) ? "" : ",")
							+ "{"
							+ "\"COL_NO\" : " + rs.getInt("COL_NO")
							+ ", \"PRODUCT_SEQ\" : " +  rs.getLong("PRODUCT_SEQ")
							+ ", \"IS_SOLD_OUT\" : " + rs.getString("IS_SOLD_OUT")
							+ ", \"NAME\" : " + rs.getString("NAME")
						+ "}"
					;
				}
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(rs);
				dbLib.close(ps);
			}
		}

	// 판매 이상
		this.error = new ArrayList<GeneralConfig>();

		if (StringEx.isEmpty(error)) {
			try {
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " COL_NO,"
								+ " MIN(TRANSACTION_DATE || TRANSACTION_TIME) AS TRANSACTION_DATE,"
								+ " COUNT(*) AS CNT"
							+ " FROM TB_SALES A"
							+ " WHERE TERMINAL_ID = ?"
								+ " AND COL_NO > 0"
								+ " AND TRANSACTION_DATE >= TO_CHAR(SYSDATE - 7, 'YYYYMMDD')"
								+ " AND PRODUCT_CODE IS NULL"
							+ " GROUP BY COL_NO"
							+ " ORDER BY COL_NO"
					);
				ps.setString(1, this.data.get("TERMINAL_ID"));
				rs = ps.executeQuery();

				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();
					c.put("COL_NO", rs.getInt("COL_NO"));
					String date = rs.getString("TRANSACTION_DATE");
					c.put("TRANSACTION_DATE",
							date != null
								? date.substring(0, 4) + "-"
									+ date.substring(4, 6) + "-"
									+ date.substring(6, 8) + " "
									+ date.substring(8, 10) + ":"
									+ date.substring(10, 12) + ":"
									+ date.substring(12, 14)
								: ""
						);
					c.put("CNT", rs.getString("CNT"));
					this.error.add(c);
				}
			} catch (Exception e) {
				this.logger.error(e);
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