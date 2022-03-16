package com.nucco.beans;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Category.java
 *
 * 상품 그룹
 *
 * 작성일 - 2011/03/30, 정원광
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

public class Category {
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
	public Category(GlobalConfig cfg) throws Exception {
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
 * @param cate 분류
 * @param pageNo 페이지
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getList(long company, long cate, int pageNo, String sField, String sQuery) throws Exception {
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

		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn);
			return error;
		}

	// 검색절 생성
		String WHERE = " A.DEPTH >= 0";

		if (user_company > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND A.COMPANY_SEQ = " + user_company;
		}

		if (company > 0) {
			WHERE += " AND A.COMPANY_SEQ = " + company;
		}

		if (cate > 0) {
			WHERE += " AND A.PARENT_SEQ = " + cate;
		}

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
		}

	// 총 레코드수
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT /*+ INDEX(A) */"
								+ " COUNT(*)"
							+ " FROM TB_CATEGORY A"
							+ " WHERE" + WHERE
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
					"SELECT /*+ ORDERED USE_NL(B C) */"
							+ " AAA.SEQ,"
							+ " AAA.CODE,"
							+ " AAA.NAME,"
							+ " AAA.MEMO,"
							+ " TO_CHAR(AAA.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE,"
							+ " TO_CHAR(AAA.MODIFY_DATE, 'YYYY-MM-DD') AS MODIFY_DATE,"
							+ " B.NAME AS CATE,"
							+ " C.NAME AS COMPANY"
						+ " FROM ("
									+ " SELECT"
											+ " ROWNUM AS ROW_NUM, AA.*"
										+ " FROM ("
												+ " SELECT /*+ INDEX(A) */"
														+ " *"
													+ " FROM TB_CATEGORY A"
													+ " WHERE" + WHERE
													+ " ORDER BY NAME"
											+ " ) AA"
										+ " WHERE ROWNUM <= " + e
								+ " ) AAA"
							+ " LEFT JOIN TB_CATEGORY B"
								+ " ON AAA.PARENT_SEQ = B.SEQ"
							+ " LEFT JOIN TB_COMPANY C"
								+ " ON AAA.COMPANY_SEQ = C.SEQ"
						+ " WHERE ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("CODE", rs.getString("CODE"));
				c.put("NAME", rs.getString("NAME"));
				c.put("MEMO", rs.getString("MEMO"));
				c.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				c.put("MODIFY_DATE", rs.getString("MODIFY_DATE"));
				c.put("CATE", rs.getString("CATE"));
				c.put("COMPANY", rs.getString("COMPANY"));
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
 * @param company 소속
 * @param cate 조직
 * @param depth 깊이
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String detail(long company, long cate, int depth) throws Exception {
		// 조직 정보가 없을 때
		if (cate < 0) {
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
				+ " AND SORT = 1"
			;
		
		if (cate > 0) {
			WHERE += " AND PARENT_SEQ = " + cate;
		} else {
			WHERE += " AND PARENT_SEQ = 0";
		}	

		if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND COMPANY_SEQ = " + this.cfg.getLong("user.company");
		}

	
		WHERE += " AND SEQ IN ("
					+ " SELECT /*+ INDEX(A_A) */"
							+ " SEQ"
						+ " FROM TB_CATEGORY A_A"
						+ " START WITH PARENT_SEQ = " + cate
						+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
				+ ")"
			;
		// 목록
		this.cate = new ArrayList<GeneralConfig>();	
		
		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " SEQ,"
							+ " NAME,"
							+ " CODE"
						+ " FROM TB_CATEGORY A"
						+ " WHERE" + WHERE
						+ " ORDER BY NAME, SEQ"
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("NAME", rs.getString("NAME"));
				c.put("CODE", rs.getString("CODE"));

				this.cate.add(c);
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
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " SEQ,"
								+ " NAME,"
								+ " CODE"
							+ " FROM TB_CATEGORY A"
							+ " WHERE COMPANY_SEQ = " + company
								+ " AND DEPTH = " + depth
								+ " AND SORT = 1"
								+ " AND ROWNUM = 1"
					);
				rs = ps.executeQuery();

				if (rs.next()) {
					this.data.put("SEQ", rs.getLong("SEQ"));
					this.data.put("NAME", rs.getString("NAME"));
					this.data.put("CODE", rs.getString("CODE"));
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
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " SEQ,"
							+ " NAME,"
							+ " CODE"
						+ " FROM TB_CATEGORY A"
						+ " WHERE COMPANY_SEQ = " + company
							+ " AND DEPTH = " + depth
							+ " AND SORT = 0 "
							+ " AND ROWNUM = 1"
				);
			rs = ps.executeQuery();

			if (rs.next()) {
				this.data.put("SEQ", rs.getLong("SEQ"));
				this.data.put("NAME", rs.getString("NAME"));
				this.data.put("CODE", rs.getString("CODE"));
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
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " PARENT_SEQ,"
							+ " DEPTH,"
							+ " COMPANY_SEQ,"
							+ " ("
									+ " SELECT /*+ INDEX(B) */"
											+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/'))"
										+ " FROM TB_CATEGORY B"
										+ " WHERE PARENT_SEQ = 0"
										+ " START WITH SEQ = A.PARENT_SEQ"
										+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
								+ ") AS NAME"
						+ " FROM TB_CATEGORY A"
						+ " WHERE SEQ = ?"
					    + (user_company > 0
							? " AND COMPANY_SEQ = ?"
							: ""
						)
					);
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

		this.cate = new ArrayList<GeneralConfig>();

		if (StringEx.isEmpty(error)) {
			try {
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " SEQ,"
								+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(NAME), '/'), '/')) AS NAME"
							+ " FROM TB_CATEGORY A"
							+ " WHERE PARENT_SEQ = 0"
							+ " START WITH COMPANY_SEQ = ?"
								+ " AND DEPTH = ?"
								+ " AND SORT = 1"
							+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
					);
				ps.setLong(1, this.data.getLong("COMPANY_SEQ"));
				ps.setInt(2, this.data.getInt("DEPTH") - 1);
				rs = ps.executeQuery();

				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();

					c.put("SEQ", rs.getLong("SEQ"));
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
		}

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}	
/**
 * 등록
 *
 * @param company 소속
 * @param cate 상위번호
 * @param depth 깊이
 * @param sort 정렬 방법
 * @param name 이름
 * @param catcode 코드
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(long company, long cate, int depth, int sort, String name, String code) throws Exception {
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		String error = null;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));
		
		this.data = new GeneralConfig();
		
		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}
		// 코드
		if (StringEx.isEmpty(code)) {
			if (cate == 0) {
				code = dbLib.getResult(conn,
						"SELECT /*+ INDEX(A) */"
								+ " 'C' || LPAD(TO_CHAR(NVL(MAX(TO_NUMBER(SUBSTR(CODE, 2, 7))), 0) + 1), 7, '0')"
							+ " FROM TB_CATEGORY"
							+ " WHERE COMPANY_SEQ = " + company + 
								" AND DEPTH = 0"
					);
			} else {
				code = dbLib.getResult(conn,
						"SELECT /*+ INDEX(A) */"
								+ " LPAD(TO_CHAR(NVL(MAX(TO_NUMBER(CODE)), 0) + 1), 8, '0')"
							+ " FROM TB_CATEGORY A"
							+ " WHERE PARENT_SEQ = ("
									+ " SELECT /*+ INDEX(B) */"
										+ " SEQ"
										+ " FROM TB_CATEGORY B"
										+ " WHERE PARENT_SEQ = 0"
											+ " AND SEQ = '" + cate + "'"
								+ " )"
					);				
			}
		}
		// 등록
		try {
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
			ps.setLong(2, cate);
			ps.setInt(3, depth);
			ps.setInt(4, sort);
			ps.setString(5, code);
			ps.setString(6, name);			
			ps.setLong(7, this.cfg.getLong("user.seq"));
			ps.executeUpdate();
			
			this.data.put("CODE", code);
			
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(ps);
		}	
	
		

	// 등록번호
		String sequence = dbLib.getResult(conn, "SELECT SQ_CATEGORY.CURRVAL FROM DUAL");
		this.data.put("SEQ", sequence);

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
			ps = dbLib.prepareStatement(conn,
					"UPDATE /*+ INDEX(A) */ TB_CATEGORY A"
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
 * 상위 그룹 변경
 *
 * @param seq 등록번호
 * @param cate 변경할 상위 그룹
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String modify(long seq, long cate) throws Exception {
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		String error = null;
		String code = null;
		ResultSet rs = null;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}

	// 수정

			code = dbLib.getResult(conn,
					"SELECT /*+ INDEX(A) */"
							+ " LPAD(TO_CHAR(NVL(MAX(TO_NUMBER(CODE)), 0) + 1), 8, '0')"
						+ " FROM TB_CATEGORY A"
						+ " WHERE PARENT_SEQ = ("
								+ " SELECT /*+ INDEX(B) */"
									+ " SEQ"
									+ " FROM TB_CATEGORY B"
									+ " WHERE PARENT_SEQ = 0"
										+ " AND SEQ = '" + cate + "'"
							+ " )"
				);	
		
			
		this.data = new GeneralConfig();	

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " COMPANY_SEQ,"
							+ " CODE"
						+ " FROM TB_CATEGORY A"
						+ " WHERE PARENT_SEQ = 0"
						+ " AND SEQ = '" + cate + "'"
				);
			rs = ps.executeQuery();

			if (rs.next()) {
				this.data.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				this.data.put("CODE", rs.getString("CODE"));
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}
		
		try {
			
			ps = dbLib.prepareStatement(conn,
					"UPDATE /*+ INDEX(A) */ TB_CATEGORY A"
						+ " SET PARENT_SEQ = ?,"
							+ " CODE = ?,"
							+ " MODIFY_USER_SEQ = ?,"
							+ " MODIFY_DATE = SYSDATE"
						+ " WHERE SEQ = ?"
				);
			ps.setLong(1, cate);
			ps.setString(2, code);
			ps.setLong(3, this.cfg.getLong("user.seq"));
			ps.setLong(4, seq);
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
		int sort = StringEx.str2int(
				dbLib.getResult(conn,
						"SELECT /*+ INDEX(A) */"
								+ " SORT"
							+ " FROM TB_CATEGORY A"
							+ " WHERE SEQ = " + seq
						),
				-1
			);
		System.out.println("sort: " + sort);
	// 실행
		switch (sort) {
			case 0 : // 명칭
				break;
			case 1 : // 목록
				try {
					ps = dbLib.prepareStatement(conn,
							"DELETE /*+ INDEX(A) */"
								+ " FROM TB_CATEGORY A"
								+ " WHERE SEQ IN ("
										+ " SELECT /*+ INDEX(A_A) */"
												+ " SEQ"
											+ " FROM TB_CATEGORY A_A"
											+ " START WITH SEQ = ?"
											+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"

									+ ")"
						);
					ps.setLong(1, seq);
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