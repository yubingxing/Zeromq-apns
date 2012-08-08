#!/bin/bash
sudo add-apt-repository ppa:chris-lea/zeromq
sudo aptitude install libzmq-dev

PID_FILE=/var/run/iphonenotifier.pid

mydir="`dirname $0`"
mylib="`dirname $mydir`"/lib

libs=`echo "$mylib"/*.jar "$mydir"/conf | sed 's/ /:/g'`

daemon \
  -n iphonenotifier \
  java -classpath $libs com.notnoop.notifier.Boot "$@"
