package io.ionic.starter;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.lang.ref.WeakReference;

@NativePlugin(
        requestCodes = {Contacts.GET_ALL_REQUEST}
)
public class Contacts extends Plugin {

    /**
     * ID for the contact permission request.
     */
    static final int GET_ALL_REQUEST = 30033;

    /**
     * Default projection for contact record information.
     */
    private static final String[] CONTACT_PROJECTION = new String[]{
            ContactsContract.Contacts._ID

    };

    /**
     * Default projection for contact name information.
     */
    private static final String[] NAME_PROJECTION = new String[]{
            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME
    };

    /**
     * Default projection for contact phone information.
     */
    private static final String[] PHONE_PROJECTION = new String[]{
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
    };

    /**
     * Default projection for contact email information.
     */
    private static final String[] EMAIL_PROJECTION = new String[]{
            ContactsContract.CommonDataKinds.Email.ADDRESS
    };

    @PluginMethod()
    public void getAll(PluginCall call) {
        if (!checkPermission(call))
            return;

        ContactLoadingTask contactLoadingTask = new ContactLoadingTask(this, call);
        contactLoadingTask.execute();
    }

    @PluginMethod()
    public void find(PluginCall call) {
        if (!checkPermission(call))
            return;

        // Todo
//        ContactLoadingTask contactLoadingTask = new ContactLoadingTask(this, call);
//        contactLoadingTask.execute();
    }

    private boolean checkPermission(PluginCall call) {
        if (!hasPermission(Manifest.permission.READ_CONTACTS) || !hasPermission(Manifest.permission.WRITE_CONTACTS)) {
            saveCall(call);
            pluginRequestPermissions(new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS}, GET_ALL_REQUEST);
            return false;
        }

        return true;
    }

    @Override
    protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);

        PluginCall savedCall = getSavedCall();
        if (savedCall == null) {
            return;
        }

        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                savedCall.error("User denied permission");
                return;
            }
        }

        if (requestCode == GET_ALL_REQUEST) {
            this.getAll(savedCall);
        }
    }

    /**
     * An asynctask to load contact information from the device off the main thread.
     */
    private static class ContactLoadingTask extends AsyncTask<Void, Void, JSObject> {

        /**
         * Weak reference to the plugin to get access to the content resolver for loading contacts.
         */
        private WeakReference<Contacts> plugin;

        /**
         * The plugin call object to respond to.
         */
        private PluginCall pluginCall;

        /**
         * Constructs a new contact loading task to execute off the main thread.
         *
         * @param plugin     The plugin class to access the current activity and content resolver.
         * @param pluginCall The plugin call object to report back to.
         */
        public ContactLoadingTask(Contacts plugin, PluginCall pluginCall) {
            this.plugin = new WeakReference<>(plugin);
            this.pluginCall = pluginCall;
        }

        /**
         * The contact loading operation that occurs off the main thread.
         *
         * @param voids Null input. Not needed.
         * @return Returns a list of contacts found on the device.
         */
        @Override
        protected JSObject doInBackground(Void... voids) {
            JSObject result = new JSObject();
            JSArray contacts = new JSArray();

            ContentResolver contentResolver = plugin.get().getActivity().getContentResolver();

            // Get all the phone number details for the contact
            try (Cursor contactCursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, CONTACT_PROJECTION, null, null, null)) {

                if (contactCursor != null) {
                    while (contactCursor.moveToNext()) {
                        String id = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.Contacts._ID));

                        String firstName = "";
                        String lastName = "";

                        // Get name details
                        String whereName = ContactsContract.Data.MIMETYPE + " = ? AND " + ContactsContract.Data.RAW_CONTACT_ID + " = ?";
                        String[] whereNameParams = new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, id};

                        try (Cursor nameCursor = contentResolver.query(ContactsContract.Data.CONTENT_URI,
                                NAME_PROJECTION, whereName, whereNameParams,
                                ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME)) {

                            if (nameCursor != null) {
                                while (nameCursor.moveToNext()) {
                                    firstName = nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                                    lastName = nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
                                }
                            }
                        }

                        // Get phone details
                        JSArray numbersArray = new JSArray();

                        try (Cursor phoneCursor = contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PHONE_PROJECTION,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{id}, null)) {

                            if (phoneCursor != null) {
                                while (phoneCursor.moveToNext()) {
                                    String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));
                                    numbersArray.put(phoneNumber);
                                }
                            }
                        }

                        // Get email details
                        JSArray emailsArray = new JSArray();

                        try (Cursor emailCursor = contentResolver.query(
                                ContactsContract.CommonDataKinds.Email.CONTENT_URI, EMAIL_PROJECTION,
                                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                                new String[]{id}, null)) {

                            if (emailCursor != null) {
                                while (emailCursor.moveToNext()) {
                                    String email = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                                    emailsArray.put(email);
                                }
                            }
                        }

                        // Construct the JSON Object
                        JSObject contactObj = new JSObject();
                        contactObj.put("firstName", firstName);
                        contactObj.put("lastName", lastName);
                        contactObj.put("phoneNumbers", numbersArray);
                        contactObj.put("emailAddresses", emailsArray);
                        contacts.put(contactObj);
                    }
                }
            }

            result.put("contacts", contacts);
            return result;
        }

        /**
         * Returns the results of the contact loading back to the main thread and to Capacitor.
         *
         * @param result The list of contacts retrieved from the device as a JSON Object.
         */
        @Override
        protected void onPostExecute(JSObject result) {
            pluginCall.success(result);
        }
    }
}
