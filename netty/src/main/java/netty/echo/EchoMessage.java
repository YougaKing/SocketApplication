package netty.echo;

/**
 * Created by Youga on 2017/6/15.
 */

public class EchoMessage extends EchoCommon {

    public static EchoMessage buildMessage(String message, @Target String target) {
        EchoMessage echo = new EchoMessage();
        byte[] bytes = message.getBytes(Constants.getCharset());
        echo.bytes = bytes;
        echo.sumCountPackage = bytes.length;
        echo.countPackage = 1;
        echo.sendTime = System.currentTimeMillis();
        echo.target = target;
        return echo;
    }

    public String getMessage() {
        return new String(bytes, Constants.getCharset());
    }
}
