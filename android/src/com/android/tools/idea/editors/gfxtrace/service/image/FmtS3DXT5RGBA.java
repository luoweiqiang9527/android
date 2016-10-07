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
package com.android.tools.idea.editors.gfxtrace.service.image;

import org.jetbrains.annotations.NotNull;

import com.android.tools.rpclib.binary.*;
import com.android.tools.rpclib.schema.*;

import java.io.IOException;

final class FmtS3DXT5RGBA extends Format {
  //<<<Start:Java.ClassBody:1>>>

  // Constructs a default-initialized {@link FmtS3DXT5RGBA}.
  public FmtS3DXT5RGBA() {}


  @Override @NotNull
  public BinaryClass klass() { return Klass.INSTANCE; }


  private static final Entity ENTITY = new Entity("image", "fmtS3_DXT5_RGBA", "", "");

  static {
    ENTITY.setFields(new Field[]{
    });
    Namespace.register(Klass.INSTANCE);
  }
  public static void register() {}
  //<<<End:Java.ClassBody:1>>>
  public enum Klass implements BinaryClass {
    //<<<Start:Java.KlassBody:2>>>
    INSTANCE;

    @Override @NotNull
    public Entity entity() { return ENTITY; }

    @Override @NotNull
    public BinaryObject create() { return new FmtS3DXT5RGBA(); }

    @Override
    public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
    }

    @Override
    public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
    }
    //<<<End:Java.KlassBody:2>>>
  }
}
