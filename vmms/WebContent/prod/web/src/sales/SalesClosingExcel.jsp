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
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0403");

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
	String payment = StringEx.getKeyword(StringEx.charset(request.getParameter("paymentAll")));
	String[] payTypes = "Y".equals(payment) ? null : request.getParameterValues("payment");
	String sDate = StringEx.getKeyword(StringEx.charset(request.getParameter("sDate")));
	String eDate = StringEx.getKeyword(StringEx.charset(request.getParameter("eDate")));
	int oMode = StringEx.str2int(request.getParameter("oMode"), -1);
	int oType = StringEx.str2int(request.getParameter("oType"), -1);
	long depth = StringEx.str2long(request.getParameter("depth"), 0);
	if (oMode == -1) oMode = 1;
	if (oType == -1) oType = 0;
	if (payTypes != null) {
		for (int i = 0; i < payTypes.length; i++) {
			payTypes[i] = StringEx.getKeyword(StringEx.charset(payTypes[i]));
		}
	}

// 인스턴스 생성
	Sales objSales = new Sales(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objSales.summaryEx("02", company, organ, depth, place, sDate, eDate, payTypes, oMode, oType);
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
	int prepayCompanyCount = objSales.company.size();

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
		sheet = workbook.createSheet("마감현황", 0);


	// 검색 조건
		sheet.addCell(new Label(0, row, "페이지명", format1));
		sheet.addCell(new Label(1, row, "마감현황", format5));
		row++;

		sheet.addCell(new Label(0, row, "검색조건", format1));
		sheet.addCell(new Label(1, row, objSales.data.get("sDesc"), format5));
		row += 2;

	// 타이틀 생성
		sheet.addCell(new Label(col, row, objSales.data.get("ORGAN"), format1));
		sheet.mergeCells(col, row, col + 2, row + 2); //2017.12.21 jwhwang 추가
		sheet.setColumnView(col++, 20);
		sheet.setColumnView(col++, 10); //2017.12.21 jwhwang 추가
		sheet.setColumnView(col++, 30);

		sheet.addCell(new Label(col, row, "설치위치", format1));
		sheet.mergeCells(col, row, col, row + 2);
		sheet.setColumnView(col++, 50);

		sheet.addCell(new Label(col, row, "자판기코드", format1));
		sheet.mergeCells(col, row, col, row + 2);
		sheet.setColumnView(col++, 10);

	// 마감
		sheet.addCell(new Label(col, row, "마감", format1));
		sheet.mergeCells(col, row, col + 9 + (prepayCompanyCount * 2), row); //2019.02.25 scheo 수정 7 -> 8
		sheet.addCell(new Label(col, row + 1, "판매기간", format1));
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

		sheet.addCell(new Label(col, row + 1, "신용", format1));
		sheet.mergeCells(col, row + 1, col + 1, row + 1);
		sheet.addCell(new Label(col, row + 2, "건수", format1));
		sheet.setColumnView(col++, 10);
		sheet.addCell(new Label(col, row + 2, "금액", format1));
		sheet.setColumnView(col++, 15);
		
		//2019.02.25 scheo 추가
		sheet.addCell(new Label(col, row + 1, "간편결제", format1));
		sheet.mergeCells(col, row + 1, col + 1, row + 1);
		sheet.addCell(new Label(col, row + 2, "건수", format1));
		sheet.setColumnView(col++, 10);
		sheet.addCell(new Label(col, row + 2, "금액", format1));
		sheet.setColumnView(col++, 15);

		for (int i = 0; i < prepayCompanyCount; i++) {
			GeneralConfig c = (GeneralConfig) objSales.company.get(i);

			sheet.addCell(new Label(col, row + 1, c.get("NAME"), format1));
			sheet.mergeCells(col, row + 1, col + 1, row + 1);
			sheet.addCell(new Label(col, row + 2, "건수", format1));
			sheet.setColumnView(col++, 10);
			sheet.addCell(new Label(col, row + 2, "금액", format1));
			sheet.setColumnView(col++, 15);
		}

		sheet.addCell(new Label(col, row + 1, "합계", format1));
		sheet.mergeCells(col, row + 1, col + 1, row + 1);
		sheet.addCell(new Label(col, row + 2, "건수", format1));
		sheet.setColumnView(col++, 10);
		sheet.addCell(new Label(col, row + 2, "금액", format1));
		sheet.setColumnView(col++, 15);

	// 매입청구
		sheet.addCell(new Label(col, row, "매입청구", format1));
		sheet.mergeCells(col, row, col + 3, row);

		sheet.addCell(new Label(col, row + 1, "미청구", format1));
		sheet.mergeCells(col, row + 1, col + 1, row + 1);
		sheet.addCell(new Label(col, row + 2, "건수", format1));
		sheet.setColumnView(col++, 10);
		sheet.addCell(new Label(col, row + 2, "금액", format1));
		sheet.setColumnView(col++, 15);

		sheet.addCell(new Label(col, row + 1, "거절", format1));
		sheet.mergeCells(col, row + 1, col + 1, row + 1);
		sheet.addCell(new Label(col, row + 2, "건수", format1));
		sheet.setColumnView(col++, 10);
		sheet.addCell(new Label(col, row + 2, "금액", format1));
		sheet.setColumnView(col++, 15);

	// 입금
		sheet.addCell(new Label(col, row, "입금", format1));
		sheet.mergeCells(col, row, col + 12 + (prepayCompanyCount * 3), row); //2019.02.25 scheo 수정 9 -> 10

		sheet.addCell(new Label(col, row + 1, "입금일", format1));
		sheet.mergeCells(col, row + 1, col, row + 2);
		sheet.setColumnView(col++, 15);

		sheet.addCell(new Label(col, row + 1, "현금", format1));
		sheet.mergeCells(col, row + 1, col + 2, row + 1);
		sheet.addCell(new Label(col, row + 2, "건수", format1));
		sheet.setColumnView(col++, 10);
		sheet.addCell(new Label(col, row + 2, "수수료", format1));
		sheet.setColumnView(col++, 12);
		sheet.addCell(new Label(col, row + 2, "금액", format1));
		sheet.setColumnView(col++, 15);

		sheet.addCell(new Label(col, row + 1, "신용", format1));
		sheet.mergeCells(col, row + 1, col + 2, row + 1);
		sheet.addCell(new Label(col, row + 2, "건수", format1));
		sheet.setColumnView(col++, 10);
		sheet.addCell(new Label(col, row + 2, "수수료", format1));
		sheet.setColumnView(col++, 12);
		sheet.addCell(new Label(col, row + 2, "금액", format1));
		sheet.setColumnView(col++, 15);

		//2019.02.25 scheo 추가
		sheet.addCell(new Label(col, row + 1, "간편결제", format1));
		sheet.mergeCells(col, row + 1, col + 2, row + 1);
		sheet.addCell(new Label(col, row + 2, "건수", format1));
		sheet.setColumnView(col++, 10);
		sheet.addCell(new Label(col, row + 2, "수수료", format1));
		sheet.setColumnView(col++, 12);
		sheet.addCell(new Label(col, row + 2, "금액", format1));
		sheet.setColumnView(col++, 15);
		
		for (int i = 0; i < objSales.company.size(); i++) {
			GeneralConfig c = (GeneralConfig) objSales.company.get(i);

			sheet.addCell(new Label(col, row + 1, c.get("NAME"), format1));
			sheet.mergeCells(col, row + 1, col + 2, row + 1);
			sheet.addCell(new Label(col, row + 2, "건수", format1));
			sheet.setColumnView(col++, 10);
			sheet.addCell(new Label(col, row + 2, "수수료", format1));
			sheet.setColumnView(col++, 12);
			sheet.addCell(new Label(col, row + 2, "금액", format1));
			sheet.setColumnView(col++, 15);
		}

		sheet.addCell(new Label(col, row + 1, "합계", format1));
		sheet.mergeCells(col, row + 1, col + 2, row + 1);
		sheet.addCell(new Label(col, row + 2, "건수", format1));
		sheet.setColumnView(col++, 10);
		sheet.addCell(new Label(col, row + 2, "수수료", format1));
		sheet.setColumnView(col++, 12);
		sheet.addCell(new Label(col, row + 2, "금액", format1));
		sheet.setColumnView(col, 15);

		sheet.mergeCells(1, 0, col, 0);
		sheet.mergeCells(1, 1, col, 1);

		row += 3;

		for (int i = 0; i < objSales.list.size(); i++) {
			GeneralConfig c = (GeneralConfig) objSales.list.get(i);
			int payCount = c.getInt("PAY_COUNT");

			col = 0;
			if (StringEx.isEmpty(c.get("PARENT_ORGAN"))) {
				sheet.addCell(new Label(col, row, c.get("ORGAN_CODE"), format2)); //2017.12.21 jwhwang 추가
				sheet.mergeCells(col, row, col + 1, row + payCount - 1);
				col += 2;
				sheet.addCell(new Label(col, row, c.get("ORGAN"), format5));
				sheet.mergeCells(col, row, col, row + payCount - 1);
				col++;
			} else {
				sheet.addCell(new Label(col, row, c.get("PARENT_ORGAN"), format5));
				if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
				col++;
				sheet.addCell(new Label(col, row, c.get("ORGAN_CODE"), format2)); //2017.12.21 jwhwang 추가
				if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
				col++;
				sheet.addCell(new Label(col, row, c.get("ORGAN"), format5));
				if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
				col++;
			}
			sheet.addCell(new Label(col, row, c.get("PLACE"), format5));
			if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
			col++;
			sheet.addCell(new Label(col, row, c.get("VM_CODE"), format2));
			if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
			col++;

			sheet.addCell(new Label(col, row, c.get("START_DATE"), format2));
			if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
			col++;
			sheet.addCell(new Label(col, row, c.get("END_DATE"), format2));
			if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
			col++;

			sheet.addCell(new Number(col, row, c.getLong("CNT_CASH"), format3));
			if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
			col++;
			sheet.addCell(new Number(col, row, c.getLong("AMOUNT_CASH"), format3));
			if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
			col++;
			sheet.addCell(new Number(col, row, c.getLong("CNT_CARD"), format3));
			if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
			col++;
			sheet.addCell(new Number(col, row, c.getLong("AMOUNT_CARD"), format3));
			if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
			col++;
			
			//2019.02.25 scheo 추가
			sheet.addCell(new Number(col, row, c.getLong("CNT_PAYCO"), format3));
			if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
			col++;
			sheet.addCell(new Number(col, row, c.getLong("AMOUNT_PAYCO"), format3));
			if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
			col++;

			for (int j = 0; j < prepayCompanyCount; j++) {
				GeneralConfig s = (GeneralConfig) objSales.company.get(j);

				sheet.addCell(new Number(col, row, c.getLong("CNT_PREPAY_" + s.get("CODE")), format3));
				if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
				col++;
				sheet.addCell(new Number(col, row, c.getLong("AMOUNT_PREPAY_" + s.get("CODE")), format3));
				if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
				col++;
			}

			sheet.addCell(new Number(col, row, c.getLong("CNT_TOTAL"), format3));
			if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
			col++;
			sheet.addCell(new Number(col, row, c.getLong("AMOUNT_TOTAL"), format3));
			if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
			col++;

			sheet.addCell(new Number(col, row, c.getLong("CNT_HELD"), format3));
			if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
			col++;
			sheet.addCell(new Number(col, row, c.getLong("AMOUNT_HELD"), format3));
			if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
			col++;
			sheet.addCell(new Number(col, row, c.getLong("CNT_DECLINED"), format3));
			if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
			col++;
			sheet.addCell(new Number(col, row, c.getLong("AMOUNT_DELINED"), format3));
			if (payCount > 1) sheet.mergeCells(col, row, col, row + payCount - 1);
			col++;

			int _col = col;
			for (int n = 1; n <= payCount; n++, row++) {
				col = _col;
				sheet.addCell(new Label(col++, row, c.get("PAY_DATE_" + n), format2));
	
				sheet.addCell(new Number(col++, row, c.getLong("PAY_CNT_CASH_" + n), format3));
				sheet.addCell(new Number(col++, row, c.getLong("COMMISSION_CASH_" + n), format3));
				sheet.addCell(new Number(col++, row, c.getLong("PAY_AMOUNT_CASH_" + n), format3));
				sheet.addCell(new Number(col++, row, c.getLong("PAY_CNT_CARD_" + n), format3));
				sheet.addCell(new Number(col++, row, c.getLong("COMMISSION_CARD_" + n), format3));
				sheet.addCell(new Number(col++, row, c.getLong("PAY_AMOUNT_CARD_" + n), format3));
				//2019.02.25 scheo 추가
				sheet.addCell(new Number(col++, row, c.getLong("PAY_CNT_PAYCO_" + n), format3));
				sheet.addCell(new Number(col++, row, c.getLong("COMMISSION_PAYCO_" + n), format3));
				sheet.addCell(new Number(col++, row, c.getLong("PAY_AMOUNT_PAYCO_" + n), format3));
	
				for (int j = 0; j < prepayCompanyCount; j++) {
					GeneralConfig s = (GeneralConfig) objSales.company.get(j);
	
					sheet.addCell(new Number(col++, row, c.getLong("PAY_CNT_PREPAY_" + s.get("CODE") + "_" + n), format3));
					sheet.addCell(new Number(col++, row, c.getLong("COMMISSION_PREPAY_" + s.get("CODE") + "_" + n), format3));
					sheet.addCell(new Number(col++, row, c.getLong("PAY_AMOUNT_PREPAY_" + s.get("CODE") + "_" + n), format3));
				}
	
				sheet.addCell(new Number(col++, row, c.getLong("PAY_CNT_TOTAL_" + n), format3));
				sheet.addCell(new Number(col++, row, c.getLong("COMMISSION_TOTAL_" + n), format3));
				sheet.addCell(new Number(col, row, c.getLong("PAY_AMOUNT_TOTAL_" + n), format3));
			}
		}

		col = 0;
		sheet.addCell(new Label(col, row, "합계", format1));
		sheet.mergeCells(col, row, col + 6, row);
		col += 7;

		Formula formula = new Formula(col++, row, "SUM(H7:H" + row + ")", format4);
		sheet.addCell(formula);
		sheet.addCell(formula.copyTo(col++, row));
		
		for (int j = 0; j < prepayCompanyCount + 5; j++) { //2019.02.25 scheo 수정 4->5
			sheet.addCell(formula.copyTo(col++, row));
			sheet.addCell(formula.copyTo(col++, row));
		}

		sheet.addCell(new Label(col++, row, "", format6));

		for (int j = 0; j < prepayCompanyCount + 4; j++) { //2019.02.25 scheo 수정 3->4
			sheet.addCell(formula.copyTo(col++, row));
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
	String excelName = StringEx.encode("마감현황");

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