import Foundation
import Capacitor
import Contacts

@objc(Contacts)
public class Contacts : CAPPlugin {
    
    @objc func getAll(_ call: CAPPluginCall) {
        let contacts = fetchContacts()
        
        
        call.success([
            "contacts": contacts
        ])
    }
    
    @objc func find(_ call: CAPPluginCall) {
        // Get the search property and value from the plugin call
        let searchProperty = call.getString("property") ?? ""
        let searchValue = call.getString("value") ?? ""
        
        let contacts = getAllMocked() // TODO: Replace mocked data with real implementation.
        
        call.success([
            "contacts": contacts
        ])
    }
    
    private func fetchContacts() -> [Any] {
        let keys = [CNContactGivenNameKey, CNContactFamilyNameKey, CNContactPhoneNumbersKey, CNContactEmailAddressesKey]
        let request = CNContactFetchRequest(keysToFetch: keys as [CNKeyDescriptor])
        
        var contacts = [Any]()
        
        do {
            try CNContactStore().enumerateContacts(with: request, usingBlock: { (contact, stopPointer) in
                
                var phones = [Any]()
                
                for phoneNumber in contact.phoneNumbers {
                    phones.append(phoneNumber.value.stringValue)
                }
                
                var emails = [Any]()
                
                for emailAddress in contact.emailAddresses {
                    emails.append(emailAddress.value)
                }
                
                contacts.append([
                    "firstName": contact.givenName,
                    "lastName": contact.familyName,
                    "phoneNumbers": phones,
                    "emailAddresses": emails
                ])
            })
            
        } catch {
            print("unable to fetch contacts")
        }
        
        return contacts
    }
}
