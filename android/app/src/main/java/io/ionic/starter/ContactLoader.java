package io.ionic.starter;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * A helper class that contains the logic for querying the contact database. It can handle an
 * unfiltered query or return results from provided IDs.
 */
class ContactLoader {

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

    /**
     * Formats an list of long integers into query format to be used with the IN operator in SQL.
     * <p>
     * Example:
     * <p>
     * Input list: 1,2,3,4,5
     * Output string: "(1,2,3,4,5)"
     *
     * @param input A list of long identifiers to be formatted.
     * @return A formatted string for the contact query.
     */
    private static String formatStringArray(List<Long> input) {
        if (input.size() > 0) {
            StringBuilder nameBuilder = new StringBuilder();

            nameBuilder.append("(");

            for (Long i : input) {
                nameBuilder.append(i).append(",");
            }

            nameBuilder.deleteCharAt(nameBuilder.length() - 1);

            nameBuilder.append(")");

            return nameBuilder.toString();
        } else {
            return "";
        }
    }

    /**
     * Queries the device contact list and returns a list of contacts. Can be filtered if provided
     * a list of long integers representing IDs for the contacts on the device DB.
     * <p>
     * WARNING: running this method on the main thread may block the UI and diminish the user
     * experience. It is recommended to access this method from off the main thread.
     *
     * @param plugin    A reference to the plugin class to access the content resolver.
     * @param filterIDs A list of longs representing contact IDs to filter the search by.
     * @return A list of result contacts.
     */
    static JSObject getContacts(WeakReference<Contacts> plugin, List<Long> filterIDs) {
        JSObject result = new JSObject();
        JSArray contacts = new JSArray();

        // Only query if this is an unfiltered search or has results from filter
        if (filterIDs == null || !filterIDs.isEmpty()) {
            ContentResolver contentResolver = plugin.get().getActivity().getContentResolver();

            final String SELECTION = filterIDs == null ? null
                    : String.format("%s IN %s", ContactsContract.Contacts._ID, formatStringArray(filterIDs));

            // Get all the phone number details for the contact
            try (Cursor contactCursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, CONTACT_PROJECTION, SELECTION, null, null)) {

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
        }

        result.put("contacts", contacts);
        return result;
    }
}
