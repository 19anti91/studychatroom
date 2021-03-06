package com.oop.projectgroup10.studychatroom;

import android.*;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.LayoutRes;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Inflater;

//This class has everything related to chat rooms, from its members, to its messages and settings

public class ChatRooms extends AppCompatActivity {
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
    public static ViewGroup view = null;
    public static LinearLayout layout;
    public static Handler UIHandler;
    static boolean isActive = false;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static Context context;
    private static String[] PERMISSIONS_STORAGE = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    static {
        UIHandler = new Handler(Looper.getMainLooper());
    }

    public ListView usersFound;
    public Activity act = this;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    //This generates random id for items in the view
    public static int generateViewId() {
        for (; ; ) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }




    private static final int READ_REQUEST_CODE = 42;
    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_rooms);

        verifyStoragePermissions(this);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        Log.e("ROOOOM",pref.getString("currentChatRoom","adsasdasd"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(pref.getString("currentChatRoom", ""));


        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem settingsItem = menu.findItem(R.id.action_settings);
        settingsItem.setVisible(false);
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat_rooms, menu);
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





    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        View rootView;

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



        public void populateReceivedMsg(String message, String from, Activity activity, int ico) {


            View view = LayoutInflater.from(activity).inflate(R.layout.msg_from_them, null);
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity);
            String toGroup = pref.getString("userGroup", "");

            if (!message.isEmpty() && from.equals(toGroup)) {

                layout.addView(view);
                TextView msgFromMe = (TextView) view.findViewById(R.id.msgFromThemTxt);
                TextView userName = (TextView) view.findViewById(R.id.msgFromGroup);
                userName.setId(generateViewId());
                userName.setText(pref.getString("userFrom", ""));
                ImageView icon = getIcon(ico, R.id.msgFromThemIcon, view);

                msgFromMe.setId(generateViewId());
                if(message.contains("https")){
                    message =  URLDecoder.decode(message);
                    Log.e("Received", message);
                }
                final String msg =message;

                msgFromMe.setId(generateViewId());

                if(msg.split("/")[0].equals("https:") && msg.split("/")[2].equals("s3.amazonaws.com")){
                    msgFromMe.setText(msg.split("/")[5] + " has been attached" + new String(Character.toChars(0x1F4CE)));
                    msgFromMe.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new getFileFromAmazonTask().execute(msg);
                        }
                    });
                }else{
                    msgFromMe.setText(msg);
                }

                layout.invalidate();


            }

        }

        //this will get the path of the image based on the version of the OSw
        public static String getPath(final Context context, final Uri uri) {

            final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

            // DocumentProvider
            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }


                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {

                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    return getDataColumn(context, contentUri, null, null);
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[] {
                            split[1]
                    };

                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
            // MediaStore (and general)
            else if ("content".equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(context, uri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }

            return null;
        }

        public static String getDataColumn(Context context, Uri uri, String selection,
                                           String[] selectionArgs) {

            Cursor cursor = null;
            final String column = "_data";
            final String[] projection = {
                    column
            };

            try {
                cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                        null);
                if (cursor != null && cursor.moveToFirst()) {
                    final int column_index = cursor.getColumnIndexOrThrow(column);
                    return cursor.getString(column_index);
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
            return null;
        }


        public static boolean isExternalStorageDocument(Uri uri) {
            return "com.android.externalstorage.documents".equals(uri.getAuthority());
        }


        public static boolean isDownloadsDocument(Uri uri) {
            return "com.android.providers.downloads.documents".equals(uri.getAuthority());
        }

        public static boolean isMediaDocument(Uri uri) {
            return "com.android.providers.media.documents".equals(uri.getAuthority());
        }

        void uploadToS3(File file, String filename){

            // Initialize the Amazon Cognito credentials provider
            CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                    getContext(),
                    "us-east-1:6bad921a-feca-4b70-b3d0-0d217a6b1d2c", // Identity Pool ID
                    Regions.US_EAST_1 // Region
            );

//Amazon handler to upload and download files
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

            AmazonS3 s3 = new AmazonS3Client(credentialsProvider);
            TransferUtility transferUtility = new TransferUtility(s3, getContext());
            s3.setRegion(Region.getRegion(Regions.US_EAST_1));

            TransferObserver observer = transferUtility.upload(
                    "studychatroom","documents/"+filename,file

            );
            String path = "https://s3.amazonaws.com/studychatroom/documents/"+filename;

            sendMessage(view, path);


        }
        //this will set the image icon
        public static ImageView getIcon(int icon, int id, View view) {
            ImageView imageView = (ImageView) view.findViewById(id);
            switch (icon) {
                case 0:
                    imageView.setImageResource(R.drawable.ic_femalelight);
                    break;
                case 1:
                    imageView.setImageResource(R.drawable.ic_femaledark);
                    break;
                case 2:
                    imageView.setImageResource(R.drawable.ic_femaledarker);
                    break;
                case 3:
                    imageView.setImageResource(R.drawable.ic_maleredhair);
                    break;
                case 4:
                    imageView.setImageResource(R.drawable.ic_malelight);
                    break;
                case 5:
                    imageView.setImageResource(R.drawable.ic_maledarker);
                    break;

            }
            return imageView;
        }

        public class SimpleImageArrayAdapter extends ArrayAdapter<Integer> {
            private Integer[] images;

            public SimpleImageArrayAdapter(Context context, Integer[] images) {
                super(context, android.R.layout.simple_spinner_item, images);
                this.images = images;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return getImageForPosition(position);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return getImageForPosition(position);
            }

            private View getImageForPosition(int position) {
                TextView emoji = new TextView(getActivity());
                emoji.setTextSize(25);
                emoji.setText(new String (Character.toChars(images[position])));
                return emoji;
            }

        }
        ListView listView;
        CustomListAdapter adapter;

        //this will get the file selected from the file picker
        @Override
        public void onActivityResult(int requestCode, int resultCode,
                                     Intent resultData) {

            if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

                Uri uri = null;
                String displayName = "";
                if (resultData != null) {
                    uri = resultData.getData();
                    Cursor cursor = getActivity().getContentResolver()
                            .query(uri, null, null, null, null, null);

                    try {

                        if (cursor != null && cursor.moveToFirst()) {

                            displayName = cursor.getString(
                                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

                        }
                    } finally {
                        cursor.close();
                    }
                    File fileToUpload = new File(getPath(getActivity(),uri));

                    uploadToS3(fileToUpload,displayName);


                }
            }
        }
        public void performFileSearch() {


            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");

            startActivityForResult(intent, READ_REQUEST_CODE);
        }
        @Override
        public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                                 Bundle savedInstanceState) {


            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            final SharedPreferences.Editor edit = pref.edit();


            int section = getArguments().getInt(ARG_SECTION_NUMBER);

            //Members
            if (section == 2) {
                rootView = inflater.inflate(R.layout.activity_private_message_user_list, container, false);
                new SendDataAsync(getActivity(), getActivity()).execute("getAllUsersFromChatRoom", String.valueOf(pref.getInt("userid", 0)), pref.getString("currentChatRoom", ""));
                listView = (ListView) rootView.findViewById(R.id.userList);


                Integer[] userIcon = {};
                String[] userlist = {};

                JSONArray userList;
                try {
                    userList = new JSONArray(pref.getString("usersFrom"+pref.getString("currentChatRoom",""), ""));
                    userlist = new String[userList.length()];
                    userIcon = new Integer[userList.length()];
                    for (int i = 0; i < userList.length(); i++) {
                        JSONObject user = (JSONObject) userList.get(i);
                        userlist[i] = user.getString("username");
                        userIcon[i] = Integer.valueOf(user.getString("usericon"));

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                adapter = new CustomListAdapter(getActivity(), userlist, userIcon);
                final Integer[] icon = userIcon;
                listView.setAdapter(adapter);


            } else if (section == 1) {
                rootView = inflater.inflate(R.layout.activity_private_message, container, false);
                verifyStoragePermissions(getActivity());
                layout = (LinearLayout) rootView.findViewById(R.id.privMsgLayout);
                view = (ViewGroup) rootView.findViewById(R.id.privMsgLayout);

                new MessageAsync(getActivity(), getActivity(), view, layout).execute("getGroupMsg", String.valueOf(pref.getInt("userid", 0)), pref.getString("currentChatRoom", ""));
                ImageView send = (ImageView) rootView.findViewById(R.id.sendMsgBtn);
                send.setClickable(true);
                send.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendMessage(v,"");
                    }
                });

                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUI(new Runnable() {
                            @Override
                            public void run() {

                                if (pref.getInt("hasMessage", 0) == 1) {
                                    Log.d("MESSAGE", pref.getString("message", ""));
                                    if(!pref.getString("userFrom","").equals(pref.getString("username","")))
                                    populateReceivedMsg(pref.getString("message", ""), pref.getString("userGroup", ""), getActivity(), Integer.valueOf(pref.getString("groupUserIcon", "")));

                                    edit.putInt("hasMessage", 0);
                                    edit.apply();

                                }


                            }
                        });

                    }
                }, 0, 500);
                SimpleImageArrayAdapter adapter =new SimpleImageArrayAdapter(getActivity(),new Integer[]{
                        0x0,
                        0x1F602,
                        0x1F603,
                        0x1F606,
                        0x1F60B,
                        0x1F61D,
                        0x1F61C,
                        0x1F620,
                        0x2705,
                        0x270C,
                        0x1F601
                });

                final Spinner emojiSpinner = (Spinner)rootView.findViewById(R.id.emojiSpinner);

                emojiSpinner.setAdapter(adapter);
                emojiSpinner.setSelection(-1);
                emojiSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Integer emojis [] = new Integer[11];
                        emojis[0] = 0x0;
                        emojis[1] = 0x1F602;
                        emojis[2] = 0x1F603;
                        emojis[3] = 0x1F606;
                        emojis[4] = 0x1F60B;
                        emojis[5] = 0x1F61D;
                        emojis[6] = 0x1F61C;
                        emojis[7] = 0x1F620;
                        emojis[8] = 0x2705;
                        emojis[9] = 0x270C;
                        emojis[10] = 0x1F601;
                        EditText msg = (EditText)rootView.findViewById(R.id.msgToSend);
                        msg.setText(msg.getText().toString()+ new String(Character.toChars(emojis[position])));
                        emojiSpinner.setSelection(0);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                ImageView attach = (ImageView)rootView.findViewById(R.id.attach);

                attach.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        performFileSearch();
                    }
                });


            } else {
                int userId = pref.getInt("userid",0);
                int ownerId = pref.getInt("currentChatOwner",0);
                String userType = pref.getString("type","");
                if(userId != ownerId && !userType.equals("Administrator")){
                    rootView = inflater.inflate(R.layout.manage_room_user, container, false);

                    TextView leaveRoom = (TextView) rootView.findViewById(R.id.leaveRoomMan);

                    leaveRoom.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new AlertDialog.Builder(getContext())
                                    .setTitle("Leave Chat Room")
                                    .setMessage("Are you sure you want to leave the Chat Room?")
                                    .setIcon(R.drawable.ic_alert)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            new SendDataAsync(getContext(),getActivity()).execute("leaveChatRoom", String.valueOf(pref.getInt("userid",0)),pref.getString("currentChatRoom",""));
                                            Intent goToDash = new Intent(getContext(),DashBoard.class);
                                            startActivity(goToDash);
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {

                                        }
                                    })
                                    .show();
                        }
                    });

                }else{
                    rootView = inflater.inflate(R.layout.manage_room, container, false);

                    final TextView roomName = (TextView) rootView.findViewById(R.id.roomNameMan);
                    final Switch makeRoomPriv = (Switch) rootView.findViewById(R.id.makePrivMan);
                    makeRoomPriv.setChecked(!(pref.getInt("currentChatRoomPriv",3)==0));
                    final Switch passProt = (Switch) rootView.findViewById(R.id.passwordProtectedSwitch);
                    passProt.setChecked(!(ChatRoomsTwoTabs.hashPass("").equals(pref.getString("currentChatRoomPass","5"))));
                    final TextView changePass = (TextView) rootView.findViewById(R.id.changePasswordMan);
                    TextView inviteUser = (TextView) rootView.findViewById(R.id.inviteUsersMan);
                    TextView banUser = (TextView) rootView.findViewById(R.id.banUsersMan);
                    TextView deleteRoom = (TextView) rootView.findViewById(R.id.deleteRoomMan);




                    final String roomN = pref.getString("currentChatRoom","");
                    roomName.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final EditText name = new EditText(getActivity());
                            name.setText(pref.getString("currentChatRoom",""));

                            new AlertDialog.Builder(getActivity())
                                    .setTitle("Change Room Name")
                                    .setMessage("Please choose the new name")
                                    .setView(name)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            if(!name.getText().toString().equals(""))
                                            {
                                                new SendDataAsync(getActivity(),getActivity()).execute("updateChatRoomName",String.valueOf(pref.getInt("userid",0)), roomN,name.getText().toString());
                                                name.setText(pref.getString("currentChatRoom",""));
                                            }else{
                                                Toast.makeText(getActivity(),"Room name cannot be empty", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    })
                                    .setIcon(R.drawable.ic_alert)
                                    .show();
                        }
                    });

                    makeRoomPriv.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {


                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                            final CompoundButton.OnCheckedChangeListener list = this;


                            if(isChecked){
                                new AlertDialog.Builder(getActivity())
                                        .setTitle("Do you want to proceed?")
                                        .setMessage("The room will only be visible to its members")
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                new SendDataAsync(getActivity(),getActivity()).execute("makeRoomPriv",String.valueOf(pref.getInt("userid",0)),roomN,String.valueOf(isChecked));
                                            }
                                        })
                                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                                makeRoomPriv.setOnCheckedChangeListener(null);
                                                makeRoomPriv.setChecked(!isChecked);
                                                makeRoomPriv.setOnCheckedChangeListener(list);
                                            }
                                        })
                                        .setIcon(R.drawable.ic_alert)
                                        .show();
                            }else{
                                new AlertDialog.Builder(getActivity())
                                        .setTitle("Do you want to proceed?")
                                        .setMessage("The room will be visible to everyone")
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                new SendDataAsync(getActivity(),getActivity()).execute("makeRoomPriv",String.valueOf(pref.getInt("userid",0)),pref.getString("currentChatRoom",""),String.valueOf(isChecked));

                                            }
                                        })

                                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                makeRoomPriv.setOnCheckedChangeListener(null);
                                                makeRoomPriv.setChecked(!isChecked);
                                                makeRoomPriv.setOnCheckedChangeListener(list);
                                            }
                                        })
                                        .setIcon(R.drawable.ic_alert)
                                        .show();
                            }


                        }
                    });

                    //type current password to remove password



                    passProt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton,final boolean isChecked) {

                            final CompoundButton.OnCheckedChangeListener list = this;
                            final EditText password = new EditText(getContext());
                            final EditText confirmPassword = new EditText(getContext());

                            final LinearLayout layout = new LinearLayout(getContext());

                            layout.setOrientation(LinearLayout.VERTICAL);

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            params.setMargins(50,0,0,0);
                            password.setHint("Password            ");
                            confirmPassword.setHint("Confirm Password    ");
                            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            password.setLayoutParams(params);
                            confirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            confirmPassword.setLayoutParams(params);

                            TextWatcher passwordValidation = new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {

                                    if (!(password.getText().toString().equals(confirmPassword.getText().toString()))) {

                                        confirmPassword.setTextColor(Color.parseColor("#FE0417"));
                                        password.setTextColor(Color.parseColor("#FE0417"));

                                    } else {

                                        confirmPassword.setTextColor(Color.parseColor("#28B463"));
                                        password.setTextColor(Color.parseColor("#28B463"));

                                    }
                                }

                                @Override
                                public void afterTextChanged(Editable s) {

                                }
                            };


                            confirmPassword.addTextChangedListener(passwordValidation);
                            layout.addView(password);
                            layout.addView(confirmPassword);

                            if(isChecked){
                                new AlertDialog.Builder(getContext())
                                        .setTitle("Please type and confirm the password")
                                        .setView(layout)
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int k) {

                                                if((password.getText().toString().equals(confirmPassword.getText().toString())) && !password.getText().toString().equals("")) {
                                                    byte byteData[] = null;
                                                    try {

                                                        MessageDigest md = MessageDigest.getInstance("SHA-256");
                                                        md.update(password.getText().toString().getBytes());
                                                        byteData = md.digest();
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                    StringBuilder pass = new StringBuilder();
                                                    for (int i = 0; i < byteData.length; i++) {
                                                        pass.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
                                                    }

                                                    new SendDataAsync(getActivity(), getActivity()).execute("setupRoomPassword", String.valueOf(pref.getInt("userid", 0)), pref.getString("currentChatRoom", ""), pass.toString());
                                                    edit.putString("currentChatRoomPass",pass.toString());
                                                    edit.apply();
                                                }
                                            }
                                        })
                                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                passProt.setOnCheckedChangeListener(null);
                                                passProt.setChecked(!isChecked);
                                                passProt.setOnCheckedChangeListener(list);
                                            }
                                        })
                                        .setIcon(R.drawable.ic_alert)
                                        .show();
                            }
                            else{
                                layout.removeView(confirmPassword);
                                new AlertDialog.Builder(getContext())
                                        .setTitle("Please enter the room password")
                                        .setView(layout)
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int k) {

                                                byte byteData[] = null;
                                                try {

                                                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                                                    md.update(password.getText().toString().getBytes());
                                                    byteData = md.digest();
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                                StringBuilder pass = new StringBuilder();
                                                for (int i = 0; i < byteData.length; i++) {
                                                    pass.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
                                                }

                                                if((pass.toString().equals(pref.getString("currentChatRoomPass",""))))
                                                {
                                                    new SendDataAsync(getActivity(), getActivity()).execute("removeChatRoomPassword", String.valueOf(pref.getInt("userid", 0)), pref.getString("currentChatRoom", ""));
                                                    edit.putString("currentChatRoomPass",pass.toString());
                                                    edit.apply();
                                                }else{
                                                    Toast.makeText(getContext(),"Incorrect Password. Please try again", Toast.LENGTH_LONG).show();
                                                    passProt.setOnCheckedChangeListener(null);
                                                    passProt.setChecked(!isChecked);
                                                    passProt.setOnCheckedChangeListener(list);
                                                }

                                            }
                                        })
                                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                passProt.setOnCheckedChangeListener(null);
                                                passProt.setChecked(!isChecked);
                                                passProt.setOnCheckedChangeListener(list);
                                            }
                                        })
                                        .setIcon(R.drawable.ic_alert)
                                        .show();
                            }

                        }
                    });

                    changePass.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {


                            final EditText password = new EditText(getContext());
                            final EditText confirmPassword = new EditText(getContext());
                            final EditText passwordOld = new EditText(getContext());
                            final LinearLayout layout = new LinearLayout(getContext());

                            layout.setOrientation(LinearLayout.VERTICAL);

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            params.setMargins(50,0,0,0);
                            password.setHint("Password            ");
                            confirmPassword.setHint("Confirm Password    ");
                            passwordOld.setHint("Old Password        ");
                            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            passwordOld.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            password.setLayoutParams(params);
                            passwordOld.setLayoutParams(params);
                            confirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            confirmPassword.setLayoutParams(params);

                            TextWatcher passwordValidation = new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {

                                    if (!(password.getText().toString().equals(confirmPassword.getText().toString()))) {

                                        confirmPassword.setTextColor(Color.parseColor("#FE0417"));
                                        password.setTextColor(Color.parseColor("#FE0417"));

                                    } else {

                                        confirmPassword.setTextColor(Color.parseColor("#28B463"));
                                        password.setTextColor(Color.parseColor("#28B463"));

                                    }
                                }

                                @Override
                                public void afterTextChanged(Editable s) {

                                }
                            };


                            confirmPassword.addTextChangedListener(passwordValidation);
                            layout.addView(passwordOld);

                            layout.addView(password);
                            layout.addView(confirmPassword);

                            new AlertDialog.Builder(getContext())
                                    .setTitle("Please type and confirm the password")
                                    .setView(layout)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int k) {


                                            byte byteData[] = null;
                                            byte byteData2[] = null;
                                            try {

                                                MessageDigest md = MessageDigest.getInstance("SHA-256");
                                                MessageDigest md1 = MessageDigest.getInstance("SHA-256");
                                                md.update(password.getText().toString().getBytes());
                                                md1.update(passwordOld.getText().toString().getBytes());
                                                byteData = md.digest();
                                                byteData2 = md1.digest();

                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            StringBuilder pass = new StringBuilder();
                                            StringBuilder pass2 = new StringBuilder();
                                            for (int i = 0; i < byteData.length; i++) {
                                                pass.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
                                                pass2.append(Integer.toString((byteData2[i] & 0xff) + 0x100, 16).substring(1));
                                            }
                                            if((password.getText().toString().equals(confirmPassword.getText().toString())) && !password.getText().toString().equals("") && pass2.toString().equals(pref.getString("currentChatRoomPass",""))){
                                                new SendDataAsync(getActivity(), getActivity()).execute("setupRoomPassword", String.valueOf(pref.getInt("userid", 0)), pref.getString("currentChatRoom", ""), pass.toString());
                                                edit.putString("currentChatRoomPass",pass.toString());
                                                edit.apply();
                                            }else{
                                                Toast.makeText(getContext(),"Please check your password", Toast.LENGTH_LONG).show();
                                            }

                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {

                                        }
                                    })
                                    .setIcon(R.drawable.ic_alert)
                                    .show();



                        }
                    });
                    inviteUser.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //Join user to room and send notification

                            rootView = inflater.inflate(R.layout.activity_private_message_user_list, container, false);
                            new SendDataAsync(getActivity(), getActivity()).execute("getAllUsersFromChatRoom", String.valueOf(pref.getInt("userid", 0)), pref.getString("currentChatRoom", ""));
                            listView = (ListView) rootView.findViewById(R.id.userList);

                            Integer[] userIcon = {};
                            String[] userlist = {};
//  userList = new JSONArray(pref.getString("usersFrom"+pref.getString("currentChatRoom",""), ""));
                            JSONArray userList;
                            try {
                                userList = new JSONArray(pref.getString("userList", ""));
                                userlist = new String[userList.length()];
                                userIcon = new Integer[userList.length()];
                                for (int i = 0; i < userList.length(); i++) {
                                    JSONObject user = (JSONObject) userList.get(i);
                                    userlist[i] = user.getString("username");
                                    userIcon[i] = Integer.valueOf(user.getString("usericon"));

                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            adapter = new CustomListAdapter(getActivity(), userlist, userIcon);
                            final Integer[] icon = userIcon;
                            listView.setAdapter(adapter);

                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                    final String username = listView.getItemAtPosition(position).toString();
                                    Log.e("Username", username);
                                    new AlertDialog.Builder(getContext())
                                            .setTitle("Do you want to invite "+ username+"?")
                                            .setIcon(R.drawable.ic_alert)
                                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    new MessageAsync(getContext(),getActivity(),getView(),null).execute("inviteUser", username,pref.getString("currentChatRoom", ""));
                                                    Toast.makeText(getContext(),"Invitation Sent", Toast.LENGTH_LONG).show();
                                                }
                                            })
                                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {

                                                }
                                            })
                                            .show();
                                }
                            });
                            new AlertDialog.Builder(getContext())
                                    .setView(rootView)
                                    .setTitle("Select user to invite")
                                    .setMessage("You can only invite one user at a time")
                                    .setIcon(R.drawable.ic_alert)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {

                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {

                                        }
                                    })
                                    .show();
                        }
                    });
                    banUser.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {


                            rootView = inflater.inflate(R.layout.activity_private_message_user_list, container, false);
                            new SendDataAsync(getActivity(), getActivity()).execute("getAllUsersFromChatRoom", String.valueOf(pref.getInt("userid", 0)), pref.getString("currentChatRoom", ""));
                            listView = (ListView) rootView.findViewById(R.id.userList);

                            Integer[] userIcon = {};
                            String[] userlist = {};

                            JSONArray userList;
                            try {
                                userList = new JSONArray(pref.getString("usersFrom"+pref.getString("currentChatRoom",""), ""));
                                userlist = new String[userList.length()];
                                userIcon = new Integer[userList.length()];
                                for (int i = 0; i < userList.length(); i++) {
                                    JSONObject user = (JSONObject) userList.get(i);
                                    userlist[i] = user.getString("username");
                                    userIcon[i] = Integer.valueOf(user.getString("usericon"));

                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            adapter = new CustomListAdapter(getActivity(), userlist, userIcon);
                            final Integer[] icon = userIcon;
                            listView.setAdapter(adapter);

                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                    final String username = listView.getItemAtPosition(position).toString();
                                    Log.e("Username", username);
                                    new AlertDialog.Builder(getContext())
                                            .setTitle("Do you want to ban "+ username+" from this Chat Room?")
                                            .setIcon(R.drawable.ic_alert)
                                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    new MessageAsync(getContext(),getActivity(),getView(),null).execute("banUser", username,pref.getString("currentChatRoom", ""));
                                                    Toast.makeText(getContext(),"User banned", Toast.LENGTH_LONG).show();
                                                    //The user has been kicked on the balls!
                                                }
                                            })
                                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {

                                                }
                                            })
                                            .show();
                                }
                            });
                            new AlertDialog.Builder(getContext())
                                    .setView(rootView)
                                    .setTitle("Select user to ban")
                                    .setMessage("You can only ban one user at a time")
                                    .setIcon(R.drawable.ic_alert)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {

                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {

                                        }
                                    })
                                    .show();

                        }
                    });

                    deleteRoom.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new AlertDialog.Builder(getContext())
                                    .setTitle("Delete Room")
                                    .setMessage("The room will no longer be available. Are you sure you want to proceed?")
                                    .setIcon(R.drawable.ic_alert)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            new SendDataAsync(getContext(),getActivity()).execute("deleteChatRoom", String.valueOf(pref.getInt("userid",0)),pref.getString("currentChatRoom",""));
                                            Intent goToDash = new Intent(getContext(),DashBoard.class);
                                            startActivity(goToDash);
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {

                                        }
                                    })
                                    .show();
                        }
                    });


                }

            }


            return rootView;
        }

        //end
        public void sendMessage(View v, final String path) {

            View view = LayoutInflater.from(getActivity()).inflate(R.layout.msg_from_me, null);
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String toGroup = pref.getString("currentChatRoom", "");

            if (!getMessage().isEmpty() && path.isEmpty()) {
                new MessageAsync(getActivity(), getActivity(), view, layout).execute("groupMsg", String.valueOf(pref.getInt("userid", 0)), toGroup, getMessage());
                layout.addView(view);
                TextView msgFromMe = (TextView) rootView.findViewById(R.id.msgFromMeTxt);
                msgFromMe.setId(generateViewId());
                msgFromMe.setText(getMessage());
                ImageView icon = getIcon(pref.getInt("usericon", 0), R.id.messageFromMeIcon, view);
                layout.invalidate();
                EditText msgToSend = (EditText) rootView.findViewById(R.id.msgToSend);
                msgToSend.setText("");

                final ScrollView scroll = (ScrollView) rootView.findViewById(R.id.scrollPriv);
                scroll.post(new Runnable() {
                                @Override
                                public void run() {
                                    scroll.fullScroll(View.FOCUS_DOWN);
                                }
                            }

                );

            }
            if(!path.isEmpty()){
                String name = path.split("/")[5];
                new MessageAsync(getActivity(), getActivity(), view, layout).execute("groupMsg", String.valueOf(pref.getInt("userid", 0)), toGroup, path);
                layout.addView(view);
                TextView msgFromMe = (TextView) rootView.findViewById(R.id.msgFromMeTxt);
                msgFromMe.setId(generateViewId());
                msgFromMe.setText(name + "has been attached" + new String(Character.toChars(0x1F4CE)));
                msgFromMe.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new getFileFromAmazonTask().execute(path);
                    }
                });
                ImageView icon = getIcon(pref.getInt("usericon", 0), R.id.messageFromMeIcon, view);
                layout.invalidate();
                EditText msgToSend = (EditText) rootView.findViewById(R.id.msgToSend);
                msgToSend.setText("");

                final ScrollView scroll = (ScrollView) rootView.findViewById(R.id.scrollPriv);
                scroll.post(new Runnable() {
                                @Override
                                public void run() {
                                    scroll.fullScroll(View.FOCUS_DOWN);
                                }
                            }

                );
            }
        }

        private class getFileFromAmazonTask extends AsyncTask<String, Void,Void> {
            @Override
            protected Void doInBackground(String ...params){
                String name = params[0].split("/")[5];
                InputStream input = null;
                OutputStream output = null;
                HttpURLConnection connection = null;
                try{
                    URL urlData = new URL(params[0]);

                    connection = (HttpURLConnection) urlData.openConnection();
                    connection.connect();

                    input = connection.getInputStream();
                    output = new FileOutputStream(Environment.getExternalStorageDirectory()+"/studychatroom/"+name);

                    byte data[] = new byte[4096];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {

                        output.write(data, 0, count);
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }finally {
                    try{
                        if(input!=null){
                            input.close();
                        }
                        if(output!=null){
                            output.close();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                return null;
            }}

        public void recieveMessage(View v) {
            new MessageAsync(getActivity(), getActivity(), view, layout).execute();
        }

        public String getMessage() {
            String msg;
            EditText msgToSend = (EditText) rootView.findViewById(R.id.msgToSend);
            msg = msgToSend.getText().toString();

            return msg;
        }

        //Beginning of class custom list adapter
        public class CustomListAdapter extends ArrayAdapter<String> {

            private final Activity context;
            private final String[] itemname;
            private final Integer[] imgid;

            public CustomListAdapter(Activity context, String[] itemname, Integer[] imgid) {
                super(context, R.layout.user_list, itemname);

                this.context = context;
                this.itemname = itemname;
                this.imgid = imgid;
            }

            public View getView(int position, View view, ViewGroup parent) {
               final LayoutInflater inflater = context.getLayoutInflater();
                View rowView = inflater.inflate(R.layout.user_list, null, true);

                TextView txtTitle = (TextView) rowView.findViewById(R.id.userListText);
                ImageView imageView = (ImageView) rowView.findViewById(R.id.userListIcon);


                txtTitle.setText(itemname[position]);
                switch (imgid[position]) {
                    case 0:
                        imageView.setImageResource(R.drawable.ic_femalelight);
                        break;
                    case 1:
                        imageView.setImageResource(R.drawable.ic_femaledark);
                        break;
                    case 2:
                        imageView.setImageResource(R.drawable.ic_femaledarker);
                        break;
                    case 3:
                        imageView.setImageResource(R.drawable.ic_maleredhair);
                        break;
                    case 4:
                        imageView.setImageResource(R.drawable.ic_malelight);
                        break;
                    case 5:
                        imageView.setImageResource(R.drawable.ic_maledarker);
                        break;

                }


                return rowView;

            }
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
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Chat Room";
                case 1:
                    return "Members";
                case 2:
                    return "Manage Chat Room";
            }
            return null;
        }
    }
}
