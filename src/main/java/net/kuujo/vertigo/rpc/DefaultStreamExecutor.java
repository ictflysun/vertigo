/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kuujo.vertigo.rpc;

import net.kuujo.vertigo.context.InstanceContext;
import net.kuujo.vertigo.message.JsonMessage;
import net.kuujo.vertigo.message.MessageId;
import net.kuujo.vertigo.runtime.FailureException;
import net.kuujo.vertigo.runtime.TimeoutException;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.impl.DefaultFutureResult;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

/**
 * A default stream executor implementation.
 *
 * @author Jordan Halterman
 */
public class DefaultStreamExecutor extends AbstractExecutor<StreamExecutor> implements StreamExecutor {
  private Handler<Void> drainHandler;
  private boolean paused;

  private static final FailureException FAILURE_EXCEPTION = new FailureException("Processing failed.");
  static { FAILURE_EXCEPTION.setStackTrace(new StackTraceElement[0]); }

  private static final TimeoutException TIMEOUT_EXCEPTION = new TimeoutException("Processing timed out.");
  static { TIMEOUT_EXCEPTION.setStackTrace(new StackTraceElement[0]); }

  public DefaultStreamExecutor(Vertx vertx, Container container, InstanceContext context) {
    super(vertx, container, context);
  }

  @Override
  public boolean queueFull() {
    return paused;
  }

  @Override
  public StreamExecutor drainHandler(Handler<Void> handler) {
    drainHandler = handler;
    return this;
  }

  @Override
  public MessageId execute(JsonObject args, Handler<AsyncResult<JsonMessage>> resultHandler) {
    return execute(args, null, resultHandler);
  }

  @Override
  public MessageId execute(JsonObject args, String tag, Handler<AsyncResult<JsonMessage>> resultHandler) {
    final Future<JsonMessage> future = new DefaultFutureResult<JsonMessage>().setHandler(resultHandler);
    MessageId id = doExecute(args, tag,
        new Handler<JsonMessage>() {
          @Override
          public void handle(JsonMessage result) {
            future.setResult(result);
            checkPause();
          }
        },
        new Handler<MessageId>() {
          @Override
          public void handle(MessageId event) {
            future.setFailure(FAILURE_EXCEPTION);
            checkPause();
          }
        },
        new Handler<MessageId>() {
          @Override
          public void handle(MessageId event) {
            future.setFailure(TIMEOUT_EXCEPTION);
            checkPause();
          }
        });
    checkPause();
    return id;
  }

  /**
   * Checks the current stream pause status.
   */
  private void checkPause() {
    if (paused) {
      if (queueFull()) {
        paused = false;
        if (drainHandler != null) {
          drainHandler.handle(null);
        }
      }
    }
    else if (queueFull()) {
      paused = true;
    }
  }

}
