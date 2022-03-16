package com.nucco.lib.http;

/**
 * Referer.java
 *
 * 접근 경로 체크
 *
 * 작성일 - 2008/11/11, 정원광
 *
 */

public class Referer {
/**
 * 같은 사이트에서 접근했는지 체크
 *
 * @param request javax.servlet.http.HttpServletRequest
 * @return boolean
 *
 */
	public static boolean isApproachFromInside(javax.servlet.http.HttpServletRequest request) {
		String h = request.getHeader("host");
		String r = request.getHeader("referer");

		if (r == null) {
			return false;
		}

		// remove protocol
		r = r.substring(r.indexOf("://") + 3, r.length());

		// remove script path
		if (r.indexOf("/") >= 0) {
			r = r.substring(0, r.indexOf("/"));
		}

		return h.equals(r);
	}
/**
 * 같은 사이트에서 접근했는지 체크
 *
 * @param request javax.servlet.http.HttpServletRequest
 * @param method = 전송 방식
 * @return boolean
 *
 */
	public static boolean isApproachFromInside(javax.servlet.http.HttpServletRequest request, String method) {
		if (!request.getMethod().equals(method)) {
			return false;
		}

		return isApproachFromInside(request);
	}
/**
 * 같은 사이트에서 접근했는지 체크
 *
 * @param request javax.servlet.http.HttpServletRequest
 * @param method = 전송 방식
 * @param isUsedSubDomain = 서브도메인 사용여부
 * @return boolean
 *
 */
	public static boolean isApproachFromInside(javax.servlet.http.HttpServletRequest request, String method, boolean isUsedSubDomain) {
		if (isUsedSubDomain) {
			String h = request.getHeader("host");
			String r = request.getHeader("referer");
			String d = ".";

		// if referer is null or is a defferent method
			if (!request.getMethod().equals(method)) {
				return false;
			} else if (r == null) {
				return false;
			}

		// remove port
			if (h.indexOf(":") >= 0) {
				h = h.substring(0, h.indexOf(":"));
			}

			String[] c = h.split(".");
			String[] a =
				{
					".new.in|.com.cn"
				,	".co.kr|.or.kr|.go.kr|.pe.kr|.ne.kr|.re.kr|.ac.kr|.co.in|.co.jp|.or.jp"
				,	".mobi|.name|.info"
				,	".com|.net|.org|.biz|.new"
				,	".kr|.in|.tv|.cn|.ac|.tw|.eu|.cc|.jp"
				};

		// get key domain
			if (h.length() >= 7 && a[0].lastIndexOf(h.substring(h.length() - 7, h.length())) >= 0 && c.length >= 3) {
				d += c[c.length - 3] + h.substring(h.length() - 7, h.length());
			} else if (h.length() >= 6 && a[1].lastIndexOf(h.substring(h.length() - 6, h.length())) >= 0 && c.length >= 3) {
				d += c[c.length - 3] + h.substring(h.length() - 6, h.length());
			} else if (h.length() >= 5 && a[2].lastIndexOf(h.substring(h.length() - 5, h.length())) >= 0 && c.length >= 2) {
				d += c[c.length - 2] + h.substring(h.length() - 5, h.length());
			} else if (h.length() >= 4 && a[3].lastIndexOf(h.substring(h.length() - 4, h.length())) >= 0 && c.length >= 2) {
				d += c[c.length - 2] + h.substring(h.length() - 4, h.length());
			} else if (h.length() >= 3 && a[4].lastIndexOf(h.substring(h.length() - 3, h.length())) >= 0 && c.length >= 2) {
				d += c[c.length - 2] + h.substring(h.length() - 3, h.length());
			} else {
				d += h;
			}

		// remove protocol
			r = "." + r.substring(r.indexOf("://") + 3, r.length());

		// remove script path
			if (r.indexOf("/") >= 0) {
				r = r.substring(0, r.indexOf("/"));
			}

			return (r.indexOf(d) >= 0);
		}

		return isApproachFromInside(request, method);
	}
}