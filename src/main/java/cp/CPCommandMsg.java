package cp;

import exceptions.IllegalMsgException;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class CPCommandMsg extends CPMsg {
    protected static final String CP_CMD_HEADER = "command";
    private int cookie;
    private int id;
    private String command;
    private long crc;

    public CPCommandMsg() {
    }

    public CPCommandMsg(int cookie, int id, String command) {
        this.cookie = cookie;
        this.id = id;
        this.command = command;
    }

    public int getCookie() {
        return cookie;
    }

    public int getId() {
        return id;
    }

    public String getCommand() {
        return command;
    }

    @Override
    protected void create(String data) {
        String crcData = this.cookie + " " + this.id + " " + this.command;
        Checksum checksum = new CRC32();
        checksum.update(crcData.getBytes());
        this.crc = checksum.getValue();

        data = CP_CMD_HEADER + " " + crcData + " " + this.crc;
        super.create(data);
    }

    @Override
    protected CPMsg parse(String sentence) throws IllegalMsgException {
        if (!sentence.startsWith(CP_CMD_HEADER)) {
            throw new IllegalMsgException();
        }
        String[] parts = sentence.split("\\s+", 5);
        if (parts.length != 5) {
            throw new IllegalMsgException();
        }

        try {
            this.cookie = Integer.parseInt(parts[1]);
            this.id = Integer.parseInt(parts[2]);
            this.command = parts[3];
            long receivedCrc = Long.parseLong(parts[4]);

            String crcData = this.cookie + " " + this.id + " " + this.command;
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
