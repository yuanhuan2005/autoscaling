#!/bin/bash

tomcat_pid=`ps -ef |grep -v grep | grep tomcat |grep java | head -n 1 |awk '{print $2}'`
if [ "${tomcat_pid}x" != "x" ]
then
        kill -9 $tomcat_pid
	if [ $? -eq 0 ]
	then
		echo "dispatcher is stopped"
	else
		echo "failed to stop dispatcher"
	fi
fi

