package com.espressif.provision.utils;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;

public class UPnPDiscovery extends AsyncTask {

    private static final String TAG = "Espressif::" + UPnPDiscovery.class.getSimpleName();

    private static final String LINE_END = "\r\n";
    private static final String DEFAULT_QUERY = "M-SEARCH * HTTP/1.1" + LINE_END +
            "HOST: 239.255.255.250:1900" + LINE_END +
            "MAN: \"ssdp:discover\"" + LINE_END +
            "MX: 1" + LINE_END +
            //"ST: urn:schemas-upnp-org:service:AVTransport:1" + LINE_END + // Use for Sonos
            //"ST: urn:schemas-upnp-org:device:InternetGatewayDevice:1" + LINE_END + // Use for Routes
            "ST: ssdp:all" + LINE_END + // Use this for all UPnP Devices
            LINE_END;
    private static int DEFAULT_PORT = 1900;
    private static final String DEFAULT_ADDRESS = "239.255.255.250";

    private HashSet<UPnPDevice> devices = new HashSet<>();
    private Context mContext;
    private Activity mActivity;
    private int mTheardsCount = 0;
    private String mCustomQuery;
    private String mInetAddress;
    private int mPort;

    public interface OnDiscoveryListener {
        void OnStart();

        void OnFoundNewDevice(UPnPDevice device);

        void OnFinish(HashSet<UPnPDevice> devices);

        void OnError(Exception e);
    }

    private OnDiscoveryListener mListener;

    private UPnPDiscovery(Activity activity, OnDiscoveryListener listener) {
        mContext = activity.getApplicationContext();
        mActivity = activity;
        mListener = listener;
        mTheardsCount = 0;
        mCustomQuery = DEFAULT_QUERY;
        mInetAddress = DEFAULT_ADDRESS;
        mPort = DEFAULT_PORT;
    }

    public UPnPDiscovery(Activity activity, OnDiscoveryListener listener, String customQuery, String address, int port) {
        mContext = activity.getApplicationContext();
        mActivity = activity;
        mListener = listener;
        mTheardsCount = 0;
        mCustomQuery = customQuery;
        mInetAddress = address;
        mPort = port;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mListener.OnStart();
    }

    @Override
    protected Void doInBackground(Object... params) {

        Log.d(TAG, "UPnPDiscovery task started.");
        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        if (wifi != null) {

            WifiManager.MulticastLock lock = wifi.createMulticastLock("The Lock");
            lock.acquire();
            DatagramSocket socket = null;

            try {

                InetAddress group = InetAddress.getByName(mInetAddress);
                int port = mPort;
                String query = mCustomQuery;
                socket = new DatagramSocket(null);
                socket.setReuseAddress(true);
                socket.setSoTimeout(5000);
                socket.bind(new InetSocketAddress(0));

                DatagramPacket datagramPacketRequest = new DatagramPacket(query.getBytes(), query.length(), group, port);
                socket.send(datagramPacketRequest);

                long time = System.currentTimeMillis();
                long curTime = System.currentTimeMillis();

                while (curTime - time < 2000) {

                    DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024);
                    socket.receive(datagramPacket);
                    String response = new String(datagramPacket.getData(), 0, datagramPacket.getLength());

                    if (response.substring(0, 12).toUpperCase().equals("HTTP/1.1 200")) {

                        UPnPDevice device = new UPnPDevice(datagramPacket.getAddress().getHostAddress(), response);
//                        Log.d("UPnP","Before getData -"+response);
//                        Log.d("UPnP","Before getData -"+device.toString());
                        mTheardsCount++;
                        mListener.OnFoundNewDevice(device);
                        devices.add(device);
                        mTheardsCount--;

                        if (mTheardsCount == 0) {

                            mActivity.runOnUiThread(new Runnable() {
                                public void run() {
                                    mListener.OnFinish(devices);
                                }
                            });
                        }
//                        getData(device.getHostAddress(), device);
                    }
                    curTime = System.currentTimeMillis();
                }

            } catch (final IOException e) {

                e.printStackTrace();

                mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        mListener.OnError(e);
                    }
                });

            } finally {

                if (socket != null) {
                    socket.close();
                }
            }
            lock.release();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        Log.d(TAG, "On Post Execute");
    }

    private void getData(final String url, final UPnPDevice device) {
//        String url = "http://"+url2+"/rootDesc.xml";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        device.update(response);
                        mListener.OnFoundNewDevice(device);
                        devices.add(device);
                        mTheardsCount--;
                        if (mTheardsCount == 0) {
                            mActivity.runOnUiThread(new Runnable() {
                                public void run() {
                                    mListener.OnFinish(devices);
                                }
                            });
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                mTheardsCount--;
//                if (mTheardsCount == 0) {
//                    mActivity.runOnUiThread(new Runnable() {
//                        public void run() {
//                            mListener.OnFinish(devices);
//                        }
//                    });
//                }
                Log.d(TAG, "URL: " + url + " get content error!");
            }
        });
        stringRequest.setTag(TAG + "SSDP description request");
        Volley.newRequestQueue(mContext).add(stringRequest);
    }
}
