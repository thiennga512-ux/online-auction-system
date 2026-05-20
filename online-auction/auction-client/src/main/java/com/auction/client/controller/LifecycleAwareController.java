package com.auction.client.controller;

/**
 * Hook vòng đời đơn giản để MainController gọi dọn dẹp trước khi đổi màn.
 */
public interface LifecycleAwareController {
  void onBeforeHide();
}
