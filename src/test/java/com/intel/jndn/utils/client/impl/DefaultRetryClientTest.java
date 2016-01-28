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
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;
import static org.junit.Assert.*;

import net.named_data.jndn.security.SecurityException;
import org.junit.Before;
import org.junit.Test;

/**
 * Test DefaultRetryClient
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class DefaultRetryClientTest {
  DefaultRetryClient client;
  MockFace face;
  int nData;
  int nTimeouts;

  @Before
  public void setUp() throws Exception {
    face = new MockFace();
    client = new DefaultRetryClient(3);
    nData = 0;
    nTimeouts = 0;

    client.retry(face, new Interest(new Name("/test/retry/client"), 10), new OnData() {
      @Override
      public void onData(Interest interest, Data data) {
        nData++;
      }
    }, new OnTimeout() {
      @Override
      public void onTimeout(Interest interest) {
        nTimeouts++;
      }
    });
  }

  @Test
  public void MaxRetries() throws Exception {
    TestHelper.run(face, 10);
    assertEquals(3, client.totalRetries());
    assertEquals(3, face.sentInterests.size());
    assertEquals(0, nData);
    assertEquals(1, nTimeouts);
  }

  @Test
  public void RetryAndSuccess() throws Exception {
    TestHelper.run(face, 1);
    assertEquals(1, client.totalRetries());
    assertEquals(1, face.sentInterests.size());
    assertEquals(0, nData);
    assertEquals(0, nTimeouts);

    TestHelper.run(face, 1);
    assertEquals(2, client.totalRetries());
    assertEquals(2, face.sentInterests.size());
    assertEquals(0, nData);
    assertEquals(0, nTimeouts);

    face.onSendInterest.add(new MockFace.SignalOnSendInterest() {
      @Override
      public void emit(Interest interest) throws EncodingException, SecurityException {
        face.receive(new Data(new Name("/test/retry/client")));
        face.onSendInterest.clear();
      }
    });

    TestHelper.run(face, 5);
    assertEquals(3, face.sentInterests.size());
    assertEquals(2, client.totalRetries());
    assertEquals(1, nData);
    assertEquals(0, nTimeouts);
  }
}
