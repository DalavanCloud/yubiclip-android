/*
 * Copyright (c) 2013 Yubico AB
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.yubico.yubiclip;

import android.app.*;
import android.content.*;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.yubico.yubiclip.scancode.KeyboardLayout;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HandleOTPActivity extends Activity {
    private static final Pattern OTP_PATTERN = Pattern.compile("^https://my\\.yubico\\.com/neo/([a-zA-Z0-9!]+)$");
    private static final int DATA_OFFSET = 23;

    private SharedPreferences prefs;

    @Override
    public void onResume() {
        super.onResume();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Matcher matcher = OTP_PATTERN.matcher(getIntent().getDataString());
        if (matcher.matches()) {
            handleOTP(matcher.group(1));
        } else {
            Parcelable[] raw = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            byte[] bytes = ((NdefMessage) raw[0]).toByteArray();
            bytes = Arrays.copyOfRange(bytes, DATA_OFFSET, bytes.length);
            String layout = prefs.getString(getString(R.string.pref_layout), "US");
            KeyboardLayout kbd = KeyboardLayout.forName(layout);
            handleOTP(kbd.fromScanCodes(bytes));
        }

        finish();
    }

    private void handleOTP(String data) {
        if(prefs.getBoolean(getString(R.string.pref_clipboard), true)) {
            copyToClipboard(data);
        }
        if(prefs.getBoolean(getString(R.string.pref_notification), false)) {
            displayNotification(data);
        }
    }

    private void copyToClipboard(String data) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(ClearClipboardService.YUBI_CLIP_DATA, data);
        clipboard.setPrimaryClip(clip);
        int timeout = prefs.getInt(getString(R.string.pref_timeout), -1);
        if (timeout > 0) {
            startService(new Intent(this, ClearClipboardService.class));
        }
        Toast.makeText(getApplication(), R.string.copied, Toast.LENGTH_SHORT).show();
    }

    private void displayNotification(String data) {
        Notification.Builder nBuilder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_yubiclip)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(data);
        int nId = 0;
        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.notify(nId, nBuilder.getNotification());
    }
}