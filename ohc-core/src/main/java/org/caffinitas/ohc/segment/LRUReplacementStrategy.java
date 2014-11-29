/*
 *      Copyright (C) 2014 Robert Stupp, Koeln, Germany, robert-stupp.de
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.caffinitas.ohc.segment;

final class LRUReplacementStrategy implements ReplacementStrategy
{
    private volatile long head;
    private volatile long tail;

    private void removeLink(long hashEntryAdr)
    {
        long next = lruNext(hashEntryAdr);
        long prev = lruPrev(hashEntryAdr);

        if (head == hashEntryAdr)
            head = next;
        if (tail == hashEntryAdr)
            tail = prev;

        if (next != 0L)
            lruPrev(next, prev);
        if (prev != 0L)
            lruNext(prev, next);
    }

    private void addLink(long hashEntryAdr)
    {
        long h = head;
        lruNext(hashEntryAdr, h);
        if (h != 0L)
            lruPrev(h, hashEntryAdr);
        lruPrev(hashEntryAdr, 0L);
        head = hashEntryAdr;

        if (tail == 0L)
            tail = hashEntryAdr;
    }

    private long lruNext(long hashEntryAdr)
    {
        return HashEntries.getEntryReplacement0(hashEntryAdr);
    }

    private long lruPrev(long hashEntryAdr)
    {
        return HashEntries.getEntryReplacement1(hashEntryAdr);
    }

    private void lruNext(long hashEntryAdr, long next)
    {
        HashEntries.setEntryReplacement0(hashEntryAdr, next);
    }

    private void lruPrev(long hashEntryAdr, long prev)
    {
        HashEntries.setEntryReplacement1(hashEntryAdr, prev);
    }

    public void entryUsed(long hashEntryAdr)
    {
        removeLink(hashEntryAdr);
        addLink(hashEntryAdr);
    }

    public void entryReplaced(long oldHashEntryAdr, long hashEntryAdr)
    {
        if (oldHashEntryAdr != 0L)
            removeLink(oldHashEntryAdr);
        addLink(hashEntryAdr);
    }

    public void entryRemoved(long hashEntryAdr)
    {
        removeLink(hashEntryAdr);
    }

    public long cleanUp(DataMemory dataMemory, long recycleGoal)
    {
        long prev;
        long evicted = 0L;
        for (long hashEntryAdr = tail;
             hashEntryAdr != 0L && recycleGoal > 0L;
             hashEntryAdr = prev)
        {
            prev = lruPrev(hashEntryAdr);

            long bytes = DataMemory.getEntryBytes(hashEntryAdr);
            removeLink(hashEntryAdr);

            HashEntries.awaitEntryUnreferenced(hashEntryAdr);
            dataMemory.free(hashEntryAdr, false);

            recycleGoal -= bytes;

            evicted ++;
        }

        return evicted;
    }
}
