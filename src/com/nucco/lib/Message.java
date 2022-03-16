package com.nucco.lib;

/**
 * Message.java
 *
 * message library
 *
 * 작성일 - 2008/04/25, 정원광
 *
 */

public class Message {
/**
 * 알럿
 *
 * @param msg = 메시지
 * @return string
 *
 */
	public static String alert(String msg) {
		return alert(msg, -1, null, null);
	}
/**
 * 알럿
 *
 * @param msg 메시지
 * @param step 뒤로 갈 단계
 * @return string
 *
 */
	public static String alert(String msg, int step) {
		return alert(msg, step, null, null);
	}
/**
 * 알럿
 *
 * @param msg 메시지
 * @param step 뒤로 갈 단계
 * @param frame 프레임
 * @return string
 *
 */
	public static String alert(String msg, int step, String frame) {
		return alert(msg, step, frame, null);
	}
/**
 * 알럿
 *
 * @param msg 메시지
 * @param step 뒤로 갈 단계
 * @param frame 프레임
 * @param script 알럿 후 실행할 스크립트
 * @return string
 *
 */
	public static String alert(String msg, int step, String frame, String script) {
		String meta = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=" + com.nucco.Common.CHARSET + "\" />";
		String amsg = msg.replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\"");
		String afrm = frame == null ? "self" : frame;

		if (step >= 0) {
			return meta + "<script language=\"javascript\">" + afrm + ".window.alert(\"" + amsg + "\");" + script + "</script>";
		}

		return meta + "<script language=\"javascript\">" + afrm + ".window.alert(\"" + amsg + "\"); " + afrm + ".history.go(" + step + ");" + script + "</script>";
	}
/**
 * 새로 고침
 *
 * @return string
 *
 */
	public static String reload() {
		return reload(null);
	}
/**
 * 새로 고침
 *
 * @param frame 프레임
 * @return string
 *
 */
	public static String reload(String frame) {
		String returnValue = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=" + com.nucco.Common.CHARSET + "\" />";

		returnValue += "<script language=\"javascript\">";
		returnValue += (frame != null && !frame.equals("") ? frame : "self") + ".location.reload();";
		returnValue += "</script>";

		return returnValue;
	}
/**
 * 페이지 이동
 *
 * @param url 이동할 경로
 * @return string
 *
 */
	public static String refresh(String url) {
		return refresh(url, null, null, false);
	}
/**
 * 페이지 이동
 *
 * @param url 이동할 경로
 * @param msg 메시지
 * @return string
 *
 */
	public static String refresh(String url, String msg) {
		return refresh(url, msg, null, false);
	}
/**
 * 페이지 이동
 *
 * @param url 이동할 경로
 * @param msg 메시지
 * @param frame 프레임
 * @return string
 *
 */
	public static String refresh(String url, String msg, String frame) {
		return refresh(url, msg, frame, false);
	}
/**
 * 페이지 이동
 *
 * @param url 이동할 경로
 * @param msg 메시지
 * @param frame 프레임
 * @param isClose 창 종료 여부
 * @return string
 *
 */
	public static String refresh(String url, String msg, String frame, boolean isClose) {
		String returnValue = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=" + com.nucco.Common.CHARSET + "\" />";

		returnValue += "<script language=\"javascript\">";

		if (msg != null && !msg.equals("")) {
			returnValue += "window.alert(\"" + msg + "\");";
		}

		returnValue += (frame != null && !frame.equals("") ? frame : "self") + ".location.replace(\"" + url + "\");";

		if (isClose) {
			returnValue += "top.window.close();";
		}

		returnValue += "</script>";

		return returnValue;
	}
}