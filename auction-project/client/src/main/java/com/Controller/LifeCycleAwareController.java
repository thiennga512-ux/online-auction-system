package com.Controller;

/**
 * Hook vòng đời đơn giản để MainController gọi dọn dẹp trước khi đổi màn.
 */
public interface LifeCycleAwareController {
  void onBeforeHide();
}