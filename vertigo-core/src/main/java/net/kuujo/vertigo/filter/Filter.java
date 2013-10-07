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
package net.kuujo.vertigo.filter;

import org.vertx.java.core.json.JsonObject;

import net.kuujo.vertigo.Initializable;
import net.kuujo.vertigo.definition.Definition;
import net.kuujo.vertigo.messaging.JsonMessage;

/**
 * A message filter.
 *
 * @author Jordan Halterman
 */
public interface Filter extends Definition, Initializable<JsonObject, Filter> {

  /**
   * Indicates whether the given message is valid.
   *
   * @param message
   *   The message to validate.
   * @return
   *   A boolean indicating whether the given message is valid.
   */
  public boolean valid(JsonMessage message);

}
