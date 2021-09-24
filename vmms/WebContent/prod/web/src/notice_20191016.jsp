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
 * 고객센터 공지사항 (명의이전 신청방법 변경안내)
 *
 * 작성일 - 2019/10/16, 김태우
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
		    <strong><font size="4" color="blue">-명의이전 신청방법 변경안내-</font></strong>
		  </td>
		</tr>
		<tr>
		  <td>
		    <br /><br />
			<strong>
			<font size="3" color="blue">
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;명의이전 신청 방법이 세분화되어 10월 21일부로 변경될 예정입니다.<br /><br /><br/>
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;진행 일시 : 2019년 10월 21일부터<br /><br />
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;*자세한 내용은 공지사항 참고 부탁드립니다. <br /><br />
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://vmms.ubcn.co.kr/web/src/cs/CsNoticeDetail.jsp?seq=18" target="_sub">▶명의이전 신청방법 공지사항으로 이동</a> <br /><br />
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;감사합니다.<br /><br />
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
	 <div id="close"> 
		    <form name="form1">
		        <input type="checkbox" name="popup" value="">
		     	<a href="" onclick="closeWin();">오늘 하루동안 보지 않기</a> 
		    </form>
		</div>
	
	
</div>
<body>
<%--@ include file="../footer.inc.jsp" --%>


<script language="javascript">
//    self.focus();

    function winclose(){
        window.close();
    }
    function setCookie( name, value, expiredays )
    {
    var todayDate = new Date();
    todayDate.setDate( todayDate.getDate() + expiredays );
    document.cookie = name + "=" + escape( value ) + "; path=/; expires=" + todayDate.toGMTString() + ";"
    }

    function closeWin()
    {
    if ( document.form1.popup.checked )
    setCookie( "popupSub", "done" , 1);
    window.close();
    }
</script>
