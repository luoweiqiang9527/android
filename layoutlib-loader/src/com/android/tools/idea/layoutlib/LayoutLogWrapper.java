/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.idea.layoutlib;

import com.android.annotations.NonNull;
import com.android.ide.common.rendering.api.ILayoutLog;
import com.android.tools.environment.Logger;

public class LayoutLogWrapper implements ILayoutLog {
  private final Logger myLog;

  public LayoutLogWrapper(@NonNull Logger log) {
    myLog = log;
  }

  @Override
  public void warning(String tag, String message, Object viewCookie, Object data) {
    myLog.warn(message);
  }

  @Override
  public void fidelityWarning(String tag, String message, Throwable throwable, Object viewCookie, Object data) {
    myLog.warn(message);
  }
  @Override
  public void error(String tag, String message, Object viewCookie, Object data) {
    myLog.error(message);
  }

  @Override
  public void error(String tag, String message, Throwable throwable, Object viewCookie, Object data) {
    myLog.error(message, throwable);
  }

  @Override
  public void logAndroidFramework(int priority, String tag, String message) {
    myLog.warn(tag + ": " + message);
  }
}
