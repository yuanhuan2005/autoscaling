package com.tcl.autoscaling.awsses.service;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AWSJavaMailTransport;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.ListVerifiedEmailAddressesResult;
import com.amazonaws.services.simpleemail.model.VerifyEmailAddressRequest;

public class AwsSesService
{
	final static private Log DEBUGGER = LogFactory.getLog(AwsSesService.class);

	private static final String FROM = "tclwebservice@126.com";

	/**
	 * ��֤�����ַ��
	 * ���SES���õ������������Ļ�������Ҫ��֤�ռ��ˣ���Ҫ��֤�����ˡ����SES�ǲ��Ի����Ļ������߶���Ҫ��֤��
	 * @param ses SES�������
	 * @param address �����ַ
	 */
	@SuppressWarnings("unused")
	private static void verifyEmailAddress(AmazonSimpleEmailService ses, String address)
	{
		ListVerifiedEmailAddressesResult verifiedEmails = ses.listVerifiedEmailAddresses();
		if (verifiedEmails.getVerifiedEmailAddresses().contains(address))
		{
			return;
		}

		ses.verifyEmailAddress(new VerifyEmailAddressRequest().withEmailAddress(address));
		AwsSesService.DEBUGGER.info("Please check the email address " + address + " to verify it");
	}

	public static AmazonSimpleEmailService getAmazonSimpleEmailService()
	{
		if (null == AwsSesClient.getInstance() || null == AwsSesClient.getInstance().getAmazonSimpleEmailService())
		{
			AWSCredentials credentials = new ClasspathPropertiesFileCredentialsProvider().getCredentials();
			return new AmazonSimpleEmailServiceClient(credentials);
		}

		return AwsSesClient.getInstance().getAmazonSimpleEmailService();
	}

	public static AWSCredentials getAWSCredentials()
	{
		if (null == AwsSesClient.getInstance() || null == AwsSesClient.getInstance().getCredentials())
		{
			AWSCredentials credentials = new ClasspathPropertiesFileCredentialsProvider().getCredentials();
			return credentials;
		}

		return AwsSesClient.getInstance().getCredentials();
	}

	/**
	 * ʹ��SES�����ʼ�
	 * @param toEmail �ռ���
	 * @param subject ����
	 * @param body �ʼ�����
	 * @return ���ͽ����true��ʾ���ͳɹ���false��ʾ����ʧ��
	 */
	public static boolean sendEmail(String toEmail, String subject, String body)
	{
		AwsSesService.DEBUGGER.debug("Sending email to " + toEmail + ", subject is: " + subject);
		try
		{
			AWSCredentials credentials = AwsSesService.getAWSCredentials();
			AmazonSimpleEmailService ses = AwsSesService.getAmazonSimpleEmailService();
			Region usWest2 = Region.getRegion(Regions.US_EAST_1);
			ses.setRegion(usWest2);

			Properties props = new Properties();
			props.setProperty("mail.transport.protocol", "aws");
			props.setProperty("mail.aws.user", credentials.getAWSAccessKeyId());
			props.setProperty("mail.aws.password", credentials.getAWSSecretKey());

			Session session = Session.getInstance(props);

			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(AwsSesService.FROM));
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
			msg.setSubject(subject);
			msg.setText(body);
			msg.saveChanges();

			Transport t = new AWSJavaMailTransport(session, null);
			t.connect();
			t.sendMessage(msg, null);

			t.close();
		}
		catch (AddressException e)
		{
			e.printStackTrace();
			AwsSesService.DEBUGGER.error("Caught an AddressException, which means one or more of your "
			        + "addresses are improperly formatted.");
			return false;
		}
		catch (MessagingException e)
		{
			e.printStackTrace();
			AwsSesService.DEBUGGER.error("Caught a MessagingException, which means that there was a "
			        + "problem sending your message to Amazon's E-mail Service check the "
			        + "stack trace for more information.");
			return false;
		}

		return true;
	}

	public static void main(String[] args)
	{
		System.out.println("Enter main");

		AwsSesService.sendEmail("yuanhuan@tcl.com", "Test", "Hello world.");

		System.out.println("End main");
	}
}
