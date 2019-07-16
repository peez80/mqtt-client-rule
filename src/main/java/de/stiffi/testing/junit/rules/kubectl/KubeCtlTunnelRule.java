package de.stiffi.testing.junit.rules.kubectl;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.google.common.base.Stopwatch;
import de.stiffi.testing.junit.rules.helpers.HttpHelper;
import de.stiffi.testing.junit.rules.helpers.ProcessHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Ansatz: Damit wir nicht auf Docker-machine, docker4win, docker4mac, vms, etc. mit Pfaden und Volume-Mounts
 * Probleme bekommen, wird der Container zuerst gestartet, dann mit exec das PortForwarding ausgeführt.
 */
public class KubeCtlTunnelRule extends ExternalResource {

    private Path kubeConfPath;

    private String kubeCtlPath = "kubectl";

    /**
     * Either podSpec or targetHost can be set.
     * If podSpec - only the tunnel to the given pod is made.
     */
    private String podSpec;

    /**
     * Either podSpec or targetHost can be set.
     * If targetHost - a port forwarding pod on kubernetes is created, that forwards requests from that pod to the target host.
     * So you can do a two-hop tunnel
     */
    private String targetHost;

    private String portForwardingPodName;

    private int podPort;
    private String kubeContext;
    private String kubeNamespace;

    private int localPort = -1;

    private URI httpReadinessUri;
    private int httpReadinessStatusCode;

    private int readinessTimeoutMs = 30000;

    /**
     * Return a tunnel rule which forwards a port directly to a pod.
     * Should be the common usecase when you have access to the namespace where the pod is residing in.
     *
     * @param kubeConfPath
     * @param podSpec
     * @param podPort
     * @param kubeContext
     * @param kubeNamespace
     * @return
     */
    public static KubeCtlTunnelRule newPodPortForward(Path kubeConfPath, String podSpec,
                                                      int podPort, String kubeContext, String kubeNamespace) {
        KubeCtlTunnelRule me = new KubeCtlTunnelRule();
        me.podSpec = podSpec;
        me.podPort = podPort;
        me.kubeContext = kubeContext;
        me.kubeNamespace = kubeNamespace;


        me.kubeConfPath = kubeConfPath != null
                ? kubeConfPath
                : findLocalKubeConfig();

        return me;
    }

    /**
     * Return a tunnel rule that starts a port forwarding port to a freely chooseable host remotely at the kubernetes namespace specified by the method parameters.
     * Useful e.g. if you have to foward to a given host where no direct access is available locally or if you want to forward a port to a service retrievable with Kubernetes service discovery
     * but not running within a namespace where the user has access to the pods directly.
     *
     * @param kubeConfPath
     * @param targetHost
     * @param podPort
     * @param kubeContext
     * @param kubeNamespace
     * @return
     */
    public static KubeCtlTunnelRule newHostPortForward(Path kubeConfPath, String targetHost,
                                                       int podPort, String kubeContext, String kubeNamespace) {
        KubeCtlTunnelRule me = new KubeCtlTunnelRule();
        me.targetHost = targetHost;
        me.podPort = podPort;
        me.kubeContext = kubeContext;
        me.kubeNamespace = kubeNamespace;


        me.kubeConfPath = kubeConfPath != null
                ? kubeConfPath
                : findLocalKubeConfig();

        return me;
    }

    /**
     * Tunnel is regarded as established only once the given http get request succeeds.
     *
     * @param applicationPath    relative path after the local port that is tried to reach until port is assumed to be open
     * @param expectedStatusCode
     * @return
     */
    public KubeCtlTunnelRule withHttpReadinessCheck(String applicationPath, int expectedStatusCode) throws IOException, InterruptedException {
        httpReadinessUri = URI.create("http://localhost:" + getLocalPort() + "/" + applicationPath);
        httpReadinessStatusCode = expectedStatusCode;
        return this;
    }

    /**
     * Set Timeout on the readiness check. If readiness check doesn't return after given Time, a exception is thrown. Default: 30000ms
     *
     * @param readinessTimeoutMs
     * @return
     */
    public KubeCtlTunnelRule withReadinessTimeout(int readinessTimeoutMs) {
        this.readinessTimeoutMs = readinessTimeoutMs;
        return this;
    }

    protected KubeCtlTunnelRule() {
        initKubeCtlPath();
    }

    private void initKubeCtlPath() {
        if (System.getenv("KUBECTL_PATH") != null) {
            kubeCtlPath = System.getenv("KUBECTL_PATH");
        }
    }

    @Override
    protected void before() throws Throwable {
        connectWithRetries();
    }

    private void connectWithRetries() throws ExecutionException, RetryException {
        Retryer<Void> retryer = RetryerBuilder.<Void>newBuilder()
                .retryIfException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .build();

        retryer.call(() -> {
            try {
                doConnect();
            } catch (Exception e) {
                System.out.println("Exception while establishing Tunnel.");
                e.printStackTrace();

                try {
                    //For cleanness reasons just try to remove a possibly existing but failed pod
                    deletePodForwardingPod();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                throw e;
            }
            return null;
        });
    }

    private void doConnect() throws IOException, InterruptedException {
        if (podSpec != null) {
            startTunnel(podSpec, podPort);
        } else if (targetHost != null) {
            startPortForwardingPod();
            startTunnel("pods/" + portForwardingPodName, 80);
        } else {
            throw new IllegalArgumentException("podSpec or TargetHost must be set!");
        }
        waitForTunnel(readinessTimeoutMs);
    }

    private void startPortForwardingPod() throws IOException {

        portForwardingPodName = "kubectl-rule-fwd-" + Math.abs(new Random().nextInt());

        String[] cmd = new String[]{
                kubeCtlPath,
                "--kubeconfig", kubeConfPath.toString(),
                "--context", kubeContext,
                "--namespace", kubeNamespace,
                "run",
                "--generator=run-pod/v1",
                portForwardingPodName,
                "--image=djfaze/port-forward",
                "--env=REMOTE_HOST=" + targetHost + "",
                "--env=REMOTE_PORT=" + podPort + ""
        };
        ProcessHelper.execute(StringUtils.join(cmd, " "), true);

        waitForPodDeployment(portForwardingPodName);
    }

    private void waitForPodDeployment(String podName) throws IOException {
        System.out.println("Wait for Pod to become ready...");
        String[] cmd = new String[]{
                kubeCtlPath,
                "--kubeconfig", kubeConfPath.toString(),
                "--context", kubeContext,
                "--namespace", kubeNamespace,
                "get",
                "pods"
        };

        Pattern regexToFind = Pattern.compile(".*" + podName + "\\s*1\\/1\\s*Running.*", Pattern.DOTALL);

        Stopwatch stopw = Stopwatch.createStarted();
        while (true) {
            String result = de.stiffi.testing.junit.rules.helpers.ProcessHelper.execute(StringUtils.join(cmd, " "));

            if (regexToFind.matcher(result).matches()) {
                break;
            }
            if (stopw.elapsed(TimeUnit.SECONDS) > 30) {
                throw new IllegalStateException("30sec Timeout while waiting for Port Forwarding Pod");
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void waitForTunnel(long timeoutMs) throws InterruptedException, IOException {
        if (httpReadinessUri != null) {
            System.out.println("Waiting for Tunnel on " + httpReadinessUri + " -> " + httpReadinessStatusCode);
            waitForTunnelWithReadinessCheck(timeoutMs);
        } else {
            System.out.println("Waiting for Tunnel on Socket localhost:" + getLocalPort());
            waitForTunnelTestingSocket(timeoutMs);
        }
    }

    private void waitForTunnelTestingSocket(long timeoutMs) throws IOException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() < start + timeoutMs) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

            try (Socket s = new Socket("localhost", getLocalPort())) {
                return;
            } catch (Exception e) {
                //Do nothing, we'll return after timeout ms
            }
        }
        throw new IllegalStateException("Timeout while waiting for tunnel - Socketcheck");
    }

    private void waitForTunnelWithReadinessCheck(long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() < start + timeoutMs) {
            try {
                CloseableHttpResponse response = HttpHelper.executeGetForJson(httpReadinessUri.toString());
                if (response.getStatusLine().getStatusCode() == httpReadinessStatusCode) {
                    return;
                }
            } catch (Exception e) {
                Thread.sleep(500);
            }
        }
        throw new IllegalStateException("Timeout while waiting for Tunnel - httpReadinessCheck");
    }

    private void startTunnel(String thePodSpec, int thePodPort) throws IOException {
        //kubectl --kubeconfig $KUBE_CONFIG --context $KUBE_CONTEXT --namespace $KUBE_NAMESPACE port-forward $KUBE_POD_SPEC 2100:$KUBE_POD_PORT
        String[] cmd = new String[]{
                kubeCtlPath,
                "--kubeconfig", kubeConfPath.toString(),
                "--context", kubeContext,
                "--namespace", kubeNamespace,
                "port-forward",
                thePodSpec,
                getLocalPort() + ":" + thePodPort
        };

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                String command = StringUtils.join(cmd, " ");
                try {
                    System.out.println("Start tunnel...");
                    ProcessHelper.execute(command, true);
                    System.out.println("End tunnel!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    @Override
    protected void after() {
        try {
            deletePodForwardingPod();
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void deletePodForwardingPod() throws IOException {
        if (portForwardingPodName != null) {
            final String[] cmd = new String[]{
                    kubeCtlPath,
                    "--kubeconfig", kubeConfPath.toString(),
                    "--context", kubeContext,
                    "--namespace", kubeNamespace,
                    "delete",
                    "pod",
                    portForwardingPodName
            };

            new Thread(() -> {
                //Because terminating a pod often takes quite much time, we do this in a separate thread.
                try {
                    ProcessHelper.execute(StringUtils.join(cmd, " "), true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        }
    }


    private int findFreePort() throws IOException {
        ServerSocket s = new ServerSocket(0);
        int port = s.getLocalPort();
        s.close();
        return port;
    }

    public int getLocalPort() {
        if (localPort == -1) {
            try {
                localPort = findFreePort();
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        return localPort;
    }

    private static Path findLocalKubeConfig() {

        if (System.getenv("KUBECONFIG") != null) {
            Path p = Paths.get(System.getenv("KUBECONFIG"));
            System.out.println("Using KUBECONFIG Env: " + p.toString());
            return p;
        }

        Path p = Paths.get("~/.kube/config");
        if (Files.exists(p)) {
            return p;
        }

        p = Paths.get("U:/.kube/config");
        System.out.println("Test: " + p.toString());
        if (Files.exists(p)) {
            return p;
        }

        p = Paths.get(System.getProperty("user.home") + "/.kube/config");
        System.out.println("Test: " + p.toString());
        if (Files.exists(p)) {
            return p;
        }


        p = Paths.get(System.getProperty("user.dir") + "/.kube/config");
        System.out.println("Test: " + p.toString());
        if (Files.exists(p)) {
            return p;
        }

        System.out.println("No .kubecfg found!");
        return null;
    }


}
