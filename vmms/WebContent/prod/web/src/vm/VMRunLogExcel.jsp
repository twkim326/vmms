<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.VM
			, java.io.*
			, java.util.*
			, jxl.Workbook
			, jxl.write.WritableWorkbook
			, jxl.write.WritableSheet
			, jxl.write.WritableCellFormat
			, jxl.write.Label
			, jxl.write.WritableFont
			, jxl.format.Alignment
			, jxl.format.VerticalAlignment
			, jxl.format.Colour
			, jxl.format.Border
			, jxl.format.UnderlineStyle
			, jxl.format.BorderLineStyle
		"
	pageEncoding="UTF-8"
%><%
/**
 * /vm/VMExcel.jsp
 *
 * 자판기 운영정보 > 운정정보로그 조회 > 엑셀
 *
 * 작성일 - 2021/03/04
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0202");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(cfg.login());
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth()) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 전송된 데이터
	String sDate = StringEx.getKeyword(StringEx.charset(request.getParameter("sDate")));
	String eDate = StringEx.getKeyword(StringEx.charset(request.getParameter("eDate")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));
	
	if(StringEx.isEmpty(sDate)) sDate = com.nucco.lib.DateTime.date("yyyyMMdd");
	if(StringEx.isEmpty(eDate)) eDate = com.nucco.lib.DateTime.date("yyyyMMdd");

// 인스턴스 생성
	VM objVM = new VM(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objVM.log(sDate, eDate, sQuery);
	} catch (Exception e) {
		error = e.getMessage();
	}

// 에러 처리
	if (error != null) {
		out.print(error);
		return;
	}

// 엑셀 경로
	File excel = new File(cfg.get("data.aDir.temp") + "/" + DateTime.date("yyyyMMddHHmmssS") + ".xls");

// 엑셀 생성
	WritableWorkbook workbook = null;
	WritableSheet sheet = null;
	int row = 2;

	try {
		workbook = Workbook.createWorkbook(excel);
		sheet = workbook.createSheet("운영정보 로그조회", 0);

		WritableCellFormat format1 = new WritableCellFormat(new WritableFont(WritableFont.createFont("맑은 고딕"), 9, WritableFont.BOLD));
		out.print("test1 > " + format1);
		
		format1.setBorder(Border.ALL, BorderLineStyle.THIN);
		format1.setBackground(Colour.VERY_LIGHT_YELLOW);
		format1.setAlignment(Alignment.CENTRE);
		format1.setVerticalAlignment(VerticalAlignment.CENTRE);
		format1.setWrap(true);

		sheet.setColumnView(0, 30);
		sheet.setColumnView(1, 45);
		sheet.setColumnView(2, 20);
		sheet.setColumnView(3, 20);
		sheet.setColumnView(4, 50);
		sheet.setColumnView(5, 20);

		sheet.addCell(new Label(0, 0, "페이지명", format1));
		sheet.addCell(new Label(1, 0, "운영정보 로그조회", format1));
		//sheet.addCell(new Label(0, 1, "검색조건", format1));
		//sheet.addCell(new Label(1, 1, objVM.data.get("sDesc"), format1));
		
		sheet.addCell(new Label(0, row + 0, "보고시간", format1));
		sheet.addCell(new Label(1, row + 0, "단말기 ID", format1));
		sheet.addCell(new Label(2, row + 0, "설치위치", format1));
		sheet.addCell(new Label(3, row + 0, "품절", format1));
		sheet.addCell(new Label(4, row + 0, "주제어부", format1));
		sheet.addCell(new Label(5, row + 0, "P/D", format1));
		
		for (int i = 0; i < objVM.list.size(); i++) {
			GeneralConfig c = (GeneralConfig) objVM.list.get(i);
			/* if(c.get("IS_SOLD_OUT").equals("Y")){
				format1.setBackground(Colour.RED);
			}else{
				format1.setBackground(Colour.VERY_LIGHT_YELLOW);
			}
			*/
			sheet.addCell(new Label(0, i + row + 1, c.get("CREATE_DATE"), format1));
			sheet.addCell(new Label(1, i + row + 1, c.get("TERMINAL_ID"), format1));
			sheet.addCell(new Label(2, i + row + 1, c.get("PLACE"), format1));
			sheet.addCell(new Label(3, i + row + 1, c.get("SOLD_OUT"), format1));
			sheet.addCell(new Label(4, i + row + 1, c.get("CONTROL_ERROR"), format1));
			sheet.addCell(new Label(5, i + row + 1, c.get("PD_ERROR"), format1));
		}

		workbook.write();
	} catch (Exception e) {
		error = e.getMessage();
	} finally {
		try {
			if (workbook != null) {
				workbook.close();
				workbook = null;
			}
		} catch (Exception e_) {
		}
	}

// 에러 처리
	if (!StringEx.isEmpty(error)) {
		out.print(error);
		return;
	}

// 파일명
	String excelName = StringEx.encode("운영정보로그조회");

	if (request.getHeader("User-Agent").indexOf("MSIE") >= 0) {
		excelName = StringEx.charset(excelName, "MS949", "ISO-8859-1");
	}

// 헤더 수정
	response.reset();
	response.setHeader("content-type", "application/x-msexcel");
	response.setHeader("content-disposition", "attachment; filename=" + excelName + ".xls");
	response.setHeader("content-length", StringEx.long2str(excel.length()));

// 다운로드
	out.clear();
	// java.lang.IllegalStateException: 이 응답을 위해 getOutputStream()이 이미 호출되었습니다.  오류 수정
	// 호출하는 jsp에서 OutputStream을 호출해서 중복 호출이 되기 때문에 현재 Excel 페이지 정보는 저장 후  호출하는 페이지에서 자원 해제한다.
	out=pageContext.pushBody();  
	FileEx.write(response, excel);

// 파일 삭제
	excel.delete();
%>