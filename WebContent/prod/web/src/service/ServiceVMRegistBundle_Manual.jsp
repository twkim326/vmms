<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.VM
			, com.oreilly.servlet.MultipartRequest
			, com.oreilly.servlet.multipart.DefaultFileRenamePolicy
			, oracle.jdbc.*
			, javax.servlet.http.*
			, java.sql.*
			, com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.db.DBLibrary
			, jxl.Workbook
			, jxl.Sheet
			, jxl.Cell
			, java.io.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceVMRegistBundle.jsp
 *
 * 서비스 > 운영 자판기 > 등록 > 일괄 등록
 *
 * 작성일 - 2011/05/06, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0202");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print("로그인이 필요합니다.");
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth("I")) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "B");

// 인스턴스 생성
	VM objVM = new VM(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		MultipartRequest req = new MultipartRequest(request, cfg.get("data.aDir.temp"), 100 * 1024 * 1024, Common.CHARSET, new DefaultFileRenamePolicy());

		long company = StringEx.str2long(req.getParameter("company"));
		File excel = req.getFile("excel");
		
		// 실행에 사용될 변수
		DBLibrary dbLib = new DBLibrary();
		Connection conn = null;
		CallableStatement cs = null;
		Workbook workbook = null;
		Sheet sheet = null;		
		String vcode = "";
		
			//error = objVM.regist(company, excel);
			
			// 인수 체크
			if (company == 0) {
				error = "소속이 존재하지 않습니다.";
			} else if (excel == null || excel.length() <= 0) {
				error = "등록하실 엑셀을 업로드하세요.";
			} else if (!StringEx.inArray(FileEx.extension(excel.getName()), "xls".split(";"))) {
				error = "XLS 파일만 등록이 가능합니다.";
			}

			if (!StringEx.isEmpty(error)) {
				if (excel.exists()) {
					excel.delete();
				}
			}

		// DB 연결
			conn = dbLib.getConnection(cfg.get("db.jdbc.name"), cfg.get("db.jdbc.host"), cfg.get("db.jdbc.user"), cfg.get("db.jdbc.pass"));

			if (conn == null) {
				if (excel.exists()) {
					excel.delete();
				}
			}

		// COMMIT 설정
			conn.setAutoCommit(false);

		// 엑셀 읽기
			try 
			{
				workbook = Workbook.getWorkbook(excel);

				if (workbook != null) 
				{
					
					sheet = workbook.getSheet(1);

					if (sheet != null) 
					{ // 상품 등록
						if (cfg.getInt("excel.limit.vmRun.goods") > 0 && sheet.getRows() <= cfg.getInt("excel.limit.vmRun.goods")) {
							cs = dbLib.prepareCall(conn, "{CALL SP_VENDING_MACHINE_GOODS_BATCH (?, ?, ?, ?, ?, ?, ?)}");

							for (int i = 1; i < sheet.getRows(); i++) {
								Cell vm = sheet.getCell(0, i);
								Cell column = sheet.getCell(1, i);
								Cell goods = sheet.getCell(2, i);

								if (vm != null && !StringEx.isEmpty(vm.getContents())) {
									vcode = vm.getContents();
								}

								if (column == null || goods == null) {
									//continue;
									if(column == null) throw new WarningException("[" + String.valueOf(i) + "행]상품시트의 컬럼정보가 없습니다.");
									if(goods == null) throw new WarningException("[" + String.valueOf(i) + "행]상품시트의 상품정보가 없습니다.");
									
								} else if (StringEx.isEmpty(vcode) || StringEx.isEmpty(column.getContents()) || StringEx.isEmpty(goods.getContents())) {
									//continue;
									if(StringEx.isEmpty(vcode)) throw new WarningException("[" + String.valueOf(i) + "행]상품시트의 자판기코드가 없습니다.");
									if(StringEx.isEmpty(column.getContents())) throw new WarningException("[" + String.valueOf(i) + "행]상품시트의 컬럼정보가 없습니다.");
									if(StringEx.isEmpty(goods.getContents())) throw new WarningException("[" + String.valueOf(i) + "행]상품시트의 상품정보가 없습니다.");
								}

								
									cs.setString(1, cfg.get("server"));
									cs.setLong(2, company);
									cs.setString(3, vcode);
									cs.setInt(4, StringEx.str2int(column.getContents()));
									cs.setString(5, goods.getContents());
									cs.setLong(6, cfg.getLong("user.seq"));
									cs.registerOutParameter(7, OracleTypes.VARCHAR);
									cs.executeUpdate();

									error = cs.getString(7);
		
								if (!StringEx.isEmpty(error)) {
									//break;
									throw new WarningException(error);
								}
							}
						} else {

							throw new WarningException("상품 데이터를 " + StringEx.comma(cfg.getInt("excel.limit.vmRun.goods")) + "건을 초과하여 등록할 수 없습니다. (라인수 = " + StringEx.comma(sheet.getRows() - 1) + "건)");
						}
					} else {
						//error = "상품 쉬트가 존재하지 않습니다.";
					}

					if (!StringEx.isEmpty(error)) {
						
						throw new WarningException(error);
					}
					
					cs.close();
					
					// 미등록 상품 중 매출 기록이 없는 상품 삭제
					cs = dbLib.prepareCall(conn, "{CALL SP_VENDING_MACHINE_GOODS_CLEAR (?)}");
					cs.registerOutParameter(1, OracleTypes.VARCHAR);
					cs.execute();

					error = cs.getString(1);

				// 에러 처리
					if (!StringEx.isEmpty(error)) {
						throw new WarningException(error);
					}
					
					cs.close();

				// 리소스 반환
					dbLib.close(conn, dbLib.COMMIT);

				// 파일 삭제
					if (excel.exists()) {
						excel.delete();
					}
					//----------------------------------
				
				}
				else 
				{
					throw new WarningException("Workbook이 존재하지 않습니다.");
				}
			
				if (excel.exists()) 
				{
					excel.delete();
				}
				
				// 리소스 반환
				dbLib.close(conn, dbLib.COMMIT);

			}
			//waring처리
			catch(WarningException Ex)
			{
				dbLib.close(conn, dbLib.ROLLBACK);

				if (excel.exists()) {
					excel.delete();
				}
				error = Ex.getMessage();
			}
			//error 처리
			catch(SQLException Ex)
			{
				dbLib.close(conn, dbLib.ROLLBACK);

				if (excel.exists()) {
					excel.delete();
				}
				error = Ex.getMessage();
			}
			catch(Exception Ex)
			{
				dbLib.close(conn, dbLib.ROLLBACK);

				if (excel.exists()) {
					excel.delete();
				}
				error = Ex.getMessage();

			}
			finally
			{
				dbLib.close(conn);
			}
			

		if (error != null) {
			out.print(Message.alert(error, 0, "parent", "try { parent._clear(); } catch (e) {}"));
			return;
		}

		out.print("<script language='javascript'>"
			+ "window.alert('일괄등록이 완료되었습니다.');"
			+ "top.location.reload();"
			+ "</script>");
		return;
	} else {
		try {
			error = objVM.regist();
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null) {
			out.print(error);
			return;
		}
	}
%>
<%@ include file="../../header.inc.jsp" %>

<div id="window">
	<div class="title">
		<span>운영 자판기 관리</span>
	</div>

	<form method="post" name="save_" id="save_" onsubmit="return _save();" target="__save" enctype="multipart/form-data">
	<input type="hidden" name="company" id="company" value="<%=cfg.getLong("user.company")%>" />
	<table cellspacing="0" class="tableType03 tableType05" id="regist">
		<colgroup>
			<col width="110" />
			<col width="*"/>
		</colgroup>
		<tr>
			<th><span>소속</span></th>
			<td>
				<select name="company_" id="company_" class="checkForm<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>" option='{"isMust" : true, "message" : "소속을 선택하세요."}'<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
					<option value="0">- 선택하세요</option>
			<% for (int i = 0; i < objVM.company.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.company.get(i); %>
					<option value="<%=c.getLong("SEQ")%>"<%=(c.getLong("SEQ") == cfg.getLong("user.company") ? " selected" : "")%>><%=c.get("NAME")%></option>
			<% } %>
				</select>
			</td>
		</tr>
		<tr>
			<th class="last"><span>엑셀</span></th>
			<td class="last"><input type="file" name="excel" id="excel" class="checkForm txtInput txtInput2" option='{"isMust" : true, "message" : "일괄 등록할 엑셀 파일을 업로드하세요."}' /></td>
		</tr>
	</table>

	<div class="buttonArea">
		<input type="submit" value="" class="btnRegiS" />
		<input type="button" value="" class="btnCancelS" onclick="new parent.IFrame().close();" />
	</div>
	</form>

	<div style="border:1px solid #ddd; padding:10px; color:#888; font-size:11px; margin-top:15px; position:relative;">
		* 반드시 정해진 형식에 맞는 엑셀을 업로드하세요.
		<a href="<%=cfg.get("topDir")%>/common/src/down.jsp?src=vm_run.xls" style="color:#888; font-weight:bold; font-size:11px; position:absolute; right:10px; top:10px;">샘플보기</a>
	</div>
</div>

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none; left:220px; top:75px;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<iframe src="about:blank" name="__save" id="__save" style="display:none"></iframe>
<script type="text/javascript">
	function _save() {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		}

		var error = Common.checkForm($('regist'));

		if (error != '') {
			window.alert(error);
			return false;
		} else if (!confirm('입력하신 내용을 등록하시겠습니까?')) {
			return false;
		}

		$('company').value = $('company_').value;
		$('sbmsg').show();

		return true;
	}

	function _clear() {
		$('sbmsg').hide();
	}
</script>