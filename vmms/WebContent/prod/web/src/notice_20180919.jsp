<%@ page import="com.nucco.*,
	com.nucco.cfg.*,
	com.nucco.lib.*,
	com.nucco.lib.http.Param,com.nucco.lib.db.DBLibrary,
	java.net.*,	org.apache.log4j.Logger,
	org.apache.log4j.PropertyConfigurator,
	java.sql.*" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
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



%>
<%--@ include file="../header.inc.jsp" --%>
<head>
<title>공지사항</title>
</head>
<body>
<div id="window">
	<table cellpadding="0" cellspacing="0" border="0" style="float:left; width:100%;  margin-bottom:20px;">
	    <tr>
		  <td align="center">
		    <br />
		    <strong><font size="4" color="blue">-거래내역 조회 지연에 대한 공지사항-</font></strong>
		  </td>
		</tr>
		<tr>
		  <td>
		    <br /><br />
			<strong>
			<font size="3" color="blue">
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;최근 VMMS 거래내역 이관 지연이 발생하여,<br /><br />
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;실시간 거래가 지연되어 조회되고 있습니다.<br /><br /><br/>
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;현재 다방면으로 개선방안을 검토하여 조치하고 있습니다.<br /><br /><br/>
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1. 2015~2017년 과거거래 데이터 백업 보관 및 삭제 - 4월05일<br /><br />
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2. 거래 이관 방식 개선을 위한 서버 재 개발 - 5월중순<br /><br />
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;3. HW 사양 업그레이드 및 고속 스토리지 교체 - 6월말<br /><br />
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;4. 온라인DB, 배치DB, VMMSDB 분리 및 독립 운영 - 8월말<br /><br /><br/>
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;빠른 시일 내에 개선이 될 수 있도록 최선을 다하겠습니다.<br /><br /><br />
			</font>
			</strong>
		  </td>
		</tr>
		<tr>
		<td align="center">
		  <a href="#none" onclick="winclose();">
		  <img src="<%=cfg.get("imgDir")%>/web/btn_ok.gif" />
		  </a>
		</td>
		</tr>
	</table>
	
</div>
<body>
<%--@ include file="../footer.inc.jsp" --%>


<script language="javascript">
//    self.focus();

    function winclose(){
        window.close();
    }
</script>
