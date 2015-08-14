/*
 * Copyright (C) 2015 The Android Open Source Project
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
 *
 * THIS FILE WAS GENERATED BY codergen. EDIT WITH CARE.
 */
package com.android.tools.idea.editors.gfxtrace.service.memory;

import org.jetbrains.annotations.NotNull;
import com.android.tools.rpclib.binary.Decoder;
import com.android.tools.rpclib.binary.Encoder;
import java.io.IOException;

public enum PoolID {
  ApplicationPool(0);

  private final int myValue;
  PoolID(int value) {
    myValue = value;
  }
  public int getValue() { return myValue; }

  public void encode(@NotNull Encoder e) throws IOException {
    e.uint32(myValue);
  }

  public static PoolID decode(@NotNull Decoder d) throws IOException {
    int value = d.uint32();
    switch (value) {
    case 0:
      return ApplicationPool;
    }
    throw new IOException("Invalid value for PoolID");
  }
}
