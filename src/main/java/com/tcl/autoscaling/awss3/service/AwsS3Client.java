package com.tcl.autoscaling.awss3.service;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.tcl.autoscaling.common.service.CommonService;

/**
 * AWS S3¿Í»§¶Ë
 * 
 * @author YuanHuan
 * 
 */
public class AwsS3Client
{
	private static AmazonS3 amazonS3;

	private static class AwsS3ClientHolder
	{
		private static final AwsS3Client instance = new AwsS3Client();
	}

	private AwsS3Client()
	{
		String s3AccessKeyId = CommonService.getAutoScalingConfValue("awsAccessKeyId");
		String s3SecretAccessKey = CommonService.getAutoScalingConfValue("awsSecretAccessKey");
		BasicAWSCredentials credentials = new BasicAWSCredentials(s3AccessKeyId, s3SecretAccessKey);
		AwsS3Client.amazonS3 = new AmazonS3Client(credentials);
	}

	public static final AwsS3Client getInstance()
	{
		return AwsS3ClientHolder.instance;
	}

	public AmazonS3 getAmazonS3()
	{
		return AwsS3Client.amazonS3;
	}

	public void setAmazonS3(AmazonS3 amazonS3)
	{
		AwsS3Client.amazonS3 = amazonS3;
	}

}
