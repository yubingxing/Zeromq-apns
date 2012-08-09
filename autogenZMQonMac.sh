#!/bin/bash
#If on mac os
brew install zeromq pkg-config
sudo ln -s /usr/local/share/aclocal/pkg.m4 /usr/share/aclocal/pkg.m4
export JAVA_HOME=$(/usr/libexec/java_home)

# Clone the github repository for the ZeroMQ Java bindings and build the project
git clone git://github.com/zeromq/jzmq.git
cd jzmq
./autogen.sh && ./configure && make && sudo make install && echo ":: ALL OK ::"
sudo ls -alF /usr/local/lib/*jzmq* /usr/local/share/java/*zmq*

export JAVA_OPTS=-Djava.library.path=/usr/local/lib