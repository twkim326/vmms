package com.nucco.lib.base64;

/**
 * Base64.java
 *
 * Base 64 인코딩/디코딩
 *
 * 작성일 - 2008/04/25, 정원광
 *
 */

public class Base64 {
/**
 * 인코딩
 *
 * @param arg 문자열
 * @return string
 *
 */
	public static String encode(String arg) {
		if (arg == null || arg.trim().equals("")) {
			return "";
		}

		try {
			return (new sun.misc.BASE64Encoder()).encode(arg.getBytes()).replaceAll("\t", "").replaceAll("\r", "").replaceAll("\n", "");
		} catch (Exception e) {
			return "";
		}
	}
/**
 * 디코딩
 *
 * @param arg 문자열
 * @return string
 *
 */
	public static String decode(String arg) {
		if (arg == null || arg.trim().equals("")) {
			return "";
		}

		try {
			return new String((new sun.misc.BASE64Decoder()).decodeBuffer(arg));
		} catch (Exception e) {
			return "";
		}
	}
}