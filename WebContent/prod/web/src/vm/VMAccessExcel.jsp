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
 * /vm/VMAccessExcel.jsp
 *
 * 자판기 운영정보 > 출입단말상태 조회 > 엑셀
 *
 * 작성일 - 2020/12/23, Chae
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0307");

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
	long company = StringEx.str2long(request.getParameter("company"), 0);
	long organ = StringEx.str2long(request.getParameter("organ"), 0);
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));

// 인스턴스 생성
	VM objVM = new VM(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objVM.getAccessList(company, organ, -1 ,sField, sQuery);
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
	int row = 8;
	int summaryRow=4;

	try {
		workbook = Workbook.createWorkbook(excel);
		sheet = workbook.createSheet("출입단말상태 조회", 0);

		WritableCellFormat format1 = new WritableCellFormat(new WritableFont(WritableFont.createFont("맑은 고딕"), 9, WritableFont.BOLD));
		format1.setBorder(Border.ALL, BorderLineStyle.THIN);
		format1.setBackground(Colour.VERY_LIGHT_YELLOW);
		format1.setAlignment(Alignment.CENTRE);
		format1.setVerticalAlignment(VerticalAlignment.CENTRE);
		format1.setWrap(true);

		WritableCellFormat format2 = new WritableCellFormat(new WritableFont(WritableFont.createFont("맑은 고딕"), 9));
		format2.setBorder(Border.ALL, BorderLineStyle.THIN);
		format2.setAlignment(Alignment.CENTRE);
		format2.setVerticalAlignment(VerticalAlignment.CENTRE);
		format2.setWrap(true);

		WritableCellFormat format3 = new WritableCellFormat(new WritableFont(WritableFont.createFont("맑은 고딕"), 9));
		format3.setBorder(Border.ALL, BorderLineStyle.THIN);
		format3.setAlignment(Alignment.RIGHT);
		format3.setVerticalAlignment(VerticalAlignment.CENTRE);
		format3.setWrap(true);

		WritableCellFormat format5 = new WritableCellFormat(new WritableFont(WritableFont.createFont("맑은 고딕"), 9));
		format5.setBorder(Border.ALL, BorderLineStyle.THIN);
		format5.setVerticalAlignment(VerticalAlignment.CENTRE);
		format5.setWrap(true);

		// 열 너비 지정
		sheet.setColumnView(0, 15);
		sheet.setColumnView(1, 25);
		sheet.setColumnView(2, 45);
		sheet.setColumnView(3, 20);
		sheet.setColumnView(4, 20);
		sheet.setColumnView(5, 20);
		sheet.setColumnView(6, 20);
		sheet.setColumnView(7, 20);
		sheet.setColumnView(8, 10);
		sheet.setColumnView(9, 20);
		sheet.setColumnView(10, 20);

		sheet.addCell(new Label(0, 0, "페이지명", format1));
		sheet.addCell(new Label(1, 0, "출입단말상태 조회", format1));
		sheet.addCell(new Label(0, 1, "검색조건", format1));
		sheet.addCell(new Label(1, 1, objVM.data.get("sDesc"), format1));

		sheet.addCell(new Label(0, row + 0, "순번", format1));
		sheet.addCell(new Label(1, row + 0, "위치 코드", format1));
		sheet.addCell(new Label(2, row + 0, "위치 명", format1));
		sheet.addCell(new Label(3, row + 0, "등록 일자", format1));
		sheet.addCell(new Label(4, row + 0, "운영 상태", format1));
		sheet.addCell(new Label(5, row + 0, "단말기 기종", format1));
		sheet.addCell(new Label(6, row + 0, "단말기 ID", format1));
		sheet.addCell(new Label(7, row + 0, "모뎀 번호", format1));
		sheet.addCell(new Label(8, row + 0, "단말기 상태", format1));
		sheet.addCell(new Label(9, row + 0, "최종 수집시간", format1));
		sheet.addCell(new Label(10, row + 0, "최종 거래시간", format1));

		for (int i = 0; i < objVM.list.size(); i++) {
			GeneralConfig c = (GeneralConfig) objVM.list.get(i);
			sheet.addCell(new Label(0, i + row + 1, c.get("SEQ"), format2));
			sheet.addCell(new Label(1, i + row + 1, c.get("CODE"), format2));
			sheet.addCell(new Label(2, i + row + 1, c.get("PLACE"), format2));
			sheet.addCell(new Label(3, i + row + 1, c.get("REGIST_DATE"), format2));
			sheet.addCell(new Label(4, i + row + 1, c.get("CONTROL_ERROR"), format2));
			sheet.addCell(new Label(5, i + row + 1, c.get("MODEL"), format2));
			sheet.addCell(new Label(6, i + row + 1, c.get("TERMINAL_ID"), format2));
			sheet.addCell(new Label(7, i + row + 1, c.get("MODEM_NBR"), format2));
			sheet.addCell(new Label(8, i + row + 1, c.get("TERM_STATUS"), format2));
			sheet.addCell(new Label(9, i + row + 1, c.get("CREATE_DATE"), format2));
			sheet.addCell(new Label(10, i + row + 1, c.get("FINAL_TRAN_DATE"), format2));
		}

		// Summary 요약 헤더
		sheet.addCell(new Label(0, summaryRow+0,"전체 수", format1));
		sheet.addCell(new Label(1, summaryRow+0,"정상(건)", format1));
		sheet.addCell(new Label(2, summaryRow+0,"비정상(건)", format1));
		sheet.addCell(new Label(3, summaryRow+0,"가동율(%)", format1));
		sheet.addCell(new Label(4, summaryRow+0,"폐점(건)", format1));
		sheet.addCell(new Label(5, summaryRow+0,"휴점(건)", format1));
		// Summary 요약 컨텐츠
		sheet.addCell(new Label(0, summaryRow+1,objVM.data.get("ALL_COUNT"), format2));
		sheet.addCell(new Label(1, summaryRow+1,objVM.data.get("NORMAL_COUNT"), format2));
		sheet.addCell(new Label(2, summaryRow+1,objVM.data.get("ABNORMAL_COUNT"), format2));
		sheet.addCell(new Label(3, summaryRow+1,(objVM.data.getInt("NORMAL_COUNT")*100 / objVM.data.getInt("ALL_COUNT"))+"%", format2));
		sheet.addCell(new Label(4, summaryRow+1,objVM.data.get("CLOSED_COUNT"), format2));
		sheet.addCell(new Label(5, summaryRow+1,objVM.data.get("REST_COUNT"), format2));
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
	String excelName = StringEx.encode("출입단말상태조회");

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