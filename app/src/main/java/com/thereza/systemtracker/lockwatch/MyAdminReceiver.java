package com.thereza.systemtracker.lockwatch;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.util.Log;

import com.thereza.systemtracker.R;
import com.thereza.systemtracker.appUtil.DetectConnection;
import com.thereza.systemtracker.appUtil.GMailSender;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by theReza on 11/29/2017.
 */

public class MyAdminReceiver extends DeviceAdminReceiver {

    public void onEnabled(Context ctxt, Intent intent) {
        ComponentName cn=new ComponentName(ctxt, MyAdminReceiver.class);
        DevicePolicyManager mgr=
                (DevicePolicyManager)ctxt.getSystemService(Context.DEVICE_POLICY_SERVICE);

        mgr.setPasswordQuality(cn,
                DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC);

        onPasswordChanged(ctxt, intent);
    }

    @Override
    public void onPasswordChanged(Context ctxt, Intent intent) {
        DevicePolicyManager mgr= (DevicePolicyManager)ctxt.getSystemService(Context.DEVICE_POLICY_SERVICE);
        int msgId;

        if (mgr.isActivePasswordSufficient()) {
            msgId= R.string.compliant;
        }
        else {
            msgId=R.string.not_compliant;
        }

        //Toast.makeText(ctxt, msgId, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPasswordFailed(final Context ctxt, Intent intent) {
        //Toast.makeText(ctxt, R.string.password_failed, Toast.LENGTH_LONG).show();
        DevicePolicyManager mgr = (DevicePolicyManager) ctxt.getSystemService(Context.DEVICE_POLICY_SERVICE);
        final int no = mgr.getCurrentFailedPasswordAttempts();
        SharedPreferences emailAddress = ctxt.getSharedPreferences("Email", MODE_PRIVATE);
        final String email = emailAddress.getString("email",null);

        final String toMailAddress = "intruderalert4mobile@gmail.com";
        final String pass = "cuetcse12";
        final GMailSender sender = new GMailSender(toMailAddress, pass);
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
        final String formattedDate = df.format(c.getTime());
        c.add(Calendar.HOUR,6);

        SharedPreferences preftAttemp = ctxt.getSharedPreferences("attemptRecord",MODE_PRIVATE);
        String atemptNo = preftAttemp.getString("attempTcount","0");

        if (no >= Integer.parseInt(atemptNo)){

            if(DetectConnection.checkInternetConnection(ctxt) && !email.equals("")){
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            sender.sendMail("Intruder Alert!",
                                    "Intruder Alert detected someone tries to unlock your device on "+formattedDate,
                                    toMailAddress,
                                    email);

                            SharedPreferences.Editor lockEditor = ctxt.getApplicationContext().getSharedPreferences("LockEntry", MODE_PRIVATE).edit();
                            lockEditor.putInt("unlockAttempt",0);
                            lockEditor.commit();

                        } catch (Exception e) {
                            Log.e("SendMail", e.getMessage(), e);
                        }
                    }

                }).start();
            }
            else {

                SharedPreferences.Editor lockEditor = ctxt.getSharedPreferences("LockEntry", MODE_PRIVATE).edit();
                lockEditor.putInt("unlockAttempt",no);
                lockEditor.putString("dateTime",formattedDate);
                lockEditor.commit();
            }
        }
    }

    @Override
    public void onPasswordSucceeded(Context ctxt, Intent intent) {
        //Toast.makeText(ctxt, R.string.password_success, Toast.LENGTH_LONG).show();
    }
}