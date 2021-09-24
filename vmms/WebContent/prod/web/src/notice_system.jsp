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
<link rel="shortcut icon" href="/image/web/favicon_ubcn.png">
<title>공지사항</title>
<style type="text/css">
	#window {
	    -ms-overflow-style: none; /* IE and Edge */
	    scrollbar-width: none; /* Firefox */
	}
	
	#window::-webkit-scrollbar {
    	display: none; /* Chrome, Safari, Opera*/
	}
	
	#content{
		width:100%; height: 92%;
	}
	
	#notice_close{
		height: 8%; width: 100%; text-align: right; font-size: 11px; color: #777; background-color: #efefef;line-height: 45px;margin-bottom: 0px;
	}
	
	#notice_close>input{
		width: 13px;height: 26px;vertical-align: middle;margin: 0;padding: 0;
	}
	
	#notice_close>button{
		width: 17px; height: 15px; vertical-align: middle; margin: 0; padding: 0; border: none; background-color: transparent; margin-right:10px;
	}
	
	#content_title{
		height: 16%; display: table;width: 80%;margin: 0px auto;
	}
	
	#content_title>p{
		font-weight: bold; font-size : 20px;
	}
	
	#content_subTitle{
		height: 28%; display: table;width: 80%;margin: 0px auto; margin-top: 3%; padding:20px; background-color: #fde9ea; font-weight:bold;
	}
			
	#errDate{
		font-weight: bold;
	}
	
	#content_cal{
		height:45%; display: table;width: 88%;margin: 0px auto; margin-top: 3%
	}
	
	#content_cal>table{
		width: 100%;height: 100%;border-collapse: collapse;
	}
	
	p{
		display: table-cell;vertical-align: middle;
	}
	th{
		font-size: 13px;
	}
	th:nth-child(1){
		color: red;
	}
	th:nth-child(7){
		color: blue;
	}
	tr>td{
		text-align: center;
		font-weight: bold;
	}
	
	tr>td>div{
		font-size:15px;
	}
	
	tr>td>div>div:nth-child(3){
		font-size:12px;
	}
			
	tr>td:nth-child(1){		
		color:red;
	}
	
	tr>td:nth-child(7){		
		color:blue;
	}
	
	tr>td:nth-child(1)>div>div:nth-child(2){		
		color:black;
	}
	tr>td:nth-child(7)>div>div:nth-child(2){		
		color:black;
	}
	
	#sales{
	
	}
	
	#error{
	
	}
	
</style>
</head>
<body style="margin:0px;">
<div id="window">
	
	<!-- <img src="./notice_calander.png" style="width:100%; height:92%;"/> -->
	<div id="content">
		<div id="content_title" align="center">
			<p>시스템사업부 심야 모니터링 안내</p>
		</div>
		<div id="content_subTitle">
			<div id="cause">작업목적 : 차세대 Online 모니터링</div>
			<div id="errDate">진행일시 : 2021.03.10 ~ 17 [00:00 ~ 07:00]</div> 
			<font>
			작업내용 : <br/>
			상기 내용에 따라 시스템 사업부에서 차세대 Online 새벽 모니터링이 진행될 예정입니다.</font>		
		</div>
		<div id="content_cal" align="center">
			<table>
				<colgroup>
					<col width="70"/>
					<col width="70"/>
					<col width="70"/>
					<col width="70"/>
					<col width="70"/>
					<col width="70"/>
					<col width="70"/>
				</colgroup>
				<thead>
			        <tr style="
    background-color: #cccccc;
">
			            <th>일요일</th>
			            <th>월요일</th>
			            <th>화요일</th>
			            <th>수요일</th>
			            <th>목요일</th>
			            <th>금요일</th>
			            <th>토요일</th>
			        </tr>
			    </thead>
				<tr style="
    background-color: #f9f6f1;
">
					<td>
						<div>
							<div>7</div>
							<div>&nbsp</div>
							<div>&nbsp</div>
						</div>
					</td>
					<td>
						<div>
							<div>8</div>
							<div>&nbsp</div>
							<div>&nbsp</div>
						</div>
					</td>
					<td>
						<div>
							<div>9</div>
							<div>&nbsp</div>	
							<div>&nbsp</div>						
						</div>
					</td>
					<td>
						<div>
							<div>10</div>
							<div>진용석</div>		
							<div>&nbsp</div>					
						</div>
					</td>
					<td>
						<div>
							<div>11</div>
							<div>최동현</div>	
							<div>&nbsp</div>					
						</div>
					</td>
					<td>
						<div>
							<div>12</div>
							<div>김태우</div>
							<div>&nbsp</div>					
						</div>
					</td>
					<td>
						<div>
							<div>13</div>
							<div>채종욱</div>	
							<div>&nbsp</div>					
						</div>
					</td>
				</tr>
				<tr style="
    background-color: #f9f6f1;
">
					<td>
						<div>
							<div>14</div>
							<div>정인석</div>	
							<div>&nbsp</div>						
						</div>
					</td>
					<td>
						<div>
							<div>15</div>
							<div>허승찬</div>		
							<div>&nbsp</div>					
						</div>
					</td>
					<td>
						<div>
							<div>16</div>
							<div>정인석</div>
							<div>&nbsp</div>							
						</div>
					</td>
					<td>
						<div>
							<div>17</div>
							<div>채종욱</div>
							<div>&nbsp</div>							
						</div>
					</td>
					<td>
						<div>
							<div>18</div>
							<div>&nbsp</div>
							<div>&nbsp</div>							
						</div>
					</td>
					<td>
						<div>
							<div>19</div>
							<div>&nbsp</div>
							<div>&nbsp</div>							 
						</div>
					</td>
					<td>
						<div>
							<div>20</div>
							<div>&nbsp</div>
							<div>&nbsp</div>							
						</div>
					</td>
				</tr>
			</table>
		</div>
	</div>
	<!-- 오늘 하루 이 창을 열지 않음 -->
	<form id="notice_close" class="popup_option" name="form1" style=" "> 
		<input type="checkbox" name="popup" value="">
		<label for="notToday" >오늘 하루 이 창을 열지 않음</label>
		<button title="닫기" onclick="closeWin();">
			<img src="/prod/common/module/btn_close2.gif" alt="X" style="vertical-align: middle;">
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