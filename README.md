# autoscaling
AutoScaling是AWS下面的一个比较强大的服务，借助于它，我们可以方便的实现业务的弹性自动伸缩。
但是，在使用过程中，发现AWS的AutoScaling（下文简称AmazonAS）存在一个问题，就是对于长时间运行的业务而言，比如一个视频转码请求需要几个小时才能处理完的，任务运行的时间几乎和视频时长一样长。在这种情况下，AmazonAS的ScaleDown策略就会出现一个问题：如果在一个虚拟机正在运行任务的时候，AmazonAS根据CloudWatch数据触发了ScaleDown策略，那么很有可能会删除掉该虚拟机，从而引起业务数据丢失或混乱。

由此可见，AmzonAS并不适合于我们的业务特性，因此有必要仿照AmazonAS来实现一套定制化的AutoScaling来满足我们的业务需求。

### 1. ScaleUp策略
基本类似AmazonAS的ScaleUp策略来实现，或者说是它的简化版本。
通过监控SQS中指定队列的消息个数来自动新建一定数量的虚拟机，来运行队列中的任务消息，新建虚拟机的同时发送邮件到指定邮箱。

### 2. ScaleDown策略
由上文介绍的AmazonAS存在的问题或不足，我们就不能简单的根据SQS中消息个数来删除虚拟机了。我们的策略是让虚拟机内部自动根据任务运行情况来自我删除。

实现方式如下：
在虚拟机内部预制好检查任务运行状态（我们是通过检测日志来实现的）的脚本，如果发现系统空转一段时间之后就执行关机命令，进而触发AWS EC2虚拟机的Shutdown Behavior进行自我删除。备注：这就要求创建虚拟机的时候设置Shutdown Behavior为Terminate，即删除。

### 3. 流程步骤
![as](http://i.imgur.com/Y49InOW.jpg)

1） 首先，AutoScaling启动一个线程，进入循环；

2）在当前循环周期内，根据SQS中消息个数进行ScaleUp策略检查，如果满足策略条件，则调用EC2创建虚拟机的API创建一台或者多台虚拟机，并将Shutdown Behavior设置为Terminate，虚拟机参数会在AutoScaling中进行预先配置。如果创建了虚拟机则会根据预制的邮件列表发送邮件通知，并且CooleDown一段时间。

3）在当前循环周期内，ScaleUp完成之后就进行ScaleDown策略检查，真正运行ScaleDown策略的机制是在EC2虚拟机里面通过cron任务来执行的，在AutoScaling中仅仅是判断哪些虚拟机在当前循环周期内被删除了，如果检测到有虚拟机被删除掉，则发邮件通知。

4）ScaleUp和ScaleDown在当前周期全部执行完毕之后，等待一段时间，然后重新进入下一次循环周期。


### 4. 代码结构
代码位于autoscaling/src/main/java/com/tcl/autoscaling下面：
awsec2    #EC2创建新的虚拟机接口
awsses    #SES接口，用于发邮件
awssqs    #SQS接口，用于操作SQS中的消息
common    #公共方法
listener    #监听器
mail    #发邮件接口
transcode    #执行业务Scale的核心代码

### 5. 配置文件
配置文件位于autoscaling/src/main/resources/autoscaling.propertites中：


> awsAccessKeyId=YOUR_ACCESS_KEY    #AWS的access key id  
> awsSecretAccessKey=YOUR_SECRET_KEY    #access key对应的secret key  
> queueMessageCheckDuration=600    #队列中的消息检查周期，单位：秒  
> transcodeMonitorQueueURL=https://sqs.us-east-1.amazonaws.com/xxxxxxxxxxxxx/transcoderQueue    #队列地址  
> transcodeMonitorQueueTotalNumberThreshold=1    #队列消息个数的阀值  
> transcodeInstanceLanchNumber=1    #启动的虚拟机个数  
> transcodeImageIdToLanchInstances=ami-88888888    #启动虚拟机使用的镜像id  
> transcodeRegion=us-west-2    #虚拟机在哪个region创建  
> transcodeAvailabilityZones=us-west-2a,us-west-2b,us-west-2c    #虚拟机在上面的region中哪个可用分区中创建  
> transcodeMaxInstancesNum=20    #创建虚拟机的最大个数  
> transcodeInstanceType=m1.xlarge    #虚拟机规格  
> transcodeKeyPair=transcoder_for_asg    #虚拟机keypair，用于ssh登录  
> transcodeSecurityGroupId=sg-f321c896    #虚拟机所在的安全组  
> transcodeInstanceName=test    #虚拟机名称，便于管理  
> transcodeCoolDownTimeInSeconds=1200    #cooldown时间，单位：秒  
> notificationEmails=user1@example.com;user2@example.com;user3@example.com    #email列表，多个email用分号分割  

3.4.3 shell脚本
在EC2虚拟机中需要安装如下的脚本，默认安装路径是/home/ec2-user/bin/，如有变化可对应修改。
脚本解释如下：
> check_dispatcher_status.sh     #检查运行状态。如果空转，则将idle_number的数字加1，当idle_number达到配置的上限时执行关机命> 令进行自我删除；如果没有空转即正在运行任务，则将idle_number清零，等待进入下次检查周期。  
> idle_number    #记录空转次数的文件  
> dispatcher     #注册为系统服务，以便可以用service命令进行管理，文件路径：/etc/init.d/dispatcher  
> restart_tomcat.sh    #重启tomcat的脚本  
> start_tomcat.sh     #启动tomcat的脚本  
> status_tomcat.sh     #检查tomcat运行状态的脚本  
> stop_tomcat.sh    #停止tomcat的脚本  

这些脚本的调用关系见下图所示：  
![](http://i.imgur.com/m4daGw0.jpg)