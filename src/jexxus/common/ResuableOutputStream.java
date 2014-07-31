package jexxus.common;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class ResuableOutputStream extends GZIPOutputStream {

  public ResuableOutputStream(OutputStream out) throws IOException {
    super(out);
  }

  @Override
  public void finish() throws IOException {
    super.finish();
    flush();
    
    def.reset();
  }

}
