package com.utility.billing.common.validation;

public final class ValidationPatterns {

    public static final String FULL_NAME = "^[A-Za-z ]+$";
    public static final String EMAIL = "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$";
    public static final String PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$";
    public static final String PHONE = "^07[0-9]{8}$";

    private ValidationPatterns() {
    }
}
