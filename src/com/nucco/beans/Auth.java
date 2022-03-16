package com.nucco.beans;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Auth.java
 *
 * 권한
 *
 * 작성일 - 2011/03/24, 정원광
 *
 */
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.nucco.GlobalConfig;
import com.nucco.cfg.GeneralConfig;
import com.nucco.lib.StringEx;
import com.nucco.lib.db.DBLibrary;

import oracle.jdbc.OracleTypes;

public class Auth {
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
 * 소속 목록
 *
 */
	public ArrayList<GeneralConfig> company;
/**
 * 조직 목록
 *
 */
	public ArrayList<GeneralConfig> organ;
/**
 * 프로그램 목록
 *
 */
	public ArrayList<GeneralConfig> mProgram;
	public ArrayList<GeneralConfig> sProgram;
/**
 * 메뉴 목록
 *
 */
	public ArrayList<GeneralConfig> menu;
/**
 * 권한 목록
 *
 */
	public ArrayList<GeneralConfig> auth;
/**
 *
 */
	public Auth(GlobalConfig cfg) throws Exception {
	// set config
		this.cfg = cfg;

	// set log4j
		PropertyConfigurator.configure(cfg.get("config.log4j"));
		this.logger = Logger.getLogger(this.getClass());
	}
/**
 * 조회
 *
 * @param company 소속
 * @param auth 권한
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String detail(long company, long auth) throws Exception {
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		CallableStatement cs = null;
		ResultSet rs = null;
		String error = null;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}

	// 조직 타이틀
		long user_company = this.cfg.getLong("user.company");

		this.organ = new ArrayList<GeneralConfig>();

		if (company > 0) {
			try {
//20160212 INDEX 힌트 추가, CASE WHEN을 JAVA에서 처리
//				ps = dbLib.prepareStatement(conn, "SELECT SEQ, DEPTH, NAME FROM TB_ORGANIZATION WHERE COMPANY_SEQ = ? AND SORT = 0 AND (CASE WHEN ? > 0 THEN COMPANY_SEQ ELSE 1 END) = (CASE WHEN ? > 0 THEN ? ELSE 1 END) ORDER BY DEPTH");
//				ps.setLong(1, company);
//				ps.setLong(2, this.cfg.getLong("user.company"));
//				ps.setLong(3, this.cfg.getLong("user.company"));
//				ps.setLong(4, this.cfg.getLong("user.company"));
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " SEQ,"
								+ " DEPTH,"
								+ " NAME"
							+ " FROM TB_ORGANIZATION A"
							+ " WHERE COMPANY_SEQ = ?"
								+ " AND SORT = 0"
						+ (user_company > 0
								? " AND COMPANY_SEQ = ?"
								: ""
							)
							+ " ORDER BY DEPTH");
				ps.setLong(1, company);
				if (user_company > 0) ps.setLong(2, user_company);
				rs = ps.executeQuery();

				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();

					c.put("SEQ", rs.getLong("SEQ"));
					c.put("DEPTH", rs.getInt("DEPTH"));
					c.put("NAME", rs.getString("NAME"));

					this.organ.add(c);
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

	// 권한
		this.auth = new ArrayList<GeneralConfig>();

		if (company > 0) {
			try {
// 20160212 INDEX 힌트 추가, CASE WHEN을 JAVA에서 처리
//				ps = dbLib.prepareStatement(conn, "SELECT SEQ, DEPTH, NAME, IS_APP_ORGAN, CASE WHEN DEPTH = (SELECT MAX(DEPTH) FROM TB_ORGANIZATION WHERE COMPANY_SEQ = A.COMPANY_SEQ) THEN 'N' ELSE 'Y' END AS IS_ABLE_APP_ORGAN FROM TB_AUTH A WHERE COMPANY_SEQ = ? AND (CASE WHEN ? > 0 THEN COMPANY_SEQ ELSE 1 END) = (CASE WHEN ? > 0 THEN ? ELSE 1 END) ORDER BY DEPTH");
//				ps.setLong(1, company);
//				ps.setLong(2, this.cfg.getLong("user.company"));
//				ps.setLong(3, this.cfg.getLong("user.company"));
//				ps.setLong(4, this.cfg.getLong("user.company"));
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " SEQ,"
								+ " DEPTH,"
								+ " NAME,"
								+ " IS_APP_ORGAN,"
								+ " CASE WHEN DEPTH = ("
										+ " SELECT /*+ INDEX(B) */"
												+ " MAX(DEPTH)"
											+ " FROM TB_ORGANIZATION B"
											+ " WHERE COMPANY_SEQ = A.COMPANY_SEQ"
										+ ") THEN 'N'"
										+ " ELSE 'Y'"
									+ " END AS IS_ABLE_APP_ORGAN"
							+ " FROM TB_AUTH A"
							+ " WHERE COMPANY_SEQ = ?"
						+ (user_company > 0
								? " AND COMPANY_SEQ = ?"
								: ""
							)
							+ " ORDER BY DEPTH, SEQ"
					);
				ps.setLong(1, company);
				if (user_company > 0) ps.setLong(2, user_company);
				rs = ps.executeQuery();

				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();

					c.put("SEQ", rs.getLong("SEQ"));
					c.put("DEPTH", rs.getInt("DEPTH"));
					c.put("NAME", rs.getString("NAME"));
					c.put("IS_APP_ORGAN", rs.getString("IS_APP_ORGAN"));
					c.put("IS_ABLE_APP_ORGAN", rs.getString("IS_ABLE_APP_ORGAN"));

					this.auth.add(c);
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

	// 메뉴
		this.menu = new ArrayList<GeneralConfig>();

		if (auth > 0) {
			try {		
				cs = dbLib.prepareCall(conn, "{ CALL PG_AUTH.MENU (?, ?, ?, ?) }");
				cs.setLong(1, auth);
				cs.setString(2, "");
				cs.setString(3, "N");
				cs.registerOutParameter(4, OracleTypes.CURSOR);
				cs.execute();

				rs = (ResultSet) cs.getObject(4);

				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();

					c.put("SEQ", rs.getString("SEQ"));
					c.put("DEPTH", rs.getInt("DEPTH"));
					c.put("ENABLE_I", rs.getString("ENABLE_I"));
					c.put("ENABLE_U", rs.getString("ENABLE_U"));
					c.put("ENABLE_D", rs.getString("ENABLE_D"));
					c.put("ENABLE_S", rs.getString("ENABLE_S"));

					this.menu.add(c);
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
//20160212 INDEX 힌트 변경, CASE WHEN을 JAVA에서 처리, ORDER BY 추가
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

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}

	// 프로그램
		this.mProgram = new ArrayList<GeneralConfig>();
		this.sProgram = new ArrayList<GeneralConfig>();

		try {
//20160212 FULL 힌트 추가
//			ps = dbLib.prepareStatement(conn, "SELECT * FROM TB_PROGRAM WHERE IS_ONLY_SYS = 'N' ORDER BY DEPTH, PARENT_SEQ, SORT");
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ FULL(A) */"
							+ " *"			
						+ " FROM TB_PROGRAM A"
						+ " WHERE IS_ONLY_SYS = 'N'"
						+ " ORDER BY DEPTH, PARENT_SEQ, SORT"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getString("SEQ"));
				c.put("PARENT_SEQ", rs.getString("PARENT_SEQ"));
				c.put("DEPTH", rs.getInt("DEPTH"));
				c.put("NAME", rs.getString("NAME"));
				c.put("SUPPORT_EXEC", rs.getString("SUPPORT_EXEC"));

				if (rs.getInt("DEPTH") == 0) {
					this.mProgram.add(c);
				} else {
					this.sProgram.add(c);
				}
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}

	// 리소스 반환
		dbLib.close(conn);

		return null;
	}
/**
 * 메뉴 등록
 *
 * @param request javax.servlet.http.HttpServletRequest
 * @param company 소속
 * @param auth 권한
 * @param depth 깊이
 * @param isAppOrgan 매출조회 조직추가 여부
 * @param mcnt 메인 메뉴 갯수
 * @param scnt 서브 메뉴 갯수
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String setMenu(HttpServletRequest request, long company, long auth, int depth, String isAppOrgan, int mcnt, int scnt) throws Exception {
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

	// COMMIT 설정
		conn.setAutoCommit(false);

	// 노출 조직 등록
		try {
//20160215 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "UPDATE TB_AUTH SET DEPTH = ?, IS_APP_ORGAN = ? WHERE SEQ = ?");
			ps = dbLib.prepareStatement(conn,
					"UPDATE /*+ INDEX(A) */ TB_AUTH A"
						+ " SET DEPTH = ?,"
							+ " IS_APP_ORGAN = ?"
						+ " WHERE SEQ = ?"
				);
			ps.setInt(1, depth);
			ps.setString(2, isAppOrgan);
			ps.setLong(3, auth);
			ps.executeUpdate();
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(ps);
		}

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn, dbLib.ROLLBACK);
			return error;
		}

	// 기 등록된 내용 삭제
		try {
//20160215 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "DELETE FROM TB_MENU WHERE AUTH_SEQ = ?");
			ps = dbLib.prepareStatement(conn,
					"DELETE /*+ INDEX(A) */"
					+ " FROM TB_MENU A"
					+ " WHERE AUTH_SEQ = ?"
				);
			ps.setLong(1, auth);
			ps.executeUpdate();
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(ps);
		}

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn, dbLib.ROLLBACK);
			return error;
		}

	// 메인 메뉴 등록
		for (int i = 0; i < mcnt; i++) {
			String mseq = request.getParameter("mseq" + i);
			String muse = StringEx.setDefaultValue(request.getParameter("muse" + i), "N");

			if (muse.equals("N")) {
				continue;
			}

			try {
//20160215
//				ps = dbLib.prepareStatement(conn, "INSERT INTO TB_MENU (AUTH_SEQ, PROGRAM_SEQ) VALUES (?, ?)");
				ps = dbLib.prepareStatement(conn,
						"INSERT INTO TB_MENU ("
								+ " AUTH_SEQ,"
								+ " PROGRAM_SEQ"
							+ ") VALUES ("
								+ " ?,"
								+ " ?"
							+ ")"
					);
				ps.setLong(1, auth);
				ps.setString(2, mseq);
				ps.executeUpdate();
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(ps);
			}

			if (!StringEx.isEmpty(error)) {
				dbLib.close(conn, dbLib.ROLLBACK);
				return error;
			}
		}

	// 서브 메뉴 등록
		for (int i = 0; i < scnt; i++) {
			String sseq = request.getParameter("sseq" + i);
			String suse = StringEx.setDefaultValue(request.getParameter("suse" + i), "N");
			String S = StringEx.setDefaultValue(request.getParameter("S" + i), "N");
			String I = StringEx.setDefaultValue(request.getParameter("I" + i), "N");
			String U = StringEx.setDefaultValue(request.getParameter("U" + i), "N");
			String D = StringEx.setDefaultValue(request.getParameter("D" + i), "N");

			if (suse.equals("N")) {
				continue;
			}

			try {
//20160215
//				ps = dbLib.prepareStatement(conn, "INSERT INTO TB_MENU (AUTH_SEQ, PROGRAM_SEQ, ENABLE_I, ENABLE_U, ENABLE_D, ENABLE_S) VALUES (?, ?, ?, ?, ?, ?)");
				ps = dbLib.prepareStatement(conn,
						"INSERT INTO TB_MENU ("
								+ " AUTH_SEQ,"
								+ " PROGRAM_SEQ,"
								+ " ENABLE_I,"
								+ " ENABLE_U,"
								+ " ENABLE_D,"
								+ " ENABLE_S"
							+ " ) VALUES ("
								+ " ?,"
								+ " ?,"
								+ " ?,"
								+ " ?,"
								+ " ?,"
								+ " ?"
							+ ")"
					);
				ps.setLong(1, auth);
				ps.setString(2, sseq);
				ps.setString(3, I);
				ps.setString(4, U);
				ps.setString(5, D);
				ps.setString(6, S);
				ps.executeUpdate();
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(ps);
			}

			if (!StringEx.isEmpty(error)) {
				dbLib.close(conn, dbLib.ROLLBACK);
				return error;
			}
		}

	// 리소스 반환
		dbLib.close(conn, dbLib.COMMIT);

		return null;
	}
/**
 * 권한 등록
 *
 * @param seq 등록번호
 * @param company 소속
 * @param name 이름
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String setAuth(long seq, long company, String name) throws Exception {
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

	// 등록
		String sequence = "";

		try {
			if (seq > 0) {
//20160215 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "UPDATE TB_AUTH SET NAME = ? WHERE SEQ = ?");
				ps = dbLib.prepareStatement(conn,
						"UPDATE /*+ INDEX(A) */ TB_AUTH A"
							+ " SET NAME = ?"
							+ " WHERE SEQ = ?"
					);
				ps.setString(1, name);
				ps.setLong(2, seq);
				ps.executeUpdate();

				sequence = StringEx.long2str(seq);
			} else {
//20160215
//				ps = dbLib.prepareStatement(conn, "INSERT INTO TB_AUTH (SEQ, COMPANY_SEQ, DEPTH, NAME) VALUES (SQ_AUTH.NEXTVAL, ?, ?, ?)");
				ps = dbLib.prepareStatement(conn,
						"INSERT INTO TB_AUTH ("
								+ " SEQ,"
								+ " COMPANY_SEQ,"
								+ " DEPTH,"
								+ " NAME"
							+ " ) VALUES ("
								+ " SQ_AUTH.NEXTVAL,"
								+ " ?,"
								+ " ?,"
								+ " ?"
							+ ")"
					);
				ps.setLong(1, company);
				ps.setInt(2, -1);
				ps.setString(3, name);
				ps.executeUpdate();

				sequence = dbLib.getResult(conn, "SELECT SQ_AUTH.CURRVAL FROM DUAL");
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(ps);
		}

	// 리소스 반환
		dbLib.close(conn);

		return StringEx.isEmpty(error) ? sequence : error;
	}
/**
 * 권한 삭제
 *
 * @param seq 등록번호
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String setAuth(long seq) throws Exception {
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
		try {
//20160215 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "DELETE FROM TB_AUTH WHERE SEQ = ?");
			ps = dbLib.prepareStatement(conn,
					"DELETE /*+ INDEX(A) */"
							+ " FROM TB_AUTH A"
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

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
}