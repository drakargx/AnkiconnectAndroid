package com.kamwithk.ankiconnectandroid.request_parsers;

import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Parser {
    public static Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private static final String FIELD_SEPARATOR = Character.toString('\u001f');

    public static JsonObject parse(String raw_data) {
        return JsonParser.parseString(raw_data).getAsJsonObject();
    }

    public static String get_action(JsonObject data) {
        return data.get("action").getAsString();
    }

    public static int get_version(JsonObject data, int fallback) {
        if (data.has("version")) {
            return data.get("version").getAsInt();
        }
        return fallback;
    }

    public static String getDeckName(JsonObject raw_data) {
        return raw_data.get("params").getAsJsonObject().get("note").getAsJsonObject().get("deckName").getAsString();
    }

    public static String getModelName(JsonObject raw_data) {
        return raw_data.get("params").getAsJsonObject().get("note").getAsJsonObject().get("modelName").getAsString();
    }

    public static String getModelNameFromParam(JsonObject raw_data) {
        return raw_data.get("params").getAsJsonObject().get("modelName").getAsString();
    }

    public static Map<String, String> getNoteValues(JsonObject raw_data) {
        Type fieldType = new TypeToken<Map<String, String>>() {}.getType();
        return gson.fromJson(raw_data.get("params").getAsJsonObject().get("note").getAsJsonObject().get("fields"), fieldType);
    }

    public static Set<String> getNoteTags(JsonObject raw_data) {
        Type fieldType = new TypeToken<Set<String>>() {}.getType();
        return gson.fromJson(raw_data.get("params").getAsJsonObject().get("note").getAsJsonObject().get("tags"), fieldType);
    }

    public static String getNoteQuery(JsonObject raw_data) {
        return raw_data.get("params").getAsJsonObject().get("query").getAsString();
    }

    public static long getUpdateNoteFieldsId(JsonObject raw_data) {
        return raw_data.get("params").getAsJsonObject().get("note").getAsJsonObject().get("id").getAsLong();
    }

    public static Map<String, String> getUpdateNoteFieldsFields(JsonObject raw_data) {
        Type fieldType = new TypeToken<Map<String, String>>() {}.getType();
        return gson.fromJson(raw_data.get("params").getAsJsonObject().get("note").getAsJsonObject().get("fields"), fieldType);
    }

    /**
     * For each key ("audio", "video", "picture"), expect EITHER a list or singular json object!
     * According to the official Anki-Connect docs:
     * > If you choose to include [audio, video, picture keys], they should contain a single object
     * > or an array of objects
     */
    public static ArrayList<MediaRequest> getNoteMediaRequests(JsonObject raw_data) {
        Map<String, MediaRequest.MediaType> media_types = Map.of(
            "audio", MediaRequest.MediaType.AUDIO,
            "video", MediaRequest.MediaType.VIDEO,
            "picture", MediaRequest.MediaType.PICTURE
        );
        JsonObject note_json = raw_data.get("params").getAsJsonObject().get("note").getAsJsonObject();

        ArrayList<MediaRequest> request_medias = new ArrayList<>();
        for (Map.Entry<String, MediaRequest.MediaType> entry: media_types.entrySet()) {
            JsonElement media_value = note_json.get(entry.getKey());
            if (media_value == null) {
                continue;
            }
            if (media_value.isJsonArray()) {
                for (JsonElement media_element: media_value.getAsJsonArray()) {
                    JsonObject media_object = media_element.getAsJsonObject();
                    MediaRequest requestMedia = MediaRequest.fromJson(media_object, entry.getValue());
                    request_medias.add(requestMedia);
                }
            } else if (media_value.isJsonObject()) {
                JsonObject media_object = media_value.getAsJsonObject();
                MediaRequest requestMedia = MediaRequest.fromJson(media_object, entry.getValue());
                request_medias.add(requestMedia);
            }
        }
        return request_medias;
    }

    public static class NoteOptions {

        private final boolean allowDuplicate;

        private final String duplicateScope;

        private final String deckName;

        private final boolean checkChildren;

        private final boolean checkAllModels;

        public NoteOptions(boolean allowDuplicate,
                           String duplicateScope,
                           String deckName,
                           boolean checkChildren,
                           boolean checkAllModels) {
            this.allowDuplicate = allowDuplicate;
            this.duplicateScope = duplicateScope;
            this.deckName = deckName;
            this.checkChildren = checkChildren;
            this.checkAllModels = checkAllModels;
        }

        public boolean isAllowDuplicate() {
            return allowDuplicate;
        }

        public String getDuplicateScope() {
            return duplicateScope;
        }

        public String getDeckName() {
            return deckName;
        }

        public boolean isCheckChildren() {
            return checkChildren;
        }

        public boolean isCheckAllModels() {
            return checkAllModels;
        }
    }

    public static class NoteFront {
        private final String fieldName;
        private final String fieldValue;
        private final String modelName;

        private final String deckName;

        private final List<String> tags;

        private final NoteOptions options;

        public NoteFront(String fieldName, String fieldValue, String modelName, String deckName, List<String> tags, NoteOptions options) {
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
            this.modelName = modelName;
            this.deckName = deckName;
            this.tags = tags;
            this.options = options;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getFieldValue() {
            return fieldValue;
        }

        public String getModelName() {
            return modelName;
        }

        public String getDeckName() {
            return deckName;
        }

        public List<String> getTags() {
            return tags;
        }

        public NoteOptions getOptions() {
            return options;
        }
    }

    /**
     * Gets the first field of the note
     */
    public static ArrayList<NoteFront> getNoteFront(JsonObject raw_data) {
        JsonArray notes = raw_data.get("params").getAsJsonObject().get("notes").getAsJsonArray();
        ArrayList<NoteFront> projections = new ArrayList<>();
        ArrayList<String> tagList;

        for (JsonElement jsonElement : notes) {
            tagList = new ArrayList<>();
            JsonObject jsonObject = jsonElement.getAsJsonObject().get("fields").getAsJsonObject();

            String field = jsonObject.keySet().toArray()[0].toString();
            String value = jsonObject.get(field).getAsString();
            String model = jsonElement.getAsJsonObject().get("modelName").getAsString();
            String deckName = jsonElement.getAsJsonObject().get("deckName").getAsString();
            JsonArray jsonTags = jsonElement.getAsJsonObject().get("tags").getAsJsonArray();
            for (JsonElement tag: jsonTags) {
                tagList.add(tag.getAsString());
            }

            //Get options if they exist
            NoteOptions noteOptions = getNoteOptions(jsonElement.getAsJsonObject());

            NoteFront projection = new NoteFront(field, value, model, deckName, tagList, noteOptions);
            projections.add(projection);
        }

        return projections;
    }

    private static NoteOptions getNoteOptions(JsonObject notesObject) {
        boolean allowDuplicate = false;
        String duplicateScope = null;
        String duplicateScopeDeckName = null;
        boolean duplicateScopeCheckChildren = false;
        boolean duplicateScopeCheckAllModels = false;

        if (!notesObject.has("options")) {
            return null;
        }

        JsonObject optionsObject = notesObject.get("options").getAsJsonObject();

        if (optionsObject.has("allowDuplicate")) {
            allowDuplicate = optionsObject.get("allowDuplicate").getAsBoolean();
        }
        if (optionsObject.has("duplicateScope")) {
            duplicateScope = optionsObject.get("duplicateScope").getAsString();
        }
        if (optionsObject.has("duplicateScopeOptions")) {
            JsonObject duplicateScopeObject = optionsObject.get("duplicateScopeOptions").getAsJsonObject();

            if (duplicateScopeObject.has("deckName")) {
                JsonElement duplicateDeckName =  duplicateScopeObject.get("deckName");
                if(!duplicateDeckName.isJsonNull()) {
                    duplicateScopeDeckName = duplicateDeckName.getAsString();
                }
            }
            if (duplicateScopeObject.has("deckName")) {
                duplicateScopeCheckChildren = duplicateScopeObject.get("checkChildren").getAsBoolean();
            }
            if (duplicateScopeObject.has("deckName")) {
                duplicateScopeCheckAllModels = duplicateScopeObject.get("checkAllModels").getAsBoolean();
            }
        }

        return new NoteOptions(allowDuplicate,
                duplicateScope,
                duplicateScopeDeckName,
                duplicateScopeCheckChildren,
                duplicateScopeCheckAllModels);
    }

    public static boolean[] getNoteTrues(JsonObject raw_data) {
        int num_notes = raw_data.get("params").getAsJsonObject().get("notes").getAsJsonArray().size();
        boolean[] array = new boolean[num_notes];
        Arrays.fill(array, true);

        return array;
    }

    public static String getMediaFilename(JsonObject raw_data) {
        return raw_data.get("params").getAsJsonObject().get("filename").getAsString();
    }

    public static byte[] getMediaData(JsonObject raw_data) {
        String encoded = raw_data.get("params").getAsJsonObject().get("data").getAsString();
        return Base64.decode(encoded, Base64.DEFAULT);
    }

    public static JsonArray getMultiActions(JsonObject raw_data) {
        return raw_data.get("params").getAsJsonObject().get("actions").getAsJsonArray();
    }

    public static String[] splitFields(String fields) {
        return fields != null? fields.split(FIELD_SEPARATOR, -1): null;
    }
}

