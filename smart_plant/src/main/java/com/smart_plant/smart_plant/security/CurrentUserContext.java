package com.smart_plant.smart_plant.security;

import com.smart_plant.smart_plant.entity.User;

public final class CurrentUserContext {

    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(User user) {
        CURRENT_USER.set(user);
    }

    public static User get() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
