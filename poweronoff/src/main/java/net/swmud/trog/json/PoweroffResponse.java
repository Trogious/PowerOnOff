package net.swmud.trog.json;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class PoweroffResponse implements Serializable {
    @SerializedName("result")
    public Result result;

    public static class Result implements Serializable {
        @SerializedName("msg")
        public String message;
    }
}
