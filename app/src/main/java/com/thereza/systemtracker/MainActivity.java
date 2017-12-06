package com.thereza.systemtracker;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.thereza.systemtracker.appUtil.MsgPushService;
import com.thereza.systemtracker.filetracker.TotalFiles;
import com.thereza.systemtracker.lockwatch.MyAdminReceiver;
import com.thereza.systemtracker.simcardtracker.TelephonyInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        CompoundButton.OnCheckedChangeListener{
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    public static final int REQUEST_CODE_PHONE_STATE_READ = 100;
    private int checkedPermission = PackageManager.PERMISSION_DENIED;
    public static final String MY_PREFS_NAME = "MyPrefsFile";
    public static final String MY_PREFS_NAME_FOR_FILE = "MyFileNumber";
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    static final String TAG = "MainActivity";
    File myDirectory;
    @RequiresApi(api = Build.VERSION_CODES.M)
    Handler mHandler = new Handler();
    static final int ACTIVATION_REQUEST = 47;
    static DevicePolicyManager devicePolicyManager;
    static ComponentName deviceAdmin;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    TabLayout tabLayout;
    InterstitialAd mInterstitialAd;

    static Switch deviceSwitch;
    static Spinner spinnerDropDown;
    static String[] attemptNumber = {
            "Please Select",
            "1",
            "2",
            "3",
            "4",
            "5"
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabbed);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_full_screen));
        AdRequest adRequest = new AdRequest.Builder()
                .build();
        mInterstitialAd.loadAd(adRequest);
        mInterstitialAd.setAdListener(new AdListener() {
            public void onAdLoaded() {
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                }
            }
        });




        /*
        * Start Service Class for never kill
        */
        startService(new Intent(getBaseContext(), MsgPushService.class));

        /*
        * Create the adapter that will return a fragment for each of the three
        * primary sections of the activity.
        * */
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);


        SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
        String emailadd = emailAddress.getString("email","");
        if( emailadd.equals("")){
            SharedPreferences.Editor editorrr = getSharedPreferences("Email", MODE_PRIVATE).edit();
            editorrr.putString("email","");
            editorrr.commit();
        }

        /*
        *
        * Permission for PHONE STATE READING and EXTERNAL STORAGE READING AND WRITTING
        *
        * */

        checkAndRequestPermissions();

        /*
        * Apps Tracking Initialization
        * Fetch Total number of apps installed and their name and stored into file
        * Use this file data for tracking apps install/uninstall
        * */
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        myDirectory = new File(Environment.getExternalStorageDirectory(), "System Tracker");
        if(!myDirectory.exists()) {
            myDirectory.mkdirs();
        }
        File f = new File(myDirectory, "appsList.txt");
        if(!f.exists()){
            FileWriter writer = null;
            StringBuilder data = new StringBuilder("");
            for (ApplicationInfo packageInfo : apps) {
                //Toast.makeText(getApplicationContext(),""+packageInfo.packageName,Toast.LENGTH_LONG).show();
                data.append(pm.getApplicationLabel(packageInfo).toString()+"\n");
            }
            try {
                writer = new FileWriter(f);
                writer.append(data);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /*
        * Initialization SIM card Tracking
        * Fetch SIM IMSI Number and Carrier name and store into sharedprefenence
        * This Shared Prefence data used for further  tracking
        * */

        if(checkAndRequestPermissions()){
            TelephonyInfo telephonyInfo = TelephonyInfo.getInstance(this);
            String imeiSIM1 = telephonyInfo.getImsiSIM1();
            String imeiSIM2 = telephonyInfo.getImsiSIM2();
            boolean isSIM1Ready = telephonyInfo.isSIM1Ready();
            boolean isSIM2Ready = telephonyInfo.isSIM2Ready();
            boolean isDualSIM = telephonyInfo.isDualSIM();

            SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
            String SIM1 = prefs.getString("SIM1", "default");
            String  SIM2 = prefs.getString("SIM2", "default");

            //Toast.makeText(this, "SIM1 :"+SIM1+" SIM2 :"+SIM2, Toast.LENGTH_SHORT).show();

            if(isSIM1Ready && isSIM2Ready){
                //Toast.makeText(this, "SIM1 Ready :"+isSIM1Ready +"Sim2 Ready :"+isSIM2Ready, Toast.LENGTH_SHORT).show();
                if (SIM1.equals("default") && SIM2.equals("default") )
                {
                    SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putString("SIM1", imeiSIM1);
                    editor.putString("SIM2", imeiSIM2);
                    editor.commit();
                }
            }

            else if(isSIM1Ready){
                Toast.makeText(this, "SIM1 Ready :"+isSIM1Ready, Toast.LENGTH_SHORT).show();
                if (SIM1.equals("default") )
                {
                    SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putString("SIM1", imeiSIM1);
                    editor.putString("SIM2", "");
                    editor.commit();
                }
            }
            else if(isSIM2Ready){
                Toast.makeText(this, "Sim2 Ready :"+isSIM2Ready, Toast.LENGTH_SHORT).show();
                if (SIM2.equals("default") )
                {
                    SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putString("SIM1", "");
                    editor.putString("SIM2", imeiSIM2);
                    editor.commit();
                }
            }

        /*
        * File Tracking Initialization
        * Fetch Total File from SD card and saved into sharedprefernce
        * Use this shsredpreference data for tracking file if delete or add
        * */
            TotalFiles a = new TotalFiles();
            Integer audioSize=a.geAudioFilePaths(getApplicationContext()).size();
            Integer videoSize=a.geVideoFilePaths(getApplicationContext()).size();
            Integer imageSize=a.geImagetFilePaths(getApplicationContext()).size();
            SharedPreferences prefs_for_file = getSharedPreferences(MY_PREFS_NAME_FOR_FILE, MODE_PRIVATE);
            Integer audioFile = prefs_for_file.getInt("AUDIO", 0);
            Integer videoFile = prefs_for_file.getInt("VIDEO", 0);
            Integer imageFile = prefs_for_file.getInt("IMAGE", 0);
            if (audioFile == 0 && videoFile == 0 && imageFile == 0){
                SharedPreferences.Editor editoddr = getSharedPreferences(MY_PREFS_NAME_FOR_FILE, MODE_PRIVATE).edit();
                editoddr.putInt("AUDIO",audioSize);
                editoddr.putInt("VIDEO",videoSize);
                editoddr.putInt("IMAGE",imageSize);
                editoddr.commit();
            }

        }


    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }
        @Override
        public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_tabbed, container, false);
            //TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            //textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));


            // Get reference of SpinnerView from layout/main_activity.xml
            spinnerDropDown =(Spinner)rootView.findViewById(R.id.spinnerAttempt);

            ArrayAdapter<String> adapter= new ArrayAdapter<String>(getActivity(),android.
                    R.layout.simple_spinner_dropdown_item ,attemptNumber);
            spinnerDropDown.setAdapter(adapter);

            spinnerDropDown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    // Get select item
                    int sid=spinnerDropDown.getSelectedItemPosition();

                    if(sid != 0){
                        SharedPreferences.Editor atempEditor = getActivity().getSharedPreferences("attemptRecord",MODE_PRIVATE).edit();
                        atempEditor.putString("attempTcount", attemptNumber[sid].toString());
                        atempEditor.commit();
                    }

                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });

            SharedPreferences preftAttemp = getActivity().getSharedPreferences("attemptRecord",MODE_PRIVATE);
            String atemptNo = preftAttemp.getString("attempTcount","0");
            attemptNumber[0] = "Current Value : "+atemptNo;
            //Toast.makeText(getActivity().getApplicationContext(),""+atemptNo,Toast.LENGTH_LONG).show();



            AdView mAdView,mAdview1;
            mAdView = (AdView) rootView.findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder()
                    .build();
            mAdView.loadAd(adRequest);


            if(getArguments().getInt(ARG_SECTION_NUMBER)==1){
                LinearLayout scroll = rootView.findViewById(R.id.scrollView);
                scroll.setVisibility(View.VISIBLE);

            }
            if(getArguments().getInt(ARG_SECTION_NUMBER)==2){
                final TextView txt = rootView.findViewById(R.id.emailtext);
                final TextView autoStart = rootView.findViewById(R.id.autostarttext);
                LinearLayout autoStartLayout = rootView.findViewById(R.id.autoStartId);
                autoStartLayout.setVisibility(View.INVISIBLE);
                LinearLayout l = rootView.findViewById(R.id.settingLayout);
                l.setVisibility(View.VISIBLE);
                SharedPreferences emailAddress = getActivity().getSharedPreferences("Email", MODE_PRIVATE);
                String emailadd = emailAddress.getString("email","");
                if(emailadd.equals("")){
                    txt.setText("Set Your Email Address");
                }
                else{
                    txt.setText(emailadd);
                }
                txt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                        final View dialogView = inflater.inflate(R.layout.custom_dialog, container, false);
                        dialogBuilder.setView(dialogView);
                        final EditText edt = (EditText) dialogView.findViewById(R.id.editTextEmail);
                        dialogBuilder.setTitle("Enter Email");
                        dialogBuilder.setMessage("Set Your Email to get Notification");
                        dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //do something with edt.getText().toString();
                                String email = edt.getText().toString();
                                if(!email.equals("")){
                                    SharedPreferences.Editor editorrr = getActivity().getSharedPreferences("Email", MODE_PRIVATE).edit();
                                    editorrr.putString("email",email);
                                    editorrr.commit();
                                    txt.setText(email);
                                }

                            }
                        });
                        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //pass
                            }
                        });
                        AlertDialog b = dialogBuilder.create();
                        b.show();
                    }
                });

                if (Helper.isAppRunning(getActivity(), "com.thereza.systemtracker")) {
                    autoStart.setText("Running!!");
                    autoStart.setTextColor(Color.parseColor("#4caf50"));
                }
                String manufacturer = "xiaomi";
                if(manufacturer.equalsIgnoreCase(android.os.Build.MANUFACTURER)) {
                    //this will open auto start screen where user can enable permission for your app
                    autoStartLayout.setVisibility(View.VISIBLE);
                    autoStart.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                            startActivity(intent);
                            autoStart.setText("Running!!");
                            autoStart.setTextColor(Color.parseColor("#4caf50"));

                        }
                    });
                }


                deviceSwitch = (Switch) rootView.findViewById(R.id.deviceAdminSwitch);
                deviceSwitch.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener) getActivity());
                deviceAdmin = new ComponentName(getActivity(), MyAdminReceiver.class);
                devicePolicyManager = (DevicePolicyManager)getActivity().getSystemService(DEVICE_POLICY_SERVICE);

                if(devicePolicyManager != null && deviceAdmin != null){
                    deviceSwitch.setChecked(true);
                }
                else{
                    deviceSwitch.setChecked(false);
                }


            }

            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "APPS INFO";
                case 1:
                    return "SETTINGS";
            }
            return null;
        }
    }


    public static class Helper {

        public static boolean isAppRunning(final Context context, final String packageName) {
            final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            final List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
            if (procInfos != null)
            {
                for (final ActivityManager.RunningAppProcessInfo processInfo : procInfos) {
                    if (processInfo.processName.equals(packageName)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            Log.d("Settings", "Act");
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }*/

        if (id == R.id.share) {
            Log.d("Settings", "Act");
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            String shareBody = "Here is the share content body";
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Subject Here");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, "Share via"));
            return true;
        }
        else if (id == R.id.rateUsa) {
            Uri uri = Uri.parse("market://details?id=" + getApplicationContext().getPackageName());
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            // To count with Play market backstack, After pressing back button,
            // to taken back to our application, we need to add following flags to intent.
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            try {
                startActivity(goToMarket);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + getApplicationContext().getPackageName())));
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCheckedChanged(CompoundButton button, boolean isChecked) {
        if (isChecked) {
            // Activate device administration
            Intent intent = new Intent(
                    DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    deviceAdmin);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Your boss told you to do this");
            startActivityForResult(intent, ACTIVATION_REQUEST);
        }
        Log.d(TAG, "onCheckedChanged to: " + isChecked);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTIVATION_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "Administration enabled!");
                    //toggleButton.setChecked(true);
                    deviceSwitch.setChecked(true);
                } else {
                    Log.i(TAG, "Administration enable FAILED!");
                    //toggleButton.setChecked(false);
                    deviceSwitch.setChecked(false);
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private  boolean checkAndRequestPermissions() {
        int camera = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA);
        int storage = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int loc = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE);
        int loc2 = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (camera != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.CAMERA);
        }
        if (storage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (loc2 != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (loc != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty())
        {
            ActivityCompat.requestPermissions(this,listPermissionsNeeded.toArray
                    (new String[listPermissionsNeeded.size()]),REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

}
