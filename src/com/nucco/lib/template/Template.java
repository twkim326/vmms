package com.nucco.lib.template;

/**
 * Template.java
 *
 * 템플릿
 *
 * 작성일 - 2009/02/17, 정원광
 *
 */

public class Template {
/**
 * 템플릿 내용
 *
 */
	protected String source = "";
/**
 * 템플릿 변수
 *
 */
	protected java.util.Hashtable<String, String> args = new java.util.Hashtable<String, String>();
/**
 * @param tpl 템플릿 경로
 *
 */
	public Template(String tpl) {
		this.source = this.read(tpl);
	}
/**
 * 템플릿 읽기
 *
 * @param tpl 템플릿 경로
 *
 */
	public String read(String tpl) {
		try {
			java.io.FileReader fr = new java.io.FileReader(tpl);
			StringBuffer sb = new StringBuffer("");

			int x = 0;

			while ((x = fr.read()) != -1) {
				 sb.append((char) x);
			}

			fr.close();

			return sb.toString();
		} catch (Exception e) {
			return "";
		}
	}
/**
 * 변수 할당
 *
 * @param name 변수 이름
 * @param value 변수 값
 *
 */
	public void setArgs(String name, String value) {
		this.args.put(name, value);
	}
/**
 * 변수 치환
 *
 * @return string
 *
 */
	public String parse() {
		String source = this.source;
		StringBuffer content = new StringBuffer();

		while (source.length() > 0) {
			int position = source.indexOf("{=");

			if (position == -1) {
				content.append(source);
				break;
			}

			if (position != 0) {
				content.append(source.substring(0, position));
			}

			if (source.length() == position + 2) {
				break;
			}

			String remainder = source.substring(position + 2);

			int markEndPos = remainder.indexOf("}");

			if ( markEndPos == -1) {
				break;
			}

			String name = remainder.substring(0, markEndPos).trim();
			String value = (String) this.args.get(name);

			if (value != null) {
				content.append(value);
			}

			if (remainder.length() == markEndPos + 1) {
				break;
			}

			source = remainder.substring(markEndPos + 1);
		}

		return content.toString();
	}
}