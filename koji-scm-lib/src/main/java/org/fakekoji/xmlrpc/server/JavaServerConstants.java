package org.fakekoji.xmlrpc.server;

public class JavaServerConstants {
    public static final String xPortAxiom = "XPORT";
    public static final String dPortAxiom = "DPORT";
    public static final int DFAULT_RP2C_PORT = 9848;
    public static final int DFAULT_DWNLD_PORT = deductDwPort(DFAULT_RP2C_PORT);

    public static int deductDwPort(int xport) {
        return xport + 1;
    }

}
