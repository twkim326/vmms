<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.VM
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/VMRunDetail.jsp
 *
 * 자판기 운영정보 > 가동상태 > 조회
 *
 * 작성일 - 2011/04/03, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0301");

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

// 페이지 유형 설정
	cfg.put("window.mode", "A");

// 전송된 데이터
	long user_id = cfg.getLong("user.company");
	long seq = StringEx.str2long(request.getParameter("seq"), 0);

	if (seq == 0) {
		out.print("자판기 정보가 존재하지 않습니다.");
		return;
	}

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "page:company:organ:sField:sQuery");

// 인스턴스 생성
	VM objVM = new VM(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objVM.detail(seq);
	} catch (Exception e) {
		error = e.getMessage();
	}

// 에러 처리
	if (error != null) {
		out.print(error);
		return;
	}
%>
<%@ include file="../../header.inc.jsp" %>

<div class="title">
	<span>자판기 가동상태 조회</span>
</div>
<table cellspacing="0" class="tableType03">
	<colgroup>
		<!-- 20130308 자판기상태정보 변경 시작 -->
		<!-- 
		<col width="110" />
		<col width="*"/>
		<col width="110" />
		<col width="*"/>					
		 -->
        <col width="120" />
		<col width="180"/>
		<col width="120" />
		<col width="180"/>
	<!-- 20130308 자판기상태정보 변경 종료 -->
	</colgroup>
	<tr>
		<th><span>소속</span></th>
		<td><%=Html.getText(objVM.data2.get(0).get("COMPANY"))%></td>
		<th><span>조직</span></th>
		<td><%=Html.getText(StringEx.setDefaultValue(objVM.data2.get(0).get("ORGAN"), "-"))%></td>
	</tr>
	<tr>
		<th><span>자판기 코드</span></th>
		<td><%=Html.getText(objVM.data2.get(0).get("CODE"))%></td>
		<%//System.out.println("자판기 코드 : " + objVM.data2.get(0).get("CODE")); %>
		<th><span>단말기 ID</span></th>
		<td><%=Html.getText(objVM.data2.get(0).get("TERMINAL_ID"))%></td>
	</tr>
	<tr>
		<th><span>모델</span></th>
		<td><%=StringEx.setDefaultValue(Html.getText(objVM.data2.get(0).get("MODEL")), "-")%></td>
		<th><span>모뎀 번호</span></th>
		<td><%=StringEx.setDefaultValue(Html.getText(objVM.data2.get(0).get("MODEM")), "-")%></td>
	</tr>
	<tr>
		<th><span>설치위치</span></th>
		<td colspan="3"><%=Html.getText(objVM.data2.get(0).get("PLACE"))%></td>
	</tr>
	<!-- 20180712 scheo 단말기 통신정보 업데이트 시작  -->
	<tr>
		<th><span>모뎀 정보 및 자판기 상태 정보</span></th>
		<td colspan="3">
<%
	//if (objVM.data.get("IS_PD_ERROR").equals("Y")) {
	
		//if (objVM.data.get("IS_PD_ERROR").equals("Y")) {
			out.println("<blockquote style='font-size:12px; font-weight:normal; color:#5f5f5f;'>" + Html.getText(objVM.data2.get(0).get("PD_ERROR")) + "</blockquote>");
		//}
	
	//} else {
		//out.println("-");
	//}
%>
		</td>
	</tr>
	<!-- 20180712 scheo 단말기 통신정보 업데이트 종료  -->
	<tr>
		<th><span>에러 내용</span></th>
		<td colspan="3">
<%
	if (objVM.data2.get(0).get("IS_UPDATE").equals("N") || objVM.data2.get(0).get("IS_EXPIRE").equals("Y") || objVM.data2.get(0).get("IS_CONTROL_ERROR").equals("Y") || objVM.data2.get(0).get("IS_PD_ERROR").equals("Y") || objVM.data2.get(0).get("IS_EMPTY_COL").equals("Y")) {
		if (objVM.data2.get(0).get("IS_UPDATE").equals("N")) {
			out.println("<strong style='display:block; color:#ff0000;'>자판기 등록 후 상태 정보를 수신하지 못함</strong>");
		} else if (objVM.data2.get(0).get("IS_EXPIRE").equals("Y")) {
			//20131213, 20200421 cdh 최종 거래일 추가
			//out.println("<strong style='display:block; color:#ff0000;'>상태 정보 수신 3시간 초과 (최종 수신일 : " + objVM.data.get("TRAN_DATE") + ")</strong>");
			//out.print("<strong style='display:block; color:#ff0000;'>상태 정보 수신 24시간 초과 (최종 수신일 : " + objVM.data2.get(0).get("TRAN_DATE") + ")</strong>");
			out.print("<strong style='display:block; color:#ff0000;'>상태 정보 수신 24시간 초과 (최종 수신일 : " + objVM.data2.get(0).get("TRAN_DATE") + ", 최종 거래일 : " + objVM.data2.get(0).get("FINAL_TRAN_DATE") + ")</strong>");
		} else {
			if (objVM.data2.get(0).get("IS_EMPTY_COL").equals("Y")) {
				//out.println("<strong style='display:block; color:#ff0000;'>미등록 컬럼 :<blockquote style='font-size:11px; font-weight:normal; color:#ff0000; padding-left:25px;'>" + Html.getText(objVM.data.get("EMPTY_COL")) + "</blockquote></strong>");
				out.println("<strong style='display:block; color:#ff0000;'>미등록 컬럼 :<blockquote style='font-size:11px; font-weight:normal; color:#ff0000; padding-left:25px;'>");
				
				for(int i = 0; i < objVM.data2.size(); i++) {
					GeneralConfig c = (GeneralConfig) objVM.data2.get(0);
					//c.get("CAB_NO")
					out.println(Html.getText(c.get("EMPTY_COL")) +  "<br />");
					//out.println("캐비넷" + i + "번 : " + Html.getText(c.get("EMPTY_COL")) +  "<br />"); // scheo
				}
				out.println("</blockquote></strong>");
			}

			if (objVM.data2.get(0).get("IS_CONTROL_ERROR").equals("Y") && (user_id == 0||user_id==337||user_id==1142||user_id==1216||user_id==1415||user_id==3||user_id==14)) {
				out.println("<strong style='display:block; color:#ff0000;'>주 제어부 :<blockquote style='font-size:11px; font-weight:normal; color:#ff0000; padding-left:25px;'>" + Html.getText(objVM.data2.get(0).get("CONTROL_ERROR")) + "</blockquote></strong>");
			}//20191017 scheo 주 제어부 삭제
			// 20180712 scheo 단말기 통신정보 업데이트 시작 
			//if (objVM.data.get("IS_PD_ERROR").equals("Y")) {
			//	out.println("<strong style='display:bFlock; color:#ff0000;'>P/D :<blockquote style='font-size:11px; font-weight:normal; color:#ff0000; padding-left:25px;'>" + Html.getText(objVM.data.get("PD_ERROR")) + "</blockquote></strong>");
			//}
			// 20180712 scheo 단말기 통신정보 업데이트 종료
			//20130308 자판기상태정보 변경 시작
			// 20201105 chae 휘닉스벤딩, 유비쿼터스커뮤니케이션 조건부 추가 유광권부장님 요청
			
			if (objVM.data2.get(0).get("IS_SOLD_OUT").equals("Y")) {
				out.println("<strong style='display:block; color:#ff0000;'>품절 :<blockquote style='font-size:11px; font-weight:normal; color:#ff0000; padding-left:25px;'>" + Html.getText(objVM.data2.get(0).get("SOLD_OUT")) + "</blockquote></strong>");
			}
			//20130308 자판기상태정보 변경 끝
		}
	} else {
		out.println("-");
	}
%>
		</td>
	</tr>
	<%-- scheo 20211110 memo 추가 --%>
	<tr <% if( user_id == 0) { %> <% } else { %>style = "display:none" <% } %>>
		<th><span>메모</span></th>
		<td colspan="3">
			<%=Html.getText(objVM.data2.get(0).get("MEMO"))%>
		</td>
	</tr>
	<tr>
		<th class="last"><span>판매 이상</span></th>
		<td class="last" colspan="3">
<%
	if (objVM.error.size() > 0) {
		out.println("<strong style='display:block; color:#ff0000;'>최근 7일간 미등록 컬럼 판매 실적 존재 :<blockquote style='font-size:11px; font-weight:normal; color:#ff0000; padding-left:25px;'>");

		for (int i = 0; i < objVM.error.size(); i++) {
			GeneralConfig c = (GeneralConfig) objVM.error.get(i);
			//20130308 자판기상태정보 변경 시작
			//out.println("거래일시 = " + c.get("TRANSACTION_DATE") + " AND 컬럼번호 = " + c.get("COL_NO") + "<br />");
			out.println(" 컬럼번호 = " + c.get("COL_NO")+ " AND 미등록발생 최초일시 = " + c.get("TRANSACTION_DATE") + " AND 건수 = " + c.get("CNT") +  "<br />");
			//20130308 자판기상태정보 변경 끝
		}

		out.println("</blockquote></strong>");
	} else {
		out.println("-");
	}
%>
		</td>
	</tr>
</table>

<div class="title mt18">
	<span>상품 상세 정보</span>
<% if (objVM.error.size() > 0) { %>
	<input type="button" class="button2" value="상품수정" onclick="location.href = '../service/ServiceVMRegist.jsp?seq=<%=seq%>';" style="position:absolute; right:0; bottom:2px;" />
<% } %>
</div>

<table cellspacing="0" class="tableType03">
	<tr>
<% for (int i = 0; i < objVM.product.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.product.get(i); %>
	<% if (i > 0 && i % 5 == 0) { %>
	</tr><tr>
	<% } %>
		<td width="20%" style="vertical-align:top; padding:0;" class="<%=(i >= ((objVM.product.size() - 1) / 5) * 5 ? "last" : "")%>">
			<table width="100%" cellpadding="0" cellspacing="0">
				<tr>
					<th style="padding:3px 3px 3px 8px; border-width:0 0 1px 0; border-style:solid; border-color:#ccc;">
						<div style="position:relative;">
							<span><%=c.getInt("COL_NO")%>번</span>
						<% if (c.get("IS_SOLD_OUT").equals("Y")) { %>
							<img src="<%=cfg.get("imgDir")%>/web/icon_sold_out.gif" alt="품절" style="position:absolute; right:1px; top:1px;" />
						<% } %>
						</div>
					</th>
				</tr>
				<tr>
					<td style="padding:3px 3px 3px 16px; border-style:none;"><%=c.get("NAME")%></td>
				</tr>
			</table>
		</td>
<% } %>
<% if ((objVM.product.size() - 1) % 5 < 4) { %>
		<td colspan="<%=(4 - ((objVM.product.size() - 1) % 5))%>" class="last">&nbsp;</td>
<% } %>
	<tr>
</table>

<HIDDEN>
<div class="buttonArea">
	<input type="button" value="" class="btnList" onclick="location.href = 'VMRun.jsp?1=1<%=addParam%>';" />
	<input type="button" value="" class="btnPrint" onclick="_print();" />
</div>
</HIDDEN>

<%@ include file="../../footer.inc.jsp" %>

<script type="text/javascript">
	function _print() {
		var win = Common.openWin("about:blank", "", 990, 500, 'yes');
		var doc = '<html>'
				+ '<head>'
				+ '<title>자판기 가동상태</title>'
				+ '<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />'
				+ '<link rel="stylesheet" type="text/css" href="<%=cfg.get("imgDir")%>/css/common.css">'
				+ '</head>'
				+ '<body onload="window.print();">'
				+ '<div id="contents" style="margin:5px; padding:0;">'
				+ $('contents').innerHTML.replace(/\<HIDDEN\>/gi, '<!--').replace(/\<\/HIDDEN\>/gi, '-->')
				+ '</div>'
				+ '</body>'
				+ '</html>';

		win.document.write(doc);

		if (Prototype.Browser.IE) {
			win.history.go(0);
		} else {
			win.print();
		}
	}
</script>