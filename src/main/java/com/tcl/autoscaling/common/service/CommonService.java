package com.tcl.autoscaling.common.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tcl.autoscaling.common.domain.CommandExecutedResult;

public class CommonService
{
	final static private Log DEBUGGER = LogFactory.getLog(CommonService.class);

	private static String outputMsg = "";
	private static String errorMsg = "";

	/**
	 * 根据配置项名称获取对应的值
	 * 
	 * @param propertitesFile
	 *            property file
	 * @param confKey
	 *            配置项名称
	 * @return confValue 配置项值
	 */
	public static String getBaseConfValue(String propertitesFile, String confKey)
	{
		String confValue = "";

		InputStream inputStream = new CommonService().getClass().getClassLoader().getResourceAsStream(propertitesFile);
		Properties p = new Properties();

		try
		{
			p.load(inputStream);
			confValue = p.getProperty(confKey);
		}
		catch (Exception e)
		{
			confValue = "";
		}
		finally
		{
			try
			{
				if (null != inputStream)
				{
					inputStream.close();
				}
			}
			catch (IOException e)
			{
				confValue = "";
			}
		}

		return confValue;
	}

	/**
	 * 普通JAVA获取 WEB项目下的WEB-INF目录路径
	 * 
	 * @return WEB-INF目录路径
	 */
	public static String getWebInfPath()
	{
		URL url = new CommonService().getClass().getProtectionDomain().getCodeSource().getLocation();
		String path = url.toString();
		int index = path.indexOf("WEB-INF");

		if (index == -1)
		{
			index = path.indexOf("classes");
		}

		if (index == -1)
		{
			index = path.indexOf("bin");
		}

		path = path.substring(0, index);

		if (path.startsWith("zip"))
		{
			// 当class文件在war中时，此时返回zip:D:/...这样的路径
			path = path.substring(4);
		}
		else if (path.startsWith("file"))
		{
			// 当class文件在class文件中时，此时返回file:/D:/...这样的路径
			path = path.substring(6);
		}
		else if (path.startsWith("jar"))
		{
			// 当class文件在jar文件里面时，此时返回jar:file:/D:/...这样的路径
			path = path.substring(10);
		}
		try
		{
			path = URLDecoder.decode(path, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			CommonService.DEBUGGER.error("Exception: " + e.toString());
		}

		if (1 == CommonService.getOperatingSystemType())
		{
			path = "/" + path;
		}
		return path;
	}

	/**
	 * 根据配置项名称获取对应的值
	 * 
	 * @param confKey
	 *            配置项名称
	 * @return confValue 配置项值
	 */
	public static String getAutoScalingConfValue(String confKey)
	{
		return CommonService.getBaseConfValue("autoscaling.propertites", confKey);
	}

	/**
	 * 执行本地Windows或者Linux命令，并获取返回值和执行结果信息
	 * 
	 * @param cmd
	 *            待执行的命令
	 * @return 命令返回值
	 */
	public static CommandExecutedResult execLocalCommand(String cmd)
	{
		CommandExecutedResult commandExecutedResult = new CommandExecutedResult();
		int exitValue = -1;
		String resultMessage = "";
		commandExecutedResult.setExitValue(exitValue);
		commandExecutedResult.setResultMessage(resultMessage);
		Process process = null;

		try
		{
			process = Runtime.getRuntime().exec(cmd);

			// 获取CPU的核数，如果是单核CPU的话，就不能启动线程去监控输出和错误流了，只能
			if (1 == Runtime.getRuntime().availableProcessors())
			{
				InputStream stderrStream = process.getErrorStream();
				InputStream stdoutStream = process.getInputStream();
				InputStreamReader errStreamReader = new InputStreamReader(stderrStream);
				InputStreamReader outStreamReader = new InputStreamReader(stdoutStream);
				BufferedReader errBufReader = new BufferedReader(errStreamReader);
				BufferedReader outBufReader = new BufferedReader(outStreamReader);
				String errLine = null;
				String outLine = null;
				while (null != (errLine = errBufReader.readLine()) || null != (outLine = outBufReader.readLine()))
				{
					if (null != errLine && !"".equals(errLine))
					{
						CommonService.DEBUGGER.debug(errLine);
						CommonService.errorMsg += errLine;
					}
					if (null != outLine && !"".equals(outLine))
					{
						CommonService.DEBUGGER.debug(outLine);
						CommonService.outputMsg += outLine;
					}
				}
			}
			else
			{
				// 获取进程的标准输入流
				final InputStream inputStream = process.getInputStream();

				// 获取进程的错误流
				final InputStream errorStream = process.getErrorStream();

				// 开启输出流监控进程
				Thread outputStreamThread = new Thread()
				{
					@Override
					public void run()
					{
						BufferedReader br1 = new BufferedReader(new InputStreamReader(inputStream));
						try
						{
							String line1 = null;
							CommonService.outputMsg = "";
							while ((line1 = br1.readLine()) != null)
							{
								if (line1 != null)
								{
									CommonService.outputMsg += line1;
									CommonService.DEBUGGER.debug(line1);
								}
							}
						}
						catch (IOException e)
						{
							CommonService.DEBUGGER.error("Exception: " + e.toString());
						}
						finally
						{
							try
							{
								inputStream.close();
							}
							catch (IOException e)
							{
								CommonService.DEBUGGER.error("Exception: " + e.toString());
							}
						}
					}
				};
				outputStreamThread.start();

				// 开启错误流监控进程
				Thread errorStreamThread = new Thread()
				{
					@Override
					public void run()
					{
						BufferedReader br2 = new BufferedReader(new InputStreamReader(errorStream));
						try
						{
							String line2 = null;
							CommonService.errorMsg = "";
							while ((line2 = br2.readLine()) != null)
							{
								if (line2 != null)
								{
									CommonService.errorMsg += line2;
									CommonService.DEBUGGER.debug(line2);
								}
							}
						}
						catch (IOException e)
						{
							CommonService.DEBUGGER.error("Exception: " + e.toString());
						}
						finally
						{
							try
							{
								errorStream.close();
							}
							catch (IOException e)
							{
								CommonService.DEBUGGER.error("Exception: " + e.toString());
							}
						}
					}
				};
				errorStreamThread.start();

				// 等待输出流和错误流监控进程结束
				while (outputStreamThread.isAlive() || errorStreamThread.isAlive())
				{
					// 只是等待，不做其他操作
					;
				}
			}

			// 获取返回值
			exitValue = process.waitFor();
			if (0 == exitValue)
			{
				resultMessage = CommonService.outputMsg;
			}
			else
			{
				resultMessage = CommonService.errorMsg;
			}
		}
		catch (Exception e)
		{
			CommonService.DEBUGGER.error("Exception: " + e.toString());
		}
		finally
		{
			if (null != process)
			{
				process.destroy();
			}
		}

		commandExecutedResult.setExitValue(exitValue);
		commandExecutedResult.setResultMessage(resultMessage);
		return commandExecutedResult;
	}

	/**
	 * 获取操作系统类型
	 * 
	 * @return 操作系统类型: 0表示windows, 1表示Linux
	 */
	public static int getOperatingSystemType()
	{
		int osType = 0;
		String osName = System.getProperty("os.name");
		if (osName.toLowerCase().indexOf("windows") < 0)
		{
			osType = 1;
		}

		return osType;
	}

	/**
	 * 判断字符串是否为空
	 * 
	 * @param str
	 *            字符串
	 * @return true表示为空，false表示非空
	 */
	public static boolean isStringNull(String str)
	{
		boolean result = false;

		if (null == str || "".equals(str))
		{
			return true;
		}

		return result;
	}

	/**
	 * 判断字符串是否不为空
	 * 
	 * @param str
	 *            字符串
	 * @return true表示不为空，false表示为空
	 */
	public static boolean isStringNotNull(String str)
	{
		return !CommonService.isStringNull(str);
	}

	/**
	 * 判断文件是否存在
	 * 
	 * @param filePath
	 *            文件路径
	 * @return true表示存在，false表示不存在
	 */
	public static boolean isFileExisted(String filePath)
	{
		File file = new File(filePath);
		if (!file.exists())
		{
			return false;
		}

		return true;
	}

	/**
	 * 判断目录是否可写
	 * 
	 * @param 目录
	 * @return true表示可写，false表示不可写
	 */
	public static boolean isDirWritable(String dir)
	{
		File file = new File(dir);
		if (!file.exists())
		{
			CommonService.DEBUGGER.error(dir + " not found");
			return false;
		}

		if (!file.isDirectory())
		{
			CommonService.DEBUGGER.error(dir + " not a directory");
			return false;
		}

		if (!file.canWrite())
		{
			CommonService.DEBUGGER.error(dir + " cat not write");
			return false;
		}

		return true;
	}

	/**
	 * 删除文件
	 * 
	 * @param filePath
	 * @return true表示删除成功，false表示删除失败
	 */
	public static boolean deleteFile(String filePath)
	{
		try
		{
			File file = new File(filePath);
			if (!file.exists())
			{
				return true;
			}

			return file.delete();
		}
		catch (Exception e)
		{
			CommonService.DEBUGGER.error("Exception: " + e.toString());
		}

		return false;
	}

	/**
	 * 在目录的后面加上必要的斜杠
	 * 
	 * @param dirPath
	 *            目录路径
	 * @return 加上斜杠的目录路径
	 */
	public static String addSlashToDirPathIfNecessary(String dirPath)
	{
		if (CommonService.isStringNull(dirPath))
		{
			return dirPath;
		}

		String finalDirPath = dirPath;
		if (!dirPath.endsWith("/"))
		{
			finalDirPath = dirPath + "/";
		}

		return finalDirPath;
	}

	/**
	 * 写入文件
	 * 
	 * @param filePath
	 *            文件路径
	 * @param fileContent
	 *            文件内容
	 * @return true表示写入成功，false表示失败
	 */
	public static boolean writeFile(String filePath, String fileContent)
	{

		FileWriter fw = null;
		try
		{
			fw = new FileWriter(filePath, true);
			PrintWriter pw = new PrintWriter(fw);
			pw.println(fileContent);
			pw.close();
			fw.close();
			return true;
		}
		catch (IOException e)
		{
			CommonService.DEBUGGER.error("Exception : " + e.toString());
		}

		return false;
	}

	/**
	 * 获取文件类型
	 * 
	 * @param filePath
	 *            文件路径
	 * @return 文件类型
	 */
	public static String getFileFormat(String filePath)
	{
		String fileFormat = "";

		// 参数非空检查
		if (CommonService.isStringNull(filePath))
		{
			CommonService.DEBUGGER.error("filePath is null");
			return fileFormat;
		}

		// 检查文件是否含有类型
		if (filePath.indexOf(".") < 0)
		{
			CommonService.DEBUGGER.error("failed to get format from filePath");
			return fileFormat;
		}

		fileFormat = filePath.substring(filePath.lastIndexOf(".") + 1);

		return fileFormat;
	}

	/**
	 * 根据新文件类型生成新的文件路径
	 * 
	 * @param filePath
	 *            文件路径，例如：/tmp/test.mp4
	 * @param newFormat
	 *            新类型，例如：flv
	 * @return 新文件路径，例如：/tmp/test.flv
	 */
	public static String genFilePathWithNewFormat(String filePath, String newFormat)
	{
		String newFilePath = "";

		// 参数非空检查
		if (CommonService.isStringNull(filePath))
		{
			CommonService.DEBUGGER.error("filePath is null");
			return newFilePath;
		}

		// 参数非空检查
		if (CommonService.isStringNull(filePath))
		{
			CommonService.DEBUGGER.error("filePath is null");
			newFilePath = filePath;
			return newFilePath;
		}

		// 检查文件是否含有类型
		if (filePath.indexOf(".") < 0)
		{
			newFilePath = filePath + "." + newFormat;
			return newFilePath;
		}

		String filePathWithoutFormat = filePath.substring(0, filePath.lastIndexOf(".") + 1);
		newFilePath = filePathWithoutFormat + newFormat;

		return newFilePath;
	}

	/**
	 * 删除目录
	 * 
	 * @param dirPath
	 *            目录路径
	 */
	public static void deleteFolder(String dirPath)
	{
		if (CommonService.isStringNull(dirPath))
		{
			return;
		}

		try
		{
			File folder = new File(dirPath);
			File[] files = folder.listFiles();
			if (null != files)
			{
				for (File f : files)
				{
					if (f.isDirectory())
					{
						CommonService.deleteFolder(f.getAbsolutePath());
					}
					else
					{
						f.delete();
					}
				}
			}
			folder.delete();
		}
		catch (Exception e)
		{
			CommonService.DEBUGGER.error("Exception: " + e.toString());
		}
	}

	/**
	 * 休眠若干秒
	 * 
	 * @param seconds
	 *            秒数
	 */
	public static void doSleep(long seconds)
	{
		try
		{
			Thread.sleep(seconds * 1000);
		}
		catch (InterruptedException e)
		{
			// ignore
		}
	}
}
