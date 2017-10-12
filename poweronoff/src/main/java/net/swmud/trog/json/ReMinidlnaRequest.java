package net.swmud.trog.json;

public class ReMinidlnaRequest {
    public JsonRpc.JsonRequest getJsonRpcRequest() {

        return JsonRpc.getRequest("reminidlna", "");
    }
}
