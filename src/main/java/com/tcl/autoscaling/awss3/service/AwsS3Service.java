package com.tcl.autoscaling.awss3.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.Grant;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.tcl.autoscaling.common.service.CommonService;

public class AwsS3Service
{
	final static private Log DEBUGGER = LogFactory.getLog(AwsS3Service.class);

	/**
	 * 从 “/桶名/对象路径名” 的相对路径中解析出桶名
	 * 
	 * @param s3FilePath
	 *            “/桶名/对象路径名” 的相对路径
	 * @return 桶名
	 */
	public static String parseBucketName(String s3FilePath)
	{
		if (CommonService.isStringNull(s3FilePath))
		{
			return null;
		}

		String tmpS3FilePath = s3FilePath;
		if (s3FilePath.indexOf("/") == 0)
		{
			tmpS3FilePath = s3FilePath.substring(s3FilePath.indexOf("/") + 1);
		}
		String bucketName = tmpS3FilePath.substring(0, tmpS3FilePath.indexOf("/"));
		return bucketName;
	}

	/**
	 * 从 “/桶名/对象路径名” 的相对路径中解析出对象路径名
	 * 
	 * @param s3FilePath
	 *            “/桶名/对象路径名” 的相对路径
	 * @return 对象路径名
	 */
	public static String parseObjectPath(String s3FilePath)
	{

		if (CommonService.isStringNull(s3FilePath))
		{
			return null;
		}

		String tmpS3FilePath = s3FilePath;
		if (s3FilePath.indexOf("/") == 0)
		{
			tmpS3FilePath = s3FilePath.substring(s3FilePath.indexOf("/") + 1);
		}

		String key = tmpS3FilePath.substring(tmpS3FilePath.indexOf("/") + 1);
		return key;
	}

	/**
	 * 生成预签名的S3 URL，过期时间由配置文件指定
	 * 
	 * @param bucketName
	 *            桶名
	 * @param key
	 *            对象路径名
	 * @return 带签名的S3全路径URL（HTTP协议）。
	 *         例如：http://rideo.s3.amazonaws.com/video/test/test.mp4?
	 *         Expires=1380345878&AWSAccessKeyId=AKIAI57VEFB4FVT7TQNA
	 *         &Signature=8kOgGMkSkynLMzEySiUB6506lGc%3D
	 */
	private static String genPresignedUrl(String bucketName, String key)
	{
		// 默认过期时间为24小时
		long s3PresignedUrlExpirationSeconds = 60 * 60 * 24;
		String s3PresignedUrlExpirationSecondsStr = CommonService
		        .getAutoScalingConfValue("s3PresignedUrlExpirationSeconds");
		if (CommonService.isStringNotNull(s3PresignedUrlExpirationSecondsStr))
		{
			s3PresignedUrlExpirationSeconds = Long.valueOf(s3PresignedUrlExpirationSecondsStr);
		}
		Date expiration = new Date(new Date().getTime() + 1000 * s3PresignedUrlExpirationSeconds);

		while (key.startsWith("/"))
		{
			key = key.replaceFirst("/", "");
		}

		AmazonS3 s3 = AwsS3Service.getAmazonS3Client();
		URL newUrl = s3.generatePresignedUrl(bucketName, key, expiration);
		if (null == newUrl || CommonService.isStringNull(newUrl.toString()))
		{
			return null;
		}

		// 生成的是https协议的URL，转化为http协议
		String s3FullPath = newUrl.toString();
		if (s3FullPath.startsWith("https"))
		{
			s3FullPath = s3FullPath.replaceFirst("https", "http");
		}

		return s3FullPath;
	}

	/**
	 * 获取S3接口调用客户端
	 * 
	 * @return S3接口调用客户端
	 */
	public static AmazonS3 getAmazonS3Client()
	{
		if (null == AwsS3Client.getInstance() || null == AwsS3Client.getInstance().getAmazonS3())
		{
			String awsAccessKeyId = CommonService.getAutoScalingConfValue("awsAccessKeyId");
			String awsSecretAccessKey = CommonService.getAutoScalingConfValue("awsSecretAccessKey");
			BasicAWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
			return new AmazonS3Client(credentials);
		}

		return AwsS3Client.getInstance().getAmazonS3();
	}

	/**
	 * 设置上传之后的文件所有用户均可读
	 * 
	 * @param s3FilePath
	 *            S3中的文件路径，例如：/rideo/video/test.mp4
	 */
	public static void setObjectPublicRead(String s3FilePath)
	{
		AwsS3Service.DEBUGGER.debug("begin to setObjectPublicRead " + s3FilePath);
		AmazonS3 s3 = AwsS3Service.getAmazonS3Client();
		String bucketName = AwsS3Service.parseBucketName(s3FilePath);
		String key = AwsS3Service.parseObjectPath(s3FilePath);

		try
		{
			s3.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead);
			AwsS3Service.DEBUGGER.info("successfully set " + s3FilePath + " to public read");
		}
		catch (Exception e)
		{
			AwsS3Service.DEBUGGER.error("Exception: " + e.toString());
		}

		AwsS3Service.DEBUGGER.debug("end to setObjectPublicRead " + s3FilePath);
	}

	/**
	 * 拷贝S3中的文件
	 * 
	 * @param srcFilePath
	 *            S3源文件路径，例如：/src/tmp/test_clip.mp4
	 * @param descFilePath
	 *            S3目的文件路径，例如：/rideo/video/test_clip.mp4
	 * @return 拷贝结果
	 */
	public static CopyObjectResult copyFile(String srcFilePath, String descFilePath)
	{
		AwsS3Service.DEBUGGER.debug("begin to copy file " + srcFilePath + " to " + descFilePath);
		AmazonS3 s3 = AwsS3Service.getAmazonS3Client();
		CopyObjectResult copyObjectResult = null;

		// 参数检查
		if (CommonService.isStringNull(srcFilePath) || CommonService.isStringNull(descFilePath))
		{
			AwsS3Service.DEBUGGER.error("input parameter is null");
			AwsS3Service.DEBUGGER.debug("end to copy file");
			return copyObjectResult;
		}

		try
		{
			String srcBucketName = AwsS3Service.parseBucketName(srcFilePath);
			String srcKey = AwsS3Service.parseObjectPath(srcFilePath);
			String descBucketName = AwsS3Service.parseBucketName(descFilePath);
			String descKey = AwsS3Service.parseObjectPath(descFilePath);
			copyObjectResult = s3.copyObject(srcBucketName, srcKey, descBucketName, descKey);
			if (null != copyObjectResult)
			{
				AwsS3Service.DEBUGGER.info("successfully copy " + srcFilePath + " to " + descFilePath);
			}
		}
		catch (Exception e)
		{
			AwsS3Service.DEBUGGER.error("Failed to copy file. Exception: " + e.toString());
			AwsS3Service.DEBUGGER.debug("end to copy file");
			return copyObjectResult;
		}

		AwsS3Service.DEBUGGER.debug("end to copy file");
		return copyObjectResult;
	}

	/**
	 * 上传文件到S3
	 * 
	 * @param localFilePath
	 *            本地文件路径，例如：/tmp/test_clip.mp4
	 * @param s3FilePath
	 *            S3文件路径，例如：/rideo/video/test_clip.mp4
	 * @return 上传结果
	 */
	public static PutObjectResult uploadFile(String localFilePath, String s3FilePath)
	{
		AwsS3Service.DEBUGGER.debug("begin to upload file " + localFilePath + " to " + s3FilePath);
		AmazonS3 s3 = AwsS3Service.getAmazonS3Client();
		PutObjectResult putObjectResult = null;
		String bucketName = AwsS3Service.parseBucketName(s3FilePath);
		String key = AwsS3Service.parseObjectPath(s3FilePath);
		try
		{
			putObjectResult = s3.putObject(bucketName, key, new File(localFilePath));
			if (null != putObjectResult)
			{
				AwsS3Service.DEBUGGER.info("successfully upload " + localFilePath + " to " + s3FilePath);
			}
		}
		catch (AmazonServiceException e)
		{
			AwsS3Service.DEBUGGER.error("Failed to upload file. Exception: " + e.toString());
		}
		catch (AmazonClientException e)
		{
			AwsS3Service.DEBUGGER.error("Exception: " + e.toString());
		}

		AwsS3Service.DEBUGGER.debug("end to upload file");
		return putObjectResult;
	}

	/**
	 * 在S3中创建一个空文件夹（即创建一个以/结尾的空对象）
	 * 
	 * @param s3DirPath
	 *            S3路径
	 * 
	 * @return 处理结果
	 */
	public static PutObjectResult createS3EmptyDir(String s3DirPath)
	{
		AwsS3Service.DEBUGGER.debug("begin to create empty folder  " + s3DirPath);

		if (CommonService.isStringNull(s3DirPath))
		{
			AwsS3Service.DEBUGGER.error("s3DirPath is null");
			AwsS3Service.DEBUGGER.debug("end to create empty folder  " + s3DirPath);
			return null;
		}

		if (!s3DirPath.endsWith("/"))
		{
			s3DirPath += "/";
		}

		PutObjectResult result = null;
		try
		{
			InputStream input = new ByteArrayInputStream("".getBytes("UTF-8"));
			result = AwsS3Service.getAmazonS3Client().putObject(AwsS3Service.parseBucketName(s3DirPath),
			        AwsS3Service.parseObjectPath(s3DirPath), input, null);
			if (null != result)
			{
				AwsS3Service.DEBUGGER.debug("successfully create empty folder  " + s3DirPath);
			}
			else
			{
				AwsS3Service.DEBUGGER.debug("failed to create empty folder  " + s3DirPath);
			}
		}
		catch (UnsupportedEncodingException e)
		{
			AwsS3Service.DEBUGGER.error("Exception: " + e.toString());
			AwsS3Service.DEBUGGER.debug("end to create empty folder  " + s3DirPath);
			return null;
		}

		AwsS3Service.DEBUGGER.debug("end to create empty folder  " + s3DirPath);
		return result;
	}

	// 已经上传的文件列表
	private static List<String> uploadedFilesList = new ArrayList<String>();

	/**
	 * 删除已经上传的文件
	 */
	private static void deleteUploadedFiles()
	{
		// 此时，有文件上传失败，将整个文件夹上传置为失败，并将以前上传成功的文件删除
		if (null != AwsS3Service.uploadedFilesList && !AwsS3Service.uploadedFilesList.isEmpty())
		{
			for (int j = 0; j < AwsS3Service.uploadedFilesList.size(); j++)
			{
				AwsS3Service.deleteObject(AwsS3Service.uploadedFilesList.get(j));
			}
		}
	}

	/**
	 * 
	 * 上传文件夹到S3
	 * 
	 * @param localDirPath
	 *            文件夹路径
	 * @param originalLocalDirPath
	 *            原始的文件夹路径
	 * @param s3DirPath
	 *            在S3中的文件夹路径
	 * @return true表示上传成功，false表示上传失败
	 */
	public static boolean uploadDir(String localDirPath, String originalLocalDirPath, String s3DirPath)
	{
		if (CommonService.isStringNull(localDirPath) || CommonService.isStringNull(originalLocalDirPath)
		        || CommonService.isStringNull(s3DirPath))
		{
			AwsS3Service.DEBUGGER.debug("localDirPath=" + localDirPath);
			AwsS3Service.DEBUGGER.debug("originalLocalDirPath=" + originalLocalDirPath);
			AwsS3Service.DEBUGGER.debug("s3DirPath=" + s3DirPath);
			AwsS3Service.DEBUGGER.error("parameter is null");
			return false;
		}

		if (!originalLocalDirPath.endsWith("/"))
		{
			originalLocalDirPath += "/";
		}

		if (!s3DirPath.endsWith("/"))
		{
			s3DirPath += "/";
		}

		localDirPath = localDirPath.replaceAll("\\\\", "/");
		File dir = new File(localDirPath);
		File[] files = dir.listFiles();

		if (null == files)
		{
			AwsS3Service.DEBUGGER.info("It is empty in " + localDirPath + ", no need to upload");
			return true;
		}

		for (int i = 0; i < files.length; i++)
		{
			String localFilePath = files[i].getAbsolutePath();
			String s3FilePath = localFilePath.replaceAll("\\\\", "/");
			s3FilePath = s3FilePath.replaceFirst(originalLocalDirPath, s3DirPath);

			// 递归上传子文件夹
			if (files[i].isDirectory())
			{
				AwsS3Service.createS3EmptyDir(s3FilePath);
				AwsS3Service.uploadDir(files[i].getAbsolutePath(), originalLocalDirPath, s3DirPath);
			}
			else
			{
				// 上传文件
				PutObjectResult uploadResult = AwsS3Service.uploadFile(localFilePath, s3FilePath);
				if (null == uploadResult)
				{
					AwsS3Service.deleteUploadedFiles();
					return false;
				}
			}

			AwsS3Service.uploadedFilesList.add(s3FilePath);
		}

		return true;
	}

	/**
	 * 从S3下载文件到本地
	 * 
	 * @param localFilePath
	 *            本地文件路径，例如：/tmp/test_clip.mp4
	 * @param s3FilePath
	 *            S3文件路径，例如：/rideo/video/test_clip.mp4
	 * @return S3对象
	 */
	public static ObjectMetadata downloadFile(String localFilePath, String s3FilePath)
	{
		AwsS3Service.DEBUGGER.debug("begin to download file " + s3FilePath + " to " + localFilePath);

		ObjectMetadata objectMetadata = null;
		AmazonS3 s3 = AwsS3Service.getAmazonS3Client();
		String bucketName = AwsS3Service.parseBucketName(s3FilePath);
		String key = AwsS3Service.parseObjectPath(s3FilePath);
		try
		{
			GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
			objectMetadata = s3.getObject(getObjectRequest, new File(localFilePath));
			if (null != objectMetadata)
			{
				AwsS3Service.DEBUGGER.info("successfully download " + s3FilePath + " to " + localFilePath);
			}
		}
		catch (AmazonServiceException e)
		{
			AwsS3Service.DEBUGGER.error("Failed to download file. Exception: " + e.toString());
		}
		catch (AmazonClientException e)
		{
			AwsS3Service.DEBUGGER.error("Exception: " + e.toString());
		}

		AwsS3Service.DEBUGGER.debug("end to download file");
		return objectMetadata;
	}

	/**
	 * 将S3上的目录下的文件和子目录全部下载到本地
	 * 
	 * @param localDirPath
	 *            本地文件夹
	 * @param s3DirPath
	 *            S3上的目录
	 */
	public static void downloadDir(String localDirPath, String s3DirPath)
	{
		if (CommonService.isStringNull(localDirPath) || CommonService.isStringNull(s3DirPath))
		{
			AwsS3Service.DEBUGGER.debug("localDirPath=" + localDirPath);
			AwsS3Service.DEBUGGER.debug("s3DirPath=" + s3DirPath);
			AwsS3Service.DEBUGGER.error("parameter is null");
			return;
		}

		if (!localDirPath.endsWith("/"))
		{
			localDirPath += "/";
		}

		if (!s3DirPath.endsWith("/"))
		{
			s3DirPath += "/";
		}

		AmazonS3 s3Client = AwsS3Service.getAmazonS3Client();
		String bucketName = AwsS3Service.parseBucketName(s3DirPath);
		String keyDir = AwsS3Service.parseObjectPath(s3DirPath);
		ObjectListing objectList = s3Client.listObjects(bucketName, keyDir);
		if (null == objectList)
		{
			return;
		}

		List<S3ObjectSummary> s3ObjectSummaryList = objectList.getObjectSummaries();
		for (int i = 0; i < s3ObjectSummaryList.size(); i++)
		{
			String key = s3ObjectSummaryList.get(i).getKey();
			String localFilePath = localDirPath + key.replaceFirst(keyDir, "");
			String s3FilePath = "/" + bucketName + "/" + key;
			if (CommonService.isStringNull(key))
			{
				continue;
			}

			if (key.endsWith("/"))
			{
				// S3对象以/结尾，表示是一个文件夹，在本地创建该文件夹，并且如果父文件夹不存在的话则一并创建。
				File localFile = new File(localFilePath);
				localFile.mkdirs();
			}
			else
			{
				// 下载文件
				AwsS3Service.downloadFile(localFilePath, s3FilePath);
			}
		}
	}

	/**
	 * 检查S3对象是否存在
	 * 
	 * @param 对象相对路径
	 * @return true表示是存在的，false表示是不存在的
	 */
	public static boolean isObjectExist(String objectPath)
	{
		try
		{
			int secondSlash = objectPath.indexOf('/', 1);
			String bucketName = objectPath.substring(1, secondSlash);
			String key = objectPath.substring(secondSlash + 1);
			while (key.startsWith("/"))
			{
				key = key.replaceFirst("/", "");
			}

			// 获取S3对象的元数据
			ObjectMetadata objectMetadata = AwsS3Service.getAmazonS3Client().getObjectMetadata(bucketName, key);
			if (null != objectMetadata)
			{
				return true;
			}
		}
		catch (Exception e)
		{
			AwsS3Service.DEBUGGER.error("Exception: " + e.toString());
			return false;
		}

		return false;
	}

	/**
	 * 检查S3对象是否是所有人可读的
	 * 
	 * @param bucketName
	 *            桶名
	 * @param key
	 *            对象路径
	 * @return true表示是所有人可读的，false表示不是所有人可读的
	 */
	public static boolean isObjectPublicRead(String bucketName, String key)
	{
		try
		{
			AmazonS3 s3 = AwsS3Service.getAmazonS3Client();
			while (key.startsWith("/"))
			{
				key = key.replaceFirst("/", "");
			}

			// 获取S3对象的ACL
			AccessControlList s3ObjectAcl = s3.getObjectAcl(bucketName, key);
			if (null == s3ObjectAcl || null == s3ObjectAcl.getGrants() || s3ObjectAcl.getGrants().isEmpty())
			{
				return false;
			}

			// 遍历ACL，检查是否具有所有用户可读的权限
			Set<Grant> grantsSet = s3ObjectAcl.getGrants();
			Iterator<Grant> grantsIterator = grantsSet.iterator();
			Grant grant = null;
			while (grantsIterator.hasNext())
			{
				grant = grantsIterator.next();

				// 找到了所有用户的可读权限
				if (grant.getGrantee().getIdentifier().endsWith("AllUsers")
				        && Permission.Read.getHeaderName().equals(grant.getPermission().getHeaderName()))
				{
					return true;
				}
			}
		}
		catch (Exception e)
		{
			AwsS3Service.DEBUGGER.error("Exception: " + e.toString());
		}

		return false;
	}

	/**
	 * 从桶名+对象名的相对路径获取S3的全路径
	 * 
	 * @param inputPath
	 *            相对路径，格式为：/桶名/对象名，例如：/rideo/video/test/test.mp4
	 * @return 能够访问的S3全路径，例如：http://rideo.s3.amazonaws.com/video/test/test.mp4
	 *         或者带签名和过期时间的地址http://
	 *         rideo.s3.amazonaws.com/video/test/test.mp4?Expires=1380345878
	 *         &AWSAccessKeyId
	 *         =AKIAI57VEFB4FVT7TQNA&Signature=8kOgGMkSkynLMzEySiUB6506lGc%3D
	 */
	public static String genS3InputPath(String inputPath)
	{
		if (CommonService.isStringNull(inputPath))
		{
			return null;
		}

		// 解析桶名
		String bucketName = AwsS3Service.parseBucketName(inputPath);
		if (CommonService.isStringNull(bucketName))
		{
			return null;
		}

		// 解析对象路径
		String key = AwsS3Service.parseObjectPath(inputPath);
		if (CommonService.isStringNull(bucketName))
		{
			return null;
		}

		// 拼接S3的HOST和URI，得到的字符串为:
		// http://rideo.s3.amazonaws.com/video/test/test.mp4
		String s3InputPath = "http://" + bucketName + ".s3.amazonaws.com";
		if (!key.startsWith("/"))
		{
			key = "/" + key;
		}
		s3InputPath += key;

		// 检查该对象是否有所有人可读的权限，如果没有的话需要加上签名
		if (!AwsS3Service.isObjectPublicRead(bucketName, key))
		{
			s3InputPath = AwsS3Service.genPresignedUrl(bucketName, key);
		}

		return s3InputPath;
	}

	/**
	 * 删除S3的一个对象
	 * 
	 * @param s3FilePath
	 *            S3对象文件路径
	 */
	public static void deleteObject(String s3FilePath)
	{
		try
		{
			if (CommonService.isStringNull(s3FilePath))
			{
				return;
			}
			AmazonS3 s3Client = AwsS3Service.getAmazonS3Client();
			String bucketName = AwsS3Service.parseBucketName(s3FilePath);
			String key = AwsS3Service.parseObjectPath(s3FilePath);
			s3Client.deleteObject(bucketName, key);
		}
		catch (Exception e)
		{
			AwsS3Service.DEBUGGER.error("Exception: " + e.toString());
		}
	}

	/**
	 * 删除S3的一个以/结尾的对象，以及其下面的子文件
	 * 
	 * @param s3DirPath
	 *            S3文件夹路径，以/结尾的对象
	 */
	public static void deleteFolderAndSubfiles(String s3DirPath)
	{
		try
		{
			if (CommonService.isStringNull(s3DirPath))
			{
				return;
			}
			AmazonS3 s3Client = AwsS3Service.getAmazonS3Client();
			String bucketName = AwsS3Service.parseBucketName(s3DirPath);
			String key = AwsS3Service.parseObjectPath(s3DirPath);

			ObjectListing list = s3Client.listObjects(bucketName, key);
			List<S3ObjectSummary> osList = list.getObjectSummaries();
			for (int i = 0; i < osList.size(); i++)
			{
				String s3ObjectPath = "/" + osList.get(i).getBucketName() + "/" + osList.get(i).getKey();
				AwsS3Service.DEBUGGER.debug("deleting " + s3ObjectPath);
				s3Client.deleteObject(osList.get(i).getBucketName(), osList.get(i).getKey());
			}
		}
		catch (Exception e)
		{
			AwsS3Service.DEBUGGER.error("Exception: " + e.toString());
		}
	}
}
