<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.lib.db.DBLibrary
			, java.net.*
			, org.apache.log4j.Logger
			, org.apache.log4j.PropertyConfigurator
			, java.sql.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /sales/SalesPaymentDifference.jsp
 *
 * 자판기 매출정보 > 매출별 입금 현황 > 입금 예정일과 입금일이 다른 판매 내역
 *
 * 작성일 - 2011/05/11, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0002");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(Message.refresh("notranvm.jsp", null, "opener.top", true));
		return;
	}

	// 로거
	PropertyConfigurator.configure(cfg.get("config.log4j"));
	Logger logger = Logger.getLogger(getClass());



// 페이지 유형 설정
	cfg.put("window.mode", "B");


	// 실행에 사용될 변수
	DBLibrary dbLib = new DBLibrary(logger);
	Connection conn = null;
	PreparedStatement ps = null;
	ResultSet rs = null;
	String error = null;

// DB 연결
	conn = dbLib.getConnection(cfg.get("db.jdbc.name"), cfg.get("db.jdbc.host"), cfg.get("db.jdbc.user"), cfg.get("db.jdbc.pass"));

	if (conn == null) {
		out.print("{\"code\" : \"FAIL\"}");
		return;
	}
%>
<%@ include file="../header.inc.jsp" %>

<div id="window">
	<div class="title">
		<span>지난 7일간 미거래자판기</span>
	</div>
	<table cellpadding="0" cellspacing="0" class="tableType01 tableType05">
	<colgroup>
		<col width="100" />
		<col width="130" />
		<col width="*" />
		<col width="80" />
		<col width="80" />
	</colgroup>
	<thead>
		<tr>
			<th>소속</th>
			<th>조직</th>			
			<th>설치위치</th>
			<th>자판기코드</th>
			<th>단말기ID</th>
		</tr>
	</thead>
	<tbody>	
	<%

try {
		long user_company = cfg.getLong("user.company");
		String WHERE = "";
		
		if (user_company > 0) 
		{ // 소속별 :: 조직 기준
			if (cfg.get("user.operator").equals("Y")) { // 자판기 운영자
				WHERE += " AND A.USER_SEQ = " + cfg.get("user.seq");
			} else {
				WHERE += " AND A.COMPANY_SEQ = " + user_company;

				if (cfg.getLong("user.organ") > 0) { // 조직이 지정된 경우, 내 하위 조직들만 검색
//20160221 INDEX 힌트 추가, UNION 통합
//					WHERE += " AND A.ORGANIZATION_SEQ IN (SELECT SEQ FROM TB_ORGANIZATION WHERE SORT = 1 START WITH PARENT_SEQ = " + cfg.getLong("user.organ") + " CONNECT BY PRIOR SEQ = PARENT_SEQ UNION SELECT SEQ FROM TB_ORGANIZATION WHERE SEQ = " + cfg.getLong("user.organ") + ")";
					WHERE += " AND A.ORGANIZATION_SEQ IN ("
								+ " SELECT /*+ INDEX(A_A) */"
										+ " SEQ"
									+ " FROM TB_ORGANIZATION A_A"
									+ " WHERE SORT = 1"
									+ " START WITH SEQ = " + cfg.getLong("user.organ")
									+ " CONNECT BY PRIOR SEQ = PARENT_SEQ"
							+ " )"
						;
				}
			}
//20160221 INDEX 힌트 추가, 쿼리통합, JOIN을 NOT EXISTS로 변경
//			StringBuffer sbsql = new StringBuffer();	
//			sbsql.append("  select  ");
//			sbsql.append("         Y.COMPNM, Y.ORGNM ,Y.VM_CODE, Y.TERMINAL_ID, Y.PLACE ");
//			sbsql.append(" FROM  ");
//			sbsql.append("     ( ");
//			sbsql.append("         select  A.TERMINAL_ID ");
//			sbsql.append("         from TB_VENDING_MACHINE A ");
//			sbsql.append("         WHERE 1=1 ");
//			sbsql.append(WHERE);			
//			sbsql.append("         minus ");
//			sbsql.append("         select terminal_id  ");
//			sbsql.append("         from tb_sales ");
//			sbsql.append("         where ");
//			sbsql.append("         transaction_date between TO_CHAR(SYSDATE - 7,'YYYYMMDD') and TO_CHAR(SYSDATE,'YYYYMMDD') ");
//			sbsql.append("         and terminal_id in ( ");
//			sbsql.append("         select terminal_id  ");
//			sbsql.append("         from TB_VENDING_MACHINE A  ");
//			sbsql.append("         WHERE 1=1 ");
//			sbsql.append(WHERE);
//			sbsql.append("         ) ");			
//			sbsql.append("     ) X, ");
//			sbsql.append("     ( ");
//			sbsql.append("         select B.NAME COMPNM, C.NAME ORGNM, A.CODE VM_CODE, A.TERMINAL_ID, A.PLACE ");
//			sbsql.append("         from TB_VENDING_MACHINE A INNER JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ INNER JOIN TB_ORGANIZATION C ON A.ORGANIZATION_SEQ = C.SEQ");
//			sbsql.append("         WHERE 1=1 ");
//			sbsql.append(WHERE);
//			sbsql.append("     ) Y ");
//			sbsql.append(" WHERE X.terminal_id = Y.TERMINAL_ID ");
//			sbsql.append(" and X.terminal_id in (select terminalid from tbltermmst@vanbt2 where 1=1 and useflag = 'Y')  ");			
//			sbsql.append(" ORDER BY   Y.COMPNM, Y.ORGNM,Y.PLACE,Y.VM_CODE  ");
//			
//			ps = dbLib.prepareStatement(conn, sbsql.toString());
//			rs = ps.executeQuery();
//		} else { // 시스템 관리자 :: 소속 기준
//			StringBuffer sbsql = new StringBuffer();			
//			
//			sbsql.append("  select  ");
//			sbsql.append("         Y.COMPNM, Y.ORGNM ,Y.VM_CODE, Y.TERMINAL_ID, Y.PLACE ");
//			sbsql.append(" FROM  ");
//			sbsql.append("     ( ");
//			sbsql.append("         select  A.TERMINAL_ID ");
//			sbsql.append("         from TB_VENDING_MACHINE A ");
//			sbsql.append("         WHERE 1=1 ");
//			sbsql.append("         minus ");
//			sbsql.append("         select terminal_id  ");
//			sbsql.append("         from tb_sales ");
//			sbsql.append("         where ");
//			sbsql.append("         transaction_date between TO_CHAR(SYSDATE - 7,'YYYYMMDD') and TO_CHAR(SYSDATE,'YYYYMMDD') ");
//			sbsql.append("         and terminal_id in ( ");
//			sbsql.append("         select terminal_id  ");
//			sbsql.append("         from TB_VENDING_MACHINE A  ");
//			sbsql.append("         WHERE 1=1 ");
//			sbsql.append("         ) ");			
//			sbsql.append("     ) X, ");
//			sbsql.append("     ( ");
//			sbsql.append("         select B.NAME COMPNM, C.NAME ORGNM, A.CODE VM_CODE, A.TERMINAL_ID, A.PLACE ");
//			sbsql.append("         from TB_VENDING_MACHINE A INNER JOIN TB_COMPANY B ON A.COMPANY_SEQ = B.SEQ INNER JOIN TB_ORGANIZATION C ON A.ORGANIZATION_SEQ = C.SEQ");
//			sbsql.append("         WHERE 1=1 ");
//			sbsql.append("     ) Y ");
//			sbsql.append(" WHERE X.terminal_id = Y.TERMINAL_ID ");
//			sbsql.append(" and X.terminal_id in (select terminalid from tbltermmst@vanbt2 where 1=1 and useflag = 'Y')  ");			
//			sbsql.append(" ORDER BY   Y.COMPNM, Y.ORGNM,Y.PLACE,Y.VM_CODE  ");
//
//			ps = dbLib.prepareStatement(conn, sbsql.toString());
//			rs = ps.executeQuery();
		}
		ps = dbLib.prepareStatement(conn,
				"SELECT /*+ ORDERED" + (WHERE.length() > 0 ? " INDEX(A)" : "") + " USE_HASH(C D) */"
						+ " A.CODE AS VM_CODE,"
						+ " A.TERMINAL_ID,"
						+ " A.PLACE,"
						+ " C.NAME AS COMPNM,"
						+ " D.NAME AS ORGNM"
					+ " FROM ("
								+ " SELECT /*+ INDEX(B_A) */"
										+ " TERMINAL_ID"
									+ " FROM TB_VENDING_MACHINE B_A"
									+ " WHERE CODE NOT LIKE 'X%'"
							+ (user_company > 0
									? " AND COMPANY_SEQ = " + user_company
									: ""
								)
								+ " MINUS"
								+ " SELECT /*+ INDEX(B_B) */"
										+ " TERMINAL_ID"
									+ " FROM TB_SALES B_B"
									+ " WHERE TRANSACTION_DATE BETWEEN TO_CHAR(SYSDATE - 7, 'YYYYMMDD') AND TO_CHAR(SYSDATE, 'YYYYMMDD')"
								+ (user_company > 0
										? " AND COMPANY_SEQ = " + user_company
										: ""
									)
							+ ") B"
						+ " INNER JOIN TB_VENDING_MACHINE A"
								+ " ON B.TERMINAL_ID = A.TERMINAL_ID"
//							+ " INNER JOIN TBLTERMMST@VANBT X"
//								+ " ON A.TERMINAL_ID = X.TERMINALID"
//									+ " AND X.USEFLAG = 'Y'"						
						+ " INNER JOIN TB_COMPANY C"
							+ " ON A.COMPANY_SEQ = C.SEQ"
						+ " INNER JOIN TB_ORGANIZATION D"
							+ " ON A.ORGANIZATION_SEQ = D.SEQ"
					//+ " WHERE B.TERMINAL_ID IS NULL"
					+ " WHERE B.TERMINAL_ID IS NOT NULL"
						+ WHERE
					+ " ORDER BY C.NAME, D.NAME, A.PLACE, A.CODE"
			);
		rs = ps.executeQuery();

		int i = 1;

		if(rs.next()){
			do{
%>
			<tr class="<%=(i % 2 == 1 ? "bgLine" : "")%>">
			<td class="center number" nowrap><%=Html.getText(rs.getString("COMPNM"))%></td>
			<td class="left number" nowrap><%=Html.getText(rs.getString("ORGNM"))%></td>
			<td class="left number"><%=Html.getText(rs.getString("PLACE"))%></td>
			<td class="center number"><%=Html.getText(rs.getString("VM_CODE"))%></td>
			<td class="center number"><%=Html.getText(rs.getString("TERMINAL_ID"))%></td>
		</tr>
<%
				i++;	
			}while(rs.next());
		}
		
		if(i==1)
		{
%>
		<tr>
			<td colspan="5" align="center">7일간 미거래 자판기 내역이 없습니다.</td>
		</tr>
<%
		}
	} catch (Exception e) {
		logger.error(e);
		error = e.getMessage();
	} finally {
		dbLib.close(rs);
		dbLib.close(ps);
	}
%>
	</tbody>
	</table>
</div>

<%@ include file="../footer.inc.jsp" %>

<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/calendar/js/calendar.js"></script>
