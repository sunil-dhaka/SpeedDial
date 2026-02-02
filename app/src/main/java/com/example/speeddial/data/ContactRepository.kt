package com.example.speeddial.data

import kotlinx.coroutines.flow.Flow

class ContactRepository(private val contactDao: ContactDao) {
    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()

    suspend fun insertContact(contact: Contact) {
        contactDao.insertContact(contact)
    }

    suspend fun insertContacts(contacts: List<Contact>) {
        if (contacts.isNotEmpty()) {
            contactDao.insertContacts(contacts)
        }
    }

    suspend fun updateContact(contact: Contact) {
        contactDao.updateContact(contact)
    }

    suspend fun deleteContact(contact: Contact) {
        contactDao.deleteContact(contact)
    }

    suspend fun getContactById(id: Long): Contact? {
        return contactDao.getContactById(id)
    }

    suspend fun getAllContactsOnce(): List<Contact> {
        return contactDao.getAllContactsOnce()
    }
}
