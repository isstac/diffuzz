import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class DummyPrintWriter extends PrintWriter {

    StringBuilder bs;

    public DummyPrintWriter() throws FileNotFoundException {
        this("");
        bs = new StringBuilder();
    }

    public DummyPrintWriter(String fileName) throws FileNotFoundException {
        super(System.out);
    }

    @Override
    public PrintWriter append(CharSequence csq) {
        if (csq != null)
            bs.append(csq.toString());
        return this;
    }

    public int getLength() {
        return bs.length();
    }

    public void printConsole() {
        System.out.println(bs.toString());
    }

}
