/*
 * Copyright 2014 the original author or authors.
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
package net.kuujo.vertigo.data.impl;

import net.kuujo.vertigo.annotations.ClusterType;
import net.kuujo.vertigo.annotations.Factory;
import net.kuujo.vertigo.annotations.LocalType;
import net.kuujo.vertigo.data.AsyncIdGenerator;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.impl.DefaultFutureResult;
import org.vertx.java.core.shareddata.ConcurrentSharedMap;

/**
 * Shared data based ID generator.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
@LocalType
@ClusterType
public class SharedDataIdGenerator implements AsyncIdGenerator {
  private static final String ID_MAP_NAME = "__ID__";
  private final String name;
  private final Vertx vertx;
  private final ConcurrentSharedMap<String, Long> map;

  @Factory
  public static SharedDataIdGenerator factory(String name, Vertx vertx) {
    return new SharedDataIdGenerator(name, vertx);
  }

  private SharedDataIdGenerator(String name, Vertx vertx) {
    this.name = name;
    this.vertx = vertx;
    this.map = vertx.sharedData().getMap(ID_MAP_NAME);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public void nextId(final Handler<AsyncResult<Long>> resultHandler) {
    vertx.runOnContext(new Handler<Void>() {
      @Override
      public void handle(Void _) {
        long value;
        if (!map.containsKey(name)) {
          map.put(name, 1L);
          value = 1L;
        } else {
          value = map.get(name)+1;
          map.put(name, value);
        }
        new DefaultFutureResult<Long>(value).setHandler(resultHandler);
      }
    });
  }

}
