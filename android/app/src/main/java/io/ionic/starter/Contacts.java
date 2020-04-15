package io.ionic.starter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.provider.ContactsContract;

import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.util.HashMap;
import java.util.Map;

import static io.ionic.starter.ContactFilterTask.EMAIL;
import static io.ionic.starter.ContactFilterTask.FIRST_NAME;
import static io.ionic.starter.ContactFilterTask.LAST_NAME;
import static io.ionic.starter.ContactFilterTask.NAME;
import static io.ionic.starter.ContactFilterTask.PHONE;

@NativePlugin(
        requestCodes = {Contacts.GET_ALL_REQUEST}
)
public class Contacts extends Plugin {

    /**
     * ID for the contact permission request.
     */
    static final int GET_ALL_REQUEST = 30033;

    /**
     * Maps the plugin contact record identifiers from filters/searches to contact field names.
     */
    private static final Map<String, String> contactDetailsMap = new HashMap<String, String>() {{
        put(FIRST_NAME, ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME);
        put(LAST_NAME, ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME);
        put(NAME, ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME);
        put(PHONE, ContactsContract.CommonDataKinds.Phone.NUMBER);
        put(EMAIL, ContactsContract.CommonDataKinds.Email.ADDRESS);
    }};

    /**
     * Retrieves all contacts on the device.
     *
     * @param call The plugin call to report back to.
     */
    @PluginMethod()
    public void getAll(PluginCall call) {
        if (!checkPermission(call))
            return;

        // Run contact loading task off main thread so UI is not blocked
        ContactLoadingTask contactLoadingTask = new ContactLoadingTask(this, call);
        contactLoadingTask.execute();
    }

    /**
     * Finds specific contacts on the device.
     *
     * @param call The plugin call to report back to.
     */
    @PluginMethod()
    public void find(PluginCall call) {
        if (!checkPermission(call))
            return;

        // Get the search property and value from the plugin call
        String searchProperty = call.getString("property");
        String searchValue = call.getString("value");

        // Make sure the property is a valid option currently supported by the plugin.
        if (contactDetailsMap.get(searchProperty) == null) {
            call.error(String.format("Unrecognized contact search property: %s", searchProperty));
            return;
        }

        // Shortcut empty result if no search value passed.
        if (searchValue == null || searchValue.isEmpty()) {
            call.success();
            return;
        }

        // Run contact filtering task off main thread so UI is not blocked
        ContactFilterTask contactFilterTask = new ContactFilterTask(this, call, searchProperty, searchValue);
        contactFilterTask.execute();
    }

    /**
     * Handles the permission checking and requesting from the user.
     *
     * @param call The plugin call object ot report back to.
     * @return True if permitted, false if not.
     */
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
}
