package com.vetautet.app.shared.common.util;

public final class MaskUtil {

    private MaskUtil() {
    }

    public static String maskEmail(String email) {
        if (email == null) return null;
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) return email;
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        int visibleChars = Math.min(2, local.length());
        return local.substring(0, visibleChars) + "***" + domain;
    }
}
