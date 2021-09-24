package com.nucco.lib;

/**
 * ImageEx.java
 *
 * image library
 *
 * 작성일 - 2008/04/25, 정원광
 *
 */

public class ImageEx {
/**
 * 가로/세로 길이 구하기
 *
 * @param source 경로
 * @return int[]
 *
 */
	public static int[] orgsize(String source) {
		int[] size = {0, 0};

		try {
			java.awt.Image image = new javax.swing.ImageIcon(source).getImage();

			size[0] = image.getWidth(null);
			size[1] = image.getHeight(null);
		} catch (Exception e) {
		}

		return size;
	}
/**
 * 사이즈 보정
 *
 * @param imgW 가로
 * @param imgH 세로
 * @param maxW 가로 최대
 * @param maxH 세로 최대
 * @return int[]
 *
 */
	public static int[] adjsize(int imgW, int imgH, int maxW, int maxH) {
		int resW = imgW;
		int resH = imgH;
		double ratW;
		double ratH;
		int[] returnValue = {0, 0};

		if (resW == 0 || resH == 0) {
			resW = maxW;
			resH = maxH;
		} else if (maxW > 0 && maxH > 0) {
			if (resW > maxW || resH > maxH) {
				ratW = (double) maxW / resW;
				ratH = (double) maxH / resH;

				if (ratW < ratH) {
					resH = (int) Math.ceil((double) resH * ratW);
					resW = maxW;
				} else {
					resW = (int) Math.ceil((double) resW * ratH);
					resH = maxH;
				}
			}
		} else if (maxW > 0) {
			if (resW > maxW) {
				ratW = (double) maxW / resW;
				resW = maxW;
			} else {
				ratW = 1;
			}

			resH = (int) Math.ceil((double) resH * ratW);
		} else if (maxH > 0) {
			if (resH > maxH) {
				ratH = (double) maxH / resH;
				resH = maxH;
			} else {
				ratH = 1;
			}

			resW = (int) Math.ceil((double) resW * ratH);
		}

		returnValue[0] = resW;
		returnValue[1] = resH;

		return returnValue;
	}
/**
 * 썸네일 만들기
 *
 * @param source 원본 경로
 * @param target 저장 경로
 * @param width 기준 가로
 * @param height 기준 세로
 * @return boolean
 *
 */
	public static boolean createThumbnail(String source, String target, int width, int height) {
		if (width == 0 && height == 0) {
			return false;
		}

		try {
			// 이미지 읽기
//			java.awt.Image image = new javax.swing.ImageIcon((new java.io.File(source)).toURL()).getImage();
			java.awt.Image image = new javax.swing.ImageIcon((new java.io.File(source)).toURI().toURL()).getImage();

			// 원본 사이즈
			int[] orgs = {0, 0};
			orgs[0] = image.getWidth(null);
			orgs[1] = image.getHeight(null);

			// 보정 사이즈
			int[] adjs = adjsize(orgs[0], orgs[1], width, height);

			// 스케일 크기
			double[] scale = {0, 0};
			scale[0] = (double) adjs[0] / (double) orgs[0];
			scale[1] = (double) adjs[1] / (double) orgs[1];

			// 이미지 버퍼
			java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(adjs[0], adjs[1], java.awt.image.BufferedImage.TYPE_INT_RGB);

			// 스케일 지정
			java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();
			at.scale(scale[0], scale[1]);

			// 이미지 그리기
			java.awt.Graphics2D grps2D = bi.createGraphics();
			java.awt.RenderingHints rHints = new java.awt.RenderingHints(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			rHints.put(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
			grps2D.setRenderingHints(rHints);
			grps2D.drawImage(image, at, null);
			grps2D.dispose();

			// 썸네일 생성
			java.io.OutputStream os = new java.io.FileOutputStream(target);
			com.sun.image.codec.jpeg.JPEGImageEncoder encoder = com.sun.image.codec.jpeg.JPEGCodec.createJPEGEncoder(os);
			encoder.encode(bi);
			os.close();
		} catch (Exception e) {
			return false;
		}

		return true;
	}
}