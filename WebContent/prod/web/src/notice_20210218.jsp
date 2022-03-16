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
 * 공지사항(SUB)
 *
 * 작성일 - 2021/02/18, 김태우
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
<link rel="shortcut icon" href="/image/web/favicon_ubcn.png">
<title>공지사항</title>
</head>
<body style="margin:0px;">
<div id="window" id="pop"style=" height: 98%;">
	<table cellpadding="0" cellspacing="0" border="0" style="width:100%;  height:89%;/* margin-bottom:20px; */">
	    <tr>
		  <td align="center">
		    <br />
		    <strong><font size="4" color="blue">“페이코 매입 및 입금 지연 안내”</font></strong>
		  </td>
		</tr>
		<tr>
		  <td>
			<strong>
			<font size="3" color="blue">
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; - 발생 시일 : 2021.02.18  01:00 ~ 08:00 <br /><br />
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 페이코 매입 오류로 인해, 02월 16일 거래내용이 <br/>
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 기존 예정일 '17일'에서 '19일'에 재매입 될 예정입니다.<br />
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 이에 따라, 입금 예정일 '23일'에서 '25일'에 입금 될 예정입니다.<br /><br/>
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 감사합니다.<br /><br/>
			</font>
			</strong>
			<div align="center">
				<img src="/image/web/ubcn_logo.jpg" alt="VMMS" style="width:80px; height:34px;">
			</div>
		  </td>
		</tr>
		<%--<tr>
		<td align="center">
		  <a href="#none" onclick="winclose();">
		  <img src="<%=cfg.get("imgDir")%>/web/btn_ok.gif" />
		  </a>
		</td>
		</tr>--%>
	</table>	
		 <form id="close" class="popup_option" name="form1"
		 style=" height: 13%; width: 100%; text-align: right; font-size: 11px; color: #777; background-color: #efefef;line-height: 45px;margin-bottom: 0px;"> 
		        <input type="checkbox" name="popup" value="" style="width: 13px;height: 26px;vertical-align: middle;margin: 0;padding: 0;">
		        	<label for="notToday" >오늘 하루 이 창을 열지 않음</label>
		        <button title="닫기" onclick="closeWin();" style="width: 17px; height: 15px; vertical-align: middle; margin: 0; padding: 0; border: none; background-color: transparent; margin-right:10px;">
			<img src="/common/module/btn_close2.gif" alt="X" style="vertical-align: middle;">
		        </button>		     	
		 
		</form>	
	<%--<div align="center">		
<a href="https://vmms.ubcn.co.kr/web/src/Product_registration.pdf" download><button type="button" class="btn btn-outline-info">상품등록 매뉴얼</button></a>		
	</div>--%>
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
    setCookie( "popupSub1", "done" , 1);
    window.close();
    }
</script>
