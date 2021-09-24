<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.VM
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceVMRegist.jsp
 *
 * 서비스 > 운영 자판기 > 등록
 *
 * 작성일 - 2011/04/04, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0207");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(cfg.login());
		return;
	}

// 페이지 권한 체크
	if (!(cfg.isAuth("I") || cfg.isAuth("U"))) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "B");

// 전송된 데이터
	long seq = StringEx.str2long(request.getParameter("seq"), 0);

// URL에 더해질 파라미터
	String addParam = Param.addParam(request, "page:company:organ:sField:sQuery");

// 인스턴스 생성
	VM objVM = new VM(cfg);

// 메서드 호출
	String error = null;

	if (request.getMethod().equals("POST")) {
		long company = StringEx.str2long(request.getParameter("company"), 0);
		long organ =   StringEx.str2long(request.getParameter("organ"), 0);
		long user =  StringEx.str2long(request.getParameter("user"), 0);
		long terminal =  StringEx.str2long(request.getParameter("terminal_id"), 0);
		
		String code = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("code"), ""), "UTF-8");
		String name = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("name"), ""), "UTF-8");
		String place = URLDecoder.decode(StringEx.setDefaultValue(request.getParameter("place"), ""), "UTF-8");
		
		long vmVendor =   StringEx.str2long(request.getParameter("vmVendor"), 0);
		long vmColumn =  StringEx.str2long(request.getParameter("vmColumn"), 0);
		String sDate = StringEx.charset(request.getParameter("sDate"));
		String eDate = StringEx.charset(request.getParameter("eDate"));

		if (company == 0 || organ == 0 || user == 0 || terminal == 0 || StringEx.isEmpty(code) || StringEx.isEmpty(name) || StringEx.isEmpty(place) ) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode("필수 입력값이 존재하지 않습니다.", "UTF-8") + "\"}");
			return;
		}
		
		
		try {
			error = objVM.registPlace(seq, company, organ, user, terminal, code, name, place, vmVendor, vmColumn, sDate, eDate);
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null) {
			out.print("{\"code\" : \"FAIL\", \"message\" : \"" + URLEncoder.encode(error.replaceAll("'", ""), "UTF-8") + "\"}");
			return;
		}

		out.print("{\"code\" : \"SUCCESS\"}");
		return;
	} else {
		try {
			error = objVM.registPlace(seq);
		} catch (Exception e) {
			error = e.getMessage();
		}

		if (error != null) {
			out.print(error);
			return;
		}
	}
	
	String strPageTitle = null;
	String strPlaceCode = null;
	String strPlaceName = null;
	String strPlace = null;
	
	if ( cfg.getLong("user.company") == 264) {
		strPageTitle = "개소코드 관리";	
		strPlaceCode = "개소코드";
		strPlaceName = "개소명";
		strPlace = "역명";
	} else {
		strPageTitle = "설치장소 관리";	
		strPlaceCode = "설치장소코드";
		strPlaceName = "설치장소명";
		strPlace = "설치장소";
	}
		
%>
<%@ include file="../../header.inc.jsp" %>
<div id="window">
	<div class="title">
		<span><%=strPageTitle %></span>
	</div>
	
	<form method="post" name="save_" id="save_"  >
	<table cellspacing="0" class="tableType03 tableType08" id="regist">
		<colgroup>
			<col width="110" />
			<col width="*"/>
		</colgroup>
		<tr>
			<th><span>소속</span></th>
			<td>
				<input type="hidden" name="company_" id="company_" value="<%=(seq > 0 ? objVM.data.getLong("COMPANY_SEQ") : cfg.getLong("user.company"))%>" />
				<select name="company" id="company" class="checkForm<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>" 
					onchange="CompanyEX2.organ(this.options[selectedIndex].value.evalJSON(), 0, 0, '');
						CompanyEX2.organDup(this.options[selectedIndex].value.evalJSON(), 0, 0,'B');
						_terminal(); _user();
						this.form.company_.value = this.options[selectedIndex].value.evalJSON().seq;"
						option='{"isMust" : true, "message" : "소속을 선택하세요."}'<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
					<option value='{"seq" : 0, "depth" : 0}'>- 소속</option>				
				</select>
			</td>
		</tr>
		<tr>
			<th><span>조직</span></th>
			<td>
				<input type="hidden" name="organ_" id="organ_" class="checkForm" value="" option='{"isMust" : true, "message" : "조직을 선택하세요."}'  />
				<span id="organ">
					<select id="organ0" class="checkForm" 
						onchange="CompanyEX2.organ($('company').value.evalJSON(), 1, this.options[selectedIndex].value, '');_terminal();_user();">
						<option value="-1">- 조직</option>
					</select>
				</span>
			</td>
		</tr>
	 	<tr>
			<th><span>담당자</span></th>
			<td >
				<select name="user" id="user" class="checkForm" option='{"isMust" : true, "message" : "담당자를 선택하세요."}'>
					<option value="0">- 계정</option>
				</select>
			</td>
		</tr>
		<tr>
			<th><span>단말기</span></th>
			<td>
				<select name="terminal_id" id="terminal_id" class="checkForm" option='{"isMust" : true, "message" : "단말기를 선택하세요."}' >
					<option value="-1">- 선택</option>
				</select>
			</td>
		</tr>
		<tr>
			<th><span><%=strPlaceCode %> </span></th>
			<td><input type="text" name="code" id="code"  class="checkForm txtInput" value="<%=objVM.data.getString("CODE") %>" maxlength="12" option='{"isMust" : true,  "message" : "<%=strPlaceCode %>를 입력하세요."}' /></td>
		</tr>
		<tr>
			<th><span><%=strPlaceName %> </span></th>
			<td><input type="text" name="name" id="name"  class="checkForm txtInput" value="<%=objVM.data.getString("NAME") %>" maxlength="32" style="width:330px" option='{"isMust" : true,  "message" : "<%=strPlaceName %>을 입력하세요."}' /></td>
		</tr>
		<tr>
			<th><span><%=strPlace %> </span></th>
			<td><input type="text" name="place" id="place"  class="checkForm txtInput" value="<%=objVM.data.getString("PLACE") %>" maxlength="32" style="width:330px" option='{"isMust" : true,  "message" : "<%=strPlace %>을 입력하세요."}' /></td>
		</tr>
		<tr>
		<th><span>자판기 구분</span></th>
			<td>
				<select id="vmVendor" name="vmVendor" class="checkForm" option='{"isMust" : true,  "message" : "자판기 구분을 입력하세요."}'>
					<option value="">- 전체</option>
			<% for (int i = 0; i < objVM.vmVendor.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.vmVendor.get(i); %>
					<option value="<%=c.getLong("CODE")%>" <%=(objVM.data.getLong("VM_VENDOR")==c.getLong("CODE")?"selected":"")%>><%=c.get("NAME")%></option>
			<% } %>
				</select>
			</td>
		</tr>
		<tr>
			<th><span>컬럼 타입</span></th>
			<td>
				<select id="vmColumn" name="vmColumn" class="checkForm" option='{"isMust" : true,  "message" : "컬럼 타입을 입력하세요."}'>
					<option value="">- 전체</option>
			<% for (int i = 0; i < objVM.vmColumn.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.vmColumn.get(i); %>
					<option value="<%=c.getLong("CODE")%>" <%=(objVM.data.getLong("VM_COLUMN")==c.getLong("CODE")?"selected":"")%>><%=c.get("NAME")%></option>
			<% } %>
				</select>
			</td>
		</tr>
		<tr>
			<th><span>시작일자 </span></th>
			<td><input type="text" name="sDate" id="sDate" class="checkForm txtInput" style="width:80px;" value="<%=objVM.data.getString("START_DATE") %>" 
					maxlength="8" option='{"isMust" : true,  "message" : "시작일자를 입력하세요."}' 
					onkeyup="if (this.value != '' && !/^[0-9]+$/.test(this.value)) { window.alert('숫자만 입력이 가능합니다.'); this.value = ''; }" />
				<img src="<%=cfg.get("imgDir")%>/web/icon_calendar.gif" style="cursor:pointer;" alt="날짜 선택" onclick="(function () {
					var e = event ? event : window.event;
					Calendar.open($('sDate'), null, Event.pointerX(e), Event.pointerY(e)-200, $('sDate').value);
				})(event)" />
			</td>
		</tr>		
		<tr>
			<th><span>종료일자 </span></th>
			<td><input type="text" name="eDate" id="eDate" class="txtInput" style="width:80px;" value="<%=objVM.data.getString("END_DATE") %>" 
				maxlength="8" onkeyup="if (this.value != '' && !/^[0-9]+$/.test(this.value)) { window.alert('숫자만 입력이 가능합니다.'); this.value = ''; }" />
				<img src="<%=cfg.get("imgDir")%>/web/icon_calendar.gif" style="cursor:pointer;" alt="날짜 선택" onclick="(function () {
					var e = event ? event : window.event;
					Calendar.open($('eDate'), null, Event.pointerX(e), Event.pointerY(e)-200, $('eDate').value);
				})(event)" />
			</td>
		</tr>			
		
		
	</table>
	
	
	
	<div class="buttonArea">
		<input type="button" value="" class="btnRegiS" onclick="_save();" />
		<input type="button" value="" class="btnCancelS" onclick="new parent.IFrame().close();" />
	</div>
	</form>
</div>
<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:block;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/layer/js/iframe.js"></script>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/calendar/js/calendar.js"></script>
<script type="text/javascript">
	
	function _terminal() {
		
		for (var i = $('company').value.evalJSON().depth; i >= 0; i--) {
			var organ = $('organ' + i);

			if (organ && organ.selectedIndex > 0) {
				$('organ_').value = organ.options[organ.selectedIndex].value;
				break;
			}
		}
		//alert($('company').value.evalJSON().seq + ":" + $('organ_').value + ":");
		
		if ($('company').value.evalJSON().seq == 0 ) return;
		
		new Ajax.Request('ServiceVMInstallPlaceTerminal.jsp', {
			asynchronous : false,
			parameters : {company : $('company').value.evalJSON().seq, organ : $('organ_').value},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'FAIL') {
						window.alert(e.message ? decodeURIComponentEx(e.message) : '그룹 정보를 가져오는데 실패하였습니다.');
						return;
					}

					var s = $('terminal_id')
					var cnt = 0;
					
					s.options[0].text = "- loading";

					for (var i = s.length - 1; i > 0; i--) {
						s.options[i] = null;
					}

					e.data.each (function (data, i) {
						s.options[i + 1] = new Option(decodeURIComponentEx( data.terminal_id + " / " + data.place), data.seq);
						if ( data.seq == <%=objVM.data.getLong("VM_SEQ")%>) {
							s.options[i + 1].selected = true;
						}
						cnt++;
					}.bind(this));

					s.options[0].text = '- 단말기ID(' + cnt + ')';
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}
			}.bind(this)
		});
		
	}
	function _user() {

		for (var i = $('company').value.evalJSON().depth; i >= 0; i--) {
			var organ = $('organ' + i);

			if (organ && organ.selectedIndex > 0) {
				$('organ_').value = organ.options[organ.selectedIndex].value;
				break;
			}
		}
		//alert($('company').value.evalJSON().seq + ":" + $('organ_').value + ":");
		
		if ($('company').value.evalJSON().seq == 0 ) return;
		
		new Ajax.Request('ServiceVMInstallPlaceUser.jsp', {
			asynchronous : false,
			parameters : {company : $('company').value.evalJSON().seq, organ : $('organ_').value},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'FAIL') {
						window.alert(e.message ? decodeURIComponentEx(e.message) : '그룹 정보를 가져오는데 실패하였습니다.');
						return;
					}

					var s = $('user')
					var cnt = 0;
					
					s.options[0].text = "- loading";

					for (var i = s.length - 1; i > 0; i--) {
						s.options[i] = null;
					}

					e.data.each (function (data, i) {
						s.options[i + 1] = new Option(decodeURIComponentEx( data.name + " / "+ data.id), data.seq);
						if ( data.seq == <%=objVM.data.getLong("USER_SEQ")%>) {
							s.options[i + 1].selected = true;
						}
						cnt++;
					}.bind(this));

					s.options[0].text = '- 계정(' + cnt + ')';
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}
			}.bind(this)
		});	
	}
	

	
	function _save() {
		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		}

		for (var i = $('company').value.evalJSON().depth; i >= 0; i--) {
			var organ = $('organ' + i);

			if (organ && organ.selectedIndex > 0) {
				$('organ_').value = organ.options[organ.selectedIndex].value;
				break;
			}
		}

		//if ($('company') && $('company').selectedIndex > 0) {
		//	$('company_').value = $('company').options[$('company').selectedIndex].value.evalJSON().seq;
		//}
		
		var error = Common.checkForm($('regist'));
		var check = new Array();
		var index = 0;

		if (error != '') {
			window.alert(error);
			return;
		}
		
		
		if (!confirm('등록하시겠습니까?')) {
			return;
		}	
		
		new Ajax.Request(location.pathname, {
			asynchronous : false,
			parameters : {
					seq : <%=seq%>
				,	company : $('company').options[$('company').selectedIndex].value.evalJSON().seq
				,	organ : $('organ_').value
				,	user : $('user').value
				,	terminal : $('terminal_id').value
				,	code : encodeURIComponentEx($('code').value)
				,	name : encodeURIComponentEx($('name').value)
				,	place : encodeURIComponentEx($('place').value)
				,	vmVendor : $('vmVendor').value
				,	vmColumn : $('vmColumn').value
				,	sDate : $('sDate').value
				,	eDate : $('eDate').value
			},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('등록이 완료되었습니다.');
						parent.location.reload();
					} else {
						window.alert(decodeURIComponentEx(e.message));
					}
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}
			}.bind(this)
		});
		

		//$('sbmsg').show();
		//$('save_').submit();
	}

	function _clear() {
		$('sbmsg').hide();
	}


//	Event.observe(window, 'load', function (event) {
		
<% if (cfg.get("user.operator").equals("Y")) { %>


		CompanyEX2.depth = <%=cfg.getInt("user.organ.depth")%>;
		<%if (seq > 0) { %>		
		//CompanyEX2.company(<%=cfg.getLong("user.company")%>, <%=objVM.data.getLong("ORGANIZATION_SEQ")%>,<%=objVM.data.getLong("USER_ORG_SEQ")%>, {mode : 'B', user : <%=objVM.data.getLong("USER_SEQ")%>});
		CompanyEX2.company(<%=cfg.getLong("user.company")%>, <%=objVM.data.getLong("ORGANIZATION_SEQ")%>,<%=cfg.getLong("user.organ")%>, {mode : 'B', user : <%=objVM.data.getLong("USER_SEQ")%>});
		<%} else{ %>
		CompanyEX2.company(<%=cfg.getLong("user.company")%>, <%=cfg.getLong("user.organ")%>,<%=cfg.getLong("user.organ")%>, {mode : 'B', user : <%=cfg.getLong("user.seq")%>});
		<%}%>		
<% } else if (seq > 0) { %>

		
		CompanyEX2.depth = <%=cfg.getInt("user.organ.depth")%>;
		CompanyEX2.company(<%=objVM.data.getLong("COMPANY_SEQ")%>, <%=objVM.data.getLong("ORGANIZATION_SEQ")%>,<%=objVM.data.getLong("USER_ORG_SEQ")%>, {mode : 'B', user : <%=objVM.data.getLong("USER_SEQ")%>});
		
		//Company.depth = <%=cfg.getInt("user.organ.depth")%>;
		//Company.company(<%=objVM.data.getLong("COMPANY_SEQ")%>, <%=objVM.data.getLong("ORGANIZATION_SEQ")%>, {mode : 'B', user : <%=objVM.data.getLong("USER_SEQ")%>});
<% } else { %>

		
		CompanyEX2.depth = <%=cfg.getInt("user.organ.depth")%>;
		CompanyEX2.company(<%=cfg.getLong("user.company")%>, <%=cfg.getLong("user.organ")%>, <%=cfg.getLong("user.organ")%>, {mode : 'B', user : <%=cfg.getLong("user.seq")%>});
		
		//Company.depth = <%=cfg.getInt("user.organ.depth")%>;
		//Company.company(<%=cfg.getLong("user.company")%>, <%=cfg.getLong("user.organ")%>, {mode : 'B', user : <%=cfg.getLong("user.seq")%>});
<% } %>
	_clear();
//	}.bind(this));
</script>