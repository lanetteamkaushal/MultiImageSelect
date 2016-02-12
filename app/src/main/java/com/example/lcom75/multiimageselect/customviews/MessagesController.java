package com.example.lcom75.multiimageselect.customviews;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.example.lcom75.multiimageselect.AndroidUtilities;
import com.example.lcom75.multiimageselect.NotificationCenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lcom75 on 12/2/16.
 */
public class MessagesController {
    public void processUpdates(final TLRPC.Updates updates, boolean fromQueue) {

        ArrayList<Integer> needGetChannelsDiff = null;
        boolean needGetDiff = false;
        boolean needReceivedQueue = false;
        boolean updateStatus = false;
        if (updates instanceof TLRPC.TL_updateShort) {
            ArrayList<TLRPC.Update> arr = new ArrayList<>();
            arr.add(updates.update);
            processUpdateArray(arr, null, null);
        } else if (updates instanceof TLRPC.TL_updateShortChatMessage || updates instanceof TLRPC.TL_updateShortMessage) {
            final int user_id = updates instanceof TLRPC.TL_updateShortChatMessage ? updates.from_id : updates.user_id;
            TLRPC.User user = getUser(user_id);
            TLRPC.User user2 = null;
            TLRPC.User user3 = null;
            TLRPC.Chat channel = null;

            if (user == null) {
                user = MessagesStorage.getInstance().getUserSync(user_id);
                putUser(user, true);
            }

            boolean needFwdUser = false;
            if (updates.fwd_from_id instanceof TLRPC.TL_peerUser) {
                user2 = getUser(updates.fwd_from_id.user_id);
                if (user2 == null) {
                    user2 = MessagesStorage.getInstance().getUserSync(updates.fwd_from_id.user_id);
                    putUser(user2, true);
                }
                needFwdUser = true;
            } else if (updates.fwd_from_id instanceof TLRPC.TL_peerChannel) {
                channel = getChat(updates.fwd_from_id.channel_id);
                if (channel == null) {
                    channel = MessagesStorage.getInstance().getChatSync(updates.fwd_from_id.channel_id);
                    putChat(channel, true);
                }
                needFwdUser = true;
            }

            boolean needBotUser = false;
            if (updates.via_bot_id != 0) {
                user3 = getUser(updates.via_bot_id);
                if (user3 == null) {
                    user3 = MessagesStorage.getInstance().getUserSync(updates.via_bot_id);
                    putUser(user3, true);
                }
                needBotUser = true;
            }

            boolean missingData;
            if (updates instanceof TLRPC.TL_updateShortMessage) {
                missingData = user == null || needFwdUser && user2 == null && channel == null || needBotUser && user3 == null;
            } else {
                TLRPC.Chat chat = getChat(updates.chat_id);
                if (chat == null) {
                    chat = MessagesStorage.getInstance().getChatSync(updates.chat_id);
                    putChat(chat, true);
                }
                missingData = chat == null || user == null || needFwdUser && user2 == null && channel == null || needBotUser && user3 == null;
            }
            if (user != null && user.status != null && user.status.expires <= 0) {
                onlinePrivacy.put(user.id, ConnectionsManager.getInstance().getCurrentTime());
                updateStatus = true;
            }

            if (missingData) {
                needGetDiff = true;
            } else {
                if (MessagesStorage.lastPtsValue + updates.pts_count == updates.pts) {
                    TLRPC.TL_message message = new TLRPC.TL_message();
                    message.id = updates.id;
                    if (updates instanceof TLRPC.TL_updateShortMessage) {
                        if (updates.out) {
                            message.from_id = UserConfig.getClientUserId();
                        } else {
                            message.from_id = user_id;
                        }
                        message.to_id = new TLRPC.TL_peerUser();
                        message.to_id.user_id = user_id;
                        message.dialog_id = user_id;
                    } else {
                        message.from_id = user_id;
                        message.to_id = new TLRPC.TL_peerChat();
                        message.to_id.chat_id = updates.chat_id;
                        message.dialog_id = -updates.chat_id;
                    }
                    message.out = updates.out;
                    message.unread = updates.unread;
                    message.mentioned = updates.mentioned;
                    message.media_unread = updates.media_unread;
                    message.entities = updates.entities;
                    message.message = updates.message;
                    message.date = updates.date;
                    message.via_bot_id = updates.via_bot_id;
                    message.flags = updates.flags | TLRPC.MESSAGE_FLAG_HAS_FROM_ID;
                    message.fwd_from_id = updates.fwd_from_id;
                    message.fwd_date = updates.fwd_date;
                    message.reply_to_msg_id = updates.reply_to_msg_id;
                    message.media = new TLRPC.TL_messageMediaEmpty();
                    MessagesStorage.lastPtsValue = updates.pts;
                    final MessageObject obj = new MessageObject(message, null, true);
                    final ArrayList<MessageObject> objArr = new ArrayList<>();
                    objArr.add(obj);
                    ArrayList<TLRPC.Message> arr = new ArrayList<>();
                    arr.add(message);
                    if (updates instanceof TLRPC.TL_updateShortMessage) {
                        final boolean printUpdate = !updates.out && updatePrintingUsersWithNewMessages(updates.user_id, objArr);
                        if (printUpdate) {
                            updatePrintingStrings();
                        }
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                if (printUpdate) {
                                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, UPDATE_MASK_USER_PRINT);
                                }
                                updateInterfaceWithMessages(user_id, objArr);
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
                            }
                        });
                    } else {
                        final boolean printUpdate = updatePrintingUsersWithNewMessages(-updates.chat_id, objArr);
                        if (printUpdate) {
                            updatePrintingStrings();
                        }
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                if (printUpdate) {
                                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, UPDATE_MASK_USER_PRINT);
                                }

                                updateInterfaceWithMessages(-updates.chat_id, objArr);
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
                            }
                        });
                    }

                    MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            AndroidUtilities.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!obj.isOut()) {
                                        NotificationsController.getInstance().processNewMessages(objArr, true);
                                    }
                                }
                            });
                        }
                    });
                    MessagesStorage.getInstance().putMessages(arr, false, true, false, 0);
                } else if (MessagesStorage.lastPtsValue != updates.pts) {
                    FileLog.e("tmessages", "need get diff short message, pts: " + MessagesStorage.lastPtsValue + " " + updates.pts + " count = " + updates.pts_count);
                    if (gettingDifference || updatesStartWaitTimePts == 0 || Math.abs(System.currentTimeMillis() - updatesStartWaitTimePts) <= 1500) {
                        if (updatesStartWaitTimePts == 0) {
                            updatesStartWaitTimePts = System.currentTimeMillis();
                        }
                        FileLog.e("tmessages", "add to queue");
                        updatesQueuePts.add(updates);
                    } else {
                        needGetDiff = true;
                    }
                }
            }
        } else if (updates instanceof TLRPC.TL_updatesCombined || updates instanceof TLRPC.TL_updates) {
            MessagesStorage.getInstance().putUsersAndChats(updates.users, updates.chats, true, true);
            Collections.sort(updates.updates, new Comparator<TLRPC.Update>() {
                @Override
                public int compare(TLRPC.Update lhs, TLRPC.Update rhs) {
                    int ltype = getUpdateType(lhs);
                    int rtype = getUpdateType(rhs);
                    if (ltype != rtype) {
                        return AndroidUtilities.compare(ltype, rtype);
                    } else if (ltype == 0) {
                        return AndroidUtilities.compare(lhs.pts, rhs.pts);
                    } else if (ltype == 1) {
                        return AndroidUtilities.compare(lhs.qts, rhs.qts);
                    }
                    return 0;
                }
            });
            for (int a = 0; a < updates.updates.size(); a++) {
                TLRPC.Update update = updates.updates.get(a);
                if (getUpdateType(update) == 0) {
                    TLRPC.TL_updates updatesNew = new TLRPC.TL_updates();
                    updatesNew.updates.add(update);
                    updatesNew.pts = update.pts;
                    updatesNew.pts_count = update.pts_count;
                    for (int b = a + 1; b < updates.updates.size(); b++) {
                        TLRPC.Update update2 = updates.updates.get(b);
                        if (getUpdateType(update2) == 0 && updatesNew.pts + update2.pts_count == update2.pts) {
                            updatesNew.updates.add(update2);
                            updatesNew.pts = update2.pts;
                            updatesNew.pts_count += update2.pts_count;
                            updates.updates.remove(b);
                            b--;
                        } else {
                            break;
                        }
                    }
                    if (MessagesStorage.lastPtsValue + updatesNew.pts_count == updatesNew.pts) {
                        if (!processUpdateArray(updatesNew.updates, updates.users, updates.chats)) {
                            FileLog.e("tmessages", "need get diff inner TL_updates, seq: " + MessagesStorage.lastSeqValue + " " + updates.seq);
                            needGetDiff = true;
                        } else {
                            MessagesStorage.lastPtsValue = updatesNew.pts;
                        }
                    } else if (MessagesStorage.lastPtsValue != updatesNew.pts) {
                        FileLog.e("tmessages", update + " need get diff, pts: " + MessagesStorage.lastPtsValue + " " + updatesNew.pts + " count = " + updatesNew.pts_count);
                        if (gettingDifference || updatesStartWaitTimePts == 0 || updatesStartWaitTimePts != 0 && Math.abs(System.currentTimeMillis() - updatesStartWaitTimePts) <= 1500) {
                            if (updatesStartWaitTimePts == 0) {
                                updatesStartWaitTimePts = System.currentTimeMillis();
                            }
                            FileLog.e("tmessages", "add to queue");
                            updatesQueuePts.add(updatesNew);
                        } else {
                            needGetDiff = true;
                        }
                    }
                } else if (getUpdateType(update) == 1) {
                    TLRPC.TL_updates updatesNew = new TLRPC.TL_updates();
                    updatesNew.updates.add(update);
                    updatesNew.pts = update.qts;
                    for (int b = a + 1; b < updates.updates.size(); b++) {
                        TLRPC.Update update2 = updates.updates.get(b);
                        if (getUpdateType(update2) == 1 && updatesNew.pts + 1 == update2.qts) {
                            updatesNew.updates.add(update2);
                            updatesNew.pts = update2.qts;
                            updates.updates.remove(b);
                            b--;
                        } else {
                            break;
                        }
                    }
                    if (MessagesStorage.lastQtsValue == 0 || MessagesStorage.lastQtsValue + updatesNew.updates.size() == updatesNew.pts) {
                        processUpdateArray(updatesNew.updates, updates.users, updates.chats);
                        MessagesStorage.lastQtsValue = updatesNew.pts;
                        needReceivedQueue = true;
                    } else if (MessagesStorage.lastPtsValue != updatesNew.pts) {
                        FileLog.e("tmessages", update + " need get diff, qts: " + MessagesStorage.lastQtsValue + " " + updatesNew.pts);
                        if (gettingDifference || updatesStartWaitTimeQts == 0 || updatesStartWaitTimeQts != 0 && Math.abs(System.currentTimeMillis() - updatesStartWaitTimeQts) <= 1500) {
                            if (updatesStartWaitTimeQts == 0) {
                                updatesStartWaitTimeQts = System.currentTimeMillis();
                            }
                            FileLog.e("tmessages", "add to queue");
                            updatesQueueQts.add(updatesNew);
                        } else {
                            needGetDiff = true;
                        }
                    }
                } else if (getUpdateType(update) == 2) {
                    int channelId;
                    if (update instanceof TLRPC.TL_updateNewChannelMessage) {
                        channelId = ((TLRPC.TL_updateNewChannelMessage) update).message.to_id.channel_id;
                    } else {
                        channelId = update.channel_id;
                    }
                    Integer channelPts = channelsPts.get(channelId);
                    if (channelPts == null) {
                        channelPts = MessagesStorage.getInstance().getChannelPtsSync(channelId);
                        if (channelPts == 0) {
                            channelPts = update.pts - update.pts_count;
                        }
                        channelsPts.put(channelId, channelPts);
                    }
                    TLRPC.TL_updates updatesNew = new TLRPC.TL_updates();
                    updatesNew.updates.add(update);
                    updatesNew.pts = update.pts;
                    updatesNew.pts_count = update.pts_count;
                    for (int b = a + 1; b < updates.updates.size(); b++) {
                        TLRPC.Update update2 = updates.updates.get(b);
                        if (getUpdateType(update2) == 2 && updatesNew.pts + update2.pts_count == update2.pts) {
                            updatesNew.updates.add(update2);
                            updatesNew.pts = update2.pts;
                            updatesNew.pts_count += update2.pts_count;
                            updates.updates.remove(b);
                            b--;
                        } else {
                            break;
                        }
                    }
                    if (channelPts + updatesNew.pts_count == updatesNew.pts) {
                        if (!processUpdateArray(updatesNew.updates, updates.users, updates.chats)) {
                            FileLog.e("tmessages", "need get channel diff inner TL_updates, channel_id = " + channelId);
                            if (needGetChannelsDiff == null) {
                                needGetChannelsDiff = new ArrayList<>();
                            } else if (!needGetChannelsDiff.contains(channelId)) {
                                needGetChannelsDiff.add(channelId);
                            }
                        } else {
                            channelsPts.put(channelId, updatesNew.pts);
                            MessagesStorage.getInstance().saveChannelPts(channelId, updatesNew.pts);
                        }
                    } else if (channelPts != updatesNew.pts) {
                        FileLog.e("tmessages", update + " need get channel diff, pts: " + channelPts + " " + updatesNew.pts + " count = " + updatesNew.pts_count + " channelId = " + channelId);
                        Long updatesStartWaitTime = updatesStartWaitTimeChannels.get(channelId);
                        Boolean gettingDifferenceChannel = gettingDifferenceChannels.get(channelId);
                        if (gettingDifferenceChannel == null) {
                            gettingDifferenceChannel = false;
                        }
                        if (gettingDifferenceChannel || updatesStartWaitTime == null || Math.abs(System.currentTimeMillis() - updatesStartWaitTime) <= 1500) {
                            if (updatesStartWaitTime == null) {
                                updatesStartWaitTimeChannels.put(channelId, System.currentTimeMillis());
                            }
                            FileLog.e("tmessages", "add to queue");
                            ArrayList<TLRPC.Updates> arrayList = updatesQueueChannels.get(channelId);
                            if (arrayList == null) {
                                arrayList = new ArrayList<>();
                                updatesQueueChannels.put(channelId, arrayList);
                            }
                            arrayList.add(updatesNew);
                        } else {
                            if (needGetChannelsDiff == null) {
                                needGetChannelsDiff = new ArrayList<>();
                            } else if (!needGetChannelsDiff.contains(channelId)) {
                                needGetChannelsDiff.add(channelId);
                            }
                        }
                    }
                } else {
                    break;
                }
                updates.updates.remove(a);
                a--;
            }

            boolean processUpdate;
            if (updates instanceof TLRPC.TL_updatesCombined) {
                processUpdate = MessagesStorage.lastSeqValue + 1 == updates.seq_start || MessagesStorage.lastSeqValue == updates.seq_start;
            } else {
                processUpdate = MessagesStorage.lastSeqValue + 1 == updates.seq || updates.seq == 0 || updates.seq == MessagesStorage.lastSeqValue;
            }
            if (processUpdate) {
                processUpdateArray(updates.updates, updates.users, updates.chats);
                if (updates.date != 0) {
                    MessagesStorage.lastDateValue = updates.date;
                }
                if (updates.seq != 0) {
                    MessagesStorage.lastSeqValue = updates.seq;
                }
            } else {
                if (updates instanceof TLRPC.TL_updatesCombined) {
                    FileLog.e("tmessages", "need get diff TL_updatesCombined, seq: " + MessagesStorage.lastSeqValue + " " + updates.seq_start);
                } else {
                    FileLog.e("tmessages", "need get diff TL_updates, seq: " + MessagesStorage.lastSeqValue + " " + updates.seq);
                }

                if (gettingDifference || updatesStartWaitTimeSeq == 0 || Math.abs(System.currentTimeMillis() - updatesStartWaitTimeSeq) <= 1500) {
                    if (updatesStartWaitTimeSeq == 0) {
                        updatesStartWaitTimeSeq = System.currentTimeMillis();
                    }
                    FileLog.e("tmessages", "add TL_updates/Combined to queue");
                    updatesQueueSeq.add(updates);
                } else {
                    needGetDiff = true;
                }
            }
        } else if (updates instanceof TLRPC.TL_updatesTooLong) {
            FileLog.e("tmessages", "need get diff TL_updatesTooLong");
            needGetDiff = true;
        } else if (updates instanceof UserActionUpdatesSeq) {
            MessagesStorage.lastSeqValue = updates.seq;
        } else if (updates instanceof UserActionUpdatesPts) {
            if (updates.chat_id != 0) {
                channelsPts.put(updates.chat_id, updates.pts);
                MessagesStorage.getInstance().saveChannelPts(updates.chat_id, updates.pts);
            } else {
                MessagesStorage.lastPtsValue = updates.pts;
            }
        }
        SecretChatHelper.getInstance().processPendingEncMessages();
        if (!fromQueue) {
            ArrayList<Integer> keys = new ArrayList<>(updatesQueueChannels.keySet());
            for (int a = 0; a < keys.size(); a++) {
                Integer key = keys.get(a);
                if (needGetChannelsDiff != null && needGetChannelsDiff.contains(key)) {
                    getChannelDifference(key);
                } else {
                    processChannelsUpdatesQueue(key, 0);
                }
            }
            if (needGetDiff) {
                getDifference();
            } else {
                for (int a = 0; a < 3; a++) {
                    processUpdatesQueue(a, 0);
                }
            }
        }
        if (needReceivedQueue) {
            TLRPC.TL_messages_receivedQueue req = new TLRPC.TL_messages_receivedQueue();
            req.max_qts = MessagesStorage.lastQtsValue;
            ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error) {

                }
            });
        }
        if (updateStatus) {
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, UPDATE_MASK_STATUS);
                }
            });
        }
        MessagesStorage.getInstance().saveDiffParams(MessagesStorage.lastSeqValue, MessagesStorage.lastPtsValue, MessagesStorage.lastDateValue, MessagesStorage.lastQtsValue);
    }

    public TLRPC.User getUser(Integer id) {
        return users.get(id);
    }

    public TLRPC.User getUser(String username) {
        if (username == null || username.length() == 0) {
            return null;
        }
        return usersByUsernames.get(username.toLowerCase());
    }

    public boolean putUser(TLRPC.User user, boolean fromCache) {
        if (user == null) {
            return false;
        }
        fromCache = fromCache && user.id / 1000 != 333 && user.id != 777000;
        TLRPC.User oldUser = users.get(user.id);
        if (oldUser != null && oldUser.username != null && oldUser.username.length() > 0) {
            usersByUsernames.remove(oldUser.username);
        }
        if (user.username != null && user.username.length() > 0) {
            usersByUsernames.put(user.username.toLowerCase(), user);
        }
        if (!fromCache) {
            users.put(user.id, user);
            if (user.id == UserConfig.getClientUserId()) {
                UserConfig.setCurrentUser(user);
                UserConfig.saveConfig(true);
            }
            if (oldUser != null && user.status != null && oldUser.status != null && user.status.expires != oldUser.status.expires) {
                return true;
            }
        } else if (oldUser == null) {
            users.put(user.id, user);
        }
        return false;
    }

    public void putUsers(ArrayList<TLRPC.User> users, boolean fromCache) {
        if (users == null || users.isEmpty()) {
            return;
        }
        boolean updateStatus = false;
        int count = users.size();
        for (int a = 0; a < count; a++) {
            TLRPC.User user = users.get(a);
            if (putUser(user, fromCache)) {
                updateStatus = true;
            }
        }
        if (updateStatus) {
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, UPDATE_MASK_STATUS);
                }
            });
        }
    }

    public void putChat(TLRPC.Chat chat, boolean fromCache) {
        if (chat == null) {
            return;
        }
        TLRPC.Chat oldChat = chats.get(chat.id);
        if (!fromCache) {
            if (oldChat != null && chat.version != oldChat.version) {
                loadedFullChats.remove((Integer) chat.id);
            }
            chats.put(chat.id, chat);
        } else if (oldChat == null) {
            chats.put(chat.id, chat);
        }
    }
    private ConcurrentHashMap<Integer, TLRPC.Chat> chats = new ConcurrentHashMap<>(100, 1.0f, 2);
    private ConcurrentHashMap<Integer, TLRPC.EncryptedChat> encryptedChats = new ConcurrentHashMap<>(10, 1.0f, 2);
    private ConcurrentHashMap<Integer, TLRPC.User> users = new ConcurrentHashMap<>(100, 1.0f, 2);
    private ConcurrentHashMap<String, TLRPC.User> usersByUsernames = new ConcurrentHashMap<>(100, 1.0f, 2);
    private ArrayList<Integer> loadedFullChats = new ArrayList<>();

    public void putChats(ArrayList<TLRPC.Chat> chats, boolean fromCache) {
        if (chats == null || chats.isEmpty()) {
            return;
        }
        int count = chats.size();
        for (int a = 0; a < count; a++) {
            TLRPC.Chat chat = chats.get(a);
            putChat(chat, fromCache);
        }
    }
    public TLRPC.Chat getChat(Integer id) {
        return chats.get(id);
    }


    public boolean processUpdateArray(ArrayList<TLRPC.Update> updates, final ArrayList<TLRPC.User> usersArr, final ArrayList<TLRPC.Chat> chatsArr) {
        if (updates.isEmpty()) {
            if (usersArr != null || chatsArr != null) {
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        putUsers(usersArr, false);
                        putChats(chatsArr, false);
                    }
                });
            }
            return true;
        }
        long currentTime = System.currentTimeMillis();

        final HashMap<Long, ArrayList<MessageObject>> messages = new HashMap<>();
        final HashMap<Long, TLRPC.WebPage> webPages = new HashMap<>();
        final ArrayList<MessageObject> pushMessages = new ArrayList<>();
        final ArrayList<TLRPC.Message> messagesArr = new ArrayList<>();
        final SparseArray<SparseIntArray> channelViews = new SparseArray<>();
        final SparseArray<ArrayList<TLRPC.TL_messageGroup>> channelsGroups = new SparseArray<>();
        final SparseArray<Long> markAsReadMessagesInbox = new SparseArray<>();
        final SparseIntArray markAsReadMessagesOutbox = new SparseIntArray();
        final ArrayList<Long> markAsReadMessages = new ArrayList<>();
        final HashMap<Integer, Integer> markAsReadEncrypted = new HashMap<>();
        final SparseArray<ArrayList<Integer>> deletedMessages = new SparseArray<>();
        boolean printChanged = false;
        final ArrayList<TLRPC.ChatParticipants> chatInfoToUpdate = new ArrayList<>();
        final ArrayList<TLRPC.Update> updatesOnMainThread = new ArrayList<>();
        final ArrayList<TLRPC.TL_updateEncryptedMessagesRead> tasks = new ArrayList<>();
        final ArrayList<Integer> contactsIds = new ArrayList<>();

        boolean checkForUsers = true;
        ConcurrentHashMap<Integer, TLRPC.User> usersDict;
        ConcurrentHashMap<Integer, TLRPC.Chat> chatsDict;
        if (usersArr != null) {
            usersDict = new ConcurrentHashMap<>();
            for (TLRPC.User user : usersArr) {
                usersDict.put(user.id, user);
            }
        } else {
            checkForUsers = false;
            usersDict = users;
        }
        if (chatsArr != null) {
            chatsDict = new ConcurrentHashMap<>();
            for (TLRPC.Chat chat : chatsArr) {
                chatsDict.put(chat.id, chat);
            }
        } else {
            checkForUsers = false;
            chatsDict = chats;
        }

        if (usersArr != null || chatsArr != null) {
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    putUsers(usersArr, false);
                    putChats(chatsArr, false);
                }
            });
        }

        int interfaceUpdateMask = 0;

        for (int c = 0; c < updates.size(); c++) {
            TLRPC.Update update = updates.get(c);
            if (update instanceof TLRPC.TL_updateNewMessage || update instanceof TLRPC.TL_updateNewChannelMessage) {
                TLRPC.Message message;
                if (update instanceof TLRPC.TL_updateNewMessage) {
                    message = ((TLRPC.TL_updateNewMessage) update).message;
                } else {
                    message = ((TLRPC.TL_updateNewChannelMessage) update).message;
                }
                if (checkForUsers) {
                    int chat_id = 0;
                    if (message.to_id.channel_id != 0) {
                        chat_id = message.to_id.channel_id;
                    } else if (message.to_id.chat_id != 0) {
                        chat_id = message.to_id.chat_id;
                    }
                    if (chat_id != 0) {
                        TLRPC.Chat chat = chatsDict.get(chat_id);
                        if (chat == null) {
                            chat = getChat(chat_id);
                        }
                        if (chat == null) {
                            return false;
                        }
                        if (chat.megagroup) {
                            message.flags |= TLRPC.MESSAGE_FLAG_MEGAGROUP;
                        }
                    }
                    if (message.from_id > 0) {
                        TLRPC.User user = getUser(message.from_id);
                        if (usersDict.get(message.from_id) == null && user == null) {
                            return false;
                        }
                        if (user != null && user.status != null && user.status.expires <= 0) {
                            onlinePrivacy.put(message.from_id, ConnectionsManager.getInstance().getCurrentTime());
                            interfaceUpdateMask |= UPDATE_MASK_STATUS;
                        }
                    }
                }
                if (message.action instanceof TLRPC.TL_messageActionChatDeleteUser) {
                    TLRPC.User user = usersDict.get(message.action.user_id);
                    if (user != null && user.bot) {
                        message.reply_markup = new TLRPC.TL_replyKeyboardHide();
                    } else if (message.action.user_id == UserConfig.getClientUserId()) {
                        continue;
                    }
                } else if (message.action instanceof TLRPC.TL_messageActionChatMigrateTo || message.action instanceof TLRPC.TL_messageActionChannelCreate) {
                    message.unread = false;
                    message.media_unread = false;
                }
                if (update instanceof TLRPC.TL_updateNewChannelMessage) {
                    if (message.to_id.channel_id != 0 && !message.out) {
                        message.unread = true;
                        if (message.from_id <= 0 || (message.flags & TLRPC.MESSAGE_FLAG_MEGAGROUP) != 0) {
                            message.media_unread = true;
                        }
                    }

                    long dialog_id = -update.channel_id;
                    Integer value = dialogs_read_inbox_max.get(dialog_id);
                    if (value == null) {
                        value = MessagesStorage.getInstance().getChannelReadInboxMax(update.channel_id);
                    }
                    if (value >= message.id) {
                        message.unread = false;
                        message.media_unread = false;
                    }
                }
                messagesArr.add(message);
                ImageLoader.saveMessageThumbs(message);
                MessageObject obj = new MessageObject(message, usersDict, chatsDict, true);
                if (obj.type == 11) {
                    interfaceUpdateMask |= UPDATE_MASK_CHAT_AVATAR;
                } else if (obj.type == 10) {
                    interfaceUpdateMask |= UPDATE_MASK_CHAT_NAME;
                }
                if (message.to_id.chat_id != 0) {
                    message.dialog_id = -message.to_id.chat_id;
                } else if (message.to_id.channel_id != 0) {
                    message.dialog_id = -message.to_id.channel_id;
                } else {
                    if (message.to_id.user_id == UserConfig.getClientUserId()) {
                        message.to_id.user_id = message.from_id;
                    }
                    message.dialog_id = message.to_id.user_id;
                }
                ArrayList<MessageObject> arr = messages.get(message.dialog_id);
                if (arr == null) {
                    arr = new ArrayList<>();
                    messages.put(message.dialog_id, arr);
                }
                arr.add(obj);
                if (!obj.isOut() && (obj.isUnread() && message.to_id.channel_id == 0 || obj.isContentUnread())) {
                    pushMessages.add(obj);
                }
            } else if (update instanceof TLRPC.TL_updateReadMessagesContents) {
                for (int a = 0; a < update.messages.size(); a++) {
                    long id = update.messages.get(a);
                    markAsReadMessages.add(id);
                }
            } else if (update instanceof TLRPC.TL_updateReadHistoryInbox) {
                TLRPC.Peer peer = ((TLRPC.TL_updateReadHistoryInbox) update).peer;
                if (peer.chat_id != 0) {
                    markAsReadMessagesInbox.put(-peer.chat_id, (long) update.max_id);
                } else {
                    markAsReadMessagesInbox.put(peer.user_id, (long) update.max_id);
                }
            } else if (update instanceof TLRPC.TL_updateReadHistoryOutbox) {
                TLRPC.Peer peer = ((TLRPC.TL_updateReadHistoryOutbox) update).peer;
                if (peer.chat_id != 0) {
                    markAsReadMessagesOutbox.put(-peer.chat_id, update.max_id);
                } else {
                    markAsReadMessagesOutbox.put(peer.user_id, update.max_id);
                }
            } else if (update instanceof TLRPC.TL_updateDeleteMessages) {
                ArrayList<Integer> arrayList = deletedMessages.get(0);
                if (arrayList == null) {
                    arrayList = new ArrayList<>();
                    deletedMessages.put(0, arrayList);
                }
                arrayList.addAll(update.messages);
            } else if (update instanceof TLRPC.TL_updateUserTyping || update instanceof TLRPC.TL_updateChatUserTyping) {
                if (update.user_id != UserConfig.getClientUserId()) {
                    long uid = -update.chat_id;
                    if (uid == 0) {
                        uid = update.user_id;
                    }
                    ArrayList<PrintingUser> arr = printingUsers.get(uid);
                    if (update.action instanceof TLRPC.TL_sendMessageCancelAction) {
                        if (arr != null) {
                            for (int a = 0; a < arr.size(); a++) {
                                PrintingUser pu = arr.get(a);
                                if (pu.userId == update.user_id) {
                                    arr.remove(a);
                                    printChanged = true;
                                    break;
                                }
                            }
                            if (arr.isEmpty()) {
                                printingUsers.remove(uid);
                            }
                        }
                    } else {
                        if (arr == null) {
                            arr = new ArrayList<>();
                            printingUsers.put(uid, arr);
                        }
                        boolean exist = false;
                        for (PrintingUser u : arr) {
                            if (u.userId == update.user_id) {
                                exist = true;
                                u.lastTime = currentTime;
                                if (u.action.getClass() != update.action.getClass()) {
                                    printChanged = true;
                                }
                                u.action = update.action;
                                break;
                            }
                        }
                        if (!exist) {
                            PrintingUser newUser = new PrintingUser();
                            newUser.userId = update.user_id;
                            newUser.lastTime = currentTime;
                            newUser.action = update.action;
                            arr.add(newUser);
                            printChanged = true;
                        }
                    }
                    onlinePrivacy.put(update.user_id, ConnectionsManager.getInstance().getCurrentTime());
                }
            } else if (update instanceof TLRPC.TL_updateChatParticipants) {
                interfaceUpdateMask |= UPDATE_MASK_CHAT_MEMBERS;
                chatInfoToUpdate.add(update.participants);
            } else if (update instanceof TLRPC.TL_updateUserStatus) {
                interfaceUpdateMask |= UPDATE_MASK_STATUS;
                updatesOnMainThread.add(update);
            } else if (update instanceof TLRPC.TL_updateUserName) {
                interfaceUpdateMask |= UPDATE_MASK_NAME;
                updatesOnMainThread.add(update);
            } else if (update instanceof TLRPC.TL_updateUserPhoto) {
                interfaceUpdateMask |= UPDATE_MASK_AVATAR;
                MessagesStorage.getInstance().clearUserPhotos(update.user_id);
                updatesOnMainThread.add(update);
            } else if (update instanceof TLRPC.TL_updateUserPhone) {
                interfaceUpdateMask |= UPDATE_MASK_PHONE;
                updatesOnMainThread.add(update);
            } else if (update instanceof TLRPC.TL_updateContactRegistered) {
                if (enableJoined && usersDict.containsKey(update.user_id) && !MessagesStorage.getInstance().isDialogHasMessages(update.user_id)) {
                    TLRPC.TL_messageService newMessage = new TLRPC.TL_messageService();
                    newMessage.action = new TLRPC.TL_messageActionUserJoined();
                    newMessage.local_id = newMessage.id = UserConfig.getNewMessageId();
                    UserConfig.saveConfig(false);
                    newMessage.unread = false;
                    newMessage.flags = TLRPC.MESSAGE_FLAG_HAS_FROM_ID;
                    newMessage.date = update.date;
                    newMessage.from_id = update.user_id;
                    newMessage.to_id = new TLRPC.TL_peerUser();
                    newMessage.to_id.user_id = UserConfig.getClientUserId();
                    newMessage.dialog_id = update.user_id;

                    messagesArr.add(newMessage);
                    MessageObject obj = new MessageObject(newMessage, usersDict, chatsDict, true);
                    ArrayList<MessageObject> arr = messages.get(newMessage.dialog_id);
                    if (arr == null) {
                        arr = new ArrayList<>();
                        messages.put(newMessage.dialog_id, arr);
                    }
                    arr.add(obj);
                }
            } else if (update instanceof TLRPC.TL_updateContactLink) {
                if (update.my_link instanceof TLRPC.TL_contactLinkContact) {
                    int idx = contactsIds.indexOf(-update.user_id);
                    if (idx != -1) {
                        contactsIds.remove(idx);
                    }
                    if (!contactsIds.contains(update.user_id)) {
                        contactsIds.add(update.user_id);
                    }
                } else {
                    int idx = contactsIds.indexOf(update.user_id);
                    if (idx != -1) {
                        contactsIds.remove(idx);
                    }
                    if (!contactsIds.contains(update.user_id)) {
                        contactsIds.add(-update.user_id);
                    }
                }
            } else if (update instanceof TLRPC.TL_updateNewAuthorization) {
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.newSessionReceived);
                    }
                });
                TLRPC.TL_messageService newMessage = new TLRPC.TL_messageService();
                newMessage.action = new TLRPC.TL_messageActionLoginUnknownLocation();
                newMessage.action.title = update.device;
                newMessage.action.address = update.location;
                newMessage.local_id = newMessage.id = UserConfig.getNewMessageId();
                UserConfig.saveConfig(false);
                newMessage.unread = true;
                newMessage.flags = TLRPC.MESSAGE_FLAG_HAS_FROM_ID;
                newMessage.date = update.date;
                newMessage.from_id = 777000;
                newMessage.to_id = new TLRPC.TL_peerUser();
                newMessage.to_id.user_id = UserConfig.getClientUserId();
                newMessage.dialog_id = 777000;

                messagesArr.add(newMessage);
                MessageObject obj = new MessageObject(newMessage, usersDict, chatsDict, true);
                ArrayList<MessageObject> arr = messages.get(newMessage.dialog_id);
                if (arr == null) {
                    arr = new ArrayList<>();
                    messages.put(newMessage.dialog_id, arr);
                }
                arr.add(obj);
                pushMessages.add(obj);
            } else if (update instanceof TLRPC.TL_updateNewGeoChatMessage) {
                //DEPRECATED
            } else if (update instanceof TLRPC.TL_updateNewEncryptedMessage) {
                ArrayList<TLRPC.Message> decryptedMessages = SecretChatHelper.getInstance().decryptMessage(((TLRPC.TL_updateNewEncryptedMessage) update).message);
                if (decryptedMessages != null && !decryptedMessages.isEmpty()) {
                    int cid = ((TLRPC.TL_updateNewEncryptedMessage) update).message.chat_id;
                    long uid = ((long) cid) << 32;
                    ArrayList<MessageObject> arr = messages.get(uid);
                    if (arr == null) {
                        arr = new ArrayList<>();
                        messages.put(uid, arr);
                    }
                    for (TLRPC.Message message : decryptedMessages) {
                        ImageLoader.saveMessageThumbs(message);
                        messagesArr.add(message);
                        MessageObject obj = new MessageObject(message, usersDict, chatsDict, true);
                        arr.add(obj);
                        pushMessages.add(obj);
                    }
                }
            } else if (update instanceof TLRPC.TL_updateEncryptedChatTyping) {
                TLRPC.EncryptedChat encryptedChat = getEncryptedChatDB(update.chat_id);
                if (encryptedChat != null) {
                    update.user_id = encryptedChat.user_id;
                    long uid = ((long) update.chat_id) << 32;
                    ArrayList<PrintingUser> arr = printingUsers.get(uid);
                    if (arr == null) {
                        arr = new ArrayList<>();
                        printingUsers.put(uid, arr);
                    }
                    boolean exist = false;
                    for (PrintingUser u : arr) {
                        if (u.userId == update.user_id) {
                            exist = true;
                            u.lastTime = currentTime;
                            u.action = new TLRPC.TL_sendMessageTypingAction();
                            break;
                        }
                    }
                    if (!exist) {
                        PrintingUser newUser = new PrintingUser();
                        newUser.userId = update.user_id;
                        newUser.lastTime = currentTime;
                        newUser.action = new TLRPC.TL_sendMessageTypingAction();
                        arr.add(newUser);
                        printChanged = true;
                    }
                    onlinePrivacy.put(update.user_id, ConnectionsManager.getInstance().getCurrentTime());
                }
            } else if (update instanceof TLRPC.TL_updateEncryptedMessagesRead) {
                markAsReadEncrypted.put(update.chat_id, Math.max(update.max_date, update.date));
                tasks.add((TLRPC.TL_updateEncryptedMessagesRead) update);
            } else if (update instanceof TLRPC.TL_updateChatParticipantAdd) {
                MessagesStorage.getInstance().updateChatInfo(update.chat_id, update.user_id, 0, update.inviter_id, update.version);
            } else if (update instanceof TLRPC.TL_updateChatParticipantDelete) {
                MessagesStorage.getInstance().updateChatInfo(update.chat_id, update.user_id, 1, 0, update.version);
            } else if (update instanceof TLRPC.TL_updateDcOptions) {
                ConnectionsManager.getInstance().updateDcSettings();
            } else if (update instanceof TLRPC.TL_updateEncryption) {
                SecretChatHelper.getInstance().processUpdateEncryption((TLRPC.TL_updateEncryption) update, usersDict);
            } else if (update instanceof TLRPC.TL_updateUserBlocked) {
                final TLRPC.TL_updateUserBlocked finalUpdate = (TLRPC.TL_updateUserBlocked) update;
                if (finalUpdate.blocked) {
                    ArrayList<Integer> ids = new ArrayList<>();
                    ids.add(finalUpdate.user_id);
                    MessagesStorage.getInstance().putBlockedUsers(ids, false);
                } else {
                    MessagesStorage.getInstance().deleteBlockedUser(finalUpdate.user_id);
                }
                MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                if (finalUpdate.blocked) {
                                    if (!blockedUsers.contains(finalUpdate.user_id)) {
                                        blockedUsers.add(finalUpdate.user_id);
                                    }
                                } else {
                                    blockedUsers.remove((Integer) finalUpdate.user_id);
                                }
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.blockedUsersDidLoaded);
                            }
                        });
                    }
                });
            } else if (update instanceof TLRPC.TL_updateNotifySettings) {
                updatesOnMainThread.add(update);
            } else if (update instanceof TLRPC.TL_updateServiceNotification) {
                TLRPC.TL_updateServiceNotification notification = (TLRPC.TL_updateServiceNotification) update;
                if (notification.popup && notification.message != null && notification.message.length() > 0) {
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.needShowAlert, 2, notification.message);
                }
                TLRPC.TL_message newMessage = new TLRPC.TL_message();
                newMessage.local_id = newMessage.id = UserConfig.getNewMessageId();
                UserConfig.saveConfig(false);
                newMessage.unread = true;
                newMessage.flags = TLRPC.MESSAGE_FLAG_HAS_FROM_ID;
                newMessage.date = ConnectionsManager.getInstance().getCurrentTime();
                newMessage.from_id = 777000;
                newMessage.to_id = new TLRPC.TL_peerUser();
                newMessage.to_id.user_id = UserConfig.getClientUserId();
                newMessage.dialog_id = 777000;
                newMessage.media = update.media;
                newMessage.flags |= TLRPC.MESSAGE_FLAG_HAS_MEDIA;
                newMessage.message = notification.message;

                messagesArr.add(newMessage);
                MessageObject obj = new MessageObject(newMessage, usersDict, chatsDict, true);
                ArrayList<MessageObject> arr = messages.get(newMessage.dialog_id);
                if (arr == null) {
                    arr = new ArrayList<>();
                    messages.put(newMessage.dialog_id, arr);
                }
                arr.add(obj);
                pushMessages.add(obj);
            } else if (update instanceof TLRPC.TL_updatePrivacy) {
                updatesOnMainThread.add(update);
            } else if (update instanceof TLRPC.TL_updateWebPage) {
                webPages.put(update.webpage.id, update.webpage);
            } else if (update instanceof TLRPC.TL_updateChannelTooLong) {
                getChannelDifference(update.channel_id);
            } else if (update instanceof TLRPC.TL_updateChannelGroup) {
                ArrayList<TLRPC.TL_messageGroup> arrayList = channelsGroups.get(update.channel_id);
                if (arrayList == null) {
                    arrayList = new ArrayList<>();
                    channelsGroups.put(update.channel_id, arrayList);
                }
                arrayList.add(update.group);
            } else if (update instanceof TLRPC.TL_updateReadChannelInbox) {
                long message_id = update.max_id;
                message_id |= ((long) update.channel_id) << 32;
                markAsReadMessagesInbox.put(-update.channel_id, message_id);

                long dialog_id = -update.channel_id;
                Integer value = dialogs_read_inbox_max.get(dialog_id);
                if (value == null) {
                    value = MessagesStorage.getInstance().getChannelReadInboxMax(update.channel_id);
                }
                dialogs_read_inbox_max.put(dialog_id, Math.max(value, update.max_id));
            } else if (update instanceof TLRPC.TL_updateDeleteChannelMessages) {
                ArrayList<Integer> arrayList = deletedMessages.get(update.channel_id);
                if (arrayList == null) {
                    arrayList = new ArrayList<>();
                    deletedMessages.put(update.channel_id, arrayList);
                }
                arrayList.addAll(update.messages);
            } else if (update instanceof TLRPC.TL_updateChannel) {
                updatesOnMainThread.add(update);
            } else if (update instanceof TLRPC.TL_updateChannelMessageViews) {
                SparseIntArray array = channelViews.get(update.channel_id);
                if (array == null) {
                    array = new SparseIntArray();
                    channelViews.put(update.channel_id, array);
                }
                array.put(update.id, update.views);
            } else if (update instanceof TLRPC.TL_updateChatParticipantAdmin) {
                MessagesStorage.getInstance().updateChatInfo(update.chat_id, update.user_id, 2, update.is_admin ? 1 : 0, update.version);
            } else if (update instanceof TLRPC.TL_updateChatAdmins) {
                updatesOnMainThread.add(update);
            } else if (update instanceof TLRPC.TL_updateStickerSets) {
                updatesOnMainThread.add(update);
            } else if (update instanceof TLRPC.TL_updateStickerSetsOrder) {
                updatesOnMainThread.add(update);
            } else if (update instanceof TLRPC.TL_updateNewStickerSet) {
                updatesOnMainThread.add(update);
            } else if (update instanceof TLRPC.TL_updateSavedGifs) {
                updatesOnMainThread.add(update);
            }
        }
        if (!messages.isEmpty()) {
            for (HashMap.Entry<Long, ArrayList<MessageObject>> pair : messages.entrySet()) {
                Long key = pair.getKey();
                ArrayList<MessageObject> value = pair.getValue();
                if (updatePrintingUsersWithNewMessages(key, value)) {
                    printChanged = true;
                }
            }
        }

        if (printChanged) {
            updatePrintingStrings();
        }

        final int interfaceUpdateMaskFinal = interfaceUpdateMask;
        final boolean printChangedArg = printChanged;

        if (!contactsIds.isEmpty()) {
            ContactsController.getInstance().processContactsUpdates(contactsIds, usersDict);
        }

        if (!pushMessages.isEmpty()) {
            MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
                @Override
                public void run() {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationsController.getInstance().processNewMessages(pushMessages, true);
                        }
                    });
                }
            });
        }

        if (!messagesArr.isEmpty()) {
            MessagesStorage.getInstance().putMessages(messagesArr, true, true, false, MediaController.getInstance().getAutodownloadMask());
        }

        if (channelViews.size() != 0) {
            MessagesStorage.getInstance().putChannelViews(channelViews, true);
        }
        if (channelsGroups.size() != 0) {
            //MessagesStorage.getInstance().applyNewChannelsGroups(channelsGroups); TODO
        }

        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                int updateMask = interfaceUpdateMaskFinal;

                if (!updatesOnMainThread.isEmpty()) {
                    ArrayList<TLRPC.User> dbUsers = new ArrayList<>();
                    ArrayList<TLRPC.User> dbUsersStatus = new ArrayList<>();
                    SharedPreferences.Editor editor = null;
                    for (int a = 0; a < updatesOnMainThread.size(); a++) {
                        final TLRPC.Update update = updatesOnMainThread.get(a);
                        final TLRPC.User toDbUser = new TLRPC.User();
                        toDbUser.id = update.user_id;
                        final TLRPC.User currentUser = getUser(update.user_id);
                        if (update instanceof TLRPC.TL_updatePrivacy) {
                            if (update.key instanceof TLRPC.TL_privacyKeyStatusTimestamp) {
                                ContactsController.getInstance().setPrivacyRules(update.rules);
                            }
                        } else if (update instanceof TLRPC.TL_updateUserStatus) {
                            if (update.status instanceof TLRPC.TL_userStatusRecently) {
                                update.status.expires = -100;
                            } else if (update.status instanceof TLRPC.TL_userStatusLastWeek) {
                                update.status.expires = -101;
                            } else if (update.status instanceof TLRPC.TL_userStatusLastMonth) {
                                update.status.expires = -102;
                            }
                            if (currentUser != null) {
                                currentUser.id = update.user_id;
                                currentUser.status = update.status;
                            }
                            toDbUser.status = update.status;
                            dbUsersStatus.add(toDbUser);
                            if (update.user_id == UserConfig.getClientUserId()) {
                                NotificationsController.getInstance().setLastOnlineFromOtherDevice(update.status.expires);
                            }
                        } else if (update instanceof TLRPC.TL_updateUserName) {
                            if (currentUser != null) {
                                if (!UserObject.isContact(currentUser)) {
                                    currentUser.first_name = update.first_name;
                                    currentUser.last_name = update.last_name;
                                }
                                if (currentUser.username != null && currentUser.username.length() > 0) {
                                    usersByUsernames.remove(currentUser.username);
                                }
                                if (update.username != null && update.username.length() > 0) {
                                    usersByUsernames.put(update.username, currentUser);
                                }
                                currentUser.username = update.username;
                            }
                            toDbUser.first_name = update.first_name;
                            toDbUser.last_name = update.last_name;
                            toDbUser.username = update.username;
                            dbUsers.add(toDbUser);
                        } else if (update instanceof TLRPC.TL_updateUserPhoto) {
                            if (currentUser != null) {
                                currentUser.photo = update.photo;
                            }
                            toDbUser.photo = update.photo;
                            dbUsers.add(toDbUser);
                        } else if (update instanceof TLRPC.TL_updateUserPhone) {
                            if (currentUser != null) {
                                currentUser.phone = update.phone;
                                Utilities.phoneBookQueue.postRunnable(new Runnable() {
                                    @Override
                                    public void run() {
                                        ContactsController.getInstance().addContactToPhoneBook(currentUser, true);
                                    }
                                });
                            }
                            toDbUser.phone = update.phone;
                            dbUsers.add(toDbUser);
                        } else if (update instanceof TLRPC.TL_updateNotifySettings) {
                            TLRPC.TL_updateNotifySettings updateNotifySettings = (TLRPC.TL_updateNotifySettings) update;
                            if (update.notify_settings instanceof TLRPC.TL_peerNotifySettings && updateNotifySettings.peer instanceof TLRPC.TL_notifyPeer) {
                                if (editor == null) {
                                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                    editor = preferences.edit();
                                }
                                long dialog_id;
                                if (updateNotifySettings.peer.peer.user_id != 0) {
                                    dialog_id = updateNotifySettings.peer.peer.user_id;
                                } else if (updateNotifySettings.peer.peer.chat_id != 0) {
                                    dialog_id = -updateNotifySettings.peer.peer.chat_id;
                                } else {
                                    dialog_id = -updateNotifySettings.peer.peer.channel_id;
                                }

                                TLRPC.Dialog dialog = dialogs_dict.get(dialog_id);
                                if (dialog != null) {
                                    dialog.notify_settings = update.notify_settings;
                                }
                                if (update.notify_settings.mute_until > ConnectionsManager.getInstance().getCurrentTime()) {
                                    int until = 0;
                                    if (update.notify_settings.mute_until > ConnectionsManager.getInstance().getCurrentTime() + 60 * 60 * 24 * 365) {
                                        editor.putInt("notify2_" + dialog_id, 2);
                                        if (dialog != null) {
                                            dialog.notify_settings.mute_until = Integer.MAX_VALUE;
                                        }
                                    } else {
                                        until = update.notify_settings.mute_until;
                                        editor.putInt("notify2_" + dialog_id, 3);
                                        editor.putInt("notifyuntil_" + dialog_id, update.notify_settings.mute_until);
                                        if (dialog != null) {
                                            dialog.notify_settings.mute_until = until;
                                        }
                                    }
                                    MessagesStorage.getInstance().setDialogFlags(dialog_id, ((long) until << 32) | 1);
                                    NotificationsController.getInstance().removeNotificationsForDialog(dialog_id);
                                } else {
                                    if (dialog != null) {
                                        dialog.notify_settings.mute_until = 0;
                                    }
                                    editor.remove("notify2_" + dialog_id);
                                    MessagesStorage.getInstance().setDialogFlags(dialog_id, 0);
                                }
                            }
                        } else if (update instanceof TLRPC.TL_updateChannel) {
                            TLRPC.Dialog dialog = dialogs_dict.get(-(long) update.channel_id);
                            TLRPC.Chat chat = getChat(update.channel_id);
                            if (dialog == null && chat instanceof TLRPC.TL_channel && !chat.left) {
                                Utilities.stageQueue.postRunnable(new Runnable() {
                                    @Override
                                    public void run() {
                                        getChannelDifference(update.channel_id, 1);
                                    }
                                });
                            } else if (chat.left && dialog != null) {
                                deleteDialog(dialog.id, 0);
                            }
                            updateMask |= UPDATE_MASK_CHANNEL;
                            loadFullChat(update.channel_id, 0, true);
                        } else if (update instanceof TLRPC.TL_updateChatAdmins) {
                            updateMask |= UPDATE_MASK_CHAT_ADMINS;
                        } else if (update instanceof TLRPC.TL_updateStickerSets) {
                            StickersQuery.loadStickers(false, true);
                        } else if (update instanceof TLRPC.TL_updateStickerSetsOrder) {
                            StickersQuery.reorderStickers(update.order);
                        } else if (update instanceof TLRPC.TL_updateNewStickerSet) {
                            StickersQuery.addNewStickerSet(update.stickerset);
                        } else if (update instanceof TLRPC.TL_updateSavedGifs) {
                            SharedPreferences.Editor editor2 = ApplicationLoader.applicationContext.getSharedPreferences("emoji", Activity.MODE_PRIVATE).edit();
                            editor2.putLong("lastGifLoadTime", 0).commit();
                        }
                    }
                    if (editor != null) {
                        editor.commit();
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.notificationsSettingsUpdated);
                    }
                    MessagesStorage.getInstance().updateUsers(dbUsersStatus, true, true, true);
                    MessagesStorage.getInstance().updateUsers(dbUsers, false, true, true);
                }

                if (!webPages.isEmpty()) {
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.didReceivedWebpagesInUpdates, webPages);
                }

                if (!messages.isEmpty()) {
                    for (HashMap.Entry<Long, ArrayList<MessageObject>> entry : messages.entrySet()) {
                        Long key = entry.getKey();
                        ArrayList<MessageObject> value = entry.getValue();
                        updateInterfaceWithMessages(key, value);
                    }
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
                }
                if (printChangedArg) {
                    updateMask |= UPDATE_MASK_USER_PRINT;
                }
                if (!contactsIds.isEmpty()) {
                    updateMask |= UPDATE_MASK_NAME;
                    updateMask |= UPDATE_MASK_USER_PHONE;
                }
                if (!chatInfoToUpdate.isEmpty()) {
                    for (TLRPC.ChatParticipants info : chatInfoToUpdate) {
                        MessagesStorage.getInstance().updateChatParticipants(info);
                    }
                }
                if (channelViews.size() != 0) {
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.didUpdatedMessagesViews, channelViews);
                }
                if (updateMask != 0) {
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, updateMask);
                }
            }
        });

        MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
            @Override
            public void run() {
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        int updateMask = 0;
                        if (markAsReadMessagesInbox.size() != 0 || markAsReadMessagesOutbox.size() != 0) {
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.messagesRead, markAsReadMessagesInbox, markAsReadMessagesOutbox);
                            NotificationsController.getInstance().processReadMessages(markAsReadMessagesInbox, 0, 0, 0, false);
                            for (int b = 0; b < markAsReadMessagesInbox.size(); b++) {
                                int key = markAsReadMessagesInbox.keyAt(b);
                                long messageId = markAsReadMessagesInbox.get(key);
                                TLRPC.Dialog dialog = dialogs_dict.get((long) key);
                                if (dialog != null && dialog.top_message <= (int) messageId) {
                                    MessageObject obj = dialogMessage.get(dialog.id);
                                    if (obj != null) {
                                        obj.setIsRead();
                                        updateMask |= UPDATE_MASK_READ_DIALOG_MESSAGE;
                                    }
                                }
                            }
                            for (int b = 0; b < markAsReadMessagesOutbox.size(); b++) {
                                int key = markAsReadMessagesOutbox.keyAt(b);
                                int messageId = markAsReadMessagesOutbox.get(key);
                                TLRPC.Dialog dialog = dialogs_dict.get((long) key);
                                if (dialog != null && dialog.top_message <= messageId) {
                                    MessageObject obj = dialogMessage.get(dialog.id);
                                    if (obj != null) {
                                        obj.setIsRead();
                                        updateMask |= UPDATE_MASK_READ_DIALOG_MESSAGE;
                                    }
                                }
                            }
                        }
                        if (!markAsReadEncrypted.isEmpty()) {
                            for (HashMap.Entry<Integer, Integer> entry : markAsReadEncrypted.entrySet()) {
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.messagesReadEncrypted, entry.getKey(), entry.getValue());
                                long dialog_id = (long) (entry.getKey()) << 32;
                                TLRPC.Dialog dialog = dialogs_dict.get(dialog_id);
                                if (dialog != null) {
                                    MessageObject message = dialogMessage.get(dialog_id);
                                    if (message != null && message.messageOwner.date <= entry.getValue()) {
                                        message.setIsRead();
                                        updateMask |= UPDATE_MASK_READ_DIALOG_MESSAGE;
                                    }
                                }
                            }
                        }
                        if (!markAsReadMessages.isEmpty()) {
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.messagesReadContent, markAsReadMessages);
                        }
                        if (deletedMessages.size() != 0) {
                            for (int a = 0; a < deletedMessages.size(); a++) {
                                int key = deletedMessages.keyAt(a);
                                ArrayList<Integer> arrayList = deletedMessages.get(key);
                                if (arrayList == null) {
                                    continue;
                                }
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.messagesDeleted, arrayList, key);
                                if (key == 0) {
                                    for (int b = 0; b < arrayList.size(); b++) {
                                        Integer id = arrayList.get(b);
                                        MessageObject obj = dialogMessagesByIds.get(id);
                                        if (obj != null) {
                                            obj.deleted = true;
                                        }
                                    }
                                } else {
                                    MessageObject obj = dialogMessage.get((long) -key);
                                    if (obj != null) {
                                        for (int b = 0; b < arrayList.size(); b++) {
                                            if (obj.getId() == arrayList.get(b)) {
                                                obj.deleted = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            NotificationsController.getInstance().removeDeletedMessagesFromNotifications(deletedMessages);
                        }
                        if (updateMask != 0) {
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, updateMask);
                        }
                    }
                });
            }
        });

        if (!webPages.isEmpty()) {
            MessagesStorage.getInstance().putWebPages(webPages);
        }
        if (markAsReadMessagesInbox.size() != 0 || markAsReadMessagesOutbox.size() != 0 || !markAsReadEncrypted.isEmpty()) {
            if (markAsReadMessagesInbox.size() != 0) {
                MessagesStorage.getInstance().updateDialogsWithReadMessages(markAsReadMessagesInbox, true);
            }
            MessagesStorage.getInstance().markMessagesAsRead(markAsReadMessagesInbox, markAsReadMessagesOutbox, markAsReadEncrypted, true);
        }
        if (!markAsReadMessages.isEmpty()) {
            MessagesStorage.getInstance().markMessagesContentAsRead(markAsReadMessages);
        }
        if (deletedMessages.size() != 0) {
            for (int a = 0; a < deletedMessages.size(); a++) {
                int key = deletedMessages.keyAt(a);
                ArrayList<Integer> arrayList = deletedMessages.get(key);
                MessagesStorage.getInstance().markMessagesAsDeleted(arrayList, true, key);
                MessagesStorage.getInstance().updateDialogsWithDeletedMessages(arrayList, true, key);
            }
        }
        if (!tasks.isEmpty()) {
            for (TLRPC.TL_updateEncryptedMessagesRead update : tasks) {
                MessagesStorage.getInstance().createTaskForSecretChat(update.chat_id, update.max_date, update.date, 1, null);
            }
        }

        return true;

    }
}
