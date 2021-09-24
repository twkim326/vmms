package com.nucco.beans;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Goods.java
 *
 * 판매 상품
 *
 * 작성일 - 2011/03/31, 정원광
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

public class Product {
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
 * 골급자
 *
 */
	public ArrayList<GeneralConfig> vendor;	
/**
 * 그룹
 *
 */
	public ArrayList<GeneralConfig> category;
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
	public Product(GlobalConfig cfg) throws Exception {
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
 * @param vendor 공급자 
 * @param category 그룹
 * @param pageNo 페이지
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getList(long company, long vendor, long category, int pageNo, String sField, String sQuery) throws Exception {
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

		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}
		
		// 공급자

		this.vendor = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */ "
							+ " SEQ,"
							+ " NAME"
						+ " FROM TB_VENDOR A"
						+ " ORDER BY NAME"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("NAME", rs.getString("NAME"));

				this.vendor.add(c);
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
		
	// 선택된 그룹
		this.data = new GeneralConfig();

		if (category > 0) {
			try {
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " DEPTH,"
								+ " PARENT_SEQ"
							+ " FROM TB_CATEGORY A"
							+ " WHERE SEQ = ?"
					);
				ps.setLong(1, category);
				rs = ps.executeQuery();

				if (rs.next()) {
					this.data.put("DEPTH", rs.getInt("DEPTH"));
					this.data.put("PARENT_SEQ", rs.getLong("PARENT_SEQ"));
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

	// 검색절 생성
		String WHERE = "";

		if (user_company > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속 및 허가된 상품 그룹군에 있는 상품만 검색
			WHERE += " AND (A.CATEGORY_SEQ, A.COMPANY_SEQ) IN ("
						+ " SELECT /*+ INDEX(A_A) */"
								+ " SEQ, COMPANY_SEQ"
							+ " FROM TB_CATEGORY A_A"
							+ " WHERE COMPANY_SEQ = " + user_company
					+ " )"
				;
		}

		if (company > 0) {
			WHERE += " AND A.COMPANY_SEQ IN ("
						+ " SELECT /*+ INDEX(A_C) */"
								+ " SEQ"
							+ " FROM TB_COMPANY A_C"
							+ " WHERE SEQ = " + company
					+ " )"
				;
		}
		
		if (vendor > 0) {
			WHERE += " AND A.VENDOR_SEQ = " + vendor;
		}

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

	// 총 레코드수
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT /*+ ORDERED" + (WHERE.length() > 0 ? " INDEX(A)" : "") + " USE_HASH(BB) */"
								+ " COUNT(*)"
							+ " FROM TB_PRODUCT A"
								+ " INNER JOIN ("
										+ " SELECT" + (category > 0 ? " /*+ INDEX(B) */" : "")
												+ " *"
											+ " FROM TB_CATEGORY B"
											+ " WHERE 1=1 "
								+ (category > 0
										?	  " START WITH SEQ = " + category
											+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
										: ""
									)
									+ " ) BB"
									+ " ON A.CATEGORY_SEQ = BB.SEQ"
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
							+ " SEQ,"
							+ " CODE,"
							+ " NAME,"
							+ " PRICE,"
							+ " BAR_CODE,"
							+ " MEMO,"
							+ " TO_CHAR(CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE,"
							+ " TO_CHAR(MODIFY_DATE, 'YYYY-MM-DD') AS MODIFY_DATE,"
							+ " GROUP_0,"
							+ " GROUP_1,"
							+ " COMPANY_SEQ,"
							+ " COMPANY, "
							+ " VENDOR_SEQ,"
							+ " VENDOR"
						+ " FROM ("
								+ " SELECT"
										+ " ROWNUM AS ROW_NUM,"
										+ " AA.*"
									+ " FROM ("
											+ " SELECT /*+ ORDERED" + (WHERE.length() > 0 ? " INDEX(A)" : "") + " USE_HASH(BB C) */"
													+ " C.NAME AS COMPANY,"
													+ " D.NAME AS VENDOR,"
													+ " CASE WHEN BB.DEPTH = 0 THEN BB.NAME"
														+ " ELSE ("
																+ " SELECT /*+ INDEX(D) */ NAME"
																	+ " FROM TB_CATEGORY D"
																	+ " WHERE SEQ = BB.PARENT_SEQ"
															+ " )"
														+ " END AS GROUP_0,"
													+ " CASE WHEN BB.DEPTH = 0 THEN '' ELSE BB.NAME END AS GROUP_1,"
													+ " A.NAME,"
													+ " A.PRICE,"
													+ " A.BAR_CODE,"
													+ " A.SEQ,"
													+ " A.CODE,"
													+ " A.MEMO,"
													+ " A.CREATE_DATE,"
													+ " A.MODIFY_DATE,"
													+ " A.COMPANY_SEQ, "
													+ " A.VENDOR_SEQ"
												+ " FROM TB_PRODUCT A"
													+ " INNER JOIN ("
															+ " SELECT" + (category > 0 ? " /*+ INDEX(B) */" : "")
																	+ " *"
																+ " FROM TB_CATEGORY B"
																+ " WHERE 1=1"
													+ (category > 0
															?	  " START WITH SEQ = " + category
																+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
															: ""
														)
														+ " ) BB"
														+ " ON A.CATEGORY_SEQ = BB.SEQ"
													+ " LEFT JOIN TB_COMPANY C"
														+ " ON A.COMPANY_SEQ = C.SEQ"
													+ " LEFT JOIN TB_VENDOR D"
														+ " ON A.VENDOR_SEQ = D.SEQ"
													+ WHERE
												+ " ORDER BY " + (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery) ? sField : "1, 2, 3, 4, 5")
										+ " ) AA"
									+ " WHERE ROWNUM <= " + e
							+ " )"
						+ " WHERE ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("CODE", rs.getString("CODE"));
				c.put("NAME", rs.getString("NAME"));
				c.put("PRICE", rs.getString("PRICE"));
				c.put("BAR_CODE", rs.getString("BAR_CODE"));
				c.put("MEMO", rs.getString("MEMO"));
				c.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				c.put("MODIFY_DATE", rs.getString("MODIFY_DATE"));
				c.put("GROUP_0", rs.getString("GROUP_0"));
				c.put("GROUP_1", rs.getString("GROUP_1"));
				c.put("COMPANY_SEQ", rs.getString("COMPANY_SEQ"));
				c.put("COMPANY", rs.getString("COMPANY"));
				c.put("VENDOR_SEQ", rs.getString("VENDOR_SEQ"));
				c.put("VENDOR", rs.getString("VENDOR"));
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
 * 공급자
 *
 * @param company 소속
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String vendor(long company) throws Exception {
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String error = null;

		if (company <= 0) return error;
		
	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}

	// 검색절 생성
		String WHERE = "";


		if (company > 0) {
			WHERE += "WHERE P.COMPANY_SEQ = " + company;
		} 

	// 목록
		this.category = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(P)*/ " 
							+ " P.VENDOR_SEQ SEQ,"
							+ " V.NAME NAME "
						+ " FROM TB_PRODUCT P "
							+ " INNER JOIN TB_VENDOR V  "
								+ " ON P.VENDOR_SEQ = V.SEQ  "
						+ WHERE
						+ " GROUP BY P.VENDOR_SEQ, V.NAME "
						+ " ORDER BY P.VENDOR_SEQ, V.NAME "
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("NAME", rs.getString("NAME"));

				this.category.add(c);
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
 * 그룹
 *
 * @param company 소속
 * @param category 상위 그룹
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String category(long company, long category) throws Exception {
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

		if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속에 대한 정보만 검색
			company = this.cfg.getLong("user.company");
		}

		if (company > 0) {

			WHERE += " AND SEQ IN ("
						+ " SELECT /*+ INDEX(A_A) */"
								+ " SEQ"
							+ " FROM TB_CATEGORY A_A"
							+ " WHERE COMPANY_SEQ = " + company
								+ " AND DEPTH = " + (category == 0 ? 0 : 1)
					+ ")"
				;
		} else {
			HINT = " /*+ INDEX(A) */";
			WHERE += " AND COMPANY_SEQ = 0"
					+ " AND DEPTH = " + (category == 0 ? 0 : 1)
				;
		}

		if (category > 0) {
			HINT = " /*+ INDEX(A) */";
			WHERE += " AND PARENT_SEQ = " + category;
		}

	// 목록
		this.category = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT" + HINT
							+ " SEQ,"
							+ " CODE,"
							+ " NAME"
						+ " FROM TB_CATEGORY A"
						+ " WHERE 1=1 "
							+ WHERE
						+ " ORDER BY NAME"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("CODE", rs.getString("CODE"));
				c.put("NAME", rs.getString("NAME"));

				this.category.add(c);
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
		
		// 공급자

		this.vendor = new ArrayList<GeneralConfig>();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */ "
							+ " SEQ,"
							+ " NAME"
						+ " FROM TB_VENDOR A"
						+ " ORDER BY NAME"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("NAME", rs.getString("NAME"));

				this.vendor.add(c);
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
 * 개별 등록/수정
 *
 * @param seq 등록번호
 * @param company 소속
 * @param vendor 공급자 
 * @param category 그룹
 * @param code 코드
 * @param name 이름
 * @param price 단가
 * @param barcode 바코드
 * @param memo 설명
 * @param isAuto 코드 자동생성 여부
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(long seq, long company, long vendor,  long category, String code, String name, long price, String barcode, String memo, boolean isAuto) throws Exception {
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
		if (isAuto) {
			code = dbLib.getResult(conn,
					"SELECT /*+ INDEX(A) */"
							+ " LPAD(TO_CHAR(NVL(MAX(TO_NUMBER(CODE)), 0) + 1), 8, '0')"
						+ " FROM TB_PRODUCT"
						+ " WHERE COMPANY_SEQ = " + company
							+ " AND REGEXP_INSTR(CODE,'[^0-9]') = 0"
				);
		}

	
		try {
				
			if (seq == 0) {
			// 등록	
				ps = dbLib.prepareStatement(conn,
						"INSERT INTO TB_PRODUCT ("
								+ " SEQ,"
								+ " COMPANY_SEQ,"
								+ " VENDOR_SEQ,"
								+ " CATEGORY_SEQ,"
								+ " CODE,"
								+ " NAME,"
								+ " BAR_CODE,"
								+ " PRICE,"
								+ " MEMO,"
								+ " CREATE_USER_SEQ,"
								+ " CREATE_DATE"
							+ " ) VALUES ("
								+ " SQ_PRODUCT.NEXTVAL,"
								+ " ?,"
								+ " ?,"
								+ " ?,"
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
				ps.setLong(2, vendor);
				ps.setLong(3, category);
				ps.setString(4, code);
				ps.setString(5, name);	
				ps.setString(6, barcode);	
				ps.setLong(7, price);
				ps.setString(8, memo);
				ps.setLong(9, this.cfg.getLong("user.seq"));
				ps.executeUpdate();
				
			} else {
			// 수정		
				ps = dbLib.prepareStatement(conn,
						"UPDATE /*+ INDEX(A) */ TB_PRODUCT A"
							+ " SET COMPANY_SEQ = ?,"
								+ " VENDOR_SEQ = ?,"
								+ " CATEGORY_SEQ = ?,"
								+ " NAME = ?,"
								+ " BAR_CODE = ?,"
								+ " PRICE = ?,"
								+ " MEMO = ?,"
								+ " MODIFY_USER_SEQ = ?,"
								+ " MODIFY_DATE = SYSDATE"
							+ " WHERE SEQ = ?"
					);
				ps.setLong(1, company);
				ps.setLong(2, vendor);
				ps.setLong(3, category);
				ps.setString(4, name);
				ps.setString(5, barcode);
				ps.setLong(6, price);
				ps.setString(7, memo);
				ps.setLong(8, this.cfg.getLong("user.seq"));
				ps.setLong(9, seq);
				ps.executeUpdate();
			}
			
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

	// 조회
		long user_company = this.cfg.getLong("user.company");

		this.data = new GeneralConfig();

		try {
		ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED INDEX(A) USE_NL(B) */"
							+ " A.*,"
							+ " B.PARENT_SEQ,"
							+ " B.DEPTH"
						+ " FROM TB_PRODUCT A"
							+ " LEFT JOIN TB_CATEGORY B"
								+ " ON A.CATEGORY_SEQ = B.SEQ"
						+ " WHERE A.SEQ = ?"
					+ (user_company > 0
							? " AND A.COMPANY_SEQ = ?"
							: ""
						)
				);
			ps.setLong(1, seq);
			if (user_company > 0) ps.setLong(2, user_company);
			rs = ps.executeQuery();

			if (rs.next()) {
				this.data.put("SEQ", rs.getLong("SEQ"));
				this.data.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				this.data.put("VENDOR_SEQ", rs.getLong("VENDOR_SEQ"));
				this.data.put("CATEGORY_SEQ", rs.getLong("CATEGORY_SEQ"));
				this.data.put("PARENT_SEQ", rs.getLong("PARENT_SEQ"));
				this.data.put("CATEGORY_DEPTH", rs.getLong("DEPTH"));
				this.data.put("CODE", rs.getString("CODE"));
				this.data.put("NAME", rs.getString("NAME"));
				this.data.put("PRICE", rs.getLong("PRICE"));
				this.data.put("BAR_CODE", rs.getString("BAR_CODE"));
				this.data.put("MEMO", rs.getString("MEMO"));
			} else {
				error = "등록되지 않았거나 수정할 권한이 없는 상품입니다.";
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

	// 소속
		this.company = new ArrayList<GeneralConfig>();

		try {
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
			if (user_company > 0)  ps.setLong(1, user_company);
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

		// 공급자

		this.vendor = new ArrayList<GeneralConfig>();
	
		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */ "
							+ " SEQ,"
							+ " NAME"
						+ " FROM TB_VENDOR A"
						+ " ORDER BY NAME"
				);
			rs = ps.executeQuery();
	
			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();
	
				c.put("SEQ", rs.getLong("SEQ"));
				c.put("NAME", rs.getString("NAME"));
	
				this.vendor.add(c);
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
		long check = 0;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}

	// 자판기 상품 체크

//		check = StringEx.str2long(
//				dbLib.getResult(conn,
//						"SELECT /*+ INDEX(A) */"
//								+ " COUNT(*)"
//						+ " FROM TB_VENDING_MACHINE_GOODS A"
//						+ " WHERE GOODS_SEQ = " + seq
//					)
//			);

//		if (check > 0) {
//			error = "삭제하고자 하는 상품을 판매하는 자판기가 존재합니다.";
//		}

	// 판매 기록 체크
//		if (StringEx.isEmpty(error)) {
//			check = StringEx.str2long(
//					dbLib.getResult(conn,
//							"SELECT /*+ INDEX(A) */"
//									+ " COUNT(*)"
//								+ " FROM TB_SALES A"
//								+ " WHERE GOODS_SEQ = " + seq
//						)
//				);

//			if (check > 0) {
//				error = "삭제하고자 하는 상품의 판매 기록이 존재합니다.";
//			}
//		}

	// 삭제
		if (StringEx.isEmpty(error)) {
			try {
//
				ps = dbLib.prepareStatement(conn,
						"DELETE /*+ INDEX(A) */"
							+ " FROM TB_PRODUCT"
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
/**
 * 일괄등록
 *
 * @param company 소속
 * @param excel 엑셀
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(long company,  File excel) throws Exception {
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		CallableStatement cs = null;
		PreparedStatement ps = null;
		Workbook workbook = null;
		Sheet sheet = null;
		String error = null;
		String category_name = null;
		String code = null;
		String startDate = DateTime.date("yyyyMMddHHmmss");
		int success = 0;
		int failure = 0;
		long seq = 0;
		long category = 0L;
		long vendor = 0L;
		long vendor_etc = 1L;
		long category_group_etc = -1L;
		
	// 인수 체크
		if (excel == null || excel.length() <= 0) {
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
			
			//기타 그룹군이 없을 때 추가
			category_group_etc = StringEx.str2long(
				dbLib.getResult(conn,
					"SELECT /*+ INDEX(A) */"
							+ " SEQ"
					+ " FROM TB_CATEGORY A"
					+ " WHERE COMPANY_SEQ = " + company
						+ " AND PARENT_SEQ = 0"
						+ " AND NAME = '기타' "
				)
			);
			
			if (category_group_etc == 0L) {
				code = dbLib.getResult(conn,
					"SELECT /*+ INDEX(A) */"
							+ " 'C' || LPAD(TO_CHAR(NVL(MAX(TO_NUMBER(SUBSTR(CODE, 2, 7))), 0) + 1), 7, '0')"
						+ " FROM TB_CATEGORY"
						+ " WHERE COMPANY_SEQ = " + company + 
							" AND DEPTH = 0"
				);
				
				ps = dbLib.prepareStatement(conn,
						"INSERT INTO TB_CATEGORY ("
								+ " SEQ,"
								+ " COMPANY_SEQ,"
								+ " PARENT_SEQ,"
								+ " DEPTH,"
								+ " SORT,"
								+ " CODE,"
								+ " NAME,"
								+ " CREATE_USER_SEQ,"
								+ " CREATE_DATE"
							+ " ) VALUES ("
								+ " SQ_CATEGORY.NEXTVAL,"
								+ " ?,"
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
				ps.setLong(2, 0);
				ps.setInt(3, 0);
				ps.setInt(4, 1);
				ps.setString(5, code);
				ps.setString(6, "기타");			
				ps.setLong(7, this.cfg.getLong("user.seq"));
				ps.executeUpdate();
				ps.close();
				
				category_group_etc = StringEx.str2long(dbLib.getResult(conn, "SELECT SQ_CATEGORY.CURRVAL FROM DUAL"));
			}
			
			workbook = Workbook.getWorkbook(excel);

			if (workbook != null) {
				sheet = workbook.getSheet(0);
				if (sheet != null) {
					if (sheet.getRows() <= 0) {
						error = "엑셀에 데이터가 존재하지 않습니다.";
					} else {
						if (this.cfg.getInt("excel.limit") > 0 && sheet.getRows() <= this.cfg.getInt("excel.limit")) {
							for (int i = 1; i < sheet.getRows(); i++) {
								Cell cellVendor = sheet.getCell(0, i);
								Cell cellCategoryGroup = sheet.getCell(1, i);
								Cell cellCategory = sheet.getCell(2, i);
								Cell cellCode = sheet.getCell(3, i);
								Cell cellName = sheet.getCell(4, i);
								Cell cellPrice = sheet.getCell(5, i);
								Cell cellMemo = sheet.getCell(6, i);
								Cell cellBarcode = sheet.getCell(7, i);
																
								if ( cellVendor == null || cellCategoryGroup == null || cellName == null || cellName.getContents().trim().length()==0 ) {
									continue;
								} 
								
								//Vendor SEQ 조회
								if ( cellVendor.getContents().trim().length() > 0) {
									vendor =  StringEx.str2long(
										dbLib.getResult(conn,
											"SELECT /*+ INDEX(A) */"
													+ " SEQ"
												+ " FROM TB_VENDOR"
												+ " WHERE NAME = '" + cellVendor.getContents().trim() + "'"
										)
									);
									
								}
								if (vendor == 0L) vendor = vendor_etc;
								
								category_name = (cellCategory.getContents().trim().length()==0? cellCategoryGroup.getContents().trim() : cellCategory.getContents().trim());
								
								if (category_name.length() > 0) {							
									category =  StringEx.str2long(
										dbLib.getResult(conn,
											"SELECT /*+ INDEX(A) */"
													+ " SEQ"
												+ " FROM TB_CATEGORY"
												+ " WHERE COMPANY_SEQ = " + company
													+ " AND NAME = '" + category_name + "'"
										)
									);
								}
								
								if (category == 0L) category = category_group_etc;
								
								seq = StringEx.str2long(
									dbLib.getResult(conn,
										"SELECT /*+ INDEX(A) */"
												+ " SEQ"
										+ " FROM TB_PRODUCT A"
										+ " WHERE COMPANY_SEQ = " + company
											//+ " AND VENDOR_SEQ = " + vendor
											//+ " AND CATEGORY_SEQ = " + category
											+ "	AND ( CODE = '" + cellCode.getContents().trim() + "'"
												+ " OR NAME = '" + cellName.getContents().trim() + "'"
											+ " )"
									)
								);
								
								try {		
									if (seq > 0) {
										// 기존에 존재하는 상품 
										ps = dbLib.prepareStatement(conn,
											"UPDATE /*+ INDEX(A) */ TB_PRODUCT A"
												+ " SET  NAME = ?,"
													+ " VENDOR_SEQ = ?,"
													+ " CATEGORY_SEQ = ?,"
													+ " BAR_CODE = ?,"
													+ " MEMO = ?,"
													+ " PRICE = ?,"
													+ " MODIFY_USER_SEQ = ?,"
													+ " MODIFY_DATE = SYSDATE"
												+ " WHERE SEQ = ?"
										);
	
										ps.setString(1, cellName.getContents().trim());
										ps.setLong(2, vendor);
										ps.setLong(3, category);
										ps.setString(4, cellBarcode.getContents().trim());
										ps.setString(5, cellMemo.getContents().trim());
										ps.setLong(6, StringEx.str2long(cellPrice.getContents().trim()));
										ps.setLong(7, this.cfg.getLong("user.seq"));
										ps.setLong(8, seq);
										ps.executeUpdate();
										
										ps.close();
									} else {
										//신규 상품
										//1. 상품코드
										if (cellCode.getContents().trim().length()==0) {
											code = 	dbLib.getResult(conn,
												"SELECT /*+ INDEX(A) */"
														+ " LPAD(TO_CHAR(NVL(MAX(TO_NUMBER(CODE)), 0) + 1), 8, '0')"
													+ " FROM TB_PRODUCT A"
													+ " WHERE COMPANY_SEQ = " + company
														+ " AND REGEXP_INSTR(CODE,'[^0-9]') = 0"
											);
										} else {
											code =  cellCode.getContents().trim();
										}
										
										ps = dbLib.prepareStatement(conn,
												"INSERT INTO TB_PRODUCT ("
														+ " SEQ,"
														+ " COMPANY_SEQ,"
														+ " VENDOR_SEQ,"
														+ " CATEGORY_SEQ,"
														+ " CODE,"
														+ " NAME,"
														+ " BAR_CODE,"
														+ " PRICE,"
														+ " MEMO,"
														+ " CREATE_USER_SEQ,"
														+ " CREATE_DATE"
													+ " ) VALUES ("
														+ " SQ_PRODUCT.NEXTVAL,"
														+ " ?,"
														+ " ?,"
														+ " ?,"
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
										ps.setLong(2, vendor);
										ps.setLong(3, category);
										ps.setString(4, code);
										ps.setString(5, cellName.getContents().trim());	
										ps.setString(6, cellBarcode.getContents().trim());	
										ps.setLong(7,  StringEx.str2long(cellPrice.getContents().trim()));
										ps.setString(8, cellMemo.getContents().trim());
										ps.setLong(9, this.cfg.getLong("user.seq"));
										ps.executeUpdate();
										
										ps.close();
									}
									
									success++;
								} catch (Exception e) {
									this.logger.error(e);
									error = e.getMessage();
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
			System.out.println("catch");
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			try {
				dbLib.close(cs);
				ps.close();
				
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


		// 리소스 반환
		dbLib.close(conn);

		// 파일 삭제
		if (excel.exists()) {
			excel.delete();
		}

		return null;
	}

}