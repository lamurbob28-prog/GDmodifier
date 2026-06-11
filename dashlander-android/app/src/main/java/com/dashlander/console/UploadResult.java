package com.dashlander.console;

import java.util.ArrayList;
import java.util.List;

public class UploadResult {
    public boolean success = false;
    public String levelId = "";
    public String error = "";
    public boolean verificationAttempted = false;
    public boolean verificationFound = false;
    public String verificationLevelName = "";
    public String verificationWarning = "";
    public final List<UploadAttempt> attempts = new ArrayList<>();
}
