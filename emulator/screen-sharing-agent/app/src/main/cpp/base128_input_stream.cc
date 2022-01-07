/*
 * Copyright (C) 2021 The Android Open Source Project
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

#include "base128_input_stream.h"

#include <unistd.h>

namespace screensharing {

using namespace std;

Base128InputStream::Base128InputStream(int fd, size_t buffer_size)
    : fd_(fd),
      buffer_(new uint8_t[buffer_size]),
      buffer_capacity_(buffer_size),
      offset_(0),
      data_end_(0) {
}

Base128InputStream::~Base128InputStream() {
  delete [] buffer_;
}

int Base128InputStream::Close() {
  return close(fd_);
}

uint8_t Base128InputStream::ReadByte() {
  if (offset_ == data_end_) {
    auto n = read(fd_, buffer_, buffer_capacity_);
    if (n < 0) {
      throw IoException();
    } else if (n == 0) {
      throw EndOfFile();
    }
    offset_ = 0;
    data_end_ = static_cast<size_t>(n);
  }
  return buffer_[offset_++];
}

int16_t Base128InputStream::ReadInt16() {
  int b = ReadByte();
  int value = b & 0x7F;
  try {
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      b = ReadByte();
      if (shift == 21 && (b & 0xFC) != 0) {
        throw StreamFormatException::InvalidFormat();
      }
      value |= (b & 0x7F) << shift;
    }
  } catch (EndOfFile& e) {
    throw StreamFormatException::PrematureEndOfStream();
  }
  return static_cast<int16_t>(value);
}

int32_t Base128InputStream::ReadInt32() {
  int b = ReadByte();
  int value = b & 0x7F;
  try {
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      b = ReadByte();
      if (shift == 28 && (b & 0xF0) != 0) {
        throw StreamFormatException::InvalidFormat();
      }
      value |= (b & 0x7F) << shift;
    }
  } catch (EndOfFile& e) {
    throw StreamFormatException::PrematureEndOfStream();
  }
  return value;
}

int64_t Base128InputStream::ReadInt64() {
  int b = ReadByte();
  long value = b & 0x7F;
  try {
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      b = ReadByte();
      if (shift == 63 && (b & 0x7E) != 0) {
        throw StreamFormatException::InvalidFormat();
      }
      value |= ((long) (b & 0x7F)) << shift;
    }
  } catch (EndOfFile& e) {
    throw StreamFormatException::PrematureEndOfStream();
  }

  return value;
}

bool Base128InputStream::ReadBool() {
  int c = ReadByte();
  if ((c & ~0x1) != 0) {
    throw StreamFormatException::InvalidFormat();
  }
  return c != 0;
}

u16string Base128InputStream::ReadString16() {
  int len = ReadInt32();
  if (len < 0) {
    throw StreamFormatException::InvalidFormat();
  }
  if (len == 0) {
    return nullptr;
  }
  --len;
  if (len == 0) {
    return u16string();
  }
  auto result = u16string(static_cast<unsigned int>(len), '\0');
  try {
    for (int i = 0; i < len; i++) {
      result[i] = ReadUInt16();
    }
  } catch (EndOfFile& e) {
    throw StreamFormatException::PrematureEndOfStream();
  }
  return result;
}

Base128InputStream::StreamFormatException Base128InputStream::StreamFormatException::PrematureEndOfStream() {
  return StreamFormatException("Premature end of stream");
}

Base128InputStream::StreamFormatException Base128InputStream::StreamFormatException::InvalidFormat() {
  return StreamFormatException("Invalid file format");
}

}  // namespace screensharing
