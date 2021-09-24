<%@ page import="com.nucco.*,
	com.nucco.cfg.*,
	com.nucco.lib.*,
	com.nucco.lib.http.Param,com.nucco.lib.db.DBLibrary,
	java.net.*,	org.apache.log4j.Logger,
	org.apache.log4j.PropertyConfigurator,
	java.sql.*" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
/**
 *  *
 * 설날 공지 *
 * 작성일 - 2020/01/20, 김태우
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
	<img src="./ubcn_notice_1.png" style="width:540px; height:600px;"/>

		 <form id="close" class="popup_option" name="form1"
		 style=" height: 7%; width: 100%; text-align: right; font-size: 11px; color: #777; background-color: #efefef;line-height: 45px;margin-bottom: 0px;"> 
		        <input type="checkbox" name="popup" value="" style="width: 13px;height: 26px;vertical-align: middle;margin: 0;padding: 0;">
		        	<label for="notToday" >오늘 하루 이 창을 열지 않음</label>
		        <button title="닫기" onclick="closeWin();" style="width: 17px; height: 15px; vertical-align: middle; margin: 0; padding: 0; border: none; background-color: transparent; margin-right:10px;">
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
    setCookie( "popupSub3", "done" , 1);
    window.close();
    }
</script>