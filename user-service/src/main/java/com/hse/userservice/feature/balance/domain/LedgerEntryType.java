package com.hse.userservice.feature.balance.domain;

public enum LedgerEntryType {
    BALANCE_TOP_UP("Пополнение баланса"), BALANCE_WITHDRAWAL("Вывод средств"), BOOKING_CHARGE("Оплата бронирования"), BOOKING_USER_CANCELLATION_REFUND(
            "Возврат за отмену бронирования"), BOOKING_ADMIN_CANCELLATION_COMPENSATION(
            "Компенсация за административную отмену"), MANUAL_CREDIT("Ручное начисление"), MANUAL_DEBIT(
            "Ручное списание"), SERVICE_REQUEST_CHARGE("Оплата сервисной заявки");

    private final String displayName;

    LedgerEntryType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
