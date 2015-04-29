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
