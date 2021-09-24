<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.Organization
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /system/SystemOrganization.jsp
 *
 * 시스템 > 조직관리
 *
 * 작성일 - 2011/03/25, 정원광
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0105");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print(cfg.login());
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth()) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "A");

// 인스턴스 생성
	Organization objOrgan = new Organization(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objOrgan.regist();
	} catch (Exception e) {
		error = e.getMessage();
	}

// 에러 처리
	if (error != null) {
		out.print(error);
		return;
	}
%>
<%@ include file="../../header.inc.jsp" %>

<div class="title">
	<span>조직관리</span>
</div>

<table cellspacing="0" class="tableType02">
	<colgroup>
		<col width="110" />
		<col width="*"/>
		<col width="73" />
	</colgroup>
	<tr>
		<th><span>소속</span></th>
		<td>
			<!-- <div style="position:relative;"> -->
				<select id="company" class="js-example-basic-single js-example-responsive" style="width: 30%"  class="<%=(cfg.getLong("user.company") > 0 ? "disabled" : "")%>" onchange="_organ(this.options[selectedIndex].value, 0, 0)"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
					<option value="0">- 선택하세요</option>
			<% for (int i = 0; i < objOrgan.company.size(); i++) { GeneralConfig c = (GeneralConfig) objOrgan.company.get(i); %>
					<option value="<%=c.getLong("SEQ")%>"<%=(c.getLong("SEQ") == cfg.getLong("user.company") ? " selected" : "")%>><%=c.get("NAME")%></option>
			<% } %>
				</select>
				<!--a style="position:absolute; right:0; top:5px;">
					* 수수료율은 운영 자판기가 등록되는 최하위 조직에만 설정하세요.
				</a-->
			<!-- </div> -->
		</td>
	</tr>
</table>

<div id="organList"></div>

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/layer/js/iframe.js"></script>
<style type="text/css">
	div.box {width:250px; height:120px; margin-left:12px; overflow:auto; float:left; border-width:1px; border-style:solid; border-color:#ccc;}
	div.box ul li a {margin:0; padding:3px 5px; display:block; cursor:pointer; font-size:12px;}
	div.box ul li a:hover {background-color:#f6f6f6;}
	div.box ul li.s a {background-color:#f6f6f6; font-weight:bold;}
	div.src {width:630px; height:120px; margin-left:20px; float:left; border-width:1px; border-style:solid; border-color:#ccc;}
	div.src dl {height:30px; clear:both;}
	div.src dl dt {width:100px; height:23px; padding-top:6px; float:left; background-color:#e4e2e0; text-align:center; border-width:0 0 1px 0; border-style:solid; border-color:#fff; font-weight:bold;}
	div.src dl dd {width:525px; height:25px; padding:4px 0 0 5px; float:left; border-width:0 0 1px 0; border-style:solid; border-color:#ccc;}
	div.src dl dd a {height:18px; padding-top:4px; display:inline-block;}
	div.src dl dd input {vertical-align:middle;}
	div.src dl.l dt {height:24px; border-color:#ccc; }
	div.src dl.l dd {height:25px; border-width:0; }
</style>
<script type="text/javascript">
	var ableMode = {S : '<%=cfg.get("ENABLE_S")%>', I : '<%=cfg.get("ENABLE_I")%>', U : '<%=cfg.get("ENABLE_U")%>', D : '<%=cfg.get("ENABLE_D")%>'};
	var template = {
		mList : '<table  cellspacing="0" class="tableType04" style="border-top-style:none;" id="organList<%='#'%>{depth}">'
				+ '<tr>'
				+ '<td>'
				+ '<input type="hidden" id="seq_0_<%='#'%>{depth}" value="<%='#'%>{seq}" />'
				+ '<input type="hidden" id="seq_1_<%='#'%>{depth}" />'
				+ '<span>명칭</span> '
				+ '<input type="text" id="name_0_<%='#'%>{depth}" value="<%='#'%>{name}" class="txtInput <%='#'%>{disabled}" <%='#'%>{disabled} /> '
				+ '<input type="button" id="regist_0_<%='#'%>{depth}" class="button1" value="등록" onclick="_regist(0, <%='#'%>{depth});" style="display:<%='#'%>{regist0};" /> '
				+ '<input type="button" id="modify_0_<%='#'%>{depth}" class="button1" value="수정" onclick="_modify(0, <%='#'%>{depth});" style="display:<%='#'%>{modify0};" /> '
				+ '<input type="button" id="delete_0_<%='#'%>{depth}" class="button1" value="삭제" onclick="_delete(0, <%='#'%>{depth});" style="display:<%='#'%>{delete0};" /> '
				+ '<input type="button" id="sTitle_0_<%='#'%>{depth}" class="button1" value="하위" onclick="_sTitle(<%='#'%>{company}, <%='#'%>{depth});" style="display:<%='#'%>{sTitle};" /> '
				+ '</td>'
				+ '</tr>'
				+ '<tr>'
				+ '<td>'
				+ '<div class="box"><ul id="organ<%='#'%>{depth}"></ul></div>'
				+ '<div class="src">'
				+ '<dl>'
				+ '<dt>현재 조직</dt>'
				+ '<dd>'
				+ '<input type="text" id="name_1_<%='#'%>{depth}_m" class="txtInput" style="width:150px;" /> '
				+ '<input type="button" class="button1" value="수정" onclick="_modify(1, <%='#'%>{depth});" style="display:<%='#'%>{modify1};" />'
				+ '</dd>'
				+ '</dl>'
				+ '<dl>'
				+ '<dt>신규 조직</dt>'
				+ '<dd>'
				+ '<input type="text" id="name_1_<%='#'%>{depth}_r" class="txtInput" style="width:150px;" /> '
				+ '<input type="button" class="button1" value="등록" onclick="_regist(1, <%='#'%>{depth});" style="display:<%='#'%>{regist1};" />'
				+ '</dd>'
				+ '</dl>'
				+ '<dl>'
				+ '<dt>상위 조직</dt>'
				+ '<dd>'
				+ '<input type="button" class="button1" value="변경" onclick="_parent(1, <%='#'%>{depth});" style="display:<%='#'%>{parent0};" /> '
				+ '<a style="display:<%='#'%>{parent1};">* 변경이 불가능한 조직입니다.</a>'
				+ '<a style="display:<%='#'%>{parent2};">* 변경할 권한이 없습니다.</a>'
				+ '</dd>'
				+ '</dl>'
				+ '<dl class="l">'
				+ '<dt>조직 삭제</dt>'
				+ '<dd>'
				+ '<input type="button" class="button1" value="삭제" onclick="_delete(1, <%='#'%>{depth});" style="display:<%='#'%>{delete1};" /> '
				+ '<a style="display:<%='#'%>{delete1};">* 하위 조직도 함께 삭제됩니다.</a>'
				+ '<a style="display:<%='#'%>{delete2};">* 삭제할 권한이 없습니다.</a>'
				+ '</dd>'
				+ '</dl>'
				+ '</div>'
				+ '</td>'
				+ '</tr>'
				+ '</table>'
	,	sList : '<li n="<%='#'%>{seq}"><a onclick="_select(<%='#'%>{seq}, \'<%='#'%>{name}\', <%='#'%>{depth}); _organ(<%='#'%>{company}, <%='#'%>{seq}, <%='#'%>{depth});"><%='#'%>{name}</a></li>'
	}

	function _organ(company, organ, depth, onload) {
		var o = {organList : $('organList'), template : null, organ : null};

		if (company == 0) {
			o.organList.update('');
			return;
		}

		if (!onload) {
			$('sbmsg').show();
		}

		new Ajax.Request('SystemOrganizationDetail.jsp', {
			asynchronous : onload ? false : true,
			parameters : {company : company, organ : organ, depth : depth},
			onSuccess : function (xmlHttp) {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == 'FAIL') {
					if (!onload) {
						window.alert('조직 정보를 가져오는데 실패하였습니다.');
						$('sbmsg').hide();
					}

					return;
				}

				o.organList.getElementsBySelector('table').findAll(function (s) {
//alert(">>>>" + s.id + ":"  +  s.id.length + ":" + parseInt(s.id.substr(9, s.id.length)) + ":" + depth);
					if (parseInt(s.id.substr(9, s.id.length)) >= depth) {
						s.remove();
					}
				}.bind(this));

				if (<%=cfg.getLong("user.company")%> > 0 && e.data.seq <= 0) {
					$('sbmsg').hide();
					return;
				}
//alert(company + ":" + depth  );
				o.template = new Template(template.mList);
				o.organList.insert(o.template.evaluate({
						company : company
					,	depth : depth
					,	seq : e.data.seq
					,	name : decodeURIComponentEx(e.data.name)
					,	regist0 : e.data.seq > 0 ? 'none' : (ableMode.I == 'Y' ? '' : 'none')
					,	modify0 : e.data.seq > 0 ? (ableMode.U == 'Y' ? '' : 'none') : 'none'
					,	delete0 : e.data.seq > 0 ? (ableMode.D == 'Y' ? '' : 'none') : 'none'
					,	parent0 : ableMode.U == 'Y' ? (depth == 0 ? 'none' : '') : 'none'
					,	regist1 : ableMode.I == 'Y' ? '' : 'none'
					,	modify1 : ableMode.U == 'Y' ? '' : 'none'
					,	parent1 : ableMode.U == 'Y' ? (depth == 0 ? '' : 'none') : 'none'
					,	delete1 : ableMode.D == 'Y' ? '' : 'none'
					,	delete2 : ableMode.D == 'Y' ? 'none' : ''
					,	parent2 : ableMode.U == 'Y' ? 'none' : ''
					,	sTitle : (<%=cfg.getLong("user.company")%> == 0) ? '' : 'none'
					,	disabled : <%=cfg.getLong("user.company")%> == 0 ? '' : 'disabled'
					}));

				o.organ = $('organ' + depth);
				o.template = new Template(template.sList);
				e.organ.each (function (data, i) {
					o.organ.insert(o.template.evaluate({
							company : company
						,	depth : depth + 1
						,	seq : data.seq
						,	name : decodeURIComponentEx(data.name)
						}));
				}.bind(this));

				if (!onload) {
					$('sbmsg').hide();
				}
			}.bind(this)
		});
	}

	function _select(organ, name, depth, onload) {
		var o = {
			organList : $('organList' + (depth - 1))
		,	organ : $('organ' + (depth - 1))
		,	seq : $('seq_1_' + (depth - 1))
		,	name : $('name_1_' + (depth - 1) + '_m')
		};

		o.organ.getElementsBySelector('li').findAll(function (s) {
			s.className = parseInt(s.getAttribute('n')) == organ ? 's' : '';
		}.bind(this));

		o.seq.value = organ;
		o.name.value = name;

		if (onload) {
			o.organList.getElementsBySelector('input').findAll(function (s) {
				if (s.id && s.id.substr(0, 5) == 'name_') {
					s.disabled = true;
					s.addClassName('disabled');
				}
			}.bind(this));
		}
	}

	function _regist(sort, depth) {
		var o = {
			company : $('company')
		,	organ : $('organ' + depth)
		,	seq : $('seq_' + sort + '_' + (depth - 1))
		,	name : $('name_' + sort + '_' + depth + (sort > 0 ? '_r' : ''))
		};
		var v =
		{
			company : parseInt(o.company.options[o.company.selectedIndex].value)
		,	seq : (o.seq ? o.seq.value : 0)
		,	name : o.name.value
		};

		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		} else if (o.name.disabled) {
			window.alert('권한이 없습니다.');
			return;
		} else if (o.company.selectedIndex == 0) {
			window.alert('소속을 선택하세요.');
			return;
		} else if (o.seq && o.seq.value == '') {
			window.alert('상위 조직이 선택되지 않았습니다.');
			return;
		} else if (o.name.value == '') {
			window.alert('등록할 조직명을 입력하세요.');
			o.name.focus();
			return;
		} else if (o.name.value.indexOf('/') >= 0) {
			window.alert('조직명에는 슬래쉬(/)를 사용하실 수 없습니다.');
			o.name.focus();
			return;
		} else if (!confirm('등록하시겠습니까?')) {
			return;
		}

		$('sbmsg').show();

		new Ajax.Request('SystemOrganizationRegist.jsp', {
			asynchronous : true,
			parameters :
			{
				company : v.company
			,	organ : v.seq
			,	depth : depth
			,	sort : sort
			,	name : encodeURIComponentEx(v.name)
			},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('등록이 완료되었습니다.');

						switch (sort) {
							case 0 : // 명칭
								$('seq_' + sort + '_' + depth).value = e.seq;
								$('regist_' + sort + '_' + depth).hide();
								$('sTitle_' + sort + '_' + depth).show();

								if (ableMode.U == 'Y') {
									$('modify_' + sort + '_' + depth).show();
								} else {
									$('modify_' + sort + '_' + depth).hide();
								}

								if (ableMode.D == 'Y') {
									$('delete_' + sort + '_' + depth).show();
								} else {
									$('delete_' + sort + '_' + depth).hide();
								}

								break;
							default :
								o.name.value = '';
								o.organ.insert((new Template(template.sList)).evaluate({
										company : v.company
									,	depth : depth + 1
									,	seq : e.seq
									,	name : v.name
									}));

								_select(e.seq, v.name, depth + 1);
								_organ(v.company, e.seq, depth + 1);
						}
					} else {
						window.alert(decodeURIComponentEx(e.message));
					}
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}

				$('sbmsg').hide();
			}.bind(this)
		});
	}

	function _modify(sort, depth) {
		var o = {
			company : $('company')
		,	organ : $('organ' + depth)
		,	seq : $('seq_' + sort + '_' + depth)
		,	name : $('name_' + sort + '_' + depth + (sort > 0 ? '_m' : ''))
		};

		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		} else if (o.name.disabled) {
			window.alert('권한이 없습니다.');
			return;
		} else if (o.seq.value == '') {
			window.alert('수정할 조직을 선택하세요.');
			return;
		} else if (o.name.value == '') {
			window.alert('수정할 조직명을 입력하세요.');
			o.name.focus();
			return;
		} else if (o.name.value.indexOf('/') >= 0) {
			window.alert('조직명에는 슬래쉬(/)를 사용하실 수 없습니다.');
			o.name.focus();
			return;
		} else if (!confirm('수정하시겠습니까?')) {
			return;
		}

		$('sbmsg').show();

		new Ajax.Request('SystemOrganizationModify.jsp', {
			asynchronous : true,
			parameters : {seq : o.seq.value, sort : sort, name : encodeURIComponentEx(o.name.value)},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('수정이 완료되었습니다.');

						switch (sort) {
							case 0 : // 명칭
								break;
							default :
								var select = null;

								o.organ.getElementsBySelector('li').findAll(function (s) {
									if (s.getAttribute('n') == o.seq.value) {
										s.update((new Template('<a onclick="_select(<%='#'%>{seq}, \'<%='#'%>{name}\', <%='#'%>{depth}); _organ(<%='#'%>{company}, <%='#'%>{seq}, <%='#'%>{depth});"><%='#'%>{name}</a>')).evaluate({
											seq : o.seq.value
										,	name : o.name.value
										,	depth : depth + 1
										,	company : o.company.value
										}));
									}
								}.bind(this));
						}
					} else {
						window.alert(decodeURIComponentEx(e.message));
					}
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}

				$('sbmsg').hide();
			}.bind(this)
		});
	}

	function _delete(sort, depth) {
		var o = {
			organList : $('organList')
		,	organ : $('organ' + depth)
		,	seq : $('seq_' + sort + '_' + depth)
		,	name : $('name_' + sort + '_' + depth + (sort > 0 ? '_m' : ''))
		};

		if ($('sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		} else if (o.name.disabled) {
			window.alert('권한이 없습니다.');
			return;
		} else if (o.seq.value == '') {
			window.alert('삭제할 조직을 선택하세요.');
			return;
		} else if (!confirm('삭제하시겠습니까?')) {
			return;
		}

		$('sbmsg').show();

		new Ajax.Request('SystemOrganizationDelete.jsp', {
			asynchronous : true,
			parameters : {seq : o.seq.value},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('삭제가 완료되었습니다.');

						switch (sort) {
							case 0 : // 명칭
								o.seq.value = '';
								o.name.value = '';

								$('modify_' + sort + '_' + depth).hide();
								$('delete_' + sort + '_' + depth).hide();
								$('sTitle_' + sort + '_' + depth).show();

								if (ableMode.I == 'Y') {
									$('regist_' + sort + '_' + depth).show();
								} else {
									$('regist_' + sort + '_' + depth).hide();
								}

								break;
							default :
								o.organ.getElementsBySelector('li').findAll(function (s) {
									if (s.getAttribute('n') == o.seq.value) {
										o.seq.value = '';
										o.name.value = '';

										o.organList.getElementsBySelector('table').findAll(function (s) {
											if (parseInt(s.id.substr(9, s.id.length)) > depth) {
												s.remove();
											}
										}.bind(this));

										s.remove();
									}
								}.bind(this));
						}
					} else {
						window.alert(decodeURIComponentEx(e.message));
					}
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}

				$('sbmsg').hide();
			}.bind(this)
		});
	}

	function _sTitle(company, depth) {
		var o = {organList : $('organList'), template : null};

		$('sbmsg').show();

		new Ajax.Request('SystemOrganizationDetail.jsp', {
			asynchronous : true,
			parameters : {company : company, depth : depth + 1},
			onSuccess : function (xmlHttp) {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == 'FAIL') {
					window.alert('조직 정보를 가져오는데 실패하였습니다.');
					$('sbmsg').hide();
					return;
				}

				o.organList.getElementsBySelector('table').findAll(function (s) {
					if (parseInt(s.id.substr(9, s.id.length)) > depth) {
						s.remove();
					}
				}.bind(this));

				o.template = new Template(template.mList);
				o.organList.insert(o.template.evaluate({
						company : company
					,	depth : depth + 1
					,	seq : e.data.seq
					,	name : decodeURIComponentEx(e.data.name)
					,	regist0 : (e.data.seq > 0 ? 'none' : (ableMode.I == 'Y' ? '' : 'none'))
					,	modify0 : (e.data.seq > 0 ? (ableMode.U == 'Y' ? '' : 'none') : 'none')
					,	delete0 : (e.data.seq > 0 ? (ableMode.D == 'Y' ? '' : 'none') : 'none')
					,	parent0 : ableMode.U == 'Y' ? (depth == 0 ? 'none' : '') : 'none'
					,	regist1 : ableMode.I == 'Y' ? '' : 'none'
					,	modify1 : ableMode.U == 'Y' ? '' : 'none'
					,	parent1 : ableMode.U == 'Y' ? (depth == 0 ? '' : 'none') : 'none'
					,	delete1 : ableMode.D == 'Y' ? '' : 'none'
					,	delete2 : ableMode.D == 'Y' ? 'none' : ''
					,	parent2 : ableMode.U == 'Y' ? 'none' : ''
					,	sTitle : (<%=cfg.getLong("user.company")%> == 0 ? '' : 'none')
					,	disabled : (<%=cfg.getLong("user.company")%> == 0 ? '' : 'disabled')
					}));

				$('sbmsg').hide();
			}.bind(this)
		});
	}

	function _parent(sort, depth) {
		var o = {
			seq : $('seq_' + sort + '_' + depth)
		,	name : $('name_' + sort + '_' + depth + (sort > 0 ? '_m' : ''))
		};

		if (depth == 0) {
			window.alert('변경이 불가능한 조직입니다.');
			return;
		} else if (o.name.disabled) {
			window.alert('권한이 없습니다.');
			return;
		} else if (o.seq.value == '') {
			window.alert('변경할 조직을 선택하세요.');
			return;
		}

		new IFrame(510, 155, 'SystemOrganizationModifyParent.jsp?seq=' + o.seq.value).open();
	}

//	Event.observe(window, 'load', function (event) {
<%
	if (cfg.getLong("user.company") > 0) {
		out.println("$('sbmsg').show();");
		out.println("_organ(" + cfg.getLong("user.company") + ", 0, 0, true);");

		if (objOrgan.organ.size() > 0) {
			for (int i = 0; i < objOrgan.organ.size(); i++) {
				GeneralConfig c = (GeneralConfig) objOrgan.organ.get(i);

				out.println("_organ(" + cfg.getLong("user.company") + ", " + c.getLong("SEQ") + ", " + (c.getInt("DEPTH") + 1) + ", true);");
				out.println("_select(" + c.getLong("SEQ") + ", '" + c.getString("NAME").replaceAll("'", "") + "', " + (c.getInt("DEPTH") + 1) + ", true);");
			}
		}

		out.println("$('sbmsg').hide();");
	}
%>
//	}.bind(this));
</script>