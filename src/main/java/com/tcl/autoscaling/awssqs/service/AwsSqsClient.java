package com.tcl.autoscaling.awssqs.service;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.tcl.autoscaling.common.service.CommonService;

/**
 * AWS SQS¿Í»§¶Ë
 * 
 * @author YuanHuan
 * 
 */
public class AwsSqsClient
{
	private static AmazonSQS amazonSQS;

	private static class CassandraClientHolder
	{
		private static final AwsSqsClient instance = new AwsSqsClient();
	}

	private AwsSqsClient()
	{
		String awsAccessKeyId = CommonService.getAutoScalingConfValue("awsAccessKeyId");
		String awsSecretAccessKey = CommonService.getAutoScalingConfValue("awsSecretAccessKey");
		BasicAWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
		AwsSqsClient.amazonSQS = new AmazonSQSClient(credentials);
	}

	public static final AwsSqsClient getInstance()
	{
		return CassandraClientHolder.instance;
	}

	public AmazonSQS getAmazonSQS()
	{
		return AwsSqsClient.amazonSQS;
	}

	public void setAmazonSQS(AmazonSQS amazonSQS)
	{
		AwsSqsClient.amazonSQS = amazonSQS;
	}

}
