package com.tcl.autoscaling.mail;

import java.util.LinkedList;

/**
 * �ʼ���Ϣ��
 * 
 * @author yuanhuan
 * 
 */
public class EmailMsg
{
	// ������Ҫ��֤���û���
	String username;

	// ����
	String password;

	// SMTP����������
	String smtpServer;

	// �˿ں�,ȱʡΪ25
	int smtpPort = 25;

	// ������
	String fromEmailAddress;

	// �ռ���
	String[] toEmailAddresses;

	// ����
	String[] ccEmailAddresses;

	// ����
	String[] bccEmailAddresses;

	// ����
	String subject;

	// �ʼ�����
	String body;

	// �������ļ����б�
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
