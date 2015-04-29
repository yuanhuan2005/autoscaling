package com.tcl.autoscaling.awsec2.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.tcl.autoscaling.common.service.CommonService;

public class AwsEC2Service
{
	final static private Log DEBUGGER = LogFactory.getLog(AwsEC2Service.class);

	public static AmazonEC2Client getAmazonEC2Client()
	{
		if (null == AwsEC2Client.getInstance() || null == AwsEC2Client.getInstance().getAmazonEC2Client())
		{
			String awsAccessKeyId = CommonService.getAutoScalingConfValue("awsAccessKeyId");
			String awsSecretAccessKey = CommonService.getAutoScalingConfValue("awsSecretAccessKey");
			BasicAWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
			AmazonEC2Client ec2Client = new AmazonEC2Client(credentials);
			String regionStr = CommonService.getAutoScalingConfValue("transcodeRegion");
			ec2Client.setEndpoint("ec2." + regionStr + ".amazonaws.com");
			return ec2Client;
		}

		return AwsEC2Client.getInstance().getAmazonEC2Client();
	}

	private static boolean lanchNewInstance(RunInstancesRequest runInstancesRequest, String availabilityZone)
	{
		AmazonEC2Client ec2 = AwsEC2Service.getAmazonEC2Client();
		try
		{
			Placement placement = new Placement();
			placement.setAvailabilityZone(availabilityZone);
			runInstancesRequest.setPlacement(placement);
			RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);
			if (null == runInstancesResult || null == runInstancesResult.getReservation()
			        || null == runInstancesResult.getReservation().getInstances()
			        || runInstancesResult.getReservation().getInstances().isEmpty())
			{
				AwsEC2Service.DEBUGGER.error("RunInstancesResult is empty");
				return false;
			}

			// 添加虚拟机名字
			String transcodeInstanceName = CommonService.getAutoScalingConfValue("transcodeInstanceName");
			List<Instance> instances = runInstancesResult.getReservation().getInstances();
			for (Instance instance : instances)
			{
				CreateTagsRequest createTagsRequest = new CreateTagsRequest();
				createTagsRequest.withResources(instance.getInstanceId()).withTags(
				        new Tag("Name", transcodeInstanceName + "_" + new Date().getTime()));
				ec2.createTags(createTagsRequest);
			}
			AwsEC2Service.DEBUGGER.debug("Instances: \n"
			        + runInstancesResult.getReservation().getInstances().toString());
		}
		catch (Exception e)
		{
			AwsEC2Service.DEBUGGER.error("Exception: " + e.toString());
			return false;
		}

		return true;
	}

	public static int launchNewInstances(int num) throws AmazonServiceException
	{
		int successNum = 0;
		AwsEC2Service.DEBUGGER.debug("begin to launch new instances");

		if (1 > num)
		{
			AwsEC2Service.DEBUGGER.info("instances num is 0, no need to launch instances");
			return successNum;
		}

		RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
		String imageId = CommonService.getAutoScalingConfValue("transcodeImageIdToLanchInstances");
		runInstancesRequest.setImageId(imageId);
		runInstancesRequest.setMaxCount(1);
		runInstancesRequest.setMinCount(1);
		runInstancesRequest.setInstanceInitiatedShutdownBehavior("terminate");
		String transcodeInstanceType = CommonService.getAutoScalingConfValue("transcodeInstanceType");
		if (CommonService.isStringNull(transcodeInstanceType))
		{
			transcodeInstanceType = InstanceType.M1Small.toString();
		}
		runInstancesRequest.setInstanceType(transcodeInstanceType);
		String keyName = CommonService.getAutoScalingConfValue("transcodeKeyPair");
		runInstancesRequest.setKeyName(keyName);
		String securityGroupId = CommonService.getAutoScalingConfValue("transcodeSecurityGroupId");
		List<String> securityGroupIds = new ArrayList<String>();
		securityGroupIds.add(securityGroupId);
		runInstancesRequest.setSecurityGroupIds(securityGroupIds);

		// 逐个创建虚拟机
		for (int i = 0; i < num; i++)
		{
			String transcodeAvailabilityZones = CommonService.getAutoScalingConfValue("transcodeAvailabilityZones");
			String[] transcodeAvailabilityZonesArr = transcodeAvailabilityZones.split(",");
			for (int j = 0; j < transcodeAvailabilityZonesArr.length; j++)
			{
				boolean result = AwsEC2Service.lanchNewInstance(runInstancesRequest, transcodeAvailabilityZonesArr[j]);
				if (result)
				{
					successNum++;
					break;
				}
			}
		}

		AwsEC2Service.DEBUGGER.debug("end to launch new instances");
		return successNum;
	}
}
