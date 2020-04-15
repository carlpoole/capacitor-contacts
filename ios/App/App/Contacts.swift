import Foundation
import Capacitor
import Contacts

@objc(Contacts)
public class Contacts : CAPPlugin {
    
    /**
    Contact properties that can be searched.
     */
    let contactDetailsMap : Set<String> = ["name", "firstName", "lastName", "phone", "email" ]

    /**
     Retrieves all contacts on the device.
     
     - Parameters:
        - call: The plugin call to report back to
     */
    @objc func getAll(_ call: CAPPluginCall) {
        
        // Fetch and respond with contacts from off the main UI thread
        DispatchQueue.global(qos: .userInitiated).async {
            call.success([
                "contacts": self.fetchContacts()
            ])
        }
    }
    
    /**
     Finds specific contacts on the device.
     
     - Parameters:
        - call: The plugin call to report back to
     */
    @objc func find(_ call: CAPPluginCall) {
        // Get the search property and value from the plugin call
        let searchProperty = call.getString("property") ?? ""
        let searchValue = call.getString("value") ?? ""
        
        // Make sure the property is a valid option currently supported by the plugin.
        if (!contactDetailsMap.contains(searchProperty)) {
            call.error(String(format: "Unrecognized contact search property: %s", searchProperty))
            return
        }
        
        // Shortcut empty result if no search value passed.
        if (searchValue.isEmpty) {
            call.success()
            return
        }
        
        // Fetch and respond with contacts from off the main UI thread
        DispatchQueue.global(qos: .userInitiated).async {
            call.success([
                "contacts": self.filterContacts(property: searchProperty, value: searchValue)
            ])
        }
    }
    
    /**
     Fetches the entire list of contacts on the device.
     
     - Returns: The list of contacts on the device
     */
    private func fetchContacts() -> [Any] {
        let keys = [CNContactGivenNameKey, CNContactFamilyNameKey, CNContactPhoneNumbersKey, CNContactEmailAddressesKey]
        let request = CNContactFetchRequest(keysToFetch: keys as [CNKeyDescriptor])
        
        var contacts = [Any]()
        
        do {
            try CNContactStore().enumerateContacts(with: request, usingBlock: { (contact, stopPointer) in
                contacts.append(self.buildContactResponse(contact: contact))
            })
            
        } catch {
            print("unable to fetch contacts")
        }
        
        return contacts
    }
    
    /**
     Fetches contacts provided a search property and value and returns filtered results.
     
     - Parameters:
        - property: The contact property to search on, eg: name, or email
        - value: The value to search contacts with
     
     - Returns: A list of contacts filtered by the provided search parameters
     */
    private func filterContacts(property: String, value: String) -> [Any] {
        let keys = [CNContactGivenNameKey, CNContactFamilyNameKey, CNContactPhoneNumbersKey, CNContactEmailAddressesKey] as [CNKeyDescriptor]
        
        let predicate = getPredicateFor(property: property, value: value)
        
        var results = [Any]()
        
        do {
            let contacts = try CNContactStore().unifiedContacts(matching: predicate, keysToFetch: keys)
            
            for contact in contacts {
                results.append(self.buildContactResponse(contact: contact))
            }
        } catch {
            print("unable to fetch contacts")
        }
        
        return results
    }
    
    /**
     Helper function to create a predicate given a property and value to search on.
     
     - Parameters:
         - property: The contact property to search on, eg: name, or email
         - value: The value to search contacts with
     
     - Returns: A predicate to use to filter the contact list with
     */
    private func getPredicateFor(property: String, value: String) -> NSPredicate {
        switch property {
        case "email":
            return CNContact.predicateForContacts(matchingEmailAddress: value)
        case "phone":
            let phone = CNPhoneNumber(stringValue: value)
            return CNContact.predicateForContacts(matching: phone)
        default:
            return CNContact.predicateForContacts(matchingName: value)
        }
    }
    
    /**
     Helper function for constructing a response object from the plugin when provided an iOS Contact object
     
     - Parameters:
        - contact: The contact to format
     
     - Returns: A contact to return from the plugin
     */
    private func buildContactResponse(contact: CNContact) -> Any {
        var phones = [Any]()
        
        for phoneNumber in contact.phoneNumbers {
            phones.append(phoneNumber.value.stringValue)
        }
        
        var emails = [Any]()
        
        for emailAddress in contact.emailAddresses {
            emails.append(emailAddress.value)
        }
        
        return [
            "firstName": contact.givenName,
            "lastName": contact.familyName,
            "phoneNumbers": phones,
            "emailAddresses": emails
        ]
    }
}
