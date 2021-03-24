/*
 * Copyright (C) 2013 SCM-Manager Team
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

package com.github.legman.shiro;

import com.github.legman.EventBus;
import com.github.legman.Subscribe;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Sebastian Sdorra
 */
class ShiroExecutorDecoratorFactoryTest {

  @BeforeEach
  void setUpShiro() {
    IniSecurityManagerFactory securityManagerFactory = new IniSecurityManagerFactory("classpath:com/github/legman/shiro/001.ini");
    ThreadContext.bind(securityManagerFactory.createInstance());
  }

  @AfterEach
  void tearDownShiro() {
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
  }

  @Test
  void testSubjectAware() throws InterruptedException {
    SecurityUtils.getSubject().login(new UsernamePasswordToken("trillian", "secret"));

    EventBus bus = new EventBus();
    Listener listener = new Listener();
    bus.register(listener);
    bus.post("event");

    // wait until async event is dispatched
    Thread.sleep(500l);
    assertThat(listener.principal).isEqualTo("trillian");
  }

  private static class Listener {

    private Object principal;

    @Subscribe
    public void handleEvent(String event){
      principal = SecurityUtils.getSubject().getPrincipal();
    }

  }
}
