package com.payangar.designsample.vendor.config;

/** Who owns a setting, and so where it is stored. */
public enum Scope {
    /** The player's own display and comfort, stored on their machine. */
    CLIENT,
    /**
     * Game rules, owned by the server and identical for every player, stored per world. In
     * singleplayer the integrated server plays that role.
     */
    SERVER
}
