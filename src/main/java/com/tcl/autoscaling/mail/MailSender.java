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
 * 发送邮件操作类 （单例模式）
 * 
 * @author yuanhuan
 * 
 */
public class MailSender
{
	private static MailSender sendMailInstance = new MailSender();

	/**
	 * 获取实例
	 * 
	 * @return 本类实例
	 */
	public static MailSender getInstance()
	{
		return MailSender.sendMailInstance;
	}

	/**
	 * 获取属性值
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @return 属性对象
	 */
	private Properties getProperties(EmailMsg emailMsg)
	{
		Properties props = System.getProperties();
		props.setProperty("mail.transport.protocol", "smtp"); // smtp协议
		props.setProperty("mail.smtp.host", emailMsg.getSmtpServer()); // 服务器地址
		props.setProperty("mail.smtp.port", "" + emailMsg.getSmtpPort()); // 端口号

		// 认证信息
		if (emailMsg.getUsername() != null && emailMsg.getPassword() != null && emailMsg.getUsername().length() > 0
		        && emailMsg.getPassword().length() > 0)
		{
			// 这样才能通过验证
			props.setProperty("mail.smtp.auth", "true");
		}

		return props;
	}

	/**
	 * 获取会话
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @return 会话对象
	 */
	private Session getSession(EmailMsg emailMsg)
	{
		Properties props = getProperties(emailMsg);
		javax.mail.Session sess = javax.mail.Session.getDefaultInstance(props, null);
		return sess;
	}

	/**
	 * 获取MimeMessage对象
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @return MimeMessage对象
	 * @throws MessagingException
	 */
	private MimeMessage getMimeMessage(EmailMsg emailMsg) throws MessagingException
	{
		javax.mail.Session sess = getSession(emailMsg);
		MimeMessage msg = new MimeMessage(sess);

		// 设置发件日期
		msg.setSentDate(new Date());

		return msg;
	}

	/**
	 * 设置发件人
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @param msg
	 *            MimeMessage对象
	 * @throws AddressException
	 * @throws MessagingException
	 */
	private void setFromEmail(EmailMsg emailMsg, MimeMessage msg) throws AddressException, MessagingException
	{
		msg.setFrom(new InternetAddress(emailMsg.getFromEmailAddress()));
	}

	/**
	 * 设置收件人
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @param msg
	 *            MimeMessage对象
	 * @throws AddressException
	 * @throws MessagingException
	 */
	private void setToEmail(EmailMsg emailMsg, MimeMessage msg) throws AddressException, MessagingException
	{
		String toEmailAddressesList = getToEmailAddressesString(emailMsg, msg);
		msg.addRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmailAddressesList));
	}

	/**
	 * 设置抄送人
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @param msg
	 *            MimeMessage对象
	 * @throws AddressException
	 * @throws MessagingException
	 */
	private void setCcEmail(EmailMsg emailMsg, MimeMessage msg) throws AddressException, MessagingException
	{
		String ccEmailAddressesList = getCcEmailAddressesString(emailMsg, msg);
		msg.addRecipients(Message.RecipientType.CC, InternetAddress.parse(ccEmailAddressesList));
	}

	/**
	 * 设置米送人
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @param msg
	 *            MimeMessage对象
	 * @throws AddressException
	 * @throws MessagingException
	 */
	private void setBccEmail(EmailMsg emailMsg, MimeMessage msg) throws AddressException, MessagingException
	{
		String bccEmailAddressesList = getBccEmailAddresseslist(emailMsg, msg);
		msg.addRecipients(Message.RecipientType.BCC, InternetAddress.parse(bccEmailAddressesList));
	}

	/**
	 * 设置主题
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @param msg
	 *            MimeMessage对象
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
	 * 设置邮件正文
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @param content
	 *            MimeMessage对象
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

			// 以<html>和<HTML>开始的文本为text/html类型
			part.setHeader("Content-Type", type);

			content.addBodyPart(part);
		}
	}

	/**
	 * 设置附件
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @param content
	 *            MimeMultipart对象
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 */
	@SuppressWarnings("rawtypes")
	private void setAttachments(EmailMsg emailMsg, MimeMultipart content) throws UnsupportedEncodingException,
	        MessagingException
	{
		MimeBodyPart part;

		// 添加附件
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
	 * 设置对象：包括邮件正文和附件
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @param msg
	 *            MimeMessage对象
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException
	 */
	private void setObject(EmailMsg emailMsg, MimeMessage msg) throws MessagingException, UnsupportedEncodingException
	{
		MimeMultipart content = new MimeMultipart();

		// 设置邮件正文和附件
		setBody(emailMsg, content);

		// 添加附件
		setAttachments(emailMsg, content);

		// 保存修改
		msg.setContent(content);
	}

	/**
	 * 获取发件人列表字符串
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @param msg
	 *            MimeMessage对象
	 * @return 发件人列表，以逗号分隔
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
	 * 获取抄送人列表字符串
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @param msg
	 *            MimeMessage对象
	 * @return 抄送人列表字符串，以逗号分隔
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
	 * 获取密送人列表字符串
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @param msg
	 *            MimeMessage对象
	 * @return 密送人列表字符串，以逗号分隔
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
	 * 是否需要验证用户
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @return true表示需要验证，false表示不需要
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
	 * 以需要验证用户的方式发送邮件
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @param msg
	 *            MimeMessage对象
	 * @throws MessagingException
	 */
	private void sendWithValidation(EmailMsg emailMsg, MimeMessage msg) throws MessagingException
	{
		// 如果需要验证用户(如:smtp.sina.com.cn),则使用connect并sendMessage
		// 注意必须设定mail.smtp.auth属性为true
		javax.mail.Session sess = getSession(emailMsg);
		Transport trans = sess.getTransport();
		trans.connect(emailMsg.getSmtpServer(), emailMsg.getUsername(), emailMsg.getPassword());
		String toEmailAddresseslist = getToEmailAddressesString(emailMsg, msg);
		InternetAddress[] internetAddress = InternetAddress.parse(toEmailAddresseslist);
		trans.sendMessage(msg, internetAddress);
		trans.close();
	}

	/**
	 * 以不需要验证用户的方式发送邮件
	 * 
	 * @param msg
	 *            MimeMessage对象
	 * @throws MessagingException
	 */
	private void sendWithoutValidation(MimeMessage msg) throws MessagingException
	{
		// 不需要用户名和口令使用静态方法发送由件
		Transport.send(msg);
	}

	/**
	 * 私有发送邮件方法
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @param msg
	 *            MimeMessage对象
	 * @throws MessagingException
	 */
	private void sendEmail(EmailMsg emailMsg, MimeMessage msg) throws MessagingException
	{
		// 是否需要验证用户
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
	 * 发送邮件
	 * 
	 * @param emailMsg
	 *            邮件信息
	 * @return 发送结果：true表示发送成功,false表示发送失败
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

		// 获取MimeMessage
		MimeMessage msg = getMimeMessage(emailMsg);

		// 设置发件人
		setFromEmail(emailMsg, msg);

		// 设置收件人
		setToEmail(emailMsg, msg);

		// 设置抄送人
		setCcEmail(emailMsg, msg);

		// 设置密送人
		setBccEmail(emailMsg, msg);

		// 设置主题
		setSubject(emailMsg, msg);

		// 设置邮件正文和附件
		setObject(emailMsg, msg);

		// 发送邮件
		sendEmail(emailMsg, msg);

		// 保存修改
		msg.saveChanges();

		return true;
	}
}
