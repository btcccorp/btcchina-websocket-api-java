/*
 * An example for Java Socket.IO Client
 */
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

public class WebsocketClient {

    public static void main(String[] args) {
        try {
            IO.Options opt = new IO.Options();
            opt.reconnection = true;
            final Socket socket = IO.socket("https://websocket.btcchina.com", opt);

            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    System.out.println("connected");
                    socket.emit("subscribe", "marketdata_cnybtc"); // subscribe
                    socket.emit("subscribe", "marketdata_cnyltc"); // subscribe another market
                }
            }).on("trade", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    JSONObject json = (JSONObject) args[0]; //receive the trade message
                    System.out.println(json); 
                }
            }).on("ticker", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    JSONObject json = (JSONObject) args[0];//receive the ticker message
                    System.out.println(json);
                }
            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    System.out.println("disconnected");
                }
            });
            socket.connect();
        } catch (URISyntaxException ex) {
            Logger.getLogger(WebsocketClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}