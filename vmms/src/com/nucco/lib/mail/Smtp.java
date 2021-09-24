package com.nucco.lib.mail;

public class Smtp {
	private String charset = "UTF-8";
	private String host = "localhost";
	private String port = "25";
	private javax.mail.Authenticator auth = null;
	private javax.mail.internet.MimeMessage msg = null;

	public Smtp(String host, String port, String user, String pass, String charset) {
		if (host != null && !host.equals("")) {
			this.host = host;
		}

		if (port != null && !port.equals("")) {
			this.port = port;
		}

		if (charset != null && !charset.equals("")) {
			this.charset = charset;
		}

		java.util.Properties p = new java.util.Properties();

		p.put("mail.smtp.host", this.host);
		p.put("mail.smtp.port", this.port);

		if ((user != null && !user.equals("")) && (pass != null && !pass.equals(""))) {
			p.put("mail.smtp.auth", "true");
		}

		if ((user != null && !user.equals("")) && (pass != null && !pass.equals(""))) {
			this.auth = new AuthenticatorEx(user, pass);
		}

		this.msg = new javax.mail.internet.MimeMessage(javax.mail.Session.getDefaultInstance(p, this.auth));
	}

	public void send(String to, String from, String subject, String contents) {
		try {
			this.msg.setFrom(new javax.mail.internet.InternetAddress(from));
			this.msg.setRecipients(javax.mail.Message.RecipientType.TO, to);
			this.msg.setSubject(subject);
			this.msg.setSentDate(new java.util.Date());
			this.msg.setContent(contents, "text/html; charset=" + this.charset);

			javax.mail.Transport.send(this.msg);
		} catch (Exception e) {
			System.out.println("failed send mail :: " + e.getMessage());
		}
	}

	public void send(String to, String from, String subject, String contents, String attache) {
		String[] attaches = {attache};

		this.send(to, from, subject, contents, attaches);
	}

	public void send(String to, String from, String subject, String contents, String[] attaches) {
		try {
			this.msg.setFrom(new javax.mail.internet.InternetAddress(from));
			this.msg.setRecipients(javax.mail.Message.RecipientType.TO, to);
			this.msg.setSubject(subject);
			this.msg.setSentDate(new java.util.Date());

			javax.mail.Multipart mp = new javax.mail.internet.MimeMultipart();
			javax.mail.BodyPart bp = null;
			javax.activation.FileDataSource ds = null;

			bp = new javax.mail.internet.MimeBodyPart();
			bp.setContent(contents, "text/html; charset=" + this.charset);
			mp.addBodyPart(bp);

			for (String v : attaches) {
				bp = new javax.mail.internet.MimeBodyPart();
				ds = new javax.activation.FileDataSource(v);
				bp.setDataHandler(new javax.activation.DataHandler(ds));
				bp.setFileName(ds.getName());
				mp.addBodyPart(bp);
			}

			this.msg.setContent(mp);

			javax.mail.Transport.send(this.msg);
		} catch (Exception e) {
			System.out.println("failed send mail :: " + e.getMessage());
		}
	}

	private class AuthenticatorEx extends javax.mail.Authenticator {
		private String user;
		private String pass;

		public AuthenticatorEx(String user, String pass) {
			this.user = user;
			this.pass = pass;
		}

		protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
			return new javax.mail.PasswordAuthentication(user, pass);
		}

	}
}
