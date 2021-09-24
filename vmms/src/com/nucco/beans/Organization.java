package com.nucco.beans;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
/**
 * Organization.java
 *
 * 조직
 *
 * 작성일 - 2011/03/25, 정원광
 *
 */
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.nucco.GlobalConfig;
import com.nucco.cfg.GeneralConfig;
import com.nucco.lib.StringEx;
import com.nucco.lib.db.DBLibrary;

import oracle.jdbc.OracleTypes;

public class Organization {
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
 * 조직 조회
 *
 */
	public GeneralConfig data;
/**
 *
 */
	public Organization(GlobalConfig cfg) throws Exception {
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
 * @param organ 조직
 * @param depth 깊이
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String detail(long company, long organ, int depth) throws Exception {
		System.out.println("wow : "+company+","+organ+","+depth);
	// 조직 정보가 없을 때
		if (organ < 0) {
			return this.detail(company, depth);
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

	// 검색절
		String WHERE = " COMPANY_SEQ = " + company
				+ " AND IS_ENABLED = 'Y'"
				+ " AND SORT = 1"
			;

		if (organ > 0) {
			WHERE += " AND PARENT_SEQ = " + organ;
		} else {
			WHERE += " AND PARENT_SEQ = 0";
		}

		if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND COMPANY_SEQ = " + this.cfg.getLong("user.company");
		}

		if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색
//20160212 INDEX 힌트 추가
//			WHERE += " AND SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE IS_ENABLED = 'Y' START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION START WITH SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY SEQ = PRIOR PARENT_SEQ)";
			WHERE += " AND SEQ IN ("
						+ " SELECT /*+ INDEX(A_A) */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION A_A"
							+ " WHERE IS_ENABLED = 'Y'"
							+ " START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ")
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " UNION"
						+ " SELECT /*+ INDEX(A_B) */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION A_B"
							+ " START WITH SEQ = " + this.cfg.getLong("user.organ")
							+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
					+ ")"
				;
		}

	// 목록
		this.organ = new ArrayList<GeneralConfig>();

		try {
//20160212 INDEX 힌트 변경, ORDER BY 추가
//			ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_ORGANIZATION) */ SEQ, NAME FROM TB_ORGANIZATION A WHERE " + WHERE);
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " SEQ,"
							+ " NAME"
						+ " FROM TB_ORGANIZATION A"
						+ " WHERE" + WHERE
						+ " ORDER BY NAME, SEQ"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
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

	// 명칭
		this.data = new GeneralConfig();

		if (StringEx.isEmpty(error)) {
			try {
//20160212 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "SELECT SEQ, NAME FROM TB_ORGANIZATION WHERE COMPANY_SEQ = " + company + " AND DEPTH = " + depth + " AND SORT = 0 AND ROWNUM = 1");
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " SEQ,"
								+ " NAME"
							+ " FROM TB_ORGANIZATION A"
							+ " WHERE COMPANY_SEQ = " + company
								+ " AND DEPTH = " + depth
								+ " AND SORT = 0"
								+ " AND ROWNUM = 1"
					);
				rs = ps.executeQuery();

				if (rs.next()) {
					this.data.put("SEQ", rs.getLong("SEQ"));
					this.data.put("NAME", rs.getString("NAME"));
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
 * 조회
 *
 * @param company 소속
 * @param depth 깊이
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String detail(long company, int depth) throws Exception {
		System.out.println(company+","+depth);
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

	// 명칭
		this.data = new GeneralConfig();

		try {
//20160212 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "SELECT SEQ, NAME FROM TB_ORGANIZATION WHERE COMPANY_SEQ = " + company + " AND DEPTH = " + depth + " AND SORT = 0 AND ROWNUM = 1");
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " SEQ,"
							+ " NAME"
						+ " FROM TB_ORGANIZATION A"
						+ " WHERE COMPANY_SEQ = " + company
							+ " AND DEPTH = " + depth
							+ " AND SORT = 0"
							+ " AND ROWNUM = 1"
				);
			rs = ps.executeQuery();

			if (rs.next()) {
				this.data.put("SEQ", rs.getLong("SEQ"));
				this.data.put("NAME", rs.getString("NAME"));
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
//20160212 INDEX 힌트 변경, CASE WHEN을 JAVA에서 처리, ORDER BY 추가
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

	// 조직 트리
		this.organ = new ArrayList<GeneralConfig>();

		if (StringEx.isEmpty(error) && this.cfg.getLong("user.organ") > 0) {
			try {
//20160212 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "SELECT SEQ, DEPTH, NAME FROM TB_ORGANIZATION WHERE IS_ENABLED = 'Y' START WITH SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY SEQ = PRIOR PARENT_SEQ ORDER BY DEPTH");
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " SEQ,"
								+ " DEPTH,"
								+ " NAME"
							+ " FROM TB_ORGANIZATION A"
							+ " WHERE IS_ENABLED = 'Y'"
							+ " START WITH SEQ = " + this.cfg.getLong("user.organ")
							+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
							+ " ORDER BY DEPTH"
					);
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
		}

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
/**
 * 등록
 *
 * @param seq 조직
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

	// 선택된 조직 정보
		long user_company = this.cfg.getLong("user.company");

		this.data = new GeneralConfig();

		try {
//20160212 INDEX 힌트 추가, CASE WHEN을 JAVA에서 처리, REVERSE 추가
//			ps = dbLib.prepareStatement(conn, "SELECT PARENT_SEQ, DEPTH, COMPANY_SEQ, (SELECT LTRIM(SYS_CONNECT_BY_PATH(NAME, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.PARENT_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS NAME FROM TB_ORGANIZATION A WHERE SEQ = ? AND (CASE WHEN ? > 0 THEN COMPANY_SEQ ELSE 1 END) = (CASE WHEN ? > 0 THEN ? ELSE 1 END) AND IS_ENABLED = 'Y'");
//			ps.setLong(1, seq);
//			ps.setLong(2, this.cfg.getLong("user.company"));
//			ps.setLong(3, this.cfg.getLong("user.company"));
//			ps.setLong(4, this.cfg.getLong("user.company"));
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " PARENT_SEQ,"
							+ " DEPTH,"
							+ " COMPANY_SEQ,"
							+ " ("
									+ " SELECT /*+ INDEX(B) */"
											+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
										+ " FROM TB_ORGANIZATION B"
										+ " WHERE PARENT_SEQ = 0"
										+ " START WITH SEQ = A.PARENT_SEQ"
										+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
								+ ") AS NAME"
						+ " FROM TB_ORGANIZATION A"
						+ " WHERE SEQ = ?"
					+ (user_company > 0
							? " AND COMPANY_SEQ = ?"
							: ""
						)
							+ " AND IS_ENABLED = 'Y'");
			ps.setLong(1, seq);
			if (user_company > 0) ps.setLong(2, user_company);
			rs = ps.executeQuery();

			if (rs.next()) {
				this.data.put("PARENT_SEQ", rs.getLong("PARENT_SEQ"));
				this.data.put("DEPTH", rs.getInt("DEPTH"));
				this.data.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				this.data.put("NAME", rs.getString("NAME"));
			} else {
				error = "등록되지 않았거나 수정할 권한이 없는 조직입니다.";
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 상위 조직
		long user_organ = this.cfg.getLong("user.organ");

		this.organ = new ArrayList<GeneralConfig>();

		if (StringEx.isEmpty(error)) {
			try {
//20160212 INDEX 힌트 추가, CASE WHEN을 JAVA에서 처리, REVERSE 적용
//				ps = dbLib.prepareStatement(conn, "SELECT SEQ, LTRIM(SYS_CONNECT_BY_PATH(NAME, '/'), '/') AS NAME FROM TB_ORGANIZATION WHERE COMPANY_SEQ = ? AND DEPTH = ? AND IS_ENABLED = 'Y' AND SORT = 1" + (this.cfg.getLong("user.organ") > 0 ? " AND SEQ IN (SELECT SEQ FROM TB_ORGANIZATION START WITH SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY SEQ = PRIOR PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ)" : "") + " START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ");
//				ps.setLong(1, this.data.getLong("COMPANY_SEQ"));
//				ps.setInt(2, this.data.getInt("DEPTH") - 1);
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " SEQ,"
								+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/')) AS NAME"
							+ " FROM TB_ORGANIZATION A"
							+ " WHERE PARENT_SEQ = 0"
							+ " START WITH COMPANY_SEQ = ?"
								+ " AND DEPTH = ?"
								+ " AND SORT = 1"
								+ " AND IS_ENABLED = 'Y'"
						+ (user_organ > 0
								? " AND SEQ IN ("
										+ " SELECT /*+ INDEX(A_A) */"
												+ " SEQ"
											+ " FROM TB_ORGANIZATION A_A"
											+ " START WITH SEQ = ?"
											+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
										+ " UNION"
										+ " SELECT /*+ INDEX(A_B) */"
												+ " SEQ"
											+ " FROM TB_ORGANIZATION A_B"
											+ " START WITH PARENT_SEQ = ?"
											+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
									+ " )"
								: ""
							)
							+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
					);
				ps.setLong(1, this.data.getLong("COMPANY_SEQ"));
				ps.setInt(2, this.data.getInt("DEPTH") - 1);
				if (user_organ > 0) {
					ps.setLong(3, user_organ);
					ps.setLong(4, user_organ);
				}
				rs = ps.executeQuery();

				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();

					c.put("SEQ", rs.getLong("SEQ"));
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
		}

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
/**
 * 등록
 *
 * @param company 소속
 * @param organ 상위번호
 * @param depth 깊이
 * @param sort 정렬 방법
 * @param name 이름
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(long company, long organ, int depth, int sort, String name) throws Exception {
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
		try {
//			ps = dbLib.prepareStatement(conn, "INSERT INTO TB_ORGANIZATION (SEQ, COMPANY_SEQ, PARENT_SEQ, DEPTH, SORT, NAME, CREATE_USER_SEQ, CREATE_DATE) VALUES (SQ_ORGANIZATION.NEXTVAL, ?, ?, ?, ?, ?, ?, SYSDATE)");
			ps = dbLib.prepareStatement(conn,
					"INSERT INTO TB_ORGANIZATION ("
							+ " SEQ,"
							+ " COMPANY_SEQ,"
							+ " PARENT_SEQ,"
							+ " DEPTH,"
							+ " SORT,"
							+ " NAME,"
							+ " CREATE_USER_SEQ,"
							+ " CREATE_DATE"
						+ " ) VALUES ("
							+ " SQ_ORGANIZATION.NEXTVAL,"
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
			ps.setInt(3, depth);
			ps.setInt(4, sort);
			ps.setString(5, name);
			ps.setLong(6, this.cfg.getLong("user.seq"));
			ps.executeUpdate();
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(ps);
		}

	// 등록번호
		String sequence = dbLib.getResult(conn, "SELECT SQ_ORGANIZATION.CURRVAL FROM DUAL");

	// 리소스 반환
		dbLib.close(conn);

		return StringEx.isEmpty(error) ? sequence : error;
	}
/**
 * 수정
 *
 * @param seq 등록번호
 * @param sort 정렬 방법
 * @param name 이름
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String modify(long seq, int sort, String name) throws Exception {
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
//20160212 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "UPDATE TB_ORGANIZATION SET NAME = ?, MODIFY_USER_SEQ = ?, MODIFY_DATE = SYSDATE WHERE SEQ = ?");
			ps = dbLib.prepareStatement(conn,
					"UPDATE /*+ INDEX(A) */ TB_ORGANIZATION A"
						+ " SET NAME = ?,"
							+ " MODIFY_USER_SEQ = ?,"
							+ " MODIFY_DATE = SYSDATE"
						+ " WHERE SEQ = ?"
				);
			ps.setString(1, name);
			ps.setLong(2, this.cfg.getLong("user.seq"));
			ps.setLong(3, seq);
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
 * 상위 조직 변경
 *
 * @param seq 등록번호
 * @param organ 상위 조직
 * @param sDate 변경일
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String modify(long seq, long organ, String sDate) throws Exception {
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

	// 수정
		try {
			cs = dbLib.prepareCall(conn, "{ CALL SP_JOB_ORGAN_PARENT_SEQ (?, ?, ?, ?, ?, ?) }");
			cs.setString(1, this.cfg.get("server"));
			cs.setLong(2, seq);
			cs.setLong(3, organ);
			cs.setString(4, sDate);
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

	// 정렬 유형
//20160212 INDEX 힌트 추가
//		int sort = StringEx.str2int(dbLib.getResult(conn, "SELECT SORT FROM TB_ORGANIZATION WHERE SEQ = " + seq), -1);
		int sort = StringEx.str2int(
				dbLib.getResult(conn,
						"SELECT /*+ INDEX(A) */"
								+ " SORT"
							+ " FROM TB_ORGANIZATION A"
							+ " WHERE SEQ = " + seq
						),
				-1
			);

	// 실행
		switch (sort) {
			case 0 : // 명칭
//20160212 INDEX 힌트 추가
//				long check = StringEx.str2int(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_ORGANIZATION WHERE PARENT_SEQ = " + seq));
				long check = StringEx.str2int(
						dbLib.getResult(conn,
								"SELECT /*+ INDEX(A) */"
										+ " COUNT(*)"
									+ " FROM TB_ORGANIZATION A"
									+ " WHERE PARENT_SEQ = " + seq
							)
					);

				if (check > 0) {
					error = "선택하신 명칭을 참조하는 하위 명칭이 존재합니다.";
				} else {
					try {
//20160212 INDEX 힌트 추가
//						ps = dbLib.prepareStatement(conn, "DELETE FROM TB_ORGANIZATION WHERE SEQ = ?");
						ps = dbLib.prepareStatement(conn,
								"DELETE /*+ INDEX(A) */"
									+ " FROM TB_ORGANIZATION A"
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

				break;
			case 1 : // 목록
				try {
//					ps = dbLib.prepareStatement(conn, "DELETE FROM TB_ORGANIZATION WHERE SEQ IN (SELECT SEQ FROM TB_ORGANIZATION START WITH PARENT_SEQ = ? CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = ?)");
					ps = dbLib.prepareStatement(conn,
							"DELETE /*+ INDEX(A) */"
								+ " FROM TB_ORGANIZATION A"
								+ " WHERE SEQ IN ("
										+ " SELECT /*+ INDEX(A_A) */"
												+ " SEQ"
											+ " FROM TB_ORGANIZATION A_A"
//20160219 UNION 통합(seq > 0)
//											+ " START WITH PARENT_SEQ = ?"
											+ " START WITH SEQ = ?"
											+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//										+ " UNION"
//										+ " SELECT /*+ INDEX(A_B) */"
//												+ " SEQ"
//											+ " FROM TB_ORGANIZATION A_B"
//											+ " WHERE SEQ = ?"
									+ ")"
						);
					ps.setLong(1, seq);
//					ps.setLong(2, seq);
					ps.executeUpdate();
				} catch (Exception e) {
					this.logger.error(e);
					error = e.getMessage();
				} finally {
					dbLib.close(ps);
				}

				break;
			default :
				error = "유효하지 않은 정렬 유형입니다.";
		}

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
}