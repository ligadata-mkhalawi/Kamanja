 #!/usr/bin/env bash
###################################################################
#
#  Copyright 2015 ligaDATA
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
###################################################################

Today=`date +"%Y%m%d"`
cd $KAMANJA_HOME/python/logs
for pid in `ls pythonserver* | sed 's/pythonserver.log//g'`; do
    if [ ! -z "$pid" -a "$pid" != "" ]; then
        kill -9 $pid
        mkdir -p ${KAMANJA_HOME}/python/logs/${Today}
        mv ${KAMANJA_HOME}/python/logs/pythonserver.log${pid}* ${KAMANJA_HOME}/python/logs/${Today}
        echo "$pid  is killed"
    fi
done

for pid in `ps -ef | grep -i python  | grep "pythonserver.py" | grep 'localhost' |  tr -s " " | cut -f2 -d" "`; do
    if [ ! -z "$pid" -a "$pid" != "" ]; then
        kill -9 $pid
        mkdir -p ${KAMANJA_HOME}/python/logs/${Today}
        mv ${KAMANJA_HOME}/python/logs/pythonserver.log${pid}* ${KAMANJA_HOME}/python/logs/${Today}
        echo "$pid  is killed"
    fi
done
