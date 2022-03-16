<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.beans.Product
			, java.net.*
		"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
%><%
/**
 * /service/ServiceProductTable.jsp
 *
 * 서비스 > 상품 > 테이블 등록
 *
 * 작성일 - 2021/01/04, scheo
 *
 */

// 헤더
	request.setCharacterEncoding("UTF-8");
	response.setHeader("content-type", "text/html; charset=utf-8");
	response.setHeader("cache-control", "no-cache");

// 설정
	GlobalConfig cfg = new GlobalConfig(request, response, session, "0205");

// 로그인을 하지 않았을 경우
	if (!cfg.isLogin()) {
		out.print("로그인이 필요합니다.");
		return;
	}

// 페이지 권한 체크
	if (!cfg.isAuth("I")) {
		out.print("접근이 불가능한 페이지입니다.");
		return;
	}

// 페이지 유형 설정
	cfg.put("window.mode", "B");

// 인스턴스 생성
	Product objProduct = new Product(cfg);

// 메서드 호출
	String error = null;

	try {
		error = objProduct.companyList();
	} catch (Exception e) {
		error = e.getMessage();
	}

%>
<%@ include file="../../header.inc.jsp" %>

<div class="title">
	<span>상품 관리</span>
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
				<select id="company" class="js-example-basic-single js-example-responsive" style="width: 30%"  class="<%=(cfg.getLong("user.company") > 0 ? "disabled" : "")%>" onchange="_table(this.options[selectedIndex].value)"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
					<option value="0">- 선택하세요</option>
			<% for (int i = 0; i < objProduct.company.size(); i++) { GeneralConfig c = (GeneralConfig) objProduct.company.get(i); %>
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
<table  cellspacing="0" class="tableType04" style="border-top-style:none;" id="organList<%='#'%>{depth}">
	<tr>
		<td>
			<div class="box">
				<ul id="table">
					<% if(cfg.getLong("user.company") != 0){%>
						<% for(int i = 0; i < objProduct.tableList.size(); i++) {GeneralConfig c = (GeneralConfig) objProduct.tableList.get(i);%>
							<li><a onclick="_select('<%=c.get("NAME")%>'); _tableDetail(<%=c.getLong("SEQ")%>);"><%=c.get("NAME")%></a><li>
						<% } %>
					<% } %>
				</ul>
			</div>
			<div class="src">
				<dl>
					<dt>현재 테이블</dt>
					<dd>
						<input type="text" id="nowTable" style="width: 150px;" /> 
						<input type="button" class="button1" value="수정" onclick="_tableModify($('nowTable').value, $('nowTable').className);"/>
					</dd>
				</dl>
				<dl>
					<dt>신규 테이블</dt>
					<dd>
						<input type="text" id="newTable" class="txtInput" style="width: 150px;" /> 
						<input type="button" class="button1" value="등록" onclick="_tableRegist($('newTable').value);" />
					</dd>
				</dl>
				<dl class="l">
					<dt>테이블 삭제</dt>
					<dd>
						<input type="button" class="button1" value="삭제" onclick="_tableDelete();" style="display:<%='#'%>{delete1};" /> 
					</dd>
				</dl>
			</div>
		</td>
	</tr>
</table>
<div class="title mt18" style="position:relative;">
	<span>자판기 상품 등록</span>
</div>

<table cellspacing="0" class="tableType04">
	<tr>
		<td>
			<span>상품 검색</span>
			<select id="sField">
				<option value="A.NAME">상품이름</option>
				<option value="A.CODE">상품코드</option>
				<option value="A.MEMO">상품설명</option>
				<option value="A.BAR_CODE">바코드</option>
			</select>
			<input type="text" id="sQuery" class="txtInput" />
			<input type="button" value="검색" class="button1" onclick="_product();" />
 			<!-- <span style="position: absolute; right:0;">
				<input type="button" class="button1" value="등록" onclick="_productRegist();" />
				<input type="button" class="button1" value="수정" onclick="_productUpdate();" style="right:80px;" />
				<input type="button" class="button1" value="삭제" onclick="_productDelete();" style="right:150px; margin-right: 30px;" />
			</span>  -->
		</td>
	</tr>
	<tr>
		<td id="sc_flag" style="border-top:1px solid #ccc;">
			<div class="box2">
				<ul id="product">
					<li><a class="none">상품을 검색하세요</a></li>
				</ul>
			</div>
			<div class="arr"></div>
			<div class="box2" style="width:550px;">
				<table cellpadding="0" cellspacing="0" id="sArea" style="">
					<colgroup>
						<col width="50" />
						<col width="350"/>
						<col width="80"/>
						<col width="30" />
					</colgroup>
					<tbody id="sList">
					<tr>
						<th>칼럼번호</th>
						<th>상품</th>
						<th>가격</th>
						<th>삭제</th>
					</tr>
					</tbody>
					<tbody id= "tableDetail">
					</tbody>
<%--  			<% for (int i = 0; i < objVM.product.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.product.get(i); %>
					<input type="hidden" name="seq<%=(i + 1)%>" id="seq<%=(i + 1)%>" value="<%=c.getLong("PRODUCT_SEQ")%>" />
					<tr>
						<td><input type="text" name="col<%=(i + 1)%>" id="col<%=(i + 1)%>" value="<%=c.getInt("COL_NO")%>" maxlength="4" class="input col" /></td>
						<td><input type="text" name="name<%=(i + 1)%>" id="name<%=(i + 1)%>" value="<%=Html.getText(c.get("NAME"))%>" class="input name" readonly /></td>
						<td nowrap><input type="text" name="price<%=(i + 1)%>" id="price<%=(i + 1)%>" value="<%=c.getInt("PRICE")%>" class="input price" readonly />원</td>
						<td><input type="checkbox" name="isDel<%=(i + 1)%>" id="isDel<%=(i + 1)%>" value="Y" class="checkbox" onclick="_sDel(<%=(i + 1)%>)" /></td>
					</tr>
			<% } %> --%>
					
				</table>
				<ul id="sNone" style="display:none;")%>">
					<li><a class="none">상품을 선택하세요</a></li>
				</ul>
			</div>
		</td>
	</tr>
</table>
<div class="buttonArea">
	<input type="button" value="" class="btnRegi" onclick="_ptDetail()"/> 
	<input type="button" value="" class="btnList" onclick="new parent.IFrame().close();" />
</div>

<%@ include file="../../footer.inc.jsp" %>

<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/layer/js/iframe.js"></script>
<style type="text/css">
	div.box {width:250px; height:90px; margin-left:12px; overflow:auto; float:left; border-width:1px; border-style:solid; border-color:#ccc;}
	div.box ul li a {margin:0; padding:3px 5px; display:block; cursor:pointer; font-size:12px;}
	div.box ul li a:hover {background-color:#f6f6f6;}
	div.box ul li.s a {background-color:#f6f6f6; font-weight:bold;}
	div.box2 {width:300px; height:200px; padding:5px; overflow:auto; float:left; border-width:1px; border-style:solid; border-color:#ccc;}
	div.box2 ul li a {height:12px; line-height:14px; display:block; margin:0; padding:3px; border:1px solid #fff; cursor:pointer; overflow:hidden;}
	div.box2 ul li a:hover {border:1px solid #ff4500;}
	div.box2 ul li a.none {border-style:none; cursor:default; padding:80px 0 0 0; text-align:center;}
	div.box2 table th {font-weight:bold; font-size:11px; text-align:center;}
	div.box2 table td {height:18px; padding:5px 0 0 0; text-align:center;}
	div.box2 table td input.input {height:18px; line-height:150%; border:1px solid #ccc;}
	div.box2 table td input.col {width:35px; padding:0 4px; text-align:right;}
	div.box2 table td input.name {width:360px;padding:0 4px; }
	div.box2 table td input.price {width:55px;padding:0 4px; text-align:right; }
	div.arr { width:50px; height:210px; background:url(<%=cfg.get("imgDir")%>/web/icon_arr_right.gif) no-repeat center center; float:left; }
	div.src {width:630px; height:90px; margin-left:20px; float:left; border-width:1px; border-style:solid; border-color:#ccc;}
	div.src dl {height:30px; clear:both;}
	div.src dl dt {width:100px; height:23px; padding-top:6px; float:left; background-color:#e4e2e0; text-align:center; border-width:0 0 1px 0; border-style:solid; border-color:#fff; font-weight:bold;}
	div.src dl dd {width:525px; height:25px; padding:4px 0 0 5px; float:left; border-width:0 0 1px 0; border-style:solid; border-color:#ccc;}
	div.src dl dd a {height:18px; padding-top:4px; display:inline-block;}
	div.src dl dd input {vertical-align:middle;}
	div.src dl.l dt {height:24px; border-color:#ccc; }
	div.src dl.l dd {height:25px; border-width:0; }
</style>

<%@ include file="../../footer.inc.jsp" %>

<script type="text/javascript">
//map 생성
Map = function(){
	 this.map = new Object();
	};  
	Map.prototype = {  
	    put : function(key, value){  
	        this.map[key] = value;
	    },  
	    get : function(key){  
	        return this.map[key];
	    },
	    containsKey : function(key){   
	     return key in this.map;
	    },
	    containsValue : function(value){   
	     for(var prop in this.map){
	      if(this.map[prop] == value) return true;
	     }
	     return false;
	    },
	    isEmpty : function(key){   
	     return (this.size() == 0);
	    },
	    clear : function(){  
	     for(var prop in this.map){
	      delete this.map[prop];
	     }
	    },
	    remove : function(key){   
	     delete this.map[key];
	    },
	    keys : function(){  
	        var keys = new Array();  
	        for(var prop in this.map){  
	            keys.push(prop);
	        }  
	        return keys;
	    },
	    values : function(){  
	     var values = new Array();  
	        for(var prop in this.map){  
	         values.push(this.map[prop]);
	        }  
	        return values;
	    },
	    size : function(){
	      var count = 0;
	      for (var prop in this.map) {
	        count++;
	      }
	      return count;
	    },
	    toString : function(){
	      var s=[];
	      for(var prop in this.map){
	         s.push(prop+':'+this.map[prop]);
	      }
	      return s.join(',');
	    }
	};

var template = {list : '<li id="<%='#'%>{i}" class="<%='#'%>{i}"><a onclick="_tableSelect(\'<%='#'%>{name}\', <%='#'%>{seq}, <%='#'%>{i}); _tableDetail(<%='#'%>{seq});"><%='#'%>{name}</a></li>'
, DList : '<tr><td><input type="text" name="col<%='#'%>{i}" id="col<%='#'%>{i}" value="<%='#'%>{col_no}" maxlength="4" class="input col" /></td>'
		+ '<td><input type="text" name="name<%='#'%>{i}" id="name<%='#'%>{i}" value="<%='#'%>{name}" class="input name" readonly /></td>'
		+ '<td nowrap><input type="text" name="price<%='#'%>{i}" id="price<%='#'%>{i}" value="<%='#'%>{price}" class="input price" readonly />원</td>'
		+ '<td><input type="checkbox" name="isDel<%='#'%>{i}" id="isDel<%='#'%>{i}" value="Y" class="checkbox" onclick="_sDel(<%='#'%>{i})" /></td></tr>'
};

function _sDel(i) {
	var o = {col : $('col' + i), name : $('name' + i), isDel : $('isDel' + i)};

	if (o.isDel.checked) {
		o.col.value = o.col.defaultValue;
		window.alert(o.col.value);
		o.col.readOnly = true;
		o.col.addClassName('disabled');
		o.name.addClassName('disabled');
	} else {
		window.alert(o.col.value);
		o.col.readOnly = false;
		o.col.removeClassName('disabled');
		o.name.removeClassName('disabled');
	}
}

function _save() {
	if ($('sbmsg').visible()) {
		window.alert('전송중입니다, 잠시만 기다려 주세요.');
		return;
	}
	
	var error = Common.checkForm($('regist'));
	var sgcnt = parseInt($('sgcnt').value);
	var check = new Array();
	var index = 0;

	if (error != '') {
		window.alert(error);
		return;
	}
	
//	if (!$('organ_').value || $('organ_').value == '0') {
//		window.alert('조직을 선택하세요.');
//		return;
//	}

	for (var i = 1; i <= sgcnt; i++) {
		var o = {col : $('col' + i), isDel : $('isDel' + i)};

		if (o.isDel.checked) {
			continue;
		}

		if (!/^[0-9]+$/.test(o.col.value)) {
			window.alert(i + '번째 순번을 숫자로 입력하세요.');
			o.col.focus();
			return;
		} else if (Common.inArray(o.col.value, check)) {
			window.alert(i + '번째 순번이 이미 존재합니다.');
			o.col.focus();
			return;
		}

		check[index++] = o.col.value;
	}
	var confirmMessage = "";
	confirmMessage = '수정하시겠습니까?';	
	
	if (!confirm(confirmMessage)) {
		return;
	}	

	$('sbmsg').show();
	$('save_').submit();
}

function _product() {
	var o = {company : $('company'), sField : $('sField'), sQuery : $('sQuery'), product : $('product'), sbmsg : $('sbmsg')};

	if (o.sbmsg.visible()) {
		window.alert('전송중입니다, 잠시만 기다려 주세요.');
		return;
	}

	if ($('company').selectedIndex == 0) {			
		window.alert('소속을 선택하세요.');
		$('company').focus();
		return;
	}
	
	o.product.update('');
	o.sbmsg.show();

	new Ajax.Request('ServiceProductTableProduct.jsp', {
		asynchronous : true,
		parameters : {
		        company : o.company.value
			,	sField : o.sField.options[o.sField.selectedIndex].value
			,	sQuery : encodeURIComponentEx(o.sQuery.value)
		},
		onSuccess : function (xmlHttp) {
			try {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == 'FAIL') {
					window.alert(e.message ? decodeURIComponentEx(e.message) : '상품 정보를 가져오는데 실패하였습니다.');
					o.sbmsg.hide();
					return;
				}

				e.data.each (function (data, i) {
					o.product.insert('<li><a onclick="_move(' + data.seq + ', \'' + decodeURIComponent(data.name) + '\', ' + data.price + ' );" title="' + decodeURIComponent(data.name) + ' | ' + data.price +  '원">' + data.code + ' | ' + decodeURIComponent(data.name) + '</a></li>');
				}.bind(this));

				if (e.data.length == 0) {
					o.product.update('<li><a class="none">등록된 상품이 없습니다</a></li>');
				}
			} catch (e) {
				window.alert('예기치 않은 오류가 발생하였습니다.');
			}

			o.sbmsg.hide();
		}.bind(this)
	});
}

function _move(seq, name, price) {
	var sgcnt = parseInt($('sgcnt').value) + 1;

	if (sgcnt > 99) {
		window.alert('운영 자판기 상품은 99개까지만 등록이 가능합니다.');
		return;
	}
	
	$('sgcnt').value = sgcnt;
	$('tableDetail').insert('<input type="hidden" name="seq' + sgcnt + '" id="seq' + sgcnt + '" value="' + seq + '" />'
		+ '<tr>'
		+ '<td><input type="text" name="col' + sgcnt + '" id="col' + sgcnt + '" maxlength="4" class="input col" /></td>'
		+ '<td><input type="text" name="name' + sgcnt + '" id="name' + sgcnt + '" value="' + name + '" class="input name" readonly /></td>'
		+ '<td><input type="text" name="price' + sgcnt + '" id="price' + sgcnt + '" value="' + price + '" class="input price" readonly />원</td>'
		+ '<td><input type="checkbox" name="isDel' + sgcnt + '" id="isDel' + sgcnt + '" value="Y" class="checkbox" onclick="_sDel(' + sgcnt + ')" /></td>'
		+ '</tr>');
}

function _ptDetail() {
	var ptDtJsonArray = new Array();
	
	var i=0;
	
	for(i = 1; i <= $('sgcnt').value; i++){
		var ptDtJson = new Object();
		ptDtJson.col = $('col'+i).value;
		ptDtJson.name = $('name'+i).value;
		ptDtJson.price = $('price'+i).value;
		ptDtJson.isDel = $('isDel'+i).value;
		ptDtJsonArray.push(ptDtJson);
		window.alert(i);
	}
	var json = JSON.stringify(ptDtJsonArray);
	window.alert(json);
	
	new Ajax.Request('ServiceProductTableDetailRegist.jsp', {
		asynchronous : true,
		parameters : {
		        jsonData : JSON.stringify(ptDtJsonArray)
		},
		onSuccess : function (xmlHttp) {
			try {
				window.alert("success!!");
/* 				var e = xmlHttp.responseText.evalJSON();
				var i = 0;
				o.template = new Template(template.list);
				e.data.each (function (data, i) {
					//o.table.update('<li><a>'+ i + data.name + '</a></li>');
					//o.table.insert('<li><a onclick="_select(\'' + data.name + '\')">'+ data.name + '</a></li>');
					o.table.insert(o.template.evaluate({
						name : data.name
						, seq : data.seq
						, i : i
					}));
				}.bind(this));

				if (e.data.length == 0) {
					o.table.update('<li><a class="none">등록된 테이블이 없습니다</a></li>');
				}
			*/	 
			} catch (e) {
				window.alert('예기치 않은 오류가 발생하였습니다.');
			}

			o.sbmsg.hide();
		}.bind(this)
	});

}

function _table(company) {
	var o = {table : $('table'), company : $('company')};
	if ($('company').selectedIndex == 0) {			
		window.alert('소속을 선택하세요.');
		$('company').focus();
		return;
	}
	
	o.table.update('');
	
	new Ajax.Request('ServiceProductTableList.jsp', {
		asynchronous : true,
		parameters : {
		        company : o.company.value
		},
		onSuccess : function (xmlHttp) {
			try {
				var e = xmlHttp.responseText.evalJSON();
				var i = 0;
				o.template = new Template(template.list);
				e.data.each (function (data, i) {
					//o.table.update('<li><a>'+ i + data.name + '</a></li>');
					//o.table.insert('<li><a onclick="_select(\'' + data.name + '\')">'+ data.name + '</a></li>');
					o.table.insert(o.template.evaluate({
						name : data.name
						, seq : data.seq
						, i : i
					}));
				}.bind(this));

				if (e.data.length == 0) {
					o.table.update('<li><a class="none">등록된 테이블이 없습니다</a></li>');
				}
				
			} catch (e) {
				window.alert('예기치 않은 오류가 발생하였습니다.');
			}

			o.sbmsg.hide();
		}.bind(this)
	});
}

function _tableDetail(tableSeq) {
	var o = {tableDetail : $('tableDetail'), scFlag : $('sc_flag')};
	
	if($('sgcnt')){
		$('sgcnt').remove();
	}
	
	o.tableDetail.update('');
	
	new Ajax.Request('ServiceProductTableDetailList.jsp', {
		asynchronous : true,
		parameters : {
		        seq : tableSeq
		},
		onSuccess : function (xmlHttp) {
			try {
				var e = xmlHttp.responseText.evalJSON();
				var i = 0;
				var j = 0;
				o.template = new Template(template.DList);
				e.data.each (function (data, i) {
					//o.table.update('<li><a>'+ i + data.name + '</a></li>');
					//o.table.insert('<li><a onclick="_select(\'' + data.name + '\')">'+ data.name + '</a></li>');
					o.tableDetail.insert(o.template.evaluate({
						name : data.name
						, col_no : data.col_no
						, price : data.price
						, i : i+1
					}));
					j++;
				}.bind(this));
				
				
				o.scFlag.insert('<input type="hidden" name="sgcnt" id="sgcnt" value="' + j +'" />');

				if (e.data.length == 0) {
					o.tableDetail.update('');
				}
				
			} catch (e) {
				window.alert('예기치 않은 오류가 발생하였습니다.');
			}

			o.sbmsg.hide();
		}.bind(this)
	});
}

function _tableSelect(name, seq, count) {
	var o = {
		name : $('nowTable')
	};
	
	o.name.className = seq + "_" + count;
	o.name.value = name;
	
}

function _tableRegist(tableName) {
	var o = {
			table : $('table'), company : $('company'), name : $('newTable')
	};
	if (tableName == '') {
		window.alert('등록할 테이블명을 입력하세요.');
		return;
	} else if (!confirm('등록하시겠습니까?')) {
		return;
	}
	
	new Ajax.Request('ServiceProductTableRegist.jsp', {
		asynchronous : true,
		parameters : {
			name : tableName
			, company : o.company.value
		},
		onSuccess : function (xmlHttp) {
			try {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == 'SUCCESS') {
					window.alert('등록이 완료되었습니다.');
					var select = null;
					
					o.name.value = '';
					o.table.insert((new Template(template.list)).evaluate({
						name : tableName
					}));
					_tableSelect(tableName, '', '');
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

function _tableDelete() {
	var seq = $('nowTable').className;
	var o = {
			table : $('table')
			, tableSeq : seq.split('_')[0]
			, id : seq.split('_')[1]
	};
	
 	if ($('nowTable').value == '' || o.tableSeq == '') {
		window.alert('삭제할 테이블을 선택하세요.');
		return;
	} else if (!confirm('삭제하시겠습니까?')) {
		return;
	}
	
 	new Ajax.Request('ServiceProductTableDelete.jsp', {
		asynchronous : true,
		parameters : {
			seq : o.tableSeq
		},
		onSuccess : function (xmlHttp) {
			try {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == 'SUCCESS') {
					window.alert('삭제가 완료되었습니다.');
					var select = null;
					
					$('nowTable').value = '';
					o.table.getElementsBySelector('li').findAll(function (s) {
						if (s.getAttribute('id') == o.id) {

							s.remove();
						}
					}.bind(this));

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

function _tableModify(tableName, seq) {
	var tableSeq = seq.split('_')[0];
	var index = seq.split('_')[1];
	var o = {
			boxSeq : $(index)
			, table : $('table')
	};
/*  	var o = {
			company : $('company')
		,	seq : $('seq_' + sort + '_' + depth)
		,	name : $('name_' + sort + '_' + depth + (sort > 0 ? '_m' : ''))
		}; */

	if (tableSeq == '') {
		window.alert('수정할 조직을 선택하세요.');
		return;
	} else if (tableName == '') {
		window.alert('수정할 조직명을 입력하세요.');
		o.name.focus();
		return;
	} else if (!confirm('수정하시겠습니까?')) {
		return;
	}

	new Ajax.Request('ServiceProductTableModify.jsp', {
		asynchronous : true,
		parameters : {seq : tableSeq, name : tableName},
		onSuccess : function (xmlHttp) {
			try {
				var e = xmlHttp.responseText.evalJSON();

				if (e.code == 'SUCCESS') {
					window.alert('수정이 완료되었습니다.');
					var select = null;
					
					o.table.getElementsBySelector('li').findAll(function (s) {
						if (s.getAttribute('id') == index) {
							s.update((new Template('<a onclick="_tableSelect(\'<%='#'%>{name}\', <%='#'%>{seq}, <%='#'%>{i}); _tableDetail(<%='#'%>{seq});"><%='#'%>{name}</a>')).evaluate({
								seq : tableSeq
							,	name : tableName
							,	i : index
							}));
						}
					}.bind(this));
				
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

</script>