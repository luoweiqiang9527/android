/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.idea.run.deployment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class VirtualDevicePathAndSnapshotPath extends Key {
  static final String PREFIX = "VirtualDevicePathAndSnapshotPath@";

  private final @NotNull String myVirtualDevicePath;
  private final @NotNull String mySnapshotPath;

  VirtualDevicePathAndSnapshotPath(@NotNull String virtualDevicePath, @NotNull String snapshotPath) {
    myVirtualDevicePath = virtualDevicePath;
    mySnapshotPath = snapshotPath;
  }

  static @NotNull VirtualDevicePathAndSnapshotPath parse(@NotNull String string) {
    int index = string.indexOf(':');
    return new VirtualDevicePathAndSnapshotPath(string.substring(PREFIX.length(), index), string.substring(index + 1));
  }

  @Override
  @NotNull NonprefixedKey asNonprefixedKey() {
    return new NonprefixedKey(myVirtualDevicePath + ':' + mySnapshotPath);
  }

  @Override
  @NotNull String getDeviceKey() {
    return myVirtualDevicePath;
  }

  @Override
  public int hashCode() {
    return 31 * myVirtualDevicePath.hashCode() + mySnapshotPath.hashCode();
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (!(object instanceof VirtualDevicePathAndSnapshotPath)) {
      return false;
    }

    VirtualDevicePathAndSnapshotPath devicePathAndSnapshotPath = (VirtualDevicePathAndSnapshotPath)object;

    return myVirtualDevicePath.equals(devicePathAndSnapshotPath.myVirtualDevicePath) &&
           mySnapshotPath.equals(devicePathAndSnapshotPath.mySnapshotPath);
  }

  @Override
  public @NotNull String toString() {
    return PREFIX + myVirtualDevicePath + ':' + mySnapshotPath;
  }
}
