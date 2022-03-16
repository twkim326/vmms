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
 * 고객센터 공지 *
 * 작성일 - 2021/03/29, 김태우
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
<link rel="shortcut icon" href="/image/web/favicon_ubcn.png">
<link rel="stylesheet" href="https://use.fontawesome.com/releases/v5.7.2/css/all.css" integrity="sha384-fnmOCqbTlWIlj8LyTjo7mOUStjsKC4pOpQbqyi7RrhN7udi9RwhKkMHpvLbHG9Sr" crossorigin="anonymous">
<title>공지사항</title>
<style type="text/css">
* { margin:0; padding:0; border:0; outline:0;}
html{width:100%;}
body{font-size:12px; font-family:"맑은 고딕","Malgun Gothic","돋움","굴림",Dotum,Arial,Helvetica,sans-serif; width:100%; color:#595959; line-height:130%; background:#fafafa; word-spacing:normal; letter-spacing:normal;}
a,a:link,a:visited{color:#666666; text-decoration:none;}
a:hover{color:#242424;}
ul,ol{list-style:none}
img, video{ border:0; vertical-align:top; }
.blind,caption{overflow:hidden; position:absolute; top:0; left:0; width:0; height:0; font-size:0; line-height:0;}
article, aside, details, figcaption, figure, footer, header, hgroup, menu, nav, section { display:block; }



#wrap {position:relative; width:100%; margin:0 auto;}
#bottom_close {position:fixed; width:100%; height:40px; bottom:0; z-index:100;}
p {font-weight: bold;}
#content_title {font-size: 20px; font-weight: bold;}
#content_subTitle {text-align: left;} 
.pop_col1 {font-size:12px; color:#ec3d01; font-weight:bold;}
.pop_col2 {font-size:12px; color:#595959; letter-spacing:-1px; line-height:130%;}
.pop_taba1 th {background:#eeeeee; color:#4d4d4d; font-weight:bold; text-align:center; height:22px; line-height:13px; padding:5px; letter-spacing:-1px; line-height:120%;} /* 테이블 관련 */
.pop_taba1 td {background:#fafafa; color:#595959; text-align:center; height:22px; line-height:13px; padding:9px 15px 9px 15px; line-height:140%;} /* 테이블 관련 */

</style>
</head>
<body style="margin:0px;">
<div id="wrap">
    <!---------------------------------- 제목/첨부파일 테이블 시작 ------------------------------------>
    
	<table width="100%" border="0" cellspacing="0" cellpadding="0">
		<tr>
			<td height="60" align="left" valign="middle" background="./pop/bg_01.png">
			    <table width="100%" height="30" border="0" cellpadding="0" cellspacing="0">
                    <tr>
					    <!--타이틀-->
                        <td text-align="center"; valign="middle" style="vertical-align:middle; font-size:16px; 
                        color:#FFFFFF; font-weight:bold; letter-spacing:-1px; line-height:120%; padding:10px 20px 10px 20px;">
                        	고장접수, 결제취소 고객센터 운영 안내
                       	</td>
                    </tr>
                </table>
			</td>
		</tr>
		<tr>
			<td height="2" background="./pop/bg_01.png"></td>
		</tr>
	</table>
	
	<!---------------------------------- 제목/첨부파일 테이블 끝 -------------------------------------->
	<!---------------------------------- 내용 테이블 시작 ------------------------------------>
	<table width="100%" border="0" cellspacing="0" cellpadding="0">
		
		<tr>
		    <!-- 내용 -->
			<td height="200" align="left" valign="top" style="text-align:left; vertical-align:top; letter-spacing:-1px; line-height:140%; padding:25px 20px 25px 20px;">			
				<br>
				<div id="content">
		<div id="content_title" align="center">
			<p><i class="fas fa-exclamation-circle"></i> 고객센터 운영 안내</p>
			<img src="./pop/vmms_notice_img.png" style="width:40%;">
		</div>
		<div id="content_subTitle">					
			<p>안녕하십니까, 유비씨엔(주) 입니다.</p>
			<p>현재 홈페이지 개선으로 인한 브라우저 캐시 변경으로 최초 1회 </p>
			<p>새로고침이 요구되는 경우가 있습니다.</p>
			<p>키보드 <strong style="color:red;">'Ctrl+F5'</strong>를 통해 진행 부탁드립니다.</p><br>
			<p><i class="fas fa-exclamation"></i> <strong style="color:red;"> 업무 특성상 전화 연결이 어려울 수 있습니다.</strong></p>
			<p><i class="fas fa-exclamation"></i> <strong style="color:red;"> VMMS 관련 문의사항은 아래 공지사항의 매뉴얼을 확인하여 주시기 바랍니다.</strong></p><br>
			<a href="https://vmms.ubcn.co.kr/web/src/cs/CsNotice.jsp" style="font-weight:bold;">
				<i class="fas fa-hand-point-right"></i> 공지사항으로 이동
			</a>
		</div>
		<br><br>
		<div id="content_subTitle">
			<div id="errDate"><i class="fas fa-map-marker-alt" style="color: #377ac6;"></i> 
			<strong style="font-weight: bold;">
			운영시간 : </strong>(영업일) 09:30 ~ 17:00
			</div>
			
			<div id="cause"><i class="fas fa-map-marker-alt" style="color: #377ac6;"></i> 
			<strong style="font-weight: bold;">
			전화번호 : </strong>1544-5737
			</div>			 			
		</div>		
	</div>
	<div align="center" style="margin-top:5%;"> 
		<p>
			<img alt="logo" src="./pop/ubcn_logo.jpg" />
		</p>
	</div>

				<br><br>			
			</td>
		</tr>
		
	</table>
    <!---------------------------------- 내용 테이블 끝 -------------------------------------->
	<!---------------------------------- 하루 창 닫기 div 시작 -------------------------------------->
	<div id="bottom_close">
	<table width="100%" height="40" border="0" cellpadding="0" cellspacing="0" background="./pop/bg_02.gif">
		<tr>
			<td align="left" valign="middle">
			    <table width="100%" height="21" border="0" cellpadding="0" cellspacing="0">
                    <tr>
                        <td width="20">&nbsp;</td>
						<!-- 하루 창 닫기 선택 -->
                        <td width="18" align="left" valign="middle">
                        <input name="popup" id="popup" type="checkbox"></td>
                        <td align="left" valign="middle" style="text-align:left; vertical-align:middle; color:#cbcbcb; 
                        letter-spacing:-1px; line-height:120%; padding-left:3px;">오늘 하루 이 창을 열지 않음</td>
						<!-- 닫기 버튼 -->
                        <td width="70" align="right" valign="middle">
                        	<a href="javascript:closeWin();">
                        		<img src="./pop/bt_close1.png" alt="창닫기" width="50" height="21" border="0" /> 
                       		</a>
                     	</td>
                        <td width="20">&nbsp;</td>
                    </tr>
                </table>
			</td>
		</tr>
	</table>
	</div>
	<!---------------------------------- 하루 창 닫기 div 끝 ---------------------------------------->
	
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
    	var check=document.getElementById('popup');
	    if ( check.checked )
	    setCookie( "popupMain", "done" , 1);
	    window.close();
    }
</script>