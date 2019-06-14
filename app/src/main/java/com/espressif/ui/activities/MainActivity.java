// Copyright 2018 Espressif Systems (Shanghai) PTE LTD
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.espressif.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.espressif.provision.BuildConfig;
import com.espressif.provision.Provision;
import com.espressif.provision.R;
import com.espressif.provision.transport.BLETransport;

import java.util.HashMap;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Espressif::" + MainActivity.class.getSimpleName();

    private String BASE_URL, NETWORK_NAME_PREFIX, DEVICE_NAME_PREFIX;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Button btnProvision = findViewById(R.id.provision_button);
        final String productDSN = generateProductDSN();

        BASE_URL = getResources().getString(R.string.wifi_base_url);
        NETWORK_NAME_PREFIX = getResources().getString(R.string.wifi_network_name_prefix);
        DEVICE_NAME_PREFIX = getResources().getString(R.string.ble_device_name_prefix);

        final String transportVersion, securityVersion;

        if (BuildConfig.FLAVOR_security.equals("sec1")) {
            securityVersion = Provision.CONFIG_SECURITY_SECURITY1;
        } else {
            securityVersion = Provision.CONFIG_SECURITY_SECURITY0;
        }

        if (BuildConfig.FLAVOR_transport.equals("ble")) {
            transportVersion = Provision.CONFIG_TRANSPORT_BLE;
        } else {
            transportVersion = Provision.CONFIG_TRANSPORT_WIFI;
        }

        btnProvision.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                vib.vibrate(HapticFeedbackConstants.VIRTUAL_KEY);

                HashMap<String, String> config = new HashMap<>();
                config.put(Provision.CONFIG_TRANSPORT_KEY, transportVersion);
                config.put(Provision.CONFIG_SECURITY_KEY, securityVersion);

                config.put(Provision.CONFIG_BASE_URL_KEY, BASE_URL);
                config.put(Provision.CONFIG_WIFI_AP_KEY, NETWORK_NAME_PREFIX);

                config.put(BLETransport.DEVICE_NAME_PREFIX_KEY, DEVICE_NAME_PREFIX);
                Provision.showProvisioningUI(MainActivity.this, config);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

//        if (BuildConfig.FLAVOR_transport.equals("ble") && BLEProvisionLanding.isBleWorkDone) {
//            BLEProvisionLanding.bleTransport.disconnect();
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String generateProductDSN() {
        return UUID.randomUUID().toString();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Provision.REQUEST_PROVISIONING_CODE &&
                resultCode == RESULT_OK) {
            setResult(resultCode);
            finish();
        }
    }
}
