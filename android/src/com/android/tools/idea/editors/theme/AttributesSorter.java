/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.tools.idea.editors.theme;

import com.intellij.openapi.util.text.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AttributesSorter {
  private AttributesSorter() { }

  /**
   * Helper data structure to hold information for (temporary) algorithm for splitting attributes
   * to labelled group.
   */
  private static class Group {
    /**
     * Group name, as appears on properties panel
     */
    public final String name;
    /**
     * To determine a group for attribute, every marker from this list is sought in
     * attribute name (case-insensitively)
     */
    public final List<String> markers;

    public Group(String name, List<String> markers) {
      this.name = name;
      this.markers = markers;
    }

    public static Group of(String name, String... markers) {
      return new Group(name, Arrays.asList(markers));
    }
  }

  private static final List<Group> GROUPS = Arrays.asList(
    Group.of("Styles", "style", "theme"),
    Group.of("Colors", "color"),
    Group.of("Drawables", "drawable"),
    Group.of("Metrics", "size", "width", "height")
  );

  @SuppressWarnings("unchecked")
  static List<TableLabel> generateLabels(final List<EditedStyleItem> source, final List<EditedStyleItem> sink) {
    final List<EditedStyleItem>[] classes = new List[GROUPS.size() + 1];
    final int otherGroupIndex = GROUPS.size();

    for (int i = 0; i < classes.length; i++) {
      classes[i] = new ArrayList<EditedStyleItem>();
    }

    outer:
    for (final EditedStyleItem item : source) {
      final String name = item.getName();

      for (int index = 0; index < GROUPS.size(); index++) {
        final Group group = GROUPS.get(index);
        for (final String marker : group.markers) {
          if (StringUtil.containsIgnoreCase(name, marker)) {
            classes[index].add(item);
            continue outer;
          }
        }
      }

      // haven't found any group, will put the item into "Other"
      classes[otherGroupIndex].add(item);
    }

    final List<TableLabel> labels = new ArrayList<TableLabel>();
    int offset = 0;
    for (int index = 0; index < GROUPS.size(); index++) {
      final Group group = GROUPS.get(index);
      final int size = classes[index].size();

      if (size != 0) {
        labels.add(new TableLabel(group.name, offset));
      }

      offset += size;
    }

    final int otherGroupSize = classes[otherGroupIndex].size();
    // Adding "Everything else" label only in case when there are at least one other label,
    // because having "Everything else" as the only label present looks quite silly
    if (otherGroupSize != 0 && labels.size() > 0) {
      labels.add(new TableLabel("Everything Else", offset));
    }

    for (final List<EditedStyleItem> list : classes) {
      for (final EditedStyleItem item : list) {
        sink.add(item);
      }
    }

    return labels;
  }
}
