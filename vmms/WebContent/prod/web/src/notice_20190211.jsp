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
			<br/>
			<img src="<%=cfg.get("imgDir")%>/web/notice/20190211.png" width="609" height="520" alt="보상판매" usemap="#DocMap" border="0">
		  </td>
		</tr>
		<tr>
		<td align="center">
		<br/>
		  <a href="#none" onclick="winclose();">
		  <img src="<%=cfg.get("imgDir")%>/web/btn_ok.gif" />
		  </a>
		  <br/>
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
