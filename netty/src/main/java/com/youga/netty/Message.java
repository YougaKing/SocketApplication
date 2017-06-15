package com.youga.netty;


import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Youga on 2017/6/15.
 */

public class Message {

    public static final String SYSTEM = "SYSTEM", LOCAL = "LOCAL", REMOTE = "REMOTE";

    @StringDef({SYSTEM, LOCAL, REMOTE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Target {
    }

    public String target;
    public String message;

    public Message() {
    }

    public Message(@Target String target, String message) {
        this.target = target;
        this.message = message;
    }
}
