# Capacitor Contacts Plugin 

### Approach

- **Part 1**: 

    Usage example `const result = await Contacts.getAll();`
    
    Android uses a device database to contain contacts. I created a helper class called ContactLoader to contain the methods and helpers involved with collecting the contact details from the DB. The getContacts() method in that class handles the database query and can be used to retrieve all records, or can be provided a list of long values representing record IDs to filter the database query with (this allows the same code to be used by the query function in part 2).
    
    ContactLoadingTask contains boilerplate code that is used by the main Contacts class to initiate the contact retrieval. This ensures the contacts are retrieved outside the main thread for the application, which is reserved for low-effort code and UI code to avoid a diminished user experience while the phone is busy loading contacts. This is especially important if someone has a large contact list.
    
    For iOS, the contacts are accessed using the fetchContacts function in Contacts.swift. This is also called off the main thread.

- **Part 2**: 

    Usage example
    
    ```
    const result = await Contacts.find({
      property: 'name',
      value: 'Bri'
    });
    ```
    
    The query function find() accepts two parameters “property” and “value” which allow the plugin user to choose which contact property to filter the contact results with. See example above. The native code validates the property against a list of supported properties. The currently supported search options are full name, phone number and email.
    
    Android uses ContactFilterTask to search the contact database from off the main application thread. ContactFilterTask builds a database query using the provided details into a LIKE query. This searches the database fields in a “starts with” behavior. E.G: if the plugin user provides “Bri” like in the example above, it will return Brian, Britney, etc. This initial query builds a result list containing the long integer record IDs for the contacts in the device database and then passes this along to the core database lookup method in Part 1 to resolve the full details for each contact to be returned from the plugin.
    
    For iOS, the contacts are accessed using the filterContacts function in Contacts.swift. This is also called off the main thread. Since there is no direct database access happening here like in Android, a predicate is built based on the input property with the search value and applied to the retrieval of contacts using CNContactStore.

- **Part 3**: 

    Designing and building this plugin with the intention of being open source and extensible involves making it flexible in capability, but also simple and easy to use if the users does not have a complex use case. These ideas can seem at odds, but I think it’s possible to provide an experience that would suit many users. 
    
    The most important thing to get right I think is to cover as much native behavior as possible without bloating the plugin and making it too complicated or making it not capable enough. It should focus on the sole purpose of interacting with data from the contact list and that’s all. If there’s demand to add more features slightly outside the scope of the plugin, it’s possible to make other plugins extending the capabilities of this one or fork it. 


### Improvement Ideas

-	Enhance the getAll() method to allow the user to optionally provide exactly which properties they want returned for each contact (E.G: name and email only). This would this remove unnecessary contact properties from the results and speed up the database queries.

-	Allow the plugin user to provide a settings object to format of the contact database results, such as a property to sort results on and whether its ascending or descending. It would be also be helpful to allow paging (a “page” and “limit” property) to allow the user to page through the contact database results incrementally to support long contact lists without severely impacting device memory.

-	The plugin allows search and filter by one property and value at a time but for full flexibility it could allow totally custom searches by any number of them at once. The user could provide an array of objects containing a property and value to search by and whether to AND or OR them all together, and the native plugin would handle it. If iOS is capable of searching contacts using DB strings like Android, this could allow the user to provide custom DB query strings up front too.

-	Allow the plugin user to subscribe to events/plugin result fired when the user refused permission for contacts so that the app can behave appropriately.

-	Add additional contact list interactivity to make the plugin more CRUD-like such that the user can also add, update, or delete contacts from the device store.

-	I think it’s possible to add a bit of front-end code in the Typescript part of the plugin to provide some enums up front that the plugin user could use instead of typing out “name” or “email” as a search property for the contact filter function. This would help reveal the options in an IDE and also help deter typos.

-	Provide a helpful readme and examples page to explain how to use the plugin easily and off explanations on how to use in more detail for more complex use cases like paging and customizing the properties returned for each contact.

-	Publish it to NPM so its accessible by everyone easily!
