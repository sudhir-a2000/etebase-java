/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package com.etesync.syncadapter.journalmanager;

import com.etesync.syncadapter.App;
import com.etesync.syncadapter.HttpClient;
import com.etesync.syncadapter.model.CollectionInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ServiceTest {
    private OkHttpClient httpClient;
    private HttpUrl remote;
    private String authToken;

    @Before
    public void setUp() throws Exception {
        httpClient = HttpClient.create(null);
        remote = HttpUrl.parse("http://localhost:8000"); // FIXME: hardcode for now, should make configureable
        JournalAuthenticator journalAuthenticator = new JournalAuthenticator(httpClient, remote);
        authToken = journalAuthenticator.getAuthToken(Helpers.USER, Helpers.PASSWORD);

        httpClient = HttpClient.create(null, App.log, null, authToken);

        /* Reset */
        Request request = new Request.Builder()
                .post(new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return null;
                    }

                    @Override
                    public void writeTo(BufferedSink sink) throws IOException {

                    }
                })
                .url(remote.newBuilder().addEncodedPathSegments("reset/").build())
                .build();
        Response response = httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new Exception("Failed resetting");
        }
    }

    @After
    public void tearDown() throws IOException {
    }

    @Test
    public void testSyncSimple() throws IOException, Exceptions.HttpException, Exceptions.GenericCryptoException, Exceptions.IntegrityException {
        Exception caught;
        JournalManager journalManager = new JournalManager(httpClient, remote);
        CollectionInfo info = CollectionInfo.defaultForServiceType(CollectionInfo.Type.ADDRESS_BOOK);
        info.uid = JournalManager.Journal.genUid();
        info.displayName = "Test";
        Crypto.CryptoManager crypto = new Crypto.CryptoManager(info.version, Helpers.keyBase64, info.uid);
        JournalManager.Journal journal = new JournalManager.Journal(crypto, info.toJson(), info.uid);
        journalManager.putJournal(journal);

        // Try pushing the same journal (uid clash)
        try {
            caught = null;
            journalManager.putJournal(journal);
        } catch (Exceptions.HttpException e) {
            caught = e;
        }
        assertNotNull(caught);

        List<JournalManager.Journal> journals = journalManager.getJournals(Helpers.keyBase64);
        assertEquals(journals.size(), 1);
        CollectionInfo info2 = CollectionInfo.fromJson(journals.get(0).getContent(crypto));
        assertEquals(info2.displayName, info.displayName);

        // Update journal
        info.displayName = "Test 2";
        journal = new JournalManager.Journal(crypto, info.toJson(), info.uid);
        journalManager.updateJournal(journal);

        journals = journalManager.getJournals(Helpers.keyBase64);
        assertEquals(journals.size(), 1);
        info2 = CollectionInfo.fromJson(journals.get(0).getContent(crypto));
        assertEquals(info2.displayName, info.displayName);

        // Delete journal
        journalManager.deleteJournal(journal);

        journals = journalManager.getJournals(Helpers.keyBase64);
        assertEquals(journals.size(), 0);

        // Bad HMAC
        info.uid = JournalManager.Journal.genUid();
        journal = new JournalManager.Journal(crypto, info.toJson(), info.uid);
        info.displayName = "Test 3";
        //// We assume this doesn't update the hmac.
        journal.setContent(crypto, info.toJson());
        journalManager.putJournal(journal);

        try {
            caught = null;
            journalManager.getJournals(Helpers.keyBase64);
        } catch (Exceptions.IntegrityException e) {
            caught = e;
        }
        assertNotNull(caught);
    }


    @Test
    public void testSyncEntry() throws IOException, Exceptions.HttpException, Exceptions.GenericCryptoException, Exceptions.IntegrityException {
        Exception caught;
        JournalManager journalManager = new JournalManager(httpClient, remote);
        CollectionInfo info = CollectionInfo.defaultForServiceType(CollectionInfo.Type.ADDRESS_BOOK);
        info.uid = JournalManager.Journal.genUid();
        info.displayName = "Test";
        Crypto.CryptoManager crypto = new Crypto.CryptoManager(info.version, Helpers.keyBase64, info.uid);
        JournalManager.Journal journal = new JournalManager.Journal(crypto, info.toJson(), info.uid);
        journalManager.putJournal(journal);

        JournalEntryManager journalEntryManager = new JournalEntryManager(httpClient, remote, info.uid);
        JournalEntryManager.Entry previousEntry = null;
        JournalEntryManager.Entry entry = new JournalEntryManager.Entry();
        entry.update(crypto, "Content", previousEntry);

        List<JournalEntryManager.Entry> entries = new LinkedList<>();

        entries.add(entry);
        journalEntryManager.putEntries(entries, null);
        previousEntry = entry;

        entries.clear();
        JournalEntryManager.Entry entry2 = new JournalEntryManager.Entry();
        entry2.update(crypto, "Content", previousEntry);
        entries.add(entry2);

        // Pushing a correct entries without the last parameter
        try {
            caught = null;
            journalEntryManager.putEntries(entries, null);
        } catch (Exceptions.HttpException e) {
            caught = e;
        }
        assertNotNull(caught);

        // Adding a second entry
        journalEntryManager.putEntries(entries, previousEntry.getUid());
        previousEntry = entry2;

        entries.clear();
        entries.add(entry);
        entries.add(entry2);

        // Check last works:
        entries = journalEntryManager.getEntries(crypto, entry.getUid());
        assertEquals(entries.size(), 1);
        entries = journalEntryManager.getEntries(crypto, entry2.getUid());
        assertEquals(entries.size(), 0);

        // Corrupt the journal and verify we catch it
        entries.clear();
        entry2 = new JournalEntryManager.Entry();
        entry2.update(crypto, "Content", null);
        entries.add(entry2);

        journalEntryManager.putEntries(entries, previousEntry.getUid());

        try {
            caught = null;
            journalEntryManager.getEntries(crypto, null);
        } catch (Exceptions.IntegrityException e) {
            caught = e;
        }
        assertNotNull(caught);
    }
}
