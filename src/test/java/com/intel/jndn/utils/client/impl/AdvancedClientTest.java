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

import java.sql.Time;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.intel.jndn.mock.MockFace;
import com.intel.jndn.utils.TestHelper;
import net.named_data.jndn.*;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.SecurityException;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class AdvancedClientTest {

  private static final Logger logger = Logger.getLogger(AdvancedClientTest.class.getName());

  MockFace face;
  AdvancedClient instance;
  DefaultRetryClient retryClient;

  @Before
  public void init() throws SecurityException {
    face = new MockFace();
    retryClient = new DefaultRetryClient(5);
    assertEquals(0, retryClient.totalRetries());
    instance = new AdvancedClient(500, 2000, new DefaultSegmentedClient(), retryClient, new DefaultStreamingClient());
  }

  @Test
  public void GetAsyncBasic() throws Exception {
    Name name = new Name("/test/advanced/client");
    Interest interest = new Interest(name, 2000);

    TestHelper.addDataPublisher(face, -1);

    final CompletableFuture<Data> future = instance.getAsync(face, interest);

    TestHelper.run(face, 20, new TestHelper.Tester() {
      @Override
      public boolean test() {
        return !future.isDone();
      }
    });

    assertFalse(future.isCompletedExceptionally());
    assertEquals(0, retryClient.totalRetries());
    assertEquals(1, face.sentInterests.size());
    assertEquals("...", future.get().getContent().toString());
  }

  @Test
  public void GetAsyncSegmented() throws Exception {
    Name name = new Name("/test/advanced/client").appendSegment(0);
    Interest interest = new Interest(name, 2000);

    TestHelper.addDataPublisher(face, 9);

    final CompletableFuture<Data> future = instance.getAsync(face, interest);

    TestHelper.run(face, 20, new TestHelper.Tester() {
      @Override
      public boolean test() {
        return !future.isDone();
      }
    });

    assertFalse(future.isCompletedExceptionally());
    assertEquals(0, retryClient.totalRetries());
    assertEquals(10, face.sentInterests.size());
    assertEquals("..............................", future.get().getContent().toString());
  }

  @Test
  public void GetAsyncBasicNoData() throws Exception {
    Name name = new Name("/test/advanced/client");
    Interest interest = new Interest(name, 100);

    final CompletableFuture<Data> future = instance.getAsync(face, interest);

    TestHelper.run(face, 20, new TestHelper.Tester() {
      @Override
      public boolean test() {
        return !future.isDone();
      }
    });

    assertTrue(future.isCompletedExceptionally());
    assertEquals(5, retryClient.totalRetries());
    assertNotEquals(face.sentInterests.get(0).getNonce(), face.sentInterests.get(1).getNonce());
    assertEquals(6, face.sentInterests.size()); // original interest and 5 retries
  }

  @Test
  public void GetSyncBasic() throws Exception {
    final Name name = new Name("/segmented/data");

    TestHelper.addDataPublisher(face, -1);

    Data data = instance.getSync(face, name);
    assertEquals("...", data.getContent().toString());
    assertEquals(1, face.sentInterests.size());
  }

  @Test
  public void GetSyncSegmented() throws Exception {
    final Name name = new Name("/segmented/data").appendSegment(0);

    TestHelper.addDataPublisher(face, 9);

    Data data = instance.getSync(face, name);
    assertEquals(10, face.sentInterests.size());
    assertEquals("..............................", data.getContent().toString());
  }

  /**
   * Verify that Data returned with a different Name than the Interest is still
   * segmented correctly.
   */
  @Test
  public void DataNameIsLongerThanInterestName() throws Exception {
    for (int i = 0; i < 10; i++) {
      face.receive(TestHelper.buildData(new Name("/a/b/c/d").appendSegment(i), "...", 9));
    }

    Data data = instance.getSync(face, new Name("/a/b"));
    assertNotNull(data);
    assertEquals("/a/b/c/d", data.getName().toUri());
    assertEquals(10, face.sentInterests.size());
    assertEquals("..............................", data.getContent().toString());
  }

  // @TODO This needs to be fixed in AdvancedClient
   @Test(expected = AssertionError.class)
   public void UnorderedSegments() throws Exception {
     for (int i = 9; i >= 0; i--) {
       face.receive(TestHelper.buildData(new Name("/a/b/c/d").appendSegment(i), "...", 9));
     }

     final CompletableFuture<Data> future = instance.getAsync(face, new Name("/a/b"));

     TestHelper.run(face, 20, new TestHelper.Tester() {
       @Override
       public boolean test() {
         return !future.isDone();
       }
     });

     assertTrue(future.isCompletedExceptionally());
     assertNotNull(future.get());
     assertEquals("/a/b/c/d", future.get().getName().toUri());
     assertEquals(10, face.sentInterests.size());
     assertEquals("..............................", future.get().getContent().toString());
   }

   /**
    * Verify that Data packets with no content do not cause errors
    */
   @Test
   public void GetSyncDataNoContent() throws Exception {
     Name name = new Name("/test/no-content").appendSegment(0);
     face.receive(TestHelper.buildData(name, "", 0));

     Data data = instance.getSync(face, name);

     assertEquals("/test/no-content", data.getName().toUri());
     assertEquals("", data.getContent().toString());
   }
}
