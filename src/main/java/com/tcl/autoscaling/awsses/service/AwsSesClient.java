package com.tcl.autoscaling.awsses.service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.tcl.autoscaling.common.service.CommonService;

/**
 * AWS S3¿Í»§¶Ë
 * 
 * @author YuanHuan
 * 
 */
public class AwsSesClient
{
	private static AmazonSimpleEmailService amazonSimpleEmailService;

	private static AWSCredentials credentials;

	private static class CassandraClientHolder
	{
		private static final AwsSesClient instance = new AwsSesClient();
	}

	private AwsSesClient()
	{
		String awsAccessKeyId = CommonService.getAutoScalingConfValue("awsAccessKeyId");
		String awsSecretAccessKey = CommonService.getAutoScalingConfValue("awsSecretAccessKey");
		AwsSesClient.credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
		AwsSesClient.amazonSimpleEmailService = new AmazonSimpleEmailServiceClient(AwsSesClient.credentials);
	}

	public static final AwsSesClient getInstance()
	{
		return CassandraClientHolder.instance;
	}

	public AmazonSimpleEmailService getAmazonSimpleEmailService()
	{
		return AwsSesClient.amazonSimpleEmailService;
	}

	public void setAmazonSimpleEmailService(AmazonSimpleEmailService amazonSimpleEmailService)
	{
		AwsSesClient.amazonSimpleEmailService = amazonSimpleEmailService;
	}

	public AWSCredentials getCredentials()
	{
		return AwsSesClient.credentials;
	}

	public void setCredentials(AWSCredentials credentials)
	{
		AwsSesClient.credentials = credentials;
	}
}
