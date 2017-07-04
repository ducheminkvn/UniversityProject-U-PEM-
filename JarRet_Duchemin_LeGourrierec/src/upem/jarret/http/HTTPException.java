package upem.jarret.http;

import java.io.IOException;

/**
 * 
 * @author Duchemin Kevin
 * @author Le Gourrierec Maugan
 */

public class HTTPException extends IOException {

    private static final long serialVersionUID = -1810727803680020453L;

    public HTTPException() {
        super();
    }

    public HTTPException(String s) {
        super(s);
    }

    public static void ensure(boolean b, String string) throws HTTPException {
        if (!b)
            throw new HTTPException(string);

    }
}