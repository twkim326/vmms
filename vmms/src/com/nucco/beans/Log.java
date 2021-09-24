package com.nucco.beans;

/**
 * Log.java
 *
 * 로그
 *
 * 작성일 - 2011/04/09, 정원광
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

public class Log {
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
 * 목록
 *
 */
	public ArrayList<GeneralConfig> list;
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
	public Log(GlobalConfig cfg) throws Exception {
	// set config
		this.cfg = cfg;

	// set log4j
		PropertyConfigurator.configure(cfg.get("config.log4j"));
		this.logger = Logger.getLogger(this.getClass());
	}
/**
 * 배치 로그
 *
 * @param pageNo 페이지
 * @param sDate 검색 시작일
 * @param eDate 검색 종료일
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String batch(int pageNo, String sDate, String eDate, String sField, String sQuery) throws Exception {
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

		if (!StringEx.isEmpty(sDate)) { // 시작일
			WHERE += " AND A.START_DATE >= '" + sDate + "'";
		}

		if (!StringEx.isEmpty(eDate)) { // 종료일
			WHERE += " AND A.START_DATE <= '" + eDate + "'";
		}

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) {
			WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

	// 총 레코드수
//20160217
//		this.records = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_BATCH_LOG A WHERE " + WHERE));
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT" + (WHERE.length() > 0 ? " /*+ INDEX(A) */" : "")
								+ " COUNT(*)"
							+ " FROM TB_BATCH_LOG A"
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
//20160217
//			ps = dbLib.prepareStatement(conn, "SELECT S2.*"
//					+ " FROM"
//					+ " ("
//					+ " SELECT ROWNUM AS RNUM, S1.*"
//					+ " FROM"
//					+ " ("
//					+ " SELECT /*+ INDEX_DESC(A PK_BATCH_LOG) */ SERVER_NAME, SERVICE_NAME, SUCCESS, FAILURE, START_DATE, START_TIME, END_DATE, END_TIME, ERR_CODE, ERR_DESC"
//					+ " FROM TB_BATCH_LOG A"
//					+ " WHERE " + WHERE
//					+ " ) S1"
//					+ " WHERE ROWNUM <= " + e
//					+ " ) S2"
//					+ " WHERE RNUM >= " + s);
			ps = dbLib.prepareStatement(conn,
					"SELECT *"
						+ " FROM ("
								+ " SELECT"
										+ " ROWNUM AS ROW_NUM,"
										+ " AA.*"
									+ " FROM ("
											+ " SELECT" + (WHERE.length() > 0 ? " /*+ INDEX(A) */" : "")
													+ " *"
												+ " FROM TB_BATCH_LOG A"
												+ WHERE
												+ " ORDER BY CREATE_DATE DESC"
										+ " ) AA"
									+ " WHERE ROWNUM <= " + e
							+ " )"
						+ " WHERE ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SERVER_NAME", rs.getString("SERVER_NAME"));
				c.put("SERVICE_NAME", rs.getString("SERVICE_NAME"));
				c.put("SUCCESS", rs.getInt("SUCCESS"));
				c.put("FAILURE", rs.getInt("FAILURE"));
				c.put("START_DATE", rs.getString("START_DATE"));
				c.put("START_TIME", rs.getString("START_TIME"));
				c.put("END_DATE", rs.getString("END_DATE"));
				c.put("END_TIME", rs.getString("END_TIME"));
				c.put("ERR_CODE", rs.getString("ERR_CODE"));
				c.put("ERR_DESC", rs.getString("ERR_DESC"));
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
 * 변경 로그
 *
 * @param pageNo 페이지
 * @param sDate 검색 시작일
 * @param eDate 검색 종료일
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String change(int pageNo, String sDate, String eDate, String sField, String sQuery) throws Exception {
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

		if (!StringEx.isEmpty(sDate)) { // 시작일
			WHERE += " AND A.CHANGE_DATE >= '" + sDate + "'";
		}

		if (!StringEx.isEmpty(eDate)) { // 종료일
			WHERE += " AND A.CHANGE_DATE <= '" + eDate + "'";
		}

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) {
			WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

	// 총 레코드수
		this.records = StringEx.str2long(
//20160217 INDEX 힌트 추가
//				dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_CHANGE_LOG A WHERE " + WHERE));
				dbLib.getResult(conn,
						"SELECT" + (WHERE.length() > 0 ? " /*+ INDEX(A) */" : "")
								+ " COUNT(*)"
							+ " FROM TB_CHANGE_LOG A"
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
//20160217 INDEX 힌트 변경
//			ps = dbLib.prepareStatement(conn, "SELECT S2.*"
//				+ " FROM"
//				+ " ("
//				+ " SELECT ROWNUM AS RNUM, S1.*"
//				+ " FROM"
//				+ " ("
//				+ " SELECT /*+ INDEX_DESC(A PK_CHANGE_LOG) */ TO_CHAR(CREATE_DATE, 'YYYY-MM-DD HH24:MI:SS') AS CREATE_DATE, DETAIL"
//				+ " FROM TB_CHANGE_LOG A"
//				+ " WHERE " + WHERE
//				+ " ) S1"
//				+ " WHERE ROWNUM <= " + e
//				+ " ) S2"
//				+ " WHERE RNUM >= " + s);
			ps = dbLib.prepareStatement(conn,
					"SELECT"
							+ " TO_CHAR(CREATE_DATE, 'YYYY-MM-DD HH24:MI:SS') AS CREATE_DATE,"
							+ " DETAIL"
						+ " FROM ("
								+ " SELECT"
										+ " ROWNUM AS ROW_NUM,"
										+ " AA.*"
									+ " FROM ("
											+ " SELECT" + (WHERE.length() > 0 ? " /*+ INDEX(A) */" : "")
													+ " *"
												+ " FROM TB_CHANGE_LOG A"
												+ WHERE
												+ " ORDER BY CREATE_DATE DESC"
										+ " ) AA"
									+ " WHERE ROWNUM <= " + e
							+ " )"
						+ " WHERE ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				c.put("DETAIL", rs.getString("DETAIL"));
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
}