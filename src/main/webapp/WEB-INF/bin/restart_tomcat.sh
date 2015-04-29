#!/bin/bash


curr_dir=`dirname $0`


${curr_dir}/stop_tomcat.sh
${curr_dir}/start_tomcat.sh
