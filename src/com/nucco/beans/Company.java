package com.nucco.beans;

/**
 * Company.java
 *
 * 소속
 *
 * 작성일 - 2011/03/24, 정원광
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

public class Company {
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
	public ArrayList<GeneralConfig> list;
/**
 * 그룹 목록
 *
 */
	public ArrayList<GeneralConfig> group;
/**
 * 소속 조회
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
	public Company(GlobalConfig cfg) throws Exception {
	// set config
		this.cfg = cfg;

	// set log4j
		PropertyConfigurator.configure(cfg.get("config.log4j"));
		this.logger = Logger.getLogger(this.getClass());
	}
/**
 * 목록
 *
 * @param pageNo 페이지
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getList(int pageNo, String sField, String sQuery) throws Exception {
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

		if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			HINT = " /*+ INDEX(A) */";
			WHERE += " AND COMPANY_SEQ = " + this.cfg.getLong("user.company");
		}

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

	// 총 레코드수
//20160212 INDEX 힌트 추가
//		this.records = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_COMPANY A WHERE " + WHERE));
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT" + HINT
								+ " COUNT(*)"
							+ " FROM TB_COMPANY A"
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

//20160212 FULL 힌트 추가
//			ps = dbLib.prepareStatement(conn, "SELECT S2.*"
//				+ " FROM"
//				+ " ("
//				+ " SELECT ROWNUM AS RNUM, S1.*"
//				+ " FROM"
//				+ " ("
//				+ " SELECT /*+ INDEX_DESC(A PK_COMPANY) */ A.SEQ, A.NAME, A.MEMO, TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE, TO_CHAR(A.MODIFY_DATE, 'YYYY-MM-DD') AS MODIFY_DATE"
//				+ " FROM TB_COMPANY A"
//				+ " WHERE " + WHERE
//				+ " ) S1"
//				+ " WHERE ROWNUM <= " + e
//				+ " ) S2"
//				+ " WHERE RNUM >= " + s);
			ps = dbLib.prepareStatement(conn,
					"SELECT *"
						+ " FROM ("
								+ " SELECT"
										+ " ROWNUM AS ROW_NUM,"
										+ " AA.*"
									+ " FROM ("
											+ " SELECT" + HINT
													+ " A.SEQ,"
													+ " A.NAME,"
													+ " A.MEMO,"
													+ " A.IS_USED_CLOSING,"
													+ " A.IS_VIEW_CLOSING,"
													+ " A.IS_RAW_SALESCOUNT,"
													+ " A.IS_SMS_ENABLED,"
													/*2020-12-03 김태우 추가 / 알림톡 발송여부 */
													+ " A.IS_KAKAO_ENABLED,"
													/*2020-12-09 김태우 추가 / 알림톡 최대개수 */
													+ " A.KAKAO_MAX_COUNT,"
													+ " TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE,"
													+ " TO_CHAR(A.MODIFY_DATE, 'YYYY-MM-DD') AS MODIFY_DATE"
												+ " FROM TB_COMPANY A"
												+ WHERE
												+ " ORDER BY NAME"
										+ " ) AA"
									+ " WHERE ROWNUM <= " + e
							+ " )"
						+ " WHERE ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("NAME", rs.getString("NAME"));
				c.put("MEMO", rs.getString("MEMO"));
				c.put("IS_USED_CLOSING", rs.getString("IS_USED_CLOSING"));
				c.put("IS_VIEW_CLOSING", rs.getString("IS_VIEW_CLOSING"));
				c.put("IS_RAW_SALESCOUNT", rs.getString("IS_RAW_SALESCOUNT"));
				c.put("IS_SMS_ENABLED", rs.getString("IS_SMS_ENABLED"));
				/*2020-12-03 김태우 추가*/
				c.put("IS_KAKAO_ENABLED", rs.getString("IS_KAKAO_ENABLED"));
				/*2020-12-09 김태우 추가 / 알림톡 최대 개수 */
				c.put("KAKAO_MAX_COUNT", rs.getLong("KAKAO_MAX_COUNT"));
				c.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				c.put("MODIFY_DATE", rs.getString("MODIFY_DATE"));
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
 * 등록/수정
 *
 * @param seq 계정번호
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

	// 등록 정보 가져오기
		this.data = new GeneralConfig();

		if (seq > 0) {
			try {
//20160212 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "SELECT * FROM TB_COMPANY WHERE SEQ = ?");
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " *"
							+ " FROM TB_COMPANY A"
							+ " WHERE SEQ = ?"
					);
				ps.setLong(1, seq);
				rs = ps.executeQuery();

				if (rs.next()) {
					this.data.put("NAME", rs.getString("NAME"));
					this.data.put("MEMO", rs.getString("MEMO"));
					this.data.put("IS_USED_CLOSING", rs.getString("IS_USED_CLOSING"));
					this.data.put("IS_VIEW_CLOSING", rs.getString("IS_VIEW_CLOSING"));
					this.data.put("IS_RAW_SALESCOUNT", rs.getString("IS_RAW_SALESCOUNT"));
					this.data.put("IS_SMS_ENABLED", rs.getString("IS_SMS_ENABLED"));
					this.data.put("IS_KAKAO_ENABLED", rs.getString("IS_KAKAO_ENABLED"));
					this.data.put("KAKAO_MAX_COUNT", rs.getLong("KAKAO_MAX_COUNT"));
				} else {
					error = "등록되지 않았거나 조회가 불가능한 소속입니다.";
				}
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(rs);
				dbLib.close(ps);
			}
		}

	// 허가된 상품 그룹
		this.group = new ArrayList<GeneralConfig>();

		if (StringEx.isEmpty(error)) {
			try {
//20160212 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "SELECT A.SEQ, A.NAME, CASE WHEN B.GROUP_SEQ IS NULL THEN 'N' ELSE 'Y' END AS IS_SELECTED FROM TB_GROUP A LEFT JOIN TB_GROUP_ENABLED B ON (A.SEQ = B.GROUP_SEQ AND B.COMPANY_SEQ = ?) WHERE A.DEPTH = 0 AND A.CODE NOT IN ('X0000001', 'X0000002')");
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " A.SEQ,"
								+ " A.NAME,"
								+ " CASE WHEN B.GROUP_SEQ IS NULL THEN 'N' ELSE 'Y' END AS IS_SELECTED"
							+ " FROM TB_GROUP A"
								+ " LEFT JOIN TB_GROUP_ENABLED B"
									+ " ON A.SEQ = B.GROUP_SEQ"
										+ " AND B.COMPANY_SEQ = ?"
							+ " WHERE A.DEPTH = 0"
								+ " AND A.CODE NOT IN ('X0000001', 'X0000002')"
							+ " ORDER BY A.NAME, A.SEQ"
					);
				ps.setLong(1, seq);
				rs = ps.executeQuery();

				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();

					c.put("SEQ", rs.getLong("SEQ"));
					c.put("NAME", rs.getString("NAME"));
					c.put("IS_SELECTED", rs.getString("IS_SELECTED"));

					this.group.add(c);
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
 * 등록/수정
 *
 * @param seq 계정번호
 * @param name 이름
 * @param memo 메모
 * @param isUsedClosing 마감 처리 여부
 * @param isViewClosing 마감 출력 여부
 * @param group 그룹
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(long seq, String name, String memo, String isUsedClosing, String isViewClosing, String[] group) throws Exception {
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

	// 등록/수정
		long sequence = seq;

		if (seq > 0) {
			try {
//20160212 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "UPDATE TB_COMPANY SET NAME = ?, MEMO = ?, IS_USED_CLOSING = ?, IS_VIEW_CLOSING = ?, MODIFY_USER_SEQ = ?, MODIFY_DATE = SYSDATE WHERE SEQ = ?");
				ps = dbLib.prepareStatement(conn,
						"UPDATE /*+ INDEX(A) */ TB_COMPANY A"
							+ " SET NAME = ?,"
								+ " MEMO = ?,"
								+ " IS_USED_CLOSING = ?,"
								+ " IS_VIEW_CLOSING = ?,"
								+ " MODIFY_USER_SEQ = ?,"
								+ " MODIFY_DATE = SYSDATE"
							+ " WHERE SEQ = ?"
					);
				ps.setString(1, name);
				ps.setString(2, memo);
				ps.setString(3, isUsedClosing);
				ps.setString(4, isViewClosing);
				ps.setLong(5, this.cfg.getLong("user.seq"));
				ps.setLong(6, seq);
				ps.executeUpdate();
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(ps);
			}

			if (StringEx.isEmpty(error)) {
				try {
//20160212 INDEX 힌트 추가
//					ps = dbLib.prepareStatement(conn, "DELETE FROM TB_GROUP_ENABLED WHERE COMPANY_SEQ = ?");
					ps = dbLib.prepareStatement(conn,
							"DELETE /*+ INDEX(A) */"
								+ " FROM TB_GROUP_ENABLED A"
								+ " WHERE COMPANY_SEQ = ?"
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
		} else {
			try {
//				ps = dbLib.prepareStatement(conn, "INSERT INTO TB_COMPANY (SEQ, NAME, MEMO, IS_USED_CLOSING, IS_VIEW_CLOSING, CREATE_USER_SEQ, CREATE_DATE) VALUES (SQ_COMPANY.NEXTVAL, ?, ?, ?, ?, ?, SYSDATE)");
				ps = dbLib.prepareStatement(conn,
						"INSERT INTO TB_COMPANY ("
								+ "	SEQ,"
								+ " NAME,"
								+ " MEMO,"
								+ " IS_USED_CLOSING,"
								+ " IS_VIEW_CLOSING,"
								+ " CREATE_USER_SEQ,"
								+ " CREATE_DATE"
							+ " ) VALUES ("
								+ " SQ_COMPANY.NEXTVAL,"
								+ " ?,"
								+ " ?,"
								+ " ?,"
								+ " ?,"
								+ " ?,"
								+ " SYSDATE"
							+ " )"
					);
				ps.setString(1, name);
				ps.setString(2, memo);
				ps.setString(3, isUsedClosing);
				ps.setString(4, isViewClosing);
				ps.setLong(5, this.cfg.getLong("user.seq"));
				ps.executeUpdate();
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(ps);
			}

			sequence = StringEx.str2long(dbLib.getResult(conn, "SELECT SQ_COMPANY.CURRVAL FROM DUAL"));
		}

		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn, dbLib.ROLLBACK);
			return error;
		}

	// 허가된 상품 그룹 등록
		for (int i = 0; i < group.length; i++) {
			try {
//				ps = dbLib.prepareStatement(conn, "INSERT INTO TB_GROUP_ENABLED (GROUP_SEQ, COMPANY_SEQ) VALUES (?, ?)");
				ps = dbLib.prepareStatement(conn,
						"INSERT INTO TB_GROUP_ENABLED ("
								+ " GROUP_SEQ,"
								+ " COMPANY_SEQ"
							+ ") VALUES ("
								+ " ?,"
								+ " ?"
							+ ")"
					);
				ps.setLong(1, StringEx.str2long(group[i]));
				ps.setLong(2, sequence);
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
 * 등록/수정
 *
 * @param seq 계정번호
 * @param name 이름
 * @param memo 메모
 * @param isUsedClosing 마감 처리 여부
 * @param isViewClosing 마감 출력 여부
 * @param isRawSalescount 판매계수 미보정 사용 여부
 * @param isSMSEnabled SMS발송 여부
 * @param isKAKAOEnabled 알림톡 발송 여부
*  @param kAKAOMaxCount 알림톡 최대 개수
 * @param group 그룹
 * @return 에러가 있을 경우 에러 내용
 *
 */
	//2020-12-03 김태우 추가
	public String regist(long seq, String name, String memo, String isUsedClosing, String isViewClosing, String isRawSalescount, String isSMSEnabled, String isKAKAOEnabled, long kAKAOMaxCount, String[] group) throws Exception {
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

	// 등록/수정
		long sequence = seq;

		if (seq > 0) {
			try {
//20160212 INDEX 힌트 추가
//					ps = dbLib.prepareStatement(conn, "UPDATE TB_COMPANY SET NAME = ?, MEMO = ?, IS_USED_CLOSING = ?, IS_VIEW_CLOSING = ?, MODIFY_USER_SEQ = ?, MODIFY_DATE = SYSDATE WHERE SEQ = ?");
				ps = dbLib.prepareStatement(conn,
						"UPDATE /*+ INDEX(A) */ TB_COMPANY A"
							+ " SET NAME = ?,"
								+ " MEMO = ?,"
								+ " IS_USED_CLOSING = ?,"
								+ " IS_VIEW_CLOSING = ?,"
								+ " IS_RAW_SALESCOUNT = ?,"
								+ " IS_SMS_ENABLED = ?,"
								/*//2020-12-03 김태우 추가*/
								+ " IS_KAKAO_ENABLED = ?,"
								//2020-12-09 김태우 추가
								+ " KAKAO_MAX_COUNT = ?,"
								+ " MODIFY_USER_SEQ = ?,"
								+ " MODIFY_DATE = SYSDATE"
							+ " WHERE SEQ = ?"
					);
				ps.setString(1, name);
				ps.setString(2, memo);
				ps.setString(3, isUsedClosing);
				ps.setString(4, isViewClosing);
				ps.setString(5, isRawSalescount);
				ps.setString(6, isSMSEnabled);
				/*2002-12-03 김태우 추가*/
				ps.setString(7, isKAKAOEnabled);
				/*2020-12-09 김태우 추가 / 알림톡 최대 개수*/
				ps.setLong(8, kAKAOMaxCount);
				ps.setLong(9, this.cfg.getLong("user.seq"));
				ps.setLong(10, seq);
				/*ps.setLong(7, this.cfg.getLong("user.seq"));
				ps.setLong(8, seq);*/
				ps.executeUpdate();
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(ps);
			}

			if (StringEx.isEmpty(error)) {
				try {
//20160212 INDEX 힌트 추가
//						ps = dbLib.prepareStatement(conn, "DELETE FROM TB_GROUP_ENABLED WHERE COMPANY_SEQ = ?");
					ps = dbLib.prepareStatement(conn,
							"DELETE /*+ INDEX(A) */"
								+ " FROM TB_GROUP_ENABLED A"
								+ " WHERE COMPANY_SEQ = ?"
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
		} else {
			try {
//					ps = dbLib.prepareStatement(conn, "INSERT INTO TB_COMPANY (SEQ, NAME, MEMO, IS_USED_CLOSING, IS_VIEW_CLOSING, CREATE_USER_SEQ, CREATE_DATE) VALUES (SQ_COMPANY.NEXTVAL, ?, ?, ?, ?, ?, SYSDATE)");
				ps = dbLib.prepareStatement(conn,
						"INSERT INTO TB_COMPANY ("
								+ "	SEQ,"
								+ " NAME,"
								+ " MEMO,"
								+ " IS_USED_CLOSING,"
								+ " IS_VIEW_CLOSING,"
								+ " IS_RAW_SALESCOUNT,"
								+ " IS_SMS_ENABLED,"
								/*//2020-12-03 김태우 추가*/
								+ " IS_KAKAO_ENABLED,"
								/*2020-12-09 김태우 추가 / 알림톡 발송개수 */
								+ " KAKAO_MAX_COUNT,"
								+ " CREATE_USER_SEQ,"
								+ " CREATE_DATE"
							+ " ) VALUES ("
								+ " SQ_COMPANY.NEXTVAL,"
								+ " ?,"
								+ " ?,"
								+ " ?,"
								+ " ?,"
								+ " ?,"
								+ " ?,"
								//2020-12-09 김태우 추가
								+ " ?,"
								+ " ?,"
								+ " ?,"
								+ " SYSDATE"
							+ " )"
					);
				ps.setString(1, name);
				ps.setString(2, memo);
				ps.setString(3, isUsedClosing);
				ps.setString(4, isViewClosing);
				ps.setString(5, isRawSalescount);
				ps.setString(6, isSMSEnabled);
				/*2020-12-03 김태우 추가*/
				ps.setString(7, isKAKAOEnabled);
				/*2020-12-09 김태우 추가*/
				ps.setLong(8, kAKAOMaxCount);
				ps.setLong(9, this.cfg.getLong("user.seq"));
				/*ps.setLong(7, this.cfg.getLong("user.seq"));*/
				ps.executeUpdate();
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(ps);
			}

			sequence = StringEx.str2long(dbLib.getResult(conn, "SELECT SQ_COMPANY.CURRVAL FROM DUAL"));
		}

		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn, dbLib.ROLLBACK);
			return error;
		}

	// 허가된 상품 그룹 등록
		for (int i = 0; i < group.length; i++) {
			try {
//					ps = dbLib.prepareStatement(conn, "INSERT INTO TB_GROUP_ENABLED (GROUP_SEQ, COMPANY_SEQ) VALUES (?, ?)");
				ps = dbLib.prepareStatement(conn,
						"INSERT INTO TB_GROUP_ENABLED ("
								+ " GROUP_SEQ,"
								+ " COMPANY_SEQ"
							+ ") VALUES ("
								+ " ?,"
								+ " ?"
							+ ")"
					);
				ps.setLong(1, StringEx.str2long(group[i]));
				ps.setLong(2, sequence);
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
 * 삭제
 *
 * @param seq 계정번호
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
//20160212 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "DELETE FROM TB_COMPANY WHERE SEQ = ?");
			ps = dbLib.prepareStatement(conn,
					"DELETE /*+ INDEX(A) */"
						+ " FROM TB_COMPANY A"
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