package io.ionic.starter;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * An async task to load contact information from the device off the main thread.
 */
public class ContactFilterTask extends AsyncTask<Void, Void, JSObject> {

    // Constants representing the valid search/filter options.
    final static String NAME = "name";
    final static String FIRST_NAME = "firstName";
    final static String LAST_NAME = "lastName";
    final static String PHONE = "phone";
    final static String EMAIL = "email";

    /**
     * Default projection for contact name information.
     */
    private static final String[] NAME_PROJECTION = new String[]{
            ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID
    };

    /**
     * Default projection for contact phone information.
     */
    private static final String[] PHONE_PROJECTION = new String[]{
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID
    };

    /**
     * Default projection for contact email information.
     */
    private static final String[] EMAIL_PROJECTION = new String[]{
            ContactsContract.CommonDataKinds.Email.CONTACT_ID
    };

    /**
     * Weak reference to the plugin to get access to the content resolver for loading contacts.
     */
    private WeakReference<Contacts> plugin;

    /**
     * The plugin call object to respond to.
     */
    private PluginCall pluginCall;

    /**
     * The search property for the contact search task.
     */
    private String searchProperty = null;

    /**
     * The search value for the contact search task.
     */
    private String searchValue = null;

    /**
     * Constructs a new contact filter task to execute off the main thread.
     *
     * @param plugin         The plugin class to access the current activity and content resolver.
     * @param pluginCall     The plugin call object to report back to.
     * @param searchProperty The contact property to search over.
     * @param searchValue    The value to search contacts with.
     */
    public ContactFilterTask(Contacts plugin, PluginCall pluginCall, String searchProperty, String searchValue) {
        this.plugin = new WeakReference<>(plugin);
        this.pluginCall = pluginCall;
        this.searchProperty = searchProperty;
        this.searchValue = searchValue;
    }

    @Override
    protected JSObject doInBackground(Void... voids) {
        List<Long> searchResults = new ArrayList<>();

        ContentResolver contentResolver = plugin.get().getActivity().getContentResolver();

        String[] whereParams = new String[]{searchValue + "%"};
        String where;
        String contactIDField;
        String[] projection;

        switch (searchProperty) {
            case NAME:
                where = ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME + " LIKE ?";
                contactIDField = ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID;
                projection = NAME_PROJECTION;
                break;
            case FIRST_NAME:
                where = ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME + " LIKE ?";
                contactIDField = ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID;
                projection = NAME_PROJECTION;
                break;
            case LAST_NAME:
                where = ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME + " LIKE ?";
                contactIDField = ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID;
                projection = NAME_PROJECTION;
                break;
            case PHONE:
                where = ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ?";
                contactIDField = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
                projection = PHONE_PROJECTION;
                break;
            case EMAIL:
                where = ContactsContract.CommonDataKinds.Email.ADDRESS + " LIKE ?";
                contactIDField = ContactsContract.CommonDataKinds.Email.CONTACT_ID;
                projection = EMAIL_PROJECTION;
                break;
            default:
                return null;
        }

        try (Cursor nameCursor = contentResolver.query(ContactsContract.Data.CONTENT_URI,
                projection, where, whereParams, null)) {

            if (nameCursor != null) {
                while (nameCursor.moveToNext()) {
                    searchResults.add(Long.parseLong(nameCursor.getString(nameCursor.getColumnIndex(contactIDField))));
                }
            }
        }

        // Build a contact list based on the search results and return
        return ContactLoader.getContacts(plugin, searchResults);
    }

    @Override
    protected void onPostExecute(JSObject result) {
        pluginCall.success(result);
    }
}
