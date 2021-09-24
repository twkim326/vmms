<%@ page import="
			  com.nucco.*
			, com.nucco.cfg.*
			, com.nucco.lib.*
			, com.nucco.lib.http.Param
			, com.nucco.beans.VM
		"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"
%><%
    /**
     * /vm/VMAccessSecurity.jsp
     *
     * 자판기 운영정보 > 출입단말상태조회
     *
     * 작성일 - 2020/12/23 by Chae
     *
     */

// 헤더
    request.setCharacterEncoding("ISO-8859-1");
    response.setHeader("content-type", "text/html; charset=utf-8");
    response.setHeader("cache-control", "no-cache");

// 설정
    GlobalConfig cfg = new GlobalConfig(request, response, session, "0307");

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

// 전송된 데이터
    long company = StringEx.str2long(request.getParameter("company"), 0);
    long organ = StringEx.str2long(request.getParameter("organ"), 0);
    int pageNo = StringEx.str2int(request.getParameter("page"), 1);
    String sField = StringEx.getKeyword(StringEx.charset(request.getParameter("sField")));
    String sQuery = StringEx.getKeyword(StringEx.charset(request.getParameter("sQuery")));
    // URL에 더해질 파라미터
    String addParam = Param.addParam(request, "page:company:organ:sField:sQuery");

// 인스턴스 생성
    VM objVM = new VM(cfg);

// 메서드 호출
    String error = null;

    try {
        error = objVM.getAccessList(company, organ, pageNo,sField, sQuery);
    } catch (Exception e) {
        error = e.getMessage();
    }

// 에러 처리
    if (error != null) {
        out.print(error);
        return;
    }

// 페이지 버튼
    String[] buttons = {"<img src='" + cfg.get("imgDir") +"/web/btn_first.gif' alt='처음' />",
            "<img src='" + cfg.get("imgDir") +"/web/btn_pre.gif' alt='이전 " + cfg.getInt("limit.page") + "개' />",
            "<img src='" + cfg.get("imgDir") +"/web/btn_next.gif' alt='다음 " + cfg.getInt("limit.page") + "개' />",
            "<img src='" + cfg.get("imgDir") +"/web/btn_last.gif' alt='마지막' />"};
%>
<%@ include file="../../header.inc.jsp" %>

<div class="title">
    <span>출입단말상태 조회</span>
</div>
<table cellspacing="0" class="tableType01">
    <thead>
    <tr>
        <th rowspan="2" scope="colgroup">전체 수</th>
        <th colspan="3" scope="colgroup">단말기 상태</th>
        <th colspan="2" scope="colgroup" onmouseover="$('noUseDesc').show()" onmouseout="$('noUseDesc').hide()">비가동 상태
            <span id="noUseDesc" class="description" style="display:none; z-index:999;">
				<span> 운영자판기 관리에서 폐점 혹은 휴점으로 등록 및 수정된 데이터 건수 </span>
            </span>
        </th>
    </tr>
    <tr>
        <th scope="col">정상</th>
        <th scope="col">비정상</th>
        <th scope="col">가동율</th>
        <th scope="col">폐점</th>
        <th scope="col">휴점</th>
    </tr>
    </thead>
    <tbody>
    <!-- 반복 예정 구간 -->
    <%if(objVM.data.getInt("ALL_COUNT")==0){ %>
    <tr>
        <td align="center" colspan="6"> 출입보안 단말기 없음. </td>
    </tr>
    <%} else {%>
    <tr>
        <td align="center"><%=Html.getText(objVM.data.getString("ALL_COUNT"))%></td>
        <td align="center"><%=Html.getText(objVM.data.getString("NORMAL_COUNT"))%></td>
        <td align="center"><%=Html.getText(objVM.data.getString("ABNORMAL_COUNT"))%></td>
        <td align="center"><%=(objVM.data.getInt("NORMAL_COUNT")*100 / objVM.data.getInt("ALL_COUNT"))%>%</td>
        <td align="center"><%=Html.getText(objVM.data.getString("CLOSED_COUNT"))%></td>
        <td align="center"><%=Html.getText(objVM.data.getString("REST_COUNT"))%></td>
    </tr>
    <%}%>
    <!-- 반복 예정 구간 -->
    </tbody>
</table>
<br/>
<table cellspacing="0" class="tableType02">
    <colgroup>
        <col width="110" />
        <col width="*"/>
        <col width="73" />
    </colgroup>
    <tr>
        <th><span>조회 조건</span></th>
        <td>
            <select id="sField">
                <option value="">- 검색필드</option>
                <option value="TVM.CODE"<%=sField.equals("TVM.CODE") ? " selected" : ""%>>위치 코드</option>
                <option value="TVM.PLACE"<%=sField.equals("TVM.PLACE") ? " selected" : ""%>>위치 명</option>
                <option value="TVM.ACCESS_STATUS"<%=sField.equals("TVM.ACCESS_STATUS") ? " selected" : ""%>>운영 상태</option>
            </select>
            <input type="text" id="sQuery" class="txtInput" value="<%=Html.getText(sQuery)%>" />
        </td>
        <td rowspan="3" class="center last">
            <input type="button" value="" class="btnSearch" onclick="_search($('search'));" />
        </td>
    </tr>
    <tr>
    <tr>
        <th><span>소속/조직</span></th>
        <td>
            <select id="company" class="<%=(cfg.getLong("user.company") > 0 ? "disabled" : "")%> js-example-basic-single js-example-responsive" style="width: 20%" onchange="Company.organ(this.options[selectedIndex].value.evalJSON(), 0, 0);"<%=(cfg.getLong("user.company") > 0 ? " disabled" : "")%>>
                <option value='{"seq" : 0, "depth" : 0}'>- 소속</option>
            </select>
            <span id="organ">
				<select id="organ0" class="js-example-basic-single js-example-responsive" style="width: 18%" onchange="Company.organ($('company').value.evalJSON(), 1, this.options[selectedIndex].value);">
					<option value="-1">- 조직</option>
				</select>
			</span>
        </td>
    </tr>
</table>

<!-- 검색결과 -->
<div class="infoBar mt23">
    검색 결과 : <strong><%=StringEx.comma(objVM.records)%></strong>건
    <input type="button" class="button2 excel_save_modify2" onclick="_excel();" value="엑셀저장" />
</div>
<table cellpadding="0" cellspacing="0" class="tableType01">
    <thead>
    <tr aria-rowspan="12">
        <th colspan="4" nowrap>위치 정보</th>
        <th colspan="4" nowrap>단말기 정보</th>
        <th colspan="4" nowrap>상태 정보</th>
    </tr>
    <tr>
        <th nowrap>순번</th>
        <th nowrap>위치 코드</th>
        <th nowrap>위치 명</th>
        <th nowrap>등록 일자</th>
        <th nowrap>운영 상태</th>
        <th nowrap>단말기 기종</th>
        <th nowrap>단말기 ID</th>
        <th nowrap>모뎀번호</th>
        <th nowrap onmouseover="$('TermStatDesc').show()" onmouseout="$('TermStatDesc').hide()">
            <span id="TermStatDesc" class="description" style="display:none; z-index:999;">
				<span> 단말기 상태 정상 기준: 수집시간 2시간 미만.</span>
            </span>
            단말기 상태
        </th>
        <th nowrap>최종 수집시간</th>
        <th nowrap>최종 거래시간</th>
    </tr>
    </thead>
    <tbody>
    <!-- 반복 예정 구간. -->
    <% for (int i = 0; i < objVM.list.size(); i++) { GeneralConfig c = (GeneralConfig) objVM.list.get(i); %>
        <tr>
            <td align="center"><%=c.getLong("SEQ")%></td>
            <td align="center"><%=c.getString("CODE")%></td>
            <td align="center"><%=c.getString("PLACE")%></td>
            <td align="center"><%=Html.getText(c.get("REGIST_DATE"))%></td>
            <td align="center"><%=c.getString("CONTROL_ERROR")%></td>
            <td align="center"><%=c.getString("MODEL")%></td>
            <td align="center"><%=c.getString("TERMINAL_ID")%></td>
            <td align="center"><%=c.getString("MODEM_NBR")%></td>
            <td align="center"><%=c.getString("TERM_STATUS")%></td>
            <td align="center"><%=Html.getText(c.get("CREATE_DATE"))%></td>
            <td align="center"><%=Html.getText(c.get("FINAL_TRAN_DATE"))%></td>
        </tr>
    <% } %>
    <% if(objVM.list.size()<=0){%>
        <tr>
            <td align="center" colspan="11">조회 데이터 없음.</td>
        </tr>
    <%}%>
    <!-- 반복 예정 구간. -->
    </tbody>
</table>
<!-- # 검색결과 -->

<!-- 페이징 -->
<div class="paging"><%=Pager.getList(request, pageNo, cfg.getInt("limit.page"), objVM.pages, "", "page", "", buttons)%></div>
<!-- # 페이징 -->

<%@ include file="../../footer.inc.jsp" %>
<style type="text/css">
    span.description {position:relative;}
    span.description span {position:absolute; left:-242px; top:100%; display:block; width:400px; padding:10px; text-align:left; border-color:#397cc8; border-width:1px; border-style:solid; background-color:#f0f7ff;}
</style>
<div id="sbmsg" style="display:none;"><img src="<%=cfg.get("topDir")%>/common/module/layer/images/loading.gif" alt="" /></div>
<form method="get" name="search" id="search">
    <input type="hidden" name="company" value="" />
    <input type="hidden" name="organ" value="" />
    <input type="hidden" name="sField" value="" />
    <input type="hidden" name="sQuery" value="" />
    <input type="hidden" name="sDate" value="" />
    <input type="hidden" name="eDate" value="" />
</form>
<script type="text/javascript" src="<%=cfg.get("topDir")%>/common/module/layer/js/iframe.js"></script>
<script type="text/javascript">
    function _search(o) {
        var obj = {company : $('company'), organ : null, sField : $('sField'), sQuery : $('sQuery'), };
        var com = obj.company.options[obj.company.selectedIndex].value.evalJSON();


        for (var i = com.depth; i >= 0; i--) {
            obj.organ = $('organ' + i);

            if (obj.organ && obj.organ.selectedIndex > 0) {
                o.organ.value = obj.organ.options[obj.organ.selectedIndex].value;
                break;
            }
        }

        if (obj.sQuery.value != '' && obj.sField.selectedIndex == 0) {
            window.alert('검색 필드를 선택하세요.');
            return;
        } else if (obj.sQuery.value == '' && obj.sField.selectedIndex > 0) {
            window.alert('검색어를 입력하세요.');
            return;
        }

        o.company.value = com.seq;
        o.sField.value = (obj.sQuery.value == '' ? '' : obj.sField.options[obj.sField.selectedIndex].value);
        o.sQuery.value = obj.sQuery.value;
        o.submit();
    }

    function _excel() {
        var records = <%=(objVM.list == null ? 0 : objVM.list.size())%>;

        if (records > 65000) {
            window.alert('65,000 라인을 초과하는 데이터입니다.\n\n보다 상세한 검색 조건으로 검색 후 다운로드하세요.');
            return;
        }

        location.href = 'VMAccessExcel.jsp?1=1<%=addParam%>';
    }

    //	Event.observe(window, 'load', function (event) {
    Company.depth = <%=cfg.getInt("user.organ.depth")%>;
    Company.company(<%=(cfg.getLong("user.company") > 0 ? cfg.getLong("user.company") : company)%>, <%=(organ > 0 ? organ : cfg.getLong("user.organ"))%>);
    //	}.bind(this));
</script>