package com.github.legman.shiro;

import com.github.legman.EventBus;
import com.github.legman.Subscribe;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;

public class ShiroEventDecoratorTest {

  private final ShiroPlugin shiroPlugin = new ShiroPlugin();
  private EventBus eventBus;

  @BeforeEach
  void setUp() {
    DefaultSecurityManager securityManager = new DefaultSecurityManager();
    ThreadContext.bind(securityManager);

    eventBus = EventBus.builder()
            .withIdentifier("test-bus")
            .withPlugins(shiroPlugin)
            .withExecutor(Executors.newFixedThreadPool(4))
            .build();
  }

  @AfterEach
  void tearDown() {
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
  }

  @Test
  void shouldPassOnSubjectSafely() {
    int expectedMatchCount = 64;
    List<String> principals = Arrays.asList("test1", "test2", "test3", "test4", "test5", "test6", "test7", "test8");
    PrincipalMatcher listener = new PrincipalMatcher();
    eventBus.register(listener);

    for(int i = 0; i < expectedMatchCount; ++i) {
      String currentPrincipal = principals.get(i % principals.size());
      bindSubject(currentPrincipal);
      eventBus.post(currentPrincipal);
    }

    await().atMost(10, TimeUnit.SECONDS).until(() -> listener.matchCount.get() == expectedMatchCount);
  }

  private void bindSubject(String principal) {
    Subject dent = new Subject.Builder()
            .authenticated(true)
            .principals(new SimplePrincipalCollection(principal, "test"))
            .buildSubject();
    ThreadContext.bind(dent);
  }

  static class PrincipalMatcher {
    private final AtomicInteger matchCount = new AtomicInteger(0);

    @Subscribe
    public void handle(String event) throws InterruptedException {
      Thread.sleep((long)(Math.random() * 100.0));
      if (event.equals(SecurityUtils.getSubject().getPrincipal())) {
        matchCount.incrementAndGet();
      }
    }
  }
}
