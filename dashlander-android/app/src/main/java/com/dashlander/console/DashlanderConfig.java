package com.dashlander.console;

public final class DashlanderConfig {
    private DashlanderConfig() {}

    public static final String OWNER = "lamurbob28-prog";
    public static final String REPO = "GDmodifier";
    public static final String BRANCH = "main";
    public static final String UPLOADS_API = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/contents/uploads?ref=" + BRANCH;
    public static final String RAW_BASE = "https://raw.githubusercontent.com/" + OWNER + "/" + REPO + "/" + BRANCH + "/";
}
