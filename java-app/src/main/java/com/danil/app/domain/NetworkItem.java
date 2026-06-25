package com.danil.app.domain;

import java.net.InetAddress;

public record NetworkItem<T>(
    T content,
    java.net.Socket socket, // null для UDP
    InetAddress udpAddr, // null для TCP
    int udpPort // 0 для TCP
) {}

