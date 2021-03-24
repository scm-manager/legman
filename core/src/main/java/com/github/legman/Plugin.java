/*
 * Copyright (C) 2007 The Guava Authors and SCM-Manager Team
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

package com.github.legman;

/**
 * A {@link Plugin} can be used to register all components needed for an integration to the event bus.
 *
 * @since 2.0.0
 */
public interface Plugin {

  /**
   * Prepare builder for integration.
   *
   * @param builder eventbus builder
   */
  void apply(EventBus.Builder builder);
}
