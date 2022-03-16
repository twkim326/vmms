package com.nucco.lib;

/**
 * Html.java
 *
 * HTML library
 *
 * 작성일 - 2008/04/25, 정원광
 *
 */

public class Html {
/**
 * HTML을 TEXT 코드로 변경
 *
 * @param arg 문자열
 * @return string
 *
 */
	public static String getText(String arg) {
		if (arg == null) {
			return "";
		}

		return arg.replace("<", "&lt;").replace("\"", "&quot;");
	}
/**
 * 태그 제거
 *
 * @param arg 문자열
 * @return string
 *
 */
	public static String stripTag(String arg) {
		if (arg == null) {
			return "";
		}

		return arg.replaceAll("<(/)?([a-zA-Z]*)(\\s[a-zA-Z]*=[^>]*)?(\\s)*(/)?>", "");
	}
/**
 * 허용된 태그만 적용
 *
 * @param arg 문자열
 * @param html 허용할 태그 (ex, a|div|span)
 * @return string
 *
 */
	public static String applyTag(String arg, String html) {
		if (arg == null) {
			return "";
		}

		java.util.regex.Pattern p = java.util.regex.Pattern.compile("&lt;(/)?(" + html + ")", java.util.regex.Pattern.CASE_INSENSITIVE);
		java.util.regex.Matcher m = p.matcher(arg);

		return m.replaceAll("<$1$2");
	}
/**
 * 줄바꿈
 *
 * @param arg 문자열
 * @return string
 *
 */
	public static String nl2br(String arg) {
		if (arg == null) {
			return "";
		}

		return arg.replace("\r\n", "<br />\r\n");
	}
/**
 * 링크 태그 적용
 *
 * @param arg 문자열
 * @return string
 *
 */
	public static String autoLink(String arg) {
		if (arg == null) {
			return "";
		}

		return arg.replaceAll("([\\p{Alnum}]+)://([a-zA-Z0-9.\\-&/%=?:@#$(),.+;~\\_]+)", "<a href='$1://$2' target=_blank>$1://$2</a>");
	}
}