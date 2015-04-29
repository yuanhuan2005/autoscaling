#!/bin/bash

max_idle_number=20
current_dir=`dirname $0`
idle_number_file=${current_dir}/idle_number
dispatcher_log_file=/home/ec2-user/apache-tomcat-7.0.42/logs/debug.log

function check_running_user
{
	curr_user=`whoami`
	if [ "$curr_user" != "root" ]
	then
		echo "$0: Need to be root"
		exit 1
	fi
}


function is_dispatcher_in_service
{
	dispatcher_log_in_end=`tail -n 1 $dispatcher_log_file | grep -c "end to dispatch message"`
	if [ $dispatcher_log_in_end -eq 0 ]
	then
		echo "dispatcher is still in service now"
		return 1
	else
		echo "dispatcher is not in service now"
		return 0
	fi	
}

function need_to_delete_vm
{
	current_idle_number=`cat $idle_number_file`
	echo current_idle_number=$current_idle_number
	echo max_idle_number=$max_idle_number
	if [ $current_idle_number -gt $max_idle_number ]
	then
		echo "need to delete this vm now"
		return 1
	else
		echo "no need to delete this vm now"
		return 0
	fi
}

function delete_this_vm
{
	echo "going to poweroff to delete this vm"
	echo 0 > $idle_number_file
	poweroff
}

function handle_with_delete_vm
{
	echo "begin to check if we can delete this vm now"
	need_to_delete_vm
	if [ $? -eq 1 ]
	then
		delete_this_vm
		exit 0
	else
		current_idle_number=`cat $idle_number_file`
		new_idl_number=`expr $current_idle_number + 1`
		echo $new_idl_number > $idle_number_file
		exit 0
	fi
}

echo ""
date

# check running user is root or not
check_running_user

echo current user: `whoami`


# check dispatcher is running or not
dispatcher_running_status=`/etc/init.d/dispatcher status | grep -c running`
if [ $dispatcher_running_status -eq 1 ]
then
	echo "dispatcher is running"
	is_dispatcher_in_service
	if [ $? -eq 1 ]
	then
		echo 0 > $idle_number_file
		exit 0
	else
		handle_with_delete_vm
	fi
else
	echo "dispatcher is stopped"
	handle_with_delete_vm
fi


