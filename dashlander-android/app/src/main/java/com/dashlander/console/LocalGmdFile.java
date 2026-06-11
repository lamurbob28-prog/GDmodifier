package com.dashlander.console;

public final class LocalGmdFile {
    public final String displayName;
    public final String uriText;
    public final String xml;

    public LocalGmdFile(String displayName, String uriText, String xml) {
        this.displayName = displayName;
        this.uriText = uriText;
        this.xml = xml;
    }

    public GitHubUploadsClient.UploadFile asUploadFile() {
        return new GitHubUploadsClient.UploadFile(displayName, "local/" + displayName, uriText);
    }
}
