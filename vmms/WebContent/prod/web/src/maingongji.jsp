<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.lib.db.DBLibrary
			, java.net.*
			, org.apache.log4j.Logger
			, org.apache.log4j.PropertyConfigurator
			, java.sql.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
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
				<br /><br /><br />
				<strong><font size="5" color="blue">●공지사항●</font></strong>
			</td>
		</tr>
		<tr>
			<td>
				<br /><br />
				<strong>
<!-- 			<font size="4"> -->
<!-- 				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;기간 : 10월8일 ~ 10월24일<br /><br /> -->
<!-- 				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1) DB정리 작업 <br /><br /> -->
<!-- 				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2) VMMS 조회 불가능<br /><br /> -->
<!-- 				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;3) VMMS 등록, 수정, 삭제 불가<br /><br /> -->
<!--				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; -->
<!--			</font> -->
<!-- 			<font size="4" color="red"> -->
<!-- 				(등록, 수정, 삭제시 데이터 적용 않됨)<br /> -->
<!-- 				<br /> -->
<!-- 				<br /> -->
<!-- 			</font> -->
			<font size="3" color="blue">
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;먼저 고객님들의 이용에 불편함을 제공한 점 사과의 말씀을 전해 드립니다.<br /><br /><br />
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;3월 7일 신용카드로 거래된 데이터는 3월 16일 거래 데이터와 합산되어 <br /><br />
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;23일에 입금될 예정입니다.<br /><br /> 
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(위 내용은 3월7일 신용거래만 해당합니다. 선불은 정상 진행 되었습니다.)<br /><br />
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;현재 서버 업데이트가 진행되고있어 일부 기능의 사용이 불가할 수 있습니다.<br /><br />
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;다시 한번 이용자 분들께 불편함을 드리게 된 점 진심으로 사과의 말씀 전해 드리며<br /><br />
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;보다 원활하고 쾌적한 서비스 환경을 제공해 드릴 수 있도록 최선을 다하겠습니다.<br /><br />
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
//	self.focus();

	function winclose() {
		window.close();
	}
</script>