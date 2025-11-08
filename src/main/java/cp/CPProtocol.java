package cp;

import core.*;
import exceptions.*;
import phy.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class CPProtocol extends Protocol {
    private static final int CP_TIMEOUT = 2000;
    private static final int CP_HASHMAP_SIZE = 20;
    private int cookie;
    private int id;
    private PhyConfiguration PhyConfigCommandServer;
    private PhyConfiguration PhyConfigCookieServer;
    private final PhyProtocol PhyProto;
    private final cp_role role;
    HashMap<PhyConfiguration, Cookie> cookieMap;
    ArrayList<CPCommandMsg> pendingCommands;
    Random rnd;

    private enum cp_role {
        CLIENT, COOKIE, COMMAND
    }

    // Constructor for clients
    public CPProtocol(InetAddress rname, int rp, PhyProtocol phyP) throws UnknownHostException {
        this.PhyConfigCommandServer = new PhyConfiguration(rname, rp, proto_id.CP);
        this.PhyProto = phyP;
        this.role = cp_role.CLIENT;
        this.cookie = -1;
    }
    // Constructor for servers
    public CPProtocol(PhyProtocol phyP, boolean isCookieServer) {
        this.PhyProto = phyP;
        if (isCookieServer) {
            this.role = cp_role.COOKIE;
            this.cookieMap = new HashMap<>();
            this.rnd = new Random();
        } else {
            this.role = cp_role.COMMAND;
            this.pendingCommands = new ArrayList<>();
        }
    }

    public void setCookieServer(InetAddress rname, int rp) throws UnknownHostException {
        this.PhyConfigCookieServer = new PhyConfiguration(rname, rp, proto_id.CP);
    }


    @Override
    public void send(String s, Configuration config) throws IOException, IWProtocolException {


        if (cookie < 0) {
            // Request a new cookie from server
            // Either updates the cookie attribute or returns with an exception
            requestCookie();
        }

        // Task 1.2.1: complete send method
        this.id++;
        CPCommandMsg msg = new CPCommandMsg(this.cookie, this.id, s);
        msg.create(null);
        this.PhyProto.send(new String(msg.getDataBytes()), this.PhyConfigCommandServer);
    }

    @Override
    public Msg receive() throws IOException, IWProtocolException {
        CPMsg cpmIn = null;

        // Task 1.2.1: implement receive method
        try {
            while (true) { // Loop to handle unexpected messages
                Msg in = this.PhyProto.receive(3000); // a. Wait for 3 seconds

                // Ignore messages not for this protocol
                if (((PhyConfiguration) in.getConfiguration()).getPid() != proto_id.CP) {
                    continue;
                }

                try {
                    // b. Parse the message
                    cpmIn = (CPMsg) new CPMsg().parse(in.getData());

                    // c. Check if it's a command response and if the ID matches
                    if (cpmIn instanceof CPCommandResponseMsg) {
                        CPCommandResponseMsg responseMsg = (CPCommandResponseMsg) cpmIn;
                        if (responseMsg.getId() == this.id) {
                            // d, e. ID matches, return the message to the client
                            return responseMsg;
                        }
                    }
                    // If not a matching response, discard and continue waiting
                } catch (IllegalMsgException e) {
                    // b. If parser throws an exception, discard the message
                    continue;
                }
            }
        } catch (SocketTimeoutException e) {
            // Timeout occurred, return null or throw an exception as per higher-level logic needs.
            // For now, returning null as cpmIn is null by default.
        }

        // Task 2.1.1: enhance receive method

        return cpmIn;
    }

    // CookieServer processing of incoming messages
    // Only CookieCommandMsg are processed, all others are ignored
    private Msg command_process(CPMsg cpmIn) throws IWProtocolException {
        CPCommandMsg stored = null;

        return stored;
    }


    // Processing of the CookieRequestMsg
    private void cookie_process(CPMsg cpmIn) throws IWProtocolException, IOException {

    }


    // Method for the client to request a cookie
    public void requestCookie() throws IOException, IWProtocolException {
        CPCookieRequestMsg reqMsg = new CPCookieRequestMsg();
        reqMsg.create(null);
        Msg resMsg = new CPMsg();

        boolean waitForResp = true;
        int count = 0;
        while(waitForResp && count < 3) {
            this.PhyProto.send(new String(reqMsg.getDataBytes()), this.PhyConfigCookieServer);

            try {
                Msg in = this.PhyProto.receive(CP_TIMEOUT);
                if (((PhyConfiguration) in.getConfiguration()).getPid() != proto_id.CP)
                    continue;
                resMsg = ((CPMsg) resMsg).parse(in.getData());
                if(resMsg instanceof CPCookieResponseMsg)
                    waitForResp = false;
            } catch (SocketTimeoutException e) {
                count += 1;
            } catch (IWProtocolException ignored) {
            }
        }

        if(count == 3)
            throw new CookieRequestException();
        if(resMsg instanceof CPCookieResponseMsg && !((CPCookieResponseMsg) resMsg).getSuccess()) {
            throw new CookieRequestException();
        }
         assert resMsg instanceof CPCookieResponseMsg;
         this.cookie = ((CPCookieResponseMsg)resMsg).getCookie();
    }
}

class Cookie {
    private final long timeOfCreation;
    private final int cookieValue;

    public Cookie(long toc, int c) {
        this.timeOfCreation = toc;
        this.cookieValue = c;
    }

    public long getTimeOfCreation() {
        return timeOfCreation;
    }

    public int getCookieValue() { return cookieValue;}
}

