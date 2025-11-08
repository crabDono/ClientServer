package cp;

import core.Msg;
import exceptions.IllegalMsgException;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class CPCommandResponseMsg extends CPMsg {
    protected static final String CP_CMD_RES_HEADER = "command_response";
    private int id;
    private String response;
    private long crc;

    public CPCommandResponseMsg() {
    }

    public CPCommandResponseMsg(int id, String response) {
        this.id = id;
        this.response = response;
    }

    public int getId() {
        return id;
    }

    public String getResponse() {
        return response;
    }

    @Override
    protected void create(String data) {
        String crcData = this.id + " " + this.response;
        Checksum checksum = new CRC32();
        checksum.update(crcData.getBytes());
        this.crc = checksum.getValue();

        data = CP_CMD_RES_HEADER + " " + crcData + " " + this.crc;
        super.create(data);
    }

    @Override
    protected Msg parse(String sentence) throws IllegalMsgException {
        if (!sentence.startsWith(CP_CMD_RES_HEADER)) {
            throw new IllegalMsgException();
        }
        String[] parts = sentence.split("\\s+", 4);
        if (parts.length != 4) {
            throw new IllegalMsgException();
        }

        try {
            this.id = Integer.parseInt(parts[1]);
            this.response = parts[2];
            long receivedCrc = Long.parseLong(parts[3]);

            String crcData = this.id + " " + this.response;
            Checksum checksum = new CRC32();
            checksum.update(crcData.getBytes());
            this.crc = checksum.getValue();

            if (receivedCrc != this.crc) {
                throw new IllegalMsgException();
            }
        } catch (NumberFormatException e) {
            throw new IllegalMsgException();
        }
        return this;
    }
}
