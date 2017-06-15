package netty.echo;

import android.support.annotation.StringDef;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Youga on 2017/6/15.
 */

public class EchoCommon implements Serializable {

    public static final String SYSTEM = "SYSTEM", CLIENT = "CLIENT", SERVER = "SERVER",
            HEART_BEAT = "HEART_BEAT";

    @StringDef({SYSTEM, CLIENT, SERVER, HEART_BEAT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Target {
    }

    public long sumCountPackage;
    public int countPackage;
    public byte[] bytes;
    public String sendUid;
    public String receiveUid;
    public long sendTime;
    public String target;
}
