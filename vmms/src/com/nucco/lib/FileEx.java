package com.nucco.lib;

/**
 * FileEx.java
 *
 * file library
 *
 * 작성일 - 2008/04/25, 정원광
 *
 */

public class FileEx {
/**
 * 파일 이름
 *
 * @param path 경로
 * @return string
 *
 */
	public static String name(String path) {
		if (path == null || path.equals("")) {
			return "";
		}

		path = path.replace('\\', '/');

		if (path.lastIndexOf('/') > -1) {
			path = path.substring(path.lastIndexOf('/') + 1, path.length());
		}

		if (path.lastIndexOf('.') == -1) {
			return path;
		}

		return path.substring(0, path.lastIndexOf('.'));
	}
/**
 * 파일 확장자
 *
 * @param path 경로
 * @return string
 *
 */
	public static String extension(String path) {
		if (path == null || path.equals("")) {
			return "";
		} else if (path.lastIndexOf('.') == -1) {
			return "";
		}

		return path.substring(path.lastIndexOf('.') + 1, path.length()).toLowerCase();
	}
/**
 * 디렉토리 내용
 *
 * @param path 경로
 * @return string[]
 *
 */
	public static String[] dirs(String path) {
		java.io.File file = new java.io.File(path);

		if (file.exists() && file.isDirectory()) {
			return file.list();
		} else {
			return new String[0];
		}
	}
/**
 * 파일/디렉토리 존재여부 체크
 *
 * @param path 경로
 * @return boolean
 *
 */
	public static boolean isExists(String path) {
		java.io.File file = new java.io.File(path);

		return file.exists();
	}
/**
 * 파일 복사
 *
 * @param from 복사대상
 * @param to 복사경로
 * @return boolean
 *
 */
	public static boolean copy(String from, String to) {
		try {
			java.io.FileInputStream fis = new java.io.FileInputStream(from);
			java.io.FileOutputStream fos = new java.io.FileOutputStream(to);

			byte[] bytes = new byte[4096];
			int x = 0;
//			int total = 0;

			while((x = fis.read(bytes)) != -1) {
				fos.write(bytes, 0, x);
			}

			fos.flush();
			fos.close();
			fis.close();
		} catch (Exception e) {
			return false;
		}

		return true;
	}
/**
 * 파일 이동
 *
 * @param from 이동대상
 * @param to 이동경로
 * @return boolean
 *
 */
	public static boolean move(String from, String to) {
		java.io.File oldFile = new java.io.File(from);
		java.io.File newFile = new java.io.File(to);

		return oldFile.renameTo(newFile);
	}
/**
 * 파일 이동
 *
 * @param oldFile 이동대상
 * @param to 이동경로
 * @return boolean
 *
 */
	public static boolean move(java.io.File oldFile, String to) {
		java.io.File newFile = new java.io.File(to);

		return oldFile.renameTo(newFile);
	}
/**
 * 파일/디렉토리 삭제
 *
 * @param path 경로
 * @return boolean
 *
 */
	public static boolean delete(String path) {
		return delete(path, false);
	}
/**
 * 파일/디렉토리 삭제
 *
 * @param path 경로
 * @param isAll = 하위 파일 제거 여부
 * @return boolean
 *
 */
	public static boolean delete(String path, boolean isAll) {
		java.io.File file = new java.io.File(path);

		if (!isAll) {
			return file.delete();
		}

		if (file.exists()) {
			if (file.isDirectory()) {
				String[] lists = file.list();

				for (int i = 0; i < lists.length; i++) {
					java.io.File del = new java.io.File(path, lists[i]);

					if (del.isDirectory()) {
						delete(del.getAbsolutePath(), true);
					} else {
						del.delete();
					}
				}
			}

			return file.delete();
		} else {
			return false;
		}
	}
/**
 * 디렉토리 생성
 *
 * @param path 경로
 *
 */
	public static void createDirectory(String path) {
		java.io.File file = new java.io.File(path);

		file.mkdirs();
	}
/**
 * 파일 읽기
 *
 * @param path 경로
 * @return string
 *
 */
	public static String read(String path) {
		try {
			java.io.FileReader fr = new java.io.FileReader(path);
			StringBuffer sb = new StringBuffer("");

			int x = 0;

			while ((x = fr.read()) != -1) {
				 sb.append((char) x);
			}

			fr.close();

			return sb.toString();
		} catch (Exception e) {
			return null;
		}
	}
/**
 * 파일 쓰기
 *
 * @param path 경로
 * @param body = 내용
 * @return boolean
 *
 */
	public static boolean write(String path, String body) {
//		java.io.File file = new java.io.File(path);

		try {
			java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(path), com.nucco.Common.CHARSET));

			bw.write(body, 0, body.length());
			bw.close();

			return true;
		} catch(Exception e) {
			return false;
		}
	}
/**
 * 파일 쓰기
 *
 * @param path 경로
 * @param body = 내용
 * @param isOver = 덮어쓰기여부
 * @return boolean
 *
 */
	public static boolean write(String path, String body, boolean isOver) {
		if (isExists(path)) {
			if (isOver) {
				return write(path, body);
			} else {
				return false;
			}
		} else {
			return write(path, body);
		}
	}
/**
 * 파일 쓰기
 *
 * @param response javax.servlet.http.HttpServletResponse
 * @param path 경로
 * @return boolean
 *
 */
	public static boolean write(javax.servlet.http.HttpServletResponse response, String path) {
		if (!isExists(path)) {
			return false;
		}

		boolean complete = true;

		try {
			java.io.FileInputStream fis = new java.io.FileInputStream(new java.io.File(path));
			java.io.BufferedInputStream bis = new java.io.BufferedInputStream(fis);
			java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(response.getOutputStream());

			int i = 0;
			byte[] b = new byte[1024];

			try {
				while((i = bis.read(b)) != -1) {
					bos.write(b, 0, i);
				}

				bos.flush();
			} catch (Exception e) {
				complete = false;
			} finally {
				if (bos != null) {
					bos.close();
					bos = null;
				}

				if (bis != null) {
					bis.close();
					bis = null;
				}

				if (fis != null) {
					fis.close();
					fis = null;
				}
			}
		} catch (Exception e) {
			complete = false;
		}

		return complete;
	}
/**
 * 파일 쓰기
 *
 * @param response javax.servlet.http.HttpServletResponse
 * @param file 파일
 * @return boolean
 *
 */
	public static boolean write(javax.servlet.http.HttpServletResponse response, java.io.File file) {
		if (!file.exists()) {
			return false;
		}

		boolean complete = true;

		try {
			java.io.FileInputStream fis = new java.io.FileInputStream(file);
			java.io.BufferedInputStream bis = new java.io.BufferedInputStream(fis);
			java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(response.getOutputStream());

			int i = 0;
			byte[] b = new byte[1024];

			try {
				while((i = bis.read(b)) != -1) {
					bos.write(b, 0, i);
				}

				bos.flush();
			} catch (Exception e) {
				complete = false;
			} finally {
				if (bos != null) {
					bos.close();
					bos = null;
				}

				if (bis != null) {
					bis.close();
					bis = null;
				}

				if (fis != null) {
					fis.close();
					fis = null;
				}
			}
		} catch (Exception e) {
			complete = false;
		}

		return complete;
	}
}