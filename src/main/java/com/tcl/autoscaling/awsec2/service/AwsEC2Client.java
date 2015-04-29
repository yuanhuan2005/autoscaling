package com.tcl.autoscaling.awsec2.service;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.tcl.autoscaling.common.service.CommonService;

/**
 * AWS S3¿Í»§¶Ë
 * 
 * @author YuanHuan
 * 
 */
public class AwsEC2Client
{
	private static AmazonEC2Client amazonEC2Client;

	private static class CassandraClientHolder
	{
		private static final AwsEC2Client instance = new AwsEC2Client();
	}

	private AwsEC2Client()
	{
		String awsAccessKeyId = CommonService.getAutoScalingConfValue("awsAccessKeyId");
		String awsSecretAccessKey = CommonService.getAutoScalingConfValue("awsSecretAccessKey");
		BasicAWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
		AwsEC2Client.amazonEC2Client = new AmazonEC2Client(credentials);
		String regionStr = CommonService.getAutoScalingConfValue("transcodeRegion");
		AwsEC2Client.amazonEC2Client.setEndpoint("ec2." + regionStr + ".amazonaws.com");
	}

	public static final AwsEC2Client getInstance()
	{
		return CassandraClientHolder.instance;
	}

	public AmazonEC2Client getAmazonEC2Client()
	{
		return AwsEC2Client.amazonEC2Client;
	}

	public void setAmazonEC2Client(AmazonEC2Client amazonEC2Client)
	{
		AwsEC2Client.amazonEC2Client = amazonEC2Client;
	}

}
