package com.dashlander.console;

import java.util.ArrayList;
import java.util.List;

public class UploadResult {
    public boolean success = false;
    public String levelId = "";
    public String error = "";
    public final List<UploadAttempt> attempts = new ArrayList<>();
}
