package com.btcc.spot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WebSocketClient {
    private static final Logger log = LoggerFactory.getLogger(WebSocketClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static String ACCESS_KEY="Your access key here";
    private static String SECRET_KEY="Your secret key here";
    private static String URL = "https://websocket.btcchina.com";

    public static void main(String[] args0) throws URISyntaxException {
        initUserClaim();

        IO.Options opts = new IO.Options();
        opts.reconnection = true;

        Socket socket = IO.socket(URL, opts);
        socket.on(Socket.EVENT_CONNECT, args -> {
            log.info("Connected.");

            socket.emit("subscribe", "marketdata_cnybtc");
            socket.emit("subscribe", "marketdata_cnyltc");
            
            socket.emit("subscribe", "grouporder_cnybtc");
            socket.emit("subscribe", "grouporder_cnyltc");
            
            socket.emit("private", Arrays.asList(payload(), sign()));
        })
        .on("message", args -> log.info("message: {}", args[0]))
        .on("trade", new JsonLogger("trade"))
        .on("ticker", new JsonLogger("ticker"))
        .on("grouporder", new JsonLogger("grouporder"))
        .on("order", new JsonLogger("order"))
        .on("account_info", new JsonLogger("account_info"))
        .on(Socket.EVENT_DISCONNECT, args -> log.info("Disconnected."));

        socket.connect();
    }

    private static class JsonLogger implements Emitter.Listener {

        private String prefix;

        public JsonLogger(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void call(Object... args) {
            JSONObject json = (JSONObject) args[0];
            log.info("{}: {}",prefix, json);
        }
    }

    private static class JsonRPC {
        public String tonce;
        public String accesskey;
        public String requestmethod;
        public String id;
        public String method;
        public List<String> params;
    }

    private static JsonRPC userClaim;

    private static void initUserClaim() {
        String tonce = "" + (System.currentTimeMillis() * 1000);
        userClaim = new JsonRPC();
        userClaim.tonce = tonce;
        userClaim.accesskey = ACCESS_KEY;
        userClaim.requestmethod = "post";
        userClaim.id = tonce;
        userClaim.method = "subscribe";
        userClaim.params = Arrays.asList("order_cnybtc", "order_cnyltc", "account_info");
    }

    private static String payload() {
        try {
            return objectMapper.writeValueAsString(userClaim);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sign() {
        String params = Stream.of(
                "tonce=" + userClaim.tonce,
                "accesskey=" + userClaim.accesskey,
                "requestmethod=" + userClaim.requestmethod,
                "id=" + userClaim.id,
                "method=" + userClaim.method,
                "params=" + userClaim.params.stream().collect(Collectors.joining(","))
        ).collect(Collectors.joining("&"));
        String hash = HmacUtils.hmacSha1Hex(SECRET_KEY, params);
        String userpass = ACCESS_KEY + ":" + hash;
        return Base64.encodeBase64String(userpass.getBytes());
    }
}
