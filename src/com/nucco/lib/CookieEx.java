package com.nucco.lib;

/**
 * CookieEx.java
 *
 * cookie library
 *
 * 작성일 - 2008/04/25, 정원광
 *
 */

public class CookieEx {
/**
 * 읽기
 *
 * @param request javax.servlet.http.HttpServletRequest
 * @param name 쿠키명
 * @return string
 *
 */
	public static String get(javax.servlet.http.HttpServletRequest request, String name) {
		String returnValue = "";

		try {
			javax.servlet.http.Cookie[] cookie = request.getCookies();

			if (cookie != null) {
				for (int i = 0; i < cookie.length; i++) {
					if (name.equals(cookie[i].getName())) {
						returnValue = java.net.URLDecoder.decode(cookie[i].getValue(), com.nucco.Common.CHARSET);
						break;
					}
				}
			}
		} catch (Exception e) {
		}

		return returnValue;
	}
/**
 * 쓰기
 *
 * @param response javax.servlet.http.HttpServletResponse
 * @param name 쿠키명
 * @param value 쿠키값
 *
 */
	public static void set(javax.servlet.http.HttpServletResponse response, String name, String value) {
		set(response, name, value, "/", -1, null);
	}
/**
 * 쓰기
 *
 * @param response javax.servlet.http.HttpServletResponse
 * @param name 쿠키명
 * @param value 쿠키값
 * @param path 저장경로
 *
 */
	public static void set(javax.servlet.http.HttpServletResponse response, String name, String value, String path) {
		set(response, name, value, path, -1, null);
	}
/**
 * 쓰기
 *
 * @param response javax.servlet.http.HttpServletResponse
 * @param name 쿠키명
 * @param value 쿠키값
 * @param expire 저장시간
 *
 */
	public static void set(javax.servlet.http.HttpServletResponse response, String name, String value, int expire) {
		set(response, name, value, "/", expire, null);
	}
/**
 * 쓰기
 *
 * @param response javax.servlet.http.HttpServletResponse
 * @param name 쿠키명
 * @param value 쿠키값
 * @param path 저장경로
 * @param expire 저장시간
 *
 */
	public static void set(javax.servlet.http.HttpServletResponse response, String name, String value, String path, int expire) {
		set(response, name, value, path, expire, null);
	}
/**
 * 쓰기
 *
 * @param response javax.servlet.http.HttpServletResponse
 * @param name 쿠키명
 * @param value 쿠키값
 * @param expire 저장시간
 * @param domain 도메인
 *
 */
	public static void set(javax.servlet.http.HttpServletResponse response, String name, String value, int expire, String domain) {
		set(response, name, value, null, expire, domain);
	}
/**
 * 쓰기
 *
 * @param response javax.servlet.http.HttpServletResponse
 * @param name 쿠키명
 * @param value 쿠키값
 * @param path 저장경로
 * @param domain 도메인
 *
 */
	public static void set(javax.servlet.http.HttpServletResponse response, String name, String value, String path, String domain) {
		set(response, name, value, path, -1, domain);
	}
/**
 * 쓰기
 *
 * @param response javax.servlet.http.HttpServletResponse
 * @param name 쿠키명
 * @param value 쿠키값
 * @param path 저장경로
 * @param expire 저장시간
 * @param domain 도메인
 *
 */
	public static void set(javax.servlet.http.HttpServletResponse response, String name, String value, String path, int expire, String domain) {
		try {
			javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie(name, java.net.URLEncoder.encode(value, com.nucco.Common.CHARSET));

			if (path != null && !path.equals("")) {
				cookie.setPath(path);
			}

			cookie.setMaxAge(expire);

			if (domain != null && !domain.equals("")) {
				cookie.setDomain(domain);
			}

			response.addCookie(cookie);
		} catch (Exception e) {
		}
	}
}