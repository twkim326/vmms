package com.nucco.beans;

import com.nucco.GlobalConfig;
import com.nucco.cfg.GeneralConfig;
import com.nucco.lib.*;
import com.nucco.lib.db.DBLibrary;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import oracle.jdbc.OracleTypes;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;

/**
 * VM.java
 * <p>
 * 자판기
 * <p>
 * 작성일 - 2011/04/02, 정원광
 */

public class VM {
/**
 * 사이트 설정
 *
 */
	private GlobalConfig cfg;
/**
 * org.apache.log4j.Logger
 *
 */
	private Logger logger = null;
/**
 * 소속
 *
 */
	public ArrayList<GeneralConfig> company;
/**
 * 공급자
 *
 */
	public ArrayList<GeneralConfig> vendor;	
/**
 * 조직
 *
 */
	public ArrayList<GeneralConfig> organ;
/**
 * 상품
 *
 */
	public ArrayList<GeneralConfig> product;
/**
 * 판매 이상
 *
 */
	public ArrayList<GeneralConfig> error;
/**
 * 목록
 *
 */
	public ArrayList<GeneralConfig> list;
	
/**
 * 단말기 공급자
 *
 */
	public ArrayList<GeneralConfig> vmVendor;

/**
 * 단말기 컬럼 종류
 *
 */
	public ArrayList<GeneralConfig> vmColumn;

/**
 * 단말기 
 *
 */
	public ArrayList<GeneralConfig> terminal;
/**
 * 사용자 
 *
 */
	public ArrayList<GeneralConfig> user;	
		
/**
 * 조회
 *
 */
	
	public GeneralConfig data;
	
//가동상태조회
	public ArrayList<GeneralConfig> data2;

/**
 * 총 레코드수
 *
 */
	public long records;
/**
 * 검색 레코드수
 *
 */
	public long records1;	
/**
 * 총 페이지수
 *
 */
	public long pages;
/**
 *
 */
	public VM(GlobalConfig cfg) throws Exception {
	// set config
		this.cfg = cfg;

	// set log4j
		PropertyConfigurator.configure(cfg.get("config.log4j"));
		this.logger = Logger.getLogger(this.getClass());
	}
/**
 * 자판기 목록
 *
 * @param company 소속
 * @param organ 조직
 * @param aspCharge ASP 과금
 * @param pageNo 페이지
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getList_org(long company, long organ, String aspCharge, int pageNo, String sField, String sQuery) throws Exception {
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

	// 검색절 생성
		String HINT = "";
		String JOIN = "";
		String WHERE = "";

		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
			WHERE += " AND A.USER_SEQ = " + this.cfg.get("user.seq");
		} else {
			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
				WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			}

			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색
//20160221 INDEX 힌트 추가, UNION 통합
//				WHERE += " AND A.ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + this.cfg.getLong("user.organ") + ")";
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(A_A) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION A_A"
								+ " WHERE SORT = 1"
								+ " START WITH SEQ = " + this.cfg.getLong("user.organ")
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
					;
			}
		}

		if (company > 0) { // 소속
			WHERE += " AND A.COMPANY_SEQ = " + company;
		}

		if (organ > 0) { // 조직
//20160221 INDEX 힌트 추가, UNION 통합
//			WHERE += " AND A.ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + organ + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + organ + ")";
			WHERE += " AND A.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_B) */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION A_B"
							+ " WHERE SORT = 1"
							+ " START WITH SEQ = " + organ
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;
		}

		if ("Y".equals(aspCharge) || "N".equals(aspCharge)) { // ASP 과금여부
			WHERE += " AND ASP_CHARGE = '" + aspCharge + "'";
		}

		if (WHERE.length() > 0) HINT = " INDEX(A)";

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			if ("C.NAME".equals(sField)) {
				HINT = " ORDERED" + HINT + " USE_HASH(C)";
				JOIN = " INNER JOIN TB_USER C"
						+ " ON A.USER_SEQ = C.SEQ"
							+ " AND C.NAME LIKE '%" + sQuery + "%'";
			} else {
				HINT = " INDEX(A)";
				WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
			}
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

	// 총 레코드수
//20160221 INDEX 힌트 추가, 불필요한 JOIN 제거
//		this.records = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_VENDING_MACHINE A LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ LEFT JOIN TB_USER C ON A.USER_SEQ = C.SEQ WHERE " + WHERE));
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT" + (HINT.length() > 0 ? "/*+" + HINT + " */" : "")
								+ " COUNT(*)"
							+ " FROM TB_VENDING_MACHINE A"
							+ JOIN
							+ WHERE
					)
			);

		HINT = HINT.replaceAll(" ORDERED", "").replaceAll(" USE_HASH\\(C\\)", "");

	// 총 페이지수
		this.pages = Pager.getSize(this.records, cfg.getInt("limit.list"));

	// 리스트 가상번호
		long no = this.records - (pageNo - 1) * cfg.getInt("limit.list");

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();

		try {
			int s = ((pageNo - 1) * cfg.getInt("limit.list")) + 1;
			int e = (s - 1) + cfg.getInt("limit.list");
//20160221 INDEX 힌트 변경/추가, REVERSE 적용
//			ps = dbLib.prepareStatement(conn, "SELECT S2.*"
//				+ " FROM"
//				+ " ("
//				+ " SELECT ROWNUM AS RNUM, S1.*"
//				+ " FROM"
//				+ " ("
//				//20130703 운영자판기 속도개선을 위한 쿼리 수정
//				//+ " SELECT /*+ ORDERED USE_NL(B C D) INDEX_DESC(A PK_VM) */ A.SEQ, A.CODE, A.PLACE, A.TERMINAL_ID, A.MODEL, A.IS_SOLD_OUT, A.IS_CONTROL_ERROR, A.IS_PD_ERROR, A.IS_EMPTY_COL, TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE, B.NAME AS COMPANY, C.NAME AS USER_NAME, TO_CHAR(D.CREATE_DATE, 'YYYYMMDDHH24MISS') AS TRAN_DATE, (SELECT LTRIM(SYS_CONNECT_BY_PATH(NAME, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS ORGAN, (SELECT COUNT(*) FROM TB_SALES WHERE TERMINAL_ID = A.TERMINAL_ID AND COL_NO > 0) AS EMPTY_COL_SELLING"
//				+ " SELECT /*+ ORDERED USE_NL(B C D) INDEX_DESC(A PK_VM) */ A.SEQ, A.CODE, A.PLACE, A.TERMINAL_ID, A.MODEL, A.IS_SOLD_OUT, A.IS_CONTROL_ERROR, A.IS_PD_ERROR, A.IS_EMPTY_COL, TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE, B.NAME AS COMPANY, C.NAME AS USER_NAME, C.ID AS USER_ID, TO_CHAR(D.CREATE_DATE, 'YYYYMMDDHH24MISS') AS TRAN_DATE, (SELECT LTRIM(SYS_CONNECT_BY_PATH(NAME, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS ORGAN "
//				+ " FROM TB_VENDING_MACHINE A"
//				+ " LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ"
//				+ " LEFT JOIN TB_USER C ON A.USER_SEQ = C.SEQ"
//				+ " LEFT JOIN TB_TXT_STATUS D ON (A.TERMINAL_ID = D.TERMINAL_ID AND A.TRANSACTION_NO = D.TRANSACTION_NO)"
//				+ " WHERE " + WHERE
//				+ " ) S1"
//				+ " WHERE ROWNUM <= " + e
//				+ " ) S2"
//				+ " WHERE RNUM >= " + s);
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED USE_NL(D) */"
							+ " A.SEQ,"
							+ " A.CODE,"
							+ " A.PLACE,"
							+ " A.TERMINAL_ID,"
							+ " A.MODEL,"
							+ " A.IS_SOLD_OUT,"
							+ " A.IS_CONTROL_ERROR,"
							+ " A.IS_PD_ERROR,"
							+ " A.IS_EMPTY_COL,"
							+ " A.ASP_CHARGE,"
							+ " TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE,"
							+ " A.COMPANY,"
							+ " A.ORGAN,"
							+ " A.USER_NAME,"
							+ " A.USER_ID,"
							+ " TO_CHAR(D.CREATE_DATE, 'YYYYMMDDHH24MISS') AS TRAN_DATE,"
							+ " ("
								+ " SELECT /*+ INDEX(E) */"
										+ " MAX(TRANSACTION_DATE || TRANSACTION_TIME)"
									+ " FROM TB_SALES E"
									+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
										+ " AND TRANSACTION_DATE >= TO_CHAR(SYSDATE - 1, 'YYYYMMDD')"
								+ ") AS TRAN_DATE_2"
						+ " FROM ("
								+ " SELECT"
										+ " ROWNUM AS ROW_NUM,"
										+ " A.*"
									+ " FROM ("
											+ " SELECT /*+ ORDERED" + HINT + " USE_HASH(B C) */"
													+ " B.NAME AS COMPANY,"
													+ " ("
															+ " SELECT /*+ INDEX(E) */"
																	+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
																+ " FROM TB_ORGANIZATION E"
																+ " WHERE PARENT_SEQ = 0"
																+ " START WITH SEQ = A.ORGANIZATION_SEQ"
																+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
														+ " ) AS ORGAN,"
														+ " A.PLACE,"
														+ " A.TERMINAL_ID,"
														+ " A.SEQ,"
														+ " A.CODE,"
														+ " A.MODEL,"
														+ " A.IS_SOLD_OUT,"
														+ " A.IS_CONTROL_ERROR,"
														+ " A.IS_PD_ERROR,"
														+ " A.IS_EMPTY_COL,"
														+ " A.ASP_CHARGE,"
														+ " A.CREATE_DATE,"
														+ " A.TRANSACTION_NO,"
														+ " C.ID AS USER_ID,"
														+ " C.NAME AS USER_NAME"
												+ " FROM TB_VENDING_MACHINE A"
													+ " INNER JOIN TB_COMPANY B"
														+ " ON A.COMPANY_SEQ = B.SEQ"
											+ (JOIN.length() > 0
													? JOIN
													: " LEFT JOIN TB_USER C"
														+ " ON A.USER_SEQ = C.SEQ"
												)
												+ WHERE
												+ " ORDER BY " + (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery) ? sField : "1, 2, 3, 4")
//												+ (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery) ? " ORDER BY " + sField : "")
										+ " ) A"
									+ " WHERE ROWNUM <= " + e
							+ " ) A"
							+ " LEFT JOIN TB_TXT_STATUS D"
								+ " ON A.TERMINAL_ID = D.TERMINAL_ID"
									+ " AND A.TRANSACTION_NO = D.TRANSACTION_NO"
						+ " WHERE ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("CODE", rs.getString("CODE"));
				c.put("PLACE", rs.getString("PLACE"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("MODEL", rs.getString("MODEL"));
				c.put("IS_SOLD_OUT", rs.getString("IS_SOLD_OUT"));
				c.put("IS_CONTROL_ERROR", rs.getString("IS_CONTROL_ERROR"));
				c.put("IS_PD_ERROR", rs.getString("IS_PD_ERROR"));
				c.put("IS_EMPTY_COL", rs.getString("IS_EMPTY_COL"));
				c.put("ASP_CHARGE", rs.getString("ASP_CHARGE"));
				c.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				c.put("COMPANY", rs.getString("COMPANY"));
				c.put("USER_NAME", rs.getString("USER_NAME"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("IS_UPDATE", StringEx.isEmpty(rs.getString("TRAN_DATE")) ? "N" : "Y");
				//20131213 3시간에서 24시간으로 변경
//				c.put("IS_EXPIRE", !StringEx.isEmpty(rs.getString("TRAN_DATE")) && DateTime.getDifferTime(rs.getString("TRAN_DATE")) > 3600 * 3 ? "Y" : "N");
//				c.put("IS_EXPIRE", !StringEx.isEmpty(rs.getString("TRAN_DATE")) && DateTime.getDifferTime(rs.getString("TRAN_DATE")) > 3600 * 24 ? "Y" : "N");
				c.put("IS_EXPIRE",
						(!StringEx.isEmpty(rs.getString("TRAN_DATE"))
									&& (DateTime.getDifferTime(rs.getString("TRAN_DATE")) <= 3600 * 24))
							|| (!StringEx.isEmpty(rs.getString("TRAN_DATE_2"))
									&& (DateTime.getDifferTime(rs.getString("TRAN_DATE_2")) <= 3600 * 24))
							? "N"
							: "Y"
					);
				////20130703 운영자판기 속도개선을 위한 쿼리 수정
				//c.put("EMPTY_COL_SELLING", rs.getLong("EMPTY_COL_SELLING"));
				c.put("EMPTY_COL_SELLING", 0);
				c.put("NO", no--);
				c.put("USER_ID", rs.getString("USER_ID"));

				this.list.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
/**
 * 자판기 목록
 *
 * @param company 소속
 * @param organ 조직
 * @param aspCharge ASP 과금
 * @param pageNo 페이지
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @param collectChk 수집오류단말기 조회
 * @return 에러가 있을 경우 에러 내용
 * 가동상태조회
 */
	//public String getList(long company, long organ, String aspCharge, int pageNo, String sField, String sQuery) throws Exception {
	public String getList(long company, long organ, String aspCharge, int pageNo, String sField, String sQuery, String collectChk) throws Exception {
	// 실행에 사용될 변수
		System.out.println("!!!!!!!!!!!");
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

	// 검색절 생성
		String HINT = "";
		String JOIN = "";
		String WHERE = "";
		String WHERE2 = "";
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 
			if (company > 0) { // 소속
				WHERE += " AND A.COMPANY_SEQ = " + company;
			}
			
			if (organ > 0) { // 조직
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_B) */"
							+ " SEQ"
						+ " FROM TB_ORGANIZATION A_B"
						+ " WHERE SORT = 1"
						+ " START WITH SEQ = " + organ
						+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
				+ " )"
				;
			}
		} else { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			
			if (organ > 0) { // 조직
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_B) */"
							+ " SEQ"
						+ " FROM TB_ORGANIZATION A_B"
						+ " WHERE SORT = 1"
						+ " START WITH SEQ = " + organ
						+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
				+ " )"
				;
			} else if (this.cfg.getLong("user.organ") > 0) {
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_A) */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION A_A"
							+ " WHERE SORT = 1"
							+ " START WITH SEQ = " + this.cfg.getLong("user.organ")
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;				
			}
			
		}
		

		if ("Y".equals(aspCharge) || "N".equals(aspCharge)) { // ASP 과금여부
			WHERE += " AND ASP_CHARGE = '" + aspCharge + "'";
		}

		if (WHERE.length() > 0) HINT = " INDEX(A)";

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			if ("C.ID".equals(sField)) {
				HINT = " ORDERED" + HINT + " USE_HASH(C)";
				JOIN = " INNER JOIN TB_USER C" 
						+ " ON A.USER_SEQ = C.SEQ"
							+ " AND C.ID LIKE '%" + sQuery + "%'";
			}else if("C.NAME".equals(sField)){
				HINT = " ORDERED" + HINT + " USE_HASH(C)";
				JOIN = " INNER JOIN TB_USER C"
						+ " ON A.USER_SEQ = C.SEQ"
						+ " AND C.NAME LIKE '%" + sQuery + "%'";
			}else if("MST.BUSINESSNO".equals(sField)){
				WHERE2 = " AND " + sField + "= '" +sQuery+"'";
			} else {
				HINT = " INDEX(A)";
				WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
			}
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");
		
		
		// 201013 cdh 상태조회 2회 이상 변경 없는 단말기 조건 추가
		if(collectChk.equals("Y") && WHERE != "") {
			//WHERE += " AND D.terminal_id = A.terminal_id" + " AND D.transaction_no = A.transaction_no" + " AND D.create_date >= sysdate -1";
			WHERE += " AND D.terminal_id = A.terminal_id" + " AND D.transaction_no = A.transaction_no";
			
			if(company == 420 || company == 1732 || company == 944) {
				if(company == 420) { // 이마트24
					WHERE += " AND (( A.MODEL LIKE '출입%' AND (trunc((to_date(to_char(sysdate,'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss') - to_date(to_char(D.create_date, 'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss')) * 24 * 60) > 30)) "
						  +  " OR ( A.MODEL NOT LIKE '출입%' AND (trunc((to_date(to_char(sysdate,'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss') - to_date(to_char(D.create_date, 'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss')) * 24 * 60) > 120))) ";
				}else{ // gs25, 에스원
					WHERE += " AND trunc((to_date(to_char(sysdate,'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss') - to_date(to_char(D.create_date, 'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss')) * 24 * 60) > 30";
				}
			}else{
				WHERE += " AND trunc((to_date(to_char(sysdate,'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss') - to_date(to_char(D.create_date, 'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss')) * 24 * 60) > 120";
			}
		}
		// 201013 cdh 상태조회 2회 이상 변경 없는 단말기 조건 추가 끝
		

	// 총 레코드수
//20160221 INDEX 힌트 추가, 불필요한 JOIN 제거
//			this.records = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_VENDING_MACHINE A LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ LEFT JOIN TB_USER C ON A.USER_SEQ = C.SEQ WHERE " + WHERE));
		
		// 201013 cdh 상태조회 2회 이상 변경 없는 단말기 리스트 찾기
		if(collectChk.equals("Y")&& WHERE != ""){
			this.records = StringEx.str2long(
					dbLib.getResult(conn,
							"SELECT" + (HINT.length() > 0 ? "/*+" + HINT + " */" : "")
									+ " COUNT(*)"
								+ " FROM TB_VENDING_MACHINE A, TB_TXT_STATUS D "
								+ JOIN
								+ WHERE
									+ (this.cfg.getLong("user.company") != 0 ?
										" AND CODE NOT LIKE 'X%X' " 
										: "")
						)
				);
		}else{
			String countQry = "";
			if(WHERE2.equals("")){
				countQry += "SELECT" + (HINT.length() > 0 ? "/*+" + HINT + " */" : "")
							+ " COUNT(*)"
							+ " FROM TB_VENDING_MACHINE A "
							+ JOIN
							+ WHERE
							+ (this.cfg.getLong("user.company") != 0 ?
							" AND CODE NOT LIKE 'X%X' "
							: "");
			}else{
				countQry += "SELECT" + (HINT.length() > 0 ? "/*+" + HINT + " */" : "")
						+ " COUNT(*)"
						+ " FROM TB_VENDING_MACHINE A "
						+ " JOIN TBLTERMMST@VANBT TERMST ON "
						+ " TERMST.TERMINALID = A.TERMINAL_ID "
						+ WHERE2.replace("MST.BUSINESSNO","TERMST.BUSINESSNO")
						+ " JOIN TBLBIZMST@VANBT MST ON "
						+ " MST.BUSINESSNO = TERMST.BUSINESSNO "
						+ " AND MST.BIZTYPE = TERMST.BIZTYPE"
						+ JOIN
						+ WHERE
						+ (this.cfg.getLong("user.company") != 0 ?
						" AND CODE NOT LIKE 'X%X' "
						: "");
			}
			this.records = StringEx.str2long(
					dbLib.getResult(conn, countQry
						)
				);
		}
		// 201013 cdh 상태조회 2회 이상 변경 없는 단말기 리스트 찾기 끝

		HINT = HINT.replaceAll(" ORDERED", "").replaceAll(" USE_HASH\\(C\\)", "");

	// 총 페이지수
		this.pages = Pager.getSize(this.records, cfg.getInt("limit.list"));

	// 리스트 가상번호
		long no = this.records - (pageNo - 1) * cfg.getInt("limit.list");

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();

		try {
			int s = ((pageNo - 1) * cfg.getInt("limit.list")) + 1;
			int e = (s - 1) + cfg.getInt("limit.list");
//20160221 INDEX 힌트 변경/추가, REVERSE 적용
//				ps = dbLib.prepareStatement(conn, "SELECT S2.*"
//					+ " FROM"
//					+ " ("
//					+ " SELECT ROWNUM AS RNUM, S1.*"
//					+ " FROM"
//					+ " ("
//					//20130703 운영자판기 속도개선을 위한 쿼리 수정
//					//+ " SELECT /*+ ORDERED USE_NL(B C D) INDEX_DESC(A PK_VM) */ A.SEQ, A.CODE, A.PLACE, A.TERMINAL_ID, A.MODEL, A.IS_SOLD_OUT, A.IS_CONTROL_ERROR, A.IS_PD_ERROR, A.IS_EMPTY_COL, TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE, B.NAME AS COMPANY, C.NAME AS USER_NAME, TO_CHAR(D.CREATE_DATE, 'YYYYMMDDHH24MISS') AS TRAN_DATE, (SELECT LTRIM(SYS_CONNECT_BY_PATH(NAME, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS ORGAN, (SELECT COUNT(*) FROM TB_SALES WHERE TERMINAL_ID = A.TERMINAL_ID AND COL_NO > 0) AS EMPTY_COL_SELLING"
//					+ " SELECT /*+ ORDERED USE_NL(B C D) INDEX_DESC(A PK_VM) */ A.SEQ, A.CODE, A.PLACE, A.TERMINAL_ID, A.MODEL, A.IS_SOLD_OUT, A.IS_CONTROL_ERROR, A.IS_PD_ERROR, A.IS_EMPTY_COL, TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE, B.NAME AS COMPANY, C.NAME AS USER_NAME, C.ID AS USER_ID, TO_CHAR(D.CREATE_DATE, 'YYYYMMDDHH24MISS') AS TRAN_DATE, (SELECT LTRIM(SYS_CONNECT_BY_PATH(NAME, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS ORGAN "
//					+ " FROM TB_VENDING_MACHINE A"
//					+ " LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ"
//					+ " LEFT JOIN TB_USER C ON A.USER_SEQ = C.SEQ"
//					+ " LEFT JOIN TB_TXT_STATUS D ON (A.TERMINAL_ID = D.TERMINAL_ID AND A.TRANSACTION_NO = D.TRANSACTION_NO)"
//					+ " WHERE " + WHERE
//					+ " ) S1"
//					+ " WHERE ROWNUM <= " + e
//					+ " ) S2"
//					+ " WHERE RNUM >= " + s);
			ps = dbLib.prepareStatement(conn,
					//"SELECT /*+ ORDERED USE_NL(D) */"
					"SELECT "
							+ " A.SEQ,"
							+ " A.CODE,"
							+ " A.PLACE,"
							+ " A.TERMINAL_ID,"
							+ " A.MODEL,"
							+ " A.IS_SOLD_OUT,"
							+ " A.IS_CONTROL_ERROR,"
							+ " A.IS_PD_ERROR,"
							+ " A.IS_EMPTY_COL,"
							+ " A.ASP_CHARGE,"
							+ " TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE,"
							+ " A.COMPANY,"
							+ " A.ORGAN,"
							+ " A.USER_NAME,"
							+ " A.USER_ID, "
							+ " A.PLACE_CODE, "
							+ " A.PLACE_NO "
							+ " ,(SELECT /*+ INDEX(D) */"
								+ " TO_CHAR(D.CREATE_DATE, 'YYYY-MM-DD HH24:MI:SS') "
							+ " FROM TB_TXT_STATUS D"
							+ " WHERE A.TERMINAL_ID = D.TERMINAL_ID"
								+ " AND A.TRANSACTION_NO = D.TRANSACTION_NO"
								+ " AND D.CREATE_DATE >= SYSDATE - 1"
							+ ") AS COLLECT_DATE" +
							", A.ACCESS_STATUS AS ACCESS_STATUS," +
							"  A.MERCHANTNAME, " +
							"  A.BUSINESSNO, " +
							"  A.BIZTYPE "
					+  (aspCharge == null ?		
							 " ,(SELECT /*+ INDEX(D) */"
								+ " TO_CHAR(D.CREATE_DATE, 'YYYYMMDDHH24MISS') "
								+ " FROM TB_TXT_STATUS D"
								+ " WHERE A.TERMINAL_ID = D.TERMINAL_ID"
									+ " AND A.TRANSACTION_NO = D.TRANSACTION_NO"
									+ " AND D.CREATE_DATE >= SYSDATE - 1"
							+ ") AS TRAN_DATE,"
							//+ " TO_CHAR(D.CREATE_DATE, 'YYYYMMDDHH24MISS') AS TRAN_DATE,"
							+ " ("
								+ " SELECT /*+ INDEX(E) */"
										+ " MAX(TRANSACTION_DATE || TRANSACTION_TIME)"
									+ " FROM TB_SALES E"
									+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
										+ " AND TRANSACTION_DATE >= TO_CHAR(SYSDATE - 1, 'YYYYMMDD')"
										+ ") AS TRAN_DATE_2, "
										// 20200421 cdh 최종거래일자 추가
										+ "( SELECT "
											+ "CONCAT(CONCAT(TO_CHAR(TO_DATE(F.TRANSACTION_DATE,'YYYYMMDD'), 'YYYY-MM-DD'), ' '), TO_CHAR(TO_DATE(F.TRANSACTION_TIME, 'HH24:MI:SS'), 'HH24:MI:SS')) "
										  + "FROM TB_SALES F " 
										  + "WHERE F.TRANSACTION_NO = (SELECT MAX(TRANSACTION_NO) FROM TB_SALES WHERE TERMINAL_ID = A.TERMINAL_ID) and F.TERMINAL_ID = A.TERMINAL_ID ) AS FINAL_TRAN_DATE"
										// 최종거래일자 끝
							: "")
						+ " FROM ("
								+ " SELECT"
										+ " ROWNUM AS ROW_NUM,"
										+ " A.*"
									+ " FROM ("
											+ " SELECT /*+ ORDERED" + HINT + " USE_HASH(B C) */"
													+ " B.NAME AS COMPANY,"
													+ " ("
															+ " SELECT /*+ INDEX(E) */"
																	+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
																+ " FROM TB_ORGANIZATION E"
																+ " WHERE PARENT_SEQ = 0"
																+ " START WITH SEQ = A.ORGANIZATION_SEQ"
																+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
														+ " ) AS ORGAN,"
														+ " CASE WHEN A.PLACE_CODE IS NULL THEN A.PLACE ELSE '[' || A.PLACE_CODE || '] ' || A.PLACE END PLACE,"
														+ " A.TERMINAL_ID,"
														+ " A.SEQ,"
														+ " A.CODE,"
														+ " A.MODEL,"
														+ " A.IS_SOLD_OUT,"
														+ " A.IS_CONTROL_ERROR,"
														+ " A.IS_PD_ERROR,"
														+ " A.IS_EMPTY_COL,"
														+ " A.ASP_CHARGE,"
														+ " A.CREATE_DATE,"
														+ " A.TRANSACTION_NO,"
														+ " A.PLACE_CODE,"
														+ " A.PLACE_NO,"
														+ " C.ID AS USER_ID,"
														+ " C.NAME AS USER_NAME,"
														+ " A.ACCESS_STATUS AS ACCESS_STATUS, "
														+ "MST.MERCHANTNAME, "
														+ "MST.BUSINESSNO, "
														+ "MST.BIZTYPE"
												+ " FROM TB_VENDING_MACHINE A"
													+ " INNER JOIN TB_COMPANY B"
														+ " ON A.COMPANY_SEQ = B.SEQ"
											+ (JOIN.length() > 0
													? JOIN
													: " LEFT JOIN TB_USER C"
														+ " ON A.USER_SEQ = C.SEQ"
													+ " LEFT JOIN TB_TXT_STATUS D"
														+ " ON A.TRANSACTION_NO = D.TRANSACTION_NO"
												)
												+"  JOIN TBLTERMMST@VANBT TERMST ON "
												+"    TERMST.TERMINALID = A.TERMINAL_ID "
												+"  JOIN TBLBIZMST@VANBT MST ON "
												+"  TERMST.BUSINESSNO = MST.BUSINESSNO AND MST.BIZTYPE=TERMST.BIZTYPE"+ WHERE2
												+ WHERE
												+ (this.cfg.getLong("user.company") != 0 ?
													" AND CODE NOT LIKE 'X%X' " 
													: "")
												+ " ORDER BY " + (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery) ? (sField.equals("A.MODEL") ? sField + ", A.COMPANY_SEQ, A.TERMINAL_ID" :sField.equals("A.BUSINESSNO") ? "1,2,3,4" : sField) : "1, 2, 3, 4")
//													+ (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery) ? " ORDER BY " + sField : "")
										+ " ) A"
									+ " WHERE ROWNUM <= " + e
							+ " ) A"
							//+ " LEFT JOIN TB_TXT_STATUS D"
							//	+ " ON A.TERMINAL_ID = D.TERMINAL_ID"
							//		+ " AND A.TRANSACTION_NO = D.TRANSACTION_NO"
							//	    + " AND D.CREATE_DATE >= SYSDATE - 1"
						+ " WHERE ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("CODE", rs.getString("CODE"));
				c.put("PLACE", rs.getString("PLACE"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("MODEL", rs.getString("MODEL"));
				c.put("IS_SOLD_OUT", rs.getString("IS_SOLD_OUT"));
				c.put("IS_CONTROL_ERROR", rs.getString("IS_CONTROL_ERROR"));
				c.put("IS_PD_ERROR", rs.getString("IS_PD_ERROR"));
				c.put("IS_EMPTY_COL", rs.getString("IS_EMPTY_COL"));
				c.put("ASP_CHARGE", rs.getString("ASP_CHARGE"));
				c.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				c.put("COMPANY", rs.getString("COMPANY"));
				c.put("USER_NAME", rs.getString("USER_NAME"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("COLLECT_DATE", rs.getString("COLLECT_DATE"));
				c.put("PLACE_CODE", rs.getString("PLACE_CODE"));
				c.put("PLACE_NO", rs.getString("PLACE_NO"));
//				c.put("IS_UPDATE", StringEx.isEmpty(rs.getString("TRAN_DATE")) ? "N" : "Y");
				//20131213 3시간에서 24시간으로 변경
//					c.put("IS_EXPIRE", !StringEx.isEmpty(rs.getString("TRAN_DATE")) && DateTime.getDifferTime(rs.getString("TRAN_DATE")) > 3600 * 3 ? "Y" : "N");
//					c.put("IS_EXPIRE", !StringEx.isEmpty(rs.getString("TRAN_DATE")) && DateTime.getDifferTime(rs.getString("TRAN_DATE")) > 3600 * 24 ? "Y" : "N");
				if (aspCharge == null) {
					c.put("IS_EXPIRE",
							(!StringEx.isEmpty(rs.getString("TRAN_DATE"))
										&& (DateTime.getDifferTime(rs.getString("TRAN_DATE")) <= 3600 * 24))
								|| (!StringEx.isEmpty(rs.getString("TRAN_DATE_2"))
										&& (DateTime.getDifferTime(rs.getString("TRAN_DATE_2")) <= 3600 * 24))
								? "N"
								: "Y"
						);
				// 20200421 cdh 최종 거래일자 추가
				c.put("FINAL_TRAN_DATE", rs.getString("FINAL_TRAN_DATE"));
				}
				////20130703 운영자판기 속도개선을 위한 쿼리 수정
				//c.put("EMPTY_COL_SELLING", rs.getLong("EMPTY_COL_SELLING"));
				c.put("EMPTY_COL_SELLING", 0);
				c.put("NO", no--);
				c.put("USER_ID", rs.getString("USER_ID"));
				// 2020.12.23 가동상태 조회시 ACCESS_STATUS 값 추가.
				c.put("ACCESS_STATUS", rs.getString("ACCESS_STATUS"));
				c.put("BUSINESSNO", rs.getString("BUSINESSNO"));
				c.put("MERCHANTNAME", rs.getString("MERCHANTNAME"));
				c.put("BIZTYPE", rs.getString("BIZTYPE"));
				this.list.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}	
/**
 * 자판기 조회
 *
 * @param seq 등록번호
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String detail(long seq) throws Exception {
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
		
		
//		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
//			WHERE += " AND A.USER_SEQ = " + this.cfg.get("user.seq");
//		} else {
//			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
//				WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
//			}
//
//			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색
//20160221 INDEX 힌트 추가, UNION 통합
//				WHERE += " AND A.ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + this.cfg.getLong("user.organ") + ")";
//				WHERE += " AND A.ORGANIZATION_SEQ IN ("
//							+ " SELECT /*+ INDEX(A_A) */"
//									+ " SEQ"
//								+ " FROM TB_ORGANIZATION"
//								+ " WHERE SORT = 1"
//								+ " START WITH SEQ = " + this.cfg.getLong("user.organ")
//								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//						+ " )"
//					;
//			}
//		}

	// 자판기 정보
		this.data2 = new ArrayList<GeneralConfig>();

		try {
//20160221 INDEX 힌트 추가, REVERSE 적용
//			//20130308 자판기상태정보 변경
//			//ps = dbLib.prepareStatement(conn, "SELECT A.*, B.NAME AS COMPANY, C.NAME AS USER_NAME, (SELECT LTRIM(SYS_CONNECT_BY_PATH(NAME, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS ORGAN, CASE WHEN D.CONTROL_ERROR IS NOT NULL THEN FN_CONTROL_ERROR(D.CONTROL_ERROR) ELSE '' END AS CONTROL_ERROR, CASE WHEN D.PD_ERROR IS NOT NULL THEN FN_PD_ERROR(D.PD_ERROR) ELSE '' END AS PD_ERROR, CASE WHEN D.EMPTY_COL IS NOT NULL THEN FN_EMPTY_COL(D.EMPTY_COL) ELSE '' END AS EMPTY_COL, TO_CHAR(D.CREATE_DATE, 'YYYYMMDDHH24MISS') AS TRAN_DATE, TO_CHAR(D.CREATE_DATE, 'YYYY-MM-DD HH24:MI:SS') AS TRAN_DATE_2 FROM TB_VENDING_MACHINE A LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ LEFT JOIN TB_USER C ON A.USER_SEQ = C.SEQ LEFT JOIN TB_TXT_STATUS D ON (A.TERMINAL_ID = D.TERMINAL_ID AND A.TRANSACTION_NO = D.TRANSACTION_NO) WHERE " + WHERE);
//			StringBuffer sbVmDetailInfo = new StringBuffer();
//			sbVmDetailInfo.append("SELECT ");
//			sbVmDetailInfo.append( 				" A.*, B.NAME AS COMPANY,  ");
//			sbVmDetailInfo.append(				" C.NAME AS USER_NAME,  ");
//			sbVmDetailInfo.append(				" ( ");
//			sbVmDetailInfo.append(					" SELECT LTRIM(SYS_CONNECT_BY_PATH(NAME, '/'), '/')  ");
//			sbVmDetailInfo.append(					" FROM TB_ORGANIZATION  ");
//			sbVmDetailInfo.append(					" WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0  ");
//			sbVmDetailInfo.append(					" CONNECT BY PRIOR SEQ = PARENT_SEQ ");
//			sbVmDetailInfo.append(				" ) AS ORGAN,  ");
//			sbVmDetailInfo.append(				" CASE WHEN D.CONTROL_ERROR IS NOT NULL THEN FN_CONTROL_ERROR(D.CONTROL_ERROR) ELSE '' END AS CONTROL_ERROR,  ");
//			sbVmDetailInfo.append(				" CASE WHEN D.PD_ERROR IS NOT NULL THEN FN_PD_ERROR(D.PD_ERROR) ELSE '' END AS PD_ERROR,  ");
//			sbVmDetailInfo.append(				" CASE WHEN D.EMPTY_COL IS NOT NULL THEN FN_EMPTY_COL(D.EMPTY_COL) ELSE '' END AS EMPTY_COL,  ");
//			sbVmDetailInfo.append(				" CASE WHEN D.SOLD_OUT IS NOT NULL THEN FN_SOLD_OUT(D.SOLD_OUT) ELSE '' END AS SOLD_OUT,  ");
//			sbVmDetailInfo.append(				" TO_CHAR(D.CREATE_DATE, 'YYYYMMDDHH24MISS') AS TRAN_DATE,  ");
//			sbVmDetailInfo.append(				" TO_CHAR(D.CREATE_DATE, 'YYYY-MM-DD HH24:MI:SS') AS TRAN_DATE_2  ");
//			sbVmDetailInfo.append(" FROM TB_VENDING_MACHINE A  ");
//			sbVmDetailInfo.append(				" LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ  ");
//			sbVmDetailInfo.append(				" LEFT JOIN TB_USER C ON A.USER_SEQ = C.SEQ  ");
//			sbVmDetailInfo.append(				" LEFT JOIN TB_TXT_STATUS D ON (A.TERMINAL_ID = D.TERMINAL_ID AND A.TRANSACTION_NO = D.TRANSACTION_NO)  ");
//			sbVmDetailInfo.append(" WHERE ");
//			sbVmDetailInfo.append(WHERE);
//
//			ps = dbLib.prepareStatement(conn, sbVmDetailInfo.toString());
//			//20130308 자판기상태정보 변경 종료
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED INDEX(A) USE_NL(B C D) */"
							+ " A.*,"
							+ " B.NAME AS COMPANY,"
							+ " C.NAME AS USER_NAME,"
							//+ " D.CAB_NO AS CAB_NO,"
							+ " ( "
									+ " SELECT /*+ INDEX(E) */"
											+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
										+ " FROM TB_ORGANIZATION E"
										+ " WHERE PARENT_SEQ = 0"
										+ " START WITH SEQ = A.ORGANIZATION_SEQ"
										+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
								+ " ) AS ORGAN,"
							+ " CASE WHEN D.CONTROL_ERROR IS NOT NULL AND SUBSTR(D.PD_ERROR, 21, 10) <> 'LVM-6141KR' AND SUBSTR(D.PD_ERROR, 21, 6) <> 'DONGGU' THEN FN_CONTROL_ERROR(D.CONTROL_ERROR) "
								+ " WHEN D.CONTROL_ERROR IS NOT NULL AND SUBSTR(D.PD_ERROR, 21, 10) = 'LVM-6141KR' THEN FN_CONTROL_ERROR_6141(D.CONTROL_ERROR) " // scheo 코레일유통 신형자판기
								+ " WHEN D.CONTROL_ERROR IS NOT NULL AND SUBSTR(D.PD_ERROR, 21, 6) = 'DONGGU' THEN FN_CONTROL_ERROR_DONGGU(D.CONTROL_ERROR) " // scheo 20201013 동구전자 신형자판기 상태정보 추가
								+ " ELSE '' END AS CONTROL_ERROR," 
							+ " CASE WHEN D.PD_ERROR IS NOT NULL THEN FN_PD_ERROR(D.PD_ERROR) ELSE '' END AS PD_ERROR,"
							+ " CASE WHEN D.EMPTY_COL IS NOT NULL THEN FN_EMPTY_COL(D.EMPTY_COL) ELSE '' END AS EMPTY_COL,"
							+ " CASE WHEN D.SOLD_OUT IS NOT NULL THEN FN_SOLD_OUT(D.SOLD_OUT) ELSE '' END AS SOLD_OUT,"
							+ " TO_CHAR(D.CREATE_DATE, 'YYYYMMDDHH24MISS') AS TRAN_DATE,"
							+ " TO_CHAR(D.CREATE_DATE, 'YYYY-MM-DD HH24:MI:SS') AS TRAN_DATE_2, "
							// 20200421 cdh 최종거래일 추가
							+ "( SELECT "
								+ "CONCAT(CONCAT(TO_CHAR(TO_DATE(F.transaction_date,'YYYYMMDD'), 'YYYY-MM-DD'), ' '), TO_CHAR(TO_DATE(F.transaction_time, 'HH24:MI:SS'), 'HH24:MI:SS')) "
							  + "FROM TB_SALES F " 
							  + "WHERE F.transaction_no = (SELECT max(transaction_no) FROM tb_sales WHERE terminal_id = A.TERMINAL_ID) and F.TERMINAL_ID = A.terminal_id ) AS FINAL_TRAN_DATE"
							// 최종거래일자 끝
						+ " FROM TB_VENDING_MACHINE A"
							+ " INNER JOIN TB_COMPANY B"
								+ " ON A.COMPANY_SEQ = B.SEQ"
							+ " LEFT JOIN TB_USER C"
								+ " ON A.USER_SEQ = C.SEQ"
							+ " LEFT JOIN TB_TXT_STATUS D"
								+ " ON A.TERMINAL_ID = D.TERMINAL_ID"
									+ " AND A.TRANSACTION_NO = D.TRANSACTION_NO"
						+ " WHERE" + WHERE
						//+ " ORDER BY CAB_NO"
				);
			rs = ps.executeQuery();

			if (rs.next()) {
				GeneralConfig c = new GeneralConfig();
				
				c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("USER_SEQ", rs.getLong("USER_SEQ"));
				c.put("CODE", rs.getString("CODE"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				//c.put("CAB_NO", rs.getString("CAB_NO"));
				c.put("MODEL", rs.getString("MODEL"));
				c.put("PLACE", rs.getString("PLACE"));
				c.put("MODEM", rs.getString("MODEM"));
				c.put("TRANSACTION_NO", rs.getString("TRANSACTION_NO"));
				c.put("IS_SOLD_OUT", rs.getString("IS_SOLD_OUT"));
				c.put("IS_CONTROL_ERROR", rs.getString("IS_CONTROL_ERROR"));
				c.put("IS_PD_ERROR", rs.getString("IS_PD_ERROR"));
				c.put("IS_EMPTY_COL", rs.getString("IS_EMPTY_COL"));
				c.put("COMPANY", rs.getString("COMPANY"));
				c.put("USER_NAME", rs.getString("USER_NAME"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("CONTROL_ERROR", rs.getString("CONTROL_ERROR"));
				c.put("PD_ERROR", rs.getString("PD_ERROR"));
				c.put("EMPTY_COL", rs.getString("EMPTY_COL"));
				//20130308 자판기상태정보 변경 시작
				c.put("SOLD_OUT", rs.getString("SOLD_OUT"));
				//20130308 자판기상태정보 변경 종료
				c.put("TRAN_DATE", rs.getString("TRAN_DATE_2"));
				c.put("IS_UPDATE", StringEx.isEmpty(rs.getString("TRAN_DATE")) ? "N" : "Y");
				//20131213 3시간에서 24시간으로 변경
//				this.data.put("IS_EXPIRE", !StringEx.isEmpty(rs.getString("TRAN_DATE")) && DateTime.getDifferTime(rs.getString("TRAN_DATE")) > 3600 * 3 ? "Y" : "N");
				c.put("IS_EXPIRE", !StringEx.isEmpty(rs.getString("TRAN_DATE")) && DateTime.getDifferTime(rs.getString("TRAN_DATE")) > 3600 * 24 ? "Y" : "N");
				//20200421 cdh 에러내용 최종거래일 추가
				c.put("FINAL_TRAN_DATE", rs.getString("FINAL_TRAN_DATE"));
				this.data2.add(c);
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
//				ps = dbLib.prepareStatement(conn, "SELECT /*+ ORDERED USE_NL(B) INDEX(A PK_VM_GOODS) */ A.COL_NO, A.GOODS_SEQ, A.IS_SOLD_OUT, B.NAME FROM TB_VENDING_MACHINE_GOODS A INNER JOIN TB_GOODS B ON A.GOODS_SEQ = B.SEQ WHERE A.VM_SEQ = ?");
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
					GeneralConfig c = new GeneralConfig();

					c.put("COL_NO", rs.getInt("COL_NO"));
					c.put("PRODUCT_SEQ", rs.getLong("PRODUCT_SEQ"));
					c.put("IS_SOLD_OUT", rs.getString("IS_SOLD_OUT"));
					c.put("NAME", rs.getString("NAME"));

					this.product.add(c);
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
//20160221 INDEX 힌트 추가, JAVA에서 날짜 포멧
//				//20130308 자판기상태정보 변경 시작
//				/*
//				ps = dbLib.prepareStatement(conn, "SELECT TRANSACTION_NO, TO_CHAR(TO_DATE(TRANSACTION_DATE || TRANSACTION_TIME, 'YYYYMMDDHH24MISS'), 'YYYY-MM-DD HH24:MI:SS') AS TRANSACTION_DATE, COL_NO FROM TB_SALES WHERE TERMINAL_ID = ? AND COL_NO > 0");
//				ps.setString(1, this.data.get("TERMINAL_ID"));
//				rs = ps.executeQuery();
//
//				while (rs.next()) {
//					GeneralConfig c = new GeneralConfig();
//
//					c.put("TRANSACTION_NO", rs.getString("TRANSACTION_NO"));
//					c.put("TRANSACTION_DATE", rs.getString("TRANSACTION_DATE"));
//					c.put("COL_NO", rs.getInt("COL_NO"));
//
//					this.error.add(c);
//				}
//				*/
//				StringBuffer sbEmptyInfo = new StringBuffer();
//
//				sbEmptyInfo.append(" SELECT          ");
//				sbEmptyInfo.append("                 COL_NO ,        ");
//				sbEmptyInfo.append("                 MIN(TO_CHAR(TO_DATE(TRANSACTION_DATE || TRANSACTION_TIME, 'YYYYMMDDHH24MISS'), 'YYYY-MM-DD HH24:MI:SS')) AS TRANSACTION_DATE,  ");
//				sbEmptyInfo.append("                 COUNT(1) CNT ");
//				sbEmptyInfo.append(" FROM TB_SALES  ");
//				sbEmptyInfo.append(" WHERE TERMINAL_ID = ? ");
//				sbEmptyInfo.append(" AND COL_NO > 0 ");
//				sbEmptyInfo.append(" GROUP BY COL_NO ");
//				sbEmptyInfo.append(" order by col_no ");
//
//				ps = dbLib.prepareStatement(conn, sbEmptyInfo.toString());
//				//20130308 자판기상태정보 변경 종료
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
				GeneralConfig d = (GeneralConfig) this.data2.get(0);
				
				ps.setString(1, d.get("TERMINAL_ID"));
				rs = ps.executeQuery();

				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();
					//20130308 자판기상태정보 변경 시작
					/*
					c.put("TRANSACTION_NO", rs.getString("TRANSACTION_NO"));
					c.put("TRANSACTION_DATE", rs.getString("TRANSACTION_DATE"));
					c.put("COL_NO", rs.getInt("COL_NO"));
					*/
					c.put("COL_NO", rs.getInt("COL_NO"));
//					c.put("TRANSACTION_DATE", rs.getString("TRANSACTION_DATE"));
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
					//20130308 자판기상태정보 변경 종료
					this.error.add(c);
				}
				//20130308 자판기상태정보 변경 시작
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

		return error;
	}
/**
 * 마감 목록
 *
 * @param company 소속
 * @param organ 조직
 * @param pageNo 페이지
 * @param sDate 검색 시작일
 * @param eDate 검색 종료일 
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getClosingList(long company, long organ,  int pageNo, String sDate, String eDate,  String sQuery) throws Exception {
	
		if ( StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
			return null;
		}
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
	// 검색절 생성
		String WHERE = "";	
		String WHERE1 = "";
		
		 // 시작일
		WHERE = " WHERE CLOSING_DATE >= '" + sDate + "'";
		// 종료일
		WHERE += " AND CLOSING_DATE <= '" + eDate + "'";
		
		
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 
			if(company > 0 || organ > 0 || sQuery != ""){
				if (company > 0) { // 소속
					WHERE1 += " SELECT  /*+ INDEX(A) */"
							+ " TERMINAL_ID "
							+ " FROM TB_VENDING_MACHINE A"
							+ " WHERE A.COMPANY_SEQ = " + company;
					
				}
				
				if (organ > 0) { // 조직
					WHERE1 += " AND A.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_B) */"
							+ " SEQ"
						+ " FROM TB_ORGANIZATION A_B"
						+ " WHERE SORT = 1"
						+ " START WITH SEQ = " + organ
						+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
					;
				}
			} else {	//빠른페이지 이동위한 임시수정 scheo
				WHERE1 += " SELECT  /*+ INDEX(A) */"
						+ " TERMINAL_ID "
						+ " FROM TB_VENDING_MACHINE A"
						+ " WHERE A.TERMINAL_ID = '3000000003'";
			}
		} else { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE1 += " SELECT  /*+ INDEX(A) */"
					+ " TERMINAL_ID "
					+ " FROM TB_VENDING_MACHINE A"
					+ " WHERE A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			

			
			if (organ > 0) { // 조직
				WHERE1 += " AND A.ORGANIZATION_SEQ IN ("
					+ " SELECT /*+ INDEX(A_B) */"
						+ " SEQ"
					+ " FROM TB_ORGANIZATION A_B"
					+ " WHERE SORT = 1"
					+ " START WITH SEQ = " + organ
					+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
				+ " )"
				;
			} else if (this.cfg.getLong("user.organ") > 0) {
				WHERE1 += " AND A.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_A) */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION A_A"
							+ " WHERE SORT = 1"
							+ " START WITH SEQ = " + this.cfg.getLong("user.organ")
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;				
			}
			
		}
		
		if (!StringEx.isEmpty(WHERE1)) {
			WHERE += " AND TERMINAL_ID IN ( "
				+ WHERE1
				+ " )"
			;
		}
		
		if (!StringEx.isEmpty(sQuery)) {
			WHERE += " AND TERMINAL_ID = '"
				+ sQuery
				+ "'"
			;
		}
			
	// 총 레코드수
		this.records = StringEx.str2long(
			dbLib.getResult(conn,
					"SELECT COUNT(*)"
					+ " FROM ("
						+ "SELECT /*+ INDEX(A) */"
							+ " COUNT(*)"
						+ " FROM TB_TXT_CLOSING A"
						+ WHERE
						+ " GROUP BY TERMINAL_ID, CLOSING_DATE || CLOSING_TIME"
					+ ")"
				)
		);	
	// 총 페이지수
		this.pages = Pager.getSize(this.records, cfg.getInt("limit.list"));

	// 리스트 가상번호
		long no = this.records - (pageNo - 1) * cfg.getInt("limit.list");		
		
	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();
		
		try {
			int s = ((pageNo - 1) * cfg.getInt("limit.list")) + 1;
			int e = (s - 1) + cfg.getInt("limit.list");

			ps = dbLib.prepareStatement(conn,
					"SELECT  /*+ ORDERED USE_NL(B C) */"
						+ " A.*"
						+ ", TO_CHAR(A.CLOSING_DATE, 'YYYY-MM-DD HH24:MI:SS') AS CLOSING_DATE1"
						+ ", TO_CHAR(A.REG_DATE, 'YYYY-MM-DD HH24:MI:SS') AS REG_DATE1"
						+ ", CASE WHEN B.PLACE_CODE IS NULL THEN B.PLACE ELSE '[' || B.PLACE_CODE || '] ' || B.PLACE END PLACE"
						+ ", B.MODEL"
						+ ", C.NAME AS COMPANY"
						+ ", ("
							+ " SELECT /*+ INDEX(E) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
								+ " FROM TB_ORGANIZATION E"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = B.ORGANIZATION_SEQ"
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						+ " ) AS ORGAN"
						+ ", ("	
							+ " SELECT COUNT(*) FROM TB_SALESCOUNT WHERE TERMINAL_ID = A.TERMINAL_ID AND COUNT_DATE = A.CLOSING_DATE"
						+ " ) AS SALES_COUNT"
					+ " FROM ("
						+ " SELECT"
								+ " ROWNUM AS ROW_NUM,"
								+ " A.*"
							+ " FROM ("
						    	+ " SELECT  /*+ INDEX(C) */"
								    	+ " TO_DATE(CLOSING_DATE||CLOSING_TIME, 'YYYYMMDDHH24MISS') CLOSING_DATE"
								    	+ " , TERMINAL_ID"
								        + " , MAX(CREATE_DATE) AS REG_DATE"
							        + " FROM TB_TXT_CLOSING C"
							        + WHERE
							        + " GROUP BY TERMINAL_ID, CLOSING_DATE || CLOSING_TIME "
							        + " ORDER BY 1 DESC, TERMINAL_ID"
								+ " ) A"
							+ " WHERE ROWNUM <= " + e
					+ ") A"
						+ " INNER JOIN TB_VENDING_MACHINE B"
							+ " ON A.TERMINAL_ID = B.TERMINAL_ID"
						+ " INNER JOIN TB_COMPANY C"
							+ " ON B.COMPANY_SEQ = C.SEQ"
					+ " WHERE A.ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("COMPANY", rs.getString("COMPANY"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("CLOSING_DATE", rs.getString("CLOSING_DATE1"));
				c.put("REG_DATE", rs.getString("REG_DATE1"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("PLACE", rs.getString("PLACE"));
				c.put("MODEL", rs.getString("MODEL"));
				c.put("SALES_COUNT", rs.getInt("SALES_COUNT"));

				c.put("NO", no--);

				this.list.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}
		
		
	// 리소스 반환
		dbLib.close(conn);

		return error;
	}	
/**
 * 자판기 목록
 *
 * @param company 소속
 * @param organ 조직
 * @param pageNo 페이지
 * @param sDate 검색 시작일
 * @param eDate 검색 종료일 
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 * 판매계수현황조회
 */
	public String getSalesCountList(long company, long organ,  int pageNo, String sDate, String eDate, String sField, String sQuery, long search_flag) throws Exception { //search_flag 추가 scheo 20200519
	
		if (StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
			return null;
		}
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
	// 검색절 생성
		String WHERE = "";	
		String WHERE1 = "";
		String WHERE2 = "";
		
		 // 시작일    // scheo 20200803 판매계수현황조회 쿼리 튜닝
//		WHERE = " WHERE TO_CHAR(COUNT_DATE, 'YYYYMMDD') >= '" + sDate + "'";
		WHERE = " WHERE COUNT_DATE >= TO_DATE('" + sDate + "000000', 'YYYYMMDDHH24MISS')";
		// 종료일
//		WHERE += " AND TO_CHAR(COUNT_DATE, 'YYYYMMDD') <= '" + eDate + "'";
		WHERE += " AND COUNT_DATE <= TO_DATE('" + eDate + "235959', 'YYYYMMDDHH24MISS')";
		
		
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 
			if(company > 0 || organ > 0 || sField != ""){
				if (company > 0) { // 소속
					WHERE1 += " SELECT  /*+ INDEX(A) */"
							+ " TERMINAL_ID "
							+ " FROM TB_VENDING_MACHINE A"
							+ " WHERE A.COMPANY_SEQ = " + company;
					
				}
				
				if (organ > 0) { // 조직
					WHERE1 += " AND A.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_B) */"
							+ " SEQ"
						+ " FROM TB_ORGANIZATION A_B"
						+ " WHERE SORT = 1"
						+ " START WITH SEQ = " + organ
						+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
					;
				}
			} else {	//빠른페이지 이동위한 임시수정 scheo
				WHERE1 += " SELECT  /*+ INDEX(A) */"
						+ " TERMINAL_ID "
						+ " FROM TB_VENDING_MACHINE A"
						+ " WHERE A.TERMINAL_ID = '3000000003'";
			}
		} else { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색					////////scheo 20200331 빠른페이지 이동위한 임시수정
			if ( StringEx.isEmpty(sQuery) && search_flag == 0) { // 키워드
				//	this.cfg.getLong("user.company") == 264	) {
				WHERE1 += " SELECT  /*+ INDEX(A) */"
						+ " TERMINAL_ID "
						+ " FROM TB_VENDING_MACHINE A"
						+ " WHERE A.TERMINAL_ID = '3000000003'"; 
			} else {
				WHERE1 += " SELECT  /*+ INDEX(A) */"
						+ " TERMINAL_ID "
						+ " FROM TB_VENDING_MACHINE A"
						+ " WHERE A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
				
				if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자는 등록된 자판기만 볼 수 있음
					WHERE1 += " AND USER_SEQ = " + this.cfg.get("user.seq");
				}
	
				
				if (organ > 0) { // 조직
					WHERE1 += " AND A.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_B) */"
							+ " SEQ"
						+ " FROM TB_ORGANIZATION A_B"
						+ " WHERE SORT = 1"
						+ " START WITH SEQ = " + organ
						+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
					;
				} else if (this.cfg.getLong("user.organ") > 0) {
					WHERE1 += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(A_A) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION A_A"
								+ " WHERE SORT = 1"
								+ " START WITH SEQ = " + this.cfg.getLong("user.organ")
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
					;				
				}
			}
		}
		
		if (!StringEx.isEmpty(WHERE1)) {
			WHERE += " AND B.TERMINAL_ID IN ( "
				+ WHERE1
				+ " )"
			;
		}
		
		/*if (!StringEx.isEmpty(sQuery)) {
			WHERE += " AND TERMINAL_ID = '"
				+ sQuery
				+ "'"
			;
		}*/
		
		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			WHERE2 += " AND " + sField + " LIKE '%" + sQuery + "%' ";
		}
		
	// 총 레코드수
		this.records = StringEx.str2long(
			dbLib.getResult(conn,
					"SELECT COUNT(*)"
					+ " FROM ("
						+ "SELECT /*+ INDEX(A) */"
							+ " COUNT(*)"
						+ " FROM TB_SALESCOUNT A, TB_VENDING_MACHINE B"
						+ WHERE
						+ " AND A.TERMINAL_ID = B.TERMINAL_ID "
						+ WHERE2
						+ " GROUP BY COUNT_DATE, A.TERMINAL_ID"
					+ ")"
				)
		);	
	// 총 페이지수
		this.pages = Pager.getSize(this.records, cfg.getInt("limit.list"));

	// 리스트 가상번호
		long no = this.records - (pageNo - 1) * cfg.getInt("limit.list");		
		
	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();
		
		try {
			int s = ((pageNo - 1) * cfg.getInt("limit.list")) + 1;
			int e = (s - 1) + cfg.getInt("limit.list");

			ps = dbLib.prepareStatement(conn,
					"SELECT  /*+ ORDERED USE_NL(B C) */"
						+ " A.*"
						+ " , TO_CHAR(A.COUNT_DATE, 'YYYY-MM-DD HH24:MI:SS') AS COUNT_DATE1"
						+ " , TO_CHAR(A.REG_DATE, 'YYYY-MM-DD HH24:MI:SS') AS REG_DATE1"
						+ " , CASE WHEN B.PLACE_CODE IS NULL THEN B.PLACE ELSE '[' || B.PLACE_CODE || '] ' || B.PLACE END PLACE"
						+ " , TRIM(B.PLACE_CODE) PLACE_CODE"
						+ " , C.SEQ AS COMPANY_SEQ"
						+ " , C.NAME AS COMPANY"
						+ " , ("
							+ " SELECT /*+ INDEX(E) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
								+ " FROM TB_ORGANIZATION E"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = B.ORGANIZATION_SEQ"
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						+ " ) AS ORGAN"
						+ " , ("
							+ "SELECT  /*+ INDEX(F) */"
								+ " TRIM(GOODS_CODE)"
								+ " FROM TB_SALESCOUNT F "
								+ " WHERE COUNT_DATE = A.COUNT_DATE"
									+ " AND TERMINAL_ID = A.TERMINAL_ID"
									+ " AND COL_NO = 0"
							+ " ) AS PLACE_CD"
						+ " FROM ("
							+ " SELECT"
									+ " ROWNUM AS ROW_NUM,"
									+ " A.*"
								+ " FROM ("
							    	+ " SELECT  /*+ INDEX(B,C) */"
									    	+ " C.COUNT_DATE"
									    	+ " , C.TERMINAL_ID"
									        + " , C.COUNT_MODE"
									        + " , C.COL_COUNT"
									        + " , SUM(DECODE(TRIM(C.GOODS_CODE), '1111111', 0, '9999999', 0, C.PRICE )) AS PRICE"	// scheo 20191104 등록가 판매가차이
									        + " , SUM(DECODE(TRIM(C.GOODS_CODE), '1111111', 0, '9999999', 0, P.PRICE )) AS PRODUCT_PRICE"	// scheo 20191104 등록가 판매가차이
									        + " , MAX(C.COL_NO) AS COL_NO_MAX"
									        + " , MIN(C.COL_NO) AS COL_NO_MIN"
									        + " , COUNT(*) AS CNT"
									        + " , MIN(C.REG_DATE) AS REG_DATE"
									        + " , MIN(C.STATE) AS STATE"
								        + " FROM TB_SALESCOUNT C" // scheo 20191104 등록가 판매가차이	
								        + " INNER JOIN TB_VENDING_MACHINE B"	// scheo 20191104 등록가 판매가차이
								        	+ " ON C.TERMINAL_ID = B.TERMINAL_ID"
								        + " LEFT OUTER JOIN TB_PRODUCT P"
								        	+ " ON TRIM(C.GOODS_CODE) = P.CODE AND B.COMPANY_SEQ = P.COMPANY_SEQ"
								        + WHERE
								        + WHERE2
								        + " GROUP BY C.COUNT_DATE, C.TERMINAL_ID, C.COUNT_MODE, C.COL_COUNT"
								        + " ORDER BY REG_DATE DESC, TERMINAL_ID"
									+ " ) A"
								+ " WHERE ROWNUM <= " + e
						+ ") A"
						+ " INNER JOIN TB_VENDING_MACHINE B"
							+ " ON A.TERMINAL_ID = B.TERMINAL_ID"
						+ " INNER JOIN TB_COMPANY C"
							+ " ON B.COMPANY_SEQ = C.SEQ"
						+ " WHERE A.ROW_NUM >= " + s
						+ " ORDER BY " + (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery) ? sField + ", 1, 2" : "1, 2")
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("COMPANY_SEQ", rs.getString("COMPANY_SEQ"));
				c.put("COMPANY", rs.getString("COMPANY"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("COUNT_DATE", rs.getString("COUNT_DATE1"));
				c.put("REG_DATE", rs.getString("REG_DATE1"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("COUNT_MODE", rs.getString("COUNT_MODE"));
				c.put("PLACE", rs.getString("PLACE"));
				c.put("PRICE", rs.getString("PRICE"));	// scheo 20191104 등록가 판매가차이
				c.put("PRODUCT_PRICE", rs.getString("PRODUCT_PRICE"));
				c.put("PLACE_CODE", rs.getString("PLACE_CODE"));
				c.put("PLACE_CD", rs.getString("PLACE_CD"));
				c.put("COL_COUNT", rs.getLong("COL_COUNT"));
				c.put("COL_NO_MAX", rs.getLong("COL_NO_MAX"));
				c.put("COL_NO_MIN", rs.getLong("COL_NO_MIN"));
				c.put("CNT", rs.getLong("CNT"));
				c.put("STATE", rs.getLong("STATE"));


				c.put("NO", no--);

				this.list.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}
		
		
	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
/**
 * 자판기 목록
 *
 * @param company 소속
 * @param organ 조직
 * @param pageNo 페이지
 * @param sDate 검색 시작일
 * @param eDate 검색 종료일 
 *
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getKTCCertKeyRenewalList(long company, long organ,  int pageNo, String sDate, String eDate,  String sQuery) throws Exception {
	
		if (StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
			return null;
		}
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
	// 검색절 생성
		String WHERE = "";	
		String WHERE1 = "";
		
		// 키갱신 
		WHERE = " WHERE  PAY_TYPE = '09' AND PAY_STEP = '01' ";
		// 시작일
		WHERE += " AND TRANSACTION_DATE >= '" + sDate + "'";
		// 종료일
		WHERE += " AND TRANSACTION_DATE <= '" + eDate + "'";
		
		
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 
			if (company > 0) { // 소속
				WHERE1 += " SELECT  /*+ INDEX(A) */"
						+ " TERMINAL_ID "
						+ " FROM TB_VENDING_MACHINE A"
						+ " WHERE A.COMPANY_SEQ = " + company;
				
			}
			
			if (organ > 0) { // 조직
				WHERE1 += " AND A.ORGANIZATION_SEQ IN ("
					+ " SELECT /*+ INDEX(A_B) */"
						+ " SEQ"
					+ " FROM TB_ORGANIZATION A_B"
					+ " WHERE SORT = 1"
					+ " START WITH SEQ = " + organ
					+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
				+ " )"
				;
			}
		} else { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE1 += " SELECT  /*+ INDEX(A) */"
					+ " TERMINAL_ID "
					+ " FROM TB_VENDING_MACHINE A"
					+ " WHERE A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			
			if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자는 등록된 자판기만 볼 수 있음
				WHERE1 += " AND USER_SEQ = " + this.cfg.get("user.seq");
			}

			
			if (organ > 0) { // 조직
				WHERE1 += " AND A.ORGANIZATION_SEQ IN ("
					+ " SELECT /*+ INDEX(A_B) */"
						+ " SEQ"
					+ " FROM TB_ORGANIZATION A_B"
					+ " WHERE SORT = 1"
					+ " START WITH SEQ = " + organ
					+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
				+ " )"
				;
			} else if (this.cfg.getLong("user.organ") > 0) {
				WHERE1 += " AND A.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_A) */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION A_A"
							+ " WHERE SORT = 1"
							+ " START WITH SEQ = " + this.cfg.getLong("user.organ")
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;				
			}
			
		}
		
		if (!StringEx.isEmpty(WHERE1)) {
			WHERE += " AND TERMINAL_ID IN ( "
				+ WHERE1
				+ " )"
			;
		}
		
		if (!StringEx.isEmpty(sQuery)) {
			WHERE += " AND TERMINAL_ID = '"
				+ sQuery
				+ "'"
			;
		}
			
	// 총 레코드수
		this.records = StringEx.str2long(
			dbLib.getResult(conn,
					"SELECT /*+ INDEX(A) */"
						+ " COUNT(*)"
					+ " FROM TB_SALES A"
					+ WHERE
				)
		);	
	// 총 페이지수
		this.pages = Pager.getSize(this.records, cfg.getInt("limit.list"));

	// 리스트 가상번호
		long no = this.records - (pageNo - 1) * cfg.getInt("limit.list");		
		
	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();
		
		try {
			int s = ((pageNo - 1) * cfg.getInt("limit.list")) + 1;
			int e = (s - 1) + cfg.getInt("limit.list");

			ps = dbLib.prepareStatement(conn,
					"SELECT  /*+ ORDERED USE_NL(B C) */"
						+ " A.TERMINAL_ID"
						+ " , TO_CHAR(TO_DATE(A.TRANSACTION_DATE || A.TRANSACTION_TIME, 'YYYYMMDDHH24MISS'), 'YYYY-MM-DD HH24:MI:SS') AS RENEWAL_DATE"
						+ " , B.PLACE"
						+ " , A.TRANSACTION_NO"
						+ " , TRIM(B.CODE) CODE"
						+ " , C.SEQ AS COMPANY_SEQ"
						+ " , C.NAME AS COMPANY"
						+ " , ("
							+ " SELECT /*+ INDEX(E) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
								+ " FROM TB_ORGANIZATION E"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = B.ORGANIZATION_SEQ"
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						+ " ) AS ORGAN"
						+ " FROM ("
							+ " SELECT"
									+ " ROWNUM AS ROW_NUM,"
									+ " A.*"
								+ " FROM ("
							    	+ " SELECT  /*+ INDEX(C) */"
									    	+ " TRANSACTION_DATE"
									    	+ " , TRANSACTION_TIME"
									    	+ " , TERMINAL_ID"
									    	+ " , TRANSACTION_NO"
								        + " FROM TB_SALES C"
								        + WHERE
								        + " ORDER BY TRANSACTION_DATE DESC, TRANSACTION_TIME DESC, TERMINAL_ID"
									+ " ) A"
								+ " WHERE ROWNUM <= " + e
						+ ") A"
						+ " INNER JOIN TB_VENDING_MACHINE B"
							+ " ON A.TERMINAL_ID = B.TERMINAL_ID"
						+ " INNER JOIN TB_COMPANY C"
							+ " ON B.COMPANY_SEQ = C.SEQ"
						+ " WHERE A.ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("COMPANY_SEQ", rs.getString("COMPANY_SEQ"));
				c.put("COMPANY", rs.getString("COMPANY"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("RENEWAL_DATE", rs.getString("RENEWAL_DATE"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("TRANSACTION_NO", rs.getString("TRANSACTION_NO"));
				c.put("CODE", rs.getString("CODE"));
				c.put("PLACE", rs.getString("PLACE"));

				c.put("NO", no--);

				this.list.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}
		
		
	// 리소스 반환
		dbLib.close(conn);

		return error;
	}	
/**
 * 자판기 목록
 *
 * @param terminal_id 단말기ID
 * @param count_date 수집일시
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getSalesCountDetail(String terminal_id, String count_date) throws Exception {
	
		if (StringEx.isEmpty(terminal_id) || StringEx.isEmpty(count_date)) {
			return null;
		}
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
		
		this.list = new ArrayList<GeneralConfig>();
		
		try {

			ps = dbLib.prepareStatement(conn,
					"SELECT  /*+ INDEX(A) */"
						+ " A.TERMINAL_ID, "
						+ " TO_CHAR(A.COUNT_DATE, 'YYYY-MM-DD HH24:MI:SS') COUNT_DATE, "
						+ " A.COUNT_TYPE, "
						+ " A.COUNT_MODE, "
						+ " A.COL_COUNT, "
						+ " A.COL_NO, "
						+ " A.TOTAL_AMOUNT, "
						+ " A.TOTAL_COUNT, "	
						+ " A.CASH_AMOUNT, "
						+ " A.CASH_COUNT, "
						+ " A.CARD_AMOUNT, "
						+ " A.CARD_COUNT, "
						+ " A.TEST_AMOUNT, "
						+ " A.TEST_COUNT, "		
						+ " A.FREE_AMOUNT, "	
						+ " A.FREE_COUNT, "
						+ " A.PRICE, "
						+ " A.GOODS_CODE, "
						+ " (SELECT PRICE FROM TB_PRODUCT WHERE CODE = TRIM(A.GOODS_CODE) AND COMPANY_SEQ = (SELECT COMPANY_SEQ FROM TB_VENDING_MACHINE WHERE TERMINAL_ID = '" + terminal_id + "')) PRODUCT_PRICE, "
						+ " CASE COL_NO WHEN 0 THEN (SELECT PLACE FROM TB_VENDING_MACHINE WHERE TERMINAL_ID = '" + terminal_id + "') "
				        	+ " ELSE (SELECT NVL(NAME, '미등록상품') FROM TB_PRODUCT WHERE CODE = TRIM(A.GOODS_CODE) AND COMPANY_SEQ = (SELECT COMPANY_SEQ FROM TB_VENDING_MACHINE WHERE TERMINAL_ID = '" + terminal_id + "'))"
				        + " END PRODUCT_NAME,"
						+ " A.TEST_CODE, "
						+ " CASE COL_NO WHEN 0 THEN ' ' "
			        		+ " ELSE (SELECT NVL(NAME, '미등록상품') FROM TB_PRODUCT WHERE CODE = TRIM(A.TEST_CODE) AND COMPANY_SEQ = (SELECT COMPANY_SEQ FROM TB_VENDING_MACHINE WHERE TERMINAL_ID = '" + terminal_id + "'))"
			        	+ " END TEST_PRODUCT_NAME,"
						+ " TO_CHAR(A.REG_DATE, 'YYYY-MM-DD HH24:MI:SS') REG_DATE "						
					+ " FROM TB_SALESCOUNT A "
					+ " WHERE "
						+ " A.TERMINAL_ID = '" + terminal_id + "'"
						+ " AND A.COUNT_DATE = TO_DATE('" + count_date + "', 'YYYY-MM-DD HH24:MI:SS') "
					+ " ORDER BY COL_NO"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("COUNT_DATE", rs.getString("COUNT_DATE"));
				c.put("COUNT_TYPE", rs.getString("COUNT_TYPE"));
				c.put("COUNT_MODE", rs.getString("COUNT_MODE"));
				c.put("COL_COUNT", rs.getLong("COL_COUNT"));
				c.put("COL_NO", rs.getLong("COL_NO"));
				c.put("TOTAL_AMOUNT", rs.getLong("TOTAL_AMOUNT"));
				c.put("TOTAL_COUNT", rs.getLong("TOTAL_COUNT"));
				c.put("CASH_AMOUNT", rs.getLong("CASH_AMOUNT"));
				c.put("CASH_COUNT", rs.getLong("CASH_COUNT"));
				c.put("CARD_AMOUNT", rs.getLong("CARD_AMOUNT"));
				c.put("CARD_COUNT", rs.getLong("CARD_COUNT"));
				c.put("TEST_AMOUNT", rs.getLong("TEST_AMOUNT"));
				c.put("TEST_COUNT", rs.getLong("TEST_COUNT"));
				c.put("FREE_AMOUNT", rs.getLong("FREE_AMOUNT"));
				c.put("FREE_COUNT", rs.getLong("FREE_COUNT"));
				c.put("PRICE", rs.getLong("PRICE"));
				c.put("PRODUCT_PRICE", rs.getLong("PRODUCT_PRICE")); // scheo 20191104 등록가 판매가 금액차이 수정 
				c.put("GOODS_CODE", rs.getString("GOODS_CODE"));
				c.put("PRODUCT_NAME", rs.getString("PRODUCT_NAME"));
				c.put("TEST_CODE", rs.getString("TEST_CODE"));
				c.put("TEST_PRODUCT_NAME", rs.getString("TEST_PRODUCT_NAME"));
				c.put("REG_DATE", rs.getString("REG_DATE"));
				
				this.list.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}
		
		// 리소스 반환
		dbLib.close(conn);

		return error;
	}

/**
 * 자판기 목록
 *
 * @param company 소속
 * @param organ 조직
 * @param pageNo 페이지
 * @param sDate 검색 시작일
 * @param eDate 검색 종료일 
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @param salesCount 검색어 
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getSalesCountCheckList(long company, long organ,  int pageNo, String sDate, String eDate,  String sQuery, String salesCount) throws Exception {
	
		if ( StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
			return null;
		}
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String error = null;
		long krr = 264L;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}
	// 검색절 생성
		String WHERE = "";	
		String WHERE1 = "";	
		String WHERE_TERMINAL = "";
		
		 // 시작일
		WHERE = " AND TO_CHAR(COUNT_DATE, 'YYYYMMDD') >= '" + sDate + "'";
		// 종료일
		WHERE += " AND TO_CHAR(COUNT_DATE, 'YYYYMMDD') <= '" + eDate + "'";
		WHERE +=  (company==krr? 
	                " AND ("
	            	+ " (COL_COUNT <= 24 AND   (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE C.TERMINAL_ID = TERMINAL_ID AND C.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1) "
	                + " OR  (COL_COUNT > 24 AND (SELECT MAX(COL_NO) FROM TB_SALESCOUNT WHERE C.TERMINAL_ID = TERMINAL_ID AND C.COUNT_DATE = COUNT_DATE) = (COL_COUNT ))) "
	            + ")" : " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE C.TERMINAL_ID = TERMINAL_ID AND C.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64, 57)"
		        );
		
		WHERE1 = " AND TO_CHAR(COUNT_DATE, 'YYYYMMDD') <= '" + eDate + "'";
		WHERE1 += (company==krr? 
	                " AND ("
	            	+ " (COL_COUNT <= 24 AND   (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE C.TERMINAL_ID = TERMINAL_ID AND C.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1) "
	                + " OR  (COL_COUNT > 24 AND (SELECT MAX(COL_NO) FROM TB_SALESCOUNT WHERE C.TERMINAL_ID = TERMINAL_ID AND C.COUNT_DATE = COUNT_DATE) = (COL_COUNT ))) "
	            + ")" : " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE C.TERMINAL_ID = TERMINAL_ID AND C.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64, 57)"
		        );

		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 
			if (company > 0) { // 소속
				WHERE_TERMINAL += " SELECT  /*+ INDEX(A) */"
						+ " TERMINAL_ID "
						+ " FROM TB_VENDING_MACHINE A"
						+ " WHERE A.COMPANY_SEQ = " + company;
			}
			
			if (organ > 0) { // 조직
				WHERE_TERMINAL += " AND A.ORGANIZATION_SEQ IN ("
					+ " SELECT /*+ INDEX(A_B) */"
						+ " SEQ"
					+ " FROM TB_ORGANIZATION A_B"
					+ " WHERE SORT = 1"
					+ " START WITH SEQ = " + organ
					+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
				+ " )"
				;
			}
		} else { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE_TERMINAL += " SELECT  /*+ INDEX(A) */"
					+ " TERMINAL_ID "
					+ " FROM TB_VENDING_MACHINE A"
					+ " WHERE A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			

			
			if (organ > 0) { // 조직
				WHERE_TERMINAL += " AND A.ORGANIZATION_SEQ IN ("
					+ " SELECT /*+ INDEX(A_B) */"
						+ " SEQ"
					+ " FROM TB_ORGANIZATION A_B"
					+ " WHERE SORT = 1"
					+ " START WITH SEQ = " + organ
					+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
				+ " )"
				;
			} else if (this.cfg.getLong("user.organ") > 0) {
				WHERE_TERMINAL += " AND A.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_A) */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION A_A"
							+ " WHERE SORT = 1"
							+ " START WITH SEQ = " + this.cfg.getLong("user.organ")
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;				
			}
			
		}
		
		if (!StringEx.isEmpty(WHERE_TERMINAL)) {
			WHERE += " AND TERMINAL_ID IN ( "
				+ WHERE_TERMINAL
				+ " )"
			;
			
			WHERE1 += " AND TERMINAL_ID IN ( "
					+ WHERE_TERMINAL
					+ " )"
		    ;
		}
		
		if (!StringEx.isEmpty(sQuery)) {
			WHERE += " AND TERMINAL_ID = '"
				+ sQuery
				+ "'"
			;
			
			WHERE1 += " AND TERMINAL_ID = '"
					+ sQuery
					+ "'"
				;
		}
	//총  레코드수
			this.records = StringEx.str2long(
				dbLib.getResult(conn,
					 " SELECT COUNT(*)"	        
					 + " FROM ("
					        + " SELECT /*+ INDEX(C) */ "
					            + " TERMINAL_ID"
		                        + ", MIN(COUNT_DATE) AS MIN_COUNT_DATE"
		                    + " FROM TB_SALESCOUNT C"
		                    + " WHERE COUNT_DATE > TO_DATE('20160101', 'YYYYMMDD')"
		                        + " AND TERMINAL_ID NOT LIKE '%AA%'"
		                        + " AND STATE = 0 "
		                        + WHERE1
		                    + " GROUP BY TERMINAL_ID"
		                  + " ) C1"
			              + " LEFT OUTER JOIN ("
					    	+ " SELECT  /*+ INDEX(C) */"
							    	+ " MAX(COUNT_DATE) AS COUNT_DATE"
							    	+ " , TERMINAL_ID"
							        + " , COUNT_MODE"
							        + " , COL_COUNT"
							        + " , MAX(COL_NO) AS COL_NO_MAX"
							        + " , MIN(COL_NO) AS COL_NO_MIN"
							        + " , COUNT(*) AS CNT"
							        + " , MIN(REG_DATE) AS REG_DATE"
						        + " FROM TB_SALESCOUNT C"
							    + " WHERE 1=1 "
							    	+ " AND STATE = 0 "
						        	+ WHERE
						        + " GROUP BY TERMINAL_ID,  COUNT_MODE, COL_COUNT"
						        + " ORDER BY REG_DATE DESC, TERMINAL_ID"
						   + " ) C2"
						   + " ON C1.TERMINAL_ID = C2.TERMINAL_ID  "     
					)
			);			
	// 검색 레코드수
		this.records1 = StringEx.str2long(
			dbLib.getResult(conn,
				 " SELECT COUNT(*)"	        
				 + " FROM ("
				        + " SELECT /*+ INDEX(C) */ "
				            + " TERMINAL_ID"
	                        + ", MIN(COUNT_DATE) AS MIN_COUNT_DATE"
	                    + " FROM TB_SALESCOUNT C"
	                    + " WHERE COUNT_DATE > TO_DATE('20160101', 'YYYYMMDD')"
	                        + " AND TERMINAL_ID NOT LIKE '%AA%'"
	                        + " AND STATE = 0 "
	                        + WHERE1
	                    + " GROUP BY TERMINAL_ID"
	                  + " ) C1"
		              + " LEFT OUTER JOIN ("
				    	+ " SELECT  /*+ INDEX(C) */"
						    	+ " MAX(COUNT_DATE) AS COUNT_DATE"
						    	+ " , TERMINAL_ID"
						        + " , COUNT_MODE"
						        + " , COL_COUNT"
						        + " , MAX(COL_NO) AS COL_NO_MAX"
						        + " , MIN(COL_NO) AS COL_NO_MIN"
						        + " , COUNT(*) AS CNT"
						        + " , MIN(REG_DATE) AS REG_DATE"
					        + " FROM TB_SALESCOUNT C"
						    + " WHERE 1=1 "
						    	+ " AND STATE = 0 "
					        	+ WHERE
					        + " GROUP BY TERMINAL_ID, COUNT_MODE, COL_COUNT"
					        + " ORDER BY REG_DATE DESC, TERMINAL_ID"
					   + " ) C2"
					   + " ON C1.TERMINAL_ID = C2.TERMINAL_ID  "     
				  + (salesCount.equals("Y")?" WHERE C2.COUNT_DATE IS NOT NULL"
						:(salesCount.equals("N")?" WHERE C2.COUNT_DATE IS  NULL":"")
				    )
				)
		);	
	// 총 페이지수
		this.pages = Pager.getSize(this.records1, cfg.getInt("limit.list"));

	// 리스트 가상번호
		long no = this.records1 - (pageNo - 1) * cfg.getInt("limit.list");		

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();
		
		try {
			int s = ((pageNo - 1) * cfg.getInt("limit.list")) + 1;
			int e = (s - 1) + cfg.getInt("limit.list");

			ps = dbLib.prepareStatement(conn,
					"SELECT  /*+ ORDERED USE_NL(B C) */"
						+ " A.*"
						+ " , TO_CHAR(A.COUNT_DATE, 'YYYY-MM-DD HH24:MI:SS') AS COUNT_DATE1"
						+ " , TO_CHAR(A.REG_DATE, 'YYYY-MM-DD HH24:MI:SS') AS REG_DATE1"
						+ ", CASE WHEN B.PLACE_CODE IS NULL THEN B.PLACE ELSE '[' || B.PLACE_CODE || '] ' || B.PLACE END PLACE"
						+ " , C.NAME AS COMPANY"
						+ " , ("
							+ " SELECT /*+ INDEX(E) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
								+ " FROM TB_ORGANIZATION E"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = B.ORGANIZATION_SEQ"
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						+ " ) AS ORGAN"
						+ " , ("
							+ "SELECT  /*+ INDEX(F) */"
								+ " GOODS_CODE"
								+ " FROM TB_SALESCOUNT F "
								+ " WHERE COUNT_DATE = A.COUNT_DATE"
									+ " AND TERMINAL_ID = A.TERMINAL_ID"
									+ " AND COL_NO = 0"
							+ " ) AS PLACE_CD"
						+ " FROM ("
							+ " SELECT"
									+ " ROWNUM AS ROW_NUM,"
									+ " A.*"
									+ ", ( CASE WHEN A.COUNT_DATE IS NOT NULL THEN"
											+ " (SELECT TO_CHAR(MAX(COUNT_DATE), 'YYYY-MM-DD HH24:MI:SS')"
											+ " FROM TB_SALESCOUNT B"
											+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
												+ " AND STATE = 0 "
												+ " AND TO_CHAR(COUNT_DATE, 'YYYY-MM-DD') < TO_CHAR(A.COUNT_DATE, 'YYYY-MM-DD')"
												+ " AND TO_CHAR(COUNT_DATE, 'YYYY-MM-DD') > SUBSTR(MIN_COUNT_DATE, 1, 10)"
												+ " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE B.TERMINAL_ID = TERMINAL_ID AND B.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64, 57))"
										 + "ELSE "
										 	+ " (SELECT TO_CHAR(MAX(COUNT_DATE), 'YYYY-MM-DD HH24:MI:SS')"
											+ " FROM TB_SALESCOUNT B"
											+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
												+ " AND STATE = 0 "
												+ " AND TO_CHAR(COUNT_DATE, 'YYYYMMDD') <= '" + eDate + "'"
												+ " AND TO_CHAR(COUNT_DATE, 'YYYY-MM-DD') > SUBSTR(MIN_COUNT_DATE, 1, 10)"
												+ " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE B.TERMINAL_ID = TERMINAL_ID AND B.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64, 57))"
									+ "END) PREV_COUNT_DATE"
								+ " FROM ("
									+ " SELECT"
								        + " C1.TERMINAL_ID"
									    + ", TO_CHAR(C1.MIN_COUNT_DATE, 'YYYY-MM-DD HH24:MI:SS') AS MIN_COUNT_DATE"
								        + ", C2.COUNT_DATE"
									    + ", C2.COUNT_MODE"
								        + ", C2.COL_COUNT"
									    + ", C2.COL_NO_MAX"
								        + ", C2.COL_NO_MIN"
									    + ", C2.CNT"
								        + ", C2.REG_DATE"
									 + " FROM ("
									        + " SELECT /*+ INDEX(C) */ "
									            + " TERMINAL_ID"
						                        + ", MIN(COUNT_DATE) AS MIN_COUNT_DATE"
						                    + " FROM TB_SALESCOUNT C"
						                    + " WHERE COUNT_DATE > TO_DATE('20160101', 'YYYYMMDD')"
						                        + " AND TERMINAL_ID NOT LIKE '%AA%'"
						                        + " AND STATE = 0 "
						                        + WHERE1
						                    + " GROUP BY TERMINAL_ID"
						                  + " ) C1"
							              + " LEFT OUTER JOIN ("
									    	+ " SELECT  /*+ INDEX(C) */"
											    	+ " MAX(COUNT_DATE) AS COUNT_DATE"
											    	+ " , TERMINAL_ID"
											        + " , COUNT_MODE"
											        + " , COL_COUNT"
											        + " , MAX(COL_NO) AS COL_NO_MAX"
											        + " , MIN(COL_NO) AS COL_NO_MIN"
											        + " , COUNT(*) AS CNT"
											        + " , MIN(REG_DATE) AS REG_DATE"
										        + " FROM TB_SALESCOUNT C"
											    + " WHERE 1=1 "
											    	+ " AND STATE = 0 "
										        	+ WHERE
										        + " GROUP BY TERMINAL_ID,  COUNT_MODE, COL_COUNT"
										        + " ORDER BY REG_DATE DESC, TERMINAL_ID"
										   + " ) C2"
										   + " ON C1.TERMINAL_ID = C2.TERMINAL_ID  "     
									   + (salesCount.equals("Y")?" WHERE C2.COUNT_DATE IS NOT NULL"
												:(salesCount.equals("N")?" WHERE C2.COUNT_DATE IS  NULL":"")
										    )
									   + " ORDER BY C2.COUNT_DATE DESC NULLS LAST, C1.MIN_COUNT_DATE"
									+ " ) A"
								+ " WHERE ROWNUM <= " + e
						+ ") A"
						+ " INNER JOIN TB_VENDING_MACHINE B"
							+ " ON A.TERMINAL_ID = B.TERMINAL_ID"
						+ " INNER JOIN TB_COMPANY C"
							+ " ON B.COMPANY_SEQ = C.SEQ"
						+ " WHERE A.ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("COMPANY", rs.getString("COMPANY"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("COUNT_DATE", rs.getString("COUNT_DATE1"));
				c.put("MIN_COUNT_DATE", rs.getString("MIN_COUNT_DATE"));
				c.put("PREV_COUNT_DATE", rs.getString("PREV_COUNT_DATE"));
				c.put("REG_DATE", rs.getString("REG_DATE1"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("COUNT_MODE", rs.getString("COUNT_MODE"));
				c.put("PLACE", rs.getString("PLACE"));
				c.put("PLACE_CD", rs.getString("PLACE_CD"));
				c.put("COL_COUNT", rs.getLong("COL_COUNT"));
				c.put("COL_NO_MAX", rs.getLong("COL_NO_MAX"));
				c.put("COL_NO_MIN", rs.getLong("COL_NO_MIN"));
				c.put("CNT", rs.getLong("CNT"));


				c.put("NO", no--);

				this.list.add(c);
			}
			
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}
		
		
	// 리소스 반환
		dbLib.close(conn);

		return error;
	}	
/**
 * 자판기 등록
 *
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String registPlace() throws Exception {
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

	// 소속
		long user_company = this.cfg.getLong("user.company");

		this.company = new ArrayList<GeneralConfig>();

		try {
//				ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_COMPANY) */ SEQ, NAME FROM TB_COMPANY A WHERE (CASE WHEN ? > 0 THEN SEQ ELSE 1 END) = (CASE WHEN ? > 0 THEN ? ELSE 1 END)");
//				ps.setLong(1, this.cfg.getLong("user.company"));
//				ps.setLong(2, this.cfg.getLong("user.company"));
//				ps.setLong(3, this.cfg.getLong("user.company"));
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A PK_COMPANY) */"
							+ " SEQ,"
							+ " NAME"
						+ " FROM TB_COMPANY A"
				+ (user_company > 0
						? " WHERE SEQ = ?"
						: ""
					)
						+ " ORDER BY NAME"
				);
			if (user_company > 0) ps.setLong(1, user_company);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("NAME", rs.getString("NAME"));

				this.company.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}

	// 리소스 반환
		dbLib.close(conn);

		return null;
	}	
/**
 * 자판기  위치 등록 등록
 *
 * @param seq 등록번호
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String registPlace(long seq) throws Exception {
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
		
		this.data = new GeneralConfig();
		
		if (seq > 0) {
			// 자판기 등록 정보
			String WHERE = " A.SEQ = " + seq;
			
			try {
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A)*/"
						+ " A.*"	
						+ " FROM TB_INSTALL_PLACE A"
						+ " WHERE" + WHERE
					);
				rs = ps.executeQuery();

				if (rs.next()) {
					this.data.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
					this.data.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
					this.data.put("USER_SEQ", rs.getLong("USER_SEQ"));
					this.data.put("USER_SEQ", rs.getLong("USER_SEQ"));
					this.data.put("VM_SEQ", rs.getLong("VM_SEQ"));
					this.data.put("CODE", rs.getString("CODE"));
					this.data.put("NAME", rs.getString("NAME"));
					this.data.put("PLACE", rs.getString("PLACE"));
					this.data.put("VM_VENDOR", rs.getLong("VM_VENDOR"));
					this.data.put("VM_COLUMN", rs.getLong("VM_COLUMN"));
					this.data.put("START_DATE", rs.getString("START_DATE"));
					this.data.put("END_DATE", rs.getString("END_DATE"));
				} else {
					error = "등록되지 않았거나 수정할 권한이 없는 자판기입니다.";
				}
			} catch (Exception e) {
				this.logger.error(e);
				e.printStackTrace();
				error = e.getMessage();
			} finally {
				dbLib.close(rs);
				dbLib.close(ps);
			}
		}
	
		// 자판기 공급자
		this.vmVendor = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(TB_CODE) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE"
						+ " WHERE TYPE = 'VM_VENDOR'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("CODE", rs.getString("CODE"));
				c.put("NAME", rs.getString("NAME"));

				this.vmVendor.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}
		
		// 자판기 종류
		this.vmColumn = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(TB_CODE) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE"
						+ " WHERE TYPE = 'VM_COLUMN'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("CODE", rs.getString("CODE"));
				c.put("NAME", rs.getString("NAME"));

				this.vmColumn.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}
		
		// 리소스 반환
		dbLib.close(conn);
		
		return error;

	}
	
/**
 * 자판기 설치장소 등록
 *
 * @param seq 등록번호
 * @param company 소속
 * @param organ 조직
 * @param user 담당계정
 * @param terminal 단말기 ID
 * @param code 코드
 * @param name 모델
 * @param place 설치위치
 * @param vmVendor 자판기 제조사
 * @param vmColumn 자판기 종류
 * @param sDate 시작일
 * @param eDate 종료일
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String registPlace(long seq, long company, long organ, long user,  long terminal, String code, String name, String place, long vmVendor, long vmColumn, String sDate, String eDate) throws Exception {
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		String error = null;
		long sequence = 0;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}
		
		try {
			
			if (seq == 0) {
			// 등록	
				ps = dbLib.prepareStatement(conn,
					"INSERT INTO TB_INSTALL_PLACE ("
							+ " SEQ,"
							+ " COMPANY_SEQ,"
							+ " ORGANIZATION_SEQ,"
							+ " USER_SEQ,"
							+ " VM_SEQ,"
							+ " CODE,"
							+ " NAME,"
							+ " PLACE,"
							+ " VM_VENDOR,"
							+ " VM_COLUMN,"
							+ " START_DATE,"
							+ " CREATE_USER_SEQ,"
							+ " CREATE_DATE"
						+ " ) VALUES ("
							+ " SQ_INSTALL_PLACE.NEXTVAL,"
							+ " ?,"
							+ " ?,"
							+ " ?,"
							+ " ?,"
							+ " ?,"
							+ " ?,"
							+ " ?,"
							+ " ?,"
							+ " ?,"
							+ " ?,"
							+ " ?,"
							+ " SYSDATE"
						+ " )"
					);
				
				ps.setLong(1, company);
				ps.setLong(2, organ);
				ps.setLong(3, user);
				ps.setLong(4, terminal);
				ps.setString(5, code);
				ps.setString(6, name);	
				ps.setString(7, place);	
				ps.setLong(8, vmVendor);
				ps.setLong(9, vmColumn);
				ps.setString(10, sDate);
				ps.setLong(11, this.cfg.getLong("user.seq"));
				ps.executeUpdate();
				
			} else {
			// 수정		
				ps = dbLib.prepareStatement(conn,
						"UPDATE /*+ INDEX(A) */ TB_INSTALL_PLACE A"
							+ " SET COMPANY_SEQ = ?,"
								+ " ORGANIZATION_SEQ = ?,"
								+ " USER_SEQ = ?,"
								+ " VM_SEQ = ?,"
								+ " CODE = ?,"
								+ " NAME = ?,"
								+ " PLACE = ?,"
								+ " VM_VENDOR = ?,"
								+ " VM_COLUMN = ?,"
								+ " START_DATE = ?,"
								+ " END_DATE = ?,"
								+ " MODIFY_USER_SEQ = ?,"
								+ " MODIFY_DATE = SYSDATE"
							+ " WHERE SEQ = ?"
					);
				ps.setLong(1, company);
				ps.setLong(2, organ);
				ps.setLong(3, user);
				ps.setLong(4, terminal);
				ps.setString(5, code);
				ps.setString(6, name);
				ps.setString(7, place);
				ps.setLong(8, vmVendor);
				ps.setLong(9, vmColumn);
				ps.setString(10, sDate);
				ps.setString(11, eDate);
				ps.setLong(12, this.cfg.getLong("user.seq"));
				ps.setLong(13, seq);
				ps.executeUpdate();
			}
			
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(ps);
		}

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
/**
 * 삭제
 *
 * @param seq 등록번호
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String deletePlace(long seq) throws Exception {
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		String error = null;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}

	// 삭제
		if (StringEx.isEmpty(error)) {
			try {
				ps = dbLib.prepareStatement(conn,
						"DELETE /*+ INDEX(A) */"
							+ " FROM TB_INSTALL_PLACE A"
							+ " WHERE SEQ = ?"
					);
				ps.setLong(1, seq);
				ps.executeUpdate();
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(ps);
			}
		}

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}	
/**
 * 자판기 설치장소 목록
 *
 * @param company 소속
 * @param organ 조직
 * @param vmVendor 자판기 제조사 
 * @param vmColumn 자판기 컬럼타입
 * @param pageNo 페이지
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getPlaceList(long company, long organ, long vmVendor, long vmColumn, int pageNo, String sField, String sQuery) throws Exception {
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

	// 검색절 생성
		String HINT = "";
		String JOIN = "";
		String WHERE = "";
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 
			if (company > 0) { // 소속
				WHERE += " AND A.COMPANY_SEQ = " + company;
			}
			
			if (organ > 0) { // 조직
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_B) */"
							+ " SEQ"
						+ " FROM TB_ORGANIZATION A_B"
						+ " WHERE SORT = 1"
						+ " START WITH SEQ = " + organ
						+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
				+ " )"
				;
			}
		} else { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			
			if (organ > 0) { // 조직
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_B) */"
							+ " SEQ"
						+ " FROM TB_ORGANIZATION A_B"
						+ " WHERE SORT = 1"
						+ " START WITH SEQ = " + organ
						+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
				+ " )"
				;
			} else if (this.cfg.getLong("user.organ") > 0) {
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_A) */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION A_A"
							+ " WHERE SORT = 1"
							+ " START WITH SEQ = " + this.cfg.getLong("user.organ")
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;				
			}
			
		}
		
		if (vmVendor > 0) {
			WHERE += " AND VM_VENDOR = " + vmVendor;
		}
		
		if (vmColumn > 0) {
			WHERE += " AND VM_COLUMN = " + vmColumn;
		}

		if (WHERE.length() > 0) HINT = " INDEX(A)";

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			if ("E.TERMINAL_ID".equals(sField)) {
				HINT = " ORDERED" + HINT + " USE_HASH(E)";
				JOIN = " INNER JOIN TB_VENDING_MACHINE E"
						+ " ON A.VM_SEQ = E.SEQ"
							+ " AND E.TERMINAL_ID LIKE '%" + sQuery + "%'";
			} else {
				HINT = " INDEX(A)";
				WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
			}
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

	// 총 레코드수
//20160221 INDEX 힌트 추가, 불필요한 JOIN 제거
//				this.records = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_VENDING_MACHINE A LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ LEFT JOIN TB_USER C ON A.USER_SEQ = C.SEQ WHERE " + WHERE));
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT" + (HINT.length() > 0 ? "/*+" + HINT + " */" : "")
								+ " COUNT(*)"
							+ " FROM TB_INSTALL_PLACE A"
							+ JOIN
							+ WHERE
					)
			);

		HINT = HINT.replaceAll(" ORDERED", "").replaceAll(" USE_HASH\\(C\\)", "");

	// 총 페이지수
		this.pages = Pager.getSize(this.records, cfg.getInt("limit.list"));

	// 리스트 가상번호
		long no = this.records - (pageNo - 1) * cfg.getInt("limit.list");

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();

		try {
			int s = ((pageNo - 1) * cfg.getInt("limit.list")) + 1;
			int e = (s - 1) + cfg.getInt("limit.list");

			ps = dbLib.prepareStatement(conn,
					"SELECT "
							+ " A.*"
						+ " FROM ("
								+ " SELECT"
										+ " ROWNUM AS ROW_NUM,"
										+ " A.*"
									+ " FROM ("
											+ " SELECT /*+ ORDERED" + HINT + " USE_HASH(B C) */"
														+ " A.SEQ,"
														+ " A.CODE,"
														+ " A.NAME,"
														+ " A.PLACE,"
														+ " (SELECT NAME FROM TB_CODE WHERE TYPE='VM_VENDOR' AND TO_NUMBER(CODE) =  A.VM_VENDOR) AS VM_VENDOR, "
														+ " (SELECT NAME FROM TB_CODE WHERE TYPE='VM_COLUMN' AND TO_NUMBER(CODE) =  A.VM_COLUMN) AS VM_COLUMN, "
														+ " TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE,"
														+ " B.NAME AS COMPANY,"
														+ " ("
														+ " SELECT /*+ INDEX(E) */"
																+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
															+ " FROM TB_ORGANIZATION E"
															+ " WHERE PARENT_SEQ = 0"
															+ " START WITH SEQ = A.ORGANIZATION_SEQ"
															+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
															+ " ) AS ORGAN,"
														+ " C.ID AS USER_ID,"
														+ " C.NAME AS USER_NAME,"
														+ " D.TERMINAL_ID"
												+ " FROM TB_INSTALL_PLACE A"
													+ " INNER JOIN TB_COMPANY B"
														+ " ON A.COMPANY_SEQ = B.SEQ"
													+ " INNER JOIN TB_USER C"
														+ " ON A.USER_SEQ = C.SEQ"
													+ " INNER JOIN TB_VENDING_MACHINE D"
														+ " ON A.VM_SEQ = D.SEQ "		
												+ JOIN
												+ WHERE
												+ " ORDER BY " + (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery) ? sField : "1, 2, 3, 4")
//														+ (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery) ? " ORDER BY " + sField : "")
										+ " ) A"
									+ " WHERE ROWNUM <= " + e
							+ " ) A"
						+ " WHERE ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("CODE", rs.getString("CODE"));
				c.put("NAME", rs.getString("NAME"));
				c.put("PLACE", rs.getString("PLACE"));
				c.put("VM_VENDOR", rs.getString("VM_VENDOR"));
				c.put("VM_COLUMN", rs.getString("VM_COLUMN"));
				c.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				c.put("COMPANY", rs.getString("COMPANY"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("USER_ID", rs.getString("USER_ID"));
				c.put("USER_NAME", rs.getString("USER_NAME"));
				
				c.put("NO", no--);

				this.list.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

		
		// 자판기 공급자
		this.vmVendor = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(TB_CODE) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE"
						+ " WHERE TYPE = 'VM_VENDOR'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("CODE", rs.getString("CODE"));
				c.put("NAME", rs.getString("NAME"));

				this.vmVendor.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}
		
		// 자판기 종류
		this.vmColumn = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(TB_CODE) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE"
						+ " WHERE TYPE = 'VM_COLUMN'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("CODE", rs.getString("CODE"));
				c.put("NAME", rs.getString("NAME"));

				this.vmColumn.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}
		
	// 리소스 반환
		dbLib.close(conn);

		return error;
	}		
/**
 * 자판기 등록
 *
 * @param company 소속
 * @param organ 조직
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getPlaceTerminalList(long company, long organ) throws Exception {	
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

		String WHERE = "";
		if (organ > 0) { // 조직
			WHERE += " AND A.ORGANIZATION_SEQ IN ("
					+ " SELECT /*+ INDEX(A_B) */"
						+ " SEQ"
					+ " FROM TB_ORGANIZATION A_B"
					+ " WHERE SORT = 1"
					+ " START WITH SEQ = " + organ
					+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
			+ " )"
			;
		}
		// 자판기 종류
		this.terminal = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " SEQ,"
							+ " TERMINAL_ID,"
							+ " PLACE"
						+ " FROM TB_VENDING_MACHINE A"
						+ " WHERE COMPANY_SEQ = " + company
						+ WHERE
						+ " ORDER BY TERMINAL_ID"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getString("SEQ"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("PLACE", rs.getString("PLACE"));

				this.terminal.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

		
		// 리소스 반환
		dbLib.close(conn);

		return error;
	}
/**
 * 사용자 목록
 *
 * @param company 소속
 * @param organ 조직
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getPlaceUserList(long company, long organ) throws Exception {	
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

		String WHERE = "";
		if (organ > 0) { // 조직
			WHERE += " AND A.ORGANIZATION_SEQ IN ("
					+ " SELECT /*+ INDEX(A_B) */"
						+ " SEQ"
					+ " FROM TB_ORGANIZATION A_B"
					+ " WHERE SORT = 1"
					+ " START WITH SEQ = " + organ
					+ " CONNECT BY  SEQ = PRIOR PARENT_SEQ"
			+ " )"
			;
		}
		// 자판기 종류
		this.user = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " SEQ,"
							+ " ID,"
							+ " NAME"
						+ " FROM TB_USER A"
						+ " WHERE COMPANY_SEQ = " + company
						+ WHERE
						+ " ORDER BY ID"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getString("SEQ"));
				c.put("ID", rs.getString("ID"));
				c.put("NAME", rs.getString("NAME"));
				this.user.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

		
		// 리소스 반환
		dbLib.close(conn);

		return error;
	}	
/**
 * 자판기 등록
 *
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist() throws Exception {
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

	// 소속
		long user_company = this.cfg.getLong("user.company");

		this.company = new ArrayList<GeneralConfig>();

		try {
//			ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_COMPANY) */ SEQ, NAME FROM TB_COMPANY A WHERE (CASE WHEN ? > 0 THEN SEQ ELSE 1 END) = (CASE WHEN ? > 0 THEN ? ELSE 1 END)");
//			ps.setLong(1, this.cfg.getLong("user.company"));
//			ps.setLong(2, this.cfg.getLong("user.company"));
//			ps.setLong(3, this.cfg.getLong("user.company"));
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A PK_COMPANY) */"
							+ " SEQ,"
							+ " NAME"
						+ " FROM TB_COMPANY A"
				+ (user_company > 0
						? " WHERE SEQ = ?"
						: ""
					)
						+ " ORDER BY NAME"
				);
			if (user_company > 0) ps.setLong(1, user_company);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("NAME", rs.getString("NAME"));

				this.company.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}

	// 리소스 반환
		dbLib.close(conn);

		return null;
	}
/**
 * 자판기 등록
 *
 * @param seq 등록번호
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist_org(long seq) throws Exception {
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
		
	// 자판기 정보
		this.data = new GeneralConfig();

		if (seq > 0) {
		// 자판기 등록 정보
			String WHERE = " A.SEQ = " + seq;

			if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
				WHERE += " AND A.USER_SEQ = " + this.cfg.get("user.seq");
			} else {
				if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
					WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
				}

				if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색
//20160221 INDEX 힌트 추가, UNION 통합
//					WHERE += " AND A.ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + this.cfg.getLong("user.organ") + ")";
					WHERE += " AND A.ORGANIZATION_SEQ IN ("
								+ " SELECT /*+ INDEX(A_A) */"
										+ " SEQ"
									+ " FROM TB_ORGANIZATION A_A"
									+ " WHERE SORT = 1"
									+ " START WITH SEQ = " + this.cfg.getLong("user.organ")
									+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
							+ " )"
						;
				}
			}

			try {
//20160221 INDEX 힌트 변경/추가, 서브쿼리를 JOIN으로 변경
//				ps = dbLib.prepareStatement(conn, "SELECT A.*,( SELECT ORGANIZATION_SEQ FROM TB_USER WHERE SEQ=A.USER_SEQ ) AS USER_ORG_SEQ ,(SELECT /*+ INDEX_DESC(SA PK_VM_HISTORY) */ START_DATE FROM TB_VENDING_MACHINE_HISTORY SA WHERE SA.VM_SEQ = A.SEQ AND COL_NO = 0 AND ROWNUM = 1) AS LATEST_HISTORY_DATE, (SELECT /*+ INDEX_DESC(SA PK_VM_PLACE) */ START_DATE FROM TB_VENDING_MACHINE_PLACE SA WHERE SA.VM_SEQ = A.SEQ AND ROWNUM = 1) AS LATEST_PLACE_DATE FROM TB_VENDING_MACHINE A WHERE " + WHERE);
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ ORDERED INDEX(A) USE_NL(B) */"
								+ " A.*,"
								+ " B.ORGANIZATION_SEQ AS USER_ORG_SEQ,"
								+ " ("
										+ " SELECT /*+ INDEX_DESC(C PK_VM_HISTORY) */"
												+ " START_DATE"
											+ " FROM TB_VENDING_MACHINE_HISTORY C"
											+ " WHERE VM_SEQ = A.SEQ"
												+ " AND COL_NO = 0"
												+ " AND ROWNUM = 1"
									+ " ) AS LATEST_HISTORY_DATE,"
								+ " ("
										+ " SELECT /*+ INDEX(D PK_VM_PLACE) */"
												+ " MAX(START_DATE)"
											+ " FROM TB_VENDING_MACHINE_PLACE D"
											+ " WHERE VM_SEQ = A.SEQ"
												+ " AND ROWNUM = 1"
									+ " ) AS LATEST_PLACE_DATE"
								+ ", ACCESS_STATUS"		// 출입 단말 상태 컬럼 추가. 2020.12.22 by Chae
							+ " FROM TB_VENDING_MACHINE A"
								+ " LEFT JOIN TB_USER B"
									+ " ON A.USER_SEQ = B.SEQ"
							+ " WHERE" + WHERE
					);
				rs = ps.executeQuery();

				if (rs.next()) {
					this.data.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
					this.data.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
					this.data.put("USER_SEQ", rs.getLong("USER_SEQ"));
					this.data.put("CODE", rs.getString("CODE"));
					this.data.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
					this.data.put("MODEL", rs.getString("MODEL"));
					this.data.put("PLACE", rs.getString("PLACE"));
					this.data.put("MODEM", rs.getString("MODEM"));
//					this.data.put("MODEM", rs.getString("MODEM_NUMBER"));
					this.data.put("REFLECT_FLAG", rs.getString("REFLECT_FLAG"));
					this.data.put("ASP_CHARGE", rs.getString("ASP_CHARGE"));

					this.data.put("USER_ORG_SEQ", rs.getLong("USER_ORG_SEQ"));

					this.data.put("LATEST_HISTORY_DATE", rs.getString("LATEST_HISTORY_DATE"));
					this.data.put("LATEST_PLACE_DATE", rs.getString("LATEST_PLACE_DATE"));
					// 출입 단말 상태 컬럼 추가. 2020.12.22 by Chae
					this.data.put("ACCESS_STATUS",rs.getString("ACCESS_STATUS"));
				} else {
					error = "등록되지 않았거나 수정할 권한이 없는 자판기입니다.";
				}
			} catch (Exception e) {
				this.logger.error(e);
				e.printStackTrace();
				error = e.getMessage();
			} finally {
				dbLib.close(rs);
				dbLib.close(ps);
			}
		}
		
	// 자판기 상품 등록 정보
		this.product = new ArrayList<GeneralConfig>();

		if (StringEx.isEmpty(error) && seq > 0) {
			try {
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ ORDERED INDEX(A) USE_NL(B) */"
								+ " A.COL_NO,"
								+ " A.PRODUCT_SEQ,"
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
					GeneralConfig c = new GeneralConfig();

					c.put("COL_NO", rs.getInt("COL_NO"));
					c.put("PRODUCT_SEQ", rs.getLong("PRODUCT_SEQ"));
					c.put("NAME", rs.getString("NAME"));

					this.product.add(c);
				}
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(rs);
				dbLib.close(ps);
			}
		}

	// 자판기 운영자일 경우
		if (StringEx.isEmpty(error) && this.cfg.get("user.operator").equals("Y")) {
			try {
//20160221 INDEX 힌트 추가, 불필요한 JOIN 제거, REVERSE 적용
//				ps = dbLib.prepareStatement(conn, "SELECT B.NAME AS COMPANY, (SELECT LTRIM(SYS_CONNECT_BY_PATH(NAME, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS ORGAN FROM TB_USER A LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ LEFT JOIN TB_AUTH C ON A.AUTH_SEQ = C.SEQ WHERE A.SEQ = ?");
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ ORDERED INDEX(A) USE_NL(B) */"
								+ " B.NAME AS COMPANY,"
								+ " ("
										+ " SELECT /*+ INDEX(C) */"
												+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
											+ " FROM TB_ORGANIZATION C"
											+ " WHERE PARENT_SEQ = 0"
											+ " START WITH SEQ = A.ORGANIZATION_SEQ"
											+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
									+ " ) AS ORGAN"
							+ " FROM TB_USER A"
								+ " LEFT JOIN TB_COMPANY B"
									+ " ON A.COMPANY_SEQ = B.SEQ"
							+ " WHERE A.SEQ = ?"
					);
				ps.setLong(1, cfg.getLong("user.seq"));
				rs = ps.executeQuery();

				if (rs.next()) {
					this.data.put("COMPANY", rs.getString("COMPANY"));
					this.data.put("ORGAN", rs.getString("ORGAN"));
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

		if (StringEx.isEmpty(error) && seq > 0) {
			try {

//TODO: 미등록 상품 생성 시 자동으로 TB_VENDING_MACHINE_GOODS에 등록하도록 수정 후, 미등록상품(< 1000)의 컬럼을 검색하도록 수정 필요
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " COL_NO"
							+ " FROM TB_SALES A"
							+ " WHERE TERMINAL_ID = ?"
								+ " AND COL_NO IS NOT NULL"
							+ " GROUP BY COL_NO"
						+ " MINUS"
						+ " SELECT /*+ ORDERED INDEX(B) USE_NL(C) */"
								+ " COL_NO"
							+ " FROM TB_VENDING_MACHINE B"
								+ " INNER JOIN TB_VENDING_MACHINE_PRODUCT C"
									+ " ON B.SEQ = C.VM_SEQ"
							+ " WHERE B.TERMINAL_ID = ?"
						+ " ORDER BY COL_NO"
					);
				ps.setString(1, this.data.get("TERMINAL_ID"));
				ps.setString(2, this.data.get("TERMINAL_ID"));
				rs = ps.executeQuery();

				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();

					c.put("COL_NO", rs.getInt("COL_NO"));

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

		// 공급자

		this.vendor = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */ "
							+ " SEQ,"
							+ " NAME"
						+ " FROM TB_VENDOR A"
						+ " ORDER BY SEQ"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("NAME", rs.getString("NAME"));

				this.vendor.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}
	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
/**
 * 자판기 등록
 *
 * @param seq 등록번호
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(long seq) throws Exception {
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
			
	// 자판기 정보
		this.data = new GeneralConfig();

		if (seq > 0) {
		// 자판기 등록 정보
			String WHERE = " A.SEQ = " + seq;

			//if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
			//	WHERE += " AND A.USER_SEQ = " + this.cfg.get("user.seq");
			//} else {
			//	if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			//		WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			//	}

			//	if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색
//20160221 INDEX 힌트 추가, UNION 통합
//						WHERE += " AND A.ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + this.cfg.getLong("user.organ") + ")";
			//		WHERE += " AND A.ORGANIZATION_SEQ IN ("
			//					+ " SELECT /*+ INDEX(A_A) */"
			//							+ " SEQ"
			//						+ " FROM TB_ORGANIZATION A_A"
			//						+ " WHERE SORT = 1"
			//						+ " START WITH SEQ = " + this.cfg.getLong("user.organ")
			//						+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
			//				+ " )"
			//			;
			//	}
			//}

			try {
//20160221 INDEX 힌트 변경/추가, 서브쿼리를 JOIN으로 변경
//					ps = dbLib.prepareStatement(conn, "SELECT A.*,( SELECT ORGANIZATION_SEQ FROM TB_USER WHERE SEQ=A.USER_SEQ ) AS USER_ORG_SEQ ,(SELECT /*+ INDEX_DESC(SA PK_VM_HISTORY) */ START_DATE FROM TB_VENDING_MACHINE_HISTORY SA WHERE SA.VM_SEQ = A.SEQ AND COL_NO = 0 AND ROWNUM = 1) AS LATEST_HISTORY_DATE, (SELECT /*+ INDEX_DESC(SA PK_VM_PLACE) */ START_DATE FROM TB_VENDING_MACHINE_PLACE SA WHERE SA.VM_SEQ = A.SEQ AND ROWNUM = 1) AS LATEST_PLACE_DATE FROM TB_VENDING_MACHINE A WHERE " + WHERE);
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ ORDERED INDEX(A) USE_NL(B) */"
								+ " A.*,"
								//+ " B.ORGANIZATION_SEQ AS USER_ORG_SEQ,"
								+ " ("
										+ " SELECT /*+ INDEX_DESC(C PK_VM_HISTORY) */"
												+ " START_DATE"
											+ " FROM TB_VENDING_MACHINE_HISTORY C"
											+ " WHERE VM_SEQ = A.SEQ"
												+ " AND COL_NO = 0"
												+ " AND ROWNUM = 1"
									+ " ) AS LATEST_HISTORY_DATE,"
								+ " ("
										+ " SELECT /*+ INDEX(D PK_VM_PLACE) */"
												+ " MAX(START_DATE)"
											+ " FROM TB_VENDING_MACHINE_PLACE D"
											+ " WHERE VM_SEQ = A.SEQ"
												+ " AND ROWNUM = 1"
									+ " ) AS LATEST_PLACE_DATE"
							+ " FROM TB_VENDING_MACHINE A"
								//+ " LEFT JOIN TB_USER B"
								//	+ " ON A.USER_SEQ = B.SEQ"
							+ " WHERE" + WHERE
					);
				rs = ps.executeQuery();

				if (rs.next()) {
					this.data.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
					this.data.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
					this.data.put("USER_SEQ", rs.getLong("USER_SEQ"));
					this.data.put("CODE", rs.getString("CODE"));
					this.data.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
					this.data.put("MODEL", rs.getString("MODEL"));
					this.data.put("PLACE", rs.getString("PLACE"));
					this.data.put("PLACE_CODE", rs.getString("PLACE_CODE"));
					this.data.put("PLACE_NO", rs.getString("PLACE_NO"));
					this.data.put("MODEM", rs.getString("MODEM"));
//						this.data.put("MODEM", rs.getString("MODEM_NUMBER"));
					this.data.put("REFLECT_FLAG", rs.getString("REFLECT_FLAG"));
					this.data.put("ASP_CHARGE", rs.getString("ASP_CHARGE"));

					//this.data.put("USER_ORG_SEQ", rs.getLong("USER_ORG_SEQ"));
					//this.data.put("USER_ORG_SEQ", this.cfg.getLong("user.organ"));

					this.data.put("LATEST_HISTORY_DATE", rs.getString("LATEST_HISTORY_DATE"));
					this.data.put("LATEST_PLACE_DATE", rs.getString("LATEST_PLACE_DATE"));
					// 출입단말 상태 조회 컬럼 추가.(2020.12.23 by Chae)
					this.data.put("ACCESS_STATUS", rs.getString("ACCESS_STATUS"));
				} else {
					error = "등록되지 않았거나 수정할 권한이 없는 자판기입니다.";
				}
			} catch (Exception e) {
				this.logger.error(e);
				e.printStackTrace();
				error = e.getMessage();
			} finally {
				dbLib.close(rs);
				dbLib.close(ps);
			}
		}
		
	// 자판기 상품 등록 정보
		this.product = new ArrayList<GeneralConfig>();

		if (StringEx.isEmpty(error) && seq > 0) {
			try {
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ ORDERED INDEX(A) USE_NL(B) */"
								+ " A.COL_NO,"
								+ " A.PRODUCT_SEQ,"
								+ " B.NAME,"
								+ " B.PRICE"
							+ " FROM TB_VENDING_MACHINE_PRODUCT A"
								+ " INNER JOIN TB_PRODUCT B"
									+ " ON A.PRODUCT_SEQ = B.SEQ"
							+ " WHERE A.VM_SEQ = ?"
							+ " ORDER BY A.COL_NO"
					);
				ps.setLong(1, seq);
				rs = ps.executeQuery();

				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();

					c.put("COL_NO", rs.getInt("COL_NO"));
					c.put("PRODUCT_SEQ", rs.getLong("PRODUCT_SEQ"));
					c.put("NAME", rs.getString("NAME"));
					c.put("PRICE", rs.getString("PRICE"));

					this.product.add(c);
				}
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(rs);
				dbLib.close(ps);
			}
		}

	// 자판기 운영자일 경우
		if (StringEx.isEmpty(error) && this.cfg.get("user.operator").equals("Y")) {
			try {
//20160221 INDEX 힌트 추가, 불필요한 JOIN 제거, REVERSE 적용
//					ps = dbLib.prepareStatement(conn, "SELECT B.NAME AS COMPANY, (SELECT LTRIM(SYS_CONNECT_BY_PATH(NAME, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS ORGAN FROM TB_USER A LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ LEFT JOIN TB_AUTH C ON A.AUTH_SEQ = C.SEQ WHERE A.SEQ = ?");
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ ORDERED INDEX(A) USE_NL(B) */"
								+ " B.NAME AS COMPANY,"
								+ " ("
										+ " SELECT /*+ INDEX(C) */"
												+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
											+ " FROM TB_ORGANIZATION C"
											+ " WHERE PARENT_SEQ = 0"
											+ " START WITH SEQ = A.ORGANIZATION_SEQ"
											+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
									+ " ) AS ORGAN"
							+ " FROM TB_USER A"
								+ " LEFT JOIN TB_COMPANY B"
									+ " ON A.COMPANY_SEQ = B.SEQ"
							+ " WHERE A.SEQ = ?"
					);
				ps.setLong(1, cfg.getLong("user.seq"));
				rs = ps.executeQuery();

				if (rs.next()) {
					this.data.put("COMPANY", rs.getString("COMPANY"));
					this.data.put("ORGAN", rs.getString("ORGAN"));
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

		if (StringEx.isEmpty(error) && seq > 0) {
			try {

//TODO: 미등록 상품 생성 시 자동으로 TB_VENDING_MACHINE_GOODS에 등록하도록 수정 후, 미등록상품(< 1000)의 컬럼을 검색하도록 수정 필요
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " COL_NO"
							+ " FROM TB_SALES A"
							+ " WHERE TERMINAL_ID = ?"
								+ " AND COL_NO IS NOT NULL"
								+ " AND TRANSACTION_DATE >= TO_CHAR(SYSDATE - 7, 'YYYYMMDD')"
							+ " GROUP BY COL_NO"
						+ " MINUS"
						+ " SELECT /*+ ORDERED INDEX(B) USE_NL(C) */"
								+ " COL_NO"
							+ " FROM TB_VENDING_MACHINE B"
								+ " INNER JOIN TB_VENDING_MACHINE_PRODUCT C"
									+ " ON B.SEQ = C.VM_SEQ"
							+ " WHERE B.TERMINAL_ID = ?"
						+ " ORDER BY COL_NO"
					);
				ps.setString(1, this.data.get("TERMINAL_ID"));
				ps.setString(2, this.data.get("TERMINAL_ID"));
				rs = ps.executeQuery();

				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();

					c.put("COL_NO", rs.getInt("COL_NO"));

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

		// 공급자

		this.vendor = new ArrayList<GeneralConfig>();
		if (seq > 0) {

			try {
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(P)*/"
								+ " P.VENDOR_SEQ SEQ, V.NAME NAME"
							+ " FROM TB_PRODUCT P"
							 	+ " INNER JOIN TB_VENDOR V"
							 		+ " ON P.VENDOR_SEQ = V.SEQ"
							 	+ " INNER JOIN TB_VENDING_MACHINE M"
							 		+ " ON M.SEQ = " + seq
							 			+ " AND M.COMPANY_SEQ = P.COMPANY_SEQ"
							 + " GROUP BY P.VENDOR_SEQ, V.NAME"	
							 + " ORDER BY P.VENDOR_SEQ, V.NAME"
						//"SELECT /*+ INDEX(A) */ "
						//		+ " SEQ,"
						//		+ " NAME"
						//	+ " FROM TB_VENDOR A"
						//	+ " ORDER BY SEQ"
					);
				rs = ps.executeQuery();
	
				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();
	
					c.put("SEQ", rs.getLong("SEQ"));
					c.put("NAME", rs.getString("NAME"));
	
					this.vendor.add(c);
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
		

		return error;
	}	
/**
 * 자판기 등록
 *
 * @param request javax.servlet.http.HttpServletRequest
 * @param seq 등록번호
 * @param company 소속
 * @param organ 조직
 * @param user 담당계정
 * @param code 코드
 * @param terminal 단말기 ID
 * @param model 모델
 * @param place 설치위치
 * @param place_code 설치위치코드
 * @param place_no 설치위치번호
 * @param modem 모뎀
 * @param sgcnt 자판기 상품 갯수
 * @param reflectFlag 상품정보 반영플래그
 * @param aspCharge ASP 과금여부
 * @return 에러가 있을 경우 에러 내용
 *
 */
	//public String regist(HttpServletRequest request, long seq, long company, long organ, long user, String code, String terminal, String model, String place, String modem, int sgcnt) throws Exception {
	//public String regist(HttpServletRequest request, long seq, long company, long organ, long user, String code, String terminal, String model, String place, String modem, int sgcnt, String reflectFlag) throws Exception {
	//public String regist(HttpServletRequest request, long seq, long company, long organ, long user, String code, String terminal, String model, String place, String modem, int sgcnt, String reflectFlag, String aspCharge) throws Exception {
	//public String regist(HttpServletRequest request, long seq, long company, long organ, long user, String code, String terminal, String model, String place, String modem, int sgcnt, String reflectFlag, String aspCharge, String placeMove) throws Exception {
    //public String regist(HttpServletRequest request, long seq, long company, long organ, long user, String code, String terminal, String model, String place, String place_code, String place_no, String modem, int sgcnt, String reflectFlag, String aspCharge, String placeMove) throws Exception {
    //public String regist(HttpServletRequest request, long seq, long company, long organ, long user, String code, String terminal, String model, String place, String place_code, String place_no, String modem, int sgcnt, String reflectFlag, String aspCharge, String placeMove, int type) throws Exception {
	public String regist(HttpServletRequest request, long seq, long company, long organ, long user, String code, String terminal, String model, String place, String place_code, String place_no, String modem, int sgcnt, String reflectFlag, String aspCharge, String placeMove, int type, String accessStatus) throws Exception {

		// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		CallableStatement cs = null;
		String error = null;
		long sequence = 0;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}

	// 전체 Row 코드값 중복 방지
	// 소속과 무관하게 코드값을 고유값으로 지정하여 중복 등록 방지를 위한 Row
	// 2020.10.12 by j.w.chae
	// code: 자판기코드 / seq: 자판기SEQ / type: 0(운영자판기 등록), 1(운영자판기 수정)
	if(!this.validCheck(code, seq, type)) {
		error="동일한 자판기 코드값이 존재합니다.";
		dbLib.close(cs);
		dbLib.close(conn);
		return error;
	}
	// 전체 Row 코드값 중복 방지 End	
	// COMMIT 설정
		conn.setAutoCommit(false);

	// 자판기 등록
		try {
			//cs = dbLib.prepareCall(conn, "{CALL SP_VENDING_MACHINE (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
			//cs = dbLib.prepareCall(conn, "{CALL SP_VENDING_MACHINE2 (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
			//20120509 상품정보 반영플래그 추가
			//cs = dbLib.prepareCall(conn, "{ CALL SP_VENDING_MACHINE2 (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ,?) }");
			//cs = dbLib.prepareCall(conn, "{ CALL SP_VENDING_MACHINE3 (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ,?, ?) }");
			//cs = dbLib.prepareCall(conn, "{ CALL SP_VENDING_MACHINE4 (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ,?, ?, ?) }");
			//cs = dbLib.prepareCall(conn, "{ CALL SP_VENDING_MACHINE5 (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ,?, ?, ?) }");
			// 신규 프로시저 생성 (2020.12.23 By Chae)
			cs = dbLib.prepareCall(conn, "{ CALL SP_VENDING_MACHINE6 (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ,?, ?, ?, ?) }");
			cs.setString(1, this.cfg.get("server"));
			cs.setLong(2, seq);
			cs.setLong(3, company);
			cs.setLong(4, organ);
			cs.setLong(5, user);
			cs.setString(6, code);
			cs.setString(7, terminal);
			cs.setString(8, model);
			cs.setString(9, place);
			cs.setString(10, place_code);
			cs.setString(11, place_no);
			cs.setString(12, modem);
			cs.setLong(13, this.cfg.getLong("user.seq"));
			//20120509 상품정보 반영플래그 추가
			cs.setString(14, reflectFlag);
			cs.setString(15, aspCharge);
			cs.setString(16, placeMove);

			cs.registerOutParameter(17, OracleTypes.NUMBER);
			cs.registerOutParameter(18, OracleTypes.VARCHAR);
			cs.setString(19, accessStatus);
			cs.execute();

			sequence = cs.getLong(17);
			error = cs.getString(18);

		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(cs);
		}

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn, dbLib.ROLLBACK);
			return error;
		}

	// 자판기 상품 삭제
		for (int i = 1; i <= sgcnt; i++) {
			int column = StringEx.str2int(request.getParameter("col" + i));
			String isDel = StringEx.setDefaultValue(request.getParameter("isDel" + i), "N");

			if (column == 0 || isDel.equals("N")) {
				continue;
			}

			try {
				cs = dbLib.prepareCall(conn, "{ CALL SP_VENDING_MACHINE_PRODUCT (?, ?, ?, ?, ?, ?) }");
				cs.setString(1, this.cfg.get("server"));
				cs.setLong(2, sequence);
				cs.setInt(3, column);
				cs.setLong(4, 0);
				cs.setLong(5, this.cfg.getLong("user.seq"));
				cs.registerOutParameter(6, OracleTypes.VARCHAR);
				cs.execute();

				error = cs.getString(6);
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(cs);
			}

			if (!StringEx.isEmpty(error)) {
				break;
			}
		}

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn, dbLib.ROLLBACK);
			return error;
		}

	// 자판기 상품 등록
		for (int i = 1; i <= sgcnt; i++) {
			int column = StringEx.str2int(request.getParameter("col" + i));
			long product = StringEx.str2long(request.getParameter("seq" + i));
			String isDel = StringEx.setDefaultValue(request.getParameter("isDel" + i), "N");

			if (column == 0 || product == 0 || isDel.equals("Y")) {
				continue;
			}

			try {
				cs = dbLib.prepareCall(conn, "{ CALL SP_VENDING_MACHINE_PRODUCT (?, ?, ?, ?, ?, ?) }");
				cs.setString(1, this.cfg.get("server"));
				cs.setLong(2, sequence);
				cs.setInt(3, column);
				cs.setLong(4, product);
				cs.setLong(5, this.cfg.getLong("user.seq"));
				cs.registerOutParameter(6, OracleTypes.VARCHAR);
				cs.execute();

				error = cs.getString(6);
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(cs);
			}

			if (!StringEx.isEmpty(error)) {
				break;
			}
		}

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn, dbLib.ROLLBACK);
			return error;
		}

	// 미등록 상품 중 매출 기록이 없는 상품 삭제
	//	try {
	//		cs = dbLib.prepareCall(conn, "{ CALL SP_VENDING_MACHINE_GOODS_CLEAR (?,?) }");
	//		cs.setString(1, terminal);
	//		cs.registerOutParameter(2, OracleTypes.VARCHAR);
	//		cs.execute();

	//		error = cs.getString(2);
	//	} catch (Exception e) {
	//		this.logger.error(e);
	//		error = e.getMessage();
	//	} finally {
	//		dbLib.close(cs);
	//	}

	// 에러 처리
	//	if (!StringEx.isEmpty(error)) {
	//		dbLib.close(conn, dbLib.ROLLBACK);
	//		return error;
	//	}

	// 리소스 반환
		dbLib.close(conn, dbLib.COMMIT);

		return StringEx.long2str(sequence);
	}
/**
 * 자판기 일괄 등록
 *
 * @param company 소속
 * @param excel 엑셀
 * @return 에러가 있을 경우 에러 내용
 *
 */
	/*
	public String regist(long company, File excel) throws Exception {
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		CallableStatement cs = null;
		Workbook workbook = null;
		Sheet sheet = null;
		String error = null;
		String vcode = "";

	// 인수 체크
		if (company == 0) {
			error = "소속이 존재하지 않습니다.";
		} else if (excel == null || excel.length() <= 0) {
			error = "등록하실 엑셀을 업로드하세요.";
		} else if (!StringEx.inArray(FileEx.extension(excel.getName()), "xls".split(";"))) {
			error = "XLS 파일만 등록이 가능합니다.";
		}

		if (!StringEx.isEmpty(error)) {
			if (excel.exists()) {
				excel.delete();
			}

			return error;
		}

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			if (excel.exists()) {
				excel.delete();
			}

			return "DB 연결에 실패하였습니다.";
		}

	// COMMIT 설정
		conn.setAutoCommit(false);

	// 엑셀 읽기
		try {
			workbook = Workbook.getWorkbook(excel);

			if (workbook != null) {
				sheet = workbook.getSheet(0);

				if (sheet != null) { // 자판기 등록
					if (this.cfg.getInt("excel.limit.vmRun") > 0 && sheet.getRows() <= this.cfg.getInt("excel.limit.vmRun")) {
						//cs = dbLib.prepareCall(conn, "{CALL SP_VENDING_MACHINE_BATCH (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
						cs = dbLib.prepareCall(conn, "{CALL SP_VENDING_MACHINE_BATCH2 (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");

						for (int i = 1; i < sheet.getRows(); i++) {
							Cell user = sheet.getCell(0, i);
							Cell code = sheet.getCell(1, i);
							Cell terminal = sheet.getCell(2, i);
							Cell model = sheet.getCell(3, i);
							Cell modem = sheet.getCell(4, i);
							Cell place = sheet.getCell(5, i);

							//if (user == null || code == null || terminal == null || place == null) {
							if (user == null || code == null || terminal == null ) {
								//continue;
								if(user == null) throw new Exception("담당자 계정이 없습니다.");
								if(code == null) throw new Exception("자판기 코드가 없습니다.");
								if(terminal == null) throw new Exception("단말기 ID가 없습니다.");
							}
							//else if (StringEx.isEmpty(user.getContents()) || StringEx.isEmpty(code.getContents()) || StringEx.isEmpty(terminal.getContents()) || StringEx.isEmpty(place.getContents())) {
							else if (StringEx.isEmpty(user.getContents()) || StringEx.isEmpty(code.getContents()) || StringEx.isEmpty(terminal.getContents())) {
								//continue;
								if(StringEx.isEmpty(user.getContents())) throw new Exception("담당자 계정이 없습니다.");
								if(StringEx.isEmpty(code.getContents())) throw new Exception("자판기 코드가 없습니다.");
								if(StringEx.isEmpty(terminal.getContents())) throw new Exception("단말기 ID가 없습니다.");
							}

							try {
								cs.setString(1, this.cfg.get("server"));
								cs.setString(2, this.cfg.get("user.operator").equals("Y") ? this.cfg.get("user.id") : user.getContents());
								cs.setLong(3, company);
								cs.setString(4, code.getContents());
								cs.setString(5, terminal.getContents());
								cs.setString(6, model.getContents());
								cs.setString(7, place.getContents());
								cs.setString(8, modem.getContents());
								cs.setLong(9, this.cfg.getLong("user.seq"));
								cs.registerOutParameter(10, OracleTypes.VARCHAR);
								cs.executeUpdate();

								error = cs.getString(10);
							} catch (Exception e_) {
								this.logger.error(e_);
								error = e_.getMessage();
							}

							if (!StringEx.isEmpty(error)) {
								break;
							}
						}
					} else {
						error = "자판기 데이터를 " + StringEx.comma(this.cfg.getInt("excel.limit.vmRun")) + "건을 초과하여 등록할 수 없습니다. (라인수 = " + StringEx.comma(sheet.getRows() - 1) + "건)";
					}
				} else {
					error = "자판기 쉬트가 존재하지 않습니다.";
				}

				if (!StringEx.isEmpty(error)) {
					dbLib.close(conn, dbLib.ROLLBACK);

					if (excel.exists()) {
						excel.delete();
					}

					return error;
				}

				sheet = workbook.getSheet(1);

				if (sheet != null) { // 상품 등록
					if (this.cfg.getInt("excel.limit.vmRun.product") > 0 && sheet.getRows() <= this.cfg.getInt("excel.limit.vmRun.product")) {
						cs = dbLib.prepareCall(conn, "{CALL SP_VENDING_MACHINE_GOODS_BATCH (?, ?, ?, ?, ?, ?, ?)}");

						for (int i = 1; i < sheet.getRows(); i++) {
							Cell vm = sheet.getCell(0, i);
							Cell column = sheet.getCell(1, i);
							Cell product = sheet.getCell(2, i);

							if (vm != null && !StringEx.isEmpty(vm.getContents())) {
								vcode = vm.getContents();
							}

							if (column == null || product == null) {
								continue;
							} else if (StringEx.isEmpty(vcode) || StringEx.isEmpty(column.getContents()) || StringEx.isEmpty(product.getContents())) {
								continue;
							}

							try {
								cs.setString(1, this.cfg.get("server"));
								cs.setLong(2, company);
								cs.setString(3, vcode);
								cs.setInt(4, StringEx.str2int(column.getContents()));
								cs.setString(5, product.getContents());
								cs.setLong(6, this.cfg.getLong("user.seq"));
								cs.registerOutParameter(7, OracleTypes.VARCHAR);
								cs.executeUpdate();

								error = cs.getString(7);
							} catch (Exception e_) {
								this.logger.error(e_);
								error = e_.getMessage();
							}

							if (!StringEx.isEmpty(error)) {
								break;
							}
						}
					} else {
						error = "상품 데이터를 " + StringEx.comma(this.cfg.getInt("excel.limit.vmRun.product")) + "건을 초과하여 등록할 수 없습니다. (라인수 = " + StringEx.comma(sheet.getRows() - 1) + "건)";
					}
				} else {
					error = "상품 쉬트가 존재하지 않습니다.";
				}

				if (!StringEx.isEmpty(error)) {
					dbLib.close(conn, dbLib.ROLLBACK);

					if (excel.exists()) {
						excel.delete();
					}

					return error;
				}
			} else {
				error = "Workbook이 존재하지 않습니다.";
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(cs);

			try {
				if (workbook != null) {
					workbook.close();
					workbook = null;
				}
			} catch (Exception e_) {
			}
		}

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn, dbLib.ROLLBACK);

			if (excel.exists()) {
				excel.delete();
			}

			return error;
		}

	// 미등록 상품 중 매출 기록이 없는 상품 삭제
		try {
			cs = dbLib.prepareCall(conn, "{CALL SP_VENDING_MACHINE_GOODS_CLEAR (?)}");
			cs.registerOutParameter(1, OracleTypes.VARCHAR);
			cs.execute();

			error = cs.getString(1);
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(cs);
		}

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn, dbLib.ROLLBACK);
			return error;
		}

	// 리소스 반환
		dbLib.close(conn, dbLib.COMMIT);

	// 파일 삭제
		if (excel.exists()) {
			excel.delete();
		}

		return null;
	}
	*/
	public String regist(long company, File excel) throws Exception {
		// 실행에 사용될 변수
			DBLibrary dbLib = new DBLibrary(this.logger);
			Connection conn = null;
			CallableStatement cs = null;
			Workbook workbook = null;
			Sheet sheet = null;
			String error = null;
			String vcode = "";

		// 인수 체크
			if (company == 0) {
				error = "소속이 존재하지 않습니다.";
			} else if (excel == null || excel.length() <= 0) {
				error = "등록하실 엑셀을 업로드하세요.";
			} else if (!StringEx.inArray(FileEx.extension(excel.getName()), "xls".split(";"))) {
				error = "XLS 파일만 등록이 가능합니다.";
			}

			if (!StringEx.isEmpty(error)) {
				if (excel.exists()) {
					excel.delete();
				}

				return error;
			}

		// DB 연결
			conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

			if (conn == null) {
				if (excel.exists()) {
					excel.delete();
				}

				return "DB 연결에 실패하였습니다.";
			}

		// COMMIT 설정
			conn.setAutoCommit(false);

		// 엑셀 읽기
			try {
				workbook = Workbook.getWorkbook(excel);

				if (workbook != null) {
					sheet = workbook.getSheet(0);

					if (sheet != null) { // 자판기 등록
						if (this.cfg.getInt("excel.limit.vmRun") > 0 && sheet.getRows() <= this.cfg.getInt("excel.limit.vmRun")) {
							//cs = dbLib.prepareCall(conn, "{CALL SP_VENDING_MACHINE_BATCH (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
							cs = dbLib.prepareCall(conn, "{ CALL SP_VENDING_MACHINE_BATCH2 (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");

							for (int i = 1; i < sheet.getRows(); i++) {
								Cell user = sheet.getCell(0, i);
								Cell code = sheet.getCell(1, i);
								Cell terminal = sheet.getCell(2, i);
								Cell model = sheet.getCell(3, i);
								Cell modem = sheet.getCell(4, i);
								Cell place = sheet.getCell(5, i);

								//if (user == null || code == null || terminal == null || place == null) {
								if (user == null || code == null || terminal == null ) {
									//continue;
									if(user == null) throw new WarningException("[" + String.valueOf(i) + "행]담당자 계정이 없습니다.");
									if(code == null) throw new WarningException("[" + String.valueOf(i) + "행]자판기 코드가 없습니다.");
									if(terminal == null) throw new WarningException("[" + String.valueOf(i) + "행]단말기 ID가 없습니다.");
								}
								//else if (StringEx.isEmpty(user.getContents()) || StringEx.isEmpty(code.getContents()) || StringEx.isEmpty(terminal.getContents()) || StringEx.isEmpty(place.getContents())) {
								else if (StringEx.isEmpty(user.getContents()) || StringEx.isEmpty(code.getContents()) || StringEx.isEmpty(terminal.getContents())) {
									//continue;
									if(StringEx.isEmpty(user.getContents())) throw new WarningException("[" + String.valueOf(i) + "행]담당자 계정이 없습니다.");
									if(StringEx.isEmpty(code.getContents())) throw new WarningException("[" + String.valueOf(i) + "행]자판기 코드가 없습니다.");
									if(StringEx.isEmpty(terminal.getContents())) throw new WarningException("[" + String.valueOf(i) + "행]단말기 ID가 없습니다.");
								}

								cs.setString(1, this.cfg.get("server"));
								cs.setString(2, this.cfg.get("user.operator").equals("Y") ? this.cfg.get("user.id") : user.getContents());
								cs.setLong(3, company);
								cs.setString(4, code.getContents());
								cs.setString(5, terminal.getContents());
								cs.setString(6, model.getContents());
								cs.setString(7, place.getContents());
								cs.setString(8, modem.getContents());
								cs.setLong(9, this.cfg.getLong("user.seq"));
								cs.registerOutParameter(10, OracleTypes.VARCHAR);
								cs.executeUpdate();

								error = cs.getString(10);

								if (!StringEx.isEmpty(error)) {
									//break;
									throw new WarningException("[" + String.valueOf(i) + "행]"+ error);

								}
							}


						} else {
							throw new WarningException("자판기 데이터를 " + StringEx.comma(this.cfg.getInt("excel.limit.vmRun")) + "건을 초과하여 등록할 수 없습니다. (라인수 = " + StringEx.comma(sheet.getRows() - 1) + "건)" );
						}
					} else {
						throw new WarningException("자판기 쉬트가 존재하지 않습니다.");
					}

					cs.close();
					//--------------------------------

					sheet = workbook.getSheet(1);

					if (sheet != null) { // 상품 등록
						if (this.cfg.getInt("excel.limit.vmRun.product") > 0 && sheet.getRows() <= this.cfg.getInt("excel.limit.vmRun.product")) {
							cs = dbLib.prepareCall(conn, "{ CALL SP_VENDING_MACHINE_GOODS_BATCH (?, ?, ?, ?, ?, ?, ?) }");

							for (int i = 1; i < sheet.getRows(); i++) {
								Cell vm = sheet.getCell(0, i);
								Cell column = sheet.getCell(1, i);
								Cell product = sheet.getCell(2, i);

								if (vm != null && !StringEx.isEmpty(vm.getContents())) {
									vcode = vm.getContents();
								}

								if (column == null || product == null) {
									//continue;
									if(column == null) throw new WarningException("[" + String.valueOf(i) + "행]상품시트의 컬럼정보가 없습니다.");
									if(product == null) throw new WarningException("[" + String.valueOf(i) + "행]상품시트의 상품정보가 없습니다.");

								} else if (StringEx.isEmpty(vcode) || StringEx.isEmpty(column.getContents()) || StringEx.isEmpty(product.getContents())) {
									//continue;
									if(StringEx.isEmpty(vcode)) throw new WarningException("[" + String.valueOf(i) + "행]상품시트의 자판기코드가 없습니다.");
									if(StringEx.isEmpty(column.getContents())) throw new WarningException("[" + String.valueOf(i) + "행]상품시트의 컬럼정보가 없습니다.");
									if(StringEx.isEmpty(product.getContents())) throw new WarningException("[" + String.valueOf(i) + "행]상품시트의 상품정보가 없습니다.");
								}


									cs.setString(1, this.cfg.get("server"));
									cs.setLong(2, company);
									cs.setString(3, vcode);
									cs.setInt(4, StringEx.str2int(column.getContents()));
									cs.setString(5, product.getContents());
									cs.setLong(6, this.cfg.getLong("user.seq"));
									cs.registerOutParameter(7, OracleTypes.VARCHAR);
									cs.executeUpdate();

									error = cs.getString(7);

								if (!StringEx.isEmpty(error)) {
									//break;
									throw new WarningException(error);
								}
							}
						} else {
							//error = "상품 데이터를 " + StringEx.comma(this.cfg.getInt("excel.limit.vmRun.product")) + "건을 초과하여 등록할 수 없습니다. (라인수 = " + StringEx.comma(sheet.getRows() - 1) + "건)";
							throw new WarningException("상품 데이터를 " + StringEx.comma(this.cfg.getInt("excel.limit.vmRun.product")) + "건을 초과하여 등록할 수 없습니다. (라인수 = " + StringEx.comma(sheet.getRows() - 1) + "건)");
						}
					} else {
						//error = "상품 쉬트가 존재하지 않습니다.";
					}

					if (!StringEx.isEmpty(error)) {

						throw new WarningException(error);
					}

					cs.close();

					// 미등록 상품 중 매출 기록이 없는 상품 삭제
					cs = dbLib.prepareCall(conn, "{ CALL SP_VENDING_MACHINE_GOODS_CLEAR (?) }");
					cs.registerOutParameter(1, OracleTypes.VARCHAR);
					cs.execute();

					error = cs.getString(1);

				// 에러 처리
					if (!StringEx.isEmpty(error)) {
						throw new WarningException(error);
					}

					cs.close();

				// 리소스 반환
					dbLib.close(conn, dbLib.COMMIT);

				// 파일 삭제
					if (excel.exists()) {
						excel.delete();
					}
					//----------------------------------

				} else {
					throw new WarningException("Workbook이 존재하지 않습니다.");
				}

				if (excel.exists()) {
					excel.delete();
				}

				// 리소스 반환
				dbLib.close(conn, dbLib.COMMIT);

				return null;
			} catch(WarningException Ex) { //waring처리
				this.logger.warn(Ex);
				dbLib.close(conn, dbLib.ROLLBACK);

				if (excel.exists()) {
					excel.delete();
				}
				return Ex.getMessage();
			} catch(SQLException Ex) { //error 처리
				//this.logger.error(Ex);
				this.logger.warn(Ex);
				dbLib.close(conn, dbLib.ROLLBACK);

				if (excel.exists()) {
					excel.delete();
				}
				return Ex.getMessage();
			} catch(Exception Ex) {
				this.logger.error(Ex);
				dbLib.close(conn, dbLib.ROLLBACK);

				if (excel.exists()) {
					excel.delete();
				}
				return Ex.getMessage();
			} finally {
				dbLib.close(conn);
			}
		}

  public String CheckVmUpdate(String strVmSeq) {

		// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		CallableStatement cs = null;

		String error = null;


	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));


		String  strReturn = null;

		try
		{

				if (conn == null) {
					throw new Exception("DB가 연결되지 않았습니다.");
				}

			// COMMIT 설정
				conn.setAutoCommit(false);

				cs = dbLib.prepareCall(conn, "{ CALL SP_VENDING_MACHINE_CHECK (?,?,?) }");

				cs.setString(1, this.cfg.get("server"));
				cs.setString(2, this.cfg.get("server"));
				cs.registerOutParameter(3, OracleTypes.VARCHAR);
				cs.execute();

				error = cs.getString(3);
				if (StringEx.isEmpty(error)) {
					strReturn = "0";
				} else if (!StringEx.isEmpty(error) && error.equals("1") ) {
					strReturn = "1";
				} else if (!StringEx.isEmpty(error) && !error.equals("1") ) {
					throw new WarningException(error);
				}

				//--------------------------------

			// 리소스 반환
			dbLib.close(conn);

			return strReturn;
		} catch(WarningException Ex) { //waring처리
			this.logger.warn(Ex);
			strReturn = Ex.getMessage();
			return strReturn;
		} catch(SQLException Ex) { //error 처리
			//this.logger.error(Ex);
			this.logger.warn(Ex);
			strReturn = Ex.getMessage();
			return strReturn;
		} catch(Exception Ex) {
			this.logger.error(Ex);
			// 리소스 반환
			strReturn = Ex.getMessage();
			return strReturn;
		} finally {
			dbLib.close(conn);
		}
  }

/**
 * 상품
 *
 * @param company 소속
 * @param vendor 공급자
 * @param category 그룹
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String product(long company, long vendor, long category, String sField, String sQuery) throws Exception {
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

	// 검색절 생성
		String WHERE = "";

		if (company > 0) { 
			WHERE += " AND A.COMPANY_SEQ = " + company;
			;
		}
		
		if (vendor > 0) {
			WHERE += " AND A.VENDOR_SEQ = " + vendor;
		}
		
		if (category > 0) {
			WHERE += " AND A.CATEGORY_SEQ IN ("
						+ " SELECT /*+ INDEX(A_D) */"
								+ " SEQ"
							+ " FROM TB_CATEGORY A_D"
							+ " START WITH SEQ = " + category
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;
		}
		// 서울 F/S 기존 상품에서 조회 안되게 처리
		if (company == 74) {
			WHERE += "  AND A.CATEGORY_SEQ <> ("
					+ " SELECT /*+ INDEX(A_D) */"
							+ " SEQ"
						+ " FROM TB_CATEGORY A_D"
						+ " WHERE COMPANY_SEQ = " + company
							+ " AND PARENT_SEQ = 0 "
							+ " AND DEPTH = 0 "
							+ " AND NAME = '기존'"
				+ " )"
				;
		}

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

	// 목록
		this.product = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " SEQ,"
							+ " CODE,"
							+ " NAME,"
							+ " PRICE"
						+ " FROM TB_PRODUCT A"
						+ WHERE
						+ " ORDER BY " + (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery) ? sField : "NAME, CODE")
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("CODE", rs.getString("CODE"));
				c.put("NAME", rs.getString("NAME"));
				c.put("PRICE", rs.getLong("PRICE"));

				this.product.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}	
/**
 * 운영 정보 로그
 *
 * @param pageNo 페이지
 * @param sDate 검색 시작일
 * @param eDate 검색 종료일
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String log(int pageNo, String sDate, String eDate, String sQuery) throws Exception {
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

	// 검색절 생성
		String HINT = "";
		String JOIN = "";
		String WHERE = "";

		if (!StringEx.isEmpty(sQuery)) { // 단말기 ID
			WHERE += " AND A.TERMINAL_ID = '" + sQuery + "'";
		}

		if (!StringEx.isEmpty(sDate)) { // 시작일
			WHERE += " AND A.RESPONSE_DATE >= '" + sDate + "'";
		}

		if (!StringEx.isEmpty(eDate)) { // 종료일
			WHERE += " AND A.RESPONSE_DATE <= '" + eDate + "'";
		}

		if (WHERE.length() > 0) HINT = " INDEX(A)";

		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
			HINT = " ORDERED" + HINT + " INDEX(B) USE_HASH(B)";
			JOIN = " INNER JOIN TB_VENDING_MACHINE B"
					+ " ON A.TERMINAL_ID = B.TERMINAL_ID"
						+ " AND B.USER_SEQ = " + this.cfg.get("user.seq");
		} else if ((this.cfg.getLong("user.company") > 0) || (this.cfg.getLong("user.organ") > 0)) {
			HINT += " ORDERED" + HINT + " INDEX(B) USE_HASH(B)";
			JOIN = " INNER JOIN TB_VENDING_MACHINE B"
					+ " ON A.TERMINAL_ID = B.TERMINAL_ID";

			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
				JOIN += " AND B.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			}

			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색
//20160221 INDEX 힌트 추가
//				WHERE += " AND B.ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ)";
				JOIN += " AND B.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(A_A) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION A_A"
								+ " WHERE SORT = 1"
								+ " START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ")
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
					;
			}
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

	// 총 레코드수
//20160221 INDEX 힌트 추가, USE_NL을 USE_HASH로 변경
//		this.records = StringEx.str2long(dbLib.getResult(conn, "SELECT /*+ ORDERED USE_NL(B C) */ COUNT(*) FROM TB_TXT_STATUS A LEFT JOIN TB_VENDING_MACHINE B ON A.TERMINAL_ID = B.TERMINAL_ID LEFT JOIN TB_USER C ON B.USER_SEQ = C.SEQ WHERE " + WHERE));
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT" + (HINT.length() > 0 ? " /*+" + HINT + " */" : "")
								+ " COUNT(*)"
							+ " FROM TB_TXT_STATUS A"
								+ JOIN
							+ WHERE
					)
			);

	// 총 페이지수
		this.pages = Pager.getSize(this.records, cfg.getInt("limit.list"));

	// 리스트 가상번호
		long no = this.records - (pageNo - 1) * cfg.getInt("limit.list");

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();

		try {
			int s = ((pageNo - 1) * cfg.getInt("limit.list")) + 1;
			int e = (s - 1) + cfg.getInt("limit.list");
//20160221 INDEX 힌트 변경, 불필요한 JOIN 제거, ORDER BY 추가
//			ps = dbLib.prepareStatement(conn, "SELECT S2.*"
//				+ " FROM"
//				+ " ("
//				+ " SELECT ROWNUM AS RNUM, S1.*"
//				+ " FROM"
//				+ " ("
//				+ " SELECT /*+ ORDERED USE_NL(B C) INDEX_DESC(A UK_TXT_STATUS) */ A.TRANSACTION_NO, A.TERMINAL_ID, A.PLACE, TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD HH24:MI:SS') AS CREATE_DATE, FN_SOLD_OUT(A.SOLD_OUT) AS SOLD_OUT, FN_CONTROL_ERROR(A.CONTROL_ERROR) AS CONTROL_ERROR, FN_PD_ERROR(A.PD_ERROR) AS PD_ERROR, FN_EMPTY_COL(A.EMPTY_COL) AS EMPTY_COL, CASE A.RES_CODE WHEN '0000' THEN '정상' WHEN '0001' THEN '재전송' WHEN '0002' THEN '타임아웃' ELSE '에러' END AS RES_CODE"
//				+ " FROM TB_TXT_STATUS A"
//				+ " LEFT JOIN TB_VENDING_MACHINE B ON A.TERMINAL_ID = B.TERMINAL_ID"
//				+ " LEFT JOIN TB_USER C ON B.USER_SEQ = C.SEQ"
//				+ " WHERE " + WHERE
//				+ " ) S1"
//				+ " WHERE ROWNUM <= " + e
//				+ " ) S2"
//				+ " WHERE RNUM >= " + s);
			ps = dbLib.prepareStatement(conn,
					"SELECT"
							+ " TRANSACTION_NO,"
							+ " TERMINAL_ID,"
							+ " PLACE,"
							+ " NVL(TO_CHAR(MODIFY_DATE, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(CREATE_DATE, 'YYYY-MM-DD HH24:MI:SS')) AS CREATE_DATE,"
							+ " FN_SOLD_OUT(SOLD_OUT) AS SOLD_OUT,"
							//+ " FN_CONTROL_ERROR(CONTROL_ERROR) AS CONTROL_ERROR,"
							+ " CASE WHEN CONTROL_ERROR IS NOT NULL AND SUBSTR(PD_ERROR, 21, 10) <> 'LVM-6141KR' AND SUBSTR(PD_ERROR, 21, 6) <> 'DONGGU' THEN FN_CONTROL_ERROR(CONTROL_ERROR) "
								+ " WHEN CONTROL_ERROR IS NOT NULL AND SUBSTR(PD_ERROR, 21, 10) = 'LVM-6141KR' THEN FN_CONTROL_ERROR_6141(CONTROL_ERROR) " // scheo 코레일유통 신형자판기
								+ " WHEN CONTROL_ERROR IS NOT NULL AND SUBSTR(PD_ERROR, 21, 6) = 'DONGGU' THEN FN_CONTROL_ERROR_DONGGU(CONTROL_ERROR) "  // scheo 20201013 동구전자 신형자판기 상태정보 추가
								+ " ELSE '' END AS CONTROL_ERROR," 
							+ " FN_PD_ERROR(PD_ERROR) AS PD_ERROR,"
							+ " FN_EMPTY_COL(EMPTY_COL) AS EMPTY_COL,"
							+ " CASE RES_CODE"
									+ " WHEN '0000' THEN '정상'"
									+ " WHEN '0001' THEN '재전송'"
									+ " WHEN '0002' THEN '타임아웃'"
									+ " ELSE '에러'"
								+ " END AS RES_CODE"
						+ " FROM ("
								+ " SELECT"
										+ " ROWNUM AS ROW_NUM,"
										+ " AA.*"
									+ " FROM ("
											+ " SELECT" + (HINT.length() > 0 ? " /*+" + HINT + " */" : "")
													+ " A.*"
												+ " FROM TB_TXT_STATUS A"
													+ JOIN
												+ WHERE
												+ " ORDER BY A.CREATE_DATE DESC, A.TRANSACTION_NO DESC, A.TERMINAL_ID"
										+ " ) AA"
									+ " WHERE ROWNUM <= " + e
							+ " )"
						+ " WHERE ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("TRANSACTION_NO", rs.getString("TRANSACTION_NO"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("PLACE", rs.getString("PLACE"));
				c.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				c.put("SOLD_OUT", rs.getString("SOLD_OUT"));
				c.put("CONTROL_ERROR", rs.getString("CONTROL_ERROR"));
				c.put("PD_ERROR", rs.getString("PD_ERROR"));
				c.put("EMPTY_COL", rs.getString("EMPTY_COL"));
				c.put("RES_CODE", rs.getString("RES_CODE"));
				c.put("NO", no--);

				this.list.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
	/**
	 * 운영 정보 로그 excel 다운로드
	 *
	 * @param sDate 검색 시작일
	 * @param eDate 검색 종료일
	 * @param sQuery 검색어
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String log(String sDate, String eDate, String sQuery) throws Exception {
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

	// 검색절 생성
		String HINT = "";
		String JOIN = "";
		String WHERE = "";

		if (!StringEx.isEmpty(sQuery)) { // 단말기 ID
			WHERE += " AND A.TERMINAL_ID = '" + sQuery + "'";
		}

		if (!StringEx.isEmpty(sDate)) { // 시작일
			WHERE += " AND A.RESPONSE_DATE >= '" + sDate + "'";
		}

		if (!StringEx.isEmpty(eDate)) { // 종료일
			WHERE += " AND A.RESPONSE_DATE <= '" + eDate + "'";
		}

		if (WHERE.length() > 0) HINT = " INDEX(A)";

		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
			HINT = " ORDERED" + HINT + " INDEX(B) USE_HASH(B)";
			JOIN = " INNER JOIN TB_VENDING_MACHINE B"
					+ " ON A.TERMINAL_ID = B.TERMINAL_ID"
						+ " AND B.USER_SEQ = " + this.cfg.get("user.seq");
		} else if ((this.cfg.getLong("user.company") > 0) || (this.cfg.getLong("user.organ") > 0)) {
			HINT += " ORDERED" + HINT + " INDEX(B) USE_HASH(B)";
			JOIN = " INNER JOIN TB_VENDING_MACHINE B"
					+ " ON A.TERMINAL_ID = B.TERMINAL_ID";

			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
				JOIN += " AND B.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			}

			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색
//20160221 INDEX 힌트 추가
//					WHERE += " AND B.ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ)";
				JOIN += " AND B.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(A_A) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION A_A"
								+ " WHERE SORT = 1"
								+ " START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ")
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
					;
			}
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

	// 총 레코드수
//20160221 INDEX 힌트 추가, USE_NL을 USE_HASH로 변경
//			this.records = StringEx.str2long(dbLib.getResult(conn, "SELECT /*+ ORDERED USE_NL(B C) */ COUNT(*) FROM TB_TXT_STATUS A LEFT JOIN TB_VENDING_MACHINE B ON A.TERMINAL_ID = B.TERMINAL_ID LEFT JOIN TB_USER C ON B.USER_SEQ = C.SEQ WHERE " + WHERE));
//			this.records = StringEx.str2long(
//					dbLib.getResult(conn,
//							"SELECT" + (HINT.length() > 0 ? " /*+" + HINT + " */" : "")
//									+ " COUNT(*)"
//								+ " FROM TB_TXT_STATUS A"
//									+ JOIN
//								+ WHERE
//						)
//				);

	// 총 페이지수
		//this.pages = Pager.getSize(this.records, cfg.getInt("limit.list"));

	// 리스트 가상번호
		//long no = this.records - (pageNo - 1) * cfg.getInt("limit.list");

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();

		try {
//				int s = ((pageNo - 1) * cfg.getInt("limit.list")) + 1;
//				int e = (s - 1) + cfg.getInt("limit.list");
//20160221 INDEX 힌트 변경, 불필요한 JOIN 제거, ORDER BY 추가
//				ps = dbLib.prepareStatement(conn, "SELECT S2.*"
//					+ " FROM"
//					+ " ("
//					+ " SELECT ROWNUM AS RNUM, S1.*"
//					+ " FROM"
//					+ " ("
//					+ " SELECT /*+ ORDERED USE_NL(B C) INDEX_DESC(A UK_TXT_STATUS) */ A.TRANSACTION_NO, A.TERMINAL_ID, A.PLACE, TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD HH24:MI:SS') AS CREATE_DATE, FN_SOLD_OUT(A.SOLD_OUT) AS SOLD_OUT, FN_CONTROL_ERROR(A.CONTROL_ERROR) AS CONTROL_ERROR, FN_PD_ERROR(A.PD_ERROR) AS PD_ERROR, FN_EMPTY_COL(A.EMPTY_COL) AS EMPTY_COL, CASE A.RES_CODE WHEN '0000' THEN '정상' WHEN '0001' THEN '재전송' WHEN '0002' THEN '타임아웃' ELSE '에러' END AS RES_CODE"
//					+ " FROM TB_TXT_STATUS A"
//					+ " LEFT JOIN TB_VENDING_MACHINE B ON A.TERMINAL_ID = B.TERMINAL_ID"
//					+ " LEFT JOIN TB_USER C ON B.USER_SEQ = C.SEQ"
//					+ " WHERE " + WHERE
//					+ " ) S1"
//					+ " WHERE ROWNUM <= " + e
//					+ " ) S2"
//					+ " WHERE RNUM >= " + s);
			ps = dbLib.prepareStatement(conn,
					"SELECT"
							+ " TRANSACTION_NO,"
							+ " TERMINAL_ID,"
							+ " PLACE,"
							+ " NVL(TO_CHAR(MODIFY_DATE, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(CREATE_DATE, 'YYYY-MM-DD HH24:MI:SS')) AS CREATE_DATE,"
							+ " FN_SOLD_OUT(SOLD_OUT) AS SOLD_OUT,"
							//+ " FN_CONTROL_ERROR(CONTROL_ERROR) AS CONTROL_ERROR,"
							+ " CASE WHEN CONTROL_ERROR IS NOT NULL AND SUBSTR(PD_ERROR, 21, 10) <> 'LVM-6141KR' AND SUBSTR(PD_ERROR, 21, 6) <> 'DONGGU' THEN FN_CONTROL_ERROR(CONTROL_ERROR) "
								+ " WHEN CONTROL_ERROR IS NOT NULL AND SUBSTR(PD_ERROR, 21, 10) = 'LVM-6141KR' THEN FN_CONTROL_ERROR_6141(CONTROL_ERROR) " // scheo 코레일유통 신형자판기
								+ " WHEN CONTROL_ERROR IS NOT NULL AND SUBSTR(PD_ERROR, 21, 6) = 'DONGGU' THEN FN_CONTROL_ERROR_DONGGU(CONTROL_ERROR) "  // scheo 20201013 동구전자 신형자판기 상태정보 추가
								+ " ELSE '' END AS CONTROL_ERROR," 
							+ " FN_PD_ERROR(PD_ERROR) AS PD_ERROR,"
							+ " FN_EMPTY_COL(EMPTY_COL) AS EMPTY_COL,"
							+ " CASE RES_CODE"
									+ " WHEN '0000' THEN '정상'"
									+ " WHEN '0001' THEN '재전송'"
									+ " WHEN '0002' THEN '타임아웃'"
									+ " ELSE '에러'"
								+ " END AS RES_CODE"
						+ " FROM ("
								+ " SELECT"
//											+ " ROWNUM AS ROW_NUM,"
										+ " AA.*"
									+ " FROM ("
											+ " SELECT" + (HINT.length() > 0 ? " /*+" + HINT + " */" : "")
													+ " A.*"
												+ " FROM TB_TXT_STATUS A"
													+ JOIN
												+ WHERE
												+ " ORDER BY A.CREATE_DATE DESC, A.TRANSACTION_NO DESC, A.TERMINAL_ID"
										+ " ) AA"
//										+ " WHERE ROWNUM <= " + e
							+ " )"
//							+ " WHERE ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("TRANSACTION_NO", rs.getString("TRANSACTION_NO"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("PLACE", rs.getString("PLACE"));
				c.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				c.put("SOLD_OUT", rs.getString("SOLD_OUT"));
				c.put("CONTROL_ERROR", rs.getString("CONTROL_ERROR"));
				c.put("PD_ERROR", rs.getString("PD_ERROR"));
				c.put("EMPTY_COL", rs.getString("EMPTY_COL"));
				c.put("RES_CODE", rs.getString("RES_CODE"));
//					c.put("NO", no--);

				this.list.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
/**
 * 삭제
 *
 * @param seq 등록번호
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String delete(long seq) throws Exception {
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		String error = null;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}

	// 판매 기록 체크
//20160221 INDEX 힌트 추가, JOIN 순서 변경
//		long check = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_SALES WHERE VM_PLACE_SEQ IN (SELECT SEQ FROM TB_VENDING_MACHINE_PLACE WHERE VM_SEQ = " + seq + ")"));
		long check = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT /*+ ORDERED INDEX(A) USE_NL(B) */"
								+ " COUNT(*)"
							+ " FROM TB_VENDING_MACHINE_PLACE A"
								+ " INNER JOIN TB_SALES B"
									+ " ON A.SEQ = B.VM_PLACE_SEQ"
						+ " WHERE VM_SEQ = " + seq
					)
			);

		if (check > 0) {
			error = "삭제하고자 하는 자판기의 판매 기록이 존재합니다.";
		}

	// 삭제
		if (StringEx.isEmpty(error)) {
			try {
//20160221 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "DELETE FROM TB_VENDING_MACHINE WHERE SEQ = ?");
				ps = dbLib.prepareStatement(conn,
						"DELETE /*+ INDEX(A) */"
							+ " FROM TB_VENDING_MACHINE A"
							+ " WHERE SEQ = ?"
					);
				ps.setLong(1, seq);
				ps.executeUpdate();
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(ps);
			}
		}

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
/**
 * 마스터 목록
 *
 * @param company 소속
 * @param pageNo 페이지
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String master(long company, int pageNo, String sField, String sQuery) throws Exception {
		return this.master(company, pageNo, sField, sQuery, true);
	}
/**
 * 마스터 목록
 *
 * @param company 소속
 * @param pageNo 페이지
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @param isAll 전체 검색 여부
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String master(long company, int pageNo, String sField, String sQuery, boolean isAll) throws Exception {
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

	// 소속
		long user_company = this.cfg.getLong("user.company");

		this.company = new ArrayList<GeneralConfig>();

		if (isAll) {
			try {
//INDEX 힌트 변경, CASE WHEN 제거 ORDER BY 추가
//				ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_COMPANY) */ SEQ, NAME FROM TB_COMPANY A WHERE (CASE WHEN ? > 0 THEN SEQ ELSE 1 END) = (CASE WHEN ? > 0 THEN ? ELSE 1 END)");
//				ps.setLong(1, this.cfg.getLong("user.company"));
//				ps.setLong(2, this.cfg.getLong("user.company"));
//				ps.setLong(3, this.cfg.getLong("user.company"));
				ps = dbLib.prepareStatement(conn,
						"SELECT" + (user_company > 0 ? " /*+ INDEX(A) */" : "")
								+ " SEQ,"
								+ " NAME"
							+ " FROM TB_COMPANY A"
					+ (user_company > 0
							? " WHERE SEQ = ?"
							: ""
						)
							+ " ORDER BY NAME"
					);
				if (user_company > 0) ps.setLong(1, user_company);
				rs = ps.executeQuery();

				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();

					c.put("SEQ", rs.getLong("SEQ"));
					c.put("NAME", rs.getString("NAME"));

					this.company.add(c);
				}
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(rs);
				dbLib.close(ps);
			}

			if (!StringEx.isEmpty(error)) {
				dbLib.close(conn);
				return error;
			}
		}

	// 검색절 생성
		String HINT = "";
		String WHERE = "";

		if (user_company > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND A.COMPANY_SEQ = " + user_company;
		}

		if (company > 0) { // 소속
			WHERE += " AND A.COMPANY_SEQ = " + company;
		}

		if (WHERE.length() > 0) HINT = " INDEX(A)";

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
		}

		if (!isAll) { // 미운영 자판기만
			WHERE += " AND NOT EXISTS ("
						+ " SELECT /*+ INDEX(A_A) */"
								+ " 1"
							+ " FROM TB_VENDING_MACHINE A_A"
							+ " WHERE COMPANY_SEQ = A.COMPANY_SEQ"
								+ " AND CODE = A.CODE"
					+ " )"
				;
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

	// 총 레코드수
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT" + (HINT.length() > 0 ? " /*+ " + HINT + " */" : "")
								+ " COUNT(*)"
							+ " FROM TB_MASTER_VENDING_MACHINE A"
							+ WHERE
					)
			);

	// 총 페이지수
		this.pages = Pager.getSize(this.records, cfg.getInt("limit.list"));

	// 리스트 가상번호
		long no = this.records - (pageNo - 1) * cfg.getInt("limit.list");

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();

		try {
			int s = ((pageNo - 1) * cfg.getInt("limit.list")) + 1;
			int e = (s - 1) + cfg.getInt("limit.list");

			ps = dbLib.prepareStatement(conn,
					"SELECT"
							+ " COMPANY_SEQ,"
							+ " CODE,"
							+ " REGION,"
							+ " PLACE,"
							+ " MODEL,"
							+ " TO_CHAR(CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE,"
							+ " TO_CHAR(MODIFY_DATE, 'YYYY-MM-DD') AS MODIFY_DATE,"
							+ " COMPANY"
						+ " FROM ("
								+ " SELECT"
										+ " ROWNUM AS ROW_NUM,"
										+ " AA.*"
									+ " FROM ("
											+ " SELECT /*+ ORDERED" + HINT + " USE_HASH(B) */"
													+ " A.*,"
													+ " B.NAME AS COMPANY"
												+ " FROM TB_MASTER_VENDING_MACHINE A"
													+ " INNER JOIN TB_COMPANY B"
														+ " ON A.COMPANY_SEQ = B.SEQ"
												+ WHERE
												+ " ORDER BY " + (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery) ? sField : "B.NAME, A.REGION, A.PLACE, CODE")
												//+ (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery) ? " ORDER BY " + sField : "")
										+ " ) AA"
									+ " WHERE ROWNUM <= " + e
							+ " )"
						+ " WHERE ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				c.put("CODE", rs.getString("CODE"));
				c.put("REGION", rs.getString("REGION"));
				c.put("PLACE", rs.getString("PLACE"));
				c.put("MODEL", rs.getString("MODEL"));
				c.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				c.put("MODIFY_DATE", rs.getString("MODIFY_DATE"));
				c.put("COMPANY", rs.getString("COMPANY"));
				c.put("NO", no--);

				this.list.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}

	// 리소스 반환
		dbLib.close(conn);

		return null;
	}
/**
 * 마스터 조회
 *
 * @param company 소속
 * @param code 코드
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String master(long company, String code) throws Exception {
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

	// 소속
		long user_company = this.cfg.getLong("user.company");

		this.company = new ArrayList<GeneralConfig>();

		try {
//20160221 INDEX 힌트 변경, CASE WHEN 제거, ORDER BY 추가
//			ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_COMPANY) */ SEQ, NAME FROM TB_COMPANY A WHERE (CASE WHEN ? > 0 THEN SEQ ELSE 1 END) = (CASE WHEN ? > 0 THEN ? ELSE 1 END)");
//			ps.setLong(1, this.cfg.getLong("user.company"));
//			ps.setLong(2, this.cfg.getLong("user.company"));
//			ps.setLong(3, this.cfg.getLong("user.company"));
			ps = dbLib.prepareStatement(conn,
					"SELECT" + (user_company > 0 ? " /*+ INDEX(A) */" : "")
							+ " SEQ,"
							+ " NAME"
						+ " FROM TB_COMPANY A"
				+ (user_company > 0
						? " WHERE SEQ = ?"
						: ""
					)
						+ " ORDER BY NAME"
				);
			if (user_company > 0) ps.setLong(1, user_company);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("NAME", rs.getString("NAME"));

				this.company.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}

	// 등록된 내용 가져오기
		this.data = new GeneralConfig();

		if (company > 0 && !StringEx.isEmpty(code)) {
			try {
//20160221 INDEX 힌트 추가, CASE WHEN 제거
//				ps = dbLib.prepareStatement(conn, "SELECT * FROM TB_MASTER_VENDING_MACHINE WHERE COMPANY_SEQ = ? AND CODE = ? AND (CASE WHEN ? > 0 THEN COMPANY_SEQ ELSE 1 END) = (CASE WHEN ? > 0 THEN ? ELSE 1 END)");
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " *"
							+ " FROM TB_MASTER_VENDING_MACHINE"
							+ " WHERE COMPANY_SEQ = ?"
								+ " AND CODE = ?"
						+ (user_company > 0
								? " AND COMPANY_SEQ = ?"
								: ""
							)
					);
				ps.setLong(1, company);
				ps.setString(2, code);
				if (user_company > 0) ps.setLong(3, user_company);
				rs = ps.executeQuery();

				if (rs.next()) {
					this.data.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
					this.data.put("CODE", rs.getString("CODE"));
					this.data.put("REGION", rs.getString("REGION"));
					this.data.put("PLACE", rs.getString("PLACE"));
					this.data.put("MODEL", rs.getString("MODEL"));
				} else {
					error = "등록되지 않았거나 수정할 권한이 없는 자판기입니다.";
				}
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(rs);
				dbLib.close(ps);
			}

			if (!StringEx.isEmpty(error)) {
				dbLib.close(conn);
				return error;
			}
		}

	// 리소스 반환
		dbLib.close(conn);

		return null;
	}
/**
 * 마스터 등록/수정
 *
 * @param company 소속
 * @param code 코드
 * @param region 설치지역
 * @param place 설치위치
 * @param model 모델
 * @param isAuto 코드 자동생성 여부
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String master(long company, String code, String region, String place, String model, boolean isAuto, long type) throws Exception {
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		CallableStatement cs = null;
		String error = null;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}

	// 코드
		if (isAuto) {
//20160221 INDEX 힌트 추가
//			code = dbLib.getResult(conn, "SELECT LPAD(TO_CHAR(NVL(MAX(TO_NUMBER(CODE)), 0) + 1), 8, '0') FROM TB_MASTER_VENDING_MACHINE WHERE COMPANY_SEQ = " + company);
			code = dbLib.getResult(conn,
					"SELECT /*+ INDEX(A) */"
							+ " LPAD(TO_CHAR(NVL(MAX(TO_NUMBER(CODE)), 0) + 1), 8, '0')"
						+ " FROM TB_MASTER_VENDING_MACHINE A"
						+ " WHERE COMPANY_SEQ = " + company
				);
		}

		// 전체 Row 코드값 중복 방지
		// 소속과 무관하게 코드값을 고유값으로 지정하여 중복 등록 방지를 위한 Row
		// 2020.10.12 by j.w.chae
		// code: 자판기코드값 / seq: 허수 (Not Use) / 1: 기초정보관리 / type: 2(등록)
		long seq=0;
		if(type==2) {
			if(!this.validCheck(code,seq, 2)) {
				error="동일한 코드값이 존재합니다.";
				dbLib.close(cs);
				dbLib.close(conn);
				return error;
			}
		}
		// 전체 Row 코드값 중복 방지 End
	// 등록/수정
		try {
			cs = dbLib.prepareCall(conn, "{ CALL SP_MASTER_VENDING_MACHINE (?, ?, ?, ?, ?, ?, ?, ?) }");
			cs.setString(1, this.cfg.get("server"));
			cs.setLong(2, company);
			cs.setString(3, code);
			cs.setString(4, region);
			cs.setString(5, place);
			cs.setString(6, model);
			cs.setLong(7, this.cfg.getLong("user.seq"));
			cs.registerOutParameter(8, OracleTypes.VARCHAR);
			cs.execute();

			if (cs.getString(8) != null && cs.getString(8).equals("Y")) {
				error = "예기치 않은 오류가 발생하여 자판기를 등록하는데 실패하였습니다.";
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(cs);
		}

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
/**
 * 마스터 일괄등록
 *
 * @param company 소속
 * @param excel 엑셀
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String master(long company, File excel) throws Exception {
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		CallableStatement cs = null;
		Workbook workbook = null;
		Sheet sheet = null;
		String error = null;
		String startDate = DateTime.date("yyyyMMddHHmmss");
		int success = 0;
		int failure = 0;

	// 인수 체크
		if (company == 0) {
			error = "소속이 존재하지 않습니다.";
		} else if (excel == null || excel.length() <= 0) {
			error = "등록하실 엑셀을 업로드하세요.";
		} else if (!StringEx.inArray(FileEx.extension(excel.getName()), "xls".split(";"))) {
			error = "XLS 파일만 등록이 가능합니다.";
		}

		if (!StringEx.isEmpty(error)) {
			if (excel.exists()) {
				excel.delete();
			}

			return error;
		}

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			if (excel.exists()) {
				excel.delete();
			}

			return "DB 연결에 실패하였습니다.";
		}

	// 엑셀 읽기
		try {
			workbook = Workbook.getWorkbook(excel);

			if (workbook != null) {
				sheet = workbook.getSheet(0);

				if (sheet != null) {
					if (sheet.getRows() <= 0) {
						error = "엑셀에 데이터가 존재하지 않습니다.";
					} else {
						if (this.cfg.getInt("excel.limit") > 0 && sheet.getRows() <= this.cfg.getInt("excel.limit")) {
							cs = dbLib.prepareCall(conn, "{ CALL SP_MASTER_VENDING_MACHINE (?, ?, ?, ?, ?, ?, ?, ?) }");

							for (int i = 1; i < sheet.getRows(); i++) {
								Cell code = sheet.getCell(0, i);
								Cell region = sheet.getCell(1, i);
								Cell place = sheet.getCell(2, i);
								Cell model = sheet.getCell(3, i);

								if (code == null || place == null) {
									continue;
								}

								try {
									cs.setString(1, this.cfg.get("server"));
									cs.setLong(2, company);
									cs.setString(3, code.getContents());
									cs.setString(4, region.getContents());
									cs.setString(5, place.getContents());
									cs.setString(6, model.getContents());
									cs.setLong(7, this.cfg.getLong("user.seq"));
									cs.registerOutParameter(8, OracleTypes.VARCHAR);
									cs.execute();

									if (cs.getString(8) != null && cs.getString(8).equals("Y")) {
										failure++;
									} else {
										success++;
									}
								} catch (Exception e_) {
									this.logger.error(e_);
									error = e_.getMessage();
								}

								if (!StringEx.isEmpty(error)) {
									break;
								}
							}
						} else {
							error = "엑셀 데이터를 " + StringEx.comma(this.cfg.getInt("excel.limit")) + "건을 초과하여 등록할 수 없습니다. (라인수 = " + StringEx.comma(sheet.getRows() - 1) + "건)";
						}
					}
				} else {
					error = "Sheet가 존재하지 않습니다.";
				}
			} else {
				error = "Workbook이 존재하지 않습니다.";
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(cs);

			try {
				if (workbook != null) {
					workbook.close();
					workbook = null;
				}
			} catch (Exception e_) {
			}
		}

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);

			if (excel.exists()) {
				excel.delete();
			}

			return error;
		}

	// 배치 기록
		try {
			cs = dbLib.prepareCall(conn, "{ CALL SP_BATCH_LOG (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");
			cs.setString(1, this.cfg.get("server"));
			cs.setString(2, "WEB");
			cs.setInt(3, success);
			cs.setInt(4, failure);
			cs.setString(5, startDate.substring(0, 8));
			cs.setString(6, startDate.substring(8, 14));
			cs.setString(7, DateTime.date("yyyyMMdd"));
			cs.setString(8, DateTime.date("HHmmss"));
			cs.setString(9, "");
			cs.setString(10, "자판기 마스터 일괄 등록 완료");
			cs.execute();
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(cs);
		}

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);

			if (excel.exists()) {
				excel.delete();
			}

			return error;
		}

	// 리소스 반환
		dbLib.close(conn);

	// 파일 삭제
		if (excel.exists()) {
			excel.delete();
		}

		return null;
	}
/**
 * 마스터 삭제
 *
 * @param company 소속
 * @param code 코드
 * @param isDel 삭제여부
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String master(long company, String code, boolean isDel) throws Exception {
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		String error = null;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}

	// 삭제
		if (StringEx.isEmpty(error)) {
			try {
//20160221 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "DELETE FROM TB_MASTER_VENDING_MACHINE WHERE COMPANY_SEQ = ? AND CODE = ?");
				ps = dbLib.prepareStatement(conn,
						"DELETE /*+ INDEX(A) */"
							+ " FROM TB_MASTER_VENDING_MACHINE A"
							+ " WHERE COMPANY_SEQ = ?"
								+ " AND CODE = ?"
					);
				ps.setLong(1, company);
				ps.setString(2, code);
				ps.executeUpdate();
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(ps);
			}
		}

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
/**
 * 자판기 목록 excel 다운로드
 *
 * @param company 소속
 * @param organ 조직
 * @param aspCharge ASP 과금
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @param collectChk 수집오류단말기 조회
 * @return 에러가 있을 경우 에러 내용
 *
 */
	//public String getList(long company, long organ, String aspCharge, String sField, String sQuery) throws Exception {
	public String getList(long company, long organ, String aspCharge, String sField, String sQuery, String collectChk) throws Exception {
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

	// 검색절 생성
		String WHERE = "";

		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
			WHERE += " AND A.USER_SEQ = " + this.cfg.get("user.seq");
		} else {
			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
				WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			}

			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색
//20160221 INDEX 힌트 추가, UNION 통합
//				WHERE += " AND A.ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + this.cfg.getLong("user.organ") + ")";
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(A_A) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION A_A"
								+ " WHERE SORT = 1"
								+ " START WITH SEQ = " + this.cfg.getLong("user.organ")
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
					;
			}
		}

		if (company > 0) { // 소속
			WHERE += " AND A.COMPANY_SEQ = " + company;
		}

		if (organ > 0) { // 조직
//20160221 INDEX 힌트 추가, UNION 통합
//			WHERE += " AND A.ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + organ + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + organ + ")";
			WHERE += " AND A.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_B) */"
								+ " SEQ "
							+ " FROM TB_ORGANIZATION A_B"
							+ " WHERE SORT = 1"
							+ " START WITH SEQ = " + organ
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;
		}

		if ("Y".equals(aspCharge) || "N".equals(aspCharge)) { // ASP 과금여부
			WHERE += " AND ASP_CHARGE = '" + aspCharge + "'";
		}

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");
		
		
		// 201013 cdh 상태조회 2회 이상 변경 없는 단말기 조건 추가
		if(collectChk.equals("Y") && WHERE != "") {
			//WHERE += " AND D.terminal_id = A.terminal_id" + " AND D.transaction_no = A.transaction_no" + " AND D.create_date >= sysdate -1";
			WHERE += " AND D.terminal_id = A.terminal_id" + " AND D.transaction_no = A.transaction_no";
			
			if(company == 420 || company == 1732 || company == 944) {
				if(company == 420) {	// 이마트 24
					WHERE += " AND (( A.MODEL LIKE '출입%' AND (trunc((to_date(to_char(sysdate,'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss') - to_date(to_char(D.create_date, 'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss')) * 24 * 60) > 30)) "
						  +  " OR ( A.MODEL NOT LIKE '출입%' AND (trunc((to_date(to_char(sysdate,'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss') - to_date(to_char(D.create_date, 'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss')) * 24 * 60) > 120))) ";
				}else{ // gs25, 에스원
					WHERE += " AND trunc((to_date(to_char(sysdate,'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss') - to_date(to_char(D.create_date, 'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss')) * 24 * 60) > 30";
				}
			}else{
				WHERE += " AND trunc((to_date(to_char(sysdate,'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss') - to_date(to_char(D.create_date, 'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss')) * 24 * 60) > 120";
			}
		}
		// 201013 cdh 상태조회 2회 이상 변경 없는 단말기 조건 추가 끝
		

	// 총 레코드수
//		this.records = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_VENDING_MACHINE A LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ LEFT JOIN TB_USER C ON A.USER_SEQ = C.SEQ WHERE " + WHERE));

	// 총 페이지수
//		this.pages = Pager.getSize(this.records, cfg.getInt("limit.list"));

	// 리스트 가상번호
//		long no = this.records - (pageNo - 1) * cfg.getInt("limit.list");

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();

		try {
//			int s = ((pageNo - 1) * cfg.getInt("limit.list")) + 1;
//			int e = (s - 1) + cfg.getInt("limit.list");

//			ps = dbLib.prepareStatement(conn, "SELECT S2.*"
//				+ " FROM"
//				+ " ("
//				+ " SELECT ROWNUM AS RNUM, S1.*"
//				+ " FROM"
//				+ " ("
//				//20130703 운영자판기 속도개선을 위한 쿼리 수정
//				//+ " SELECT /*+ ORDERED USE_NL(B C D) INDEX_DESC(A PK_VM) */ A.SEQ, A.CODE, A.PLACE, A.TERMINAL_ID, A.MODEL, A.IS_SOLD_OUT, A.IS_CONTROL_ERROR, A.IS_PD_ERROR, A.IS_EMPTY_COL, TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE, B.NAME AS COMPANY, C.NAME AS USER_NAME, TO_CHAR(D.CREATE_DATE, 'YYYYMMDDHH24MISS') AS TRAN_DATE, (SELECT LTRIM(SYS_CONNECT_BY_PATH(NAME, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS ORGAN, (SELECT COUNT(*) FROM TB_SALES WHERE TERMINAL_ID = A.TERMINAL_ID AND COL_NO > 0) AS EMPTY_COL_SELLING"
//				+ " SELECT /*+ ORDERED USE_NL(B C D) INDEX_DESC(A PK_VM) */ A.SEQ, A.CODE, A.PLACE, A.TERMINAL_ID, A.MODEL, A.IS_SOLD_OUT, A.IS_CONTROL_ERROR, A.IS_PD_ERROR, A.IS_EMPTY_COL, TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE, B.NAME AS COMPANY, C.NAME AS USER_NAME, C.ID AS USER_ID, TO_CHAR(D.CREATE_DATE, 'YYYYMMDDHH24MISS') AS TRAN_DATE, (SELECT LTRIM(SYS_CONNECT_BY_PATH(NAME, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS ORGAN "
//				+ " FROM TB_VENDING_MACHINE A"
//				+ " LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ"
//				+ " LEFT JOIN TB_USER C ON A.USER_SEQ = C.SEQ"
//				+ " LEFT JOIN TB_TXT_STATUS D ON (A.TERMINAL_ID = D.TERMINAL_ID AND A.TRANSACTION_NO = D.TRANSACTION_NO)"
//				+ " WHERE " + WHERE + ") S1 ) S2");
////				+ " ) S1"
////				+ " WHERE ROWNUM <= " + e
////				+ " ) S2"
////				+ " WHERE RNUM >= " + s);
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED" + (WHERE.length() > 0 ? " INDEX(A)" : "") + " USE_HASH(B C) USE_NL(D) */"
							+ " B.NAME AS COMPANY,"
							+ " ("
									+ " SELECT /*+ INDEX(E) */"
											+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
										+ " FROM TB_ORGANIZATION E"
										+ " WHERE PARENT_SEQ = 0"
										+ " START WITH SEQ = A.ORGANIZATION_SEQ"
										+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
								+ " ) AS ORGAN,"
							+ " A.PLACE,"
							/*20180611 허승찬 운영자판기관리 엑셀 다운로드 수정 시작*/
							+ " A.MODEM,"
							/*20180611 허승찬 운영자판기관리 엑셀 다운로드 수정 시작*/
							+ " A.CODE,"
							+ " A.SEQ,"
							+ " A.TERMINAL_ID,"
							+ " A.MODEL,"
							+ " A.IS_SOLD_OUT,"
							/* 20180621 품절상세 정보 시작 허승찬 */
							+ " CASE WHEN D.SOLD_OUT IS NOT NULL THEN FN_SOLD_OUT(D.SOLD_OUT) ELSE '' END AS SOLD_OUT,"
							+ " CASE WHEN D.CONTROL_ERROR IS NOT NULL THEN FN_CONTROL_ERROR(D.CONTROL_ERROR) ELSE '' END AS CONTROL_ERROR,"
							+ " CASE WHEN D.PD_ERROR IS NOT NULL THEN FN_PD_ERROR(D.PD_ERROR) ELSE '' END AS PD_ERROR,"
							/* 20180621 품절상세 정보 종료 허승찬 */
							+ " A.IS_CONTROL_ERROR,"
							+ " A.IS_PD_ERROR,"
							+ " A.IS_EMPTY_COL,"
							+ " A.ASP_CHARGE,"
							+ " TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE,"
							/*20180622 허승찬 자판기관리 전자세금계산서 내역 시작*/
							+ " TO_CHAR(A.CREATE_DATE, 'YYYYMM') AS YYYYMM,"
							/*20180607 허승찬 가동상태 조회 엑셀 다운로드 추가 시작*/
							+ " TO_CHAR(D.CREATE_DATE, 'YYYY-MM-DD') AS CRT_DATE,"
							/*20180607 허승찬 가동상태 조회 엑셀 다운로드 추가 종료*/
							/**/
							+ " (31-TO_CHAR(A.create_date, 'DD'))*167 AS OPEN_COM,"
							+ " (31-TO_CHAR(A.create_date, 'DD'))*160 AS OPEN_COM_COCA,"
							/*20180622 허승찬 자판기관리 전자세금계산서 내역 종료*/
							+ " C.NAME AS USER_NAME,"
							+ " C.ID AS USER_ID,"
							+ " TO_CHAR(D.CREATE_DATE, 'YYYYMMDDHH24MISS') AS TRAN_DATE"
						+ " FROM TB_VENDING_MACHINE A"
							+ " LEFT JOIN TB_COMPANY B"
								+ " ON A.COMPANY_SEQ = B.SEQ"
							+ " LEFT JOIN TB_USER C"
								+ " ON A.USER_SEQ = C.SEQ"
							+ " LEFT JOIN TB_TXT_STATUS D"
								+ " ON A.TERMINAL_ID = D.TERMINAL_ID"
									+ " AND A.TRANSACTION_NO = D.TRANSACTION_NO"
							+ " JOIN TBLTERMMST@VANBT TERMST ON TERMST.TERMINALID = A.TERMINAL_ID"
							+ " JOIN TBLBIZMST@VANBT MST ON TERMST.BUSINESSNO = MST.BUSINESSNO AND MST.BIZTYPE = TERMST.BIZTYPE "
						+ WHERE
						+ " ORDER BY 1, 2, 3, 4"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("CODE", rs.getString("CODE"));
				c.put("PLACE", rs.getString("PLACE"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("MODEL", rs.getString("MODEL"));
				/*20180611 허승찬 운영자판기관리 엑셀 다운로드 추가 시작*/
				c.put("MODEM", rs.getString("MODEM"));
				/*20180611 허승찬 운영자판기관리 엑셀 다운로드 추가 종료*/
				/*20180622 허승찬 자판기관리 전자세금계산서 내역 시작*/
				c.put("OPEN_COM", rs.getString("OPEN_COM"));
				c.put("OPEN_COM_COCA", rs.getString("OPEN_COM_COCA"));
				/*20180622 허승찬 자판기관리 전자세금계산서 내역 종료*/
				c.put("IS_SOLD_OUT", rs.getString("IS_SOLD_OUT"));
				/* 20180621 품절상세 정보 시작 허승찬 */
				c.put("SOLD_OUT", rs.getString("SOLD_OUT"));
				c.put("CONTROL_ERROR", rs.getString("CONTROL_ERROR"));
				c.put("PD_ERROR", rs.getString("PD_ERROR"));
				/* 20180621 품절상세 정보 종료 허승찬 */
				c.put("IS_CONTROL_ERROR", rs.getString("IS_CONTROL_ERROR"));
				c.put("IS_PD_ERROR", rs.getString("IS_PD_ERROR"));
				c.put("IS_EMPTY_COL", rs.getString("IS_EMPTY_COL"));
				c.put("ASP_CHARGE", rs.getString("ASP_CHARGE"));
				c.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				/*20180622 허승찬 자판기관리 전자세금계산서 내역 시작*/
				c.put("YYYYMM", rs.getString("YYYYMM"));
				/*20180622 허승찬 자판기관리 전자세금계산서 내역 종료*/
				/*20180607 허승찬 가동상태 조회 엑셀 다운로드 추가 시작*/
				c.put("CRT_DATE", rs.getString("CRT_DATE"));
				/*20180607 허승찬 가동상태 조회 엑셀 다운로드 추가 종료*/
				c.put("COMPANY", rs.getString("COMPANY"));
				c.put("USER_NAME", rs.getString("USER_NAME"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("IS_UPDATE", StringEx.isEmpty(rs.getString("TRAN_DATE")) ? "N" : "Y");
				//20131213 3시간에서 24시간으로 변경
//				c.put("IS_EXPIRE", !StringEx.isEmpty(rs.getString("TRAN_DATE")) && DateTime.getDifferTime(rs.getString("TRAN_DATE")) > 3600 * 3 ? "Y" : "N");
				c.put("IS_EXPIRE", !StringEx.isEmpty(rs.getString("TRAN_DATE")) && DateTime.getDifferTime(rs.getString("TRAN_DATE")) > 3600 * 24 ? "Y" : "N");
				////20130703 운영자판기 속도개선을 위한 쿼리 수정
				//c.put("EMPTY_COL_SELLING", rs.getLong("EMPTY_COL_SELLING"));
				c.put("EMPTY_COL_SELLING", 0);
//				c.put("NO", no--);
				c.put("USER_ID", rs.getString("USER_ID"));

				this.list.add(c);
			}

			// 검색 설정
			String sDesc = "검색어=" + sQuery;

			if (company > 0) {
//20160221 INDEX 힌트 추가
//				sDesc += "&소속=" + dbLib.getResult(conn, "SELECT NAME FROM TB_COMPANY WHERE SEQ = " + company);
				sDesc += "&소속="
						+ dbLib.getResult(conn,
								"SELECT /*+ INDEX(A) */"
										+ " NAME"
									+ " FROM TB_COMPANY A"
									+ " WHERE SEQ = " + company
							);
			}

			if (organ > 0) {
//20160221 INDEX 힌트 추가, REVERSE 적용
//				sDesc += "&조직=" + dbLib.getResult(conn, "SELECT LTRIM(SYS_CONNECT_BY_PATH(NAME, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = " + organ + " START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ");
				sDesc += "&조직="
						+ dbLib.getResult(conn,
								"SELECT /*+ INDEX(A) */"
										+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
									+ " FROM TB_ORGANIZATION A"
									+ " WHERE PARENT_SEQ = 0"
									+ " START WITH SEQ = " + organ
									+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
							)
					;
			}

			this.data.put("sDesc", sDesc);

		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
	
/**
 * 유효성 검사 추가.
 * 자판기 코드 고유 값으로 중복 방어 로직
 * @param code 자판기코드
 * @param type 0:운영자판기 / 1:기초정보 관리
 * @param seq  운영자판기관리 페이지 내에서만 사용하는 파라미터
 */
	public boolean validCheck(String code, long seq, int type) {
	// 실행에 사용될 변수
	DBLibrary dbLib = new DBLibrary(this.logger);
	Connection conn = null;
	PreparedStatement ps = null;
	ResultSet rs = null;
	String error = null;
	String query ="";
	// DB 연결
	conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

	if (conn == null) {
		return false;
	}
		
	// 코드값 검사.
	switch(type) {
	// 운영자판기 등록
	case 0:
		query = "SELECT COUNT(*) AS CNT"
			   +"  FROM TB_VENDING_MACHINE"
			   +" WHERE CODE='"+code+"'";
		break;
	// 운영자판기 수정
	case 1:
		query = "SELECT COUNT(*) AS CNT"
				   +"  FROM TB_VENDING_MACHINE"
				   +" WHERE CODE='"+code+"'"
				   +"   AND SEQ<>'"+seq+"'";
		break;
	// 기초정보 관리 등록
	case 2:
		query = "SELECT COUNT(*) AS CNT"
		       +"  FROM TB_MASTER_VENDING_MACHINE"
			   +" WHERE CODE='"+code+"'";
		break;
	// 기초정보 관리 수정
	// 해당 사항 없음.
	default:
		break;
	}
	
	try {
		// Query 실행
		ps = dbLib.prepareStatement(conn,query);
		rs = ps.executeQuery();
		while(rs.next()) {
			int cnt=rs.getInt("CNT");
			System.out.println(cnt);
			if(cnt>=1) {
				return false;
			}
		}
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} finally {
		dbLib.close(rs);
		dbLib.close(ps);
	}
	dbLib.close(conn);
	return true;
	}

	/**
	 * 출입 자판기 리스트 (엑셀 다운로드)
	 * PageNo가 -1인 경우 메인 메소드에서 분기하여 처리된다.
	 * by Chae 2020.12.23
	 */
	public String getAccessList(long company, long organ,  String sField, String sQuery) throws Exception {
		return this.getAccessList(company, organ,-1,sField,sQuery);
	}
	/**
	 * 출입 자판기 리스트
	 *
	 * @param company 소속
	 * @param organ 조직
	 * @param pageNo 페이지
	 * @param sField 검색 필드
	 * @param sQuery 검색어
	 * @return 에러가 있을 경우 에러 내용
	 * 출입단말상태 조회
	 * by Chae 2020.12.23
	 */
	public String getAccessList(long company, long organ, int pageNo, String sField, String sQuery) throws Exception {
		// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String error = null;

		// DB연결 세팅
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}

		// 검색절 생성
		String WHERE = " AND TVM.MODEL LIKE '%출입%' ";
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자
			if (company > 0) { // 소속
				WHERE += " AND TVM.COMPANY_SEQ = " + company;
			}

			if (organ > 0) { // 조직
				WHERE += " AND TVM.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_B) */"
						+ " SEQ"
						+ " FROM TB_ORGANIZATION A_B"
						+ " WHERE SORT = 1"
						+ " START WITH SEQ = " + organ
						+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
				;
			}
		} else { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND TVM.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			if (organ > 0) { // 조직
				WHERE += " AND TVM.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_B) */"
						+ " SEQ"
						+ " FROM TB_ORGANIZATION A_B"
						+ " WHERE SORT = 1"
						+ " START WITH SEQ = " + organ
						+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
				;
			} else if (this.cfg.getLong("user.organ") > 0) {
				WHERE += " AND TVM.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_A) */"
						+ " SEQ"
						+ " FROM TB_ORGANIZATION A_A"
						+ " WHERE SORT = 1"
						+ " START WITH SEQ = " + this.cfg.getLong("user.organ")
						+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
				;
			}
		}
		// 조건절 세팅 완료.

		// WHERE2의 경우 출입단말 상태 Summary의 경우 사용하지 않아 별도로 세팅 시킨다.
		// 공통 조건절은 WHERE 변수에 세팅.
		// 데이터 테이블 세팅 조건절은 WHERE2변수에 세팅된다.
		String WHERE2="";
		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			
			//JRE 1.5 reBuild 2020.12.24 twkim / _sfEnum : sField numberic
			int _sFEnum=0;
			if(sField.equals("TVM.PLACE")) {
				_sFEnum=1;
			}else if(sField.equals("TVM.CODE")) {
				_sFEnum=2;
			}else if(sField.equals("TVM.ACCESS_STATUS")) {
				_sFEnum=3;
			}
			switch (_sFEnum){
				case 1:					// 위치명 검색인 경우
					WHERE2 += " AND TVM.PLACE LIKE '%"+sQuery+"%'";
					break;
				case 2:					// 위치 코드 검색인 경우
					WHERE2 += " AND TVM.CODE LIKE '%"+sQuery+"%'";
					break;
				case 3:			// 운영상태 조건 검색인 경우
					if(sQuery.contains("폐"))
						sQuery="C";
					else if(sQuery.contains("휴"))
						sQuery="R";
					else if(sQuery.contains("운"))
						sQuery="O";
					WHERE2 += " AND TVM.ACCESS_STATUS = '"+sQuery+"'";
					break;
				default:
					break;
			}
		}

		// 총 레코드수
		// 검색 결과에 나올 건수로 WHERE2조건을 추가하여 세팅한다.
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
				  "SELECT /*+ INDEX(A) */"
					+ " COUNT(*)"
					+ " FROM TB_TXT_STATUS TS "
					+ " INNER JOIN TB_VENDING_MACHINE TVM ON (TVM.TERMINAL_ID = TS.TERMINAL_ID AND TVM.TRANSACTION_NO = TS.TRANSACTION_NO " + WHERE+WHERE2+
					")"
				)
		);
		// data에 세팅할 기본 변수 세팅
		this.data = new GeneralConfig();

		// 조건에 사용할 ROWNUM 계산에 사용될 변수
		long s=0;		// row 시작 변수
		long e=0;		// row 마지막 변수

		// 웹페이지 검색인 경우 해당 로직으로 검색 (공통 계산 로직으로 적용)
		if(pageNo>=0){
			this.pages = Pager.getSize(this.records, cfg.getInt("limit.list"));
			long no = this.records - (pageNo - 1) * cfg.getInt("limit.list");
			s = ((pageNo - 1) * cfg.getInt("limit.list")) + 1;
			e = (s - 1) + cfg.getInt("limit.list");
		}else{	// 엑셀 저장인 경우 세팅되는 변수 값
			String desc="";
			if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
				System.out.println("검색 키워드: "+sField);
				System.out.println("검색 내용: "+sQuery);
				
				//JRE 1.5 reBuild 2020.12.24 twkim / _sfEnum : sField numberic
				int _sFEnum=0;
				if(sField.equals("TVM.PLACE")) {
					_sFEnum=1;
				}else if(sField.equals("TVM.CODE")) {
					_sFEnum=2;
				}else if(sField.equals("TVM.ACCESS_STATUS")) {
					_sFEnum=3;
				}				
				switch (_sFEnum){
					case 1:
						desc += "위치명: "+sQuery;
						break;
					case 2:
						desc += "위치 코드: "+sQuery;
						break;
					case 3:
						if(sQuery.equals("O"))
							desc += "운영상태: 운영";
						else if(sQuery.equals("C"))
							desc += "운영상태: 폐점";
						else if(sQuery.equals("R"))
							desc += "운영상태: 휴점";
						break;
					default:
						break;
				}
			}
			this.data.put("sDesc",desc);	// 검색 조건에 넣을 데이터를 세팅한다.
			s=1;
			e=this.records;	// row를 최대 치인 레코드 수만큼까지 출력하게 세팅한다.
		}
		// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		try {
			// 데이터 출력 영역(엑셀과 웹페이지 공통 사용)
			ps = dbLib.prepareStatement(conn,
					"SELECT " +
							"  A.ROW_NUM AS SEQ, "+
							"  A.CODE, " +
							"  A.PLACE, " +
							"  A.REGIST_DATE, " +
							"  A.CONTROL_ERROR, " +
							"  A.MODEL, " +
							"  A.TERMINAL_ID, " +
							"  A.MODEM_NBR, " +
							"  A.TERM_STATUS, " +
							"  TO_CHAR(A.CREATE_DATE,'YYYY-MM-DD HH24:MI:SS') AS CREATE_DATE, " +
							"  DECODE (A.FINAL_TRAN_DATE,NULL,'거래내역 없음',TO_CHAR( TO_DATE('20' || SUBSTR(A.FINAL_TRAN_DATE, 1, 12), 'YYYY-MM-DD HH24:MI:SS'),'YYYY-MM-DD HH24:MI:SS')) AS FINAL_TRAN_DATE, " +
							"  A.ACCESS_STATUS " +
							"FROM " +
							"  ( " +
							"  SELECT " +
							"    ROWNUM AS ROW_NUM, " +
							"    A.* " +
							"  FROM " +
							"    (SELECT " +
							"      CODE, " +
							"      TVM.PLACE, " +
							"      TO_CHAR(TVM.CREATE_DATE,'YYYY-MM-DD') AS REGIST_DATE, " +
							"    CASE " +
							"        WHEN TVM.ACCESS_STATUS = 'C' THEN '폐점' " +
							"		 WHEN TVM.ACCESS_STATUS = 'R' THEN '휴점' "	+
							"        ELSE '운영' " +
							"    END AS CONTROL_ERROR, " +
							"      MODEL, " +
							"      TVM.TERMINAL_ID, " +
							"      SUBSTR(TS.PD_ERROR , 41, 10) AS MODEM_NBR, " +
							"    CASE " +
							"        WHEN TRUNC((TO_DATE(TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss') - TO_DATE(TO_CHAR(TS.create_date, 'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss')) * 24 * 60) > 120 THEN '비정상' " +
							"        ELSE '정상' " +
							"    END AS TERM_STATUS, " +
							"      TS.CREATE_DATE, " +
							"      SUBSTR((SELECT MAX(TRANSACTION_NO) FROM TB_SALES TMP WHERE TMP.TERMINAL_ID = TVM.TERMINAL_ID ), 1, 12) AS FINAL_TRAN_DATE, " +
							"      ACCESS_STATUS " +
							"    FROM " +
							"      TB_VENDING_MACHINE TVM " +
							"    INNER JOIN TB_TXT_STATUS TS ON " +
							"      (TVM.TERMINAL_ID = TS.TERMINAL_ID " +
							"      AND TVM.TRANSACTION_NO = TS.TRANSACTION_NO ) " +
							WHERE+WHERE2+
							" ) A" +
							"  WHERE " +
							"    ROWNUM <= "+e+" ) A " +
							"WHERE " +
							"  ROW_NUM >= "+s
			);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();
				c.put("SEQ", rs.getString("SEQ"));
				c.put("CODE", rs.getString("CODE"));
				c.put("PLACE", rs.getString("PLACE"));
				c.put("REGIST_DATE", rs.getString("REGIST_DATE"));
				c.put("CONTROL_ERROR", rs.getString("CONTROL_ERROR"));
				c.put("MODEL", rs.getString("MODEL"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("MODEM_NBR", rs.getString("MODEM_NBR"));
				c.put("TERM_STATUS", rs.getString("TERM_STATUS"));
				c.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				c.put("FINAL_TRAN_DATE", rs.getString("FINAL_TRAN_DATE"));
				this.list.add(c);
			}
			// Summary 데이터 출력 영역.(전체 건수에 대한 내용을 출력시킨다.)
			rs=null;
			ps=null;
			ps = dbLib.prepareStatement(conn,
					"SELECT COUNT(CASE WHEN TRUNC((TO_DATE(TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss') - TO_DATE(TO_CHAR(TS.create_date, 'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss')) * 24 * 60) > 120 THEN 1 END ) AS ABNORMAL_COUNT, " +
					"   COUNT(CASE WHEN TRUNC((TO_DATE(TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss') - TO_DATE(TO_CHAR(TS.create_date, 'YYYY-MM-DD HH24:MI:SS'), 'yyyy-mm-dd hh24:mi:ss')) * 24 * 60) < 120 THEN 1 END ) AS NORMAL_COUNT, " +
					"   COUNT(CASE WHEN TVM.ACCESS_STATUS ='C' THEN 1 END) AS CLOSED_COUNT, " +
					"   COUNT(CASE WHEN TVM.ACCESS_STATUS ='R' THEN 1 END) AS REST_COUNT " +
					" FROM TB_TXT_STATUS TS " +
					" INNER JOIN TB_VENDING_MACHINE TVM ON (TS.TRANSACTION_NO = TVM.TRANSACTION_NO AND TS.TERMINAL_ID = TVM.TERMINAL_ID ) "+ WHERE
			);
			rs=ps.executeQuery();
			while(rs.next()){
				this.data.put("ABNORMAL_COUNT",rs.getInt("ABNORMAL_COUNT"));
				this.data.put("NORMAL_COUNT",rs.getInt("NORMAL_COUNT"));
				this.data.put("CLOSED_COUNT",rs.getInt("CLOSED_COUNT"));
				this.data.put("REST_COUNT",rs.getInt("REST_COUNT"));
				this.data.put("ALL_COUNT",rs.getInt("ABNORMAL_COUNT")+rs.getInt("NORMAL_COUNT"));
			}
		} catch (Exception exception) {
			this.logger.error(exception);
			error = exception.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

		// 리소스 반환
		dbLib.close(conn);
		return error;
	}

}