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

import com.intel.jndn.utils.client.OnComplete;
import com.intel.jndn.utils.client.OnException;
import com.intel.jndn.utils.client.DataStream;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.named_data.jndn.*;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.sync.ChronoSync2013;

/**
 * As packets are received, they are mapped by their last component's segment
 * marker; if the last component is not parseable, an exception is thrown and
 * processing completes. The exception to this is if the first packet returned
 * does not have a segment marker as the last component; in this case, the
 * packet is assumed to be the only packet returned and is placed as the first
 * and only packet to assemble. Observers may register callbacks to watch when
 * data is received; if data is received out of order, the callbacks will not be
 * fired until adjoining packets are received.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SegmentedDataStream implements DataStream {

  private static final Logger logger = Logger.getLogger(SegmentedDataStream.class.getName());
  private final byte PARTITION_MARKER = 0x00;
  private volatile long current = -1;
  private volatile long end = Long.MAX_VALUE;
  private Map<Long, Data> packets = new HashMap<>();
  private List<OnData> observersOnData = new ArrayList<>();
  private List<OnComplete> observersOnComplete = new ArrayList<>();
  private List<OnException> observersOnException = new ArrayList<>();
  private List<OnTimeout> observersOnTimeout = new ArrayList<>();
  private Exception exception;

  @Override
  public boolean isComplete() {
    return current == end || isCompletedExceptionally();
  }

  public boolean isCompletedExceptionally() {
    return exception != null;
  }

  public boolean hasEnd() {
    return end != Long.MAX_VALUE;
  }

  public long end() {
    return end;
  }

  public long current() {
    return current;
  }

  @Override
  public Data[] list() {
    return packets.values().toArray(new Data[]{});
  }

  @Override
  public Data assemble() throws StreamException {
    if (isCompletedExceptionally()) {
      throw new StreamException(exception);
    }

    Object o = new LinkedList();
    return new DataAssembler(list(), PARTITION_MARKER).assemble();
  }

  @Override
  public void observe(OnData onData) {
    observersOnData.add(onData);
  }

  @Override
  public void observe(OnComplete onComplete) {
    observersOnComplete.add(onComplete);
  }

  @Override
  public void observe(OnException onException) {
    observersOnException.add(onException);
  }

  @Override
  public void observe(OnTimeout onTimeout) {
    observersOnTimeout.add(onTimeout);
  }

  @Override
  public synchronized void onData(Interest interest, Data data) {
    logger.info("Data received: " + data.getName().toUri());
    long id;

    // no segment component
    if (!SegmentationHelper.isSegmented(data.getName(), PARTITION_MARKER) && packets.size() == 0) {
      id = 0;
      packets.put(id, data);

      // mark processing complete if the first packet has no segment component
      end = 0;
    } // with segment component
    else {
      Name.Component lastComponent = data.getName().get(-1);
      try {
        id = lastComponent.toNumberWithMarker(PARTITION_MARKER);
        packets.put(id, data);
      } catch (EncodingException ex) {
        onException(ex);
        return;
      }
    }

    if (hasFinalBlockId(data)) {
      try {
        end = data.getMetaInfo().getFinalBlockId().toNumberWithMarker(PARTITION_MARKER);
      } catch (EncodingException ex) {
        onException(ex);
      }
    }

    // call data observers
    if (isNextPacket(id)) {
      do {
        current++;
        assert (packets.containsKey(current));
        final Data retrieved = packets.get(current);
        for (OnData cb : observersOnData) {
          cb.onData(interest, retrieved);
        }
      } while (hasNextPacket());
    }

    // call completion observers
    if (isComplete()) {
      onComplete();
    }
  }

  private boolean hasFinalBlockId(Data data) {
    return data.getMetaInfo().getFinalBlockId().getValue().size() > 0;
  }

  private boolean isNextPacket(long id) {
    return current + 1 == id;
  }

  private boolean hasNextPacket() {
    return packets.containsKey(current + 1);
  }

  @Override
  public synchronized void onComplete() {
    for (OnComplete cb : observersOnComplete) {
      cb.onComplete();
    }
  }

  @Override
  public synchronized void onTimeout(Interest interest) {
    for (OnTimeout cb : observersOnTimeout) {
      cb.onTimeout(interest);
    }
  }

  @Override
  public synchronized void onException(Exception exception) {
    this.exception = exception;

    for (OnException cb : observersOnException) {
      cb.onException(exception);
    }
  }
}
