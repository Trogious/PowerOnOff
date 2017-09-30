package net.swmud.trog.json;

import java.util.HashMap;
import java.util.Map;

public class PoweroffRequest {
    public JsonRpc.JsonRequest getJsonRpcRequest(int time) {
        Map<String, Object> args = new HashMap<>();
        args.put("time", time);
        return JsonRpc.getRequest("poweroff", args);
    }
}
