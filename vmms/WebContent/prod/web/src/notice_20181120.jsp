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
<link rel="stylesheet" href="/common/module/style.css" />
<title>공지사항</title>

</head>
<body style="margin:0px;">
<div id="window" id="pop"style="
    height: 100%;
">
	<table cellpadding="0" cellspacing="0" border="0" style="width:100%;  height:89%;/* margin-bottom:20px; */">
	    <tr>
		  <td align="center">
		    <br />
		    <strong><font size="4" color="blue">“고장접수, 결제취소 고객센터 운영 안내”</font></strong>
		  </td>
		</tr>
		<tr>
		  <td>
		    <br /><br />
			<strong>
			<font size="3" color="blue">
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; - 고객센터 운영시간 : (평일) 09:30 ~ 17:00<br /><br />
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; - 고객센터 전화번호 : 02)2082-7880 내선(500번)<br /><br />
			</font>
			</strong>
		  </td>
		</tr>
<%-- 		<tr>
		<td align="center">
		  <a href="#none" onclick="winclose();">
		  <img src="<%=cfg.get("imgDir")%>/web/btn_ok.gif" />
		  </a>
		</td>
		</tr> --%>
	</table>
		 <form id="close" class="popup_option" name="form1"
		 style=" height: 11%; width: 100%; text-align: right; font-size: 11px; color: #777; background-color: #efefef;"> 
		        <input type="checkbox" name="popup" value="" style="width: 13px;height: 26px;vertical-align: middle;margin: 0;padding: 0;">
		        	<label for="notToday" >오늘 하루 이 창을 열지 않음</label>
		        <button title="닫기" onclick="javascript:closeWin();" style="width: 17px; height: 15px; vertical-align: middle; margin: 0; padding: 0; border: none; background-color: transparent;">
		        	<img src="/common/module/btn_close2.gif" alt="X" style="vertical-align: middle;">
		        </button>		     	
		 
		</form>
	
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
    setCookie( "popupMain", "done" , 1);
    window.close();
    }
</script>
