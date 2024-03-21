package com.github.legman.shiro;

import org.apache.shiro.subject.Subject;

import static org.apache.shiro.SecurityUtils.getSubject;

public class ShiroEventDecorator implements com.github.legman.EventDecorator {
  @Override
  public Runnable decorate(Runnable runnable) {
    Subject subject = getSubject();
    return subject.associateWith(runnable);
  }
}
