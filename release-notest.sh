#!/bin/bash

export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.7.0_72.jdk/Contents/Home

mvn -Dmaven.test.skip=true -DperformRelease=true clean install
