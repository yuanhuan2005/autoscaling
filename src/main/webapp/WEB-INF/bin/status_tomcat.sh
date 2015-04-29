#!/bin/bash

tomcat_pid=`ps -ef |grep -v grep | grep tomcat |grep java | head -n 1 |awk '{print $2}'`
if [ "${tomcat_pid}x" == "x" ]
then
        echo dispatcher is stopped
	exit 0
else
	echo dispatcher is running
	exit 0
fi

