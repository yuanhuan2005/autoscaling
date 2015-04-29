package com.tcl.autoscaling.mail;

import java.util.LinkedList;

/**
 * 邮件信息类
 * 
 * @author yuanhuan
 * 
 */
public class EmailMsg
{
	// 用于需要验证的用户名
	String username;

	// 口令
	String password;

	// SMTP服务器名称
	String smtpServer;

	// 端口号,缺省为25
	int smtpPort = 25;

	// 发件人
	String fromEmailAddress;

	// 收件人
	String[] toEmailAddresses;

	// 抄送
	String[] ccEmailAddresses;

	// 密送
	String[] bccEmailAddresses;

	// 主题
	String subject;

	// 邮件正文
	String body;

	// 附件的文件名列表
	@SuppressWarnings("rawtypes")
	LinkedList attachments;

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public String getSmtpServer()
	{
		return smtpServer;
	}

	public void setSmtpServer(String smtpServer)
	{
		this.smtpServer = smtpServer;
	}

	public int getSmtpPort()
	{
		return smtpPort;
	}

	public void setSmtpPort(int smtpPort)
	{
		this.smtpPort = smtpPort;
	}

	public String getFromEmailAddress()
	{
		return fromEmailAddress;
	}

	public void setFromEmailAddress(String fromEmailAddress)
	{
		this.fromEmailAddress = fromEmailAddress;
	}

	public String[] getToEmailAddresses()
	{
		return toEmailAddresses;
	}

	public void setToEmailAddresses(String[] toEmailAddresses)
	{
		this.toEmailAddresses = toEmailAddresses;
	}

	public String[] getCcEmailAddresses()
	{
		return ccEmailAddresses;
	}

	public void setCcEmailAddresses(String[] ccEmailAddresses)
	{
		this.ccEmailAddresses = ccEmailAddresses;
	}

	public String[] getBccEmailAddresses()
	{
		return bccEmailAddresses;
	}

	public void setBccEmailAddresses(String[] bccEmailAddresses)
	{
		this.bccEmailAddresses = bccEmailAddresses;
	}

	public String getSubject()
	{
		return subject;
	}

	public void setSubject(String subject)
	{
		this.subject = subject;
	}

	public String getBody()
	{
		return body;
	}

	public void setBody(String body)
	{
		this.body = body;
	}

	@SuppressWarnings("rawtypes")
	public LinkedList getAttachments()
	{
		return attachments;
	}

	@SuppressWarnings("rawtypes")
	public void setAttachments(LinkedList attachments)
	{
		this.attachments = attachments;
	}

}
