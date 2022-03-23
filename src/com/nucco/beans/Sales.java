package com.nucco.beans;

/**
 * Sales.java
 *
 * 매출
 *
 * 작성일 - 2011/04/05, 정원광
 * 수정일 - 2015/12/30 ~ 2016/01/06, 박영길
 * 		- 2016/06/13 ~	, 박영길
 *
 */

import com.nucco.GlobalConfig;
import com.nucco.cfg.GeneralConfig;
import com.nucco.lib.Pager;
import com.nucco.lib.StringEx;
import com.nucco.lib.db.DBLibrary;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class Sales {
	/** 사이트 설정 */
	private GlobalConfig cfg;
	/** org.apache.log4j.Logger*/
	private Logger logger = null;
	/** 소속 */
	public ArrayList<GeneralConfig> company;
	/** 조직 */
	public ArrayList<GeneralConfig> organ;
	/** 설치위치 */
	public ArrayList<GeneralConfig> place;
	/** 단말기 */
	public ArrayList<GeneralConfig> terminal;	
	/** 상품 */
	public ArrayList<GeneralConfig> goods;
	/** 결제 방식 */
	public ArrayList<GeneralConfig> payment;
	/** 결제 진행 상태 */
	public ArrayList<GeneralConfig> step;
	/** 선불 */
	public ArrayList<GeneralConfig> prepay;
	/** 카드 */
	public ArrayList<GeneralConfig> card;
	/** 결과 목록 */
	public ArrayList<GeneralConfig> list;
	/** 조회 값 */
	public GeneralConfig data;
	/** 총 레코드수 */
	public long records;
	/** 총 페이지수 */
	public long pages;
	
	/**
	 * 생성자
	 */
	public Sales(GlobalConfig cfg) throws Exception {
	// set config
		this.cfg = cfg;

	// set log4j
		PropertyConfigurator.configure(cfg.get("config.log4j"));
		this.logger = Logger.getLogger(this.getClass());
	}

	/**
	 * 매출현황
	 *
	 * @param mode 유형
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String report(int mode) throws Exception {
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
		this.company = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT" + (this.cfg.getLong("user.company") > 0L ? " /*+ INDEX(A) */" : "")
							+ " SEQ,"
							+ " NAME,"
							+ " IS_RAW_SALESCOUNT,"
							+ " ("
								+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
										+ " NVL(MAX(DEPTH), 0)"
									+ " FROM TB_ORGANIZATION"
									+ " WHERE COMPANY_SEQ = A.SEQ"
							+ " ) AS DEPTH"
						+ " FROM TB_COMPANY A"
				+ (this.cfg.getLong("user.company") > 0L
						? " WHERE SEQ = ?"
						: ""
					)
						+ " ORDER BY NAME"
				);

			if (this.cfg.getLong("user.company") > 0L) {
				ps.setLong(1, this.cfg.getLong("user.company"));
			}

			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("NAME", rs.getString("NAME"));
				c.put("IS_RAW_SALESCOUNT", rs.getString("IS_RAW_SALESCOUNT"));
				c.put("DEPTH", rs.getInt("DEPTH"));

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

	// 조직 및 설치 위치
		if (this.cfg.getLong("user.company") > 0) {
		// 최상위 조직 타이틀
			this.data = new GeneralConfig();

			try {
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(TB_ORGANIZATION) */"
								+ " NAME"
							+ " FROM TB_ORGANIZATION"
							+ " WHERE COMPANY_SEQ = ?"	// 1:user.company
								+ " AND DEPTH = 0"
								+ " AND SORT = 0"
								+ " AND ROWNUM = 1"
					);
				ps.setLong(1, this.cfg.getLong("user.company"));
				rs = ps.executeQuery();

				if (rs.next()) {
					this.data.put("TITLE", rs.getString("NAME"));
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

		// 최상위 조직 목록
		// :: 내가 속한 조직의 최상위 단계 및 나에게 조회가 허락된 조직의 최상위 단계
		// :: 자판기 운영자일 경우, 과거 조직 중 매출이 발생한 조직 추가
			this.organ = new ArrayList<GeneralConfig>();

			try {
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(TB_ORGANIZATION) */"
								+ " SEQ,"
								+ " NAME"
							+ " FROM TB_ORGANIZATION"
							+ " WHERE DEPTH = 0"
								+ " AND SORT = 1"
				+ (this.cfg.getLong("user.organ") > 0L
					// 조직이 정해진 일반관리자, 자판기 운영자
						? 	" START WITH COMPANY_SEQ = ?"				// 1:user.company
								+ " AND SEQ IN ("
										+ " SELECT ? FROM DUAL"			// 2:user.organ
										+ " UNION"
										+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
												+ " ORGANIZATION_SEQ"
											+ " FROM TB_USER_APP_ORGAN"
											+ " WHERE SEQ = ?"		// 3:user.seq
								+ (this.cfg.get("user.operator").equals("Y")
									// 자판기 운영자
										? " UNION"
										+ " SELECT /*+ INDEX(TB_VENDING_MACHINE_HISTORY) */"
												+ " ORGANIZATION_SEQ"
											+ " FROM TB_VENDING_MACHINE_HISTORY"
											+ " WHERE COMPANY_SEQ = ?"	// 4:user.company
												+ " AND USER_SEQ = ?"	// 5:user.seq
											+ " GROUP BY ORGANIZATION_SEQ"
									// 조직이 정해진 일반관리자
										: ""
									)
									+ " )"
							+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
							+ " GROUP BY NAME, SEQ"
						: 		" AND COMPANY_SEQ = ?"					// 1:user.company
					)
							+ " ORDER BY NAME, SEQ"
					);
				ps.setLong(1, this.cfg.getLong("user.company"));
				if (this.cfg.getLong("user.organ") > 0) {
					ps.setLong(2, this.cfg.getLong("user.organ"));
					ps.setLong(3, this.cfg.getLong("user.seq"));
					if (this.cfg.get("user.operator").equals("Y")) {
						ps.setLong(4, this.cfg.getLong("user.company"));
						ps.setLong(5, this.cfg.getLong("user.seq"));
					}
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

			if (!StringEx.isEmpty(error)) {
				dbLib.close(conn);
				return error;
			}

		// 설치 위치
			this.place = new ArrayList<GeneralConfig>();
			
			try {
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ ORDERED INDEX(A) USE_NL(B C) */"
								+ " B.SEQ,"
								+ " B.PLACE,"
								+ " B.END_DATE"
							+ " FROM TB_VENDING_MACHINE_HISTORY A"
								+ " INNER JOIN TB_VENDING_MACHINE_PLACE B"
									+ " ON A.VM_SEQ = B.VM_SEQ"
										+ " AND ( A.END_DATE IS NULL OR A.END_DATE <= B.END_DATE )"  //20180309 jwhwang
								+ " INNER JOIN TB_VENDING_MACHINE C"
									+ " ON A.VM_SEQ = C.SEQ"
							+ " WHERE A.COMPANY_SEQ = ?"						// 1:user.company
						//+ (this.cfg.get("user.operator").equals("Y")
							// 자판기 운영자
						//		? " AND A.USER_SEQ = ?"							// 2:user.seq
							// 시스템관리자, 일반관리자
						//		: " AND A.ORGANIZATION_SEQ IN ("
								+ " AND A.ORGANIZATION_SEQ IN ("
										+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
												+ " SEQ"
											+ " FROM TB_ORGANIZATION"
											+ " WHERE SEQ = ?"					// 2:user.organ
										+ " UNION"
										+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
												+ " ORGANIZATION_SEQ"
											+ " FROM TB_USER_APP_ORGAN"
											+ " WHERE SEQ = ?"					// 3:user.seq
										+ " UNION"
										+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
												+ " SEQ"
											+ " FROM TB_ORGANIZATION"
											+ " WHERE SORT = 1"
											+ " START WITH COMPANY_SEQ = ?"		// 4:user.company
												+ " AND PARENT_SEQ IN ("
														+ " SELECT ? FROM DUAL"	// 5:user.organ
														+ " UNION"
														+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
																+ " ORGANIZATION_SEQ"
															+ " FROM TB_USER_APP_ORGAN"
															+ " WHERE SEQ = ?"	// 6:user.seq
													+" )"
											+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
									+ " )"
							//)
							+ " GROUP BY B.PLACE, B.SEQ, B.END_DATE"
							+ " ORDER BY B.PLACE, B.SEQ"
					);
				ps.setLong(1, this.cfg.getLong("user.company"));
				//if (this.cfg.get("user.operator").equals("Y")) {
				//	ps.setLong(2, this.cfg.getLong("user.seq"));
				//} else {
					ps.setLong(2, this.cfg.getLong("user.organ"));
					ps.setLong(3, this.cfg.getLong("user.seq"));
					ps.setLong(4, this.cfg.getLong("user.company"));
					ps.setLong(5, this.cfg.getLong("user.organ"));
					ps.setLong(6, this.cfg.getLong("user.seq"));
				//}
				rs = ps.executeQuery();

				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();

					c.put("SEQ", rs.getLong("SEQ"));
		
				//20160113 설치장소를 [삭제날짜]+설치장소로 변경
				//	c.put("PLACE", rs.getString("PLACE"));
					String endDate = rs.getString("END_DATE");
					c.put("PLACE", (endDate == null ? "" : "[" + endDate + "] ") + rs.getString("PLACE"));

					this.place.add(c);
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

	// 유형별 추가 정보
		switch (mode) {
		case 2:
			// 결제 진행 상태
				this.step = new ArrayList<GeneralConfig>();

				try {
					ps = dbLib.prepareStatement(conn,
							"SELECT /*+ INDEX(TB_CODE) */"
									+ " CODE,"
									+ " NAME"
								+ " FROM TB_CODE"
								+ " WHERE TYPE = 'PAY_STEP'"
									+ " AND CODE <> '000'"
								+ " ORDER BY CODE"
						);
					rs = ps.executeQuery();

					while (rs.next()) {
						GeneralConfig c = new GeneralConfig();

						c.put("CODE", rs.getString("CODE"));
						c.put("NAME", rs.getString("NAME"));

						this.step.add(c);
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

		case 1 :
			// 결제 방식
				this.payment = new ArrayList<GeneralConfig>();

				try {
					ps = dbLib.prepareStatement(conn,
							"SELECT /*+ INDEX(TB_CODE) */"
									+ " CODE,"
									+ " NAME"
								+ " FROM TB_CODE"
								+ " WHERE TYPE = 'PAY_TYPE'"
									+ " AND CODE <> '000'"
								+ " ORDER BY CODE"
						);
					rs = ps.executeQuery();

					while (rs.next()) {
						GeneralConfig c = new GeneralConfig();

						c.put("CODE", rs.getString("CODE"));
						c.put("NAME", rs.getString("NAME"));

						this.payment.add(c);
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
	 * 하부조직
	 *
	 * @param organ 조직
	 * @param depth 깊이
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String organ(long organ, int depth) throws Exception {
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

	// 하위 조직 타이틀
		this.data = new GeneralConfig();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(TB_ORGANIZATION) */"
							+ " NAME"
						+ " FROM TB_ORGANIZATION"
						+ " WHERE COMPANY_SEQ = ?"	// 1:user.company
							+ " AND DEPTH = ?"
							+ " AND SORT = 0"
							+ " AND ROWNUM = 1"
				);
			ps.setLong(1, this.cfg.getLong("user.company"));
			ps.setLong(2, depth);
			rs = ps.executeQuery();

			if (rs.next()) {
				this.data.put("DISPLAY", rs.getString("NAME"));
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 하위 조직 검색
	// :: 내가 속한 조직 및 나에게 조회가 허락된 조직에서의 트리
	// :: 자판기 운영자일 경우, 과거 조직 중 매출이 발생한 조직 추가
		this.organ = new ArrayList<GeneralConfig>();
		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(TB_ORGANIZATION) */"
							+ " SEQ,"
							+ " NAME || CASE WHEN IS_ENABLED = 'N' THEN '(' || NVL(SUBSTR(SUM_DATE, 3, 6), '000000') || ')' ELSE '' END AS NAME"
						+ " FROM TB_ORGANIZATION"
			+ (this.cfg.getLong("user.organ") > 0
				// 조직이 지정된 일반관리자, 자판기 운영자
					?	" WHERE DEPTH = ?"											// 1:depth
							+ " AND SORT = 1"
							+ " AND SEQ IN ("
									+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
											+ " SEQ"
											+ " FROM TB_ORGANIZATION"
										+ " WHERE DEPTH = ?"						// 2:depth
											+ " AND SORT = 1"
										+ " START WITH COMPANY_SEQ = ?"				// 3:user.company
											+ " AND SEQ IN ("
													+ " SELECT ? FROM DUAL"			// 4:user.organ
													+ " UNION"
													+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
															+ " ORGANIZATION_SEQ"
														+ " FROM TB_USER_APP_ORGAN"
														+ " WHERE SEQ = ?"			// 5:user.seq
														+ " GROUP BY ORGANIZATION_SEQ"
								+ (this.cfg.get("user.operator").equals("Y")
									// 자판기 운영자
										?			" UNION"
													+ " SELECT /*+ INDEX(TB_VENDING_MACHINE_HISTORY) */"
															+ " ORGANIZATION_SEQ"
														+ " FROM TB_VENDING_MACHINE_HISTORY"
														+ " WHERE COMPANY_SEQ = ?"	// 6:user.company
															+ " AND USER_SEQ = ?"	// 7:user.seq
														+ " GROUP BY ORGANIZATION_SEQ"
									// 조직이 지정된 일반관리자
										: ""
									)
												+ " )"
										+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
									+ " UNION"
									+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
											+ " SEQ"
										+ " FROM TB_ORGANIZATION"
										+ " WHERE DEPTH = ?"						// 8, 6:depth
											+ " AND SORT = 1"
							+ (this.cfg.get("user.operator").equals("Y")
								// 자판기 운영자
									?		" AND COMPANY_SEQ = ?"					// 9:user.company
											+ " AND PARENT_SEQ = ?"					// 10:user.organ
								// 조직이 지정된 일반관리자
									:	" START WITH COMPANY_SEQ = ?"				// 7:user.company
											+ " AND PARENT_SEQ IN ("
													+ " SELECT ? FROM DUAL"			// 8:user.organ
													+ " UNION"
													+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
															+ " ORGANIZATION_SEQ"
														+ " FROM TB_USER_APP_ORGAN"
														+ " WHERE SEQ = ?"			// 9:user.seq
														+ " GROUP BY ORGANIZATION_SEQ"
												+ " )"
										+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
								)
								+ " )"
						+ " START WITH PARENT_SEQ = ?"								// 11, 10:organ
						+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
				// 시스템관리자, 조직이 지정되지 않은 일반관리자
					:	" WHERE PARENT_SEQ = ?"										// 1:organ
							+ " AND SORT = 1"
					+ (this.cfg.getLong("user.company") > 0
							? " AND COMPANY_SEQ = ?"								// 2:user.company
							: ""
						)
				)
						+ " ORDER BY NAME, SEQ"
				);
			
			if (this.cfg.getLong("user.organ") > 0) {
				ps.setInt(1, depth);
				ps.setInt(2, depth);
				ps.setLong(3, this.cfg.getLong("user.company"));
				ps.setLong(4, this.cfg.getLong("user.organ"));
				ps.setLong(5, this.cfg.getLong("user.seq"));
				if (this.cfg.get("user.operator").equals("Y")) {
					ps.setLong(6, this.cfg.getLong("user.company"));
					ps.setLong(7, this.cfg.getLong("user.seq"));
					ps.setInt(8, depth);
					ps.setLong(9, this.cfg.getLong("user.company"));
					//ps.setLong(10, this.cfg.getLong("user.organ")); //20170822 jwhwang 하위조직 거맥안됨 수정
					ps.setLong(10, organ);
					ps.setLong(11, organ);
				} else {
					ps.setInt(6, depth);
					ps.setLong(7, this.cfg.getLong("user.company"));
					ps.setLong(8, this.cfg.getLong("user.organ"));
					ps.setLong(9, this.cfg.getLong("user.seq"));
					ps.setLong(10, organ);
				}
			} else {
				ps.setLong(1, organ);
				if (this.cfg.getLong("user.company") > 0) {
					ps.setLong(2, this.cfg.getLong("user.company"));
				}
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

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}

	// 설치 위치
		error = this.place(organ);

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
	 * 설치위치
	 *
	 * @param organ 조직
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String place(long organ) throws Exception {
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

	// 검색
		this.place = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED INDEX(A) USE_NL(B C) */"
							+ " B.SEQ,"
							+ " B.PLACE,"
							+ " B.END_DATE"
						+ " FROM TB_VENDING_MACHINE_HISTORY A"
							+ " INNER JOIN TB_VENDING_MACHINE_PLACE B"
								+ " ON A.VM_SEQ = B.VM_SEQ"
									+ " AND ( A.END_DATE IS NULL OR A.END_DATE <= B.END_DATE )" //20180309 jwhwang
							+ " INNER JOIN TB_VENDING_MACHINE C"
								+ " ON A.VM_SEQ = C.SEQ"
						+ " WHERE A.ORGANIZATION_SEQ IN ("
									+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
											+ " SEQ"
										+ " FROM TB_ORGANIZATION"
										+ " WHERE SEQ = ?"					// 1:organ
									+ " UNION"
									+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
											+ " SEQ"
										+ " FROM TB_ORGANIZATION"
										+ " WHERE SORT = 1"
										+ " START WITH PARENT_SEQ = ?"		// 2:organ
										+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
								+ " )"
					//+ (this.cfg.get("user.operator").equals("Y")
						// 자판기 운영자
					//		? " AND A.USER_SEQ = ?"							// 3:user.seq
						// 시스템관리자, 일반관리자
					//		: " AND A.ORGANIZATION_SEQ IN ("
								+ " AND A.ORGANIZATION_SEQ IN ("
									+ " SELECT	/*+ INDEX(TB_ORGANIZATION) */"
											+ " SEQ"
										+ " FROM TB_ORGANIZATION"
										+ " WHERE SEQ = ?"					// 3:user.organ
									+ " UNION"
									+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
											+ " ORGANIZATION_SEQ"
										+ " FROM TB_USER_APP_ORGAN"
										+ " WHERE SEQ = ?"					// 4:user.seq
									+ " UNION"
									+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
											+ " SEQ"
										+ " FROM TB_ORGANIZATION"
										+ " WHERE SORT = 1"
										+ " START WITH PARENT_SEQ IN ("
													+ " SELECT ? FROM DUAL"	// 5:user.organ
													+ " UNION"
													+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
															+ " ORGANIZATION_SEQ"
														+ " FROM TB_USER_APP_ORGAN"
														+ " WHERE SEQ = ?"	// 6:user.seq
														+ " GROUP BY ORGANIZATION_SEQ"
												+ " )"
									+ (this.cfg.getLong("user.company") > 0
											? " AND COMPANY_SEQ = ?"		// 7:user.company
											: ""
										)
										+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
								+ ")"
						//)
					+ (this.cfg.getLong("user.company") > 0
							? " AND A.COMPANY_SEQ = ?"						// 4, 8:user.company
							: ""
						)
						+ " GROUP BY B.PLACE, B.SEQ, B.END_DATE"
						+ " ORDER BY B.PLACE, B.SEQ"
				);

			ps.setLong(1, organ);
			ps.setLong(2, organ);
			//if (this.cfg.get("user.operator").equals("Y")) {
			//	ps.setLong(3, this.cfg.getLong("user.seq"));
			//	if (this.cfg.getLong("user.company") > 0) {
			//		ps.setLong(4, this.cfg.getLong("user.company"));
			//	}
			//} else {
				ps.setLong(3, this.cfg.getLong("user.organ"));
				ps.setLong(4, this.cfg.getLong("user.seq"));
				ps.setLong(5, this.cfg.getLong("user.organ"));
				ps.setLong(6, this.cfg.getLong("user.seq"));
				if (this.cfg.getLong("user.company") > 0) {
					ps.setLong(7, this.cfg.getLong("user.company"));
					ps.setLong(8, this.cfg.getLong("user.company"));
				}
			//}

			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
			//20160113 설치장소를 [삭제날짜]+설치장소로 변경
			//	c.put("PLACE", rs.getString("PLACE"));
				String endDate = rs.getString("END_DATE");
				c.put("PLACE", (endDate == null ? "" : "[" + endDate + "] ") + rs.getString("PLACE"));

				this.place.add(c);
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
	 * 일별 매출집계
	 *
	 * @param sType 집계 유형
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 시작일
	 * @param eDate 종료일
	 * @param payTypes 결제유형
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String daily(String sType, long company, long organ,long depth, long place, String sDate, String eDate, String[] payTypes, int oMode, int oType) throws Exception {
	// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sType) || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
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
			return "DB 연결에 실패하였습니다.";
		}

	// 검색절 생성
		String WHERE = "";
		String YYYYMMDD
				= "01".equals(sType) ? "TRANSACTION_DATE"	// 거래일
				: "02".equals(sType) ? "CLOSING_DATE"		// 마감일
				: "03".equals(sType) ? "PAY_DATE"			// 입금일
				: "04".equals(sType) ? "PURCHASE_DATE"		// 매입청구일
				: "05".equals(sType) ? "PAY_DATE_EXP"		// 입금예정일
				: "CANCEL_DATE"								// 취소일/마감일(취소)/환수일/매입취소일/환수예정일 - 미사용
			;
		//System.out.println(this.cfg.getLong("user.organ"));
		//System.out.println(organ);		
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 
			company = (company > 0 ? company : this.cfg.getLong("user.company"));
			if (company > 0) { // 소속
				WHERE += " AND A.COMPANY_SEQ = " + company;
			} 
			
			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			
			if (organ > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
							//20160121 타소속의 조직이 검색되지 않도록 COMPANY_SEQ 조건 추가
								+ " START WITH COMPANY_SEQ = " + company
									+ " AND SEQ IN ("
											+ " SELECT " + organ + " FROM DUAL"
											+ " UNION"
											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
													+ " ORGANIZATION_SEQ"
												+ " FROM TB_USER_APP_ORGAN"
												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
										+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
					;
			}
			
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			//organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			//organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ")!=organ ? this.cfg.getLong("user.organ"): organ : organ);
			organ = (organ <= 0 ? this.cfg.getLong("user.organ") : (this.cfg.getInt("user.organ.depth") > depth ? this.cfg.getLong("user.organ") : organ));
			if (organ > 0) { // 조직
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
								+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ ")"
					; 
			} 
		}

		
// 2017.06.28 jwhwang		
//		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
//			WHERE += " AND USER_SEQ = " + this.cfg.getLong("user.seq");
//		} else {
//			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
//				WHERE += " AND COMPANY_SEQ = " + this.cfg.getLong("user.company");
//			}
//
//			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
//				WHERE += " AND ORGANIZATION_SEQ IN ("
//							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//									+ " SEQ"
//								+ " FROM TB_ORGANIZATION"
//								+ " WHERE SORT = 1"
//							//20160121 타소속의 조직이 검색되지 않도록 COMPANY_SEQ 조건 추가
//								+ " START WITH COMPANY_SEQ = " + this.cfg.getLong("user.company")
//									+ " AND SEQ IN ("
//											+ " SELECT " + this.cfg.getLong("user.organ") + " FROM DUAL"
//											+ " UNION"
//											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
//													+ " ORGANIZATION_SEQ"
//												+ " FROM TB_USER_APP_ORGAN"
//												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
//										+ " )"
//								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//						+ " )"
//					;
//			}
//		}
//
//		if (company > 0) { // 소속
//			WHERE += " AND COMPANY_SEQ = " + company;
//		}
//
//		if (organ > 0) { // 조직
//			WHERE += " AND ORGANIZATION_SEQ IN ("
//						+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//								+ " SEQ"
//							+ " FROM TB_ORGANIZATION"
//							+ " WHERE SORT = 1"
//							+ " START WITH SEQ = " + organ
//							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//					+ " )"
//				;
//		}

		if (place > 0) { // 위치
			WHERE += " AND VM_PLACE_SEQ = " + place;
		}

	//20160614 결제유형 다중선택 추가
		if ((payTypes != null) && (payTypes.length > 0)) {
			WHERE += " AND PAY_TYPE IN ('" + payTypes[0] + "'";
			for (int i = 1; i < payTypes.length; i++) WHERE += ", '" + payTypes[i] + "'";
			WHERE += ")";
	//20170329 키갱신 제외
		} else {
			WHERE += " AND A.PAY_TYPE <> '09'";
		}

		String SUM_OF_EACH_PREPAY_COMPANY_1 = "";
		String SUM_OF_EACH_PREPAY_COMPANY_2 = "";
		String[] prepaySummaryColumns = new String[] {};
		ArrayList<String> prepaySummaryColumnList = new ArrayList<String>();

	// 선불 카드사
		this.company = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(TB_CODE) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE"
						+ " WHERE TYPE = 'PAY_CARD'"
							+ " AND CODE <> '000'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				String code = rs.getString("CODE");
				c.put("CODE", code);
				c.put("NAME", rs.getString("NAME"));

				this.company.add(c);

				SUM_OF_EACH_PREPAY_COMPANY_1 += " COUNT(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN 1 END) AS CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN AMOUNT END), 0) AS AMOUNT_PREPAY_" + code + ","
					;
				SUM_OF_EACH_PREPAY_COMPANY_2 += " COUNT(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN 1 END) AS CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN -AMOUNT END), 0) AS AMOUNT_PREPAY_" + code + ","
					;
				prepaySummaryColumnList.add("CNT_PREPAY_" + code);
				prepaySummaryColumnList.add("AMOUNT_PREPAY_" + code);
			}

			prepaySummaryColumns = prepaySummaryColumnList.toArray(new String[prepaySummaryColumnList.size()]);
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			prepaySummaryColumnList = null;

			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();

		try {
			long[] cnt = {0, 0, 0, 0, 0, 0};	//scheo 2018.10.10 추가
			long[] amt = {0, 0, 0, 0, 0, 0};	//scheo 2018.10.10 추가

			ps = dbLib.prepareStatement(conn,
					" SELECT /*+ ORDERED USE_HASH(B C D) */"
							+ " A.*,"
							+ " B.NAME AS ORGAN,"
							+ " B.CODE AS ORGAN_CODE," //2017.12.21 jwhwang 추가
							+ " C.PLACE AS PLACE,"
							+ " D.CODE AS VM_CODE"
						+ " FROM ("
									+ " SELECT /*+ INDEX(A IX_SALES_" + YYYYMMDD + ") */"
											+ " " + YYYYMMDD + " AS YYYYMMDD,"
											+ " COMPANY_SEQ,"
											+ " ORGANIZATION_SEQ,"
											+ " VM_PLACE_SEQ,"
											+ " COUNT(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN AMOUNT END), 0) AS AMOUNT_CARD,"
											+ " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO,"	//scheo 2018.10.10 추가
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN AMOUNT END), 0) AS AMOUNT_PAYCO,"	//scheo 2018.10.10 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '80' THEN 1 END) AS CNT_ITEM,"	//scheo 2021.09.06 추가
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '80' THEN AMOUNT END), 0) AS AMOUNT_ITEM,"	//scheo 2021.09.06 추가
											+ " COUNT(CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN AMOUNT END), 0) AS AMOUNT_CASH,"
											+ SUM_OF_EACH_PREPAY_COMPANY_1
											+ " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN AMOUNT END), 0) AS AMOUNT_PREPAY"
										+ " FROM TB_SALES A"
										+ " WHERE " + YYYYMMDD + " BETWEEN '"+ sDate + "' AND '" + eDate + "'"
											+ " AND PAY_STEP NOT IN ('00', '99')"
											+ WHERE
										+ " GROUP BY " + YYYYMMDD + ", COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ"
									+ " UNION ALL"
									+ " SELECT"
											+ " YYYYMMDD,"
											+ " COMPANY_SEQ,"
											+ " ORGANIZATION_SEQ,"
											+ " VM_PLACE_SEQ,"
											+ " COUNT(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN -AMOUNT END), 0) AS AMOUNT_CARD,"
											+ " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO,"	//scheo 2018.10.10 추가
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN -AMOUNT END), 0) AS AMOUNT_PAYCO,"	//scheo 2018.10.10 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '80' THEN 1 END) AS CNT_ITEM,"	//scheo 2021.09.06 추가
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '80' THEN -AMOUNT END), 0) AS AMOUNT_ITEM,"	//scheo 2021.09.06 추가
											+ " COUNT(CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN -AMOUNT END), 0) AS AMOUNT_CASH,"
											+ SUM_OF_EACH_PREPAY_COMPANY_2
											+ " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN -AMOUNT END), 0) AS AMOUNT_PREPAY"
										+ " FROM ("
												+ " SELECT /*+ INDEX(A IX_SALES_PAY_STEP) */"
										+ (
											// 마감일(취소)
												"02".equals(sType) ? (
														" CASE WHEN (CANCEL_DATE || CANCEL_TIME) < (CLOSING_DATE || CLOSING_TIME)"
															+ " THEN CLOSING_DATE"
															+ " ELSE ("
																	+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																			+ " SUBSTR(MIN(CLOSING_DATE || CLOSING_TIME), 1, 8)"
																		+ " FROM TB_TXT_CLOSING"
																		+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																			+ " AND (CLOSING_DATE || CLOSING_TIME) >= (A.CANCEL_DATE || A.CANCEL_TIME)"
																+ " )"
															+ " END AS YYYYMMDD,"
													) :
											// 환수일/환수예정일
												"03".equals(sType) || "05".equals(sType) ? (
														" F_GET_WORKDAY@VANBT(CANCEL_DATE, 5) AS YYYYMMDD,"
													) :
											// 매입취소일
												"04".equals(sType) ? (
														" F_GET_WORKDAY@VANBT(CANCEL_DATE, 1) AS YYYYMMDD,"
													) :
											// 취소일
												(
														" CANCEL_DATE AS YYYYMMDD,"
													)
											)
														+ " A.*"
													+ " FROM TB_SALES A"
													+ " WHERE PAY_STEP = '29'"
														+ " AND " + YYYYMMDD + " IS NOT NULL"
														+ WHERE
										+ (
											// 마감일(취소)
												"02".equals(sType) ? (
														" AND (CANCEL_DATE || CANCEL_TIME) BETWEEN ("
																+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																		+ " MAX(CLOSING_DATE || CLOSING_TIME) || '0'"
																	+ " FROM TB_TXT_CLOSING"
																	+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																		+ " AND (CLOSING_DATE || CLOSING_TIME) < '" + sDate + "'"
															+ " ) AND ("
																+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																		+ " MAX(CLOSING_DATE || CLOSING_TIME)"
																	+ " FROM TB_TXT_CLOSING"
																	+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																	+ " AND (CLOSING_DATE || CLOSING_TIME) <= '" + eDate + "235959'"
															+ " )"
														+ " AND CLOSING_DATE <= '" + eDate + "'"
														+ " AND CANCEL_DATE <= '" + eDate + "'"
													) :
											// 환수일/환수예정일
												"03".equals(sType) || "05".equals(sType) ? (
														" AND CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -5) AND F_GET_WORKDAY@VANBT('" + eDate + "', -5)"
													) :
											// 매입취소일
												"04".equals(sType) ? (
														" AND CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -1) AND F_GET_WORKDAY@VANBT('" + eDate + "', -1)"
													) :
											// 취소일
												(
														" AND CANCEL_DATE BETWEEN '" + sDate + "' AND '" + eDate + "'"
													)
											)
											+ " ) A"
										+ " WHERE YYYYMMDD BETWEEN '" + sDate + "' AND '" + eDate + "'"
										+ " GROUP BY YYYYMMDD, COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ"
								+ " ) A"
							+ " INNER JOIN TB_ORGANIZATION B"
								+ " ON A.ORGANIZATION_SEQ = B.SEQ"
							+ " INNER JOIN TB_VENDING_MACHINE_PLACE C"
								+ " ON A.VM_PLACE_SEQ = C.SEQ"
							+ " INNER JOIN TB_VENDING_MACHINE D"
								+ " ON C.VM_SEQ = D.SEQ"
						+ " ORDER BY"
// 정렬 값(oMode, oType)에 따라 ORDER BY 변경
	+ ((oType == 1) ? (	// DESC
				// 거래처
					(oMode == 1) ? (
							" ORGAN DESC, YYYYMMDD DESC, PLACE DESC"
						) :
				// 설치장소
					(oMode == 2) ? (
							" ORGAN DESC, PLACE DESC, YYYYMMDD DESC"
						) :
				// default: 날짜
					(
							" YYYYMMDD DESC, ORGAN, PLACE"
						)
			) : (	// ASC
				// 거래처
					(oMode == 1) ? (
							" ORGAN, YYYYMMDD, PLACE"
						) :

				// 설치장소
					(oMode == 2) ? (
							" ORGAN, PLACE, YYYYMMDD"
						) :
				// default: 날짜
					(
							" YYYYMMDD, ORGAN, PLACE"
						)
				)
		)
							+ ", VM_CODE, A.AMOUNT_CARD DESC"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				String yyyymmdd = rs.getString("YYYYMMDD");
				c.put("YYYYMMDD", yyyymmdd);
				c.put("DATE", yyyymmdd.substring(0, 4) + "-" + yyyymmdd.substring(4, 6) + "-" + yyyymmdd.substring(6, 8));

				c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("VM_PLACE_SEQ", rs.getLong("VM_PLACE_SEQ"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("ORGAN_CODE", rs.getString("ORGAN_CODE")); //2017.12.21 jwhwang 추가
				c.put("PLACE", rs.getString("PLACE"));
				c.put("VM_CODE", rs.getString("VM_CODE"));
				c.put("CNT_CARD", rs.getLong("CNT_CARD"));
				c.put("AMOUNT_CARD", rs.getLong("AMOUNT_CARD"));
				c.put("CNT_PAYCO", rs.getLong("CNT_PAYCO"));	//scheo 2018.10.10 추가
				c.put("AMOUNT_PAYCO", rs.getLong("AMOUNT_PAYCO"));	//scheo 2018.10.10 추가
				c.put("CNT_ITEM", rs.getLong("CNT_ITEM"));	//scheo 2021.09.06 추가
				c.put("AMOUNT_ITEM", rs.getLong("AMOUNT_ITEM"));	//scheo 2021.09.06 추가
				c.put("CNT_CASH", rs.getLong("CNT_CASH"));
				c.put("AMOUNT_CASH", rs.getLong("AMOUNT_CASH"));
				c.put("CNT_PREPAY", rs.getLong("CNT_PREPAY"));
				c.put("AMOUNT_PREPAY", rs.getLong("AMOUNT_PREPAY"));

				c.put("CNT_TOTAL", c.getLong("CNT_CARD") + c.getLong("CNT_PAYCO") + c.getLong("CNT_CASH") + c.getLong("CNT_PREPAY"));	//scheo 2018.10.10 추가
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_CARD") + c.getLong("AMOUNT_PAYCO") + c.getLong("AMOUNT_CASH") + c.getLong("AMOUNT_PREPAY"));	//scheo 2018.10.10 추가

				for (String columnName : prepaySummaryColumns) {
					c.put(columnName, rs.getLong(columnName));
					this.data.put(columnName, this.data.getLong(columnName) + rs.getLong(columnName));
				}

				this.list.add(c);

				cnt[0] += c.getLong("CNT_CARD");
				cnt[1] += c.getLong("CNT_PAYCO");	//scheo 2018.10.10 추가
				cnt[2] += c.getLong("CNT_ITEM");	//scheo 2021.09.06 추가
				cnt[3] += c.getLong("CNT_CASH");
				cnt[4] += c.getLong("CNT_PREPAY");
				cnt[5] += c.getLong("CNT_TOTAL");
				amt[0] += c.getLong("AMOUNT_CARD");
				amt[1] += c.getLong("AMOUNT_PAYCO");	//scheo 2018.10.10 추가
				amt[2] += c.getLong("AMOUNT_ITEM");	//scheo 2021.09.06 추가
				amt[3] += c.getLong("AMOUNT_CASH");
				amt[4] += c.getLong("AMOUNT_PREPAY");
				amt[5] += c.getLong("AMOUNT_TOTAL");
			}

			this.data.put("CNT_CARD", cnt[0]);
			this.data.put("CNT_PAYCO", cnt[1]);	//scheo 2018.10.10 추가
			this.data.put("CNT_ITEM", cnt[2]);	//scheo 2021.09.06 추가
			this.data.put("CNT_CASH", cnt[3]);
			this.data.put("CNT_PREPAY", cnt[4]);
			this.data.put("CNT_TOTAL", cnt[5]);
			this.data.put("AMOUNT_CARD", amt[0]);
			this.data.put("AMOUNT_PAYCO", amt[1]);	//scheo 2018.10.10 추가
			this.data.put("AMOUNT_ITEM", amt[2]);	//scheo 2021.09.06 추가
			this.data.put("AMOUNT_CASH", amt[3]);
			this.data.put("AMOUNT_PREPAY", amt[4]);
			this.data.put("AMOUNT_TOTAL", amt[5]);
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 조직명
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
						+ " FROM ("
								+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
										+ " NAME"
									+ " FROM TB_ORGANIZATION"
									+ " WHERE COMPANY_SEQ = " + company
										+ " AND SORT = 0"
									+ " ORDER BY DEPTH DESC"
							+ " )"
						+ " WHERE ROWNUM = 1"
					)
			);

	// 검색 설정
		String sDesc = "집계기준="
				+ dbLib.getResult(conn,
						"SELECT /*+ INDEX(TB_CODE) */"
								+ " NAME"
							+ " FROM TB_CODE"
							+ " WHERE TYPE = 'SUM_TYPE'"
								+ " AND CODE = '" + sType + "'"
					)
				+ "&집계기간=" + sDate + "-" + eDate
			;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}

		this.data.put("sDesc", sDesc);

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
	
	
	/**
	 * 거래내역 상세보기
	 * 2020-07-10 scheo 추가
	 *
	 * @param mode 유형
	 * @return 에러가 있을 경우 에러 내용
	 * @
	 *
	 */
	public String Detail_Normal(String TERMINAL_ID, String TRANSACTION_NO) throws Exception {
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
		
		this.list = new ArrayList<GeneralConfig>();
		
		try {
			ps = dbLib.prepareStatement(conn,
				
			"SELECT "
			    +"DECODE(S.PAY_TYPE, '01', C.CARD_NO, '02', C.CARD_NO, '10', C.CARD_NO, '07', C.CARD_NO, '11', P.CARD_NO, '') CARD_NO, " 
			    +"DECODE(S.PAY_TYPE, '01', C.PURCHASE_ORGAN_NAME, '02', '현금', '10', '현금', '07', C.PURCHASE_ORGAN_NAME, '11', " 
			        +"DECODE(P.ORGAN_CODE, 'TMN', '티머니', 'CSB', '캐시비', 'MYB', '마이비', 'KRP', '레일플러스', '선불'), '카드') PURCHASE_ORGAN_NAME, "
			    +"DECODE(S.PAY_TYPE, '01', C.APPROVAL_NO, '02', C.APPROVAL_NO, '07', C.APPROVAL_NO, '') APPROVAL_NO, "
			    +"DECODE(S.PAY_TYPE, '01', C.TERMINAL_TRANS_SEQ, '02', C.TERMINAL_TRANS_SEQ, '10', C.TERMINAL_TRANS_SEQ, '07', C.TERMINAL_TRANS_SEQ, '11', P.TERMINAL_TRANS_SEQ) TERMINAL_TRANS_SEQ, "
			    +"NVL(S.CLOSING_DATE,'-') AS CLOSING_DATE "
			+"FROM TB_SALES S "
			    +"LEFT OUTER JOIN TB_TXT_TRANSACTION_CREDIT C "
			        +"ON (S.TERMINAL_ID = C.TERMINAL_ID AND S.TRANSACTION_NO = C.TRANSACTION_NO) "
			    +"LEFT OUTER JOIN TB_TXT_TRANSACTION_PREPAY P "
			        +"ON (S.TERMINAL_ID = P.TERMINAL_ID AND S.TRANSACTION_NO = P.TRANSACTION_NO) "
			+"WHERE S.TERMINAL_ID = '"+TERMINAL_ID+"' AND S.TRANSACTION_NO = '"+TRANSACTION_NO+"' "

			);
	
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();
				this.data = new GeneralConfig();
				
				/*쿼리에서 가져온 금액과 상품명*/
				c.put("CARD_NO", rs.getString("CARD_NO"));
				c.put("PURCHASE_ORGAN_NAME", rs.getString("PURCHASE_ORGAN_NAME"));
				c.put("APPROVAL_NO", rs.getString("APPROVAL_NO"));
				if(rs.getString("CLOSING_DATE").equals("-") || rs.getString("CLOSING_DATE")=="-") {
					c.put("TERMINAL_TRANS_SEQ", rs.getString("TERMINAL_TRANS_SEQ"));
				}else {
					c.put("TERMINAL_TRANS_SEQ","-");
				}
				this.list.add(c);
				
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
		return error;
	}
	
	
	/**
	 * 장바구니 상세보기 (확장)
	 * 2019-06-17 김태우 추가
	 * 2020-07-14 카드 번호 추가 및 쿼리, 함수 파라미터 전체 수정
	 *
	 * @param mode 유형
	 * @return 에러가 있을 경우 에러 내용
	 * @
	 *
	 */
	public String Detail_Basket(String TERMINAL_ID, String TRANSACTION_NO) throws Exception {
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
		
		this.list = new ArrayList<GeneralConfig>();
		
		try {
			ps = dbLib.prepareStatement(conn,
				"SELECT "
				    +"I.AMOUNT AS SEARCH_AMOUNT, "
				    +"NVL(T.NAME, '미등록 상품[' || I.COL_NO || ']') AS SEARCH_PRODUCT_NAME, "
				    +"DECODE(S.PAY_TYPE, '01', C.CARD_NO, '02', C.CARD_NO, '10', C.CARD_NO, '07', C.CARD_NO, '11', P.CARD_NO, '') CARD_NO, " 
				    +"DECODE(S.PAY_TYPE, '01', C.PURCHASE_ORGAN_NAME, '02', '현금', '10', '현금', '07', C.PURCHASE_ORGAN_NAME, '11', " 
				    +"DECODE(P.ORGAN_CODE, 'TMN', '티머니', 'CSB', '캐시비', 'MYB', '마이비', 'KRP', '레일플러스', '선불'), '카드') PURCHASE_ORGAN_NAME, "
				    +"DECODE(S.PAY_TYPE, '01', C.APPROVAL_NO, '02', C.APPROVAL_NO, '07', C.APPROVAL_NO, '') APPROVAL_NO, "
				    +"DECODE(S.PAY_TYPE, '01', C.TERMINAL_TRANS_SEQ, '02', C.TERMINAL_TRANS_SEQ, '10', C.TERMINAL_TRANS_SEQ, '07', C.TERMINAL_TRANS_SEQ, '11', P.TERMINAL_TRANS_SEQ) TERMINAL_TRANS_SEQ ,"
				    +"NVL(S.CLOSING_DATE,'-') AS CLOSING_DATE "
				+"FROM TB_SALES S "
				    +"LEFT OUTER JOIN TB_PURCHASE_ITEMS I "
			        	+"ON (S.TERMINAL_ID = I.TERMINAL_ID AND S.TRANSACTION_NO = I.TRANSACTION_NO) "
			        +"LEFT OUTER JOIN TB_PRODUCT T "
			        	+"ON (S.COMPANY_SEQ = T.COMPANY_SEQ AND I.PRODUCT_CODE = T.CODE) "
				    +"LEFT OUTER JOIN TB_TXT_TRANSACTION_CREDIT C "
				        +"ON (S.TERMINAL_ID = C.TERMINAL_ID AND S.TRANSACTION_NO = C.TRANSACTION_NO) "
				    +"LEFT OUTER JOIN TB_TXT_TRANSACTION_PREPAY P "
				        +"ON (S.TERMINAL_ID = P.TERMINAL_ID AND S.TRANSACTION_NO = P.TRANSACTION_NO) "
				+"WHERE I.TERMINAL_ID = '"+TERMINAL_ID+"' AND I.TRANSACTION_NO = '"+TRANSACTION_NO+"' "
/*					"SELECT " 
					+"T1.AMOUNT AS SEARCH_AMOUNT, "
					+ " NVL(T4.NAME, '미등록 상품[' || T1.COL_NO || ']') AS SEARCH_PRODUCT_NAME " //20190619 twkim326  미등록 상품 처리
					/*+"case "
				 		+"when T4.name is NULL then "
				 			+"case "
								+"when length(T1.col_no) = '2' then '미등록 상품[' || '00' || T1.col_no || ']' "
								+"when length(T1.col_no) = '1' then '미등록 상품[' || '000' || T1.col_no || ']' "
								+"when MOD(T1.col_no,100) || TRUNC(T1.col_no/100) <= '999' then '미등록 상품[' || '0' || MOD(T1.col_no,100) || TRUNC(T1.col_no/100) || ']' "
							+"else '미등록 상품[' || MOD(T1.col_no,100) || TRUNC(T1.col_no/100) || ']' "
				 		+"end "
				 +"else T4.NAME "
				 +"END AS SEARCH_PRODUCT_NAME "*/
/*			  +"FROM TB_PURCHASE_ITEMS T1 "
			    +" LEFT JOIN "
			      +" TB_VENDING_MACHINE T2 "
			    +"ON T1.TERMINAL_ID = T2.TERMINAL_ID "
			       +"LEFT JOIN "
			       +"TB_PRODUCT T4 "
			    +"ON T2.COMPANY_SEQ = T4.COMPANY_SEQ "
			   +"AND T1.PRODUCT_CODE = T4.CODE "
				+"WHERE T1.TERMINAL_ID='"+TERMINAL_ID+"'"
				+" AND "
				+"T1.TRANSACTION_NO='"+TRANSACTION_NO+"'"*/

			);
	
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();
				this.data = new GeneralConfig();
				
				/*쿼리에서 가져온 금액과 상품명*/
				c.put("SEARCH_AMOUNT", rs.getString("SEARCH_AMOUNT"));
				c.put("SEARCH_PRODUCT_NAME", rs.getString("SEARCH_PRODUCT_NAME"));
				c.put("CARD_NO", rs.getString("CARD_NO"));
				c.put("PURCHASE_ORGAN_NAME", rs.getString("PURCHASE_ORGAN_NAME"));
				c.put("APPROVAL_NO", rs.getString("APPROVAL_NO"));
				if(rs.getString("CLOSING_DATE").equals("-") || rs.getString("CLOSING_DATE")=="-")
					c.put("TERMINAL_TRANS_SEQ", rs.getString("TERMINAL_TRANS_SEQ"));
				else
					c.put("TERMINAL_TRANS_SEQ", "-");
				this.list.add(c);
				
                /*기존 파라미터를 그대로 넘김*/
//				this.data.put("TERMINAL_ID", TERMINAL_ID);
//				this.data.put("NO",NO);
//				this.data.put("TRANSACTION_DATE", TRANSACTION_DATE);
//				this.data.put("ORGAN", ORGAN);
//				this.data.put("PLACE", PLACE);
//				this.data.put("VMCODE", VMCODE);
//				this.data.put("GOODS", GOODS);
//				this.data.put("AMOUNT", AMOUNT);
//				this.data.put("PAY_TYPE", PAY_TYPE);
//				this.data.put("INPUT_TYPE", INPUT_TYPE);
//				this.data.put("PAY_STEP", PAY_STEP);
//				this.data.put("CLOSING_DATE", CLOSING_DATE);
//				this.data.put("PAY_DATE", PAY_DATE);
//				this.data.put("CANCEL_DATE", CANCEL_DATE);		 
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
		return error;
	}


	/**
	 * 일별 매출집계(확장)
	 *
	 * @param sType 집계유형
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 시작일
	 * @param eDate 종료일
	 * @param payTypes 결제유형
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String dailyEx(String sType, long company, long organ, long place, String sDate, String eDate, String[] payTypes, int oMode, int oType) throws Exception {
	// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sType) || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
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
			return "DB 연결에 실패하였습니다.";
		}

	// 검색절 생성
		String WHERE = "";
		String YYYYMMDD
				= "01".equals(sType) ? "TRANSACTION_DATE"	// 거래일
				: "02".equals(sType) ? "CLOSING_DATE"		// 마감일
				: "03".equals(sType) ? "PAY_DATE"			// 입금일
				: "04".equals(sType) ? "PURCHASE_DATE"		// 매입청구일
				: "05".equals(sType) ? "PAY_DATE_EXP"		// 입금예정일
				: "CANCEL_DATE"								// 취소일/마감일(취소)/환수일/매입취소일/환수예정일 - 미사용
			;
		
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 
			company = (company > 0 ? company : this.cfg.getLong("user.company"));
			if (company > 0) { // 소속
				WHERE += " AND A.COMPANY_SEQ = " + company;
			} 
			
			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			if (organ > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
							//20160121 타소속의 조직이 검색되지 않도록 COMPANY_SEQ 조건 추가
								+ " START WITH COMPANY_SEQ = " + company
									+ " AND SEQ IN ("
											+ " SELECT " + organ + " FROM DUAL"
											+ " UNION"
											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
													+ " ORGANIZATION_SEQ"
												+ " FROM TB_USER_APP_ORGAN"
												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
										+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
					;
			}
			
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			
			organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ") : organ);
			if (organ > 0) { // 조직
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ ")"
					;
			} 
		}
		
// 2017.06.26 jwhwang
//		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
//			WHERE += " AND USER_SEQ = " + this.cfg.getLong("user.seq");
//		} else {
//			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
//				WHERE += " AND COMPANY_SEQ = " + this.cfg.getLong("user.company");
//			}
//
//			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
//				WHERE += " AND ORGANIZATION_SEQ IN ("
//							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//									+ " SEQ"
//								+ " FROM TB_ORGANIZATION"
//								+ " WHERE SORT = 1"
//								+ " START WITH COMPANY_SEQ = " + this.cfg.getLong("user.company")
//									+ " AND SEQ IN ("
//											+ " SELECT " + this.cfg.getLong("user.organ") + " FROM DUAL"
//											+ " UNION"
//											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
//													+ " ORGANIZATION_SEQ"
//												+ " FROM TB_USER_APP_ORGAN"
//												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
//										+ " )"
//								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//						+ " )"
//					;
//			}
//		}
//
//		if (company > 0) { // 소속
//			WHERE += " AND COMPANY_SEQ = " + company;
//		}
//
//		if (organ > 0) { // 조직
//			WHERE += " AND ORGANIZATION_SEQ IN ("
//						+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//								+ " SEQ"
//							+ " FROM TB_ORGANIZATION"
//							+ " WHERE SORT = 1"
//							+ " START WITH SEQ = " + organ
//							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//					+ " )"
//				;
//		}
		

		if (place > 0) { // 위치
			WHERE += " AND VM_PLACE_SEQ = " + place;
		}

	//20160614 결제유형 다중선택 추가
		if ((payTypes != null) && (payTypes.length > 0)) {
			WHERE += " AND PAY_TYPE IN ('" + payTypes[0] + "'";
			for (int i = 1; i < payTypes.length; i++) WHERE += ", '" + payTypes[i] + "'";
			WHERE += ")";
	//20170329 키갱신 제외
		} else {
			WHERE += " AND A.PAY_TYPE <> '09'";
		}

		String SUM_OF_EACH_PREPAY_COMPANY_1 = "";
		String SUM_OF_EACH_PREPAY_COMPANY_2 = "";
		String[] prepaySummaryColumns = new String[] {};
		ArrayList<String> prepaySummaryColumnList = new ArrayList<String>();

	// 선불 카드사
		this.company = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(TB_CODE) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE"
						+ " WHERE TYPE = 'PAY_CARD'"
							+ " AND CODE <> '000'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				String code = rs.getString("CODE");
				c.put("CODE", code);
				c.put("NAME", rs.getString("NAME"));

				this.company.add(c);

				SUM_OF_EACH_PREPAY_COMPANY_1 += " COUNT(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN 1 END) AS CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN AMOUNT END), 0) AS AMOUNT_PREPAY_" + code + ","
						+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN 1 END) AS PAY_CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN COMMISSION END), 0) AS COMMISSION_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_PREPAY_" + code + ","
					;
				SUM_OF_EACH_PREPAY_COMPANY_2 += " COUNT(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN 1 END) AS CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN -AMOUNT END), 0) AS AMOUNT_PREPAY_" + code + ","
						+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN 1 END) AS PAY_CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN COMMISSION END), 0) AS COMMISSION_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN -AMOUNT + COMMISSION - OUTAMOUNT END), 0) AS PAY_AMOUNT_PREPAY_" + code + ","
					;
				prepaySummaryColumnList.add("CNT_PREPAY_" + code);
				prepaySummaryColumnList.add("AMOUNT_PREPAY_" + code);
				prepaySummaryColumnList.add("PAY_CNT_PREPAY_" + code);
				prepaySummaryColumnList.add("COMMISSION_PREPAY_" + code);
				prepaySummaryColumnList.add("PAY_AMOUNT_PREPAY_" + code);
			}

			prepaySummaryColumns = prepaySummaryColumnList.toArray(new String[prepaySummaryColumnList.size()]);
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			prepaySummaryColumnList = null;

			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();

		try {
			long[] cnt = { 0, 0, 0, 0, 0 };
			long[] amt = { 0, 0, 0, 0, 0 };
			long[] cms = { 0, 0, 0, 0, 0 };
			long[] pcnt = { 0, 0, 0, 0, 0 };
			long[] pamt = { 0, 0, 0, 0, 0 };
			long[] acnt = { 0, 0 };
			long[] aamt = { 0, 0 };

			ps = dbLib.prepareStatement(conn,
					" SELECT /*+ ORDERED USE_HASH(B C D) */"
							+ " A.*,"
							+ " NVL(TO_CHAR(TO_DATE(("
									+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
											+ " MAX(CLOSING_DATE || CLOSING_TIME)"
										+ " FROM TB_TXT_CLOSING"
										+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
											+ " AND (CLOSING_DATE || CLOSING_TIME) < A.MIN_DATE"
								+ " ), 'YYYYMMDDHH24MISS') + INTERVAL '1' SECOND, 'YYYYMMDDHH24MISS'), MIN_DATE) AS START_DATE,"
							+ " ("
										+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
												+ " NAME"
											+ " FROM TB_ORGANIZATION"
											+ " WHERE PARENT_SEQ = " + (organ > 0 ? organ : 0)
												+ " AND SORT = 1"
												+ " AND SEQ <> A.ORGANIZATION_SEQ"
											+ " START WITH SEQ = A.ORGANIZATION_SEQ"
											+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
									+ ") AS PARENT_ORGAN,"
							+ " B.NAME AS ORGAN,"
							+ " C.PLACE AS PLACE,"
							+ " D.CODE AS VM_CODE"
						+ " FROM ("
									+ " SELECT /*+ INDEX(A IX_SALES_" + YYYYMMDD + ") */"
											+ " " + YYYYMMDD + " AS YYYYMMDD,"
											+ " COMPANY_SEQ,"
											+ " ORGANIZATION_SEQ,"
											+ " VM_PLACE_SEQ,"
											+ " TERMINAL_ID,"
											+ " MIN(TRANSACTION_DATE || TRANSACTION_TIME) AS MIN_DATE,"
											+ " MAX(CASE WHEN CLOSING_DATE IS NOT NULL THEN CLOSING_DATE || NVL(CLOSING_TIME, '000000') END) AS END_DATE,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN AMOUNT END), 0) AS AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE IN ('02', '10') THEN 1 END) AS PAY_CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE IN ('02', '10') THEN COMMISSION END), 0) AS COMMISSION_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE IN ('02', '10') THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN AMOUNT END), 0) AS AMOUNT_CARD,"
											+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS PAY_CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '01' and pay_step <> '06' THEN COMMISSION END), 0) AS COMMISSION_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '01' and pay_step <> '06' THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_CARD,"
											//scheo 2018.10.10 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN AMOUNT END), 0) AS AMOUNT_PAYCO,"
											+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '07' THEN 1 END) AS PAY_CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '07' THEN COMMISSION END), 0) AS COMMISSION_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '07' THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_PAYCO,"
											//scheo 2018.10.10 추가
											+ SUM_OF_EACH_PREPAY_COMPANY_1
											+ " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN AMOUNT END), 0) AS AMOUNT_PREPAY,"
											+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' THEN 1 END) AS PAY_CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' THEN COMMISSION END), 0) AS COMMISSION_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_PREPAY,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('01', '11') AND PAY_STEP IN ('01', '21') THEN 1 END) AS CNT_HELD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('01', '11') AND PAY_STEP IN ('01', '21') THEN AMOUNT END), 0) AS AMOUNT_HELD,"
											+ " COUNT(CASE WHEN PAY_STEP = '22' THEN 1 END) AS CNT_DECLINED,"
											+ " NVL(SUM(CASE WHEN PAY_STEP = '22' THEN AMOUNT END), 0) AS AMOUNT_DECLINED,"
											+ " MIN(PAY_DATE) AS PAY_START_DATE,"
											+ " MAX(PAY_DATE) AS PAY_END_DATE"
										+ " FROM TB_SALES A"
										+ " WHERE " + YYYYMMDD + " BETWEEN '"+ sDate + "' AND '" + eDate + "'"
											+ " AND PAY_STEP NOT IN ('00', '99')"
											+ WHERE
										+ " GROUP BY " + YYYYMMDD + ", COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, TERMINAL_ID"
									+ " UNION ALL"
									+ " SELECT"
											+ " YYYYMMDD,"
											+ " COMPANY_SEQ,"
											+ " ORGANIZATION_SEQ,"
											+ " VM_PLACE_SEQ,"
											+ " TERMINAL_ID,"
											+ " MIN(CANCEL_DATE || CANCEL_TIME) AS MIN_DATE,"
											+ " MAX(("
													+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
															+ " MIN(CLOSING_DATE || NVL(CLOSING_TIME, '000000'))"
														+ " FROM TB_TXT_CLOSING"
														+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
													+ " AND (CLOSING_DATE || CLOSING_TIME) >= (A.CANCEL_DATE || A.CANCEL_TIME)"
												+ " )) AS END_DATE,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN -AMOUNT END), 0) AS AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE IN ('02', '10') THEN 1 END) AS PAY_CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN -COMMISSION END), 0) AS COMMISSION_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN -AMOUNT + COMMISSION - OUTAMOUNT END), 0) AS PAY_AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN -AMOUNT END), 0) AS AMOUNT_CARD,"
											+ " COUNT(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS PAY_CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '01' and pay_step <> '06' THEN -COMMISSION END), 0) AS COMMISSION_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '01' and pay_step <> '06' THEN -AMOUNT + COMMISSION - OUTAMOUNT END), 0) AS PAY_AMOUNT_CARD,"
											//scheo 2018.10.10 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN -AMOUNT END), 0) AS AMOUNT_PAYCO,"
											+ " COUNT(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '07' THEN 1 END) AS PAY_CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '07' THEN -COMMISSION END), 0) AS COMMISSION_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '07' THEN -AMOUNT + COMMISSION - OUTAMOUNT END), 0) AS PAY_AMOUNT_PAYCO,"
											//scheo 2018.10.10 추가
											+ SUM_OF_EACH_PREPAY_COMPANY_2
											+ " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN -AMOUNT END), 0) AS AMOUNT_PREPAY,"
											+ " COUNT(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '11' THEN 1 END) AS PAY_CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '11' THEN -COMMISSION END), 0) AS COMMISSION_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '11' THEN -AMOUNT + COMMISSION - OUTAMOUNT END), 0) AS PAY_AMOUNT_PREPAY,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('01', '11') AND PAY_STEP IN ('01', '21') THEN 1 END) AS CNT_HELD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('01', '11') AND PAY_STEP IN ('01', '21') THEN AMOUNT END), 0) AS AMOUNT_HELD,"
											+ " COUNT(CASE WHEN PAY_STEP = '22' THEN 1 END) AS CNT_DECLINED,"
											+ " NVL(SUM(CASE WHEN PAY_STEP = '22' THEN AMOUNT END), 0) AS AMOUNT_DECLINED,"
											+ " MIN(PAY_DATE2) AS PAY_START_DATE,"
											+ " MAX(PAY_DATE2) AS PAY_END_DATE"
										+ " FROM ("
												+ " SELECT /*+ INDEX(A IX_SALES_PAY_STEP) */"
										+ (
											// 마감일(취소)
												"02".equals(sType) ? (
														" CASE WHEN (CANCEL_DATE || CANCEL_TIME) < (CLOSING_DATE || CLOSING_TIME)"
															+ " THEN CLOSING_DATE"
															+ " ELSE ("
																	+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																			+ " SUBSTR(MIN(CLOSING_DATE || CLOSING_TIME), 1, 8)"
																		+ " FROM TB_TXT_CLOSING"
																		+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																			+ " AND (CLOSING_DATE || CLOSING_TIME) >= (A.CANCEL_DATE || A.CANCEL_TIME)"
																+ " )"
															+ " END AS YYYYMMDD,"
													) :
											// 환수일/환수예정일
												"03".equals(sType) || "05".equals(sType) ? (
														" F_GET_WORKDAY@VANBT(CANCEL_DATE, 5) AS YYYYMMDD,"
													) :
											// 매입취소일
												"04".equals(sType) ? (
														" F_GET_WORKDAY@VANBT(CANCEL_DATE, 1) AS YYYYMMDD,"
													) :
											// 취소일
												(
														" CANCEL_DATE AS YYYYMMDD,"
													)
											)
														+ " A.*,"
														+ " CASE WHEN F_GET_WORKDAY@VANBT(CANCEL_DATE, 5) <= TO_CHAR(SYSDATE, 'YYYYMMDD') THEN F_GET_WORKDAY@VANBT(CANCEL_DATE, 5) END AS PAY_DATE2"
													+ " FROM TB_SALES A"
													+ " WHERE PAY_STEP = '29'"
														+ " AND " + YYYYMMDD + " IS NOT NULL"
														+ WHERE
										+ (
											// 마감일(취소)
												"02".equals(sType) ? (
														" AND (CANCEL_DATE || CANCEL_TIME) BETWEEN ("
																+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																		+ " MAX(CLOSING_DATE || CLOSING_TIME) || '0'"
																	+ " FROM TB_TXT_CLOSING"
																	+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																		+ " AND (CLOSING_DATE || CLOSING_TIME) < '" + sDate + "'"
															+ " ) AND ("
																+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																		+ " MAX(CLOSING_DATE || CLOSING_TIME)"
																	+ " FROM TB_TXT_CLOSING"
																	+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																	+ " AND (CLOSING_DATE || CLOSING_TIME) <= '" + eDate + "235959'"
															+ " )"
														+ " AND CLOSING_DATE <= '" + eDate + "'"
														+ " AND CANCEL_DATE <= '" + eDate + "'"
													) :
											// 환수일/환수예정일
												"03".equals(sType) || "05".equals(sType) ? (
														" AND CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -5) AND F_GET_WORKDAY@VANBT('" + eDate + "', -5)"
													) :
											// 매입취소일
												"04".equals(sType) ? (
														" AND CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -1) AND F_GET_WORKDAY@VANBT('" + eDate + "', -1)"
													) :
											// 취소일
												(
														" AND CANCEL_DATE BETWEEN '" + sDate + "' AND '" + eDate + "'"
													)
											)
											+ " ) A"
										+ " WHERE YYYYMMDD BETWEEN '" + sDate + "' AND '" + eDate + "'"
										+ " GROUP BY YYYYMMDD, COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, TERMINAL_ID"
								+ " ) A"
							+ " INNER JOIN TB_ORGANIZATION B"
								+ " ON A.ORGANIZATION_SEQ = B.SEQ"
							+ " INNER JOIN TB_VENDING_MACHINE_PLACE C"
								+ " ON A.VM_PLACE_SEQ = C.SEQ"
							+ " INNER JOIN TB_VENDING_MACHINE D"
								+ " ON C.VM_SEQ = D.SEQ"
						+ " ORDER BY"
// 정렬 값(oMode, oType)에 따라 ORDER BY 변경
	+ ((oType == 1) ? (	// DESC
				// 거래처
					(oMode == 1) ? (
							" PARENT_ORGAN DESC, ORGAN DESC, YYYYMMDD, PLACE DESC"
						) :
				// 설치장소
					(oMode == 2) ? (
							" PARENT_ORGAN DESC, ORGAN DESC, PLACE DESC, YYYYMMDD"
						) :
				// default: 날짜
					(
							" YYYYMMDD DESC, PARENT_ORGAN, ORGAN, PLACE"
						)
			) : (			// ASC
				// 거래처
					(oMode == 1) ? (
							" PARENT_ORGAN, ORGAN, YYYYMMDD, PLACE"
						) :
				// 설치장소
					(oMode == 2) ? (
							" PARENT_ORGAN, ORGAN, PLACE, YYYYMMDD"
						) :
				// default: 날짜
					(
							" YYYYMMDD, PARENT_ORGAN, ORGAN, PLACE"
						)
				)
		)
							+ ", VM_CODE, A.AMOUNT_CARD DESC"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				String yyyymmdd = rs.getString("YYYYMMDD");
				c.put("YYYYMMDD", yyyymmdd);
				c.put("DATE", yyyymmdd.substring(0, 4) + "-" + yyyymmdd.substring(4, 6) + "-" + yyyymmdd.substring(6, 8));

				c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("VM_PLACE_SEQ", rs.getLong("VM_PLACE_SEQ"));
				c.put("PARENT_ORGAN", rs.getString("PARENT_ORGAN"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("PLACE", rs.getString("PLACE"));
				c.put("VM_CODE", rs.getString("VM_CODE"));

				String date = rs.getString("START_DATE");
				c.put("START_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				date = rs.getString("END_DATE");
				c.put("END_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);

				date = rs.getString("PAY_START_DATE");
				c.put("PAY_START_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8)
							: ""
					);
				date = rs.getString("PAY_END_DATE");
				c.put("PAY_END_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8)
							: ""
					);

				c.put("CNT_CASH", rs.getLong("CNT_CASH"));
				c.put("AMOUNT_CASH", rs.getLong("AMOUNT_CASH"));
				c.put("PAY_CNT_CASH", rs.getLong("PAY_CNT_CASH"));
				c.put("COMMISSION_CASH", rs.getLong("COMMISSION_CASH"));
				c.put("PAY_AMOUNT_CASH", rs.getLong("PAY_AMOUNT_CASH"));
				c.put("CNT_CARD", rs.getLong("CNT_CARD"));
				c.put("AMOUNT_CARD", rs.getLong("AMOUNT_CARD"));
				c.put("PAY_CNT_CARD", rs.getLong("PAY_CNT_CARD"));
				c.put("COMMISSION_CARD", rs.getLong("COMMISSION_CARD"));
				c.put("PAY_AMOUNT_CARD", rs.getLong("PAY_AMOUNT_CARD"));
				//scheo 2018.10.10 추가
				c.put("CNT_PAYCO", rs.getLong("CNT_PAYCO"));
				c.put("AMOUNT_PAYCO", rs.getLong("AMOUNT_PAYCO"));
				c.put("PAY_CNT_PAYCO", rs.getLong("PAY_CNT_PAYCO"));
				c.put("COMMISSION_PAYCO", rs.getLong("COMMISSION_PAYCO"));
				c.put("PAY_AMOUNT_PAYCO", rs.getLong("PAY_AMOUNT_PAYCO"));
				//scheo 2018.10.10 추가
				c.put("CNT_PREPAY", rs.getLong("CNT_PREPAY"));
				c.put("AMOUNT_PREPAY", rs.getLong("AMOUNT_PREPAY"));
				for (String columnName : prepaySummaryColumns) {
					c.put(columnName, rs.getLong(columnName));
					this.data.put(columnName, this.data.getLong(columnName) + rs.getLong(columnName));
				}
				c.put("PAY_CNT_PREPAY", rs.getLong("PAY_CNT_PREPAY"));
				c.put("COMMISSION_PREPAY", rs.getLong("COMMISSION_PREPAY"));
				c.put("PAY_AMOUNT_PREPAY", rs.getLong("PAY_AMOUNT_PREPAY"));
				c.put("CNT_HELD", rs.getLong("CNT_HELD"));
				c.put("AMOUNT_HELD", rs.getLong("AMOUNT_HELD"));
				c.put("CNT_DECLINED", rs.getLong("CNT_DECLINED"));
				c.put("AMOUNT_DECLINED", rs.getLong("AMOUNT_DECLINED"));

				c.put("CNT_TOTAL", c.getLong("CNT_CASH") + c.getLong("CNT_CARD") + c.getLong("CNT_PAYCO") + c.getLong("CNT_PREPAY"));	//scheo 2018.10.10 추가
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_CASH") + c.getLong("AMOUNT_CARD") + c.getLong("AMOUNT_PAYCO") + c.getLong("AMOUNT_PREPAY"));	//scheo 2018.10.10 추가
				c.put("PAY_CNT_TOTAL", c.getLong("PAY_CNT_CASH") + c.getLong("PAY_CNT_CARD") + c.getLong("PAY_CNT_PAYCO") + c.getLong("PAY_CNT_PREPAY"));	//scheo 2018.10.10 추가
				c.put("COMMISSION_TOTAL", c.getLong("COMMISSION_CASH") + c.getLong("COMMISSION_CARD") + c.getLong("COMMISSION_PAYCO") + c.getLong("COMMISSION_PREPAY"));	//scheo 2018.10.10 추가
				c.put("PAY_AMOUNT_TOTAL", c.getLong("PAY_AMOUNT_CASH") + c.getLong("PAY_AMOUNT_CARD") + c.getLong("PAY_AMOUNT_PAYCO") + c.getLong("PAY_AMOUNT_PREPAY"));	//scheo 2018.10.10 추가

				this.list.add(c);

				cnt[0] += c.getLong("CNT_CASH");
				cnt[1] += c.getLong("CNT_CARD");
				cnt[2] += c.getLong("CNT_PAYCO");//scheo 2018.10.10 추가
				cnt[3] += c.getLong("CNT_PREPAY");
				cnt[4] += c.getLong("CNT_TOTAL");
				amt[0] += c.getLong("AMOUNT_CASH");
				amt[1] += c.getLong("AMOUNT_CARD");
				amt[2] += c.getLong("AMOUNT_PAYCO");//scheo 2018.10.10 추가
				amt[3] += c.getLong("AMOUNT_PREPAY");
				amt[4] += c.getLong("AMOUNT_TOTAL");
				pcnt[0] += c.getLong("PAY_CNT_CASH");
				pcnt[1] += c.getLong("PAY_CNT_CARD");
				pcnt[2] += c.getLong("PAY_CNT_PAYCO");//scheo 2018.10.10 추가
				pcnt[3] += c.getLong("PAY_CNT_PREPAY");
				pcnt[4] += c.getLong("PAY_CNT_TOTAL");
				cms[0] += c.getLong("COMMISSION_CASH");
				cms[1] += c.getLong("COMMISSION_CARD");
				cms[2] += c.getLong("COMMISSION_PAYCO");//scheo 2018.10.10 추가
				cms[3] += c.getLong("COMMISSION_PREPAY");
				cms[4] += c.getLong("COMMISSION_TOTAL");
				pamt[0] += c.getLong("PAY_AMOUNT_CASH");
				pamt[1] += c.getLong("PAY_AMOUNT_CARD");
				pamt[2] += c.getLong("PAY_AMOUNT_PAYCO");//scheo 2018.10.10 추가
				pamt[3] += c.getLong("PAY_AMOUNT_PREPAY");
				pamt[4] += c.getLong("PAY_AMOUNT_TOTAL");
				acnt[0] += c.getLong("CNT_HELD");
				acnt[1] += c.getLong("CNT_DECLINED");
				aamt[0] += c.getLong("AMOUNT_HELD");
				aamt[1] += c.getLong("AMOUNT_DECLINED");
			}

			this.data.put("CNT_CASH", cnt[0]);
			this.data.put("CNT_CARD", cnt[1]);
			this.data.put("CNT_PAYCO", cnt[2]);//scheo 2018.10.10 추가
			this.data.put("CNT_PREPAY", cnt[3]);
			this.data.put("CNT_TOTAL", cnt[4]);
			this.data.put("AMOUNT_CASH", amt[0]);
			this.data.put("AMOUNT_CARD", amt[1]);
			this.data.put("AMOUNT_PAYCO", amt[2]);//scheo 2018.10.10 추가
			this.data.put("AMOUNT_PREPAY", amt[3]);
			this.data.put("AMOUNT_TOTAL", amt[4]);
			this.data.put("PAY_CNT_CASH", pcnt[0]);
			this.data.put("PAY_CNT_CARD", pcnt[1]);
			this.data.put("PAY_CNT_PAYCO", pcnt[2]);//scheo 2018.10.10 추가
			this.data.put("PAY_CNT_PREPAY", pcnt[3]);
			this.data.put("PAY_CNT_TOTAL", pcnt[4]);
			this.data.put("COMMISSION_CASH", cms[0]);
			this.data.put("COMMISSION_CARD", cms[1]);
			this.data.put("COMMISSION_PAYCO", cms[2]);//scheo 2018.10.10 추가
			this.data.put("COMMISSION_PREPAY", cms[3]);
			this.data.put("COMMISSION_TOTAL", cms[4]);
			this.data.put("PAY_AMOUNT_CASH", pamt[0]);
			this.data.put("PAY_AMOUNT_CARD", pamt[1]);
			this.data.put("PAY_AMOUNT_PAYCO", pamt[2]);//scheo 2018.10.10 추가
			this.data.put("PAY_AMOUNT_PREPAY", pamt[3]);
			this.data.put("PAY_AMOUNT_TOTAL", pamt[4]);
			this.data.put("CNT_HELD", acnt[0]);
			this.data.put("CNT_DECLINED", acnt[1]);
			this.data.put("AMOUNT_HELD", aamt[0]);
			this.data.put("AMOUNT_DECLINED", aamt[1]);
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 조직명
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
						+ " FROM ("
								+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
										+ " NAME"
									+ " FROM TB_ORGANIZATION"
									+ " WHERE COMPANY_SEQ = " + company
										+ " AND SORT = 0"
									+ " ORDER BY DEPTH DESC"
							+ " )"
						+ " WHERE ROWNUM = 1"
					)
			);

	// 검색 설정
		String sDesc = "집계기준="
				+ dbLib.getResult(conn,
						"SELECT /*+ INDEX(TB_CODE) */"
								+ " NAME"
							+ " FROM TB_CODE"
							+ " WHERE TYPE = 'SUM_TYPE'"
								+ " AND CODE = '" + sType + "'"
					)
				+ "&집계기간=" + sDate + "-" + eDate
			;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}

		this.data.put("sDesc", sDesc);

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}

	/**
	 * 월별 매출집계
	 *
	 * @param sType 집계 유형
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 시작일
	 * @param eDate 종료일
	 * @param payTypes 결제유형
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String monthly(String sType, long company, long organ,long depth, long place, String sDate, String eDate, String[] payTypes, int oMode, int oType) throws Exception {
	// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sType) || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
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
			return "DB 연결에 실패하였습니다.";
		}

	// 검색절 생성
		String WHERE = "";
		String YYYYMMDD
				= "01".equals(sType) ? "TRANSACTION_DATE"	// 거래일
				: "02".equals(sType) ? "CLOSING_DATE"		// 마감일
				: "03".equals(sType) ? "PAY_DATE"			// 입금일
				: "04".equals(sType) ? "PURCHASE_DATE"		// 매입청구일
				: "05".equals(sType) ? "PAY_DATE_EXP"		// 입금예정일
				: "CANCEL_DATE"								// 취소일/마감일(취소)/환수일/매입취소일/환수예정일 - 미사용
			;

		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 
			company = (company > 0 ? company : this.cfg.getLong("user.company"));
			if (company > 0) { // 소속
				WHERE += " AND A.COMPANY_SEQ = " + company;
			} 
			
			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			if (organ > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
							//20160121 타소속의 조직이 검색되지 않도록 COMPANY_SEQ 조건 추가
								+ " START WITH COMPANY_SEQ = " + company
									+ " AND SEQ IN ("
											+ " SELECT " + organ + " FROM DUAL"
											+ " UNION"
											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
													+ " ORGANIZATION_SEQ"
												+ " FROM TB_USER_APP_ORGAN"
												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
										+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
					;
			}
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			
			//organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ") : organ);
			organ = (organ <= 0 ? this.cfg.getLong("user.organ") : (this.cfg.getInt("user.organ.depth") > depth ? this.cfg.getLong("user.organ") : organ));
			if (organ > 0) { // 조직
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ ")"
					;
			} 			
		}
		
		// 2017.06.26 jwhwang
//		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
//			WHERE += " AND USER_SEQ = " + this.cfg.getLong("user.seq");
//		} else {
//			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
//				WHERE += " AND COMPANY_SEQ = " + this.cfg.getLong("user.company");
//			}
//
//			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
//				WHERE += " AND ORGANIZATION_SEQ IN ("
//							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//									+ " SEQ"
//								+ " FROM TB_ORGANIZATION"
//								+ " WHERE SORT = 1"
//							//20160121 타소속의 조직이 검색되지 않도록 COMPANY_SEQ 조건 추가
//								+ " START WITH COMPANY_SEQ = " + this.cfg.getLong("user.company")
//									+ " AND SEQ IN ("
//											+ " SELECT " + this.cfg.getLong("user.organ") + " FROM DUAL"
//											+ " UNION"
//											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
//													+ " ORGANIZATION_SEQ"
//												+ " FROM TB_USER_APP_ORGAN"
//												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
//										+ " )"
//								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//						+ " )"
//					;
//			}
//		}
//
//		if (company > 0) { // 소속
//			WHERE += " AND COMPANY_SEQ = " + company;
//		}
//
//		if (organ > 0) { // 조직
//			WHERE += " AND ORGANIZATION_SEQ IN ("
//						+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//								+ " SEQ"
//							+ " FROM TB_ORGANIZATION"
//							+ " WHERE SORT = 1"
//							+ " START WITH SEQ = " + organ
//							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//					+ " )"
//				;
//		}

		if (place > 0) { // 위치
			WHERE += " AND VM_PLACE_SEQ = " + place;
		}

	//20160629 결제유형 다중선택 추가
		if ((payTypes != null) && (payTypes.length > 0)) {
			WHERE += " AND PAY_TYPE IN ('" + payTypes[0] + "'";
			for (int i = 1; i < payTypes.length; i++) WHERE += ", '" + payTypes[i] + "'";
			WHERE += ")";
	//20170329 키갱신 제외
		} else {
			WHERE += " AND A.PAY_TYPE <> '09'";
		}

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();

		try {
			long[] cnt = {0, 0, 0, 0, 0, 0};
			long[] amt = {0, 0, 0, 0, 0, 0};

			ps = dbLib.prepareStatement(conn,
					" SELECT /*+ ORDERED USE_HASH(B C D) */"
							+ " A.*,"
							+ " B.NAME AS ORGAN,"
							+ " B.CODE AS ORGAN_CODE," //2017.12.21 jwhwang 추가
							+ " C.PLACE AS PLACE,"
							+ " D.CODE AS VM_CODE"
						+ " FROM ("
									+ " SELECT /*+ INDEX(A IX_SALES_" + YYYYMMDD + ") */"
											+ " SUBSTR(" + YYYYMMDD + ", 1, 6) AS YYYYMM,"
											+ " COMPANY_SEQ,"
											+ " ORGANIZATION_SEQ,"
											+ " VM_PLACE_SEQ,"
											+ " COUNT(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN AMOUNT END), 0) AS AMOUNT_CARD,"
											//scheo 2018.10.10 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN AMOUNT END), 0) AS AMOUNT_PAYCO,"
											//scheo 2021.09.07 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '80' THEN 1 END) AS CNT_ITEM,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '80' THEN AMOUNT END), 0) AS AMOUNT_ITEM,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN AMOUNT END), 0) AS AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN AMOUNT END), 0) AS AMOUNT_PREPAY"
										+ " FROM TB_SALES A"
										+ " WHERE " + YYYYMMDD + " BETWEEN '"+ sDate + "' AND '" + eDate + "'"
											+ " AND PAY_STEP NOT IN ('00', '99')"
											+ WHERE
										+ " GROUP BY SUBSTR(" + YYYYMMDD + ", 1, 6), COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ"
									+ " UNION ALL"
									+ " SELECT"
											+ " SUBSTR(YYYYMMDD, 1, 6) AS YYYYMM,"
											+ " COMPANY_SEQ,"
											+ " ORGANIZATION_SEQ,"
											+ " VM_PLACE_SEQ,"
											+ " COUNT(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN -AMOUNT END), 0) AS AMOUNT_CARD,"
											//scheo 2018.10.10 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN -AMOUNT END), 0) AS AMOUNT_PAYCO,"
											//scheo 2021.09.07 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '80' THEN 1 END) AS CNT_ITEM,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '80' THEN -AMOUNT END), 0) AS AMOUNT_ITEM,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN -AMOUNT END), 0) AS AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN -AMOUNT END), 0) AS AMOUNT_PREPAY"
										+ " FROM ("
												+ " SELECT /*+ INDEX(A IX_SALES_PAY_STEP) */"
										+ (
											// 마감일
												"02".equals(sType) ? (
														" CASE WHEN (CANCEL_DATE || CANCEL_TIME) < (CLOSING_DATE || CLOSING_TIME)"
															+ " THEN CLOSING_DATE"
															+ " ELSE ("
																	+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																			+ " SUBSTR(MIN(CLOSING_DATE || CLOSING_TIME), 1, 8)"
																		+ " FROM TB_TXT_CLOSING"
																		+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																			+ " AND (CLOSING_DATE || CLOSING_TIME) >= (A.CANCEL_DATE || A.CANCEL_TIME)"
																+ " )"
															+ " END AS YYYYMMDD,"
													) :
											// 입금일/입금예정일
												"03".equals(sType) || "05".equals(sType) ? (
														" F_GET_WORKDAY@VANBT(CANCEL_DATE, 5) AS YYYYMMDD,"
													) :
											// 매입청구일
												"04".equals(sType) ? (
														" F_GET_WORKDAY@VANBT(CANCEL_DATE, 1) AS YYYYMMDD,"
													) :
											// 거래일
												(
														" CANCEL_DATE AS YYYYMMDD,"
													)
											)
														+ " A.*"
													+ " FROM TB_SALES A"
													+ " WHERE PAY_STEP = '29'"
														+ " AND " + YYYYMMDD + " IS NOT NULL"
														+ WHERE
										+ (
											// 마감일
												"02".equals(sType) ? (
														" AND (CANCEL_DATE || CANCEL_TIME) BETWEEN ("
																+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																		+ " MAX(CLOSING_DATE || CLOSING_TIME) || '0'"
																	+ " FROM TB_TXT_CLOSING"
																	+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																		+ " AND (CLOSING_DATE || CLOSING_TIME) < '" + sDate + "'"
															+ " ) AND ("
																+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																		+ " MAX(CLOSING_DATE || CLOSING_TIME)"
																	+ " FROM TB_TXT_CLOSING"
																	+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																	+ " AND (CLOSING_DATE || CLOSING_TIME) <= '" + eDate + "235959'"
															+ " )"
														+ " AND CLOSING_DATE <= '" + eDate + "'"
														+ " AND CANCEL_DATE <= '" + eDate + "'"
													) :
											// 입금일/입금예정일
												"03".equals(sType) || "05".equals(sType) ? (
														" AND CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -5) AND F_GET_WORKDAY@VANBT('" + eDate + "', -5)"
													) :
											// 매입청구일
												"04".equals(sType) || "05".equals(sType) ? (
														" AND CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -1) AND F_GET_WORKDAY@VANBT('" + eDate + "', -1)"
													) :
											// 거래일
												(
														" AND CANCEL_DATE BETWEEN '" + sDate + "' AND '" + eDate + "'"
													)
											)
											+ " ) A"
										+ " WHERE YYYYMMDD BETWEEN '" + sDate + "' AND '" + eDate + "'"
										+ " GROUP BY SUBSTR(YYYYMMDD, 1, 6), COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ"
								+ " ) A"
							+ " INNER JOIN TB_ORGANIZATION B"
								+ " ON A.ORGANIZATION_SEQ = B.SEQ"
							+ " INNER JOIN TB_VENDING_MACHINE_PLACE C"
								+ " ON A.VM_PLACE_SEQ = C.SEQ"
							+ " INNER JOIN TB_VENDING_MACHINE D"
								+ " ON C.VM_SEQ = D.SEQ"
						+ " ORDER BY"
// 정렬 값(oMode, oType)에 따라 ORDER BY 변경
	+ ((oType == 1) ? (	// DESC
				// 거래처
					(oMode == 1) ? (
							" ORGAN DESC, YYYYMM DESC, PLACE DESC"
						) :
				// 설치장소
					(oMode == 2) ? (
							" ORGAN DESC, PLACE DESC, YYYYMM DESC"
						) :
				// default: 날짜
					(
							" YYYYMM DESC, ORGAN, PLACE"
						)
			) : (			// ASC
				// 거래처
					(oMode == 1) ? (
							" ORGAN, YYYYMM, PLACE"
						) :
				// 설치장소
					(oMode == 2) ? (
							" PLACE, ORGAN, YYYYMM"
						) :
				// default: 날짜
					(
							" YYYYMM, ORGAN, PLACE"
						)
				)
		)
							+ ", VM_CODE, A.AMOUNT_CARD DESC"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				String yyyymm = rs.getString("YYYYMM");
				c.put("YYYYMM", yyyymm);
				c.put("DATE", yyyymm.substring(0, 4) + "-" + yyyymm.substring(4, 6));

				c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("VM_PLACE_SEQ", rs.getLong("VM_PLACE_SEQ"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("ORGAN_CODE", rs.getString("ORGAN_CODE")); //2017.12.21 jwhwang 추가
				c.put("PLACE", rs.getString("PLACE"));
				c.put("VM_CODE", rs.getString("VM_CODE"));

				c.put("CNT_CARD", rs.getLong("CNT_CARD"));
				c.put("AMOUNT_CARD", rs.getLong("AMOUNT_CARD"));
				c.put("CNT_PAYCO", rs.getLong("CNT_PAYCO"));	//scheo 2018.10.10 추가
				c.put("AMOUNT_PAYCO", rs.getLong("AMOUNT_PAYCO"));	//scheo 2018.10.10 추가
				c.put("CNT_ITEM", rs.getLong("CNT_ITEM"));	//scheo 2021.09.07 추가
				c.put("AMOUNT_ITEM", rs.getLong("AMOUNT_ITEM"));	//scheo 2021.09.07 추가
				c.put("CNT_CASH", rs.getLong("CNT_CASH"));
				c.put("AMOUNT_CASH", rs.getLong("AMOUNT_CASH"));
				c.put("CNT_PREPAY", rs.getLong("CNT_PREPAY"));
				c.put("AMOUNT_PREPAY", rs.getLong("AMOUNT_PREPAY"));

				c.put("CNT_TOTAL", c.getLong("CNT_CARD") + c.getLong("CNT_PAYCO") + c.getLong("CNT_CASH") + c.getLong("CNT_PREPAY"));	//scheo 2018.10.10 추가
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_CARD") + c.getLong("AMOUNT_PAYCO") + c.getLong("AMOUNT_CASH") + c.getLong("AMOUNT_PREPAY"));	//scheo 2018.10.10 추가

				this.list.add(c);

				cnt[0] += c.getLong("CNT_CARD");
				cnt[1] += c.getLong("CNT_PAYCO");//scheo 2018.10.10 추가
				cnt[2] += c.getLong("CNT_ITEM");//scheo 2021.09.07 추가
				cnt[3] += c.getLong("CNT_CASH");
				cnt[4] += c.getLong("CNT_PREPAY");
				cnt[5] += c.getLong("CNT_TOTAL");
				amt[0] += c.getLong("AMOUNT_CARD");
				amt[1] += c.getLong("AMOUNT_PAYCO");//scheo 2018.10.10 추가
				amt[2] += c.getLong("AMOUNT_ITEM");//scheo 2021.09.07 추가
				amt[3] += c.getLong("AMOUNT_CASH");
				amt[4] += c.getLong("AMOUNT_PREPAY");
				amt[5] += c.getLong("AMOUNT_TOTAL");
			}

			this.data.put("CNT_CARD", cnt[0]);
			this.data.put("CNT_PAYCO", cnt[1]);//scheo 2018.10.10 추가
			this.data.put("CNT_ITEM", cnt[2]);//scheo 2021.09.07 추가
			this.data.put("CNT_CASH", cnt[3]);
			this.data.put("CNT_PREPAY", cnt[4]);
			this.data.put("CNT_TOTAL", cnt[5]);
			this.data.put("AMOUNT_CARD", amt[0]);
			this.data.put("AMOUNT_PAYCO", amt[1]);//scheo 2018.10.10 추가
			this.data.put("AMOUNT_ITEM", amt[2]);//scheo 2021.09.07 추가
			this.data.put("AMOUNT_CASH", amt[3]);
			this.data.put("AMOUNT_PREPAY", amt[4]);
			this.data.put("AMOUNT_TOTAL", amt[5]);
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 조직명
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
							+ " FROM ("
									+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
											+ " NAME"
										+ " FROM TB_ORGANIZATION"
										+ " WHERE COMPANY_SEQ = " + company
											+ " AND SORT = 0"
										+ " ORDER BY DEPTH DESC"
								+ " )"
							+ " WHERE ROWNUM = 1"
					)
			);

	// 검색 설정
		String sDesc = "집계기준="
				+ dbLib.getResult(conn,
						"SELECT /*+ INDEX(TB_CODE) */"
								+ " NAME"
							+ " FROM TB_CODE"
							+ " WHERE TYPE = 'SUM_TYPE'"
								+ " AND CODE = '" + sType + "'"
					)
				+ "&집계기간=" + sDate + "-" + eDate
			;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}

		this.data.put("sDesc", sDesc);

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}

	/**
	 * 자판기별 매출집계
	 *
	 * @param sType 집계 유형
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 시작일
	 * @param eDate 종료일
	 * @param payTypes 결제유형
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String vm(String sType, long company, long organ,long depth, long place, String sDate, String eDate, String[] payTypes, int oMode, int oType) throws Exception {
	// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sType) || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
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
			return "DB 연결에 실패하였습니다.";
		}

	// 검색절 생성
		String WHERE = "";
		String YYYYMMDD
				= "01".equals(sType) ? "TRANSACTION_DATE"	// 거래일
				: "02".equals(sType) ? "CLOSING_DATE"		// 마감일
				: "03".equals(sType) ? "PAY_DATE"			// 입금일
				: "04".equals(sType) ? "PURCHASE_DATE"		// 매입청구일
				: "05".equals(sType) ? "PAY_DATE_EXP"		// 입금예정일
				: "CANCEL_DATE"								// 취소일/마감일(취소)/환수일/매입취소일/환수예정일 - 미사용
			;
		
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 
			company = (company > 0 ? company : this.cfg.getLong("user.company"));
			if (company > 0) { // 소속
				WHERE += " AND A.COMPANY_SEQ = " + company;
			} 
			
			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			if (organ > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
							//20160121 타소속의 조직이 검색되지 않도록 COMPANY_SEQ 조건 추가
								+ " START WITH COMPANY_SEQ = " + company
									+ " AND SEQ IN ("
											+ " SELECT " + organ + " FROM DUAL"
											+ " UNION"
											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
													+ " ORGANIZATION_SEQ"
												+ " FROM TB_USER_APP_ORGAN"
												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
										+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
					;
			}
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			
			//organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ") : organ);
			organ = (organ <= 0 ? this.cfg.getLong("user.organ") : (this.cfg.getInt("user.organ.depth") > depth ? this.cfg.getLong("user.organ") : organ));
			if (organ > 0) { // 조직
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ ")"
					;
			} 			
		}
// 2017.06.26 jwhwang
//		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
//			WHERE += " AND USER_SEQ = " + this.cfg.getLong("user.seq");
//		} else {
//			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
//				WHERE += " AND COMPANY_SEQ = " + this.cfg.getLong("user.company");
//			}
//
//			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
//				WHERE += " AND ORGANIZATION_SEQ IN ("
//							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//									+ " SEQ"
//								+ " FROM TB_ORGANIZATION"
//								+ " WHERE SORT = 1"
//							//20160121 타소속의 조직이 검색되지 않도록 COMPANY_SEQ 조건 추가
//								+ " START WITH COMPANY_SEQ = " + this.cfg.getLong("user.company")
//									+ " AND SEQ IN ("
//											+ " SELECT " + this.cfg.getLong("user.organ") + " FROM DUAL"
//											+ " UNION"
//											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
//													+ " ORGANIZATION_SEQ"
//												+ " FROM TB_USER_APP_ORGAN"
//												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
//										+ " )"
//								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//						+ " )"
//					;
//			}
//		}
//
//		if (company > 0) { // 소속
//			WHERE += " AND COMPANY_SEQ = " + company;
//		}
//
//		if (organ > 0) { // 조직
//			WHERE += " AND ORGANIZATION_SEQ IN ("
//						+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//								+ " SEQ"
//							+ " FROM TB_ORGANIZATION"
//							+ " WHERE SORT = 1"
//							+ " START WITH SEQ = " + organ
//							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//					+ " )"
//				;
//		}

		if (place > 0) { // 위치
			WHERE += " AND VM_PLACE_SEQ = " + place;
		}

	//20160630 결제유형 다중선택 추가
		if ((payTypes != null) && (payTypes.length > 0)) {
			WHERE += " AND PAY_TYPE IN ('" + payTypes[0] + "'";
			for (int i = 1; i < payTypes.length; i++) WHERE += ", '" + payTypes[i] + "'";
			WHERE += ")";
	//20170329 키갱신 제외
		} else {
			WHERE += " AND A.PAY_TYPE <> '09'";
		}

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();

		try {
			long[] cnt = {0, 0, 0, 0, 0, 0};//scheo 2018.10.10 추가
			long[] amt = {0, 0, 0, 0, 0, 0};//scheo 2018.10.10 추가

			ps = dbLib.prepareStatement(conn,
					" SELECT /*+ ORDERED USE_HASH(B C D) */"
							+ " A.*,"
							+ " B.NAME AS ORGAN,"
							+ " B.CODE AS ORGAN_CODE," //2017.12.21 jwhwang 추가
							+ " C.PLACE AS PLACE,"
							+ " D.CODE AS VM_CODE"
						+ " FROM ("
									+ " SELECT /*+ INDEX(A IX_SALES_" + YYYYMMDD + ") */"
											+ " COMPANY_SEQ,"
											+ " ORGANIZATION_SEQ,"
											+ " VM_PLACE_SEQ,"
											+ " COUNT(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN AMOUNT END), 0) AS AMOUNT_CARD,"
											//scheo 2018.10.10 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN AMOUNT END), 0) AS AMOUNT_PAYCO,"
											//scheo 2021.09.07 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '80' THEN 1 END) AS CNT_ITEM,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '80' THEN AMOUNT END), 0) AS AMOUNT_ITEM,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN AMOUNT END), 0) AS AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN AMOUNT END), 0) AS AMOUNT_PREPAY"
										+ " FROM TB_SALES A"
										+ " WHERE " + YYYYMMDD + " BETWEEN '"+ sDate + "' AND '" + eDate + "'"
											+ " AND PAY_STEP NOT IN ('00', '99')"
											+ WHERE
										+ " GROUP BY COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ"
									+ " UNION ALL"
									+ " SELECT"
											+ " COMPANY_SEQ,"
											+ " ORGANIZATION_SEQ,"
											+ " VM_PLACE_SEQ,"
											+ " COUNT(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN -AMOUNT END), 0) AS AMOUNT_CARD,"
											//scheo 2018.10.10 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN -AMOUNT END), 0) AS AMOUNT_PAYCO,"
											//scheo 2021.09.07 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '80' THEN 1 END) AS CNT_ITEM,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '80' THEN -AMOUNT END), 0) AS AMOUNT_ITEM,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN -AMOUNT END), 0) AS AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN -AMOUNT END), 0) AS AMOUNT_PREPAY"
										+ " FROM ("
												+ " SELECT /*+ INDEX(A IX_SALES_PAY_STEP) */"
										+ (
											// 마감일
												"02".equals(sType) ? (
														" CASE WHEN (CANCEL_DATE || CANCEL_TIME) < (CLOSING_DATE || CLOSING_TIME)"
															+ " THEN CLOSING_DATE"
															+ " ELSE ("
																	+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																			+ " SUBSTR(MIN(CLOSING_DATE || CLOSING_TIME), 1, 8)"
																		+ " FROM TB_TXT_CLOSING"
																		+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																			+ " AND (CLOSING_DATE || CLOSING_TIME) >= (A.CANCEL_DATE || A.CANCEL_TIME)"
																+ " )"
															+ " END AS YYYYMMDD,"
													) :
											// 입금일/입금예정일
												"03".equals(sType) || "05".equals(sType) ? (
														" F_GET_WORKDAY@VANBT(CANCEL_DATE, 5) AS YYYYMMDD,"
													) :
											// 매입청구일
												"04".equals(sType) ? (
														" F_GET_WORKDAY@VANBT(CANCEL_DATE, 1) AS YYYYMMDD,"
													) :
											// 거래일
												(
														" CANCEL_DATE AS YYYYMMDD,"
													)
											)
														+ " A.*"
													+ " FROM TB_SALES A"
													+ " WHERE PAY_STEP = '29'"
														+ " AND " + YYYYMMDD + " IS NOT NULL"
														+ WHERE
										+ (
											// 마감일
												"02".equals(sType) ? (
														" AND (CANCEL_DATE || CANCEL_TIME) BETWEEN ("
																+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																		+ " MAX(CLOSING_DATE || CLOSING_TIME) || '0'"
																	+ " FROM TB_TXT_CLOSING"
																	+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																		+ " AND (CLOSING_DATE || CLOSING_TIME) < '" + sDate + "'"
															+ " ) AND ("
																+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																		+ " MAX(CLOSING_DATE || CLOSING_TIME)"
																	+ " FROM TB_TXT_CLOSING"
																	+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																	+ " AND (CLOSING_DATE || CLOSING_TIME) <= '" + eDate + "235959'"
															+ " )"
														+ " AND CLOSING_DATE <= '" + eDate + "'"
														+ " AND CANCEL_DATE <= '" + eDate + "'"
													) :
											// 입금일/입금예정일
												"03".equals(sType) || "05".equals(sType) ? (
														" AND CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -5) AND F_GET_WORKDAY@VANBT('" + eDate + "', -5)"
													) :
											// 매입청구일
												"04".equals(sType) ? (
														" AND CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -1) AND F_GET_WORKDAY@VANBT('" + eDate + "', -1)"
													) :
											// 거래일
												(
														" AND CANCEL_DATE BETWEEN '" + sDate + "' AND '" + eDate + "'"
													)
											)
											+ " ) A"
										+ " WHERE YYYYMMDD BETWEEN '" + sDate + "' AND '" + eDate + "'"
										+ " GROUP BY COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ"
								+ " ) A"
							+ " INNER JOIN TB_ORGANIZATION B"
								+ " ON A.ORGANIZATION_SEQ = B.SEQ"
							+ " INNER JOIN TB_VENDING_MACHINE_PLACE C"
								+ " ON A.VM_PLACE_SEQ = C.SEQ"
							+ " INNER JOIN TB_VENDING_MACHINE D"
								+ " ON C.VM_SEQ = D.SEQ"
						+ " ORDER BY"
// 정렬 값(oMode, oType)에 따라 ORDER BY 변경
	+ ((oType == 1) ? (	// DESC
				// 거래처
					(oMode == 1) ? (
							" ORGAN DESC, PLACE DESC"
						) :
				// default: 설치장소
					(
							" PLACE DESC, ORGAN DESC"
						)
			) : (	// ASC
				// 거래처
					(oMode == 1) ? (
							" ORGAN, PLACE"
						) :
				// default: 설치장소
					(
							" PLACE, ORGAN"
						)
				)
		)
							+ ", VM_CODE, A.AMOUNT_CARD DESC"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("VM_PLACE_SEQ", rs.getLong("VM_PLACE_SEQ"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("ORGAN_CODE", rs.getString("ORGAN_CODE")); //2017.12.21 jwhwang 추가
				c.put("PLACE", rs.getString("PLACE"));
			//20120604 자판기코드 추가
				c.put("VM_CODE", rs.getString("VM_CODE"));

				c.put("CNT_CARD", rs.getLong("CNT_CARD"));
				c.put("AMOUNT_CARD", rs.getLong("AMOUNT_CARD"));
				c.put("CNT_PAYCO", rs.getLong("CNT_PAYCO"));//scheo 2018.10.10 추가
				c.put("AMOUNT_PAYCO", rs.getLong("AMOUNT_PAYCO"));//scheo 2018.10.10 추가
				c.put("CNT_ITEM", rs.getLong("CNT_ITEM"));//scheo 2021.09.07 추가
				c.put("AMOUNT_ITEM", rs.getLong("AMOUNT_ITEM"));//scheo 2021.09.07 추가
				c.put("CNT_CASH", rs.getLong("CNT_CASH"));
				c.put("AMOUNT_CASH", rs.getLong("AMOUNT_CASH"));
				c.put("CNT_PREPAY", rs.getLong("CNT_PREPAY"));
				c.put("AMOUNT_PREPAY", rs.getLong("AMOUNT_PREPAY"));

				c.put("CNT_TOTAL", c.getLong("CNT_CARD") + c.getLong("CNT_PAYCO") + c.getLong("CNT_CASH") + c.getLong("CNT_PREPAY"));//scheo 2018.10.10 추가
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_CARD") + c.getLong("AMOUNT_PAYCO") + c.getLong("AMOUNT_CASH") + c.getLong("AMOUNT_PREPAY"));//scheo 2018.10.10 추가

				this.list.add(c);

				cnt[0] += c.getLong("CNT_CARD");
				cnt[1] += c.getLong("CNT_PAYCO");//scheo 2018.10.10 추가
				cnt[2] += c.getLong("CNT_ITEM");//scheo 2021.09.07 추가
				cnt[3] += c.getLong("CNT_CASH");
				cnt[4] += c.getLong("CNT_PREPAY");
				cnt[5] += c.getLong("CNT_TOTAL");
				amt[0] += c.getLong("AMOUNT_CARD");
				amt[1] += c.getLong("AMOUNT_PAYCO");//scheo 2018.10.10 추가
				amt[2] += c.getLong("AMOUNT_ITEM");//scheo 2021.09.07 추가
				amt[3] += c.getLong("AMOUNT_CASH");
				amt[4] += c.getLong("AMOUNT_PREPAY");
				amt[5] += c.getLong("AMOUNT_TOTAL");
			}

			this.data.put("CNT_CARD", cnt[0]);
			this.data.put("CNT_PAYCO", cnt[1]);//scheo 2018.10.10 추가
			this.data.put("CNT_ITEM", cnt[2]);//scheo 2021.09.07 추가
			this.data.put("CNT_CASH", cnt[3]);
			this.data.put("CNT_PREPAY", cnt[4]);
			this.data.put("CNT_TOTAL", cnt[5]);
			this.data.put("AMOUNT_CARD", amt[0]);
			this.data.put("AMOUNT_PAYCO", amt[1]);//scheo 2018.10.10 추가
			this.data.put("AMOUNT_ITEM", amt[2]);//scheo 2021.09.07 추가
			this.data.put("AMOUNT_CASH", amt[3]);
			this.data.put("AMOUNT_PREPAY", amt[4]);
			this.data.put("AMOUNT_TOTAL", amt[5]);
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 조직명
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
							+ " FROM ("
									+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
											+ " NAME"
										+ " FROM TB_ORGANIZATION"
										+ " WHERE COMPANY_SEQ = " + company
											+ " AND SORT = 0"
										+ " ORDER BY DEPTH DESC"
								+ " )"
							+ " WHERE ROWNUM = 1"
					)
			);

	// 검색 설정
		String sDesc = "집계기준="
				+ dbLib.getResult(conn,
						"SELECT /*+ INDEX(TB_CODE) */"
								+ " NAME"
							+ " FROM TB_CODE"
							+ " WHERE TYPE = 'SUM_TYPE'"
								+ " AND CODE = '" + sType + "'"
					)
				+ "&집계기간=" + sDate + "-" + eDate
			;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}

		this.data.put("sDesc", sDesc);

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
	/**
	 * 자판기별 매출집계
	 *
	 * @param sType 집계 유형
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 시작일
	 * @param eDate 종료일
	 * @param payTypes 결제유형
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String column(String sType, long company, long organ, long depth,long place, String sDate, String eDate, String[] payTypes, int oMode, int oType) throws Exception {
	// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sType) || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
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
			return "DB 연결에 실패하였습니다.";
		}

	// 검색절 생성
		String WHERE = "";
		String YYYYMMDD
				= "01".equals(sType) ? "TRANSACTION_DATE"	// 거래일
				: "02".equals(sType) ? "CLOSING_DATE"		// 마감일
				: "03".equals(sType) ? "PAY_DATE"			// 입금일
				: "04".equals(sType) ? "PURCHASE_DATE"		// 매입청구일
				: "05".equals(sType) ? "PAY_DATE_EXP"		// 입금예정일
				: "CANCEL_DATE"								// 취소일/마감일(취소)/환수일/매입취소일/환수예정일 - 미사용
			;
		
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 
			company = (company > 0 ? company : this.cfg.getLong("user.company"));
			if (company > 0) { // 소속
				WHERE += " AND A.COMPANY_SEQ = " + company;
			} 
			
			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			if (organ > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
							//20160121 타소속의 조직이 검색되지 않도록 COMPANY_SEQ 조건 추가
								+ " START WITH COMPANY_SEQ = " + company
									+ " AND SEQ IN ("
											+ " SELECT " + organ + " FROM DUAL"
											+ " UNION"
											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
													+ " ORGANIZATION_SEQ"
												+ " FROM TB_USER_APP_ORGAN"
												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
										+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
					;
			}			
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			
			//organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ") : organ);
			organ = (organ <= 0 ? this.cfg.getLong("user.organ") : (this.cfg.getInt("user.organ.depth") > depth ? this.cfg.getLong("user.organ") : organ));
			if (organ > 0) { // 조직
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ ")"
					;
			} 			
		}
// 2017.06.26 jwhwang
//		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
//			WHERE += " AND USER_SEQ = " + this.cfg.getLong("user.seq");
//		} else {
//			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
//				WHERE += " AND COMPANY_SEQ = " + this.cfg.getLong("user.company");
//			}
//
//			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
//				WHERE += " AND ORGANIZATION_SEQ IN ("
//							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//									+ " SEQ"
//								+ " FROM TB_ORGANIZATION"
//								+ " WHERE SORT = 1"
//							//20160121 타소속의 조직이 검색되지 않도록 COMPANY_SEQ 조건 추가
//								+ " START WITH COMPANY_SEQ = " + this.cfg.getLong("user.company")
//									+ " AND SEQ IN ("
//											+ " SELECT " + this.cfg.getLong("user.organ") + " FROM DUAL"
//											+ " UNION"
//											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
//													+ " ORGANIZATION_SEQ"
//												+ " FROM TB_USER_APP_ORGAN"
//												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
//										+ " )"
//								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//						+ " )"
//					;
//			}
//		}
//
//		if (company > 0) { // 소속
//			WHERE += " AND COMPANY_SEQ = " + company;
//		}
//
//		if (organ > 0) { // 조직
//			WHERE += " AND ORGANIZATION_SEQ IN ("
//						+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//								+ " SEQ"
//							+ " FROM TB_ORGANIZATION"
//							+ " WHERE SORT = 1"
//							+ " START WITH SEQ = " + organ
//							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//					+ " )"
//				;
//		}

		if (place > 0) { // 위치
			WHERE += " AND VM_PLACE_SEQ = " + place;
		}

	//20160630 결제유형 다중선택 추가
		if ((payTypes != null) && (payTypes.length > 0)) {
			WHERE += " AND PAY_TYPE IN ('" + payTypes[0] + "'";
			for (int i = 1; i < payTypes.length; i++) WHERE += ", '" + payTypes[i] + "'";
			WHERE += ")";
	//20170329 키갱신 제외
		} else {
			WHERE += " AND A.PAY_TYPE <> '09'";
		}

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();

		try {
			long[] cnt = {0, 0, 0, 0, 0, 0};//scheo 2018.10.10 추가
			long[] amt = {0, 0, 0, 0, 0, 0};//scheo 2018.10.10 추가

			ps = dbLib.prepareStatement(conn,
					" SELECT /*+ ORDERED USE_HASH(B C D E) */"
							+ " A.*,"
							+ " B.NAME AS ORGAN,"
							+ " B.CODE AS ORGAN_CODE," //2017.12.21 jwhwang 추가
							+ " C.PLACE AS PLACE,"
							+ " D.CODE AS VM_CODE,"
							+ " DECODE(SUBSTR(PRODUCT_CODE, 1, 1), '장', PRODUCT_CODE, NVL (E.NAME, '미등록 상품[' || A.COL_NO || ']')) AS PRODUCT, " //scheo 20181214 유광권부장요청 - 20190619 scheo 원복 / scheo 20220321 장바구니내역 추가
							//+ " case when E.name is null then case when length(A.col_no) = '2' then '미등록 상품[' || '00' || A.col_no || ']' when length(A.col_no) = '1' then '미등록 상품[' || '000' || A.col_no || ']' when MOD(A.col_no,100) || TRUNC(A.col_no/100) <= '999' then '미등록 상품[' || '0' || MOD(A.col_no,100) || TRUNC(A.col_no/100) || ']' else '미등록 상품[' || MOD(A.col_no,100) || TRUNC(A.col_no/100) || ']' end else E.name end as PRODUCT ,"
							+ " E.CODE AS PRODUCT_CODE "
						+ " FROM ("
									+ " SELECT /*+ INDEX(A IX_SALES_" + YYYYMMDD + ") */"
											+ " COMPANY_SEQ,"
											+ " ORGANIZATION_SEQ,"
											+ " VM_PLACE_SEQ,"
											+ " TERMINAL_ID,"
											+ " PRODUCT_CODE,"
											+ " COL_NO, "
											+ " COUNT(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN AMOUNT END), 0) AS AMOUNT_CARD,"
											//scheo 2018.10.10 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN AMOUNT END), 0) AS AMOUNT_PAYCO,"
											//scheo 2021.09.07 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '80' THEN 1 END) AS CNT_ITEM,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '80' THEN AMOUNT END), 0) AS AMOUNT_ITEM,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN AMOUNT END), 0) AS AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN AMOUNT END), 0) AS AMOUNT_PREPAY"
										+ " FROM TB_SALES A"
										+ " WHERE " + YYYYMMDD + " BETWEEN '"+ sDate + "' AND '" + eDate + "'"
											+ " AND PAY_STEP NOT IN ('00', '99')"
											+ WHERE
											+ " AND A.ITEM_COUNT = 1 "
										+ " GROUP BY COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, TERMINAL_ID, PRODUCT_CODE, COL_NO"
									
									+ " UNION ALL"	// scheo 20220321 장바구니 내역 추가
									+ " SELECT " 
										+ " COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, TERMINAL_ID, '장바구니 ' || PRODUCT_CODE || '건' PRODUCT_CODE , COL_NO, " 
										+ " SUM(CNT_CARD), SUM(AMOUNT_CARD), SUM(CNT_PAYCO), SUM(AMOUNT_PAYCO), SUM(CNT_ITEM), " 
										+ " SUM(AMOUNT_ITEM), SUM(CNT_CASH), SUM(AMOUNT_CASH), SUM(CNT_PREPAY), SUM(AMOUNT_PREPAY) " 
									+ " FROM ( "
										+ " SELECT                              /*+ INDEX(A IX_SALES_TRANSACTION_DATE) */ "
											+ " COMPANY_SEQ, "
											+ " ORGANIZATION_SEQ, "
											+ " VM_PLACE_SEQ, "
											+ " TERMINAL_ID, "
											+ " TO_CHAR(ITEM_COUNT) PRODUCT_CODE, "
											+ " 0 COL_NO, "
											+ " COUNT (CASE WHEN PAY_TYPE = '01' AND pay_step <> '06' THEN 1 END) "
											+ " AS CNT_CARD, "
											+ " NVL(SUM( CASE WHEN PAY_TYPE = '01' AND pay_step <> '06' THEN AMOUNT END), 0) AS AMOUNT_CARD, "
											+ " COUNT (CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO, "
									        + " NVL (SUM (CASE WHEN PAY_TYPE = '07' THEN AMOUNT END), 0) AS AMOUNT_PAYCO, "
									        + " COUNT (CASE WHEN PAY_TYPE = '80' THEN 1 END) AS CNT_ITEM, "
									        + " NVL (SUM (CASE WHEN PAY_TYPE = '80' THEN AMOUNT END), 0) AS AMOUNT_ITEM, "
									        + " COUNT (CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH, "
									        + " NVL (SUM (CASE WHEN PAY_TYPE IN ('02', '10') THEN AMOUNT END), 0) AS AMOUNT_CASH, "
									        + " COUNT (CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY, "
									        + " NVL (SUM (CASE WHEN PAY_TYPE = '11' THEN AMOUNT END), 0) AS AMOUNT_PREPAY "
										+ " FROM TB_SALES A"
										+ " WHERE " + YYYYMMDD + " BETWEEN '"+ sDate + "' AND '" + eDate + "'"
											+ " AND PAY_STEP NOT IN ('00', '99')"
											+ WHERE
											+ " AND A.ITEM_COUNT > 1 "
										+ " GROUP BY COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, TERMINAL_ID, PRODUCT_CODE, COL_NO, ITEM_COUNT	) "
									+ " GROUP BY   COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, TERMINAL_ID, PRODUCT_CODE , COL_NO "
									+ " UNION ALL"
									+ " SELECT"
											+ " COMPANY_SEQ,"
											+ " ORGANIZATION_SEQ,"
											+ " VM_PLACE_SEQ,"
											+ " TERMINAL_ID,"
											+ " PRODUCT_CODE,"
											+ " COL_NO, "
											+ " COUNT(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN -AMOUNT END), 0) AS AMOUNT_CARD,"
											//scheo 2018.10.10 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN -AMOUNT END), 0) AS AMOUNT_PAYCO,"
											//scheo 2021.09.07 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '80' THEN 1 END) AS CNT_ITEM,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '80' THEN -AMOUNT END), 0) AS AMOUNT_ITEM,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN -AMOUNT END), 0) AS AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN -AMOUNT END), 0) AS AMOUNT_PREPAY"
										+ " FROM ("
												+ " SELECT /*+ INDEX(A IX_SALES_PAY_STEP) */"
										+ (
											// 마감일
												"02".equals(sType) ? (
														" CASE WHEN (CANCEL_DATE || CANCEL_TIME) < (CLOSING_DATE || CLOSING_TIME)"
															+ " THEN CLOSING_DATE"
															+ " ELSE ("
																	+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																			+ " SUBSTR(MIN(CLOSING_DATE || CLOSING_TIME), 1, 8)"
																		+ " FROM TB_TXT_CLOSING"
																		+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																			+ " AND (CLOSING_DATE || CLOSING_TIME) >= (A.CANCEL_DATE || A.CANCEL_TIME)"
																+ " )"
															+ " END AS YYYYMMDD,"
													) :
											// 입금일/입금예정일
												"03".equals(sType) || "05".equals(sType) ? (
														" F_GET_WORKDAY@VANBT(CANCEL_DATE, 5) AS YYYYMMDD,"
													) :
											// 매입청구일
												"04".equals(sType) ? (
														" F_GET_WORKDAY@VANBT(CANCEL_DATE, 1) AS YYYYMMDD,"
													) :
											// 거래일
												(
														" CANCEL_DATE AS YYYYMMDD,"
													)
											)
														+ " A.*"
													+ " FROM TB_SALES A"
													+ " WHERE PAY_STEP = '29'"
														+ " AND " + YYYYMMDD + " IS NOT NULL"
														+ WHERE
										+ (
											// 마감일
												"02".equals(sType) ? (
														" AND (CANCEL_DATE || CANCEL_TIME) BETWEEN ("
																+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																		+ " MAX(CLOSING_DATE || CLOSING_TIME) || '0'"
																	+ " FROM TB_TXT_CLOSING"
																	+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																		+ " AND (CLOSING_DATE || CLOSING_TIME) < '" + sDate + "'"
															+ " ) AND ("
																+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																		+ " MAX(CLOSING_DATE || CLOSING_TIME)"
																	+ " FROM TB_TXT_CLOSING"
																	+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																	+ " AND (CLOSING_DATE || CLOSING_TIME) <= '" + eDate + "235959'"
															+ " )"
														+ " AND CLOSING_DATE <= '" + eDate + "'"
														+ " AND CANCEL_DATE <= '" + eDate + "'"
													) :
											// 입금일/입금예정일
												"03".equals(sType) || "05".equals(sType) ? (
														" AND CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -5) AND F_GET_WORKDAY@VANBT('" + eDate + "', -5)"
													) :
											// 매입청구일
												"04".equals(sType) ? (
														" AND CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -1) AND F_GET_WORKDAY@VANBT('" + eDate + "', -1)"
													) :
											// 거래일
												(
														" AND CANCEL_DATE BETWEEN '" + sDate + "' AND '" + eDate + "'"
													)
											)
											+ " ) A"
										+ " WHERE YYYYMMDD BETWEEN '" + sDate + "' AND '" + eDate + "'"
										+ " GROUP BY COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, TERMINAL_ID, PRODUCT_CODE, COL_NO"
								+ " ) A"
							+ " INNER JOIN TB_ORGANIZATION B"
								+ " ON A.ORGANIZATION_SEQ = B.SEQ"
							+ " INNER JOIN TB_VENDING_MACHINE_PLACE C"
								+ " ON A.VM_PLACE_SEQ = C.SEQ"
							+ " INNER JOIN TB_VENDING_MACHINE D"
								+ " ON C.VM_SEQ = D.SEQ"
							+ " LEFT OUTER JOIN TB_PRODUCT E "
								+ " ON A.COMPANY_SEQ = E.COMPANY_SEQ "
									+ " AND A.PRODUCT_CODE = E.CODE "
						+ " ORDER BY"
// 정렬 값(oMode, oType)에 따라 ORDER BY 변경
	+ ((oType == 1) ? (	// DESC
				// 거래처
					(oMode == 1) ? (
							" ORGAN DESC, PLACE DESC"
						) :
				// default: 설치장소
					(
							" PLACE DESC, ORGAN DESC"
						)
			) : (	// ASC
				// 거래처
					(oMode == 1) ? (
							" ORGAN, PLACE"
						) :
				// default: 설치장소
					(
							" PLACE, ORGAN"
						)
				)
		)
							+ ", VM_CODE, COL_NO, A.AMOUNT_CARD DESC"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("VM_PLACE_SEQ", rs.getLong("VM_PLACE_SEQ"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("ORGAN_CODE", rs.getString("ORGAN_CODE")); //2017.12.21 jwhwang 추가
				c.put("PLACE", rs.getString("PLACE"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("VM_CODE", rs.getString("VM_CODE"));
				c.put("COL_NO", rs.getString("COL_NO"));
				c.put("PRODUCT", rs.getString("PRODUCT"));
				c.put("PRODUCT_CODE", rs.getString("PRODUCT_CODE"));
				
				c.put("CNT_CARD", rs.getLong("CNT_CARD"));
				c.put("AMOUNT_CARD", rs.getLong("AMOUNT_CARD"));
				c.put("CNT_PAYCO", rs.getLong("CNT_PAYCO"));//scheo 2018.10.10 추가
				c.put("AMOUNT_PAYCO", rs.getLong("AMOUNT_PAYCO"));//scheo 2018.10.10 추가
				c.put("CNT_ITEM", rs.getLong("CNT_ITEM"));//scheo 2021.09.07 추가
				c.put("AMOUNT_ITEM", rs.getLong("AMOUNT_ITEM"));//scheo 2021.09.07 추가
				c.put("CNT_CASH", rs.getLong("CNT_CASH"));
				c.put("AMOUNT_CASH", rs.getLong("AMOUNT_CASH"));
				c.put("CNT_PREPAY", rs.getLong("CNT_PREPAY"));
				c.put("AMOUNT_PREPAY", rs.getLong("AMOUNT_PREPAY"));

				
				
				c.put("CNT_TOTAL", c.getLong("CNT_CARD") + c.getLong("CNT_CASH") + c.getLong("CNT_PREPAY") + c.getLong("CNT_PAYCO"));
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_CARD") + c.getLong("AMOUNT_CASH") + c.getLong("AMOUNT_PREPAY") + c.getLong("AMOUNT_PAYCO"));

				this.list.add(c);

				cnt[0] += c.getLong("CNT_CARD");
				cnt[1] += c.getLong("CNT_PAYCO");//scheo 2018.10.10 추가
				cnt[2] += c.getLong("CNT_ITEM");//scheo 2021.09.07 추가
				cnt[3] += c.getLong("CNT_CASH");
				cnt[4] += c.getLong("CNT_PREPAY");
				cnt[5] += c.getLong("CNT_TOTAL");
				amt[0] += c.getLong("AMOUNT_CARD");
				amt[1] += c.getLong("AMOUNT_PAYCO");//scheo 2018.10.10 추가
				amt[2] += c.getLong("AMOUNT_ITEM");//scheo 2021.09.07 추가
				amt[3] += c.getLong("AMOUNT_CASH");
				amt[4] += c.getLong("AMOUNT_PREPAY");
				amt[5] += c.getLong("AMOUNT_TOTAL");
			}

			this.data.put("CNT_CARD", cnt[0]);
			this.data.put("CNT_PAYCO", cnt[1]);//scheo 2018.10.10 추가
			this.data.put("CNT_ITEM", cnt[2]);//scheo 2021.09.07 추가
			this.data.put("CNT_CASH", cnt[3]);
			this.data.put("CNT_PREPAY", cnt[4]);
			this.data.put("CNT_TOTAL", cnt[5]);
			this.data.put("AMOUNT_CARD", amt[0]);
			this.data.put("AMOUNT_PAYCO", amt[1]);//scheo 2018.10.10 추가
			this.data.put("AMOUNT_ITEM", amt[2]);//scheo 2021.09.07 추가
			this.data.put("AMOUNT_CASH", amt[3]);
			this.data.put("AMOUNT_PREPAY", amt[4]);
			this.data.put("AMOUNT_TOTAL", amt[5]);
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 조직명
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
							+ " FROM ("
									+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
											+ " NAME"
										+ " FROM TB_ORGANIZATION"
										+ " WHERE COMPANY_SEQ = " + company
											+ " AND SORT = 0"
										+ " ORDER BY DEPTH DESC"
								+ " )"
							+ " WHERE ROWNUM = 1"
					)
			);

	// 검색 설정
		String sDesc = "집계기준="
				+ dbLib.getResult(conn,
						"SELECT /*+ INDEX(TB_CODE) */"
								+ " NAME"
							+ " FROM TB_CODE"
							+ " WHERE TYPE = 'SUM_TYPE'"
								+ " AND CODE = '" + sType + "'"
					)
				+ "&집계기간=" + sDate + "-" + eDate
			;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}

		this.data.put("sDesc", sDesc);

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
	/**
	 * 상품별 매출집계
	 *
	 * @param sType 집계 유형
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 시작일
	 * @param eDate 종료일
	 * @param goods 상품
	 * @param payTypes 결제유형
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	//public String goods(String sType, long company, long organ, long place, String sDate, String eDate, String goods, String[] payTypes, int oMode, int oType) throws Exception {
	public String product(String sType, long company, long organ, long depth, long place, String sDate, String eDate, String product, String[] payTypes, int oMode, int oType) throws Exception {
			
		// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sType) || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
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
			return "DB 연결에 실패하였습니다.";
		}

	// 검색절 생성
		String WHERE = "";
		String YYYYMMDD
				= "01".equals(sType) ? "TRANSACTION_DATE"	// 거래일
				: "02".equals(sType) ? "CLOSING_DATE"		// 마감일
				: "03".equals(sType) ? "PAY_DATE"			// 입금일
				: "04".equals(sType) ? "PURCHASE_DATE"		// 매입청구일
				: "05".equals(sType) ? "PAY_DATE_EXP"		// 입금예정일
				: "CANCEL_DATE"								// 취소일/마감일(취소)/환수일/매입취소일/환수예정일 - 미사용
			;
		
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 
			company = (company > 0 ? company : this.cfg.getLong("user.company"));
			if (company > 0) { // 소속
				WHERE += " AND A.COMPANY_SEQ = " + company;
			} 
			
			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			if (organ > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
							//20160121 타소속의 조직이 검색되지 않도록 COMPANY_SEQ 조건 추가
								+ " START WITH COMPANY_SEQ = " + company
									+ " AND SEQ IN ("
											+ " SELECT " + organ + " FROM DUAL"
											+ " UNION"
											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
													+ " ORGANIZATION_SEQ"
												+ " FROM TB_USER_APP_ORGAN"
												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
										+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
					;
			}
			
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			
			//organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ") : organ);
			organ = (organ <= 0 ? this.cfg.getLong("user.organ") : (this.cfg.getInt("user.organ.depth") > depth ? this.cfg.getLong("user.organ") : organ));
			if (organ > 0) { // 조직
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ ")"
					;
			} 
		}
		
// 2017.06.26 jwhwang		
//		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
//			WHERE += " AND USER_SEQ = " + this.cfg.getLong("user.seq");
//		} else {
//			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
//				WHERE += " AND COMPANY_SEQ = " + this.cfg.getLong("user.company");
//			}
//
//			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
//				WHERE += " AND ORGANIZATION_SEQ IN ("
//							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//									+ " SEQ"
//								+ " FROM TB_ORGANIZATION"
//								+ " WHERE SORT = 1"
//							//20160121 타소속의 조직이 검색되지 않도록 COMPANY_SEQ 조건 추가
//								+ " START WITH COMPANY_SEQ = " + this.cfg.getLong("user.company")
//									+ " AND SEQ IN ("
//											+ " SELECT " + this.cfg.getLong("user.organ") + " FROM DUAL"
//											+ " UNION"
//											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
//													+ " ORGANIZATION_SEQ"
//												+ " FROM TB_USER_APP_ORGAN"
//												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
//										+ " )"
//								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//						+ " )"
//					;
//			}
//		}
//
//		if (company > 0) { // 소속
//			WHERE += " AND COMPANY_SEQ = " + company;
//		}
//
//		if (organ > 0) { // 조직
//			WHERE += " AND ORGANIZATION_SEQ IN ("
//						+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//								+ " SEQ"
//							+ " FROM TB_ORGANIZATION"
//							+ " WHERE SORT = 1"
//							+ " START WITH SEQ = " + organ
//							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//					+ " )"
//				;
//		}

		if (place > 0) { // 위치
			WHERE += " AND VM_PLACE_SEQ = " + place;
		}

	//20160630 결제유형 다중선택 추가
		if ((payTypes != null) && (payTypes.length > 0)) {
			WHERE += " AND PAY_TYPE IN ('" + payTypes[0] + "'";
			for (int i = 1; i < payTypes.length; i++) WHERE += ", '" + payTypes[i] + "'";
			WHERE += ")";
	//20170329 키갱신 제외
		} else {
			WHERE += " AND A.PAY_TYPE <> '09'";
		}

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();

		try {
			long[] cnt = {0, 0, 0, 0, 0, 0};//scheo 2018.10.10 추가
			long[] amt = {0, 0, 0, 0, 0, 0};//scheo 2018.10.10 추가

			ps = dbLib.prepareStatement(conn,
					" SELECT /*+ ORDERED USE_HASH(B C D) */"
							+ " A.*,"
							+ " B.NAME AS ORGAN,"
							+ " B.CODE AS ORGAN_CODE," //2017.12.21 jwhwang 추가
							+ " C.PLACE AS PLACE,"
							+ " D.CODE AS VM_CODE"
						+ " FROM ("
				        		+ " SELECT AA.TYPE, AA.COMPANY_SEQ, "
				        			+ " AA.ORGANIZATION_SEQ, "
				        			+ " AA.VM_PLACE_SEQ, "
				        			+ " AA.PRODUCT_CODE, "
				        			+ " SUM(AA.CNT_CARD) CNT_CARD, "
				        			+ " SUM(AA.AMOUNT_CARD) AMOUNT_CARD, "
				        			//scheo 2018.10.10 추가
				        			+ " SUM(AA.CNT_PAYCO) CNT_PAYCO, "
				        			+ " SUM(AA.AMOUNT_PAYCO) AMOUNT_PAYCO, "
				        			//scheo 2021.09.07 추가
				        			+ " SUM(AA.CNT_ITEM) CNT_ITEM, "
				        			+ " SUM(AA.AMOUNT_ITEM) AMOUNT_ITEM, "
				        			+ " SUM(AA.CNT_CASH) CNT_CASH, "
				        			+ " SUM(AA.AMOUNT_CASH) AMOUNT_CASH, "
				        			+ " SUM(AA.CNT_PREPAY) CNT_PREPAY, "
				        			+ " SUM(AA.AMOUNT_PREPAY) AMOUNT_PREPAY, "
				        			+ " DECODE(SUBSTR(PRODUCT_CODE, 1, 1), '장', PRODUCT_CODE, NVL (E.NAME, '미등록 상품[' || AA.COL_NO || ']')) AS PRODUCT " //scheo 20181214 유광권부장 요청 - 20190619 scheo 원복 // scheo 20220322 장바구니 내역 추가
				        			//+ " case when E.name is null then case when length(AA.col_no) = '2' then '미등록 상품[' || '00' || AA.col_no || ']' when length(AA.col_no) = '1' then '미등록 상품[' || '000' || AA.col_no || ']' when MOD(AA.col_no,100) || TRUNC(AA.col_no/100) <= '999' then '미등록 상품[' || '0' || MOD(AA.col_no,100) || TRUNC(AA.col_no/100) || ']' else '미등록 상품[' || MOD(AA.col_no,100) || TRUNC(AA.col_no/100) || ']' end else E.name end as PRODUCT"
				        		+ " FROM (	"					
				
									+ " SELECT /*+ INDEX(A IX_SALES_" + YYYYMMDD + ") */"
											+ " 'A' TYPE, COMPANY_SEQ,"
											+ " ORGANIZATION_SEQ,"
											+ " VM_PLACE_SEQ,"
											+ " PRODUCT_CODE, "
											//+ " NVL(GOODS_SEQ, -COL_NO) AS GOODS_SEQ,"
											+ " COL_NO,"
											+ " COUNT(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN AMOUNT END), 0) AS AMOUNT_CARD,"
											//scheo 2018.10.10 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN AMOUNT END), 0) AS AMOUNT_PAYCO,"
											//scheo 2021.09.07 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '80' THEN 1 END) AS CNT_ITEM,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '80' THEN AMOUNT END), 0) AS AMOUNT_ITEM,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN AMOUNT END), 0) AS AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN AMOUNT END), 0) AS AMOUNT_PREPAY"
										+ " FROM TB_SALES A"
										+ " WHERE " + YYYYMMDD + " BETWEEN '"+ sDate + "' AND '" + eDate + "'"
											+ " AND PAY_STEP NOT IN ('00', '99')"
											+ " AND A.ITEM_COUNT = 1 "	 // scheo 20220322 장바구니 내역 추가
											+ WHERE
										//+ " GROUP BY COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, GOODS_SEQ, COL_NO"
										//+ " GROUP BY COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, NVL(GOODS_SEQ, -COL_NO)"
										+ " GROUP BY COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, PRODUCT_CODE, COL_NO"
									+ " UNION ALL"	// scheo 20220322 장바구니 내역 추가
									+ " SELECT " 
										+ " TYPE, COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, '장바구니 ' || PRODUCT_CODE || '건' PRODUCT_CODE, COL_NO, " 
										+ " SUM(CNT_CARD), SUM(AMOUNT_CARD), SUM(CNT_PAYCO), SUM(AMOUNT_PAYCO), SUM(CNT_ITEM), " 
										+ " SUM(AMOUNT_ITEM), SUM(CNT_CASH), SUM(AMOUNT_CASH), SUM(CNT_PREPAY), SUM(AMOUNT_PREPAY) " 
									+ " FROM ( "
										+ " SELECT /*+ INDEX(A IX_SALES_" + YYYYMMDD + ") */"
											+ " 'A' TYPE, COMPANY_SEQ,"
											+ " ORGANIZATION_SEQ,"
											+ " VM_PLACE_SEQ,"
											+ " TO_CHAR(ITEM_COUNT) PRODUCT_CODE, "
											//+ " NVL(GOODS_SEQ, -COL_NO) AS GOODS_SEQ,"
											+ " 0 COL_NO,"
											+ " COUNT(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN AMOUNT END), 0) AS AMOUNT_CARD,"
											//scheo 2018.10.10 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN AMOUNT END), 0) AS AMOUNT_PAYCO,"
											//scheo 2021.09.07 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '80' THEN 1 END) AS CNT_ITEM,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '80' THEN AMOUNT END), 0) AS AMOUNT_ITEM,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN AMOUNT END), 0) AS AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN AMOUNT END), 0) AS AMOUNT_PREPAY"
										+ " FROM TB_SALES A"
										+ " WHERE " + YYYYMMDD + " BETWEEN '"+ sDate + "' AND '" + eDate + "'"
											+ " AND PAY_STEP NOT IN ('00', '99')"
											+ " AND A.ITEM_COUNT > 1 "
											+ WHERE
										//+ " GROUP BY COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, GOODS_SEQ, COL_NO"
										//+ " GROUP BY COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, NVL(GOODS_SEQ, -COL_NO)"
										+ " GROUP BY COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, ITEM_COUNT, COL_NO )"
									+ " GROUP BY TYPE, COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, PRODUCT_CODE, COL_NO"
										
									+ " UNION ALL"
									+ " SELECT"
											+ " 'C' TYPE, COMPANY_SEQ,"
											+ " ORGANIZATION_SEQ,"
											+ " VM_PLACE_SEQ,"
											+ " PRODUCT_CODE, "
											//+ " NVL(GOODS_SEQ, -COL_NO) AS GOODS_SEQ,"
											+ " COL_NO,"
											+ " COUNT(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN -AMOUNT END), 0) AS AMOUNT_CARD,"
											//scheo 2018.10.10 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN -AMOUNT END), 0) AS AMOUNT_PAYCO,"
											//scheo 2021.09.07 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '80' THEN 1 END) AS CNT_ITEM,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '80' THEN -AMOUNT END), 0) AS AMOUNT_ITEM,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN -AMOUNT END), 0) AS AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN -AMOUNT END), 0) AS AMOUNT_PREPAY"
										+ " FROM ("
												+ " SELECT /*+ INDEX(A IX_SALES_PAY_STEP) */"
										+ (
											// 마감일
												"02".equals(sType) ? (
														" CASE WHEN (CANCEL_DATE || CANCEL_TIME) < (CLOSING_DATE || CLOSING_TIME)"
															+ " THEN CLOSING_DATE"
															+ " ELSE ("
																	+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																			+ " SUBSTR(MIN(CLOSING_DATE || CLOSING_TIME), 1, 8)"
																		+ " FROM TB_TXT_CLOSING"
																		+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																			+ " AND (CLOSING_DATE || CLOSING_TIME) >= (A.CANCEL_DATE || A.CANCEL_TIME)"
																+ " )"
															+ " END AS YYYYMMDD,"
													) :
											// 입금일/입금예정일
												"03".equals(sType) || "05".equals(sType) ? (
														" F_GET_WORKDAY@VANBT(CANCEL_DATE, 5) AS YYYYMMDD,"
													) :
											// 매입청구일
												"04".equals(sType) ? (
														" F_GET_WORKDAY@VANBT(CANCEL_DATE, 1) AS YYYYMMDD,"
													) :
											// 거래일
												(
														" CANCEL_DATE AS YYYYMMDD,"
													)
												)
														+ " A.*"
													+ " FROM TB_SALES A"
													+ " WHERE PAY_STEP = '29'"
														+ " AND " + YYYYMMDD + " IS NOT NULL"
														+ WHERE
										+ (
											// 마감일
												"02".equals(sType) ? (
														" AND (CANCEL_DATE || CANCEL_TIME) BETWEEN ("
																+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																		+ " MAX(CLOSING_DATE || CLOSING_TIME) || '0'"
																	+ " FROM TB_TXT_CLOSING"
																	+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																		+ " AND (CLOSING_DATE || CLOSING_TIME) < '" + sDate + "'"
															+ " ) AND ("
																+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																		+ " MAX(CLOSING_DATE || CLOSING_TIME)"
																	+ " FROM TB_TXT_CLOSING"
																	+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																	+ " AND (CLOSING_DATE || CLOSING_TIME) <= '" + eDate + "235959'"
															+ " )"
														+ " AND CLOSING_DATE <= '" + eDate + "'"
														+ " AND CANCEL_DATE <= '" + eDate + "'"
													) :
											// 입금일/입금예정일
												"03".equals(sType) || "05".equals(sType) ? (
														" AND CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -5) AND F_GET_WORKDAY@VANBT('" + eDate + "', -5)"
													) :
											// 매입청구일
												"04".equals(sType) ? (
														" AND CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -1) AND F_GET_WORKDAY@VANBT('" + eDate + "', -1)"
													) :
											// 거래일
												(
														" AND CANCEL_DATE BETWEEN '" + sDate + "' AND '" + eDate + "'"
													)
											)
											+ " ) A"
										+ " WHERE YYYYMMDD BETWEEN '" + sDate + "' AND '" + eDate + "'"
										//+ " GROUP BY COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, GOODS_SEQ, COL_NO"
										//+ " GROUP BY COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, NVL(GOODS_SEQ, -COL_NO)"
										+ " GROUP BY COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, PRODUCT_CODE, COL_NO"
									+ " ) AA "
									+ 	(!StringEx.isEmpty(product) ? (	
											" INNER JOIN TB_PRODUCT E ON AA.COMPANY_SEQ = E.COMPANY_SEQ AND AA.PRODUCT_CODE = E.CODE  "
												+ " AND E.NAME LIKE '%" + product + "%'"
										) : (
											" LEFT OUTER JOIN TB_PRODUCT E ON AA.COMPANY_SEQ = E.COMPANY_SEQ AND AA.PRODUCT_CODE = E.CODE  "
										))	
									+ " GROUP BY AA.TYPE, AA.COMPANY_SEQ, AA.ORGANIZATION_SEQ, AA.VM_PLACE_SEQ, AA.PRODUCT_CODE, NVL(E.NAME, '미등록 상품[' || AA.COL_NO || ']') " //scheo 20181214 유광권부장 요청 - 20190619 scheo 원복
									//+ " GROUP BY AA.COMPANY_SEQ, AA.ORGANIZATION_SEQ, AA.VM_PLACE_SEQ, AA.PRODUCT_CODE, case when E.name is null then case when length(AA.col_no) = '2' then '미등록 상품[' || '00' || AA.col_no || ']' when length(AA.col_no) = '1' then '미등록 상품[' || '000' || AA.col_no || ']' when MOD(AA.col_no,100) || TRUNC(AA.col_no/100) <= '999' then '미등록 상품[' || '0' || MOD(AA.col_no,100) || TRUNC(AA.col_no/100) || ']' else '미등록 상품[' || MOD(AA.col_no,100) || TRUNC(AA.col_no/100) || ']' end else E.name end "
								+ " ) A"
							+ " INNER JOIN TB_ORGANIZATION B"
								+ " ON A.ORGANIZATION_SEQ = B.SEQ"
							+ " INNER JOIN TB_VENDING_MACHINE_PLACE C"
								+ " ON A.VM_PLACE_SEQ = C.SEQ"
							+ " INNER JOIN TB_VENDING_MACHINE D"
								+ " ON C.VM_SEQ = D.SEQ"
							//+ " INNER JOIN TB_PRODUCT E"
							//	+ " ON A.COMPANY_SEQ = E.COMPANY_SEQ AND A.PRODUCT_CODE = E.CODE "
							//+ " LEFT JOIN TB_GOODS E"
							//	+ " ON A.GOODS_SEQ = E.SEQ"
									// 상품
									//+ (!StringEx.isEmpty(product) ? (
									//			" AND E.NAME LIKE '%" + product + "%'"
									//		) : ""
									//	)
								+ " ORDER BY"
// 정렬 값(oMode, oType)에 따라 ORDER BY 변경
	+ ((oType == 1) ? (	// DESC
				// 거래처
					(oMode == 1) ? (
							" ORGAN DESC, PRODUCT, PLACE DESC"
						) :
				// 설치장소
					(oMode == 2) ? (
							" ORGAN DESC, PLACE DESC, PRODUCT"
						) :
				// default: 날짜
					(
							" PRODUCT DESC, ORGAN, PLACE"
						)
			) : (	// ASC
				// 거래처
					(oMode == 1) ? (
							" ORGAN, PRODUCT, PLACE"
						) :
				// 설치장소
					(oMode == 2) ? (
							" ORGAN, PLACE, PRODUCT"
						) :
				// default: 날짜
					(
							" PRODUCT, ORGAN, PLACE"
						)
				)
		)
							+ ", VM_CODE, A.AMOUNT_CARD DESC"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("VM_PLACE_SEQ", rs.getLong("VM_PLACE_SEQ"));
				c.put("PRODUCT_CODE", rs.getString("PRODUCT_CODE"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("ORGAN_CODE", rs.getString("ORGAN_CODE")); //2017.12.21 jwhwang 추가
				c.put("PLACE", rs.getString("PLACE"));
				c.put("PRODUCT", rs.getString("PRODUCT"));
				//c.put("COL_NO", rs.getString("COL_NO"));
				c.put("VM_CODE", rs.getString("VM_CODE"));

				c.put("CNT_CARD", rs.getLong("CNT_CARD"));
				c.put("AMOUNT_CARD", rs.getLong("AMOUNT_CARD"));
				c.put("CNT_PAYCO", rs.getLong("CNT_PAYCO"));//scheo 2018.10.10 추가
				c.put("AMOUNT_PAYCO", rs.getLong("AMOUNT_PAYCO"));//scheo 2018.10.10 추가
				c.put("CNT_ITEM", rs.getLong("CNT_ITEM"));//scheo 2021.09.07 추가
				c.put("AMOUNT_ITEM", rs.getLong("AMOUNT_ITEM"));//scheo 2021.09.07 추가
				c.put("CNT_CASH", rs.getLong("CNT_CASH"));
				c.put("AMOUNT_CASH", rs.getLong("AMOUNT_CASH"));
				c.put("CNT_PREPAY", rs.getLong("CNT_PREPAY"));
				c.put("AMOUNT_PREPAY", rs.getLong("AMOUNT_PREPAY"));

				c.put("CNT_TOTAL", c.getLong("CNT_CARD") + c.getLong("CNT_PAYCO") + c.getLong("CNT_CASH") + c.getLong("CNT_PREPAY"));//scheo 2018.10.10 추가
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_CARD") + c.getLong("AMOUNT_PAYCO") + c.getLong("AMOUNT_CASH") + c.getLong("AMOUNT_PREPAY"));//scheo 2018.10.10 추가

				this.list.add(c);

				cnt[0] += c.getLong("CNT_CARD");
				cnt[1] += c.getLong("CNT_PAYCO");//scheo 2018.10.10 추가
				cnt[2] += c.getLong("CNT_ITEM");//scheo 2021.09.07 추가
				cnt[3] += c.getLong("CNT_CASH");
				cnt[4] += c.getLong("CNT_PREPAY");
				cnt[5] += c.getLong("CNT_TOTAL");
				amt[0] += c.getLong("AMOUNT_CARD");
				amt[1] += c.getLong("AMOUNT_PAYCO");//scheo 2018.10.10 추가
				amt[2] += c.getLong("AMOUNT_ITEM");//scheo 2021.09.07 추가
				amt[3] += c.getLong("AMOUNT_CASH");
				amt[4] += c.getLong("AMOUNT_PREPAY");
				amt[5] += c.getLong("AMOUNT_TOTAL");
			}

			this.data.put("CNT_CARD", cnt[0]);
			this.data.put("CNT_PAYCO", cnt[1]);//scheo 2018.10.10 추가
			this.data.put("CNT_ITEM", cnt[2]);//scheo 2021.09.07 추가
			this.data.put("CNT_CASH", cnt[3]);
			this.data.put("CNT_PREPAY", cnt[4]);
			this.data.put("CNT_TOTAL", cnt[5]);
			this.data.put("AMOUNT_CARD", amt[0]);
			this.data.put("AMOUNT_PAYCO", amt[1]);//scheo 2018.10.10 추가
			this.data.put("AMOUNT_ITEM", amt[2]);//scheo 2021.09.07 추가
			this.data.put("AMOUNT_CASH", amt[3]);
			this.data.put("AMOUNT_PREPAY", amt[4]);
			this.data.put("AMOUNT_TOTAL", amt[5]);
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 조직명
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
							+ " FROM ("
									+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
											+ " NAME"
										+ " FROM TB_ORGANIZATION"
										+ " WHERE COMPANY_SEQ = " + company
											+ " AND SORT = 0"
											+ " ORDER BY DEPTH DESC"
								+ " )"
							+ " WHERE ROWNUM = 1"
					)
			);

	// 검색 설정
		String sDesc = "집계기준="
				+ dbLib.getResult(conn,
						"SELECT /*+ INDEX(TB_CODE) */"
								+ " NAME"
							+ " FROM TB_CODE"
							+ " WHERE TYPE = 'SUM_TYPE'"
								+ " AND CODE = '" + sType + "'"
					)
				+ "&집계기간=" + sDate + "-" + eDate
			;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}

		if (!StringEx.isEmpty(product)) {
			sDesc += "&상품명=" + product;
		}

		this.data.put("sDesc", sDesc);

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}

	
	
	/**
	 * 거래내역 - 엑셀
	 *
	 * @param sType 집계 유형
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 시작일
	 * @param eDate 종료일
	 * @param product 상품
	 * @param payment 결제 방식
	 * @param step 진행 상태
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String sales(String sType, long company, long organ,long depth, long place, String sDate, String eDate, String product, String[] payTypes, String[] paySteps) throws Exception {
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
		String YYYYMMDD
				= "01".equals(sType) ? "TRANSACTION_DATE"	// 거래일
				: "02".equals(sType) ? "CLOSING_DATE"		// 마감일
				: "03".equals(sType) ? "PAY_DATE"			// 입금일
				: "04".equals(sType) ? "PURCHASE_DATE"		// 매입청구일
				: "05".equals(sType) ? "PAY_DATE_EXP"		// 입금예정일
				: "CANCEL_DATE"								// 취소일/마감일(취소)/환수일/매입취소일/환수예정일
			;
	
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 
			company = (company > 0 ? company : this.cfg.getLong("user.company"));
			if (company > 0) { // 소속
				WHERE += " AND A.COMPANY_SEQ = " + company;
			} 
			
			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			if (organ > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
							//20160121 타소속의 조직이 검색되지 않도록 COMPANY_SEQ 조건 추가
								+ " START WITH COMPANY_SEQ = " + company
									+ " AND SEQ IN ("
											+ " SELECT " + organ + " FROM DUAL"
											+ " UNION"
											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
													+ " ORGANIZATION_SEQ"
												+ " FROM TB_USER_APP_ORGAN"
												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
										+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
					;
			}
			
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			
			//organ = (organ<= 0 ? organ: this.cfg.getLong("user.organ"));
			//organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ") : organ);
			organ = (organ <= 0 ? this.cfg.getLong("user.organ") : (this.cfg.getInt("user.organ.depth") > depth ? this.cfg.getLong("user.organ") : organ));
			if (organ > 0) { // 조직
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ ")"
					;
			} 
		}
		
// 2017.06.26 jwhwang
//		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
//			WHERE += " AND A.USER_SEQ = " + this.cfg.getLong("user.seq");
//		} else {
//			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
//				WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
//			}
//
//			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
//				WHERE += " AND A.ORGANIZATION_SEQ IN ("
//							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//									+ " SEQ"
//								+ " FROM TB_ORGANIZATION"
//								+ " WHERE SORT = 1"
//							//20160121 타소속의 조직이 검색되지 않도록 COMPANY_SEQ 조건 추가
//								+ " START WITH COMPANY_SEQ = " + this.cfg.getLong("user.company")
//									+ " AND SEQ IN ("
//											+ " SELECT " + this.cfg.getLong("user.organ") + " FROM DUAL"
//											+ " UNION"
//											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
//													+ " ORGANIZATION_SEQ"
//												+ " FROM TB_USER_APP_ORGAN"
//												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
//										+ " )"
//								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//						+ " )"
//					;
//			}
//		}
//
//		if (company > 0) { // 소속
//			WHERE += " AND A.COMPANY_SEQ = " + company;
//		}

//		if (organ > 0) { // 조직
//			WHERE += " AND A.ORGANIZATION_SEQ IN ("
//						+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//								+ " SEQ"
//							+ " FROM TB_ORGANIZATION"
//							+ " WHERE SORT = 1"
//							+ " START WITH SEQ = " + organ
//							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//					+ ")"
//				;
//		}

		if (place > 0) { // 위치
			WHERE += " AND A.VM_PLACE_SEQ = " + place;
		}

		//if (goods > 0) { // 상품
		//	WHERE += " AND GOODS_SEQ = " + goods;
		//}
		
		if (!StringEx.isEmpty(product)) { // 상품
			WHERE += " AND PRODUCT_CODE = '" + product + "'";
		}

	//20160613 결제유형을 다중선택으로 변경
	//	if (!StringEx.isEmpty(payment)) { // 결제유형
	//		WHERE += " AND A.PAY_TYPE = '" + payment + "'";
	//	}
		if ((payTypes != null) && (payTypes.length > 0)) {
			WHERE += " AND A.PAY_TYPE IN ('" + payTypes[0] + "'";
			for (int i = 1; i < payTypes.length; i++) WHERE += ", '" + payTypes[i] + "'";
			WHERE += ")";
	//20170329 키갱신 제외
		} else {
			WHERE += " AND A.PAY_TYPE <> '09'";
		}

	//20160613 진행상태를 다중선택으로 변경
	//	if (!StringEx.isEmpty(step)) { // 진행상태
	//		WHERE += " AND A.PAY_STEP = '" + step + "'";
	//	}
		if ((paySteps != null) && (paySteps.length > 0)) {
			WHERE += " AND A.PAY_STEP IN ('" + paySteps[0] + "'";
			for (int i = 1; i < paySteps.length; i++) WHERE += ", '" + paySteps[i] + "'";
			WHERE += ")";
	//20170404 집계상세내역 취소/망상취소 제외
		} else if (paySteps != null) {
			WHERE += " AND PAY_STEP NOT IN ('00', '99')";
	//20170329 망상취소 제외
		} else {
			//WHERE += " AND PAY_STEP <> '00'";
			WHERE += " AND ( ((A.AMOUNT = '100' AND A.PAY_TYPE = '07')) OR ((A.AMOUNT <> '100' OR A.PAY_TYPE <> '07') AND A.PAY_STEP <> '00') )"; //20201221 scheo 간편결제 100원 망상취소 출력 (박경환 주임 요청)
		}

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED INDEX(A IX_SALES_" + YYYYMMDD + ") USE_NL(B C D E F G H I) */"
							+ " A.TRANSACTION_DATE || A.TRANSACTION_TIME AS TRANSACTION_DATE,"
							+ " CASE WHEN A.CLOSING_DATE IS NOT NULL THEN A.CLOSING_DATE || NVL(A.CLOSING_TIME, '000000') END AS CLOSING_DATE,"
							+ " A.PURCHASE_DATE,"
							+ " CASE WHEN A.PAY_STEP = '02' THEN A.PAY_DATE_EXP ELSE A.PAY_DATE END AS PAY_DATE,"
							+ " CASE WHEN A.CANCEL_DATE IS NOT NULL THEN A.CANCEL_DATE || NVL(A.CANCEL_TIME, '000000') END AS CANCEL_DATE,"
							+ " A.TERMINAL_ID,"
							+ " A.TRANSACTION_NO,"
							+ " A.COL_NO, "				//자판기 매출정보 > 거래내역  칼럼 내역 엑셀 추가 수정 Start. Chae jong wook 2020-04-27
							+ " A.AMOUNT,"
							+ " B.NAME AS ORGAN,"
							+ " B.CODE AS ORGAN_CODE," //2017.12.21 jwhwang 추가
							+ " C.PLACE,"
							+ " NVL(D.NAME, '미등록 상품[' || A.COL_NO || ']') AS PRODUCT," //scheo123 20181214 유광권부장 요청 - 20190619 scheo 원복
							//+ " case when D.name is null then case when length(A.col_no) = '2' then '미등록 상품[' || '00' || A.col_no || ']' when length(A.col_no) = '1' then '미등록 상품[' || '000' || A.col_no || ']' when MOD(A.col_no,100) || TRUNC(A.col_no/100) <= '999' then '미등록 상품[' || '0' || MOD(A.col_no,100) || TRUNC(A.col_no/100) || ']' else '미등록 상품[' || MOD(A.col_no,100) || TRUNC(A.col_no/100) || ']' end else D.name end as PRODUCT ,"
							+ " D.CODE AS PRODUCT_CODE,"
							+ " D.BAR_CODE AS BAR_CODE,"
							+ " E.CODE AS VM_CODE,"
							+ " F.NAME AS PAY_TYPE,"
							+ " G.NAME AS PAY_STEP,"
						    + " DECODE(A.PAY_TYPE, '01', J.CARD_NO, '02', J.CARD_NO, '10', J.CARD_NO, '07', J.CARD_NO, '11', K.CARD_NO, '') CARD_NO, " 
						    + " DECODE(A.PAY_TYPE, '01', J.PURCHASE_ORGAN_NAME, '02', '현금', '10', '현금', '07', J.PURCHASE_ORGAN_NAME, '11', " 
						        + " DECODE(K.ORGAN_CODE, 'TMN', '티머니', 'CSB', '캐시비', 'MYB', '마이비', 'KRP', '레일플러스', '선불'), '카드') PURCHASE_ORGAN_NAME, "
						    + " DECODE(A.PAY_TYPE, '01', J.APPROVAL_NO, '02', J.APPROVAL_NO, '07', J.APPROVAL_NO, '') APPROVAL_NO, "
							+ " H.NAME AS INPUT_TYPE,"
							+ " I.NAME AS PAY_CARD"
						+ " FROM TB_SALES A"
							+ " INNER JOIN TB_ORGANIZATION B"
								+ " ON A.ORGANIZATION_SEQ = B.SEQ"
							+ " INNER JOIN TB_VENDING_MACHINE_PLACE C"
								+ " ON A.VM_PLACE_SEQ = C.SEQ"
							+ " LEFT JOIN TB_PRODUCT D"
								+ " ON A.COMPANY_SEQ = D.COMPANY_SEQ"
									+ " AND A.PRODUCT_CODE = D.CODE"
							//+ " LEFT JOIN TB_GOODS D"
							//	+ " ON A.GOODS_SEQ = D.SEQ"
						//TODO: FK_SALES_TERMINAL_ID 생성 후 INNER JOIN으로 변경
							+ " LEFT JOIN TB_VENDING_MACHINE E"
								+ " ON A.TERMINAL_ID = E.TERMINAL_ID"
							+ " LEFT JOIN TB_CODE F"
								+ " ON A.PAY_TYPE = F.CODE"
									+ " AND F.TYPE = 'PAY_TYPE'"
							+ " LEFT JOIN TB_CODE G"
								+ " ON A.PAY_STEP = G.CODE"
									+ " AND G.TYPE = 'PAY_STEP'"
							+ " LEFT JOIN TB_CODE H"
								+ " ON A.INPUT_TYPE = H.CODE"
									+ " AND H.TYPE = 'INPUT_TYPE'"
							+ " LEFT JOIN TB_CODE I"
								+ " ON A.PAY_CARD = I.CODE"
									+ " AND I.TYPE = 'PAY_CARD'"
						    + " LEFT OUTER JOIN TB_TXT_TRANSACTION_CREDIT J "
						        + " ON (A.TERMINAL_ID = J.TERMINAL_ID AND A.TRANSACTION_NO = J.TRANSACTION_NO) "
						    + " LEFT OUTER JOIN TB_TXT_TRANSACTION_PREPAY K "
						        + " ON (A.TERMINAL_ID = K.TERMINAL_ID AND A.TRANSACTION_NO = K.TRANSACTION_NO) "
						+ " WHERE"
			+ (
				// 마감일(취소)
					"12".equals(sType) ? (
							" (A.CANCEL_DATE || A.CANCEL_TIME) BETWEEN ("
									+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
											+ " MAX(CLOSING_DATE || CLOSING_TIME) || '0'"
										+ " FROM TB_TXT_CLOSING"
										+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
											+ " AND (CLOSING_DATE || CLOSING_TIME) < '" + sDate + "'"
								+ " ) AND ("
									+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
											+ " MAX(CLOSING_DATE || CLOSING_TIME)"
										+ " FROM TB_TXT_CLOSING"
										+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
										+ " AND (CLOSING_DATE || CLOSING_TIME) <= '" + eDate + "235959'"
								+ " )"
							+ " AND A.CLOSING_DATE <= '" + eDate + "'"
							+ " AND A.CANCEL_DATE <= '" + eDate + "'"
						) :
				// 환수일/환수예정일
					"13".equals(sType) || "15".equals(sType) ? (
							" A.CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -5) AND F_GET_WORKDAY@VANBT('" + eDate + "', -5)"
						) :
				// 매입취소일
					"14".equals(sType) ? (
							" A.CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -1) AND F_GET_WORKDAY@VANBT('" + eDate + "', -1)"
						) :
				// 기타
					(
							" A." + YYYYMMDD + " BETWEEN '" + sDate + "' AND '" + eDate + "'"
						)
				)
							+ WHERE
						+ " ORDER BY A.TRANSACTION_DATE DESC, A.TRANSACTION_TIME DESC"
					);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				String date = rs.getString("TRANSACTION_DATE");
				c.put("TRANSACTION_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				date = rs.getString("PURCHASE_DATE");
				c.put("PURCHASE_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8)
							: ""
					);
				date = rs.getString("PAY_DATE");
				c.put("PAY_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8)
							: ""
					);
				date = rs.getString("CLOSING_DATE");
				c.put("CLOSING_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				date = rs.getString("CANCEL_DATE");
				c.put("CANCEL_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
						: ""
					);
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("TRANSACTION_NO", rs.getString("TRANSACTION_NO"));
				c.put("COL_NO", rs.getString("COL_NO"));		//자판기 매출정보 > 거래내역  칼럼 내역 엑셀 추가 수정 Start. Chae jong wook 2020-04-27
				c.put("AMOUNT", rs.getLong("AMOUNT"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("ORGAN_CODE", rs.getString("ORGAN_CODE")); //2017.12.21 jwhwang 추가
				c.put("PLACE", rs.getString("PLACE"));
				c.put("VMCODE", rs.getString("VM_CODE"));
				c.put("PRODUCT", rs.getString("PRODUCT"));
				c.put("PRODUCT_CODE", rs.getString("PRODUCT_CODE"));
				c.put("BAR_CODE", rs.getString("BAR_CODE"));	//scheo 추가
				c.put("PAY_TYPE", rs.getString("PAY_TYPE"));
				c.put("PAY_STEP", rs.getString("PAY_STEP"));
				c.put("CARD_NO", rs.getString("CARD_NO"));
				c.put("PURCHASE_ORGAN_NAME", rs.getString("PURCHASE_ORGAN_NAME"));
				c.put("APPROVAL_NO", rs.getString("APPROVAL_NO"));
				c.put("INPUT_TYPE", rs.getString("INPUT_TYPE"));
				c.put("PAY_CARD", rs.getString("PAY_CARD"));

				this.list.add(c);
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 마감 출력 여부, 2011.06.11, 정원광
		this.data.put("IS_VIEW_CLOSING",
				dbLib.getResult(conn,
						"SELECT /*+ INDEX(TB_COMPANY) */"
								+ " IS_VIEW_CLOSING"
							+ " FROM TB_COMPANY"
							+ " WHERE SEQ = " + company
					)
			);

	// 검색 설정
		String sDesc = "조회기준="
				+ dbLib.getResult(conn,
						"SELECT /*+ INDEX(TB_CODE) */"
								+ " NAME"
							+ " FROM TB_CODE"
							+ " WHERE TYPE = 'SUM_TYPE'"
								+ " AND CODE = '" + sType + "'"
					)
				+ "&조회기간=" + sDate + "-" + eDate
			;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}
		if (!StringEx.isEmpty(product)) { // 상품
		//if (goods > 0) {
			sDesc += "&상품명="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_PRODUCT) */"
									+ " NAME"
								+ " FROM TB_PRODUCT"
								+ " WHERE  COMPANY_SEQ = " + company
									+ " AND CODE = " + product
						)
				;
		}

	//20160613 결제유형을 다중선택으로 변경
	//	if (!StringEx.isEmpty(payment)) {
		if ((payTypes != null) && (payTypes.length > 0)) {
			sDesc += "&결제유형="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_CODE) */"
									+ " NAME"
								+ " FROM TB_CODE"
								+ " WHERE TYPE = 'PAY_TYPE'"
									+ " AND CODE = '" + payTypes[0] + "'"
						)
				;
			for (int i = 1; i < payTypes.length; i++) {
				sDesc += ","
						+ dbLib.getResult(conn,
								"SELECT /*+ INDEX(TB_CODE) */"
										+ " NAME"
									+ " FROM TB_CODE"
									+ " WHERE TYPE = 'PAY_TYPE'"
										+ " AND CODE = '" + payTypes[i] + "'"
							)
					;
			}
		}

	//20160613 진행상태를 다중선택으로 변경
	//	if (!StringEx.isEmpty(step)) {
		if ((paySteps != null) && (paySteps.length > 0)) {
			sDesc += "&진행상태="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_CODE) */"
									+ " NAME"
								+ " FROM TB_CODE"
								+ " WHERE TYPE = 'PAY_STEP'"
								+ " AND CODE = '" + paySteps[0] + "'"
						)
				;
			for (int i = 1; i < paySteps.length; i++) {
				sDesc += ","
						+ dbLib.getResult(conn,
								"SELECT /*+ INDEX(TB_CODE) */"
										+ " NAME"
									+ " FROM TB_CODE"
									+ " WHERE TYPE = 'PAY_STEP'"
									+ " AND CODE = '" + paySteps[i] + "'"
							)
					;
			}
		}

		this.data.put("sDesc", sDesc);

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}

	/**
	 * 거래내역
	 *
	 * @param sType 집계 유형
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 시작일
	 * @param eDate 종료일
	 * @param product 상품
	 * @param payment 결제 방식
	 * @param step 진행 상태
	 * @param pageNo 페이지
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	//public String sales(String sType, long company, long organ, long place, String sDate, String eDate, long goods, String[] payTypes, String[] paySteps, int pageNo) throws Exception {
	public String sales(String sType, long company, long organ, long depth, long place, String sDate, String eDate, String product, String[] payTypes, String[] paySteps, int pageNo) throws Exception {
	// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sType) || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
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
			return "DB 연결에 실패하였습니다.";
		}

	// 검색절 생성
		String WHERE = "";
		String YYYYMMDD
				= "01".equals(sType) ? "TRANSACTION_DATE"	// 거래일
				: "02".equals(sType) ? "CLOSING_DATE"		// 마감일
				: "03".equals(sType) ? "PAY_DATE"			// 입금일
				: "04".equals(sType) ? "PURCHASE_DATE"		// 매입청구일
				: "05".equals(sType) ? "PAY_DATE_EXP"		// 입금예정일
				: "CANCEL_DATE"								// 취소일/마감일(취소)/환수일/매입취소일/환수예정일
			;

		//System.out.println("user_operator 이다 : "+cfg.get("user.operator"));
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 
			company = (company > 0 ? company : this.cfg.getLong("user.company"));
			if (company > 0) { // 소속
				WHERE += " AND A.COMPANY_SEQ = " + company;
			} 
			
			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			if (organ > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
							//20160121 타소속의 조직이 검색되지 않도록 COMPANY_SEQ 조건 추가
								+ " START WITH COMPANY_SEQ = " + company
									+ " AND SEQ IN ("
											+ " SELECT " + organ + " FROM DUAL"
											+ " UNION"
											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
													+ " ORGANIZATION_SEQ"
												+ " FROM TB_USER_APP_ORGAN"
												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
										+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
					;
			}
			
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
				WHERE += " AND USER_SEQ = " + this.cfg.getLong("user.seq");
			} 
			WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			
			//organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ") : organ);
			organ = (organ <= 0 ? this.cfg.getLong("user.organ") : (this.cfg.getInt("user.organ.depth") > depth ? this.cfg.getLong("user.organ") : organ));
			//System.out.println("조직번호 : "+organ);
			if (organ > 0) { // 조직
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ ")"
					;
			} 
		}
		
// 2016.06.21 jwhwang 수정		
//		
//		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
//			//WHERE += " AND USER_SEQ = " + this.cfg.getLong("user.seq");
//		} else {
//			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
//				WHERE += " AND COMPANY_SEQ = " + this.cfg.getLong("user.company");
//			}
//
//			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
//				WHERE += " AND ORGANIZATION_SEQ IN ("
//							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//									+ " SEQ"
//								+ " FROM TB_ORGANIZATION"
//								+ " WHERE SORT = 1"
//							//20160121 타소속의 조직이 검색되지 않도록 COMPANY_SEQ 조건 추가
//								+ " START WITH COMPANY_SEQ = " + this.cfg.getLong("user.company")
//									+ " AND SEQ IN ("
//											+ " SELECT " + this.cfg.getLong("user.organ") + " FROM DUAL"
//											+ " UNION"
//											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
//													+ " ORGANIZATION_SEQ"
//												+ " FROM TB_USER_APP_ORGAN"
//												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
//										+ " )"
//								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//						+ " )"
//					;
//			}
//		}
//
//		if (company > 0) { // 소속
//			WHERE += " AND COMPANY_SEQ = " + company;
//		}
//
//		if (organ > 0) { // 조직
//			WHERE += " AND ORGANIZATION_SEQ IN ("
//						+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//								+ " SEQ"
//							+ " FROM TB_ORGANIZATION"
//							+ " WHERE SORT = 1"
//							+ " START WITH SEQ = " + organ
//							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//					+ ")"
//				;
//		}

		if (place > 0) { // 위치
			WHERE += " AND VM_PLACE_SEQ = " + place;
		}

		//if (goods > 0) { // 상품
		//	WHERE += " AND GOODS_SEQ = " + goods;
		//}
		if (!StringEx.isEmpty(product)) { // 상품
			WHERE += " AND PRODUCT_CODE = '" + product + "'";
		}

	//20160613 결제유형을 다중선택으로 변경
	//	if (!StringEx.isEmpty(payment)) { // 결제유형
	//		WHERE += " AND PAY_TYPE = '" + payment + "'";
	//	}
		if ((payTypes != null) && (payTypes.length > 0)) {
			WHERE += " AND PAY_TYPE IN ('" + payTypes[0] + "'";
			for (int i = 1; i < payTypes.length; i++) WHERE += ", '" + payTypes[i] + "'";
			WHERE += ")";
	//20170329 키갱신 제외
		} else {
			WHERE += " AND A.PAY_TYPE <> '09'";
		}

	//20160613 진행상태를 다중선택으로 변경
	//	if (!StringEx.isEmpty(step)) { // 진행상태
	//		WHERE += " AND PAY_STEP = '" + step + "'";
	//	}
		if ((paySteps != null) && (paySteps.length > 0)) {
			WHERE += " AND PAY_STEP IN ('" + paySteps[0] + "'";
			for (int i = 1; i < paySteps.length; i++) WHERE += ", '" + paySteps[i] + "'";
			WHERE += ")";
	//20170404 집계상세내역 취소/망상취소 제외
		} else if (paySteps != null) {
			WHERE += " AND PAY_STEP NOT IN ('00', '99')";
	//20170329 망상취소 제외
		} else {
			//WHERE += " AND PAY_STEP <> '00'";
			WHERE += "AND ( ((AMOUNT = '100' AND PAY_TYPE = '07')) OR ((AMOUNT <> '100' OR PAY_TYPE <> '07') AND PAY_STEP <> '00') )"; //20201221 scheo 간편결제 100원 망상취소 출력 (박경환 주임 요청)
		}

	// 총 레코드수
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT /*+ INDEX(A IX_SALES_" + YYYYMMDD + ") */"
								+ " COUNT(*)"
							+ " FROM TB_SALES A"
							+ " WHERE " + YYYYMMDD + " BETWEEN '" + sDate + "' AND '" + eDate + "'"
								+ WHERE
					)
			);

	// 총 페이지수
		this.pages = Pager.getSize(this.records, cfg.getInt("limit.list"));

	// 리스트 가상번호
		long no = this.records - (pageNo - 1) * cfg.getInt("limit.list");

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();

		try {
			int s = ((pageNo - 1) * cfg.getInt("limit.list")) + 1;
			int e = (s - 1) + cfg.getInt("limit.list");

			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED USE_NL(B C D E F G H I) */"
							+ " A.TRANSACTION_DATE || A.TRANSACTION_TIME AS TRANSACTION_DATE,"
							+ " CASE WHEN A.CLOSING_DATE IS NOT NULL THEN A.CLOSING_DATE || NVL(A.CLOSING_TIME, '000000') END AS CLOSING_DATE,"
							+ " A.PURCHASE_DATE,"
							+ " CASE WHEN A.PAY_STEP = '02' THEN A.PAY_DATE_EXP ELSE A.PAY_DATE END AS PAY_DATE,"
							+ " CASE WHEN A.CANCEL_DATE IS NOT NULL THEN A.CANCEL_DATE || NVL(A.CANCEL_TIME, '000000') END AS CANCEL_DATE,"
							+ " A.TERMINAL_ID,"
							+ " A.TRANSACTION_NO,"
							+ " A.ITEM_COUNT," //2019-06-19 김태우 추가  exp) 구매 품목 갯수	
							+ " A.AMOUNT,"
							+ " A.COL_NO," //<!-- 20191028 컬럼추가 twkim326 -->							
							+ " B.NAME AS ORGAN,"
							+ " C.PLACE,"
							+ " NVL(D.NAME, '미등록 상품[' || A.COL_NO || ']') AS GOODS," // scheo 20181214 유광권부장 요청사항 - 20190619 scheo 원복
							//+ " case when D.name is null then case when length(A.col_no) = '2' then '미등록 상품[' || '00' || A.col_no || ']' when length(A.col_no) = '1' then '미등록 상품[' || '000' || A.col_no || ']' when MOD(A.col_no,100) || TRUNC(A.col_no/100) <= '999' then '미등록 상품[' || '0' || MOD(A.col_no,100) || TRUNC(A.col_no/100) || ']' else '미등록 상품[' || MOD(A.col_no,100) || TRUNC(A.col_no/100) || ']' end else D.name end as GOODS ,"
							+ " E.CODE AS VM_CODE,"
							+ " F.NAME AS PAY_TYPE,"
							+ " G.NAME AS PAY_STEP,"
							+ " H.NAME AS INPUT_TYPE,"
							+ " I.NAME AS PAY_CARD"
						+ " FROM ("
									+ " SELECT"
											+ " ROWNUM AS ROW_NUM,"
											+ " A.*"
										+ " FROM ("
												+ " SELECT /*+ INDEX(A IX_SALES_" + YYYYMMDD + ") */"
														+ " *"
													+ " FROM TB_SALES A"
													+ " WHERE"
										+ (
											// 마감일(취소)
												"12".equals(sType) ? (
														" (A.CANCEL_DATE || A.CANCEL_TIME) BETWEEN ("
																+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																		+ " MAX(CLOSING_DATE || CLOSING_TIME) || '0'"
																	+ " FROM TB_TXT_CLOSING"
																	+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																		+ " AND (CLOSING_DATE || CLOSING_TIME) < '" + sDate + "'"
															+ " ) AND ("
																+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																		+ " MAX(CLOSING_DATE || CLOSING_TIME)"
																	+ " FROM TB_TXT_CLOSING"
																	+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																	+ " AND (CLOSING_DATE || CLOSING_TIME) <= '" + eDate + "235959'"
															+ " )"
														+ " AND A.CLOSING_DATE <= '" + eDate + "'"
														+ " AND A.CANCEL_DATE <= '" + eDate + "'"
													) :
											// 환수일/환수예정일
												"13".equals(sType) || "15".equals(sType) ? (
														" A.CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -5) AND F_GET_WORKDAY@VANBT('" + eDate + "', -5)"
													) :
											// 매입취소일
												"14".equals(sType) ? (
														" A.CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -1) AND F_GET_WORKDAY@VANBT('" + eDate + "', -1)"
													) :
											// 기타
												(
														" A." + YYYYMMDD + " BETWEEN '" + sDate + "' AND '" + eDate + "'"
													)
											)
													+ WHERE
													+ " ORDER BY A.TRANSACTION_DATE DESC, A.TRANSACTION_TIME DESC"
											+ " ) A"
										+ " WHERE ROWNUM <= " + e
								+ ") A"
							+ " INNER JOIN TB_ORGANIZATION B"
								+ " ON A.ORGANIZATION_SEQ = B.SEQ"
							+ " INNER JOIN TB_VENDING_MACHINE_PLACE C"
								+ " ON A.VM_PLACE_SEQ = C.SEQ"
							+ " LEFT JOIN TB_PRODUCT D"
								+ " ON A.COMPANY_SEQ = D.COMPANY_SEQ"
									+ " AND A.PRODUCT_CODE = D.CODE"
							//+ " LEFT JOIN TB_GOODS D"
							//	+ " ON A.GOODS_SEQ = D.SEQ"
						//TODO: FK_SALES_TERMINAL_ID 생성 후 INNER JOIN으로 변경
							+ " LEFT JOIN TB_VENDING_MACHINE E"
								+ " ON A.TERMINAL_ID = E.TERMINAL_ID"
								//+ " ON C.VM_SEQ = E.SEQ
							+ " LEFT JOIN TB_CODE F"
								+ " ON A.PAY_TYPE = F.CODE"
									+ " AND F.TYPE = 'PAY_TYPE'"
							+ " LEFT JOIN TB_CODE G"
								+ " ON A.PAY_STEP = G.CODE"
									+ " AND G.TYPE = 'PAY_STEP'"
							+ " LEFT JOIN TB_CODE H"
								+ " ON A.INPUT_TYPE = H.CODE"
									+ " AND H.TYPE = 'INPUT_TYPE'"
							+ " LEFT JOIN TB_CODE I"
								+ " ON A.PAY_CARD = I.CODE"
									+ " AND I.TYPE = 'PAY_CARD'"
						+ " WHERE A.ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				String date = rs.getString("TRANSACTION_DATE");
				c.put("TRANSACTION_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				date = rs.getString("PURCHASE_DATE");
				c.put("PURCHASE_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8)
							: ""
					);
				date = rs.getString("PAY_DATE");
				c.put("PAY_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8)
							: ""
					);
				date = rs.getString("CLOSING_DATE");
				if ((date != null) && (date.length() < 14)) System.out.println(date);
				c.put("CLOSING_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				date = rs.getString("CANCEL_DATE");
				c.put("CANCEL_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("TRANSACTION_NO", rs.getString("TRANSACTION_NO"));
				c.put("AMOUNT", rs.getLong("AMOUNT"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("PLACE", rs.getString("PLACE"));
				c.put("VMCODE", rs.getString("VM_CODE"));
				c.put("GOODS", rs.getString("GOODS"));
				c.put("PAY_TYPE", rs.getString("PAY_TYPE"));
				c.put("PAY_STEP", rs.getString("PAY_STEP"));
				c.put("INPUT_TYPE", rs.getString("INPUT_TYPE"));
				c.put("PAY_CARD", rs.getString("PAY_CARD"));
				c.put("ITEM_COUNT", rs.getLong("ITEM_COUNT"));//2019-06-19 김태우 추가  exp) 구매 품목 갯수	
				c.put("ITEM_COUNT_minus", rs.getLong("ITEM_COUNT")-1);//2019-06-19 김태우 추가  exp) 구매 품목 외 n 건을 위한 -1 
				c.put("COL_NO", rs.getString("COL_NO"));//<!-- 20191028 컬럼추가 twkim326 -->
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

	// 조직명
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
							+ " FROM ("
									+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
											+ " NAME"
										+ " FROM TB_ORGANIZATION"
										+ " WHERE COMPANY_SEQ = " + company
											+ " AND SORT = 0"
										+ " ORDER BY DEPTH DESC"
								+ ")"
							+ " WHERE ROWNUM = 1"
					)
			);

	// 마감 출력 여부, 2011.06.11, 정원광
		this.data.put("IS_VIEW_CLOSING",
				dbLib.getResult(conn,
						"SELECT /*+ INDEX(TB_COMPANY) */"
								+ " IS_VIEW_CLOSING"
							+ " FROM TB_COMPANY"
							+ " WHERE SEQ = " + company
					)
			);

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}

	/**
	 * 자판기별 거래/마감/입금/매입청구/입금예정현황 _ 정산현황
	 *
	 * @param sType 집계유형
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 시작일
	 * @param eDate 종료일
	 * @param payTypes 결제유형
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String summary(String sType, long company, long organ, long place, String sDate, String eDate, String[] payTypes, int oMode, int oType) throws Exception {
	// 필수 검색 조건이 없을 때
		if (StringEx.isEmpty(sType) || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
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
			return "DB 연결에 실패하였습니다.";
		}

	// 검색절 생성
		String WHERE = "";
		String YYYYMMDD
				= "01".equals(sType) ? "TRANSACTION_DATE"	// 거래일 - 미사용
				: "02".equals(sType) ? "CLOSING_DATE"		// 마감일
				: "03".equals(sType) ? "PAY_DATE"			// 입금일(신용정산)
				: "04".equals(sType) ? "PURCHASE_DATE"		// 매입청구일(선불정산)
				: "05".equals(sType) ? "PURCHASE_DATE"		// 입금예정일 - 미사용 --> 매입청구일 기준으로 변경.
				: "CANCEL_DATE"								// 취소일/마감일(취소)/환수일/매입취소일/환수예정일 - 미사용
			;

		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 
			company = (company > 0 ? company : this.cfg.getLong("user.company"));
			if (company > 0) { // 소속
				WHERE += " AND A.COMPANY_SEQ = " + company;
			} 
			
			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			if (organ > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
							//20160121 타소속의 조직이 검색되지 않도록 COMPANY_SEQ 조건 추가
								+ " START WITH COMPANY_SEQ = " + company
									+ " AND SEQ IN ("
											+ " SELECT " + organ + " FROM DUAL"
											+ " UNION"
											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
													+ " ORGANIZATION_SEQ"
												+ " FROM TB_USER_APP_ORGAN"
												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
										+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
					;
			}
			
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			
			organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ") : organ);
			if (organ > 0) { // 조직
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ ")"
					;
			} 
		}
		
// 2016.06.26 jwhwang 수정	
//		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
//			WHERE += " AND USER_SEQ = " + this.cfg.getLong("user.seq");
//		} else {
//			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
//				WHERE += " AND COMPANY_SEQ = " + this.cfg.getLong("user.company");
//			}
//
//			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
//				WHERE += " AND ORGANIZATION_SEQ IN ("
//							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//									+ " SEQ"
//								+ " FROM TB_ORGANIZATION"
//								+ " WHERE SORT = 1"
//								+ " START WITH COMPANY_SEQ = " + this.cfg.getLong("user.company")
//									+ " AND SEQ IN ("
//											+ " SELECT " + this.cfg.getLong("user.organ") + " FROM DUAL"
//											+ " UNION"
//											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
//													+ " ORGANIZATION_SEQ"
//												+ " FROM TB_USER_APP_ORGAN"
//												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
//										+ " )"
//								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//						+ " )"
//					;
//			}
//		}
//
//		if (company > 0) { // 소속
//			WHERE += " AND COMPANY_SEQ = " + company;
//		}
//
//		if (organ > 0) { // 조직
//			WHERE += " AND ORGANIZATION_SEQ IN ("
//						+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//								+ " SEQ"
//							+ " FROM TB_ORGANIZATION"
//							+ " WHERE SORT = 1"
//							+ " START WITH SEQ = " + organ
//							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//					+ ")"
//				;
//		}

		if (place > 0) { // 위치
			WHERE += " AND VM_PLACE_SEQ = " + place;
		}

	//20160701 결제유형 다중선택 추가
		if ((payTypes != null) && (payTypes.length > 0)) {
			WHERE += " AND PAY_TYPE IN ('" + payTypes[0] + "'";
			for (int i = 1; i < payTypes.length; i++) WHERE += ", '" + payTypes[i] + "'";
			WHERE += ")";
	//20170329 키갱신 제외
		} else {
			WHERE += " AND A.PAY_TYPE <> '09'";
		}

		String SUM_OF_EACH_PREPAY_COMPANY_1 = "";
		String SUM_OF_EACH_PREPAY_COMPANY_2 = "";
		String[] prepaySummaryColumns = new String[] {};
		ArrayList<String> prepaySummaryColumnList = new ArrayList<String>();

	// 선불 카드사
		this.company = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(TB_CODE) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE"
						+ " WHERE TYPE = 'PAY_CARD'"
							+ " AND CODE <> '000'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				String code = rs.getString("CODE");
				c.put("CODE", code);
				c.put("NAME", rs.getString("NAME"));

				this.company.add(c);

				SUM_OF_EACH_PREPAY_COMPANY_1 += " COUNT(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN 1 END) AS CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN AMOUNT END), 0) AS AMOUNT_PREPAY_" + code + ","
						+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN 1 END) AS PAY_CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN COMMISSION END), 0) AS COMMISSION_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_PREPAY_" + code + ","
					;
				SUM_OF_EACH_PREPAY_COMPANY_2 += " COUNT(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN 1 END) AS CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN -AMOUNT END), 0) AS AMOUNT_PREPAY_" + code + ","
						+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN 1 END) AS PAY_CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN COMMISSION END), 0) AS COMMISSION_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN -AMOUNT + COMMISSION - OUTAMOUNT END), 0) AS PAY_AMOUNT_PREPAY_" + code + ","
					;
				prepaySummaryColumnList.add("CNT_PREPAY_" + code);
				prepaySummaryColumnList.add("AMOUNT_PREPAY_" + code);
				prepaySummaryColumnList.add("PAY_CNT_PREPAY_" + code);
				prepaySummaryColumnList.add("COMMISSION_PREPAY_" + code);
				prepaySummaryColumnList.add("PAY_AMOUNT_PREPAY_" + code);
			}

			prepaySummaryColumns = prepaySummaryColumnList.toArray(new String[prepaySummaryColumnList.size()]);
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			prepaySummaryColumnList = null;

			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}

	// 자판기별 집계현황
		this.data = new GeneralConfig();
		this.list = new ArrayList<GeneralConfig>();

		try {
			long[] cnt = { 0, 0, 0, 0, 0 };
			long[] amt = { 0, 0, 0, 0, 0 };
			long[] cms = { 0, 0, 0, 0, 0 };
			long[] pcnt = { 0, 0, 0, 0, 0 };
			long[] pamt = { 0, 0, 0, 0, 0 };
			long[] acnt = { 0, 0 };
			long[] aamt = { 0, 0 };

			ps = dbLib.prepareStatement(conn,
					" SELECT /*+ ORDERED USE_HASH(B C D E) */"
							+ " A.*,"
							+ " NVL(TO_CHAR(TO_DATE(("
									+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
											+ " MAX(CLOSING_DATE || CLOSING_TIME)"
										+ " FROM TB_TXT_CLOSING"
										+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
											+ " AND (CLOSING_DATE || CLOSING_TIME) < A.MIN_DATE"
								+ " ), 'YYYYMMDDHH24MISS') + INTERVAL '1' SECOND, 'YYYYMMDDHH24MISS'), MIN_DATE) AS START_DATE,"
			+ ( company <= 0 ? " E.NAME AS COMPANY," : "")
							+ " ("
									+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
											+ " NAME"
										+ " FROM TB_ORGANIZATION"
										+ " WHERE PARENT_SEQ = " + (organ > 0 ? organ : 0)
											+ " AND SORT = 1"
											+ " AND SEQ <> A.ORGANIZATION_SEQ"
										+ " START WITH SEQ = A.ORGANIZATION_SEQ"
										+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
								+ ") AS PARENT_ORGAN,"
								+ " B.NAME AS ORGAN,"
								+ " B.CODE AS ORGAN_CODE,"
			+ (
					company <= 0 ? (
							" NULL AS PLACE,"
							+ " NULL AS VM_CODE"
						) : (
							" C.PLACE AS PLACE,"
							+ " D.CODE AS VM_CODE"
						)
				)
						+ " FROM ("
									+ " SELECT /*+ INDEX(A IX_SALES_" + YYYYMMDD + ") */"
											+ " COMPANY_SEQ,"
											+ " ORGANIZATION_SEQ,"
							+ (
									company <= 0 ? (
											" NULL AS VM_PLACE_SEQ,"
											+ " NULL AS TERMINAL_ID,"
										) : (
											" VM_PLACE_SEQ,"
											+ " TERMINAL_ID,"
										)
								)
											+ " MIN(TRANSACTION_DATE || TRANSACTION_TIME) AS MIN_DATE,"
											+ " MAX(CASE WHEN CLOSING_DATE IS NOT NULL THEN CLOSING_DATE || NVL(CLOSING_TIME, '000000') END) AS END_DATE,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN AMOUNT END), 0) AS AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE IN ('02', '10') THEN 1 END) AS PAY_CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN COMMISSION END), 0) AS COMMISSION_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN AMOUNT END), 0) AS AMOUNT_CARD,"
											+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS PAY_CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '01' and pay_step <> '06' THEN COMMISSION END), 0) AS COMMISSION_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '01' and pay_step <> '06' THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_CARD,"
											//scheo 2018.10.11 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN AMOUNT END), 0) AS AMOUNT_PAYCO,"
											+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '07' THEN 1 END) AS PAY_CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '07' THEN COMMISSION END), 0) AS COMMISSION_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '07' THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_PAYCO,"
											//scheo 2018.10.11 추가
											+ SUM_OF_EACH_PREPAY_COMPANY_1
											+ " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN AMOUNT END), 0) AS AMOUNT_PREPAY,"
											+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' THEN 1 END) AS PAY_CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' THEN COMMISSION END), 0) AS COMMISSION_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_PREPAY,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('01', '11') AND PAY_STEP IN ('01', '21') THEN 1 END) AS CNT_HELD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('01', '11') AND PAY_STEP IN ('01', '21') THEN AMOUNT END), 0) AS AMOUNT_HELD,"
											+ " COUNT(CASE WHEN PAY_STEP = '22' THEN 1 END) AS CNT_DECLINED,"
											+ " NVL(SUM(CASE WHEN PAY_STEP = '22' THEN AMOUNT END), 0) AS AMOUNT_DECLINED,"
											+ " MIN(PAY_DATE) AS PAY_START_DATE,"
											+ " MAX(PAY_DATE) AS PAY_END_DATE"
										+ " FROM TB_SALES A"
										+ " WHERE " + YYYYMMDD + " BETWEEN '" + sDate + "' AND '" + eDate + "'"
											+ " AND PAY_STEP NOT IN ('00', '99')"
											+ WHERE
										+ " GROUP BY COMPANY_SEQ, ORGANIZATION_SEQ" + (company <= 0 ? "" : ", VM_PLACE_SEQ, TERMINAL_ID")
									+ " UNION ALL"
									+ " SELECT"
											+ " COMPANY_SEQ,"
											+ " ORGANIZATION_SEQ,"
							+ (
									company <= 0 ? (
											" NULL AS VM_PLACE_SEQ,"
											+ " NULL AS TERMINAL_ID,"
										) : (
											" VM_PLACE_SEQ,"
											+ " TERMINAL_ID,"
										)
								)
											+ " MIN(CANCEL_DATE || CANCEL_TIME) AS MIN_DATE,"
											+ " MAX(("
													+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
															+ " MIN(CLOSING_DATE || NVL(CLOSING_TIME, '000000'))"
														+ " FROM TB_TXT_CLOSING"
														+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
													+ " AND (CLOSING_DATE || CLOSING_TIME) >= (A.CANCEL_DATE || A.CANCEL_TIME)"
												+ " )) AS END_DATE,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN -AMOUNT END), 0) AS AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE IN ('02', '10') THEN 1 END) AS PAY_CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE IN ('02', '10') THEN -COMMISSION END), 0) AS COMMISSION_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE IN ('02', '10') THEN -AMOUNT + COMMISSION - OUTAMOUNT END), 0) AS PAY_AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' THEN -AMOUNT END), 0) AS AMOUNT_CARD,"
											+ " COUNT(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS PAY_CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '01' THEN -COMMISSION END), 0) AS COMMISSION_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '01' THEN -AMOUNT + COMMISSION - OUTAMOUNT END), 0) AS PAY_AMOUNT_CARD,"
											+ " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN -AMOUNT END), 0) AS AMOUNT_PAYCO,"
											+ " COUNT(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '07' THEN 1 END) AS PAY_CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '07' THEN -COMMISSION END), 0) AS COMMISSION_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '07' THEN -AMOUNT + COMMISSION - OUTAMOUNT END), 0) AS PAY_AMOUNT_PAYCO,"
											+ SUM_OF_EACH_PREPAY_COMPANY_2
											+ " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN -AMOUNT END), 0) AS AMOUNT_PREPAY,"
											+ " COUNT(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '11' THEN 1 END) AS PAY_CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '11' THEN -COMMISSION END), 0) AS COMMISSION_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '11' THEN -AMOUNT + COMMISSION - OUTAMOUNT END), 0) AS PAY_AMOUNT_PREPAY,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('01', '11') AND PAY_STEP IN ('01', '21') THEN 1 END) AS CNT_HELD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('01', '11') AND PAY_STEP IN ('01', '21') THEN AMOUNT END), 0) AS AMOUNT_HELD,"
											+ " COUNT(CASE WHEN PAY_STEP = '22' THEN 1 END) AS CNT_DECLINED,"
											+ " NVL(SUM(CASE WHEN PAY_STEP = '22' THEN AMOUNT END), 0) AS AMOUNT_DECLINED,"
											+ " MIN(PAY_DATE2) AS PAY_START_DATE,"
											+ " MAX(PAY_DATE2) AS PAY_END_DATE"
										+ " FROM ("
												+ " SELECT /*+ INDEX(A IX_SALES_PAY_STEP) */"
										+ (
											// 마감일(취소)
												"02".equals(sType) ? (
														" CASE WHEN (CANCEL_DATE || CANCEL_TIME) < (CLOSING_DATE || CLOSING_TIME)"
															+ " THEN CLOSING_DATE"
															+ " ELSE ("
																	+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																			+ " SUBSTR(MIN(CLOSING_DATE || CLOSING_TIME), 1, 8)"
																		+ " FROM TB_TXT_CLOSING"
																		+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																			+ " AND (CLOSING_DATE || CLOSING_TIME) >= (A.CANCEL_DATE || A.CANCEL_TIME)"
																+ " )"
															+ " END AS YYYYMMDD,"
													) :
											// 환수일/환수예정일
												"03".equals(sType) || "05".equals(sType) ? (
														" F_GET_WORKDAY@VANBT(CANCEL_DATE, 5) AS YYYYMMDD,"
													) :
											// 매입취소일
												"04".equals(sType) ? (
														" F_GET_WORKDAY@VANBT(CANCEL_DATE, 1) AS YYYYMMDD,"
													) :
											// 취소일
												(
														" CANCEL_DATE AS YYYYMMDD,"
													)
											)
														+ " A.*,"
														+ " CASE WHEN F_GET_WORKDAY@VANBT(CANCEL_DATE, 5) <= TO_CHAR(SYSDATE, 'YYYYMMDD') THEN F_GET_WORKDAY@VANBT(CANCEL_DATE, 5) END AS PAY_DATE2"
													+ " FROM TB_SALES A"
													+ " WHERE PAY_STEP = '29'"
														+ " AND " + YYYYMMDD + " IS NOT NULL"
														+ WHERE
										+ (
											// 마감일(취소)
												"02".equals(sType) ? (
														" AND (CANCEL_DATE || CANCEL_TIME) BETWEEN ("
																+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																		+ " MAX(CLOSING_DATE || CLOSING_TIME) || '0'"
																	+ " FROM TB_TXT_CLOSING"
																	+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																		+ " AND (CLOSING_DATE || CLOSING_TIME) < '" + sDate + "'"
															+ " ) AND ("
																	+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																			+ " MAX(CLOSING_DATE || CLOSING_TIME)"
																		+ " FROM TB_TXT_CLOSING"
																		+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																		+ " AND (CLOSING_DATE || CLOSING_TIME) <= '" + eDate + "235959'"
															+ " )"
														+ " AND CLOSING_DATE <= '" + eDate + "'"
														+ " AND CANCEL_DATE <= '" + eDate + "'"
													) :
											// 환수일/환수예정일
												"03".equals(sType) || "05".equals(sType) ? (
														" AND CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -5) AND F_GET_WORKDAY@VANBT('" + eDate + "', -5)"
													) :
											// 매입취소일
												"04".equals(sType) ? (
														" AND CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -1) AND F_GET_WORKDAY@VANBT('" + eDate + "', -1)"
													) :
											// 취소일
												(
														" AND CANCEL_DATE BETWEEN '" + sDate + "' AND '" + eDate + "'"
													)
											)
											+ " ) A"
										+ " WHERE YYYYMMDD BETWEEN '" + sDate + "' AND '" + eDate + "'"
										+ " GROUP BY COMPANY_SEQ, ORGANIZATION_SEQ" + (company <= 0 ? "" : ", VM_PLACE_SEQ, TERMINAL_ID")
								+ " ) A"
							+ " INNER JOIN TB_ORGANIZATION B"
									+ " ON A.ORGANIZATION_SEQ = B.SEQ"
			+ (
					company <= 0 ? (
							" INNER JOIN TB_COMPANY E"
									+ " ON A.COMPANY_SEQ = E.SEQ"
						) : (
							" INNER JOIN TB_VENDING_MACHINE_PLACE C"
									+ " ON A.VM_PLACE_SEQ = C.SEQ"
							+ " INNER JOIN TB_VENDING_MACHINE D"
									+ " ON C.VM_SEQ = D.SEQ"
						)
				)
						// 정렬 값(oMode, oType)에 따라 ORDER BY 변경
						+ " ORDER BY" + (company <= 0 ? " COMPANY," : "")
		+ ((oType == 1) ? (	// DESC
				// 설치장소
					(oMode == 2) ? (
							" PLACE DESC, PARENT_ORGAN DESC, ORGAN DESC"
						) : (
				// default: 거래처
							" PARENT_ORGAN DESC, ORGAN DESC, PLACE DESC"
						)
			) : (			// ASC
				// 설치장소
					(oMode == 2) ? (
							" PLACE, PARENT_ORGAN, ORGAN"
						) : (
				// default: 거래처
							" PARENT_ORGAN, ORGAN, PLACE"
						)
				)
			)
							+ ", VM_CODE, A.AMOUNT_CARD DESC"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("PARENT_ORGAN", rs.getString("PARENT_ORGAN"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("ORGAN_CODE", rs.getString("ORGAN_CODE"));
				if (company <= 0) {
					c.put("COMPANY", rs.getString("COMPANY"));
				} else {
					c.put("VM_PLACE_SEQ", rs.getLong("VM_PLACE_SEQ"));
					c.put("PLACE", rs.getString("PLACE"));
					c.put("VM_CODE", rs.getString("VM_CODE"));
				}

				String date = rs.getString("START_DATE");
				c.put("START_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				date = rs.getString("END_DATE");
				c.put("END_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);

				date = rs.getString("PAY_START_DATE");
				c.put("PAY_START_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8)
							: ""
					);
				date = rs.getString("PAY_END_DATE");
				c.put("PAY_END_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8)
							: ""
					);

				c.put("CNT_CASH", rs.getLong("CNT_CASH"));
				c.put("AMOUNT_CASH", rs.getLong("AMOUNT_CASH"));
				c.put("PAY_CNT_CASH", rs.getLong("PAY_CNT_CASH"));
				c.put("COMMISSION_CASH", rs.getLong("COMMISSION_CASH"));
				c.put("PAY_AMOUNT_CASH", rs.getLong("PAY_AMOUNT_CASH"));
				c.put("CNT_CARD", rs.getLong("CNT_CARD"));
				c.put("AMOUNT_CARD", rs.getLong("AMOUNT_CARD"));
				c.put("PAY_CNT_CARD", rs.getLong("PAY_CNT_CARD"));
				c.put("COMMISSION_CARD", rs.getLong("COMMISSION_CARD"));
				c.put("PAY_AMOUNT_CARD", rs.getLong("PAY_AMOUNT_CARD"));
				c.put("CNT_PAYCO", rs.getLong("CNT_PAYCO"));
				c.put("AMOUNT_PAYCO", rs.getLong("AMOUNT_PAYCO"));
				c.put("PAY_CNT_PAYCO", rs.getLong("PAY_CNT_PAYCO"));
				c.put("COMMISSION_PAYCO", rs.getLong("COMMISSION_PAYCO"));
				c.put("PAY_AMOUNT_PAYCO", rs.getLong("PAY_AMOUNT_PAYCO"));
				c.put("CNT_PREPAY", rs.getLong("CNT_PREPAY"));
				c.put("AMOUNT_PREPAY", rs.getLong("AMOUNT_PREPAY"));
				c.put("PAY_CNT_PREPAY", rs.getLong("PAY_CNT_PREPAY"));
				c.put("COMMISSION_PREPAY", rs.getLong("COMMISSION_PREPAY"));
				c.put("PAY_AMOUNT_PREPAY", rs.getLong("PAY_AMOUNT_PREPAY"));
				c.put("CNT_HELD", rs.getLong("CNT_HELD"));
				c.put("AMOUNT_HELD", rs.getLong("AMOUNT_HELD"));
				c.put("CNT_DECLINED", rs.getLong("CNT_DECLINED"));
				c.put("AMOUNT_DECLINED", rs.getLong("AMOUNT_DECLINED"));

				c.put("CNT_TOTAL", c.getLong("CNT_CASH") + c.getLong("CNT_CARD") + c.getLong("CNT_PAYCO") + c.getLong("CNT_PREPAY"));
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_CASH") + c.getLong("AMOUNT_CARD") + c.getLong("AMOUNT_PAYCO") + c.getLong("AMOUNT_PREPAY"));
				c.put("PAY_CNT_TOTAL", c.getLong("PAY_CNT_CASH") + c.getLong("PAY_CNT_CARD") + c.getLong("PAY_CNT_PAYCO") + c.getLong("PAY_CNT_PREPAY"));
				c.put("COMMISSION_TOTAL", c.getLong("COMMISSION_CASH") + c.getLong("COMMISSION_CARD") + c.getLong("COMMISSION_PAYCO") + c.getLong("COMMISSION_PREPAY"));
				c.put("PAY_AMOUNT_TOTAL", c.getLong("PAY_AMOUNT_CASH") + c.getLong("PAY_AMOUNT_CARD") + c.getLong("PAY_AMOUNT_PAYCO") + c.getLong("PAY_AMOUNT_PREPAY"));

				for (String columnName : prepaySummaryColumns) {
					c.put(columnName, rs.getLong(columnName));
					this.data.put(columnName, this.data.getLong(columnName) + rs.getLong(columnName));
				}

				this.list.add(c);

				cnt[0] += c.getLong("CNT_CASH");
				cnt[1] += c.getLong("CNT_CARD");
				cnt[2] += c.getLong("CNT_PAYCO");
				cnt[3] += c.getLong("CNT_PREPAY");
				cnt[4] += c.getLong("CNT_TOTAL");
				amt[0] += c.getLong("AMOUNT_CASH");
				amt[1] += c.getLong("AMOUNT_CARD");
				amt[2] += c.getLong("AMOUNT_PAYCO");
				amt[3] += c.getLong("AMOUNT_PREPAY");
				amt[4] += c.getLong("AMOUNT_TOTAL");
				pcnt[0] += c.getLong("PAY_CNT_CASH");
				pcnt[1] += c.getLong("PAY_CNT_CARD");
				pcnt[2] += c.getLong("PAY_CNT_PAYCO");
				pcnt[3] += c.getLong("PAY_CNT_PREPAY");
				pcnt[4] += c.getLong("PAY_CNT_TOTAL");
				cms[0] += c.getLong("COMMISSION_CASH");
				cms[1] += c.getLong("COMMISSION_CARD");
				cms[2] += c.getLong("COMMISSION_PAYCO");
				cms[3] += c.getLong("COMMISSION_PREPAY");
				cms[4] += c.getLong("COMMISSION_TOTAL");
				pamt[0] += c.getLong("PAY_AMOUNT_CASH");
				pamt[1] += c.getLong("PAY_AMOUNT_CARD");
				pamt[2] += c.getLong("PAY_AMOUNT_PAYCO");
				pamt[3] += c.getLong("PAY_AMOUNT_PREPAY");
				pamt[4] += c.getLong("PAY_AMOUNT_TOTAL");
				acnt[0] += c.getLong("CNT_HELD");
				acnt[1] += c.getLong("CNT_DECLINED");
				aamt[0] += c.getLong("AMOUNT_HELD");
				aamt[1] += c.getLong("AMOUNT_DECLINED");
			}

			this.data.put("CNT_CASH", cnt[0]);
			this.data.put("CNT_CARD", cnt[1]);
			this.data.put("CNT_PAYCO", cnt[2]);
			this.data.put("CNT_PREPAY", cnt[3]);
			this.data.put("CNT_TOTAL", cnt[4]);
			this.data.put("AMOUNT_CASH", amt[0]);
			this.data.put("AMOUNT_CARD", amt[1]);
			this.data.put("AMOUNT_PAYCO", amt[2]);
			this.data.put("AMOUNT_PREPAY", amt[3]);
			this.data.put("AMOUNT_TOTAL", amt[4]);
			this.data.put("PAY_CNT_CASH", pcnt[0]);
			this.data.put("PAY_CNT_CARD", pcnt[1]);
			this.data.put("PAY_CNT_PAYCO", pcnt[2]);
			this.data.put("PAY_CNT_PREPAY", pcnt[3]);
			this.data.put("PAY_CNT_TOTAL", pcnt[4]);
			this.data.put("COMMISSION_CASH", cms[0]);
			this.data.put("COMMISSION_CARD", cms[1]);
			this.data.put("COMMISSION_PAYCO", cms[2]);
			this.data.put("COMMISSION_PREPAY", cms[3]);
			this.data.put("COMMISSION_TOTAL", cms[4]);
			this.data.put("PAY_AMOUNT_CASH", pamt[0]);
			this.data.put("PAY_AMOUNT_CARD", pamt[1]);
			this.data.put("PAY_AMOUNT_PAYCO", pamt[2]);
			this.data.put("PAY_AMOUNT_PREPAY", pamt[3]);
			this.data.put("PAY_AMOUNT_TOTAL", pamt[4]);
			this.data.put("CNT_HELD", acnt[0]);
			this.data.put("CNT_DECLINED", acnt[1]);
			this.data.put("AMOUNT_HELD", aamt[0]);
			this.data.put("AMOUNT_DECLINED", aamt[1]);
		} catch (Exception e) {
			e.printStackTrace();
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

	// 최하위 조직 타이틀
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
							+ " FROM ("
									+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
											+ " NAME"
										+ " FROM TB_ORGANIZATION"
										+ " WHERE COMPANY_SEQ = " + company
											+ " AND SORT = 0"
										+ " ORDER BY DEPTH DESC"
								+ " )"
							+ " WHERE ROWNUM = 1"
					)
			);

	// 검색 설정
		String sDesc = "집계기준="
				+ dbLib.getResult(conn,
						"SELECT /*+ INDEX(TB_CODE) */"
								+ " NAME"
							+ " FROM TB_CODE"
							+ " WHERE TYPE = 'SUM_TYPE'"
								+ " AND CODE = '" + sType + "'"
					)
				+ "&집계기간=" + sDate + "-" + eDate
			;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}

		this.data.put("sDesc", sDesc);

	// 리소스 반환
		dbLib.close(conn);

		return null;
	}

	/**
	 * 입금일별 거래/마감/입금/매입청구/입금예정 현황
	 *
	 * @param sType 집계유형
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 시작일
	 * @param eDate 종료일
	 * @param payTypes 결제유형
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String summaryEx(String sType, long company, long organ, long depth, long place, String sDate, String eDate, String[] payTypes, int oMode, int oType) throws Exception {
	// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sType) || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
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
			return "DB 연결에 실패하였습니다.";
		}

	// 검색절 생성
		String WHERE = "";
		String YYYYMMDD
				= "01".equals(sType) ? "TRANSACTION_DATE"	// 거래일
				: "02".equals(sType) ? "CLOSING_DATE"		// 마감일
				: "03".equals(sType) ? "PAY_DATE"			// 입금일
				: "04".equals(sType) ? "PURCHASE_DATE"		// 매입청구일
				: "05".equals(sType) ? "PAY_DATE_EXP"		// 입금예정일
				: "CANCEL_DATE"								// 취소일/마감일(취소)/환수일/매입취소일/환수예정일 - 미사용
			;

		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 
			company = (company > 0 ? company : this.cfg.getLong("user.company"));
			if (company > 0) { // 소속
				WHERE += " AND A.COMPANY_SEQ = " + company;
			} 
			
			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			if (organ > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
							//20160121 타소속의 조직이 검색되지 않도록 COMPANY_SEQ 조건 추가
								+ " START WITH COMPANY_SEQ = " + company
									+ " AND SEQ IN ("
											+ " SELECT " + organ + " FROM DUAL"
											+ " UNION"
											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
													+ " ORGANIZATION_SEQ"
												+ " FROM TB_USER_APP_ORGAN"
												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
										+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
					;
			}
			
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자 20211018 scheo 다른조직 검색이 가능하여 수정
				WHERE += " AND USER_SEQ = " + this.cfg.getLong("user.seq");
			} 
			WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			
			//organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ") : organ);
			organ = (organ <= 0 ? this.cfg.getLong("user.organ") : (this.cfg.getInt("user.organ.depth") > depth ? this.cfg.getLong("user.organ") : organ));
			if (organ > 0) { // 조직
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ ")"
					;
			} 
		}
		
// 2017.06.26
//		
//		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
//			WHERE += " AND USER_SEQ = " + this.cfg.getLong("user.seq");
//		} else {
//			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
//				WHERE += " AND COMPANY_SEQ = " + this.cfg.getLong("user.company");
//			}
//
//			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
//				WHERE += " AND ORGANIZATION_SEQ IN ("
//							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//									+ " SEQ"
//								+ " FROM TB_ORGANIZATION"
//								+ " WHERE SORT = 1"
//								+ " START WITH COMPANY_SEQ = " + this.cfg.getLong("user.company")
//									+ " AND SEQ IN ("
//											+ " SELECT " + this.cfg.getLong("user.organ") + " FROM DUAL"
//											+ " UNION"
//											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
//													+ " ORGANIZATION_SEQ"
//												+ " FROM TB_USER_APP_ORGAN"
//												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
//										+ " )"
//								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//						+ " )"
//					;
//			}
//		}
//
//		if (company > 0) { // 소속
//			WHERE += " AND COMPANY_SEQ = " + company;
//		}
//
//		if (organ > 0) { // 조직
//			WHERE += " AND ORGANIZATION_SEQ IN ("
//						+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
//								+ " SEQ"
//							+ " FROM TB_ORGANIZATION"
//							+ " WHERE SORT = 1"
//							+ " START WITH SEQ = " + organ
//							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//					+ " )"
//				;
//		}

		if (place > 0) { // 위치
			WHERE += " AND VM_PLACE_SEQ = " + place;
		}

	//20160614 결제유형 다중선택 추가
		if ((payTypes != null) && (payTypes.length > 0)) {
			WHERE += " AND PAY_TYPE IN ('" + payTypes[0] + "'";
			for (int i = 1; i < payTypes.length; i++) WHERE += ", '" + payTypes[i] + "'";
			WHERE += ")";
	//20170329 키갱신 제외
		} else {
			WHERE += " AND A.PAY_TYPE <> '09'";
		}

		String SUM_OF_EACH_PREPAY_COMPANY_1 = "";
		String SUM_OF_EACH_PREPAY_COMPANY_2 = "";
		String[] prepaySummaryColumns = new String[] {};
		ArrayList<String> prepaySummaryColumnList = new ArrayList<String>();

	// 선불 카드사
		this.company = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(TB_CODE) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE"
						+ " WHERE TYPE = 'PAY_CARD'"
							+ " AND CODE <> '000'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				String code = rs.getString("CODE");
				c.put("CODE", code);
				c.put("NAME", rs.getString("NAME"));

				this.company.add(c);

				SUM_OF_EACH_PREPAY_COMPANY_1 += " COUNT(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN 1 END) AS CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN AMOUNT END), 0) AS AMOUNT_PREPAY_" + code + ","
						+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN 1 END) AS PAY_CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN COMMISSION END), 0) AS COMMISSION_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_PREPAY_" + code + ","
					;
				SUM_OF_EACH_PREPAY_COMPANY_2 += " COUNT(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN 1 END) AS CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN -AMOUNT END), 0) AS AMOUNT_PREPAY_" + code + ","
						+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN 1 END) AS PAY_CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN COMMISSION END), 0) AS COMMISSION_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN -AMOUNT + COMMISSION - OUTAMOUNT END), 0) AS PAY_AMOUNT_PREPAY_" + code + ","
					;
				prepaySummaryColumnList.add("CNT_PREPAY_" + code);
				prepaySummaryColumnList.add("AMOUNT_PREPAY_" + code);
				prepaySummaryColumnList.add("PAY_CNT_PREPAY_" + code);
				prepaySummaryColumnList.add("COMMISSION_PREPAY_" + code);
				prepaySummaryColumnList.add("PAY_AMOUNT_PREPAY_" + code);
			}

			prepaySummaryColumns = prepaySummaryColumnList.toArray(new String[prepaySummaryColumnList.size()]);
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			prepaySummaryColumnList = null;

			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();

		try {
			//scheo 2018.10.11 추가
			long[] grandTotalCounts = { 0, 0, 0, 0, 0, 0 };
			long[] grandTotalAmounts = { 0, 0, 0, 0, 0, 0 };
			long[] grandTotalPayCounts = { 0, 0, 0, 0 };
			long[] grandTotalCommissions = { 0, 0, 0, 0 };
			long[] grandTotalPayAmounts = { 0, 0, 0, 0 };
			//scheo 2018.10.11 추가

			ps = dbLib.prepareStatement(conn,
					" SELECT /*+ ORDERED USE_HASH(B C D) */"
							+ " A.*,"
							+ " NVL(TO_CHAR(TO_DATE(("
									+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
											+ " MAX(CLOSING_DATE || CLOSING_TIME)"
										+ " FROM TB_TXT_CLOSING"
										+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
											+ " AND (CLOSING_DATE || CLOSING_TIME) < A.MIN_DATE"
								+ " ), 'YYYYMMDDHH24MISS') + INTERVAL '1' SECOND, 'YYYYMMDDHH24MISS'), MIN_DATE) AS START_DATE,"
							+ " ("
										+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
												+ " NAME"
											+ " FROM TB_ORGANIZATION"
											+ " WHERE PARENT_SEQ = " + (organ > 0 ? organ : 0)
												+ " AND SORT = 1"
												+ " AND SEQ <> A.ORGANIZATION_SEQ"
											+ " START WITH SEQ = A.ORGANIZATION_SEQ"
											+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
									+ ") AS PARENT_ORGAN,"
							+ " B.NAME AS ORGAN,"
							+ " B.CODE AS ORGAN_CODE," //2017.12.21 jwhwang 추가
							+ " C.PLACE AS PLACE,"
							+ " D.CODE AS VM_CODE"
						+ " FROM ("
									+ " SELECT /*+ INDEX(A IX_SALES_" + YYYYMMDD + ") */"
											+ " " + YYYYMMDD + " AS YYYYMMDD,"
											+ " COMPANY_SEQ,"
											+ " ORGANIZATION_SEQ,"
											+ " VM_PLACE_SEQ,"
											+ " TERMINAL_ID,"
											+ " PAY_DATE,"
											+ " MIN(TRANSACTION_DATE || TRANSACTION_TIME) AS MIN_DATE,"
											+ " MAX(CASE WHEN CLOSING_DATE IS NOT NULL THEN CLOSING_DATE || NVL(CLOSING_TIME, '000000') END) AS END_DATE,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN AMOUNT END), 0) AS AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE IN ('02', '10') THEN 1 END) AS PAY_CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE IN ('02', '10') THEN COMMISSION END), 0) AS COMMISSION_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE IN ('02', '10') THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN AMOUNT END), 0) AS AMOUNT_CARD,"
											+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS PAY_CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '01' and pay_step <> '06' THEN COMMISSION END), 0) AS COMMISSION_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '01' and pay_step <> '06' THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_CARD,"
											//scheo 2018.10.11 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN AMOUNT END), 0) AS AMOUNT_PAYCO,"
											+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '07' THEN 1 END) AS PAY_CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '07' THEN COMMISSION END), 0) AS COMMISSION_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '07' THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_PAYCO,"
											//scheo 2018.10.11 추가
											+ SUM_OF_EACH_PREPAY_COMPANY_1
											+ " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN AMOUNT END), 0) AS AMOUNT_PREPAY,"
											+ " COUNT(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' THEN 1 END) AS PAY_CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' THEN COMMISSION END), 0) AS COMMISSION_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_DATE IS NOT NULL AND PAY_TYPE = '11' THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_PREPAY,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('01', '11') AND PAY_STEP IN ('01', '21') THEN 1 END) AS CNT_HELD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('01', '11') AND PAY_STEP IN ('01', '21') THEN AMOUNT END), 0) AS AMOUNT_HELD,"
											+ " COUNT(CASE WHEN PAY_STEP = '22' THEN 1 END) AS CNT_DECLINED,"
											+ " NVL(SUM(CASE WHEN PAY_STEP = '22' THEN AMOUNT END), 0) AS AMOUNT_DECLINED"
										+ " FROM TB_SALES A"
										+ " WHERE " + YYYYMMDD + " BETWEEN '"+ sDate + "' AND '" + eDate + "'"
											+ " AND PAY_STEP NOT IN ('00', '99')"
											+ WHERE
										+ " GROUP BY " + YYYYMMDD + ", COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, TERMINAL_ID, PAY_DATE"
									+ " UNION ALL"
									+ " SELECT"
											+ " YYYYMMDD,"
											+ " COMPANY_SEQ,"
											+ " ORGANIZATION_SEQ,"
											+ " VM_PLACE_SEQ,"
											+ " TERMINAL_ID,"
											+ " PAY_DATE2 AS PAY_DATE,"
											+ " MIN(CANCEL_DATE || CANCEL_TIME) AS MIN_DATE,"
											+ " MAX(("
													+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
															+ " MIN(CLOSING_DATE || NVL(CLOSING_TIME, '000000'))"
														+ " FROM TB_TXT_CLOSING"
														+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
													+ " AND (CLOSING_DATE || CLOSING_TIME) >= (A.CANCEL_DATE || A.CANCEL_TIME)"
												+ " )) AS END_DATE,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('02', '10') THEN 1 END) AS CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN -AMOUNT END), 0) AS AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE IN ('02', '10') THEN 1 END) AS PAY_CNT_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN -COMMISSION END), 0) AS COMMISSION_CASH,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('02', '10') THEN -AMOUNT + COMMISSION - OUTAMOUNT END), 0) AS PAY_AMOUNT_CASH,"
											+ " COUNT(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '01' and pay_step <> '06' THEN -AMOUNT END), 0) AS AMOUNT_CARD,"
											+ " COUNT(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '01' and pay_step <> '06' THEN 1 END) AS PAY_CNT_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '01' and pay_step <> '06' THEN -COMMISSION END), 0) AS COMMISSION_CARD,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '01' and pay_step <> '06' THEN -AMOUNT + COMMISSION - OUTAMOUNT END), 0) AS PAY_AMOUNT_CARD,"
											//scheo 2018.10.11 추가
											+ " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END) AS CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN -AMOUNT END), 0) AS AMOUNT_PAYCO,"
											+ " COUNT(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '07' THEN 1 END) AS PAY_CNT_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '07' THEN -COMMISSION END), 0) AS COMMISSION_PAYCO,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '07' THEN -AMOUNT + COMMISSION - OUTAMOUNT END), 0) AS PAY_AMOUNT_PAYCO,"
											//scheo 2018.10.11 추가
											+ SUM_OF_EACH_PREPAY_COMPANY_2
											+ " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END) AS CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN -AMOUNT END), 0) AS AMOUNT_PREPAY,"
											+ " COUNT(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '11' THEN 1 END) AS PAY_CNT_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '11' THEN -COMMISSION END), 0) AS COMMISSION_PREPAY,"
											+ " NVL(SUM(CASE WHEN PAY_DATE2 IS NOT NULL AND PAY_TYPE = '11' THEN -AMOUNT + COMMISSION - OUTAMOUNT END), 0) AS PAY_AMOUNT_PREPAY,"
											+ " COUNT(CASE WHEN PAY_TYPE IN ('01', '11') AND PAY_STEP IN ('01', '21') THEN 1 END) AS CNT_HELD,"
											+ " NVL(SUM(CASE WHEN PAY_TYPE IN ('01', '11') AND PAY_STEP IN ('01', '21') THEN AMOUNT END), 0) AS AMOUNT_HELD,"
											+ " COUNT(CASE WHEN PAY_STEP = '22' THEN 1 END) AS CNT_DECLINED,"
											+ " NVL(SUM(CASE WHEN PAY_STEP = '22' THEN AMOUNT END), 0) AS AMOUNT_DECLINED"
										+ " FROM ("
												+ " SELECT /*+ INDEX(A IX_SALES_PAY_STEP) */"
										+ (
											// 마감일(취소)
												"02".equals(sType) ? (
														" CASE WHEN (CANCEL_DATE || CANCEL_TIME) < (CLOSING_DATE || CLOSING_TIME)"
															+ " THEN CLOSING_DATE"
															+ " ELSE ("
																	+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																			+ " SUBSTR(MIN(CLOSING_DATE || CLOSING_TIME), 1, 8)"
																		+ " FROM TB_TXT_CLOSING"
																		+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																			+ " AND (CLOSING_DATE || CLOSING_TIME) >= (A.CANCEL_DATE || A.CANCEL_TIME)"
																+ " )"
															+ " END AS YYYYMMDD,"
													) :
											// 환수일/환수예정일
												"03".equals(sType) || "05".equals(sType) ? (
														" F_GET_WORKDAY@VANBT(CANCEL_DATE, 5) AS YYYYMMDD,"
													) :
											// 매입취소일
												"04".equals(sType) ? (
														" F_GET_WORKDAY@VANBT(CANCEL_DATE, 1) AS YYYYMMDD,"
													) :
											// 취소일
												(
														" CANCEL_DATE AS YYYYMMDD,"
													)
											)
														+ " A.*,"
														//+ " CASE WHEN (CANCEL_DATE || CANCEL_TIME) < (CLOSING_DATE || CLOSING_TIME)"
														//	+ " THEN CLOSING_DATE"
														//	+ " ELSE ("
														//			+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
														//					+ " SUBSTR(MIN(CLOSING_DATE || CLOSING_TIME), 1, 8)"
														//				+ " FROM TB_TXT_CLOSING"
														//				+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
														//					+ " AND (CLOSING_DATE || CLOSING_TIME) >= (A.CANCEL_DATE || A.CANCEL_TIME)"
														//		+ " )"
														//	+ " END AS CLOSING_DATE2,"
														+ " CASE WHEN F_GET_WORKDAY@VANBT(CANCEL_DATE, 5) <= TO_CHAR(SYSDATE, 'YYYYMMDD') THEN F_GET_WORKDAY@VANBT(CANCEL_DATE, 5) END AS PAY_DATE2"
													+ " FROM TB_SALES A"
													+ " WHERE PAY_STEP = '29'"
														+ " AND " + YYYYMMDD + " IS NOT NULL"
														+ WHERE
										+ (
											// 마감일(취소)
												"02".equals(sType) ? (
														" AND (CANCEL_DATE || CANCEL_TIME) BETWEEN ("
																+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																		+ " MAX(CLOSING_DATE || CLOSING_TIME) || '0'"
																	+ " FROM TB_TXT_CLOSING"
																	+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																		+ " AND (CLOSING_DATE || CLOSING_TIME) < '" + sDate + "'"
															+ " ) AND ("
																+ " SELECT /*+ INDEX(TB_TXT_CLOSING) */"
																		+ " MAX(CLOSING_DATE || CLOSING_TIME)"
																	+ " FROM TB_TXT_CLOSING"
																	+ " WHERE TERMINAL_ID = A.TERMINAL_ID"
																	+ " AND (CLOSING_DATE || CLOSING_TIME) <= '" + eDate + "235959'"
															+ " )"
														+ " AND CLOSING_DATE <= '" + eDate + "'"
														+ " AND CANCEL_DATE <= '" + eDate + "'"
													) :
											// 환수일/환수예정일
												"03".equals(sType) || "05".equals(sType) ? (
														" AND CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -5) AND F_GET_WORKDAY@VANBT('" + eDate + "', -5)"
													) :
											// 매입취소일
												"04".equals(sType) ? (
														" AND CANCEL_DATE BETWEEN F_GET_WORKDAY@VANBT('" + sDate + "', -1) AND F_GET_WORKDAY@VANBT('" + eDate + "', -1)"
													) :
											// 취소일
												(
														" AND CANCEL_DATE BETWEEN '" + sDate + "' AND '" + eDate + "'"
													)
											)
											+ " ) A"
										+ " WHERE YYYYMMDD BETWEEN '" + sDate + "' AND '" + eDate + "'"
										+ " GROUP BY YYYYMMDD, COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, TERMINAL_ID, PAY_DATE2"
								+ " ) A"
							+ " INNER JOIN TB_ORGANIZATION B"
								+ " ON A.ORGANIZATION_SEQ = B.SEQ"
							+ " INNER JOIN TB_VENDING_MACHINE_PLACE C"
								+ " ON A.VM_PLACE_SEQ = C.SEQ"
							+ " INNER JOIN TB_VENDING_MACHINE D"
								+ " ON C.VM_SEQ = D.SEQ"
						+ " ORDER BY"
// 정렬 값(oMode, oType)에 따라 ORDER BY 변경
	+ ((oType == 1) ? (	// DESC
				// 거래처
					(oMode == 1) ? (
							" PARENT_ORGAN DESC, ORGAN DESC, PLACE DESC" + (!"03".equals(sType) ? ", YYYYMMDD" : "")
						) :
				// 설치장소
					(oMode == 2) ? (
							" PARENT_ORGAN DESC, ORGAN DESC, PLACE DESC" + (!"03".equals(sType) ? ", YYYYMMDD" : "")
						) :
				// default: 날짜
					(
							 (!"03".equals(sType) ? " YYYYMMDD DESC," : "") + " PARENT_ORGAN, ORGAN, PLACE"
						)
			) : (			// ASC
				// 거래처
					(oMode == 1) ? (
							" PARENT_ORGAN, ORGAN, PLACE" + (!"03".equals(sType) ? ", YYYYMMDD" : "")
						) :
				// 설치장소
					(oMode == 2) ? (
							" PARENT_ORGAN, ORGAN, PLACE" + (!"03".equals(sType) ? ", YYYYMMDD" : "")
						) :
				// default: 날짜
					(
							 (!"03".equals(sType) ? " YYYYMMDD," : "") + " PARENT_ORGAN, ORGAN, PLACE"
						)
				)
		)
							+ ", VM_CODE, A.PAY_DATE NULLS FIRST, A.AMOUNT_CARD DESC"
				);
			rs = ps.executeQuery();

			int n = 0;
			String key = null;
			GeneralConfig c = null;
			String startDate = null;
			String endDate = null;


			while (rs.next()) {
				String newKey = (!"03".equals(sType) ? rs.getString("YYYYMMDD") : "") + rs.getString("VM_CODE");

				if (!newKey.equals(key)) {
					c = new GeneralConfig();
					this.list.add(c);
					key = newKey;
					n = 0;

					String yyyymmdd = rs.getString("YYYYMMDD");
					c.put("YYYYMMDD", yyyymmdd);
					c.put("DATE", yyyymmdd.substring(0, 4) + "-" + yyyymmdd.substring(4, 6) + "-" + yyyymmdd.substring(6, 8));

					c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
					c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
					c.put("VM_PLACE_SEQ", rs.getLong("VM_PLACE_SEQ"));
					c.put("PARENT_ORGAN", rs.getString("PARENT_ORGAN"));
					c.put("ORGAN", rs.getString("ORGAN"));
					c.put("ORGAN_CODE", rs.getString("ORGAN_CODE")); //2017.12.21 jwhwang 추가
					c.put("PLACE", rs.getString("PLACE"));
					c.put("VM_CODE", rs.getString("VM_CODE"));

					startDate = null;
					endDate = null;
				}

				if ((n == 0) || !c.get("PAY_DATE_" + n).equals("-")) n++;

				c.put("PAY_COUNT", n);

				String date = rs.getString("START_DATE");
				c.put("START_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				if ((startDate == null) || (startDate.compareTo(date) > 0)) {
					if (startDate != null) System.out.println("START_DATE : " + startDate + "-->" + date);
					c.put("START_DATE", c.get("START_DATE_" + n));
					startDate = date;
				}

				date = rs.getString("END_DATE");
				c.put("END_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				if ((endDate == null) || (endDate.compareTo(date) < 0)) {
					if (endDate != null) System.out.println("END_DATE : " + endDate + "-->" + date);
					c.put("END_DATE", c.get("END_DATE_" + n));
					endDate = date;
				}

				date = rs.getString("PAY_DATE");
				c.put("PAY_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8)
							: "-"
					);

				long count =  rs.getLong("CNT_CASH");
				long amount = rs.getLong("AMOUNT_CASH");
				long payCount = rs.getLong("PAY_CNT_CASH");
				long commission = rs.getLong("COMMISSION_CASH");
				long payAmount = rs.getLong("PAY_AMOUNT_CASH");
				long totalCount = count;
				long totalAmount = amount;
				long totalPayCount = payCount;
				long totalCommission = commission;
				long totalPayAmount = payAmount;

				grandTotalCounts[0] += count;
				grandTotalAmounts[0] += amount;
				grandTotalPayCounts[0] += payCount;
				grandTotalCommissions[0] += commission;
				grandTotalPayAmounts[0] += payAmount;
				c.put("PAY_CNT_CASH_" + n, payCount);
				c.put("COMMISSION_CASH_" + n, commission);
				c.put("PAY_AMOUNT_CASH_" + n, payAmount);
				c.put("CNT_CASH", c.getLong("CNT_CASH") + count);
				c.put("AMOUNT_CASH", c.getLong("AMOUNT_CASH") + amount);
				c.put("PAY_CNT_CASH", c.getLong("PAY_CNT_CASH") + payCount);
				c.put("COMMISSION_CASH", c.getLong("COMMISSION_CASH") + commission);
				c.put("PAY_AMOUNT_CASH", c.getLong("PAY_AMOUNT_CASH") + payAmount);

				count =  rs.getLong("CNT_CARD");
				amount = rs.getLong("AMOUNT_CARD");
				payCount = rs.getLong("PAY_CNT_CARD");
				commission = rs.getLong("COMMISSION_CARD");
				payAmount = rs.getLong("PAY_AMOUNT_CARD");
				totalCount += count;
				totalAmount += amount;
				totalPayCount += payCount;
				totalCommission += commission;
				totalPayAmount += payAmount;
				grandTotalCounts[1] += count;
				grandTotalAmounts[1] += amount;
				grandTotalPayCounts[1] += payCount;
				grandTotalCommissions[1] += commission;
				grandTotalPayAmounts[1] += payAmount;
				c.put("PAY_CNT_CARD_" + n, payCount);
				c.put("COMMISSION_CARD_" + n, commission);
				c.put("PAY_AMOUNT_CARD_" + n, payAmount);
				c.put("CNT_CARD", c.getLong("CNT_CARD") + count);
				c.put("AMOUNT_CARD", c.getLong("AMOUNT_CARD") + amount);
				c.put("PAY_CNT_CARD", c.getLong("PAY_CNT_CARD") + payCount);
				c.put("COMMISSION_CARD", c.getLong("COMMISSION_CARD") + commission);
				c.put("PAY_AMOUNT_CARD", c.getLong("PAY_AMOUNT_CARD") + payAmount);

				//scheo 2018.10.11 추가
				count =  rs.getLong("CNT_PAYCO");
				amount = rs.getLong("AMOUNT_PAYCO");
				payCount = rs.getLong("PAY_CNT_PAYCO");
				commission = rs.getLong("COMMISSION_PAYCO");
				payAmount = rs.getLong("PAY_AMOUNT_PAYCO");
				totalCount += count;
				totalAmount += amount;
				totalPayCount += payCount;
				totalCommission += commission;
				totalPayAmount += payAmount;
				grandTotalCounts[2] += count;
				grandTotalAmounts[2] += amount;
				grandTotalPayCounts[2] += payCount;
				grandTotalCommissions[2] += commission;
				grandTotalPayAmounts[2] += payAmount;
				c.put("PAY_CNT_PAYCO_" + n, payCount);
				c.put("COMMISSION_PAYCO_" + n, commission);
				c.put("PAY_AMOUNT_PAYCO_" + n, payAmount);
				c.put("CNT_PAYCO", c.getLong("CNT_PAYCO") + count);
				c.put("AMOUNT_PAYCO", c.getLong("AMOUNT_PAYCO") + amount);
				c.put("PAY_CNT_PAYCO", c.getLong("PAY_CNT_PAYCO") + payCount);
				c.put("COMMISSION_PAYCO", c.getLong("COMMISSION_PAYCO") + commission);
				c.put("PAY_AMOUNT_PAYCO", c.getLong("PAY_AMOUNT_PAYCO") + payAmount);
				//scheo 2018.10.11 추가
				
				count =  rs.getLong("CNT_PREPAY");
				amount = rs.getLong("AMOUNT_PREPAY");
				payCount = rs.getLong("PAY_CNT_PREPAY");
				commission = rs.getLong("COMMISSION_PREPAY");
				payAmount = rs.getLong("PAY_AMOUNT_PREPAY");
				totalCount += count;
				totalAmount += amount;
				totalPayCount += payCount;
				totalCommission += commission;
				totalPayAmount += payAmount;
				grandTotalCounts[3] += count;
				grandTotalAmounts[3] += amount;
				grandTotalPayCounts[3] += payCount;
				grandTotalCommissions[3] += commission;
				grandTotalPayAmounts[3] += payAmount;
				/*
				c.put("PAY_CNT_PREPAY" + n, count);
				c.put("COMMISSION_PREPAY" + n, commission);
				c.put("PAY_AMOUNT_PREPAY" + n, amount);
				c.put("CNT_PREPAY", c.getLong("CNT_PREPAY") + rs.getLong("CNT_PREPAY"));
				c.put("AMOUNT_PREPAY", c.getLong("AMOUNT_PREPAY") + rs.getLong("AMOUNT_PREPAY"));
				c.put("PAY_CNT_PREPAY", c.getLong("PAY_CNT_PREPAY") + count);
				c.put("COMMISSION_PREPAY", c.getLong("COMMISSION_PREPAY") + commission);
				c.put("PAY_AMOUNT_PREPAY", c.getLong("PAY_AMOUNT_PREPAY") + amount);
				*/
				c.put("PAY_CNT_TOTAL_" + n, totalPayCount);
				c.put("COMMISSION_TOTAL_" + n, totalCommission);
				c.put("PAY_AMOUNT_TOTAL_" + n, totalPayAmount);
				c.put("CNT_TOTAL", c.getLong("CNT_TOTAL") + totalCount);
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_TOTAL") + totalAmount);
				c.put("PAY_CNT_TOTAL", c.getLong("PAY_CNT_TOTAL") + totalPayCount);
				c.put("COMMISSION_TOTAL", c.getLong("COMMISSION_TOTAL") + totalCommission);
				c.put("PAY_AMOUNT_TOTAL", c.getLong("PAY_AMOUNT_TOTAL") + totalPayAmount);

				grandTotalCounts[4] += count = rs.getLong("CNT_HELD");
				grandTotalAmounts[4] += amount = rs.getLong("AMOUNT_HELD");
				c.put("CNT_HELD", c.getLong("CNT_HELD") + count);
				c.put("AMOUNT_HELD", c.getLong("AMOUNT_HELD") + amount);

				grandTotalCounts[5] += count = rs.getLong("CNT_DECLINED");
				grandTotalAmounts[5] += amount = rs.getLong("AMOUNT_DECLINED");
				c.put("CNT_DECLINED", c.getLong("CNT_DECLINED") + count);
				c.put("AMOUNT_DECLINED", c.getLong("AMOUNT_DECLINED") + amount);

				for (String columnName : prepaySummaryColumns) {
					c.put(columnName + "_" + n, rs.getLong(columnName));
					c.put(columnName, c.getLong(columnName) + rs.getLong(columnName));
					this.data.put(columnName, this.data.getLong(columnName) + rs.getLong(columnName));
				}
			}

			this.data.put("CNT_CASH", grandTotalCounts[0]);
			this.data.put("AMOUNT_CASH", grandTotalAmounts[0]);
			this.data.put("PAY_CNT_CASH", grandTotalPayCounts[0]);
			this.data.put("COMMISSION_CASH", grandTotalCommissions[0]);
			this.data.put("PAY_AMOUNT_CASH", grandTotalPayAmounts[0]);

			this.data.put("CNT_CARD", grandTotalCounts[1]);
			this.data.put("AMOUNT_CARD", grandTotalAmounts[1]);
			this.data.put("PAY_CNT_CARD", grandTotalPayCounts[1]);
			this.data.put("COMMISSION_CARD", grandTotalCommissions[1]);
			this.data.put("PAY_AMOUNT_CARD", grandTotalPayAmounts[1]);
			
			//scheo 2018.10.11 추가
			this.data.put("CNT_PAYCO", grandTotalCounts[2]);
			this.data.put("AMOUNT_PAYCO", grandTotalAmounts[2]);
			this.data.put("PAY_CNT_PAYCO", grandTotalPayCounts[2]);
			this.data.put("COMMISSION_PAYCO", grandTotalCommissions[2]);
			this.data.put("PAY_AMOUNT_PAYCO", grandTotalPayAmounts[2]);
			//scheo 2018.10.11 추가
			
			/*
			this.data.put("CNT_PREPAY", grandTotalCounts[2]);
			this.data.put("AMOUNT_PREPAY", grandTotalAmounts[2]);
			this.data.put("PAY_CNT_PREPAY", grandTotalPayCounts[2]);
			this.data.put("COMMISSION_PREPAY", grandTotalCommissions[2]);
			this.data.put("PAY_AMOUNT_PREPAY", grandTotalPayAmounts[2]);
			*/
			this.data.put("CNT_TOTAL", grandTotalCounts[0] + grandTotalCounts[1] + grandTotalCounts[2] + grandTotalCounts[3]);//scheo 2018.10.11 추가
			this.data.put("AMOUNT_TOTAL", grandTotalAmounts[0] + grandTotalAmounts[1] + grandTotalAmounts[2] + grandTotalAmounts[3]);//scheo 2018.10.11 추가
			this.data.put("PAY_CNT_TOTAL", grandTotalPayCounts[0] + grandTotalPayCounts[1] + grandTotalPayCounts[2] + grandTotalPayCounts[3]);//scheo 2018.10.11 추가
			this.data.put("COMMISSION_TOTAL", grandTotalCommissions[0] + grandTotalCommissions[1] + grandTotalCommissions[2] + grandTotalCommissions[3]);//scheo 2018.10.11 추가
			this.data.put("PAY_AMOUNT_TOTAL", grandTotalPayAmounts[0] + grandTotalPayAmounts[1] + grandTotalPayAmounts[2] + grandTotalPayAmounts[3]);//scheo 2018.10.11 추가

			this.data.put("CNT_HELD", grandTotalCounts[4]);
			this.data.put("AMOUNT_HELD", grandTotalAmounts[4]);

			this.data.put("CNT_DECLINED", grandTotalCounts[5]);
			this.data.put("AMOUNT_DECLINED", grandTotalAmounts[5]);
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 조직명
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
						+ " FROM ("
								+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
										+ " NAME"
									+ " FROM TB_ORGANIZATION"
									+ " WHERE COMPANY_SEQ = " + company
										+ " AND SORT = 0"
									+ " ORDER BY DEPTH DESC"
							+ " )"
						+ " WHERE ROWNUM = 1"
					)
			);

	// 검색 설정
		String sDesc = "집계기준="
				+ dbLib.getResult(conn,
						"SELECT /*+ INDEX(TB_CODE) */"
								+ " NAME"
							+ " FROM TB_CODE"
							+ " WHERE TYPE = 'SUM_TYPE'"
								+ " AND CODE = '" + sType + "'"
					)
				+ "&집계기간=" + sDate + "-" + eDate
			;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}

		this.data.put("sDesc", sDesc);

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}

	/**
	 * 마감별 입금현황(쿼리수정본)
	 *
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 검색일
	 * @param eDate 종료일
	 * @param payTypes 결제유형
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String payment(long company, long organ, long place, String sDate, String eDate, String[] payTypes, int oMode, int oType) throws Exception {
	// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
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

	// 자판기별 매출 현황
		this.company = new ArrayList<GeneralConfig>();

	// 검색절 생성
		String WHERE = " A.YYYYMMDD BETWEEN '" + sDate + "' AND '" + eDate + "'"
				+ " AND A.TYPE IN ('05', '07', '08')"
			;

		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
			WHERE += " AND USER_SEQ = " + this.cfg.getLong("user.seq");
		} else {
			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
				WHERE += " AND COMPANY_SEQ = " + this.cfg.getLong("user.company");
			}

			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
				WHERE += " AND ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
								+ " START WITH COMPANY_SEQ = " + this.cfg.getLong("user.company")
									+ " AND SEQ IN ("
											+ " SELECT " + this.cfg.getLong("user.organ") + " FROM DUAL"
											+ " UNION"
											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
													+ " ORGANIZATION_SEQ"
												+ " FROM TB_USER_APP_ORGAN"
												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
										+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
					;
			}
		}

		if (company > 0) { // 소속
			WHERE += " AND COMPANY_SEQ = " + company;
		}

		if (organ > 0) { // 조직
			WHERE += " AND ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION"
							+ " WHERE SORT = 1"
							+ " START WITH SEQ = " + organ
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;
		}

		if (place > 0) { // 위치
			WHERE += " AND VM_PLACE_SEQ = " + place;
		}

	//20160712 결제유형 다중선택 추가
		if ((payTypes != null) && (payTypes.length > 0)) {
			WHERE += " AND PAY_TYPE IN ('" + payTypes[0] + "'";
			for (int i = 1; i < payTypes.length; i++) WHERE += ", '" + payTypes[i] + "'";
			WHERE += ")";
	//20170329 키갱신 제외
		} else {
			WHERE += " AND A.PAY_TYPE <> '09'";
		}

		String SUMMARIES_OF_EACH_PREPAY_COMPANY = "";
		String PAYMENT_SUMMARIES_OF_EACH_PREPAY_COMPANY = "";
		String COLUMNS_OF_EACH_PREPAY_COMPANY = "";
		String[] prepaySummaryColumns = new String[] {};
		String[] prepayPaymentSummaryColumns = new String[] {};

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(TB_CODE) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE"
						+ " WHERE TYPE = 'PAY_CARD'"
							+ " AND CODE <> '000'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			ArrayList<String> prepaySummaryColumnList = new ArrayList<String>();
			ArrayList<String> prepayPaymentSummaryColumnList = new ArrayList<String>();

			while(rs.next())
			{
				GeneralConfig c = new GeneralConfig();
				String code = rs.getString("CODE");

				c.put("CODE", code);
				c.put("NAME", rs.getString("NAME"));

				this.company.add(c);

				SUMMARIES_OF_EACH_PREPAY_COMPANY += " NVL(SUM(CASE WHEN TYPE = '04' AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN CNT END), 0) AS CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN TYPE = '04' AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN AMOUNT END), 0) AS AMOUNT_PREPAY_" + code + ","
					;
				PAYMENT_SUMMARIES_OF_EACH_PREPAY_COMPANY += " NVL(SUM(CASE WHEN TYPE = '05' AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN CNT END), 0) AS PAY_CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN TYPE = '05' AND PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_PREPAY_" + code + ","
					;
				COLUMNS_OF_EACH_PREPAY_COMPANY += " AA.CNT_PREPAY_" + code + ","
						+ " AA.AMOUNT_PREPAY_" + code + ","
						+ " AB.PAY_CNT_PREPAY_" + code + ","
						+ " AB.PAY_AMOUNT_PREPAY_" + code + ","
					;
				prepaySummaryColumnList.add("CNT_PREPAY_" + code);
				prepaySummaryColumnList.add("AMOUNT_PREPAY_" + code);
				prepayPaymentSummaryColumnList.add("PAY_CNT_PREPAY_" + code);
				prepayPaymentSummaryColumnList.add("PAY_AMOUNT_PREPAY_" + code);
			}

			prepaySummaryColumns = prepaySummaryColumnList.toArray(new String[prepaySummaryColumnList.size()]);
			prepayPaymentSummaryColumns = prepayPaymentSummaryColumnList.toArray(new String[prepayPaymentSummaryColumnList.size()]);
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

		this.data = new GeneralConfig();
		this.list = new ArrayList<GeneralConfig>();

		try {
			long[] cnt = {0, 0, 0, 0, 0, 0, 0, 0};
			long[] amt = {0, 0, 0, 0, 0, 0, 0, 0};

			ps = dbLib.prepareStatement(conn,
					"WITH AB AS ("
							+ " SELECT /*+ INDEX(A IX_CLOSING_YYYYMMDD) */"
									+ " YYYY || MM || DD AS CLOSING_DATE,"
									+ " ORGANIZATION_SEQ,"
									+ " VM_PLACE_SEQ,"
									+ " YYYYMMDD AS PAY_DATE,"
									+ " NVL(SUM(CASE WHEN TYPE = '05' AND PAY_TYPE = '01' and pay_step <> '06' THEN CNT END), 0) AS PAY_CNT_CARD,"
									+ " NVL(SUM(CASE WHEN TYPE = '05' AND PAY_TYPE = '01' and pay_step <> '06' THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_CARD,"
									+ " NVL(SUM(CASE WHEN TYPE = '05' AND PAY_TYPE = '07' THEN CNT END), 0) AS PAY_CNT_PAYCO,"
									+ " NVL(SUM(CASE WHEN TYPE = '05' AND PAY_TYPE = '07' THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_PAYCO,"
									+ PAYMENT_SUMMARIES_OF_EACH_PREPAY_COMPANY
									+ " NVL(SUM(CASE WHEN TYPE = '05' AND PAY_TYPE = '11' THEN CNT END), 0) AS PAY_CNT_PREPAY,"
									+ " NVL(SUM(CASE WHEN TYPE = '05' AND PAY_TYPE = '11' THEN AMOUNT - COMMISSION + OUTAMOUNT END), 0) AS PAY_AMOUNT_PREPAY"
								+ " FROM TB_CLOSING A"
								+ " WHERE" + WHERE
								+ " GROUP BY YYYY || MM || DD, ORGANIZATION_SEQ, VM_PLACE_SEQ, YYYYMMDD"
						+ " )"
					+ " SELECT /*+ ORDERED USE_HASH(AB B C D) */"
								+ " AA.CLOSING_DATE AS YYYYMMDD,"
								+ " AA.CLOSING_DATE,"
								+ " AA.ORGANIZATION_SEQ,"
								+ " AA.VM_PLACE_SEQ,"
								+ " B.NAME AS ORGAN,"
								+ " C.PLACE,"
								+ " D.CODE AS VM_CODE,"
								+ " AA.START_DATE,"
								+ " AA.END_DATE,"
								+ " AA.CNT,"
								+ " AA.AMOUNT,"
								+ " AA.CNT_CARD,"
								+ " AA.AMOUNT_CARD,"
								+ " AA.CNT_PREPAY,"
								+ " AA.AMOUNT_PREPAY,"
								+ " AA.CNT_POSTPONE,"
								+ " AA.AMOUNT_POSTPONE,"
								+ " AA.CNT_REFUSE,"
								+ " AA.AMOUNT_REFUSE,"
								+ " AB.PAY_DATE,"
								+ " AB.PAY_CNT_CARD,"
								+ " AB.PAY_AMOUNT_CARD,"
								+ COLUMNS_OF_EACH_PREPAY_COMPANY
								+ " AB.PAY_CNT_PREPAY,"
								+ " AB.PAY_AMOUNT_PREPAY"
							+ " FROM ("
										+ " SELECT /*+ INDEX(A) */"
												+ " YYYY || MM || DD AS CLOSING_DATE,"
												+ " ORGANIZATION_SEQ,"
												+ " VM_PLACE_SEQ,"
												+ " MIN(CASE WHEN START_DATE IS NOT NULL THEN START_DATE || NVL(START_TIME, '000000') END) AS START_DATE,"
												+ " MAX(CASE WHEN END_DATE IS NOT NULL THEN END_DATE || NVL(END_TIME, '000000') END) AS END_DATE,"
												+ " NVL(SUM(CNT), 0) AS CNT, NVL(SUM(AMOUNT), 0) AS AMOUNT,"
												+ " NVL(SUM(CASE WHEN TYPE = '04' AND PAY_TYPE = '01' and pay_step <> '06' THEN CNT END), 0) AS CNT_CARD,"
												+ " NVL(SUM(CASE WHEN TYPE = '04' AND PAY_TYPE = '01' and pay_step <> '06' THEN AMOUNT END), 0) AS AMOUNT_CARD,"
												+ " NVL(SUM(CASE WHEN TYPE = '04' AND PAY_TYPE = '07' THEN CNT END), 0) AS CNT_PAYCO,"
												+ " NVL(SUM(CASE WHEN TYPE = '04' AND PAY_TYPE = '07' THEN AMOUNT END), 0) AS AMOUNT_PAYCO,"
												+ SUMMARIES_OF_EACH_PREPAY_COMPANY
												+ " NVL(SUM(CASE WHEN TYPE = '04' AND PAY_TYPE = '11' THEN CNT END), 0) AS CNT_PREPAY,"
												+ " NVL(SUM(CASE WHEN TYPE = '04' AND PAY_TYPE = '11' THEN AMOUNT END), 0) AS AMOUNT_PREPAY,"
												+ " NVL(SUM(CASE WHEN TYPE = '07' THEN CNT END), 0) AS CNT_POSTPONE,"
												+ " NVL(SUM(CASE WHEN TYPE = '07' THEN AMOUNT END), 0) AS AMOUNT_POSTPONE,"
												+ " NVL(SUM(CASE WHEN TYPE = '08' THEN CNT END), 0) AS CNT_REFUSE,"
												+ " NVL(SUM(CASE WHEN TYPE = '08' THEN AMOUNT END), 0) AS AMOUNT_REFUSE"
											+ " FROM TB_CLOSING A"
											+ " WHERE COMPANY_SEQ = " + company
												+ " AND TYPE IN ('04', '07', '08')"
												+ " AND EXISTS ("
														+ " SELECT 1"
															+ " FROM AB"
															+ " WHERE CLOSING_DATE = (A.YYYY || A.MM || A.DD)"
																+ " AND ORGANIZATION_SEQ = A.ORGANIZATION_SEQ"
																+ " AND VM_PLACE_SEQ = A.VM_PLACE_SEQ"
													+ " )"
											+ " GROUP BY YYYY || MM || DD, ORGANIZATION_SEQ, VM_PLACE_SEQ"
									+ " ) AA"
								+ " INNER JOIN AB"
									+ " ON AB.CLOSING_DATE = AA.CLOSING_DATE"
										+ " AND AB.ORGANIZATION_SEQ = AA.ORGANIZATION_SEQ"
										+ " AND AB.VM_PLACE_SEQ = AA.VM_PLACE_SEQ"
								+ " INNER JOIN TB_ORGANIZATION B"
									+ " ON AA.ORGANIZATION_SEQ = B.SEQ"
								+ " INNER JOIN TB_VENDING_MACHINE_PLACE C"
									+ " ON AA.VM_PLACE_SEQ = C.SEQ"
								+ " INNER JOIN TB_VENDING_MACHINE D"
									+ " ON C.VM_SEQ = D.SEQ"
							+ " WHERE AB.PAY_CNT_CARD > 0"
								+ " OR AB.PAY_CNT_PREPAY > 0"
					// 정렬 값(oMode, oType)에 따라 ORDER BY 변경
						+ ((oType == 1) ? (	// DESC
							// 설치장소
								(oMode == 2) ?	" ORDER BY C.PLACE DESC, B.NAME DESC, D.CODE DESC, AA.START_DATE, AA.END_DATE, AB.PAY_DATE" :
							// default: 거래처
												" ORDER BY B.NAME DESC, C.PLACE DESC, D.CODE DESC, AA.START_DATE, AA.END_DATE, AB.PAY_DATE"
							) : (			// ASC
							// 설치장소
								(oMode == 2) ?	" ORDER BY C.PLACE, B.NAME, D.CODE, AA.START_DATE, AA.END_DATE, AB.PAY_DATE" :
							// default: 거래처
												" ORDER BY B.NAME, C.PLACE, D.CODE, AA.START_DATE, AA.END_DATE, AB.PAY_DATE"
								)
							)
				);
			rs = ps.executeQuery();

			int rowCount = 0;
			int listCount = 0;

			String preRowKey = null;
			GeneralConfig masterRow = null;

			while (rs.next())
			{
				GeneralConfig row = new GeneralConfig();
				String rowKey = rs.getString("YYYYMMDD")
						+ rs.getString("ORGANIZATION_SEQ")
						+ rs.getString("VM_PLACE_SEQ")
						+ rs.getString("START_DATE")
						+ rs.getString("END_DATE")
					;
				boolean isMasterRow = !rowKey.equals(preRowKey);

				preRowKey = rowKey;

			//공통사항
				row.put("YYYYMMDD", rs.getString("YYYYMMDD"));
				//row.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				row.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				row.put("VM_PLACE_SEQ", rs.getLong("VM_PLACE_SEQ"));
				row.put("ORGAN", rs.getString("ORGAN"));
				row.put("PLACE", rs.getString("PLACE"));
				row.put("VM_CODE", rs.getString("VM_CODE"));

			//마감현황 입력
				String date = rs.getString("START_DATE");
				row.put("START_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: "-"
					);
				date = rs.getString("END_DATE");
				row.put("END_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: "-"
					);
			//마감일은 포멧처리 안 함
				row.put("CLOSING_DATE", rs.getString("CLOSING_DATE"));

				row.put("CNT_CARD", rs.getLong("CNT_CARD"));
				row.put("AMOUNT_CARD", rs.getLong("AMOUNT_CARD"));
				row.put("CNT_PAYCO", rs.getLong("CNT_PAYCO"));
				row.put("AMOUNT_PAYCO", rs.getLong("AMOUNT_PAYCO"));
				row.put("CNT_PREPAY", rs.getLong("CNT_PREPAY"));
				row.put("AMOUNT_PREPAY", rs.getLong("AMOUNT_PREPAY"));
				row.put("CNT_POSTPONE", rs.getLong("CNT_POSTPONE"));
				row.put("AMOUNT_POSTPONE", rs.getLong("AMOUNT_POSTPONE"));
				row.put("CNT_REFUSE", rs.getLong("CNT_REFUSE"));
				row.put("AMOUNT_REFUSE", rs.getLong("AMOUNT_REFUSE"));

				//선불카드사별 마감현황
				for (String columnName : prepaySummaryColumns) {
					row.put(columnName, rs.getLong(columnName));
					if (isMasterRow) this.data.put(columnName, this.data.getLong(columnName) + rs.getLong(columnName));
				}

			//입금현황 입력
			//마감일은 포멧처리 안 함
				row.put("PAY_DATE", rs.getString("PAY_DATE"));

				row.put("PAY_CNT_CARD", rs.getLong("PAY_CNT_CARD"));
				row.put("PAY_AMOUNT_CARD", rs.getLong("PAY_AMOUNT_CARD"));
				row.put("PAY_CNT_PAYCO", rs.getLong("PAY_CNT_PAYCO"));
				row.put("PAY_AMOUNT_PAYCO", rs.getLong("PAY_AMOUNT_PAYCO"));
				row.put("PAY_CNT_PREPAY", rs.getLong("PAY_CNT_PREPAY"));
				row.put("PAY_AMOUNT_PREPAY", rs.getLong("PAY_AMOUNT_PREPAY"));

			//선불카드사별 입금현황
				for (String columnName : prepayPaymentSummaryColumns) {
					row.put(columnName, rs.getLong(columnName));
					this.data.put(columnName, this.data.getLong(columnName) + rs.getLong(columnName));
				}

				this.list.add(row);

				if (isMasterRow) {
					cnt[0] += rs.getLong("CNT_CARD");
					cnt[1] += rs.getLong("CNT_PAYCO");
					cnt[2] += rs.getLong("CNT_PREPAY");
					cnt[3] += rs.getLong("PAY_CNT_CARD");
					cnt[4] += rs.getLong("PAY_CNT_PAYCO");
					cnt[5] += rs.getLong("PAY_CNT_PREPAY");
					cnt[6] += rs.getLong("CNT_POSTPONE");
					cnt[7] += rs.getLong("CNT_REFUSE");

					amt[0] += rs.getLong("AMOUNT_CARD");
					amt[1] += rs.getLong("AMOUNT_PAYCO");
					amt[2] += rs.getLong("AMOUNT_PREPAY");
					amt[3] += rs.getLong("PAY_AMOUNT_CARD");
					amt[4] += rs.getLong("PAY_AMOUNT_PAYCO");
					amt[5] += rs.getLong("PAY_AMOUNT_PREPAY");
					amt[6] += rs.getLong("AMOUNT_POSTPONE");
					amt[7] += rs.getLong("AMOUNT_REFUSE");

					if (masterRow != null) masterRow.put("ROWSPANCNT", rowCount);

					listCount++;
					rowCount = 1;
					masterRow = row;
				} else {
					cnt[3] += rs.getLong("PAY_CNT_CARD");
					cnt[4] += rs.getLong("PAY_CNT_PAYCO");
					cnt[5] += rs.getLong("PAY_CNT_PREPAY");

					amt[3] += rs.getLong("PAY_AMOUNT_CARD");
					amt[4] += rs.getLong("PAY_AMOUNT_PAYCO");
					amt[5] += rs.getLong("PAY_AMOUNT_PREPAY");

					rowCount++;
					row.put("ROWSPANCNT", 0);
				}
			}

			if (masterRow != null) masterRow.put("ROWSPANCNT", rowCount);

			this.data.put("LISTCNT", listCount);

		// 합계
			this.data.put("CNT_CARD", cnt[0]);
			this.data.put("CNT_PAYCO", cnt[1]);
			this.data.put("CNT_PREPAY", cnt[2]);
			this.data.put("PAY_CNT_CARD", cnt[3]);
			this.data.put("PAY_CNT_PAYCO", cnt[4]);
			this.data.put("PAY_CNT_PREPAY", cnt[5]);
			this.data.put("CNT_POSTPONE", cnt[6]);
			this.data.put("CNT_REFUSE", cnt[7]);
			this.data.put("AMOUNT_CARD", amt[0]);
			this.data.put("AMOUNT_PAYCO", amt[1]);
			this.data.put("AMOUNT_PREPAY", amt[2]);
			this.data.put("PAY_AMOUNT_CARD", amt[3]);
			this.data.put("PAY_AMOUNT_PAYCO", amt[4]);
			this.data.put("PAY_AMOUNT_PREPAY", amt[5]);
			this.data.put("AMOUNT_POSTPONE", amt[6]);
			this.data.put("AMOUNT_REFUSE", amt[7]);
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
			e.printStackTrace();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}

	// 최하위 조직 타이틀
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
							+ " FROM ("
									+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
											+ " NAME"
										+ " FROM TB_ORGANIZATION"
										+ " WHERE COMPANY_SEQ = " + company
											+ " AND SORT = 0"
										+ " ORDER BY DEPTH DESC"
								+ " )"
							+ " WHERE ROWNUM = 1"
					)
			);

	// 검색 설정
		String sDesc = "집계기간=" + sDate + "-" + eDate;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}

		this.data.put("sDesc", sDesc);

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}

	/**
	 * 판매계수
	 *
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 검색일
	 * @param eDate 종료일
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String salesCountList(long company, long organ, long depth, long place, String sDate, String eDate, int oMode, int oType) throws Exception {
	// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
			return null;
		}

	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String error = null;
		long krr = 264L;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			throw new Exception("DB 연결에 실패하였습니다.");
		}
		
		//검색절 생성
		String WHERE = "";
		String TERMINAL_WHERE = "";
		String SALESCOUNT_RANGE = "";
		
		company = (company > 0 ? company : this.cfg.getLong("user.company"));
		if (company > 0) { // 소속
			TERMINAL_WHERE += " A.TERMINAL_ID IN ("
					+ "SELECT /*+ INDEX(TB_VENDING_MACHINE) */"
					+ " TERMINAL_ID"
				+ " FROM TB_VENDING_MACHINE "
				+ " WHERE COMPANY_SEQ = " + company;
			
			if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자는 등록된 자판기만 볼 수 있음
				TERMINAL_WHERE += " AND USER_SEQ = " + this.cfg.get("user.seq");
			}
		} 
		
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 

			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			if (organ > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
				TERMINAL_WHERE += " AND ORGANIZATION_SEQ IN ("						
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
								+ " START WITH COMPANY_SEQ = " + company
								+ " AND SEQ IN ("
										+ " SELECT " + organ + " FROM DUAL"
										+ " UNION"
										+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
												+ " ORGANIZATION_SEQ"
											+ " FROM TB_USER_APP_ORGAN"
											+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
									+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ ")"
				;
			} 
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
		
			//organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ") : organ);
			organ = (organ <= 0 ? this.cfg.getLong("user.organ") : (this.cfg.getInt("user.organ.depth") > depth ? this.cfg.getLong("user.organ") : organ));
			if (organ > 0) { // 조직
				TERMINAL_WHERE +=  " AND ORGANIZATION_SEQ IN ("						
								+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
										+ " SEQ"
									+ " FROM TB_ORGANIZATION"
									+ " WHERE SORT = 1"
									+ " START WITH SEQ = " + organ
									+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
							+ ")"
					;
			} 
		}
		
		if (place > 0) {// 위치
			TERMINAL_WHERE += " AND A.TERMINAL_ID = ("
					+ " SELECT /*+ INDEX(TB_VENDING_MACHINE) */"
						+ " TERMINAL_ID"
					+ " FROM TB_VENDING_MACHINE"
					+ " WHERE SEQ = ("
						+ "SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
							+ " VM_SEQ"
						+ " FROM TB_VENDING_MACHINE_PLACE"
						+ " WHERE SEQ =" + place
					+ ")"
			    + ")"
			;	
		}
		
		//if (!TERMINAL_WHERE.isEmpty())
		TERMINAL_WHERE += " ) ";
		
		String SUM_OF_EACH_PREPAY_COMPANY = "";
		String[] prepaySummaryColumns = new String[] {};
		ArrayList<String> prepaySummaryColumnList = new ArrayList<String>();		
		
		// 선불 카드사
		this.company = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(TB_CODE) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE"
						+ " WHERE TYPE = 'PAY_CARD'"
							+ " AND CODE <> '000'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				String code = rs.getString("CODE");
				c.put("CODE", code);
				c.put("NAME", rs.getString("NAME"));

				this.company.add(c);

				SUM_OF_EACH_PREPAY_COMPANY += " COUNT(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN 1 END) AS CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN AMOUNT END), 0) AS AMOUNT_PREPAY_" + code + ","
					;
				prepaySummaryColumnList.add("CNT_PREPAY_" + code);
				prepaySummaryColumnList.add("AMOUNT_PREPAY_" + code);
			}

			prepaySummaryColumns = prepaySummaryColumnList.toArray(new String[prepaySummaryColumnList.size()]);
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			prepaySummaryColumnList = null;

			dbLib.close(rs);
			dbLib.close(ps);
		}		
		
		// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}		
		

		
		SALESCOUNT_RANGE += " SELECT TERMINAL_ID, MAX(COUNT_DATE) AS COUNT_DATE " //당일 마지막 마감
            + " FROM TB_SALESCOUNT A "
            + " WHERE " + TERMINAL_WHERE
                + " AND COL_NO = 0" 
                + " AND COUNT_MODE = 'A'"
                + (company==krr?
                		" AND TRIM(GOODS_CODE) <> '000000'":"" 
                )
                + " AND COUNT_DATE >= TO_DATE('" + sDate + "' || '000000', 'YYYYMMDDHH24MISS')" 
                + " AND COUNT_DATE < TO_DATE('" + eDate + "' || '000000', 'YYYYMMDDHH24MISS') + 1"
                //+ " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64)"
                + (company==krr? 
	                " AND ("
	                	+ " (COL_COUNT <= 24 AND   (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1)) "
	                    + " OR  (COL_COUNT > 24 AND (SELECT MAX(COL_NO) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) = (COL_COUNT )) "
	                + ")" : " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64, 57)"
                )
                + " AND STATE = 0"
            + " GROUP BY TERMINAL_ID, TO_CHAR(COUNT_DATE, 'YYYYMMDD')"    
            + " UNION "      
            + " SELECT TERMINAL_ID,  COUNT_DATE "  //B, C
            + " FROM TB_SALESCOUNT A"
            + " WHERE " + TERMINAL_WHERE
                + " AND COL_NO = 0"
                + (company==krr?
                		" AND TRIM(GOODS_CODE) <> '000000'":"" 
                )
                + " AND COUNT_MODE <> 'A'"
                + " AND COUNT_DATE >= TO_DATE('" + sDate + "' || '000000', 'YYYYMMDDHH24MISS')"  
                + " AND COUNT_DATE < TO_DATE('" + eDate + "' || '235959', 'YYYYMMDDHH24MISS') + 1"
                //+ " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64)"
                + (company==krr? 
    	                " AND ("
    	                	+ " (COL_COUNT <= 24 AND   (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1)) "
    	                    + " OR  (COL_COUNT > 24 AND (SELECT MAX(COL_NO) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) = (COL_COUNT )) "
    	                + ")" : " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64, 57)"
                    )
                + " AND STATE = 0"
            + " GROUP BY TERMINAL_ID, COUNT_DATE "
            + " UNION "
            + " SELECT TERMINAL_ID,  MAX(COUNT_DATE) AS COUNT_DATE " //이전 마지막 마감
            + " FROM TB_SALESCOUNT A"
            + " WHERE " + TERMINAL_WHERE
                + " AND COL_NO = 0"
                + (company==krr?
                		" AND TRIM(GOODS_CODE) <> '000000'":"" 
                )
                + " AND COUNT_DATE < TO_DATE('" + sDate + "' || '000000', 'YYYYMMDDHH24MISS')" 
                //+ " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64)"
                + (company==krr? 
    	                " AND ("
    	                	+ " (COL_COUNT <= 24 AND   (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1)) "
    	                    + " OR  (COL_COUNT > 24 AND (SELECT MAX(COL_NO) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) = (COL_COUNT )) "
    	                + ")" : " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64, 57)"
                    )
                + " AND STATE = 0"
            + " GROUP BY TERMINAL_ID"
            ;
		
		
		// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();
			
		try {
			
			long[] grandTotalCounts = { 0, 0, 0, 0 };
			long[] grandTotalAmounts = { 0, 0, 0, 0 };
		
			ps = dbLib.prepareStatement(conn,
			   "SELECT /*+ ORDERED USE_HASH(B C D) */"
					+ " A.*, "
					+ " (SELECT /*+ INDEX(TB_ORGANIZATION) */"
							+ " NAME"
						+ " FROM TB_ORGANIZATION"
						+ " WHERE PARENT_SEQ = " + (organ > 0 ? organ : 0)
							+ " AND SORT = 1"
							+ " AND SEQ <> A.ORGANIZATION_SEQ"
						+ " START WITH SEQ = A.ORGANIZATION_SEQ"
						+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
					+ " ) AS PARENT_ORGAN,"	
					+ " B.NAME AS ORGAN,"
					+ " C.PLACE AS PLACE,"
					+ " D.CODE AS VM_CODE"
			   + " FROM ( "
				    + "SELECT "
				        + " COUNT_DAY," 
				        + " COMPANY_SEQ,"
				        + " ORGANIZATION_SEQ,"
				        + " VM_PLACE_SEQ,"
				        + " TERMINAL_ID," 				        
				        + " NUM,"
				        + " COUNT_DATE_START AS START_DATE,"
				        + " COUNT_DATE_END AS END_DATE,"
				        + " TOTAL_COUNT,"
				        + " TOTAL_AMOUNT,"
				        + " COUNT(CASE WHEN PAY_TYPE = '01' THEN 1 END)  AS CNT_CARD,"
				        + " NVL(SUM(CASE WHEN PAY_TYPE = '01' THEN AMOUNT ELSE 0 END), 0) AS AMOUNT_CARD,"
				        + " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END)  AS CNT_SMTPAY,"
				        + " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN AMOUNT ELSE 0 END), 0) AS AMOUNT_SMTPAY,"
				        + SUM_OF_EACH_PREPAY_COMPANY
				        + " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END)  AS CNT_PREPAY,"
				        + " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN AMOUNT ELSE 0 END), 0) AS AMOUNT_PREPAY "
				    + "FROM (" 
				    	+ "SELECT /*+ INDEX(S)*/" //20181116 수정
				        	+ " C.*,"
				            + " S.COMPANY_SEQ,"
				            + " S.ORGANIZATION_SEQ,"
				            + " S.VM_PLACE_SEQ,"
				            + " S.PAY_TYPE,"
				            + " S.PAY_CARD,"
				            + " S.AMOUNT "
				        + "FROM  (  "    
				            + "SELECT "
				                + " COUNT_DAY,"
				                + " TERMINAL_ID," 
				                + " NUM,"
				                + " TO_CHAR(COUNT_DATE_START, 'YYYYMMDDHH24MISS') AS COUNT_DATE_START,"
				                + " TO_CHAR(COUNT_DATE_END, 'YYYYMMDDHH24MISS') AS COUNT_DATE_END,"
				                + " TOTAL_COUNT - TOTAL_COUNT_PREV AS TOTAL_COUNT,"
				                + " TOTAL_AMOUNT - TOTAL_AMOUNT_PREV  AS TOTAL_AMOUNT"
				            + " FROM ("            
				                + " SELECT "
				                	+ " TO_CHAR(COUNT_DATE, 'YYYYMMDD') AS COUNT_DAY,"
				                    + " TERMINAL_ID,"
				                    + " CASE WHEN NUM > 1 THEN LAG(COUNT_DATE) OVER (ORDER BY TERMINAL_ID, COUNT_DATE, NUM) ELSE TO_DATE('20161013000001', 'YYYYMMDDHH24MISS') END AS COUNT_DATE_START,"
				                    + " COUNT_DATE AS COUNT_DATE_END,"
				                    + " NUM, "
				                    + " CASE WHEN NUM > 1 THEN LAG(TOTAL_COUNT) OVER (ORDER BY TERMINAL_ID, COUNT_DATE, NUM) ELSE NULL END TOTAL_COUNT_PREV," 
				                    + " TOTAL_COUNT,"
				                    + " CASE WHEN NUM > 1 THEN LAG(TOTAL_AMOUNT) OVER (ORDER BY TERMINAL_ID, COUNT_DATE, NUM) ELSE NULL END TOTAL_AMOUNT_PREV," 
				                    + " TOTAL_AMOUNT "
				                + " FROM ( "   
				                    + " SELECT " 
				                        + " TERMINAL_ID,"
				                        + " COUNT_DATE,"
				                        + " ROW_NUMBER() OVER (PARTITION BY TERMINAL_ID ORDER BY TERMINAL_ID, MAX(COUNT_DATE)) AS NUM,"
				                        + " SUM(TOTAL_COUNT) AS TOTAL_COUNT,"
				                        + " SUM(TOTAL_AMOUNT) AS TOTAL_AMOUNT " 
				                    + " FROM ( "
				                        + " SELECT "
				                            + " A.TERMINAL_ID, A.COUNT_DATE, B.MODEL, COL_NO,"
				                            + " TOTAL_COUNT,"
				                            + " TOTAL_AMOUNT "
				                        + " FROM  TB_SALESCOUNT A "
				                            + " INNER JOIN TB_VENDING_MACHINE B"
				                                + " ON A.TERMINAL_ID = B.TERMINAL_ID "
			                                	+ " AND ((B.MODEL IN ('LVM-6112') AND COL_NO NOT IN (0, 12, 13)) "
			                                		+ " OR (B.MODEL IN ('R-6107') AND COL_NO NOT IN (0, 11, 12)) "
			                                		+ " OR (B.MODEL IN ('CVK-6024','LVM-6141') AND COL_NO NOT IN (0, 13, 14)) "
			                                		+ " OR (B.MODEL NOT IN ('LVM-6112', 'R-6107', 'CVK-6024','LVM-6141') AND COL_NO NOT IN (0))) "
				                        + " WHERE (A.TERMINAL_ID , COUNT_DATE) IN ("  
				                            + SALESCOUNT_RANGE
	                                        + " ) "
				                        + " ) "
				                    + " GROUP BY TERMINAL_ID, COUNT_DATE"
				                    + " ORDER BY TERMINAL_ID, COUNT_DATE"
				                + " ) "  
				            + " ) " 
				            + " ORDER BY COUNT_DATE_END DESC "
	
				        + " ) C "
				            + " LEFT OUTER JOIN TB_SALES S "
				                + " ON C.TERMINAL_ID = S.TERMINAL_ID"
				                	+ " AND S.COMPANY_SEQ = " + company
				                    //+ " AND S.PAY_TYPE  IN ( '01', '11') " //신용, 선불
				                    + " AND S.PAY_STEP NOT IN ('00', '99') " //(망)취소제외
				                    + " AND S.TRANSACTION_DATE || S.TRANSACTION_TIME > C.COUNT_DATE_START "
				                    + " AND S.TRANSACTION_DATE || S.TRANSACTION_TIME <= C.COUNT_DATE_END "
				         + " WHERE NUM > 1 "  
				     + " ) "    
				     + " GROUP BY COUNT_DAY, COMPANY_SEQ, ORGANIZATION_SEQ, VM_PLACE_SEQ, TERMINAL_ID, NUM, COUNT_DATE_START, COUNT_DATE_END, TOTAL_COUNT, TOTAL_AMOUNT"
			     + " ) A"
					+ " INNER JOIN TB_ORGANIZATION B"
					    + " ON A.ORGANIZATION_SEQ = B.SEQ"
				    + " INNER JOIN TB_VENDING_MACHINE_PLACE C"
					    + " ON A.VM_PLACE_SEQ = C.SEQ"
				    + " INNER JOIN TB_VENDING_MACHINE D"
					    + " ON C.VM_SEQ = D.SEQ"
			     + " ORDER BY "   	
			     // 정렬 값(oMode, oType)에 따라 ORDER BY 변경
			     + ((oType == 1) ? ( //DESC
			    		// 거래처
			    		(oMode == 1) ? ( " PARENT_ORGAN DESC, ORGAN DESC, PLACE DESC, A.TERMINAL_ID, A.END_DATE, A.NUM  " ) :
			    		// 설치장소
			    		(oMode == 2) ? ( " PARENT_ORGAN DESC, ORGAN DESC, PLACE DESC, A.TERMINAL_ID, A.END_DATE, A.NUM " ) :
			    		// default : 날짜
			    			 ( "  PARENT_ORGAN, ORGAN, PLACE, A.TERMINAL_ID, A.END_DATE DESC, A.NUM DESC" )
			        ) : ( //ASC
				    		// 거래처
				    		(oMode == 1) ? ( " PARENT_ORGAN , ORGAN , PLACE, A.TERMINAL_ID, A.END_DATE, A.NUM  " ) :
				    		// 설치장소
				    		(oMode == 2) ? ( " PARENT_ORGAN , ORGAN , PLACE, A.TERMINAL_ID , A.END_DATE, A.NUM " ) :
				    		// default : 날짜
				    	    ( "PARENT_ORGAN, ORGAN, PLACE, A.TERMINAL_ID,  A.END_DATE , A.NUM " )
			            ) 
			    	)
			     //+ ", A.TERMINAL_ID "
			);	
			rs = ps.executeQuery();

			int n = 0;
			GeneralConfig c = null;
			String startDate = null;
			String endDate = null;
			
			while (rs.next()) {

				c = new GeneralConfig();
				
				n = 0;

				String yyyymmdd = rs.getString("COUNT_DAY");
				c.put("YYYYMMDD", yyyymmdd);
				c.put("DATE", yyyymmdd.substring(0, 4) + "-" + yyyymmdd.substring(4, 6) + "-" + yyyymmdd.substring(6, 8));

				c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("VM_PLACE_SEQ", rs.getLong("VM_PLACE_SEQ"));
				c.put("PARENT_ORGAN", rs.getString("PARENT_ORGAN"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("PLACE", rs.getString("PLACE"));		
	
				c.put("VM_CODE", rs.getString("VM_CODE"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));

				startDate = null;
				endDate = null;	
				
				
				if ((n == 0) || !c.get("START_DATE_" + n).equals("-")) n++;
				c.put("PAY_COUNT", n);
				
				String date = rs.getString("START_DATE");
				c.put("START_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
				);
				if ((startDate == null) || (startDate.compareTo(date) > 0)) {
					if (startDate != null) System.out.println("START_DATE : " + startDate + "-->" + date);
					c.put("START_DATE", c.get("START_DATE_" + n));
					startDate = date;
				}

				date = rs.getString("END_DATE");
				c.put("END_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
				);
				if ((endDate == null) || (endDate.compareTo(date) < 0)) {
					if (endDate != null) System.out.println("END_DATE : " + endDate + "-->" + date);
					c.put("END_DATE", c.get("END_DATE_" + n));
					endDate = date;
				}
				
				//전체
				long totalCount = rs.getLong("TOTAL_COUNT");
				long totalAmount = rs.getLong("TOTAL_AMOUNT");
				c.put("CNT_TOTAL", c.getLong("CNT_TOTAL") + totalCount);
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_TOTAL") + totalAmount);
				
				//선불
				long countPrepay =  rs.getLong("CNT_PREPAY");
				long amountPrepay = rs.getLong("AMOUNT_PREPAY");
				c.put("CNT_PREPAY", c.getLong("CNT_PREPAY") + countPrepay);
				c.put("AMOUNT_PREPAY", c.getLong("AMOUNT_PREPAY") + amountPrepay);
				grandTotalCounts[2] += countPrepay;
				grandTotalAmounts[2] += amountPrepay;
				
				//개별선불 
				for (String columnName : prepaySummaryColumns) {
					c.put(columnName + "_" + n, rs.getLong(columnName));
					c.put(columnName, c.getLong(columnName) + rs.getLong(columnName));
					this.data.put(columnName, this.data.getLong(columnName) + rs.getLong(columnName));
				}
				
				//신용
				long countCard =  rs.getLong("CNT_CARD");
				long amountCard = rs.getLong("AMOUNT_CARD");
				c.put("CNT_CARD", c.getLong("CNT_CARD") + countCard);
				c.put("AMOUNT_CARD", c.getLong("AMOUNT_CARD") + amountCard);
				grandTotalCounts[1] += countCard;
				grandTotalAmounts[1] += amountCard;
				
				//간편결제
				long countSmtpay =  rs.getLong("CNT_SMTPAY");
				long amountSmtpay = rs.getLong("AMOUNT_SMTPAY");
				c.put("CNT_SMTPAY", c.getLong("CNT_SMTPAY") + countSmtpay);
				c.put("AMOUNT_SMTPAY", c.getLong("AMOUNT_SMTPAY") + amountSmtpay);
				grandTotalCounts[3] += countSmtpay;
				grandTotalAmounts[3] += amountSmtpay;
				
				//현금
				long countCash = totalCount - countPrepay - countCard;
				long amountCash = totalAmount - amountPrepay - amountCard;
				c.put("CNT_CASH", c.getLong("CNT_CASH") + countCash);
				c.put("AMOUNT_CASH", c.getLong("AMOUNT_CASH") + amountCash);
				grandTotalCounts[0] += countCash;
				grandTotalAmounts[0] += amountCash;
				
				this.list.add(c);
				
			}
			this.data.put("CNT_CASH", grandTotalCounts[0]);
			this.data.put("AMOUNT_CASH", grandTotalAmounts[0]);
			
			this.data.put("CNT_CARD", grandTotalCounts[1]);
			this.data.put("AMOUNT_CARD", grandTotalAmounts[1]);
			
			this.data.put("CNT_PREPAY", grandTotalCounts[2]);
			this.data.put("AMOUNT_PREPAY", grandTotalAmounts[2]);
			
			this.data.put("CNT_SMTPAY", grandTotalCounts[3]);
			this.data.put("AMOUNT_SMTPAY", grandTotalAmounts[3]);
			
			this.data.put("CNT_TOTAL", grandTotalCounts[0] + grandTotalCounts[1] + grandTotalCounts[2] + grandTotalCounts[3]);
			this.data.put("AMOUNT_TOTAL", grandTotalAmounts[0] + grandTotalAmounts[1] + grandTotalAmounts[2] + grandTotalAmounts[3]);
			
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}
		
		// 조직명
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
						+ " FROM ("
								+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
										+ " NAME"
									+ " FROM TB_ORGANIZATION"
									+ " WHERE COMPANY_SEQ = " + company
										+ " AND SORT = 0"
									+ " ORDER BY DEPTH DESC"
							+ " )"
						+ " WHERE ROWNUM = 1"
					)
			);
		String sDesc = null;
		// 검색 설정
		//sDesc = "집계기준="
		//		+ dbLib.getResult(conn,
		//				"SELECT /*+ INDEX(TB_CODE) */"
		//						+ " NAME"
		//					+ " FROM TB_CODE"
		//					+ " WHERE TYPE = 'SUM_TYPE'"
		//						+ " AND CODE = '" + sType + "'"
		//			)
		//		+ "&집계기간=" + sDate + "-" + eDate
		//	;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}

		this.data.put("sDesc", sDesc);
		
		// 리소스 반환
		dbLib.close(conn);
		
		return error;
		
	}
	/**
	 * 판매계수
	 *
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 검색일
	 * @param eDate 종료일
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String salesCountListNew(long company, long organ, long depth, long place, String sDate, String eDate, int oMode, int oType) throws Exception {
	// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
			return null;
		}

	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String error = null;
		long krr = 264L;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			throw new Exception("DB 연결에 실패하였습니다.");
		}
		
		//검색절 생성
		String WHERE = "";
		String TERMINAL_WHERE = "";
		
		company = (company > 0 ? company : this.cfg.getLong("user.company"));
		if (company > 0) { // 소속
			TERMINAL_WHERE += " TERMINAL_ID IN ("
					+ "SELECT /*+ INDEX(TB_VENDING_MACHINE) */"
					+ " TERMINAL_ID"
				+ " FROM TB_VENDING_MACHINE "
				+ " WHERE COMPANY_SEQ = " + company ;
			
			if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자는 등록된 자판기만 볼 수 있음
				TERMINAL_WHERE += " AND USER_SEQ = " + this.cfg.get("user.seq");
			}
		} 
		
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 

			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			if (organ > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
				TERMINAL_WHERE += " AND ORGANIZATION_SEQ IN ("						
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
								+ " START WITH COMPANY_SEQ = " + company
								+ " AND SEQ IN ("
										+ " SELECT " + organ + " FROM DUAL"
										+ " UNION"
										+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
												+ " ORGANIZATION_SEQ"
											+ " FROM TB_USER_APP_ORGAN"
											+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
									+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ ")"
				;
			} 
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
		
			//organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ") : organ);
			organ = (organ <= 0 ? this.cfg.getLong("user.organ") : (this.cfg.getInt("user.organ.depth") > depth ? this.cfg.getLong("user.organ") : organ));
			if (organ > 0) { // 조직
				TERMINAL_WHERE +=  " AND ORGANIZATION_SEQ IN ("						
								+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
										+ " SEQ"
									+ " FROM TB_ORGANIZATION"
									+ " WHERE SORT = 1"
									+ " START WITH SEQ = " + organ
									+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
							+ ")"
					;
			} 
		}
		
		if (place > 0) {// 위치
			TERMINAL_WHERE += " AND TERMINAL_ID = ("
					+ " SELECT /*+ INDEX(TB_VENDING_MACHINE) */"
						+ " TERMINAL_ID"
					+ " FROM TB_VENDING_MACHINE"
					+ " WHERE SEQ = ("
						+ "SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
							+ " VM_SEQ"
						+ " FROM TB_VENDING_MACHINE_PLACE"
						+ " WHERE SEQ =" + place
					+ ")"
			    + ")"
			;	
		}
		
		//if (!TERMINAL_WHERE.isEmpty())
		TERMINAL_WHERE += " ) ";
		
		String SUM_OF_EACH_PREPAY_COMPANY = "";
		String[] prepaySummaryColumns = new String[] {};
		ArrayList<String> prepaySummaryColumnList = new ArrayList<String>();		
		
		// 선불 카드사
		this.company = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(TB_CODE) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE"
						+ " WHERE TYPE = 'PAY_CARD'"
							+ " AND CODE <> '000'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				String code = rs.getString("CODE");
				c.put("CODE", code);
				c.put("NAME", rs.getString("NAME"));

				this.company.add(c);

				SUM_OF_EACH_PREPAY_COMPANY += "SUM(VMMS_" + code + "_COUNT) AS CNT_PREPAY_" + code + ","
						+ " SUM(VMMS_" + code + "_AMOUNT) AS AMOUNT_PREPAY_" + code + ","
					;
				prepaySummaryColumnList.add("CNT_PREPAY_" + code);
				prepaySummaryColumnList.add("AMOUNT_PREPAY_" + code);
			}

			prepaySummaryColumns = prepaySummaryColumnList.toArray(new String[prepaySummaryColumnList.size()]);
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			prepaySummaryColumnList = null;

			dbLib.close(rs);
			dbLib.close(ps);
		}		
		
		// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}		
		
		// 목록 가져오기
		//salesCountListNew
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();
			
		try {
			long[] grandTotalCounts = { 0, 0, 0, 0 };//scheo 
			long[] grandTotalAmounts = { 0, 0, 0, 0 };
		//scheo 판매계수집계
			ps = dbLib.prepareStatement(conn,
				"SELECT /*+ ORDERED USE_HASH(B C) */ "
					+ " A.*, " 
				    + " (SELECT /*+ INDEX(TB_ORGANIZATION) */ "
				    	+ " NAME "
				    + " FROM TB_ORGANIZATION "
				    + " WHERE PARENT_SEQ = " + (organ > 0 ? organ : 0)
				    	+ " AND SORT = 1 "
				    	+ " AND SEQ <> A.ORGANIZATION_SEQ "
				    	+ " START WITH SEQ = A.ORGANIZATION_SEQ "
				    	+ " CONNECT BY SEQ = PRIOR PARENT_SEQ "
				    + " ) AS PARENT_ORGAN," 
				    + " B.NAME AS ORGAN,"
				    + " C.PLACE AS PLACE," 
				    + " C.SEQ AS VM_PLACE_SEQ" 
				 + " FROM (" 
				    + " SELECT /*+ ORDERED USE_HASH(C S) */ "
				        + " C.*," 
				        + " D.TOTAL_PRICE," //scheo
				        + " S.COMPANY_SEQ," 
				        + " S.ORGANIZATION_SEQ," 
				        + " S.SEQ AS VM_SEQ," 
				        + " S.CODE AS VM_CODE" 
				    + " FROM "
				    + " ("     //scheo 여기부터
				    + " SELECT" 
			            + " SUM(PRICE) - SUM(UNIT_PRICE) TOTAL_PRICE,"
			            + " TERMINAL_ID,"
			            + " COUNT_DAY"
		            + " FROM TB_SALESCOUNT_DETAIL "
			        + " WHERE "
			            + TERMINAL_WHERE
			            + (company==krr?" AND PLACE_CODE <> '000000'":"")
			            + " AND COUNT_DAY >= '" + sDate + "'"
			            + " AND COUNT_DAY <= '" + eDate + "'"
			            + " AND UNIT_PRICE <> 0"
			        + " GROUP BY TERMINAL_ID, COUNT_DAY "    
			        + " ) D," //scheo 여기까지
				    + " ("   
				        + " SELECT" 
				            + " COUNT_DAY," 
				            + " TERMINAL_ID," 
				            + " MAX(PLACE_CODE) PLACE_CODE,"
				            + " TO_CHAR(MIN(COUNT_DATE_PREV), 'YYYYMMDDHH24MISS') START_DATE," 
				            + " TO_CHAR(MAX(COUNT_DATE), 'YYYYMMDDHH24MISS') END_DATE," 
				            + " SUM(TOTAL_COUNT) TOTAL_COUNT," 
				            + " SUM(TOTAL_AMOUNT) TOTAL_AMOUNT," 
				            + " SUM(TOTAL_COUNT) - SUM(VMMS_CARD_COUNT) CNT_CASH," 
				            + " SUM(TOTAL_AMOUNT) - SUM(VMMS_CARD_AMOUNT) AMOUNT_CASH," 
				            + " SUM(VMMS_CRDT_COUNT) CNT_CARD," 
				            + " SUM(VMMS_CRDT_AMOUNT) AMOUNT_CARD," 
					        //+ " SUM(VMMS_NPC_COUNT) CNT_SMTPAY," //scheo 
	                        //+ " SUM(VMMS_NPC_AMOUNT) AMOUNT_SMTPAY,"
	                        + " SUM(VMMS_NPC_COUNT) + SUM(VMMS_KKO_COUNT) CNT_SMTPAY,"         //카카오페이 추가
	                        + " SUM(VMMS_NPC_AMOUNT) + SUM(VMMS_KKO_AMOUNT) AMOUNT_SMTPAY," //카카오페이 추가
				            + SUM_OF_EACH_PREPAY_COMPANY
				            + " MAX(MEMO) " 
				        + " FROM TB_SALESCOUNT_DETAIL "
				        + " WHERE "
				            + TERMINAL_WHERE
				            + (company==krr?" AND PLACE_CODE <> '000000'":"")
				            + " AND COL_NO = 0" 
				            + " AND COUNT_DAY >= '" + sDate + "'"
				            + " AND COUNT_DAY <= '" + eDate + "'"
				        + " GROUP BY COUNT_DAY, TERMINAL_ID" 
				       + "  ORDER BY COUNT_DAY DESC, TERMINAL_ID"
				    + " ) C"
				        + " INNER JOIN TB_VENDING_MACHINE S ON C.TERMINAL_ID = S.TERMINAL_ID AND S.COMPANY_SEQ = " + company
				        + " WHERE C.TERMINAL_ID = D.TERMINAL_ID and C.COUNT_DAY = D.COUNT_DAY "	//scheo 
				+ " ) A" 
				    + " INNER JOIN TB_ORGANIZATION B ON A.ORGANIZATION_SEQ = B.SEQ" 
				    + " INNER JOIN TB_VENDING_MACHINE_PLACE C ON A.VM_SEQ = C.VM_SEQ AND C.END_DATE IS NULL "
			    + " ORDER BY "   	
			     // 정렬 값(oMode, oType)에 따라 ORDER BY 변경
			     + ((oType == 1) ? ( //DESC
			    		// 거래처
			    		(oMode == 1) ? ( " PARENT_ORGAN DESC, ORGAN DESC, PLACE DESC, A.TERMINAL_ID, A.END_DATE " ) :
			    		// 설치장소
			    		(oMode == 2) ? ( " PARENT_ORGAN DESC, ORGAN DESC, PLACE DESC, A.TERMINAL_ID, A.END_DATE" ) :
			    		// default : 날짜
			    			 ( "  PARENT_ORGAN, ORGAN, PLACE, A.TERMINAL_ID, A.END_DATE DESC" )
			        ) : ( //ASC
				    		// 거래처
				    		(oMode == 1) ? ( " PARENT_ORGAN , ORGAN , PLACE, A.TERMINAL_ID, A.END_DATE " ) :
				    		// 설치장소
				    		(oMode == 2) ? ( " PARENT_ORGAN , ORGAN , PLACE, A.TERMINAL_ID , A.END_DATE " ) :
				    		// default : 날짜
				    	    ( "PARENT_ORGAN, ORGAN, PLACE, A.TERMINAL_ID,  A.END_DATE " )
			            ) 
			    	)    				
				);
			
			
			rs = ps.executeQuery();

			int n = 0;
			GeneralConfig c = null;
			String startDate = null;
			String endDate = null;
			
			while (rs.next()) {

				c = new GeneralConfig();
				
				n = 0;

				String yyyymmdd = rs.getString("COUNT_DAY");
				c.put("YYYYMMDD", yyyymmdd);
				c.put("DATE", yyyymmdd.substring(0, 4) + "-" + yyyymmdd.substring(4, 6) + "-" + yyyymmdd.substring(6, 8));

				c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("VM_PLACE_SEQ", rs.getLong("VM_PLACE_SEQ"));
				c.put("PARENT_ORGAN", rs.getString("PARENT_ORGAN"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("PLACE", rs.getString("PLACE"));
				
				c.put("TOTAL_PRICE", rs.getString("TOTAL_PRICE")); //scheo
	
				c.put("PLACE_CODE", rs.getString("PLACE_CODE"));
				c.put("VM_CODE", rs.getString("VM_CODE"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));

				startDate = null;
				endDate = null;	
				
				
				if ((n == 0) || !c.get("START_DATE_" + n).equals("-")) n++;
				c.put("PAY_COUNT", n);
				
				String date = rs.getString("START_DATE");
				c.put("START_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
				);
				if ((startDate == null) || (startDate.compareTo(date) > 0)) {
					if (startDate != null) System.out.println("START_DATE : " + startDate + "-->" + date);
					c.put("START_DATE", c.get("START_DATE_" + n));
					startDate = date;
				}

				date = rs.getString("END_DATE");
				c.put("END_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
				);
				if ((endDate == null) || (endDate.compareTo(date) < 0)) {
					if (endDate != null) System.out.println("END_DATE : " + endDate + "-->" + date);
					c.put("END_DATE", c.get("END_DATE_" + n));
					endDate = date;
				}
				
				//전체
				long totalCount = rs.getLong("TOTAL_COUNT");
				long totalAmount = rs.getLong("TOTAL_AMOUNT");
				c.put("CNT_TOTAL", c.getLong("CNT_TOTAL") + totalCount);
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_TOTAL") + totalAmount);
				grandTotalCounts[2] += totalCount;
				grandTotalAmounts[2] += totalAmount;
				
				//개별선불 
				for (String columnName : prepaySummaryColumns) {
					c.put(columnName + "_" + n, rs.getLong(columnName));
					c.put(columnName, c.getLong(columnName) + rs.getLong(columnName));
					this.data.put(columnName, this.data.getLong(columnName) + rs.getLong(columnName));
				}
				
				//신용
				long countCard =  rs.getLong("CNT_CARD");
				long amountCard = rs.getLong("AMOUNT_CARD");
				c.put("CNT_CARD", c.getLong("CNT_CARD") + countCard);
				c.put("AMOUNT_CARD", c.getLong("AMOUNT_CARD") + amountCard);
				grandTotalCounts[1] += countCard;
				grandTotalAmounts[1] += amountCard;
				
				//간편결제 scheo
				long countSmtpay=  rs.getLong("CNT_SMTPAY");
				long amountSmtpay = rs.getLong("AMOUNT_SMTPAY");
				c.put("CNT_SMTPAY", c.getLong("CNT_SMTPAY") + countSmtpay);
				c.put("AMOUNT_SMTPAY", c.getLong("AMOUNT_SMTPAY") + amountSmtpay);
				grandTotalCounts[3] += countSmtpay;
				grandTotalAmounts[3] += amountSmtpay;
				
				//현금
				long countCash = rs.getLong("CNT_CASH");
				long amountCash = rs.getLong("AMOUNT_CASH");
				c.put("CNT_CASH", c.getLong("CNT_CASH") + countCash);
				c.put("AMOUNT_CASH", c.getLong("AMOUNT_CASH") + amountCash);
				grandTotalCounts[0] += countCash;
				grandTotalAmounts[0] += amountCash;
				
				this.list.add(c);
				
			}
			
			this.data.put("CNT_CASH", grandTotalCounts[0]);
			this.data.put("AMOUNT_CASH", grandTotalAmounts[0]);
			
			this.data.put("CNT_CARD", grandTotalCounts[1]);
			this.data.put("AMOUNT_CARD", grandTotalAmounts[1]);
			
			this.data.put("CNT_SMTPAY", grandTotalCounts[3]);
			this.data.put("AMOUNT_SMTPAY", grandTotalAmounts[3]);
			
			this.data.put("CNT_TOTAL", grandTotalCounts[2]);
			this.data.put("AMOUNT_TOTAL", grandTotalAmounts[2]);
			
			
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}
		
		// 조직명
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
						+ " FROM ("
								+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
										+ " NAME"
									+ " FROM TB_ORGANIZATION"
									+ " WHERE COMPANY_SEQ = " + company
										+ " AND SORT = 0"
									+ " ORDER BY DEPTH DESC"
							+ " )"
						+ " WHERE ROWNUM = 1"
					)
			);
		String sDesc = null;
		// 검색 설정
		//sDesc = "집계기준="
		//		+ dbLib.getResult(conn,
		//				"SELECT /*+ INDEX(TB_CODE) */"
		//						+ " NAME"
		//					+ " FROM TB_CODE"
		//					+ " WHERE TYPE = 'SUM_TYPE'"
		//						+ " AND CODE = '" + sType + "'"
		//			)
		//		+ "&집계기간=" + sDate + "-" + eDate
		//	;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}

		this.data.put("sDesc", sDesc);
		
		// 리소스 반환
		dbLib.close(conn);
		
		return error;
		
	}
	/**
	 * 판매계수
	 *
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 검색일
	 * @param eDate 종료일
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String salesCountRawList(long company, long organ, long depth, long place, String sDate, String eDate, int oMode, int oType) throws Exception {
	// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
			return null;
		}

	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String error = null;
		long krr = 264L;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			throw new Exception("DB 연결에 실패하였습니다.");
		}
		
		//검색절 생성
		String WHERE = "";
		String TERMINAL_WHERE = "";
		String SALESCOUNT_RANGE = "";
		
		company = (company > 0 ? company : this.cfg.getLong("user.company"));
		if (company > 0) { // 소속
			TERMINAL_WHERE += " A.TERMINAL_ID IN ("
					+ "SELECT /*+ INDEX(TB_VENDING_MACHINE) */"
					+ " TERMINAL_ID"
				+ " FROM TB_VENDING_MACHINE "
				+ " WHERE COMPANY_SEQ = " + company
			;
		} 
		
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 

			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			if (organ > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
				TERMINAL_WHERE += " AND ORGANIZATION_SEQ IN ("						
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
								+ " START WITH COMPANY_SEQ = " + company
								+ " AND SEQ IN ("
										+ " SELECT " + organ + " FROM DUAL"
										+ " UNION"
										+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
												+ " ORGANIZATION_SEQ"
											+ " FROM TB_USER_APP_ORGAN"
											+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
									+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ ")"
				;
			} 
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
		
			//organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ") : organ);
			organ = (organ <= 0 ? this.cfg.getLong("user.organ") : (this.cfg.getInt("user.organ.depth") > depth ? this.cfg.getLong("user.organ") : organ));
			if (organ > 0) { // 조직
				TERMINAL_WHERE +=  " AND ORGANIZATION_SEQ IN ("						
								+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
										+ " SEQ"
									+ " FROM TB_ORGANIZATION"
									+ " WHERE SORT = 1"
									+ " START WITH SEQ = " + organ
									+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
							+ ")"
					;
			} 
		}
		
		if (place > 0) {// 위치
			TERMINAL_WHERE += " AND A.TERMINAL_ID = ("
					+ " SELECT /*+ INDEX(TB_VENDING_MACHINE) */"
						+ " TERMINAL_ID"
					+ " FROM TB_VENDING_MACHINE"
					+ " WHERE SEQ = ("
						+ "SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
							+ " VM_SEQ"
						+ " FROM TB_VENDING_MACHINE_PLACE"
						+ " WHERE SEQ =" + place
					+ ")"
			    + ")"
			;	
		}
		
		//if (!TERMINAL_WHERE.isEmpty())
		TERMINAL_WHERE += " ) ";
		
		// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}		
		

		SALESCOUNT_RANGE += " SELECT TERMINAL_ID, MAX(COUNT_DATE) AS COUNT_DATE " //당일 마지막 마감
            + " FROM TB_SALESCOUNT A "
            + " WHERE " + TERMINAL_WHERE
                + " AND COL_NO = 0" 
                + " AND COUNT_MODE = 'A'"
                + (company==krr?
                		" AND TRIM(GOODS_CODE) <> '000000'":"" 
                )
                + " AND COUNT_DATE >= TO_DATE('" + sDate + "' || '000000', 'YYYYMMDDHH24MISS')" 
                + " AND COUNT_DATE < TO_DATE('" + eDate + "' || '000000', 'YYYYMMDDHH24MISS') + 1"
                //+ " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64, 57)"
                + (company==krr? 
	                " AND ("
	                	+ " (COL_COUNT <= 24 AND   (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1) "
	                    + " OR  (COL_COUNT > 24 AND (SELECT MAX(COL_NO) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) = (COL_COUNT ))) "
	                + ")" : " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64, 57)"
                )
                + " AND STATE = 0"
            + " GROUP BY TERMINAL_ID, TO_CHAR(COUNT_DATE, 'YYYYMMDD')"    
            + " UNION "      
            + " SELECT TERMINAL_ID,  COUNT_DATE "  //B, C
            + " FROM TB_SALESCOUNT A"
            + " WHERE " + TERMINAL_WHERE
                + " AND COL_NO = 0"
                + (company==krr?
                		" AND TRIM(GOODS_CODE) <> '000000'":"" 
                )
                + " AND COUNT_MODE <> 'A'"
                + " AND COUNT_DATE >= TO_DATE('" + sDate + "' || '000000', 'YYYYMMDDHH24MISS')"  
                + " AND COUNT_DATE < TO_DATE('" + eDate + "' || '235959', 'YYYYMMDDHH24MISS') + 1"
                //+ " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64, 57)"
                + (company==krr? 
	                " AND ("
	                	+ " (COL_COUNT <= 24 AND   (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1) "
	                    + " OR  (COL_COUNT > 24 AND (SELECT MAX(COL_NO) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) = (COL_COUNT ))) "
	                + ")" : " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64, 57)"
                )
                + " AND STATE = 0"
            + " GROUP BY TERMINAL_ID, COUNT_DATE "
            + " UNION "
            + " SELECT TERMINAL_ID,  MAX(COUNT_DATE) AS COUNT_DATE " //이전 마지막 마감
            + " FROM TB_SALESCOUNT A"
            + " WHERE " + TERMINAL_WHERE
                + " AND COL_NO = 0"
                + (company==krr?
                		" AND TRIM(GOODS_CODE) <> '000000'":"" 
                )
                + " AND COUNT_DATE < TO_DATE('" + sDate + "' || '000000', 'YYYYMMDDHH24MISS')" 
                //+ " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64, 57)"
                + (company==krr? 
    	                " AND ("
    	                	+ " (COL_COUNT <= 24 AND   (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1) "
    	                    + " OR  (COL_COUNT > 24 AND (SELECT MAX(COL_NO) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) = (COL_COUNT ))) "
    	                + ")" : " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64, 57)"
                    )
                + " AND STATE = 0"
            + " GROUP BY TERMINAL_ID"
            ;
		
		
		// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();
			
		try {
			
			long[] grandTotalCounts = { 0, 0 };
			long[] grandTotalAmounts = { 0, 0 };
		
			ps = dbLib.prepareStatement(conn,
			   "SELECT /*+ ORDERED USE_HASH(B C D) */"
					+ " A.*, "
					+ " (SELECT /*+ INDEX(TB_ORGANIZATION) */"
							+ " NAME"
						+ " FROM TB_ORGANIZATION"
						+ " WHERE PARENT_SEQ = " + (organ > 0 ? organ : 0)
							+ " AND SORT = 1"
							+ " AND SEQ <> A.ORGANIZATION_SEQ"
						+ " START WITH SEQ = A.ORGANIZATION_SEQ"
						+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
					+ " ) AS PARENT_ORGAN,"	
					+ " B.NAME AS ORGAN,"
					+ " C.PLACE AS PLACE,"
					+ " C.SEQ AS VM_PLACE_SEQ"
			   + " FROM ( "
			        + "SELECT /*+ ORDERED USE_HASH(C S) */"
			        	+ " C.*,"
			            + " S.COMPANY_SEQ,"
			            + " S.ORGANIZATION_SEQ,"
			            + " S.SEQ AS VM_SEQ,"
			            + " S.CODE AS VM_CODE"
			        + " FROM  (  "    
			            + "SELECT "
			                + " COUNT_DAY,"
			                + " TERMINAL_ID," 
			                + " NUM,"
			                + " TO_CHAR(COUNT_DATE_START, 'YYYYMMDDHH24MISS') AS START_DATE,"
			                + " TO_CHAR(COUNT_DATE_END, 'YYYYMMDDHH24MISS') AS END_DATE,"
			                + " TOTAL_COUNT - TOTAL_COUNT_PREV AS TOTAL_COUNT,"
			                + " TOTAL_AMOUNT - TOTAL_AMOUNT_PREV  AS TOTAL_AMOUNT,"
			                + " CASH_COUNT - CASH_COUNT_PREV AS CASH_COUNT,"
			                + " CASH_AMOUNT - CASH_AMOUNT_PREV  AS CASH_AMOUNT,"
			                + " CARD_COUNT - CARD_COUNT_PREV AS CARD_COUNT,"
			                + " CARD_AMOUNT - CARD_AMOUNT_PREV  AS CARD_AMOUNT"
			            + " FROM ("            
			                + " SELECT "
			                	+ " TO_CHAR(COUNT_DATE, 'YYYYMMDD') AS COUNT_DAY,"
			                    + " TERMINAL_ID,"
			                    + " CASE WHEN NUM > 1 THEN LAG(COUNT_DATE) OVER (ORDER BY TERMINAL_ID, COUNT_DATE, NUM) ELSE TO_DATE('20161013000001', 'YYYYMMDDHH24MISS') END AS COUNT_DATE_START,"
			                    + " COUNT_DATE AS COUNT_DATE_END,"
			                    + " NUM, "
			                    + " CASE WHEN NUM > 1 THEN LAG(TOTAL_COUNT) OVER (ORDER BY TERMINAL_ID, COUNT_DATE, NUM) ELSE NULL END TOTAL_COUNT_PREV," 
			                    + " TOTAL_COUNT,"
			                    + " CASE WHEN NUM > 1 THEN LAG(TOTAL_AMOUNT) OVER (ORDER BY TERMINAL_ID, COUNT_DATE, NUM) ELSE NULL END TOTAL_AMOUNT_PREV," 
			                    + " TOTAL_AMOUNT, "
			                    + " CASE WHEN NUM > 1 THEN LAG(CASH_COUNT) OVER (ORDER BY TERMINAL_ID, COUNT_DATE, NUM) ELSE NULL END CASH_COUNT_PREV," 
			                    + " CASH_COUNT,"
			                    + " CASE WHEN NUM > 1 THEN LAG(CASH_AMOUNT) OVER (ORDER BY TERMINAL_ID, COUNT_DATE, NUM) ELSE NULL END CASH_AMOUNT_PREV," 
			                    + " CASH_AMOUNT, "
			                    + " CASE WHEN NUM > 1 THEN LAG(CARD_COUNT) OVER (ORDER BY TERMINAL_ID, COUNT_DATE, NUM) ELSE NULL END CARD_COUNT_PREV," 
			                    + " CARD_COUNT,"
			                    + " CASE WHEN NUM > 1 THEN LAG(CARD_AMOUNT) OVER (ORDER BY TERMINAL_ID, COUNT_DATE, NUM) ELSE NULL END CARD_AMOUNT_PREV," 
			                    + " CARD_AMOUNT "
			                + " FROM ( "   
			                    + " SELECT " 
			                        + " TERMINAL_ID,"
			                        + " COUNT_DATE,"
			                        + " ROW_NUMBER() OVER (PARTITION BY TERMINAL_ID ORDER BY TERMINAL_ID, MAX(COUNT_DATE)) AS NUM,"
			                        + " SUM(TOTAL_COUNT) AS TOTAL_COUNT,"
			                        + " SUM(TOTAL_AMOUNT) AS TOTAL_AMOUNT, " 
			                        + " SUM(CASH_COUNT) AS CASH_COUNT,"
			                        + " SUM(CASH_AMOUNT) AS CASH_AMOUNT, " 
			                        + " SUM(CARD_COUNT) AS CARD_COUNT,"
			                        + " SUM(CARD_AMOUNT) AS CARD_AMOUNT "
			                    + " FROM ( "
			                        + " SELECT "
			                            + " A.TERMINAL_ID, A.COUNT_DATE, B.MODEL, COL_NO,"
			                            + " TOTAL_COUNT,"
			                            + " TOTAL_AMOUNT, "
			                            + " CASH_COUNT,"
			                            + " CASH_AMOUNT, "
			                            + " CARD_COUNT,"
			                            + " CARD_AMOUNT "
			                        + " FROM  TB_SALESCOUNT A "
			                            + " INNER JOIN TB_VENDING_MACHINE B"
			                                + " ON A.TERMINAL_ID = B.TERMINAL_ID "
				                                + " AND ((B.MODEL IN ('LVM-6112') AND COL_NO NOT IN (0, 12, 13)) "
				                            		+ " OR (B.MODEL IN ('R-6107') AND COL_NO NOT IN (0, 11, 12)) "
				                            		+ " OR (B.MODEL IN ('CVK-6024','LVM-6141') AND COL_NO NOT IN (0, 13, 14)) "
			                                		+ " OR (B.MODEL NOT IN ('LVM-6112', 'R-6107', 'CVK-6024','LVM-6141') AND COL_NO NOT IN (0))) "
			                        + " WHERE (A.TERMINAL_ID , COUNT_DATE) IN ("  
			                            + SALESCOUNT_RANGE
                                        + " ) "
			                        + " ) "
			                    + " GROUP BY TERMINAL_ID, COUNT_DATE"
			                    + " ORDER BY TERMINAL_ID, COUNT_DATE"
			                + " ) "  
			            + " ) " 
			            + " ORDER BY COUNT_DATE_END DESC "
			        + " ) C "
			        	+ "INNER JOIN TB_VENDING_MACHINE S"
			        		+ " ON C.TERMINAL_ID = S.TERMINAL_ID"
			        		+ " AND S.COMPANY_SEQ = " + company
			         + " WHERE NUM > 1 "   
			         	//+ " AND  C.TOTAL_COUNT > 0 " 
			     + " ) A"
					+ " INNER JOIN TB_ORGANIZATION B"
					    + " ON A.ORGANIZATION_SEQ = B.SEQ"
				    + " INNER JOIN TB_VENDING_MACHINE_PLACE C"
					    + " ON A.VM_SEQ = C.VM_SEQ"
					    	+ " AND C.END_DATE IS NULL"
			     + " ORDER BY "   	
			     // 정렬 값(oMode, oType)에 따라 ORDER BY 변경
			     + ((oType == 1) ? ( //DESC
			    		// 거래처
			    		(oMode == 1) ? ( " PARENT_ORGAN DESC, ORGAN DESC, PLACE DESC, A.TERMINAL_ID, A.END_DATE, A.NUM  " ) :
			    		// 설치장소
			    		(oMode == 2) ? ( " PARENT_ORGAN DESC, ORGAN DESC, PLACE DESC, A.TERMINAL_ID, A.END_DATE, A.NUM " ) :
			    		// default : 날짜
			    			 ( "  PARENT_ORGAN, ORGAN, PLACE, A.TERMINAL_ID, A.END_DATE DESC, A.NUM DESC" )
			        ) : ( //ASC
				    		// 거래처
				    		(oMode == 1) ? ( " PARENT_ORGAN , ORGAN , PLACE, A.TERMINAL_ID, A.END_DATE, A.NUM  " ) :
				    		// 설치장소
				    		(oMode == 2) ? ( " PARENT_ORGAN , ORGAN , PLACE, A.TERMINAL_ID , A.END_DATE, A.NUM " ) :
				    		// default : 날짜
				    	    ( "PARENT_ORGAN, ORGAN, PLACE, A.TERMINAL_ID,  A.END_DATE , A.NUM " )
			            ) 
			    	)
			     //+ ", A.TERMINAL_ID "
			);	
			rs = ps.executeQuery();

			int n = 0;
			GeneralConfig c = null;
			String startDate = null;
			String endDate = null;
			
			while (rs.next()) {

				c = new GeneralConfig();
				
				n = 0;

				String yyyymmdd = rs.getString("COUNT_DAY");
				c.put("YYYYMMDD", yyyymmdd);
				c.put("DATE", yyyymmdd.substring(0, 4) + "-" + yyyymmdd.substring(4, 6) + "-" + yyyymmdd.substring(6, 8));

				c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("VM_PLACE_SEQ", rs.getLong("VM_PLACE_SEQ"));
				c.put("PARENT_ORGAN", rs.getString("PARENT_ORGAN"));
				c.put("ORGAN", rs.getString("ORGAN"));
				
				c.put("PLACE", rs.getString("PLACE"));		
	
				c.put("VM_CODE", rs.getString("VM_CODE"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));

				startDate = null;
				endDate = null;	
				
				
				if ((n == 0) || !c.get("START_DATE_" + n).equals("-")) n++;
				c.put("PAY_COUNT", n);
				
				String date = rs.getString("START_DATE");
				c.put("START_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
				);
				if ((startDate == null) || (startDate.compareTo(date) > 0)) {
					if (startDate != null) System.out.println("START_DATE : " + startDate + "-->" + date);
					c.put("START_DATE", c.get("START_DATE_" + n));
					startDate = date;
				}

				date = rs.getString("END_DATE");
				c.put("END_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
				);
				if ((endDate == null) || (endDate.compareTo(date) < 0)) {
					if (endDate != null) System.out.println("END_DATE : " + endDate + "-->" + date);
					c.put("END_DATE", c.get("END_DATE_" + n));
					endDate = date;
				}
				
				/*
				//전체 TOTAL 이 CASH와 CARD의 합과 다른것 발견하여 수정 20170901 jwhwang
				long totalCount = rs.getLong("TOTAL_COUNT");
				long totalAmount = rs.getLong("TOTAL_AMOUNT");
				c.put("CNT_TOTAL", c.getLong("CNT_TOTAL") + totalCount);
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_TOTAL") + totalAmount);
				*/
				
				//신용
				long countCard =  rs.getLong("CARD_COUNT");
				long amountCard = rs.getLong("CARD_AMOUNT");
				c.put("CNT_CARD", c.getLong("CNT_CARD") + countCard);
				c.put("AMOUNT_CARD", c.getLong("AMOUNT_CARD") + amountCard);
				grandTotalCounts[1] += countCard;
				grandTotalAmounts[1] += amountCard;
				
				//현금
				long countCash =  rs.getLong("CASH_COUNT");
				long amountCash = rs.getLong("CASH_AMOUNT");
				
				c.put("CNT_CASH", c.getLong("CNT_CASH") + countCash);
				c.put("AMOUNT_CASH", c.getLong("AMOUNT_CASH") + amountCash);
				grandTotalCounts[0] += countCash;
				grandTotalAmounts[0] += amountCash;
				
				//전체
				long totalCount = countCard + countCash;
				long totalAmount =amountCard + amountCash ;
				c.put("CNT_TOTAL", c.getLong("CNT_TOTAL") + totalCount);
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_TOTAL") + totalAmount);
				
				this.list.add(c);
				
			}
			this.data.put("CNT_CASH", grandTotalCounts[0]);
			this.data.put("AMOUNT_CASH", grandTotalAmounts[0]);
			
			this.data.put("CNT_CARD", grandTotalCounts[1]);
			this.data.put("AMOUNT_CARD", grandTotalAmounts[1]);
			

			this.data.put("CNT_TOTAL", grandTotalCounts[0] + grandTotalCounts[1] );
			this.data.put("AMOUNT_TOTAL", grandTotalAmounts[0] + grandTotalAmounts[1] );
			
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}
		
		// 조직명
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
						+ " FROM ("
								+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
										+ " NAME"
									+ " FROM TB_ORGANIZATION"
									+ " WHERE COMPANY_SEQ = " + company
										+ " AND SORT = 0"
									+ " ORDER BY DEPTH DESC"
							+ " )"
						+ " WHERE ROWNUM = 1"
					)
			);
		String sDesc = null;
		// 검색 설정
		//sDesc = "집계기준="
		//		+ dbLib.getResult(conn,
		//				"SELECT /*+ INDEX(TB_CODE) */"
		//						+ " NAME"
		//					+ " FROM TB_CODE"
		//					+ " WHERE TYPE = 'SUM_TYPE'"
		//						+ " AND CODE = '" + sType + "'"
		//			)
		//		+ "&집계기간=" + sDate + "-" + eDate
		//	;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}

		this.data.put("sDesc", sDesc);
		
		// 리소스 반환
		dbLib.close(conn);
		
		return error;
		
	}
	
	/**
	 * 판매계수
	 *
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 검색일
	 * @param eDate 종료일
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String salesCountRawListNew(long company, long organ, long depth, long place, String sDate, String eDate, int oMode, int oType) throws Exception {
	// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
			return null;
		}

	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String error = null;
		long krr = 264L;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			throw new Exception("DB 연결에 실패하였습니다.");
		}
		
		//검색절 생성
		String WHERE = "";
		String TERMINAL_WHERE = "";
		String SALESCOUNT_RANGE = "";
		
		company = (company > 0 ? company : this.cfg.getLong("user.company"));
		if (company > 0) { // 소속
			TERMINAL_WHERE += " TERMINAL_ID IN ("
					+ "SELECT /*+ INDEX(TB_VENDING_MACHINE) */"
					+ " TERMINAL_ID"
				+ " FROM TB_VENDING_MACHINE "
				+ " WHERE COMPANY_SEQ = " + company
			;
		} 
		
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 

			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			if (organ > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
				TERMINAL_WHERE += " AND ORGANIZATION_SEQ IN ("						
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
								+ " START WITH COMPANY_SEQ = " + company
								+ " AND SEQ IN ("
										+ " SELECT " + organ + " FROM DUAL"
										+ " UNION"
										+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
												+ " ORGANIZATION_SEQ"
											+ " FROM TB_USER_APP_ORGAN"
											+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
									+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ ")"
				;
			} 
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
		
			//organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ") : organ);
			organ = (organ <= 0 ? this.cfg.getLong("user.organ") : (this.cfg.getInt("user.organ.depth") > depth ? this.cfg.getLong("user.organ") : organ));
			if (organ > 0) { // 조직
				TERMINAL_WHERE +=  " AND ORGANIZATION_SEQ IN ("						
								+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
										+ " SEQ"
									+ " FROM TB_ORGANIZATION"
									+ " WHERE SORT = 1"
									+ " START WITH SEQ = " + organ
									+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
							+ ")"
					;
			} 
		}
		
		if (place > 0) {// 위치
			TERMINAL_WHERE += " AND TERMINAL_ID = ("
					+ " SELECT /*+ INDEX(TB_VENDING_MACHINE) */"
						+ " TERMINAL_ID"
					+ " FROM TB_VENDING_MACHINE"
					+ " WHERE SEQ = ("
						+ "SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
							+ " VM_SEQ"
						+ " FROM TB_VENDING_MACHINE_PLACE"
						+ " WHERE SEQ =" + place
					+ ")"
			    + ")"
			;	
		}
		
		//if (!TERMINAL_WHERE.isEmpty())
		TERMINAL_WHERE += " ) ";
		
		// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}		
		

	
		
		
		// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();
			
		try {
			
			long[] grandTotalCounts = { 0, 0 };
			long[] grandTotalAmounts = { 0, 0 };
		
			ps = dbLib.prepareStatement(conn,
			   "SELECT /*+ ORDERED USE_HASH(B C D) */"
					+ " A.*, "
					+ " (SELECT /*+ INDEX(TB_ORGANIZATION) */"
							+ " NAME"
						+ " FROM TB_ORGANIZATION"
						+ " WHERE PARENT_SEQ = " + (organ > 0 ? organ : 0)
							+ " AND SORT = 1"
							+ " AND SEQ <> A.ORGANIZATION_SEQ"
						+ " START WITH SEQ = A.ORGANIZATION_SEQ"
						+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
					+ " ) AS PARENT_ORGAN,"	
					+ " B.NAME AS ORGAN,"
					+ " C.PLACE AS PLACE,"
					+ " C.SEQ AS VM_PLACE_SEQ"
			   + " FROM ( "
			        + "SELECT /*+ ORDERED USE_HASH(C S) */"
			        	+ " C.*,"
			            + " S.COMPANY_SEQ,"
			            + " S.ORGANIZATION_SEQ,"
			            + " S.SEQ AS VM_SEQ,"
			            + " S.CODE AS VM_CODE"
			        + " FROM  (  "    
			            + " SELECT"
				            + " COUNT_DAY,"
			                + " TERMINAL_ID," 
			                + " TO_CHAR(MIN(COUNT_DATE_PREV), 'YYYYMMDDHH24MISS') START_DATE," 
			                + " TO_CHAR(MAX(COUNT_DATE), 'YYYYMMDDHH24MISS') END_DATE," 
			                + " SUM(TOTAL_COUNT) TOTAL_COUNT," 
			                + " SUM(TOTAL_AMOUNT) TOTAL_AMOUNT,"
			                + " SUM(CASH_COUNT) CASH_COUNT," 
			                + " SUM(CASH_AMOUNT) CASH_AMOUNT,"
			                + " SUM(CARD_COUNT) CARD_COUNT," 
			                + " SUM(CARD_AMOUNT) CARD_AMOUNT"
			            + " FROM TB_SALESCOUNT_DETAIL"   
			            + " WHERE " + TERMINAL_WHERE
			                + " AND COL_NO = 0"
			                + " AND COUNT_DAY >= '" + sDate + "'"
			                + " AND COUNT_DAY <= '" + eDate + "'"
			            + "  GROUP BY COUNT_DAY, TERMINAL_ID "   
			            + " ORDER BY COUNT_DAY DESC, TERMINAL_ID"			           
			        + " ) C "
			        	+ "INNER JOIN TB_VENDING_MACHINE S"
			        		+ " ON C.TERMINAL_ID = S.TERMINAL_ID"
			        		+ " AND S.COMPANY_SEQ = " + company
			     + " ) A"
					+ " INNER JOIN TB_ORGANIZATION B"
					    + " ON A.ORGANIZATION_SEQ = B.SEQ"
				    + " INNER JOIN TB_VENDING_MACHINE_PLACE C"
					    + " ON A.VM_SEQ = C.VM_SEQ"
					    	+ " AND C.END_DATE IS NULL"
			     + " ORDER BY "   	
			     // 정렬 값(oMode, oType)에 따라 ORDER BY 변경
			     + ((oType == 1) ? ( //DESC
			    		// 거래처
			    		(oMode == 1) ? ( " PARENT_ORGAN DESC, ORGAN DESC, PLACE DESC, A.TERMINAL_ID, A.END_DATE " ) :
			    		// 설치장소
			    		(oMode == 2) ? ( " PARENT_ORGAN DESC, ORGAN DESC, PLACE DESC, A.TERMINAL_ID, A.END_DATE " ) :
			    		// default : 날짜
			    			 ( "  PARENT_ORGAN, ORGAN, PLACE, A.TERMINAL_ID, A.END_DATE DESC" )
			        ) : ( //ASC
				    		// 거래처
				    		(oMode == 1) ? ( " PARENT_ORGAN , ORGAN , PLACE, A.TERMINAL_ID, A.END_DATE  " ) :
				    		// 설치장소
				    		(oMode == 2) ? ( " PARENT_ORGAN , ORGAN , PLACE, A.TERMINAL_ID , A.END_DATE " ) :
				    		// default : 날짜
				    	    ( "PARENT_ORGAN, ORGAN, PLACE, A.TERMINAL_ID,  A.END_DATE " )
			            ) 
			    	)
			     //+ ", A.TERMINAL_ID "
			);	
			rs = ps.executeQuery();

			int n = 0;
			GeneralConfig c = null;
			String startDate = null;
			String endDate = null;
			
			while (rs.next()) {

				c = new GeneralConfig();
				
				n = 0;

				String yyyymmdd = rs.getString("COUNT_DAY");
				c.put("YYYYMMDD", yyyymmdd);
				c.put("DATE", yyyymmdd.substring(0, 4) + "-" + yyyymmdd.substring(4, 6) + "-" + yyyymmdd.substring(6, 8));

				c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("VM_PLACE_SEQ", rs.getLong("VM_PLACE_SEQ"));
				c.put("PARENT_ORGAN", rs.getString("PARENT_ORGAN"));
				c.put("ORGAN", rs.getString("ORGAN"));
				
				c.put("PLACE", rs.getString("PLACE"));		
	
				c.put("VM_CODE", rs.getString("VM_CODE"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));

				startDate = null;
				endDate = null;	
				
				
				if ((n == 0) || !c.get("START_DATE_" + n).equals("-")) n++;
				c.put("PAY_COUNT", n);
				
				String date = rs.getString("START_DATE");
				c.put("START_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
				);
				if ((startDate == null) || (startDate.compareTo(date) > 0)) {
					if (startDate != null) System.out.println("START_DATE : " + startDate + "-->" + date);
					c.put("START_DATE", c.get("START_DATE_" + n));
					startDate = date;
				}

				date = rs.getString("END_DATE");
				c.put("END_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
				);
				if ((endDate == null) || (endDate.compareTo(date) < 0)) {
					if (endDate != null) System.out.println("END_DATE : " + endDate + "-->" + date);
					c.put("END_DATE", c.get("END_DATE_" + n));
					endDate = date;
				}
				
				/*
				//전체 TOTAL 이 CASH와 CARD의 합과 다른것 발견하여 수정 20170901 jwhwang
				long totalCount = rs.getLong("TOTAL_COUNT");
				long totalAmount = rs.getLong("TOTAL_AMOUNT");
				c.put("CNT_TOTAL", c.getLong("CNT_TOTAL") + totalCount);
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_TOTAL") + totalAmount);
				*/
				
				//신용
				long countCard =  rs.getLong("CARD_COUNT");
				long amountCard = rs.getLong("CARD_AMOUNT");
				c.put("CNT_CARD", c.getLong("CNT_CARD") + countCard);
				c.put("AMOUNT_CARD", c.getLong("AMOUNT_CARD") + amountCard);
				grandTotalCounts[1] += countCard;
				grandTotalAmounts[1] += amountCard;
				
				//현금
				long countCash =  rs.getLong("CASH_COUNT");
				long amountCash = rs.getLong("CASH_AMOUNT");
				
				c.put("CNT_CASH", c.getLong("CNT_CASH") + countCash);
				c.put("AMOUNT_CASH", c.getLong("AMOUNT_CASH") + amountCash);
				grandTotalCounts[0] += countCash;
				grandTotalAmounts[0] += amountCash;
				
				//전체
				long totalCount = countCard + countCash;
				long totalAmount =amountCard + amountCash ;
				c.put("CNT_TOTAL", c.getLong("CNT_TOTAL") + totalCount);
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_TOTAL") + totalAmount);
				
				this.list.add(c);
				
			}
			this.data.put("CNT_CASH", grandTotalCounts[0]);
			this.data.put("AMOUNT_CASH", grandTotalAmounts[0]);
			
			this.data.put("CNT_CARD", grandTotalCounts[1]);
			this.data.put("AMOUNT_CARD", grandTotalAmounts[1]);
			

			this.data.put("CNT_TOTAL", grandTotalCounts[0] + grandTotalCounts[1] );
			this.data.put("AMOUNT_TOTAL", grandTotalAmounts[0] + grandTotalAmounts[1] );
			
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}
		
		// 조직명
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
						+ " FROM ("
								+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
										+ " NAME"
									+ " FROM TB_ORGANIZATION"
									+ " WHERE COMPANY_SEQ = " + company
										+ " AND SORT = 0"
									+ " ORDER BY DEPTH DESC"
							+ " )"
						+ " WHERE ROWNUM = 1"
					)
			);
		String sDesc = null;
		// 검색 설정
		//sDesc = "집계기준="
		//		+ dbLib.getResult(conn,
		//				"SELECT /*+ INDEX(TB_CODE) */"
		//						+ " NAME"
		//					+ " FROM TB_CODE"
		//					+ " WHERE TYPE = 'SUM_TYPE'"
		//						+ " AND CODE = '" + sType + "'"
		//			)
		//		+ "&집계기간=" + sDate + "-" + eDate
		//	;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}

		this.data.put("sDesc", sDesc);
		
		// 리소스 반환
		dbLib.close(conn);
		
		return error;
		
	}	
	
	/**
	 * 판매계수상세
	 *
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 검색일
	 * @param eDate 종료일
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String salesCount(long company, long organ,long depth, long place, String sDate, String eDate, String terminal) throws Exception {
	// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
			return null;
		}

	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String error = null;
		long krr = 264L;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			throw new Exception("DB 연결에 실패하였습니다.");
		}
		
		//검색절 생성
		String SALESCOUNT_RANGE = "";
		
		company = (company > 0 ? company : this.cfg.getLong("user.company"));
		
		
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 

			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
		
			//organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			//organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ") : organ);
			organ = (organ <= 0 ? this.cfg.getLong("user.organ") : (this.cfg.getInt("user.organ.depth") > depth ? this.cfg.getLong("user.organ") : organ));
			
		}
		


		
		String SUM_OF_EACH_PREPAY_COMPANY = "";
		String[] prepaySummaryColumns = new String[] {};
		ArrayList<String> prepaySummaryColumnList = new ArrayList<String>();		
		
		// 선불 카드사
		this.company = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(TB_CODE) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE"
						+ " WHERE TYPE = 'PAY_CARD'"
							+ " AND CODE <> '000'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				String code = rs.getString("CODE");
				c.put("CODE", code);
				c.put("NAME", rs.getString("NAME"));

				this.company.add(c);

				SUM_OF_EACH_PREPAY_COMPANY += " COUNT(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN 1 END) AS CNT_PREPAY_" + code + ","
						+ " NVL(SUM(CASE WHEN PAY_TYPE = '11' AND PAY_CARD = '" + code + "' THEN AMOUNT END), 0) AS AMOUNT_PREPAY_" + code + ","
					;
				prepaySummaryColumnList.add("CNT_PREPAY_" + code);
				prepaySummaryColumnList.add("AMOUNT_PREPAY_" + code);
			}

			prepaySummaryColumns = prepaySummaryColumnList.toArray(new String[prepaySummaryColumnList.size()]);
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			prepaySummaryColumnList = null;

			dbLib.close(rs);
			dbLib.close(ps);
		}		
		
		// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}		
		

		
		SALESCOUNT_RANGE += " SELECT TERMINAL_ID, MAX(COUNT_DATE) AS COUNT_DATE " //당일 마지막 마감
            + " FROM TB_SALESCOUNT A "
            + " WHERE TERMINAL_ID = '" + terminal + "'"
                + " AND COL_NO = 0" 
                + " AND COUNT_MODE = 'A'"
                + (company==krr?
                		" AND TRIM(GOODS_CODE) <> '000000'":"" 
                )
                + " AND COUNT_DATE >= TO_DATE('" + sDate + "', 'YYYY-MM-DD HH24:MI:SS')" 
                + " AND COUNT_DATE <= TO_DATE('" + eDate + "', 'YYYY-MM-DD HH24:MI:SS')"
                //+ " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64)"
                + (company==krr? 
    	                " AND ("
    	                	+ " (COL_COUNT <= 24 AND   (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1)) "
    	                    + " OR  (COL_COUNT > 24 AND (SELECT MAX(COL_NO) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) = (COL_COUNT )) "
    	                + ")" : " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64, 57)"
                    )
                + " AND STATE = 0"
            + " GROUP BY TERMINAL_ID, TO_CHAR(COUNT_DATE, 'YYYYMMDD')"    
            ;
		
		
		// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();
			
		try {
			
			long[] grandTotalCounts = { 0, 0, 0, 0 };
			long[] grandTotalAmounts = { 0, 0, 0, 0 };
		
			ps = dbLib.prepareStatement(conn,
			   "SELECT /*+ ORDERED USE_HASH(B C D P) */"
					+ " A.*, "
					+ " (SELECT /*+ INDEX(TB_ORGANIZATION) */"
							+ " NAME"
						+ " FROM TB_ORGANIZATION"
						+ " WHERE  SORT = 1"
							+ " AND SEQ <> A.ORGANIZATION_SEQ"
					        + " AND DEPTH = B.DEPTH - 1" //20181116 jwhwang 앱코때문에 추가
						+ " START WITH SEQ = A.ORGANIZATION_SEQ"
						+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
					+ " ) AS PARENT_ORGAN,"	
					+ " B.NAME AS ORGAN,"
					+ " C.PLACE AS PLACE,"
					+ " D.CODE AS VM_CODE,"
					+ " P.PRICE PRODUCT_PRICE,"
					+ " P.NAME PRODUCT_NAME"
			   + " FROM ( "
				    + "SELECT "
				        + " COUNT_DAY," 
				        + " MAX(COMPANY_SEQ) COMPANY_SEQ,"
				        + " MAX(ORGANIZATION_SEQ) ORGANIZATION_SEQ,"
				        + " MAX(VM_PLACE_SEQ) VM_PLACE_SEQ,"
				        + " TERMINAL_ID," 
				        + " COL_NO,"
				        + " PRICE,"
				        + " GOODS_CODE,"
				        + " NUM,"
				        + " COUNT_DATE_START AS START_DATE,"
				        + " COUNT_DATE_END AS END_DATE,"
				        + " TOTAL_COUNT,"
				        + " TOTAL_AMOUNT,"
				        + " COUNT(CASE WHEN PAY_TYPE = '01' THEN 1 END)  AS CNT_CARD,"
				        + " NVL(SUM(CASE WHEN PAY_TYPE = '01' THEN AMOUNT ELSE 0 END), 0) AS AMOUNT_CARD,"
				        + " COUNT(CASE WHEN PAY_TYPE = '07' THEN 1 END)  AS CNT_SMTPAY,"
				        + " NVL(SUM(CASE WHEN PAY_TYPE = '07' THEN AMOUNT ELSE 0 END), 0) AS AMOUNT_SMTPAY,"
				        + SUM_OF_EACH_PREPAY_COMPANY
				        + " COUNT(CASE WHEN PAY_TYPE = '11' THEN 1 END)  AS CNT_PREPAY,"
				        + " NVL(SUM(CASE WHEN PAY_TYPE = '11' THEN AMOUNT ELSE 0 END), 0) AS AMOUNT_PREPAY "
				    + "FROM (" 
				        //+ "SELECT /*+ ORDERED USE_HASH(C S) */"
				        + "SELECT /*+ INDEX(S)*/" //20181116 수정
				        	+ " C.*,"
				        	+  company + " AS COMPANY_SEQ,"
				        	+  organ + " AS ORGANIZATION_SEQ,"
				        	+  place + " AS VM_PLACE_SEQ,"
				            + " S.PAY_TYPE,"
				            + " S.PAY_CARD,"
				            + " S.AMOUNT "
				        + "FROM  (  "    
				            + "SELECT "
				                + " COUNT_DAY,"
				                + " TERMINAL_ID," 
				                + " COL_NO,"
				                + " PRICE,"
				                + " GOODS_CODE,"
				                + " NUM,"
				                + " TO_CHAR(COUNT_DATE_START, 'YYYYMMDDHH24MISS') AS COUNT_DATE_START,"
				                + " TO_CHAR(COUNT_DATE_END, 'YYYYMMDDHH24MISS') AS COUNT_DATE_END,"
				                + " TOTAL_COUNT - TOTAL_COUNT_PREV AS TOTAL_COUNT,"
				                + " TOTAL_AMOUNT - TOTAL_AMOUNT_PREV  AS TOTAL_AMOUNT"
				            + " FROM ("            
				                + " SELECT "
				                	+ " TO_CHAR(COUNT_DATE, 'YYYYMMDD') AS COUNT_DAY,"
				                    + " TERMINAL_ID,"
				                    + " COL_NO,"
				                    + " PRICE,"
				                    + " GOODS_CODE,"
				                    + " CASE WHEN NUM > 1 THEN LAG(COUNT_DATE) OVER (ORDER BY TERMINAL_ID, COL_NO, COUNT_DATE, NUM) ELSE TO_DATE('20161013000001', 'YYYYMMDDHH24MISS') END AS COUNT_DATE_START,"
				                    + " COUNT_DATE AS COUNT_DATE_END,"
				                    + " NUM, "
				                    + " CASE WHEN NUM > 1 THEN LAG(TOTAL_COUNT) OVER (ORDER BY TERMINAL_ID, COL_NO,  COUNT_DATE, NUM) ELSE NULL END TOTAL_COUNT_PREV," 
				                    + " TOTAL_COUNT,"
				                    + " CASE WHEN NUM > 1 THEN LAG(TOTAL_AMOUNT) OVER (ORDER BY TERMINAL_ID, COL_NO, COUNT_DATE, NUM) ELSE NULL END TOTAL_AMOUNT_PREV," 
				                    + " TOTAL_AMOUNT "
				                + " FROM ( "   
				                    + " SELECT " 
				                        + " TERMINAL_ID,"
				                        + " COL_NO,"
				                        + " PRICE,"
				                        + " GOODS_CODE,"
				                        + " COUNT_DATE,"
				                        + " ROW_NUMBER() OVER (PARTITION BY TERMINAL_ID, COL_NO ORDER BY TERMINAL_ID, COL_NO, MAX(COUNT_DATE)) AS NUM,"
				                        + " SUM(TOTAL_COUNT) AS TOTAL_COUNT,"
				                        + " SUM(TOTAL_AMOUNT) AS TOTAL_AMOUNT " 
				                    + " FROM ( "
				                        + " SELECT "
				                            + " A.TERMINAL_ID, A.COUNT_DATE, B.MODEL, A.COL_NO, A.PRICE, A.GOODS_CODE,"
				                            + " TOTAL_COUNT,"
				                            + " TOTAL_AMOUNT "
				                        + " FROM  TB_SALESCOUNT A "
				                            + " INNER JOIN TB_VENDING_MACHINE B"
				                                + " ON A.TERMINAL_ID = B.TERMINAL_ID "
				                                	+ " AND ((B.MODEL IN ('LVM-6112') AND COL_NO NOT IN (0, 12, 13)) "
				                            			+ " OR (B.MODEL IN ('R-6107') AND COL_NO NOT IN (0, 11, 12)) "	
				                            			+ " OR (B.MODEL IN ('CVK-6024','LVM-6141') AND COL_NO NOT IN (0, 13, 14)) "
				                                		+ " OR (B.MODEL NOT IN ('LVM-6112', 'R-6107', 'CVK-6024','LVM-6141') AND COL_NO NOT IN (0))) "
				                        + " WHERE (A.TERMINAL_ID , COUNT_DATE) IN ("  
				                            + SALESCOUNT_RANGE
	                                        + " ) "
				                        + " ) "
				                    + " GROUP BY TERMINAL_ID, COL_NO, PRICE, GOODS_CODE, COUNT_DATE"
				                    + " ORDER BY TERMINAL_ID, COL_NO, COUNT_DATE"
				                + " ) "  
				            + " ) " 
				            + " ORDER BY COL_NO, COUNT_DATE_END DESC "
	
				        + " ) C "
				            + " LEFT OUTER JOIN TB_SALES S "
				                + " ON C.TERMINAL_ID = S.TERMINAL_ID"
				                	+ " AND S.COMPANY_SEQ =  " + company
				                    //+ " AND S.PAY_TYPE  IN ( '01', '11') " //신용, 선불
				                    + " AND S.PAY_STEP NOT IN ('00', '99') " //(망)취소제외
				                    + " AND S.TRANSACTION_DATE || S.TRANSACTION_TIME > C.COUNT_DATE_START "
				                    + " AND S.TRANSACTION_DATE || S.TRANSACTION_TIME <= C.COUNT_DATE_END "
				                    + " AND S.COL_NO = C.COL_NO"
				         + " WHERE NUM > 1 "
				     + " ) "    
				     + " GROUP BY COUNT_DAY, TERMINAL_ID, COL_NO, PRICE, GOODS_CODE, NUM, COUNT_DATE_START, COUNT_DATE_END, TOTAL_COUNT, TOTAL_AMOUNT"
			     + " ) A"
					+ " INNER JOIN TB_ORGANIZATION B"
						+ " ON A.ORGANIZATION_SEQ = B.SEQ"
				    + " INNER JOIN TB_VENDING_MACHINE_PLACE C"
				    	+ " ON A.VM_PLACE_SEQ = C.SEQ"
				    + " INNER JOIN TB_VENDING_MACHINE D"
					    + " ON C.VM_SEQ = D.SEQ"
					+ " LEFT OUTER JOIN TB_PRODUCT P"
					    + " ON P.COMPANY_SEQ = " + company
					    	+ " AND P.CODE = TRIM(A.GOODS_CODE)"
			     + " ORDER BY PARENT_ORGAN, ORGAN, PLACE, A.TERMINAL_ID, COL_NO, A.END_DATE DESC, A.NUM DESC"   	
			);	
			rs = ps.executeQuery();

			int n = 0;
			GeneralConfig c = null;
			String startDate = null;
			String endDate = null;
			
			while (rs.next()) {

				c = new GeneralConfig();
				
				n = 0;

				String yyyymmdd = rs.getString("COUNT_DAY");
				c.put("YYYYMMDD", yyyymmdd);
				c.put("DATE", yyyymmdd.substring(0, 4) + "-" + yyyymmdd.substring(4, 6) + "-" + yyyymmdd.substring(6, 8));

				c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("VM_PLACE_SEQ", rs.getLong("VM_PLACE_SEQ"));
				c.put("PARENT_ORGAN", rs.getString("PARENT_ORGAN"));
				c.put("ORGAN", rs.getString("ORGAN"));

				c.put("PLACE", rs.getString("PLACE"));							

				c.put("VM_CODE", rs.getString("VM_CODE"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("COL_NO", rs.getLong("COL_NO"));
				c.put("PRODUCT_NAME", rs.getString("PRODUCT_NAME"));
				c.put("PRODUCT_PRICE", rs.getLong("PRODUCT_PRICE"));
				c.put("PRICE", rs.getLong("PRICE"));
				c.put("PRODUCT_CODE", rs.getString("GOODS_CODE"));
				
				startDate = null;
				endDate = null;	
				
				
				if ((n == 0) || !c.get("START_DATE_" + n).equals("-")) n++;
				c.put("PAY_COUNT", n);
				
				String date = rs.getString("START_DATE");
				c.put("START_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				if ((startDate == null) || (startDate.compareTo(date) > 0)) {
					if (startDate != null) System.out.println("START_DATE : " + startDate + "-->" + date);
					c.put("START_DATE", c.get("START_DATE_" + n));
					startDate = date;
				}

				date = rs.getString("END_DATE");
				c.put("END_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				if ((endDate == null) || (endDate.compareTo(date) < 0)) {
					if (endDate != null) System.out.println("END_DATE : " + endDate + "-->" + date);
					c.put("END_DATE", c.get("END_DATE_" + n));
					endDate = date;
				}
				
				//전체
				long totalCount = rs.getLong("TOTAL_COUNT");
				long totalAmount = rs.getLong("TOTAL_AMOUNT");
				c.put("CNT_TOTAL", c.getLong("CNT_TOTAL") + totalCount);
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_TOTAL") + totalAmount);
				
				//선불
				long countPrepay =  rs.getLong("CNT_PREPAY");
				long amountPrepay = rs.getLong("AMOUNT_PREPAY");
				c.put("CNT_PREPAY", c.getLong("CNT_PREPAY") + countPrepay);
				c.put("AMOUNT_PREPAY", c.getLong("AMOUNT_PREPAY") + amountPrepay);
				grandTotalCounts[2] += countPrepay;
				grandTotalAmounts[2] += amountPrepay;
				
				//개별선불 
				for (String columnName : prepaySummaryColumns) {
					c.put(columnName + "_" + n, rs.getLong(columnName));
					c.put(columnName, c.getLong(columnName) + rs.getLong(columnName));
					this.data.put(columnName, this.data.getLong(columnName) + rs.getLong(columnName));
				}
				
				//신용
				long countCard =  rs.getLong("CNT_CARD");
				long amountCard = rs.getLong("AMOUNT_CARD");
				c.put("CNT_CARD", c.getLong("CNT_CARD") + countCard);
				c.put("AMOUNT_CARD", c.getLong("AMOUNT_CARD") + amountCard);
				grandTotalCounts[1] += countCard;
				grandTotalAmounts[1] += amountCard;
				
				//간편결제 scheo
				long countSmtpay =  rs.getLong("CNT_SMTPAY");
				long amountSmtpay = rs.getLong("AMOUNT_SMTPAY");
				c.put("CNT_SMTPAY", c.getLong("CNT_SMTPAY") + countSmtpay);
				c.put("AMOUNT_SMTPAY", c.getLong("AMOUNT_SMTPAY") + amountSmtpay);
				grandTotalCounts[3] += countSmtpay;
				grandTotalAmounts[3] += amountSmtpay;
				
				//현금
				long countCash = totalCount - countPrepay - countCard;
				long amountCash = totalAmount - amountPrepay - amountCard;
				c.put("CNT_CASH", c.getLong("CNT_CASH") + countCash);
				c.put("AMOUNT_CASH", c.getLong("AMOUNT_CASH") + amountCash);
				grandTotalCounts[0] += countCash;
				grandTotalAmounts[0] += amountCash;
				
				this.list.add(c);
				
			}
			this.data.put("CNT_CASH", grandTotalCounts[0]);
			this.data.put("AMOUNT_CASH", grandTotalAmounts[0]);
			
			this.data.put("CNT_CARD", grandTotalCounts[1]);
			this.data.put("AMOUNT_CARD", grandTotalAmounts[1]);
			
			this.data.put("CNT_PREPAY", grandTotalCounts[2]);
			this.data.put("AMOUNT_PREPAY", grandTotalAmounts[2]);
			
			this.data.put("CNT_SMTPAY", grandTotalCounts[3]);
			this.data.put("AMOUNT_SMTPAY", grandTotalAmounts[3]);
			
			this.data.put("CNT_TOTAL", grandTotalCounts[0] + grandTotalCounts[1] + grandTotalCounts[2] + grandTotalCounts[3]);
			this.data.put("AMOUNT_TOTAL", grandTotalAmounts[0] + grandTotalAmounts[1] + grandTotalAmounts[2] + grandTotalAmounts[3]);
			
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}
		
		// 조직명
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
						+ " FROM ("
								+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
										+ " NAME"
									+ " FROM TB_ORGANIZATION"
									+ " WHERE COMPANY_SEQ = " + company
										+ " AND SORT = 0"
									+ " ORDER BY DEPTH DESC"
							+ " )"
						+ " WHERE ROWNUM = 1"
					)
			);
		String sDesc = null;
		// 검색 설정
		//sDesc = "집계기준="
		//		+ dbLib.getResult(conn,
		//				"SELECT /*+ INDEX(TB_CODE) */"
		//						+ " NAME"
		//					+ " FROM TB_CODE"
		//					+ " WHERE TYPE = 'SUM_TYPE'"
		//						+ " AND CODE = '" + sType + "'"
		//			)
		//		+ "&집계기간=" + sDate + "-" + eDate
		//	;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}

		this.data.put("sDesc", sDesc);
		
		// 리소스 반환
		dbLib.close(conn);
		
		return error;
		
	}	
	/**
	 * 판매계수상세
	 *
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 검색일
	 * @param eDate 종료일
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String salesCountNew(long company, long organ, long depth, long place, String sDate, String eDate, String terminal) throws Exception {
	// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
			return null;
		}

	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String error = null;
		long krr = 264L;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			throw new Exception("DB 연결에 실패하였습니다.");
		}
		
		//검색절 생성
		String SALESCOUNT_RANGE = "";
		
		company = (company > 0 ? company : this.cfg.getLong("user.company"));
		
		
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 

			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
		
			//organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			//organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ") : organ);
			organ = (organ <= 0 ? this.cfg.getLong("user.organ") : (this.cfg.getInt("user.organ.depth") > depth ? this.cfg.getLong("user.organ") : organ));
		}
		
		String SUM_OF_EACH_PREPAY_COMPANY = "";
		String[] prepaySummaryColumns = new String[] {};
		ArrayList<String> prepaySummaryColumnList = new ArrayList<String>();		
		
		// 선불 카드사
		this.company = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(TB_CODE) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE"
						+ " WHERE TYPE = 'PAY_CARD'"
							+ " AND CODE <> '000'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				String code = rs.getString("CODE");
				c.put("CODE", code);
				c.put("NAME", rs.getString("NAME"));

				this.company.add(c);

				SUM_OF_EACH_PREPAY_COMPANY += "SUM(VMMS_" + code + "_COUNT) AS CNT_PREPAY_" + code + ","
						+ " SUM(VMMS_" + code + "_AMOUNT) AS AMOUNT_PREPAY_" + code + ","
					;
				prepaySummaryColumnList.add("CNT_PREPAY_" + code);
				prepaySummaryColumnList.add("AMOUNT_PREPAY_" + code);
			}

			prepaySummaryColumns = prepaySummaryColumnList.toArray(new String[prepaySummaryColumnList.size()]);
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			prepaySummaryColumnList = null;

			dbLib.close(rs);
			dbLib.close(ps);
		}		
		
		// 에러 처리
		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}		
		

		
		// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();
			
		try {
			
			long[] grandTotalCounts = { 0, 0, 0, 0 };
			long[] grandTotalAmounts = { 0, 0, 0, 0 };
			ps = dbLib.prepareStatement(conn,
				"SELECT /*+ ORDERED USE_HASH(B C) */"  //scheo 판매계수집계 디테일
					+ " A.*,"  
				    + " (SELECT /*+ INDEX(TB_ORGANIZATION) */ "
				    		+ " NAME"
				    	+ " FROM TB_ORGANIZATION"
				    	+ " WHERE PARENT_SEQ = " + (organ > 0 ? organ : 0)
				    		+ " AND SORT = 1"
				    		+ " AND SEQ <> A.ORGANIZATION_SEQ "
				    	+ "START WITH SEQ = A.ORGANIZATION_SEQ "
				    	+ "CONNECT BY SEQ = PRIOR PARENT_SEQ"
			    	+ " ) AS PARENT_ORGAN," 
				    + " B.NAME AS ORGAN," 
				    + " C.PLACE AS PLACE," 
				    + " C.SEQ AS VM_PLACE_SEQ" 
				 + " FROM (" 
				    + " SELECT /*+ ORDERED USE_HASH(C S) */" 
				        + " C.*," 
				        + " S.COMPANY_SEQ," 
				        + " S.ORGANIZATION_SEQ," 
				        + " S.SEQ AS VM_SEQ," 
				        + " S.CODE AS VM_CODE" 
				    + " FROM  ("   
				        + " SELECT" 
				            + " COUNT_DAY," 
				            + " TERMINAL_ID,"
				            + " COL_NO," 
				            + " MAX(GOODS_CODE) GOODS_CODE,"
				            + " MAX(GOODS_NAME) PRODUCT_NAME,"
				            + " MAX(PRICE) PRICE,"
				            + " MAX(UNIT_PRICE) PRODUCT_PRICE,"
				            + " TO_CHAR(MIN(COUNT_DATE_PREV), 'YYYYMMDDHH24MISS') START_DATE," 
				            + " TO_CHAR(MAX(COUNT_DATE), 'YYYYMMDDHH24MISS') END_DATE," 
				            + " SUM(TOTAL_COUNT) TOTAL_COUNT," 
				            + " SUM(TOTAL_AMOUNT) TOTAL_AMOUNT," 
				            + " SUM(TOTAL_COUNT) - SUM(VMMS_CARD_COUNT) CNT_CASH," 
				            + " SUM(TOTAL_AMOUNT) - SUM(VMMS_CARD_AMOUNT) AMOUNT_CASH," 
				            + " SUM(VMMS_CRDT_COUNT) CNT_CARD," 
				            + " SUM(VMMS_CRDT_AMOUNT) AMOUNT_CARD," 
					        //+ " SUM(VMMS_NPC_COUNT) CNT_SMTPAY," 
	                        //+ " SUM(VMMS_NPC_AMOUNT) AMOUNT_SMTPAY,"
	                        + " SUM(VMMS_NPC_COUNT) + SUM(VMMS_KKO_COUNT) CNT_SMTPAY,"             // 카카오페이 추가
	                        + " SUM(VMMS_NPC_AMOUNT) + SUM(VMMS_KKO_AMOUNT) AMOUNT_SMTPAY,"     // 카카오페이 추가
							+ SUM_OF_EACH_PREPAY_COMPANY
							+ " MAX(MEMO) "
				        + " FROM TB_SALESCOUNT_DETAIL" 
				        + " WHERE  TERMINAL_ID = '" + terminal + "'"
				            + " AND COUNT_DATE_PREV >= TO_DATE('" + sDate + "', 'YYYY-MM-DD HH24:MI:SS')"
				            + " AND COUNT_DATE <= TO_DATE('" + eDate + "', 'YYYY-MM-DD HH24:MI:SS')"
				        + " GROUP BY COUNT_DAY, TERMINAL_ID, COL_NO"  
				        + " ORDER BY COUNT_DAY DESC, TERMINAL_ID, COL_NO" 
				    + " ) C" 
				        + " INNER JOIN TB_VENDING_MACHINE S "
				        	+ "ON C.TERMINAL_ID = S.TERMINAL_ID "
				        	+ " AND ((S.MODEL IN ('LVM-6112') AND COL_NO NOT IN (0, 12, 13)) "
				        		+ " OR (S.MODEL IN ('R-6107') AND COL_NO NOT IN (0, 11, 12)) "
				        		+ " OR (S.MODEL IN ('CVK-6024','LVM-6141') AND COL_NO NOT IN (0, 13, 14)) "
                        		+ " OR (S.MODEL NOT IN ('LVM-6112', 'R-6107', 'CVK-6024','LVM-6141') AND COL_NO NOT IN (0))) " 
				+ " ) A" 
				    + " INNER JOIN TB_ORGANIZATION B ON A.ORGANIZATION_SEQ = B.SEQ" 
				    + " INNER JOIN TB_VENDING_MACHINE_PLACE C ON A.VM_SEQ = C.VM_SEQ AND C.END_DATE IS NULL" 
				+ " ORDER BY  PARENT_ORGAN , ORGAN , PLACE, A.TERMINAL_ID, A.COL_NO" 					
					);			

			rs = ps.executeQuery();

			int n = 0;
			GeneralConfig c = null;
			String startDate = null;
			String endDate = null;
			
			while (rs.next()) {

				c = new GeneralConfig();
				
				n = 0;

				String yyyymmdd = rs.getString("COUNT_DAY");
				c.put("YYYYMMDD", yyyymmdd);
				c.put("DATE", yyyymmdd.substring(0, 4) + "-" + yyyymmdd.substring(4, 6) + "-" + yyyymmdd.substring(6, 8));

				c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("VM_PLACE_SEQ", rs.getLong("VM_PLACE_SEQ"));
				c.put("PARENT_ORGAN", rs.getString("PARENT_ORGAN"));
				c.put("ORGAN", rs.getString("ORGAN"));

				c.put("PLACE", rs.getString("PLACE"));

				c.put("VM_CODE", rs.getString("VM_CODE"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("COL_NO", rs.getLong("COL_NO"));
				c.put("PRODUCT_NAME", rs.getString("PRODUCT_NAME"));
				c.put("PRODUCT_PRICE", rs.getLong("PRODUCT_PRICE"));
				c.put("PRICE", rs.getLong("PRICE"));
				c.put("PRODUCT_CODE", rs.getString("GOODS_CODE"));
				
				startDate = null;
				endDate = null;	
				
				
				if ((n == 0) || !c.get("START_DATE_" + n).equals("-")) n++;
				c.put("PAY_COUNT", n);
				
				String date = rs.getString("START_DATE");
				c.put("START_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				if ((startDate == null) || (startDate.compareTo(date) > 0)) {
					if (startDate != null) System.out.println("START_DATE : " + startDate + "-->" + date);
					c.put("START_DATE", c.get("START_DATE_" + n));
					startDate = date;
				}

				date = rs.getString("END_DATE");
				c.put("END_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				if ((endDate == null) || (endDate.compareTo(date) < 0)) {
					if (endDate != null) System.out.println("END_DATE : " + endDate + "-->" + date);
					c.put("END_DATE", c.get("END_DATE_" + n));
					endDate = date;
				}
				
				//전체
				long totalCount = rs.getLong("TOTAL_COUNT");
				long totalAmount = rs.getLong("TOTAL_AMOUNT");
				c.put("CNT_TOTAL", c.getLong("CNT_TOTAL") + totalCount);
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_TOTAL") + totalAmount);
				grandTotalCounts[2] += totalCount;
				grandTotalAmounts[2] += totalAmount;
				
				//개별선불 
				for (String columnName : prepaySummaryColumns) {
					c.put(columnName + "_" + n, rs.getLong(columnName));
					c.put(columnName, c.getLong(columnName) + rs.getLong(columnName));
					this.data.put(columnName, this.data.getLong(columnName) + rs.getLong(columnName));
				}
				
				//신용
				long countCard =  rs.getLong("CNT_CARD");
				long amountCard = rs.getLong("AMOUNT_CARD");
				c.put("CNT_CARD", c.getLong("CNT_CARD") + countCard);
				c.put("AMOUNT_CARD", c.getLong("AMOUNT_CARD") + amountCard);
				grandTotalCounts[1] += countCard;
				grandTotalAmounts[1] += amountCard;
				
				//간편결제 scheo
				long countSmtpay =  rs.getLong("CNT_SMTPAY");
				long amountSmtpay = rs.getLong("AMOUNT_SMTPAY");
				c.put("CNT_SMTPAY", c.getLong("CNT_SMTPAY") + countSmtpay);
				c.put("AMOUNT_SMTPAY", c.getLong("AMOUNT_SMTPAY") + amountSmtpay);
				grandTotalCounts[3] += countSmtpay;
				grandTotalAmounts[3] += amountSmtpay;
				
				//현금
				long countCash =  rs.getLong("CNT_CASH");
				long amountCash = rs.getLong("AMOUNT_CASH");
				c.put("CNT_CASH", c.getLong("CNT_CASH") + countCash);
				c.put("AMOUNT_CASH", c.getLong("AMOUNT_CASH") + amountCash);
				grandTotalCounts[0] += countCash;
				grandTotalAmounts[0] += amountCash;
				
				this.list.add(c);
				
			}
			this.data.put("CNT_CASH", grandTotalCounts[0]);
			this.data.put("AMOUNT_CASH", grandTotalAmounts[0]);
			
			this.data.put("CNT_CARD", grandTotalCounts[1]);
			this.data.put("AMOUNT_CARD", grandTotalAmounts[1]);
			
			this.data.put("CNT_SMTPAY", grandTotalCounts[3]);
			this.data.put("AMOUNT_SMTPAY", grandTotalAmounts[3]);
			
			this.data.put("CNT_TOTAL", grandTotalCounts[2]);
			this.data.put("AMOUNT_TOTAL", grandTotalAmounts[2]);
			
			
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}
		
		// 조직명
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
						+ " FROM ("
								+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
										+ " NAME"
									+ " FROM TB_ORGANIZATION"
									+ " WHERE COMPANY_SEQ = " + company
										+ " AND SORT = 0"
									+ " ORDER BY DEPTH DESC"
							+ " )"
						+ " WHERE ROWNUM = 1"
					)
			);
		String sDesc = null;
		// 검색 설정
		//sDesc = "집계기준="
		//		+ dbLib.getResult(conn,
		//				"SELECT /*+ INDEX(TB_CODE) */"
		//						+ " NAME"
		//					+ " FROM TB_CODE"
		//					+ " WHERE TYPE = 'SUM_TYPE'"
		//						+ " AND CODE = '" + sType + "'"
		//			)
		//		+ "&집계기간=" + sDate + "-" + eDate
		//	;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}

		this.data.put("sDesc", sDesc);
		
		// 리소스 반환
		dbLib.close(conn);
		
		return error;
		
	}		
	/**
	 * 판매계수상세
	 *
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 검색일
	 * @param eDate 종료일
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String salesCountRaw(long company, long organ, long depth, long place, String sDate, String eDate, String terminal) throws Exception {
	// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
			return null;
		}

	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String error = null;
		long krr = 264L;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			throw new Exception("DB 연결에 실패하였습니다.");
		}
		
		//검색절 생성
		String SALESCOUNT_RANGE = "";
		
		company = (company > 0 ? company : this.cfg.getLong("user.company"));
		
		
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 

			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
		
			//organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			//organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ") : organ);
			organ = (organ <= 0 ? this.cfg.getLong("user.organ") : (this.cfg.getInt("user.organ.depth") > depth ? this.cfg.getLong("user.organ") : organ));
			
		}
		
		// 선불 카드사
		this.company = new ArrayList<GeneralConfig>();
		
		SALESCOUNT_RANGE += " SELECT TERMINAL_ID, MAX(COUNT_DATE) AS COUNT_DATE " //당일 마지막 마감
            + " FROM TB_SALESCOUNT A "
            + " WHERE TERMINAL_ID = '" + terminal + "'"
                + " AND COL_NO = 0" 
                + " AND COUNT_MODE = 'A'"
                + (company==krr?
                		" AND TRIM(GOODS_CODE) <> '000000'":"" 
                )
                + " AND COUNT_DATE >= TO_DATE('" + sDate + "', 'YYYY-MM-DD HH24:MI:SS')" 
                + " AND COUNT_DATE <= TO_DATE('" + eDate + "', 'YYYY-MM-DD HH24:MI:SS')"
                + " AND (SELECT COUNT(*) FROM TB_SALESCOUNT WHERE A.TERMINAL_ID = TERMINAL_ID AND A.COUNT_DATE = COUNT_DATE) IN (COL_COUNT + 1, 64, 57)"
                + " AND STATE = 0"
            + " GROUP BY TERMINAL_ID, TO_CHAR(COUNT_DATE, 'YYYYMMDD')"    
            ;
		
		
		// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();
			
		try {
			
			long[] grandTotalCounts = { 0, 0};
			long[] grandTotalAmounts = { 0, 0};
		
			ps = dbLib.prepareStatement(conn,
			   "SELECT /*+ ORDERED USE_HASH(B C P) */"
					+ " A.*, "
					+ " (SELECT /*+ INDEX(TB_ORGANIZATION) */"
							+ " NAME"
						+ " FROM TB_ORGANIZATION"
						+ " WHERE  SORT = 1"
							+ " AND SEQ <> A.ORGANIZATION_SEQ"
						+ " START WITH SEQ = A.ORGANIZATION_SEQ"
						+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
					+ " ) AS PARENT_ORGAN,"	
					+ " B.NAME AS ORGAN,"
					+ " C.PLACE AS PLACE,"
					+ " C.SEQ AS VM_PLACE_SEQ, "
					+ " P.PRICE PRODUCT_PRICE,"
					+ " P.NAME PRODUCT_NAME"
			   + " FROM ( " 
			        + "SELECT /*+ ORDERED USE_HASH(C S) */"
			        	+ " C.*,"
			        	+ " S.COMPANY_SEQ,"
				        + " S.ORGANIZATION_SEQ,"
				        + " S.SEQ AS VM_SEQ,"
				        + " S.CODE AS VM_CODE"
			        + " FROM  (  "    
			            + "SELECT "
			                + " COUNT_DAY,"
			                + " TERMINAL_ID," 
			                + " COL_NO,"
			                + " PRICE,"
			                + " GOODS_CODE,"
			                + " NUM,"
			                + " TO_CHAR(COUNT_DATE_START, 'YYYYMMDDHH24MISS') AS START_DATE,"
			                + " TO_CHAR(COUNT_DATE_END, 'YYYYMMDDHH24MISS') AS END_DATE,"
			                + " TOTAL_COUNT - TOTAL_COUNT_PREV AS TOTAL_COUNT,"
			                + " TOTAL_AMOUNT - TOTAL_AMOUNT_PREV  AS TOTAL_AMOUNT,"
			                + " CASH_COUNT - CASH_COUNT_PREV AS CASH_COUNT,"
			                + " CASH_AMOUNT - CASH_AMOUNT_PREV  AS CASH_AMOUNT,"
			                + " CARD_COUNT - CARD_COUNT_PREV AS CARD_COUNT,"
			                + " CARD_AMOUNT - CARD_AMOUNT_PREV  AS CARD_AMOUNT"
			            + " FROM ("            
			                + " SELECT "
			                	+ " TO_CHAR(COUNT_DATE, 'YYYYMMDD') AS COUNT_DAY,"
			                    + " TERMINAL_ID,"
			                    + " COL_NO,"
			                    + " PRICE,"
			                    + " GOODS_CODE,"
			                    + " CASE WHEN NUM > 1 THEN LAG(COUNT_DATE) OVER (ORDER BY TERMINAL_ID, COL_NO, COUNT_DATE, NUM) ELSE TO_DATE('20161013000001', 'YYYYMMDDHH24MISS') END AS COUNT_DATE_START,"
			                    + " COUNT_DATE AS COUNT_DATE_END,"
			                    + " NUM, "
			                    + " CASE WHEN NUM > 1 THEN LAG(TOTAL_COUNT) OVER (ORDER BY TERMINAL_ID, COL_NO,  COUNT_DATE, NUM) ELSE NULL END TOTAL_COUNT_PREV," 
			                    + " TOTAL_COUNT,"
			                    + " CASE WHEN NUM > 1 THEN LAG(TOTAL_AMOUNT) OVER (ORDER BY TERMINAL_ID, COL_NO, COUNT_DATE, NUM) ELSE NULL END TOTAL_AMOUNT_PREV," 
			                    + " TOTAL_AMOUNT, "
			                    + " CASE WHEN NUM > 1 THEN LAG(CASH_COUNT) OVER (ORDER BY TERMINAL_ID, COL_NO,  COUNT_DATE, NUM) ELSE NULL END CASH_COUNT_PREV," 
			                    + " CASH_COUNT,"
			                    + " CASE WHEN NUM > 1 THEN LAG(CASH_AMOUNT) OVER (ORDER BY TERMINAL_ID, COL_NO, COUNT_DATE, NUM) ELSE NULL END CASH_AMOUNT_PREV," 
			                    + " CASH_AMOUNT, "
			                    + " CASE WHEN NUM > 1 THEN LAG(CARD_COUNT) OVER (ORDER BY TERMINAL_ID, COL_NO,  COUNT_DATE, NUM) ELSE NULL END CARD_COUNT_PREV," 
			                    + " CARD_COUNT,"
			                    + " CASE WHEN NUM > 1 THEN LAG(CARD_AMOUNT) OVER (ORDER BY TERMINAL_ID, COL_NO, COUNT_DATE, NUM) ELSE NULL END CARD_AMOUNT_PREV," 
			                    + " CARD_AMOUNT "
			                + " FROM ( "   
			                    + " SELECT " 
			                        + " TERMINAL_ID,"
			                        + " COL_NO,"
			                        + " PRICE,"
			                        + " GOODS_CODE,"
			                        + " COUNT_DATE,"
			                        + " ROW_NUMBER() OVER (PARTITION BY TERMINAL_ID, COL_NO ORDER BY TERMINAL_ID, COL_NO, MAX(COUNT_DATE)) AS NUM,"
			                        + " SUM(TOTAL_COUNT) AS TOTAL_COUNT,"
			                        + " SUM(TOTAL_AMOUNT) AS TOTAL_AMOUNT, " 
			                        + " SUM(CASH_COUNT) AS CASH_COUNT,"
			                        + " SUM(CASH_AMOUNT) AS CASH_AMOUNT, " 
			                        + " SUM(CARD_COUNT) AS CARD_COUNT,"
			                        + " SUM(CARD_AMOUNT) AS CARD_AMOUNT " 
			                    + " FROM ( "
			                        + " SELECT "
			                            + " A.TERMINAL_ID, A.COUNT_DATE, B.MODEL, A.COL_NO, A.PRICE, A.GOODS_CODE,"
			                            + " TOTAL_COUNT,"
			                            + " TOTAL_AMOUNT, "
			                            + " CASH_COUNT,"
			                            + " CASH_AMOUNT, "
			                            + " CARD_COUNT,"
			                            + " CARD_AMOUNT "
			                        + " FROM  TB_SALESCOUNT A "
			                            + " INNER JOIN TB_VENDING_MACHINE B"
			                                + " ON A.TERMINAL_ID = B.TERMINAL_ID "
			                                	+ " AND ((B.MODEL IN ('LVM-6112') AND COL_NO NOT IN (0, 12, 13)) "
			                                		+ " OR (B.MODEL IN ('R-6107') AND COL_NO NOT IN (0, 11, 12)) "	
			                                		+ " OR (B.MODEL IN ('CVK-6024','LVM-6141') AND COL_NO NOT IN (0, 13, 14)) "
			                                		+ " OR (B.MODEL NOT IN ('LVM-6112', 'R-6107', 'CVK-6024','LVM-6141') AND COL_NO NOT IN (0))) " 
			                        + " WHERE (A.TERMINAL_ID , COUNT_DATE) IN ("  
			                            + SALESCOUNT_RANGE
                                        + " ) "
			                        + " ) "
			                    + " GROUP BY TERMINAL_ID, COL_NO, PRICE, GOODS_CODE, COUNT_DATE"
			                    + " ORDER BY TERMINAL_ID, COL_NO, COUNT_DATE"
			                + " ) "  
			            + " ) " 
			            + " ORDER BY COL_NO, COUNT_DATE_END DESC "

			        + " ) C "
				        + "INNER JOIN TB_VENDING_MACHINE S"
			        		+ " ON C.TERMINAL_ID = S.TERMINAL_ID"
			        			+ " AND S.COMPANY_SEQ = " + company
			         + " WHERE NUM > 1 "  
			     + " ) A"
					+ " INNER JOIN TB_ORGANIZATION B"
						+ " ON A.ORGANIZATION_SEQ = B.SEQ"
				    + " INNER JOIN TB_VENDING_MACHINE_PLACE C"
				    	+ " ON A.VM_SEQ = C.VM_SEQ"
				    		+ " AND C.END_DATE IS NULL"
		    		+ " LEFT OUTER JOIN TB_VENDING_MACHINE_PRODUCT D"
				    	+ " ON A.VM_SEQ = D.VM_SEQ"
				    		+ " AND A.COL_NO = D.COL_NO" 	
					+ " LEFT OUTER JOIN TB_PRODUCT P"
					    + " ON P.COMPANY_SEQ = " + company
					    	+ " AND P.SEQ = D.PRODUCT_SEQ"				    	
			     + " ORDER BY PARENT_ORGAN, ORGAN, PLACE, A.TERMINAL_ID, A.COL_NO, A.END_DATE DESC, A.NUM DESC"   	
			);	
			rs = ps.executeQuery();

			int n = 0;
			GeneralConfig c = null;
			String startDate = null;
			String endDate = null;
			
			while (rs.next()) {

				c = new GeneralConfig();
				
				n = 0;

				String yyyymmdd = rs.getString("COUNT_DAY");
				c.put("YYYYMMDD", yyyymmdd);
				c.put("DATE", yyyymmdd.substring(0, 4) + "-" + yyyymmdd.substring(4, 6) + "-" + yyyymmdd.substring(6, 8));

				c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("VM_PLACE_SEQ", rs.getLong("VM_PLACE_SEQ"));
				c.put("PARENT_ORGAN", rs.getString("PARENT_ORGAN"));
				c.put("ORGAN", rs.getString("ORGAN"));

				c.put("PLACE", rs.getString("PLACE"));							

				c.put("VM_CODE", rs.getString("VM_CODE"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("COL_NO", rs.getLong("COL_NO"));
				c.put("PRODUCT_NAME", rs.getString("PRODUCT_NAME"));
				c.put("PRODUCT_PRICE", rs.getLong("PRODUCT_PRICE"));
				c.put("PRICE", rs.getLong("PRICE"));
				c.put("PRODUCT_CODE", rs.getString("GOODS_CODE"));
				
				startDate = null;
				endDate = null;	
				
				
				if ((n == 0) || !c.get("START_DATE_" + n).equals("-")) n++;
				c.put("PAY_COUNT", n);
				
				String date = rs.getString("START_DATE");
				c.put("START_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				if ((startDate == null) || (startDate.compareTo(date) > 0)) {
					if (startDate != null) System.out.println("START_DATE : " + startDate + "-->" + date);
					c.put("START_DATE", c.get("START_DATE_" + n));
					startDate = date;
				}

				date = rs.getString("END_DATE");
				c.put("END_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				if ((endDate == null) || (endDate.compareTo(date) < 0)) {
					if (endDate != null) System.out.println("END_DATE : " + endDate + "-->" + date);
					c.put("END_DATE", c.get("END_DATE_" + n));
					endDate = date;
				}
				
				/*
				//전체
				long totalCount = rs.getLong("TOTAL_COUNT");
				long totalAmount = rs.getLong("TOTAL_AMOUNT");
				c.put("CNT_TOTAL", c.getLong("CNT_TOTAL") + totalCount);
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_TOTAL") + totalAmount);
				*/
				
				//신용
				long countCard =  rs.getLong("CARD_COUNT");
				long amountCard = rs.getLong("CARD_AMOUNT");
				c.put("CNT_CARD", c.getLong("CNT_CARD") + countCard);
				c.put("AMOUNT_CARD", c.getLong("AMOUNT_CARD") + amountCard);
				grandTotalCounts[1] += countCard;
				grandTotalAmounts[1] += amountCard;
				
				//현금
				long countCash = rs.getLong("CASH_COUNT");
				long amountCash = rs.getLong("CASH_AMOUNT");
				c.put("CNT_CASH", c.getLong("CNT_CASH") + countCash);
				c.put("AMOUNT_CASH", c.getLong("AMOUNT_CASH") + amountCash);
				grandTotalCounts[0] += countCash;
				grandTotalAmounts[0] += amountCash;
				
				//전체
				long totalCount = countCard + countCash;
				long totalAmount =amountCard + amountCash ;
				c.put("CNT_TOTAL", c.getLong("CNT_TOTAL") + totalCount);
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_TOTAL") + totalAmount);
				
				this.list.add(c);
				
			}
			this.data.put("CNT_CASH", grandTotalCounts[0]);
			this.data.put("AMOUNT_CASH", grandTotalAmounts[0]);
			
			this.data.put("CNT_CARD", grandTotalCounts[1]);
			this.data.put("AMOUNT_CARD", grandTotalAmounts[1]);
			
			
			this.data.put("CNT_TOTAL", grandTotalCounts[0] + grandTotalCounts[1]);
			this.data.put("AMOUNT_TOTAL", grandTotalAmounts[0] + grandTotalAmounts[1]);
			
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}
		
		// 조직명
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
						+ " FROM ("
								+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
										+ " NAME"
									+ " FROM TB_ORGANIZATION"
									+ " WHERE COMPANY_SEQ = " + company
										+ " AND SORT = 0"
									+ " ORDER BY DEPTH DESC"
							+ " )"
						+ " WHERE ROWNUM = 1"
					)
			);
		String sDesc = null;
		// 검색 설정
		//sDesc = "집계기준="
		//		+ dbLib.getResult(conn,
		//				"SELECT /*+ INDEX(TB_CODE) */"
		//						+ " NAME"
		//					+ " FROM TB_CODE"
		//					+ " WHERE TYPE = 'SUM_TYPE'"
		//						+ " AND CODE = '" + sType + "'"
		//			)
		//		+ "&집계기간=" + sDate + "-" + eDate
		//	;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}

		this.data.put("sDesc", sDesc);
		
		// 리소스 반환
		dbLib.close(conn);
		
		return error;
		
	}	
	/**
	 * 판매계수상세
	 *
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 검색일
	 * @param eDate 종료일
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String salesCountRawNew(long company, long organ, long depth, long place, String sDate, String eDate, String terminal) throws Exception {
	// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
			return null;
		}

	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String error = null;
		long krr = 264L;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			throw new Exception("DB 연결에 실패하였습니다.");
		}
		
		//검색절 생성
		String SALESCOUNT_RANGE = "";
		
		company = (company > 0 ? company : this.cfg.getLong("user.company"));
		
		
		if (this.cfg.getLong("user.company") == 0) { // 시스템 관리자 

			organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			
		} else { //시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
		
			//organ = (organ> 0 ? organ: this.cfg.getLong("user.organ"));
			//organ = (organ <=0 ? this.cfg.getLong("user.organ") : this.cfg.getInt("user.organ.depth")>0 ? this.cfg.getLong("user.organ") : organ);
			organ = (organ <= 0 ? this.cfg.getLong("user.organ") : (this.cfg.getInt("user.organ.depth") > depth ? this.cfg.getLong("user.organ") : organ));
			
		}
		
		// 선불 카드사
		this.company = new ArrayList<GeneralConfig>();
		
		
		// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();
			
		try {
			
			long[] grandTotalCounts = { 0, 0};
			long[] grandTotalAmounts = { 0, 0};
		
			ps = dbLib.prepareStatement(conn,
			   "SELECT /*+ ORDERED USE_HASH(B C) */"
					+ " A.*, "
					+ " (SELECT /*+ INDEX(TB_ORGANIZATION) */"
							+ " NAME"
						+ " FROM TB_ORGANIZATION"
						+ " WHERE  SORT = 1"
							+ " AND SEQ <> A.ORGANIZATION_SEQ"
						+ " START WITH SEQ = A.ORGANIZATION_SEQ"
						+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
					+ " ) AS PARENT_ORGAN,"	
					+ " B.NAME AS ORGAN,"
					+ " C.PLACE AS PLACE,"
					+ " C.SEQ AS VM_PLACE_SEQ"
			   + " FROM ( " 
			        + "SELECT /*+ ORDERED USE_HASH(C S) */"
			        	+ " C.*,"
			        	+ " S.COMPANY_SEQ,"
				        + " S.ORGANIZATION_SEQ,"
				        + " S.SEQ AS VM_SEQ,"
				        + " S.CODE AS VM_CODE"
			        + " FROM  (  "   
				        
						+ " SELECT"
							+ " COUNT_DAY,"
							+ " TERMINAL_ID,"
			                + " COL_NO,"
			                + " MAX(GOODS_CODE) GOODS_CODE,"
			                + " MAX(GOODS_NAME) PRODUCT_NAME,"
			                + " MAX(PRICE) PRICE,"
			                + " MAX(UNIT_PRICE) PRODUCT_PRICE,"
			                + " TO_CHAR(MIN(COUNT_DATE_PREV), 'YYYYMMDDHH24MISS') START_DATE," 
			                + " TO_CHAR(MAX(COUNT_DATE), 'YYYYMMDDHH24MISS') END_DATE," 
			                + " SUM(TOTAL_COUNT) TOTAL_COUNT," 
			                + " SUM(TOTAL_AMOUNT) TOTAL_AMOUNT,"
			                + " SUM(CASH_COUNT) CASH_COUNT," 
			                + " SUM(CASH_AMOUNT) CASH_AMOUNT," 
			                + " SUM(CARD_COUNT) CARD_COUNT," 
			                + " SUM(CARD_AMOUNT) CARD_AMOUNT,"
			                + " MAX(MEMO) MEMO"		//scheo 20191223 판매계수 실시간집계
			            + " FROM TB_SALESCOUNT_DETAIL"   
			            + " WHERE TERMINAL_ID = '" + terminal + "'"
			                + " AND COL_NO <> 0"
			                + " AND COUNT_DATE_PREV >= TO_DATE('" + sDate + "', 'YYYY-MM-DD HH24:MI:SS')"
			                + " AND COUNT_DATE <= TO_DATE('" + eDate + "', 'YYYY-MM-DD HH24:MI:SS')"
			            + " GROUP BY COUNT_DAY, TERMINAL_ID , COL_NO" 
			            + " ORDER BY COUNT_DAY, TERMINAL_ID, COL_NO"
			        + " ) C "
				        + "INNER JOIN TB_VENDING_MACHINE S"
			        		+ " ON C.TERMINAL_ID = S.TERMINAL_ID"
			        			+ " AND S.COMPANY_SEQ = " + company
			     + " ) A"
					+ " INNER JOIN TB_ORGANIZATION B"
						+ " ON A.ORGANIZATION_SEQ = B.SEQ"
				    + " INNER JOIN TB_VENDING_MACHINE_PLACE C"
				    	+ " ON A.VM_SEQ = C.VM_SEQ"
				    		+ " AND C.END_DATE IS NULL"
			     + " ORDER BY PARENT_ORGAN, ORGAN, PLACE, A.TERMINAL_ID, COL_NO, A.END_DATE DESC"   	
			);	
			rs = ps.executeQuery();

			int n = 0;
			GeneralConfig c = null;
			String startDate = null;
			String endDate = null;
			
			while (rs.next()) {

				c = new GeneralConfig();
				
				n = 0;

				String yyyymmdd = rs.getString("COUNT_DAY");
				c.put("YYYYMMDD", yyyymmdd);
				c.put("DATE", yyyymmdd.substring(0, 4) + "-" + yyyymmdd.substring(4, 6) + "-" + yyyymmdd.substring(6, 8));

				c.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("VM_PLACE_SEQ", rs.getLong("VM_PLACE_SEQ"));
				c.put("PARENT_ORGAN", rs.getString("PARENT_ORGAN"));
				c.put("ORGAN", rs.getString("ORGAN"));

				c.put("PLACE", rs.getString("PLACE"));							

				c.put("VM_CODE", rs.getString("VM_CODE"));
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("COL_NO", rs.getLong("COL_NO"));
				c.put("PRODUCT_NAME", rs.getString("PRODUCT_NAME"));
				c.put("PRODUCT_PRICE", rs.getLong("PRODUCT_PRICE"));
				c.put("PRICE", rs.getLong("PRICE"));
				c.put("PRODUCT_CODE", rs.getString("GOODS_CODE"));
				c.put("MEMO", rs.getString("MEMO"));	//scheo 20191223 판매계수 실시간집계
				
				startDate = null;
				endDate = null;	
				
				
				if ((n == 0) || !c.get("START_DATE_" + n).equals("-")) n++;
				c.put("PAY_COUNT", n);
				
				String date = rs.getString("START_DATE");
				c.put("START_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				if ((startDate == null) || (startDate.compareTo(date) > 0)) {
					if (startDate != null) System.out.println("START_DATE : " + startDate + "-->" + date);
					c.put("START_DATE", c.get("START_DATE_" + n));
					startDate = date;
				}

				date = rs.getString("END_DATE");
				c.put("END_DATE_" + n,
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				if ((endDate == null) || (endDate.compareTo(date) < 0)) {
					if (endDate != null) System.out.println("END_DATE : " + endDate + "-->" + date);
					c.put("END_DATE", c.get("END_DATE_" + n));
					endDate = date;
				}
				
				/*
				//전체
				long totalCount = rs.getLong("TOTAL_COUNT");
				long totalAmount = rs.getLong("TOTAL_AMOUNT");
				c.put("CNT_TOTAL", c.getLong("CNT_TOTAL") + totalCount);
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_TOTAL") + totalAmount);
				*/
				
				//신용
				long countCard =  rs.getLong("CARD_COUNT");
				long amountCard = rs.getLong("CARD_AMOUNT");
				c.put("CNT_CARD", c.getLong("CNT_CARD") + countCard);
				c.put("AMOUNT_CARD", c.getLong("AMOUNT_CARD") + amountCard);
				grandTotalCounts[1] += countCard;
				grandTotalAmounts[1] += amountCard;
				
				//현금
				long countCash = rs.getLong("CASH_COUNT");
				long amountCash = rs.getLong("CASH_AMOUNT");
				c.put("CNT_CASH", c.getLong("CNT_CASH") + countCash);
				c.put("AMOUNT_CASH", c.getLong("AMOUNT_CASH") + amountCash);
				grandTotalCounts[0] += countCash;
				grandTotalAmounts[0] += amountCash;
				
				//전체
				long totalCount = countCard + countCash;
				long totalAmount =amountCard + amountCash ;
				c.put("CNT_TOTAL", c.getLong("CNT_TOTAL") + totalCount);
				c.put("AMOUNT_TOTAL", c.getLong("AMOUNT_TOTAL") + totalAmount);
				
				this.list.add(c);
				
			}
			this.data.put("CNT_CASH", grandTotalCounts[0]);
			this.data.put("AMOUNT_CASH", grandTotalAmounts[0]);
			
			this.data.put("CNT_CARD", grandTotalCounts[1]);
			this.data.put("AMOUNT_CARD", grandTotalAmounts[1]);
			
			
			this.data.put("CNT_TOTAL", grandTotalCounts[0] + grandTotalCounts[1]);
			this.data.put("AMOUNT_TOTAL", grandTotalAmounts[0] + grandTotalAmounts[1]);
			
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}
		
		// 조직명
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
						+ " FROM ("
								+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
										+ " NAME"
									+ " FROM TB_ORGANIZATION"
									+ " WHERE COMPANY_SEQ = " + company
										+ " AND SORT = 0"
									+ " ORDER BY DEPTH DESC"
							+ " )"
						+ " WHERE ROWNUM = 1"
					)
			);
		String sDesc = null;
		// 검색 설정
		//sDesc = "집계기준="
		//		+ dbLib.getResult(conn,
		//				"SELECT /*+ INDEX(TB_CODE) */"
		//						+ " NAME"
		//					+ " FROM TB_CODE"
		//					+ " WHERE TYPE = 'SUM_TYPE'"
		//						+ " AND CODE = '" + sType + "'"
		//			)
		//		+ "&집계기간=" + sDate + "-" + eDate
		//	;

		if (company > 0) {
			sDesc += "&소속="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_COMPANY) */"
									+ " NAME"
								+ " FROM TB_COMPANY"
								+ " WHERE SEQ = " + company
						)
				;
		}

		if (organ > 0) {
			sDesc += "&조직="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE PARENT_SEQ = 0"
								+ " START WITH SEQ = " + organ
								+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
						)
				;
		}

		if (place > 0) {
			sDesc += "&설치위치="
					+ dbLib.getResult(conn,
							"SELECT /*+ INDEX(TB_VENDING_MACHINE_PLACE) */"
									+ " PLACE"
								+ " FROM TB_VENDING_MACHINE_PLACE"
								+ " WHERE SEQ = " + place
						)
				;
		}

		this.data.put("sDesc", sDesc);
		
		// 리소스 반환
		dbLib.close(conn);
		
		return error;
		
	}		
	/**
	 * 판매계수상세
	 *
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param sDate 검색일
	 * @param eDate 종료일
	 * @param oMode 정렬필드
	 * @param oType 정렬방법
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String salesCountRealTime(long company, long organ, long depth, long place, String sDate, String eDate, String terminal, int pageNo) throws Exception {
	// 필수 검색 조건이 없을 때
		if (company == 0 || StringEx.isEmpty(sDate) || StringEx.isEmpty(eDate)) {
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
		
	// 검색절 생성
		String WHERE = "";		
		company = (company > 0 ? company : this.cfg.getLong("user.company"));
		WHERE += " AND A.COMPANY_SEQ = " + company
			+ " AND A.ORGANIZATION_SEQ = " + organ
			+ " AND VM_PLACE_SEQ = " + place
		;
		
	//망상취소 제외	
		WHERE += " AND PAY_STEP <> '00'";
	// 총 레코드수
		this.records = StringEx.str2long(
			dbLib.getResult(conn,
					"SELECT /*+ INDEX(A IX_SALES_TRANSACTION_DATE) */"
							+ " COUNT(*)"
						+ " FROM TB_SALES A"
						+ " WHERE  "
								+ " TRANSACTION_DATE BETWEEN  TO_CHAR(TO_DATE('" + sDate + "', 'YYYY-MM-DD HH24:MI:SS'), 'YYYYMMDD')"
								     + " AND TO_CHAR(TO_DATE('" + eDate + "', 'YYYY-MM-DD HH24:MI:SS'), 'YYYYMMDD')" 
								+ " AND TRANSACTION_DATE || TRANSACTION_TIME  BETWEEN  TO_CHAR(TO_DATE('" + sDate + "', 'YYYY-MM-DD HH24:MI:SS'), 'YYYYMMDDHH24MISS')"
									 + " AND TO_CHAR(TO_DATE('" + eDate + "', 'YYYY-MM-DD HH24:MI:SS'), 'YYYYMMDDHH24MISS')"      
							+ WHERE
					)
			);

	// 총 페이지수
		this.pages = Pager.getSize(this.records, cfg.getInt("limit.list"));

	// 리스트 가상번호
		long no = this.records - (pageNo - 1) * cfg.getInt("limit.list");

	// 목록 가져오기
	//salesCountList
		this.list = new ArrayList<GeneralConfig>();
		this.data = new GeneralConfig();	
		
		
		try {
			int s = pageNo>0?(((pageNo - 1) * cfg.getInt("limit.list")) + 1):1;
			int e = pageNo>0?((s - 1) + cfg.getInt("limit.list")):(int)this.records;

			ps = dbLib.prepareStatement(conn,
				"SELECT /*+ ORDERED USE_NL(B C D E F G H I) */"
						+ " A.TRANSACTION_DATE || A.TRANSACTION_TIME AS TRANSACTION_DATE,"
						+ " CASE WHEN A.CLOSING_DATE IS NOT NULL THEN A.CLOSING_DATE || NVL(A.CLOSING_TIME, '000000') END AS CLOSING_DATE,"
						+ " A.PURCHASE_DATE,"
						+ " CASE WHEN A.PAY_STEP = '02' THEN A.PAY_DATE_EXP ELSE A.PAY_DATE END AS PAY_DATE,"
						+ " CASE WHEN A.CANCEL_DATE IS NOT NULL THEN A.CANCEL_DATE || NVL(A.CANCEL_TIME, '000000') END AS CANCEL_DATE,"
						+ " A.TERMINAL_ID,"
						+ " A.TRANSACTION_NO,"
						+ " A.ITEM_COUNT," //2019-06-19 김태우 추가  exp) 구매 품목 갯수	
						+ " A.AMOUNT,"
						+ " A.COL_NO,"	//20200715 scheo 컬럼추가
						+ " B.NAME AS ORGAN,"
						+ " C.PLACE,"
						+ " NVL(D.NAME, '미등록 상품[' || A.COL_NO || ']') AS PRODUCT," //scheo 20181214 유광권부장 요청 - 20190619 scheo 원복
						//+ " case when D.name is null then case when length(A.col_no) = '2' then '미등록 상품[' || '00' || A.col_no || ']' when length(A.col_no) = '1' then '미등록 상품[' || '000' || A.col_no || ']' when MOD(A.col_no,100) || TRUNC(A.col_no/100) <= '999' then '미등록 상품[' || '0' || MOD(A.col_no,100) || TRUNC(A.col_no/100) || ']' else '미등록 상품[' || MOD(A.col_no,100) || TRUNC(A.col_no/100) || ']' end else D.name end as PRODUCT ,"
						+ " NVL(D.PRICE, 0) AS PRICE, "
						+ " E.CODE AS VM_CODE,"
						+ " F.NAME AS PAY_TYPE,"
						+ " G.NAME AS PAY_STEP,"
						+ " H.NAME AS INPUT_TYPE,"
						+ " I.NAME AS PAY_CARD"
					+ " FROM ("
								+ " SELECT"
										+ " ROWNUM AS ROW_NUM,"
										+ " A.*"
									+ " FROM ("
											+ " SELECT /*+ INDEX(A IX_SALES_TRANSACTION_DATE) */"
													+ " *"
												+ " FROM TB_SALES A"
												+ " WHERE  "
													+ " TRANSACTION_DATE BETWEEN  TO_CHAR(TO_DATE('" + sDate + "', 'YYYY-MM-DD HH24:MI:SS'), 'YYYYMMDD')"
													     + " AND TO_CHAR(TO_DATE('" + eDate + "', 'YYYY-MM-DD HH24:MI:SS'), 'YYYYMMDD')" 
													+ " AND TRANSACTION_DATE || TRANSACTION_TIME  BETWEEN  TO_CHAR(TO_DATE('" + sDate + "', 'YYYY-MM-DD HH24:MI:SS'), 'YYYYMMDDHH24MISS')"
														 + " AND TO_CHAR(TO_DATE('" + eDate + "', 'YYYY-MM-DD HH24:MI:SS'), 'YYYYMMDDHH24MISS')"   
													+ WHERE
												+ " ORDER BY A.TRANSACTION_DATE DESC, A.TRANSACTION_TIME DESC"
										+ " ) A"
									+ " WHERE ROWNUM <= " + e
							+ ") A"
						+ " INNER JOIN TB_ORGANIZATION B"
							+ " ON A.ORGANIZATION_SEQ = B.SEQ"
						+ " INNER JOIN TB_VENDING_MACHINE_PLACE C"
							+ " ON A.VM_PLACE_SEQ = C.SEQ"
						+ " LEFT JOIN TB_PRODUCT D"
							+ " ON A.COMPANY_SEQ = D.COMPANY_SEQ"
								+ " AND A.PRODUCT_CODE = D.CODE"
						+ " LEFT JOIN TB_VENDING_MACHINE E"
							+ " ON A.TERMINAL_ID = E.TERMINAL_ID"
							//+ " ON C.VM_SEQ = E.SEQ
						+ " LEFT JOIN TB_CODE F"
							+ " ON A.PAY_TYPE = F.CODE"
								+ " AND F.TYPE = 'PAY_TYPE'"
						+ " LEFT JOIN TB_CODE G"
							+ " ON A.PAY_STEP = G.CODE"
								+ " AND G.TYPE = 'PAY_STEP'"
						+ " LEFT JOIN TB_CODE H"
							+ " ON A.INPUT_TYPE = H.CODE"
								+ " AND H.TYPE = 'INPUT_TYPE'"
						+ " LEFT JOIN TB_CODE I"
							+ " ON A.PAY_CARD = I.CODE"
								+ " AND I.TYPE = 'PAY_CARD'"
					+ " WHERE A.ROW_NUM >= " + s
			);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				String date = rs.getString("TRANSACTION_DATE");
				c.put("TRANSACTION_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				date = rs.getString("PURCHASE_DATE");
				c.put("PURCHASE_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8)
							: ""
					);
				date = rs.getString("PAY_DATE");
				c.put("PAY_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8)
							: ""
					);
				date = rs.getString("CLOSING_DATE");
				if ((date != null) && (date.length() < 14)) System.out.println(date);
				c.put("CLOSING_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				date = rs.getString("CANCEL_DATE");
				c.put("CANCEL_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				c.put("TERMINAL_ID", rs.getString("TERMINAL_ID"));
				c.put("TRANSACTION_NO", rs.getString("TRANSACTION_NO"));
				c.put("AMOUNT", rs.getLong("AMOUNT"));
				c.put("ORGAN", rs.getString("ORGAN"));
				c.put("PLACE", rs.getString("PLACE"));
				c.put("VMCODE", rs.getString("VM_CODE"));
				c.put("COL_NO", rs.getString("COL_NO"));	//20200715 scheo 컬럼추가
				c.put("PRODUCT", rs.getString("PRODUCT"));
				c.put("PRICE", rs.getLong("PRICE"));
				c.put("PAY_TYPE", rs.getString("PAY_TYPE"));
				c.put("PAY_STEP", rs.getString("PAY_STEP"));
				c.put("INPUT_TYPE", rs.getString("INPUT_TYPE"));
				c.put("PAY_CARD", rs.getString("PAY_CARD"));
				c.put("ITEM_COUNT", rs.getLong("ITEM_COUNT")); //2019-06-19 김태우 추가  exp) 구매 품목 갯수				
				c.put("ITEM_COUNT_minus", rs.getLong("ITEM_COUNT")-1);//2019-06-19 김태우 추가  exp) 구매 품목 외 n 건을 위한 -1 
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

	// 조직명
		this.data.put("ORGAN",
				dbLib.getResult(conn,
						"SELECT NAME"
							+ " FROM ("
									+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
											+ " NAME"
										+ " FROM TB_ORGANIZATION"
										+ " WHERE COMPANY_SEQ = " + company
											+ " AND SORT = 0"
										+ " ORDER BY DEPTH DESC"
								+ ")"
							+ " WHERE ROWNUM = 1"
					)
			);

	// 마감 출력 여부, 2011.06.11, 정원광
		this.data.put("IS_VIEW_CLOSING",
				dbLib.getResult(conn,
						"SELECT /*+ INDEX(TB_COMPANY) */"
								+ " IS_VIEW_CLOSING"
							+ " FROM TB_COMPANY"
							+ " WHERE SEQ = " + company
					)
			);		
		
	// 리소스 반환
		dbLib.close(conn);
		
		return error;
		
	}			
		
	/**
	 * 입금 예정일과 입금일이 다른 판매 내역
	 *
	 * @param company 소속
	 * @param organ 조직
	 * @param place 위치
	 * @param aDate 입금일
	 * @param cDate 마감일
	 * @param card 선불 카드사
	 * @param sField 검색 필드
	 * @param sQuery 검색어
	 * @param pageNo 페이지
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String difference(long company, long organ, long place, String aDate, String cDate, String card, String sField, String sQuery, int pageNo) throws Exception {
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
		String WHERE = " A.PAY_STEP IN ('02', '03', '21', '22')"
				+ " AND A.COMPANY_SEQ = " + company
				+ " AND A.ORGANIZATION_SEQ = " + organ
				+ " AND A.VM_PLACE_SEQ = " + place
				+ " AND A.CLOSING_DATE = '" + cDate + "'"
			;

	//20160211 조회 권한체크 추가
		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
			WHERE += " AND A.USER_SEQ = " + this.cfg.getLong("user.seq");
		} else {
			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
				WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			}

			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
				
				
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
								+ " START WITH COMPANY_SEQ = " + this.cfg.getLong("user.company")
									+ " AND SEQ IN ("
											+ " SELECT " + this.cfg.getLong("user.organ") + " FROM DUAL"
											+ " UNION"
											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
													+ " ORGANIZATION_SEQ"
												+ " FROM TB_USER_APP_ORGAN"
												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
										+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
				
				
					;
			}
		}

		if (StringEx.isEmpty(card)) { // 신용
			WHERE += " AND A.PAY_TYPE = '01'";
		} else { // 선불
			WHERE += " AND A.PAY_TYPE = '11' AND A.PAY_CARD = '" + card + "'";
		}

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) {
			if (StringEx.inArray(sField, "A.TRANSACTION_DATE;A.CLOSING_DATE".split(";"))) {
				WHERE += " AND " + sField + " = '" + sQuery + "'";
			} else {
				WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
			}
		}

		WHERE += " AND ("
					+ " (A.PAY_STEP IN ('02') AND A.PAY_DATE_EXP = '" + aDate + "')"
					+ " OR (A.PAY_STEP IN ('03') AND A.PAY_DATE != A.PAY_DATE_EXP)"
					+ " OR A.PAY_STEP IN ('21', '22')"
				+ " )"
			;

	// 총 레코드수
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT /*+ INDEX(A) */"
								+ " COUNT(*)"
							+ " FROM TB_SALES A"
							+ " WHERE" + WHERE
					)
			);

	// 총 페이지수
		this.pages = Pager.getSize(this.records, cfg.getInt("limit.list"));

	// 리스트 가상번호
		long no = this.records - (pageNo - 1) * cfg.getInt("limit.list");

	// 판매 내역
		this.list = new ArrayList<GeneralConfig>();

		try {
			int s = ((pageNo - 1) * cfg.getInt("limit.list")) + 1;
			int e = (s - 1) + cfg.getInt("limit.list");

			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED USE_NL(B C) */"
							+ " A.TRANSACTION_DATE || A.TRANSACTION_TIME AS TRANSACTION_DATE,"
							+ " CASE WHEN A.CLOSING_DATE IS NULL THEN NULL ELSE A.CLOSING_DATE || NVL(A.CLOSING_TIME, '000000') END AS CLOSING_DATE,"
							+ " A.AMOUNT,"
							+ " A.COMMISSION,"
							+ " A.OUTAMOUNT,"
							+ " CASE WHEN A.PAY_TYPE = '11' THEN C.NAME ELSE B.NAME END AS PAY_TYPE,"
							+ " CASE WHEN A.PAY_STEP = '02' AND A.PAY_DATE_EXP = '" + aDate + "' THEN '입금누락'"
									+ " WHEN A.PAY_STEP = '03' AND TO_DATE(A.PAY_DATE, 'YYYYMMDD') > TO_DATE(A.PAY_DATE_EXP, 'YYYYMMDD') THEN '후입금'"
									+ " WHEN A.PAY_STEP = '03' AND TO_DATE(A.PAY_DATE, 'YYYYMMDD') < TO_DATE(A.PAY_DATE_EXP, 'YYYYMMDD') THEN '선입금'"
									+ " WHEN A.PAY_STEP = '21' THEN '매입보류'"
									+ " WHEN A.PAY_STEP = '22' THEN '매입거절'"
									+ " ELSE '-'"
								+ " END AS REASON"
						+ " FROM ("
									+ " SELECT"
											+ " ROWNUM AS ROW_NUM,"
											+ " A.*"
										+ " FROM ("
												+ " SELECT /*+ INDEX(A) */"
														+ " *"
													+ " FROM TB_SALES A"
													+ " WHERE" + WHERE
													+ " ORDER BY A.TRANSACTION_DATE DESC, A.TRANSACTION_TIME DESC"
											+ " ) A"
										+ " WHERE ROWNUM <= " + e
								+ " ) A"
							+ " LEFT JOIN TB_CODE B"
								+ " ON A.PAY_TYPE = B.CODE"
									+ " AND B.TYPE = 'PAY_TYPE'"
							+ " LEFT JOIN TB_CODE C"
								+ " ON A.PAY_CARD = C.CODE"
									+ " AND C.TYPE = 'PAY_CARD'"
						+ " WHERE ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				String date = rs.getString("TRANSACTION_DATE");
				c.put("TRANSACTION_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				date = rs.getString("CLOSING_DATE");
				c.put("CLOSING_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: ""
					);
				c.put("AMOUNT", rs.getLong("AMOUNT"));
				c.put("COMMISSION", rs.getLong("COMMISSION"));
				c.put("OUTAMOUNT", rs.getLong("OUTAMOUNT"));
				c.put("PAY_TYPE", rs.getString("PAY_TYPE"));
				c.put("REASON", rs.getString("REASON"));
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
	 * 월 정산 리포트
	 *
	 * @param sType 정산 유형
	 * @param sDate 날짜
	 * @return 에러가 있을 경우 에러 내용
	 *
	 */
	public String account(String sType, String sDate, long company, long organ) throws Exception {
	// 필수 검색 조건이 없을 때
		if (StringEx.isEmpty(sType) || StringEx.isEmpty(sDate)) {
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
			return "DB 연결에 실패하였습니다.";
		}

	// 검색절 생성
		String WHERE = " A.YYYYMM = '" + sDate + "'"
				+ " AND A.TYPE = '" + sType + "'";

		if (this.cfg.get("user.operator").equals("Y")) { // 자판기 운영자
			WHERE += " AND A.USER_SEQ = " + this.cfg.getLong("user.seq");
		} else {
			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
				WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			}

			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색 (내가 속한 조직 및 내 하위 조직 AND 매출 조회가 가능한 조직에 등록된 조직)
				WHERE += " AND A.ORGANIZATION_SEQ IN ("
							+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
									+ " SEQ"
								+ " FROM TB_ORGANIZATION"
								+ " WHERE SORT = 1"
								+ " START WITH COMPANY_SEQ = " + this.cfg.getLong("user.company")
									+ " AND SEQ IN ("
											+ " SELECT " + this.cfg.getLong("user.organ") + " FROM DUAL"
											+ " UNION"
											+ " SELECT /*+ INDEX(TB_USER_APP_ORGAN) */"
													+ " ORGANIZATION_SEQ"
												+ " FROM TB_USER_APP_ORGAN"
												+ " WHERE SEQ = " + this.cfg.getLong("user.seq")
										+ " )"
								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
						+ " )"
					;
			}
		}

		if (company > 0) { // 소속
			WHERE += " AND A.COMPANY_SEQ = " + company;
		}

		if (organ > 0) { // 조직
			WHERE += " AND A.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION"
							+ " WHERE SORT = 1"
							+ " START WITH SEQ = " + organ
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;
		}

	// 선불 카드사
		this.company = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(TB_CODE) */"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CODE"
						+ " WHERE TYPE = 'PAY_CARD'"
							+ " AND CODE <> '000'"
						+ " ORDER BY CODE"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("CODE", rs.getString("CODE"));
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

	// 선불
		this.prepay = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED INDEX(A IX_SALES_MONTHLY_YYYYMM) USE_HASH(B) */"
							+ " B.NAME AS COMPANY,"
							+ " ("
									+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
											+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
										+ " FROM TB_ORGANIZATION"
										+ " WHERE PARENT_SEQ = 0"
										+ " START WITH SEQ = A.ORGANIZATION_SEQ"
										+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
								+ " ) AS ORGAN,"
							+ " A.ORGANIZATION_SEQ,"
							+ " A.PAY_CARD,"
							+ " NVL(SUM(A.CNT), 0) AS CNT,"
							+ " NVL(SUM(A.AMOUNT), 0) AS AMOUNT,"
							+ " NVL(SUM(A.COMMISSION), 0) AS COMMISSION,"
							+ " NVL(SUM(A.OUTAMOUNT), 0) AS OUTAMOUNT"
						+ " FROM TB_SALES_MONTHLY A"
							+ " INNER JOIN TB_COMPANY B"
								+ " ON A.COMPANY_SEQ = B.SEQ"
						+ " WHERE" + WHERE
							+ " AND A.PAY_TYPE IN ('11')"
						+ " GROUP BY B.NAME, A.ORGANIZATION_SEQ, A.PAY_CARD"
						+ " ORDER BY 1, 2"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("CNT", rs.getLong("CNT"));
				c.put("AMOUNT", rs.getLong("AMOUNT"));
				c.put("COMMISSION", rs.getLong("COMMISSION"));

				c.put("OUTAMOUNT", rs.getLong("OUTAMOUNT"));

				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("PAY_CARD", rs.getString("PAY_CARD"));
				c.put("ORGAN", (this.cfg.getLong("user.company") > 0 ? "" : rs.getString("COMPANY") + "/") + rs.getString("ORGAN"));

				this.prepay.add(c);
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

	// 카드 (선불 + 신용)
		this.card = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED INDEX(A IX_SALES_MONTHLY_YYYYMM) USE_HASH(B) */"
							+ " B.NAME AS COMPANY,"
							+ " ("
										+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
												+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
											+ " FROM TB_ORGANIZATION"
											+ " WHERE PARENT_SEQ = 0"
											+ " START WITH SEQ = A.ORGANIZATION_SEQ"
											+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
									+ " ) AS ORGAN,"
							+ " A.ORGANIZATION_SEQ,"
							+ " NVL(SUM(A.CNT), 0) AS CNT,"
							+ " NVL(SUM(A.AMOUNT), 0) AS AMOUNT,"
							+ " NVL(SUM(A.COMMISSION), 0) AS COMMISSION,"
							+ " NVL(SUM(A.OUTAMOUNT), 0) AS OUTAMOUNT"
				+ " FROM TB_SALES_MONTHLY A"
					+ " INNER JOIN TB_COMPANY B"
						+ " ON A.COMPANY_SEQ = B.SEQ"
				+ " WHERE" + WHERE
					+ " AND A.PAY_TYPE IN ('01', '11')"
				+ " GROUP BY B.NAME, A.ORGANIZATION_SEQ"
				+ " ORDER BY 1, 2"
			);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("ORGAN", (this.cfg.getLong("user.company") > 0 ? "" : rs.getString("COMPANY") + "/") + rs.getString("ORGAN"));

				c.put("CNT", rs.getLong("CNT"));
				c.put("AMOUNT", rs.getLong("AMOUNT"));
				c.put("COMMISSION", rs.getLong("COMMISSION"));
				c.put("OUTAMOUNT", rs.getLong("OUTAMOUNT"));

				this.card.add(c);
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

	// 전체
		this.list = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED INDEX(A IX_SALES_MONTHLY_YYYYMM) USE_HASH(B) */"
							+ " B.NAME AS COMPANY,"
							+ " ("
									+ " SELECT /*+ INDEX(TB_ORGANIZATION) */"
											+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
										+ " FROM TB_ORGANIZATION"
										+ " WHERE PARENT_SEQ = 0"
										+ " START WITH SEQ = A.ORGANIZATION_SEQ"
										+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
								+ " ) AS ORGAN,"
							+ " A.ORGANIZATION_SEQ,"
							+ " A.PAY_TYPE,"
							+ " NVL(SUM(A.CNT), 0) AS CNT,"
							+ " NVL(SUM(A.AMOUNT), 0) AS AMOUNT,"
							+ " NVL(SUM(A.COMMISSION), 0) AS COMMISSION,"
							+ " NVL(SUM(A.OUTAMOUNT), 0) AS OUTAMOUNT"
					+ " FROM TB_SALES_MONTHLY A"
						+ " INNER JOIN TB_COMPANY B"
							+ " ON A.COMPANY_SEQ = B.SEQ"
					+ " WHERE" + WHERE
						+ " AND A.PAY_TYPE IN ('01', '11')"
					+ " GROUP BY B.NAME, A.ORGANIZATION_SEQ, A.PAY_TYPE"
					+ " ORDER BY 1, 2"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				c.put("PAY_TYPE", rs.getString("PAY_TYPE"));
				c.put("ORGAN", (this.cfg.getLong("user.company") > 0 ? "" : rs.getString("COMPANY") + "/") + rs.getString("ORGAN"));

				c.put("CNT", rs.getLong("CNT"));
				c.put("AMOUNT", rs.getLong("AMOUNT"));
				c.put("COMMISSION", rs.getLong("COMMISSION"));
				c.put("OUTAMOUNT", rs.getLong("OUTAMOUNT"));

				this.list.add(c);
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
}