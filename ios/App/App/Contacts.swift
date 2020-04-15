import Foundation
import Capacitor
import Contacts

@objc(Contacts)
public class Contacts : CAPPlugin {

    @objc func getAll(_ call: CAPPluginCall) {
        
        // Fetch and respond with contacts from off the main UI thread
        DispatchQueue.global(qos: .userInitiated).async {
            call.success([
                "contacts": self.fetchContacts()
            ])
        }
    }
    
    @objc func find(_ call: CAPPluginCall) {
        // Get the search property and value from the plugin call
        let searchProperty = call.getString("property") ?? ""
        let searchValue = call.getString("value") ?? ""
        
        // Fetch and respond with contacts from off the main UI thread
        DispatchQueue.global(qos: .userInitiated).async {
            call.success([
                "contacts": self.fetchContacts2(property: searchProperty, value: searchValue)
            ])
        }
    }
    
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
    
    private func fetchContacts2(property: String, value: String) -> [Any] {
        let keys = [CNContactGivenNameKey, CNContactFamilyNameKey, CNContactPhoneNumbersKey, CNContactEmailAddressesKey] as [CNKeyDescriptor]
        let predicate = CNContact.predicateForContacts(matchingName: "Apple")
        
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
