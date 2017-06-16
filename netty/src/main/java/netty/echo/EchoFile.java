package netty.echo;

/**
 * Created by Youga on 2017/6/15.
 */

public class EchoFile extends EchoCommon {
    private static final long serialVersionUID = -7073394178771948754L;
    public String fileName;
    public long fileSize;
    public String filePath;

    public static EchoFile buildFile(byte[] bytes, String fileName, String filePath, Target target) {
        EchoFile echo = new EchoFile();
        echo.bytes = bytes;
        echo.fileSize = bytes.length;
        echo.fileName = fileName;
        echo.filePath = filePath;
        echo.countPackage = 1;
        echo.sendTime = System.currentTimeMillis();
        echo.target = target;
        return echo;
    }
}
