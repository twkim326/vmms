package com.nucco.beans;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Goods.java
 *
 * 판매 상품
 *
 * 작성일 - 2011/03/31, 정원광
 *
 */
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.nucco.GlobalConfig;
import com.nucco.cfg.GeneralConfig;
import com.nucco.lib.DateTime;
import com.nucco.lib.FileEx;
import com.nucco.lib.Pager;
import com.nucco.lib.StringEx;
import com.nucco.lib.db.DBLibrary;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import oracle.jdbc.OracleTypes;

public class Goods {
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
 * 그룹
 *
 */
	public ArrayList<GeneralConfig> group;
/**
 * 목록
 *
 */
	public ArrayList<GeneralConfig> list;
/**
 * 조회
 *
 */
	public GeneralConfig data;
/**
 * 총 레코드수
 *
 */
	public long records;
/**
 * 총 페이지수
 *
 */
	public long pages;
/**
 *
 */
	public Goods(GlobalConfig cfg) throws Exception {
	// set config
		this.cfg = cfg;

	// set log4j
		PropertyConfigurator.configure(cfg.get("config.log4j"));
		this.logger = Logger.getLogger(this.getClass());
	}
/**
 * 목록
 *
 * @param company 소속
 * @param group 그룹
 * @param pageNo 페이지
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getList(long company, long group, int pageNo, String sField, String sQuery) throws Exception {
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
//20160219 INDEX 힌트 변경, CASE WHEN 제거
//			ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_COMPANY) */ SEQ, NAME FROM TB_COMPANY A WHERE (CASE WHEN ? > 0 THEN SEQ ELSE 1 END) = (CASE WHEN ? > 0 THEN ? ELSE 1 END)");
//			ps.setLong(1, this.cfg.getLong("user.company"));
//			ps.setLong(2, this.cfg.getLong("user.company"));
//			ps.setLong(3, this.cfg.getLong("user.company"));
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
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

	// 선택된 그룹
		this.data = new GeneralConfig();

		if (group > 0) {
			try {
//20160219 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "SELECT DEPTH, PARENT_SEQ FROM TB_GROUP WHERE SEQ = ?");
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " DEPTH,"
								+ " PARENT_SEQ"
							+ " FROM TB_GROUP A"
							+ " WHERE SEQ = ?"
					);
				ps.setLong(1, group);
				rs = ps.executeQuery();

				if (rs.next()) {
					this.data.put("DEPTH", rs.getInt("DEPTH"));
					this.data.put("PARENT_SEQ", rs.getLong("PARENT_SEQ"));
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
//		String WHERE = " B.CODE <> 'X0000002'";
		String WHERE = "";

		if (user_company > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속 및 허가된 상품 그룹군에 있는 상품만 검색
//20160219 INDEX 힌트 추가
//			WHERE += " AND A.GROUP_SEQ IN (SELECT SEQ FROM TB_GROUP WHERE COMPANY_SEQ = " + this.cfg.getLong("user.company") + " UNION SELECT SEQ FROM TB_GROUP START WITH SEQ IN (SELECT GROUP_SEQ FROM TB_GROUP_ENABLED WHERE COMPANY_SEQ = " + this.cfg.getLong("user.company") + " GROUP BY GROUP_SEQ) CONNECT BY PRIOR SEQ = PARENT_SEQ)";
			WHERE += " AND (A.GROUP_SEQ, A.COMPANY_SEQ) IN ("
						+ " SELECT /*+ INDEX(A_A) */"
								+ " SEQ, COMPANY_SEQ"
							+ " FROM TB_GROUP A_A"
							+ " WHERE COMPANY_SEQ = " + user_company
						+ " UNION"
						+ " SELECT /*+ INDEX(A_B) */"
								+ " SEQ, COMPANY_SEQ"
							+ " FROM TB_GROUP A_B"
							+ " START WITH SEQ IN ("
									+ " SELECT /*+ INDEX(A_B_A) */"
											+ " GROUP_SEQ"
										+ " FROM TB_GROUP_ENABLED A_B_A"
										+ " WHERE COMPANY_SEQ = " + user_company
										+ " GROUP BY GROUP_SEQ"
								+ " )"
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " UNION"
						+ " SELECT /*+ INDEX(A_B) */"
								+ " SEQ, " + user_company
							+ " FROM TB_GROUP A_B"
							+ " START WITH SEQ IN ("
									+ " SELECT /*+ INDEX(A_B_A) */"
											+ " GROUP_SEQ"
										+ " FROM TB_GROUP_ENABLED A_B_A"
										+ " WHERE COMPANY_SEQ = " + user_company
										+ " GROUP BY GROUP_SEQ"
								+ " )"
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;
		}

		if (company > 0) {
//201602123 INDEX 힌트 추가, USE_NL 추가
//			WHERE += " AND A.COMPANY_SEQ IN (SELECT SEQ FROM TB_COMPANY WHERE SEQ = " + company + " UNION SELECT SB.COMPANY_SEQ FROM TB_GROUP_ENABLED SA INNER JOIN TB_GROUP SB ON SA.GROUP_SEQ = SB.SEQ AND SA.COMPANY_SEQ = " + company + ")";
			WHERE += " AND A.COMPANY_SEQ IN ("
						+ " SELECT /*+ INDEX(A_C) */"
								+ " SEQ"
							+ " FROM TB_COMPANY A_C"
							+ " WHERE SEQ = " + company
						+ " UNION"
						+ " SELECT /*+ ORDERED INDEX(A_D) USE_NL(A_E) */"
								+ " A_E.COMPANY_SEQ"
							+ " FROM TB_GROUP_ENABLED A_D"
								+ " INNER JOIN TB_GROUP A_E"
									+ " ON A_D.GROUP_SEQ = A_E.SEQ"
							+ " WHERE A_D.COMPANY_SEQ = " + company
					+ " )"
				;
		}

		if (group > 0) {
//20160219 INDEX 힌트 추가, UNION 통합 -> 서브쿼리 JOIN으로 변경
//			WHERE += " AND A.GROUP_SEQ IN (SELECT SEQ FROM TB_GROUP START WITH PARENT_SEQ = " + group + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_GROUP WHERE SEQ = " + group + ")";
//			WHERE += " AND A.GROUP_SEQ IN ("
//						+ " SELECT /*+ INDEX(A_E) */"
//								+ " SEQ"
//							+ " FROM TB_GROUP A_E"
//							+ " START WITH SEQ = " + group
//							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//					+ ")"
//				;
		}

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

	// 총 레코드수
//20160219 INDEX 힌트 추가
//		this.records = StringEx.str2long(dbLib.getResult(conn, "SELECT /*+ ORDERED USE_NL(B) */ COUNT(*) FROM TB_GOODS A LEFT JOIN TB_GROUP B ON A.GROUP_SEQ = B.SEQ WHERE " + WHERE));
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT /*+ ORDERED" + (WHERE.length() > 0 ? " INDEX(A)" : "") + " USE_HASH(BB) */"
								+ " COUNT(*)"
							+ " FROM TB_GOODS A"
								+ " INNER JOIN ("
										+ " SELECT" + (group > 0 ? " /*+ INDEX(B) */" : "")
												+ " *"
											+ " FROM TB_GROUP B"
											+ " WHERE CODE <> 'X0000002'"
								+ (group > 0
										?	  " START WITH SEQ = " + group
											+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
										: ""
									)
									+ " ) BB"
									+ " ON A.GROUP_SEQ = BB.SEQ"
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
//20160219 INDEX 힌트 변경/추가
//			ps = dbLib.prepareStatement(conn, "SELECT S2.*"
//					+ " FROM"
//					+ " ("
//					+ " SELECT ROWNUM AS RNUM, S1.*"
//					+ " FROM"
//					+ " ("
//					+ " SELECT /*+ ORDERED USE_NL(B C) INDEX_DESC(A PK_GOODS) */ A.SEQ, A.CODE, A.NAME, A.MEMO, TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE, TO_CHAR(A.MODIFY_DATE, 'YYYY-MM-DD') AS MODIFY_DATE, (CASE WHEN B.DEPTH = 0 THEN B.NAME ELSE (SELECT NAME FROM TB_GROUP WHERE SEQ = B.PARENT_SEQ) END) AS GROUP_0, (CASE WHEN B.DEPTH = 0 THEN '' ELSE B.NAME END) AS GROUP_1, C.NAME AS COMPANY"
//					+ " FROM TB_GOODS A LEFT JOIN TB_GROUP B ON A.GROUP_SEQ = B.SEQ LEFT JOIN TB_COMPANY C ON A.COMPANY_SEQ = C.SEQ"
//					+ " WHERE " + WHERE
//					+ " ) S1"
//					+ " WHERE ROWNUM <= " + e
//					+ " ) S2"
//					+ " WHERE RNUM >= " + s);
			ps = dbLib.prepareStatement(conn,
					"SELECT"
							+ " SEQ,"
							+ " CODE,"
							+ " NAME,"
							+ " MEMO,"
							+ " TO_CHAR(CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE,"
							+ " TO_CHAR(MODIFY_DATE, 'YYYY-MM-DD') AS MODIFY_DATE,"
							+ " GROUP_0,"
							+ " GROUP_1,"
							+ " COMPANY_SEQ,"
							+ " COMPANY"
						+ " FROM ("
								+ " SELECT"
										+ " ROWNUM AS ROW_NUM,"
										+ " AA.*"
									+ " FROM ("
											+ " SELECT /*+ ORDERED" + (WHERE.length() > 0 ? " INDEX(A)" : "") + " USE_HASH(BB C) */"
													+ " C.NAME AS COMPANY,"
													+ " CASE WHEN BB.DEPTH = 0 THEN BB.NAME"
														+ " ELSE ("
																+ " SELECT /*+ INDEX(D) */ NAME"
																	+ " FROM TB_GROUP D"
																	+ " WHERE SEQ = BB.PARENT_SEQ"
															+ " )"
														+ " END AS GROUP_0,"
													+ " CASE WHEN BB.DEPTH = 0 THEN '' ELSE BB.NAME END AS GROUP_1,"
													+ " A.NAME,"
													+ " A.SEQ,"
													+ " A.CODE,"
													+ " A.MEMO,"
													+ " A.CREATE_DATE,"
													+ " A.MODIFY_DATE,"
													+ " A.COMPANY_SEQ"
												+ " FROM TB_GOODS A"
													+ " INNER JOIN ("
															+ " SELECT" + (group > 0 ? " /*+ INDEX(B) */" : "")
																	+ " *"
																+ " FROM TB_GROUP B"
																+ " WHERE CODE <> 'X0000002'"
													+ (group > 0
															?	  " START WITH SEQ = " + group
																+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
															: ""
														)
														+ " ) BB"
														+ " ON A.GROUP_SEQ = BB.SEQ"
													+ " LEFT JOIN TB_COMPANY C"
														+ " ON A.COMPANY_SEQ = C.SEQ"
													+ WHERE
												+ " ORDER BY " + (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery) ? sField : "1, 2, 3, 4")
										+ " ) AA"
									+ " WHERE ROWNUM <= " + e
							+ " )"
						+ " WHERE ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("CODE", rs.getString("CODE"));
				c.put("NAME", rs.getString("NAME"));
				c.put("MEMO", rs.getString("MEMO"));
				c.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				c.put("MODIFY_DATE", rs.getString("MODIFY_DATE"));
				c.put("GROUP_0", rs.getString("GROUP_0"));
				c.put("GROUP_1", rs.getString("GROUP_1"));
				c.put("COMPANY_SEQ", rs.getString("COMPANY_SEQ"));
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
 * 그룹
 *
 * @param company 소속
 * @param group 상위 그룹
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String group(long company, long group) throws Exception {
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
		String WHERE = "";

		if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속에 대한 정보만 검색
			company = this.cfg.getLong("user.company");
		}

		if (company > 0) {
//20160219 INDEX 힌트 추가, GROUP BY 제거
//			if (group == 0) {
//				WHERE += " AND A.SEQ IN (SELECT SEQ FROM TB_GROUP WHERE COMPANY_SEQ = " + company + " AND DEPTH = 0 UNION SELECT GROUP_SEQ FROM TB_GROUP_ENABLED WHERE COMPANY_SEQ = " + company + " GROUP BY GROUP_SEQ)";
//			} else {
//				WHERE += " AND A.SEQ IN (SELECT SEQ FROM TB_GROUP WHERE COMPANY_SEQ = " + company + " AND DEPTH = 1 UNION SELECT SEQ FROM TB_GROUP WHERE PARENT_SEQ IN (SELECT GROUP_SEQ FROM TB_GROUP_ENABLED WHERE COMPANY_SEQ = " + company + " GROUP BY GROUP_SEQ))";
//			}
			WHERE += " AND SEQ IN ("
						+ " SELECT /*+ INDEX(A_A) */"
								+ " SEQ"
							+ " FROM TB_GROUP A_A"
							+ " WHERE COMPANY_SEQ = " + company
								+ " AND DEPTH = " + (group == 0 ? 0 : 1)
						+ " UNION"
				+ (group == 0
						? " SELECT /*+ INDEX(A_B) */"
								+ " GROUP_SEQ"
							+ " FROM TB_GROUP_ENABLED A_B"
							+ " WHERE COMPANY_SEQ = " + company
						: " SELECT /*+ ORDERED INDEX(A_B) */"
								+ " A_C.SEQ"
							+ " FROM TB_GROUP_ENABLED A_B"
								+ " INNER JOIN TB_GROUP A_C"
									+ " ON A_B.GROUP_SEQ = A_C.PARENT_SEQ"
							+ " WHERE A_B.COMPANY_SEQ = " + company
					)
					+ " )"
				;
		} else {
//20160219
//			if (group == 0) {
//				WHERE += " AND A.DEPTH = 0 AND A.COMPANY_SEQ = 0";
//			} else {
//				WHERE += " AND A.DEPTH = 1 AND A.COMPANY_SEQ = 0";
//			}
			HINT = " /*+ INDEX(A) */";
			WHERE += " AND COMPANY_SEQ = 0"
					+ " AND DEPTH = " + (group == 0 ? 0 : 1)
				;
		}

		if (group > 0) {
			HINT = " /*+ INDEX(A) */";
			WHERE += " AND PARENT_SEQ = " + group;
		}

	// 목록
		this.group = new ArrayList<GeneralConfig>();

		try {
//20160219 INDEX 힌트 변경
//			ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX_DESC(A PK_GROUP) */ SEQ, CODE, NAME FROM TB_GROUP A WHERE " + WHERE + " AND A.CODE NOT IN ('X0000002')");
			ps = dbLib.prepareStatement(conn,
					"SELECT" + HINT
							+ " SEQ,"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_GROUP A"
						+ " WHERE A.CODE <> 'X0000002'"
							+ WHERE
						+ " ORDER BY NAME"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("CODE", rs.getString("CODE"));
				c.put("NAME", rs.getString("NAME"));

				this.group.add(c);
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
 * 등록
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
//20160219 INDEX 힌트 변경, CASE WHEN 제거, ORDER BY 추가
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

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
/**
 * 개별 등록/수정
 *
 * @param seq 등록번호
 * @param company 소속
 * @param group 그룹
 * @param code 코드
 * @param name 이름
 * @param barcode 바코드
 * @param memo 설명
 * @param isAuto 코드 자동생성 여부
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(long seq, long company, long group, String code, String name, String barcode, String memo, boolean isAuto) throws Exception {
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
//20160219 INDEX 힌트 추가
//			code = dbLib.getResult(conn, "SELECT LPAD(TO_CHAR(NVL(MAX(TO_NUMBER(CODE)), 0) + 1), 8, '0') FROM TB_GOODS WHERE COMPANY_SEQ = " + company);
			code = dbLib.getResult(conn,
					"SELECT /*+ INDEX(A) */"
							+ " LPAD(TO_CHAR(NVL(MAX(TO_NUMBER(CODE)), 0) + 1), 8, '0')"
						+ " FROM TB_GOODS"
						+ " WHERE COMPANY_SEQ = " + company
				);
		}

	// 등록
		try {
			cs = dbLib.prepareCall(conn, "{ CALL SP_GOODS (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");
			cs.setString(1, this.cfg.get("server"));
			cs.setLong(2, seq);
			cs.setString(3, "");
			cs.setString(4, "");
			cs.setLong(5, company);
			cs.setLong(6, group);
			cs.setString(7, code);
			cs.setString(8, name);
			cs.setString(9, barcode);
			cs.setString(10, memo);
			cs.setLong(11, this.cfg.getLong("user.seq"));
			cs.registerOutParameter(12, OracleTypes.VARCHAR);
			cs.execute();

			if (cs.getString(12) != null && cs.getString(12).equals("Y")) {
				error = "예기치 않은 오류가 발생하여 상품을 등록하는데 실패하였습니다.";
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
 * 수정
 *
 * @param seq 등록번호
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String modify(long seq) throws Exception {
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

	// 조회
		long user_company = this.cfg.getLong("user.company");

		this.data = new GeneralConfig();

		try {
//			ps = dbLib.prepareStatement(conn, "SELECT A.*, B.PARENT_SEQ, B.DEPTH FROM TB_GOODS A LEFT JOIN TB_GROUP B ON A.GROUP_SEQ = B.SEQ WHERE A.SEQ = ? AND (CASE WHEN ? > 0 THEN A.COMPANY_SEQ ELSE 1 END) = (CASE WHEN ? > 0 THEN ? ELSE 1 END)");
//			ps.setLong(1, seq);
//			ps.setLong(2, this.cfg.getLong("user.company"));
//			ps.setLong(3, this.cfg.getLong("user.company"));
//			ps.setLong(4, this.cfg.getLong("user.company"));

			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED INDEX(A) USE_NL(B) */"
							+ " A.*,"
							+ " B.PARENT_SEQ,"
							+ " B.DEPTH"
						+ " FROM TB_GOODS A"
							+ " LEFT JOIN TB_GROUP B"
								+ " ON A.GROUP_SEQ = B.SEQ"
						+ " WHERE A.SEQ = ?"
					+ (user_company > 0
							? " AND A.COMPANY_SEQ = ?"
							: ""
						)
				);
			ps.setLong(1, seq);
			if (user_company > 0) ps.setLong(2, user_company);
			rs = ps.executeQuery();

			if (rs.next()) {
				this.data.put("SEQ", rs.getLong("SEQ"));
				this.data.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				this.data.put("GROUP_SEQ", rs.getLong("GROUP_SEQ"));
				this.data.put("PARENT_SEQ", rs.getLong("PARENT_SEQ"));
				this.data.put("GROUP_DEPTH", rs.getLong("DEPTH"));
				this.data.put("CODE", rs.getString("CODE"));
				this.data.put("NAME", rs.getString("NAME"));
				this.data.put("BAR_CODE", rs.getString("BAR_CODE"));
				this.data.put("MEMO", rs.getString("MEMO"));
			} else {
				error = "등록되지 않았거나 수정할 권한이 없는 상품입니다.";
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

	// 소속
		this.company = new ArrayList<GeneralConfig>();

		try {
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
			if (user_company > 0)  ps.setLong(1, user_company);
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
 * 일괄등록
 *
 * @param maker 그룹군
 * @param excel 엑셀
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(String maker, File excel) throws Exception {
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
		if (StringEx.isEmpty(maker)) {
			error = "그룹군이 존재하지 않습니다.";
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
							cs = dbLib.prepareCall(conn, "{ CALL SP_GOODS (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");

							for (int i = 1; i < sheet.getRows(); i++) {
								Cell code = sheet.getCell(0, i);
								Cell group = sheet.getCell(1, i);
								Cell name = sheet.getCell(2, i);
								Cell barcode = sheet.getCell(3, i);

								if (code == null || group == null || name == null) {
									continue;
								}

								try {
									cs.setString(1, this.cfg.get("server"));
									cs.setLong(2, 0);
									cs.setString(3, maker);
									cs.setString(4, group.getContents());
									cs.setLong(5, 0);
									cs.setLong(6, 0);
									cs.setString(7, code.getContents());
									cs.setString(8, name.getContents());
									cs.setString(9, barcode.getContents());
									cs.setString(10, "");
									cs.setLong(11, this.cfg.getLong("user.seq"));
									cs.registerOutParameter(12, OracleTypes.VARCHAR);
									cs.execute();

									if (cs.getString(12) != null && cs.getString(12).equals("Y")) {
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
			cs.setString(10, "상품 일괄 등록 완료");
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

	// 자판기 상품 체크
//20160219 INDEX 힌트 추가
//		long check = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_VENDING_MACHINE_GOODS WHERE GOODS_SEQ = " + seq));
		long check = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT /*+ INDEX(A) */"
								+ " COUNT(*)"
						+ " FROM TB_VENDING_MACHINE_GOODS A"
						+ " WHERE GOODS_SEQ = " + seq
					)
			);

		if (check > 0) {
			error = "삭제하고자 하는 상품을 판매하는 자판기가 존재합니다.";
		}

	// 판매 기록 체크
		if (StringEx.isEmpty(error)) {
//20160219 INDEX 힌트 추가
//			check = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_SALES WHERE GOODS_SEQ = " + seq));
			check = StringEx.str2long(
					dbLib.getResult(conn,
							"SELECT /*+ INDEX(A) */"
									+ " COUNT(*)"
								+ " FROM TB_SALES A"
								+ " WHERE GOODS_SEQ = " + seq
						)
				);

			if (check > 0) {
				error = "삭제하고자 하는 상품의 판매 기록이 존재합니다.";
			}
		}

	// 삭제
		if (StringEx.isEmpty(error)) {
			try {
//20160219 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "DELETE FROM TB_GOODS WHERE SEQ = ?");
				ps = dbLib.prepareStatement(conn,
						"DELETE /*+ INDEX(A) */"
							+ " FROM TB_GOODS"
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
}