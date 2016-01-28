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
import net.named_data.jndn.security.SecurityException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;
import org.junit.rules.ExpectedException;

/**
 * Test SimpleClient.java
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SimpleClientTest {

  private static final Logger logger = Logger.getLogger(SimpleClient.class.getName());

  MockFace face;
  SimpleClient instance;

  @Before
  public void init() throws SecurityException {
    face = new MockFace();
    instance = new SimpleClient();
  }

  @Test
  public void GetAsync() throws Exception {
    Name name = new Name("/test/simple/client/async");
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
    assertEquals(1, face.sentInterests.size());
    assertEquals("...", future.get().getContent().toString());
  }

  @Test
  public void GetAsyncNoData() throws Exception {
    Name name = new Name("/test/simple/client/async/timeout");
    Interest interest = new Interest(name, 100);

    final CompletableFuture<Data> future = instance.getAsync(face, interest);

    TestHelper.run(face, 20, new TestHelper.Tester() {
      @Override
      public boolean test() {
        return !future.isDone();
      }
    });

    assertTrue(future.isCompletedExceptionally());
  }

  @Test
  public void GetSync() throws Exception {
    final Name name = new Name("/test/simple/client/sync");

    TestHelper.addDataPublisher(face, -1);

    Data data = instance.getSync(face, name);
    assertEquals("...", data.getContent().toString());
    assertEquals(1, face.sentInterests.size());
  }

//  @Test(expected = Exception.class)
//  public void testAsyncFailureToRetrieve() throws Exception {
//    logger.info("Client expressing interest asynchronously: /test/no-data");
//    Interest interest = new Interest(new Name("/test/no-data"), 10);
//    Future future = SimpleClient.getDefault().getAsync(face, interest);
//
//    face.processEvents();
//    future.get(15, TimeUnit.MILLISECONDS);
//  }
//
//  @Test(expected = IOException.class)
//  public void testSyncFailureToRetrieve() throws IOException {
//    logger.info("Client expressing interest synchronously: /test/no-data");
//    Interest interest = new Interest(new Name("/test/no-data"), 10);
//    SimpleClient.getDefault().getSync(new Face(), interest);
//  }
}
