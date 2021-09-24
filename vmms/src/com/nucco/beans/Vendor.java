package com.nucco.beans;

/**
 Vendor.java
 *
 * 공급자
 *
 * 작성일 - 2011/04/28, 황재원
 *
 */

import java.util.*;
import java.io.File;
import java.sql.*;
import com.nucco.*;
import com.nucco.cfg.*;
import com.nucco.lib.*;
import com.nucco.lib.db.DBLibrary;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Vendor {
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
 * 상품 공급자 목록
 *
 */
	public ArrayList<GeneralConfig> list;
/**
 * 카드사 리스트
 *
 */	
	public ArrayList<GeneralConfig> cardList;
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
	public Vendor(GlobalConfig cfg) throws Exception {
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
			//WHERE += " AND COMPANY_SEQ = " + this.cfg.getLong("user.company");
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
							+ " FROM TB_VENDOR A"
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
													+ " TO_CHAR(A.CREATE_DATE, 'YYYY-MM-DD') AS CREATE_DATE,"
													+ " TO_CHAR(A.MODIFY_DATE, 'YYYY-MM-DD') AS MODIFY_DATE"
												+ " FROM TB_VENDOR A"
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
				ps = dbLib.prepareStatement(conn,
						"SELECT /*+ INDEX(A) */"
								+ " *"
							+ " FROM TB_VENDOR A"
							+ " WHERE SEQ = ?"
					);
				ps.setLong(1, seq);
				rs = ps.executeQuery();

				if (rs.next()) {
					this.data.put("NAME", rs.getString("NAME"));
					this.data.put("MEMO", rs.getString("MEMO"));
				} else {
					error = "등록되지 않았거나 조회가 불가능한 상품 공급자입니다.";
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
 * @return 에러가 있을 경우 에러 내용
 *
 */
	public String regist(long seq, String name, String memo) throws Exception {
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
				ps = dbLib.prepareStatement(conn,
						"UPDATE /*+ INDEX(A) */ TB_VENDOR A"
							+ " SET NAME = ?,"
								+ " MEMO = ?,"
								+ " MODIFY_USER_SEQ = ?,"
								+ " MODIFY_DATE = SYSDATE"
							+ " WHERE SEQ = ?"
					);
				ps.setString(1, name);
				ps.setString(2, memo);
				ps.setLong(3, this.cfg.getLong("user.seq"));
				ps.setLong(4, seq);
				ps.executeUpdate();
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(ps);
			}

		} else {
			try {
				ps = dbLib.prepareStatement(conn,
						"INSERT INTO TB_VENDOR ("
								+ "	SEQ,"
								+ " NAME,"
								+ " MEMO,"
								+ " CREATE_USER_SEQ,"
								+ " CREATE_DATE"
							+ " ) VALUES ("
								+ " SQ_VENDOR.NEXTVAL,"
								+ " ?,"
								+ " ?,"
								+ " ?,"
								+ " SYSDATE"
							+ " )"
					);
				ps.setString(1, name);
				ps.setString(2, memo);
				ps.setLong(3, this.cfg.getLong("user.seq"));
				ps.executeUpdate();
			} catch (Exception e) {
				this.logger.error(e);
				error = e.getMessage();
			} finally {
				dbLib.close(ps);
			}

			sequence = StringEx.str2long(dbLib.getResult(conn, "SELECT SQ_VENDOR.CURRVAL FROM DUAL"));
		}

		if (!StringEx.isEmpty(error)) {
			dbLib.close(conn, dbLib.ROLLBACK);
			return error;
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

			ps = dbLib.prepareStatement(conn,
					"DELETE /*+ INDEX(A) */"
						+ " FROM TB_VENDOR A"
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
	/**
	 * 카드사 심사현황 등록
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
			String biz_no = null;
			
			int success = 0;
			int failure = 0;
			
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
				
				workbook = Workbook.getWorkbook(excel);

				if (workbook != null) {
					sheet = workbook.getSheet(0);
					if (sheet != null) {
						if (sheet.getRows() <= 0) {
							error = "엑셀에 데이터가 존재하지 않습니다.";
						} else {
							if (this.cfg.getInt("excel.limit") > 0 && sheet.getRows() <= this.cfg.getInt("excel.limit")) {
								for (int i = 1; i < sheet.getRows(); i++) {
									Cell cellName = sheet.getCell(0, i);
									Cell cellNum = sheet.getCell(1, i);
									Cell cellSS = sheet.getCell(2, i);
									Cell cellSH = sheet.getCell(3, i);
									Cell cellBC = sheet.getCell(4, i);
									Cell cellKB = sheet.getCell(5, i);
									Cell cellHD = sheet.getCell(6, i);
									Cell cellLT = sheet.getCell(7, i);
									Cell cellNH = sheet.getCell(8, i);
									Cell cellHN = sheet.getCell(9, i);
																	
									if ( cellName == null || cellNum == null) {
										continue;
									} 
									
									biz_no = dbLib.getResult(conn,
											"SELECT /*+ INDEX(A) */"
													+ "BIZ_NO"
											+ " FROM TB_CARD_REGIST A"
											+ " WHERE "
												+ "	( BIZ_NO = '" + cellNum.getContents().trim() + "'"
													+ " AND BIZ_NAME = '" + cellName.getContents().trim() + "'"
												+ " )"
									);
									
									
									try {		
										if (!biz_no.equals("")) {
											// 기존에 존재하는 상품 
											ps = dbLib.prepareStatement(conn,
												"UPDATE /*+ INDEX(A) */ TB_CARD_REGIST A"
													+ " SET  SSC = ?,"
														+ " SHC = ?,"
														+ " BCC = ?,"
														+ " KBC = ?,"
														+ " HDC = ?,"
														+ " LTC = ?,"
														+ " NHC = ?,"
														+ " HNC = ?,"
														+ " MODIFY_DATE = SYSDATE"
													+ " WHERE BIZ_NAME = ? "
													+ " AND BIZ_NO = ?"
											);
		
											ps.setString(1, cellSS.getContents().trim());
											ps.setString(2, cellSH.getContents().trim());
											ps.setString(3, cellBC.getContents().trim());
											ps.setString(4, cellKB.getContents().trim());
											ps.setString(5, cellHD.getContents().trim());
											ps.setString(6, cellLT.getContents().trim());
											ps.setString(7, cellNH.getContents().trim());
											ps.setString(8, cellHN.getContents().trim());
											ps.setString(9, cellName.getContents().trim());
											ps.setString(10, cellNum.getContents().trim());
											ps.executeUpdate();
											
											ps.close();
										} else {
											ps = dbLib.prepareStatement(conn,
													"INSERT INTO TB_CARD_REGIST ("
															+ " BIZ_NAME,"
															+ " BIZ_NO,"
															+ " SSC,"
															+ " SHC,"
															+ " BCC,"
															+ " KBC,"
															+ " HDC,"
															+ " LTC,"
															+ " NHC,"
															+ " HNC,"
															+ " CREATE_DATE,"
															+ " MODIFY_DATE"
														+ " ) VALUES ("
															+ " ?,"
															+ " ?,"
															+ " ?,"
															+ " ?,"
															+ " ?,"
															+ " ?,"
															+ " ?,"
															+ " ?,"
															+ " ?,"
															+ " ?,"
															+ " SYSDATE,"
															+ " ''"
														+ " )"
												);
											ps.setString(1, cellName.getContents().trim());
											ps.setString(2, cellNum.getContents().trim());											
											ps.setString(3, cellSS.getContents().trim());
											ps.setString(4, cellSH.getContents().trim());
											ps.setString(5, cellBC.getContents().trim());
											ps.setString(6, cellKB.getContents().trim());
											ps.setString(7, cellHD.getContents().trim());
											ps.setString(8, cellLT.getContents().trim());
											ps.setString(9, cellNH.getContents().trim());
											ps.setString(10, cellHN.getContents().trim());
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
		
		/**
		 * 카드사 심사현황 조회
		 * *******************************************************************************************************************************************************************
		 * *******************************************************************************************************************************************************************
		 * *******************************************************************************************************************************************************************
		 *
		 * @return 에러가 있을 경우 에러 내용
		 *
		 */
			public String cardSearch(String bizNo){
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
				this.cardList = new ArrayList<GeneralConfig>();
				try {
					ps = dbLib.prepareStatement(conn,
							"SELECT /*+ INDEX(A) */"
									+ " BIZ_NAME"
									+ ", BIZ_NO"
									+ ", SSC"
									+ ", SHC"
									+ ", BCC"
									+ ", KBC"
									+ ", HDC"
									+ ", LTC"
									+ ", NHC"
									+ ", HNC"
								+ " FROM TB_CARD_REGIST A"
								+ " WHERE (BIZ_NO, CREATE_DATE) IN ("
									+ " SELECT BIZ_NO, MAX(CREATE_DATE) FROM TB_CARD_REGIST"
									+ " WHERE BIZ_NO = ?"
									+ " GROUP BY BIZ_NO"
								+ " )"
						);
					ps.setString(1, bizNo);
					rs = ps.executeQuery();
					
					while (rs.next()) {
						GeneralConfig c = new GeneralConfig();
						c.put("BIZ_NAME", rs.getString("BIZ_NAME"));
						c.put("BIZ_NO", rs.getString("BIZ_NO"));
						c.put("SSC", rs.getString("SSC"));
						c.put("SHC", rs.getString("SHC"));
						c.put("BCC", rs.getString("BCC"));
						c.put("KBC", rs.getString("KBC"));
						c.put("HDC", rs.getString("HDC"));
						c.put("LTC", rs.getString("LTC"));
						c.put("NHC", rs.getString("NHC"));
						c.put("HNC", rs.getString("HNC"));
						this.cardList.add(c);
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