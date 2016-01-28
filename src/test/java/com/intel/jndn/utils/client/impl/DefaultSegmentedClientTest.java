/*
 * jndn-utils
 * Copyright (c) 2015, Intel Corporation.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the GNU Lesser General Public License,
 * version 3, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 */
package com.intel.jndn.utils.client.impl;

import com.intel.jndn.mock.MockFace;
import com.intel.jndn.utils.TestHelper;
import com.intel.jndn.utils.client.DataStream;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Test DefaultSegmentedClient
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class DefaultSegmentedClientTest {

  DefaultSegmentedClient instance;
  MockFace face;
  int counter;

  @Before
  public void setUp() throws Exception {
    instance = new DefaultSegmentedClient();
    face = new MockFace();
    counter = 0;
  }

  @Test
  public void testGetSegmentsAsync() throws Exception {
    Name name = new Name("/test/segmented/client");
    Interest interest = new Interest(name);
    DataStream stream = instance.getSegmentsAsync(face, interest);

    stream.observe(new OnData() {
      @Override
      public void onData(Interest interest, Data data) {
        counter++;
      }
    });

    for (Data segment : TestHelper.buildSegments(name, 0, 5)) {
      stream.onData(interest, segment);
    }

    assertEquals(5, counter);
    assertEquals("01234", stream.assemble().getContent().toString());
  }

  @Test
  public void testReplacingFinalComponents() throws Exception {
    long segmentNumber = 99;

    Name name1 = new Name("/a/b/c");
    Interest interest1 = new Interest(name1);
    Interest copied1 = instance.replaceFinalComponent(interest1, segmentNumber, (byte) 0x00);
    assertEquals(segmentNumber, copied1.getName().get(-1).toSegment());

    Name name2 = new Name("/a/b/c").appendSegment(17);
    Interest interest2 = new Interest(name2);
    Interest copied2 = instance.replaceFinalComponent(interest2, segmentNumber, (byte) 0x00);
    assertEquals(segmentNumber, copied2.getName().get(-1).toSegment());

    assertEquals(copied1.toUri(), copied2.toUri());
  }

  @Test
  public void verifyThatSegmentsAreRetrievedOnlyOnce() throws Exception {
    Name name = new Name("/test/segmented/client");
    Interest interest = new Interest(name);
    DataStream stream = instance.getSegmentsAsync(face, interest);

    stream.observe(new OnData() {
      @Override
      public void onData(Interest interest, Data data) {
        counter++;
      }
    });

    for (Data segment : TestHelper.buildSegments(name, 0, 5)) {
      face.receive(segment);
    }

    TestHelper.run(face, 10);

    assertEquals(5, counter);
    assertEquals(5, face.sentInterests.size());
    assertEquals("01234", stream.assemble().getContent().toString());
  }
}
