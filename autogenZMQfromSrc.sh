#!/bin/bash
sudo apt-get install libtool autoconf automake uuid-dev e2fsprogs
git clone git://github.com/zeromq/libzmq.git
cd libzmq
./autogen.sh && ./configure && make && sudo make install && echo ":: ALL OK ::"
sudo cp src/.libs/libzmq.so /usr/local/lib
sudo ldconfig -v
ls -al /usr/local/lib/libzmq.*
cd ..

# Verify that JAVA_HOME environment variable is correctly set
echo $JAVA_HOME/bin/java
# Clone the github repository for the ZeroMQ Java bindings and build the project
git clone https://github.com/zeromq/jzmq.git
cd jzmq
./autogen.sh && ./configure && make && sudo make install && echo ":: ALL OK ::"
ls -al /usr/local/lib/*jzmq* /usr/local/share/java/*zmq*

export JAVA_OPTS=-Djava.library.path=/usr/local/lib