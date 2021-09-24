package com.nucco.lib.security;

/**
 * Cryptograph.java
 *
 * 암호화/복호화 관련
 *
 * 작성일 - 2008/04/25, 정원광
 *
 */

public class Cryptograph {
/**
 * 암호화/복호화 키
 *
 */
	private String key = "HP6H545KN05N97IL3BL72ODP27RNATSCW9ZO34AZYNJ8HLNNOS3933X8NLGDF30CL";
/**
 *
 */
	public Cryptograph() {
		this(null);
	}
/**
 * @param key 암호화/복호화 키
 *
 */
	public Cryptograph(String key) {
		if (key != null) {
			this.key = key;
		}
	}
/**
 * 암호화
 *
 * @param arg 문자열
 * @return string
 *
 */
	public String encrypt(String arg) {
		String returnValue = "";

		try {
			String key = this.base64Encode(this.key);
			String enc = this.base64Encode(arg);

			for (int i = 1; i <= enc.length(); i++) {
				returnValue += enc.charAt(i - 1) + key.charAt((i % key.length() + 1) - 1) + "$#";
			}

			returnValue = this.swap(returnValue);
		} catch (Exception e) {
			returnValue = null;
		}

		return returnValue;
	}
/**
 * 복호화
 *
 * @param arg 문자열
 * @return string
 *
 */
	public String decrypt(String arg) {
		String returnValue = "";

		try {
			String key = this.base64Encode(this.key);
			String[] tmp = this.swap(arg).split("\\$#");

			for (int i = 1; i <= tmp.length; i++) {
				returnValue += (char)(Integer.parseInt(tmp[i - 1]) - key.charAt((i % key.length() + 1) - 1));
			}

			returnValue = this.base64Decode(returnValue);
		} catch (Exception e) {
			returnValue = null;
		}

		return returnValue;
	}
/**
 * 스와핑
 *
 * @param arg 문자열
 * @return string
 *
 */
	private String swap(String arg) {
		String returnValue = "";

		for (int i = 1; i <= arg.length(); i += 4) {
			if (i + 2 < arg.length()) {
				String[] tmp = new String[4];

				for (int j = 0; j <= 3; j++) {
					tmp[j] = arg.substring(i + j - 1, i + j);
				}

				if (arg.length() % 4 > 1) {
					returnValue += tmp[2] + tmp[3] + tmp[0] + tmp[1];
				} else {
					returnValue += tmp[3] + tmp[2] + tmp[1] + tmp[0];
				}
			} else {
				returnValue += arg.substring(i - 1, arg.length());
			}
		}

		return returnValue;
	}
/**
 * BASE64 인코딩
 *
 * @param arg 문자열
 * @return string
 *
 */
	private String base64Encode(String arg) {
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
 * BASE64 디코딩
 *
 * @param arg 문자열
 * @return string
 *
 */
	private String base64Decode(String arg) {
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