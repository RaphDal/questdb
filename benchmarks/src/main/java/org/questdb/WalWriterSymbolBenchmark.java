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

package org.questdb;

import io.questdb.cairo.sql.SymbolTable;
import io.questdb.std.CharSequenceIntHashMap;
import io.questdb.std.NonGCCharSequenceIntHashMap;
import io.questdb.std.Rnd;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
// This benchmark compares the speed of some hash maps implementation for the Wal Writer Symbols map.
// In this use case, we are only appending new keys to the map, and at tx completion we are clearing the whole
// map (thus the .clear in each Invocation).
public class WalWriterSymbolBenchmark {
    private static final double loadFactor = 0.7;
    private static final int symbolSize = 1024;
    private static final CharSequenceIntHashMap hmap = new CharSequenceIntHashMap(1024, loadFactor, SymbolTable.VALUE_NOT_FOUND);
    private static final NonGCCharSequenceIntHashMap offHeap = new NonGCCharSequenceIntHashMap(1024, loadFactor, SymbolTable.VALUE_NOT_FOUND);
    private final Rnd rnd = new Rnd();

    @Param({"10", "20", "100"})
    public int size;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(WalWriterSymbolBenchmark.class.getSimpleName())
                .warmupIterations(2)
                .measurementIterations(3)
                .forks(0)
                .addProfiler(GCProfiler.class)
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Invocation)
    public void reset() {
        rnd.reset();

        hmap.clear();
        offHeap.clear();
    }

    @Benchmark
    public void CharSequenceIntHashMap() {
        for (int i = 0; i < size; i++) {
            hmap.put(rnd.nextChars(symbolSize), rnd.nextInt());
        }
    }

    @Benchmark
    public void NonGCCharSequenceIntHashMap() {
        for (int i = 0; i < size; i++) {
            offHeap.put(rnd.nextChars(symbolSize), rnd.nextInt());
        }
    }
}
