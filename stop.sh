#!/bin/bash
sudo kill -9 `ps aux | grep com.icestar.Server | grep -v grep | awk '{print $2}'`