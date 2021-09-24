package com.nucco.lib;

/**
 * Pager.java
 *
 * page library
 *
 * 작성일 - 2008/04/25, 정원광
 *
 */

public class Pager {
/**
 * 파라미터 구하기
 *
 * @param old 기존 파라미터
 * @param add 더할 파라미터
 * @return string
 *
 */
	public static String getParam(String old, String add) {
		String returnValue = old;

		if (returnValue == null || returnValue.equals("")) {
			returnValue = "?";
		} else {
			returnValue += "&";
		}

		return returnValue + add;
	}
/**
 * 페이지 사이즈
 *
 * @param total 총 레코드수
 * @param limit 목록당 출력수
 * @return long
 *
 */
	public static long getSize(long total, int limit) {
		return (long) Math.ceil((double) total / (double) limit);
	}
/**
 * 페이지 목록
 *
 * @param request javax.servlet.http.HttpServletRequest
 * @param page 현재 페이지
 * @param limit 목록당 출력수
 * @param total 총 페이지수
 * @return string
 *
 */
	public static String getList(javax.servlet.http.HttpServletRequest request, long page, int limit, long total) {
		String[] buttons = {"[처음]", "[이전]", "[다음]", "[마지막]"};

		return getList(request, page, limit, total, "", "p", "", buttons);
	}
/**
 * 페이지 목록
 *
 * @param page 현재 페이지
 * @param limit 목록당 출력수
 * @param total 총 페이지수
 * @param urlPrefix URL prefix
 * @param urlSuffix URL suffix
 * @param border 페이지와 페이지 사이에 출력될 문자열
 * @param buttons 버튼
 * @return string
 *
 */
	public static String getList(long page, int limit, long total, String urlPrefix, String urlSuffix, String border, String[] buttons) {
		String returnValue = "";
		long start = (long) ((page - 1) / limit) * limit;
		long move = 0;
		long n = 1;

		if (page > 1) {
			returnValue += "<a href=\"" + urlPrefix + "1" + urlSuffix + "\" class=\"button\">" + buttons[0] + "</a> ";
		} else {
			returnValue += buttons[0] + " ";
		}

		if (page > limit) {
			returnValue += "<a href=\"" + urlPrefix + start + urlSuffix + "\" class=\"button\">" + buttons[1] + "</a> ";
		} else {
			returnValue += buttons[1] + " ";
		}

		while (n + start <= total && n <= limit) {
			move = n + start;

			if (page == move) {
				returnValue += (n == 1 ? "" : border) + " <a class=\"s\">" + move + "</a> ";
			} else {
				returnValue += (n == 1 ? "" : border) + " <a class=\"n\" href=\"" + urlPrefix + move + urlSuffix + "\">" + move + "</a> ";
			}

			n++;
		}

		if (total > move) {
			returnValue += " <a href=\"" + urlPrefix + (move + 1) + urlSuffix + "\" class=\"button\">" + buttons[2] + "</a>";
		} else {
			returnValue += " " + buttons[2];
		}

		if (total > 1 && page < total) {
			returnValue += " <a href=\"" + urlPrefix + total + urlSuffix + "\" class=\"button\">" + buttons[3] + "</a>";
		} else {
			returnValue += " " + buttons[3];
		}

		return returnValue;
	}

	/**
	 * 페이지 목록
	 *
	 * @param request javax.servlet.http.HttpServletRequest
	 * @param page 현재 페이지
	 * @param limit 목록당 출력수
	 * @param total 총 페이지수
	 * @param linkBase 스크립트
	 * @param param 페이지 파라미터
	 * @param border 페이지와 페이지 사이에 출력될 문자열
	 * @param buttons 버튼
	 * @return string
	 *
	 */
		public static String getList(javax.servlet.http.HttpServletRequest request, long page, int limit, long total, String linkBase, String param, String border, String[] buttons) {
			String returnValue = "";
			String linkRequest = "";
			String queryString = request.getQueryString();
			long start = (long) ((page - 1) / limit) * limit;
			long move = 0;
			long n = 1;

			if (queryString != null) {
				String[] val1 = queryString.split("&");

				for (int i = 0; i < val1.length; i++) {
					String[] val2 = val1[i].split("=");

					try {
						if (val2.length == 2) {
							if (!val2[0].equals(param) && !(val2[1] == null || val2[1].equals(""))) {
								linkRequest = getParam(linkRequest, val2[0] + "=" + java.net.URLEncoder.encode(java.net.URLDecoder.decode(val2[1], com.nucco.Common.CHARSET), com.nucco.Common.CHARSET));
							}
						}
					} catch (Exception e) {
					}
				}
			}

			if (page > 1) {
				returnValue += "<a href=\"" + linkBase + getParam(linkRequest, param + "=1") + "\" class=\"button\">" + buttons[0] + "</a> ";
			} else {
				returnValue += buttons[0] + " ";
			}

			if (page > limit) {
				returnValue += "<a href=\"" + linkBase + getParam(linkRequest, param + "=" + start) + "\" class=\"button\">" + buttons[1] + "</a> ";
			} else {
				returnValue += buttons[1] + " ";
			}

			while (n + start <= total && n <= limit) {
				move = n + start;

				if (page == move) {
					returnValue += (n == 1 ? "" : border) + " <a class=\"s\">" + move + "</a> ";
				} else {
					returnValue += (n == 1 ? "" : border) + " <a class=\"n\" href=\"" + linkBase + getParam(linkRequest, param + "=" + move) + "\">" + move + "</a> ";
				}

				n++;
			}

			if (total > move) {
				returnValue += " <a href=\"" + linkBase + getParam(linkRequest, param + "=" + (move + 1)) + "\" class=\"button\">" + buttons[2] + "</a>";
			} else {
				returnValue += " " + buttons[2];
			}

			if (total > 1 && page < total) {
				returnValue += " <a href=\"" + linkBase + getParam(linkRequest, param + "=" + total) + "\" class=\"button\">" + buttons[3] + "</a>";
			} else {
				returnValue += " " + buttons[3];
			}

			return returnValue;
		}
}