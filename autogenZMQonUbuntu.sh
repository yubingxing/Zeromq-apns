#!/bin/bash
sudo aptitude install -y python-software-properties
sudo add-apt-repository ppa:webupd8team/java
sudo add-apt-repository ppa:chris-lea/zeromq
sudo aptitude update
sudo aptitude install -y orcale-java7-installer orcale-java7-jdk orcale-java7-jre
sudo aptitude install -y libzmq-dev
sudo aptitude install -y daemon


