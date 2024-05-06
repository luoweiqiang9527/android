#!/bin/sh
#
# Copyright (C) 2022 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# ---------------------------------------------------------------------
# Android Studio startup script.
# ---------------------------------------------------------------------

message()
{
  TITLE="Cannot start Android Studio"
  if [ -n "$(command -v zenity)" ]; then
    zenity --error --title="$TITLE" --text="$1" --no-wrap
  elif [ -n "$(command -v kdialog)" ]; then
    kdialog --error "$1" --title "$TITLE"
  elif [ -n "$(command -v notify-send)" ]; then
    notify-send "ERROR: $TITLE" "$1"
  elif [ -n "$(command -v xmessage)" ]; then
    xmessage -center "ERROR: $TITLE: $1"
  else
    printf "ERROR: %s\n%s\n" "$TITLE" "$1"
  fi
}

if [ -z "$(command -v uname)" ] || [ -z "$(command -v realpath)" ] || [ -z "$(command -v dirname)" ] || [ -z "$(command -v cat)" ] || \
   [ -z "$(command -v egrep)" ]; then
  TOOLS_MSG="Required tools are missing:"
  for tool in uname realpath egrep dirname cat ; do
     test -z "$(command -v $tool)" && TOOLS_MSG="$TOOLS_MSG $tool"
  done
  message "$TOOLS_MSG (SHELL=$SHELL PATH=$PATH)"
  exit 1
fi

# shellcheck disable=SC2034
GREP_OPTIONS=''
OS_TYPE=$(uname -s)
OS_ARCH=$(uname -m)

# ---------------------------------------------------------------------
# Ensure $IDE_HOME points to the directory where the IDE is installed.
# ---------------------------------------------------------------------
IDE_BIN_HOME=$(dirname "$(realpath "$0")")
IDE_HOME=$(dirname "${IDE_BIN_HOME}")
CONFIG_HOME="${XDG_CONFIG_HOME:-${HOME}/.config}"

# ---------------------------------------------------------------------
# Locate a JRE installation directory command -v will be used to run the IDE.
# Try (in order): $STUDIO_JDK, .../studio.jdk, .../jbr, $JDK_HOME, $JAVA_HOME, "java" in $PATH.
# ---------------------------------------------------------------------
JRE="$IDE_HOME/jbr"
JAVA_HOME=""
STUDIO_JDK=""
JDK_HOME=""
JAVA_BIN="$JRE/bin/java"
if [ -z "$JAVA_BIN" ] || [ ! -x "$JAVA_BIN" ]; then
  message "ERROR: cannot start Android Studio. \nNo JRE found. Please make sure \$STUDIO_JDK, \$JDK_HOME, or \$JAVA_HOME point to valid JRE installation."
  exit 1
fi

# Collect JVM options and IDE properties.
# ---------------------------------------------------------------------
IDE_PROPERTIES_PROPERTY=""
# shellcheck disable=SC2154
if [ -n "$STUDIO_PROPERTIES" ]; then
  IDE_PROPERTIES_PROPERTY="-Didea.properties.file=$STUDIO_PROPERTIES"
fi

VM_OPTIONS_FILE="${IDE_BIN_HOME}/studio64.vmoptions"
VM_OPTIONS_FILE=""
USER_VM_OPTIONS_FILE=""

STUDIO_BIN="${IDE_BIN_HOME}/studio.sh"
STUDIO_VERSION=$(cat ../product-info.json | grep AndroidStudio | awk '{ print $2}' | sed 's/"//' | sed 's/"//' | sed 's/,//')
STUDIO_CONFIG_DIR=${CONFIG_HOME}/Google/${STUDIO_VERSION}
if [ ! -d "${STUDIO_CONFIG_DIR}" ]; then
  # Android Studio config is not set up
  echo "Android Studio config doesn't exist ${STUDIO_CONFIG_DIR} "
  ${STUDIO_BIN} disableNonBundledPlugins dontReopenProjects
  exit
fi

STUDIO_VERSION_SAFE="${STUDIO_VERSION}.safe"
STUDIO_SAFE_CONFIG_DIR=${CONFIG_HOME}/Google/${STUDIO_VERSION_SAFE}
if [ ! -d “${STUDIO_SAFE_CONFIG_DIR}” ]; then
  mkdir “${STUDIO_SAFE_CONFIG_DIR}”
fi

cp -R "${STUDIO_CONFIG_DIR}" "${STUDIO_SAFE_CONFIG_DIR}"
rm -rf ${STUDIO_SAFE_CONFIG_DIR}/idea.properties
rm -rf ${STUDIO_SAFE_CONFIG_DIR}/studio64.vmoptions

CLASS_PATH="$IDE_HOME/lib/platform-loader.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/intellij-coverage-agent-1.0.723.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/lib.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/util.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/grpc.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/rd.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/util-8.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/app.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/jps-model.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/stats.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/protobuf.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/external-system-rt.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/forms_rt.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/intellij-test-discovery.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/annotations.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/groovy.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/annotations-java5.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/byte-buddy-agent.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/error-prone-annotations.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/externalProcess-rt.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/idea_rt.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/junit4.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/bouncy-castle.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/resources.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/util_rt.jar"
CLASS_PATH="$CLASS_PATH:$IDE_HOME/lib/ant/lib/ant.jar"

# ---------------------------------------------------------------------
# Run the IDE.
# ---------------------------------------------------------------------
# shellcheck disable=SC2086
exec "$JAVA_BIN" \
  -classpath "$CLASS_PATH" \
  ${VM_OPTIONS} \
  "-XX:ErrorFile=$HOME/java_error_in_studio_%p.log" \
  "-XX:HeapDumpPath=$HOME/java_error_in_studio_.hprof" \
  "-Djb.vmOptionsFile=${USER_VM_OPTIONS_FILE:-${VM_OPTIONS_FILE}}" \
  ${IDE_PROPERTIES_PROPERTY} \
  -Djava.system.class.loader=com.intellij.util.lang.PathClassLoader -Didea.strict.classpath=true -Didea.vendor.name=Google -Didea.paths.selector="${STUDIO_VERSION_SAFE}" -Didea.platform.prefix=AndroidStudio -XX:FlightRecorderOptions=stackdepth=256 -Didea.jre.check=true -Dsplash=true --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.nio.charset=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED --add-opens=java.base/jdk.internal.vm=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.security.ssl=ALL-UNNAMED --add-opens=java.base/sun.security.util=ALL-UNNAMED --add-opens=java.desktop/com.sun.java.swing.plaf.gtk=ALL-UNNAMED --add-opens=java.desktop/java.awt=ALL-UNNAMED --add-opens=java.desktop/java.awt.dnd.peer=ALL-UNNAMED --add-opens=java.desktop/java.awt.event=ALL-UNNAMED --add-opens=java.desktop/java.awt.image=ALL-UNNAMED --add-opens=java.desktop/java.awt.peer=ALL-UNNAMED --add-opens=java.desktop/javax.swing=ALL-UNNAMED --add-opens=java.desktop/javax.swing.plaf.basic=ALL-UNNAMED --add-opens=java.desktop/javax.swing.text=ALL-UNNAMED --add-opens=java.desktop/javax.swing.text.html=ALL-UNNAMED --add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED --add-opens=java.desktop/sun.awt.datatransfer=ALL-UNNAMED --add-opens=java.desktop/sun.awt.image=ALL-UNNAMED --add-opens=java.desktop/sun.awt=ALL-UNNAMED --add-opens=java.desktop/sun.font=ALL-UNNAMED --add-opens=java.desktop/sun.java2d=ALL-UNNAMED --add-opens=java.desktop/sun.swing=ALL-UNNAMED --add-opens=jdk.attach/sun.tools.attach=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED --add-opens=jdk.jdi/com.sun.tools.jdi=ALL-UNNAMED \
  com.intellij.idea.Main \
  "$@" disableNonBundledPlugins dontReopenProjects

