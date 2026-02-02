package com.example.speeddial.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speeddial.data.Contact
import com.example.speeddial.data.ContactDatabase
import com.example.speeddial.data.ContactRepository
import com.example.speeddial.data.ContactTransferManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ContactRepository
    val allContacts: Flow<List<Contact>>
    private val _isTransferInProgress = MutableStateFlow(false)
    val isTransferInProgress: StateFlow<Boolean> = _isTransferInProgress.asStateFlow()

    private val _transferEvents = MutableSharedFlow<ContactTransferEvent>()
    val transferEvents: SharedFlow<ContactTransferEvent> = _transferEvents.asSharedFlow()

    init {
        val contactDao = ContactDatabase.getDatabase(application).contactDao()
        repository = ContactRepository(contactDao)
        allContacts = repository.allContacts
    }

    fun insertContact(contact: Contact) = viewModelScope.launch {
        repository.insertContact(contact)
    }

    fun updateContact(contact: Contact) = viewModelScope.launch {
        repository.updateContact(contact)
    }

    fun deleteContact(contact: Contact) = viewModelScope.launch {
        repository.deleteContact(contact)
    }

    suspend fun getContactById(id: Long): Contact? {
        return repository.getContactById(id)
    }

    fun exportContacts(destination: Uri) {
        viewModelScope.launch {
            if (_isTransferInProgress.value) return@launch
            _isTransferInProgress.value = true
            val appContext = getApplication<Application>()
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val contacts = repository.getAllContactsOnce()
                    appContext.contentResolver.openOutputStream(destination)?.use { stream ->
                        ContactTransferManager.exportContacts(appContext, contacts, stream)
                        contacts.size
                    } ?: throw IllegalStateException("Unable to open export destination")
                }
            }
            _isTransferInProgress.value = false
            result.onSuccess { count ->
                _transferEvents.emit(ContactTransferEvent.ExportCompleted(count))
            }.onFailure { error ->
                _transferEvents.emit(ContactTransferEvent.Error(error.message ?: "Export failed"))
            }
        }
    }

    fun importContacts(source: Uri) {
        viewModelScope.launch {
            if (_isTransferInProgress.value) return@launch
            _isTransferInProgress.value = true
            val appContext = getApplication<Application>()
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openInputStream(source)?.use { stream ->
                        val contacts = ContactTransferManager.importContacts(appContext, stream)
                        repository.insertContacts(contacts)
                        contacts.size
                    } ?: throw IllegalStateException("Unable to open import source")
                }
            }
            _isTransferInProgress.value = false
            result.onSuccess { count ->
                _transferEvents.emit(ContactTransferEvent.ImportCompleted(count))
            }.onFailure { error ->
                _transferEvents.emit(ContactTransferEvent.Error(error.message ?: "Import failed"))
            }
        }
    }
}

sealed interface ContactTransferEvent {
    data class ExportCompleted(val count: Int) : ContactTransferEvent
    data class ImportCompleted(val count: Int) : ContactTransferEvent
    data class Error(val message: String) : ContactTransferEvent
}
