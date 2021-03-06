package com.leo.appmaster.cloud;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import android.os.Process;
import android.text.TextUtils;

/**
 * Created by Jasper on 2015/12/16.
 */
public abstract class CloudService extends Thread {
    private static final byte[] LOCK = new byte[1];

    private static Map<String, CloudService> sCloudServiceMap = new HashMap<String, CloudService>();
    private BlockingQueue<CloudRequest> mWorkQueue;

    public CloudService() {
        mWorkQueue = new PriorityBlockingQueue<CloudRequest>();
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true) {
            try {
                final CloudRequest request = mWorkQueue.take();
                if (request.isCanceled()) {
                    continue;
                }

                performRequest(request);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void addRequest(CloudRequest request) {
        if (request == null) {
            return;
        }
        mWorkQueue.add(request);
    }

    protected abstract void performRequest(CloudRequest request);
    protected abstract HttpURLConnection openConnection(CloudRequest request) throws MalformedURLException, IOException;

    public static CloudService createService(Class<?> clazz) {
        if (clazz == null || TextUtils.isEmpty(clazz.getName())) {
            return null;
        }

        String name = clazz.getName();
        if (!name.equals(UploadRequest.TAG) || !name.equals(DownloadRequest.TAG)) {
            return null;
        }

        synchronized (LOCK) {
            CloudService service = sCloudServiceMap.get(clazz.getName());
            if (service != null) {
                return service;
            }
            if (name.equals(UploadRequest.TAG)) {
                service = new UploadService();
            } else {
                service = new DownloadService();
            }
            service.start();
            sCloudServiceMap.put(name, service);

            return service;
        }
    }
}
