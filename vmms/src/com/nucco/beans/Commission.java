package com.nucco.beans;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Commission.java
 *
 * 수수료
 *
 * 작성일 - 2011/06/11, 정원광
 *
 */
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.nucco.GlobalConfig;
import com.nucco.cfg.GeneralConfig;
import com.nucco.lib.Pager;
import com.nucco.lib.StringEx;
import com.nucco.lib.db.DBLibrary;

import oracle.jdbc.OracleTypes;

public class Commission {
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
 * 수수료 목록
 *
 */
	public ArrayList<GeneralConfig> list;
/**
 * 매입사 목록
 *
 */
	public ArrayList<GeneralConfig> card;
/**
 * 수수료 조회
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
	public Commission(GlobalConfig cfg) throws Exception {
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
 * @param company 소속
 * @param organ 조직
 * @param card 매입사
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getList(int pageNo, long company, long organ, String card) throws Exception {
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

	// 매입사 목록
		this.card = new ArrayList<GeneralConfig>();

		try {
//20160217 INDEX 힌트 변경
//			ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_CODE) */ CODE, NAME FROM TB_CODE A WHERE TYPE = 'PAY_CARD' AND CODE NOT IN ('000')");
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE A"
						+ " WHERE TYPE = 'PAY_CARD'"
							+ " AND CODE <> '000'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("CODE", rs.getString("CODE"));
				c.put("NAME", rs.getString("NAME"));

				this.card.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 검색절 생성
		String WHERE = "";

		if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND COMPANY_SEQ = " + this.cfg.getLong("user.company");
		}

		if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색
//20160217 INDEX 힌트 추가
//			WHERE += " AND ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ)";
			WHERE += " AND ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_A) */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION A_A"
							+ " WHERE SORT = 1"
							+ " START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ")
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;
		}

		if (company > 0) { // 소속
			WHERE += " AND COMPANY_SEQ = " + company;
		}

		if (organ > 0) { // 조직
//20160217 INDEX 힌트 추가, UNION 통합
//			WHERE += " AND ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + organ + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + organ + ")";
			WHERE += " AND ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_B) */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION A_B"
							+ " WHERE SORT = 1"
							+ " START WITH SEQ = " + organ
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;
		}

		if (!StringEx.isEmpty(card)) { // 매입사
			WHERE += " AND PAY_CARD = '" + card + "'";
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

	// 총 레코드수
//20160217 INDEX 힌트 추가
//		this.records = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_COMMISSION A WHERE " + WHERE));
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT" + (WHERE.length() > 0 ? " /*+ INDEX(A) */" : "")
								+ " COUNT(*)"
							+ " FROM TB_COMMISSION A"
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
//20160217 INDEX 힌트 추가, ORDER BY 추가, REVERSE 적용
//			ps = dbLib.prepareStatement(conn, "SELECT S2.*"
//				+ " FROM"
//				+ " ("
//				+ " SELECT ROWNUM AS RNUM, S1.*"
//				+ " FROM"
//				+ " ("
//				+ " SELECT /*+ ORDERED USE_NL(B C) INDEX_DESC(A PK_COMMISSION) */ A.SEQ, A.COMMISSION_RATE, TO_CHAR(TO_DATE(A.START_DATE, 'YYYYMMDD'), 'YYYY-MM-DD') AS START_DATE, TO_CHAR(TO_DATE(A.END_DATE, 'YYYYMMDD'), 'YYYY-MM-DD') AS END_DATE, A.MEMO, B.NAME AS COMPANY, C.NAME AS PAY_CARD_NAME, (SELECT LTRIM(SYS_CONNECT_BY_PATH(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS ORGAN, CASE WHEN A.END_DATE IS NULL THEN 'Y' ELSE 'N' END AS IS_ABLED_MODIFY"
//				+ " FROM TB_COMMISSION A"
//				+ " LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ"
//				+ " LEFT JOIN TB_CODE C ON (A.PAY_CARD = C.CODE AND C.TYPE = 'PAY_CARD')"
//				+ " WHERE " + WHERE
//				+ " ) S1"
//				+ " WHERE ROWNUM <= " + e
//				+ " ) S2"
//				+ " WHERE RNUM >= " + s);
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED USE_NL(C) */"
							+ " AAA.*,"
							+ " C.NAME AS PAY_CARD_NAME"
						+ " FROM ("
									+ " SELECT"
											+ " ROWNUM AS ROW_NUM,"
											+ " AA.*"
										+ " FROM ("
												+ " SELECT /*+ ORDERED"  + (WHERE.length() > 0 ? " INDEX(A)" : "") + " USE_HASH(B) */"
														+ " B.NAME AS COMPANY,"
														+ " ("
																+ " SELECT /*+ INDEX(D) */"
																		+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
																	+ " FROM TB_ORGANIZATION D"
																	+ " WHERE PARENT_SEQ = 0"
																	+ " START WITH SEQ = A.ORGANIZATION_SEQ"
																	+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
															+ " ) AS ORGAN,"
														+ " A.PAY_CARD,"
														+ " A.SEQ,"
														+ " A.COMMISSION_RATE,"
														+ " A.START_DATE,"
														+ " A.END_DATE,"
														+ " A.MEMO,"
														+ " CASE WHEN A.END_DATE IS NULL THEN 'Y' ELSE 'N' END AS IS_ABLED_MODIFY"
													+ " FROM TB_COMMISSION A"
														+ " LEFT JOIN TB_COMPANY B"
															+ " ON A.COMPANY_SEQ = B.SEQ"
													+ WHERE
													+ " ORDER BY 1, 2, 3"
											+ " ) AA"
										+ " WHERE ROWNUM <= " + e
								+ " ) AAA"
							+ " LEFT JOIN TB_CODE C"
								+ " ON AAA.PAY_CARD = C.CODE"
									+ " AND C.TYPE = 'PAY_CARD'"
					+ " WHERE ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("COMMISSION_RATE", rs.getString("COMMISSION_RATE"));
//20160217 JAVA로 날짜 포멧
//				c.put("START_DATE", rs.getString("START_DATE"));
//				c.put("END_DATE", rs.getString("END_DATE"));
				String date = rs.getString("START_DATE");
				c.put("START_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8)
							: null
					);
				date = rs.getString("END_DATE");
				c.put("END_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8)
							: null
					);
				c.put("MEMO", rs.getString("MEMO"));
				c.put("COMPANY", rs.getString("COMPANY"));
				c.put("PAY_CARD_NAME", rs.getString("PAY_CARD_NAME"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("IS_ABLED_MODIFY", rs.getString("IS_ABLED_MODIFY"));
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

	// 매입사 목록
		this.card = new ArrayList<GeneralConfig>();

		try {
//20160218 INDEX 힌트 변경, ORDER BY 추가
//			ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_CODE) */ CODE, NAME FROM TB_CODE A WHERE TYPE = 'PAY_CARD' AND CODE NOT IN ('000')");
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE A"
						+ " WHERE TYPE = 'PAY_CARD'"
							+ " AND CODE <> '000'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("CODE", rs.getString("CODE"));
				c.put("NAME", rs.getString("NAME"));

				this.card.add(c);
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
 * @param organ 조직
 * @param card 매입사
 * @param commission 수수료
 * @param sDate 시작일
 * @param eDate 종료일
 * @param memo 설명
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(long company, long organ, String card, float commission, String sDate, String eDate, String memo) throws Exception {
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

	// 등록
		try {
			cs = dbLib.prepareCall(conn, "{ CALL SP_COMMISSION (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");
			cs.setLong(1, 0);
			cs.setLong(2, company);
			cs.setLong(3, organ);
			cs.setString(4, card);
			cs.setFloat(5, commission);
			cs.setString(6, sDate);
			cs.setString(7, eDate.length() == 8 ? eDate : "");
			cs.setString(8, memo);
			cs.setLong(9, this.cfg.getLong("user.seq"));
			cs.registerOutParameter(10, OracleTypes.VARCHAR);
			cs.execute();

			error = cs.getString(10);
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

	// 등록정보
		this.data = new GeneralConfig();

		try {
//20160218 INDEX 힌트 추가, REVERSE 적용, UNION 통합
//			ps = dbLib.prepareStatement(conn, "SELECT A.*, B.NAME AS COMPANY, C.NAME AS PAY_CARD_NAME, (SELECT LTRIM(SYS_CONNECT_BY_PATH(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS ORGAN FROM TB_COMMISSION A LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ LEFT JOIN TB_CODE C ON (A.PAY_CARD = C.CODE AND C.TYPE = 'PAY_CARD') WHERE A.SEQ = ?"
//					+ (this.cfg.getLong("user.company") > 0 ? " AND COMPANY_SEQ = " + this.cfg.getLong("user.company") : "")
//					+ (this.cfg.getLong("user.organ") > 0 ? " AND ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + this.cfg.getLong("user.organ") + ")" : ""));
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED INDEX(A) USE_NL(B C) */"
							+ " A.*,"
							+ " B.NAME AS COMPANY,"
							+ " C.NAME AS PAY_CARD_NAME,"
							+ " ("
									+ " SELECT /*+ INDEX(D) */"
											+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
										+ " FROM TB_ORGANIZATION D"
										+ " WHERE PARENT_SEQ = 0"
										+ " START WITH SEQ = A.ORGANIZATION_SEQ"
										+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
								+ " ) AS ORGAN"
						+ " FROM TB_COMMISSION A"
							+ " LEFT JOIN TB_COMPANY B"
								+ " ON A.COMPANY_SEQ = B.SEQ"
							+ " LEFT JOIN TB_CODE C"
								+ " ON A.PAY_CARD = C.CODE"
									+ " AND C.TYPE = 'PAY_CARD'"
						+ " WHERE A.SEQ = ?"
					+ (this.cfg.getLong("user.company") > 0
							? " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company")
							: ""
						)
					+ (this.cfg.getLong("user.organ") > 0
							? " AND A.ORGANIZATION_SEQ IN ("
									+ " SELECT /*+ INDEX(A_A) */"
											+ " SEQ"
										+ " FROM TB_ORGANIZATION A_A"
										+ " WHERE SORT = 1"
										+ " START WITH SEQ = " + this.cfg.getLong("user.organ")
										+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
								+ ")"
							: ""
						)
				);
			ps.setLong(1, seq);
			rs = ps.executeQuery();

			if (rs.next()) {
				this.data.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				this.data.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				this.data.put("PAY_CARD", rs.getString("PAY_CARD"));
				this.data.put("COMPANY", rs.getString("COMPANY"));
				this.data.put("PAY_CARD_NAME", rs.getString("PAY_CARD_NAME"));
				this.data.put("ORGAN", rs.getString("ORGAN"));
			} else {
				error = "등록되지 않았거나 조회가 불가능한 정보입니다.";
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
 * @param seq 등록번호
 * @param company 소속
 * @param organ 조직
 * @param card 매입사
 * @param commission 수수료
 * @param sDate 시작일
 * @param eDate 종료일
 * @param memo 메모
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String modify(long seq, long company, long organ, String card, float commission, String sDate, String eDate, String memo) throws Exception {
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

	// 등록
		try {
			cs = dbLib.prepareCall(conn, "{ CALL SP_COMMISSION (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");
			cs.setLong(1, seq);
			cs.setLong(2, company);
			cs.setLong(3, organ);
			cs.setString(4, card);
			cs.setFloat(5, commission);
			cs.setString(6, sDate);
			cs.setString(7, eDate.length() == 8 ? eDate : "");
			cs.setString(8, memo);
			cs.setLong(9, this.cfg.getLong("user.seq"));
			cs.registerOutParameter(10, OracleTypes.VARCHAR);
			cs.execute();

			error = cs.getString(10);
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
		ResultSet rs = null;
		String error = null;
		long organ = 0;
		String card = "";

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}

	// COMMIT 설정
		conn.setAutoCommit(false);

	// 삭제 전 조직 및 매입사 정보
		try {
//20160218 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "SELECT ORGANIZATION_SEQ, PAY_CARD FROM TB_COMMISSION WHERE SEQ = ?");
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " ORGANIZATION_SEQ,"
							+ " PAY_CARD"
					+ " FROM TB_COMMISSION A"
					+ " WHERE SEQ = ?"
				);
			ps.setLong(1, seq);
			rs = ps.executeQuery();

			if (rs.next()) {
				organ = rs.getLong("ORGANIZATION_SEQ");
				card = rs.getString("PAY_CARD");
			} else {
				error = "등록되지 않은 수수료 정보입니다.";
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
			dbLib.close(conn, dbLib.ROLLBACK);
			return error;
		}

	// 삭제
		try {
//20160218 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "DELETE FROM TB_COMMISSION WHERE SEQ = ?");
			ps = dbLib.prepareStatement(conn,
					"DELETE /*+ INDEX(A) */"
					+ " FROM TB_COMMISSION A"
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

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn, dbLib.ROLLBACK);
			return error;
		}

	// 가장 최근 등록된 수수료의 종료일 변경
		try {
//20160218 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "UPDATE TB_COMMISSION SET END_DATE = NULL WHERE SEQ = (SELECT /*+ INDEX_DESC(A PK_COMMISSION) */ SEQ FROM TB_COMMISSION A WHERE ORGANIZATION_SEQ = ? AND PAY_CARD = ? AND ROWNUM = 1)");
			ps = dbLib.prepareStatement(conn,
					"UPDATE /*+ INDEX A */ TB_COMMISSION A"
						+ " SET END_DATE = NULL"
						+ " WHERE SEQ = ("
								+ " SELECT /*+ INDEX(B) */"
										+ " SEQ"
									+ " FROM TB_COMMISSION B"
									+ " WHERE ORGANIZATION_SEQ = ?"
										+ " AND PAY_CARD = ?"
										+ " AND ROWNUM = 1"
							+ " )"
				);
			ps.setLong(1, organ);
			ps.setString(2, card);
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

	// 리소스 반환
		dbLib.close(conn, dbLib.COMMIT);

		return error;
	}
}