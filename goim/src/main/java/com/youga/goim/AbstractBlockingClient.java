package com.youga.goim;


import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Observable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An abstract blocking client, designed to connect to implementations of
 * AbstractServer in its own thread. Since the client only has a single
 * connection to a single server it can use blocking IO. This class provides a
 * set of callback methods for concrete implementations to know the state of the
 * client and its connection This client does not log, implementations should
 * handle this.
 * <p>
 * This client does not support SSL or UDP connections.
 */
public abstract class AbstractBlockingClient extends Observable implements Runnable {

    private static final String TAG = AbstractBlockingClient.class.getSimpleName();


    private enum State {
        STOPPED, STOPPING, RUNNING
    }

    private static short DEFAULT_MESSAGE_SIZE = 1024;

    private final AtomicReference<State> state = new AtomicReference<>(State.STOPPED);
    protected final InetAddress server;
    protected final int port;
    private final int defaultBufferSize;
    private int defaultHeartBeatTimeOut = 5;
    private int defaultSocketTimeOut = 3 * 60 * 1000;
    protected final Integer uid;
    protected final String game;
    private final AtomicReference<DataOutputStream> out = new AtomicReference<>();
    private final AtomicReference<DataInputStream> in = new AtomicReference<>();

    /**
     * Construct an unstarted client which will attempt to connect to the given
     * server on the given port.
     *
     * @param server the server address.
     * @param port   the port on which to connect to the server.
     */
    public AbstractBlockingClient(InetAddress server, int port, Integer uid, String game) {
        this(server, port, uid, game, DEFAULT_MESSAGE_SIZE);
    }

    /**
     * Construct an unstarted client which will attempt to connect to the given
     * server on the given port.
     *
     * @param server            the server address.
     * @param port              the port on which to connect to the server.
     * @param defaultBufferSize the default buffer size for reads. This should as small as
     *                          possible value that doesn't get exceeded often - see class
     *                          documentation.
     */
    public AbstractBlockingClient(InetAddress server, int port, Integer uid, String game, int defaultBufferSize) {
        this.server = server;
        this.port = port;
        this.uid = uid;
        this.game = game;
        this.defaultBufferSize = defaultBufferSize;
    }

    /**
     * Returns the port to which this client will connect.
     *
     * @return the port to which this client will connect.
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the host to which this client will connect.
     *
     * @return the host to which this client will connect.
     */
    public InetAddress getServer() {
        return server;
    }

    /**
     * Returns true if this client is the running state (either connected or
     * trying to connect).
     *
     * @return true if this client is the running state (either connected or
     * trying to connect).
     */
    public boolean isRunning() {
        return state.get() == State.RUNNING;
    }

    /**
     * Returns true if this client is the stopped state.
     *
     * @return true if this client is the stopped state.
     */
    public boolean isStopped() {
        return state.get() == State.STOPPED;
    }

    /**
     * Attempt to connect to the server and receive messages. If the client is
     * already running, it will not be started again. This method is designed to
     * be called in its own thread and will not return until the client is
     * stopped.
     *
     * @throws RuntimeException if the client fails
     */
    @Override
    public void run() {
        Socket socket = null;
        try {
            socket = new Socket(server, port);
            socket.setSoTimeout(defaultSocketTimeOut);

            out.set(new DataOutputStream(socket.getOutputStream()));
            in.set(new DataInputStream(socket.getInputStream()));

            if (!state.compareAndSet(State.STOPPED, State.RUNNING)) {
                return;
            }

            authWrite();

            while (state.get() == State.RUNNING) {
                byte[] inBuffer = new byte[defaultBufferSize];
                int readPoint = in.get().read(inBuffer);
                if (readPoint != -1) {
                    byte[] result = BruteForceCoding.tail(inBuffer, inBuffer.length - 16);

                    Long operation = BruteForceCoding.decodeIntBigEndian(inBuffer, 8, 4);
                    if (3 == operation) {//服务端心跳答复
                        heartBeatReceived();
                    } else if (8 == operation) {//auth认证返回
                        authSuccess();
                        heartBeat();
                    } else if (5 == operation) {//下行消息
                        Long packageLength = BruteForceCoding.decodeIntBigEndian(inBuffer, 0, 4);
                        Long headLength = BruteForceCoding.decodeIntBigEndian(inBuffer, 4, 2);
                        Long version = BruteForceCoding.decodeIntBigEndian(inBuffer, 6, 2);
                        Long sequenceId = BruteForceCoding.decodeIntBigEndian(inBuffer, 12, 4);
                        //messageReceived(packageLength, headLength, version,operation, sequenceId,new String(result).trim());
                        messageReceived(new String(result).trim());
                    }
                }
            }
        } catch (Exception ioe) {
            ioe.printStackTrace();
            Log.d(TAG, "Client failure: " + ioe.getMessage());
            try {
                socket.close();
                state.set(State.STOPPED);
                disconnected();
            } catch (Exception e) {
                // do nothing - server failed
            }
            restart();
        }
    }

    private void restart() {
        if (true) {
            super.setChanged();
        }
        notifyObservers();
    }

    /**
     * Stop the client in a graceful manner. After this call the client may
     * spend some time in the process of stopping. A disconnected callback will
     * occur when the client actually stops.
     *
     * @return if the client was successfully set to stop.
     */
    public boolean stop() {
        if (state.compareAndSet(State.RUNNING, State.STOPPING)) {
            try {
                in.get().close();
            } catch (IOException e) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * 参数名	必选	类型	说明
     * package length	true	int32 bigendian	包长度
     * header Length	true	int16 bigendian	包头长度
     * ver	true	int16 bigendian	协议版本
     * operation	true	int32 bigendian	协议指令
     * seq	true	int32 bigendian	序列号
     * body	false	binary	$(package lenth) - $(header length)
     * Send the given message to the server.
     *
     * @return true if the message was sent to the server.
     * @throws IOException
     */
    public synchronized Boolean authWrite() throws IOException {
        String msg = uid + "," + getGameCode(game);

        int packLength = msg.length() + 16;
        byte[] message = new byte[4 + 2 + 2 + 4 + 4];

        // package length
        int offset = BruteForceCoding.encodeIntBigEndian(message, packLength, 0, 4 * BruteForceCoding.BSIZE);
        // header lenght
        offset = BruteForceCoding.encodeIntBigEndian(message, 16, offset, 2 * BruteForceCoding.BSIZE);
        // ver
        offset = BruteForceCoding.encodeIntBigEndian(message, 1, offset, 2 * BruteForceCoding.BSIZE);
        // operation
        offset = BruteForceCoding.encodeIntBigEndian(message, 7, offset, 4 * BruteForceCoding.BSIZE);
        // jsonp callback
        offset = BruteForceCoding.encodeIntBigEndian(message, 1, offset, 4 * BruteForceCoding.BSIZE);

        byte[] bytes = BruteForceCoding.add(message, msg.getBytes());

        Log.d(TAG, Arrays.toString(bytes));

        out.get().write(bytes);
        out.get().flush();

        return true;

    }

    private ExecutorService mMessageExecutor = Executors.newCachedThreadPool();

    public void sendMessage(final String game) {
        mMessageExecutor.execute(new Runnable() {
            @Override
            public void run() {
                String msg = uid + "," + getGameCode(game);

                int packLength = msg.length() + 16;
                byte[] message = new byte[4 + 2 + 2 + 4 + 4];

                // package length
                int offset = BruteForceCoding.encodeIntBigEndian(message, packLength, 0, 4 * BruteForceCoding.BSIZE);
                // header lenght
                offset = BruteForceCoding.encodeIntBigEndian(message, 16, offset, 2 * BruteForceCoding.BSIZE);
                // ver
                offset = BruteForceCoding.encodeIntBigEndian(message, 1, offset, 2 * BruteForceCoding.BSIZE);
                // operation
                offset = BruteForceCoding.encodeIntBigEndian(message, 254, offset, 4 * BruteForceCoding.BSIZE);
                // jsonp callback
                offset = BruteForceCoding.encodeIntBigEndian(message, 1, offset, 4 * BruteForceCoding.BSIZE);

                try {
                    out.get().write(BruteForceCoding.add(message, msg.getBytes()));
                    out.get().flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /****
     * get game code
     */
    private int getGameCode(String game) {
        int sum = 0;
        byte[] array = game.getBytes();
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
    }


    private ScheduledExecutorService mHeartBeatExecutor = Executors.newScheduledThreadPool(2);

    /*****
     * heart beat Thread
     */
    private void heartBeat() {
        mHeartBeatExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    heartBeatWrite();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, defaultHeartBeatTimeOut, TimeUnit.SECONDS);
    }


    /**
     * Send the given message to the server.
     * 客户端请求心跳
     *
     * @return true if the message was sent to the server.
     * @throws IOException
     */
    public synchronized Boolean heartBeatWrite() throws IOException {
        String msg = uid + "," + getGameCode(game);

        int packLength = msg.length() + 16;
        byte[] message = new byte[4 + 2 + 2 + 4 + 4];

        // package length
        int offset = BruteForceCoding.encodeIntBigEndian(message, packLength, 0, 4 * BruteForceCoding.BSIZE);
        // header lenght
        offset = BruteForceCoding.encodeIntBigEndian(message, 16, offset, 2 * BruteForceCoding.BSIZE);
        // ver
        offset = BruteForceCoding.encodeIntBigEndian(message, 1, offset, 2 * BruteForceCoding.BSIZE);
        // operation
        offset = BruteForceCoding.encodeIntBigEndian(message, 2, offset, 4 * BruteForceCoding.BSIZE);
        // jsonp callback
        offset = BruteForceCoding.encodeIntBigEndian(message, 1, offset, 4 * BruteForceCoding.BSIZE);

        out.get().write(BruteForceCoding.add(message, msg.getBytes()));
        out.get().flush();

        return true;
    }

    /**
     * Callback method for when the client receives a message from the server.
     *
     * @param message the message from the server.
     */
    protected abstract void messageReceived(Long packageLength, Long headLength, Long version, Long operation, Long sequenceId, String message);

    /**
     * Callback method for when the client receives a message from the server.
     *
     * @param message the message from the server.
     */
    protected abstract void messageReceived(String message);

    /**
     * Callback method for when the client receives a message from the server.
     */
    protected abstract void heartBeatReceived();

    /**
     * Callback method for when the client receives a message from the server.
     */
    protected abstract void authSuccess();

    /**
     * Callback method for when the client connects to the server.
     *
     * @param alreadyConnected whether the client was already connected to the server.
     */
    protected abstract void connected(boolean alreadyConnected);

    /**
     * Callback method for when the client disconnects from the server.
     */
    protected abstract void disconnected();
}