package com.nucco.beans;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Group.java
 *
 * 상품 그룹
 *
 * 작성일 - 2011/03/30, 정원광
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

public class Group {
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
 * 분류
 *
 */
	public ArrayList<GeneralConfig> cate;
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
	public Group(GlobalConfig cfg) throws Exception {
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
 * @param cate 분류
 * @param pageNo 페이지
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getList(long company, long cate, int pageNo, String sField, String sQuery) throws Exception {
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
//20160218 INDEX 힌트 변경, CASE WHEN 제거, ORDER BY 추가
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

	// 검색절 생성
		String WHERE = " A.DEPTH = 1";

		if (user_company > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND A.COMPANY_SEQ = " + user_company;
		}

		if (company > 0) {
			WHERE += " AND A.COMPANY_SEQ = " + company;
		}

		if (cate > 0) {
			WHERE += " AND A.PARENT_SEQ = " + cate;
		}

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
		}

	// 총 레코드수
//20160218 INDEX 힌트 추가
//		this.records = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_GROUP A WHERE " + WHERE));
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT /*+ INDEX(A) */"
								+ " COUNT(*)"
							+ " FROM TB_GROUP A"
							+ " WHERE" + WHERE
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
//20160218 INDEX 힌트 변경, ORDER BY 추가
//			ps = dbLib.prepareStatement(conn, "SELECT S2.*"
//					+ " FROM"
//					+ " ("
//					+ " SELECT ROWNUM AS RNUM, S1.*"
//					+ " FROM"
//					+ " ("
//					+ " SELECT /*+ ORDERED USE_NL(B C) INDEX_DESC(A PK_GROUP) */ A.SEQ, A.CODE, A.NAME, A.MEMO, TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE, TO_CHAR(A.MODIFY_DATE, 'YYYY-MM-DD') AS MODIFY_DATE, B.NAME AS CATE, C.NAME AS COMPANY"
//					+ " FROM TB_GROUP A LEFT JOIN TB_GROUP B ON A.PARENT_SEQ = B.SEQ LEFT JOIN TB_COMPANY C ON A.COMPANY_SEQ = C.SEQ"
//					+ " WHERE " + WHERE
//					+ " ) S1"
//					+ " WHERE ROWNUM <= " + e
//					+ " ) S2"
//					+ " WHERE RNUM >= " + s);
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED USE_NL(B C) */"
							+ " AAA.SEQ,"
							+ " AAA.CODE,"
							+ " AAA.NAME,"
							+ " AAA.MEMO,"
							+ " TO_CHAR(AAA.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE,"
							+ " TO_CHAR(AAA.MODIFY_DATE, 'YYYY-MM-DD') AS MODIFY_DATE,"
							+ " B.NAME AS CATE,"
							+ " C.NAME AS COMPANY"
						+ " FROM ("
									+ " SELECT"
											+ " ROWNUM AS ROW_NUM, AA.*"
										+ " FROM ("
												+ " SELECT /*+ INDEX(A) */"
														+ " *"
													+ " FROM TB_GROUP A"
													+ " WHERE" + WHERE
													+ " ORDER BY NAME"
											+ " ) AA"
										+ " WHERE ROWNUM <= " + e
								+ " ) AAA"
							+ " LEFT JOIN TB_GROUP B"
								+ " ON AAA.PARENT_SEQ = B.SEQ"
							+ " LEFT JOIN TB_COMPANY C"
								+ " ON AAA.COMPANY_SEQ = C.SEQ"
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
				c.put("CATE", rs.getString("CATE"));
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
 * 조회
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

	// 소속
		long user_company = this.cfg.getLong("user.company");

		this.company = new ArrayList<GeneralConfig>();

		try {
//20160218 INDEX 힌트 변경, CASE WHEN 제거, ORDER BY 추가
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

	// 조회
		if (StringEx.isEmpty(error)) {
			this.data = new GeneralConfig();

			try {
//20160218 INDEX 힌트 추가, CASE WHEN 제거
//				ps = dbLib.prepareStatement(conn, "SELECT * FROM TB_GROUP WHERE SEQ = ? AND (CASE WHEN ? > 0 THEN COMPANY_SEQ ELSE 1 END) = (CASE WHEN ? > 0 THEN ? ELSE 1 END)");
//				ps.setLong(1, seq);
//				ps.setLong(2, this.cfg.getLong("user.company"));
//				ps.setLong(3, this.cfg.getLong("user.company"));
//				ps.setLong(4, this.cfg.getLong("user.company"));
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " *"
							+ " FROM TB_GROUP A"
							+ " WHERE SEQ = ?"
						+ (user_company > 0
								? " AND COMPANY_SEQ = ?"
								: ""
							)
					);
				ps.setLong(1, seq);
				if (user_company > 0) ps.setLong(2, user_company);
				rs = ps.executeQuery();

				if (rs.next()) {
					this.data.put("SEQ", rs.getLong("SEQ"));
					this.data.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
					this.data.put("PARENT_SEQ", rs.getLong("PARENT_SEQ"));
					this.data.put("DEPTH", rs.getInt("DEPTH"));
					this.data.put("CODE", rs.getString("CODE"));
					this.data.put("NAME", rs.getString("NAME"));
					this.data.put("MEMO", rs.getString("MEMO"));
				} else {
					error = "등록되지 않은 상품 그룹입니다.";
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
 * 분류
 *
 * @param company 소속
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String cate(long company) throws Exception {
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

	// 목록
		long user_company = this.cfg.getLong("user.company");

		this.cate = new ArrayList<GeneralConfig>();

		try {
//20160218 INDEX 힌트 변경, CASE WHEN 제거, ORDER BY 추가
//			ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX_DESC(A PK_GROUP) */ SEQ, CODE, NAME FROM TB_GROUP A WHERE DEPTH = 0 AND CODE NOT IN ('X0000001', 'X0000002') AND COMPANY_SEQ = ? AND (CASE WHEN ? > 0 THEN COMPANY_SEQ ELSE 1 END) = (CASE WHEN ? > 0 THEN ? ELSE 1 END)");
//			ps.setLong(1, company);
//			ps.setLong(2, this.cfg.getLong("user.company"));
//			ps.setLong(3, this.cfg.getLong("user.company"));
//			ps.setLong(4, this.cfg.getLong("user.company"));
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " SEQ,"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_GROUP A"
						+ " WHERE DEPTH = 0"
							+ " AND CODE NOT IN ('X0000001', 'X0000002')"
							+ " AND COMPANY_SEQ = ?"
					+ (user_company > 0
							? " AND COMPANY_SEQ = ?"
							: ""
						)
						+ " ORDER BY NAME"
				);
			ps.setLong(1, company);
			if (user_company > 0) ps.setLong(2, user_company);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("CODE", rs.getString("CODE"));
				c.put("NAME", rs.getString("NAME"));

				this.cate.add(c);
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
 * @param company 소속
 * @param depth 깊이
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(long company, int depth) throws Exception {
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

	// 소속 목록 :: 그룹 (DEPTH = 1) 등록 시 사용
		long user_company = this.cfg.getLong("user.company");

		this.company = new ArrayList<GeneralConfig>();

		if (company == 0 && depth > 0) {
			try {
//20160218 INDEX 힌트 변경, CASE WHEN 제거
//				ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_COMPANY) */ SEQ, NAME FROM TB_COMPANY A WHERE (CASE WHEN ? > 0 THEN SEQ ELSE 1 END) = (CASE WHEN ? > 0 THEN ? ELSE 1 END)");
//				ps.setLong(1, this.cfg.getLong("user.company"));
//				ps.setLong(2, this.cfg.getLong("user.company"));
//				ps.setLong(3, this.cfg.getLong("user.company"));
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " SEQ,"
								+ " NAME"
							+ " FROM TB_COMPANY A"
					+ (this.cfg.getLong("user.company") > 0
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

	// 선택된 소속 :: 분류 등록 시 사용
		this.data = new GeneralConfig();

		if (company > 0 && depth == 0) {
//20160218 INDEX 힌트 추가
//			this.data.put("COMPANY", dbLib.getResult(conn, "SELECT NAME FROM TB_COMPANY WHERE SEQ = " + company));
			this.data.put("COMPANY",
					dbLib.getResult(conn,
							"SELECT /*+ INDEX(A) */"
									+ " NAME"
								+ " FROM TB_COMPANY A"
								+ " WHERE SEQ = " + company
						)
				);
		}

	// 리소스 반환
		dbLib.close(conn);

		return null;
	}
/**
 * 등록 :: DEPTH = 0
 *
 * @param company 소속
 * @param name 이름
 * @param memo 설명
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(long company, String name, String memo) throws Exception {
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

	// 코드
//20160218 INDEX 힌트 추가
//		String code = dbLib.getResult(conn, "SELECT 'A' || LPAD(TO_CHAR(NVL(MAX(TO_NUMBER(SUBSTR(CODE, 2, 7))), 0) + 1), 7, '0') FROM TB_GROUP WHERE DEPTH = 0");
		String code = dbLib.getResult(conn,
				"SELECT /*+ INDEX(A) */"
						+ " 'A' || LPAD(TO_CHAR(NVL(MAX(TO_NUMBER(SUBSTR(CODE, 2, 7))), 0) + 1), 7, '0')"
					+ " FROM TB_GROUP"
					+ " WHERE DEPTH = 0"
			);

	// 등록
		try {
//20160218
//			ps = dbLib.prepareStatement(conn, "INSERT INTO TB_GROUP (SEQ, COMPANY_SEQ, PARENT_SEQ, DEPTH, CODE, NAME, MEMO, CREATE_USER_SEQ, CREATE_DATE) VALUES (SQ_GROUP.NEXTVAL, ?, 0, 0, ?, ?, ?, ?, SYSDATE)");
			ps = dbLib.prepareStatement(conn,
					"INSERT INTO TB_GROUP ("
							+ " SEQ,"
							+ " COMPANY_SEQ,"
							+ " PARENT_SEQ,"
							+ " DEPTH,"
							+ " CODE,"
							+ " NAME,"
							+ " MEMO,"
							+ " CREATE_USER_SEQ,"
							+ " CREATE_DATE"
						+ " ) VALUES ("
							+ " SQ_GROUP.NEXTVAL,"
							+ " ?,"
							+ " 0,"
							+ " 0,"
							+ " ?,"
							+ " ?,"
							+ " ?,"
							+ " ?,"
							+ " SYSDATE"
						+ " )"
				);
			ps.setLong(1, company);
			ps.setString(2, code);
			ps.setString(3, name);
			ps.setString(4, memo);
			ps.setLong(5, this.cfg.getLong("user.seq"));
			ps.executeUpdate();
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
 * 등록 :: DEPTH = 1
 *
 * @param cate 분류
 * @param code 코드
 * @param name 이름
 * @param memo 설명
 * @param isAuto 코드 자동생성 여부
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(String cate, String code, String name, String memo, boolean isAuto) throws Exception {
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
//20160218 INDEX 힌트 추가
//			code = dbLib.getResult(conn, "SELECT LPAD(TO_CHAR(NVL(MAX(TO_NUMBER(CODE)), 0) + 1), 8, '0') FROM TB_GROUP WHERE PARENT_SEQ = (SELECT SEQ FROM TB_GROUP WHERE PARENT_SEQ = 0 AND CODE = '" + cate + "')");
			code = dbLib.getResult(conn,
					"SELECT /*+ INDEX(A) */"
							+ " LPAD(TO_CHAR(NVL(MAX(TO_NUMBER(CODE)), 0) + 1), 8, '0')"
						+ " FROM TB_GROUP A"
						+ " WHERE PARENT_SEQ = ("
								+ " SELECT /*+ INDEX(B) */"
									+ " SEQ"
									+ " FROM TB_GROUP B"
									+ " WHERE PARENT_SEQ = 0"
										+ " AND CODE = '" + cate + "'"
							+ " )"
				);
		}

	// 등록
		try {
			cs = dbLib.prepareCall(conn, "{ CALL SP_GROUP (?, ?, ?, ?, ?, ?, ?) }");
			cs.setString(1, this.cfg.get("server"));
			cs.setString(2, cate);
			cs.setString(3, code);
			cs.setString(4, name);
			cs.setString(5, memo);
			cs.setLong(6, this.cfg.getLong("user.seq"));
			cs.registerOutParameter(7, OracleTypes.VARCHAR);
			cs.execute();

			if (cs.getString(7) != null && cs.getString(7).equals("Y")) {
				error = "예기치 않은 오류가 발생하여 그룹을 등록하는데 실패하였습니다.";
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
 * 수정 :: DEPTH = 0
 *
 * @param seq 등록번호
 * @param company 소속
 * @param name 이름
 * @param memo 설명
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String modify(long seq, long company, String name, String memo) throws Exception {
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

	// 수정
		try {
//20160218 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "UPDATE TB_GROUP SET COMPANY_SEQ = ?, NAME = ?, MEMO = ?, MODIFY_USER_SEQ = ?, MODIFY_DATE = SYSDATE WHERE SEQ = ?");
			ps = dbLib.prepareStatement(conn,
					"UPDATE /*+ INDEX(A) */ TB_GROUP A"
						+ " SET COMPANY_SEQ = ?,"
							+ " NAME = ?,"
							+ " MEMO = ?,"
							+ " MODIFY_USER_SEQ = ?,"
							+ " MODIFY_DATE = SYSDATE"
						+ " WHERE SEQ = ?"
				);
			ps.setLong(1, company);
			ps.setString(2, name);
			ps.setString(3, memo);
			ps.setLong(4, this.cfg.getLong("user.seq"));
			ps.setLong(5, seq);
			ps.executeUpdate();
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(ps);
		}

	// 하위 그룹들의 소속 변경
		if (StringEx.isEmpty(error)) {
			try {
//20160218 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "UPDATE TB_GROUP SET COMPANY_SEQ = ? WHERE PARENT_SEQ = ?");
				ps = dbLib.prepareStatement(conn,
						"UPDATE /*+ INDEX(A) */ TB_GROUP A"
							+ " SET COMPANY_SEQ = ?"
							+ " WHERE PARENT_SEQ = ?"
					);
				ps.setLong(1, company);
				ps.setLong(2, seq);
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
 * 일괄등록
 *
 * @param cate 분류
 * @param excel 엑셀
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(String cate, File excel) throws Exception {
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
		if (StringEx.isEmpty(cate)) {
			error = "상위 그룹이 존재하지 않습니다.";
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
							cs = dbLib.prepareCall(conn, "{ CALL SP_GROUP (?, ?, ?, ?, ?, ?, ?) }");

							for (int i = 1; i < sheet.getRows(); i++) {
								Cell code = sheet.getCell(0, i);
								Cell name = sheet.getCell(1, i);

								if (code == null || name == null) {
									continue;
								}

								try {
									cs.setString(1, this.cfg.get("server"));
									cs.setString(2, cate);
									cs.setString(3, code.getContents());
									cs.setString(4, name.getContents());
									cs.setString(5, "");
									cs.setLong(6, this.cfg.getLong("user.seq"));
									cs.registerOutParameter(7, OracleTypes.VARCHAR);
									cs.execute();

									if (cs.getString(7) != null && cs.getString(7).equals("Y")) {
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
			cs.setString(10, "그룹 일괄 등록 완료");
			cs.execute();
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(cs);
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

	// 자신을 상위로 물고 있는지 체크
//20160218 INDEX 힌트 추가
//		long check = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_GROUP WHERE PARENT_SEQ = " + seq));
		long check = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT /*+ INDEX(A) */"
								+ " COUNT(*)"
							+ " FROM TB_GROUP A"
							+ " WHERE PARENT_SEQ = " + seq
					)
			);

		if (check > 0) {
			error = "삭제하고자 하는 그룹에 등록된 하위 그룹이 존재합니다.";
		}

	// 자신을 그룹으로 지정하는 상품이 있는지 체크
		if (StringEx.isEmpty(error)) {
//20160218 INDEX 힌트 추가
//			check = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_GOODS WHERE GROUP_SEQ = " + seq));
			check = StringEx.str2long(
					dbLib.getResult(conn,
							"SELECT /*+ INDEX(A) */"
									+ " COUNT(*)"
								+ " FROM TB_GOODS A"
								+ " WHERE GROUP_SEQ = " + seq
						)
				);

			if (check > 0) {
				error = "삭제하고자 하는 그룹에 등록된 상품이 존재합니다.";
			}
		}

	// 삭제
		if (StringEx.isEmpty(error)) {
			try {
//20160218 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "DELETE FROM TB_GROUP WHERE SEQ = ?");
				ps = dbLib.prepareStatement(conn,
						"DELETE /*+ INDEX(A) */"
							+ " FROM TB_GROUP A"
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