package com.github.legman;

public interface EventDecorator {
  Runnable decorate(Runnable runnable);
}
