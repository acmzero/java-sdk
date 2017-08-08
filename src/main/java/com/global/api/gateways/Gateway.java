package com.global.api.gateways;

import com.global.api.entities.exceptions.GatewayException;
import sun.misc.IOUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

abstract class Gateway {
    private String contentType;
    protected HashMap<String, String> headers;
    protected int timeout;
    protected String serviceUrl;

    public HashMap<String, String> getHeaders() {
        return headers;
    }
    public void setHeaders(HashMap<String, String> headers) {
        this.headers = headers;
    }
    public int getTimeout() {
        return timeout;
    }
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    public String getServiceUrl() {
        return serviceUrl;
    }
    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public Gateway(String contentType) {
        headers = new HashMap<String, String>();
        this.contentType = contentType;
    }

    protected GatewayResponse sendRequest(String verb, String endpoint) throws GatewayException {
        return sendRequest(verb, endpoint, null, null);
    }
    protected GatewayResponse sendRequest(String verb, String endpoint, String data) throws GatewayException {
        return sendRequest(verb, endpoint, data, null);
    }
    protected GatewayResponse sendRequest(String verb, String endpoint, String data, HashMap<String, String> queryStringParams) throws GatewayException {
        HttpsURLConnection conn;
        try{
            String queryString = buildQueryString(queryStringParams);
            conn = (HttpsURLConnection)new URL((serviceUrl + endpoint + queryString).trim()).openConnection();
            conn.setSSLSocketFactory(new SSLSocketFactoryEx());
            conn.setConnectTimeout(timeout);
            conn.setDoInput(true);
            conn.setRequestMethod(verb);
            conn.addRequestProperty("Content-Type", String.format("%s; charset=UTF-8", contentType));

            for(String key: headers.keySet()) {
                conn.addRequestProperty(key, headers.get(key));
            }

            if(!verb.equals("GET")) {
                byte[] request = data.getBytes();

                conn.setDoOutput(true);
                conn.addRequestProperty("Content-Length", String.valueOf(request.length));

                System.out.println("Request: " + data);
                DataOutputStream requestStream = new DataOutputStream(conn.getOutputStream());
                requestStream.write(request);
                requestStream.flush();
                requestStream.close();
            }
            else System.out.println("Request: " + endpoint);

            InputStream responseStream = conn.getInputStream();
            String rawResponse = new String(IOUtils.readFully(responseStream, conn.getContentLength(), true));
            responseStream.close();
            System.out.println("Response: " + rawResponse);

            GatewayResponse response = new GatewayResponse();
            response.setStatusCode(conn.getResponseCode());
            response.setRawResponse(rawResponse);
            return response;
        }
        catch(Exception exc) {
            throw new GatewayException("Error occurred while communicating with gateway.", exc);
        }
        finally { }
    }

    private String buildQueryString(HashMap<String, String> queryStringParams) throws UnsupportedEncodingException {
        if(queryStringParams == null)
            return "";
        StringBuilder sb = new StringBuilder();
        sb.append("?");
        for (Map.Entry<String, String> entry : queryStringParams.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(String.format("%s=%s", URLEncoder.encode(entry.getKey(), "UTF-8"), URLEncoder.encode(entry.getValue(), "UTF-8")));
        }
        return sb.toString();
    }
}