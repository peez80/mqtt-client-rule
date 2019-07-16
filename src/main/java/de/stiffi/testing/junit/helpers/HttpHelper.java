package de.stiffi.testing.junit.helpers;

import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class HttpHelper {

    private static CloseableHttpClient getClient(URI uri, UsernamePasswordCredentials basicAuth, int timeoutMs) {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        if (basicAuth != null) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(uri.getHost(), uri.getPort()),
                    basicAuth);
            httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
        }
        httpClientBuilder.setSSLHostnameVerifier((s, sslSession) -> true);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeoutMs)
                .setSocketTimeout(timeoutMs)
                .setConnectionRequestTimeout(timeoutMs)
                .build();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);

        CloseableHttpClient httpClient = httpClientBuilder.build();
        return httpClient;
    }

    public static CloseableHttpResponse executePost(String url, String payload, String payloadContentType, String accept, UsernamePasswordCredentials basicAuth) throws IOException {
        return executePost(url, payload != null ? payload.getBytes() : null, payloadContentType, accept, basicAuth, null);
    }

    /**
     * Calls POST /reservations on cs-reservation-service-rest
     */
    public static CloseableHttpResponse executePost(String url, byte[] payload, String payloadContentType, String accept, UsernamePasswordCredentials basicAuth, Map<String, String> headers) throws IOException {
        URI uri = URI.create(url);
        HttpPost post = new HttpPost(uri);

        if (payload != null) {
            post.setEntity(new ByteArrayEntity(payload));
        }
        post.setHeader(HttpHeaders.CONTENT_TYPE, payloadContentType);
        if (accept != null) {
            post.setHeader(HttpHeaders.ACCEPT, accept);
        }
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                post.setHeader(e.getKey(), e.getValue());
            }
        }
        Dumper.sout("HTTP POST: " + url + ": " + payload);
        CloseableHttpClient httpclient = getClient(uri, basicAuth, 60000);
        CloseableHttpResponse response = httpclient.execute(post);
        Dumper.sout("HTTP Response: " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
        return response;
    }

    public static CloseableHttpResponse executeJsonPost(String url, String payload, UsernamePasswordCredentials basicAuth) throws IOException {
        return executePost(url, payload, "application/json", "application/json", basicAuth);
    }


    public static CloseableHttpResponse executeGetForJson(String url) throws IOException {
        return executeGet(url, "application/json");

    }

    public static CloseableHttpResponse executeGet(String url, String... acceptedMediaTypes) throws IOException {
        URI uri = URI.create(url);
        CloseableHttpClient httpClient = getClient(uri, null, 10000);
        HttpGet get = new HttpGet(uri);
        if (acceptedMediaTypes != null) {
            for (String accept : acceptedMediaTypes) {
                get.addHeader(HttpHeaders.ACCEPT, accept);
            }
        }
        System.out.println("HTTP GET: " + url);
        CloseableHttpResponse response = httpClient.execute(get);
        System.out.println("--> "+ response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
        return response;
    }

    public static CloseableHttpResponse executeOptions(String url) throws IOException {
        URI uri = URI.create(url);
        CloseableHttpClient httpClient = getClient(uri, null, 10000);
        HttpOptions options = new HttpOptions(uri);
        System.out.println("HTTP OPTIONS: " + uri);
        CloseableHttpResponse response = httpClient.execute(options);
        return response;
    }

    public static String readResponseBody(CloseableHttpResponse response) throws IOException {
        return readResponseBody(response, true);
    }

    public static String readResponseBody(CloseableHttpResponse response, boolean trace) throws IOException {
        String s = StreamReader.read(response.getEntity().getContent());
        if (trace) {
            System.out.println("Response: " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase() + ": \n" + s);
        }
        return s;
    }


}
