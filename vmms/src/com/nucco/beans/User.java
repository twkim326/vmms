package com.nucco.beans;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * User.java
 *
 * 계정
 *
 * 작성일 - 2011/03/21, 정원광
 *
 */
import java.util.ArrayList;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.nucco.GlobalConfig;
import com.nucco.cfg.GeneralConfig;
import com.nucco.lib.DateTime;
import com.nucco.lib.Pager;
import com.nucco.lib.StringEx;
import com.nucco.lib.base64.Base64;
import com.nucco.lib.db.DBLibrary;
import com.nucco.lib.security.Cryptograph;

import oracle.jdbc.OracleTypes;

public class User {
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
 * 계정 목록
 *
 */
	public ArrayList<GeneralConfig> list;
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
 * 매출조회 조직추가 목록
 *
 */
	public ArrayList<GeneralConfig> appOrgan;
/**
 * 계정 조회
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
	public User(GlobalConfig cfg) throws Exception {
	// set config
		this.cfg = cfg;

	// set log4j
		PropertyConfigurator.configure(cfg.get("config.log4j"));
		this.logger = Logger.getLogger(this.getClass());
	}
/**
 * 로그인
 *
 * @param response javax.servlet.http.HttpServletResponse
 * @param id 아이디
 * @param pass 패스워드
 * @param ip 아이피
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String login(HttpServletResponse response, String id, String pass, String ip) throws Exception {
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		CallableStatement cs = null;
		String error = null;

	// DB 연결
		System.out.println("db.jdbc.name==="+this.cfg.get("db.jdbc.name"));
		System.out.println("db.jdbc.host==="+this.cfg.get("db.jdbc.host"));
		System.out.println("db.jdbc.user==="+this.cfg.get("db.jdbc.user"));
		System.out.println("db.jdbc.pass==="+this.cfg.get("db.jdbc.pass"));

		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "예기치 않은 오류로 DB에 연결하는데 실패하였습니다.";
		}

	// 정보 가져오기
		try {
			cs = dbLib.prepareCall(conn, "{ CALL PG_AUTH.LOGIN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");
			cs.setString(1, id);
			cs.setString(2, pass);
			cs.setString(3, ip);
			cs.registerOutParameter(4, OracleTypes.NUMBER);
			cs.registerOutParameter(5, OracleTypes.NUMBER);
			cs.registerOutParameter(6, OracleTypes.NUMBER);
			cs.registerOutParameter(7, OracleTypes.NUMBER);
			cs.registerOutParameter(8, OracleTypes.NUMBER);
			cs.registerOutParameter(9, OracleTypes.VARCHAR);
			cs.registerOutParameter(10, OracleTypes.VARCHAR);
			cs.registerOutParameter(11, OracleTypes.VARCHAR);
			cs.registerOutParameter(12, OracleTypes.VARCHAR);
			cs.execute();

			long seq = cs.getLong(4);
			long company = cs.getLong(5);
			long organ = cs.getLong(6);
			int organDepth = cs.getInt(7);
			long auth = cs.getLong(8);
			String name = cs.getString(9);
			String operator = cs.getString(10);
			String hash = cs.getString(11);

			error = cs.getString(12);

			if (StringEx.isEmpty(error)) {
				String cookie = Base64.encode((new Cryptograph()).encrypt("SEQ=" + seq
					+ "&ID=" + StringEx.encode(id)
					+ "&NAME=" + StringEx.encode(name)
					+ "&AUTH=" + auth
					+ "&COMPANY=" + company
					+ "&ORGAN=" + organ
					+ "&ORGAN.DEPTH=" + organDepth
					+ "&OPERATOR=" + StringEx.encode(operator)
					+ "&HASH=" + StringEx.encode(hash)
					+ "&TIME=" + System.currentTimeMillis()
					+ "&IP=" + StringEx.encode(ip)));

			// -- 2011.05.19, 계정 인증 정보를 쿠키에서 세션으로 변경
			//	CookieEx.set(response, "MEM", , "/");
				this.cfg.getSession().setAttribute("MEM", cookie);
				this.cfg.getSession().setMaxInactiveInterval(60 * this.cfg.getInt("cookie.expire"));
			// --
			}
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
 * 로그아웃
 *
 * @return void
 * @param response javax.servlet.http.HttpServletResponse
 * @param ip 아이피
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String logout(HttpServletResponse response, String ip) {
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		String error = null;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "예기치 않은 오류로 DB에 연결하는데 실패하였습니다.";
		}

	// 로그아웃 정보 등록
		try {
//20160216 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "UPDATE TB_USER_LOG SET OUT_DATE = TO_CHAR(SYSDATE, 'YYYYMMDD'), OUT_TIME = TO_CHAR(SYSDATE, 'HH24MISS'), OUT_IP = ? WHERE SEQ = ? AND HASH_DATA = ?");
			ps = dbLib.prepareStatement(conn,
					"UPDATE /*+ INDEX(A) */ TB_USER_LOG A"
						+ " SET OUT_DATE = TO_CHAR(SYSDATE, 'YYYYMMDD'),"
							+ " OUT_TIME = TO_CHAR(SYSDATE, 'HH24MISS'),"
							+ " OUT_IP = ?"
						+ " WHERE SEQ = ?"
							+ " AND HASH_DATA = ?"
				);
			ps.setString(1, ip);
			ps.setLong(2, this.cfg.getLong("user.seq"));
			ps.setString(3, this.cfg.get("user.hash"));
			ps.executeUpdate();
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(ps);
		}

	// 리소스 반환
		dbLib.close(conn);

	// 로그아웃
	// -- 2011.05.19, 계정 인증 정보를 쿠키에서 세션으로 변경
	//	CookieEx.set(response, "MEM", "", "/", 0);
		this.cfg.getSession().setAttribute("MEM", "");
		this.cfg.getSession().invalidate();
	// --

		return error;
	}
/**
 * 목록
 *
 * @param pageNo 페이지
 * @param company 소속
 * @param organ 조직
 * @param auth 권한
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getList_org(int pageNo, long company, long organ, long auth, String sField, String sQuery) throws Exception {
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

		if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
			WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
		}

		if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색
//20160216 INDEX 힌트 추가
//				WHERE += " AND ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ)";
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
			WHERE += " AND A.COMPANY_SEQ = " + company;
		}

		if (organ > 0) { // 조직
//20160216 INDEX 힌트 추가
//				WHERE += " AND ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + organ + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + organ + ")";
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

		if (auth > 0) { // 권한
			WHERE += " AND AUTH_SEQ = " + auth;
		}

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

	// 총 레코드수
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT " + (WHERE.length() > 0 ? " /*+ INDEX(A) */" : "")
								+ " COUNT(*)"
							+ " FROM TB_USER A"
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

//20160216 INDEX 힌트 변경, ORDER BY 추가
//				ps = dbLib.prepareStatement(conn, "SELECT S2.*"
//					+ " FROM"
//					+ " ("
//					+ " SELECT ROWNUM AS RNUM, S1.*"
//					+ " FROM"
//					+ " ("
//					+ " SELECT /*+ ORDERED USE_NL(B C) INDEX_DESC(A PK_USER) */ A.SEQ, A.ID, A.NAME, A.CELLPHONE, TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE, B.NAME AS COMPANY, CASE WHEN A.AUTH_SEQ = 0 THEN '시스템' ELSE C.NAME END AS AUTH, (SELECT LTRIM(SYS_CONNECT_BY_PATH(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS ORGAN"
//					+ " FROM TB_USER A"
//					+ " LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ"
//					+ " LEFT JOIN TB_AUTH C ON A.AUTH_SEQ = C.SEQ"
//					+ " WHERE " + WHERE
//					+ " ) S1"
//					+ " WHERE ROWNUM <= " + e
//					+ " ) S2"
//					+ " WHERE RNUM >= " + s);
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED USE_NL(C) */"
							+ " AAA.SEQ,"
							+ " AAA.ID,"
							+ " AAA.NAME,"
							+ " AAA.CELLPHONE,"
							+ " TO_CHAR(AAA.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE,"
							+ " AAA.COMPANY,"
							+ " AAA.ORGAN,"
							+ " CASE WHEN AAA.AUTH_SEQ = 0 THEN '시스템' ELSE C.NAME END AS AUTH"
						+ " FROM ("
									+ " SELECT"
											+ " ROWNUM AS ROW_NUM,"
											+ " AA.*"
										+ " FROM ("
												+ " SELECT /*+ ORDERED" + (WHERE.length() > 0 ? " INDEX(A)" : "") + " USE_HASH(B) */"
														+ " B.NAME AS COMPANY,"
														+ " ("
														//20160216 REVERSE 적용
		//														+ " SELECT /*+ INDEX(D) */"
		//																	+ " LTRIM(SYS_CONNECT_BY_PATH(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END, '/'), '/')"
		//																+ " FROM TB_ORGANIZATION D"
		//																+ " WHERE SEQ = AAA.ORGANIZATION_SEQ"
		//																+ " START WITH PARENT_SEQ = 0"
		//																+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
																+ " SELECT /*+ INDEX(D) */"
																			+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
																		+ " FROM TB_ORGANIZATION D"
																		+ " WHERE PARENT_SEQ = 0"
																		+ " START WITH SEQ = A.ORGANIZATION_SEQ"
																		+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
															+ " ) AS ORGAN,"
														+ " A.ID,"
														+ " A.SEQ,"
														+ " A.NAME,"
														+ " A.CELLPHONE,"
														+ " A.CREATE_DATE,"
														+ " A.AUTH_SEQ"
													+ " FROM TB_USER A"
														+ " LEFT JOIN TB_COMPANY B"
															+ " ON A.COMPANY_SEQ = B.SEQ"
													+ WHERE
													+ " ORDER BY " + (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery) ? sField : "1, 2, 3")
											+ " ) AA"
										+ " WHERE ROWNUM <= " + e
								+ " ) AAA"
							+ " LEFT JOIN TB_AUTH C"
								+ " ON AAA.AUTH_SEQ = C.SEQ"
						+ " WHERE ROW_NUM >= " + s);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("ID", rs.getString("ID"));
				c.put("NAME", rs.getString("NAME"));
				c.put("CELLPHONE", rs.getString("CELLPHONE"));
				c.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				c.put("COMPANY", rs.getString("COMPANY"));
				c.put("AUTH", rs.getString("AUTH"));
				c.put("ORGAN", rs.getString("ORGAN"));
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
 * 목록
 *
 * @param pageNo 페이지
 * @param company 소속
 * @param organ 조직
 * @param auth 권한
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String getList(int pageNo, long company, long organ, long auth, String sField, String sQuery) throws Exception {
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
		
		if (company > 0) { // 소속
			WHERE += " AND A.COMPANY_SEQ = " + company;
		} else {
			if (this.cfg.getLong("user.company") > 0) { // 시스템 관리자가 아닌 경우, 내가 속한 소속만 검색
				WHERE += " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company");
			}
		}
		
		
		if (organ > 0) { // 조직
//20160216 INDEX 힌트 추가
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
		} else {
			if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색
//20160216 INDEX 힌트 추가
//				WHERE += " AND ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ)";
//				WHERE += " AND ORGANIZATION_SEQ IN ("
//							+ " SELECT /*+ INDEX(A_A) */"
//									+ " SEQ"
//								+ " FROM TB_ORGANIZATION A_A"
//								+ " WHERE SORT = 1"
//								+ " START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ")
//								+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
//							+ " )"
//						;
				WHERE += " AND ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(A_B) */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION A_B"
							+ " WHERE SORT = 1"
							+ " START WITH SEQ = " + this.cfg.getLong("user.organ")
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;				
			}			
		}

		if (auth > 0) { // 권한
			WHERE += " AND AUTH_SEQ = " + auth;
		}

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

	// 총 레코드수
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT " + (WHERE.length() > 0 ? " /*+ INDEX(A) */" : "")
								+ " COUNT(*)"
							+ " FROM TB_USER A"
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

//20160216 INDEX 힌트 변경, ORDER BY 추가
//			ps = dbLib.prepareStatement(conn, "SELECT S2.*"
//				+ " FROM"
//				+ " ("
//				+ " SELECT ROWNUM AS RNUM, S1.*"
//				+ " FROM"
//				+ " ("
//				+ " SELECT /*+ ORDERED USE_NL(B C) INDEX_DESC(A PK_USER) */ A.SEQ, A.ID, A.NAME, A.CELLPHONE, TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE, B.NAME AS COMPANY, CASE WHEN A.AUTH_SEQ = 0 THEN '시스템' ELSE C.NAME END AS AUTH, (SELECT LTRIM(SYS_CONNECT_BY_PATH(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS ORGAN"
//				+ " FROM TB_USER A"
//				+ " LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ"
//				+ " LEFT JOIN TB_AUTH C ON A.AUTH_SEQ = C.SEQ"
//				+ " WHERE " + WHERE
//				+ " ) S1"
//				+ " WHERE ROWNUM <= " + e
//				+ " ) S2"
//				+ " WHERE RNUM >= " + s);
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED USE_NL(C) */"
							+ " AAA.SEQ,"
							+ " AAA.ID,"
							+ " AAA.NAME,"
							+ " AAA.CELLPHONE,"
							+ " AAA.IS_SMS_SOLDOUT," 
							+ " AAA.IS_SMS_STATE,"
							+ " AAA.IS_SMS_CLOSING,"
							/*2020-12-04 김태우 추가*/							
							+ " AAA.IS_KAKAO_STATUS,"
							+ " TO_CHAR(AAA.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE,"
							+ " AAA.COMPANY,"
							+ " AAA.ORGAN,"
							+ " CASE WHEN AAA.AUTH_SEQ = 0 THEN '시스템' ELSE C.NAME END AS AUTH"
						+ " FROM ("
									+ " SELECT"
											+ " ROWNUM AS ROW_NUM,"
											+ " AA.*"
										+ " FROM ("
												+ " SELECT /*+ ORDERED" + (WHERE.length() > 0 ? " INDEX(A)" : "") + " USE_HASH(B) */"
														+ " B.NAME AS COMPANY,"
														+ " ("
														//20160216 REVERSE 적용
		//														+ " SELECT /*+ INDEX(D) */"
		//																	+ " LTRIM(SYS_CONNECT_BY_PATH(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END, '/'), '/')"
		//																+ " FROM TB_ORGANIZATION D"
		//																+ " WHERE SEQ = AAA.ORGANIZATION_SEQ"
		//																+ " START WITH PARENT_SEQ = 0"
		//																+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
																+ " SELECT /*+ INDEX(D) */"
																			+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
																		+ " FROM TB_ORGANIZATION D"
																		+ " WHERE PARENT_SEQ = 0"
																		+ " START WITH SEQ = A.ORGANIZATION_SEQ"
																		+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
															+ " ) AS ORGAN,"
														+ " A.ID,"
														+ " A.SEQ,"
														+ " A.NAME,"
														+ " A.CELLPHONE,"
														+ " A.IS_SMS_SOLDOUT,"
														+ " A.IS_SMS_STATE,"
														+ " A.IS_SMS_CLOSING,"								
														/*2020-12-04 김태우 추가*/
														+ " A.IS_KAKAO_STATUS,"
														+ " A.CREATE_DATE,"
														+ " A.AUTH_SEQ"
													+ " FROM TB_USER A"
														+ " LEFT JOIN TB_COMPANY B"
															+ " ON A.COMPANY_SEQ = B.SEQ"
													+ WHERE
													+ " ORDER BY " + (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery) ? sField : "1, 2, 3")
											+ " ) AA"
										+ " WHERE ROWNUM <= " + e
								+ " ) AAA"
							+ " LEFT JOIN TB_AUTH C"
								+ " ON AAA.AUTH_SEQ = C.SEQ"
						+ " WHERE ROW_NUM >= " + s);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
				c.put("ID", rs.getString("ID"));
				c.put("NAME", rs.getString("NAME"));
				c.put("CELLPHONE", rs.getString("CELLPHONE"));
				c.put("IS_SMS_SOLDOUT", rs.getString("IS_SMS_SOLDOUT"));
				c.put("IS_SMS_STATE", rs.getString("IS_SMS_STATE"));
				c.put("IS_SMS_CLOSING", rs.getString("IS_SMS_CLOSING"));
				//2020-12-04 김태우 추가
				c.put("IS_KAKAO_STATUS", rs.getString("IS_KAKAO_STATUS"));
				c.put("CREATE_DATE", rs.getString("CREATE_DATE"));
				c.put("COMPANY", rs.getString("COMPANY"));
				c.put("AUTH", rs.getString("AUTH"));
				c.put("ORGAN", rs.getString("ORGAN"));
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
 * 조회 (2018/4월 이전)
 *
 * @param seq 계정번호
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String detail_org(long seq) throws Exception {
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

		try {
//20160216 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "SELECT A.*, B.NAME AS COMPANY, C.NAME AS AUTH, C.IS_APP_ORGAN, SUBSTR(A.EMAIL, 1, INSTR(A.EMAIL, '@') - 1) AS EMAIL_1, SUBSTR(EMAIL, INSTR(EMAIL, '@') + 1, LENGTH(EMAIL)) AS EMAIL_2, (SELECT LTRIM(SYS_CONNECT_BY_PATH(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS ORGAN FROM TB_USER A LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ LEFT JOIN TB_AUTH C ON A.AUTH_SEQ = C.SEQ WHERE A.SEQ = ?"
//					+ (this.cfg.getLong("user.company") > 0 ? " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company") : "")
//					+ (this.cfg.getLong("user.organ") > 0 ? " AND A.ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + this.cfg.getLong("user.organ") + ")" : ""));
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED INDEX(A) USE_NL(B C) */"
							+ " A.*,"
							+ " B.NAME AS COMPANY,"
							+ " C.NAME AS AUTH,"
							+ " C.IS_APP_ORGAN,"
							+ " SUBSTR(A.EMAIL, 1, INSTR(A.EMAIL, '@') - 1) AS EMAIL_1,"
							+ " SUBSTR(EMAIL, INSTR(EMAIL, '@') + 1, LENGTH(EMAIL)) AS EMAIL_2,"
							+ " ("
//20160216 REVERSE 적용
//									+ "SELECT /*+ INDEX(D) */"
//											+ " LTRIM(SYS_CONNECT_BY_PATH(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END, '/'), '/')"
//										+ " FROM TB_ORGANIZATION D"
//										+ " WHERE SEQ = A.ORGANIZATION_SEQ"
//										+ " START WITH PARENT_SEQ = 0"
//										+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
									+ " SELECT /*+ INDEX(D) */"
											+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
										+ " FROM TB_ORGANIZATION D"
										+ " WHERE PARENT_SEQ = 0"
										+ " START WITH SEQ = A.ORGANIZATION_SEQ"
										+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
								+ " ) AS ORGAN"
						+ " FROM TB_USER A"
							+ " LEFT JOIN TB_COMPANY B"
								+ " ON A.COMPANY_SEQ = B.SEQ"
							+ " LEFT JOIN TB_AUTH C"
								+ " ON A.AUTH_SEQ = C.SEQ"
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
								+ " )"
							: ""
						)
				);
			ps.setLong(1, seq);
			rs = ps.executeQuery();

			if (rs.next()) {
				this.data.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				this.data.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				this.data.put("AUTH_SEQ", rs.getLong("AUTH_SEQ"));
				this.data.put("ID", rs.getString("ID"));
				this.data.put("NAME", rs.getString("NAME"));
				this.data.put("IS_ENABLED", StringEx.setDefaultValue(rs.getString("IS_ENABLED"), "N"));
				this.data.put("EMAIL", rs.getString("EMAIL"));
				this.data.put("EMAIL_1", StringEx.setDefaultValue(rs.getString("EMAIL_1"), ""));
				this.data.put("EMAIL_2", StringEx.setDefaultValue(rs.getString("EMAIL_2"), ""));
				this.data.put("TELEPHONE", rs.getString("TELEPHONE"));
				this.data.put("CELLPHONE", rs.getString("CELLPHONE"));
				this.data.put("FAX", rs.getString("FAX"));
				this.data.put("EMPLOYEE_NO", rs.getString("EMPLOYEE_NO"));
				this.data.put("COMPANY", rs.getString("COMPANY"));
				this.data.put("AUTH", rs.getString("AUTH"));
				this.data.put("IS_APP_ORGAN", rs.getString("IS_APP_ORGAN"));
				this.data.put("ORGAN", rs.getString("ORGAN"));

				String[] TELEPHONE = StringEx.fixArray(StringEx.split(rs.getString("TELEPHONE"), "-"), 3);
				String[] CELLPHONE = StringEx.fixArray(StringEx.split(rs.getString("CELLPHONE"), "-"), 3);
				String[] FAX = StringEx.fixArray(StringEx.split(rs.getString("FAX"), "-"), 3);

				this.data.put("TELEPHONE_1", TELEPHONE[0]);
				this.data.put("TELEPHONE_2", TELEPHONE[1]);
				this.data.put("TELEPHONE_3", TELEPHONE[2]);
				this.data.put("CELLPHONE_1", CELLPHONE[0]);
				this.data.put("CELLPHONE_2", CELLPHONE[1]);
				this.data.put("CELLPHONE_3", CELLPHONE[2]);
				this.data.put("FAX_1", FAX[0]);
				this.data.put("FAX_2", FAX[1]);
				this.data.put("FAX_3", FAX[2]);
			} else {
				error = "등록되지 않았거나 조회가 불가능한 계정입니다.";
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 매출조회 추가조직
		this.appOrgan = new ArrayList<GeneralConfig>();

		if (this.data.get("IS_APP_ORGAN").equals("Y")) {
			try {
//20160216 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "SELECT A.SEQ, A.ORGANIZATION_SEQ, (SELECT LTRIM(SYS_CONNECT_BY_PATH(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS ORGAN FROM TB_USER_APP_ORGAN A WHERE A.SEQ = ?");
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " SEQ,"
								+ " ORGANIZATION_SEQ,"
								+ " ("
//20160216 REVERSE 적용
//										+ " SELECT /*+ INDEX(D) */"
//												+ " LTRIM(SYS_CONNECT_BY_PATH(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END, '/'), '/')"
//											+ " FROM TB_ORGANIZATION B"
//											+ " WHERE SEQ = A.ORGANIZATION_SEQ"
//											+ " START WITH PARENT_SEQ = 0"
//											+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
										+ " SELECT /*+ INDEX(B) */"
												+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
											+ " FROM TB_ORGANIZATION B"
											+ " WHERE PARENT_SEQ = 0"
											+ " START WITH SEQ = A.ORGANIZATION_SEQ"
											+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
									+ " ) AS ORGAN"
							+ " FROM TB_USER_APP_ORGAN A"
							+ " WHERE SEQ = ?"
					);
				ps.setLong(1, seq);
				rs = ps.executeQuery();

				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();

					c.put("SEQ", rs.getLong("SEQ"));
					c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
					c.put("ORGAN", rs.getString("ORGAN"));

					this.appOrgan.add(c);
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
 * @param seq 계정번호
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String detail(long seq) throws Exception {
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

		try {
//20160216 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "SELECT A.*, B.NAME AS COMPANY, C.NAME AS AUTH, C.IS_APP_ORGAN, SUBSTR(A.EMAIL, 1, INSTR(A.EMAIL, '@') - 1) AS EMAIL_1, SUBSTR(EMAIL, INSTR(EMAIL, '@') + 1, LENGTH(EMAIL)) AS EMAIL_2, (SELECT LTRIM(SYS_CONNECT_BY_PATH(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS ORGAN FROM TB_USER A LEFT JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ LEFT JOIN TB_AUTH C ON A.AUTH_SEQ = C.SEQ WHERE A.SEQ = ?"
//						+ (this.cfg.getLong("user.company") > 0 ? " AND A.COMPANY_SEQ = " + this.cfg.getLong("user.company") : "")
//						+ (this.cfg.getLong("user.organ") > 0 ? " AND A.ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + this.cfg.getLong("user.organ") + ")" : ""));
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED INDEX(A) USE_NL(B C) */"
							+ " A.*,"
							+ " B.NAME AS COMPANY,"
							+ " B.IS_SMS_ENABLED,"
							//2020-12-04 김태우 추가
							+ " B.IS_KAKAO_ENABLED,"
							+ " C.NAME AS AUTH,"
							+ " C.IS_APP_ORGAN,"
							+ " SUBSTR(A.EMAIL, 1, INSTR(A.EMAIL, '@') - 1) AS EMAIL_1,"
							+ " SUBSTR(EMAIL, INSTR(EMAIL, '@') + 1, LENGTH(EMAIL)) AS EMAIL_2,"
							+ " ("
//20160216 REVERSE 적용
//										+ "SELECT /*+ INDEX(D) */"
//												+ " LTRIM(SYS_CONNECT_BY_PATH(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END, '/'), '/')"
//											+ " FROM TB_ORGANIZATION D"
//											+ " WHERE SEQ = A.ORGANIZATION_SEQ"
//											+ " START WITH PARENT_SEQ = 0"
//											+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
									+ " SELECT /*+ INDEX(D) */"
											+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
										+ " FROM TB_ORGANIZATION D"
										+ " WHERE PARENT_SEQ = 0"
										+ " START WITH SEQ = A.ORGANIZATION_SEQ"
										+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
								+ " ) AS ORGAN"
						+ " FROM TB_USER A"
							+ " LEFT JOIN TB_COMPANY B"
								+ " ON A.COMPANY_SEQ = B.SEQ"
							+ " LEFT JOIN TB_AUTH C"
								+ " ON A.AUTH_SEQ = C.SEQ"
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
								+ " )"
							: ""
						)
				);
			ps.setLong(1, seq);
			rs = ps.executeQuery();

			if (rs.next()) {
				this.data.put("COMPANY_SEQ", rs.getLong("COMPANY_SEQ"));
				this.data.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
				this.data.put("AUTH_SEQ", rs.getLong("AUTH_SEQ"));
				this.data.put("ID", rs.getString("ID"));
				this.data.put("NAME", rs.getString("NAME"));
				this.data.put("IS_ENABLED", StringEx.setDefaultValue(rs.getString("IS_ENABLED"), "N"));
				this.data.put("EMAIL", rs.getString("EMAIL"));
				this.data.put("EMAIL_1", StringEx.setDefaultValue(rs.getString("EMAIL_1"), ""));
				this.data.put("EMAIL_2", StringEx.setDefaultValue(rs.getString("EMAIL_2"), ""));
				this.data.put("TELEPHONE", rs.getString("TELEPHONE"));
				this.data.put("CELLPHONE", rs.getString("CELLPHONE"));
				/* 2018/4/18 jwhwang 추가 */
				this.data.put("IS_SMS_SOLDOUT", StringEx.setDefaultValue(rs.getString("IS_SMS_SOLDOUT"), "N"));
				this.data.put("IS_SMS_STATE", StringEx.setDefaultValue(rs.getString("IS_SMS_STATE"), "N"));
				/* 2018/6/12 jwhwang 추가 */
				this.data.put("IS_SMS_CLOSING", StringEx.setDefaultValue(rs.getString("IS_SMS_CLOSING"), "N"));
				/*2020/12/04 김태우 추가*/
				this.data.put("IS_KAKAO_STATUS", StringEx.setDefaultValue(rs.getString("IS_KAKAO_STATUS"), "N"));
				/* 2018/4/18 jwhwang 추가 */
				this.data.put("FAX", rs.getString("FAX"));
				this.data.put("EMPLOYEE_NO", rs.getString("EMPLOYEE_NO"));
				this.data.put("COMPANY", rs.getString("COMPANY"));
				/* 2018/4/18 jwhwang 추가 */
				this.data.put("IS_SMS_ENABLED", rs.getString("IS_SMS_ENABLED"));
				/*2020/12/04 김태우 추가 */
				this.data.put("IS_KAKAO_ENABLED", rs.getString("IS_KAKAO_ENABLED"));
				/* 2018/4/18 jwhwang 추가 */
				this.data.put("AUTH", rs.getString("AUTH"));
				this.data.put("IS_APP_ORGAN", rs.getString("IS_APP_ORGAN"));
				this.data.put("ORGAN", rs.getString("ORGAN"));

				String[] TELEPHONE = StringEx.fixArray(StringEx.split(rs.getString("TELEPHONE"), "-"), 3);
				String[] CELLPHONE = StringEx.fixArray(StringEx.split(rs.getString("CELLPHONE"), "-"), 3);
				String[] FAX = StringEx.fixArray(StringEx.split(rs.getString("FAX"), "-"), 3);

				this.data.put("TELEPHONE_1", TELEPHONE[0]);
				this.data.put("TELEPHONE_2", TELEPHONE[1]);
				this.data.put("TELEPHONE_3", TELEPHONE[2]);
				this.data.put("CELLPHONE_1", CELLPHONE[0]);
				this.data.put("CELLPHONE_2", CELLPHONE[1]);
				this.data.put("CELLPHONE_3", CELLPHONE[2]);
				this.data.put("FAX_1", FAX[0]);
				this.data.put("FAX_2", FAX[1]);
				this.data.put("FAX_3", FAX[2]);
			} else {
				error = "등록되지 않았거나 조회가 불가능한 계정입니다.";
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 매출조회 추가조직
		this.appOrgan = new ArrayList<GeneralConfig>();

		if (this.data.get("IS_APP_ORGAN").equals("Y")) {
			try {
//20160216 INDEX 힌트 추가
//					ps = dbLib.prepareStatement(conn, "SELECT A.SEQ, A.ORGANIZATION_SEQ, (SELECT LTRIM(SYS_CONNECT_BY_PATH(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END, '/'), '/') FROM TB_ORGANIZATION WHERE SEQ = A.ORGANIZATION_SEQ START WITH PARENT_SEQ = 0 CONNECT BY PRIOR SEQ = PARENT_SEQ) AS ORGAN FROM TB_USER_APP_ORGAN A WHERE A.SEQ = ?");
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " SEQ,"
								+ " ORGANIZATION_SEQ,"
								+ " ("
//20160216 REVERSE 적용
//											+ " SELECT /*+ INDEX(D) */"
//													+ " LTRIM(SYS_CONNECT_BY_PATH(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END, '/'), '/')"
//												+ " FROM TB_ORGANIZATION B"
//												+ " WHERE SEQ = A.ORGANIZATION_SEQ"
//												+ " START WITH PARENT_SEQ = 0"
//												+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
										+ " SELECT /*+ INDEX(B) */"
												+ " REVERSE(LTRIM(SYS_CONNECT_BY_PATH(REVERSE(CASE WHEN IS_ENABLED = 'N' THEN '{' || NAME || '}' ELSE NAME END), '/'), '/'))"
											+ " FROM TB_ORGANIZATION B"
											+ " WHERE PARENT_SEQ = 0"
											+ " START WITH SEQ = A.ORGANIZATION_SEQ"
											+ " CONNECT BY SEQ = PRIOR PARENT_SEQ"
									+ " ) AS ORGAN"
							+ " FROM TB_USER_APP_ORGAN A"
							+ " WHERE SEQ = ?"
					);
				ps.setLong(1, seq);
				rs = ps.executeQuery();

				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();

					c.put("SEQ", rs.getLong("SEQ"));
					c.put("ORGANIZATION_SEQ", rs.getLong("ORGANIZATION_SEQ"));
					c.put("ORGAN", rs.getString("ORGAN"));

					this.appOrgan.add(c);
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
 * 아이디 중복 체크
 *
 * @param id 아이디
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String checkUniqueID(String id) throws Exception {
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

	// 중복 체크
		try {
//20160216 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "SELECT SEQ FROM TB_USER WHERE ID = ?");
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ " SEQ"
						+ " FROM TB_USER A"
						+ " WHERE ID = ?"
				);
			ps.setString(1, id);
			rs = ps.executeQuery();

			if (rs.next()) {
				error = "이미 등록된 아이디입니다.";
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
 * @param auth 권한
 * @param id 아이디
 * @param pass 패스워드
 * @param name 이름
 * @param employeeNo 사번
 * @param email 이메일
 * @param telephone 전화번호
 * @param cellphone 휴대전화
 * @param fax 팩스번호
 * @param enabled 사용 여부
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(long company, long organ, long auth, String id, String pass, String name, String employeeNo, String email, String telephone, String cellphone, String fax, String enabled) throws Exception {
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
			cs = dbLib.prepareCall(conn, "{CALL SP_USER (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
			cs.setLong(1, 0);
			cs.setLong(2, company);
			cs.setLong(3, organ);
			cs.setLong(4, auth);
			cs.setString(5, id);
			cs.setString(6, pass);
			cs.setString(7, name);
			cs.setString(8, enabled);
			cs.setString(9, email);
			cs.setString(10, telephone);
			cs.setString(11, cellphone);
			cs.setString(12, fax);
			cs.setString(13, employeeNo);
			cs.setLong(14, this.cfg.getLong("user.seq"));
			cs.registerOutParameter(15, OracleTypes.VARCHAR);
			cs.execute();

			error = cs.getString(15);
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
 * 사번으로만 운영자 등록 
 *
 * @param company 소속
 * @param organ 조직
 * @param name 이름
 * @param employeeNo 사번
 * @param enabled 사용 여부
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(long company, long organ, String name, String employeeNo, String enabled) throws Exception {
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		CallableStatement cs = null;
		String error = null;
		long auth = -1;

	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}

		
		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ INDEX(A) */"
							+ "SEQ"
						+ " FROM TB_AUTH A"
						+ " WHERE COMPANY_SEQ = ?"
							+ " AND NAME = '운영자' "
				);
			ps.setLong(1, company);
			rs = ps.executeQuery();

			if (rs.next()) {
				auth = rs.getLong("SEQ");
			}
		} catch (Exception e) {
			this.logger.error(e);
			error = e.getMessage();
		} finally {
			dbLib.close(rs);
			dbLib.close(ps);
		}

	// 등록
		try {
			cs = dbLib.prepareCall(conn, "{ CALL SP_USER (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");
			cs.setLong(1, 0);
			cs.setLong(2, company);
			cs.setLong(3, organ);
			cs.setLong(4, auth);
			cs.setString(5, employeeNo);
			cs.setString(6, employeeNo);
			cs.setString(7, name);
			cs.setString(8, enabled);
			cs.setString(9, "");
			cs.setString(10, "");
			cs.setString(11, "");
			cs.setString(12, "");
			cs.setString(13, employeeNo);
			cs.setLong(14, this.cfg.getLong("user.seq"));
			cs.registerOutParameter(15, OracleTypes.VARCHAR);
			cs.execute();

			error = cs.getString(15);
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
 * @param seq 계정번호
 * @param company 소속
 * @param organ 조직
 * @param auth 권한
 * @param pass 패스워드
 * @param employeeNo 사번
 * @param email 이메일
 * @param telephone 전화번호
 * @param cellphone 휴대전화
 * @param fax 팩스번호
 * @param enabled 사용 여부
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String update(long seq, long company, long organ, long auth, String pass, String employeeNo, String email, String telephone, String cellphone, String fax, String enabled) throws Exception {
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
			cs = dbLib.prepareCall(conn, "{ CALL SP_USER (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");
			cs.setLong(1, seq);
			cs.setLong(2, company);
			cs.setLong(3, organ);
			cs.setLong(4, auth);
			cs.setString(5, "");
			cs.setString(6, pass);
			cs.setString(7, "");
			cs.setString(8, enabled);
			cs.setString(9, email);
			cs.setString(10, telephone);
			cs.setString(11, cellphone);
			cs.setString(12, fax);
			cs.setString(13, employeeNo);
			cs.setLong(14, this.cfg.getLong("user.seq"));
			cs.registerOutParameter(15, OracleTypes.VARCHAR);
			cs.execute();

			error = cs.getString(15);
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
 * @param seq 계정번호
 * @param company 소속
 * @param organ 조직
 * @param auth 권한
 * @param pass 패스워드
 * @param employeeNo 사번
 * @param email 이메일
 * @param telephone 전화번호
 * @param cellphone 휴대전화
 * @param fax 팩스번호
 * @param enabled 사용 여부
 * @param smsSoldout 사용 여부
 * @param smsState 사용 여부
 * @return 에러가 있을 경우 에러 내용
 *
 */

//public String update(long seq, long company, long organ, long auth, String pass, String employeeNo, String email, String telephone, String cellphone, String fax, String enabled, String smsSoldout, String smsState) throws Exception {
	//2020-12-04 김태우 추가
public String update(long seq, long company, long organ, long auth, String pass, String employeeNo, String email, String telephone, String cellphone, String fax, String enabled, String smsSoldout, String smsState, String kakaoStatus) throws Exception {
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
			cs = dbLib.prepareCall(conn, "{ CALL SP_USER_2(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");
			cs.setLong(1, seq);
			cs.setLong(2, company);
			cs.setLong(3, organ);
			cs.setLong(4, auth);
			cs.setString(5, "");
			cs.setString(6, pass);
			cs.setString(7, "");
			cs.setString(8, enabled);
			cs.setString(9, email);
			cs.setString(10, telephone);
			cs.setString(11, cellphone);
			cs.setString(12, smsSoldout);
			cs.setString(13, smsState);
			/*2020-12-04 김태우 추가*/
			cs.setString(14, kakaoStatus);
			cs.setString(15, fax);
			cs.setString(16, employeeNo);
			cs.setLong(17, this.cfg.getLong("user.seq"));
			cs.registerOutParameter(18, OracleTypes.VARCHAR);
			cs.execute();

			/*2020-12-04 김태우 추가*/
			error = cs.getString(18);
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
 * @param seq 계정번호
 * @param company 소속
 * @param organ 조직
 * @param auth 권한
 * @param pass 패스워드
 * @param employeeNo 사번
 * @param email 이메일
 * @param telephone 전화번호
 * @param cellphone 휴대전화
 * @param fax 팩스번호
 * @param enabled 사용 여부
 * @param smsSoldout 사용 여부
 * @param smsState 사용 여부
 * @param smsClosing 사용 여부
 * @param KAKAOSTATUS 사용 여부
 * @return 에러가 있을 경우 에러 내용
 *
 */
//public String update(long seq, long company, long organ, long auth, String pass, String employeeNo, String email, String telephone, String cellphone, String fax, String enabled, String smsSoldout, String smsState, String smsClosing) throws Exception {
	//2020-12-04 김태우 추가
public String update(long seq, long company, long organ, long auth, String pass, String employeeNo, String email, String telephone, String cellphone, String fax, String enabled, String smsSoldout, String smsState, String smsClosing, String kakaoStatus) throws Exception {
	// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary(this.logger);
		Connection conn = null;
		CallableStatement cs = null;
		String error = null;
		System.out.println("smsClosing:" + smsClosing);
		System.out.println("kakaoStatus:" + kakaoStatus);
	// DB 연결
		conn = dbLib.getConnection(this.cfg.get("db.jdbc.name"), this.cfg.get("db.jdbc.host"), this.cfg.get("db.jdbc.user"), this.cfg.get("db.jdbc.pass"));

		if (conn == null) {
			return "DB 연결에 실패하였습니다.";
		}

	// 수정
		try {
			cs = dbLib.prepareCall(conn, "{ CALL SP_USER_3(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");
			cs.setLong(1, seq);
			cs.setLong(2, company);
			cs.setLong(3, organ);
			cs.setLong(4, auth);
			cs.setString(5, "");
			cs.setString(6, pass);
			cs.setString(7, "");
			cs.setString(8, enabled);
			cs.setString(9, email);
			cs.setString(10, telephone);
			cs.setString(11, cellphone);
			cs.setString(12, smsSoldout);
			cs.setString(13, smsState);
			cs.setString(14, smsClosing);
			//2020-12-04 김태우 추가
			cs.setString(15, kakaoStatus);
			cs.setString(16, fax);
			cs.setString(17, employeeNo);
			cs.setLong(18, this.cfg.getLong("user.seq"));
			cs.registerOutParameter(19, OracleTypes.VARCHAR);
			
			/*cs.setString(15, fax);
			cs.setString(16, employeeNo);
			cs.setLong(17, this.cfg.getLong("user.seq"));
			cs.registerOutParameter(18, OracleTypes.VARCHAR);*/
			cs.execute();

			//error = cs.getString(18);
			//2020-12-04 김태우 추가
			error = cs.getString(19);
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

	// 판매 기록 체크
//20160216 INDEX 힌트 추가
//		long check = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_SALES WHERE USER_SEQ = " + seq));
		long check = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT /*+ INDEX(A) */"
								+ " COUNT(*)"
							+ " FROM TB_SALES A"
							+ " WHERE USER_SEQ = " + seq
					)
			);

		if (check > 0) {
			dbLib.close(conn);
			return "삭제하고자 하는 계정의 판매 기록이 존재합니다.";
		}

	// COMMIT 설정
		conn.setAutoCommit(false);

	// 삭제
		try {
//20160216 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "DELETE FROM TB_USER WHERE SEQ = ?");
			ps = dbLib.prepareStatement(conn,
					"DELETE /*+ INDEX(A) */"
						+ " FROM TB_USER A"
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

	// 자판기 운영자 변경
		try {
//20160216 INDEX 힌트 추가
//			ps = dbLib.prepareStatement(conn, "UPDATE TB_VENDING_MACHINE SET USER_SEQ = 0 WHERE USER_SEQ = ?");
			ps = dbLib.prepareStatement(conn,
					"UPDATE /*+ INDEX(A) */ TB_VENDING_MACHINE A"
						+ " SET USER_SEQ = 0"
						+ " WHERE USER_SEQ = ?"
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

	// 리소스 반환
		dbLib.close(conn, dbLib.COMMIT);

		return error;
	}
/**
 * 매출 조회 조직 추가
 *
 * @param company 소속
 * @param organ 조직
 * @param depth 깊이
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String appOrganList(long company, long organ, int depth) {
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

		if (organ == 0) {
			try {
//20160216 INDEX 힌트 변경, CASE WHEN 제거
//				ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_COMPANY) */ A.SEQ, A.NAME, (SELECT MAX(DEPTH) AS DEPTH FROM TB_ORGANIZATION WHERE COMPANY_SEQ = A.SEQ) AS DEPTH FROM TB_COMPANY A WHERE A.SEQ = ? AND (CASE WHEN ? > 0 THEN A.SEQ ELSE 1 END) = (CASE WHEN ? > 0 THEN ? ELSE 1 END)");
//				ps.setLong(1, company);
//				ps.setLong(2, this.cfg.getLong("user.company"));
//				ps.setLong(3, this.cfg.getLong("user.company"));
//				ps.setLong(4, this.cfg.getLong("user.company"));
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " SEQ,"
								+ " NAME,"
								+ " ("
										+ " SELECT /*+ INDEX(B) */"
												+ " MAX(DEPTH) AS DEPTH"
											+ " FROM TB_ORGANIZATION"
											+ " WHERE COMPANY_SEQ = A.SEQ"
									+ " ) AS DEPTH"
							+ " FROM TB_COMPANY A"
							+ " WHERE SEQ = ?"
						+ (user_company > 0
								? " AND SEQ = ?"
								: ""
							)
					);
				ps.setLong(1, company);
				if (user_company > 0) ps.setLong(2, user_company);
				rs = ps.executeQuery();

				while (rs.next()) {
					GeneralConfig c = new GeneralConfig();

					c.put("SEQ", rs.getLong("SEQ"));
					c.put("NAME", rs.getString("NAME"));
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

			if (this.company.size() == 0) {
				error = "등록되지 않은 소속입니다.";
			}
		}

	// 조직
		this.organ = new ArrayList<GeneralConfig>();

		if (StringEx.isEmpty(error)) {
			try {
//20160216 INDEX 힌트 변경, CASE WHEN 제거, ORDER BY 추가
//				ps = dbLib.prepareStatement(conn, "SELECT /*+ INDEX(A PK_ORGANIZATION) */ SEQ, NAME FROM TB_ORGANIZATION A WHERE COMPANY_SEQ = ? AND IS_ENABLED = 'Y' AND SORT = 1 AND PARENT_SEQ = (CASE WHEN ? > 0 THEN ? ELSE 0 END)");
//				ps.setLong(1, company);
//				ps.setLong(2, organ);
//				ps.setLong(3, organ);
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " SEQ,"
								+ " NAME"
							+ " FROM TB_ORGANIZATION A"
							+ " WHERE COMPANY_SEQ = ?"
								+ " AND IS_ENABLED = 'Y'"
								+ " AND SORT = 1"
								+ " AND PARENT_SEQ = ?"
							+ " ORDER BY NAME, SEQ"
					);
				ps.setLong(1, company);
				ps.setLong(2, organ > 0 ? organ : 0);
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

	// 조직
		this.data = new GeneralConfig();

		if (StringEx.isEmpty(error)) {
//20160216 INDEX 힌트 추가
//			this.data.put("TITLE", dbLib.getResult(conn, "SELECT NAME FROM TB_ORGANIZATION WHERE COMPANY_SEQ = " + company + " AND DEPTH = " + depth + " AND SORT = 0"));
			this.data.put("TITLE",
					dbLib.getResult(conn,
							"SELECT /*+ INDEX(A) */"
									+ " NAME"
								+ " FROM TB_ORGANIZATION A"
								+ " WHERE COMPANY_SEQ = " + company
								+ " AND DEPTH = " + depth
								+ " AND SORT = 0"
						)
				);
		}

	// 리소스 반환
		dbLib.close(conn);

		return error;
	}
/**
 * 매출 조회 추가 조직 등록
 *
 * @param seq 계정
 * @param organ 조직
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String appOrganRegist(long seq, long organ) {
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

	// 중복 등록 체크
//20160216 INDEX 힌트 추가
//		int check = StringEx.str2int(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_USER_APP_ORGAN WHERE SEQ = " + seq + " AND ORGANIZATION_SEQ = " + organ));
		int check = StringEx.str2int(
				dbLib.getResult(conn,
						"SELECT /*+ INDEX(A) */"
								+ " COUNT(*)"
							+ " FROM TB_USER_APP_ORGAN A"
							+ " WHERE SEQ = " + seq
								+ " AND ORGANIZATION_SEQ = " + organ
					)
			);

		if (check > 0) {
			error = "이미 등록된 조직입니다.";
		}

	// 등록
		if (StringEx.isEmpty(error)) {
			try {
//20160216
//				ps = dbLib.prepareStatement(conn, "INSERT INTO TB_USER_APP_ORGAN (SEQ, ORGANIZATION_SEQ) VALUES (?, ?)");
				ps = dbLib.prepareStatement(conn,
						"INSERT INTO TB_USER_APP_ORGAN ("
								+ " SEQ,"
								+ " ORGANIZATION_SEQ"
							+ " ) VALUES ("
								+ " ?,"
								+ " ?"
							+ " )"
					);
				ps.setLong(1, seq);
				ps.setLong(2, organ);
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
 * 매출 조회 추가 조직 삭제
 *
 * @param seq 계정
 * @param organ 조직
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String appOrganDelete(long seq, long organ) {
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
		if (StringEx.isEmpty(error)) {
			try {
//20160216 INDEX 힌트 추가
//				ps = dbLib.prepareStatement(conn, "DELETE FROM TB_USER_APP_ORGAN WHERE SEQ = ? AND ORGANIZATION_SEQ = ?");
				ps = dbLib.prepareStatement(conn,
						"DELETE /*+ INDEX(A) */"
							+ " FROM TB_USER_APP_ORGAN A"
							+ " WHERE SEQ = ?"
								+ " AND ORGANIZATION_SEQ = ?"
					);
				ps.setLong(1, seq);
				ps.setLong(2, organ);
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
 * 계정 접속 로그 목록
 *
 * @param pageNo 페이지
 * @param dateType 날짜 유형
 * @param sDate 검색 시작일
 * @param eDate 검색 종료일
 * @param sField 검색 필드
 * @param sQuery 검색어
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String log(int pageNo, String dateType, String sDate, String eDate, String sField, String sQuery) throws Exception {
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
			HINT = " INDEX(B)";
			WHERE += " AND B.COMPANY_SEQ = " + this.cfg.getLong("user.company");
		}

		if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색
//20160216 INDEX 힌트 추가
//			WHERE += " AND B.ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ)";
			WHERE += " AND B.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(B_A) */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION B_A"
							+ " WHERE SORT = 1"
							+ " START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ")
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;
		}

		if (!StringEx.isEmpty(dateType)) {
			if (!StringEx.isEmpty(sDate)) { // 시작일
				HINT = " INDEX(A)" + HINT;
				WHERE += " AND A." + dateType + "_DATE >= '" + sDate + "'";
			}

			if (!StringEx.isEmpty(eDate)) { // 종료일
				if (StringEx.isEmpty(sDate)) HINT = " INDEX(A)" + HINT;
				WHERE += " AND A." + dateType + "_DATE <= '" + eDate + "'";
			}
		}

		if (!StringEx.isEmpty(sField) && !StringEx.isEmpty(sQuery)) { // 키워드
			WHERE += " AND " + sField + " LIKE '%" + sQuery + "%'";
		}

		WHERE = WHERE.replaceFirst("^ AND ", " WHERE ");

	// 총 레코드수
//		this.records = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_USER_LOG A LEFT JOIN TB_USER B ON A.SEQ = B.SEQ WHERE " + WHERE));
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT /*+ ORDERED" + HINT + " USE_HASH(B) */"
								+ " COUNT(*)"
						+ " FROM TB_USER_LOG A"
							+ " INNER JOIN TB_USER B"
								+ " ON A.SEQ = B.SEQ"
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

//20160216 INDEX 힌트 변경
//			ps = dbLib.prepareStatement(conn, "SELECT S2.*"
//					+ " FROM"
//					+ " ("
//					+ " SELECT ROWNUM AS RNUM, S1.*"
//					+ " FROM"
//					+ " ("
//					+ " SELECT /*+ ORDERED USE_NL(B C) INDEX_DESC(A UK_USER_LOG) */ A.SEQ, TO_CHAR(TO_DATE(A.IN_DATE || A.IN_TIME, 'YYYYMMDDHH24MISS'), 'YYYY-MM-DD HH24:MI:SS') AS IN_DATE,  TO_CHAR(TO_DATE(A.OUT_DATE || A.OUT_TIME, 'YYYYMMDDHH24MISS'), 'YYYY-MM-DD HH24:MI:SS') AS OUT_DATE, B.ID, B.NAME, (CASE WHEN B.AUTH_SEQ = 0 THEN '시스템' ELSE C.NAME END) AS AUTH"
//					+ " FROM TB_USER_LOG A"
//					+ " LEFT JOIN TB_USER B ON A.SEQ = B.SEQ"
//					+ " LEFT JOIN TB_AUTH C ON B.AUTH_SEQ = C.SEQ"
//					+ " WHERE " + WHERE
//					+ " ORDER BY IN_DATE DESC "
//					+ " ) S1"
//					+ " WHERE ROWNUM <= " + e
//					+ " ) S2"
//					+ " WHERE RNUM >= " + s );
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED USE_NL(C) */"
							+ " AAA.*,"
							+ " CASE WHEN AAA.AUTH_SEQ = 0 THEN '시스템' ELSE C.NAME END AS AUTH"
						+ " FROM ("
									+ " SELECT"
											+ " ROWNUM AS ROW_NUM,"
											+ " AA.*"
										+ " FROM ("
												+ " SELECT /*+ ORDERED" + HINT + " USE_HASH(B) */"
														+ " A.SEQ,"
														+ " A.IN_DATE || A.IN_TIME AS IN_DATE,"
														+ " CASE WHEN A.OUT_DATE IS NOT NULL THEN A.OUT_DATE || NVL(A.OUT_TIME, '000000') END AS OUT_DATE,"
														+ " B.ID,"
														+ " B.NAME,"
														+ " B.AUTH_SEQ"
													+ " FROM TB_USER_LOG A"
														+ " INNER JOIN TB_USER B"
															+ " ON A.SEQ = B.SEQ"
													+ WHERE
													+ " ORDER BY A.IN_DATE DESC, A.IN_TIME DESC"
											+ " ) AA"
										+ " WHERE ROWNUM <= " + e
								+ " ) AAA"
							+ " LEFT JOIN TB_AUTH C"
								+ " ON AAA.AUTH_SEQ = C.SEQ"
						+ " WHERE AAA.ROW_NUM >= " + s
				);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

				c.put("SEQ", rs.getLong("SEQ"));
//20160216 JAVA로 날짜 포멧
//				c.put("IN_DATE", rs.getString("IN_DATE"));
//				c.put("OUT_DATE", rs.getString("OUT_DATE"));
				String date = rs.getString("IN_DATE");
				c.put("IN_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: null
					);
				date = rs.getString("OUT_DATE");
				c.put("OUT_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: null
					);
				c.put("ID", rs.getString("ID"));
				c.put("NAME", rs.getString("NAME"));
				c.put("AUTH", rs.getString("AUTH"));
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
 * 계정 접속 로그 조회
 *
 * @param seq 계정
 * @param pageNo 페이지
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String log(long seq, int pageNo) throws Exception {
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
			HINT = " INDEX(B)";
			WHERE += " AND B.COMPANY_SEQ = " + this.cfg.getLong("user.company");
		}

		if (this.cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색
//20160216 INDEX 힌트 추가
//			WHERE += " AND B.ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ)";
			WHERE += " AND B.ORGANIZATION_SEQ IN ("
						+ " SELECT /*+ INDEX(B_A) */"
								+ " SEQ"
							+ " FROM TB_ORGANIZATION B_A"
							+ " WHERE SORT = 1"
							+ " START WITH PARENT_SEQ = " + this.cfg.getLong("user.organ")
							+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
					+ " )"
				;
		}

	// 총 레코드수
//20160216 INDEX 힌트 추가
//		this.records = StringEx.str2long(dbLib.getResult(conn, "SELECT COUNT(*) FROM TB_USER_LOG A LEFT JOIN TB_USER B ON A.SEQ = B.SEQ WHERE " + WHERE));
		this.records = StringEx.str2long(
				dbLib.getResult(conn,
						"SELECT" + (WHERE.length() > 0 ? " /*+ ORDERED INDEX(A)" + HINT + " USE_HASH(B) */" : " /*+ INDEX(A) */")
								+ " COUNT(*)"
							+ " FROM TB_USER_LOG A"
						+ (WHERE.length() > 0
								? " INNER JOIN TB_USER B"
									+ " ON A.SEQ = B.SEQ"
										+ WHERE
								: ""
							)
							+ " WHERE A.SEQ = " + seq
					)
			);

	// 사용자 정보
		this.data = new GeneralConfig();

		try {
			ps = dbLib.prepareStatement(conn,
					"SELECT /*+ ORDERED INDEX(B) USE_NL(C) */"
							+ " B.ID,"
							+ " B.NAME,"
							+ " B.EMPLOYEE_NO,"
							+ " CASE WHEN B.AUTH_SEQ = 0 THEN '시스템' ELSE C.NAME END AS AUTH"
						+ " FROM TB_USER B"
							+ " LEFT JOIN TB_AUTH C"
								+ " ON B.AUTH_SEQ = C.SEQ"
						+ " WHERE B.SEQ = ?"
							+ WHERE
				);
			ps.setLong(1, seq);
			rs = ps.executeQuery();

			if (rs.next()) {
				this.data.put("ID", rs.getString("ID"));
				this.data.put("NAME", rs.getString("NAME"));
				this.data.put("EMPLOYEE_NO", rs.getString("EMPLOYEE_NO"));
				this.data.put("AUTH", rs.getString("AUTH"));
			}

			if (StringEx.isEmpty(this.data.get("ID"))) {
				error = "등록되지 않았거나 조회가 불가능한 계정입니다.";
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

	// 총 페이지수
		this.pages = Pager.getSize(this.records, cfg.getInt("limit.list"));

	// 리스트 가상번호
		long no = this.records - (pageNo - 1) * cfg.getInt("limit.list");

	// 목록 가져오기
		this.list = new ArrayList<GeneralConfig>();

		try {
			int s = ((pageNo - 1) * cfg.getInt("limit.list")) + 1;
			int e = (s - 1) + cfg.getInt("limit.list");
//20160216
//			ps = dbLib.prepareStatement(conn, "SELECT S2.*"
//				+ " FROM"
//				+ " ("
//				+ " SELECT ROWNUM AS RNUM, S1.*"
//				+ " FROM"
//				+ " ("
//				+ " SELECT /*+ ORDERED USE_NL(B C) INDEX_DESC(A UK_USER_LOG) */ IN_DATE || IN_TIME AS IN_DATE_1, TO_CHAR(TO_DATE(A.IN_DATE || A.IN_TIME, 'YYYYMMDDHH24MISS'), 'YYYY-MM-DD HH24:MI:SS') AS IN_DATE_2, A.IN_IP, OUT_DATE || OUT_TIME AS OUT_DATE_1, TO_CHAR(TO_DATE(A.OUT_DATE || A.OUT_TIME, 'YYYYMMDDHH24MISS'), 'YYYY-MM-DD HH24:MI:SS') AS OUT_DATE_2, A.OUT_IP, B.ID, B.NAME, B.EMPLOYEE_NO, (CASE WHEN B.AUTH_SEQ = 0 THEN '시스템' ELSE C.NAME END) AS AUTH"
//				+ " FROM TB_USER_LOG A"
//				+ " LEFT JOIN TB_USER B ON A.SEQ = B.SEQ"
//				+ " LEFT JOIN TB_AUTH C ON B.AUTH_SEQ = C.SEQ"
//				+ " WHERE" + WHERE
//				+ " ORDER BY IN_DATE DESC "
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
											+ " SELECT" + (WHERE.length() > 0 ? " /*+ ORDERED INDEX(A)" + HINT + " USE_HASH(B) */" : " /*+ INDEX(A) */")
													+ " IN_DATE || IN_TIME AS IN_DATE,"
													+ " A.IN_IP,"
													+ " CASE WHEN A.OUT_DATE IS NOT NULL THEN A.OUT_DATE || NVL(A.OUT_TIME, '000000') END AS OUT_DATE,"
													+ " A.OUT_IP"
												+ " FROM TB_USER_LOG A"
											+ (WHERE.length() > 0
													? " INNER JOIN TB_USER B"
														+ " ON A.SEQ = B.SEQ"
															+ WHERE
													: ""
												)
												+ " WHERE A.SEQ = ?"
												+ " ORDER BY A.IN_DATE DESC, A.IN_TIME DESC"
										+ " ) AA"
									+ " WHERE ROWNUM <= " + e
							+ " )"
						+ " WHERE ROW_NUM >= " + s
				);
			ps.setLong(1, seq);
			rs = ps.executeQuery();

			while (rs.next()) {
				GeneralConfig c = new GeneralConfig();

//20160216 JAVA로 날짜 포멧
//				c.put("IN_DATE", rs.getString("IN_DATE_2"));
				String date = rs.getString("IN_DATE");
				c.put("IN_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: null
					);
				c.put("IN_IP", rs.getString("IN_IP"));
//				c.put("OUT_DATE", rs.getString("OUT_DATE_2"));
				date = rs.getString("OUT_DATE");
				c.put("OUT_DATE",
						date != null
							? date.substring(0, 4) + "-"
								+ date.substring(4, 6) + "-"
								+ date.substring(6, 8) + " "
								+ date.substring(8, 10) + ":"
								+ date.substring(10, 12) + ":"
								+ date.substring(12, 14)
							: null
					);
				c.put("OUT_IP", rs.getString("OUT_IP"));
//20160216
//				c.put("CONN_TIME", StringEx.isEmpty(rs.getString("OUT_DATE_1")) ? 0 : DateTime.getDifferTime(rs.getString("IN_DATE_1"), rs.getString("OUT_DATE_1")));
				c.put("CONN_TIME", StringEx.isEmpty(rs.getString("OUT_DATE")) ? 0 : DateTime.getDifferTime(rs.getString("IN_DATE"), rs.getString("OUT_DATE")));
				c.put("NO", no--);

				this.list.add(c);

//20160218 별도 쿼리로 조회
//				if (StringEx.isEmpty(this.data.get("ID"))) {
//					this.data.put("ID", rs.getString("ID"));
//					this.data.put("NAME", rs.getString("NAME"));
//					this.data.put("EMPLOYEE_NO", rs.getString("EMPLOYEE_NO"));
//					this.data.put("AUTH", rs.getString("AUTH"));
//				}
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
