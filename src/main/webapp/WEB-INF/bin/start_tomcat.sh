#!/bin/bash

tomcat_pid=`ps -ef |grep -v grep | grep tomcat |grep java | head -n 1 |awk '{print $2}'`
if [ "${tomcat_pid}x" != "x" ]
then
	echo "dispatcher is already running"
	exit 0
fi

curr_user=`whoami`
if [ "$curr_user" == "ec2-user" ]
then
        /home/ec2-user/apache-tomcat-7.0.42/bin/startup.sh > /dev/null 2>&1
else
        su ec2-user -c /home/ec2-user/apache-tomcat-7.0.42/bin/startup.sh > /dev/null 2>&1
fi

if [ $? -eq 0 ]
then
	echo "dispatcher is running"
else
	echo "faield to start dispatcher"
fi

