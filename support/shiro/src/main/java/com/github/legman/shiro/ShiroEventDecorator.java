package com.github.legman.shiro;

import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.subject.Subject;

import static org.apache.shiro.SecurityUtils.getSubject;

public class ShiroEventDecorator implements com.github.legman.EventDecorator {
  @Override
  public Runnable decorate(Runnable runnable) {
    try {
      Subject subject = getSubject();
      return subject.associateWith(runnable);
    } catch (UnavailableSecurityManagerException e) {
      return runnable;
    }
  }
}
