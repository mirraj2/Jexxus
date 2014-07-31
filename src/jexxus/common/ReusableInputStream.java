package jexxus.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class ReusableInputStream extends GZIPInputStream {

  public ReusableInputStream(InputStream in) throws IOException {
    super(in);
  }

  public void getReady() {
    inf.reset();
  }

}
