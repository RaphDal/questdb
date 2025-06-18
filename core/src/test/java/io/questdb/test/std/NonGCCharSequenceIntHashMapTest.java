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

package io.questdb.test.std;

import io.questdb.log.Log;
import io.questdb.log.LogFactory;
import io.questdb.std.NonGCCharSequenceIntHashMap;
import io.questdb.std.Rnd;
import io.questdb.test.tools.TestUtils;
import org.junit.Assert;
import org.junit.Test;

public class NonGCCharSequenceIntHashMapTest {
    private static final Log LOG = LogFactory.getLog(IntLongSortedListTest.class);

    @Test
    public void testBasic() {
        final int N = 10000;
        final NonGCCharSequenceIntHashMap map = new NonGCCharSequenceIntHashMap();
        final Rnd rnd = TestUtils.generateRandom(LOG);

        for (int i = 0; i < N; i++) {
            final CharSequence key = rnd.nextChars(8);
            final int value = rnd.nextInt();
            map.put(key, value);
            Assert.assertEquals(value, map.get(key));
        }

        map.clear();

        Assert.assertEquals(0, map.size());
    }

    @Test
    public void testConflictingKeys() {
        final NonGCCharSequenceIntHashMap map = new NonGCCharSequenceIntHashMap();

        Assert.assertTrue(map.put("foo", 1));
        Assert.assertFalse(map.put("foo", 2));

        Assert.assertEquals(2, map.get("foo"));
    }
}
