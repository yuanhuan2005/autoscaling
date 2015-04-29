package com.tcl.autoscaling.mail;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

/**
 * �����ʼ������� ������ģʽ��
 * 
 * @author yuanhuan
 * 
 */
public class MailSender
{
	private static MailSender sendMailInstance = new MailSender();

	/**
	 * ��ȡʵ��
	 * 
	 * @return ����ʵ��
	 */
	public static MailSender getInstance()
	{
		return MailSender.sendMailInstance;
	}

	/**
	 * ��ȡ����ֵ
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @return ���Զ���
	 */
	private Properties getProperties(EmailMsg emailMsg)
	{
		Properties props = System.getProperties();
		props.setProperty("mail.transport.protocol", "smtp"); // smtpЭ��
		props.setProperty("mail.smtp.host", emailMsg.getSmtpServer()); // ��������ַ
		props.setProperty("mail.smtp.port", "" + emailMsg.getSmtpPort()); // �˿ں�

		// ��֤��Ϣ
		if (emailMsg.getUsername() != null && emailMsg.getPassword() != null && emailMsg.getUsername().length() > 0
		        && emailMsg.getPassword().length() > 0)
		{
			// ��������ͨ����֤
			props.setProperty("mail.smtp.auth", "true");
		}

		return props;
	}

	/**
	 * ��ȡ�Ự
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @return �Ự����
	 */
	private Session getSession(EmailMsg emailMsg)
	{
		Properties props = getProperties(emailMsg);
		javax.mail.Session sess = javax.mail.Session.getDefaultInstance(props, null);
		return sess;
	}

	/**
	 * ��ȡMimeMessage����
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @return MimeMessage����
	 * @throws MessagingException
	 */
	private MimeMessage getMimeMessage(EmailMsg emailMsg) throws MessagingException
	{
		javax.mail.Session sess = getSession(emailMsg);
		MimeMessage msg = new MimeMessage(sess);

		// ���÷�������
		msg.setSentDate(new Date());

		return msg;
	}

	/**
	 * ���÷�����
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @param msg
	 *            MimeMessage����
	 * @throws AddressException
	 * @throws MessagingException
	 */
	private void setFromEmail(EmailMsg emailMsg, MimeMessage msg) throws AddressException, MessagingException
	{
		msg.setFrom(new InternetAddress(emailMsg.getFromEmailAddress()));
	}

	/**
	 * �����ռ���
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @param msg
	 *            MimeMessage����
	 * @throws AddressException
	 * @throws MessagingException
	 */
	private void setToEmail(EmailMsg emailMsg, MimeMessage msg) throws AddressException, MessagingException
	{
		String toEmailAddressesList = getToEmailAddressesString(emailMsg, msg);
		msg.addRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmailAddressesList));
	}

	/**
	 * ���ó�����
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @param msg
	 *            MimeMessage����
	 * @throws AddressException
	 * @throws MessagingException
	 */
	private void setCcEmail(EmailMsg emailMsg, MimeMessage msg) throws AddressException, MessagingException
	{
		String ccEmailAddressesList = getCcEmailAddressesString(emailMsg, msg);
		msg.addRecipients(Message.RecipientType.CC, InternetAddress.parse(ccEmailAddressesList));
	}

	/**
	 * ����������
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @param msg
	 *            MimeMessage����
	 * @throws AddressException
	 * @throws MessagingException
	 */
	private void setBccEmail(EmailMsg emailMsg, MimeMessage msg) throws AddressException, MessagingException
	{
		String bccEmailAddressesList = getBccEmailAddresseslist(emailMsg, msg);
		msg.addRecipients(Message.RecipientType.BCC, InternetAddress.parse(bccEmailAddressesList));
	}

	/**
	 * ��������
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @param msg
	 *            MimeMessage����
	 * @throws MessagingException
	 */
	private void setSubject(EmailMsg emailMsg, MimeMessage msg) throws MessagingException
	{
		if (emailMsg.getSubject() != null)
		{
			msg.setSubject(emailMsg.getSubject());
		}
	}

	/**
	 * �����ʼ�����
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @param content
	 *            MimeMessage����
	 * @throws MessagingException
	 */
	private void setBody(EmailMsg emailMsg, MimeMultipart content) throws MessagingException
	{
		MimeBodyPart part;
		if (emailMsg.getBody() != null && emailMsg.getBody().length() > 0)
		{
			part = new MimeBodyPart();
			String type = "text/plain";
			if (emailMsg.getBody().startsWith("<html>") || emailMsg.getBody().startsWith("<HTML>"))
			{
				type = "text/html";
			}
			part.setText(emailMsg.getBody());

			// ��<html>��<HTML>��ʼ���ı�Ϊtext/html����
			part.setHeader("Content-Type", type);

			content.addBodyPart(part);
		}
	}

	/**
	 * ���ø���
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @param content
	 *            MimeMultipart����
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 */
	@SuppressWarnings("rawtypes")
	private void setAttachments(EmailMsg emailMsg, MimeMultipart content) throws UnsupportedEncodingException,
	        MessagingException
	{
		MimeBodyPart part;

		// ��Ӹ���
		if (emailMsg.getAttachments() != null)
		{

			String filename;
			Iterator it = emailMsg.getAttachments().iterator();

			while (it.hasNext())
			{
				filename = (String) it.next();
				if (filename == null || filename.length() <= 0)
				{
					continue;
				}

				part = new MimeBodyPart();
				FileDataSource fds = new FileDataSource(filename);
				part.setDataHandler(new DataHandler(fds));
				part.setFileName(MimeUtility.encodeText(fds.getName()));

				content.addBodyPart(part);
			}
		}
	}

	/**
	 * ���ö��󣺰����ʼ����ĺ͸���
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @param msg
	 *            MimeMessage����
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException
	 */
	private void setObject(EmailMsg emailMsg, MimeMessage msg) throws MessagingException, UnsupportedEncodingException
	{
		MimeMultipart content = new MimeMultipart();

		// �����ʼ����ĺ͸���
		setBody(emailMsg, content);

		// ��Ӹ���
		setAttachments(emailMsg, content);

		// �����޸�
		msg.setContent(content);
	}

	/**
	 * ��ȡ�������б��ַ���
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @param msg
	 *            MimeMessage����
	 * @return �������б��Զ��ŷָ�
	 * @throws AddressException
	 * @throws MessagingException
	 */
	private String getToEmailAddressesString(EmailMsg emailMsg, MimeMessage msg) throws AddressException,
	        MessagingException
	{
		String toEmailAddressesString = "";
		String[] toEmailAddresses = emailMsg.getToEmailAddresses();
		if (null != toEmailAddresses)
		{
			for (int i = 0; i < toEmailAddresses.length; i++)
			{
				toEmailAddressesString += toEmailAddresses[i];
				if (i < toEmailAddresses.length - 1)
				{
					toEmailAddressesString += ",";
				}
			}
		}

		return toEmailAddressesString;
	}

	/**
	 * ��ȡ�������б��ַ���
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @param msg
	 *            MimeMessage����
	 * @return �������б��ַ������Զ��ŷָ�
	 * @throws AddressException
	 * @throws MessagingException
	 */
	private String getCcEmailAddressesString(EmailMsg emailMsg, MimeMessage msg) throws AddressException,
	        MessagingException
	{
		String ccEmailAddressesString = "";
		String[] ccEmailAddresses = emailMsg.getCcEmailAddresses();
		if (null != ccEmailAddresses)
		{
			for (int i = 0; i < ccEmailAddresses.length; i++)
			{
				ccEmailAddressesString += ccEmailAddresses[i];
				if (i < ccEmailAddresses.length - 1)
				{
					ccEmailAddressesString += ",";
				}
			}
		}

		return ccEmailAddressesString;
	}

	/**
	 * ��ȡ�������б��ַ���
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @param msg
	 *            MimeMessage����
	 * @return �������б��ַ������Զ��ŷָ�
	 * @throws AddressException
	 * @throws MessagingException
	 */
	private String getBccEmailAddresseslist(EmailMsg emailMsg, MimeMessage msg) throws AddressException,
	        MessagingException
	{
		String bccEmailAddresseslist = "";
		String[] bccEmailAddresses = emailMsg.getBccEmailAddresses();
		if (null != bccEmailAddresses)
		{
			for (int i = 0; i < bccEmailAddresses.length; i++)
			{
				bccEmailAddresseslist += bccEmailAddresses[i];
				if (i < bccEmailAddresses.length - 1)
				{
					bccEmailAddresseslist += ",";
				}
			}
		}

		return bccEmailAddresseslist;
	}

	/**
	 * �Ƿ���Ҫ��֤�û�
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @return true��ʾ��Ҫ��֤��false��ʾ����Ҫ
	 */
	private boolean needValidateUser(EmailMsg emailMsg)
	{
		if (emailMsg.getUsername() != null && emailMsg.getPassword() != null && emailMsg.getUsername().length() > 0
		        && emailMsg.getPassword().length() > 0)
		{
			return true;
		}

		return false;
	}

	/**
	 * ����Ҫ��֤�û��ķ�ʽ�����ʼ�
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @param msg
	 *            MimeMessage����
	 * @throws MessagingException
	 */
	private void sendWithValidation(EmailMsg emailMsg, MimeMessage msg) throws MessagingException
	{
		// �����Ҫ��֤�û�(��:smtp.sina.com.cn),��ʹ��connect��sendMessage
		// ע������趨mail.smtp.auth����Ϊtrue
		javax.mail.Session sess = getSession(emailMsg);
		Transport trans = sess.getTransport();
		trans.connect(emailMsg.getSmtpServer(), emailMsg.getUsername(), emailMsg.getPassword());
		String toEmailAddresseslist = getToEmailAddressesString(emailMsg, msg);
		InternetAddress[] internetAddress = InternetAddress.parse(toEmailAddresseslist);
		trans.sendMessage(msg, internetAddress);
		trans.close();
	}

	/**
	 * �Բ���Ҫ��֤�û��ķ�ʽ�����ʼ�
	 * 
	 * @param msg
	 *            MimeMessage����
	 * @throws MessagingException
	 */
	private void sendWithoutValidation(MimeMessage msg) throws MessagingException
	{
		// ����Ҫ�û����Ϳ���ʹ�þ�̬���������ɼ�
		Transport.send(msg);
	}

	/**
	 * ˽�з����ʼ�����
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @param msg
	 *            MimeMessage����
	 * @throws MessagingException
	 */
	private void sendEmail(EmailMsg emailMsg, MimeMessage msg) throws MessagingException
	{
		// �Ƿ���Ҫ��֤�û�
		if (needValidateUser(emailMsg))
		{
			sendWithValidation(emailMsg, msg);
		}
		else
		{
			sendWithoutValidation(msg);
		}
	}

	/**
	 * �����ʼ�
	 * 
	 * @param emailMsg
	 *            �ʼ���Ϣ
	 * @return ���ͽ����true��ʾ���ͳɹ�,false��ʾ����ʧ��
	 * @throws AddressException
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException
	 */
	public boolean sendMail(EmailMsg emailMsg) throws AddressException, MessagingException,
	        UnsupportedEncodingException
	{
		if (null == emailMsg)
		{
			return false;
		}

		// ��ȡMimeMessage
		MimeMessage msg = getMimeMessage(emailMsg);

		// ���÷�����
		setFromEmail(emailMsg, msg);

		// �����ռ���
		setToEmail(emailMsg, msg);

		// ���ó�����
		setCcEmail(emailMsg, msg);

		// ����������
		setBccEmail(emailMsg, msg);

		// ��������
		setSubject(emailMsg, msg);

		// �����ʼ����ĺ͸���
		setObject(emailMsg, msg);

		// �����ʼ�
		sendEmail(emailMsg, msg);

		// �����޸�
		msg.saveChanges();

		return true;
	}
}
