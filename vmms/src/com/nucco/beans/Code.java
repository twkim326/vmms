package com.nucco.beans;

/**
 * Code.java
 *
 * 코드
 *
 * 작성일 - 2011/03/28, 정원광
 *
 */

import java.util.*;
import java.sql.*;
import com.nucco.*;
import com.nucco.cfg.*;
import com.nucco.lib.*;
import com.nucco.lib.db.DBLibrary;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Code {
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
 * 모코드
 *
 */
	public ArrayList<GeneralConfig> type;
/**
 * 자코드
 *
 */
	public ArrayList<GeneralConfig> code;
/**
 * 조회
 *
 */
	public GeneralConfig data;
/**
 *
 */
	public Code(GlobalConfig cfg) throws Exception {
	// set config
		this.cfg = cfg;

	// set log4j
		PropertyConfigurator.configure(cfg.get("config.log4j"));
		this.logger = Logger.getLogger(this.getClass());
	}
/**
 * 조회
 *
 * @param type 모코드
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String detail(String type) throws Exception {
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

	// 선택된 모코드 정보
		this.data = new GeneralConfig();

	// 모코드
		this.type = new ArrayList<GeneralConfig>();

		try {
//20160217 INDEX 힌트 변경, ORDER BY 추가
//			ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_CODE) */ TYPE, NAME, MEMO FROM TB_CODE A WHERE CODE = '000'");
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " TYPE,"
							+ " NAME,"
							+ " MEMO"
						+ " FROM TB_CODE A"
						+ " WHERE CODE = '000'"
						+ " ORDER BY NAME, TYPE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("TYPE", rs.getString("TYPE"));
				c.put("NAME", rs.getString("NAME"));

				this.type.add(c);

				if ((StringEx.isEmpty(type) && StringEx.isEmpty(this.data.get("TYPE"))) || (!StringEx.isEmpty(type) && type.equals(rs.getString("TYPE")))) {
					this.data.put("TYPE", rs.getString("TYPE"));
					this.data.put("NAME", rs.getString("NAME"));
					this.data.put("MEMO", rs.getString("MEMO"));
				}
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 자코드
		this.code = new ArrayList<GeneralConfig>();

		if (StringEx.isEmpty(error)) {
			try {
//20160217 INDEX 힌트 변경, ORDER BY 추가
//				ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_CODE) */ TYPE, CODE, NAME, MEMO FROM TB_CODE A WHERE TYPE = ? AND CODE NOT IN ('000')");
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " TYPE,"
								+ " CODE,"
								+ " NAME,"
								+ " MEMO"
							+ " FROM TB_CODE A"
							+ " WHERE TYPE = ?"
								+ " AND CODE  <> '000'"
							+ " ORDER BY CODE"
					);
				ps.setString(1, this.data.get("TYPE"));
				rs = ps.executeQuery();

				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();

					c.put("TYPE", rs.getString("TYPE"));
					c.put("CODE", rs.getString("CODE"));
					c.put("NAME", rs.getString("NAME"));
					c.put("MEMO", rs.getString("MEMO"));

					this.code.add(c);
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
 * @param type 모코드
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(String type) throws Exception {
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

	// 코드 정보
		this.data = new GeneralConfig();

		if (!StringEx.isEmpty(type)) {
			try {
//20160217 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "SELECT NAME FROM TB_CODE WHERE TYPE = ? AND CODE = ?");
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " NAME"
							+ " FROM TB_CODE A"
							+ " WHERE TYPE = ?"
								+ " AND CODE = ?"
					);
				ps.setString(1, type);
				ps.setString(2, "000");
				rs = ps.executeQuery();

				if (rs.next()) {
					this.data.put("NAME", rs.getString("NAME"));
				} else {
					error = "등록되지 않은 모코드입니다.";
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
 * @param type 모코드
 * @param code 자코드
 * @param name 코드명
 * @param memo 설명
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(String type, String code, String name, String memo) throws Exception {
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
//20160217
//			ps = dbLib.prepareStatement(conn, "INSERT INTO TB_CODE (TYPE, CODE, NAME, MEMO, CREATE_USER_SEQ, CREATE_DATE) VALUES (?, ?, ?, ?, ?, SYSDATE)");
			ps = dbLib.prepareStatement(conn,
					"INSERT INTO TB_CODE ("
							+ " TYPE,"
							+ " CODE,"
							+ " NAME,"
							+ " MEMO,"
							+ " CREATE_USER_SEQ,"
							+ " CREATE_DATE"
						+ " ) VALUES ("
							+ " ?,"
							+ " ?,"
							+ " ?,"
							+ " ?,"
							+ " ?,"
							+ " SYSDATE"
						+ " )"
				);
			ps.setString(1, type);
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
 * 수정
 *
 * @param type 모코드
 * @param code 자코드
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String update(String type, String code) throws Exception {
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

	// 코드 정보
		this.data = new GeneralConfig();

		try {
//20160217 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "SELECT TYPE, CODE, NAME, MEMO FROM TB_CODE WHERE TYPE = ? AND CODE = ?");
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " TYPE,"
							+ " CODE,"
							+ " NAME,"
							+ " MEMO"
						+ " FROM TB_CODE A"
						+ " WHERE TYPE = ?"
							+ " AND CODE = ?"
				);
			ps.setString(1, type);
			ps.setString(2, code);
			rs = ps.executeQuery();

			if (rs.next()) {
				this.data.put("TYPE", rs.getString("TYPE"));
				this.data.put("CODE", rs.getString("CODE"));
				this.data.put("NAME", rs.getString("NAME"));
				this.data.put("MEMO", rs.getString("MEMO"));
			} else {
				error = "등록되지 않은 코드입니다.";
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
 * 수정
 *
 * @param type 모코드
 * @param code 자코드
 * @param name 코드명
 * @param memo 설명
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String update(String type, String code, String name, String memo) throws Exception {
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
//20160217 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "UPDATE TB_CODE SET NAME = ?, MEMO = ?, MODIFY_USER_SEQ = ?, MODIFY_DATE = SYSDATE WHERE TYPE = ? AND CODE = ?");
			ps = dbLib.prepareStatement(conn,
					"UPDATE /*+ INDEX(A) */ TB_CODE A"
						+ " SET NAME = ?,"
							+ " MEMO = ?,"
							+ " MODIFY_USER_SEQ = ?,"
							+ " MODIFY_DATE = SYSDATE"
						+ " WHERE TYPE = ?"
						+ " AND CODE = ?"
				);
			ps.setString(1, name);
			ps.setString(2, memo);
			ps.setLong(3, this.cfg.getLong("user.seq"));
			ps.setString(4, type);
			ps.setString(5, code);
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
 * 삭제
 *
 * @param type 모코드
 * @param code 자코드
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String delete(String type, String code) throws Exception {
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
//20160217 INDEX 힌트 추가, CASE WHEN 제거
//			ps = dbLib.prepareStatement(conn, "DELETE FROM TB_CODE WHERE TYPE = ? AND (CASE WHEN ? = '000' THEN '1' ELSE CODE END) = (CASE WHEN ? = '000' THEN '1' ELSE ? END)");
//			ps.setString(1, type);
//			ps.setString(2, code);
//			ps.setString(3, code);
//			ps.setString(4, code);
			ps = dbLib.prepareStatement(conn,
					"DELETE /*+ INDEX(A) */"
						+ " FROM TB_CODE A"
						+ " WHERE TYPE = ?"
					+ (!"000".equals(code)
							? " AND CODE  = ?"
							: ""
						)
				);
			ps.setString(1, type);
			if (!"000".equals(code)) {
				ps.setString(2, code);
			}
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