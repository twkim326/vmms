package com.nucco.beans;

/**
 * Adjust.java
 *
 * 정산정보 조회
 *
 * 작성일 - 2012/01/25, 최문봉
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

public class Adjust {
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
  조회
 *
 */
	public GeneralConfig data;

	/**
	 * 총합정보
	 *
	 */
	public GeneralConfig SumData;

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
	public Adjust(GlobalConfig cfg) throws Exception {
	// set config
		this.cfg = cfg;

	// set log4j
		PropertyConfigurator.configure(cfg.get("config.log4j"));
		this.logger = Logger.getLogger(this.getClass());
	}
/**
 * 선불카드 정산정보 목록
 *
 * @param pageNo 페이지
 * @param sDate 검색시작일
 * @param eDate 검색종료일
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getPrepayAdjList(int pageNo, String sDate, String eDate, String sField, String sQuery) throws Exception
	{
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String error = null;

		// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null)
		{
			throw new Exception("DB 연결에 실패하였습니다.");
		}

	// 검색절 생성
		String WHERE = "";

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
		}


		if (!StringEx.isEmpty(sDate) && !StringEx.isEmpty(eDate)) { // 검색일자
			WHERE += " AND PAY_DATE BETWEEN '" + sDate + "' AND '" + eDate + "'";
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

		// 총 레코드수
//20160218 INDEX 힌트 추가
//			this.records = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_TXT_PAYMENT_PREPAY_INFO A WHERE " + WHERE));
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT" + (WHERE.length() > 0 ? " /*+ INDEX(A) */" : "")
								+ " COUNT(*)"
							+ " FROM TB_TXT_PAYMENT_PREPAY_INFO A"
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
							+ " SID,"
							+ " NVL(PAY_DATE, '') AS PAY_DATE,"
							+ " TO_NUMBER(NVL(AMOUNT,'0')) AS AMOUNT,"
							+ " TO_NUMBER(NVL(COMMISSION,'0')) AS COMMISSION,"
							+ " TO_NUMBER(NVL(PAY_AMOUNT,'0')) AS PAY_AMOUNT,"
							+ " TO_NUMBER(NVL(COMMISSION_RATE, '0')) AS COMMISSION_RATE,"
							+ " TO_CHAR(CREATE_DATE, 'YYYY-MM-DD HH24:MI:SS') AS CREATE_DATE"
						+ " FROM ("
								+ " SELECT"
										+ " ROWNUM AS ROW_NUM,"
										+ " AA.*"
									+ " FROM ("
											+ " SELECT" + (WHERE.length() > 0 ? " /*+ INDEX(A) */" : "")
													+ " *"
												+ " FROM TB_TXT_PAYMENT_PREPAY_INFO A "
												+ WHERE
												+ " ORDER BY A.PAY_DATE DESC, A.CREATE_DATE DESC "
										+ " ) AA"
									+ " WHERE ROWNUM <= " + e
							+ " )"
						+ " WHERE ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SID", rs.getString("SID"));
				c.put("PAY_DATE", rs.getString("PAY_DATE"));
				c.put("AMOUNT", String.valueOf(rs.getLong("AMOUNT")));
				c.put("COMMISSION", String.valueOf(rs.getLong("COMMISSION")));
				c.put("PAY_AMOUNT", String.valueOf(rs.getLong("PAY_AMOUNT")));
				c.put("COMMISSION_RATE", String.valueOf(rs.getFloat("COMMISSION_RATE")));
				c.put("CREATE_DATE", rs.getString("CREATE_DATE"));

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

		dbLib.close(conn);

		return error;
	}

	/**
	 * 입금정산 상세 현황
	 *
	 * @param strSid SID
	 * @param sDate 입금일
	 * @param pageNo 페이지
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
		public String detail(String strSid, String sDate, int pageNo) throws Exception
		{
		// 필수 검색 조건이 없을 때
			if ( StringEx.isEmpty(strSid) || StringEx.isEmpty(sDate) ) {
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
				throw new Exception("DB 연결에 실패하였습니다.");
			}

			try {
				// 검색절 생성
//					String WHERE = "A.PAY_DATE = '" + sDate + "' AND A.SID='" + strSid  + "' GROUP BY  A.TERMINAL_ID,  B.PLACE, C.NAME , A.PAY_DATE " ;
//					String TABLE = "TB_SALES A INNER JOIN TB_VENDING_MACHINE_PLACE B ON A.VM_PLACE_SEQ = B.SEQ INNER JOIN TB_ORGANIZATION C ON A.ORGANIZATION_SEQ = C.SEQ INNER JOIN TB_GOODS D ON A.GOODS_SEQ = D.SEQ LEFT JOIN TB_CODE E ON (A.PAY_TYPE = E.CODE AND E.TYPE = 'PAY_TYPE') LEFT JOIN TB_CODE F ON (A.PAY_STEP = F.CODE AND F.TYPE = 'PAY_STEP') LEFT JOIN TB_CODE G ON (A.INPUT_TYPE = G.CODE AND G.TYPE = 'INPUT_TYPE') LEFT JOIN TB_CODE H ON (A.PAY_CARD = H.CODE AND H.TYPE = 'PAY_CARD')";

				// 총 레코드수
//20160218 INDEX 힌트 추가, 불필요한 JOIN 제거, 서브쿼리 통합
//					StringBuffer sbsql_total = new StringBuffer();
//					sbsql_total.append(" SELECT count(*) totcnt,SUM(CNT) CNT, SUM(AMOUNT) AMOUNT,SUM(COMMISSION) COMMISSION , SUM(OUTAMOUNT) OUTAMOUNT, SUM(IN_AMOUNT) IN_AMOUNT FROM ( " )
//							.append(" SELECT ")
//							.append("  COUNT(TRANSACTION_NO) CNT,SUM(A.AMOUNT) AMOUNT , SUM(A.COMMISSION) COMMISSION , SUM(A.OUTAMOUNT) OUTAMOUNT, SUM(A.AMOUNT-A.COMMISSION+A.OUTAMOUNT) IN_AMOUNT   ")
//							.append(" FROM " + TABLE + " WHERE " + WHERE )
//							.append(" ) ");
//
//					ps = dbLib.prepareStatement(conn, sbsql_total.toString());

					ps = dbLib.prepareStatement(conn,
							"SELECT /* INDEX(A) */"
									+ " COUNT(DISTINCT TERMINAL_ID || '-' || ORGANIZATION_SEQ || '-' || VM_PLACE_SEQ) AS TOTCNT,"
									+ " COUNT(*) AS CNT,"
									+ " SUM(AMOUNT) AS AMOUNT,"
									+ " SUM(COMMISSION) AS COMMISSION,"
									+ " SUM(OUTAMOUNT) AS OUTAMOUNT,"
									+ " SUM(A.AMOUNT - A.COMMISSION + A.OUTAMOUNT) AS IN_AMOUNT"
								+ " FROM TB_SALES A"
								+ " WHERE PAY_DATE = ?"
									+ " AND SID = ?"
						);
					ps.setString(1, sDate);
					ps.setString(2, strSid);
					rs = ps.executeQuery();

					rs.next();
					//this.records = StringEx.str2long(rs.getString(1));
					this.records = rs.getLong("TOTCNT");
					this.SumData = new GeneralConfig();

					this.SumData.put("CNT", rs.getLong("CNT"));
					this.SumData.put("AMOUNT", rs.getLong("AMOUNT"));
					this.SumData.put("COMMISSION", rs.getLong("COMMISSION"));
					this.SumData.put("OUTAMOUNT", rs.getLong("OUTAMOUNT"));
					this.SumData.put("IN_AMOUNT", rs.getLong("IN_AMOUNT"));

					rs.close();
					ps.close();

					rs = null;
					ps = null;


				// 총 페이지수
					this.pages = Pager.getSize(this.records, cfg.getInt("limit.list"));

				// 리스트 가상번호
					long no = this.records - (pageNo - 1) * cfg.getInt("limit.list");

				// 목록 가져오기
					this.list = new ArrayList<GeneralConfig>();
					this.data = new GeneralConfig();

						int s = ((pageNo - 1) * cfg.getInt("limit.list")) + 1;
						int e = (s - 1) + cfg.getInt("limit.list");

//20160218 INDEX 힌트 추가, 불필요한 JOIN 제거, 사용하지 않는 컬럼 제외
//						ps = dbLib.prepareStatement(conn, "SELECT S2.*"
//								+ " FROM"
//								+ " ("
//								+ " SELECT ROWNUM AS RNUM, S1.*"
//								+ " FROM"
//								+ " ("
//								+ " SELECT "
//								+ "  A.TERMINAL_ID,  B.PLACE, C.NAME AS ORGAN, "
//								+ "  TO_CHAR(TO_DATE(A.PAY_DATE, 'YYYYMMDD'), 'YYYY-MM-DD') AS PAY_DATE, "
//								+ "  COUNT(TRANSACTION_NO) CNT ,SUM(A.AMOUNT) AMOUNT , SUM(A.COMMISSION) COMMISSION , SUM(A.OUTAMOUNT) OUTAMOUNT, SUM(A.AMOUNT-A.COMMISSION+A.OUTAMOUNT) IN_AMOUNT "
//								+ " FROM " + TABLE
//								+ " WHERE " + WHERE
//								+ " ) S1"
//								+ " WHERE ROWNUM <= " + e
//								+ " ) S2"
//								+ " WHERE RNUM >= " + s);
						ps = dbLib.prepareStatement(conn,
								"SELECT *"
									+ " FROM ("
											+ " SELECT"
													+ " ROWNUM AS ROW_NUM,"
													+ " AA.*"
												+ " FROM ("
														+ " SELECT /*+ ORDERED INDEX(A) USE_HASH(B C D E F G H) */"
																+ " A.TERMINAL_ID,"
																+ " B.NAME AS ORGAN,"
																+ " C.PLACE,"
																//+ " TO_CHAR(TO_DATE(A.PAY_DATE, 'YYYYMMDD'), 'YYYY-MM-DD') AS PAY_DATE, "
																+ " COUNT(TRANSACTION_NO) AS CNT,"
																+ " SUM(A.AMOUNT) AS AMOUNT,"
																+ " SUM(A.COMMISSION) AS COMMISSION,"
																+ " SUM(A.OUTAMOUNT) AS OUTAMOUNT,"
																+ " SUM(A.AMOUNT - A.COMMISSION + A.OUTAMOUNT) AS IN_AMOUNT"
															+ " FROM TB_SALES A"
																+ " INNER JOIN TB_ORGANIZATION B"
																	+ " ON A.ORGANIZATION_SEQ = B.SEQ"
																+ " INNER JOIN TB_VENDING_MACHINE_PLACE C"
																	+ " ON A.VM_PLACE_SEQ = C.SEQ"
															+ " WHERE PAY_DATE = ?"
																+ " AND SID = ?"
															+ " GROUP BY B.NAME, C.PLACE, A.TERMINAL_ID" // , A.PAY_DATE"
															+ " ORDER BY B.NAME, C.PLACE, A.TERMINAL_ID"
													+ " ) AA"
												+ " WHERE ROWNUM <= " + e
										+ " ) AAA"
									+ " WHERE ROW_NUM >= " + s
							);
						ps.setString(1, sDate);
						ps.setString(2, strSid);
						rs = ps.executeQuery();

						while (rs.next()) {
							GeneralConfig c = new GeneralConfig();

							c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
							c.put("PLACE", rs.getString("PLACE"));
							c.put("ORGAN", rs.getString("ORGAN"));
							//c.put("PAY_DATE", rs.getString("PAY_DATE"));
							c.put("CNT", rs.getLong("CNT"));
							c.put("AMOUNT", rs.getLong("AMOUNT"));
							c.put("COMMISSION", rs.getLong("COMMISSION"));
							c.put("OUTAMOUNT", rs.getLong("OUTAMOUNT"));
							c.put("IN_AMOUNT", rs.getLong("IN_AMOUNT"));
							c.put("NO", no--);

							this.list.add(c);
						}

						return null;
			} catch (Exception e) {
				e.printStackTrace();

				this.logger.error(e);
				error = e.getMessage();

				return error;
			}
			finally
			{
				dbLib.close(rs);
				dbLib.close(ps);
				// 리소스 반환
				dbLib.close(conn);
			}
		}
}