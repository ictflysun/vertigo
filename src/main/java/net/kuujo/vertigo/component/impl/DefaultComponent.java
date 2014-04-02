/*
 * Copyright 2013-2014 the original author or authors.
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
package net.kuujo.vertigo.component.impl;

import java.util.ArrayList;
import java.util.List;

import net.kuujo.vertigo.cluster.VertigoCluster;
import net.kuujo.vertigo.component.Component;
import net.kuujo.vertigo.component.ComponentCoordinator;
import net.kuujo.vertigo.context.ComponentContext;
import net.kuujo.vertigo.context.InstanceContext;
import net.kuujo.vertigo.hooks.ComponentHook;
import net.kuujo.vertigo.hooks.InputHook;
import net.kuujo.vertigo.hooks.OutputHook;
import net.kuujo.vertigo.input.InputCollector;
import net.kuujo.vertigo.input.impl.DefaultInputCollector;
import net.kuujo.vertigo.network.auditor.Acker;
import net.kuujo.vertigo.network.auditor.impl.DefaultAcker;
import net.kuujo.vertigo.output.OutputCollector;
import net.kuujo.vertigo.output.impl.DefaultOutputCollector;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.impl.DefaultFutureResult;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.platform.Container;

/**
 * A default component implementation.
 *
 * @author Jordan Halterman
 */
public class DefaultComponent implements Component {
  protected final Vertx vertx;
  protected final EventBus eventBus;
  protected final Container container;
  protected final Logger logger;
  protected final VertigoCluster cluster;
  private final ComponentCoordinator coordinator;
  private Acker acker;
  protected final String address;
  protected InstanceContext context;
  protected InputCollector input;
  protected OutputCollector output;
  protected List<ComponentHook> hooks = new ArrayList<>();
  private boolean started;

  private InputHook inputHook = new InputHook() {
    @Override
    public void handleReceive(String id) {
      for (ComponentHook hook : hooks) {
        hook.handleReceive(id);
      }
    }
    @Override
    public void handleAck(String id) {
      for (ComponentHook hook : hooks) {
        hook.handleAck(id);
      }
    }
    @Override
    public void handleFail(String id) {
      for (ComponentHook hook : hooks) {
        hook.handleFail(id);
      }
    }
  };

  private OutputHook outputHook = new OutputHook() {
    @Override
    public void handleEmit(String id) {
      for (ComponentHook hook : hooks) {
        hook.handleEmit(id);
      }
    }
    @Override
    public void handleAcked(String id) {
      for (ComponentHook hook : hooks) {
        hook.handleAcked(id);
      }
    }
    @Override
    public void handleFailed(String id) {
      for (ComponentHook hook : hooks) {
        hook.handleFailed(id);
      }
    }
    @Override
    public void handleTimeout(String id) {
      for (ComponentHook hook : hooks) {
        hook.handleTimeout(id);
      }
    }
  };

  protected DefaultComponent(String network, String address, Vertx vertx, Container container, VertigoCluster cluster) {
    this.address = address;
    this.vertx = vertx;
    this.eventBus = vertx.eventBus();
    this.container = container;
    this.logger = LoggerFactory.getLogger(String.format("%s-%s", getClass().getCanonicalName(), address));
    this.cluster = cluster;
    this.coordinator = new DefaultComponentCoordinator(network, address, cluster);
  }

  @Override
  public Vertx vertx() {
    return vertx;
  }

  @Override
  public Container container() {
    return container;
  }

  @Override
  public InputCollector input() {
    return input;
  }

  @Override
  public OutputCollector output() {
    return output;
  }

  @Override
  public InstanceContext context() {
    return context;
  }

  @Override
  public VertigoCluster cluster() {
    return cluster;
  }

  @Override
  public Logger logger() {
    return logger;
  }

  @Override
  public Component addHook(ComponentHook hook) {
    if (hooks.isEmpty()) {
      input.addHook(inputHook);
      output.addHook(outputHook);
    }
    hooks.add(hook);
    return  this;
  }

  /**
   * Calls start hooks.
   */
  private void hookStart() {
    for (ComponentHook hook : hooks) {
      hook.handleStart(this);
    }
  }

  /**
   * Calls stop hooks
   */
  @SuppressWarnings("unused")
  private void hookStop() {
    for (ComponentHook hook : hooks) {
      hook.handleStop(this);
    }
  }

  /**
   * Sets up the component.
   */
  private void setup(final Handler<AsyncResult<Void>> doneHandler) {
    coordinator.start(new Handler<AsyncResult<InstanceContext>>() {
      @Override
      public void handle(AsyncResult<InstanceContext> result) {
        if (result.failed()) {
          new DefaultFutureResult<Void>(result.cause()).setHandler(doneHandler);
        } else {
          context = result.result();
          acker = new DefaultAcker(eventBus, context.component().network().auditors());
          input = new DefaultInputCollector(vertx, context.input(), cluster, acker);
          output = new DefaultOutputCollector(vertx, context.output(), cluster, acker);
          for (ComponentHook hook : context.<ComponentContext<?>>component().hooks()) {
            addHook(hook);
          }
          acker.start(new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> result) {
              if (result.failed()) {
                new DefaultFutureResult<Void>(result.cause()).setHandler(doneHandler);
              } else {
                output.open(new Handler<AsyncResult<Void>>() {
                  @Override
                  public void handle(AsyncResult<Void> result) {
                    if (result.failed()) {
                      new DefaultFutureResult<Void>(result.cause()).setHandler(doneHandler);
                    } else {
                      input.open(new Handler<AsyncResult<Void>>() {
                        @Override
                        public void handle(AsyncResult<Void> result) {
                          if (result.failed()) {
                            new DefaultFutureResult<Void>(result.cause()).setHandler(doneHandler);
                          } else {
                            coordinator.resume();
                          }
                        }
                      });
                    }
                  }
                });
              }
            }
          });
        }
      }
    });
    coordinator.resumeHandler(new Handler<Void>() {
      @Override
      public void handle(Void _) {
        started = true;
        new DefaultFutureResult<Void>((Void) null).setHandler(doneHandler);
      }
    });
  }

  @Override
  public Component start() {
    return start(null);
  }

  @Override
  public Component start(Handler<AsyncResult<Component>> doneHandler) {
    final Future<Component> future = new DefaultFutureResult<Component>().setHandler(doneHandler);
    if (!started) {
      setup(new Handler<AsyncResult<Void>>() {
        @Override
        public void handle(AsyncResult<Void> result) {
          if (result.failed()) {
            future.setFailure(result.cause());
          } else {
            hookStart();
            future.setResult(DefaultComponent.this);
          }
        }
      });
    }
    else {
      vertx.runOnContext(new Handler<Void>() {
        @Override
        public void handle(Void _) {
          future.setResult(DefaultComponent.this);
        }
      });
    }
    return this;
  }

}