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
package net.kuujo.vertigo.network.impl;

import net.kuujo.vertigo.network.NetworkConfig;
import net.kuujo.vertigo.network.VerticleConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Default verticle configuration implementation.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public class DefaultVerticleConfig extends AbstractComponentConfig<VerticleConfig> implements VerticleConfig {
  private String main;
  private boolean worker = false;
  @JsonProperty("multi-threaded")
  private boolean multiThreaded = false;

  public DefaultVerticleConfig() {
    super();
  }

  public DefaultVerticleConfig(String name, String main, NetworkConfig network) {
    super(name, network);
    this.main = main;
  }

  @Override
  public Type getType() {
    return Type.VERTICLE;
  }

  @Override
  public VerticleConfig setMain(String main) {
    this.main = main;
    return this;
  }

  @Override
  public String getMain() {
    return main;
  }

  @Override
  public VerticleConfig setWorker(boolean isWorker) {
    this.worker = isWorker;
    if (!worker) {
      multiThreaded = false;
    }
    return this;
  }

  @Override
  public boolean isWorker() {
    return worker;
  }

  @Override
  public VerticleConfig setMultiThreaded(boolean isMultiThreaded) {
    this.multiThreaded = isMultiThreaded;
    return this;
  }

  @Override
  public boolean isMultiThreaded() {
    return multiThreaded;
  }

}
