<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.Category
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceCategoryRegist.jsp
 *
 * 서비스 > 상품 카테고리
 *
 * 작성일 - 2017/05/10, 황재원
 *
 */

// 헤더
	request.setCharacterEncoding("ISO-8859-1");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0204");

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
	Category objCate = new Category(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objCate.regist();
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
<div id="window">
	<div class="title">
		<span>상품 그룹 관리</span>
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
					<select id="company" class="js-example-basic-single js-example-responsive" style="width: 20%"  class="<%=(cfg.getLong("user.company") > 0 ? "disabled" : "")%>" onchange="_cate(this.options[selectedIndex].value, 0, 0)"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%> >
						<option value="0">- 선택하세요</option>
				<% for (int i = 0; i < objCate.company.size(); i++) { GeneralConfig c = (GeneralConfig) objCate.company.get(i); %>
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
	
	<div id="cateList"></div>
</div>
<%@ include file="../../footer.inc.jsp" %>

<div id="window_sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/layer/js/iframe.js"></script>
<style type="text/css">
	#window_sbmsg { width:32px; height:32px; padding:19px; background:url(../web/bg_loading.gif); position:absolute; left:335px; top:250px;}
	
	div.box {width:250px; height:120px; margin-left:12px; overflow:auto; float:left; border-width:1px; border-style:solid; border-color:#ccc;}
	div.box ul li a {margin:0; padding:3px 5px; display:block; cursor:pointer; font-size:12px;}
	div.box ul li a:hover {background-color:#f6f6f6;}
	div.box ul li.s a {background-color:#f6f6f6; font-weight:bold;}
	
	div.src_a {width:630px; height:120px; float:left; border-width:0px;}
	
	div.src_l {width:260px; height:120px; margin-left:0px; float:left;  border-top: 1px solid #ccc;  border-bottom: 1px solid #ccc;  border-left: 1px solid #ccc; }
	div.src_l dl {height:30px; clear:both;}
	div.src_l dl dt {width:80px; height:23px; padding-top:6px; float:left; background-color:#e4e2e0; text-align:center; border-width:0 0 1px 0; border-style:solid; border-color:#fff; font-weight:bold;}
	div.src_l dl dd {width:175px; height:25px; padding:4px 0 0 5px; float:left; border-width:0 0 1px 0; border-style:solid; border-color:#ccc;}
	div.src_l dl dd a {height:18px; padding-top:4px; display:inline-block;}
	div.src_l dl dd input {vertical-align:middle;}
	div.src_l dl.l dt {height:24px; border-color:#ccc; }
	div.src_l dl.l dd {height:25px; border-width:0; }
	
	div.src_r {width:345px; height:120px; margin-left:0px; float:left;  border-top: 1px solid #ccc;  border-right: 1px solid #ccc; border-bottom: 1px solid #ccc;  }
	div.src_r dl {height:30px; clear:both;}
	div.src_r dl dt {width:30px; height:23px; padding-top:6px; float:left; text-align:left; border-bottom: 1px solid #ccc; }
	div.src_r dl dd {width:310px; height:25px; padding:4px 0 0 5px; float:left; border-width:0 0 1px 0; border-style:solid; border-color:#ccc;}
	div.src_r dl dd a {height:18px; padding-top:4px; display:inline-block;}
	div.src_r dl dd input {vertical-align:middle;}
	div.src_r dl.l dt {height:24px; border-color:#ccc; }
	div.src_r dl.l dd {height:25px; border-width:0; }
</style>
<script type="text/javascript">
	var ableMode = {S : '<%=cfg.get("ENABLE_S")%>', I : '<%=cfg.get("ENABLE_I")%>', U : '<%=cfg.get("ENABLE_U")%>', D : '<%=cfg.get("ENABLE_D")%>'};
	var template = {
		mList : '<table  cellspacing="0"  class="tableType04" style="border-top-style:none;" id="cateList<%='#'%>{depth}">'
				+ '<tr>'
				+ '<td colspan="3">'
				+    '<input type="hidden" id="seq_0_<%='#'%>{depth}" value="<%='#'%>{seq}" />'
				+    '<input type="hidden" id="seq_1_<%='#'%>{depth}" />'
				+    '<span style="display:<%='#'%>{parent1};">그룹군</span> '
				+    '<span style="display:<%='#'%>{parent0};">그룹</span> '
				+ '</td>'
				+ '</tr>'
				+ '<tr>'
				+ '<td width="274">'
				+    '<div class="box"><ul id="cate<%='#'%>{depth}"></ul></div>'
				+ '</td>'
				+ '<td>'				
				+    '<div class="src_a"><div class="src_l">'
				+       '<dl>'
				+          '<dt>현재 그룹</dt>'
				+          '<dd>'
				+             '<input type="text" id="name_1_<%='#'%>{depth}_m" class="txtInput" style="width:150px;" /> '
				+          '</dd>'
				+       '</dl>'
				+       '<dl>'
				+          '<dt>신규 그룹</dt>'
				+          '<dd>'
				+             '<input type="text" id="name_1_<%='#'%>{depth}_r" class="txtInput" style="width:150px;" /> '
				+          '</dd>'
				+       '</dl>'
				+       '<dl>'
				+          '<dt>상위 그룹</dt>'
				+          '<dd>'
				+             '<input type="button" class="button1" value="변경" onclick="_parent(1, <%='#'%>{depth});" style="display:<%='#'%>{parent0};" /> '
				+             '<a style="display:<%='#'%>{parent1};">* 변경 불가 그룹.</a>'
				+             '<a style="display:<%='#'%>{parent2};">* 변경할 권한없슴.</a>'
				+          '</dd>'
				+       '</dl>'
				+       '<dl class="l">'
				+          '<dt>그룹 삭제</dt>'
				+          '<dd>'
				+             '<input type="button" class="button1" value="삭제" onclick="_delete(1, <%='#'%>{depth});" style="display:<%='#'%>{delete1};" /> '
				+             '<a style="display:<%='#'%>{delete1};">* 하위 그룹 함께삭제.</a>'
				+             '<a style="display:<%='#'%>{delete2};">* 삭제할 권한없슴.</a>'
				+          '</dd>'
				+       '</dl>'
				+    '</div>'
				+    '<div class="src_r">'
				+       '<dl>'
				+          '<dt>코드</dt>'
				+          '<dd>'
				+             '<input type="text" id="code_1_<%='#'%>{depth}_m" class="txtInput" style="width:150px;" /> '
				+             '<input type="checkbox" id="auto_1_<%='#'%>{depth}_m" class="checkbox" onclick="checkcode(this.checked, code_1_<%='#'%>{depth}_m)" /> <label for="auto_1_<%='#'%>{depth}_m">자동생성</label> '
				+             '<input type="button" class="button1" value="수정" onclick="_modify(1, <%='#'%>{depth});" style="display:<%='#'%>{modify1};" />'
				+          '</dd>'
				+       '</dl>'
				+       '<dl>'
				+          '<dt>코드</dt>'
				+          '<dd>'
				+             '<input type="text" id="code_1_<%='#'%>{depth}_r" class="txtInput" style="width:150px;" /> '
				+             '<input type="checkbox" id="auto_1_<%='#'%>{depth}_r" class="checkbox"  onclick="checkcode(this.checked, code_1_<%='#'%>{depth}_r)"/> <label for="auto_1_<%='#'%>{depth}_r">자동생성</label> '				
				+             '<input type="button" class="button1" value="등록" onclick="_regist(1, <%='#'%>{depth});" style="display:<%='#'%>{regist1};" />'
				+          '</dd>'
				+        '</dl>'
				+        '<dl>'
				+          '<dt></dt>'
				+          '<dd></dd>'
				+        '</dl>'					
				+    '</div></div>'
				+ '</td>'
				+ '</tr>'
				+ '</table>'
	,	sList : '<li n="<%='#'%>{seq}"><a onclick="_select(<%='#'%>{seq}, \'<%='#'%>{name}\', \'<%='#'%>{code}\', <%='#'%>{depth}); _cate(<%='#'%>{company}, <%='#'%>{seq}, <%='#'%>{depth});"><%='#'%>{name}</a></li>'
	}

	function checkcode(checked, codetxt) {

		if (checked) { 
			$(codetxt).value = ''; 
			$(codetxt).disabled = true; 
			$(codetxt).addClassName('disabled'); 
		} else { 
			$(codetxt).disabled = false; 
			$(codetxt).removeClassName('disabled'); 
		}

	}
	
	function _cate(company, cate, depth, onload) {
		var o = {cateList : $('cateList'), template : null, cate : null};
	
		//alert(company + ":" + cate + ":" + depth);
		if (depth > 1) return;
		
		if (company == 0) {
			o.cateList.update('');
			return;
		}
		
		if (!onload) {
			$('window_sbmsg').show();
		}

		new Ajax.Request('ServiceCategoryDetail.jsp', {
			asynchronous : onload ? false : true,
			parameters : {company : company, cate : cate, depth : depth},
			onSuccess : function (xmlHttp) {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == 'FAIL') {
					if (!onload) {
						window.alert('그룹 정보를 가져오는데 실패하였습니다.');
						$('window_sbmsg').hide();
					}

					return;
				}

				o.cateList.getElementsBySelector('table').findAll(function (s) {
					//alert(">>>>" + parseInt(s.id.substr(8, s.id.length)) + ":" + depth);
					if (parseInt(s.id.substr(8, s.id.length)) >= depth) {
						s.remove();
					}
				}.bind(this));

				if (<%=cfg.getLong("user.company")%> > 0 && e.data.seq < 0) {
					$('window_sbmsg').hide();
					return;
				}

				o.template = new Template(template.mList);
				o.cateList.insert(o.template.evaluate({
						company : company
					,	depth : depth
					,	seq : e.data.seq
					,	name : decodeURIComponentEx(e.data.name)
					,	code : decodeURIComponentEx(e.data.catcode)
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

				o.cate = $('cate' + depth);
				o.template = new Template(template.sList);
				e.cate.each (function (data, i) {
					o.cate.insert(o.template.evaluate({
							company : company
						,	depth : depth + 1
						,	seq : data.seq
						,	name : decodeURIComponentEx(data.name)
						,	code : decodeURIComponentEx(data.catcode)
						}));
				}.bind(this));

				if (!onload) {
					$('window_sbmsg').hide();
				}
			}.bind(this)
		});
	}

	function _select(cate, name, code, depth, onload) {
		//alert(cate + ":" + name + ":" + code + ":" + depth);
		var o = {
				cateList : $('cateList' + (depth - 1))
			,	cate : $('cate' + (depth - 1))
			,	seq : $('seq_1_' + (depth - 1))
			,	name : $('name_1_' + (depth - 1) + '_m')
			,	code : $('code_1_' + (depth - 1) + '_m')
			,	checkbox : $('auto_1_' + (depth - 1) + '_m')
		};

		o.cate.getElementsBySelector('li').findAll(function (s) {
			s.className = parseInt(s.getAttribute('n')) == cate ? 's' : '';
		}.bind(this));

		o.seq.value = cate;
		o.name.value = name;
		o.code.value = code;
		
		o.code.disabled = true;
		o.code.addClassName('disabled');
		
		o.checkbox.disabled = true;
		
		if (onload) {
			o.cateList.getElementsBySelector('input').findAll(function (s) {
				if (s.id && (s.id.substr(0, 5) == 'name_' || s.id.substr(0, 5) == 'code_' )) {
					s.disabled = true;
					s.addClassName('disabled');
				}
			}.bind(this));
		}
	}

	function _regist(sort, depth) {
		var o = {
				company : $('company')
			,	cate : $('cate' + depth)
			,	seq : $('seq_' + sort + '_' + (depth - 1))
			,	name : $('name_' + sort + '_' + depth + (sort > 0 ? '_r' : ''))
			,	code : $('code_' + sort + '_' + depth + (sort > 0 ? '_r' : ''))
		};
		var v =
		{
				company : parseInt(o.company.options[o.company.selectedIndex].value)
			,	seq : (o.seq ? o.seq.value : 0)
			,	name : o.name.value
			,	code : o.code.value
		};

		//alert(v.company + ":" + v.seq + ":" + v.name + ":" + v.code);

		if ($('window_sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		} else if (o.name.disabled) {
			window.alert('권한이 없습니다.');
			return;
		} else if (o.company.selectedIndex == 0) {
			window.alert('그룹을 선택하세요.');
			return;
		} else if (o.seq && o.seq.value == '') {
			window.alert('상위 그룹이 선택되지 않았습니다.');
			return;
		} else if (o.name.value == '') {
			window.alert('등록할 그룹명을 입력하세요.');
			o.name.focus();
			return;
		} else if (o.name.value.indexOf('/') >= 0) {
			window.alert('그룹명에는 슬래쉬(/)를 사용하실 수 없습니다.');
			o.name.focus();
			return;
		} else if (!o.code.disabled && o.code.value == '') {
			window.alert('등록할 코드를 입력하세요.');
			o.code.focus();
			return;
		} else if (!confirm('등록하시겠습니까?')) {
			return;
		}

		$('window_sbmsg').show();

		new Ajax.Request('ServiceCategoryRegist.jsp', {
			asynchronous : true,
			parameters :
			{
					company : v.company
				,	cate : v.seq
				,	depth : depth
				,	sort : sort
				,	name : encodeURIComponentEx(v.name)
				,	code : encodeURIComponentEx(v.code)
			},
			onSuccess : function (xmlHttp) {
				try {
					var e = xmlHttp.responseText.evalJSON();

					if (e.code == 'SUCCESS') {
						window.alert('등록이 완료되었습니다.');
//alert("sort: " + sort);
						switch (sort) {
							case 0 : // 명칭
								$('seq_' + sort + '_' + depth).value = e.seq;
								$('regist_' + sort + '_' + depth).hide();
								//$('sTitle_' + sort + '_' + depth).show();

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
								o.cate.insert((new Template(template.sList)).evaluate({
											company : v.company
										,	depth : depth + 1
										,	seq : e.seq
										,	name : v.name
										,	code : e.catcode
									}));
//alert( "seq: " + e.seq + " name: " + v.name + " code: " + e.catcode + " depth: " +  (parseInt(depth) + 1))
								_select(e.seq, v.name, e.catcode, depth + 1 );
								_cate(v.company, e.seq, depth + 1 );
						}
					} else {
						window.alert(decodeURIComponentEx(e.message));
					}
				} catch (e) {
					window.alert('예기치 않은 오류가 발생하였습니다.');
				}

				$('window_sbmsg').hide();
			}.bind(this)
		});
	}

	function _modify(sort, depth) {
		var o = {
				company : $('company')
			,	cate : $('cate' + depth)
			,	seq : $('seq_' + sort + '_' + depth)
			,	name : $('name_' + sort + '_' + depth + (sort > 0 ? '_m' : ''))
			,	code : $('code_' + sort + '_' + depth + (sort > 0 ? '_m' : ''))
		};
		
		if ($('window_sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		} else if (o.name.disabled) {
			window.alert('권한이 없습니다.');
			return;
		} else if (o.seq.value == '') {
			window.alert('수정할 그룹을 선택하세요.');
			return;
		} else if (o.name.value == '') {
			window.alert('수정할 그룹명을 입력하세요.');
			o.name.focus();
			return;
		} else if (o.name.value.indexOf('/') >= 0) {
			window.alert('그룹명에는 슬래쉬(/)를 사용하실 수 없습니다.');
			o.name.focus();
			return;
		} else if (!confirm('수정하시겠습니까?')) {
			return;
		}

		$('window_sbmsg').show();

		new Ajax.Request('ServiceCategoryModify.jsp', {
			asynchronous : true,
			parameters : {seq : o.seq.value, sort : sort, name : encodeURIComponentEx(o.name.value) , code : encodeURIComponentEx(o.code.value)},
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

								o.cate.getElementsBySelector('li').findAll(function (s) {
									if (s.getAttribute('n') == o.seq.value) {
										s.update((new Template('<a onclick="_select(<%='#'%>{seq}, \'<%='#'%>{name}\', \'<%='#'%>{code}\', <%='#'%>{depth}); _cate(<%='#'%>{company}, <%='#'%>{seq}, <%='#'%>{depth});"><%='#'%>{name}</a>')).evaluate({
											seq : o.seq.value
										,	name : o.name.value
										,	code : o.code.value
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

				$('window_sbmsg').hide();
			}.bind(this)
		});
	}

	function _delete(sort, depth) {
		var o = {
			cateList : $('cateList')
		,	cate : $('cate' + depth)
		,	seq : $('seq_' + sort + '_' + depth)
		,	name : $('name_' + sort + '_' + depth + (sort > 0 ? '_m' : ''))
		,	code : $('code_' + sort + '_' + depth + (sort > 0 ? '_m' : ''))
		};

		if ($('window_sbmsg').visible()) {
			window.alert('전송중입니다, 잠시만 기다려 주세요.');
			return;
		} else if (o.name.disabled) {
			window.alert('권한이 없습니다.');
			return;
		} else if (o.seq.value == '') {
			window.alert('삭제할 그룹을 선택하세요.');
			return;
		} else if (!confirm('삭제하시겠습니까?')) {
			return;
		}

		$('window_sbmsg').show();
		alert( o.seq.value);
		new Ajax.Request('ServiceCategoryDelete.jsp', {
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
								//$('sTitle_' + sort + '_' + depth).show();

								if (ableMode.I == 'Y') {
									$('regist_' + sort + '_' + depth).show();
								} else {
									$('regist_' + sort + '_' + depth).hide();
								}

								break;
							default :
								o.cate.getElementsBySelector('li').findAll(function (s) {
									if (s.getAttribute('n') == o.seq.value) {
										o.seq.value = '';
										o.name.value = '';
										o.code.value = '';

										o.cateList.getElementsBySelector('table').findAll(function (s) {
											if (parseInt(s.id.substr(8, s.id.length)) > depth) {
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

				$('window_sbmsg').hide();
			}.bind(this)
		});
	}

	function _parent(sort, depth) {
		var o = {
			seq : $('seq_' + sort + '_' + depth)
		,	name : $('name_' + sort + '_' + depth + (sort > 0 ? '_m' : ''))
		};

		if (depth == 0) {
			window.alert('변경이 불가능한 그룹입니다.');
			return;
		} else if (o.name.disabled) {
			window.alert('권한이 없습니다.');
			return;
		} else if (o.seq.value == '') {
			window.alert('변경할 그룹을 선택하세요.');
			return;
		}

		new IFrame(510, 155, 'ServiceCategoryModifyParent.jsp?seq=' + o.seq.value).open();
	}

//	Event.observe(window, 'load', function (event) {
<%


	if (cfg.getLong("user.company") > 0) {
		out.println("$('window_sbmsg').show();");
		out.println("_cate(" + cfg.getLong("user.company") + ", 0, 0, true);");
		if (objCate.cate != null) {
			if (objCate.cate.size() > 0) {
				for (int i = 0; i < objCate.cate.size(); i++) {
					GeneralConfig c = (GeneralConfig) objCate.cate.get(i);
	
					out.println("_cate(" + cfg.getLong("user.company") + ", " + c.getLong("SEQ") + ", " + (c.getInt("DEPTH") + 1) + ", true);");
					out.println("_select(" + c.getLong("SEQ") + ", '" + c.getString("NAME").replaceAll("'", "") + "', "  + ", '" + c.getString("CODE").replaceAll("'", "") + "', " + (c.getInt("DEPTH") + 1) + ", true);");
				}
			}
		}

		out.println("$('window_sbmsg').hide();");
	}
%>

//	}.bind(this));
</script>