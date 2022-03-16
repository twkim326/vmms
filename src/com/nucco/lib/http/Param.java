package com.nucco.lib.http;

/**
 * Param.java
 *
 * 파라미터 관리
 *
 * 작성일 - 2011/02/14, 정원광
 *
 */

public class Param {
/**
 * 파라미터 더하기
 *
 * @param request javax.servlet.http.HttpServletRequest
 * @param param 더할 파라미터 명 (:을 구분자로 입력)
 * @return string
 *
 */
	public static String addParam(javax.servlet.http.HttpServletRequest request, String param) {
		String returnValue = "";
		String queryString = request.getQueryString();

		if (queryString != null) {
			String[] val1 = queryString.split("&");

			for (int i = 0; i < val1.length; i++) {
				String[] val2 = val1[i].split("=");

				try {
					if (val2.length == 2) {
						if ((":" + param + ":").indexOf(val2[0]) >= 0 && !(val2[1] == null || val2[1].equals(""))) {
							returnValue += "&" + val2[0] + "=" + java.net.URLEncoder.encode(java.net.URLDecoder.decode(val2[1], com.nucco.Common.CHARSET), com.nucco.Common.CHARSET);
						}
					}
				} catch (Exception e) {
				}
			}
		}

		return returnValue;
	}
}
