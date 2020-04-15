package io.ionic.starter;

import android.os.AsyncTask;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import java.lang.ref.WeakReference;

public class ContactLoadingTask extends AsyncTask<Void, Void, JSObject> {

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
        return ContactLoader.getContacts(plugin, null);
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
