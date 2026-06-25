package com.danil.app.domain;

public record Message(int command, int userId, byte[] data) {}