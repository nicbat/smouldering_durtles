package com.smouldering_durtles.wk.livedata;

import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.enums.OnlineStatus;

/** Connectivity events, independent of the result of the last API request. */
public final class LiveOnlineStatus extends ConservativeLiveData<OnlineStatus> {
    private static final LiveOnlineStatus instance = new LiveOnlineStatus();
    private LiveOnlineStatus() {}
    public static LiveOnlineStatus getInstance() { return instance; }
    @Override protected void updateLocal() {
        postValue(WkApplication.getInstance().getOnlineStatus());
    }
    @Override public OnlineStatus getDefaultValue() { return OnlineStatus.NO_CONNECTION; }
}
