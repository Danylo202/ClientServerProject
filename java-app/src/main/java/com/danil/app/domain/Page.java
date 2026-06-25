package com.danil.app.domain;

import java.util.List;

public record Page<T>(
    List<T> items,      // список товарів на цій сторінці
    long totalItems,    // всього товарів, що пройшли запит
    int totalPages,     // загальна кількість сторінок
    int currentPage     // номер цієї сторінки
) {}