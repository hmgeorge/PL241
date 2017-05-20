import java.lang.Thread;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.String;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class SyncService {
    class GetItemList implements Runnable {
        public void run() {
            HttpURLConnection urlConn = null;
            try {
                URL url = new URL("http://httpbin.org/get");
                urlConn = (HttpURLConnection) url.openConnection();
                if (urlConn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    System.out.println("URL conn failed w/ err " + urlConn.getResponseCode());
                } else {
		    BufferedReader br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
		    String l;
		    System.out.println("Content type " + urlConn.getContentType());
		    while ((l = br.readLine()) != null) {
			System.out.println(l);
		    }
                }
            } catch (InterruptedIOException e) {

            } catch (MalformedURLException e) {

            } catch (IOException e) {

            } finally {
                if (urlConn != null) urlConn.disconnect();
            }
	}
    };

    public static void main(String [] args) {
	System.out.println("SyncService");
	SyncService s = new SyncService();
	s.start();
	s.join();
    }

    SyncService() {
	mThread = new Thread(new GetItemList());
    }

    public void start() {
	mThread.start();
    }

    public void interrupt() {
	mThread.interrupt();
	System.out.println("interupted");
    }

    public void join() {
	try {
	    mThread.join();
	} catch (InterruptedException e) {
	}
    }

    private Thread mThread;
}
