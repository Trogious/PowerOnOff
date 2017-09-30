package net.swmud.trog.json;

import com.google.gson.annotations.SerializedName;

public class JsonResponse {
    @SerializedName("jsonrpc")
    private String jsonrpc;
    @SerializedName("id")
    private String id;
    @SerializedName("result")
    private Object result;

    public long getId() {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
        }

        return 0;
    }
}
