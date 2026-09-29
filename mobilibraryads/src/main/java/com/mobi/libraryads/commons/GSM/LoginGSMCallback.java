package com.mobi.libraryads.commons.GSM;

public interface LoginGSMCallback {
    void loginSuccess(String accessToken);
    void loginFail();
}
