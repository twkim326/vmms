﻿<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /main.jsp
 *
 * 메인 페이지
 *
 * 작성일 - 2011/03/21, 정원광
 *
 */

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0002");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(cfg.login());
		return;
	}
	long user_id = cfg.getLong("user.company");
	long user_seq = cfg.getLong("user.seq");
	
	// 코레일유통 로그인시 판매계수로 바로 이동
	
	if ( user_id == 264 ) { 
		response.sendRedirect("/prod/web/src/sales/SalesCount.jsp");
	}
/* 	if(real_id.equals("twkim326")){
		response.sendRedirect("/prod/web/src/sales/SalesRealTime.jsp");
	}  */
// 페이지 유형 설정
	cfg.put("window.mode", "A");

// 날짜
	String date1 = DateTime.date("yyyyMMdd");
	String date2 = DateTime.getAddDay(date1, -1);
	String date3 = DateTime.getAddDay(date1, -2);

// 요일
	int week1 = DateTime.getDayOfWeek(date1);
	int week2 = DateTime.getDayOfWeek(date2);
	int week3 = DateTime.getDayOfWeek(date3);

// 요일
	String[] week = {"", "일", "월", "화", "수", "목", "금", "토"};
%>
<%@ include file="../header.inc.jsp" %>

<!-- 최근 매출 현황 -->
<div class="title">최근 매출 현황</div>
<!-- tab bar -->
<div class="tabType01" style="margin-bottom:0;min-width:960px;width:100%;" >
	<ul id="tab">
		<li id="<%=date1%>" onclick="_sales('<%=date1%>');" class="selected">금일</li>
		<li id="<%=date2%>" onclick="_sales('<%=date2%>');"><%=date2.substring(4, 6)%>-<%=date2.substring(6, 8)%> (<%=week[week2]%>)</li>
		<li id="<%=date3%>" onclick="_sales('<%=date3%>');"><%=date3.substring(4, 6)%>-<%=date3.substring(6, 8)%> (<%=week[week3]%>)</li>
	</ul>
	<a href="./sales/SalesRealTime.jsp" class="btn" ><img src="<%=cfg.get("imgDir")%>/web/btn_more.gif" alt="더보기" /></a>
</div>
<!-- # tab bar -->

<!-- -->
<table cellspacing="0" class="tableType01">
	<colgroup>
		<col style="min-width:96px;width:10%"/>
		<col style="min-width:144px;width:15%"/>
		<col style="min-width:96px;width:10%"/>
		<col style="min-width:144px;width:15%"/>
		<col style="min-width:96px;width:10%"/>
		<col style="min-width:144px;width:15%"/>
		<col style="min-width:96px;width:10%"/>
		<col style="min-width:144px;width:15%"/>
		<col style="min-width:96px;width:10%"/>
		<col style="min-width:144px;width:15%"/>
	</colgroup>
	<thead>
		<tr>
			<th colspan="2">현금</th>
			<th colspan="2">현금영수증</th>
			<th colspan="2">신용</th>
			<th colspan="2">선불</th>
			<th colspan="2">간편결제</th>
		</tr>
		<tr>
			<th class="lv2">건수</th>
			<th class="lv2">금액</th>
			<th class="lv2">건수</th>
			<th class="lv2">금액</th>
			<th class="lv2">건수</th>
			<th class="lv2">금액</th>
			<th class="lv2">건수</th>
			<th class="lv2">금액</th>
			<th class="lv2">건수</th>
			<th class="lv2">금액</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td class="right"><span id="cash0">0</span></td>
			<td class="right"><span id="cash1">0</span></td>
			<td class="right"><span id="receipt0">0</span></td>
			<td class="right"><span id="receipt1">0</span></td>
			<td class="right"><span id="card0">0</span></td>
			<td class="right"><span id="card1">0</span></td>
			<td class="right"><span id="prepay0">0</span></td>
			<td class="right"><span id="prepay1">0</span></td>
			<td class="right"><span id="smartpay0">0</span></td>
			<td class="right"><span id="smartpay1">0</span></td>
		</tr>
		<tr>
			<td colspan="10" class="sum">총 건수: <strong id="total0">0</strong>, 총 금액: <strong id="total1">0</strong></td>
		</tr>
	</tbody>
</table>
<!-- # 최근 매출 현황 -->

<!-- 자판기 가동상태 조회 -->
<div class="title mt23 clearfx" >자판기 가동상태 조회
	<a href="./vm/VMRun.jsp" style="padding-right:15px"><img src="<%=cfg.get("imgDir")%>/web/btn_more.gif" alt="더보기" /></a>
</div>
<table cellspacing="0" class="tableType01">
	<colgroup>
		<col width="*" />
		<col width="160" />
		<col width="160" />
		<col width="160" />
		<!-- 
		<col width="160" />
		 -->
	</colgroup>
	<thead>
		<tr>
			<th><%=(cfg.getLong("user.company") > 0 ? "조직" : "소속")%></th>
			<th>총 자판기</th>
			<th>정상 가동</th>
			<!-- 
			<th>이상 가동</th>
			 -->
			 <th>품절 발생</th>
			<!-- 
			<th>판매 이상</th>
			 -->
		</tr>
	</thead>
	<tbody id="vmRun">
		<tr>
		<!-- 
			<td colspan="5" style="text-align:center; padding:10px 0;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" /></td>
		 -->
		 <td colspan="4" style="text-align:center; padding:10px 0;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" /></td>
		</tr>
	</tbody>
</table>
<!-- # 자판기 가동상태 조회 -->

<!-- 공지사항 & 자주묻는질문 -->
<div class="notice mt23 cl">
	<dl class="mr32">
		<dt><a href="./cs/CsNotice.jsp"><img src="<%=cfg.get("imgDir")%>/web/tit_notice.gif" alt="공지사항" /></a></dt>
		<dd>
			<ul id="notice">
				<li style="text-align:center; padding:22px 0;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" /></li>
			</ul>
		</dd>
		<div style="width:100%">&nbsp;</div>
	</dl>

	<dl>
		<dt><a href="./cs/CsFAQ.jsp"><img src="<%=cfg.get("imgDir")%>/web/tit_faq.gif" alt="자주묻는질문" /></a></dt>
		<dd>
			<ul id="faq">
				<li style="text-align:center; padding:22px 0;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" /></li>
			</ul>
		</dd>
	</dl>
</div>
<!-- # 공지사항 & 자주묻는질문 -->

<%@ include file="../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script language="javascript">
function opennotranvm() 
{
	Common.openWin('./notranvm.jsp', 'notranvm', 530, 400, 'yes');
}
function openmodal1() 
{
    window.showModalDialog("./notice_20180219.jsp", "modal", "dialogWidth:650px;dialogHeight:890px;scroll:1;help:0;status:0");
}

//김태우 추가
//쿠키 확인
function setCookie( name, value, expiredays )
{
	var todayDate = new Date();
	todayDate.setDate( todayDate.getDate() + expiredays );
	document.cookie = name + "=" + escape( value ) + "; path=/; expires=" + todayDate.toGMTString() + ";"
	}
	function getCookie( name )
	{
	var nameOfCookie = name + "=";
	var x = 0;
	while ( x <= document.cookie.length )
	{
	var y = (x+nameOfCookie.length);
	if ( document.cookie.substring( x, y ) == nameOfCookie ) {
	if ( (endOfCookie=document.cookie.indexOf( ";", y )) == -1 )
	endOfCookie = document.cookie.length;
	return unescape( document.cookie.substring( y, endOfCookie ) );
	}
	x = document.cookie.indexOf( " ", x ) + 1;
	if ( x == 0 )
	break;
	}
	return "";
}
	//첫 번째 공지사항
	if ( getCookie( "popupMain" ) != "done" ){
		openmodal();		
}
//김태우 추가

	//추가1 공지사항
	if ( getCookie( "popupSub1" ) != "done" ){
		opensubmodal1();		
}
	
	//추가2 공지사항
	if ( getCookie( "popupSub2" ) != "done" ){
		opensubmodal2();		
}
	//추가3 공지사항
	if ( getCookie( "popupSub3" ) != "done" ){
		opensubmodal3();		
}


function openmodal() 
{
	//Common.openWin('./notice_20190730.jsp', 'openmodal', 500, 520, 'yes'); //긴급공지사항(vmmsServer 지연시)
	//Common.openWin('./notice_20190624.jsp', 'openmodal', 500, 250, 'yes'); //나이스페이먼츠 신용 오류건
	Common.openWin('./notice_cs.jsp', 'openmodal', 510, 740, 'yes'); //고객센터
	//Common.openWin('./notice_20190911.jsp', 'openmodal', 560, 620, 'yes'); // 추석연휴공지
	//Common.openWin('./notice_20180919.jsp', 'openmodal', 520, 350, 'yes');
	//Common.openWin('./notice_20191016.jsp', 'openmodal', 650, 600, 'yes'); //명의이전 신청방법 공지사항
	//Common.openWin('./notice_20190722.jsp', 'openmodal', 650, 540, 'yes'); //서버이전안내
	//Common.openWin('./notice_20190724.jsp', 'openmodal', 550, 540, 'yes'); //매입 늦어질 때 안내
	//Common.openWin('./notice_20190502.jsp', 'openmodal', 600, 450, 'yes');
	//Common.openWin('./notice_20181114.jsp', 'openmodal', 600, 420, 'yes'); //티머니 입금지연
	//Common.openWin('./notice_20190429.jsp', 'openmodal', 600, 450, 'yes'); //새벽 서버 분리 작업
	//Common.openWin('./notice_20190211.jsp', 'openmodal', 650, 620, 'yes'); // 영중소 가맹점 수수료율
	//Common.openWin('./notice_20180709.jsp', 'openmodal', 650, 890, 'yes');
	//Common.openWin('./notice_20180219.jsp', 'openmodal', 650, 890, 'yes');
	//Common.openWin('./notice_20180622.jsp', 'openmodal', 650, 890, 'yes');
	//Common.openWin('./notice_20180308.jsp', 'openmodal', 650, 650, 'no');
}
//김태우 추가
function opensubmodal1() 
{
	//Common.openWinSub('./notice_20191016.jsp', 'opensubmodal1', 630, 420, 'yes'); //명의이전 신청방법 공지사항
	//Common.openWinSub('./notice_20191023.jsp', 'opensubmodal1', 630, 350, 'yes'); //한국신용카드결제(코세스) 사이트 이용 중단 안내
	//Common.openWinSub('./notice_20191121.jsp', 'opensubmodal1', 630, 350, 'yes'); //티머니 지연안내
	//Common.openWinSub1('./notice_20210218.jsp', 'opensubmodal1', 630, 460, 'yes'); //페이코 매입입금 지연안내
	//Common.openWinSub('./notice_20191210.jsp', 'opensubmodal1', 630, 350, 'yes'); //코세스 입금 지연안내 
	//Common.openWinSub('./notice_20191217.jsp', 'opensubmodal1', 630, 350, 'yes'); //정기 PM  
	//Common.openWinSub1('./notice_20200103.jsp', 'opensubmodal1', 630, 390, 'yes'); //방화벽 교체
	//Common.openWinSub1('./notice_20200129.jsp', 'opensubmodal1', 630, 420, 'yes'); //명의이전 최신 공지사항
}
function opensubmodal2() 
{
	//Common.openWinSub2('./notice_20200120.jsp', 'opensubmodal2', 620, 400, 'yes'); //부가세 확인
	//Common.openWinSub2('./notice_20210218.jsp', 'opensubmodal2', 560, 460, 'yes'); //페이코 매입입금 지연안내
}
function opensubmodal3() 
{
	//Common.openWinSub3('./notice_payco.jsp', 'opensubmodal3', 560, 460, 'yes'); //태우 테스트	
	//Common.openWinSub3('./notice_20200123.jsp', 'opensubmodal3', 630, 530, 'yes'); //설날연휴공지		
	//내부 공지용 
	/*if(<%=user_id%>=='0'){		
		//Common.openWin('./notice_batch.jsp', 'opensubmodal3', 510, 740, 'yes'); //고객센터
	}*/
	
	//특정업체 공지용 ex)1639=동구전자 company_seq
	if(<%=user_id%>=='0' || <%=user_id%>=='1639'){		
		//Common.openWinSub3('./notice_company_donggu.jsp', 'opensubmodal3', 510, 610, 'yes'); //동구전자 작업	
	}
	
	//특정계정 공지 테스트용 ex)1927=twkim326 user_seq
	if(<%=user_seq%>=='1927'){		
		Common.openWin('./notice_batch.jsp', 'opensubmodal3', 510, 740, 'yes'); //고객센터
		//Common.openWinSub3('./notice_system.jsp', 'opensubmodal3', 510, 740, 'yes'); //
		//Common.openWinSub3('./notice_system2.jsp', 'opensubmodal3', 510, 740, 'yes'); //
	}
}



<%
	long user_company = cfg.getLong("user.company");
	// (주)롯데칠성, 롯데칠성, 서울 F/S, 서서울OP, 동서울OP 제외
	if ( user_company != 0 && user_company != 1 && user_company != 34 && user_company != 74 && user_company != 80 && user_company != 82) { %>
		//opennotranvm();
	<%}
%>
	function _sales(date) {
		$('tab').getElementsBySelector('li').findAll(function (s) {
			s.className = (s.id == date) ? 'selected' : '';
		}.bind(this));

		$('sbmsg').show();

		new Ajax.Request(G_TOP_DIR + '/common/src/main.jsp', {
			asynchronous : true,
			parameters : {date : date},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'FAIL') {
						window.alert(e.message ? decodeURIComponentEx(e.message) : '최근 매출 현황을 가져오는데 실패하였습니다.');
						$('notice').update('');
						return;
					}

					$('cash0').update(e.sales[0].cash);
					$('cash1').update(e.sales[1].cash);
					$('receipt0').update(e.sales[0].receipt);
					$('receipt1').update(e.sales[1].receipt);
					$('card0').update(e.sales[0].card);
					$('card1').update(e.sales[1].card);
					$('prepay0').update(e.sales[0].prepay);
					$('prepay1').update(e.sales[1].prepay);
					$('smartpay0').update(e.sales[0].smartpay);
					$('smartpay1').update(e.sales[1].smartpay);
					$('total0').update(e.sales[0].total);
					$('total1').update(e.sales[1].total);
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}

				$('sbmsg').hide();
			}.bind(this)
		});
	}

	Event.observe(window, 'load', function (event) {
		new Ajax.Request(G_TOP_DIR + '/common/src/main.jsp', {
			asynchronous : true,
			parameters : {date : '<%=date1%>', isVMRun : 'Y', isNotice : 'Y', isFAQ : 'Y'},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'FAIL') {
						window.alert(e.message ? decodeURIComponentEx(e.message) : '최근 데이터를 가져오는데 실패하였습니다.');
						$('notice').update('');
						return;
					}

					$('cash0').update(e.sales[0].cash);
					$('cash1').update(e.sales[1].cash);
					$('receipt0').update(e.sales[0].receipt);
					$('receipt1').update(e.sales[1].receipt);
					$('card0').update(e.sales[0].card);
					$('card1').update(e.sales[1].card);
					$('prepay0').update(e.sales[0].prepay);
					$('prepay1').update(e.sales[1].prepay);
					$('smartpay0').update(e.sales[0].smartpay);
					$('smartpay1').update(e.sales[1].smartpay);
					$('total0').update(e.sales[0].total);
					$('total1').update(e.sales[1].total);

					var vmRun = '';

					e.vmRun.each (function (data, i) {
						vmRun += '<tr class="' + (i % 2 == 0 ? '' : 'bgLine') + '" onclick="location.href = \'./vm/VMRun.jsp?company=' + data.company + '&organ=' + data.organ + '\';" style="cursor:pointer;">'
							+ '<td align="left" style="padding:0 5px;">' + decodeURIComponentEx(data.name) + '</td>'
							+ '<td align="right">' + data.total + ' 대</td>'
							+ '<td class="right normal">' + data.response + ' 대</td>'
							+ '<td class="right error">' + data.soldout + ' 대</td>'
							//+ '<td class="right" style="font-color:#ff8500;">' + data.empty + ' 대</td>'
							+ '</tr>';
					}.bind(this));

					$('vmRun').update(vmRun);

					var notice = '';

					e.notice.each (function (data, i) {
						if (i > 3) {
							return;
						}

						notice += '<li>'
							+ '<a href="./cs/CsNoticeDetail.jsp?seq=' + data.seq + '">' + Common.strCut(decodeURIComponentEx(data.title), 30) + '</a>'
							+ '<span>' + data.date + '</span>'
							+ '</li>';
					}.bind(this));

					$('notice').update(notice);

					var faq = '';

					e.faq.each (function (data, i) {
						if (i > 3) {
							return;
						}

						faq += '<li>'
							+ '<a href="./cs/CsFAQDetail.jsp?seq=' + data.seq + '">' + Common.strCut(decodeURIComponentEx(data.title), 30) + '</a>'
							+ '<span>' + data.date + '</span>'
							+ '</li>';
					}.bind(this));

					$('faq').update(faq);
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}
			}.bind(this)
		});
	}.bind(this));
</script>
