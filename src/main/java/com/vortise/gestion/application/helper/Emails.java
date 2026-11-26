package com.vortise.gestion.application.helper;

import java.util.Locale;

/** Accounts are identified by email; every comparison and lookup uses this canonical form. */
public final class Emails {
    private Emails() { }

    public static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
