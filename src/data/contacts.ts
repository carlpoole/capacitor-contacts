import { Plugins } from '@capacitor/core';

const { Contacts } = Plugins;

export interface Contact {
  id: string;
  firstName: string;
  lastName: string;
  phoneNumbers: string[];
  emailAddresses: string[];
}

export const getContacts = async (): Promise<Contact[]> => {
  try {
    const result = await Contacts.getAll();

    /*
      Search examples

      const result = await Contacts.find({
        property: 'name',
        value: 'John'
      });

      const result = await Contacts.find({
        property: 'phone',
        value: '8885555512'
      });

      const result = await Contacts.find({
        property: 'email',
        value: 'jdoe@'
      });

    */

    return result.contacts;
  } catch (e) {
    console.error(`ERR (${getContacts.name}):`, e);
  }

  return [];
};
