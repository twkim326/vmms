<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Sales
			, java.io.*
			, java.util.*
			, jxl.Workbook
			, jxl.write.WritableWorkbook
			, jxl.write.WritableSheet
			, jxl.write.WritableCellFormat
			, jxl.write.Label
			, jxl.write.Number
			, jxl.write.NumberFormats
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
 * /sales/SalesRealTimeExcel.jsp
 *
 * 자판기 매출정보 > 거래내역 > 엑셀
 *
 * 작성일 - 2011/04/07, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0402");

// 로그인을 하지 않았을 경우
// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(Message.refresh(cfg.get("script.login") + "?goUrl=" + StringEx.encode(cfg.get("topDir") + "/web/src/sales/SalesRealTime.jsp"), null, "top"));
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth()) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 전송된 데이터
	String sType = StringEx.getKeyword(StringEx.charset(request.getParameter("sType")));
	long company = StringEx.str2long(request.getParameter("company"), 0);
	long organ = StringEx.str2long(request.getParameter("organ"), 0);
	long place = StringEx.str2long(request.getParameter("place"), 0);
	int pageNo = StringEx.str2int(request.getParameter("page"), 1);
	String sDate = StringEx.getKeyword(StringEx.charset(request.getParameter("sDate")));
	String eDate = StringEx.getKeyword(StringEx.charset(request.getParameter("eDate")));


	long organSeq = StringEx.str2long(request.getParameter("organSeq"));
	long placeSeq = StringEx.str2long(request.getParameter("placeSeq"));
	String startDate = StringEx.getKeyword(StringEx.charset(request.getParameter("startDate")));
	String endDate = StringEx.getKeyword(StringEx.charset(request.getParameter("endDate")));
	String terminalId = StringEx.getKeyword(StringEx.charset(request.getParameter("terminalId")));

	long depth = StringEx.str2long(request.getParameter("depth"), 0);

// 인스턴스 생성
	Sales objSales = new Sales(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objSales.salesCountRealTime(company, organSeq, depth, placeSeq, startDate, endDate, terminalId, 0);
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

	try {
		WritableCellFormat format1 = new WritableCellFormat(new WritableFont(WritableFont.createFont("맑은 고딕"), 9, WritableFont.BOLD), NumberFormats.TEXT);
		format1.setBorder(Border.ALL, BorderLineStyle.THIN);
		format1.setBackground(Colour.VERY_LIGHT_YELLOW);
		format1.setAlignment(Alignment.CENTRE);
		format1.setVerticalAlignment(VerticalAlignment.CENTRE);
		format1.setWrap(true);

		WritableCellFormat format2 = new WritableCellFormat(new WritableFont(WritableFont.createFont("맑은 고딕"), 9), NumberFormats.TEXT);
		format2.setBorder(Border.ALL, BorderLineStyle.THIN);
		format2.setAlignment(Alignment.CENTRE);
		format2.setVerticalAlignment(VerticalAlignment.CENTRE);
		format2.setWrap(true);

		WritableCellFormat format3 = new WritableCellFormat(new WritableFont(WritableFont.createFont("맑은 고딕"), 9), NumberFormats.THOUSANDS_INTEGER);
		format3.setBorder(Border.ALL, BorderLineStyle.THIN);
		format3.setAlignment(Alignment.RIGHT);
		format3.setVerticalAlignment(VerticalAlignment.CENTRE);
		format3.setWrap(true);

		WritableCellFormat format5 = new WritableCellFormat(new WritableFont(WritableFont.createFont("맑은 고딕"), 9), NumberFormats.TEXT);
		format5.setBorder(Border.ALL, BorderLineStyle.THIN);
		format5.setVerticalAlignment(VerticalAlignment.CENTRE);
		format5.setWrap(true);

		int col = 0;
		int row = 0;
		workbook = Workbook.createWorkbook(excel);
		sheet = workbook.createSheet("거래내역", 0);

		sheet.addCell(new Label(0, row, "페이지명", format1));
		sheet.addCell(new Label(1, row, "거래내역", format5));
		sheet.mergeCells(1, 0, 13, row);
		row++;

		sheet.addCell(new Label(0, row, "검색조건", format1));
		sheet.addCell(new Label(1, row, objSales.data.get("sDesc"), format5));
		sheet.mergeCells(1, 1, 13, row);
		row += 2;

		sheet.addCell(new Label(col, row, "거래일", format1));
		sheet.setColumnView(col++, 20);
		sheet.addCell(new Label(col, row, "조직", format1));
		sheet.setColumnView(col++, 30);
		sheet.addCell(new Label(col, row, "설치 위치", format1));
		sheet.setColumnView(col++, 50);
		sheet.addCell(new Label(col, row, "자판기 코드", format1));
		sheet.setColumnView(col++, 15);
		sheet.addCell(new Label(col, row, "상품", format1));
		sheet.setColumnView(col++, 40);
		sheet.addCell(new Label(col, row, "결제금액", format1));
		sheet.setColumnView(col++, 10);
		sheet.addCell(new Label(col, row, "지불형태", format1));
		sheet.setColumnView(col++, 10);
		sheet.addCell(new Label(col, row, "입력유형", format1));
		sheet.setColumnView(col++, 10);
		sheet.addCell(new Label(col, row, "진행상태", format1));
		sheet.setColumnView(col++, 15);
		sheet.addCell(new Label(col, row, "마감일시", format1));
		sheet.setColumnView(col++, 20);
		sheet.addCell(new Label(col, row, "입금(예정)일", format1));
		sheet.setColumnView(col++, 20);
		sheet.addCell(new Label(col, row, "취소일시", format1));
		sheet.setColumnView(col++, 20);
		sheet.addCell(new Label(col, row, "단말기 ID", format1));
		sheet.setColumnView(col++, 15);
		sheet.addCell(new Label(col, row, "거래번호", format1));
		sheet.setColumnView(col, 20);
		row++;

		for (int i = 0; i < objSales.list.size(); i++, row++) {
			GeneralConfig c = (GeneralConfig) objSales.list.get(i);

			col = 0;
			sheet.addCell(new Label(col++, row, c.get("TRANSACTION_DATE"), format2));
			sheet.addCell(new Label(col++, row, c.get("ORGAN"), format5));
			sheet.addCell(new Label(col++, row, c.get("PLACE"), format5));
			sheet.addCell(new Label(col++, row, c.get("VMCODE"), format2));
			sheet.addCell(new Label(col++, row, c.get("PRODUCT"), format5));
			sheet.addCell(new Number(col++, row, c.getLong("AMOUNT"), format3));
			sheet.addCell(new Label(col++, row, c.get("PAY_TYPE").equals("선불") ? c.get("PAY_CARD") : c.get("PAY_TYPE"), format2));
			sheet.addCell(new Label(col++, row, c.get("INPUT_TYPE"), format2));
			sheet.addCell(new Label(col++, row, c.get("PAY_STEP"), format2));
			sheet.addCell(new Label(col++, row, c.get("CLOSING_DATE"), format2));
			sheet.addCell(new Label(col++, row, c.get("PAY_DATE"), format2));
			sheet.addCell(new Label(col++, row, c.get("CANCEL_DATE"), format2));
			sheet.addCell(new Label(col++, row, c.get("TERMINAL_ID"), format2));
			sheet.addCell(new Label(col, row, c.get("TRANSACTION_NO"), format2));
		}

		sheet.getSettings().setAutomaticFormulaCalculation(true);
		sheet.getSettings().setRecalculateFormulasBeforeSave(true);

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
	String excelName = StringEx.encode("거래내역");

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