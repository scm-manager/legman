package com.github.legman.shiro;

import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.shiro.SecurityUtils.getSubject;

public class ShiroEventDecorator implements com.github.legman.EventDecorator {

  private static final Logger logger = LoggerFactory.getLogger(ShiroEventDecorator.class);

  @Override
  public Runnable decorate(Runnable runnable) {
    try {
      Subject subject = getSubject();
      return subject.associateWith(runnable);
    } catch (UnavailableSecurityManagerException e) {
      logger.warn("no security manager available, running event without security context");
      return runnable;
    }
  }
}
