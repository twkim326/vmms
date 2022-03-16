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
			, jxl.write.Formula
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
 * /sales/SalesClosingExcel.jsp
 *
 * 자판기 매출정보 > 매출 마감현황 > 엑셀
 *
 * 작성일 - 2011/04/09, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0406");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(Message.refresh(cfg.get("script.login") + "?goUrl=" + StringEx.encode(cfg.get("topDir") + "/web/src/sales/SalesClosing.jsp"), null, "top"));
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
	long place = StringEx.str2long(request.getParameter("place"), 0);
	
	String pType = request.getParameter("pType");
	String sDate = StringEx.getKeyword(StringEx.charset(request.getParameter("sDate")));
	String eDate = StringEx.getKeyword(StringEx.charset(request.getParameter("eDate")));
	int oMode = StringEx.str2int(request.getParameter("oMode"), -1);
	int oType = StringEx.str2int(request.getParameter("oType"), -1);

	long depth = StringEx.str2long(request.getParameter("depth"), 0);
	
	if (oMode == -1) oMode = 1;
	if (oType == -1) oType = 0;


// 인스턴스 생성
	Sales objSales = new Sales(cfg);

// 메서드 호출
	String error = null;

	try {
		if (pType.equals("0") )
			error = objSales.salesCountRawList(company, organ, depth, place, sDate, eDate, oMode, oType);
		else
			error = objSales.salesCountRawListNew(company, organ, depth, place, sDate, eDate, oMode, oType);
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
	// 타이틀 속성
		WritableCellFormat format1 = new WritableCellFormat(new WritableFont(WritableFont.createFont("맑은 고딕"), 9, WritableFont.BOLD), NumberFormats.TEXT);
		format1.setBorder(Border.ALL, BorderLineStyle.THIN);
		format1.setBackground(Colour.VERY_LIGHT_YELLOW);
		format1.setAlignment(Alignment.CENTRE);
		format1.setVerticalAlignment(VerticalAlignment.CENTRE);
		format1.setWrap(false);

	// 조직/마감시간 속성
		WritableCellFormat format2 = new WritableCellFormat(new WritableFont(WritableFont.createFont("맑은 고딕"), 9), NumberFormats.TEXT);
		format2.setBorder(Border.ALL, BorderLineStyle.THIN);
		format2.setAlignment(Alignment.CENTRE);
		format2.setVerticalAlignment(VerticalAlignment.CENTRE);
		format2.setWrap(false);

	// 건수/금액 속성
		//20160121 숫자 형식을 통화로 설정
		WritableCellFormat format3 = new WritableCellFormat(new WritableFont(WritableFont.createFont("맑은 고딕"), 9), NumberFormats.THOUSANDS_INTEGER);
		format3.setBorder(Border.ALL, BorderLineStyle.THIN);
		format3.setAlignment(Alignment.RIGHT);
		format3.setVerticalAlignment(VerticalAlignment.CENTRE);
		format3.setWrap(false);

	// 합계 속성
		//20160121 숫자 형식을 통화로 설정
		WritableCellFormat format4 = new WritableCellFormat(new WritableFont(WritableFont.createFont("맑은 고딕"), 9), NumberFormats.THOUSANDS_INTEGER);
		format4.setBorder(Border.ALL, BorderLineStyle.THIN);
		format4.setBackground(Colour.VERY_LIGHT_YELLOW);
		format4.setAlignment(Alignment.RIGHT);
		format4.setVerticalAlignment(VerticalAlignment.CENTRE);
		format4.setWrap(false);

	// 검색조건 속성
		WritableCellFormat format5 = new WritableCellFormat(new WritableFont(WritableFont.createFont("맑은 고딕"), 9), NumberFormats.TEXT);
		format5.setBorder(Border.ALL, BorderLineStyle.THIN);
		format5.setVerticalAlignment(VerticalAlignment.CENTRE);
		format5.setWrap(false);

	// 합계 마감일 속성
		WritableCellFormat format6 = new WritableCellFormat(new WritableFont(WritableFont.createFont("맑은 고딕"), 9), NumberFormats.TEXT);
		format6.setBorder(Border.ALL, BorderLineStyle.THIN);
		format6.setBackground(Colour.VERY_LIGHT_YELLOW);
		format6.setAlignment(Alignment.CENTRE);
		format6.setVerticalAlignment(VerticalAlignment.CENTRE);
		format6.setWrap(false);

		int col = 0;
		int row = 0;
		workbook = Workbook.createWorkbook(excel);
		sheet = workbook.createSheet("마감집계", 0);


	// 검색 조건
		sheet.addCell(new Label(0, row, "페이지명", format1));
		sheet.addCell(new Label(1, row, "마감집계", format5));
		row++;

		sheet.addCell(new Label(0, row, "검색조건", format1));
		sheet.addCell(new Label(1, row, objSales.data.get("sDesc"), format5));
		row += 2;

	// 타이틀 생성
		sheet.addCell(new Label(col, row, objSales.data.get("ORGAN"), format1));
		sheet.mergeCells(col, row, col + 1, row + 2);
		sheet.setColumnView(col++, 20);
		sheet.setColumnView(col++, 30);

		sheet.addCell(new Label(col, row, "설치위치", format1));
		sheet.mergeCells(col, row, col, row + 2);
		sheet.setColumnView(col++, 40);

		sheet.addCell(new Label(col, row, "자판기코드", format1));
		sheet.mergeCells(col, row, col, row + 2);
		sheet.setColumnView(col++, 10);
		
		sheet.addCell(new Label(col, row, "단말기ID", format1));
		sheet.mergeCells(col, row, col, row + 2);
		sheet.setColumnView(col++, 15);

	// 마감
		sheet.addCell(new Label(col, row, "마감", format1));
		sheet.mergeCells(col, row, col + 7 , row);
		sheet.addCell(new Label(col, row + 1, "마감기간", format1));
		sheet.mergeCells(col, row + 1, col + 1, row + 1);

		sheet.addCell(new Label(col, row + 2, "시작일시", format1));
		sheet.setColumnView(col++, 20);
		sheet.addCell(new Label(col, row + 2, "종료일시", format1));
		sheet.setColumnView(col++, 20);

		sheet.addCell(new Label(col, row + 1, "현금", format1));
		sheet.mergeCells(col, row + 1, col + 1, row + 1);
		sheet.addCell(new Label(col, row + 2, "건수", format1));
		sheet.setColumnView(col++, 10);
		sheet.addCell(new Label(col, row + 2, "금액", format1));
		sheet.setColumnView(col++, 15);

		sheet.addCell(new Label(col, row + 1, "카드", format1));
		sheet.mergeCells(col, row + 1, col + 1, row + 1);
		sheet.addCell(new Label(col, row + 2, "건수", format1));
		sheet.setColumnView(col++, 10);
		sheet.addCell(new Label(col, row + 2, "금액", format1));
		sheet.setColumnView(col++, 15);

		sheet.addCell(new Label(col, row + 1, "합계", format1));
		sheet.mergeCells(col, row + 1, col + 1, row + 1);
		sheet.addCell(new Label(col, row + 2, "건수", format1));
		sheet.setColumnView(col++, 10);
		sheet.addCell(new Label(col, row + 2, "금액", format1));
		sheet.setColumnView(col, 15);

		sheet.mergeCells(1, 0, col, 0);
		sheet.mergeCells(1, 1, col, 1);

		row += 3;

		for (int i = 0; i < objSales.list.size(); i++) {
			GeneralConfig c = (GeneralConfig) objSales.list.get(i);
			

			col = 0;
			if (StringEx.isEmpty(c.get("PARENT_ORGAN"))) {
				sheet.addCell(new Label(col, row, c.get("ORGAN"), format5));
				sheet.mergeCells(col, row, col + 1, row );
				col += 2;
			} else {
				sheet.addCell(new Label(col, row, c.get("PARENT_ORGAN"), format5));
				col++;
				sheet.addCell(new Label(col, row, c.get("ORGAN"), format5));
				col++;
			}
			sheet.addCell(new Label(col, row, c.get("PLACE"), format5));
			col++;
			sheet.addCell(new Label(col, row, c.get("VM_CODE"), format2));
			col++;
			sheet.addCell(new Label(col, row, c.get("TERMINAL_ID"), format2));
			col++;

			sheet.addCell(new Label(col, row, c.get("START_DATE"), format2));
			col++;
			sheet.addCell(new Label(col, row, c.get("END_DATE"), format2));
			col++;

			sheet.addCell(new Number(col, row, c.getLong("CNT_CASH"), format3));
			col++;
			sheet.addCell(new Number(col, row, c.getLong("AMOUNT_CASH"), format3));
			col++;
			sheet.addCell(new Number(col, row, c.getLong("CNT_CARD"), format3));
			col++;
			sheet.addCell(new Number(col, row, c.getLong("AMOUNT_CARD"), format3));
			col++;

			sheet.addCell(new Number(col, row, c.getLong("CNT_TOTAL"), format3));
			col++;
			sheet.addCell(new Number(col, row, c.getLong("AMOUNT_TOTAL"), format3));
			col++;
		
			row++;
		
		}

		col = 0;
		sheet.addCell(new Label(col, row, "합계", format1));
		sheet.mergeCells(col, row, col + 6, row);
		col += 7;

		Formula formula = new Formula(col++, row, "SUM(H7:H" + row + ")", format4);
		sheet.addCell(formula);
		sheet.addCell(formula.copyTo(col++, row));
		
		for (int j = 0; j < 2; j++) {
			sheet.addCell(formula.copyTo(col++, row));
			sheet.addCell(formula.copyTo(col++, row));
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
	String excelName = StringEx.encode("마감집계");

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
<%!
/*
	private int rowspan(String organ, ArrayList data) {
		int rows = 0;

		for (int i = 0; i < data.size(); i++) {
			GeneralConfig c = (GeneralConfig) data.get(i);

			if (StringEx.inArray(organ, c.get("ORGAN_PATH").split(";"))) {
				rows++;
			}
		}

		return rows;
	}
*/
%>