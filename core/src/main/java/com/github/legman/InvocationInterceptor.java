package com.github.legman;

import java.lang.reflect.InvocationTargetException;

public interface InvocationInterceptor {
  void invoke(InvocationContext context) throws InvocationTargetException;
}
