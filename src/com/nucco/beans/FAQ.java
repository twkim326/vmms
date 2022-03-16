package com.nucco.beans;

/**
 * FAQ.java
 *
 * FAQ
 *
 * 작성일 - 2011/04/03, 정원광
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

public class FAQ {
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
	public FAQ(GlobalConfig cfg) throws Exception {
	// set config
		this.cfg = cfg;

	// set log4j
		PropertyConfigurator.configure(cfg.get("config.log4j"));
		this.logger = Logger.getLogger(this.getClass());
	}
/**
 * 목록
 *
 * @param cate 분류
 * @param pageNo 페이지
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getList(String cate, int pageNo, String sField, String sQuery) throws Exception {
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

	// 분류
		this.cate = new ArrayList<GeneralConfig>();

		try {
//20160220 INDEX 힌트 변경, ORDER BY 추가
//			ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_CODE) */ CODE, NAME FROM TB_CODE A WHERE TYPE = 'FAQ' AND CODE NOT IN ('000')");
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE A"
						+ " WHERE TYPE = 'FAQ'"
							+ " AND CODE  <> '000'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

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

		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}

	// 검색절 생성
		String HINT = "";
		String WHERE = "";

		if (!StringEx.isEmpty(cate)) {
			HINT = " /*+ INDEX(A) */";
			WHERE += " AND A.CATEGORY = '" + cate + "'";
		}

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

	// 총 레코드수
//		this.records = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_FAQ A WHERE " + WHERE));
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT" + HINT
								+ " COUNT(*)"
							+ " FROM TB_FAQ A"
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
//20160220 INDEX 힌트 변경, ORDER BY 추가
//			ps = dbLib.prepareStatement(conn, "SELECT S2.*"
//				+ " FROM"
//				+ " ("
//				+ " SELECT ROWNUM AS RNUM, S1.*"
//				+ " FROM"
//				+ " ("
//				+ " SELECT /*+ ORDERED USE_NL(B) INDEX_DESC(A PK_FAQ) */ A.SEQ, A.TITLE, A.READS, TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE, B.NAME AS CATEGORY"
//				+ " FROM TB_FAQ A LEFT JOIN TB_CODE B ON (A.CATEGORY = B.CODE AND B.TYPE = 'FAQ')"
//				+ " WHERE " + WHERE
//				+ " ) S1"
//				+ " WHERE ROWNUM <= " + e
//				+ " ) S2"
//				+ " WHERE RNUM >= " + s);
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED USE_NL(B) */"
							+ " AAA.SEQ,"
							+ " AAA.TITLE,"
							+ " AAA.READS,"
							+ " TO_CHAR(AAA.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE,"
							+ " B.NAME AS CATEGORY"
						+ " FROM ("
									+ " SELECT"
											+ " ROWNUM AS ROW_NUM,"
											+ " AA.*"
										+ " FROM ("
												+ " SELECT" + HINT
														+ " *"
													+ " FROM TB_FAQ A"
													+ WHERE
													+ " ORDER BY SEQ DESC"
											+ " ) AA"
										+ " WHERE ROWNUM <= " + e
								+ " ) AAA"
							+ " LEFT JOIN TB_CODE B"
								+ " ON AAA.CATEGORY = B.CODE"
									+ " AND B.TYPE = 'FAQ'"
						+ " WHERE ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("TITLE", rs.getString("TITLE"));
				c.put("READS", rs.getInt("READS"));
				c.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				c.put("CATEGORY", rs.getString("CATEGORY"));
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
	public String detail(long seq, String cate, String sField, String sQuery) throws Exception {
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

	// 등록된 내용
		this.data = new GeneralConfig();

		try {
//20160220 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "SELECT A.TITLE, A.DETAIL, A.READS, TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE, B.NAME AS CATEGORY FROM TB_FAQ A LEFT JOIN TB_CODE B ON (A.CATEGORY = B.CODE AND B.TYPE = 'FAQ') WHERE A.SEQ = ?");
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED INDEX(A) USE_NL(B) */"
							+ " A.TITLE,"
							+ " A.DETAIL,"
							+ " A.READS,"
							+ " TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE,"
							+ " B.NAME AS CATEGORY"
						+ " FROM TB_FAQ A"
							+ " LEFT JOIN TB_CODE B"
								+ " ON A.CATEGORY = B.CODE"
									+ " AND B.TYPE = 'FAQ'"
						+ " WHERE A.SEQ = ?"
				);
			ps.setLong(1, seq);
			rs = ps.executeQuery();

			if (rs.next()) {
				this.data.put("TITLE", rs.getString("TITLE"));
				this.data.put("DETAIL", dbLib.getClob(rs, "DETAIL"));
				this.data.put("READS", rs.getInt("READS"));
				this.data.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				this.data.put("CATEGORY", rs.getString("CATEGORY"));
			} else {
				error = "등록되지 않은 내용입니다.";
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

	// 검색절
		String WHERE = "";

		if (!StringEx.isEmpty(cate)) {
			WHERE += " AND CATEGORY = '" + cate + "'";
		}

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
		}

	// 이전글
		try {
//20160220 쿼리변경
//			ps = dbLib.prepareStatement(conn, "SELECT * FROM (SELECT /*+ INDEX_DESC(A PK_FAQ) */ SEQ, TITLE FROM TB_FAQ A WHERE SEQ < ? " + WHERE + ") S1 WHERE ROWNUM = 1");
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " *"
						+ " FROM TB_FAQ A"
						+ " WHERE SEQ = ("
								+ " SELECT /*+ INDEX(A_A) */"
										+ " MAX(SEQ)"
									+ " FROM TB_FAQ A_A"
									+ " WHERE SEQ < ?"
										+ WHERE
							+ ")"
				);
			ps.setLong(1, seq);
			rs = ps.executeQuery();

			if (rs.next()) {
				this.data.put("PREV.SEQ", rs.getLong("SEQ"));
				this.data.put("PREV.TITLE", rs.getString("TITLE"));
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

	// 다음글
		try {
//20160220 쿼리변경
//			ps = dbLib.prepareStatement(conn, "SELECT * FROM (SELECT /*+ INDEX(A PK_FAQ) */ SEQ, TITLE FROM TB_FAQ A WHERE SEQ > ? " + WHERE + ") S1 WHERE ROWNUM = 1");
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " *"
						+ " FROM TB_FAQ A"
						+ " WHERE SEQ = ("
								+ " SELECT /*+ INDEX(A_A) */"
										+ " MIN(SEQ)"
									+ " FROM TB_FAQ A_A"
									+ " WHERE SEQ > ? "
										+ WHERE
							+ ")"
				);
			ps.setLong(1, seq);
			rs = ps.executeQuery();

			if (rs.next()) {
				this.data.put("NEXT.SEQ", rs.getLong("SEQ"));
				this.data.put("NEXT.TITLE", rs.getString("TITLE"));
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

	// 조회수
		try {
//20160220 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "UPDATE TB_FAQ SET READS = READS + 1 WHERE SEQ = ?");
			ps = dbLib.prepareStatement(conn,
					"UPDATE /*+ INDEX(A) */ TB_FAQ A"
						+ " SET READS = READS + 1"
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

		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}

	// 리소스 반환
		dbLib.close(conn);

		return null;
	}
/**
 * 등록/수정
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

	// 분류
		this.cate = new ArrayList<GeneralConfig>();

		try {
//20160220 INDEX 힌트 변경, ORDER BY 추가
//			ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_CODE) */ CODE, NAME FROM TB_CODE A WHERE TYPE = 'FAQ' AND CODE NOT IN ('000')");
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE A"
						+ " WHERE TYPE = 'FAQ' AND CODE <> '000'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

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

		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}

	// 등록된 내용
		this.data = new GeneralConfig();

		if (StringEx.isEmpty(error) && seq > 0) {
			try {
//20160220 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "SELECT CATEGORY, TITLE, DETAIL FROM TB_FAQ WHERE SEQ = ?");
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " CATEGORY,"
								+ " TITLE,"
								+ " DETAIL"
							+ " FROM TB_FAQ A"
							+ " WHERE SEQ = ?"
						);
				ps.setLong(1, seq);
				rs = ps.executeQuery();

				if (rs.next()) {
					this.data.put("CATEGORY", rs.getString("CATEGORY"));
					this.data.put("TITLE", rs.getString("TITLE"));
					this.data.put("DETAIL", dbLib.getClob(rs, "DETAIL"));
				} else {
					error = "등록되지 않은 내용입니다.";
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
 * 등록/수정
 *
 * @param seq 등록번호
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(long seq, String cate, String title, String detail) throws Exception {
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		String error = null;
		long sequence = seq;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}

	// COMMIT 설정
		conn.setAutoCommit(false);

	// 등록
		try {
			if (seq > 0) {
//20160220 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "UPDATE TB_FAQ SET CATEGORY = ?, TITLE = ?, DETAIL = EMPTY_CLOB(), MODIFY_USER_SEQ = ?, MODIFY_DATE = SYSDATE WHERE SEQ = ?");
				ps = dbLib.prepareStatement(conn,
						"UPDATE /*+ INDEX(A) */ TB_FAQ A"
							+ " SET CATEGORY = ?,"
								+ " TITLE = ?,"
								+ " DETAIL = EMPTY_CLOB(),"
								+ " MODIFY_USER_SEQ = ?,"
								+ " MODIFY_DATE = SYSDATE"
							+ " WHERE SEQ = ?"
					);
				ps.setString(1, cate);
				ps.setString(2, title);
				ps.setLong(3, this.cfg.getLong("user.seq"));
				ps.setLong(4, seq);
				ps.executeUpdate();
			} else {
//20160220
//				ps = dbLib.prepareStatement(conn, "INSERT INTO TB_FAQ (SEQ, CATEGORY, TITLE, DETAIL, READS, CREATE_USER_SEQ, CREATE_DATE) VALUES(SQ_FAQ.NEXTVAL, ?, ?, EMPTY_CLOB(), 0, ?, SYSDATE)");
				sequence = StringEx.str2long(dbLib.getResult(conn, "SELECT SQ_FAQ.NEXTVAL FROM DUAL"));
				ps = dbLib.prepareStatement(conn,
						"INSERT INTO TB_FAQ ("
								+ " SEQ,"
								+ " CATEGORY,"
								+ " TITLE,"
								+ " DETAIL,"
								+ " READS,"
								+ " CREATE_USER_SEQ,"
								+ " CREATE_DATE"
							+ " ) VALUES("
								+ " ?,"
								+ " ?,"
								+ " ?,"
								+ " EMPTY_CLOB(),"
								+ " 0,"
								+ " ?,"
								+ " SYSDATE"
							+ " )"
					);
				ps.setLong(1, sequence);
				ps.setString(2, cate);
				ps.setString(3, title);
				ps.setLong(4, this.cfg.getLong("user.seq"));
				ps.executeUpdate();
			}

			dbLib.setClob(conn, "TB_FAQ", "DETAIL", "SEQ = " + sequence, detail);
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

	// 리소스 반환
		dbLib.close(conn, dbLib.COMMIT);

		return StringEx.long2str(sequence);
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

	// 삭제
		try {
//20160220 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "DELETE FROM TB_FAQ WHERE SEQ = ?");
			ps = dbLib.prepareStatement(conn,
					"DELETE /*+ INDEX(A) */"
						+ " FROM TB_FAQ A"
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