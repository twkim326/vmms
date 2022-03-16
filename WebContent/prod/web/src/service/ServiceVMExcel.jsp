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
 * /sales/ServiceVMExcel.jsp
 *
 * 서비스관리 > 운영자판기관리 > 엑셀
 *
 * 작성일 - 2014/03/10, 이정현
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
	long company = StringEx.str2long(request.getParameter("company"), 0);
	long organ = StringEx.str2long(request.getParameter("organ"), 0);
	String aspCharge = StringEx.getKeyword(StringEx.charset(request.getParameter("aspCharge")));
	String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
	String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));
	
// 수집오류 단말기 체크
	String collectChk = request.getParameter("collectChk");

// 수집오류 flag 초기화
	if ((collectChk == null) || (collectChk.equals(""))){
		collectChk = "N";
	}


// 인스턴스 생성
	VM objVM = new VM(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objVM.getList(company, organ, aspCharge, sField, sQuery, collectChk);
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
	int row = 3;

	try {
		/**/
		int nYear;
		int nMonth;
		
		workbook = Workbook.createWorkbook(excel);
		sheet = workbook.createSheet("운영 자판기 관리", 0);

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

		sheet.setColumnView(0, 30);
		sheet.setColumnView(1, 45);
		sheet.setColumnView(2, 20);
		sheet.setColumnView(3, 20);
		sheet.setColumnView(4, 20);
		sheet.setColumnView(5, 20);
		sheet.setColumnView(6, 50);
		sheet.setColumnView(7, 20);
		sheet.setColumnView(8, 15);
		sheet.setColumnView(9, 20);
		sheet.setColumnView(10, 20);
		sheet.setColumnView(11, 20);
		
		sheet.setColumnView(12, 30);
		
		if(cfg.getLong("user.company") == 0){
			sheet.setColumnView(13, 20);
			sheet.setColumnView(14, 20);
		}
		sheet.addCell(new Label(0, 0, "페이지명", format1));
		sheet.addCell(new Label(1, 0, "운영 자판기 관리", format1));
		sheet.addCell(new Label(0, 1, "검색조건", format1));
		sheet.addCell(new Label(1, 1, objVM.data.get("sDesc"), format1));

		sheet.addCell(new Label(0, row + 0, "소속", format1));
		sheet.addCell(new Label(1, row + 0, "자판기 조직", format1));
		sheet.addCell(new Label(2, row + 0, "코드", format1));
		sheet.addCell(new Label(3, row + 0, "단말기 ID", format1));
		sheet.addCell(new Label(4, row + 0, "모델", format1));
		sheet.addCell(new Label(5, row + 0, "모뎀", format1));
		sheet.addCell(new Label(6, row + 0, "설치위치", format1));
		sheet.addCell(new Label(7, row + 0, "담당자", format1));
		sheet.addCell(new Label(8, row + 0, "담당자ID", format1));
		sheet.addCell(new Label(9, row + 0, "등록일", format1));
		/*20180622 허승찬 자판기관리 전자세금계산서 내역 시작*/
		sheet.addCell(new Label(10, row + 0, "위치코드", format1));
		sheet.addCell(new Label(11, row + 0, "위치순번", format1));
		
		sheet.addCell(new Label(12, row + 0, "상태정보 수신시간", format1));
		
		if(cfg.getLong("user.company") == 0){
			sheet.addCell(new Label(13, row + 0, "개통통신료", format1));
			sheet.addCell(new Label(14, row + 0, "ASP", format1));
		}


		Calendar calendar = new GregorianCalendar(Locale.KOREA);
		nYear = calendar.get(Calendar.YEAR);
		nMonth = calendar.get(Calendar.MONTH) + 1;
		int dayyy = (nYear * 100 + nMonth);
		String dayy = Integer.toString(dayyy);
		/*20180622 허승찬 자판기관리 전자세금계산서 내역 종료*/
		
		for (int i = 0; i < objVM.list.size(); i++) {
			GeneralConfig c = (GeneralConfig) objVM.list.get(i);

			sheet.addCell(new Label(0, i + row + 1, c.get("COMPANY"), format1));
			sheet.addCell(new Label(1, i + row + 1, c.get("ORGAN"), format1));
			sheet.addCell(new Label(2, i + row + 1, c.get("CODE"), format1));
			sheet.addCell(new Label(3, i + row + 1, c.get("TERMINAL_ID"), format1));
			sheet.addCell(new Label(4, i + row + 1, c.get("MODEL"), format1));
			sheet.addCell(new Label(5, i + row + 1, c.get("MODEM"), format1));
			sheet.addCell(new Label(6, i + row + 1, c.get("PLACE"), format1));
			sheet.addCell(new Label(7, i + row + 1, c.get("USER_NAME"), format1));
			sheet.addCell(new Label(8, i + row + 1, c.get("USER_ID"), format1));
			sheet.addCell(new Label(9, i + row + 1, c.get("CREATE_DATE"), format1));
			/*허승찬 위치코드 위치순번 출력*/
			sheet.addCell(new Label(10, i + row + 1, c.get("PLACE_CODE"), format1));
			sheet.addCell(new Label(11, i + row + 1, c.get("PLACE_NO"), format1));
			
			sheet.addCell(new Label(12, i + row + 1, c.get("CRT_DATE"), format1));
			
			/*20180622 허승찬 자판기관리 전자세금계산서 내역 시작*/
			if(cfg.getLong("user.company") == 0){
				if(c.get("COMPANY").equals("(주)코카콜라") || c.get("COMPANY").equals("(주)코카콜라 부산지점") || c.get("COMPANY").equals("(주)코카콜라 전라지점") || c.get("COMPANY").equals("(주)코카콜라 충청지점")){				
					/*if(c.get("YYYYMM").equals(dayy) && c.get("IS_EXPIRE").equals("N")){
						sheet.addCell(new Label(10, i + row + 1, c.get("OPEN_COM_COCA"), format1));
					}else if(!c.get("YYYYMM").equals(dayy) && c.get("IS_EXPIRE").equals("N")){
						sheet.addCell(new Label(10, i + row + 1, "0", format1));                         개통통신료 당월로만 계산*/
					if(c.get("IS_EXPIRE").equals("N")){
						sheet.addCell(new Label(13, i + row + 1, c.get("OPEN_COM_COCA"), format1));
					}else{
						sheet.addCell(new Label(13, i + row + 1, "0", format1));
					}
				}else{
					/*if(c.get("YYYYMM").equals(dayy) && c.get("IS_EXPIRE").equals("N")){
						sheet.addCell(new Label(10, i + row + 1, c.get("OPEN_COM"), format1));
					}else if(!c.get("YYYYMM").equals(dayy) && c.get("IS_EXPIRE").equals("N")){
						sheet.addCell(new Label(10, i + row + 1, "0", format1));                         개통통신료 당월로만 계산*/
					if(c.get("IS_EXPIRE").equals("N")){
						sheet.addCell(new Label(13, i + row + 1, c.get("OPEN_COM"), format1));
					}else{
						sheet.addCell(new Label(13, i + row + 1, "0", format1));
					}
				}
				if(c.get("COMPANY").equals("(주)SECO") || c.get("COMPANY").equals("무인멘")){
					if(c.get("YYYYMM").equals(dayy) && c.get("IS_EXPIRE").equals("N")){
						sheet.addCell(new Label(14, i + row + 1, "0", format1));
					}else if(!c.get("YYYYMM").equals(dayy) && c.get("IS_EXPIRE").equals("N")){
						sheet.addCell(new Label(14, i + row + 1, "5000", format1));
					}else{
						sheet.addCell(new Label(14, i + row + 1, "0", format1));
					}
				}else{
					if(c.get("YYYYMM").equals(dayy) && c.get("IS_EXPIRE").equals("N")){
						sheet.addCell(new Label(14, i + row + 1, "0", format1));
					}else if(!c.get("YYYYMM").equals(dayy) && c.get("IS_EXPIRE").equals("N")){
						sheet.addCell(new Label(14, i + row + 1, "2000", format1));
					}else{
						sheet.addCell(new Label(14, i + row + 1, "0", format1));
					}
				}
			}
			/*20180622 허승찬 자판기관리 전자세금계산서 내역 종료*/
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
	String excelName = StringEx.encode("운영자판기관리");

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