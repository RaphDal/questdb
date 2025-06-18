/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2024 QuestDB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package io.questdb.std;

import io.questdb.cairo.vm.MemoryPARWImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;


public class NonGCCharSequenceIntHashMap extends AbstractOffsetCharSequenceHashSet {
    public static final int NO_ENTRY_VALUE = -1;
    private final int noEntryValue;
    private int[] values;
    private final MemoryPARWImpl memory;
    private long currentOffset = 0;

    public NonGCCharSequenceIntHashMap() {
        this(8);
    }

    public NonGCCharSequenceIntHashMap(int initialCapacity) {
        this(initialCapacity, 0.4, NO_ENTRY_VALUE);
    }

    public NonGCCharSequenceIntHashMap(int initialCapacity, double loadFactor, int noEntryValue) {
        super(initialCapacity, loadFactor);
        this.noEntryValue = noEntryValue;
        this.memory = new MemoryPARWImpl(1024 * 1024L, Integer.MAX_VALUE, MemoryTag.NATIVE_DEFAULT);
        values = new int[offsets.length];
        clear();
    }

    @Override
    public final void clear() {
        super.clear();
        Arrays.fill(values, noEntryValue);
        this.memory.clear();
        this.currentOffset = 0;
    }

    public int get(@NotNull CharSequence key) {
        return valueAt(keyIndex(key));
    }

    public boolean put(@NotNull CharSequence key, int value) {
        return putAt(keyIndex(key), key, value);
    }

    public void putAll(@NotNull NonGCCharSequenceIntHashMap other) {
        long[] otherOffsets = other.offsets;
        int[] otherValues = other.values;
        for (int i = 0, n = otherOffsets.length; i < n; i++) {
            if (otherOffsets[i] != noEntryOffset) {
                put(other.getCharSequence(otherOffsets[i]), otherValues[i]);
            }
        }
    }

    public boolean putAt(int index, @NotNull CharSequence key, int value) {
        if (index < 0) {
            values[-index - 1] = value;
            return false;
        }

        final long offset = this.writeKey(key);
        putAt0(index, offset, value);
        return true;
    }

    public void putIfAbsent(@NotNull CharSequence key, int value) {
        int index = keyIndex(key);
        if (index > -1) {
            long offset = this.writeKey(key);
            putAt0(index, offset, value);
        }
    }

    private long writeKey(CharSequence key) {
        this.memory.putStr(currentOffset, key);

        final long oldOffset = currentOffset;
        currentOffset += (key.length() << 1) + 4;

        return oldOffset;
    }

    public int valueAt(int index) {
        int index1 = -index - 1;
        return index < 0 ? values[index1] : noEntryValue;
    }

    private void putAt0(int index, long offset, int value) {
        offsets[index] = offset;
        values[index] = value;
        if (--free == 0) {
            rehash();
        }
    }

    public void rehash() {
        int[] oldValues = values;
        long[] oldOffsets = offsets;
        int size = capacity - free;
        capacity = capacity * 2;
        free = capacity - size;
        mask = Numbers.ceilPow2((int) (capacity / loadFactor)) - 1;
        this.offsets = new long[mask + 1];
        Arrays.fill(offsets, noEntryOffset);
        this.values = new int[mask + 1];
        for (int i = oldOffsets.length - 1; i > -1; i--) {
            long offset = oldOffsets[i];
            if (offset != noEntryOffset) {
                CharSequence key = memory.getStrB(offset);
                final int index = keyIndex(key);
                offsets[index] = offset;
                values[index] = oldValues[i];
            }
        }
    }

    @Override
    protected CharSequence getCharSequence(long offset) {
        return memory.getStrA(offset);
    }
}