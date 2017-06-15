package netty.echo;

/**
 * Created by Youga on 2017/6/15.
 */

public class EchoFile extends EchoCommon {
    public String fileName;
    public long fileSize;

    public static EchoFile buildFile(long sumCountPackage ,@Target String target) {
        EchoFile echo = new EchoFile();
        echo.sumCountPackage = sumCountPackage;
        echo.countPackage = 1;
        echo.sendTime = System.currentTimeMillis();
        echo.target = target;
        return echo;
    }
}
