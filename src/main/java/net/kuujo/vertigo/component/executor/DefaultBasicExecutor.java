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
package net.kuujo.vertigo.component.executor;

import net.kuujo.vertigo.context.ComponentContext;
import net.kuujo.vertigo.messaging.JsonMessage;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

/**
 * A default basic executor implementation.
 *
 * @author Jordan Halterman
 */
public class DefaultBasicExecutor extends AbstractExecutor<BasicExecutor> implements BasicExecutor {

  public DefaultBasicExecutor(Vertx vertx, Container container, ComponentContext context) {
    super(vertx, container, context);
  }

  @Override
  public BasicExecutor execute(JsonObject args, Handler<AsyncResult<JsonMessage>> resultHandler) {
    return doExecute(args, null, resultHandler);
  }

  @Override
  public BasicExecutor execute(JsonObject args, String tag, Handler<AsyncResult<JsonMessage>> resultHandler) {
    return doExecute(args, tag, resultHandler);
  }

}
