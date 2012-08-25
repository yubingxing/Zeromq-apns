#!/bin/bash
#sudo add-apt-repository ppa:chris-lea/zeromq
#sudo aptitude install libzmq-dev

PID_FILE=/var/run/zeromq-apns.pid

kill -9 `ps aux | grep com.icestar.Server | grep -v grep | awk '{print $2}'`

mydir="`dirname $0`"
mylib="`dirname $mydir`"/lib
echo mydir=$mydir 
echo mylib=$mylib

libs=`echo "$mylib"/*.jar "$mydir"/conf | sed 's/ /:/g'`
echo libs=$libs

daemon \
  -n zmq-apnserver \
  `java -server -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=1536m -Xmx1024M -Xss4M -classpath $libs com.icestar.Server "$@"` &
